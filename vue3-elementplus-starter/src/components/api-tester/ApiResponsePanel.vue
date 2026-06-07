<template>
  <div class="response-panel">
    <!-- 状态栏 -->
    <div v-if="response" class="response-status">
      <span class="status-code" :class="statusClass(response.statusCode)">
        {{ response.statusCode || '错误' }}
      </span>
      <span class="status-time">{{ response.time }}ms</span>
      <span class="status-size">{{ formatSize(response.size) }}</span>
    </div>
    <div v-else class="response-empty">
      点击"发送"查看响应结果
    </div>

    <!-- 响应内容 -->
    <div v-if="response" class="response-body">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="Body" name="body">
          <div class="json-viewer">
            <pre class="json-content">{{ formattedBody }}</pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="Headers" name="headers">
          <div v-if="response.headers && Object.keys(response.headers).length" class="headers-table">
            <div v-for="(value, key) in response.headers" :key="key" class="header-row">
              <span class="header-key">{{ key }}</span>
              <span class="header-value">{{ value }}</span>
            </div>
          </div>
          <div v-else class="empty-hint">无响应头</div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  response: { type: Object, default: null }
})

const activeTab = ref('body')

const formattedBody = computed(() => {
  if (!props.response?.body) return ''
  try {
    const parsed = JSON.parse(props.response.body)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return props.response.body
  }
})

function statusClass(code) {
  if (!code) return ''
  if (code >= 200 && code < 300) return 'status-ok'
  if (code >= 300 && code < 400) return 'status-redirect'
  if (code >= 400 && code < 500) return 'status-warn'
  if (code >= 500) return 'status-error'
  return 'status-error'
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}
</script>

<style scoped>
.response-panel { display: flex; flex-direction: column; height: 100%; }
.response-status { display: flex; gap: 12px; align-items: center; padding: 8px 12px; background: var(--color-bg-warm); border-radius: 4px; }
.status-code { font-weight: 600; }
.status-ok { color: #67c23a; }
.status-redirect { color: var(--color-accent); }
.status-warn { color: #e6a23c; }
.status-error { color: #f56c6c; }
.status-time { color: var(--color-text-secondary); }
.status-size { color: var(--color-text-tertiary); }
.response-empty { padding: 20px; text-align: center; color: var(--color-text-tertiary); }
.response-body { flex: 1; overflow: auto; }
.json-viewer { height: 100%; overflow: auto; }
.json-content { margin: 0; padding: 12px; font-family: 'Consolas', 'Monaco', monospace; font-size: 13px; white-space: pre-wrap; word-break: break-all; background: var(--color-bg-warm); border-radius: 4px; }
.headers-table { display: flex; flex-direction: column; }
.header-row { display: flex; padding: 6px 0; border-bottom: 1px solid var(--color-border-light); }
.header-key { font-weight: 600; width: 200px; color: var(--color-text); }
.header-value { flex: 1; color: var(--color-text-secondary); word-break: break-all; }
.empty-hint { padding: 20px; text-align: center; color: var(--color-text-tertiary); }
</style>
