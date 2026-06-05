<template>
  <div class="ai-assistant">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="createNewConversation">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          <span>新对话</span>
        </button>
        <button class="theme-toggle" @click="toggleTheme" :title="isDark ? '切换亮色主题' : '切换暗色主题'">
          <!-- 月亮图标（暗色模式下显示，表示"当前是暗色"） -->
          <svg v-if="isDark" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
          <!-- 太阳图标（亮色模式下显示） -->
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
          </svg>
        </button>
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
      <!-- 空状态欢迎页 -->
      <div v-if="!messages.length && !isGenerating" class="welcome-screen">
        <div class="welcome-icon">
          <div class="welcome-glow"></div>
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="url(#welcome-grad)" stroke-width="1.5">
            <defs>
              <linearGradient id="welcome-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#818cf8"/>
                <stop offset="100%" stop-color="#c084fc"/>
              </linearGradient>
            </defs>
            <path d="M12 2a7 7 0 0 1 7 7c0 2.5-1.5 4.5-3 6v2a1 1 0 0 1-1 1h-4a1 1 0 0 1-1-1v-2c-1.5-1.5-3-3.5-3-6a7 7 0 0 1 7-7z"/>
            <line x1="9" y1="21" x2="15" y2="21"/>
            <line x1="10" y1="24" x2="14" y2="24"/>
          </svg>
        </div>
        <h2 class="welcome-title">MiMo 助手</h2>
        <p class="welcome-desc">有什么我可以帮你的？</p>
        <div class="welcome-prompts">
          <button class="prompt-card" v-for="p in welcomePrompts" :key="p.text" @click="inputMessage = p.text; sendMessage()">
            <span class="prompt-icon">{{ p.icon }}</span>
            <span class="prompt-text">{{ p.text }}</span>
          </button>
        </div>
      </div>

      <!-- 消息区域 -->
      <div v-else class="messages-container" ref="messagesRef">
        <ChatMessage
          v-for="msg in messages"
          :key="msg.id"
          :message="msg"
          :show-thinking="thinkingEnabled"
          @preview="lightboxUrl = $event"
        />

        <!-- 流式输出 -->
        <div v-if="isGenerating" class="chat-message assistant streaming">
          <div class="message-avatar">
            <div class="ai-avatar">
              <div class="ai-avatar-ring"></div>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M12 2a7 7 0 0 1 7 7c0 2.5-1.5 4.5-3 6v2a1 1 0 0 1-1 1h-4a1 1 0 0 1-1-1v-2c-1.5-1.5-3-3.5-3-6a7 7 0 0 1 7-7z"/>
                <line x1="9" y1="21" x2="15" y2="21"/>
              </svg>
            </div>
          </div>
          <div class="message-body">
            <div class="message-role">MiMo</div>
            <div v-if="currentThinking" class="thinking-block">
              <div class="thinking-header" @click="thinkingExpanded = !thinkingExpanded">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="thinking-icon"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
                <span>深度思考中...</span>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :style="{ transform: thinkingExpanded ? 'rotate(180deg)' : '' }" class="thinking-chevron"><polyline points="6 9 12 15 18 9"/></svg>
              </div>
              <div v-show="thinkingExpanded" class="thinking-content">{{ currentThinking }}</div>
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
          <button class="preview-remove" @click="removeImage(index)">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </div>

      <!-- 灯箱 -->
      <Teleport to="body">
        <div v-if="lightboxUrl" class="lightbox" @click="lightboxUrl = null">
          <button class="lightbox-close" @click.stop="lightboxUrl = null">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
          <img :src="lightboxUrl" class="lightbox-img" @click.stop />
        </div>
      </Teleport>

      <!-- 输入区域 -->
      <div class="input-area">
        <div class="input-controls">
          <div class="model-selector">
            <select v-model="selectedModel" class="model-select">
              <option v-for="m in models" :key="m.id" :value="m.id">
                {{ m.name }}
              </option>
            </select>
          </div>
          <label class="thinking-toggle" v-if="currentModelSupportThinking">
            <input type="checkbox" v-model="thinkingEnabled" class="thinking-checkbox" />
            <span class="thinking-slider"></span>
            <span class="thinking-label">思考</span>
          </label>
        </div>
        <div class="input-box">
          <button class="icon-btn upload-btn" @click="triggerImageUpload" :disabled="!currentModelSupportImage" :title="currentModelSupportImage ? '上传图片' : '当前模型不支持图像'">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></svg>
          </button>

          <input
            ref="fileInput"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
            multiple
            style="display: none"
            @change="handleImageUpload"
          />

          <textarea
            v-model="inputMessage"
            class="chat-input"
            placeholder="输入你的问题... (Ctrl+Enter 发送)"
            @keydown.enter.ctrl="sendMessage"
            rows="1"
            ref="textareaRef"
          ></textarea>

          <button
            class="send-btn"
            @click="sendMessage"
            :disabled="(!inputMessage.trim() && pendingImages.length === 0) || isGenerating"
            :class="{ generating: isGenerating }"
          >
            <template v-if="isGenerating">
              <button class="stop-btn" @click.stop="stopGeneration">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>
              </button>
            </template>
            <template v-else>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
            </template>
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiApi } from '@/api/aiApi'
import { useAiChat } from '@/composables/useAiChat'
import ChatMessage from '@/components/ChatMessage.vue'
import ChatHistory from '@/components/ChatHistory.vue'
import StreamingText from '@/components/StreamingText.vue'

