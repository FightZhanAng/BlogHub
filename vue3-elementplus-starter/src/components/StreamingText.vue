<template>
  <span class="streaming-text" v-html="renderedContent"></span>
</template>

<script setup>
import { computed } from 'vue'
import { useMarkdown } from '@/composables/useMarkdown'

const props = defineProps({
  content: { type: String, default: '' }
})

const { renderMarkdown } = useMarkdown()

const renderedContent = computed(() => {
  if (!props.content) return '<span class="cursor">▌</span>'
  return renderMarkdown(props.content) + '<span class="cursor">▌</span>'
})
</script>

<style scoped>
.streaming-text :deep(.cursor) {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: var(--el-color-primary);
  margin-left: 2px;
  animation: blink 1s infinite;
  vertical-align: text-bottom;
}
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}
</style>
