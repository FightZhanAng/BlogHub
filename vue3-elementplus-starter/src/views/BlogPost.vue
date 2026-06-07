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
                  <div class="related-card-img" v-if="rp.coverImage">
                    <img :src="rp.coverImage.startsWith('http') ? rp.coverImage : STATIC_BASE + rp.coverImage" alt="" />
                  </div>
                  <div class="related-name">{{ rp.title }}</div>
                  <div class="related-meta">
                    <i class="bi bi-eye" style="font-size:12px"></i>
                    {{ rp.views }} 次阅读
                  </div>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePosts } from '@/composables/usePosts'
import ImageLightbox from '@/components/ImageLightbox.vue'
import { useLikes, useBookmarks } from '@/composables/useInteraction'
import { useAuthStore } from '@/stores/auth'
import request from '@/api/request'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])
import BlogToc from '@/components/BlogToc.vue'
import BlogActionBar from '@/components/BlogActionBar.vue'
import BlogReadingProgress from '@/components/BlogReadingProgress.vue'
import BlogComments from '@/components/BlogComments.vue'

const route = useRoute()
const router = useRouter()
import { API_BASE, STATIC_BASE } from '@/utils/baseUrl'
const { getPost } = usePosts()
const slug = ref(route.params.slug)
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
const { liked, count, toggleLike } = useLikes(slug.value)

async function togglePin() {
  try {
    const res = await request.put(`/posts/${post.value.id}/pin`)
    post.value.isPinned = res.isPinned
    ElMessage.success(res.isPinned ? '已置顶' : '已取消置顶')
  } catch { ElMessage.error('操作失败') }
}
const { bookmarked, toggleBookmark } = useBookmarks(slug.value)
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

const codeTheme = ref(document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light')
const hasCodeBlock = computed(() => post.value?.html?.includes('<pre') || false)

// 监听全局主题变化，同步代码块主题
const observer = new MutationObserver(() => {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark'
  codeTheme.value = isDark ? 'dark' : 'light'
})
observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
onUnmounted(() => observer.disconnect())

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
  post.value = await getPost(slug.value)
  // 如果文章未找到，检查是否是自己的草稿，跳转到编辑页
  if (!post.value && authStore.isLoggedIn) {
    try {
      const raw = await request.get(`/posts/${slug.value}/edit`)
      if (raw && raw.authorId && String(raw.authorId) === String(authStore.userId)) {
        router.push(`/blog/${slug.value}/edit`)
        return
      }
    } catch { /* 不是自己的草稿或不存在 */ }
  }
  loading.value = false
  checkFollow()
  // 加载相关文章
  try {
    const res = await request.get('/posts/' + slug.value + '/related', { params: { limit: 4 } })
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
  // 使用整页跳转确保组件完全重新初始化
  window.location.href = '/blog/' + rp.slug
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
  position: sticky;
  top: 88px;
  flex-shrink: 0;
  width: 48px;
}

@media (max-width: 768px) {
  .post-sidebar {
    display: none;
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
  border: 1px solid var(--color-border);
  background: var(--color-card);
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
  color: var(--color-accent);
  background: var(--color-accent-light);
  border-radius: 4px;
}

.meta-reading-time {
  font-size: 12px;
  color: var(--color-text-placeholder);
}

.post-title {
  margin: 0 0 20px 0;
  font-size: 30px;
  font-weight: 700;
  color: var(--color-text);
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
  color: var(--color-text);
}

.author-date {
  font-size: 12px;
  color: var(--color-text-placeholder);
}

.meta-views {
  margin-left: auto;
  font-size: 13px;
  color: var(--color-text-placeholder);
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ===== Banner 分隔线 ===== */
.post-banner {
  height: 1px;
  background: linear-gradient(to right, var(--color-border), transparent);
  margin: 28px 0 32px;
}

/* ===== Markdown 正文 ===== */
:deep(.markdown-body) {
  font-size: 16px;
  line-height: 1.8;
  color: var(--color-text);
}

@media (max-width: 768px) {
  :deep(.markdown-body) {
    font-size: 15px;
  }
}

:deep(.markdown-body h1),
:deep(.markdown-body h2),
:deep(.markdown-body h3),
:deep(.markdown-body h4) {
  margin-top: 36px;
  margin-bottom: 14px;
  font-weight: 600;
  color: var(--color-text);
}

:deep(.markdown-body h1) {
  font-size: 26px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--color-border-light);
}

:deep(.markdown-body h2) {
  font-size: 22px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border-light);
}

:deep(.markdown-body h3) {
  font-size: 18px;
}

:deep(.markdown-body h4) {
  font-size: 16px;
}

:deep(.markdown-body p) {
  margin: 0 0 20px 0;
}

/* 代码 — 暗色（默认） */
:deep(.markdown-body pre) {
  --code-bg: #1e1e2e;
  --code-fg: #cdd6f4;
  padding: 20px 24px;
  border-radius: 10px;
  overflow-x: auto;
  font-size: 14px;
  line-height: 1.65;
  margin: 0 0 20px 0;
  background: var(--code-bg);
  color: var(--code-fg);
}

:deep(.markdown-body.code-light pre) {
  --code-bg: #f8f9fa;
  --code-fg: #333;
}

/* pre 在 .code-block 内时去掉独立样式，由 .code-block 统一控制 */
:deep(.markdown-body .code-block pre) {
  margin: 0;
  border-radius: 0;
}

:deep(.markdown-body .code-block) {
  margin: 0 0 20px 0;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--color-border);
}

