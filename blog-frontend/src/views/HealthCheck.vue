<template>
  <div class="health-page">
    <div class="health-card">
      <div class="card-header">
        <div class="header-icon" :class="overallStatus">
          <el-icon :size="24"><component :is="overallStatus === 'UP' ? 'CircleCheckFilled' : 'CircleCloseFilled'" /></el-icon>
        </div>
        <div class="header-text">
          <h2>系统健康检查</h2>
          <p class="subtitle">{{ overallStatus === 'UP' ? '所有服务运行正常' : '部分服务异常' }}</p>
        </div>
        <div class="status-badge" :class="overallStatus">
          {{ overallStatus === 'UP' ? '运行中' : '异常' }}
        </div>
      </div>

      <div class="checks-grid">
        <div v-for="(status, name) in filteredChecks" :key="name" class="service-card" :class="status === 'UP' ? 'up' : 'down'">
          <div class="service-icon">
            <el-icon :size="20"><component :is="getServiceIcon(name)" /></el-icon>
          </div>
          <div class="service-info">
            <span class="service-name">{{ serviceName(name) }}</span>
            <span class="service-status">{{ status === 'UP' ? '运行正常' : '连接失败' }}</span>
          </div>
          <div class="status-dot" :class="status === 'UP' ? 'up' : 'down'"></div>
        </div>
      </div>

      <div class="card-footer">
        <span class="update-time">上次检查: {{ lastCheck }}</span>
        <el-button type="primary" :icon="Refresh" @click="fetchHealth" :loading="loading">
          刷新状态
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh, CircleCheckFilled, CircleCloseFilled, Coin, Connection, Timer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const checks = ref({})
const overallStatus = ref('UP')
const loading = ref(false)
const lastCheck = ref('--')

const filteredChecks = computed(() => {
  const result = {}
  for (const [key, val] of Object.entries(checks.value)) {
    if (key !== 'status') result[key] = val
  }
  return result
})

function serviceName(name) {
  const map = {
    mysql: 'MySQL 数据库',
    redis: 'Redis 缓存',
    'xxl-job': 'XXL-Job 定时任务'
  }
  return map[name] || name
}

function getServiceIcon(name) {
  const map = {
    mysql: 'Coin',
    redis: 'Connection',
    'xxl-job': 'Timer'
  }
  return map[name] || 'CircleCheck'
}

async function fetchHealth() {
  loading.value = true
  try {
    const res = await request.get('/health')
    checks.value = res
    overallStatus.value = res.status || 'UP'
    lastCheck.value = new Date().toLocaleTimeString()
  } catch {
    checks.value = { status: 'DOWN' }
    overallStatus.value = 'DOWN'
    lastCheck.value = new Date().toLocaleTimeString()
    ElMessage.error('健康检查请求失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchHealth)
</script>

<style scoped>
.health-page {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: var(--space-xl) var(--space-md);
  min-height: 80vh;
}

.health-card {
  background: var(--color-card);
  border-radius: var(--radius-xl);
  padding: var(--space-xl);
  width: 100%;
  max-width: 560px;
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-icon.UP {
  background: rgba(90, 138, 90, 0.1);
  color: var(--color-success);
}

.header-icon.DOWN {
  background: rgba(184, 84, 80, 0.1);
  color: var(--color-danger);
}

.header-text {
  flex: 1;
}

.header-text h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
}

.subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.status-badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.status-badge.UP {
  background: rgba(90, 138, 90, 0.1);
  color: var(--color-success);
}

.status-badge.DOWN {
  background: rgba(184, 84, 80, 0.1);
  color: var(--color-danger);
}

.checks-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.service-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg);
  border-radius: var(--radius-lg);
  transition: all var(--duration) var(--ease);
  border: 1px solid transparent;
}

.service-card.up {
  background: var(--color-bg);
}

.service-card.up:hover {
  background: var(--color-bg-warm);
  border-color: var(--color-border-light);
}

.service-card.down {
  background: rgba(184, 84, 80, 0.08);
  border-color: rgba(184, 84, 80, 0.2);
}

.service-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--color-card);
  border: 1px solid var(--color-border);
}

.service-card.up .service-icon {
  color: var(--color-success);
}

.service-card.down .service-icon {
  color: var(--color-danger);
}

.service-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.service-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
}

.service-status {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.up {
  background: var(--color-success);
  box-shadow: 0 0 8px rgba(90, 138, 90, 0.4);
}

.status-dot.down {
  background: var(--color-danger);
  box-shadow: 0 0 8px rgba(184, 84, 80, 0.4);
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-xl);
  padding-top: var(--space-lg);
  border-top: 1px solid var(--color-border-light);
}

.update-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* 深色主题适配 */
[data-theme="dark"] .service-card.down {
  background: rgba(212, 106, 106, 0.12);
  border-color: rgba(212, 106, 106, 0.25);
}

[data-theme="dark"] .header-icon.DOWN {
  background: rgba(212, 106, 106, 0.15);
}

[data-theme="dark"] .status-badge.DOWN {
  background: rgba(212, 106, 106, 0.15);
}

[data-theme="dark"] .service-card.down .service-name {
  color: var(--color-danger);
}

[data-theme="dark"] .service-card.down .service-status {
  color: var(--color-danger);
  opacity: 0.8;
}

[data-theme="dark"] .status-dot.down {
  box-shadow: 0 0 10px rgba(212, 106, 106, 0.5);
}
</style>
