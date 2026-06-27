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
    path: '/h5-login',
    name: 'H5Login',
    component: () => import('@/views/auth/H5Login.vue'),
    meta: { public: true, title: 'H5 跨平台登录' }
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
        path: 'chat/stream',
        name: 'ChatStream',
        component: () => import('@/views/chat/Stream.vue'),
        meta: { title: '双向流聊天 (V5.19)', icon: 'ChatLineRound' }
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
      // Agent 路由 (V5.16-V5.17, V5.22: 修复路由缺 Layout 包装导致页面跳转失败)
      {
        path: 'agent',
        name: 'Agent',
        component: () => import('@/views/agent/Index.vue'),
        meta: { title: 'Agent 自主任务', icon: 'MagicStick' }
      },
      {
        path: 'agent/stream',
        name: 'AgentStream',
        component: () => import('@/views/agent/Stream.vue'),
        meta: { title: 'Agent 流式 (V5.16)', icon: 'VideoPlay' }
      },
      {
        path: 'agent/multi',
        name: 'AgentMulti',
        component: () => import('@/views/agent/Multi.vue'),
        meta: { title: '多智能体 (V5.17)', icon: 'Connection' }
      },
      {
        path: 'notification',
        name: 'Notification',
        component: () => import('@/views/notification/Index.vue'),
        meta: { title: '通知中心', icon: 'Bell' }
      },
      {
        path: 'prompts',
        name: 'Prompts',
        component: () => import('@/views/prompts/Index.vue'),
        meta: { title: 'Prompt 模板', icon: 'DocumentCopy' }
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
          {
            path: 'metrics',
            name: 'AdminMetrics',
            component: () => import('@/views/admin/Metrics.vue'),
            meta: { title: '实时指标 (V5.10)', icon: 'TrendCharts' }
          },
          {
            path: 'traces',
            name: 'AdminTraces',
            component: () => import('@/views/admin/Traces.vue'),
            meta: { title: '分布式追踪 (V5.14)', icon: 'Connection' }
          },
          {
            path: 'monitor',
            name: 'AdminMonitor',
            component: () => import('@/views/monitor/Index.vue'),
            meta: { title: '系统监控', icon: 'Monitor' }
          },
          {
            path: 'provider',
            name: 'AdminProvider',
            component: () => import('@/views/admin/Provider.vue'),
            meta: { title: '模型 Provider (V5.24)', icon: 'Cpu' }
          },
          {
            path: 'leaderboard',
            name: 'AdminLeaderboard',
            component: () => import('@/views/admin/Leaderboard.vue'),
            meta: { title: '模型排行榜 (V5.24)', icon: 'Trophy' }
          },
          {
            path: 'apikey-stats',
            name: 'AdminApiKeyStats',
            component: () => import('@/views/admin/ApiKeyStats.vue'),
            meta: { title: 'API Key 配额统计 (Day 20)', icon: 'Key' }
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
      // V1: Day 23 模型训练控制台
      {
        path: 'training',
        name: 'TrainingConsole',
        component: () => import('@/views/training/Console.vue'),
        meta: { title: '模型训练', icon: 'Cpu' }
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
      },
      // V5: 微信扫码登录演示 + 我的绑定
      {
        path: 'wechat',
        name: 'WechatScan',
        component: () => import('@/views/auth/WechatScanPage.vue'),
        meta: { title: '微信扫码登录', icon: 'ChatDotRound', public: true }
      },
      {
        path: 'profile/wechat',
        name: 'MyWechat',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '我的微信' }
      },
      {
        path: 'profile/wechat/cross',
        name: 'CrossAppBinding',
        component: () => import('@/views/user/CrossAppBinding.vue'),
        meta: { title: '跨应用绑定 (unionid)' }
      },
      {
        path: 'admin/wechat/unionid',
        name: 'WechatUnionidAdmin',
        component: () => import('@/views/admin/WechatUnionidAdmin.vue'),
        meta: { title: 'unionid 跨应用管理', icon: 'Connection', requiresSuper: true }
      },
      {
        path: 'admin/wechat',
        name: 'WechatBindings',
        component: () => import('@/views/admin/WechatBindings.vue'),
        meta: { title: '微信绑定管理', icon: 'ChatDotRound', requiresSuper: true }
      },
      {
        path: 'tenant',
        name: 'TenantMgmt',
        component: () => import('@/views/tenant/Index.vue'),
        meta: { title: '租户管理', icon: 'Office', requiresSuper: true }
      },
      // V1.8: V5.31 数据分析 (analytics)
      {
        path: 'analytics/datasource',
        name: 'AnalyticsDataSource',
        component: () => import('@/views/analytics/DataSource.vue'),
        meta: { title: '数据源管理', icon: 'Coin' }
      },
      {
        path: 'analytics/schema',
        name: 'AnalyticsSchema',
        component: () => import('@/views/analytics/Schema.vue'),
        meta: { title: 'Schema 浏览', icon: 'Files' }
      },
      {
        path: 'analytics/nlsql',
        name: 'AnalyticsNl2Sql',
        component: () => import('@/views/analytics/Nl2Sql.vue'),
        meta: { title: 'NL2SQL 实验室', icon: 'ChatLineRound' }
      },
      {
        path: 'analytics/ingest',
        name: 'AnalyticsIngest',
        component: () => import('@/views/analytics/Ingest.vue'),
        meta: { title: '文件导入', icon: 'UploadFilled' }
      },
      {
        path: 'analytics/reports',
        name: 'AnalyticsReports',
        component: () => import('@/views/analytics/Reports.vue'),
        meta: { title: '报告中心', icon: 'DataAnalysis' }
      },
      // V1.8: V5.32 画布工作流 (pipeline)
      {
        path: 'pipeline',
        name: 'PipelineList',
        component: () => import('@/views/pipeline/Index.vue'),
        meta: { title: '工作流列表', icon: 'Connection' }
      },
      {
        path: 'pipeline/designer',
        name: 'PipelineDesigner',
        component: () => import('@/views/pipeline/Designer.vue'),
        meta: { title: '画布设计器', icon: 'EditPen' }
      },
      {
        path: 'pipeline/runs',
        name: 'PipelineRuns',
        component: () => import('@/views/pipeline/RunMonitor.vue'),
        meta: { title: '运行监控', icon: 'Monitor' }
      },
      {
        path: 'pipeline/runs/:id',
        name: 'PipelineWorkflowRuns',
        component: () => import('@/views/pipeline/RunMonitor.vue'),
        meta: { title: '工作流运行历史', hidden: true }
      },
      // V5.33: API Key 管理
      {
        path: 'apikey',
        name: 'ApiKeyMgmt',
        component: () => import('@/views/apikey/Index.vue'),
        meta: { title: 'API Key', icon: 'Key' }
      },
      // V5.9 Day 20: API Key 用量统计
      {
        path: 'apikey-stats',
        name: 'ApiKeyStats',
        component: () => import('@/views/apikey/Stats.vue'),
        meta: { title: 'Key 统计', icon: 'DataLine', requiresSuper: true }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
  {
    // V3.2: 移动端 H5 路由 (/mobile/*)
    path: '/mobile',
    component: () => import('@/views/mobile/Index.vue'),
    redirect: '/mobile/chat',
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
