# 标签系统升级设计文档

## 概述

将 BlogHub 标签从 `Post.tags` 逗号分隔字符串升级为独立 `tag` 表 + `post_tag` 关联表的规范化结构，并新增标签云页面，提升标签的可管理性和前端交互体验。

## 背景

当前标签存储在 `Post.tags` 字段中，格式为逗号分隔字符串（如 `"Vue, 前端"`）。存在以下问题：

- 无法独立管理标签（重命名/合并/统计）
- 标签筛选是客户端行为（仅过滤已加载文章）
- 没有独立的标签发现页面
- 推荐相关文章用 `LIKE '%tag%'` 匹配，性能差且不精确

## 数据模型

### tag 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(50) | NOT NULL | 标签显示名，如 "Vue.js" |
| slug | VARCHAR(100) | NOT NULL, UNIQUE | URL 友好标识，如 "vuejs" |
| post_count | INT | DEFAULT 0 | 文章数（冗余，避免频繁 COUNT） |
| created_at | DATETIME | | |
| updated_at | DATETIME | | |

### post_tag 关联表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| post_id | BIGINT | PK, FK → posts.id | 文章 ID |
| tag_id | BIGINT | PK, FK → tag.id | 标签 ID |

复合主键 `(post_id, tag_id)`，无独立 ID。创建唯一索引避免重复关联。

### Slug 生成策略

- 自动从 `name` 生成：`slug = name.toLowerCase().trim().replace(/[^a-z0-9\u4e00-\u9fff]+/g, '-').replace(/^-|-$/g, '')`
- 英文标签："Vue.js" → `vuejs` / "TypeScript" → `typescript`
- 中文标签："前端开发" → slug 保持 "前端开发"（浏览器会自动 URL 编码，SEO 友好）
- 支持用户手动编辑 slug（在 tag 管理接口中可 PUT 修改）
- 唯一约束由数据库 `UNIQUE` 保证

### 存量数据迁移

执行 SQL 脚本将 `Post.tags` 中的逗号分隔值拆分为 tag 记录，写入 post_tag：

```
INSERT INTO tag (name, slug, post_count)
SELECT DISTINCT TRIM(value), urlify(TRIM(value)), COUNT(*) ...

INSERT INTO post_tag (post_id, tag_id)
SELECT p.id, t.id FROM posts p JOIN tag t ON FIND_IN_SET(t.name, p.tags) ...
```

迁移后 `Post.tags` 字段保留（前向兼容旧代码调用），后续可择机移除。

## API 设计

### 新增 TagController

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/tags` | 获取所有标签（含文章数），按 post_count 降序 | 公开 |
| GET | `/api/tags/{slug}` | 获取单个标签信息 | 公开 |
| GET | `/api/tags/{slug}/posts` | 获取该标签下的文章列表（分页） | 公开 |

### 修改 PostController

- `GET /api/posts?tag=xxx` — 增加 `tag` 查询参数，按 slug 筛选文章
- `POST /api/posts` — 创建时同时写入 post_tag 关联（request 增加 `tags` 字段支持 array 或逗号字符串）
- `PUT /api/posts/{id}` — 更新时同步更新 post_tag 关联

### 新增/修改 DTO

- `TagResponse` — id, name, slug, postCount
- `CreatePostRequest` / `UpdatePostRequest` — tags 字段保持逗号字符串兼容，后端解析为 array 写入关联表

## 前端变更

### 文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/views/TagCloud.vue` | **新建** | /tags 标签云页面 |
| `src/views/PostEditor.vue` | 修改 | 标签输入改为 autocomplete 选择器 |
| `src/views/PostEdit.vue` | 修改 | 同上 |
| `src/views/BlogList.vue` | 修改 | 标签筛选改为服务端过滤 + 支持 `/tags/:slug` 路由进入 |
| `src/views/BlogPost.vue` | 修改 | 标签点击跳转到 `/tags/:slug` |
| `src/composables/usePosts.js` | 修改 | 新增 fetchTags() 等方法 |
| `src/router/index.js` | 修改 | 新增 `/tags` 和 `/tags/:slug` 路由 |

### 标签云页面 (TagCloud.vue)

路由 `/tags`，展示所有标签为弹性布局的标签块。每个标签：
- 按文章数比例决定字号（12px ~ 32px）
- 显示标签名 + 文章数
- 点击跳转到 `/tags/:slug`

### 标签选择器

替换 PostEditor / PostEdit 中的 `<el-input>` 为 `el-autocomplete` 组件：
- 输入时从 `/api/tags` 搜索匹配已有标签
- 选中后以 `el-tag` 展示，带 ✕ 删除
- 全新标签直接输入，提交时自动创建
- 提交前前端将 `string[]` 用 `.join(',')` 转为逗号字符串，保持与后端 `CreatePostRequest.tags`（String）兼容，后端无需改 DTO

### BlogList.vue 标签筛选

- 标签筛选行从客户端 `computed` 过滤改为调用 `GET /api/posts?tag=slug`
- 从 `/tags/:slug` 进入时自动选中对应标签

## 后端文件清单

| 文件 | 操作 |
|------|------|
| `entity/Tag.java` | 新建 |
| `mapper/TagMapper.java` | 新建 |
| `service/TagService.java` | 新建 |
| `service/impl/TagServiceImpl.java` | 新建 |
| `controller/TagController.java` | 新建 |
| `dto/TagResponse.java` | 新建 |
| `service/PostService.java` | 修改 — 新增 tag 相关方法 |
| `service/impl/PostServiceImpl.java` | 修改 — 写入/更新 tag 关联 + 按 tag 筛选 |
| `dto/CreatePostRequest.java` | 不改（tags 仍为 String，后端解析） |
| `dto/UpdatePostRequest.java` | 不改 |

## 迁移脚本

```sql
-- 创建标签表
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    post_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建文章-标签关联表
CREATE TABLE IF NOT EXISTS post_tag (
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 迁移现有标签数据（需在应用层或存储过程执行拆分逻辑）
```

## 后端 DTO 兼容说明

`CreatePostRequest.tags` 和 `UpdatePostRequest.tags` **保持不变**（String 类型）。
- 前端标签选择器产生 `string[]` → 提交前 `.join(',')` 转为逗号字符串
- `PostServiceImpl.createPost()` 和 `updatePost()` 中新增逻辑：解析 tags 字符串为数组，写入 `tag` 表和 `post_tag` 关联表
- `PostResponse.tags` 仍返回逗号字符串，保持前端 `usePosts.js` 的 `split(',')` 逻辑不变

## 优先级与依赖

1. 后端：建表 + 实体 + Mapper + Service + Controller + 修改 PostService
2. 迁移脚本：处理存量数据
3. 前端：TagCloud.vue + 编辑器选择器 + 路由 + 列表页改造

无外部依赖，各步骤可串行执行。
