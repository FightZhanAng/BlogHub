<template>
  <div class="bookmarks">
    <div class="page-header">
      <h1><el-icon :size="24" style="vertical-align:middle;margin-right:8px"><StarFilled /></el-icon>我的收藏</h1>
      <p class="subtitle" v-if="bookmarkedPosts.length">共 {{ bookmarkedPosts.length }} 篇文章</p>
    </div>

    <!-- 加载中 -->
    <div class="loading-container" v-if="loading">
      <el-skeleton :rows="3" animated />
    </div>

    <el-row :gutter="20" v-else-if="bookmarkedPosts.length">
      <el-col
        v-for="post in bookmarkedPosts"
        :key="post.slug"
        :xs="24"
        :sm="12"
        :lg="8"
        class="post-col"
      >
        <BlogCard
          v-bind="post"
          :is-bookmarked="true"
        />
      </el-col>
    </el-row>

    <el-empty v-else description="还没有收藏任何文章" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { usePosts } from '@/composables/usePosts'
import { useBookmarks } from '@/composables/useInteraction'
import BlogCard from '@/components/BlogCard.vue'

const { posts, fetchPosts } = usePosts()
const { bookmarksList } = useBookmarks()
const loading = ref(true)

const bookmarkedPosts = computed(() => {
  return posts.value.filter((p) => bookmarksList.value.includes(p.slug))
})

onMounted(async () => {
  await fetchPosts()
  loading.value = false
})
</script>

<style scoped>
.bookmarks {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
}

.subtitle {
  color: var(--color-text-tertiary);
  margin: 0;
  font-size: 14px;
}

.post-col {
  margin-bottom: 20px;
}

.loading-container {
  padding: 40px 0;
}
</style>
