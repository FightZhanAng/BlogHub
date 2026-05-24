<template>
  <div class="profile-page">
    <el-card shadow="never" class="profile-card">
      <!-- 头部头像 + 基本信息 -->
      <div class="profile-header">
        <div class="avatar-wrapper">
          <el-avatar :size="72" :src="avatarSrc" class="profile-avatar">
            <template #error><el-icon :size="36"><UserFilled /></el-icon></template>
          </el-avatar>
          <div class="avatar-overlay" @click="$refs.avatarInput.click()">
            <el-icon :size="20"><Camera /></el-icon>
            <span>更换头像</span>
          </div>
          <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="uploadAvatar" />
        </div>
        <div class="profile-info">
          <h1>{{ userInfo.nickname || userInfo.username || '用户' }}</h1>
          <p class="profile-role">{{ userInfo.role === 'admin' ? '管理员' : '普通用户' }}</p>
        </div>
      </div>

      <el-divider style="margin: 20px 0" />

      <!-- Tab -->
      <el-tabs v-model="activeTab" class="profile-tabs">
        <!-- ===== 个人信息 ===== -->
        <el-tab-pane label="个人信息" name="info">
          <el-form :model="profileForm" label-width="100px" class="profile-form" ref="profileFormRef">
            <el-form-item label="用户名">
              <el-input :model-value="userInfo.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" maxlength="50" />
            </el-form-item>
            <el-form-item label="角色">
              <el-tag :type="userInfo.role === 'admin' ? 'danger' : 'info'">
                {{ userInfo.role === 'admin' ? '管理员' : '普通用户' }}
              </el-tag>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile" :loading="savingProfile">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ===== 我的文章 ===== -->
        <el-tab-pane label="我的文章" name="posts">
          <div v-if="myPosts.length" class="post-list">
            <div v-for="post in myPosts" :key="post.id" class="post-row" @click="$router.push(`/blog/${post.slug}`)">
              <div class="post-row-info">
                <span class="post-row-title">{{ post.title }}</span>
                <span class="post-row-meta">{{ formatDate(post.createdAt) }} · {{ post.views }} 次阅读</span>
              </div>
              <el-tag v-if="post.status === 0" size="small" type="warning">草稿</el-tag>
            </div>
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="postTotal"
              :page-size="postSize"
              :page-sizes="[10,20,50]"
              v-model:current-page="postPage"
              @current-change="fetchMyPosts"
              @size-change="onPostSizeChange"
              class="pagination"
            />
          </div>
          <el-empty v-else description="还没有写过文章" :image-size="60" />
        </el-tab-pane>

        <!-- ===== 我的评论 ===== -->
        <el-tab-pane label="我的评论" name="comments">
          <div v-if="myComments.length" class="comment-list">
            <div v-for="c in myComments" :key="c.id" class="comment-row">
              <div class="comment-row-body">
                <p class="comment-row-content">{{ c.content }}</p>
                <div class="comment-row-meta">
                  <span class="comment-row-time">{{ formatDate(c.createdAt) }}</span>
                  <span class="comment-row-sep">·</span>
                  <el-link type="primary" :underline="false" @click="$router.push(`/blog/${c.postSlug}`)" v-if="c.postSlug">
                    查看原文
                  </el-link>
                </div>
              </div>
            </div>
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="commentTotal"
              :page-size="commentSize"
              :page-sizes="[10,20,50]"
              v-model:current-page="commentPage"
              @current-change="fetchMyComments"
              @size-change="onCommentSizeChange"
              class="pagination"
            />
          </div>
          <el-empty v-else description="还没有发表过评论" :image-size="60" />
        </el-tab-pane>

        <!-- ===== 修改密码 ===== -->
        <el-tab-pane label="修改密码" name="password">
          <el-form :model="pwdForm" ref="pwdFormRef" :rules="pwdRules" label-width="100px" class="profile-form" style="max-width:400px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changePwd" :loading="savingPwd">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, UserFilled } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const activeTab = ref('info')

// ===== 用户信息 =====
const userInfo = ref({})
const profileForm = reactive({ nickname: '' })
const profileFormRef = ref(null)
const savingProfile = ref(false)

