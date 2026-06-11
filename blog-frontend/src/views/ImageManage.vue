<template>
  <div class="image-manage">
    <div class="page-header">
      <h1><el-icon :size="24" style="vertical-align:middle;margin-right:8px"><Picture /></el-icon>图片管理</h1>
      <p class="subtitle">共 {{ images.length }} 张图片</p>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="4" animated />
    </div>

    <el-empty v-else-if="!images.length" description="暂无图片" :image-size="80" />

    <div v-else class="image-grid">
      <div v-for="img in images" :key="img.path" class="image-card">
        <el-image
          :src="img.path.startsWith('http') ? img.path : img.path"
          fit="cover"
          class="image-preview"
          :preview-src-list="[img.path.startsWith('http') ? img.path : img.path]"
        />
        <div class="image-overlay">
          <div class="image-info">
            <span class="image-name" :title="img.name">{{ img.name }}</span>
            <div class="image-meta">
              <span class="image-size">{{ (img.size / 1024).toFixed(1) }} KB</span>
              <el-tag v-if="img.type === 'cover'" size="small" type="warning" effect="plain">封面</el-tag>
              <el-tag v-else-if="img.type === 'content'" size="small" type="primary" effect="plain">正文</el-tag>
              <el-tag v-else size="small" effect="plain">未使用</el-tag>
            </div>
            <el-link v-if="img.postTitle" type="primary" :underline="false" size="small" @click="$router.push('/blog/' + img.postSlug)" class="image-post-link">
              {{ img.postTitle }}
            </el-link>
            <span v-else class="image-no-post">未关联文章</span>
          </div>
          <el-button size="small" type="danger" text @click="deleteImage(img.path)">删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import request from '@/api/request'

const images = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await request.get('/images')
    images.value = Array.isArray(res) ? res : []
  } catch {
    images.value = []
  } finally {
    loading.value = false
  }
})

async function deleteImage(path) {
  try {
    await ElMessageBox.confirm('确定删除这张图片？此操作不可恢复。', '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await request.delete('/images', { params: { path } })
    images.value = images.value.filter(i => i.path !== path)
    ElMessage.success('已删除')
  } catch { /* 取消或失败 */ }
}
</script>

<style scoped>
.image-manage {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px;
}
.page-header { margin-bottom: 24px; }
.page-header h1 { font-size: 24px; color: var(--color-text); margin: 0; }
.subtitle { color: var(--color-text-tertiary); font-size: 14px; margin: 8px 0 0; }
.loading-state { padding: 40px 0; }
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.image-card {
  border: 1px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;
  position: relative;
  background: var(--color-card);
  transition: box-shadow 0.2s;
}
.image-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.image-preview {
  width: 100%;
  height: 180px;
  display: block;
}
.image-overlay {
  padding: 8px 12px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
}
.image-info {
  flex: 1;
  min-width: 0;
}
.image-name {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text);
  font-weight: 500;
  margin-bottom: 4px;
}
.image-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.image-size { color: var(--color-text-placeholder); }
.image-post-link {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.image-no-post {
  color: var(--color-text-placeholder);
  font-size: 11px;
}
</style>
