import request from './request'

/**
 * 评论 API
 */
export const commentApi = {
  /** 获取文章评论 */
  list(slug) {
    return request.get(`/posts/${slug}/comments`)
  },

  /** 懒加载某条评论的回复 */
  getReplies(commentId, page = 1, size = 10) {
    return request.get(`/comments/${commentId}/replies`, { params: { page, size } })
  },

  /** 发表评论 */
  create(slug, data) {
    return request.post(`/posts/${slug}/comments`, data)
  },

  /** 点赞/点踩评论 */
  react(commentId, type) {
    return request.post(`/comments/${commentId}/reaction`, { type })
  },

  /** 取消评论反应 */
  removeReaction(commentId) {
    return request.delete(`/comments/${commentId}/reaction`)
  },

  /** 获取评论反应统计 */
  getReactions(commentId) {
    return request.get(`/comments/${commentId}/reactions`)
  },
}
