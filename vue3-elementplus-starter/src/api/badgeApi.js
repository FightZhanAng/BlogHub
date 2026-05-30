import request from './request'

/**
 * 徽章 API
 */
export const badgeApi = {
  /** 获取所有徽章定义 */
  list() {
    return request.get('/badges')
  },

  /** 获取用户已获得的徽章 */
  getUserBadges(userId) {
    return request.get(`/users/${userId}/badges`)
  },

  /** 标记徽章通知已读 */
  markRead(userId) {
    return request.put(`/users/${userId}/badges/mark-read`)
  },
}
