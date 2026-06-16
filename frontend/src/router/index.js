import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/Index.vue'),
    redirect: '/chat',
    children: [
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/chat/Index.vue'),
        meta: { title: '对话', icon: 'ChatDotRound' }
      },
      {
        path: 'chat/:sessionId',
        name: 'ChatSession',
        component: () => import('@/views/chat/Index.vue'),
        meta: { title: '对话', icon: 'ChatDotRound' }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/Index.vue'),
        meta: { title: '知识库', icon: 'Files' }
      },
      {
        path: 'memory',
        name: 'Memory',
        component: () => import('@/views/memory/Index.vue'),
        meta: { title: '记忆', icon: 'Memory' }
      },
      {
        path: 'admin',
        name: 'Admin',
        component: () => import('@/views/admin/Index.vue'),
        meta: { title: '管理后台', icon: 'Setting' },
        children: [
          {
            path: '',
            name: 'AdminDashboard',
            component: () => import('@/views/admin/Dashboard.vue'),
            meta: { title: '仪表盘', icon: 'DataLine' }
          },
        ]
      },
      {
        path: 'about',
        name: 'About',
        component: () => import('@/views/About.vue'),
        meta: { title: '关于', icon: 'InfoFilled' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - MiniMax` : 'MiniMax 大模型平台'
  const userStore = useUserStore()
  if (!to.meta.public && !userStore.isLogin) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.name === 'Login' && userStore.isLogin) {
    next({ path: '/' })
  } else {
    next()
  }
})

export default router
