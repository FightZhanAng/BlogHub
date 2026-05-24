<template>
  <div class="tag-cloud-page">
    <div class="page-header">
      <h1><el-icon :size="24" style="vertical-align:middle;margin-right:8px"><CollectionTag /></el-icon>标签云</h1>
      <p class="subtitle">共 {{ tags.length }} 个标签</p>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="tags.length === 0" class="empty-state">
      <el-empty description="暂无标签" :image-size="80" />
    </div>

    <div v-else class="tag-cloud">
      <span
        v-for="tag in tags"
        :key="tag.id"
        class="cloud-tag"
        :style="{ fontSize: tagFontSize(tag.postCount) + 'px' }"
        @click="$router.push('/tags/' + tag.slug)"
      >
        {{ tag.name }}
        <sup class="tag-badge">{{ tag.postCount }}</sup>
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { tagApi } from '@/api/tagApi'
import { CollectionTag } from '@element-plus/icons-vue'

const tags = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await tagApi.list()
    tags.value = Array.isArray(res) ? res : []
  } catch {
    tags.value = []
  } finally {
    loading.value = false
  }
})

/** 按文章数比例计算字号（12px ~ 36px） */
function tagFontSize(count) {
  if (tags.value.length === 0) return 14
  const counts = tags.value.map((t) => t.postCount || 0)
  const max = Math.max(...counts, 1)
  const min = Math.min(...counts, 0)
  if (max === min) return 18
  // 在 12-36 范围内映射
  return 12 + ((count - min) / (max - min)) * 24
}
</script>

<style scoped>
.tag-cloud-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 24px;
  color: #303133;
  margin: 0;
}

.subtitle {
  color: #909399;
  font-size: 14px;
  margin: 8px 0 0;
}

.loading-state,
.empty-state {
  padding: 60px 0;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
  align-items: center;
  justify-content: center;
  padding: 32px 0;
  min-height: 300px;
}

.cloud-tag {
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
  color: #409eff;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 6px;
  transition: all 0.2s ease;
  line-height: 1.4;
}

.cloud-tag:hover {
  background: #ecf5ff;
  color: #2a6bb0;
  transform: scale(1.05);
}

.tag-badge {
  font-size: 11px;
  color: #909399;
  font-weight: normal;
}
</style>
