<template>
  <div class="user-badges">
    <h2 class="page-title">我的徽章</h2>

    <div v-for="category in categories" :key="category.key" class="badge-section">
      <h3 class="section-title">{{ category.label }}</h3>
      <div class="badge-grid">
        <div
          v-for="badge in getBadgesByCategory(category.key)"
          :key="badge.id"
          class="badge-card"
          :class="{ earned: badge.earned }"
        >
          <div class="badge-icon">{{ badge.icon }}</div>
          <div class="badge-name">{{ badge.name }}</div>
          <div class="badge-desc">{{ badge.description }}</div>
          <div class="badge-status">
            <el-tag v-if="badge.earned" type="success" size="small">已获得</el-tag>
            <el-tag v-else type="info" size="small">未解锁</el-tag>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!badges.length" description="暂无徽章数据" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { badgeApi } from '@/api/badgeApi'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const badges = ref([])

const categories = [
  { key: 'interaction', label: '🌟 互动类' },
  { key: 'content', label: '✍️ 内容类' },
  { key: 'activity', label: '🔗 活跃类' },
  { key: 'special', label: '👑 特殊类' },
]

function getBadgesByCategory(category) {
  return badges.value.filter(b => b.category === category)
}

async function fetchBadges() {
  const userId = authStore.userId
  if (!userId) return
  try {
    const res = await badgeApi.getUserBadges(userId)
    badges.value = res || []
  } catch (e) {
    console.error('获取徽章失败', e)
  }
}

onMounted(fetchBadges)
</script>

<style scoped>
.user-badges {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 24px;
  color: var(--color-text);
}

.badge-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 16px;
  color: var(--color-text-secondary);
}

.badge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.badge-card {
  background: var(--color-bg-warm);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  transition: all 0.3s;
  opacity: 0.6;
}

.badge-card.earned {
  background: linear-gradient(135deg, #fff9e6, #fff3cc);
  opacity: 1;
  box-shadow: 0 2px 12px rgba(255, 193, 7, 0.15);
}

[data-theme="dark"] .badge-card.earned {
  background: linear-gradient(135deg, rgba(201, 169, 110, 0.2), rgba(201, 169, 110, 0.1));
  box-shadow: 0 2px 12px rgba(201, 169, 110, 0.1);
}

[data-theme="dark"] .badge-card.earned .badge-name {
  color: var(--color-accent);
}

.badge-card:hover {
  transform: translateY(-2px);
}

.badge-icon {
  font-size: 36px;
  margin-bottom: 8px;
}

.badge-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 4px;
}

.badge-desc {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-bottom: 8px;
}
</style>
