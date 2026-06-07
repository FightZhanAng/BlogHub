<template>
  <div :class="['chat-message', message.role]">
    <!-- AI 头像 -->
    <div v-if="message.role === 'assistant'" class="message-avatar">
      <div class="ai-avatar">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M12 2a7 7 0 0 1 7 7c0 2.5-1.5 4.5-3 6v2a1 1 0 0 1-1 1h-4a1 1 0 0 1-1-1v-2c-1.5-1.5-3-3.5-3-6a7 7 0 0 1 7-7z"/>
          <line x1="9" y1="21" x2="15" y2="21"/>
        </svg>
      </div>
    </div>
    <!-- 用户头像 -->
    <div v-else class="message-avatar">
      <div class="user-avatar">
        <img v-if="authStore.avatar" :src="avatarUrl" class="avatar-img" />
        <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
      </div>
    </div>

    <div class="message-body">
      <div class="message-role">{{ message.role === 'user' ? '你' : 'MiMo' }}</div>

      <!-- 思考过程 -->
      <div v-if="message.thinkingContent && showThinking" class="thinking-block">
        <div class="thinking-header" @click="expandedThinking = !expandedThinking">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
          <span>思考过程</span>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :style="{ transform: expandedThinking ? 'rotate(180deg)' : '' }" class="expand-icon"><polyline points="6 9 12 15 18 9"/></svg>
        </div>
        <div v-show="expandedThinking" class="thinking-content">{{ message.thinkingContent }}</div>
      </div>

      <!-- 消息内容 -->
      <div class="message-content" v-html="renderedContent"></div>

      <!-- 时间 + 操作 -->
      <div class="message-footer">
        <span class="message-time">{{ formatTime(message.createdAt || message.timestamp) }}</span>
        <div class="message-actions">
          <button class="action-btn" @click="copyContent" :title="copied ? '已复制' : '复制'">
            <svg v-if="!copied" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg>
            <svg v-else width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
            <span>{{ copied ? '已复制' : '复制' }}</span>
          </button>
        </div>
      </div>

      <!-- 图片 -->
      <div v-if="message.images?.urls?.length" class="message-images">
        <img v-for="(url, i) in message.images.urls" :key="i" :src="url" @click="$emit('preview', url)" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useMarkdown } from '@/composables/useMarkdown'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const avatarUrl = computed(() => {
  if (!authStore.avatar) return ''
  // 头像路径如 /uploads/avatars/xxx.jpg，通过 Vite proxy 或 nginx 转发到后端
  return authStore.avatar
})

const props = defineProps({
  message: { type: Object, required: true },
  showThinking: { type: Boolean, default: false }
})

defineEmits(['preview'])

const expandedThinking = ref(false)
const copied = ref(false)
const { renderMarkdown } = useMarkdown()

const renderedContent = computed(() => {
  if (!props.message.content) return ''
  return renderMarkdown(props.message.content)
})

function copyContent() {
  if (!props.message.content) return
  navigator.clipboard.writeText(props.message.content).then(() => {
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  })
}

