<template>
  <div class="home">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-content">
        <div class="welcome-text">
          <h1>
            <el-icon :size="28" color="#409eff" style="vertical-align:middle;margin-right:8px"><UserFilled /></el-icon>
            <span style="vertical-align:middle">欢迎回来</span>
          </h1>
          <p class="subtitle">管理你的博客，书写你的想法</p>
          <el-space>
            <el-button type="primary" icon="EditPen" @click="$router.push('/blog/new')">
              写篇文章
            </el-button>
            <el-button icon="Notebook" @click="$router.push('/blog')">
              浏览博客
            </el-button>
          </el-space>
        </div>
        <div class="welcome-stats">
          <el-statistic title="文章总数" :value="stats.postCount" />
          <el-statistic title="阅读量" :value="stats.totalViews" />
          <el-statistic title="评论" :value="stats.commentCount" />
        </div>
      </div>
    </el-card>

    <!-- 快捷入口 -->
    <el-row :gutter="20">
      <el-col :xs="24" :sm="8" v-for="item in shortcuts" :key="item.title">
        <el-card shadow="never" class="shortcut-card" @click="$router.push(item.path)">
          <div class="shortcut-inner">
            <div class="shortcut-icon" :style="{ background: item.bg }">
              <el-icon :size="24" color="#fff">
                <component :is="item.icon" />
              </el-icon>
            </div>
            <div class="shortcut-info">
              <h3>{{ item.title }}</h3>
              <p>{{ item.desc }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近文章 + 排行榜 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="recent-card">
          <template #header>
            <div class="card-header">
              <span><el-icon style="vertical-align:middle;margin-right:4px"><Notebook /></el-icon> 最近文章</span>
              <el-link type="primary" @click="$router.push('/blog')">查看全部</el-link>
            </div>
          </template>
          <div v-if="recentPosts.length">
            <div
              v-for="(post, i) in recentPosts"
              :key="post.slug"
              class="recent-item"
              :class="{ 'no-border': i === recentPosts.length - 1 }"
              @click="$router.push(`/blog/${post.slug}`)"
            >
              <div class="recent-title">
                <el-tag v-if="post.isPinned" size="small" type="warning" effect="plain" style="margin-right:4px;vertical-align:middle">置顶</el-tag>
                {{ post.title }}
              </div>
              <div class="recent-meta">
                <span>{{ post.date }}</span>
                <span>·</span>
                <span>{{ post.tags.join(', ') }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="还没有文章" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="ranking-card">
          <template #header>
            <div class="card-header">
              <span><el-icon style="vertical-align:middle;margin-right:4px" color="#e6a23c"><TrendCharts /></el-icon> 点赞排行榜</span>
            </div>
          </template>
          <div v-if="ranking.length">
            <div
              v-for="(post, i) in ranking"
              :key="post.slug"
              class="ranking-item"
              @click="$router.push(`/blog/${post.slug}`)"
            >
              <span class="ranking-num" :class="i < 3 ? 'top' : ''">{{ i + 1 }}</span>
              <span class="ranking-title">{{ post.title }}</span>
              <span class="ranking-likes">{{ post.likes }} 赞</span>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
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
  { icon: 'Notebook', title: '博客管理', desc: '查看和管理所有文章', path: '/blog', bg: 'linear-gradient(135deg, #409eff, #337ecc)' },
  { icon: 'EditPen', title: '写文章', desc: '创建一篇新博客文章', path: '/blog/new', bg: 'linear-gradient(135deg, #67c23a, #529b2e)' },
  { icon: 'StarFilled', title: '我的收藏', desc: '浏览你收藏的文章', path: '/bookmarks', bg: 'linear-gradient(135deg, #e6a23c, #cf9236)' },
]

onMounted(async () => {
  fetchPosts()
  try {
    const res = await request.get('/comments/stats')
    totalComments.value = res.totalComments || 0
  } catch {}
  try {
    const res = await request.get('/posts/ranking/likes', { params: { limit: 10 } })
    ranking.value = Array.isArray(res) ? res : []
  } catch {}
})
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 960px;
  margin: 0 auto;
}

.welcome-card {
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 40px;
}

.welcome-text h1 {
  margin: 0 0 4px 0;
  font-size: 22px;
  color: #303133;
}

.subtitle {
  color: #909399;
  margin: 0 0 20px 0;
  font-size: 14px;
}

.welcome-stats {
  display: flex;
  gap: 40px;
  flex-shrink: 0;
}

/* 快捷入口 */
.shortcut-card {
  border-radius: 12px;
  border: 1px solid #e8eaed;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.shortcut-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}

.shortcut-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.shortcut-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.shortcut-info h3 {
  margin: 0 0 4px 0;
  font-size: 15px;
  color: #303133;
}

.shortcut-info p {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

/* 最近文章 */
.recent-card {
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.recent-item {
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: padding-left 0.2s;
}

.recent-item:hover {
  padding-left: 8px;
}

.recent-item.no-border {
  border-bottom: none;
}

.recent-title {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  margin-bottom: 4px;
}

.recent-meta {
  font-size: 12px;
  color: #c0c4cc;
  display: flex;
  gap: 6px;
}

/* 排行榜 */
.ranking-card {
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: padding-left 0.2s;
}

.ranking-item:hover {
  padding-left: 6px;
}

.ranking-item:last-child {
  border-bottom: none;
}

.ranking-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #909399;
  flex-shrink: 0;
}

.ranking-num.top {
  background: linear-gradient(135deg, #f56c6c, #e6a23c);
  color: #fff;
}

.ranking-title {
  flex: 1;
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-likes {
  font-size: 12px;
  color: #c0c4cc;
  flex-shrink: 0;
}
</style>
