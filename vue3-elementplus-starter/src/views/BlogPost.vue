<template>
  <div class="post-page">
    <!-- 加载中 -->
    <div class="loading-wrap" v-if="loading">
      <el-skeleton :rows="12" animated />
    </div>

    <template v-else-if="post">
      <BlogReadingProgress />
      <article class="post-article">
        <!-- 侧边浮动栏 -->
        <aside class="post-sidebar">
          <BlogActionBar :slug="slug" />
        </aside>

        <!-- 正文 -->
        <div class="post-main">
          <el-card shadow="never" class="post-card">
            <!-- 头部 -->
            <header class="post-header">
              <div class="header-meta">
                <span class="meta-tag" v-if="post.tags && post.tags[0]">
                  {{ post.tags[0] }}
                </span>
                <span class="meta-reading-time">{{ readingTime }}</span>
                <button v-if="hasCodeBlock" class="code-theme-btn" @click="toggleCodeTheme" :title="codeTheme === 'dark' ? '切换亮色代码' : '切换暗色代码'">
                  <i :class="codeTheme === 'dark' ? 'bi bi-sun' : 'bi bi-moon'"></i>
                </button>
                <a class="export-btn" :href="`${API_BASE}/posts/${post.slug}/export`" title="下载 Markdown" download><i class="bi bi-download"></i></a>
              </div>
              <h1 class="post-title">{{ post.title }}</h1>
              <div class="header-author">
                <el-avatar :size="32" :src="post.authorAvatar ? (post.authorAvatar.startsWith('http') ? post.authorAvatar : STATIC_BASE + post.authorAvatar) : undefined" />
                <div class="author-info">
                  <span class="author-name">{{ post.author || '匿名' }}</span>
                  <span class="author-date">{{ post.date }}</span>
                </div>
                <el-button v-if="authStore.isLoggedIn && post.authorId && authStore.userId !== String(post.authorId)"
                  size="small" :type="isFollowing ? 'default' : 'primary'" plain
                  @click="toggleFollow">
                  {{ isFollowing ? '已关注' : '+ 关注' }}
                </el-button>
                <span class="meta-views">
                  <el-icon :size="14"><View /></el-icon>
                  {{ post.views }}
                </span>
              </div>
            </header>

            <!-- Banner 分隔 -->
            <div class="post-banner" />

            <!-- Markdown 正文 -->
            <div class="markdown-body" :class="`code-${codeTheme}`" v-html="post.html" @click="onPostContentClick" />

            <!-- 空内容回退 -->
            <el-alert
              v-if="!post.html && post.content"
              title="Markdown 渲染为空，显示原文"
              type="warning"
              :closable="false"
              class="content-fallback"
            >
              <pre class="raw-content">{{ post.content }}</pre>
            </el-alert>
            <el-empty v-if="!post.html && !post.content" description="暂无正文" :image-size="80" />

            <!-- 标签 -->
            <div class="post-tags" v-if="post.tags && post.tags.length">
              <el-tag
                v-for="tag in post.tags"
                :key="tag"
                size="small"
                effect="plain"
                round
                style="cursor:pointer"
                @click="$router.push('/blog?tag=' + encodeURIComponent(tag))"
              >
                # {{ tag }}
              </el-tag>
            </div>

            <!-- 互动按钮 -->
            <div class="post-interactions">
              <el-button round size="small" :type="liked ? 'danger' : 'default'" @click="toggleLike">
                <i :class="liked ? 'bi bi-heart-fill' : 'bi bi-heart'"></i>
                {{ count || '点赞' }}
              </el-button>
              <el-button round size="small" :type="bookmarked ? 'warning' : 'default'" @click="toggleBookmark">
                <i :class="bookmarked ? 'bi bi-bookmark-fill' : 'bi bi-bookmark'"></i>
                {{ bookmarked ? '已收藏' : '收藏' }}
              </el-button>
              <el-dropdown trigger="click" size="small" placement="top">
                <el-button round size="small">
                  <i class="bi bi-share"></i>
                  分享
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="shareTo('weibo')"><i class="bi bi-sina-weibo"></i> 微博</el-dropdown-item>
                    <el-dropdown-item @click="shareTo('zhihu')"><i class="bi bi-question-circle"></i> 知乎</el-dropdown-item>
                    <el-dropdown-item @click="shareTo('copy')"><i class="bi bi-link-45deg"></i> 复制链接</el-dropdown-item>
                    <el-dropdown-item @click="shareTo('wechat')"><i class="bi bi-wechat"></i> 微信</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button v-if="authStore.isAdmin" round size="small" :type="post.isPinned ? 'warning' : 'default'" @click="togglePin">
                <i :class="post.isPinned ? 'bi bi-pin-angle-fill' : 'bi bi-pin-angle'"></i>
                {{ post.isPinned ? '已置顶' : '置顶' }}
              </el-button>
              <el-button v-if="canEdit" round size="small" @click="$router.push(`/blog/${slug}/edit`)">
                <i class="bi bi-pencil"></i>
                编辑
              </el-button>
            </div>

            <!-- 相关文章 -->
            <div class="related-posts" v-if="relatedPosts.length">
              <h3 class="related-title"><el-icon style="vertical-align:middle;margin-right:6px"><Connection /></el-icon>推荐阅读</h3>
              <div class="related-grid">
                <div
                  v-for="rp in relatedPosts"
                  :key="rp.slug"
                  class="related-card"
                  @click="goToRelated(rp)"
                >
                  <div class="related-name">{{ rp.title }}</div>
                  <div class="related-meta">{{ rp.views }} 次阅读</div>
                </div>
              </div>
            </div>

            <!-- 阅读趋势图（管理员可见） -->
            <div v-if="authStore.isAdmin && post.id" class="trend-section">
              <h3 class="trend-title"><el-icon style="vertical-align:middle;margin-right:6px"><DataAnalysis /></el-icon>阅读趋势</h3>
              <div ref="trendChartRef" style="height:200px"></div>
            </div>

            <!-- 评论区 -->
            <BlogComments :slug="slug" />
          </el-card>
        </div>

        <!-- 目录 -->
        <aside class="post-toc">
          <BlogToc selector=".markdown-body" />
        </aside>
      </article>

      <!-- 底部导航 -->
      <nav class="post-nav">
        <el-button text icon="ArrowLeft" @click="$router.push('/blog')">返回文章列表</el-button>
      </nav>
    </template>

    <!-- 404 -->
    <el-result v-else icon="warning" title="文章未找到" sub-title="请检查链接是否正确">
      <el-button type="primary" @click="$router.push('/blog')">返回博客</el-button>
    </el-result>

    <!-- 图片灯箱 -->
    <ImageLightbox :src="lightboxSrc" @close="lightboxSrc = ''" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePosts } from '@/composables/usePosts'
