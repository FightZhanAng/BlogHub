import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/authApi'

export const useAuthStore = defineStore('auth', () => {
  const token = ref('')
  const username = ref('')
  const nickname = ref('')
  const avatar = ref('')
  const role = ref('')
  const userId = ref('')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'admin')

  function saveUser(t, u, n, a, r, id) {
    token.value = t
    username.value = u
    nickname.value = n
    avatar.value = a || ''
    role.value = r
    userId.value = id || ''
  }

  async function login(user, password) {
    const res = await authApi.login({ username: user, password })
    saveUser(res.token, res.username, res.nickname, res.avatar, res.role, res.userId)
    return res
  }

  async function register(user, password) {
    const res = await authApi.register({ username: user, password })
    saveUser(res.token, res.username, res.nickname, res.avatar, res.role, res.userId)
    return res
  }

  function logout() {
    token.value = ''
    username.value = ''
    nickname.value = ''
    avatar.value = ''
    role.value = ''
    userId.value = ''
  }

  return { token, username, nickname, avatar, role, userId, isLoggedIn, isAdmin, login, register, logout, saveUser }
}, {
  persist: {
    key: 'blog_auth',
    pick: ['token', 'username', 'nickname', 'avatar', 'role', 'userId'],
  },
})
