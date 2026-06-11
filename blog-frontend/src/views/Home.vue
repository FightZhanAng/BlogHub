<template>
  <div class="home">
    <!-- 欢迎区域 + 统计 -->
    <div class="welcome-section">
      <div class="welcome-left">
        <div class="welcome-overline">Dashboard</div>
        <h1 class="welcome-title">欢迎回来</h1>
        <div class="editorial-divider"></div>
        <p class="welcome-subtitle">管理你的博客，书写你的想法</p>
        <div class="welcome-actions">
          <el-button type="primary" @click="$router.push('/blog/new')">
            写篇文章
          </el-button>
          <el-button @click="$router.push('/blog')">
            浏览博客
          </el-button>
        </div>
      </div>
      <div class="welcome-stats">
        <div class="stat-item">
          <div class="stat-value">{{ formatNumber(stats.postCount) }}</div>
          <div class="stat-label">文章</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-value">{{ formatNumber(stats.totalViews) }}</div>
          <div class="stat-label">阅读量</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-value">{{ formatNumber(stats.commentCount) }}</div>
          <div class="stat-label">评论</div>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="shortcuts-section">
      <div v-for="item in shortcuts" :key="item.title" class="shortcut-card" @click="$router.push(item.path)">
        <div class="shortcut-icon">
          <el-icon :size="20">
            <component :is="item.icon" />
          </el-icon>
        </div>
        <div class="shortcut-info">
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </div>
      </div>
    </div>

    <!-- 最近文章 + 排行榜 -->
    <div class="content-grid">
      <!-- 最近文章 -->
      <div class="content-section">
        <div class="section-header">
          <div class="section-overline">Latest</div>
          <h2 class="section-title">最近文章</h2>
          <el-button text size="small" @click="$router.push('/blog')">查看全部 →</el-button>
        </div>
        <div v-if="recentPosts.length" class="post-list">
          <div
            v-for="(post, i) in recentPosts"
            :key="post.slug"
            class="post-item"
            @click="$router.push(`/blog/${post.slug}`)"
          >
            <div class="post-title">
              <span v-if="post.isPinned" class="pin-badge">置顶</span>
              <span v-if="post.isPrivate" class="private-badge">🔒 仅自己可见</span>
              <span v-if="post.isHidden" class="hidden-badge">🚫 已隐藏</span>
              {{ post.title }}
            </div>
            <div class="post-meta">
              <span>{{ post.date }}</span>
              <span class="meta-dot">·</span>
              <span>{{ post.tags.join(', ') }}</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">还没有文章</div>
      </div>

      <!-- 排行榜 -->
      <div class="content-section">
        <div class="section-header">
          <div class="section-overline">Popular</div>
          <h2 class="section-title">点赞排行榜</h2>
        </div>
        <div v-if="ranking.length" class="ranking-list">
          <div
            v-for="(post, i) in ranking"
            :key="post.slug"
            class="ranking-item"
            @click="$router.push(`/blog/${post.slug}`)"
          >
            <span class="ranking-num" :class="{ top: i < 3 }">{{ String(i + 1).padStart(2, '0') }}</span>
            <span class="ranking-title">
              {{ post.title }}
              <span v-if="post.isHidden" class="ranking-hidden-tag">隐藏</span>
            </span>
            <span class="ranking-likes">{{ post.likes }}</span>
          </div>
        </div>
        <div v-else class="empty-state">暂无数据</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated } from 'vue'
import { usePosts } from '@/composables/usePosts'
import request from '@/api/request'

const { posts, fetchPosts } = usePosts()
const totalComments = ref(0)
const ranking = ref([])

const stats = computed(() => ({
  postCount: posts.value.length,
  totalViews: posts.value.reduce((s, p) => s + p.views, 0),
  commentCount: totalComments.value,
}))

const recentPosts = computed(() => {
  return [...posts.value].slice(0, 5)
})

const shortcuts = [
  { icon: 'Notebook', title: '博客管理', desc: '查看和管理所有文章', path: '/blog' },
  { icon: 'EditPen', title: '写文章', desc: '创建一篇新博客文章', path: '/blog/new' },
  { icon: 'StarFilled', title: '我的收藏', desc: '浏览你收藏的文章', path: '/bookmarks' },
]

function formatNumber(n) {
  if (n == null) return '0'
  return n.toLocaleString('zh-CN')
}

onMounted(async () => {
  const [, statsRes, rankRes] = await Promise.allSettled([
    fetchPosts(),
    request.get('/comments/stats'),
    request.get('/posts/ranking/likes', { params: { limit: 10 } }),
  ])
  if (statsRes.status === 'fulfilled') totalComments.value = statsRes.value?.totalComments || 0
  if (rankRes.status === 'fulfilled') ranking.value = Array.isArray(rankRes.value) ? rankRes.value : []
})

