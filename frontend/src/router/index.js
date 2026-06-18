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
      },
      // V4: 真实 AI 对接演示
      {
        path: 'showcase/battle',
        name: 'MiniMaxShowcase',
        component: () => import('@/views/showcase/MiniMaxShowcase.vue'),
        meta: { title: '多模型对决', icon: 'Aim' }
      },
      {
        path: 'showcase/vision',
        name: 'VisionShowcase',
        component: () => import('@/views/showcase/VisionShowcase.vue'),
        meta: { title: '视觉对决', icon: 'View' }
      },
      {
        path: 'showcase/playground',
        name: 'SingleChatPlayground',
        component: () => import('@/views/showcase/SingleChatPlayground.vue'),
        meta: { title: '单模型 PlayGround', icon: 'Lightning' }
      },
      {
        path: 'showcase/imagegen',
        name: 'ImageGenShowcase',
        component: () => import('@/views/showcase/ImageGenShowcase.vue'),
        meta: { title: '文生图', icon: 'Picture' }
      },
      {
        path: 'showcase/audio',
        name: 'AudioShowcase',
        component: () => import('@/views/showcase/AudioShowcase.vue'),
        meta: { title: '语音能力 (ASR/TTS)', icon: 'Microphone' }
      },
      {
        path: 'showcase/leaderboard',
        name: 'LeaderboardShowcase',
        component: () => import('@/views/showcase/LeaderboardShowcase.vue'),
        meta: { title: '模型排行榜', icon: 'Trophy' }
      },
      {
        path: 'showcase/plugins',
        name: 'PluginShowcase',
        component: () => import('@/views/showcase/PluginShowcase.vue'),
        meta: { title: '插件 SDK', icon: 'Connection' }
      },
      {
        path: 'showcase/stream',
        name: 'StreamShowcase',
        component: () => import('@/views/showcase/StreamShowcase.vue'),
        meta: { title: 'WebSocket 流式', icon: 'Connection' }
      },
      {
        path: 'showcase/dag',
        name: 'DagShowcase',
        component: () => import('@/views/showcase/DagShowcase.vue'),
        meta: { title: 'Agent DAG', icon: 'Share' }
      },
      {
        path: 'showcase/videogen',
        name: 'VideoGenShowcase',
        component: () => import('@/views/showcase/VideoGenShowcase.vue'),
        meta: { title: '文生视频', icon: 'VideoCamera' }
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
