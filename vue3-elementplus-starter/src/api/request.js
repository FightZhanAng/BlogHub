import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 生成或获取访客 ID（UUID 存 localStorage）
 */
function getVisitorId() {
  let id = localStorage.getItem('blog_visitor_id')
  if (!id) {
    id = 'visitor_' + Date.now().toString(36) + '_' + Math.random().toString(36).slice(2, 8)
    localStorage.setItem('blog_visitor_id', id)
  }
  return id
}

/**
 * Axios 实例
 */
const request = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/** 请求拦截器 — 注入访客 ID + Token */
request.interceptors.request.use(
  (config) => {
    config.headers['X-Visitor-Id'] = getVisitorId()

    // 自动注入 Token
    const token = localStorage.getItem('blog_token')
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
        localStorage.removeItem('blog_token')
        localStorage.removeItem('blog_username')
        localStorage.removeItem('blog_nickname')
        localStorage.removeItem('blog_role')
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
