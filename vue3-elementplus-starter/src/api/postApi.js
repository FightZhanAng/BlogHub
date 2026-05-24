import request from './request'

/**
 * 文章 API
 */
export const postApi = {
  /** 文章列表（分页） */
  list(params) {
    return request.get('/posts', { params })
  },

  /** 文章详情（按 slug） */
  detail(slug) {
    return request.get(`/posts/${slug}`)
  },

  /** 创建文章 */
  create(data) {
    return request.post('/posts', data)
  },

  /** 更新文章 */
  update(id, data) {
    return request.put(`/posts/${id}`, data)
  },

  /** 删除文章 */
  delete(id) {
    return request.delete(`/posts/${id}`)
  },

  /** 点赞/取消点赞 */
  toggleLike(slug) {
    return request.post(`/posts/${slug}/like`)
  },

  /** 查询点赞状态 */
  getLikeStatus(slug) {
    return request.get(`/posts/${slug}/like`)
  },

  /** 收藏/取消收藏 */
  toggleBookmark(slug) {
    return request.post(`/posts/${slug}/bookmark`)
  },

  /** 查询收藏状态 */
  getBookmarkStatus(slug) {
    return request.get(`/posts/${slug}/bookmark`)
  },

  /** 获取评论列表 */
  getComments(slug) {
    return request.get(`/posts/${slug}/comments`)
  },

  /** 发表评论 */
  addComment(slug, data) {
    return request.post(`/posts/${slug}/comments`, data)
  },
}
