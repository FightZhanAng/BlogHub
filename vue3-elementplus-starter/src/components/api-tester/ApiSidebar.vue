<template>
  <div class="api-sidebar">
    <!-- 搜索框 -->
    <div class="sidebar-search">
      <el-input v-model="searchQuery" placeholder="搜索..." size="small" clearable>
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 集合 -->
    <ApiCollectionTree
      :collections="filteredCollections"
      :requestsMap="requestsMap"
      :activeRequestId="activeRequestId"
      @selectRequest="$emit('selectRequest', $event)"
      @createCollection="$emit('createCollection')"
      @renameCollection="$emit('renameCollection', $event)"
      @deleteCollection="$emit('deleteCollection', $event)"
      @loadRequests="$emit('loadRequests', $event)"
    />

    <!-- 分隔线 -->
    <el-divider />

    <!-- 历史记录 -->
    <ApiHistoryList
      :history="filteredHistory"
      @loadHistory="$emit('loadHistory', $event)"
      @clearHistory="$emit('clearHistory')"
    />

    <!-- 分隔线 -->
    <el-divider />

    <!-- 环境变量 -->
    <div class="env-section">
      <el-button size="small" text @click="$emit('openEnvManager')">
        ⚙️ 环境变量
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import ApiCollectionTree from './ApiCollectionTree.vue'
import ApiHistoryList from './ApiHistoryList.vue'

const props = defineProps({
  collections: { type: Array, default: () => [] },
  requestsMap: { type: Object, default: () => ({}) },
  activeRequestId: { type: Number, default: null },
  history: { type: Array, default: () => [] }
})

defineEmits([
  'selectRequest', 'createCollection', 'renameCollection',
  'deleteCollection', 'loadRequests', 'loadHistory',
  'clearHistory', 'openEnvManager'
])

const searchQuery = ref('')

const filteredCollections = computed(() => {
  if (!searchQuery.value) return props.collections
  const q = searchQuery.value.toLowerCase()
  return props.collections.filter(c => c.name?.toLowerCase().includes(q))
})

const filteredHistory = computed(() => {
  if (!searchQuery.value) return props.history
  const q = searchQuery.value.toLowerCase()
  return props.history.filter(h => h.url?.toLowerCase().includes(q))
})
</script>

<style scoped>
.api-sidebar { display: flex; flex-direction: column; height: 100%; overflow-y: auto; }
.sidebar-search { padding: 8px; }
.env-section { padding: 8px; }
</style>
