# BlogHub 新功能设计：评论互动 + 徽章系统 + API 文档

## 概述

为 BlogHub 新增三个功能模块：
1. **评论点赞/点踩** — 独立反应表，支持游客和登录用户
2. **用户徽章/成就系统** — 数据库驱动规则，自动触发授予
3. **Knife4j API 文档** — 集成 Swagger UI 增强工具

---

## 功能一：评论点赞/点踩

### 数据库

```sql
CREATE TABLE comment_reaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT DEFAULT NULL,
    ip VARCHAR(50) DEFAULT NULL,
    reaction_type TINYINT NOT NULL COMMENT '1=赞, 0=踩',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_comment (comment_id, user_id),
    UNIQUE KEY uk_ip_comment (comment_id, ip),
    INDEX idx_comment_id (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### API 设计

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/comments/{id}/reaction` | 点赞/点踩 | 可选 |
| DELETE | `/api/comments/{id}/reaction` | 取消反应 | 可选 |
| GET | `/api/comments/{id}/reactions` | 获取反应统计 | 否 |

**请求体（POST）：**
```json
{ "type": 1 }
```

**响应（GET）：**
```json
{
  "likeCount": 12,
  "dislikeCount": 3,
  "myReaction": 1
}
```

### 业务逻辑

- 登录用户：`user_id` 唯一索引防重
- 游客：`ip` 唯一索引防重（`user_id` 为 NULL）
- 点赞/点踩互斥：点击踩会替换之前的赞，反之亦然
- 取消反应：删除对应记录
- 白名单：`/api/comments/*/reaction` 加入 `WebMvcConfig` excludePathPatterns

### 评论响应扩展

`CommentServiceImpl.getPostComments` 和 `getCommentReplies` 返回数据增加：
```json
{
  "likeCount": 12,
  "dislikeCount": 3,
  "myReaction": 1
}
```

---

## 功能二：用户徽章/成就系统

### 数据库

```sql
CREATE TABLE badge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(255) NOT NULL,
    description VARCHAR(200) NOT NULL,
    category VARCHAR(20) NOT NULL COMMENT 'interaction/content/activity/special',
    sort_order INT DEFAULT 0,
    is_active TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE badge_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    badge_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    condition_json TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (badge_id) REFERENCES badge(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_badge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    badge_id BIGINT NOT NULL,
    earned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_new TINYINT DEFAULT 1,
    UNIQUE KEY uk_user_badge (user_id, badge_id),
    FOREIGN KEY (badge_id) REFERENCES badge(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### condition_json 格式

```json
{
  "metric": "total_comments",
  "op": ">=",
  "value": 1
}
```

**支持的 metric：**
- `total_comments` — 用户评论总数
- `total_likes_received` — 收到的总点赞数
- `total_posts` — 发布文章数
- `total_bookmarks_received` — 文章被收藏次数
- `max_post_views` — 单篇文章最高阅读量
- `consecutive_login_days` — 连续登录天数
- `registered_days` — 注册天数

**支持的 op：** `>=`, `<=`, `=`

### 初始徽章列表

| 分类 | 名称 | 图标 | 条件 |
|------|------|------|------|
| interaction | 初次发声 | 🌟 | total_comments >= 1 |
| interaction | 评论达人 | 💬 | total_comments >= 50 |
| interaction | 人气王 | ❤️ | total_likes_received >= 100 |
| content | 笔耕不辍 | ✍️ | total_posts >= 10 |
| content | 收藏焦点 | 📚 | total_bookmarks_received >= 50 |
| content | 爆款作者 | 🔥 | max_post_views >= 1000 |
| activity | 连续签到 7 天 | 🔗 | consecutive_login_days >= 7 |
| activity | 博龄一年 | 🎂 | registered_days >= 365 |
| special | 管理员 | 👑 | 角色自动授予（无规则） |
| special | 早期用户 | 🌱 | registered_days >= 365（系统上线前注册） |

### 核心流程

```
用户行为（评论/点赞/登录等）
  → 更新行为计数（Redis 或直接查 DB）
  → BadgeCheckService.checkAndGrant(userId, eventType)
    → 查询 badge_rule WHERE event_type = ?
    → 遍历规则，解析 condition_json
    → 查询用户当前指标值
    → 指标满足 AND user_badge 中无记录 → 授予徽章
    → 创建站内通知（is_new=1）
```

### API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/badges` | 获取所有徽章定义 |
| GET | `/api/users/{id}/badges` | 获取用户已获得徽章 |
| PUT | `/api/users/{id}/badges/mark-read` | 标记徽章通知已读 |

### 前端展示

**个人主页：** 新增「徽章」Tab，网格展示所有徽章，已获得的高亮显示，未获得的灰色 + 锁定图标。

**评论区：** 用户名右侧显示最多 3 个徽章图标，按 `sort_order` 排序，tooltip 显示徽章名称。

**通知：** 获得新徽章时创建站内通知，前端在通知中心显示「🎉 恭喜获得徽章：xxx」。

### WebMvcConfig 白名单更新

新增路径（可选认证）：
- `/api/badges`
- `/api/users/*/badges`

---

## 功能三：Knife4j API 文档

### 依赖

```xml
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi2-spring-boot-starter</artifactId>
    <parameter name="version">4.3.0</version>
</dependency>
```

### 配置

`application.yml`：
```yaml
knife4j:
  enable: true
  setting:
    language: zh_cn

springfox:
  documentation:
    enabled: true
```

### 需要注解的 Controller

| Controller | @Api tags | 接口数 |
|------------|-----------|--------|
| AuthController | 认证管理 | ~5 |
| PostController | 文章管理 | ~8 |
| CommentController | 评论管理 | ~5 |
| UserController | 用户管理 | ~4 |
| InteractionController | 社交互动 | ~6 |
| ExportController | 数据导出 | ~2 |
| FeedController | 订阅源 | ~2 |
| UploadController | 文件上传 | ~1 |
| HotTopicController | 热点话题 | ~2 |
| AlbumController | 相册管理 | ~5 |

### 访问地址

`http://localhost:8080/doc.html`

---

## 实施顺序

1. Knife4j 集成（最快，30 分钟）
2. 评论点赞/点踩（中等，1-2 小时）
3. 用户徽章系统（最复杂，3-4 小时）

## 涉及文件

### 后端新增
- `entity/CommentReaction.java`
- `entity/Badge.java`
- `entity/BadgeRule.java`
- `entity/UserBadge.java`
- `mapper/CommentReactionMapper.java`
- `mapper/BadgeMapper.java`
- `mapper/BadgeRuleMapper.java`
- `mapper/UserBadgeMapper.java`
- `service/CommentReactionService.java`
- `service/BadgeService.java`
- `service/BadgeCheckService.java`
- `controller/CommentReactionController.java`
- `controller/BadgeController.java`
- `dto/ReactionRequest.java`

### 后端修改
- `service/impl/CommentServiceImpl.java` — 返回 reaction 数据
- `config/WebMvcConfig.java` — 白名单更新
- 所有 Controller — 添加 `@Api` / `@ApiOperation` 注解

### 前端新增
- `views/UserBadges.vue` — 徽章墙页面
- `components/CommentReactions.vue` — 点赞/点踩组件
- `components/BadgeIcons.vue` — 徽章图标展示组件
- `api/badgeApi.js` — 徽章 API

### 前端修改
- `components/BlogComments.vue` — 集成 CommentReactions + BadgeIcons
- `views/UserProfile.vue` — 新增徽章 Tab
- `router/index.js` — 新增路由
