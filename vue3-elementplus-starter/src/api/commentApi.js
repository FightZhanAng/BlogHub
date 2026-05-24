import request from './request'

/**
 * 评论 API
 */
export const commentApi = {
  /** 获取文章评论 */
  list(slug) {
    return request.get(`/posts/${slug}/comments`)
  },

  /** 发表评论 */
  create(slug, data) {
    return request.post(`/posts/${slug}/comments`, data)
  },
}
