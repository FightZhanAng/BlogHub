# 博客项目迭代设计 — 内容生态 + 社交互动 + 数据统计

> 日期: 2025-05-23  
> 状态: 待实现  

---

## 概述

在第一轮（UX + 架构 + 安全）基础上，继续从内容生态、社交互动、数据统计三个维度增强博客平台。三部分相互独立，可并行推进。

---

## 1. 内容生态

### 1.1 字数统计 / 阅读时长

**前端计算**，在 `BlogPost.vue` 文章标题下方展示：

- 字数：`post.content.length`
- 阅读时长：`Math.ceil(字数 / 500)`
- 格式：`📝 约 1,200 字 · 阅读 3 分钟`
- 纯前端改动，不涉及后端

### 1.2 代码高亮主题切换

`BlogPost.vue` 文章顶部增加亮/暗代码主题切换按钮（仅当页面包含 `<pre><code>` 时显示）。

- 默认跟随 `prefers-color-scheme`
- 切换时给 `.markdown-body` 增加 class `code-dark` / `code-light`
- CSS 变量：
  - 暗色：`background: #1e1e2e; color: #cdd6f4`
  - 亮色：`background: #f8f9fa; color: #333`
- 偏好存入 `localStorage`，跨页面保持

### 1.3 图片上传

**后端** `POST /api/upload`：
- 接收 `multipart/form-data`，文件字段名 `file`
- 类型白名单：`jpg/png/gif/webp/svg`，最大 5MB
- 存储路径：`uploads/{yyyy}/{MM}/{uuid}.{ext}`
- 返回：`{ url: "/uploads/2025/05/xxx.png" }`
- Nginx 配置 `/uploads/` 静态文件访问

**前端** `PostEditor.vue`：
- 编辑工具栏增加图片按钮，调用原生 `<input type="file">`
- 选择后自动上传 → 光标位置插入 `![](/uploads/xxx.png)`
- 上传中显示 loader，完成后替换为 Markdown 文本

### 1.4 文章封面图

**数据库**：`posts` 表新增 `cover_image VARCHAR(500)`

**后端**：
- `CreatePostRequest` / `UpdatePostRequest` 增加 `coverImage`
- `PostResponse` 增加 `coverImage`

**前端**：
- `PostEditor.vue` 表单增加封面图上传区域（预览 + 删除）
- `BlogCard.vue` 增加封面展示（有图则图，无图用渐变色块）
- `BlogPost.vue` 文章顶部展示封面大图（可选）

### 1.5 文章推荐

**后端** `GET /api/posts/{slug}/related`：
- 取当前文章的 tags 列表
- 查询同标签的已发布文章，排除自身，`LIMIT 4`
- 按匹配标签数排序，同标签数按阅读量降序
- 返回 `List<PostResponse>`

**前端**：
- `BlogPost.vue` 文章底部增加「相关文章」区块
- 使用 `el-row` 展示 2×2 或 4 列迷你卡片

### 1.6 定时发布

**数据库**：`posts` 表新增 `scheduled_at DATETIME`

**后端**：
- 创建/更新文章时可设置 `scheduledAt`
- 定时任务 `@Scheduled(fixedRate = 60000)`：
  ```sql
  UPDATE posts SET status = 1 WHERE status = 0 AND scheduled_at IS NOT NULL AND scheduled_at <= NOW()
  ```
- `PostService.getPublishedPosts` 排除 `status=0 AND scheduled_at>NOW()` 的文章

**前端**：
- 编辑表单增加「定时发布」开关 + `el-date-picker type="datetime"`
- 草稿列表区分「草稿」与「待发布」
- 待发布文章在列表展示计划发布时间

### 1.7 草稿版本历史

**数据库**：新建 `post_versions` 表

```sql
CREATE TABLE post_versions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  title VARCHAR(255),
  content TEXT,
  version INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_post_id (post_id)
);
```

**后端**：
- `PostService` 每次 `save` / `update` 时自动插入版本记录（最大保留 50 版，超出删最旧）
- `GET /api/posts/{id}/versions` — 版本列表（version + created_at）
- `GET /api/posts/{id}/versions/{version}` — 某一版本的完整内容
- `POST /api/posts/{id}/versions/{version}/restore` — 恢复指定版本

