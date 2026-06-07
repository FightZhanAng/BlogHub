<template>
  <div class="post-editor">
    <div class="page-header">
      <h1><el-icon :size="24" style="vertical-align:middle;margin-right:8px"><EditPen /></el-icon>写文章</h1>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="editor-form"
    >
      <el-row :gutter="20">
        <el-col :xs="24" :md="16">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="文章标题" maxlength="255" show-word-limit />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-form-item label="URL 标识" prop="slug">
            <el-autocomplete
              v-model="form.slug"
              :fetch-suggestions="querySlugs"
              placeholder="hello-world"
              maxlength="128"
              clearable
              @select="handleSlugSelect"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :xs="24" :md="8">
          <el-form-item label="标签">
            <TagSelector v-model="form.tags" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-form-item label="作者">
            <el-input v-model="form.authorName" placeholder="作者名" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-form-item label="摘要">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="2"
              placeholder="文章摘要（可选）"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-form-item label="封面图">
            <div class="cover-upload" @click="triggerCoverUpload">
              <img v-if="form.coverImage" :src="form.coverImage" class="cover-preview" />
              <div v-else class="cover-placeholder">
                <i class="bi bi-image" style="font-size:24px"></i>
                <span>点击上传封面</span>
              </div>
            </div>
            <input type="file" ref="coverInputRef" accept="image/*" style="display:none" @change="handleCoverChange" />
            <el-button v-if="form.coverImage" size="small" text type="danger" @click="form.coverImage = ''">移除</el-button>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 正文编辑器 -->
      <el-form-item label="正文（Markdown）" prop="content" class="editor-field">
        <div
          class="markdown-editor"
          :class="{ 'drag-over': isDragOver }"
          @dragover.prevent="isDragOver = true"
          @dragleave.prevent="isDragOver = false"
          @drop.prevent="handleDrop"
        >
          <div class="drag-hint" v-if="isDragOver">
            <el-icon :size="40"><Upload /></el-icon>
            <p>释放以上传图片</p>
          </div>
          <div class="editor-toolbar">
            <el-radio-group v-model="activeTab" size="small">
              <el-radio-button value="edit">
                <el-icon style="vertical-align:middle"><EditPen /></el-icon> 编辑
              </el-radio-button>
              <el-radio-button value="preview">
                <el-icon style="vertical-align:middle"><View /></el-icon> 预览
              </el-radio-button>
            </el-radio-group>
            <div class="toolbar-right">
              <el-button size="small" text @click="triggerImageUpload" title="插入图片">
                <i class="bi bi-image"></i>
              </el-button>
              <input type="file" ref="fileInputRef" accept="image/*" style="display:none" @change="handleFileChange" />
              <span class="word-count" v-if="activeTab === 'edit'">
                {{ form.content.length }} / 65535
              </span>
            </div>
          </div>

          <!-- 编辑区 -->
          <el-input
            v-if="activeTab === 'edit'"
            v-model="form.content"
            type="textarea"
            :rows="22"
            placeholder="支持 Markdown 语法...（可拖拽/粘贴上传图片）"
            maxlength="65535"
            class="editor-textarea"
            @paste="handlePaste"
          />

          <!-- 预览区 -->
          <div v-else class="preview-markdown markdown-body" v-html="previewHtml" />
        </div>
      </el-form-item>

      <div class="form-actions">
        <el-button size="large" @click="$router.push('/blog')">取消</el-button>
        <el-button size="large" type="danger" plain @click="clearForm">清空内容</el-button>
        <div class="form-actions-right">
          <el-checkbox v-model="enableSchedule" label="定时发布" border size="small" />
          <el-date-picker v-if="enableSchedule" v-model="scheduledAt" type="datetime"
            placeholder="选择发布时间" value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="d => d < new Date()" size="small" style="width:200px" />
          <el-button size="large" :loading="submitting" @click="submitForm(0)">保存草稿</el-button>
          <el-button size="large" type="primary" :loading="submitting" @click="submitForm(1)">
            发布文章
          </el-button>
        </div>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { postApi } from '@/api/postApi'
