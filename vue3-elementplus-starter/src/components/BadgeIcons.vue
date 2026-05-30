<template>
  <span class="badge-icons" v-if="badges.length">
    <el-tooltip
      v-for="badge in displayedBadges"
      :key="badge.id"
      :content="badge.name + '：' + badge.description"
      placement="top"
    >
      <span class="badge-icon">{{ badge.icon }}</span>
    </el-tooltip>
  </span>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { badgeApi } from '@/api/badgeApi'

const props = defineProps({
  userId: { type: [Number, String], required: true },
  max: { type: Number, default: 3 }
})

const badges = ref([])

const displayedBadges = computed(() => {
  return badges.value
    .filter(b => b.earned)
    .slice(0, props.max)
})

async function fetchBadges() {
  if (!props.userId) return
  try {
    const res = await badgeApi.getUserBadges(props.userId)
    badges.value = res || []
  } catch (e) {
    console.error('获取徽章失败', e)
  }
}

onMounted(fetchBadges)
</script>

<style scoped>
.badge-icons {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: 4px;
}

.badge-icon {
  font-size: 14px;
  cursor: default;
}
</style>
