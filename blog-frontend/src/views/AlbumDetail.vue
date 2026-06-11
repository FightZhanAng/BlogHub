<template>
  <div class="album-detail" v-loading="loading">
    <!-- 顶部信息栏 -->
    <div class="detail-header">
      <div class="header-left">
        <el-button text @click="$router.push('/albums')">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <div class="album-meta">
          <h1>{{ album.title }}</h1>
          <p v-if="album.babyName" class="baby-info">
            {{ album.babyName }}
            <span v-if="album.babyBirthDate"> · {{ album.babyBirthDate }}</span>
          </p>
          <p class="photo-count">{{ photos.length }} 个文件</p>
        </div>
      </div>
      <div class="header-actions">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="waterfall">瀑布流</el-radio-button>
          <el-radio-button value="timeline">时间线</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="showUpload = true">
          <el-icon><Upload /></el-icon> 上传
        </el-button>
        <el-dropdown trigger="click">
          <el-button circle><el-icon><MoreFilled /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="showEditDialog = true">编辑相册</el-dropdown-item>
              <el-dropdown-item divided @click="handleDeleteAlbum">
                <span style="color: #f56c6c">删除相册</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <p v-if="album.description" class="album-desc">{{ album.description }}</p>

    <!-- 瀑布流视图 -->
    <div v-if="viewMode === 'waterfall'" class="waterfall-grid">
      <div v-for="photo in photos" :key="photo.id" class="photo-card">
        <!-- 视频 -->
        <div v-if="isVideo(photo)" class="video-thumb" @click="openLightbox(photo)">
          <video :src="photo.path" preload="metadata" muted />
          <div class="play-icon"><el-icon :size="36"><VideoPlay /></el-icon></div>
          <span class="video-badge">视频</span>
        </div>
        <!-- 图片 -->
        <el-image v-else :src="photo.path" fit="cover" lazy @click="openLightbox(photo)">
          <template #error>
            <div class="img-error"><el-icon><Picture /></el-icon></div>
          </template>
        </el-image>
        <div class="photo-overlay">
          <el-button circle size="small" class="overlay-btn" @click.stop="openLightbox(photo)">
            <el-icon><ZoomIn /></el-icon>
          </el-button>
          <el-button circle size="small" class="overlay-btn danger" @click.stop="handleDeletePhoto(photo)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <div v-if="photo.title" class="photo-title">{{ photo.title }}</div>
      </div>
    </div>

    <!-- 时间线视图 -->
    <div v-if="viewMode === 'timeline'" class="timeline-view">
      <div v-if="timelineGroups.length === 0" class="empty-timeline">
        <el-empty description="暂无内容" />
      </div>
      <div v-for="group in timelineGroups" :key="group.label" class="timeline-group">
        <div class="timeline-dot"></div>
        <div class="timeline-label">
          <span class="month-age">{{ group.label }}</span>
          <span class="date-range">{{ group.dateRange }}</span>
        </div>
        <div class="timeline-photos">
          <div v-for="photo in group.photos" :key="photo.id" class="timeline-photo">
            <div v-if="isVideo(photo)" class="video-thumb small" @click="openLightbox(photo)">
              <video :src="photo.path" preload="metadata" muted />
              <div class="play-icon"><el-icon :size="24"><VideoPlay /></el-icon></div>
            </div>
            <el-image v-else :src="photo.path" fit="cover" lazy @click="openLightbox(photo)">
              <template #error>
                <div class="img-error"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
            <div class="photo-overlay">
              <el-button circle size="small" class="overlay-btn danger" @click.stop="handleDeletePhoto(photo)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && photos.length === 0" class="empty-state">
      <el-empty description="还没有照片">
        <el-button type="primary" @click="showUpload = true">上传第一个文件</el-button>
      </el-empty>
    </div>

    <!-- 灯箱 -->
    <ImageLightbox ref="lightboxRef" />

    <!-- 上传对话框 -->
    <el-dialog v-model="showUpload" title="上传照片/视频" width="600px">
      <div
        class="upload-area"
        @drop.prevent="handleDrop"
        @dragover.prevent="dragover = true"
        @dragleave="dragover = false"
        :class="{ dragover }"
      >
        <el-icon :size="40"><Upload /></el-icon>
        <p>拖拽图片或视频到此处，或 <label class="upload-link" for="file-input">点击选择</label></p>
        <p class="upload-hint">支持 jpg/png/gif/webp 图片和 mp4/webm 视频</p>
        <input id="file-input" type="file" multiple accept="image/*,video/mp4,video/webm,video/quicktime" @change="handleFileSelect" style="display:none" />
      </div>
      <div v-if="uploadQueue.length" class="upload-queue">
        <div v-for="(file, i) in uploadQueue" :key="i" class="queue-item">
          <el-icon v-if="file.type.startsWith('video/')" class="file-icon video"><VideoPlay /></el-icon>
          <el-icon v-else class="file-icon image"><Picture /></el-icon>
          <span class="file-name">{{ file.name }}</span>
          <span class="file-size">{{ formatSize(file.size) }}</span>
          <el-button text size="small" @click="uploadQueue.splice(i, 1)">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!uploadQueue.length" @click="doUpload">
          上传 ({{ uploadQueue.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑相册对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑相册" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="相册标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="宝宝昵称">
          <el-input v-model="editForm.babyName" />
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker v-model="editForm.babyBirthDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { albumApi } from '@/api/albumApi'
import ImageLightbox from '@/components/ImageLightbox.vue'

const route = useRoute()
const router = useRouter()
const albumId = route.params.id

const loading = ref(false)
const album = ref({})
const photos = ref([])
const viewMode = ref('waterfall')
const timelineGroups = ref([])

// 灯箱
const lightboxRef = ref(null)

// 上传
const showUpload = ref(false)
const uploadQueue = ref([])
const uploading = ref(false)
const dragover = ref(false)

// 编辑
const showEditDialog = ref(false)
const editForm = ref({})
const saving = ref(false)

const VIDEO_EXTS = /\.(mp4|webm|ogg|mov)(\?|$)/i

function isVideo(photo) {
  return VIDEO_EXTS.test(photo.path || '')
}

async function fetchAlbum() {
  loading.value = true
  try {
    const [albumRes, photosRes] = await Promise.all([
      albumApi.detail(albumId),
      albumApi.listPhotos(albumId, { page: 1, size: 999 }),
    ])
    album.value = albumRes
    photos.value = photosRes.list || []
  } catch (e) {
    ElMessage.error('获取相册失败')
  } finally {
    loading.value = false
  }
}

async function fetchTimeline() {
  try {
    const res = await albumApi.timeline(albumId)
    timelineGroups.value = res.groups || []
  } catch (e) {
    console.error('获取时间线失败', e)
  }
}

watch(viewMode, (mode) => {
  if (mode === 'timeline' && timelineGroups.value.length === 0) {
    fetchTimeline()
  }
})

function openLightbox(photo) {
  lightboxRef.value?.open(photo.path, photo.title || photo.filename || '')
}

// 上传
function handleDrop(e) {
  dragover.value = false
  const files = [...e.dataTransfer.files].filter(f =>
    f.type.startsWith('image/') || f.type.startsWith('video/')
  )
  uploadQueue.value.push(...files)
}

function handleFileSelect(e) {
  const files = [...e.target.files].filter(f =>
    f.type.startsWith('image/') || f.type.startsWith('video/')
  )
  uploadQueue.value.push(...files)
  e.target.value = ''
}

async function doUpload() {
  if (!uploadQueue.value.length) return
  uploading.value = true
  try {
    const newPhotos = await albumApi.uploadPhotos(albumId, uploadQueue.value)
    photos.value.push(...(newPhotos || []))
    album.value.photoCount = photos.value.length
    ElMessage.success(`成功上传 ${newPhotos?.length || 0} 个文件`)
    uploadQueue.value = []
    showUpload.value = false
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

// 编辑相册
function openEditDialog() {
  editForm.value = {
    title: album.value.title,
    description: album.value.description,
    babyName: album.value.babyName,
    babyBirthDate: album.value.babyBirthDate,
    isPrivate: album.value.isPrivate,
  }
  showEditDialog.value = true
}

watch(showEditDialog, (v) => { if (v) openEditDialog() })

async function handleSave() {
  saving.value = true
  try {
    await albumApi.update(albumId, editForm.value)
    ElMessage.success('保存成功')
    showEditDialog.value = false
    fetchAlbum()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDeletePhoto(photo) {
  try {
    await ElMessageBox.confirm('确定要删除这个文件吗？', '删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await albumApi.deletePhoto(photo.id)
    photos.value = photos.value.filter(p => p.id !== photo.id)
    album.value.photoCount = photos.value.length
    timelineGroups.value.forEach(g => {
      g.photos = g.photos.filter(p => p.id !== photo.id)
    })
    timelineGroups.value = timelineGroups.value.filter(g => g.photos.length > 0)
    ElMessage.success('已删除')
  } catch { /* cancelled */ }
}

async function handleDeleteAlbum() {
  try {
    await ElMessageBox.confirm('确定要删除这个相册吗？所有文件将一并删除。', '删除相册', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await albumApi.delete(albumId)
    ElMessage.success('相册已删除')
    router.push('/albums')
  } catch { /* cancelled */ }
}

onMounted(fetchAlbum)
</script>

<style scoped>
.album-detail {
  max-width: 1200px;
  margin: 0 auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.album-meta h1 {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.baby-info {
  font-size: 14px;
  color: var(--color-accent);
  margin: 4px 0 0;
}

.photo-count {
  font-size: 13px;
  color: var(--color-text-tertiary);
  margin: 2px 0 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.album-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 20px;
  padding: 12px 16px;
  background: var(--color-bg-warm);
  border-radius: 8px;
}

/* ========== 瀑布流 ========== */
.waterfall-grid {
  column-count: 3;
  column-gap: 16px;
}

@media (max-width: 900px) {
  .waterfall-grid { column-count: 2; }
}
@media (max-width: 500px) {
  .waterfall-grid { column-count: 1; }
}

.photo-card {
  break-inside: avoid;
  margin-bottom: 16px;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  background: var(--color-card);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  transition: transform 0.2s, box-shadow 0.2s;
  position: relative;
}

.photo-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.12);
}

.photo-card:hover .photo-overlay,
.timeline-photo:hover .photo-overlay {
  opacity: 1;
}

.photo-overlay {
  position: absolute;
  top: 0;
  right: 0;
  padding: 8px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.2s;
  z-index: 2;
}

.overlay-btn {
  background: rgba(0, 0, 0, 0.5) !important;
  border: none !important;
  color: #fff !important;
}

.overlay-btn:hover {
  background: rgba(0, 0, 0, 0.7) !important;
}

.overlay-btn.danger:hover {
  background: rgba(245, 108, 108, 0.8) !important;
}

.photo-card :deep(.el-image) {
  width: 100%;
  display: block;
}

.photo-title {
  padding: 8px 12px;
  font-size: 13px;
  color: var(--color-text);
}

.img-error {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-warm);
  color: var(--color-text-placeholder);
}

/* ========== 视频缩略图 ========== */
.video-thumb {
  position: relative;
  cursor: pointer;
  background: #000;
}

.video-thumb video {
  width: 100%;
  display: block;
  opacity: 0.7;
}

.video-thumb.small video {
  height: 100%;
  object-fit: cover;
}

.play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.video-thumb:hover .play-icon {
  background: rgba(64, 158, 255, 0.8);
}

.video-thumb.small .play-icon {
  width: 40px;
  height: 40px;
}

.video-badge {
  position: absolute;
  bottom: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
}

/* ========== 时间线 ========== */
.timeline-view {
  padding-left: 24px;
  position: relative;
}

.timeline-view::before {
  content: '';
  position: absolute;
  left: 11px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: var(--color-border);
}

.timeline-group {
  position: relative;
  margin-bottom: 32px;
  padding-left: 24px;
}

.timeline-dot {
  position: absolute;
  left: -29px;
  top: 4px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--color-accent);
  border: 2px solid var(--color-card);
  box-shadow: 0 0 0 2px var(--color-accent);
}

.timeline-label {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}

.month-age {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.date-range {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.timeline-photos {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

.timeline-photo {
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  aspect-ratio: 1;
  transition: transform 0.2s;
  position: relative;
}

.timeline-photo:hover {
  transform: scale(1.03);
}

.timeline-photo :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.empty-timeline,
.empty-state {
  padding: 60px 0;
}

/* ========== 上传 ========== */
.upload-area {
  border: 2px dashed var(--color-border);
  border-radius: 12px;
  padding: 40px;
  text-align: center;
  color: var(--color-text-tertiary);
  transition: all 0.2s;
  cursor: pointer;
}

.upload-area.dragover {
  border-color: var(--color-accent);
  background: var(--color-accent-light);
}

.upload-link {
  color: var(--color-accent);
  cursor: pointer;
}

.upload-hint {
  font-size: 12px;
  color: var(--color-text-placeholder);
  margin-top: 4px;
}

.upload-queue {
  margin-top: 16px;
  max-height: 200px;
  overflow-y: auto;
}

.queue-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-light);
  font-size: 13px;
}

.file-icon.video { color: #e6a23c; }
.file-icon.image { color: var(--color-accent); }

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}
</style>