// 主题切换
const isDark = ref(localStorage.getItem('ai-theme') !== 'light')
function applyTheme() {
  document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
}
function toggleTheme() {
  isDark.value = !isDark.value
  localStorage.setItem('ai-theme', isDark.value ? 'dark' : 'light')
  applyTheme()
}
applyTheme()

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
const thinkingEnabled = ref(true)
const thinkingExpanded = ref(false)

const conversations = ref([])
const currentConversationId = ref(null)
const messages = ref([])
const inputMessage = ref('')
const messagesRef = ref(null)
const textareaRef = ref(null)
const fileInput = ref(null)
const pendingImages = ref([])
const lightboxUrl = ref(null)

const welcomePrompts = [
  { icon: '💡', text: '解释一下什么是大模型' },
  { icon: '📝', text: '帮我写一篇关于 AI 的短文' },
  { icon: '🔧', text: '用 Python 写一个快速排序' },
  { icon: '🌐', text: '翻译成英文：你好世界' },
]

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

watch([currentContent, currentThinking], () => {
  if (isGenerating.value) scrollToBottom()
})

onMounted(async () => {
  try {
    const [modelsRes, conversationsRes] = await Promise.all([
      aiApi.getModels(),
      aiApi.getConversations()
    ])
    models.value = modelsRes
    conversations.value = conversationsRes
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
    pendingImages.value.push({ file, preview: URL.createObjectURL(file) })
  }
  event.target.value = ''
}

function removeImage(index) {
  URL.revokeObjectURL(pendingImages.value[index].preview)
  pendingImages.value.splice(index, 1)
}

async function selectConversation(id) {
  currentConversationId.value = id
  const conversation = conversations.value.find(c => c.id === id)
  if (conversation) {
    selectedModel.value = conversation.modelId
    thinkingEnabled.value = conversation.thinkingEnabled ?? true
  }
  messages.value = await aiApi.getHistory(id)
  scrollToBottom()
}

async function sendMessage() {
  if (!inputMessage.value.trim() && pendingImages.value.length === 0) return
  if (isGenerating.value) return

  if (!currentConversationId.value) {
    const convData = await aiApi.createConversation({
      title: inputMessage.value.slice(0, 50) || '图片对话',
      modelId: selectedModel.value,
      thinkingEnabled: thinkingEnabled.value
    })
    currentConversationId.value = convData.id
    convData.createdAt = convData.createdAt || new Date().toISOString()
    conversations.value.unshift(convData)
  }

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

  inputMessage.value = ''
  pendingImages.value = []
  scrollToBottom()

  try {
    await streamChat(messageData)
    const history = await aiApi.getHistory(currentConversationId.value)
    messages.value = history
    const list = await aiApi.getConversations()
    conversations.value = list
  } catch (error) {
    ElMessage.error(error.message || '生成失败')
  }

  scrollToBottom()
}

async function createNewConversation() {
  currentConversationId.value = null
  messages.value = []
}

async function deleteConversation(id) {
  try {
    await ElMessageBox.confirm('确定要删除这个对话吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch { return }
  try { await aiApi.deleteConversation(id) } catch {}
  conversations.value = conversations.value.filter(c => c.id !== id)
  if (currentConversationId.value === id) {
    currentConversationId.value = conversations.value[0]?.id || null
    messages.value = []
    if (currentConversationId.value) selectConversation(currentConversationId.value)
  }
}

function scrollToBottom() {
  setTimeout(() => {
    messagesRef.value?.scrollTo({ top: messagesRef.value.scrollHeight, behavior: 'smooth' })
  }, 100)
}
</script>

<style scoped>
/* ==============================
   布局
   ============================== */
.ai-assistant {
  display: flex;
  height: calc(100vh - 60px);
  margin: -24px;
  overflow: hidden;
  background: var(--ai-bg);
  color: var(--ai-text);
}

/* ==============================
   侧边栏
   ============================== */
.sidebar {
  width: 260px;
  background: var(--ai-bg-deep);
  border-right: 1px solid var(--ai-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--ai-border);
  display: flex;
  gap: 8px;
}
.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex: 1;
  padding: 10px 16px;
  background: var(--ai-surface);
  border: 1px solid var(--ai-border);
  border-radius: var(--ai-radius);
  color: var(--ai-text);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--ai-transition);
  font-family: inherit;
}
.new-chat-btn:hover {
  background: var(--ai-surface-hover);
  border-color: var(--ai-border-hover);
}
.theme-toggle {
  width: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ai-surface);
  border: 1px solid var(--ai-border);
  border-radius: var(--ai-radius);
  color: var(--ai-text-secondary);
  cursor: pointer;
  transition: all var(--ai-transition);
}
.theme-toggle:hover {
  background: var(--ai-surface-hover);
  color: var(--ai-text);
  border-color: var(--ai-border-hover);
}

