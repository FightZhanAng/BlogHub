<template>
  <div v-if="error" class="error-boundary">
    <div class="error-content">
      <div class="error-icon">⚠️</div>
      <h3>页面出错了</h3>
      <p class="error-message">{{ error.message || '未知错误' }}</p>
      <div class="error-actions">
        <button class="btn-retry" @click="retry">重试</button>
        <button class="btn-home" @click="goHome">返回首页</button>
      </div>
    </div>
  </div>
  <slot v-else />
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'
import { useRouter } from 'vue-router'

const error = ref(null)
const router = useRouter()

onErrorCaptured((err) => {
  error.value = err
  return false // 阻止错误继续向上传播
})

function retry() {
  error.value = null
}

function goHome() {
  error.value = null
  router.push('/')
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 40px 20px;
}

.error-content {
  text-align: center;
  max-width: 400px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-content h3 {
  font-size: 18px;
  color: var(--color-text);
  margin-bottom: 8px;
}

.error-message {
  color: var(--color-text-tertiary);
  font-size: 14px;
  margin-bottom: 24px;
  word-break: break-all;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.btn-retry,
.btn-home {
  padding: 8px 24px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-retry {
  background: var(--color-accent);
  color: white;
}

.btn-retry:hover {
  background: #66b1ff;
}

.btn-home {
  background: var(--color-bg-warm);
  color: var(--color-text-secondary);
}

.btn-home:hover {
  background: var(--color-border);
}
</style>
