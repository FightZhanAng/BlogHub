<template>
  <div class="blog-comments" id="blog-comments">
    <h3 class="comments-title">
      <el-icon style="vertical-align:middle;margin-right:6px"><ChatDotSquare /></el-icon>
      评论 ({{ rootComments.length }})
    </h3>

    <!-- 评论输入 -->
    <div class="comment-form">
      <el-input
        v-model="nickname"
        placeholder="你的昵称"
        size="small"
        class="nickname-input"
        maxlength="20"
        show-word-limit
      />
      <el-input
        v-model="content"
        type="textarea"
        :rows="3"
        placeholder="写下你的评论..."
        maxlength="500"
        show-word-limit
      />
      <div class="form-footer">
        <el-button type="primary" size="small" :disabled="!canSubmit" @click="submitComment">
          发表评论
        </el-button>
      </div>
    </div>

    <!-- 评论列表 -->
    <div class="comment-list" v-if="comments.length">
      <div v-for="comment in rootComments" :key="comment.id" class="comment-item">
        <div class="comment-avatar">
          <el-avatar :size="36" :src="comment.avatar?.startsWith('http') ? comment.avatar : 'http://localhost:8080' + (comment.avatar || '')">{{ comment.nickname[0] }}</el-avatar>
        </div>
        <div class="comment-body">
          <div class="comment-header">
            <span class="comment-nickname">{{ comment.nickname }}</span>
            <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
          </div>
          <p class="comment-content">{{ comment.content }}</p>

          <!-- 赞/踩/回复 -->
          <div class="comment-actions">
            <button class="action-btn" @click="handleLike(comment)">
              <el-icon :size="14"><ThumbsUp /></el-icon>
              {{ comment.likes || '' }}
            </button>
            <button class="action-btn" @click="handleDislike(comment)">
              <el-icon :size="14"><ThumbsDown /></el-icon>
              {{ comment.dislikes || '' }}
            </button>
            <button class="action-btn" @click="startReply(comment)">
              <el-icon :size="14"><ChatLineSquare /></el-icon>
              回复
            </button>
          </div>

          <!-- 回复输入 -->
          <div class="reply-form" v-if="replyingTo === comment.id">
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="2"
              :placeholder="'回复 @' + comment.nickname"
              maxlength="500"
            />
            <div class="reply-actions">
              <el-button size="small" text @click="cancelReply">取消</el-button>
              <el-button size="small" type="primary" :disabled="!replyContent.trim()" @click="submitReply(comment)">
                回复
              </el-button>
            </div>
          </div>

          <!-- 子评论（支持多级嵌套回复） -->
          <div class="replies" v-if="getReplies(comment.id).length">
            <template v-for="reply in getReplies(comment.id)" :key="reply.id">
              <div class="reply-item">
                <div class="reply-avatar">
                  <el-avatar :size="28" :src="reply.avatar?.startsWith('http') ? reply.avatar : 'http://localhost:8080' + (reply.avatar || '')">{{ reply.nickname[0] }}</el-avatar>
                </div>
                <div class="reply-body">
                  <div class="reply-header">
                    <span class="reply-nickname">{{ reply.nickname }}</span>
                    <span class="reply-time">{{ formatTime(reply.createdAt) }}</span>
                  </div>
                  <p class="reply-content">{{ reply.content }}</p>
                  <div class="comment-actions">
                    <button class="action-btn" @click="handleLike(reply)">
                      <el-icon :size="13"><ThumbsUp /></el-icon>
                      {{ reply.likes || '' }}
                    </button>
                    <button class="action-btn" @click="handleDislike(reply)">
                      <el-icon :size="13"><ThumbsDown /></el-icon>
                      {{ reply.dislikes || '' }}
                    </button>
                    <button class="action-btn" @click="startReply(reply)">
                      <el-icon :size="13"><ChatLineSquare /></el-icon>
                      回复
                    </button>
                  </div>

                  <!-- 回复的回复表单 -->
                  <div class="reply-form" v-if="replyingTo === reply.id">
                    <el-input v-model="replyContent" type="textarea" :rows="2"
                      :placeholder="'回复 @' + reply.nickname" maxlength="500" />
                    <div class="reply-actions">
                      <el-button size="small" text @click="cancelReply">取消</el-button>
                      <el-button size="small" type="primary" :disabled="!replyContent.trim()" @click="submitReply(reply)">
                        回复
                      </el-button>
                    </div>
                  </div>

                  <!-- 更深一层子回复 -->
                  <div class="sub-replies" v-if="getReplies(reply.id).length">
                    <div v-for="sub in getReplies(reply.id)" :key="sub.id" class="reply-item">
                      <div class="reply-avatar">
                        <el-avatar :size="26" :src="sub.avatar?.startsWith('http') ? sub.avatar : 'http://localhost:8080' + (sub.avatar || '')">{{ sub.nickname[0] }}</el-avatar>
                      </div>
                      <div class="reply-body">
                        <div class="reply-header">
                          <span class="reply-nickname">{{ sub.nickname }}</span>
                          <span class="reply-time">{{ formatTime(sub.createdAt) }}</span>
                        </div>
                        <p class="reply-content">{{ sub.content }}</p>
                        <div class="comment-actions">
                          <button class="action-btn" @click="handleLike(sub)">
                            <el-icon :size="13"><ThumbsUp /></el-icon>
                            {{ sub.likes || '' }}
                          </button>
                          <button class="action-btn" @click="handleDislike(sub)">
                            <el-icon :size="13"><ThumbsDown /></el-icon>
                            {{ sub.dislikes || '' }}
                          </button>
                          <button class="action-btn" @click="startReply(sub)">
                            <el-icon :size="13"><ChatLineSquare /></el-icon>
                            回复
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-else description="暂无评论，来写第一条吧" :image-size="60" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useComments } from '@/composables/useInteraction'
import { useAuthStore } from '@/stores/auth'
import request from '@/api/request'