**前端**：
- `PostEditor.vue` 增加「历史版本」按钮
- 弹窗展示版本列表（时间线样式）
- 点击预览 → 确认恢复

### 1.8 文章系列/专栏

**数据库**：
```sql
CREATE TABLE series (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  cover_image VARCHAR(500),
  author_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE series_posts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  series_id BIGINT NOT NULL,
  post_id BIGINT NOT NULL,
  sort_order INT DEFAULT 0,
  UNIQUE KEY uk_series_post (series_id, post_id)
);
```

**后端**：
- `GET /api/series` — 系列列表
- `POST /api/series` — 创建系列
- `GET /api/series/{id}` — 系列详情（含文章列表）
- 创建/更新文章时可选择 `seriesId` + `sortOrder`
- 文章详情返回 `series` 信息（上一章/下一章导航）

**前端**：
- 系列管理页面 `/series`（管理员）
- 文章编辑表单增加「所属系列」下拉选择 + 排序
- 文章页顶部展示系列面包屑（系列名 → 第 N 篇）
- 系列详情页 `/series/{id}` 展示系列文章列表 + 阅读进度

---

## 2. 社交互动

### 2.1 用户主页

**前端** `/user/:id`：

- 展示：头像 + 昵称 + 简介（bio）+ 统计数据（文章数/获赞/关注/粉丝）
- 文章列表 Tab（分页）
- 关注/取关按钮（已登录用户）
- 所有文章卡片上的作者名链接到用户主页

**后端** `GET /api/users/{id}/profile`：
- 返回 `UserDTO` + `postCount` + `likeCount` + `followerCount` + `followingCount`
- 如果请求者是已登录用户，额外返回 `isFollowing` 布尔值

**数据库**：`users` 表新增 `bio VARCHAR(500)`

### 2.2 关注作者

**数据库**：
```sql
CREATE TABLE follows (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  follower_id BIGINT NOT NULL,
  following_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_follow (follower_id, following_id)
);
```

**后端**：
- `POST /api/follows` — 关注 `{ userId }`
- `DELETE /api/follows/{userId}` — 取关
- `GET /api/users/{id}/followers?page&size` — 粉丝列表
- `GET /api/users/{id}/following?page&size` — 关注列表
- `GET /api/posts/following` — 当前用户关注的人的文章（分页）

**前端**：
- 用户主页关注/取关按钮
- 首页增加「关注」Tab，展示关注人最新文章

### 2.3 分享到社交平台

**前端** `BlogPost.vue` 文章底部：

- 微博：`window.open('https://service.weibo.com/share/share.php?title=...&url=...')`
- 知乎：`window.open('https://zhuanlan.zhihu.com/write?title=...')`
- 复制链接：`navigator.clipboard.writeText(url)` → `ElMessage.success('已复制')`
- 微信：使用 `qrcode` 库生成二维码弹窗
- 使用 `navigator.share` 作为移动端优先方案

**不涉及后端改动**

### 2.4 评论回复通知

**数据库**：
```sql
CREATE TABLE notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '接收者',
  type VARCHAR(20) NOT NULL COMMENT 'reply/like/follow',
  message VARCHAR(500) NOT NULL,
  related_id BIGINT COMMENT '关联资源 ID',
  is_read TINYINT(1) DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_read (user_id, is_read)
);
```

**后端**：
- `PostService.updateLikesCount` 触发时创建通知
- `CommentService.addComment` 回复他人时创建通知
- `FollowService.follow` 创建通知
- `GET /api/notifications?page&size` — 通知列表（未读优先）
- `GET /api/notifications/unread-count` — 未读数
- `PUT /api/notifications/read-all` — 全部标为已读

**前端**：
- 右上角铃铛图标 + 未读红点数字
- 点击弹出通知面板（最近 10 条）
- 点击通知跳转到对应文章/评论
- 点击「全部已读」按钮

