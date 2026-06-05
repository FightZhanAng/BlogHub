<template>
  <div class="chat-history">
    <div class="history-list">
      <div v-for="group in groupedConversations" :key="group.label" class="history-group">
        <div class="group-header" @click="toggleGroup(group.label)">
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="group-arrow" :class="{ collapsed: collapsedGroups[group.label] }"><polyline points="9 18 15 12 9 6"/></svg>
          <span class="group-label">{{ group.label }}</span>
          <span class="group-count">{{ group.items.length }}</span>
        </div>
        <div v-show="!collapsedGroups[group.label]" class="group-items">
          <div
            v-for="conv in group.items"
            :key="conv.id"
            :class="['history-item', { active: conv.id === activeId }]"
            @click="$emit('select', conv.id)"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="history-icon"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            <span class="conv-title">{{ conv.title }}</span>
            <button class="delete-btn" @click.stop="$emit('delete', conv.id)" title="删除">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </button>
          </div>
        </div>
      </div>
      <div v-if="conversations.length === 0" class="empty">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" style="color: var(--ai-text-tertiary); margin-bottom: 12px;">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
        <p>暂无对话</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  conversations: { type: Array, default: () => [] },
  activeId: { type: [Number, null], default: null }
})

defineEmits(['select', 'delete'])

const collapsedGroups = ref({})

function toggleGroup(label) {
  collapsedGroups.value[label] = !collapsedGroups.value[label]
}

function getTimeGroup(conv) {
  const raw = conv.createdAt || conv.updatedAt
  if (!raw) return '今天'
  const date = new Date(raw)
  if (isNaN(date.getTime())) return '今天'
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  const weekAgo = new Date(today)
  weekAgo.setDate(weekAgo.getDate() - 7)

  if (date >= today) return '今天'
  if (date >= yesterday) return '昨天'
  if (date >= weekAgo) return '近 7 天'
  return '更早'
}

const groupedConversations = computed(() => {
  const groups = {}
  const order = ['今天', '昨天', '近 7 天', '更早']
  for (const conv of props.conversations) {
    const label = getTimeGroup(conv)
    if (!groups[label]) groups[label] = []
    groups[label].push(conv)
  }
  return order.filter(l => groups[l]).map(l => ({ label: l, items: groups[l] }))
})
</script>

<style scoped>
.chat-history {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.group-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 8px 4px;
  cursor: pointer;
  user-select: none;
}
.group-arrow {
  color: var(--ai-text-tertiary);
  transition: transform 0.2s;
  flex-shrink: 0;
}
.group-arrow.collapsed {
  transform: rotate(-90deg);
}
.group-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--ai-text-tertiary);
  letter-spacing: 0.5px;
  text-transform: uppercase;
}
.group-count {
  font-size: 10px;
  color: var(--ai-text-tertiary);
  margin-left: auto;
  opacity: 0.5;
}
.history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: var(--ai-radius-sm);
  cursor: pointer;
  transition: all var(--ai-transition);
  position: relative;
}
.history-item:hover {
  background: var(--ai-surface-hover);
}
.history-item.active {
  background: var(--ai-surface-active);
}
.history-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background: var(--ai-accent-gradient);
  border-radius: 0 3px 3px 0;
}
.history-icon {
  color: var(--ai-text-tertiary);
  flex-shrink: 0;
}
.conv-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: var(--ai-text-secondary);
}
.history-item.active .conv-title {
  color: var(--ai-text);
}
.delete-btn {
  opacity: 0;
  transition: opacity var(--ai-transition);
  background: none;
  border: none;
  color: var(--ai-text-tertiary);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
}
.delete-btn:hover {
  color: #ef4444;
  background: rgba(239,68,68,0.1);
}
.history-item:hover .delete-btn {
  opacity: 1;
}
.empty {
  text-align: center;
  color: var(--ai-text-tertiary);
  padding: 60px 0;
  font-size: 13px;
}
</style>
