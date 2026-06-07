# BlogHub 前端

Vue 3 + Element Plus + Vite 构建的博客前端，采用杂志编辑风格设计。

## 技术栈

| 技术 | 版本 | 用途 |
| --- | --- | --- |
| Vue | 3.4+ | 前端框架 (Composition API) |
| Element Plus | 2.6+ | UI 组件库 |
| Vite | 5+ | 构建工具 |
| Pinia | 2+ | 状态管理 |
| Vue Router | 4+ | 路由 |
| Axios | 1.6+ | HTTP 请求 |

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器（端口 3000，代理 API 到 :8080）
npm run dev

# 构建生产版本
npm run build

# 预览构建结果
npm run preview

# 代码检查
npm run lint
```

## 目录结构

```
src/
├── api/                    # API 请求封装
│   ├── request.js          # Axios 实例（自动注入 Token）
│   ├── postApi.js          # 文章 API
│   ├── authApi.js          # 认证 API
│   ├── aiApi.js            # AI 助手 API
│   └── apiTesterApi.js     # API 测试工具 API
├── components/             # 公共组件
│   ├── BlogCard.vue        # 文章卡片
│   ├── BlogComments.vue    # 评论组件
│   ├── BlogToc.vue         # 目录导航
│   ├── BlogActionBar.vue   # 文章操作栏
│   ├── ChatMessage.vue     # AI 聊天消息
│   ├── ChatHistory.vue     # AI 对话历史
│   ├── TagViews.vue        # 标签页导航
│   ├── ImageLightbox.vue   # 图片灯箱
│   └── api-tester/         # API 测试工具组件
├── composables/            # 组合式函数
│   ├── usePosts.js         # 文章数据
│   ├── useMarkdown.js      # Markdown 渲染
│   └── useAiChat.js        # AI 聊天
├── layouts/
│   └── MainLayout.vue      # 主布局（侧边栏+顶栏+主题切换）
├── views/                  # 页面组件
│   ├── Home.vue            # 首页
│   ├── BlogList.vue        # 博客列表
│   ├── BlogPost.vue        # 文章详情
│   ├── AiAssistant.vue     # AI 助手
│   ├── ApiTester.vue       # API 测试工具
│   ├── Login.vue           # 登录/注册
│   └── ...                 # 其他页面
├── stores/                 # Pinia 状态
│   └── auth.js             # 认证状态
├── router/                 # 路由配置
├── styles/                 # 全局样式
│   └── global.css          # 设计系统 + 暗色/亮色主题变量
└── utils/                  # 工具函数
```

## 特性

### 🎨 视觉设计
- 杂志编辑风格：Playfair Display 衬线标题 + 金色点缀
- 暗色/亮色双主题切换（顶栏按钮，localStorage 持久化）
- 全站 40+ 页面适配暗黑主题（CSS 变量驱动）

### 🤖 AI 助手
- 集成 MiMo V2.5 大模型（Pro / Flash / 多模态）
- SSE 流式输出 + 深度思考模式
- 暗色/亮色主题跟随全局
- 悬浮快捷入口（可收起）

### 🔧 API 测试工具
- 网页版 Postman，支持 GET/POST/PUT/DELETE
- 后端代理转发（无 CORS 限制）
- 环境变量管理（`{{variable}}` 语法）
- 请求收藏集合 + 历史记录

### 📱 响应式
- 移动端适配（768px 断点）
- 侧边栏可折叠
- 标签页导航（最多 8 个）

## 开发规范

- 组件使用 `<script setup>` + Composition API
- 样式使用 CSS 变量（`var(--color-*)`）确保主题兼容
- 新增页面需适配暗色主题
- API 模块按领域拆分（postApi, authApi 等）
- 路由使用懒加载（`() => import(...)`）