/* ==============================
   主区域
   ============================== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  background: var(--ai-bg);
}

/* ==============================
   欢迎页
   ============================== */
.welcome-screen {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  min-height: 0;
}
.welcome-icon {
  position: relative;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}
.welcome-glow {
  position: absolute;
  inset: -10px;
  background: radial-gradient(circle, rgba(99,102,241,0.2) 0%, transparent 70%);
  border-radius: 50%;
  animation: pulse-glow 3s ease-in-out infinite;
}
@keyframes pulse-glow {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.1); }
}
.welcome-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px;
  background: linear-gradient(135deg, #818cf8, #c084fc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.welcome-desc {
  font-size: 15px;
  color: var(--ai-text-secondary);
  margin: 0 0 40px;
}
.welcome-prompts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-width: 560px;
  width: 100%;
}
.prompt-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--ai-surface);
  border: 1px solid var(--ai-border);
  border-radius: var(--ai-radius);
  color: var(--ai-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--ai-transition);
  text-align: left;
  font-family: inherit;
}
.prompt-card:hover {
  background: var(--ai-surface-hover);
  border-color: var(--ai-border-hover);
  color: var(--ai-text);
}
.prompt-icon { font-size: 18px; }
.prompt-text { line-height: 1.4; }

/* ==============================
   消息区域
   ============================== */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  min-height: 0;
}

/* 流式输出 */
.chat-message.streaming {
  animation: msg-enter 0.3s ease-out;
}
@keyframes msg-enter {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.message-avatar {
  flex-shrink: 0;
}
.ai-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--ai-accent-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  position: relative;
}
.ai-avatar-ring {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 1px solid rgba(99,102,241,0.2);
  animation: ring-pulse 2s ease-in-out infinite;
}
@keyframes ring-pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 1; }
}
.message-body { max-width: 72%; }
.message-role {
  font-size: 12px;
  font-weight: 500;
  color: var(--ai-text-tertiary);
  margin-bottom: 6px;
}
.message-content {
  background: var(--ai-surface);
  border: 1px solid var(--ai-border);
  border-radius: 2px var(--ai-radius-lg) var(--ai-radius-lg) var(--ai-radius-lg);
  padding: 12px 16px;
  line-height: 1.6;
  font-size: 13px;
  word-break: break-word;
  user-select: text;
  cursor: text;
}

