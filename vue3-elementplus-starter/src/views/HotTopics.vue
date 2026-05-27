<template>
  <div class="hot-topics">
    <div class="page-header">
      <h1>每日热点</h1>
      <p class="subtitle">多平台热搜聚合 · 每 2 小时自动更新</p>
    </div>

    <div class="platform-tabs">
      <el-radio-group v-model="activePlatform" size="large" @change="onPlatformChange">
        <el-radio-button v-for="p in platforms" :key="p.key" :value="p.key">
          {{ p.icon }} {{ p.label }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="topic-list">
      <div v-if="currentTopics.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无热点数据">
          <el-button type="primary" @click="handleRefresh">立即抓取</el-button>
        </el-empty>
      </div>

      <div v-for="(topic, idx) in currentTopics" :key="topic.id" class="topic-item" @click="openUrl(topic.url)">
        <div class="topic-rank" :class="rankClass(idx + 1)">{{ idx + 1 }}</div>
        <div class="topic-content">
          <div class="topic-title">{{ topic.title }}</div>
          <div v-if="topic.extra" class="topic-extra">{{ topic.extra }}</div>
        </div>
        <div v-if="topic.hotValue" class="topic-hot">
          <el-icon><Fire /></el-icon>
          {{ formatHot(topic.hotValue) }}
        </div>
      </div>
    </div>

    <div class="footer-actions">
      <el-button @click="handleRefresh" :loading="refreshing">
        <el-icon><Refresh /></el-icon> 刷新数据
      </el-button>
      <span v-if="lastFetchTime" class="fetch-time">最后更新: {{ lastFetchTime }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { hotTopicApi } from '@/api/hotTopicApi'

const loading = ref(false)
const refreshing = ref(false)
const allTopics = ref({})
const activePlatform = ref('github')
const lastFetchTime = ref('')

const platforms = [
  { key: 'weibo', label: '微博', icon: '🔥' },
  { key: 'zhihu', label: '知乎', icon: '💡' },
  { key: 'douyin', label: '抖音', icon: '🎵' },
  { key: 'bili', label: 'B站', icon: '📺' },
  { key: 'toutiao', label: '头条', icon: '📰' },
  { key: 'github', label: 'GitHub', icon: '🐙' },
]

const currentTopics = computed(() => {
  return allTopics.value[activePlatform.value] || []
})

function rankClass(rank) {
  if (rank <= 3) return 'top-three'
  if (rank <= 10) return 'top-ten'
  return ''
}

function formatHot(val) {
  const num = parseInt(val)
  if (isNaN(num)) return val
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return String(num)
}

function openUrl(url) {
  if (url) window.open(url, '_blank')
}

async function fetchTopics() {
  loading.value = true
  try {
    const res = await hotTopicApi.getTodayTopics()
    allTopics.value = res || {}
    const now = new Date()
    lastFetchTime.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
  } catch (e) {
    console.error('获取热点失败', e)
  } finally {
    loading.value = false
  }
}

async function handleRefresh() {
  refreshing.value = true
  try {
    await hotTopicApi.refresh()
    ElMessage.success('热点数据已刷新')
    await fetchTopics()
  } catch (e) {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

function onPlatformChange() {}

onMounted(fetchTopics)
</script>

<style scoped>
.hot-topics {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 26px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.subtitle {
  font-size: 14px;
  color: #909399;
  margin: 4px 0 0;
}

.platform-tabs {
  margin-bottom: 20px;
}

.topic-list {
  min-height: 200px;
}

.topic-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
  border-radius: 8px;
}

.topic-item:hover {
  background: #f5f7fa;
}

.topic-rank {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #909399;
  background: #f0f2f5;
  flex-shrink: 0;
}

.topic-rank.top-three {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: #fff;
}

.topic-rank.top-ten {
  background: linear-gradient(135deg, #ffa502, #ff6348);
  color: #fff;
}

.topic-content {
  flex: 1;
  min-width: 0;
}

.topic-title {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-extra {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-hot {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #e6a23c;
  flex-shrink: 0;
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.fetch-time {
  font-size: 12px;
  color: #c0c4cc;
}

.empty-state {
  padding: 60px 0;
}
</style>
