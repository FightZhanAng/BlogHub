<template>
  <div class="user-profile">
    <el-card shadow="never" class="profile-card">
      <div class="profile-header">
        <el-avatar :size="64" :src="user?.avatar?.startsWith('http') ? user.avatar : 'http://localhost:8080' + (user.avatar || '')">
          <template #error><el-icon :size="32"><UserFilled /></el-icon></template>
        </el-avatar>
        <div class="profile-info">
          <h2>{{ user?.username || '用户' }}</h2>
          <p class="bio">{{ user?.bio || '这个人很懒，什么都没写' }}</p>
          <div class="stats">
            <span><strong>{{ stats.postCount || 0 }}</strong> 文章</span>
            <span><strong>{{ stats.followerCount || 0 }}</strong> 粉丝</span>
            <span><strong>{{ stats.followingCount || 0 }}</strong> 关注</span>
          </div>
        </div>
        <el-button v-if="authStore.isLoggedIn && authStore.userId !== id" :type="stats.isFollowing ? 'default' : 'primary'"
          @click="toggleFollow">
          {{ stats.isFollowing ? '已关注' : '关注' }}
        </el-button>
      </div>
    </el-card>

    <h3 style="margin:24px 0 16px">文章</h3>
    <div class="loading-wrap" v-if="loading"><el-skeleton :rows="3" animated /></div>
    <el-empty v-else-if="!posts.length" description="暂无文章" />
    <el-row :gutter="16" v-else>
      <el-col v-for="p in posts" :key="p.id" :xs="24" :sm="12" :lg="8" class="post-col">
        <el-card shadow="never" class="post-card" @click="$router.push(`/blog/${p.slug}`)">
          <h4>{{ p.title }}</h4>
          <span class="date">{{ p.createdAt?.slice(0, 10) }}</span>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()
const id = route.params.id
const user = ref(null)
const stats = ref({})
const posts = ref([])
const loading = ref(true)

async function toggleFollow() {
  try {
    if (stats.value.isFollowing) {
      await request.delete(`/follows/${id}`)
      stats.value.isFollowing = false
      stats.value.followerCount--
    } else {
      await request.post('/follows', { userId: Number(id) })
      stats.value.isFollowing = true
      stats.value.followerCount++
    }
  } catch { ElMessage.error('操作失败') }
}

onMounted(async () => {
  try {
    const [profileRes, postsRes] = await Promise.all([
      request.get(`/users/${id}/profile`),
      request.get('/posts', { params: { authorId: id, page: 1, size: 20 } }),
    ])
    stats.value = profileRes
    posts.value = postsRes.list || []
  } catch { /* ignore */ }
  loading.value = false
})
</script>

<style scoped>
.user-profile { max-width: 1100px; margin: 0 auto; }
.profile-card { border-radius: 12px; border: 1px solid #e8eaed; }
.profile-header { display: flex; align-items: center; gap: 20px; }
.profile-info { flex: 1; }
.profile-info h2 { margin: 0 0 4px; font-size: 20px; }
.bio { color: #909399; font-size: 14px; margin: 0 0 8px; }
.stats { display: flex; gap: 16px; font-size: 13px; color: #606266; }
.post-col { margin-bottom: 16px; }
.post-card { border-radius: 10px; border: 1px solid #e8eaed; cursor: pointer; }
.post-card h4 { margin: 0 0 4px; font-size: 15px; }
.post-card .date { font-size: 12px; color: #c0c4cc; }
.loading-wrap { padding: 40px 0; }
</style>
