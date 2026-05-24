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
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        background-color="#fff"
        text-color="#606266"
        active-text-color="#409eff"
        class="sidebar-menu"
      >
        
        <el-menu-item index="/home">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
      
        <!-- 博客核心功能 -->
        <el-menu-item index="/blog" class="nav-primary">
          <el-icon :size="20"><Notebook /></el-icon>
          <template #title>博客</template>
        </el-menu-item>
        <el-menu-item index="/my-posts">
          <el-icon :size="18"><Document /></el-icon>
          <template #title>我的文章</template>
        </el-menu-item>
        <el-menu-item index="/blog/new">
          <el-icon :size="18"><EditPen /></el-icon>
          <template #title>写文章</template>
        </el-menu-item>
        <el-menu-item index="/tags">
          <el-icon><CollectionTag /></el-icon>
          <template #title>标签云</template>
        </el-menu-item>
        <el-menu-item index="/archive">
          <el-icon><Timer /></el-icon>
          <template #title>文章归档</template>
        </el-menu-item>

        <el-divider class="menu-divider" />

        <!-- 管理（仅管理员） -->
        <el-menu-item index="/dashboard" v-if="isAdmin">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/users" v-if="isAdmin">
          <el-icon><UserFilled /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/comments" v-if="isAdmin">
          <el-icon><ChatDotSquare /></el-icon>
          <template #title>评论管理</template>
        </el-menu-item>
        <el-menu-item index="/logs" v-if="isAdmin">
          <el-icon><Document /></el-icon>
          <template #title>操作日志</template>
        </el-menu-item>
        <el-menu-item index="/images" v-if="isAdmin">
          <el-icon><Picture /></el-icon>
          <template #title>图片管理</template>
        </el-menu-item>
        <el-menu-item index="/bookmarks">
          <el-icon><StarFilled /></el-icon>
          <template #title>收藏</template>
        </el-menu-item>
        <el-menu-item index="/about">
          <el-icon><InfoFilled /></el-icon>
          <template #title>关于</template>
        </el-menu-item>
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
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ authStore.nickname || authStore.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item icon="User" @click="$router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item icon="Setting">设置</el-dropdown-item>
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

      <!-- 内容区域 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import ReadingProgress from '@/components/ReadingProgress.vue'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapse = ref(false)
const isAdmin = computed(() => authStore.isAdmin)
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
document.addEventListener('click', (e) => {
  if (showNotifications.value && !e.target.closest('.notification-panel') && !e.target.closest('.notification-badge')) {
    showNotifications.value = false
  }
})

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}

function handleLogout() {
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/blog')
}

onMounted(fetchNotifications)

/** 面包屑导航 */
const crumbConfig = {
  '/home': [{ path: '/home', title: '首页' }],
  '/about': [{ path: '/about', title: '关于' }],
  '/tags': [{ path: '/tags', title: '标签云' }],
  '/search': [{ path: '/search', title: '搜索' }],
  '/archive': [{ path: '/archive', title: '文章归档' }],
  '/images': [{ path: '/images', title: '图片管理' }],
  '/bookmarks': [{ path: '/bookmarks', title: '收藏' }],
  '/blog': [{ path: '/blog', title: '博客' }],
  '/blog/new': [{ path: '/blog', title: '博客' }, { path: '/blog/new', title: '写文章' }],
  '/users': [{ path: '/users', title: '用户管理' }],
  '/comments': [{ path: '/comments', title: '评论管理' }],
  '/profile': [{ path: '/profile', title: '个人中心' }],
}

const breadcrumbs = computed(() => {
  const path = route.path
  if (crumbConfig[path]) return crumbConfig[path]
  if (path.startsWith('/blog/')) {
    return [{ path: '/blog', title: '博客' }, { path, title: '文章详情' }]
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
  background-color: #fff;
  border-right: 1px solid #e8eaed;
  transition: width 0.25s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 10px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  flex-shrink: 0;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}

/* 侧边栏菜单 */
.sidebar-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  padding-top: 4px;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: 8px;
  font-size: 14px;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: #f0f5ff;
  color: #409eff;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}

.sidebar-menu :deep(.el-sub-menu .el-menu) {
  background-color: #fff;
}

.menu-divider {
  margin: 8px 16px;
  border-top-color: #f0f0f0;
}

.sidebar-menu :deep(.el-sub-menu__title .el-icon) {
  margin-right: 8px;
}

/* ========== 顶部栏 ========== */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  padding: 0 20px;
  height: 56px !important;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #909399;
  padding: 6px;
  border-radius: 6px;
  transition: all 0.2s;
}

.collapse-btn:hover {
  background: #f0f5ff;
  color: #409eff;
}

.header-breadcrumb :deep(.el-breadcrumb__inner) {
  font-size: 13px;
  color: #909399;
}

.header-breadcrumb :deep(.el-breadcrumb__inner.is-link) {
  font-weight: 400;
  color: #606266;
}

.header-breadcrumb :deep(.el-breadcrumb__inner.is-link:hover) {
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.notification-badge :deep(.el-badge__content) {
  top: 2px;
  right: 2px;
}

.notification-panel {
  position: absolute;
  top: 52px;
  right: 60px;
  width: 320px;
  max-height: 400px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  z-index: 999;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  font-weight: 500;
}

.panel-body {
  max-height: 340px;
  overflow-y: auto;
}

.notif-item {
  padding: 10px 16px;
  font-size: 13px;
  color: #606266;
  border-bottom: 1px solid #f9f9f9;
  cursor: default;
}

.notif-item.unread {
  background: #ecf5ff;
  color: #303133;
}

.notif-time {
  display: block;
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 2px;
}

.header-action-btn {
  border: 1px solid #e4e7ed;
  background: #fafafa;
  color: #606266;
}

.header-action-btn:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 8px 2px 2px;
  border-radius: 20px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f7fa;
}

.username {
  font-size: 13px;
  color: #303133;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ========== 内容区域 ========== */
.layout-main {
  background-color: #f5f7fa;
  padding: 24px;
  overflow-y: auto;
}
</style>