import ImageLightbox from '@/components/ImageLightbox.vue'
import { useLikes, useBookmarks } from '@/composables/useInteraction'
import { useAuthStore } from '@/stores/auth'
import request from '@/api/request'
import * as echarts from 'echarts'
import BlogToc from '@/components/BlogToc.vue'
import BlogActionBar from '@/components/BlogActionBar.vue'
import BlogReadingProgress from '@/components/BlogReadingProgress.vue'
import BlogComments from '@/components/BlogComments.vue'

const route = useRoute()
const router = useRouter()
const STATIC_BASE = 'http://localhost:8080'
const API_BASE = 'http://localhost:8080/api'
const slug = route.params.slug
const { getPost } = usePosts()
const lightboxSrc = ref('')

function onPostContentClick(e) {
  const target = e.target
  if (target.tagName === 'IMG' && target.closest('.markdown-body')) {
    lightboxSrc.value = target.getAttribute('src') || ''
  }
}
const authStore = useAuthStore()

const post = ref(null)
const loading = ref(true)
const { liked, count, toggleLike } = useLikes(slug)

async function togglePin() {
  try {
    const res = await request.put(`/posts/${post.value.id}/pin`)
    post.value.isPinned = res.isPinned
    ElMessage.success(res.isPinned ? '已置顶' : '已取消置顶')
  } catch { ElMessage.error('操作失败') }
}
const { bookmarked, toggleBookmark } = useBookmarks(slug)
const relatedPosts = ref([])
const trendChartRef = ref(null)

const canEdit = computed(() => {
  if (!authStore.isLoggedIn || !post.value) return false
  if (authStore.isAdmin) return true
  return post.value.authorId && authStore.userId && String(post.value.authorId) === String(authStore.userId)
})

const readingTime = computed(() => {
  if (!post.value?.content) return ''
  const chars = post.value.content.replace(/\s/g, '').length
  const minutes = Math.max(1, Math.ceil(chars / 500))
  return `📝 约 ${chars.toLocaleString()} 字 · 阅读 ${minutes} 分钟`
})

