<template>
  <div class="blog-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-header-row">
        <h1><el-icon :size="24" style="vertical-align:middle;margin-right:8px"><Notebook /></el-icon>博客</h1>
        <div>
          <el-button icon="Refresh" @click="fetchPosts(activeTag)" :loading="loading">刷新</el-button>
          <el-button type="primary" icon="Edit" @click="$router.push('/blog/new')">写文章</el-button>
        </div>
      </div>
      <p class="subtitle" v-if="!loading">共 {{ posts.length }} 篇文章</p>
    </div>

    <!-- 搜索框 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索文章..."
        clearable
        :prefix-icon="Search"
        @input="onSearchInput"
        @clear="searchKeyword = ''; fetchPosts(activeTag.value)"
      />
    </div>

    <!-- 标签筛选（从 /api/tags 获取） -->
    <div class="tag-filter" v-if="allTags.length">
      <el-tag
        :type="activeTag === '' ? 'primary' : 'info'"
        effect="plain"
        class="filter-tag"
        @click="selectTag('')"
      >
        全部
      </el-tag>
      <el-tag
        v-for="tag in allTags"
        :key="tag.slug"
        :type="activeTag === tag.slug ? 'primary' : 'info'"
        effect="plain"
        class="filter-tag"
        @click="selectTag(tag.slug)"
      >
        {{ tag.name }}
      </el-tag>
    </div>

    <!-- 加载中 — 骨架屏 -->
    <el-row :gutter="20" v-if="loading">
      <el-col v-for="i in 6" :key="i" :xs="24" :sm="12" :lg="8" class="post-col">
        <SkeletonCard />
      </el-col>
    </el-row>

    <!-- 文章列表 -->
    <el-row :gutter="20" v-else-if="posts.length">
      <el-col
        v-for="post in posts"
        :key="post.slug"
        :xs="24"
        :sm="12"
        :lg="8"
        class="post-col"
      >
        <BlogCard v-bind="post" :is-bookmarked="isBookmarked(post.slug)" />
      </el-col>
    </el-row>

    <!-- 空状态 -->
    <el-empty v-else :description="error || '暂无文章'" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onActivated } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { usePosts } from '@/composables/usePosts'
import { useBookmarks } from '@/composables/useInteraction'
import { tagApi } from '@/api/tagApi'
import BlogCard from '@/components/BlogCard.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'

const route = useRoute()
const { posts, loading, error, fetchPosts } = usePosts()
const { isBookmarked } = useBookmarks()
const allTags = ref([])
const activeTag = ref('')
const searchKeyword = ref('')
let searchTimer = null

/** 选中标签 */
function selectTag(slug) {
  activeTag.value = slug
  fetchPosts(slug || undefined, searchKeyword.value || undefined)
}

/** 搜索输入防抖 */
function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    fetchPosts(activeTag.value || undefined, searchKeyword.value || undefined)
  }, 300)
}

onMounted(async () => {
  // 载入全部标签用于筛选栏
  try {
    const res = await tagApi.list()
    allTags.value = Array.isArray(res) ? res : []
  } catch {
    allTags.value = []
  }

  // 从路由参数读取初始筛选条件（支持 /tags/:slug 进入）
  const tagSlug = route.query.tag || ''
  if (tagSlug) {
    activeTag.value = tagSlug
    fetchPosts(tagSlug, searchKeyword.value || undefined)
  } else {
    fetchPosts()
  }
})

// keep-alive 恢复时刷新数据
onActivated(() => {
  const tagSlug = route.query.tag || ''
  activeTag.value = tagSlug
  fetchPosts(tagSlug || undefined, searchKeyword.value || undefined)
})

// 监听路由查询参数变化（如从标签云跳转）
watch(() => route.query.tag, (newTag) => {
  const slug = newTag || ''
  if (slug !== activeTag.value) {
    activeTag.value = slug
    fetchPosts(slug || undefined, searchKeyword.value || undefined)
  }
})
</script>

<style scoped>
.blog-list {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.search-bar {
  margin-bottom: 16px;
}

.tag-filter {
  margin-bottom: 20px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-tag {
  cursor: pointer;
}

.post-col {
  margin-bottom: 20px;
}
</style>
