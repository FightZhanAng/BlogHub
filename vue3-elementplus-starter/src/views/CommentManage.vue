<template>
  <div class="comment-manage">
    <div class="page-header">
      <h1><el-icon :size="24" style="vertical-align:middle;margin-right:8px"><ChatDotSquare /></el-icon>评论管理</h1>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" @submit.prevent="doSearch">
        <el-form-item>
          <el-input v-model="searchKeyword" placeholder="搜索评论内容" clearable style="width:220px" @clear="doSearch" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="searchNickname" placeholder="搜索昵称" clearable style="width:140px" @clear="doSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="doSearch">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card" style="margin-top:16px">
      <el-table :data="comments" stripe v-loading="loading" style="width:100%" empty-text="暂无评论">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="postTitle" label="文章" min-width="140">
          <template #default="{ row }">
            <el-link type="primary" @click="goToPost(row)" v-if="row.postSlug">
              {{ row.postTitle }}
            </el-link>
            <span v-else>{{ row.postTitle }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" width="100" />
        <el-table-column prop="content" label="评论内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="160">
          <template #default="{ row }">
            {{ row.createdAt ? row.createdAt.slice(0,19).replace('T',' ') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next, jumper" @current-change="fetchComments" @size-change="onSizeChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const router = useRouter()

const comments = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchNickname = ref('')

async function fetchComments() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchKeyword.value.trim()) params.keyword = searchKeyword.value.trim()
    if (searchNickname.value.trim()) params.nickname = searchNickname.value.trim()
    const res = await request.get('/comments', { params })
    comments.value = res.list || []
    total.value = res.total || 0
  } catch {
    comments.value = []
  } finally {
    loading.value = false
  }
}

function goToPost(row) {
  if (row.postSlug) router.push(`/blog/${row.postSlug}`)
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除「${row.nickname}」的评论？`, '确认', {
    type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
  }).then(async () => {
    await request.delete(`/comments/${row.id}`)
    ElMessage.success('已删除')
    fetchComments()
  }).catch(() => {})
}

function doSearch() { page.value = 1; fetchComments() }
function onSizeChange(newSize) { size.value = newSize; page.value = 1; fetchComments() }

onMounted(fetchComments)
</script>

<style scoped>
.comment-manage { max-width: 1200px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-header h1 { margin: 0; font-size: 24px; }
.table-card { border-radius: 12px; border: 1px solid #e8eaed; }
.pagination-wrap { display: flex; justify-content: flex-start; padding: 20px 0 8px; }
.search-card { border-radius: 12px; border: 1px solid #e8eaed; padding: 4px 0; }
.search-card :deep(.el-card__body) { padding: 12px 20px; }
</style>