const codeTheme = ref(localStorage.getItem('code-theme') || 'dark')
const hasCodeBlock = computed(() => post.value?.html?.includes('<pre') || false)

function toggleCodeTheme() {
  codeTheme.value = codeTheme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem('code-theme', codeTheme.value)
}

const isFollowing = ref(false)

async function checkFollow() {
  if (!post.value?.authorId || !authStore.isLoggedIn) return
  try {
    const res = await request.get(`/users/${post.value.authorId}/profile`)
    isFollowing.value = res.isFollowing || false
  } catch { /* ignore */ }
}

async function toggleFollow() {
  if (!post.value?.authorId) return
  try {
    if (isFollowing.value) {
      await request.delete(`/follows/${post.value.authorId}`)
      isFollowing.value = false
    } else {
      await request.post('/follows', { userId: Number(post.value.authorId) })
      isFollowing.value = true
    }
  } catch { ElMessage.error('操作失败') }
}

function handleShare() {
  if (navigator.share) {
    navigator.share({ title: document.title, url: window.location.href }).catch(() => {})
  } else {
    navigator.clipboard.writeText(window.location.href).then(() => {
      ElMessage.success('链接已复制')
    })
  }
}

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

onMounted(async () => {
  post.value = await getPost(slug)
  loading.value = false
  checkFollow()
  // 加载相关文章
  try {
    const res = await request.get('/posts/' + slug + '/related', { params: { limit: 4 } })
    relatedPosts.value = Array.isArray(res) ? res : []
  } catch (e) { /* ignore */ }
  // 加载阅读趋势图（管理员）
  if (authStore.isAdmin && post.value?.id) {
    try {
      const trendRes = await request.get(`/stats/post/${post.value.id}/trend`)
      if (Array.isArray(trendRes) && trendRes.length && trendChartRef.value) {
        const chart = echarts.init(trendChartRef.value)
        chart.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: 40, right: 16, top: 16, bottom: 24 },
          xAxis: { type: 'category', data: trendRes.map(p => p.date.slice(5)), axisLabel: { fontSize: 11 } },
          yAxis: { type: 'value', minInterval: 1 },
          series: [{ type: 'line', data: trendRes.map(p => p.views), smooth: true,
            areaStyle: { color: 'rgba(64,158,255,0.1)' },
            lineStyle: { color: '#409eff' },
            itemStyle: { color: '#409eff' } }]
        })
      }
    } catch (e) { /* ignore */ }
  }
})

function goToRelated(rp) {
  if (!rp || !rp.slug) return
  router.push({ name: 'BlogPost', params: { slug: rp.slug } })
}
</script>

<style scoped>
.post-page {
  max-width: 1100px;
  margin: 0 auto;
}

.loading-wrap {
  padding: 60px 40px;
}

/* ===== 文章主体布局 ===== */
.post-article {
  display: flex;
  gap: 32px;
  align-items: flex-start;
  position: relative;
}

/* 侧边栏 */
.post-sidebar {
  display: none;
  position: sticky;
  top: 88px;
  flex-shrink: 0;
  width: 48px;
}

@media (min-width: 1200px) {
  .post-sidebar {
    display: block;
  }
}

/* 正文 */
.post-main {
  flex: 1;
  min-width: 0;
}

/* 目录 */
.post-toc {
  display: none;
  position: sticky;
  top: 80px;
  width: 180px;
  flex-shrink: 0;
}

@media (min-width: 1200px) {
  .post-toc {
    display: block;
  }
}

/* ===== 文章卡片 ===== */
.post-card {
  border-radius: 12px;
  border: 1px solid #e8eaed;
  background: #fff;
}

.post-card :deep(.el-card__body) {
  padding: 40px;
}

@media (max-width: 768px) {
  .post-card :deep(.el-card__body) {
    padding: 24px 20px;
  }
}

