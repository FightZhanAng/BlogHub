import axios from 'axios'
import axiosRetry from 'axios-retry'
import { ElMessage } from 'element-plus'
import { getVisitorId } from '@/utils/visitorId'

/** 由 main.js 注入的路由实例，用于在 401 时跳转而不整页刷新 */
let routerInstance = null
export function bindRouter(router) {
  routerInstance = router
}

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

/**
 * 是否是"可选鉴权"的公开接口。
 *
 * 这些接口游客也能访问，后端按 visitorId 返回游客数据；即使返回 401
 * 也只应该静默降级，而不能把访客踢到登录页（否则公开站点对游客完全不可用）。
 */
const OPTIONAL_AUTH_PREFIXES = [
  '/bookmarks',
  '/comments/stats',
  '/comments/',
  '/posts/',
  '/tags',
  '/series',
  '/stats/',
  '/hot-topics',
  '/menus/tree',
  '/badges',
  '/health',
]

function isOptionalAuthRequest(config) {
  const url = config?.url || ''
  if (OPTIONAL_AUTH_PREFIXES.some((p) => url.startsWith(p))) {
    return true
  }
  // 游客场景：本身就没有 token
  return !getAuthToken()
}

/** 处理未授权：清登录态并按需跳转 */
function handleUnauthorized() {
  localStorage.removeItem('blog_auth')
  const current = routerInstance?.currentRoute?.value?.fullPath || window.location.pathname
  if (current && current.startsWith('/login')) return
  if (routerInstance) {
    routerInstance.push({ path: '/login', query: { redirect: current } })
  } else {
    window.location.href = '/login'
  }
}

/** 请求拦截器 — 注入访客 ID + Token */
request.interceptors.request.use(
  (config) => {
    config.headers['X-Visitor-Id'] = getVisitorId()

    // FormData 时清除 Content-Type，让 axios 自动设置 multipart boundary
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }

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
      if (res.code === 401) {
        // 公开接口 / 游客请求：静默降级，返回 null 让调用方走空态
        if (isOptionalAuthRequest(response.config)) {
          return null
        }
        handleUnauthorized()
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

      // 真正的 HTTP 401：公开接口静默降级，其余清登录态并跳转
      if (status === 401) {
        if (isOptionalAuthRequest(error.config)) {
          return Promise.resolve(null)
        }
        handleUnauthorized()
        return Promise.reject(error)
      }

      const msg = data?.message || getStatusText(status)
      ElMessage.error(msg)
    } else if (error.message?.includes('timeout')) {
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
    429: '请求过于频繁，请稍后再试',
    500: '服务器内部错误',
    503: '服务不可用',
  }
  return map[status] || `请求失败 (${status})`
}

export default request