onActivated(async () => {
  await fetchPosts()
  try {
    const rankRes = await request.get('/posts/ranking/likes', { params: { limit: 10 } })
    ranking.value = Array.isArray(rankRes) ? rankRes : []
  } catch { /* ignore */ }
})
</script>

<style scoped>
.home {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 var(--space-md);
}

/* ========== 欢迎区域 ========== */
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
  gap: var(--space-xl);
}

.welcome-left {
  flex: 1;
}

.welcome-overline {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-accent);
  margin-bottom: var(--space-sm);
}

.welcome-title {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 var(--space-md) 0;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.editorial-divider {
  width: 40px;
  height: 2px;
  background: var(--color-accent);
  margin-bottom: var(--space-md);
}

.welcome-subtitle {
  font-size: 15px;
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-lg) 0;
  line-height: 1.6;
}

.welcome-actions {
  display: flex;
  gap: var(--space-sm);
}

/* ========== 统计数据 ========== */
.welcome-stats {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  flex-shrink: 0;
}

.stat-item {
  text-align: center;
  min-width: 60px;
}

.stat-value {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: -0.02em;
  line-height: 1;
  margin-bottom: var(--space-xs);
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-tertiary);
  letter-spacing: 0.02em;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--color-border-light);
}

/* ========== 快捷入口 ========== */
.shortcuts-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-md);
  margin-bottom: var(--space-2xl);
}

@media (max-width: 768px) {
  .shortcuts-section {
    grid-template-columns: 1fr;
  }
  .welcome-section {
    flex-direction: column;
    gap: var(--space-lg);
  }
  .welcome-stats {
    width: 100%;
    justify-content: space-around;
  }
}

.shortcut-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg);
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration) var(--ease);
}

.shortcut-card:hover {
  border-color: var(--color-border);
  box-shadow: var(--shadow-sm);
}

.shortcut-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius);
  background: var(--color-bg-warm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.shortcut-card:hover .shortcut-icon {
  background: var(--color-accent-light);
  color: var(--color-accent);
}

.shortcut-info h3 {
  margin: 0 0 2px 0;
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.shortcut-info p {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* ========== 内容区域 ========== */
.content-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: var(--space-2xl);
}

@media (max-width: 768px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

.content-section {
  min-width: 0;
}

.section-header {
  display: flex;
  align-items: baseline;
  gap: var(--space-sm);
  margin-bottom: var(--space-lg);
}

.section-overline {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-accent);
}

.section-title {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
  flex: 1;
}

/* 最近文章 */
.post-item {
  padding: var(--space-md) 0;
  border-bottom: 1px solid var(--color-border-light);
  cursor: pointer;
  transition: padding-left var(--duration) var(--ease);
}

.post-item:hover {
  padding-left: var(--space-sm);
}

.post-item:last-child {
  border-bottom: none;
}

.post-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: var(--space-xs);
  line-height: 1.4;
}

.pin-badge {
  font-size: 10px;
  font-weight: 600;
  color: var(--color-accent);
  border: 1px solid var(--color-accent);
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  margin-right: 6px;
  vertical-align: middle;
}

.private-badge {
  font-size: 10px;
  font-weight: 600;
  color: #e6a23c;
  border: 1px solid #e6a23c;
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  margin-right: 6px;
  vertical-align: middle;
}

.hidden-badge {
  font-size: 10px;
  font-weight: 600;
  color: #f56c6c;
  border: 1px solid #f56c6c;
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  margin-right: 6px;
  vertical-align: middle;
}

.post-meta {
  font-size: 12px;
  color: var(--color-text-tertiary);
  display: flex;
  gap: var(--space-xs);
}

.meta-dot {
  color: var(--color-border);
}

/* 排行榜 */
.ranking-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-sm) 0;
  cursor: pointer;
  transition: padding-left var(--duration) var(--ease);
}

.ranking-item:hover {
  padding-left: var(--space-xs);
}

.ranking-num {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-placeholder);
  width: 24px;
  flex-shrink: 0;
}

.ranking-num.top {
  color: var(--color-accent);
}

.ranking-title {
  flex: 1;
  font-size: 13px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-likes {
  font-size: 12px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.ranking-hidden-tag {
  font-size: 10px;
  color: #f56c6c;
  border: 1px solid #f56c6c;
  padding: 0 4px;
  border-radius: 3px;
  margin-left: 6px;
  vertical-align: middle;
  font-weight: 600;
}

.empty-state {
  padding: var(--space-xl) 0;
  text-align: center;
  color: var(--color-text-placeholder);
  font-size: 13px;
}
</style>
