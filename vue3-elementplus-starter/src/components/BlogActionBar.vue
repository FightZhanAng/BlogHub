<template>
  <div class="action-bar" :class="{ 'is-sticky': sticky }">
    <!-- 点赞 -->
    <button class="act-btn" type="button" :class="{ liked }" @click="toggleLike" title="点赞">
      <i :class="liked ? 'bi bi-heart-fill' : 'bi bi-heart'"></i>
    </button>
    <span class="count">{{ count }}</span>

    <!-- 收藏 -->
    <button class="act-btn" type="button" :class="{ saved: bookmarked }" @click="toggleBookmark" title="收藏">
      <i :class="bookmarked ? 'bi bi-bookmark-fill' : 'bi bi-bookmark'"></i>
    </button>

    <!-- 分享 -->
    <el-dropdown trigger="click" placement="right" @command="shareTo">
      <button class="act-btn" type="button" title="分享">
        <i class="bi bi-share"></i>
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="weibo"><i class="bi bi-sina-weibo"></i> 微博</el-dropdown-item>
          <el-dropdown-item command="zhihu"><i class="bi bi-question-circle"></i> 知乎</el-dropdown-item>
          <el-dropdown-item command="copy"><i class="bi bi-link-45deg"></i> 复制链接</el-dropdown-item>
          <el-dropdown-item command="wechat"><i class="bi bi-wechat"></i> 微信</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 评论 -->
    <button class="act-btn" type="button" @click="scrollToComments" title="评论">
      <i class="bi bi-chat-dots"></i>
    </button>

    <!-- 返回顶部 -->
    <button class="act-btn" type="button" @click="scrollToTop" title="返回顶部">
      <i class="bi bi-arrow-up"></i>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useLikes, useBookmarks } from '@/composables/useInteraction'

const props = defineProps({
  slug: String,
})

const { liked, count, toggleLike } = useLikes(props.slug)
const { bookmarked, toggleBookmark } = useBookmarks(props.slug)
const sticky = ref(false)

function shareTo(platform) {
  const url = encodeURIComponent(window.location.href)
  const title = encodeURIComponent(document.title)
  switch (platform) {
    case 'weibo':
      window.open(`https://service.weibo.com/share/share.php?title=${title}&url=${url}`, '_blank', 'width=600,height=500')
      break
    case 'zhihu':
      window.open(`https://zhuanlan.zhihu.com/write?title=${title}`, '_blank')
      break
    case 'copy':
      navigator.clipboard.writeText(window.location.href).then(() => ElMessage.success('链接已复制'))
      break
    case 'wechat':
      ElMessage.info('请在微信内打开此页面分享给好友')
      break
  }
}

function scrollToComments() {
  const el = document.getElementById('blog-comments')
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

function scrollToTop() {
  const el = document.querySelector('.layout-main') || document.querySelector('.el-main') || document.documentElement
  el?.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleScroll() {
  const el = document.querySelector('.layout-main') || document.documentElement
  sticky.value = (el.scrollTop || window.scrollY) > 200
}

onMounted(() => {
  const container = document.querySelector('.layout-main') || window
  container.addEventListener('scroll', handleScroll, { passive: true })
})
onUnmounted(() => {
  const container = document.querySelector('.layout-main') || window
  container.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.action-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 4px;
  background: var(--color-card);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.3s;
  width: 52px;
}

.action-bar.is-sticky {
  position: fixed;
  top: 100px;
  z-index: 100;
}

.act-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  color: var(--color-text-tertiary);
  font-size: 17px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  padding: 0;
  line-height: 1;
}

.act-btn:hover {
  background: var(--color-accent-light);
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.act-btn.liked {
  background: #fef0f0;
  border-color: #f56c6c;
  color: #f56c6c;
}

.act-btn.saved {
  background: #fdf6ec;
  border-color: #e6a23c;
  color: #e6a23c;
}

[data-theme="dark"] .act-btn.liked {
  background: rgba(185, 74, 72, 0.15);
}
[data-theme="dark"] .act-btn.saved {
  background: rgba(196, 148, 58, 0.15);
}

.count {
  display: block;
  width: 36px;
  font-size: 11px;
  color: var(--color-text-tertiary);
  text-align: center;
  line-height: 1;
}
</style>
