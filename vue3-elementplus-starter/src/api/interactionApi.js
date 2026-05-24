import request from './request'

/**
 * 互动 API（点赞 + 收藏）
 */
export const interactionApi = {
  // ========== 点赞 ==========

  /** 点赞/取消点赞 */
  toggleLike(slug) {
    return request.post(`/posts/${slug}/like`)
  },

  /** 查询点赞状态 + 点赞数 */
  getLikeStatus(slug) {
    return request.get(`/posts/${slug}/like`)
  },

  // ========== 收藏 ==========

  /** 收藏/取消收藏 */
  toggleBookmark(slug) {
    return request.post(`/posts/${slug}/bookmark`)
  },

  /** 查询收藏状态 */
  getBookmarkStatus(slug) {
    return request.get(`/posts/${slug}/bookmark`)
  },

  /** 获取当前访客的收藏列表 */
  getBookmarks() {
    return request.get('/bookmarks')
  },
}
