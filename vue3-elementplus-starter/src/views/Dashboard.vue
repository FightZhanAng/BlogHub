<template>
  <div class="dashboard">
    <h1>仪表盘</h1>

    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图 -->
    <el-card shadow="never" class="chart-card">
      <template #header>最近 7 天趋势</template>
      <div ref="chartRef" style="height:280px"></div>
    </el-card>

    <!-- 热门搜索 -->
    <el-card v-if="hotKeywords.length" shadow="never" class="chart-card">
      <template #header>热门搜索词</template>
      <el-tag v-for="kw in hotKeywords" :key="kw" size="small" effect="plain" class="hot-tag">{{ kw }}</el-tag>
    </el-card>

    <!-- 导出 -->
    <el-card shadow="never" class="chart-card">
      <template #header>数据导出</template>
      <div class="export-btns">
        <el-button size="small" @click="download('/api/export/posts')">导出文章 CSV</el-button>
        <el-button size="small" @click="download('/api/export/users')">导出用户 CSV</el-button>
        <el-button size="small" @click="download('/api/export/comments')">导出评论 CSV</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import * as echarts from 'echarts'

const chartRef = ref(null)
const stats = ref({})
const hotKeywords = ref([])

const statCards = computed(() => [
  { label: '文章总数', value: stats.value.totalPosts ?? '-' },
  { label: '用户总数', value: stats.value.totalUsers ?? '-' },
  { label: '评论总数', value: stats.value.totalComments ?? '-' },
  { label: '总阅读量', value: stats.value.totalViews ?? '-' },
])

function download(url) {
  const token = localStorage.getItem('blog_token')
  if (!token) { ElMessage.warning('请先登录'); return }
  fetch('http://localhost:8080' + url, {
    headers: { 'Authorization': `Bearer ${token}` }
  }).then(res => {
    if (!res.ok) throw new Error('导出失败')
    return res.blob()
  }).then(blob => {
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = url.split('/').pop() + '_' + new Date().toISOString().slice(0, 10) + '.csv'
    a.click()
    URL.revokeObjectURL(a.href)
  }).catch(() => ElMessage.error('导出失败'))
}

onMounted(async () => {
  try {
    const res = await request.get('/stats/dashboard')
    stats.value = res

    // 搜索词（从后端获取）
    try {
      const kwRes = await request.get('/stats/search-keywords', { params: { top: 20 } })
      hotKeywords.value = kwRes || []
    } catch { /* ignore */ }

    // 趋势图
    if (chartRef.value && res.recentTrend) {
      const chart = echarts.init(chartRef.value)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: res.recentTrend.map(d => d.date.slice(5)) },
        yAxis: { type: 'value', min: 0 },
        series: [{
          name: '新增文章',
          type: 'bar',
          data: res.recentTrend.map(d => d.posts),
          itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        }],
        grid: { left: 40, right: 16, top: 20, bottom: 24 },
      })
    }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.dashboard h1 { font-size: 24px; margin: 0 0 20px; }
.stat-cards { margin-bottom: 16px; }
.stat-card { border-radius: 12px; border: 1px solid var(--color-border); margin-bottom: 16px; }
.stat-value { font-size: 28px; font-weight: 700; color: var(--color-text); }
.stat-label { font-size: 13px; color: var(--color-text-tertiary); margin-top: 4px; }
.chart-card { border-radius: 12px; border: 1px solid var(--color-border); margin-bottom: 16px; }
.hot-tag { margin: 4px; }
.export-btns { display: flex; gap: 8px; }
</style>