async function fetchUserInfo() {
  try {
    const res = await request.get('/users/me')
    userInfo.value = res
    profileForm.nickname = res.nickname || ''
  } catch {}
}

const avatarInput = ref(null)
const BASE = 'http://localhost:8080'

const avatarSrc = computed(() => {
  const a = userInfo.value?.avatar
  if (!a) return undefined
  return a.startsWith('http') ? a : BASE + a
})

async function uploadAvatar(e) {
  const file = e.target.files?.[0]
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await request.post('/users/me/avatar', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    userInfo.value.avatar = res.url + '?t=' + Date.now()
    authStore.saveUser(authStore.token, authStore.username, authStore.nickname, res.url, authStore.role, authStore.userId)
    ElMessage.success('头像已更新')
  } catch { ElMessage.error('上传失败') }
}

async function saveProfile() {
  savingProfile.value = true
  try {
    const res = await request.put('/users/me', { nickname: profileForm.nickname })
    userInfo.value = res
    // 同步 authStore
    authStore.saveUser(authStore.token, authStore.username, res.nickname, authStore.role, authStore.userId)
    ElMessage.success('已保存')
  } catch {
  } finally {
    savingProfile.value = false
  }
}

// ===== 我的文章 =====
const myPosts = ref([])
const postPage = ref(1)
const postSize = ref(10)
const postTotal = ref(0)

async function fetchMyPosts(page = 1) {
  postPage.value = page
  try {
    const res = await request.get('/users/me/posts', { params: { page, size: postSize.value } })
    myPosts.value = res.list || []
    postTotal.value = res.total || 0
  } catch {
    myPosts.value = []
  }
}

// ===== 我的评论 =====
const myComments = ref([])
const commentPage = ref(1)
const commentSize = ref(10)
const commentTotal = ref(0)

async function fetchMyComments(page = 1) {
  commentPage.value = page
  try {
    const res = await request.get('/users/me/comments', { params: { page, size: commentSize.value } })
    myComments.value = res.list || []
    commentTotal.value = res.total || 0
  } catch {
    myComments.value = []
  }
}

// ===== 修改密码 =====
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdFormRef = ref(null)
const savingPwd = ref(false)

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (rule, value, cb) => value === pwdForm.newPassword ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' },
  ],
}

async function changePwd() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  savingPwd.value = true
  try {
    await request.put('/users/me/password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码已修改')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch {
  } finally {
    savingPwd.value = false
  }
}

function formatDate(raw) {
  if (!raw) return ''
  const d = new Date(raw)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

function onPostSizeChange(newSize) { postSize.value = newSize; postPage.value = 1; fetchMyPosts(1) }
function onCommentSizeChange(newSize) { commentSize.value = newSize; commentPage.value = 1; fetchMyComments(1) }

onMounted(() => {
  fetchUserInfo()
  fetchMyPosts()
  fetchMyComments()
})
</script>

<style scoped>
.profile-page {
  max-width: 800px;
  margin: 0 auto;
}

.profile-card {
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.profile-card :deep(.el-card__body) {
  padding: 32px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
}

.profile-avatar {
  flex-shrink: 0;
}
.avatar-wrapper {
  position: relative;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
}
.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.5);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  font-size: 11px;
  gap: 2px;
}
.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.profile-info h1 {
  margin: 0 0 4px 0;
  font-size: 22px;
  color: #303133;
}

.profile-role {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.profile-tabs {
  margin-top: 0;
}

.profile-form {
  max-width: 480px;
  margin-top: 8px;
}

/* 文章列表 */
.post-list {
  display: flex;
  flex-direction: column;
}

.post-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: padding-left 0.2s;
}

.post-row:hover {
  padding-left: 8px;
}

.post-row:last-child {
  border-bottom: none;
}

.post-row-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.post-row-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.post-row-meta {
  font-size: 12px;
  color: #c0c4cc;
}

/* 评论列表 */
.comment-list {
  display: flex;
  flex-direction: column;
}

.comment-row {
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.comment-row:last-child {
  border-bottom: none;
}

.comment-row-content {
  margin: 0 0 6px 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.comment-row-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #c0c4cc;
}

.pagination {
  margin-top: 20px;
  text-align: left;
}
</style>
