<template>
  <div class="collection-tree">
    <div class="tree-header">
      <span>我的集合</span>
      <el-button size="small" type="primary" text @click="$emit('createCollection')">+ 新建</el-button>
    </div>
    <div v-if="collections.length === 0" class="empty-hint">暂无集合</div>
    <div v-for="col in collections" :key="col.id" class="collection-item">
      <div class="col-header" @click="toggleExpand(col.id)">
        <el-icon><component :is="expandedIds.has(col.id) ? 'ArrowDown' : 'ArrowRight'" /></el-icon>
        <span class="col-name">{{ col.name }}</span>
        <el-dropdown trigger="click" @command="handleCommand($event, col)">
          <el-button size="small" text circle @click.stop>
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="rename">重命名</el-dropdown-item>
              <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div v-if="expandedIds.has(col.id)" class="col-requests">
        <div v-if="!requestsMap[col.id]?.length" class="empty-hint">暂无请求</div>
        <div
          v-for="req in requestsMap[col.id]"
          :key="req.id"
          class="request-item"
          :class="{ active: activeRequestId === req.id }"
          @click="$emit('selectRequest', req)"
        >
          <span class="req-method" :class="req.method?.toLowerCase()">{{ req.method }}</span>
          <span class="req-name">{{ req.name }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { MoreFilled, ArrowDown, ArrowRight } from '@element-plus/icons-vue'

defineProps({
  collections: { type: Array, default: () => [] },
  requestsMap: { type: Object, default: () => ({}) },
  activeRequestId: { type: Number, default: null }
})

const emit = defineEmits(['selectRequest', 'createCollection', 'renameCollection', 'deleteCollection', 'loadRequests'])

const expandedIds = reactive(new Set())

function toggleExpand(colId) {
  if (expandedIds.has(colId)) {
    expandedIds.delete(colId)
  } else {
    expandedIds.add(colId)
    emit('loadRequests', colId)
  }
}

function handleCommand(command, col) {
  if (command === 'rename') emit('renameCollection', col)
  else if (command === 'delete') emit('deleteCollection', col)
}
</script>

<style scoped>
.collection-tree { display: flex; flex-direction: column; }
.tree-header { display: flex; justify-content: space-between; align-items: center; padding: 8px; font-weight: 600; }
.collection-item { border-bottom: 1px solid var(--color-border-light); }
.col-header { display: flex; align-items: center; gap: 6px; padding: 8px; cursor: pointer; }
.col-header:hover { background: var(--color-bg-warm); }
.col-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-requests { padding-left: 20px; }
.request-item { display: flex; align-items: center; gap: 6px; padding: 6px 8px; cursor: pointer; font-size: 13px; }
.request-item:hover { background: var(--color-bg-warm); }
.request-item.active { background: var(--color-accent-light); }
.req-method { font-size: 11px; font-weight: 600; padding: 1px 4px; border-radius: 3px; }
.req-method.get { color: #67c23a; }
.req-method.post { color: var(--color-accent); }
.req-method.put { color: #e6a23c; }
.req-method.delete { color: #f56c6c; }
.req-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty-hint { padding: 8px; color: var(--color-text-tertiary); font-size: 12px; text-align: center; }
</style>
