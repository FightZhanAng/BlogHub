<template>
  <ReadingProgress />
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <!-- Logo -->
      <div class="logo" @click="$router.push('/home')">
        <div class="logo-icon">B</div>
        <span v-show="!isCollapse" class="logo-text">BlogHub</span>
      </div>

      <el-menu
        :default-active="route.path"
        :default-openeds="defaultOpeneds"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        background-color="transparent"
        text-color="#6b6b6b"
        active-text-color="#c9a96e"
        class="sidebar-menu"
      >
        <template v-for="item in menuTree" :key="item.id">
          <!-- 独立顶级项（无子菜单） -->
          <el-menu-item v-if="!item.children" :index="item.path">
            <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>

          <!-- 分组（有子菜单） -->
          <el-sub-menu v-else :index="item.groupKey">
            <template #title>
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.id"
              :index="child.path"
            >
              <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
              <template #title>{{ child.title }}</template>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <!-- 主区域 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse" :size="18">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/" class="header-breadcrumb">
            <el-breadcrumb-item
              v-for="crumb in breadcrumbs"
              :key="crumb.path"
              :to="crumb.path !== route.path ? { path: crumb.path } : undefined"
            >
              {{ crumb.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 主题切换 -->
          <el-tooltip :content="isDark ? '切换亮色主题' : '切换暗色主题'" placement="bottom">
            <el-button circle size="small" class="header-action-btn" @click="toggleTheme">
              <el-icon>
                <Moon v-if="!isDark" />
                <Sunny v-else />
              </el-icon>
            </el-button>
          </el-tooltip>

          <el-tooltip content="写文章" placement="bottom">
            <el-button circle size="small" class="header-action-btn" @click="$router.push('/blog/new')">
              <el-icon><EditPen /></el-icon>
            </el-button>
          </el-tooltip>

          <!-- 通知铃铛 -->
          <el-badge :value="unreadCount" :hidden="!unreadCount" class="notification-badge">
            <el-button circle size="small" class="header-action-btn" @click.stop="toggleNotifications">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>

          <!-- 通知面板 -->
          <div v-if="showNotifications" class="notification-panel" @click.stop>
            <div class="panel-header">
              <span>通知</span>
              <el-button text size="small" @click="markAllRead">全部已读</el-button>
            </div>
            <div class="panel-body">
              <div v-for="n in notifications" :key="n.id" class="notif-item" :class="{ unread: !n.isRead }">
                {{ n.message }}
                <span class="notif-time">{{ n.createdAt?.slice(0, 16).replace('T', ' ') }}</span>
              </div>
              <el-empty v-if="!notifications.length" description="暂无通知" :image-size="40" />
            </div>
          </div>

          <!-- 未登录 -->
          <template v-if="!authStore.isLoggedIn">
            <el-button size="small" @click="$router.push('/login')">登录</el-button>
          </template>

          <!-- 已登录 -->
          <el-dropdown v-else trigger="click">
            <span class="user-info">
              <el-avatar :size="32" :src="authStore.avatar || undefined" icon="UserFilled" />
              <span class="username">{{ authStore.nickname || authStore.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item icon="User" @click="$router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item icon="Setting" v-if="authStore.isAdmin" @click="$router.push('/admin/menu-settings')">菜单设置</el-dropdown-item>
                <el-dropdown-item v-if="authStore.isAdmin" icon="UserFilled" @click="$router.push('/users')">
                  用户管理
                </el-dropdown-item>
                <el-dropdown-item divided icon="SwitchButton" @click="handleLogout">
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 标签页导航 -->
      <TagViews />

      <!-- 内容区域 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <keep-alive :max="15">
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>

  <!-- AI 助手悬浮按钮（在 AI 助手页面隐藏） -->
  <div v-if="route.path !== '/ai-assistant'" class="ai-fab-wrapper">
    <!-- 展开状态：显示操作项 -->
    <template v-if="!fabCollapsed">
      <div class="fab-action" @click="$router.push('/ai-assistant')" title="AI 对话">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
      </div>
    </template>
    <!-- 主按钮 -->
    <div class="ai-fab" @click="fabCollapsed = !fabCollapsed" :title="fabCollapsed ? '展开' : '收起'">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" :style="{ transform: fabCollapsed ? 'rotate(0deg)' : 'rotate(45deg)', transition: 'transform 0.25s ease' }">
        <path d="M12 2a7 7 0 0 1 7 7c0 2.5-1.5 4.5-3 6v2a1 1 0 0 1-1 1h-4a1 1 0 0 1-1-1v-2c-1.5-1.5-3-3.5-3-6a7 7 0 0 1 7-7z"/>
        <line x1="9" y1="21" x2="15" y2="21"/>
      </svg>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import ReadingProgress from '@/components/ReadingProgress.vue'
import TagViews from '@/components/TagViews.vue'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const menuStore = useMenuStore()
const isCollapse = ref(false)
const isAdmin = computed(() => authStore.isAdmin)
const menuTree = computed(() => menuStore.menuTree)
const fabCollapsed = ref(true)

// ========== 主题切换 ==========
const isDark = ref(localStorage.getItem('blog-theme') === 'dark')

function applyTheme(dark) {
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
}

function toggleTheme() {
  isDark.value = !isDark.value
  localStorage.setItem('blog-theme', isDark.value ? 'dark' : 'light')
  applyTheme(isDark.value)
}

// 初始化主题
applyTheme(isDark.value)

// 监听外部主题变化（如从 AI 助手页面切换）
const themeObserver = new MutationObserver(() => {
  const theme = document.documentElement.getAttribute('data-theme')
  isDark.value = theme === 'dark'
})
themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
onUnmounted(() => themeObserver.disconnect())

// 自动展开包含当前路由的分组
const defaultOpeneds = computed(() => {
  for (const item of menuTree.value) {
    if (item.children && item.children.some(c => c.path === route.path)) {
      return [item.groupKey]
    }
  }
  return []
})
const showNotifications = ref(false)
const notifications = ref([])
const unreadCount = ref(0)

async function fetchNotifications() {
  if (!authStore.isLoggedIn) return
  try {
    const [listRes, countRes] = await Promise.all([
      request.get('/notifications', { params: { page: 1, size: 10 } }),
      request.get('/notifications/unread-count'),
    ])
    notifications.value = listRes.records || listRes.list || []
    unreadCount.value = countRes.count || 0
  } catch { /* ignore */ }
}

async function markAllRead() {
  try {
    await request.put('/notifications/read-all')
    unreadCount.value = 0
    notifications.value.forEach(n => n.isRead = 1)
  } catch { /* ignore */ }
}

function toggleNotifications() {
  showNotifications.value = !showNotifications.value
  if (showNotifications.value) fetchNotifications()
}

// 点击通知面板外部关闭
function handleGlobalClick(e) {
  if (showNotifications.value && !e.target.closest('.notification-panel') && !e.target.closest('.notification-badge')) {
    showNotifications.value = false
  }
}
document.addEventListener('click', handleGlobalClick)
onUnmounted(() => document.removeEventListener('click', handleGlobalClick))

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}

function handleLogout() {
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

onMounted(() => {
  fetchNotifications()
  // 刷新后如果已登录但菜单为空，重新加载
  if (authStore.isLoggedIn && menuTree.value.length === 0) {
    menuStore.fetchMenuTree()
  }
})

/** 面包屑导航 */
const crumbConfig = {
  '/home': [{ path: '/home', title: '首页' }],
  '/about': [{ path: '/about', title: '关于' }],
  '/tags': [{ path: '/tags', title: '标签云' }],
  '/search': [{ path: '/search', title: '搜索' }],
  '/archive': [{ path: '/archive', title: '文章归档' }],
  '/images': [{ path: '/images', title: '图片管理' }],
  '/bookmarks': [{ path: '/bookmarks', title: '收藏' }],
  '/albums': [{ path: '/albums', title: '宝宝相册' }],
  '/hot': [{ path: '/hot', title: '每日热点' }],
  '/ai-assistant': [{ path: '/ai-assistant', title: 'AI 助手' }],
  '/api-tester': [{ path: '/api-tester', title: 'API 测试工具' }],
  '/blog': [{ path: '/blog', title: '博客' }],
  '/blog/new': [{ path: '/blog', title: '博客' }, { path: '/blog/new', title: '写文章' }],
  '/users': [{ path: '/users', title: '用户管理' }],
  '/comments': [{ path: '/comments', title: '评论管理' }],
  '/profile': [{ path: '/profile', title: '个人中心' }],
  '/badges': [{ path: '/badges', title: '我的徽章' }],
  '/my-posts': [{ path: '/my-posts', title: '我的文章' }],
  '/dashboard': [{ path: '/dashboard', title: '仪表盘' }],
  '/logs': [{ path: '/logs', title: '操作日志' }],
  '/login': [{ path: '/login', title: '登录' }],
  '/register': [{ path: '/register', title: '注册' }],
  '/admin/menu-settings': [{ path: '/admin/menu-settings', title: '菜单配置' }],
  '/sensitive-words': [{ path: '/sensitive-words', title: '敏感词管理' }],
  '/health': [{ path: '/health', title: '健康检查' }],
}

const breadcrumbs = computed(() => {
  const path = route.path
  if (crumbConfig[path]) return crumbConfig[path]
  if (path.startsWith('/blog/')) {
    return [{ path: '/blog', title: '博客' }, { path, title: '文章详情' }]
  }
  if (path.startsWith('/albums/')) {
    return [{ path: '/albums', title: '宝宝相册' }, { path, title: '相册详情' }]
  }
  return [{ path, title: route.meta.title || '未知' }]
})
</script>

<style scoped>
.layout-container {
  height: 100%;
  min-height: 100vh;
}

/* ========== 侧边栏 ========== */
.layout-aside {
  background-color: var(--color-card);
  border-right: 1px solid var(--color-border-light);
  transition: width 0.25s var(--ease);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.logo-icon {
  width: 30px;
  height: 30px;
  background: var(--color-text);
  color: var(--color-card);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
}

.logo-text {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text);
  white-space: nowrap;
  letter-spacing: -0.02em;
}

/* 侧边栏菜单 */
.sidebar-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  padding-top: 8px;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 40px;
  line-height: 40px;
  margin: 1px 10px;
  border-radius: var(--radius);
  font-size: 13px;
  color: var(--color-text-secondary);
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: var(--color-bg-warm);
  color: var(--color-text);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: var(--color-accent-light);
  color: var(--color-accent-hover);
  font-weight: 500;
}

.sidebar-menu :deep(.el-sub-menu .el-menu) {
  background-color: transparent;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  height: 36px;
  line-height: 36px;
  margin: 1px 10px;
  border-radius: var(--radius);
  font-size: 12.5px;
  padding-left: 48px !important;
  min-width: auto;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item:hover) {
  background-color: var(--color-bg-warm);
  color: var(--color-text);
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item.is-active) {
  background-color: var(--color-accent-light);
  color: var(--color-accent-hover);
  font-weight: 500;
}

.sidebar-menu :deep(.el-sub-menu__title .el-icon) {
  margin-right: 8px;
}

/* ========== 顶部栏 ========== */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border-light);
  padding: 0 24px;
  height: 56px !important;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: var(--color-text-tertiary);
  padding: 6px;
  border-radius: var(--radius);
  transition: all var(--duration) var(--ease);
}

.collapse-btn:hover {
  background: var(--color-bg-warm);
  color: var(--color-text);
}

.header-breadcrumb :deep(.el-breadcrumb__inner) {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.header-breadcrumb :deep(.el-breadcrumb__inner.is-link) {
  font-weight: 400;
  color: var(--color-text-secondary);
}

.header-breadcrumb :deep(.el-breadcrumb__inner.is-link:hover) {
  color: var(--color-accent);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.notification-badge :deep(.el-badge__content) {
  top: 2px;
  right: -4px;
}

.notification-panel {
  position: absolute;
  top: 52px;
  right: 60px;
  width: 320px;
  max-height: 400px;
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  z-index: 999;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border-light);
  font-size: 13px;
  font-weight: 600;
  font-family: var(--font-display);
  color: var(--color-text);
}

.panel-body {
  max-height: 340px;
  overflow-y: auto;
}

.notif-item {
  padding: 10px 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border-light);
  cursor: default;
}

.notif-item.unread {
  background: var(--color-accent-lighter);
  color: var(--color-text);
}

.notif-item:last-child {
  border-bottom: none;
}

.notif-time {
  display: block;
  font-size: 11px;
  color: var(--color-text-placeholder);
  margin-top: 2px;
}

.header-action-btn {
  border: 1px solid var(--color-border);
  background: var(--color-card);
  color: var(--color-text-secondary);
}

.header-action-btn:hover {
  background: var(--color-accent-light);
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 10px 2px 2px;
  border-radius: 20px;
  transition: background var(--duration) var(--ease);
}

.user-info:hover {
  background: var(--color-bg-warm);
}

.username {
  font-size: 13px;
  color: var(--color-text);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ========== 内容区域 ========== */
.layout-main {
  background-color: var(--color-bg);
  padding: 32px;
  overflow-y: auto;
}

/* ========== AI 助手悬浮按钮 ========== */
.ai-fab-wrapper {
  position: fixed;
  bottom: 32px;
  right: 32px;
  display: flex;
  flex-direction: column-reverse;
  align-items: center;
  gap: 10px;
  z-index: 900;
}

.ai-fab {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--color-accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(201, 169, 110, 0.35);
  transition: all var(--duration) var(--ease);
}

.ai-fab:hover {
  background: var(--color-accent-hover);
  transform: scale(1.06);
  box-shadow: 0 6px 24px rgba(201, 169, 110, 0.4);
}

.ai-fab:active {
  transform: scale(0.95);
}

.fab-action {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-card);
  color: var(--color-accent);
  border: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: all var(--duration) var(--ease);
  opacity: 0;
  transform: translateY(8px);
  animation: fab-action-in 0.2s ease forwards;
}

.fab-action:hover {
  background: var(--color-accent-light);
  border-color: var(--color-accent);
}

@keyframes fab-action-in {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
