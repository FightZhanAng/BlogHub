<template>
  <div class="comment-reactions">
    <button
      class="reaction-btn"
      :class="{ active: myReaction === 1 }"
      @click="toggleReaction(1)"
    >
      <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
        <path d="M2 20h2V8H2v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/>
      </svg>
      <span v-if="likeCount > 0">{{ likeCount }}</span>
    </button>
    <button
      class="reaction-btn"
      :class="{ active: myReaction === 0 }"
      @click="toggleReaction(0)"
    >
      <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
        <path d="M15 3H6c-.83 0-1.54.5-1.84 1.22l-3.02 7.05c-.09.23-.14.47-.14.73v2c0 1.1.9 2 2 2h6.31l-.95 4.57-.03.32c0 .41.17.79.44 1.06L9.83 23l6.59-6.59c.36-.36.58-.86.58-1.41V5c0-1.1-.9-2-2-2zm4 0v12h4V3h-4z"/>
      </svg>
      <span v-if="dislikeCount > 0">{{ dislikeCount }}</span>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { commentApi } from '@/api/commentApi'

const props = defineProps({
  commentId: { type: [Number, String], required: true }
})

const likeCount = ref(0)
const dislikeCount = ref(0)
const myReaction = ref(null) // 1=已赞, 0=已踩, null=未操作

async function fetchReactions() {
  try {
    const res = await commentApi.getReactions(props.commentId)
    likeCount.value = res.likeCount || 0
    dislikeCount.value = res.dislikeCount || 0
    myReaction.value = res.myReaction
  } catch (e) {
    console.error('获取反应数据失败', e)
  }
}

async function toggleReaction(type) {
  try {
    if (myReaction.value === type) {
      // 取消反应
      await commentApi.removeReaction(props.commentId)
      if (type === 1) likeCount.value--
      else dislikeCount.value--
      myReaction.value = null
    } else {
      // 添加或切换反应
      await commentApi.react(props.commentId, type)
      if (myReaction.value === 1) likeCount.value--
      else if (myReaction.value === 0) dislikeCount.value--
      if (type === 1) likeCount.value++
      else dislikeCount.value++
      myReaction.value = type
    }
  } catch (e) {
    console.error('反应操作失败', e)
  }
}

onMounted(fetchReactions)
</script>

<style scoped>
.comment-reactions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.reaction-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  border-radius: 4px;
  font-size: 12px;
  transition: all 0.2s;
}

.reaction-btn:hover {
  background: var(--color-bg-warm);
  color: var(--color-text-secondary);
}

.reaction-btn.active {
  color: #fe2c55;
}

.reaction-btn.active:hover {
  background: rgba(254, 44, 85, 0.1);
}

.reaction-btn span {
  font-size: 12px;
}
</style>
