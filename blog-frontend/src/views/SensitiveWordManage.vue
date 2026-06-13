<template>
  <div class="sensitive-word-manage">
    <div class="page-header">
      <h1><el-icon :size="24" style="vertical-align:middle;margin-right:8px"><Filter /></el-icon>敏感词管理</h1>
    </div>

    <el-card shadow="never" class="search-card">
      <el-form :inline="true" @submit.prevent="doSearch">
        <el-form-item>
          <el-input v-model="searchKeyword" placeholder="搜索敏感词" clearable style="width:200px" @clear="doSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="doSearch">搜索</el-button>
          <el-button type="success" icon="Plus" @click="showAdd">添加</el-button>
          <el-button type="warning" icon="Upload" @click="showBatch">批量导入</el-button>
          <el-button icon="Refresh" @click="handleReload">热加载</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card" style="margin-top:16px">
      <el-table :data="words" stripe v-loading="loading" style="width:100%" empty-text="暂无敏感词">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="word" label="敏感词" min-width="150" />
        <el-table-column prop="level" label="级别" width="120">
          <template #default="{ row }">
            <el-tag :type="row.level === 2 ? 'danger' : 'warning'" size="small">
              {{ row.level === 2 ? '直接拒绝' : '替换为*' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ row.createdAt ? row.createdAt.slice(0,19).replace('T',' ') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" :page-sizes="[20,50,100]" layout="total, sizes, prev, pager, next, jumper" @current-change="fetchWords" @size-change="onSizeChange" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑敏感词' : '添加敏感词'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="敏感词">
          <el-input v-model="form.word" placeholder="请输入敏感词" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="form.level" style="width:100%">
            <el-option :value="1" label="替换为 *" />
            <el-option :value="2" label="直接拒绝" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchVisible" title="批量导入敏感词" width="500px">
      <el-input v-model="batchText" type="textarea" :rows="8" placeholder="每行一个敏感词，格式：词语 或 词语,级别(1或2)" />
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatch">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sensitiveWordApi } from '@/api/sensitiveWordApi'

const words = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const searchKeyword = ref('')

const dialogVisible = ref(false)
const batchVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const form = ref({ word: '', level: 1, enabled: 1 })
const batchText = ref('')

async function fetchWords() {
  loading.value = true
  try {
    const res = await sensitiveWordApi.list(page.value, size.value, searchKeyword.value)
    words.value = res.records || []
    total.value = res.total || 0
  } catch {
    words.value = []
  } finally {
    loading.value = false
  }
}

function showAdd() {
  isEdit.value = false
  editId.value = null
  form.value = { word: '', level: 1, enabled: 1 }
  dialogVisible.value = true
}

function showEdit(row) {
  isEdit.value = true
  editId.value = row.id
  form.value = { word: row.word, level: row.level, enabled: row.enabled }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.word.trim()) {
    ElMessage.warning('请输入敏感词')
    return
  }
  try {
    if (isEdit.value) {
      await sensitiveWordApi.update(editId.value, form.value)
      ElMessage.success('修改成功')
    } else {
      await sensitiveWordApi.add(form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchWords()
  } catch {}
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除敏感词「${row.word}」？`, '确认', {
    type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
  }).then(async () => {
    await sensitiveWordApi.delete(row.id)
    ElMessage.success('已删除')
    fetchWords()
  }).catch(() => {})
}

function showBatch() {
  batchText.value = ''
  batchVisible.value = true
}

async function handleBatch() {
  const lines = batchText.value.split('\n').filter(l => l.trim())
  if (!lines.length) {
    ElMessage.warning('请输入敏感词')
    return
  }
  const words = lines.map(line => {
    const parts = line.split(',')
    return { word: parts[0].trim(), level: parts[1] ? parseInt(parts[1].trim()) : 1 }
  })
  try {
    const res = await sensitiveWordApi.batchAdd(words)
    ElMessage.success(`成功导入 ${res.imported} 个，共 ${res.total} 个`)
    batchVisible.value = false
    fetchWords()
  } catch {}
}

async function handleReload() {
  try {
    await sensitiveWordApi.reload()
    ElMessage.success('敏感词库已热加载')
  } catch {}
}

function doSearch() { page.value = 1; fetchWords() }
function onSizeChange(newSize) { size.value = newSize; page.value = 1; fetchWords() }

onMounted(fetchWords)
</script>

<style scoped>
.sensitive-word-manage { max-width: 1200px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-header h1 { margin: 0; font-size: 24px; }
.table-card { border-radius: 12px; border: 1px solid var(--color-border); }
.pagination-wrap { display: flex; justify-content: flex-start; padding: 20px 0 8px; }
.search-card { border-radius: 12px; border: 1px solid var(--color-border); padding: 4px 0; }
.search-card :deep(.el-card__body) { padding: 12px 20px; }
</style>
