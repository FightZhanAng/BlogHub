<template>
  <div class="chat-history">
    <div class="history-header">
      <span>对话历史</span>
    </div>
    <div class="history-list">
      <div
        v-for="conv in conversations"
        :key="conv.id"
        :class="['history-item', { active: conv.id === activeId }]"
        @click="$emit('select', conv.id)"
      >
        <el-icon><ChatDotRound /></el-icon>
        <span class="conv-title">{{ conv.title }}</span>
        <el-button
          class="delete-btn"
          type="danger"
          size="small"
          text
          @click.stop="$emit('delete', conv.id)"
        >
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
      <div v-if="conversations.length === 0" class="empty">暂无对话</div>
    </div>
  </div>
</template>

<script setup>
import { ChatDotRound, Delete } from '@element-plus/icons-vue'

defineProps({
  conversations: { type: Array, default: () => [] },
  activeId: { type: [Number, null], default: null }
})

defineEmits(['select', 'delete'])
</script>

<style scoped>
.chat-history {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.history-header {
  padding: 16px;
  font-weight: 600;
  border-bottom: 1px solid var(--el-border-color-light);
}
.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.history-item:hover {
  background: var(--el-fill-color-light);
}
.history-item.active {
  background: var(--el-color-primary-light-9);
}
.conv-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}
.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
}
.history-item:hover .delete-btn {
  opacity: 1;
}
.empty {
  text-align: center;
  color: #909399;
  padding: 40px 0;
}
</style>
