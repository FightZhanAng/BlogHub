<template>
  <div class="log-page">
    <div class="page-header">
      <h1><el-icon :size="24" style="vertical-align:middle;margin-right:8px"><Document /></el-icon>操作日志</h1>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="logs" stripe v-loading="loading" style="width:100%" empty-text="暂无操作日志">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="操作人" width="100" />
        <el-table-column prop="action" label="操作" width="140" />
        <el-table-column prop="resource" label="资源" width="100" />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="createdAt" label="时间" width="170">
          <template #default="{ row }">
            {{ row.createdAt ? row.createdAt.replace('T', ' ') : '-' }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next, jumper" @current-change="fetchLogs" @size-change="onSizeChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const logs = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

async function fetchLogs() {
  loading.value = true
  try {
    const res = await request.get('/logs', { params: { page: page.value, size: size.value } })
    logs.value = res.list || []
    total.value = res.total || 0
  } catch {
    logs.value = []
  } finally {
    loading.value = false
  }
}

function onSizeChange(newSize) { size.value = newSize; page.value = 1; fetchLogs() }

onMounted(fetchLogs)
</script>

<style scoped>
.log-page { max-width: 1200px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-header h1 { margin: 0; font-size: 24px; }
.table-card { border-radius: 12px; border: 1px solid #e8eaed; }
.pagination-wrap { display: flex; justify-content: flex-start; padding: 20px 0 8px; }
</style>
