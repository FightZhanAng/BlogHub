<template>
  <div class="album-page">
    <div class="page-header">
      <h1><el-icon><Camera /></el-icon> 宝宝相册</h1>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon> 新建相册
      </el-button>
    </div>

    <div v-loading="loading" class="album-grid">
      <div
        v-for="album in albums"
        :key="album.id"
        class="album-card"
        @click="$router.push(`/albums/${album.id}`)"
      >
        <div class="album-cover">
          <el-image
            v-if="album.coverImage"
            :src="'http://localhost:8080' + album.coverImage"
            fit="cover"
            lazy
          >
            <template #error>
              <div class="cover-placeholder">
                <el-icon :size="48"><Picture /></el-icon>
              </div>
            </template>
          </el-image>
          <div v-else class="cover-placeholder">
            <el-icon :size="48"><Picture /></el-icon>
          </div>
          <div class="album-count">{{ album.photoCount || 0 }} 张</div>
        </div>
        <div class="album-info">
          <h3>{{ album.title }}</h3>
          <p v-if="album.babyName" class="baby-name">{{ album.babyName }}</p>
          <p class="album-date">{{ formatDate(album.createdAt) }}</p>
        </div>
      </div>

      <div v-if="!loading && albums.length === 0" class="empty-state">
        <el-empty description="还没有相册">
          <el-button type="primary" @click="showCreateDialog = true">创建第一个相册</el-button>
        </el-empty>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchAlbums"
      />
    </div>

    <!-- 新建相册对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="新建相册"
      width="500px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="相册标题" prop="title">
          <el-input v-model="form.title" placeholder="如：满月照、百天照" maxlength="100" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="记录一下..." maxlength="500" />
        </el-form-item>
        <el-form-item label="宝宝昵称">
          <el-input v-model="form.babyName" placeholder="如：小豆豆" maxlength="50" />
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker
            v-model="form.babyBirthDate"
            type="date"
            placeholder="选择宝宝出生日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="隐私设置">
          <el-switch v-model="form.isPrivate" active-text="仅登录可见" inactive-text="公开" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { albumApi } from '@/api/albumApi'

const router = useRouter()
const loading = ref(false)
const albums = ref([])
const currentPage = ref(1)
const pageSize = 12
const total = ref(0)

const showCreateDialog = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = ref({
  title: '',
  description: '',
  babyName: '',
  babyBirthDate: null,
  isPrivate: true,
})
const rules = {
  title: [{ required: true, message: '请输入相册标题', trigger: 'blur' }],
}

async function fetchAlbums() {
  loading.value = true
  try {
    const res = await albumApi.list({ page: currentPage.value, size: pageSize })
    albums.value = res.list || []
    total.value = res.total || 0
  } catch (e) {
    console.error('获取相册列表失败', e)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const album = await albumApi.create(form.value)
    ElMessage.success('相册创建成功')
    showCreateDialog.value = false
    router.push(`/albums/${album.id}`)
  } catch (e) {
    ElMessage.error('创建失败')
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.value = { title: '', description: '', babyName: '', babyBirthDate: null, isPrivate: true }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.slice(0, 10)
}

onMounted(fetchAlbums)
</script>

<style scoped>
.album-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text);
  display: flex;
  align-items: center;
  gap: 8px;
}

.album-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.album-card {
  background: var(--color-card);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.album-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.album-cover {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.album-cover :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-bg-warm) 0%, var(--color-border) 100%);
  color: var(--color-text-placeholder);
}

.album-count {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.album-info {
  padding: 14px 16px;
}

.album-info h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.baby-name {
  font-size: 13px;
  color: var(--color-accent);
  margin: 0 0 4px;
}

.album-date {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin: 0;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
