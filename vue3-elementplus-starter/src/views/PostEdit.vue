<template>
  <div class="post-editor">
    <div class="page-header">
      <h1><el-icon :size="24" style="vertical-align:middle;margin-right:8px"><EditPen /></el-icon>编辑文章</h1>
    </div>

    <!-- 加载中 -->
    <el-skeleton :rows="8" animated v-if="loading" />

    <el-form
      v-else
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="editor-form"
    >
      <el-row :gutter="20">
        <el-col :xs="24" :md="16">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" maxlength="255" show-word-limit />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-form-item label="作者">
            <el-input v-model="form.authorName" placeholder="作者名" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :xs="24" :md="8">
          <el-form-item label="标签">
            <TagSelector v-model="form.tags" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="16">
          <el-form-item label="摘要">
            <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
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

      <el-form-item label="正文（Markdown）" prop="content" class="editor-field">
        <div class="markdown-editor">
          <div class="editor-toolbar">
            <el-radio-group v-model="activeTab" size="small">
              <el-radio-button value="edit"><el-icon style="vertical-align:middle"><EditPen /></el-icon> 编辑</el-radio-button>
              <el-radio-button value="preview"><el-icon style="vertical-align:middle"><View /></el-icon> 预览</el-radio-button>
            </el-radio-group>
            <span class="word-count" v-if="activeTab === 'edit'">{{ form.content.length }} / 65535</span>
          </div>
          <el-input v-if="activeTab === 'edit'" v-model="form.content" type="textarea" :rows="22" maxlength="65535" class="editor-textarea" />
          <div v-else class="preview-markdown markdown-body" v-html="previewHtml" />
        </div>
      </el-form-item>

      <div class="form-actions">
        <el-button size="large" @click="$router.back()">取消</el-button>
        <div class="form-actions-right">
          <el-checkbox v-model="enableSchedule" label="定时发布" border size="small" />
          <el-date-picker v-if="enableSchedule" v-model="scheduledAt" type="datetime"
            placeholder="选择发布时间" value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="d => d < new Date()" size="small" style="width:200px" />
          <el-button size="large" :loading="saving" @click="submitForm(0)">保存草稿</el-button>
          <el-button size="large" type="primary" :loading="saving" @click="submitForm(1)">保存修改</el-button>
        </div>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { postApi } from '@/api/postApi'
import request from '@/api/request'
import { markdownToHtml } from '@/composables/useMarkdown'
import { useAuthStore } from '@/stores/auth'
import TagSelector from '@/components/TagSelector.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(true)
const saving = ref(false)
const activeTab = ref('edit')
const slug = route.params.slug

const enableSchedule = ref(false)
const scheduledAt = ref('')
const API_BASE = 'http://localhost:8080'
const coverInputRef = ref(null)

function triggerCoverUpload() {
  coverInputRef.value?.click()
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
    form.value.coverImage = API_BASE + res.url
    ElMessage.success('封面已上传')
  } catch {
    ElMessage.error('封面上传失败')
  }
  e.target.value = ''
}

const form = ref({
  title: '',
  slug: '',
  description: '',
  coverImage: '',
  content: '',
  tags: '',
  authorName: '',
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }],
}

const previewHtml = computed(() => {
  if (!form.value.content) return '<p style="color:#c0c4cc">暂无内容</p>'
  try {
    const { html } = markdownToHtml(form.value.content)
    return html || '<p style="color:#909399">（渲染为空）</p>'
  } catch {
    return `<pre style="white-space:pre-wrap;background:#f5f7fa;padding:16px;border-radius:8px">${form.value.content.replace(/</g, '&lt;')}</pre>`
  }
})

onMounted(async () => {
  try {
    const raw = await postApi.detail(slug)
    form.value = {
      title: raw.title || '',
      slug: raw.slug || '',
      description: raw.description || '',
      coverImage: raw.coverImage || '',
      content: raw.content || '',
      tags: raw.tags || '',
      authorName: raw.authorName || '',
    }
  } catch {
    ElMessage.error('文章不存在')
    router.push('/blog')
  } finally {
    loading.value = false
  }
})

async function submitForm(status) {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    // 先获取文章 ID
    const raw = await postApi.detail(slug)
    const payload = {
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
    await postApi.update(raw.id, payload)
    ElMessage.success(status === 1 ? '已发布' : '已保存为草稿')
    router.push(`/blog/${slug}`)
  } catch {
    // 错误已在拦截器提示
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.post-editor { max-width: 960px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-header h1 { margin: 0; font-size: 24px; }
.editor-form { background: #fff; padding: 28px 32px; border-radius: 12px; border: 1px solid #e8eaed; }

.editor-field { width: 100%; }
.editor-field :deep(.el-form-item__content) { width: 100%; }

.markdown-editor { width: 100%; border: 1px solid #dcdfe6; border-radius: 8px; overflow: hidden; background: #fff; }
.editor-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; background: #fafafa; border-bottom: 1px solid #e4e7ed; }
.word-count { font-size: 12px; color: #c0c4cc; }

.editor-textarea { width: 100%; }
.editor-textarea :deep(.el-textarea__inner) { min-height: 420px; padding: 16px 20px; border: none; border-radius: 0; font-family: 'Monaco','Menlo','Consolas',monospace; font-size: 14px; line-height: 1.7; resize: vertical; }
.editor-textarea :deep(.el-textarea__inner:focus) { box-shadow: none; }

.preview-markdown { padding: 24px 20px; min-height: 420px; }

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.form-actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cover-upload {
  width: 120px;
  height: 80px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s;
}

.cover-upload:hover { border-color: #409eff; }

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
  color: #c0c4cc;
  font-size: 12px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0; 
}

.preview-markdown :deep(h1), .preview-markdown :deep(h2), .preview-markdown :deep(h3) { margin-top: 28px; margin-bottom: 12px; font-weight: 600; color: #303133; }
.preview-markdown :deep(h1) { font-size: 24px; }
.preview-markdown :deep(h2) { font-size: 20px; border-bottom: 1px solid #ebeef5; padding-bottom: 8px; }
.preview-markdown :deep(h3) { font-size: 17px; }
.preview-markdown :deep(p) { margin: 0 0 16px 0; line-height: 1.8; color: #303133; }
.preview-markdown :deep(code) { background: #f0f2f5; color: #476582; padding: 2px 6px; border-radius: 4px; font-size: 13px; font-family: 'Monaco','Menlo',monospace; }
.preview-markdown :deep(pre) { background: #1e1e1e; color: #d4d4d4; padding: 16px 20px; border-radius: 8px; overflow-x: auto; font-size: 13px; line-height: 1.6; margin: 0 0 16px 0; }
.preview-markdown :deep(pre code) { background: none; color: inherit; padding: 0; font-size: inherit; }
.preview-markdown :deep(blockquote) { margin: 16px 0; padding: 12px 20px; border-left: 4px solid #409eff; background: #f5f7fa; border-radius: 0 8px 8px 0; color: #606266; }
.preview-markdown :deep(ul), .preview-markdown :deep(ol) { padding-left: 24px; margin: 0 0 16px 0; }
.preview-markdown :deep(li) { margin-bottom: 4px; line-height: 1.8; }
.preview-markdown :deep(img) { max-width: 100%; border-radius: 8px; }
.preview-markdown :deep(a) { color: #409eff; text-decoration: none; }
.preview-markdown :deep(hr) { border: none; border-top: 1px solid #ebeef5; margin: 24px 0; }
</style>
