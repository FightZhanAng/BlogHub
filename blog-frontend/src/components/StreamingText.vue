<template>
  <div class="streaming-text" v-html="renderedContent"></div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  content: { type: String, default: '' }
})

const renderedContent = computed(() => {
  if (!props.content) return '<span class="cursor"></span>'
  const escaped = props.content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
  return escaped + '<span class="cursor"></span>'
})
</script>

<style scoped>
.streaming-text {
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  user-select: text;
  cursor: text;
  color: var(--ai-text);
}
.streaming-text :deep(.cursor) {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: linear-gradient(180deg, #818cf8, #c084fc);
  margin-left: 1px;
  vertical-align: text-bottom;
  animation: cursor-blink 1.2s step-end infinite;
  border-radius: 1px;
}
@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
