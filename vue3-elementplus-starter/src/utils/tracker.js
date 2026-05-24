/**
 * 前端埋点工具
 * 使用 navigator.sendBeacon 确保页面关闭时数据不丢失
 */

const TRACK_URL = '/api/stats/track'

let visitorId = localStorage.getItem('visitor_id')
if (!visitorId) {
  visitorId = 'v_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
  localStorage.setItem('visitor_id', visitorId)
}

/**
 * 发送埋点事件
 * @param {string} event 事件名
 * @param {object} data  附加数据
 */
export function track(event, data = {}) {
  try {
    const payload = JSON.stringify({
      event,
      data,
      url: location.href,
      visitorId,
      ts: Date.now(),
    })
    navigator.sendBeacon(TRACK_URL, payload)
  } catch {
    // 静默失败，埋点不应影响主流程
  }
}

/**
 * 自动埋点：页面访问
 */
export function trackPageView() {
  track('page_view', { title: document.title })
}