/* ===== 文章头部 ===== */
.post-header {
  margin-bottom: 0;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.meta-tag {
  display: inline-block;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 500;
  color: #409eff;
  background: #ecf5ff;
  border-radius: 4px;
}

.meta-reading-time {
  font-size: 12px;
  color: #c0c4cc;
}

.code-theme-btn {
  background: none;
  border: 1px solid #e4e7ed;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #909399;
  font-size: 12px;
  transition: all 0.2s;
  padding: 0;
  line-height: 1;
}

.code-theme-btn:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

.post-title {
  margin: 0 0 20px 0;
  font-size: 30px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.35;
  letter-spacing: -0.02em;
}

@media (max-width: 768px) {
  .post-title {
    font-size: 22px;
  }
}

.header-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.author-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.author-date {
  font-size: 12px;
  color: #c0c4cc;
}

.meta-views {
  margin-left: auto;
  font-size: 13px;
  color: #c0c4cc;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ===== Banner 分隔线 ===== */
.post-banner {
  height: 1px;
  background: linear-gradient(to right, #e8eaed, transparent);
  margin: 28px 0 32px;
}

/* ===== Markdown 正文 ===== */
.markdown-body {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
}

@media (max-width: 768px) {
  .markdown-body {
    font-size: 15px;
  }
}

.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4 {
  margin-top: 36px;
  margin-bottom: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.markdown-body h1 {
  font-size: 26px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.markdown-body h2 {
  font-size: 22px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.markdown-body h3 {
  font-size: 18px;
}

.markdown-body h4 {
  font-size: 16px;
}

.markdown-body p {
  margin: 0 0 20px 0;
}

/* 代码 — 暗色（默认） */
.markdown-body pre {
  --code-bg: #1e1e2e;
  --code-fg: #cdd6f4;
  padding: 20px 24px;
  border-radius: 10px;
  overflow-x: auto;
  font-size: 14px;
  line-height: 1.65;
  margin: 0 0 20px 0;
}

:deep(.markdown-body.code-light) pre {
  --code-bg: #f8f9fa;
  --code-fg: #333;
}

.markdown-body pre {
  background: var(--code-bg, #1e1e2e);
  color: var(--code-fg, #cdd6f4);
}

.markdown-body code {
  background: #f3f4f6;
  color: #d63384;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 14px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Monaco', monospace;
}

.markdown-body pre code {
  background: none;
  color: inherit;
  padding: 0;
  font-size: inherit;
}

.markdown-body.code-light pre {
  box-shadow: inset 0 1px 3px rgba(0,0,0,0.08);
  border: 1px solid #e4e7ed;
}

.markdown-body.code-light code {
  background: #e8eaed;
  color: #d63384;
}

/* 引用 */
.markdown-body blockquote {
  margin: 20px 0;
  padding: 4px 20px;
  border-left: 4px solid #409eff;
  color: #606266;
}

.markdown-body blockquote p {
  margin-bottom: 8px;
}

.markdown-body blockquote p:last-child {
  margin-bottom: 0;
}

/* 列表 */
.markdown-body ul,
.markdown-body ol {
  padding-left: 24px;
  margin: 0 0 20px 0;
}

.markdown-body li {
  margin-bottom: 6px;
}

.markdown-body ul ul,
.markdown-body ol ol {
  margin-bottom: 0;
}

/* 图片 */
.markdown-body img {
  max-width: 100%;
  border-radius: 10px;
  margin: 8px 0;
}

/* 链接 */
.markdown-body a {
  color: #409eff;
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: border-color 0.2s;
}

.markdown-body a:hover {
  border-bottom-color: #409eff;
}

/* 分割线 */
.markdown-body hr {
  border: none;
  height: 1px;
  background: #f0f0f0;
  margin: 28px 0;
}

/* 表格 */
.markdown-body table {
  width: 100%;
  border-collapse: collapse;
  margin: 0 0 20px 0;
  font-size: 14px;
}

.markdown-body th,
.markdown-body td {
  padding: 10px 16px;
  border: 1px solid #e4e7ed;
  text-align: left;
}

.markdown-body th {
  background: #f8f9fa;
  font-weight: 600;
}

.markdown-body tr:nth-child(even) td {
  background: #fafbfc;
}

/* ===== 标签 ===== */
.post-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.post-tags :deep(.el-tag) {
  border: none;
  background: #f5f7fa;
  color: #606266;
  font-size: 12px;
  padding: 0 12px;
  height: 26px;
  line-height: 26px;
}

/* ===== 底部互动 ===== */
.post-interactions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

/* ===== 阅读趋势 ===== */
.trend-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}
.trend-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 16px;
  color: #303133;
}
.export-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 1px solid #e4e7ed;
  border-radius: 50%;
  color: #909399;
  text-decoration: none;
  font-size: 14px;
  vertical-align: middle;
  margin-left: 6px;
}
.export-btn:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

/* ===== 底部导航 ===== */
.post-nav {
  margin-top: 32px;
  text-align: center;
  padding-bottom: 8px;
}

/* ===== 回退 ===== */
.content-fallback {
  margin-top: 24px;
}

.raw-content {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  margin: 8px 0 0;
  overflow-x: auto;
  max-height: 500px;
}
</style>
