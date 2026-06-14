<div align="center">
  <h1>📝 BlogHub</h1>
  <p>全栈技术博客系统 — Spring Boot + Vue 3 + Element Plus</p>
</div>

## 🚀 简介

BlogHub 是一个功能完整的技术博客系统，支持文章管理、评论互动、社交功能、AI 助手、管理后台等全链路博客运营需求。

## 🛠 技术栈

| 层 | 技术 |
|------|------|
| **前端** | Vue 3 + Vite + Element Plus + Pinia + Vue Router |
| **后端** | Spring Boot 2.7 + MyBatis-Plus + MySQL 8 |
| **缓存** | Redis（Spring Cache 注解，allkeys-lru 策略） |
| **认证** | JWT（Access Token + Refresh Token） |
| **AI** | MiMo V2.5（小米大模型，SSE 流式输出） |
| **定时任务** | XXL-Job 2.4（可视化管理、执行日志、失败重试） |
| **热点数据** | 60s API + GitHub API |
| **构建** | Maven（后端） / Vite（前端） / Docker Compose |
| **设计** | 杂志编辑风格 + 暗色/亮色双主题 |

## ✨ 功能总览

### 🤖 AI 助手
- 集成小米 MiMo V2.5 大模型（Pro / Flash / 多模态三款可选）
- SSE 流式输出，逐字显示 + 渐变光标动画
- 深度思考模式（Thinking），可折叠查看思考过程
- 图片输入理解（MiMo V2.5 多模态模型）
- 对话历史管理（按时间归档：今天/昨天/近7天/更早，可折叠分组）
- Markdown 渲染（代码高亮 + 折叠 + 复制）
- 暗色/亮色主题一键切换
- 消息复制、图片灯箱预览

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
- 点赞/点踩互动（支持游客 IP + 登录用户 ID 防重，显示用户操作状态）
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

### 🏆 用户徽章/成就系统
- 10 种徽章（互动类/内容类/活跃类/特殊类）
- 自动触发规则引擎（JSON 条件配置，数据库驱动）
- 评论首次发声、文章笔耕不辍、爆款作者、连续签到等成就
- 评论区用户徽章展示（最多 3 个）
- 个人主页徽章墙（已获得/未解锁状态）
- 管理员登录自动授予管理员徽章

### 📊 管理后台
- 仪表盘（文章/用户/评论统计 + ECharts 7 日趋势图）
- 用户管理、评论管理、操作日志审计
- 图片管理（查看/删除，显示文章来源和用途）
- 敏感词管理（增删改查、批量导入、热加载词库）
- 健康检查（MySQL/Redis/XXL-Job 实时状态监控）
- 数据导出 CSV（文章/用户/评论）

### 🔧 API 测试工具
- 网页版 Postman，所有登录用户可用
- 支持 GET/POST/PUT/DELETE 请求，通过后端代理转发（无 CORS 限制）
- 环境变量管理（`{{variable}}` 语法替换）
- 请求收藏集合（分组管理常用请求）
- 请求历史记录（自动保存，最近 100 条）
- Body 格式：JSON / Form / Multipart / Raw
- 响应展示：JSON 高亮、Headers、状态码/耗时/大小

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

### 🎨 视觉设计
- 杂志编辑风格：Playfair Display 衬线标题 + 金色点缀
- 暗色/亮色双主题切换（localStorage 持久化）
- 全站 40+ 页面适配暗黑主题
- AI 助手悬浮快捷入口（可收起）
- 响应式布局（移动端适配）

### 🔍 其他
- 全文搜索（标题/内容/描述模糊匹配）
- SEO 优化（每页独立 title + description）
- 标签页导航（最多 8 个，超出自动关闭）

### 🛡️ 内容安全
- 敏感词过滤（DFA 算法，数据库词库，管理员可增删）
- 两级策略：替换为 `***`（评论/文章）或直接拒绝（注册/昵称）
- 批量导入 + 热加载（修改后立即生效，无需重启）
- 预留第三方内容审核接口（阿里云内容安全）

### 📡 Redis 缓存
- Redis 替代 Caffeine 本地缓存，支持多实例共享
- 高频接口自动缓存：文章详情、归档、排行、菜单树、徽章、热点
- 写操作自动清除缓存（`@CacheEvict`）
- `allkeys-lru` 淘汰策略，256MB 内存限制