:deep(.markdown-body .code-block.collapsed pre) {
  display: none;
}

:deep(.markdown-body .code-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: var(--color-bg-warm);
  border-bottom: 1px solid var(--color-border);
  font-size: 12px;
}

:deep(.markdown-body .code-lang) {
  color: var(--color-text-tertiary);
  font-family: 'JetBrains Mono', monospace;
}

:deep(.markdown-body .code-actions) {
  display: flex;
  gap: 6px;
}

:deep(.markdown-body .code-copy-btn),
:deep(.markdown-body .code-collapse-btn) {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: var(--color-text-tertiary);
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-family: inherit;
  transition: all 0.2s;
}

:deep(.markdown-body .code-copy-btn:hover),
:deep(.markdown-body .code-collapse-btn:hover) {
  color: var(--color-text);
  background: var(--color-accent-light);
}

:deep(.markdown-body .code-line-count) {
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-left: 8px;
}

:deep(.markdown-body code) {
  background: var(--color-bg-warm);
  color: #d63384;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 14px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Monaco', monospace;
}

:deep(.markdown-body pre code) {
  background: none;
  color: inherit;
  padding: 0;
  font-size: inherit;
}

:deep(.markdown-body.code-light pre) {
  box-shadow: inset 0 1px 3px rgba(0,0,0,0.08);
  border: 1px solid var(--color-border);
}

:deep(.markdown-body.code-light code) {
  background: var(--color-border-light);
  color: #d63384;
}

/* 引用 */
:deep(.markdown-body blockquote) {
  margin: 20px 0;
  padding: 4px 20px;
  border-left: 4px solid var(--color-accent);
  color: var(--color-text-secondary);
}

:deep(.markdown-body blockquote p) {
  margin-bottom: 8px;
}

:deep(.markdown-body blockquote p:last-child) {
  margin-bottom: 0;
}

/* 列表 */
:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  padding-left: 24px;
  margin: 0 0 20px 0;
}

:deep(.markdown-body li) {
  margin-bottom: 6px;
}

:deep(.markdown-body ul ul),
:deep(.markdown-body ol ol) {
  margin-bottom: 0;
}

/* 图片 */
:deep(.markdown-body img) {
  max-width: 100%;
  border-radius: 10px;
  margin: 8px 0;
}

/* 链接 */
:deep(.markdown-body a) {
  color: var(--color-accent);
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: border-color 0.2s;
}

:deep(.markdown-body a:hover) {
  border-bottom-color: var(--color-accent);
}

/* 分割线 */
:deep(.markdown-body hr) {
  border: none;
  height: 1px;
  background: var(--color-border-light);
  margin: 28px 0;
}

/* 表格 */
:deep(.markdown-body table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0 0 20px 0;
  font-size: 14px;
}

:deep(.markdown-body th),
:deep(.markdown-body td) {
  padding: 10px 16px;
  border: 1px solid var(--color-border);
  text-align: left;
}

:deep(.markdown-body th) {
  background: var(--color-bg-warm);
  font-weight: 600;
}

:deep(.markdown-body tr:nth-child(even) td) {
  background: var(--color-bg-warm);
}

/* ===== 标签 ===== */
.post-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border-light);
}

.post-tags :deep(.el-tag) {
  border: none;
  background: var(--color-bg-warm);
  color: var(--color-text-secondary);
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
  border-top: 1px solid var(--color-border-light);
}

/* ===== 推荐阅读 ===== */
.related-posts {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border-light);
}
.related-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 18px;
  color: var(--color-text);
  display: flex;
  align-items: center;
  gap: 8px;
}
.related-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}
.related-card {
  background: var(--color-bg-warm);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 18px 20px;
  cursor: pointer;
  transition: all 0.25s ease;
}
.related-card-img {
  width: 100%;
  height: 100px;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 10px;
}
.related-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.related-card:hover .related-card-img img {
  transform: scale(1.05);
}
.related-card:hover {
  background: var(--color-card);
  border-color: var(--color-border);
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
  transform: translateY(-2px);
}
.related-card:active {
  transform: translateY(0);
}
.related-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
}
.related-card:hover .related-name {
  color: var(--color-accent);
}
.related-meta {
  font-size: 12px;
  color: var(--color-text-placeholder);
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ===== 阅读趋势 ===== */
.trend-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border-light);
}
.trend-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 16px;
  color: var(--color-text);
}
.export-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  color: var(--color-text-tertiary);
  text-decoration: none;
  font-size: 14px;
  vertical-align: middle;
  margin-left: 6px;
}
.export-btn:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
  background: var(--color-accent-light);
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
  background: var(--color-bg-warm);
  padding: 16px;
  border-radius: 8px;
  margin: 8px 0 0;
  overflow-x: auto;
  max-height: 500px;
}
</style>
