# Vue 3 + Element Plus + Vite Starter

开箱即用的前端中后台脚手架。

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

# 启动开发服务器
npm run dev

# 构建生产
npm run build

# 预览构建结果
npm run preview
```

## 目录结构

```
src/
├── layouts/       # 布局组件
│   └── MainLayout.vue
├── views/         # 页面组件
│   ├── Home.vue
│   └── About.vue
├── components/    # 公共组件
├── router/        # 路由配置
│   └── index.js
├── stores/        # Pinia 状态
│   └── counter.js
├── styles/        # 全局样式
│   └── global.css
├── App.vue
└── main.js
```

## 特性

- ✅ Vite 极速热更新
- ✅ Element Plus 完整引入 + 图标自动注册
- ✅ Pinia 状态管理 + 计数器演示
- ✅ Vue Router 嵌套路由 + 路由守卫
- ✅ 侧边栏折叠布局
- ✅ Axios 请求 (proxy 代理配置)
- ✅ 环境变量 (.env 多环境)