import request from '@/api/request'
import { markdownToHtml } from '@/composables/useMarkdown'
import { useAuthStore } from '@/stores/auth'
import TagSelector from '@/components/TagSelector.vue'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const submitting = ref(false)
const activeTab = ref('edit')

const form = ref({
  slug: '',
  title: '',
  description: '',
  coverImage: '',
  content: '',
  tags: '',
  authorName: authStore.nickname || authStore.username || '',
})

// ========== Slug 管理 ==========
const mySlugs = ref([])

async function loadMySlugs() {
  try {
    mySlugs.value = await postApi.mySlugs()
  } catch { /* ignore */ }
}

function querySlugs(queryString, cb) {
  const results = queryString
    ? mySlugs.value.filter(s => s.toLowerCase().includes(queryString.toLowerCase()))
    : mySlugs.value
  cb(results.map(s => ({ value: s })))
}

function clearForm() {
  ElMessageBox.confirm('确定清空所有内容？此操作不可恢复。', '清空内容', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    form.value = {
      slug: '',
      title: '',
      description: '',
      coverImage: '',
      content: '',
      tags: '',
      authorName: authStore.nickname || authStore.username || '',
    }
    enableSchedule.value = false
    scheduledAt.value = ''
    localStorage.removeItem(DRAFT_KEY)
    ElMessage.success('内容已清空')
  }).catch(() => {})
}

async function handleSlugSelect(item) {
  form.value.slug = item.value
  // 加载已有文章内容
  await loadExistingPost(item.value)
}

async function loadExistingPost(slug) {
  if (!slug) return
  try {
    const raw = await request.get(`/posts/${slug}/edit`)
    if (raw && raw.title) {
      ElMessage.info('已加载已有文章内容')
      form.value.title = raw.title || form.value.title
      form.value.description = raw.description || form.value.description
      form.value.coverImage = raw.coverImage || form.value.coverImage
      form.value.content = raw.content || form.value.content
      form.value.tags = raw.tags || form.value.tags
      form.value.authorName = raw.authorName || form.value.authorName
    }
  } catch { /* slug 不存在或不是自己的文章，忽略 */ }
}

onMounted(() => { loadMySlugs() })

const enableSchedule = ref(false)
const scheduledAt = ref('')
const fileInputRef = ref(null)
const coverInputRef = ref(null)
const isDragOver = ref(false)

/** 自动保存草稿 key */
const DRAFT_KEY = 'draft_autosave'

function triggerImageUpload() {
  fileInputRef.value?.click()
}

function triggerCoverUpload() {
  coverInputRef.value?.click()
}

async function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await request.post('/upload', formData, {
      headers: { 'Content-Type': undefined }
    })
    const url = res.url
    const textarea = document.querySelector('.editor-textarea textarea')
    if (textarea) {
      const start = textarea.selectionStart
      const end = textarea.selectionEnd
      const before = form.value.content.substring(0, start)
      const after = form.value.content.substring(end)
      form.value.content = before + `![image](${url})` + after
    } else {
      form.value.content += `\n![image](${url})\n`
    }
    ElMessage.success('图片已插入')
  } catch (err) {
    ElMessage.error('图片上传失败')
  }
  e.target.value = ''
}

/** 拖拽上传图片 */
async function handleDrop(e) {
  const files = e.dataTransfer?.files
  if (!files?.length) return
  isDragOver.value = false
  for (const file of files) {
    if (!file.type.startsWith('image/')) continue
    await uploadImageToEditor(file)
  }
}

/** 粘贴上传图片 */
async function handlePaste(e) {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (file) await uploadImageToEditor(file)
    }
  }
}

/** 通用：上传图片并插入编辑器 */
async function uploadImageToEditor(file) {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await request.post('/upload', formData, {
      headers: { 'Content-Type': undefined }
    })
    const url = res.url || ''
    form.value.content += `\n![image](${url})\n`
    ElMessage.success('图片已插入')
  } catch (err) {
    ElMessage.error('图片上传失败')
  }
}

