<template>
  <div class="search-page">
    <div class="search-header">
      <el-input
        v-model="keyword"
        placeholder="搜索文章标题、内容、描述..."
        size="large"
        clearable
        :prefix-icon="Search"
        @keyup.enter="doSearch"
        @clear="clearSearch"
      >
        <template #append>
          <el-button @click="doSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <div v-if="searched" class="search-meta">
      搜索 <strong>"{{ query }}"</strong>，共 {{ total }} 条结果
    </div>

    <!-- 热门搜索词（未搜索时展示） -->
    <div v-if="!searched && hotKeywords.length" class="hot-keywords">
      <div class="hot-title">🔥 热门搜索</div>
      <div class="hot-list">
        <el-tag
          v-for="(kw, i) in hotKeywords"
          :key="kw"
          :type="i < 3 ? 'danger' : 'info'"
          effect="plain"
          class="hot-tag"
          @click="keyword = kw; doSearch()"
        >
          {{ kw }}
        </el-tag>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>

    <el-empty v-else-if="searched && !posts.length" description="未找到相关文章" :image-size="80" />

    <div v-else-if="posts.length" class="search-results">
      <el-card
        v-for="p in posts"
        :key="p.id"
        shadow="never"
        class="result-card"
        @click="$router.push('/blog/' + p.slug)"
      >
        <div class="result-title">{{ p.title }}</div>
        <div class="result-desc">{{ p.description || '暂无摘要' }}</div>
        <div class="result-meta">
          <span>{{ new Date(p.createdAt).toLocaleDateString() }}</span>
          <span>·</span>
          <span>{{ p.likes || 0 }} 赞</span>
          <span>·</span>
          <span>{{ p.views || 0 }} 阅读</span>
        </div>
      </el-card>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          :page-sizes="[10,20,50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const query = ref('')
const posts = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const searched = ref(false)
const hotKeywords = ref([])

// 手动清空输入框时也重置结果
watch(keyword, (val) => {
  if (!val.trim() && searched.value) {
    clearSearch()
  }
})

async function fetchHotKeywords() {
  try {
    const res = await request.get('/stats/search-keywords')
    hotKeywords.value = Array.isArray(res) ? res : []
  } catch { hotKeywords.value = [] }
}

async function doSearch() {
  const q = keyword.value.trim()
  if (!q) return
  query.value = q
  searched.value = true
  page.value = 1
  await fetchResults()
}

async function fetchResults() {
  loading.value = true
  try {
    const res = await request.get('/posts/search', {
      params: { q: query.value, page: page.value, size: size.value }
    })
    const data = res.records || res.list || []
    posts.value = Array.isArray(data) ? data : []
    total.value = res.total || res.count || 0
  } catch {
    posts.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  page.value = p
  fetchResults()
}

function onSizeChange(newSize) { size.value = newSize; page.value = 1; fetchResults() }

function clearSearch() {
  searched.value = false
  posts.value = []
  total.value = 0
  query.value = ''
}

// 如果 URL 带 ?q=xxx 自动搜索
onMounted(() => {
  fetchHotKeywords()
  const q = route.query.q
  if (q) {
    keyword.value = q
    doSearch()
  }
})
</script>

<style scoped>
.search-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}
.search-header {
  margin-bottom: 24px;
}
.search-meta {
  color: var(--color-text-tertiary);
  font-size: 14px;
  margin-bottom: 16px;
}

.hot-keywords {
  margin-bottom: 24px;
}

.hot-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 12px;
}

.hot-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.hot-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.loading-state {
  padding: 40px 0;
}
.result-card {
  margin-bottom: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.result-card:hover {
  background: var(--color-bg-warm);
}
.result-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
}
.result-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.result-meta {
  font-size: 12px;
  color: var(--color-text-placeholder);
  display: flex;
  gap: 6px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-start;
  margin-top: 24px;
}
</style>
