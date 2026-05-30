<div align="center">
  <h1>📝 BlogHub</h1>
  <p>全栈技术博客系统 — Spring Boot + Vue 3 + Element Plus</p>
</div>

## 🚀 简介

BlogHub 是一个功能完整的技术博客系统，支持文章管理、评论互动、社交功能、管理后台等全链路博客运营需求。

## 🛠 技术栈

| 层 | 技术 |
|------|------|
| **前端** | Vue 3 + Vite + Element Plus + Pinia + Vue Router |
| **后端** | Spring Boot + MyBatis-Plus + MySQL |
| **认证** | JWT（Access Token + Refresh Token） |
| **热点数据** | 60s API + GitHub API |
| **构建** | Maven（后端） / Vite（前端） |

## ✨ 功能总览

### 📰 文章系统
- Markdown 编辑器（实时预览、图片拖拽/粘贴上传）
- 文章 CRUD + 自定义 Slug + 封面图
- 草稿 / 定时发布（每分钟自动扫描到期文章）
- 版本历史（每次编辑自动快照，最多 50 版）
- 相关文章推荐（基于标签匹配）
- 文章归档（按年月时间线）
- RSS 订阅（`/feed.xml`）+ Sitemap（`/sitemap.xml`）

### 💬 评论系统
- 抖音风格扁平评论（两层结构，`A 回复 B` 格式）
- 折叠懒加载（默认折叠子回复，点击展开按需加载）
- 支持匿名 + 登录用户评论
- 赞 / 踩互动
- @提及用户自动通知
- HTML 清洗防 XSS
- 管理员后台管理

### 🏷️ 标签系统
- 规范化标签表（`tag` + `post_tag` 关联）
- 标签自动创建（`findOrCreate`）
- 标签云（字号按文章数比例缩放）
- 按标签筛选文章

### 🔐 用户与认证
- 注册 / 登录（BCrypt 加密，兼容旧 MD5 自动升级）
- JWT Token + 7 天 Refresh Token
- 登录限流（5 次/60s）、注册限流（3 次/60s）
- 个人中心（信息编辑、密码修改、我的文章、我的评论）
- 管理员用户管理（创建/编辑/禁用/角色分配）

### 🤝 社交互动
- 文章点赞（未登录按 IP，已登录按用户）
- 文章收藏 + 收藏列表
- 用户关注 + 用户主页（粉丝/关注数）
- 点赞排行榜（首页展示 TOP10）
- 分享（微博/知乎/微信/复制链接）

### 🔔 通知系统
- 自动通知：点赞、评论、回复、关注、@提及
- 头栏铃铛 + 未读角标
- 全部标记已读

### 📊 管理后台
- 仪表盘（文章/用户/评论统计 + ECharts 7 日趋势图）
- 用户管理、评论管理、操作日志审计
- 图片管理（查看/删除，显示文章来源和用途）
- 数据导出 CSV（文章/用户/评论）

### 📸 宝宝相册
- 相册管理（创建/编辑/删除相册，支持宝宝昵称和出生日期）
- 照片/视频上传（拖拽上传，支持 jpg/png/gif/webp/mp4/webm）
- 瀑布流展示 + 时间线视图（按月龄分组）
- 图片灯箱预览 + 视频播放
- 成长时间线（根据出生日期自动计算月龄）

### 🔥 每日热点
- 多平台热搜聚合（微博、知乎、抖音、B站、头条、GitHub）
- 每 2 小时自动刷新 + 手动刷新
- 平台 Tab 切换，排名前 3 红色渐变高亮
- 热度值智能格式化（k/w）
- 点击跳转原平台链接

### 🔍 其他
- 全文搜索（标题/内容/描述模糊匹配）
- SEO 优化（每页独立 title + description）
- 响应式布局 + 深色模式

## 🏗 项目结构

```
blog-hub/
├── blog-backend/                  # Spring Boot 后端
│   ├── src/main/java/com/blog/
│   │   ├── config/                # JWT、拦截器、跨域、安全
│   │   ├── controller/            # REST API 控制器
│   │   ├── service/               # 业务逻辑层
│   │   ├── mapper/                # MyBatis-Plus Mapper
│   │   ├── entity/                # 数据实体
│   │   ├── dto/                   # 请求/响应 DTO
│   │   ├── common/                # 公共工具、响应封装
│   │   └── exception/             # 全局异常处理
│   └── src/main/resources/
│       ├── application.yml        # 应用配置
│       └── db/                    # 数据库迁移脚本
│
├── vue3-elementplus-starter/      # Vue 3 前端
│   ├── src/
│   │   ├── views/                 # 页面组件
│   │   ├── components/            # 公共组件
│   │   ├── composables/           # 组合式函数
│   │   ├── layouts/               # 布局组件
│   │   ├── router/                # 路由 + 守卫
│   │   ├── stores/                # Pinia 状态
│   │   ├── api/                   # API 请求封装
│   │   └── styles/                # 全局样式
│   └── public/
│
├── scripts/                       # 辅助脚本（DB 扫描等）
├── mysql.js                       # MySQL 命令行助手
└── docs/                          # 设计文档
```

## 🚦 本地运行

### 前置条件

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven

### 1. 数据库

```bash
mysql -u root -p < blog-backend/sql/init.sql
```

### 2. 后端

```bash
cd blog-backend
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`

### 3. 前端

```bash
cd vue3-elementplus-starter
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`

## 🔗 API 前缀

所有接口以 `/api` 开头，前端通过 `request.js` 自动代理到 `http://localhost:8080`。

## 📄  License

MIT