/* 思考块 */
.thinking-block {
  background: var(--ai-thinking-bg);
  border: 1px solid var(--ai-thinking-border);
  border-radius: var(--ai-radius);
  padding: 10px 14px;
  margin-bottom: 10px;
}
.thinking-header {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--ai-thinking-text);
  font-size: 13px;
}
.thinking-icon { animation: spin 2s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.thinking-chevron {
  margin-left: auto;
  transition: transform 0.2s;
}
.thinking-content {
  margin-top: 8px;
  font-size: 12px;
  color: var(--ai-text-secondary);
  white-space: pre-wrap;
  max-height: 250px;
  overflow-y: auto;
}

/* ==============================
   图片预览
   ============================== */
.image-preview {
  display: flex;
  gap: 8px;
  padding: 12px 24px;
  background: var(--ai-bg-elevated);
  border-top: 1px solid var(--ai-border);
}
.preview-item {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: var(--ai-radius-sm);
  overflow: hidden;
  border: 1px solid var(--ai-border);
}
.preview-item img { width: 100%; height: 100%; object-fit: cover; }
.preview-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0,0,0,0.6);
  border: none;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity var(--ai-transition);
}
.preview-item:hover .preview-remove { opacity: 1; }

/* ==============================
   输入区域
   ============================== */
.input-area {
  padding: 12px 24px 20px;
}
.input-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.model-select {
  background: var(--ai-surface);
  border: 1px solid var(--ai-border);
  border-radius: var(--ai-radius-sm);
  color: var(--ai-text);
  padding: 6px 28px 6px 10px;
  font-size: 13px;
  cursor: pointer;
  outline: none;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1L5 5L9 1' stroke='%23666' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  font-family: inherit;
  transition: border-color var(--ai-transition);
}
.model-select:hover { border-color: var(--ai-border-hover); }
.model-select option { background: var(--ai-bg-elevated); color: var(--ai-text); }

/* 思考模式 toggle */
.thinking-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}
.thinking-checkbox {
  display: none;
}
.thinking-slider {
  width: 32px;
  height: 18px;
  background: var(--ai-surface-hover);
  border: 1px solid var(--ai-border);
  border-radius: 12px;
  position: relative;
  transition: all var(--ai-transition);
}
.thinking-slider::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--ai-text-tertiary);
  transition: all var(--ai-transition);
}
.thinking-checkbox:checked + .thinking-slider {
  background: rgba(99,102,241,0.2);
  border-color: var(--ai-accent);
}
.thinking-checkbox:checked + .thinking-slider::after {
  transform: translateX(14px);
  background: var(--ai-accent);
}
.thinking-label {
  font-size: 13px;
  color: var(--ai-text-secondary);
}

/* 输入框 */
.input-box {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  background: var(--ai-surface);
  border: 1px solid var(--ai-border);
  border-radius: var(--ai-radius-lg);
  padding: 8px 12px;
  transition: border-color var(--ai-transition), box-shadow var(--ai-transition);
}
.input-box:focus-within {
  border-color: var(--ai-border-focus);
  box-shadow: 0 0 0 3px var(--ai-accent-glow);
}
.chat-input {
  flex: 1;
  background: transparent;
  border: none;
  color: var(--ai-text);
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  outline: none;
  min-height: 24px;
  max-height: 120px;
  padding: 4px 0;
  font-family: inherit;
}
.chat-input::placeholder {
  color: var(--ai-text-tertiary);
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--ai-radius-sm);
  border: none;
  background: transparent;
  color: var(--ai-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--ai-transition);
  flex-shrink: 0;
}
.icon-btn:hover:not(:disabled) {
  background: var(--ai-surface-hover);
  color: var(--ai-text);
}
.icon-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/* 发送按钮 */
.send-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--ai-radius-sm);
  border: none;
  background: var(--ai-accent-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--ai-transition);
  flex-shrink: 0;
}
.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: var(--ai-shadow-glow);
}
.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.stop-btn {
  background: rgba(239,68,68,0.2);
  border: none;
  color: #ef4444;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  border-radius: inherit;
  cursor: pointer;
}
.stop-btn:hover {
  background: rgba(239,68,68,0.3);
}

/* ==============================
   灯箱
   ============================== */
.lightbox {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: lb-in 0.2s ease-out;
}
@keyframes lb-in {
  from { opacity: 0; }
  to { opacity: 1; }
}
.lightbox-close {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background var(--ai-transition);
}
.lightbox-close:hover {
  background: rgba(255, 255, 255, 0.2);
}
.lightbox-img {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 8px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.5);
  animation: lb-zoom 0.25s ease-out;
}
@keyframes lb-zoom {
  from { transform: scale(0.9); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
</style>