const props = defineProps({
  slug: String,
})

const authStore = useAuthStore()
const { comments, addComment, fetchComments } = useComments(props.slug)

const nickname = ref(authStore.nickname || authStore.username || localStorage.getItem('blog_nickname') || '')
const content = ref('')
const replyingTo = ref(null)
const replyContent = ref('')
const replyingToNickname = ref('')

const canSubmit = computed(() => nickname.value.trim() && content.value.trim())

// 根评论（按时间倒序）
const rootComments = computed(() => {
  return [...comments.value]
    .filter(c => !c.parentId)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

// 获取指定父评论的子评论
function getReplies(parentId) {
  return [...comments.value]
    .filter(c => c.parentId === parentId)
    .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
}

function startReply(comment) {
  replyingTo.value = comment.id
  replyingToNickname.value = comment.nickname
  replyContent.value = ''
}

function cancelReply() {
  replyingTo.value = null
  replyContent.value = ''
}

function formatTime(raw) {
  const d = new Date(raw)
  if (isNaN(d.getTime())) return raw
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 259200000) return `${Math.floor(diff / 86400000)} 天前`
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function submitComment() {
  if (!canSubmit.value) return
  await addComment(nickname.value, content.value)
  localStorage.setItem('blog_nickname', nickname.value)
  content.value = ''
  ElMessage.success('评论已发表')
}

async function submitReply(comment) {
  if (!replyContent.value.trim()) return
  await request.post(`/posts/${props.slug}/comments`, {
    nickname: nickname.value || '匿名',
    content: replyContent.value,
    parentId: comment.id,
  })
  replyContent.value = ''
  replyingTo.value = null
  await fetchComments()
  ElMessage.success('回复已发表')
}

async function handleLike(comment) {
  try {
    await request.post(`/comments/${comment.id}/like`)
    comment.likes = (comment.likes || 0) + 1
  } catch {}
}

async function handleDislike(comment) {
  try {
    await request.post(`/comments/${comment.id}/dislike`)
    comment.dislikes = (comment.dislikes || 0) + 1
  } catch {}
}
</script>

<style scoped>
.blog-comments {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.comments-title {
  font-size: 17px;
  margin: 0 0 20px 0;
  color: #303133;
  font-weight: 600;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 28px;
  padding: 20px;
  background: #fafbfc;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
}

.nickname-input {
  max-width: 200px;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-nickname {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.comment-time {
  font-size: 12px;
  color: #c0c4cc;
}

.comment-content {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  word-break: break-word;
}

/* 赞/踩/回复 */
.comment-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #c0c4cc;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.action-btn:hover {
  background: #f0f5ff;
  color: #409eff;
}

/* 回复表单 */
.reply-form {
  margin: 8px 0 12px;
  padding: 12px;
  background: #fafbfc;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.reply-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 8px;
}

/* 子评论 */
.replies {
  margin-top: 12px;
  padding-left: 16px;
  border-left: 2px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-item {
  display: flex;
  gap: 10px;
}

.reply-avatar {
  flex-shrink: 0;
}

.reply-body {
  flex: 1;
  min-width: 0;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.reply-nickname {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.reply-time {
  font-size: 11px;
  color: #c0c4cc;
}

.reply-content {
  margin: 0 0 6px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  word-break: break-word;
}

.sub-replies {
  margin-top: 8px;
  padding-left: 12px;
  border-left: 2px solid #e8eaed;
}
</style>