async function handleCoverChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await request.post('/upload', formData, {
      headers: { 'Content-Type': undefined }
    })
    form.value.coverImage = res.url
    ElMessage.success('封面已上传')
  } catch {
    ElMessage.error('封面上传失败')
  }
  e.target.value = ''
}

const rules = {
  title: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { max: 255, message: '标题不能超过 255 个字符', trigger: 'blur' },
  ],
  slug: [
    { required: true, message: '请输入 URL 标识', trigger: 'blur' },
    { pattern: /^[a-z0-9]+(-[a-z0-9]+)*$/, message: '仅支持小写字母、数字和中划线', trigger: 'blur' },
  ],
  content: [
    { required: true, message: '请输入正文内容', trigger: 'blur' },
  ],
}

const previewHtml = computed(() => {
  if (!form.value.content) return '<p style="color:var(--color-text-placeholder)">暂无内容</p>'
  try {
    const { html } = markdownToHtml(form.value.content)
    return html || '<p style="color:var(--color-text-tertiary)">（内容为空）</p>'
  } catch {
    return `<pre style="white-space:pre-wrap;background:var(--color-bg-warm);padding:16px;border-radius:8px">${form.value.content.replace(/</g, '&lt;')}</pre>`
  }
})

async function submitForm(status) {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  const payload = {
    slug: form.value.slug,
    title: form.value.title,
    description: form.value.description,
    coverImage: form.value.coverImage,
    content: form.value.content,
    tags: form.value.tags,
    authorName: form.value.authorName,
    status: status,
  }
  if (status === 0 && enableSchedule.value && scheduledAt.value) {
    payload.scheduledAt = scheduledAt.value
  }
  try {
    await postApi.create(payload)
    if (status === 1) {
      ElMessage.success('文章已发布')
      router.push(`/blog/${form.value.slug}`)
    } else {
      ElMessage.success('已保存为草稿')
      // 草稿保存成功，留在当前页面
    }
  } catch {
    // 错误已在拦截器提示
  } finally {
    submitting.value = false
    // 提交成功后清除自动保存的草稿
    localStorage.removeItem(DRAFT_KEY)
  }
}

// ========== 自动保存草稿 ==========

/** 自动保存定时器 */
let autoSaveTimer = null

/** 保存当前草稿到 localStorage */
function saveDraft() {
  const draft = {
    userId: authStore.userId,
    slug: form.value.slug,
    title: form.value.title,
    description: form.value.description,
    coverImage: form.value.coverImage,
    content: form.value.content,
    tags: form.value.tags,
    authorName: form.value.authorName,
    savedAt: Date.now(),
  }
  localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
}

/** 自动保存：内容变化后每 30 秒存一次 */
watch(
  () => [form.value.content, form.value.title],
  () => {
    clearTimeout(autoSaveTimer)
    autoSaveTimer = setTimeout(saveDraft, 30000)
  },
  { deep: false }
)

/** 页面加载时检查是否有未提交的草稿 */
onMounted(() => {
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) return
    const draft = JSON.parse(raw)
    // 只恢复当前用户的草稿
    if (draft.userId && draft.userId !== authStore.userId) {
      localStorage.removeItem(DRAFT_KEY)
      return
    }
    if (!draft.content && !draft.title) return
    // 只有内容非空时才提示恢复
    if (draft.content || draft.title) {
      ElMessage.info('检测到未保存的草稿，内容已自动恢复')
      form.value.slug = draft.slug || form.value.slug
      form.value.title = draft.title || form.value.title
      form.value.description = draft.description || form.value.description
      form.value.coverImage = draft.coverImage || form.value.coverImage
      form.value.content = draft.content || form.value.content
      form.value.tags = draft.tags || form.value.tags
      form.value.authorName = draft.authorName || form.value.authorName
    }
  } catch { /* ignore */ }
})

// ========== 键盘快捷键 ==========

function handleKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    saveDraft()
    ElMessage.success('草稿已保存')
  }
  if ((e.ctrlKey || e.metaKey) && e.shiftKey && (e.key === 'p' || e.key === 'P')) {
    e.preventDefault()
    isPreview.value = !isPreview.value
  }
}

onMounted(() => { document.addEventListener('keydown', handleKeydown) })
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  clearTimeout(autoSaveTimer)
  // 离开页面前保存一次
  if (form.value.content || form.value.title) {
    saveDraft()
  }
})
</script>

<style scoped>
.post-editor {
  max-width: 960px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
}

.editor-form {
  background: var(--color-card);
  padding: 28px 32px;
  border-radius: 12px;
  border: 1px solid var(--color-border);
}

/* ===== Markdown 编辑器 ===== */
.editor-field {
  width: 100%;
}

.editor-field :deep(.el-form-item__content) {
  width: 100%;
}

.markdown-editor {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  position: relative;
  overflow: hidden;
  background: var(--color-card);
}

.markdown-editor.drag-over {
  border-color: var(--color-accent);
  background: rgba(201, 169, 110, 0.05);
}

.drag-hint {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(28, 28, 31, 0.92);
  z-index: 10;
  border-radius: 8px;
  color: var(--color-accent);
  font-size: 14px;
  pointer-events: none;
}

.drag-hint p {
  margin: 8px 0 0;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--color-bg-warm);
  border-bottom: 1px solid var(--color-border-light);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.word-count {
  font-size: 12px;
  color: var(--color-text-placeholder);
}

/* 编辑区文本域 */
.editor-textarea {
  width: 100%;
}

.editor-textarea :deep(.el-textarea__inner) {
  min-height: 420px;
  padding: 16px 20px;
  border: none;
  border-radius: 0;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.7;
  resize: vertical;
}

.editor-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

/* 预览区 */
.preview-markdown {
  padding: 24px 20px;
  min-height: 420px;
}

/* 操作按钮 */
.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-light);
}

.form-actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ===== Markdown 预览样式 ===== */
.preview-markdown :deep(h1),
.preview-markdown :deep(h2),
.preview-markdown :deep(h3) {
  margin-top: 28px;
  margin-bottom: 12px;
  font-weight: 600;
  color: var(--color-text);
}
.preview-markdown :deep(h1) { font-size: 24px; }
.preview-markdown :deep(h2) { font-size: 20px; border-bottom: 1px solid var(--color-border-light); padding-bottom: 8px; }
.preview-markdown :deep(h3) { font-size: 17px; }
.preview-markdown :deep(p) { margin: 0 0 16px 0; line-height: 1.8; color: var(--color-text); }
.preview-markdown :deep(code) {
  background: var(--color-bg-warm);
  color: var(--color-text-secondary);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'Monaco', 'Menlo', monospace;
}
.preview-markdown :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px 20px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.6;
  margin: 0 0 16px 0;
}
.preview-markdown :deep(pre code) {
  background: none;
  color: inherit;
  padding: 0;
  font-size: inherit;
}
.preview-markdown :deep(blockquote) {
  margin: 16px 0;
  padding: 12px 20px;
  border-left: 4px solid var(--color-accent);
  background: var(--color-bg-warm);
  border-radius: 0 8px 8px 0;
  color: var(--color-text-secondary);
}
.preview-markdown :deep(ul),
.preview-markdown :deep(ol) {
  padding-left: 24px;
  margin: 0 0 16px 0;
}
.preview-markdown :deep(li) { margin-bottom: 4px; line-height: 1.8; }
.preview-markdown :deep(img) { max-width: 100%; border-radius: 8px; }
.preview-markdown :deep(a) { color: var(--color-accent); text-decoration: none; }
.preview-markdown :deep(hr) { border: none; border-top: 1px solid var(--color-border-light); margin: 24px 0; }

/* 封面上传 */
.cover-upload {
  width: 120px;
  height: 80px;
  border: 2px dashed var(--color-border);
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s;
}

.cover-upload:hover { border-color: var(--color-accent); }

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--color-text-placeholder);
  font-size: 12px;
}
</style>
