import request from './request'

export const albumApi = {
  // 相册
  list(params) {
    return request.get('/albums', { params })
  },
  detail(id) {
    return request.get(`/albums/${id}`)
  },
  create(data) {
    return request.post('/albums', data)
  },
  update(id, data) {
    return request.put(`/albums/${id}`, data)
  },
  delete(id) {
    return request.delete(`/albums/${id}`)
  },

  // 照片
  listPhotos(albumId, params) {
    return request.get(`/albums/${albumId}/photos`, { params })
  },
  uploadPhotos(albumId, files) {
    const formData = new FormData()
    files.forEach(f => formData.append('files', f))
    return request.post(`/albums/${albumId}/photos`, formData, {
      headers: { 'Content-Type': undefined }
    })
  },
  updatePhoto(id, data) {
    return request.put(`/albums/photos/${id}`, null, { params: data })
  },
  deletePhoto(id) {
    return request.delete(`/albums/photos/${id}`)
  },

  // 时间线
  timeline(albumId) {
    return request.get(`/albums/${albumId}/timeline`)
  },
}