### 🏥 健康检查
- `/api/health` 端点：检查 MySQL、Redis、XXL-Job 状态
- 管理后台可视化页面（状态灯 + 刷新按钮）
- Docker Compose 服务间健康检查（依赖链：MySQL → Redis → Backend → Frontend）

## 🏗 项目结构

```
BlogHub/
├── blog-backend/                  # Spring Boot 后端
│   ├── src/main/java/com/blog/
│   │   ├── config/                # JWT、拦截器、跨域、MiMo 配置
│   │   ├── controller/            # REST API 控制器（含 AiChatController）
│   │   ├── service/               # 业务逻辑层（含 AiChatService 流式处理）
│   │   ├── mapper/                # MyBatis-Plus Mapper
│   │   ├── entity/                # 数据实体
│   │   ├── dto/                   # 请求/响应 DTO
│   │   ├── common/                # 公共工具、响应封装
│   │   └── exception/             # 全局异常处理
│   ├── sql/
│   │   ├── init.sql               # 完整建表 + 种子数据（含所有表）
│   │   └── xxl_job_init.sql       # XXL-Job 建表 SQL（独立库 xxl_job）
│   └── src/main/resources/
│       ├── application.yml        # 应用配置
│       └── application-local.yml  # 本地 API Key（gitignore）
│
├── blog-frontend/      # Vue 3 前端
│   ├── src/
│   │   ├── views/                 # 页面组件（含 AiAssistant/ApiTester）
│   │   ├── components/            # 公共组件（含 api-tester/ 子目录）
│   │   ├── composables/           # 组合式函数（useAiChat/useMarkdown）
│   │   ├── layouts/               # 布局组件（含主题切换）
│   │   ├── router/                # 路由 + 守卫
│   │   ├── stores/                # Pinia 状态
│   │   ├── api/                   # API 请求封装（含 aiApi/apiTesterApi）
│   │   └── styles/                # 全局样式 + 暗色/亮色主题变量
│   └── public/
│
├── docker-compose.yml             # 一键部署（MySQL + 后端 + Nginx + XXL-Job Admin）
├── nginx.conf                     # Nginx 反向代理配置
└── docs/                          # 设计文档
```

## 🚀 快速开始

### 前置条件

- JDK 8+
- Node.js 18+
- MySQL 8.0+
- Redis（端口 6379）
- Maven

### 1. 数据库

```bash
mysql -u root -p < blog-backend/sql/init.sql
mysql -u root -p < blog-backend/sql/xxl_job_init.sql
```

### 2. AI 配置

在 `blog-backend/src/main/resources/` 下创建 `application-local.yml`：

```yaml
mimo:
  api-key: 你的_MiMo_Token_Plan_API_Key
```

> ⚠️ `application-local.yml` 已被 gitignore，不会提交到版本库。

### 3. 后端

```bash
cd blog-backend
mvn spring-boot:run
# 或指定本地 profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

后端运行在 `http://localhost:8081`

### 4. 前端

```bash
cd blog-frontend
npm install
npm run dev
```

前端运行在 `http://localhost:3000`（代理 API 到 :8081）

> 详细前端文档见 [blog-frontend/README.md](blog-frontend/README.md)

### 5. Docker（可选）

```bash
docker-compose up -d    # MySQL :3307, Redis :6379, Backend :8081, Frontend :80, XXL-Job Admin :8082
```

### 6. XXL-Job 定时任务管理

XXL-Job Admin 提供定时任务的可视化管理：

```bash
# 本地启动（需先构建 xxl-job-admin.jar）
cd xxl-job-admin
java -jar xxl-job-admin.jar --spring.config.location=./application.properties
```

访问 `http://localhost:8082/xxl-job-admin`，登录 `admin` / `123456`。

在任务管理中新建任务：
- `publishScheduledPosts`：cron `0 0/1 * * * ?`（每分钟检查定时发布文章）
- `fetchAllTopics`：cron `0 0 0/3 * * ?`（每 3 小时抓取热点话题）

## 🔗 API 文档
 
启动后端后访问 Knife4j 文档：

```
http://localhost:8081/doc.html
```

## 📄 License

MIT
