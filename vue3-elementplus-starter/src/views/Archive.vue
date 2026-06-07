<template>
  <div class="archive-page">
    <div class="page-header">
      <h1><el-icon :size="24" style="vertical-align:middle;margin-right:8px"><Timer /></el-icon>文章归档</h1>
      <p class="subtitle" v-if="!loading">共 {{ totalPosts }} 篇文章</p>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="5" animated />
    </div>

    <el-empty v-else-if="archive.length === 0" description="暂无文章" :image-size="80" />

    <div v-else class="archive-timeline">
      <div v-for="group in archive" :key="group.month" class="archive-group">
        <div class="month-header">
          <el-tag size="large" effect="dark" class="month-tag">
            {{ group.month }}
          </el-tag>
          <span class="month-count">{{ group.posts.length }} 篇</span>
        </div>
        <div
          v-for="post in group.posts"
          :key="post.slug"
          class="archive-item"
          @click="$router.push('/blog/' + post.slug)"
        >
          <span class="item-date">{{ post.date }}</span>
          <span class="item-title">{{ post.title }}</span>
          <el-icon class="item-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Timer, ArrowRight } from '@element-plus/icons-vue'
import request from '@/api/request'

const archive = ref([])
const loading = ref(false)

const totalPosts = computed(() => archive.value.reduce((s, g) => s + g.posts.length, 0))

onMounted(async () => {
  loading.value = true
  try {
    const res = await request.get('/posts/archive')
    archive.value = Array.isArray(res) ? res : []
  } catch {
    archive.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.archive-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 24px;
  color: var(--color-text);
  margin: 0;
}

.subtitle {
  color: var(--color-text-tertiary);
  font-size: 14px;
  margin: 8px 0 0;
}

.loading-state {
  padding: 60px 0;
}

.archive-group {
  margin-bottom: 32px;
}

.month-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.month-count {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.archive-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  border-left: 3px solid transparent;
}

.archive-item:hover {
  background: var(--color-bg-warm);
  border-left-color: var(--color-accent);
}

.item-date {
  font-size: 13px;
  color: var(--color-text-tertiary);
  min-width: 80px;
  flex-shrink: 0;
}

.month-tag {
  background-color: var(--color-accent) !important;
  border-color: var(--color-accent) !important;
  color: #fff !important;
  font-family: var(--font-display);
  font-weight: 600;
}

.item-title {
  flex: 1;
  font-size: 15px;
  color: var(--color-text);
  font-weight: 500;
}

.item-arrow {
  color: var(--color-text-placeholder);
  flex-shrink: 0;
}

.archive-item:hover .item-arrow {
  color: var(--color-accent);
}
</style>