function formatTime(ts) {
  if (!ts) return ''
  const date = new Date(ts)
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`
  if (date.toDateString() === now.toDateString()) return time
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) return '昨天 ' + time
  return `${date.getMonth() + 1}/${date.getDate()} ${time}`
}
</script>

<style scoped>
/* ==============================
   消息布局
   ============================== */
.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: msg-in 0.35s ease-out both;
}
.chat-message.user {
  flex-direction: row-reverse;
}
@keyframes msg-in {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
.message-body {
  max-width: 72%;
}
.message-role {
  font-size: 12px;
  font-weight: 500;
  color: var(--ai-text-tertiary);
  margin-bottom: 6px;
}
.chat-message.user .message-role {
  text-align: right;
}

/* ==============================
   头像
   ============================== */
.message-avatar { flex-shrink: 0; }
.ai-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--ai-accent-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(255,255,255,0.08);
  border: 1px solid var(--ai-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ai-text-secondary);
  overflow: hidden;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ==============================
   消息气泡
   ============================== */
.message-content {
  background: var(--ai-surface);
  border: 1px solid var(--ai-border);
  border-radius: 2px var(--ai-radius-lg) var(--ai-radius-lg) var(--ai-radius-lg);
  padding: 10px 14px;
  line-height: 1.6;
  font-size: 13px;
  word-break: break-word;
  user-select: text;
  cursor: text;
}
.chat-message.user .message-content {
  background: var(--ai-user-bg);
  border-color: var(--ai-user-border);
  border-radius: var(--ai-radius-lg) 2px var(--ai-radius-lg) var(--ai-radius-lg);
  color: var(--ai-text);
}

/* ==============================
   Markdown 渲染样式
   ============================== */
.message-content :deep(p) {
  margin: 0 0 8px 0;
  color: var(--ai-text);
}
.message-content :deep(p:last-child) {
  margin-bottom: 0;
}
.message-content :deep(pre) {
  background: var(--ai-code-bg);
  color: #c9d1d9;
  padding: 12px 14px;
  border-radius: 0 0 var(--ai-radius-sm) var(--ai-radius-sm);
  overflow-x: auto;
  margin: 0 0 8px 0;
  font-size: 12px;
  line-height: 1.5;
  border: 1px solid var(--ai-code-border);
}
.message-content :deep(.code-block) {
  margin: 8px 0;
  border-radius: var(--ai-radius-sm);
  overflow: hidden;
  border: 1px solid var(--ai-code-border);
}
.message-content :deep(.code-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  background: var(--ai-code-header);
  border-bottom: 1px solid var(--ai-code-border);
  font-size: 12px;
}
.message-content :deep(.code-lang) {
  color: var(--ai-text-tertiary);
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}
.message-content :deep(.code-actions) {
  display: flex;
  gap: 6px;
}
.message-content :deep(.code-copy-btn),
.message-content :deep(.code-collapse-btn) {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: var(--ai-text-tertiary);
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-family: inherit;
  transition: all var(--ai-transition);
}
.message-content :deep(.code-block.collapsed pre) {
  display: none;
}
.message-content :deep(.code-line-count) {
  font-size: 11px;
  color: var(--ai-text-tertiary);
  margin-left: 8px;
}
.message-content :deep(.code-copy-btn:hover),
.message-content :deep(.code-collapse-btn:hover) {
  color: var(--ai-text);
  background: var(--ai-surface-hover);
}
.message-content :deep(code) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 0.85em;
}
.message-content :deep(:not(pre) > code) {
  background: rgba(99,102,241,0.1);
  padding: 2px 6px;
  border-radius: 4px;
  color: #c084fc;
  border: 1px solid rgba(99,102,241,0.15);
}
.message-content :deep(ul), .message-content :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
  color: var(--ai-text);
}
.message-content :deep(li) {
  margin: 4px 0;
}
.message-content :deep(blockquote) {
  border-left: 3px solid var(--ai-accent);
  padding: 8px 14px;
  margin: 8px 0;
  background: var(--ai-accent-glow);
  border-radius: 0 var(--ai-radius-sm) var(--ai-radius-sm) 0;
  color: var(--ai-text-secondary);
}
.message-content :deep(h1), .message-content :deep(h2), .message-content :deep(h3) {
  margin: 12px 0 8px 0;
  font-weight: 600;
  color: var(--ai-text);
}
.message-content :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}
.message-content :deep(th), .message-content :deep(td) {
  border: 1px solid var(--ai-border);
  padding: 6px 12px;
  font-size: 12px;
}
.message-content :deep(th) {
  background: var(--ai-surface-hover);
  font-weight: 600;
}
.message-content :deep(a) {
  color: var(--ai-accent-hover);
  text-decoration: none;
}
.message-content :deep(a:hover) {
  text-decoration: underline;
}

/* ==============================
   思考块
   ============================== */
.thinking-block {
  background: var(--ai-thinking-bg);
  border: 1px solid var(--ai-thinking-border);
  border-radius: var(--ai-radius-sm);
  padding: 8px 12px;
  margin-bottom: 8px;
}
.thinking-header {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--ai-thinking-text);
  font-size: 13px;
}
.expand-icon {
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
   操作栏
   ============================== */
.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}
.message-actions {
  display: flex;
  opacity: 0;
  transition: opacity var(--ai-transition);
}
.chat-message:hover .message-actions {
  opacity: 1;
}
.message-time {
  font-size: 11px;
  color: var(--ai-text-tertiary);
}
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: var(--ai-text-tertiary);
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-family: inherit;
  transition: all var(--ai-transition);
}
.action-btn:hover {
  color: var(--ai-text);
  background: var(--ai-surface-hover);
}

/* ==============================
   图片
   ============================== */
.message-images {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.message-images img {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: var(--ai-radius-sm);
  border: 1px solid var(--ai-border);
  cursor: pointer;
  transition: opacity var(--ai-transition);
}
.message-images img:hover {
  opacity: 0.85;
}
</style>
