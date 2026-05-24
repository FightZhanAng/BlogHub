import request from './request'

/**
 * 标签 API
 */
export const tagApi = {
  /** 标签列表（全部，按文章数降序） */
  list() {
    return request.get('/tags')
  },

  /** 标签详情 */
  detail(slug) {
    return request.get(`/tags/${slug}`)
  },

  /** 标签下的文章 */
  posts(slug, params) {
    return request.get(`/tags/${slug}/posts`, { params })
  },
}
