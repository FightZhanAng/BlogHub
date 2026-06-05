<template>
  <div class="ai-assistant">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <el-button type="primary" @click="createNewConversation">
          <el-icon><Plus /></el-icon> 新对话
        </el-button>
      </div>
      <ChatHistory
        :conversations="conversations"
        :active-id="currentConversationId"
        @select="selectConversation"
        @delete="deleteConversation"
      />
    </aside>

    <!-- 主区域 -->
    <main class="chat-main">
      <!-- 设置栏 -->
      <div class="settings-bar">
        <div class="model-selector">
          <span class="label">模型</span>
          <el-select v-model="selectedModel" size="small" style="width: 200px">
            <el-option
              v-for="m in models"
              :key="m.id"
              :value="m.id"
              :label="m.name"
            >
              <div class="model-option">
                <span class="model-name">{{ m.name }}</span>
                <span class="model-desc">{{ m.description }}</span>
                <div>
                  <el-tag v-if="m.supportImage" size="small" type="info" class="model-badge image">图像</el-tag>
                  <el-tag v-if="m.supportThinking" size="small" type="warning" class="model-badge thinking">思考</el-tag>
                </div>
              </div>
            </el-option>
          </el-select>
        </div>
        <div class="thinking-toggle" v-if="currentModelSupportThinking">
          <el-switch v-model="thinkingEnabled" size="small" />
          <span class="label">思考模式</span>
          <el-tooltip content="开启后AI会先进行深度思考，再给出回答" placement="top">
            <el-icon><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
      </div>

      <!-- 消息区域 -->
      <div class="messages-container" ref="messagesRef">
        <ChatMessage
          v-for="msg in messages"
          :key="msg.id"
          :message="msg"
          :show-thinking="thinkingEnabled"
        />

        <!-- 流式输出 -->
        <div v-if="isGenerating" class="chat-message assistant">
          <div class="message-avatar">
            <el-avatar :size="36" style="background: #409eff">
              <span style="font-size: 18px">🤖</span>
            </el-avatar>
          </div>
          <div class="message-body">
            <div class="message-role">AI 助手</div>
            <div v-if="currentThinking" class="thinking-block">
              <div class="thinking-header">
                <el-icon><Loading /></el-icon>
                <span>思考中...</span>
              </div>
              <div class="thinking-content">{{ currentThinking }}</div>
            </div>
            <div class="message-content">
              <StreamingText :content="currentContent" />
            </div>
          </div>
        </div>
      </div>

      <!-- 图片预览 -->
      <div v-if="pendingImages.length > 0" class="image-preview">
        <div v-for="(img, index) in pendingImages" :key="index" class="preview-item">
          <img :src="img.preview" alt="预览" />
          <el-button type="danger" size="small" circle @click="removeImage(index)">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <QuickActions @action="handleQuickAction" />
        <div class="input-box">
          <el-tooltip :content="currentModelSupportImage ? '上传图片' : '当前模型不支持图像'" placement="top">
            <el-button @click="triggerImageUpload" :disabled="!currentModelSupportImage">
              <el-icon><Picture /></el-icon>
            </el-button>
          </el-tooltip>

          <input
            ref="fileInput"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
            multiple
            style="display: none"
            @change="handleImageUpload"
          />

          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            placeholder="输入你的问题... (Ctrl+Enter 发送)"
            @keydown.enter.ctrl="sendMessage"
          />

          <el-button
            type="primary"
            @click="sendMessage"
            :loading="isGenerating"
            :disabled="!inputMessage.trim() && pendingImages.length === 0"
          >
            {{ isGenerating ? '生成中...' : '发送' }}
          </el-button>

          <el-button v-if="isGenerating" type="danger" @click="stopGeneration">停止</el-button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Plus, Close, Loading, Picture, QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { aiApi } from '@/api/aiApi'
import { useAiChat } from '@/composables/useAiChat'
import ChatMessage from '@/components/ChatMessage.vue'
import ChatHistory from '@/components/ChatHistory.vue'
import StreamingText from '@/components/StreamingText.vue'
import QuickActions from '@/components/QuickActions.vue'

const {
  isGenerating,
  currentContent,
  currentThinking,
  streamChat,
  uploadImage,
  stopGeneration
} = useAiChat()

const models = ref([])
const selectedModel = ref('mimo-v2.5-pro')
const thinkingEnabled = ref(false)

const conversations = ref([])
const currentConversationId = ref(null)
const messages = ref([])
const inputMessage = ref('')
const messagesRef = ref(null)
const fileInput = ref(null)
const pendingImages = ref([])

const currentModelSupportImage = computed(() => {
  const model = models.value.find(m => m.id === selectedModel.value)
  return model?.supportImage || false
})

const currentModelSupportThinking = computed(() => {
  const model = models.value.find(m => m.id === selectedModel.value)
  return model?.supportThinking || false
})

watch(selectedModel, (newModel) => {
  const model = models.value.find(m => m.id === newModel)
  if (!model?.supportThinking) thinkingEnabled.value = false
  if (!model?.supportImage) pendingImages.value = []
})

