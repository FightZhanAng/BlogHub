import request from '@/api/request'

export const menuSettingsApi = {
  // ========== 菜单组 ==========
  getGroups() {
    return request.get('/menus/groups')
  },
  createGroup(data) {
    return request.post('/menus/groups', data)
  },
  updateGroup(id, data) {
    return request.put(`/menus/groups/${id}`, data)
  },
  deleteGroup(id) {
    return request.delete(`/menus/groups/${id}`)
  },
  sortGroups(ids) {
    return request.put('/menus/groups/sort', { ids })
  },

  // ========== 菜单项 ==========
  getItems(groupId) {
    return request.get('/menus/items', { params: { groupId } })
  },
  getAllItems() {
    return request.get('/menus/items')
  },
  createItem(data) {
    return request.post('/menus/items', data)
  },
  updateItem(id, data) {
    return request.put(`/menus/items/${id}`, data)
  },
  deleteItem(id) {
    return request.delete(`/menus/items/${id}`)
  },
  sortItems(ids) {
    return request.put('/menus/items/sort', { ids })
  },
  sortAll(entries) {
    return request.put('/menus/sort-all', { entries })
  },
}
