import axios from 'axios'
import axiosRetry from 'axios-retry'
import { ElMessage } from 'element-plus'
import { getVisitorId } from '@/utils/visitorId'

/**
 * Axios 实例
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 配置重试：仅对幂等请求（GET/HEAD/OPTIONS）的 5xx 和网络超时重试，最多 2 次
axiosRetry(request, {
  retries: 2,
  retryDelay: axiosRetry.exponentialDelay,
  retryCondition: (error) => {
    // 幂等请求的网络错误或超时
    if (axiosRetry.isNetworkOrIdempotentRequestError(error)) return true
    // 仅对幂等方法的 5xx 重试，避免 POST/PUT/DELETE 产生重复操作
    if (error.response && error.response.status >= 500) {
      const method = (error.config?.method || '').toLowerCase()
      return ['get', 'head', 'options'].includes(method)
    }
    return false
  },
})

/** 从 pinia-plugin-persistedstate 存储中读取 token */
function getAuthToken() {
  try {
    const auth = JSON.parse(localStorage.getItem('blog_auth'))
    return auth?.token || ''
  } catch {
    return ''
  }
}

/** 请求拦截器 — 注入访客 ID + Token */
request.interceptors.request.use(
  (config) => {
    config.headers['X-Visitor-Id'] = getVisitorId()

    // 自动注入 Token（从 pinia persist 存储读取）
    const token = getAuthToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 响应拦截器 — 统一解包 Result<T> 并处理错误 */
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一返回 Result { code, message, data }
    if (res.code !== undefined && res.code !== 200 && res.code !== 201) {
      // 401 未授权 — 清除登录态并跳转
      if (res.code === 401) {
        localStorage.removeItem('blog_auth')
        window.location.href = '/login'
        return Promise.reject(new Error(res.message || '未授权'))
      }
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data !== undefined ? res.data : res
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const data = error.response.data
      const msg = data?.message || getStatusText(status)
      ElMessage.error(msg)
    } else if (error.message.includes('timeout')) {
      ElMessage.error('请求超时，请检查网络')
    } else {
      ElMessage.error('网络异常')
    }
    return Promise.reject(error)
  }
)

function getStatusText(status) {
  const map = {
    400: '请求参数错误',
    401: '未授权',
    403: '禁止访问',
    404: '资源不存在',
    422: '参数校验失败',
    500: '服务器内部错误',
    503: '服务不可用',
  }
  return map[status] || `请求失败 (${status})`
}

export default request
