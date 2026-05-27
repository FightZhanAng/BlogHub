import request from './request'

export const hotTopicApi = {
  getTodayTopics() {
    return request.get('/hot-topics')
  },
  getByPlatform(platform) {
    return request.get(`/hot-topics/${platform}`)
  },
  refresh() {
    return request.post('/hot-topics/refresh')
  },
}
