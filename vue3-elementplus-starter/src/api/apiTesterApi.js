import request from './request'

export const apiTesterApi = {
  // ========== 代理请求 ==========
  /** 发送代理请求 */
  proxy(data) {
    return request.post('/tester/proxy', data)
  },

  // ========== 环境变量 ==========
  /** 获取环境变量列表 */
  getEnvs() {
    return request.get('/tester/env')
  },

  /** 创建环境变量 */
  createEnv(data) {
    return request.post('/tester/env', data)
  },

  /** 更新环境变量 */
  updateEnv(id, data) {
    return request.put(`/tester/env/${id}`, data)
  },

  /** 删除环境变量 */
  deleteEnv(id) {
    return request.delete(`/tester/env/${id}`)
  },

  // ========== 集合管理 ==========
  /** 获取集合列表 */
  getCollections() {
    return request.get('/tester/collections')
  },

  /** 创建集合 */
  createCollection(data) {
    return request.post('/tester/collections', data)
  },

  /** 更新集合 */
  updateCollection(id, data) {
    return request.put(`/tester/collections/${id}`, data)
  },

  /** 删除集合 */
  deleteCollection(id) {
    return request.delete(`/tester/collections/${id}`)
  },

  // ========== 请求管理 ==========
  /** 获取集合下的请求列表 */
  getRequests(collectionId) {
    return request.get(`/tester/collections/${collectionId}/requests`)
  },

  /** 保存请求 */
  saveRequest(data) {
    return request.post('/tester/requests', data)
  },

  /** 更新请求 */
  updateRequest(id, data) {
    return request.put(`/tester/requests/${id}`, data)
  },

  /** 删除请求 */
  deleteRequest(id) {
    return request.delete(`/tester/requests/${id}`)
  },

  // ========== 历史记录 ==========
  /** 获取历史记录列表 */
  getHistory(page = 1, size = 20) {
    return request.get('/tester/history', { params: { page, size } })
  },

  /** 删除单条历史记录 */
  deleteHistory(id) {
    return request.delete(`/tester/history/${id}`)
  },

  /** 清空历史记录 */
  clearHistory() {
    return request.delete('/tester/history')
  }
}
