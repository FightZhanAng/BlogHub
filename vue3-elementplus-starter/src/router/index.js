import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        redirect: '/home',
      },
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'user/:id',
        name: 'UserProfile',
        component: () => import('@/views/UserProfile.vue'),
        meta: { title: '用户主页' },
      },
      {
        path: 'search',
        name: 'SearchResults',
        component: () => import('@/views/SearchResults.vue'),
        meta: { title: '搜索' },
      },
      {
        path: 'archive',
        name: 'Archive',
        component: () => import('@/views/Archive.vue'),
        meta: { title: '文章归档' },
      },
      {
        path: 'tags',
        name: 'TagCloud',
        component: () => import('@/views/TagCloud.vue'),
        meta: { title: '标签云' },
      },
      {
        path: 'tags/:slug',
        redirect: (to) => ({ path: '/blog', query: { tag: to.params.slug } }),
      },
      {
        path: 'about',
        name: 'About',
        component: () => import('@/views/About.vue'),
        meta: { title: '关于' },
      },
      {
        path: 'blog',
        name: 'BlogList',
        component: () => import('@/views/BlogList.vue'),
        meta: { title: '博客' },
      },
      {
        path: 'blog/new',
        name: 'PostEditor',
        component: () => import('@/views/PostEditor.vue'),
        meta: { title: '写文章', requiresAuth: true },
      },
      {
        path: 'blog/:slug/edit',
        name: 'PostEdit',
        component: () => import('@/views/PostEdit.vue'),
        meta: { title: '编辑文章', requiresAuth: true },
      },
      {
        path: 'blog/:slug',
        name: 'BlogPost',
        component: () => import('@/views/BlogPost.vue'),
        meta: { title: '文章详情' },
      },
      {
        path: 'bookmarks',
        name: 'Bookmarks',
        component: () => import('@/views/Bookmarks.vue'),
        meta: { title: '我的收藏' },
      },
      {
        path: 'albums',
        name: 'AlbumList',
        component: () => import('@/views/AlbumList.vue'),
        meta: { title: '宝宝相册', requiresAuth: true },
      },
      {
        path: 'albums/:id',
        name: 'AlbumDetail',
        component: () => import('@/views/AlbumDetail.vue'),
        meta: { title: '相册详情', requiresAuth: true },
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/views/UserManage.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true },
      },
      {
        path: 'my-posts',
        name: 'MyPosts',
        component: () => import('@/views/MyPosts.vue'),
        meta: { title: '我的文章', requiresAuth: true },
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', requiresAuth: true, roles: ['admin'] },
      },
      {
        path: 'logs',
        name: 'LogManage',
        component: () => import('@/views/LogManage.vue'),
        meta: { title: '操作日志', requiresAuth: true, roles: ['admin'] },
      },
      {
        path: 'comments',
        name: 'CommentManage',
        component: () => import('@/views/CommentManage.vue'),
        meta: { title: '评论管理', requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'images',
        name: 'ImageManage',
        component: () => import('@/views/ImageManage.vue'),
        meta: { title: '图片管理', requiresAuth: true, roles: ['admin'] },
      },
      {
        path: 'hot',
        name: 'HotTopics',
        component: () => import('@/views/HotTopics.vue'),
        meta: { title: '每日热点' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫 — SEO + 认证检查
router.beforeEach((to, from, next) => {
  // 页面标题
  let title = to.meta.title
  if (to.params.slug && to.name === 'BlogPost') {
    title = '文章详情'
  }
  document.title = title
    ? `${title} | BlogHub`
    : 'BlogHub'

  // Meta 描述
  const descMap = {
    '首页': 'BlogHub - 一个简约现代的技术博客平台',
    '博客': '浏览所有博客文章',
    '标签云': '按标签浏览文章',
    '文章归档': '按时间线浏览所有文章',
    '搜索': '搜索博客文章',
  }
  let descEl = document.querySelector('meta[name="description"]')
  if (!descEl) {
    descEl = document.createElement('meta')
    descEl.name = 'description'
    document.head.appendChild(descEl)
  }
  descEl.content = descMap[title] || 'BlogHub - 技术博客平台'

  const token = localStorage.getItem('blog_token')
  const role = localStorage.getItem('blog_role')

  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin && role !== 'admin') {
    next('/')
    return
  }

  if (to.path === '/login' && token) {
    next('/')
    return
  }

  next()
})

export default router
