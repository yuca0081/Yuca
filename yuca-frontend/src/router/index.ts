import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页', requireAuth: true }
  },
  {
    path: '/blog',
    name: 'Blog',
    component: () => import('@/views/Blog.vue'),
    meta: { title: '博客', requireAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Auth.vue'),
    meta: { title: '登录', requireAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Auth.vue'),
    meta: { title: '注册', requireAuth: false }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人资料', requireAuth: true }
  },
  {
    path: '/tools',
    name: 'Tools',
    component: () => import('@/views/Tools.vue'),
    meta: { title: '工具', requireAuth: true }
  },
  {
    path: '/tools/json',
    name: 'JsonTool',
    component: () => import('@/views/tools/JsonTool.vue'),
    meta: { title: 'JSON 格式化', requireAuth: true }
  },
  {
    path: '/tools/jwt',
    name: 'JwtTool',
    component: () => import('@/views/tools/JwtTool.vue'),
    meta: { title: 'JWT 加解密', requireAuth: true }
  },
  {
    path: '/tools/base64',
    name: 'Base64Tool',
    component: () => import('@/views/tools/Base64Tool.vue'),
    meta: { title: 'Base64 编解码', requireAuth: true }
  },
  {
    path: '/tools/url',
    name: 'UrlTool',
    component: () => import('@/views/tools/UrlTool.vue'),
    meta: { title: 'URL 编解码', requireAuth: true }
  },
  {
    path: '/tools/timestamp',
    name: 'TimestampTool',
    component: () => import('@/views/tools/TimestampTool.vue'),
    meta: { title: '时间戳转换', requireAuth: true }
  },
  {
    path: '/tools/hash',
    name: 'HashTool',
    component: () => import('@/views/tools/HashTool.vue'),
    meta: { title: '哈希生成', requireAuth: true }
  },
  {
    path: '/tools/regex',
    name: 'RegexTool',
    component: () => import('@/views/tools/RegexTool.vue'),
    meta: { title: '正则表达式测试', requireAuth: true }
  },
  {
    path: '/tools/uuid',
    name: 'UuidTool',
    component: () => import('@/views/tools/UuidTool.vue'),
    meta: { title: 'UUID 生成器', requireAuth: true }
  },
  {
    path: '/notes',
    name: 'Notes',
    component: () => import('@/views/Notes.vue'),
    meta: { title: '笔记', requireAuth: true }
  },
  {
    path: '/wiki',
    name: 'Wiki',
    component: () => import('@/views/Wiki.vue'),
    meta: { title: '知识库', requireAuth: true }
  },
  {
    path: '/assistant',
    name: 'Assistant',
    component: () => import('@/views/Assistant.vue'),
    meta: { title: '小助手', requireAuth: true }
  },
  {
    path: '/icon-test',
    name: 'IconTest',
    component: () => import('@/components/IconTest.vue'),
    meta: { title: '图标测试', requireAuth: false }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫：实现登录验证
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || 'Yuca'} - 个人主页`

  const userStore = useUserStore()
  const requireAuth = to.meta.requireAuth

  if (requireAuth && !userStore.isLoggedIn()) {
    // 需要登录但未登录，跳转到登录页
    next({
      name: 'Login',
      query: { redirect: to.fullPath }
    })
  } else {
    next()
  }
})

export default router
