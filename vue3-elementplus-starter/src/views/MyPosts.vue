<template>
  <div class="my-posts">
    <div class="page-header">
      <h1>我的文章</h1>
      <el-button type="primary" @click="$router.push('/blog/new')">写文章</el-button>
    </div>

    <!-- 筛选标签 -->
    <el-tabs v-model="filterStatus" class="filter-tabs">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="已发布" name="1" />
      <el-tab-pane label="草稿" name="0" />
      <el-tab-pane label="定时发布" name="scheduled" />
    </el-tabs>

    <!-- 文章列表 -->
    <div class="loading-wrap" v-if="loading"><el-skeleton :rows="5" animated /></div>

    <template v-else-if="filteredPosts.length">
      <div v-for="p in filteredPosts" :key="p.id" class="post-item">
        <div class="post-info">
          <el-tag v-if="p.status === 0 && !p.scheduledAt" size="small" type="warning">草稿</el-tag>
          <el-tag v-else-if="p.scheduledAt" size="small" type="info">定时 {{ p.scheduledAt?.slice(0, 16).replace('T', ' ') }}</el-tag>
          <el-tag v-else size="small" type="success">已发布</el-tag>
          <router-link :to="`/blog/${p.slug}`" class="post-title">{{ p.title || '无标题' }}</router-link>
          <span class="post-meta">{{ p.createdAt?.slice(0, 10) }} · {{ p.views || 0 }} 阅读</span>
        </div>
        <div class="post-actions">
          <el-button v-if="p.status === 0 || p.scheduledAt" size="small" @click="publishPost(p)">
            <i class="bi bi-check-circle"></i> 发布
          </el-button>
          <el-button size="small" @click="$router.push(`/blog/${p.slug}/edit`)">
            <i class="bi bi-pencil"></i> 编辑
          </el-button>
          <el-button size="small" type="danger" @click="deletePost(p)">
            <i class="bi bi-trash"></i> 删除
          </el-button>
        </div>
      </div>
      <el-pagination background layout="total, sizes, prev, pager, next, jumper"
        :total="total" :page-size="size" :page-sizes="[10,20,50]" v-model:current-page="page" @current-change="fetchPosts" @size-change="onSizeChange" />
    </template>

    <el-empty v-else description="暂无文章" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const posts = ref([])
const loading = ref(true)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterStatus = ref('all')

const filteredPosts = computed(() => {
  const list = posts.value
  switch (filterStatus.value) {
    case '0': return list.filter(p => p.status === 0 && !p.scheduledAt)
    case '1': return list.filter(p => p.status === 1 && !p.scheduledAt)
    case 'scheduled': return list.filter(p => p.scheduledAt)
    default: return list
  }
})

async function fetchPosts() {
  loading.value = true
  try {
    const res = await request.get('/users/me/posts', { params: { page: page.value, size: size.value } })
    if (res.list) {
      posts.value = res.list
      total.value = res.total || 0
    } else if (res.records) {
      posts.value = res.records
      total.value = res.total || 0
    } else {
      posts.value = Array.isArray(res) ? res : []
    }
  } catch { /* ignore */ }
  loading.value = false
}

async function publishPost(p) {
  try {
    await request.put(`/posts/${p.id}`, { status: 1 })
    ElMessage.success('已发布')
    fetchPosts()
  } catch { ElMessage.error('发布失败') }
}

async function deletePost(p) {
  try {
    await ElMessageBox.confirm(`确定删除「${p.title || '无标题'}」？`, '确认删除')
    await request.delete(`/posts/${p.id}`)
    ElMessage.success('已删除')
    fetchPosts()
  } catch {
    if (ElMessageBox) { /* cancel */ }
  }
}

function onSizeChange(newSize) { size.value = newSize; page.value = 1; fetchPosts() }

onMounted(fetchPosts)
</script>

<style scoped>
.my-posts { max-width: 900px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h1 { font-size: 22px; margin: 0; }
.filter-tabs { margin-bottom: 16px; }
.loading-wrap { padding: 40px 0; }

.post-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border: 1px solid #e8eaed;
  border-radius: 10px;
  margin-bottom: 10px;
  background: #fff;
  transition: box-shadow 0.2s;
}

.post-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }

.post-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.post-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.post-title:hover { color: #409eff; }

.post-meta {
  font-size: 12px;
  color: #c0c4cc;
  white-space: nowrap;
}

.post-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  margin-left: 12px;
}
</style>
