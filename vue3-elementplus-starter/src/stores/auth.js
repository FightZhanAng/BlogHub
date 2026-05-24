import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/authApi'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('blog_token') || '')
  const username = ref(localStorage.getItem('blog_username') || '')
  const nickname = ref(localStorage.getItem('blog_nickname') || '')
  const avatar = ref(localStorage.getItem('blog_avatar') || '')
  const role = ref(localStorage.getItem('blog_role') || '')
  const userId = ref(localStorage.getItem('blog_userId') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'admin')

  function saveUser(t, u, n, a, r, id) {
    token.value = t
    username.value = u
    nickname.value = n
    avatar.value = a || ''
    role.value = r
    userId.value = id || ''
    localStorage.setItem('blog_token', t)
    localStorage.setItem('blog_username', u)
    localStorage.setItem('blog_nickname', n)
    localStorage.setItem('blog_avatar', a || '')
    localStorage.setItem('blog_role', r)
    if (id) localStorage.setItem('blog_userId', id)
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
    localStorage.removeItem('blog_token')
    localStorage.removeItem('blog_username')
    localStorage.removeItem('blog_nickname')
    localStorage.removeItem('blog_avatar')
    localStorage.removeItem('blog_role')
    localStorage.removeItem('blog_userId')
  }

  return { token, username, nickname, avatar, role, userId, isLoggedIn, isAdmin, login, register, logout, saveUser }
})
