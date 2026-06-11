import { ref, computed } from 'vue'
import { interactionApi } from '@/api/interactionApi'
import { commentApi } from '@/api/commentApi'

/**
 * 点赞功能
 */
export function useLikes(slug) {
  const liked = ref(false)
  const count = ref(0)

  /** 加载点赞状态 */
  async function fetchStatus() {
    try {
      const res = await interactionApi.getLikeStatus(slug)
      liked.value = res.liked
      count.value = res.count
    } catch {
      // 静默失败
    }
  }

  /** 切换点赞 */
  async function toggleLike() {
    try {
      const res = await interactionApi.toggleLike(slug)
      liked.value = res.liked
      count.value = res.count
    } catch {
      // 静默失败
    }
  }

  // 立即加载
  fetchStatus()

  return { liked, count, toggleLike }
}

/**
 * 收藏功能
 */
export function useBookmarks(slug) {
  const allBookmarks = ref([])
  const bookmarked = computed(() => slug ? allBookmarks.value.includes(slug) : false)
  const bookmarksList = computed(() => allBookmarks.value)

  /** 加载收藏列表 */
  async function fetchBookmarks() {
    try {
      const res = await interactionApi.getBookmarks()
      allBookmarks.value = (res || []).map((p) => p.slug)
    } catch {
      // 静默失败
    }
  }

  /** 切换收藏 */
  async function toggleBookmark() {
    try {
      const res = await interactionApi.toggleBookmark(slug)
      if (res.bookmarked) {
        if (!allBookmarks.value.includes(slug)) {
          allBookmarks.value.push(slug)
        }
      } else {
        allBookmarks.value = allBookmarks.value.filter((s) => s !== slug)
      }
    } catch {
      // 静默失败
    }
  }

  function isBookmarked(s) {
    return allBookmarks.value.includes(s)
  }

  // 立即加载
  fetchBookmarks()

  return { bookmarked, bookmarksList, toggleBookmark, isBookmarked }
}

/**
 * 评论功能
 */
export function useComments(slug) {
  const comments = ref([])

  /** 加载评论 */
  async function fetchComments() {
    try {
      const res = await commentApi.list(slug)
      comments.value = res || []
    } catch {
      comments.value = []
    }
  }

  /** 发表评论 */
  async function addComment(nickname, content) {
    if (!nickname.trim() || !content.trim()) return
    try {
      await commentApi.create(slug, { nickname, content })
      // 重新加载
      await fetchComments()
    } catch {
      // 错误已在拦截器提示
    }
  }

  // 立即加载
  fetchComments()

  return { comments, addComment, fetchComments }
}

/**
 * 阅读统计（前端计数 + 后端计数）
 */
export function useReadingStats(slug) {
  const views = ref(0)

  async function recordView() {
    try {
      // 后端在 GET /api/posts/:slug 时自动 +1 views
      // 所以这里不需要额外请求
      const res = await interactionApi.getLikeStatus(slug)
      // 不需要 views，通过 post detail 获取
    } catch {
      // 静默
    }
  }

  return { recordView, views }
}
