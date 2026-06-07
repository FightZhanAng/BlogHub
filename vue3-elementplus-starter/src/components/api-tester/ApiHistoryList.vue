<template>
  <div class="history-list">
    <div class="history-header">
      <span>最近请求</span>
      <el-button size="small" type="danger" text @click="$emit('clearHistory')">清空</el-button>
    </div>
    <div v-if="history.length === 0" class="empty-hint">暂无历史记录</div>
    <div
      v-for="item in history"
      :key="item.id"
      class="history-item"
      @click="$emit('loadHistory', item)"
    >
      <span class="hist-method" :class="item.method?.toLowerCase()">{{ item.method }}</span>
      <span class="hist-url">{{ shortenUrl(item.url) }}</span>
      <span class="hist-status" :class="statusClass(item.statusCode)">{{ item.statusCode }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  history: { type: Array, default: () => [] }
})

defineEmits(['loadHistory', 'clearHistory'])

function shortenUrl(url) {
  if (!url) return ''
  try {
    const u = new URL(url)
    return u.pathname + u.search
  } catch {
    return url.length > 40 ? url.substring(0, 40) + '...' : url
  }
}

function statusClass(code) {
  if (!code) return ''
  if (code >= 200 && code < 300) return 'status-ok'
  if (code >= 400 && code < 500) return 'status-warn'
  if (code >= 500) return 'status-error'
  return ''
}
</script>

<style scoped>
.history-header { display: flex; justify-content: space-between; align-items: center; padding: 8px; font-weight: 600; }
.history-item { display: flex; align-items: center; gap: 6px; padding: 6px 8px; cursor: pointer; font-size: 12px; }
.history-item:hover { background: var(--color-bg-warm); }
.hist-method { font-size: 10px; font-weight: 600; padding: 1px 4px; border-radius: 3px; }
.hist-method.get { color: #67c23a; }
.hist-method.post { color: var(--color-accent); }
.hist-method.put { color: #e6a23c; }
.hist-method.delete { color: #f56c6c; }
.hist-url { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--color-text-secondary); }
.hist-status { font-size: 11px; }
.status-ok { color: #67c23a; }
.status-warn { color: #e6a23c; }
.status-error { color: #f56c6c; }
.empty-hint { padding: 8px; color: var(--color-text-tertiary); font-size: 12px; text-align: center; }
</style>
