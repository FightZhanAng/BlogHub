import { ref, computed } from 'vue'
import { postApi } from '@/api/postApi'
import { markdownToHtml } from './useMarkdown'

/** 将 API 返回的 post 转换为前端需要的格式 */
function formatPost(raw) {
  if (!raw) {
    console.warn('[formatPost] raw is null/undefined')
    return null
  }
  const markdown = raw.content || ''
  let html = ''
  try {
    const rendered = markdownToHtml(markdown)
    html = rendered.html || ''
  } catch (e) {
    console.error('[formatPost] markdown 渲染失败:', e)
    // fallback: 按换行符转义显示纯文本
    html = '<pre style="white-space:pre-wrap;background:var(--color-bg-warm);padding:16px;border-radius:8px">'
         + markdown.replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</pre>'
  }
  if (!html && markdown) {
    // markdown-it 返回空但内容不为空 — 纯文本 fallback
    html = '<pre style="white-space:pre-wrap;background:var(--color-bg-warm);padding:16px;border-radius:8px">'
         + markdown.replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</pre>'
  }
  return {
    id: raw.id,
    slug: raw.slug,
    title: raw.title || raw.slug,
    date: raw.createdAt ? raw.createdAt.slice(0, 10) : '',
    tags: raw.tags ? raw.tags.split(',').map((s) => s.trim()).filter(Boolean) : [],
    description: raw.description || '',
    html,
    content: markdown,
    views: raw.views || 0,
    likes: raw.likes || 0,
    author: raw.authorName || '',
    authorId: raw.authorId || null,
    authorAvatar: raw.authorAvatar || '',
    coverImage: raw.coverImage || '',
    isPinned: raw.isPinned || 0,
    isPrivate: raw.isPrivate || 0,
    isHidden: raw.isHidden || 0,
  }
}

/**
 * 文章数据（从后端 API 获取）
 */
export function usePosts() {
  const posts = ref([])
  const loading = ref(false)
  const error = ref(null)

  /** 加载文章列表 */
  async function fetchPosts(tag, keyword) {
    loading.value = true
    error.value = null
    try {
      const params = { page: 1, size: 50 }
      if (tag) params.tag = tag
      if (keyword) params.keyword = keyword
      const res = await postApi.list(params)
      // res 是 PageResult { list, total, page, size }
      const list = res.list || []
      posts.value = list.map(formatPost)
    } catch (e) {
      error.value = e.message || '加载文章失败'
      posts.value = []
    } finally {
      loading.value = false
    }
  }

  /** 获取单篇文章 */
  async function getPost(slug) {
    loading.value = true
    error.value = null
    try {
      const raw = await postApi.detail(slug)
      console.log('[usePosts] API raw data:', raw)
      const post = formatPost(raw)
      return post
    } catch (e) {
      error.value = e.message || '加载文章失败'
      return null
    } finally {
      loading.value = false
    }
  }

  /** 获取所有标签 */
  function getTags() {
    const tagSet = new Set()
    posts.value.forEach((p) => p.tags.forEach((t) => tagSet.add(t)))
    return [...tagSet]
  }

  return { posts, loading, error, fetchPosts, getPost, getTags }
}
