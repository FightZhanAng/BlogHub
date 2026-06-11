import { ref } from 'vue'
import { defineStore } from 'pinia'
import request from '@/api/request'

export const useMenuStore = defineStore('menu', () => {
  const menuTree = ref([])
  const loading = ref(false)

  async function fetchMenuTree() {
    loading.value = true
    try {
      const res = await request.get('/menus/tree')
      menuTree.value = res || []
    } catch (e) {
      console.error('加载菜单失败', e)
    } finally {
      loading.value = false
    }
  }

  function clearMenu() {
    menuTree.value = []
  }

  return { menuTree, loading, fetchMenuTree, clearMenu }
})