### 2.5 阅读排行榜

**后端** `GET /api/posts/ranking?period=weekly`：
- `weekly`：过去 7 天 `views` 增量排序（需要 `post_views` 表，或直接用总 `views`）
- 简单实现：直接用总 `views` 排序取 Top 10
- 缓存 1 小时（本地 `ConcurrentHashMap` 或 `@Cacheable`）

**前端**：
- 首页右侧「热门文章」卡片
- 排名序号 + 标题 + 阅读量
- 点击跳转文章

---

## 3. 数据统计

### 3.1 Dashboard 仪表盘

**后端** `GET /api/dashboard/stats`：
```json
{
  "totalPosts": 42,
  "totalUsers": 128,
  "totalComments": 356,
  "todayPosts": 2,
  "todayUsers": 5,
  "totalViews": 15800,
  "totalLikes": 892,
  "recentTrend": [
    { "date": "2025-05-16", "posts": 1, "users": 3, "comments": 8 },
    ...
  ]
}
```

**前端** `/dashboard`：
- 4 个 stats 大卡片（文章数、用户数、评论数、阅读量）
- ECharts 趋势折线图（最近 7 天）
- 管理员 Dashboard 替代当前首页空白

### 3.2 访问量统计

**数据库**：
```sql
CREATE TABLE page_views (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT,
  page_url VARCHAR(500),
  visitor_ip VARCHAR(45),
  viewed_at DATE NOT NULL,
  INDEX idx_post_date (post_id, viewed_at)
);
```

**后端**：
- `POST /api/stats/pageview` — 前端上报阅读事件
- 防抖策略：同 IP + 同 PostId 30 秒内只记一次
- `GET /api/stats/pv/{postId}` — 文章 PV 趋势（按天分组）
- 不阻塞主流程，异步写入

**前端**：
- `BlogPost.vue` `onMounted` 时调用 `POST /api/stats/pageview`
- 文章标题下方展示 `👁️ xxx 次阅读`
- 文章详情页增加迷你 PV 趋势图（可选）

### 3.3 搜索词统计

**数据库**：
```sql
CREATE TABLE search_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  keyword VARCHAR(100) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**后端**：
- `PostService.getPublishedPosts` 收到 keyword 时异步写入 `search_logs`
- `GET /api/stats/search-keywords?top=20` — 热门搜索词排行

**前端**：
- Dashboard 中展示热门搜索词列表 / 标签云

### 3.4 简单埋点

**前端** `src/utils/tracker.js`：
```js
export function track(event, data = {}) {
  const payload = { event, data, url: location.href, ts: Date.now() }
  navigator.sendBeacon('/api/stats/track', JSON.stringify(payload))
}
```

**数据库**：
```sql
CREATE TABLE track_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  event VARCHAR(50) NOT NULL,
  page_url VARCHAR(500),
  data JSON,
  visitor_id VARCHAR(100),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_event_date (event, created_at)
);
```

**后端** `POST /api/stats/track`：
- 接收 JSON body，验证 `event` 字段，存入 `track_events`
- 事件列表：`page_view` / `post_read` / `comment_add` / `like_toggle` / `search`

**Dashboard 集成**：事件趋势曲线

### 3.5 导出报表

**后端**：
- `GET /api/export/posts?format=csv` — 导出文章
- `GET /api/export/comments?format=csv` — 导出评论
- `GET /api/export/users?format=csv` — 导出用户
- 响应 `Content-Type: text/csv; charset=UTF-8`
- 文件名 `posts_2025-05-23.csv`
- 仅管理员可访问

**前端**：
- Dashboard 页面各卡片增加「导出」按钮

---

## 实施顺序

```
Phase 1 — 内容生态基础
  字数统计/阅读时长 → 代码高亮主题 → 图片上传 → 封面图
  → 文章推荐 → 定时发布 → 版本历史 → 文章系列

Phase 2 — 社交互动
  用户主页 → 关注 → 分享 → 通知 → 排行榜

Phase 3 — 数据统计
  Dashboard → PV 统计 → 搜索词统计 → 埋点 → 导出
```
