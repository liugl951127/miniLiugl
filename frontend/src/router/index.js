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
        path: 'agent',
        name: 'Agent',
        component: () => import('@/views/agent/Index.vue'),
        meta: { title: 'Agent 自主任务', icon: 'MagicStick' }
      },
      {
        path: 'kg',
        name: 'KnowledgeGraph',
        component: () => import('@/views/kg/Index.vue'),
        meta: { title: '知识图谱', icon: 'Share' }
      },
      {
        path: 'collab',
        name: 'Collab',
        component: () => import('@/views/collab/Index.vue'),
        meta: { title: '实时协作', icon: 'UserFilled' }
      },
      {
        path: 'plugins',
        name: 'Plugins',
        component: () => import('@/views/plugins/Index.vue'),
        meta: { title: '插件市场', icon: 'Grid' }
      },
      {
        path: 'super',
        name: 'SuperAdmin',
        component: () => import('@/views/super/Index.vue'),
        meta: { title: '超级管理 (adminLiugl)', icon: 'Key', requiresSuper: true }
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
  { path: '/:pathMatch(.*)*', redirect: '/' },
  {
    // V3.2: 移动端 H5 路由 (/m/*)
    path: '/m',
    component: () => import('@/views/mobile/Index.vue'),
    redirect: '/m/chat',
    children: [
      {
        path: 'chat',
        name: 'MChat',
        component: () => import('@/views/mobile/Chat.vue'),
        meta: { title: '对话', public: false }
      },
      {
        path: 'agent',
        name: 'MAgent',
        component: () => import('@/views/mobile/Agent.vue'),
        meta: { title: 'Agent' }
      },
      {
        path: 'kg',
        name: 'MKg',
        component: () => import('@/views/mobile/Kg.vue'),
        meta: { title: '知识图谱' }
      },
      {
        path: 'plugins',
        name: 'MPlugins',
        component: () => import('@/views/mobile/Plugins.vue'),
        meta: { title: '插件' }
      },
      {
        path: 'me',
        name: 'MMe',
        component: () => import('@/views/mobile/Me.vue'),
        meta: { title: '我的' }
      },
    ]
  },
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
  } else if (to.meta.requiresSuper && !userStore.isSuperAdmin) {
    // 需要超级管理员 (adminLiugl) 才能访问
    next({ path: '/', query: { error: 'need_super_admin' } })
  } else {
    next()
  }
})

export default router