onMounted(async () => {
  try {
    const [modelsRes, conversationsRes] = await Promise.all([
      aiApi.getModels(),
      aiApi.getConversations()
    ])
    models.value = modelsRes.data
    conversations.value = conversationsRes.data
    if (conversations.value.length > 0) {
      selectConversation(conversations.value[0].id)
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  }
})

function triggerImageUpload() {
  fileInput.value.click()
}

async function handleImageUpload(event) {
  const files = event.target.files
  if (!files.length) return

  for (const file of files) {
    if (file.size > 50 * 1024 * 1024) {
      ElMessage.error('图片大小不能超过 50MB')
      continue
    }
    const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp']
    if (!validTypes.includes(file.type)) {
      ElMessage.error('不支持的图片格式')
      continue
    }
    const preview = URL.createObjectURL(file)
    pendingImages.value.push({ file, preview })
  }
  event.target.value = ''
}

function removeImage(index) {
  const img = pendingImages.value[index]
  URL.revokeObjectURL(img.preview)
  pendingImages.value.splice(index, 1)
}

async function selectConversation(id) {
  currentConversationId.value = id
  const conversation = conversations.value.find(c => c.id === id)
  if (conversation) {
    selectedModel.value = conversation.modelId
    thinkingEnabled.value = conversation.thinkingEnabled
  }
  const { data } = await aiApi.getHistory(id)
  messages.value = data
  scrollToBottom()
}

async function sendMessage() {
  if (!inputMessage.value.trim() && pendingImages.value.length === 0) return
  if (isGenerating.value) return

  if (!currentConversationId.value) {
    const { data } = await aiApi.createConversation({
      title: inputMessage.value.slice(0, 50) || '图片对话',
      modelId: selectedModel.value,
      thinkingEnabled: thinkingEnabled.value
    })
    currentConversationId.value = data.id
    conversations.value.unshift(data)
  }

  // 上传图片
  const imageUrls = []
  for (const img of pendingImages.value) {
    try {
      const result = await uploadImage(img.file)
      imageUrls.push(result.url)
    } catch (error) {
      console.error('图片上传失败:', error)
      ElMessage.error('图片上传失败')
    }
  }

  const messageData = {
    conversationId: currentConversationId.value,
    message: inputMessage.value,
    modelId: selectedModel.value,
    thinkingEnabled: thinkingEnabled.value
  }
  if (imageUrls.length > 0) messageData.imageUrls = imageUrls

  messages.value.push({
    role: 'user',
    content: inputMessage.value,
    images: imageUrls.length > 0 ? { urls: imageUrls } : null,
    timestamp: Date.now()
  })

  const userMessage = inputMessage.value
  inputMessage.value = ''
  pendingImages.value = []
  scrollToBottom()

  try {
    const result = await streamChat(messageData)
    messages.value.push({
      role: 'assistant',
      content: result.content,
      thinkingContent: result.thinking || null,
      timestamp: Date.now()
    })
  } catch (error) {
    ElMessage.error(error.message || '生成失败')
  }

  scrollToBottom()
}

async function createNewConversation() {
  const { data } = await aiApi.createConversation({
    title: '新对话',
    modelId: selectedModel.value,
    thinkingEnabled: thinkingEnabled.value
  })
  conversations.value.unshift(data)
  currentConversationId.value = data.id
  messages.value = []
}

async function deleteConversation(id) {
  await aiApi.deleteConversation(id)
  conversations.value = conversations.value.filter(c => c.id !== id)
  if (currentConversationId.value === id) {
    currentConversationId.value = conversations.value[0]?.id || null
    messages.value = []
  }
}

function handleQuickAction(action) {
  const prompts = {
    summarize: '请总结这篇文章的要点',
    explain: '请解释这段代码的含义',
    optimize: '请帮我优化这段写作',
    translate: '请将以下内容翻译成英文'
  }
  inputMessage.value = prompts[action] || ''
}

function scrollToBottom() {
  setTimeout(() => {
    messagesRef.value?.scrollTo(0, messagesRef.value.scrollHeight)
  }, 100)
}
</script>

<style scoped>
.ai-assistant {
  display: flex;
  height: calc(100vh - 60px);
}
.sidebar {
  width: 280px;
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-light);
}
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.settings-bar {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
}
.model-selector, .thinking-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
}
.label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}
.model-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.model-name { font-weight: 500; }
.model-desc { font-size: 12px; color: var(--el-text-color-secondary); }
.model-badge { font-size: 11px; padding: 1px 6px; border-radius: 4px; }
.model-badge.image { background: rgba(64,158,255,0.1); color: var(--el-color-primary); }
.model-badge.thinking { background: rgba(230,162,60,0.1); color: var(--el-color-warning); }

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
.thinking-block {
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}
.thinking-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-color-warning);
}
.thinking-content {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  white-space: pre-wrap;
}
.image-preview {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-top: 1px solid var(--el-border-color-light);
}
.preview-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
}
.preview-item img { width: 100%; height: 100%; object-fit: cover; }
.preview-item .el-button { position: absolute; top: 4px; right: 4px; }

.input-area {
  padding: 16px;
  border-top: 1px solid var(--el-border-color-light);
}
.input-box {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.input-box .el-input { flex: 1; }

/* 消息样式 */
.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.message-body { max-width: 75%; }
.message-role {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.message-content {
  background: var(--el-fill-color-light);
  border-radius: 12px;
  padding: 12px 16px;
  line-height: 1.6;
  word-break: break-word;
}
</style>
