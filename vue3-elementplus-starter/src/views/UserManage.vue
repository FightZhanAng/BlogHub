<template>
  <div class="user-manage">
    <div class="page-header">
      <div class="page-header-row">
        <h1><el-icon :size="24" style="vertical-align:middle;margin-right:8px"><UserFilled /></el-icon>用户管理</h1>
        <el-button type="primary" icon="Plus" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" @submit.prevent="doSearch">
        <el-form-item>
          <el-input v-model="searchKeyword" placeholder="搜索用户名/昵称" clearable style="width:240px" @clear="fetchUsers" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="doSearch">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card" style="margin-top:16px">
      <el-table :data="users" stripe style="width: 100%" v-loading="loading" empty-text="暂无用户">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
              {{ row.role === 'admin' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="toggleStatus(row)"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ row.createdAt ? row.createdAt.slice(0, 19).replace('T', ' ') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="editUser(row)">编辑</el-button>
            <el-button
              size="small"
              type="danger"
              :disabled="row.role === 'admin'"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          :page-sizes="[10,20,50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchUsers"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="80px" ref="formRef" :rules="formRules">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit ? undefined : 'password'">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空则不修改' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const users = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editId = ref(null)

const form = ref({ username: '', password: '', nickname: '', role: 'user' })
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
}

async function fetchUsers() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchKeyword.value.trim()) params.keyword = searchKeyword.value.trim()
    const res = await request.get('/users', { params })
    users.value = res.list || []
    total.value = res.total || 0
  } catch {
    users.value = []
  } finally {
    loading.value = false
  }
}

function doSearch() { page.value = 1; fetchUsers() }
function onSizeChange(newSize) { size.value = newSize; page.value = 1; fetchUsers() }

function openCreate() {
  isEdit.value = false
  editId.value = null
  form.value = { username: '', password: '', nickname: '', role: 'user' }
  dialogVisible.value = true
}

function editUser(row) {
  isEdit.value = true
  editId.value = row.id
  form.value = { username: row.username, nickname: row.nickname, role: row.role, password: '' }
  dialogVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除用户「${row.username}」？`, '确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  }).then(async () => {
    await request.delete(`/users/${row.id}`)
    ElMessage.success('已删除')
    fetchUsers()
  }).catch(() => {})
}

async function toggleStatus(row) {
  try {
    await request.put(`/users/${row.id}`, { status: row.status === 1 ? '0' : '1' })
    row.status = row.status === 1 ? 0 : 1
  } catch {
    // 错误已在拦截器提示
  }
}

async function saveUser() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      const payload = { nickname: form.value.nickname, role: form.value.role }
      if (form.value.password) payload.password = form.value.password
      await request.put(`/users/${editId.value}`, payload)
      ElMessage.success('已更新')
    } else {
      await request.post('/users', form.value)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    fetchUsers()
  } catch {
    // 错误已在拦截器提示
  } finally {
    saving.value = false
  }
}

onMounted(fetchUsers)
</script>

<style scoped>
.user-manage {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
}

.table-card {
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.search-card {
  border-radius: 12px;
  border: 1px solid #e8eaed;
  padding: 4px 0;
}

.search-card :deep(.el-card__body) {
  padding: 12px 20px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-start;
  padding: 20px 0 8px;
}
</style>
