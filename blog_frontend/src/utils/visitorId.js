/**
 * 统一的访客 ID 管理
 * 生成 UUID 并存储在 localStorage，供 request.js 和 tracker.js 共用
 */

const STORAGE_KEY = 'blog_visitor_id'

/**
 * 获取或生成访客 ID（UUID 格式）
 */
export function getVisitorId() {
  let id = localStorage.getItem(STORAGE_KEY)
  if (!id) {
    id = 'visitor_' + Date.now().toString(36) + '_' + Math.random().toString(36).slice(2, 8)
    localStorage.setItem(STORAGE_KEY, id)
  }
  return id
}
