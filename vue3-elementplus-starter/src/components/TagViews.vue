<template>
  <div class="tag-views">
    <div class="tag-list" ref="tagListRef">
      <div
        v-for="tag in visitedViews"
        :key="tag.path"
        class="tag-item"
        :class="{ active: isActive(tag) }"
        @click="handleClick(tag)"
        @contextmenu.prevent="openContextMenu($event, tag)"
      >
        <span class="tag-title">{{ tag.title }}</span>
        <el-icon
          v-if="!tag.affix"
          class="tag-close"
          :size="12"
          @click.stop="closeTag(tag)"
        >
          <Close />
        </el-icon>
      </div>
    </div>

    <!-- 右侧操作按钮 -->
    <el-dropdown trigger="click" @command="handleDropdown">
      <el-icon class="tag-actions" :size="14"><ArrowDown /></el-icon>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="closeOthers">关闭其他</el-dropdown-item>
          <el-dropdown-item command="closeAll">关闭所有</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 右键菜单 -->
    <ul
      v-show="contextMenu.visible"
      class="context-menu"
      :style="{ left: contextMenu.left + 'px', top: contextMenu.top + 'px' }"
    >
      <li @click="closeCurrent(contextMenu.tag)">关闭当前</li>
      <li @click="closeOthers(contextMenu.tag)">关闭其他</li>
      <li @click="closeAll">关闭所有</li>
    </ul>
  </div>
</template>

<script setup>
import { ref, reactive, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const tagListRef = ref(null)

// 固定标签：首页
const affixTags = [{ path: '/home', title: '首页', affix: true }]

const visitedViews = ref([...affixTags])

// 路由 → 标题映射
const titleMap = {
  '/home': '首页', '/blog': '博客', '/my-posts': '我的文章',
  '/blog/new': '写文章', '/tags': '标签云', '/archive': '文章归档',
  '/bookmarks': '收藏', '/badges': '我的徽章', '/hot': '每日热点',
  '/albums': '宝宝相册', '/ai-assistant': 'AI 助手', '/api-tester': 'API 测试',
  '/dashboard': '仪表盘', '/users': '用户管理', '/comments': '评论管理',
  '/images': '图片管理', '/logs': '操作日志', '/about': '关于', '/profile': '个人中心',
  '/login': '登录', '/register': '注册', '/search': '搜索',
  '/admin/menu-settings': '菜单配置',
}

function getTitle(route) {
  if (titleMap[route.path]) return titleMap[route.path]
  if (route.path.startsWith('/blog/')) return '文章详情'
  if (route.path.startsWith('/albums/')) return '相册详情'
  return route.meta?.title || '未知'
}

function isActive(tag) {
  return tag.path === route.path
}

function addView(to) {
  const title = getTitle(to)
  const exists = visitedViews.value.some(v => v.path === to.path)
  if (!exists) {
    visitedViews.value.push({ path: to.path, title })
  }
}

// 从 sessionStorage 恢复
function restoreTags() {
  try {
    const saved = sessionStorage.getItem('tagViews')
    if (saved) {
      const parsed = JSON.parse(saved)
      // 合并：先 affix，再恢复的非 affix
      const restored = parsed.filter(t => !t.affix)
      visitedViews.value = [...affixTags, ...restored]
    }
  } catch { /* ignore */ }
}

function persistTags() {
  sessionStorage.setItem('tagViews', JSON.stringify(visitedViews.value))
}

// 监听路由变化，添加标签
watch(() => route.path, () => {
  addView(route)
  persistTags()
}, { immediate: true })

// ========== 标签操作 ==========

function handleClick(tag) {
  router.push(tag.path)
}

function closeTag(tag) {
  const idx = visitedViews.value.findIndex(v => v.path === tag.path)
  if (idx === -1) return
  visitedViews.value.splice(idx, 1)
  persistTags()

  // 如果关闭的是当前页，跳转到最后一个标签
  if (isActive(tag)) {
    const last = visitedViews.value[visitedViews.value.length - 1]
    if (last) router.push(last.path)
  }
}

// ========== 右键菜单 ==========

const contextMenu = reactive({ visible: false, left: 0, top: 0, tag: null })

function openContextMenu(e, tag) {
  contextMenu.visible = true
  contextMenu.left = e.clientX
  contextMenu.top = e.clientY
  contextMenu.tag = tag
}

function closeContextMenu() {
  contextMenu.visible = false
}

function closeCurrent(tag) {
  closeTag(tag)
  closeContextMenu()
}

function closeOthers(tag) {
  visitedViews.value = visitedViews.value.filter(v => v.affix || v.path === tag.path)
  persistTags()
  if (!isActive(tag)) router.push(tag.path)
  closeContextMenu()
}

function closeAll() {
  visitedViews.value = [...affixTags]
  persistTags()
  router.push('/home')
  closeContextMenu()
}

function handleDropdown(command) {
  if (command === 'closeOthers') {
    closeOthers({ path: route.path })
  } else if (command === 'closeAll') {
    closeAll()
  }
}

// 点击外部关闭右键菜单
function handleOutsideClick(e) {
  if (!e.target.closest('.context-menu')) {
    closeContextMenu()
  }
}

onMounted(() => {
  restoreTags()
  addView(route)
  document.addEventListener('click', handleOutsideClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleOutsideClick)
})
</script>

<style scoped>
.tag-views {
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  padding: 0 12px;
  height: 36px;
  display: flex;
  align-items: center;
  position: relative;
  flex-shrink: 0;
}

.tag-list {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
  overflow-y: hidden;
  flex: 1;
  scrollbar-width: none;
}

.tag-list::-webkit-scrollbar {
  display: none;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 10px;
  border: 1px solid #e8eaed;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  background: #fff;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.2s;
  user-select: none;
}

.tag-item:hover {
  color: #409eff;
  border-color: #c6e2ff;
  background: #ecf5ff;
}

.tag-item.active {
  color: #fff;
  background: #409eff;
  border-color: #409eff;
}

.tag-item.active .tag-close:hover {
  background: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.tag-close {
  border-radius: 50%;
  width: 14px;
  height: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  color: #c0c4cc;
}

.tag-close:hover {
  background: #c0c4cc;
  color: #fff;
}

.context-menu {
  position: fixed;
  z-index: 9999;
  list-style: none;
  padding: 4px 0;
  margin: 0;
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  font-size: 13px;
  min-width: 120px;
}

.context-menu li {
  padding: 6px 16px;
  cursor: pointer;
  color: #606266;
  transition: all 0.15s;
}

.context-menu li:hover {
  background: #ecf5ff;
  color: #409eff;
}

.tag-actions {
  cursor: pointer;
  color: #909399;
  padding: 4px;
  border-radius: 4px;
  margin-left: 8px;
  flex-shrink: 0;
  transition: all 0.2s;
}

.tag-actions:hover {
  color: #409eff;
  background: #ecf5ff;
}
</style>
