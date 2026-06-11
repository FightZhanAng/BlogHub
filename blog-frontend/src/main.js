import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

import 'bootstrap-icons/font/bootstrap-icons.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './styles/global.css'

// 修复 Edge/Chromium：history.replaceState 导致窗口最小化后重新聚焦
(function patchHistoryFocusBug() {
  const origReplaceState = window.history.replaceState.bind(window.history)
  const origPushState = window.history.pushState.bind(window.history)
  window.history.replaceState = function () {
    if (document.hidden) return
    return origReplaceState.apply(this, arguments)
  }
  window.history.pushState = function () {
    if (document.hidden) return
    return origPushState.apply(this, arguments)
  }
})()

// 迁移旧版 localStorage 键到 pinia-plugin-persistedstate 统一存储
;(function migrateOldAuthKeys() {
  const oldToken = localStorage.getItem('blog_token')
  const newAuth = localStorage.getItem('blog_auth')
  if (oldToken && !newAuth) {
    // 旧版用户：将分散的键合并为新格式
    const auth = {
      token: oldToken,
      username: localStorage.getItem('blog_username') || '',
      nickname: localStorage.getItem('blog_nickname') || '',
      avatar: localStorage.getItem('blog_avatar') || '',
      role: localStorage.getItem('blog_role') || '',
      userId: localStorage.getItem('blog_userId') || '',
    }
    localStorage.setItem('blog_auth', JSON.stringify(auth))
  }
  // 清理旧键（无论是否迁移）
  ;['blog_token', 'blog_username', 'blog_nickname', 'blog_avatar', 'blog_role', 'blog_userId'].forEach(k => localStorage.removeItem(k))
})()

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error('全局错误:', err, info)
}

app.mount('#app')
