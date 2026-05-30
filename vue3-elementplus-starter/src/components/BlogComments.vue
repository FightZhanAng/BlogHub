<template>
  <div class="blog-comments" id="blog-comments">
    <h3 class="comments-title">评论</h3>

    <!-- 评论输入 -->
    <div class="comment-form">
      <div class="form-row">
        <el-input v-model="nickname" placeholder="昵称" size="small" class="nickname-input" />
      </div>
      <div class="form-row">
        <el-input v-model="content" type="textarea" :rows="2" placeholder="写评论..." maxlength="500" show-word-limit class="content-input" />
      </div>
      <div class="form-footer">
        <el-button type="primary" size="small" :disabled="!content.trim()" @click="submitComment">发送</el-button>
      </div>
    </div>

    <!-- 评论列表 -->
    <div class="comment-list" v-if="rootCommentsSorted.length">
      <div v-for="comment in rootCommentsSorted" :key="comment.id" class="comment-item">
        <!-- 左侧头像 -->
        <div class="item-avatar">
          <img v-if="comment.avatar" :src="'http://localhost:8080' + comment.avatar" class="avatar-img" />
          <div v-else class="avatar-placeholder">{{ comment.nickname[0] }}</div>
        </div>
        <!-- 右侧内容 -->
        <div class="item-content">
          <div class="item-nickname">{{ comment.nickname }}</div>
          <div class="item-text">{{ comment.content }}</div>
          <div class="item-meta">
            <span>{{ formatTime(comment.createdAt) }}</span>
          </div>
          <!-- 操作栏 -->
          <div class="item-actions">
            <button class="act-btn" @click="startReply(comment)">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
              <span>回复</span>
            </button>
            <button class="act-btn" v-if="comment.replyCount > 0" @click="toggleReplies(comment)">
              <span>{{ expandedSet.has(comment.id) ? '收起' : '展开更多' }}</span>
              <svg v-if="!expandedSet.has(comment.id)" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
              <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15"/></svg>
            </button>
            <div class="act-spacer"></div>
            <button class="act-btn like" @click="handleLike(comment)">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
              <span v-if="comment.likes">{{ comment.likes }}</span>
            </button>
          </div>

          <!-- 回复输入框 -->
          <div class="reply-editor" v-if="replyingTo === comment.id">
            <div class="reply-editor-row">
              <el-input v-model="replyContent" :placeholder="'回复 ' + comment.nickname" size="small" class="reply-input" @keyup.enter="submitReply(comment)" />
              <el-button size="small" type="primary" :disabled="!replyContent.trim()" @click="submitReply(comment)">发送</el-button>
            </div>
          </div>

          <!-- 二级回复区（灰色背景） -->
          <div class="reply-box" v-if="expandedSet.has(comment.id) && (repliesMap[comment.id] || []).length">
            <div v-for="reply in (repliesMap[comment.id] || [])" :key="reply.id" class="reply-row">
              <div class="reply-avatar-wrap">
                <img v-if="reply.avatar" :src="'http://localhost:8080' + reply.avatar" class="reply-avatar-img" />
                <div v-else class="reply-avatar-ph">{{ reply.nickname[0] }}</div>
              </div>
              <div class="reply-content-wrap">
                <div class="reply-header">
                  <span class="reply-name">{{ reply.nickname }}</span>
                  <template v-if="getReplyTarget(reply)">
                    <span class="reply-sep">回复</span>
                    <span class="reply-target">{{ getReplyTarget(reply) }}</span>
                  </template>
                </div>
                <div class="reply-text">{{ stripMention(reply.content) }}</div>
                <div class="reply-footer">
                  <span class="reply-time">{{ formatTime(reply.createdAt) }}</span>
                  <button class="reply-act" @click="startReply(reply, comment.id)">回复</button>
                  <button class="reply-act like-sm" @click="handleLike(reply)">
                    <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                    <span v-if="reply.likes">{{ reply.likes }}</span>
                  </button>
                </div>

                <!-- 回复输入框 -->
                <div class="reply-editor" v-if="replyingTo === reply.id">
                  <div class="reply-editor-row">
                    <el-input v-model="replyContent" :placeholder="'回复 ' + reply.nickname" size="small" class="reply-input" @keyup.enter="submitReply(reply, comment.id)" />
                    <el-button size="small" type="primary" :disabled="!replyContent.trim()" @click="submitReply(reply, comment.id)">发送</el-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 加载更多 -->
            <div class="load-more" v-if="hasMore[comment.id]">
              <button class="load-more-btn" @click="loadMoreReplies(comment)" :disabled="loadingMore[comment.id]">
                {{ loadingMore[comment.id] ? '加载中...' : '展开更多回复' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-else description="暂无评论，来写第一条吧" :image-size="60" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { commentApi } from '@/api/commentApi'
import { useAuthStore } from '@/stores/auth'
import request from '@/api/request'

const props = defineProps({ slug: String })

const authStore = useAuthStore()
const nickname = ref(authStore.nickname || authStore.username || localStorage.getItem('blog_nickname') || '')
const content = ref('')
const replyingTo = ref(null)
const replyContent = ref('')
const rootComments = ref([])
const repliesMap = reactive({})
const expandedSet = reactive(new Set())
const hasMore = reactive({})
const loadingMore = reactive({})
const replyPages = reactive({})

const rootCommentsSorted = computed(() => {
  return [...rootComments.value]
    .filter(c => !c.parentId)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

async function fetchComments() {
  try {
    const res = await commentApi.list(props.slug)
    rootComments.value = res || []
  } catch { rootComments.value = [] }
}

function getReplyTarget(c) {
  if (!c.content) return ''
  const m = c.content.match(/^@(\S+)\s/)
  return m ? m[1] : ''
}

function stripMention(text) {
  return text ? text.replace(/^@\S+\s/, '') : text
}

function formatTime(raw) {
  const d = new Date(raw)
  if (isNaN(d.getTime())) return raw
  const diff = Date.now() - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 259200000) return `${Math.floor(diff / 86400000)}天前`
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

function startReply(comment, rootId) {
  replyingTo.value = comment.id
  replyContent.value = ''
  nextTick(() => {
    const el = document.querySelector('.reply-editor .reply-input input, .reply-editor .reply-input textarea')
    if (el) el.focus()
  })
}

async function submitComment() {
  if (!content.value.trim()) return
  const name = nickname.value.trim() || '匿名'
  try {
    await commentApi.create(props.slug, { nickname: name, content: content.value })
    localStorage.setItem('blog_nickname', name)
    content.value = ''
    ElMessage.success('评论已发表')
    await fetchComments()
  } catch {}
}

async function submitReply(comment, rootId) {
  if (!replyContent.value.trim()) return
  const name = nickname.value.trim() || '匿名'
  try {
    const fullContent = replyContent.value.startsWith('@')
      ? replyContent.value
      : `@${comment.nickname} ${replyContent.value}`
    await commentApi.create(props.slug, { nickname: name, content: fullContent, parentId: rootId || comment.id })
    replyContent.value = ''
    replyingTo.value = null
    ElMessage.success('回复已发表')
    await fetchComments()
    const targetRoot = rootId || comment.id
    if (expandedSet.has(targetRoot)) {
      await loadReplies(targetRoot, 1, true)
    }
  } catch {}
}

async function toggleReplies(comment) {
  const id = comment.id
  if (expandedSet.has(id)) {
    expandedSet.delete(id)
  } else {
    expandedSet.add(id)
    if (!repliesMap[id]) await loadReplies(id, 1)
  }
}

async function loadReplies(commentId, page, replace = false) {
  try {
    loadingMore[commentId] = true
    const res = await commentApi.getReplies(commentId, page, 10)
    repliesMap[commentId] = replace
      ? (res.list || [])
      : [...(repliesMap[commentId] || []), ...(res.list || [])]
    replyPages[commentId] = page
    hasMore[commentId] = (res.list || []).length === 10
  } catch {} finally { loadingMore[commentId] = false }
}

async function loadMoreReplies(comment) {
  await loadReplies(comment.id, (replyPages[comment.id] || 1) + 1)
}

async function handleLike(comment) {
  try {
    await request.post(`/comments/${comment.id}/like`)
    comment.likes = (comment.likes || 0) + 1
  } catch {}
}

fetchComments()
</script>

<style scoped>
.blog-comments {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.comments-title {
  font-size: 18px;
  margin: 0 0 16px;
  color: #303133;
  font-weight: 600;
}

/* ===== 评论输入 ===== */
.comment-form {
  margin-bottom: 20px;
}

.form-row {
  margin-bottom: 8px;
}

.nickname-input :deep(.el-input__wrapper) {
  border-radius: 20px;
  background: #f5f7fa;
  box-shadow: none;
}

.content-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: #f5f7fa;
  box-shadow: none;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
}

/* ===== 评论列表 ===== */
.comment-list {
  display: flex;
  flex-direction: column;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.item-avatar {
  flex-shrink: 0;
}

.avatar-img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-nickname {
  font-size: 13px;
  color: #909399;
  margin-bottom: 2px;
}

.item-text {
  font-size: 15px;
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
  margin-bottom: 4px;
}

.item-meta {
  font-size: 12px;
  color: #c0c4cc;
  margin-bottom: 6px;
}

/* 操作栏 */
.item-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.act-spacer { flex: 1; }

.act-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #909399;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.15s;
}

.act-btn:hover { color: #409eff; }

.act-btn.like:hover { color: #f56c6c; }
.act-btn.like:hover svg { fill: #f56c6c; stroke: #f56c6c; }

/* ===== 回复编辑器 ===== */
.reply-editor {
  margin: 8px 0;
}

.reply-editor-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.reply-input {
  flex: 1;
}

.reply-input :deep(.el-input__wrapper) {
  border-radius: 18px;
  background: #f0f2f5;
  box-shadow: none;
}

/* ===== 二级回复区（灰色背景） ===== */
.reply-box {
  background: #f7f8fa;
  border-radius: 8px;
  padding: 8px 12px;
  margin-top: 8px;
}

.reply-row {
  display: flex;
  gap: 8px;
  padding: 8px 0;
}

.reply-row + .reply-row {
  border-top: 1px solid #eee;
}

.reply-avatar-wrap {
  flex-shrink: 0;
}

.reply-avatar-img {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.reply-avatar-ph {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f093fb, #f5576c);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}

.reply-content-wrap {
  flex: 1;
  min-width: 0;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 2px;
  flex-wrap: wrap;
}

.reply-name {
  font-size: 13px;
  color: #909399;
  font-weight: 500;
}

.reply-sep {
  font-size: 12px;
  color: #c0c4cc;
}

.reply-target {
  font-size: 13px;
  color: #909399;
  font-weight: 500;
}

.reply-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
  margin-bottom: 4px;
}

.reply-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #c0c4cc;
}

.reply-act {
  border: none;
  background: none;
  color: #c0c4cc;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  transition: color 0.15s;
}

.reply-act:hover { color: #409eff; }

.like-sm:hover { color: #f56c6c; }
.like-sm:hover svg { fill: #f56c6c; stroke: #f56c6c; }

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 8px 0 2px;
  border-top: 1px solid #eee;
  margin-top: 4px;
}

.load-more-btn {
  background: none;
  border: none;
  color: #909399;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 12px;
  transition: color 0.15s;
}

.load-more-btn:hover { color: #409eff; }
.load-more-btn:disabled { opacity: 0.5; cursor: default; }
</style>
