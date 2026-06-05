<template>
  <div :class="['chat-message', message.role]">
    <div class="message-avatar">
      <el-avatar v-if="message.role === 'user'" :size="36">
        <el-icon><User /></el-icon>
      </el-avatar>
      <el-avatar v-else :size="36" style="background: #409eff">
        <span style="font-size: 18px">🤖</span>
      </el-avatar>
    </div>
    <div class="message-body">
      <div class="message-role">{{ message.role === 'user' ? '你' : 'AI 助手' }}</div>
      <!-- 思考过程 -->
      <div v-if="message.thinkingContent && showThinking" class="thinking-block">
        <div class="thinking-header" @click="expandedThinking = !expandedThinking">
          <el-icon><Loading /></el-icon>
          <span>思考过程</span>
          <el-icon class="expand-icon">
            <ArrowDown v-if="!expandedThinking" />
            <ArrowUp v-else />
          </el-icon>
        </div>
        <div v-show="expandedThinking" class="thinking-content">{{ message.thinkingContent }}</div>
      </div>
      <!-- 消息内容 -->
      <div class="message-content" v-html="renderedContent"></div>
      <!-- 图片 -->
      <div v-if="message.images?.urls?.length" class="message-images">
        <img v-for="(url, i) in message.images.urls" :key="i" :src="url" @click="$emit('preview', url)" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { User, Loading, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { useMarkdown } from '@/composables/useMarkdown'

const props = defineProps({
  message: { type: Object, required: true },
  showThinking: { type: Boolean, default: false }
})

defineEmits(['preview'])

const expandedThinking = ref(false)
const { renderMarkdown } = useMarkdown()

const renderedContent = computed(() => {
  if (!props.message.content) return ''
  return renderMarkdown(props.message.content)
})
</script>

<style scoped>
.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.chat-message.user {
  flex-direction: row-reverse;
}
.message-body {
  max-width: 75%;
}
.message-role {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.chat-message.user .message-role {
  text-align: right;
}
.message-content {
  background: var(--el-fill-color-light);
  border-radius: 12px;
  padding: 12px 16px;
  line-height: 1.6;
  word-break: break-word;
}
.chat-message.user .message-content {
  background: var(--el-color-primary);
  color: #fff;
}
.thinking-block {
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 8px;
  font-size: 13px;
}
.thinking-header {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #e6a23c;
}
.expand-icon {
  margin-left: auto;
}
.thinking-content {
  margin-top: 8px;
  color: #909399;
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
}
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
  border-radius: 8px;
  cursor: pointer;
}
</style>
