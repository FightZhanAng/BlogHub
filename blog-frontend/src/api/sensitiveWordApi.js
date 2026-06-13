import request from './request'

export const sensitiveWordApi = {
  list(page = 1, size = 20, keyword = '') {
    return request.get('/admin/sensitive-words', { params: { page, size, keyword } })
  },
  add(word) {
    return request.post('/admin/sensitive-words', word)
  },
  update(id, word) {
    return request.put(`/admin/sensitive-words/${id}`, word)
  },
  delete(id) {
    return request.delete(`/admin/sensitive-words/${id}`)
  },
  batchAdd(words) {
    return request.post('/admin/sensitive-words/batch', words)
  },
  reload() {
    return request.post('/admin/sensitive-words/reload')
  },
}
