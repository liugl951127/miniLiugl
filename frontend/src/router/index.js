/**
 * @file router/index.js - Vue Router 配置 (V6.9 重构版)
 *
 * V6.9: 合并系统管理路由为 /settings，移除冗余子路由
 * 所有路由使用懒加载，守卫: JWT token 校验 (meta.public = true 跳过)
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  // ─── 公开路由 ───
  {
    path: '/login', name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/h5-login', name: 'H5Login',
    component: () => import('@/views/auth/H5Login.vue'),
    meta: { public: true, title: 'H5 跨平台登录' }
  },

  // ─── 受保护路由 (均包裹 layout) ───
  {
    path: '/',
    component: () => import('@/layout/Index.vue'),
    redirect: '/chat',
    children: [
      // ── AI 对话 ──
      { path: 'chat', name: 'Chat', component: () => import('@/views/chat/Index.vue'), meta: { title: '智能对话' } },
      { path: 'chat/stream', name: 'ChatStream', component: () => import('@/views/chat/Stream.vue'), meta: { title: '流式对话' } },
      { path: 'chat/:sessionId', name: 'ChatSession', component: () => import('@/views/chat/Index.vue'), meta: { title: '对话' } },

      // ── 知识中心 (单页 tab) ──
      {
        path: 'knowledge',
        component: () => import('@/views/knowledge/Index.vue'),
        meta: { title: '知识中心' },
        children: [
          { path: '',           name: 'KnowledgeHome',   redirect: 'knowledge/list' },
          { path: 'list',       name: 'KnowledgeList',   component: () => import('@/views/knowledge/KbList.vue'),  meta: { title: '知识库' } },
          { path: 'kg',         name: 'KnowledgeKg',     component: () => import('@/views/knowledge/Kg.vue'),     meta: { title: '知识图谱' } },
          { path: 'memory',     name: 'KnowledgeMemory', component: () => import('@/views/knowledge/Memory.vue'), meta: { title: '记忆中心' } }
        ]
      },
      { path: 'kg',     name: 'KgLegacy',     redirect: '/knowledge/kg' },
      { path: 'memory', name: 'MemoryLegacy', redirect: '/knowledge/memory' },

      // ── Agent 编排 (单页 tab) ──
      { path: 'agent', name: 'Agent', component: () => import('@/views/agent/Index.vue'), meta: { title: 'Agent 编排' } },
      { path: 'agent/stream', name: 'AgentStream', redirect: to => ({ path: '/agent', query: { tab: 'stream' } }) },
      { path: 'agent/canvas', name: 'AgentCanvas', redirect: to => ({ path: '/agent', query: { tab: 'canvas' } }) },
      { path: 'agent/multi', name: 'AgentMulti', redirect: to => ({ path: '/agent', query: { tab: 'multi' } }) },
      { path: 'agent/training', name: 'AgentTraining', redirect: to => ({ path: '/agent', query: { tab: 'training' } }) },
      { path: 'agent/approval', name: 'AgentApproval', redirect: to => ({ path: '/agent', query: { tab: 'approval' } }) },
      { path: 'agent-auto', name: 'AgentAuto', component: () => import('@/views/agent/Auto.vue'), meta: { title: '智能体群生成', icon: 'MagicStick' } },
      { path: 'agent/group-designer', name: 'AgentGroupDesigner', component: () => import('@/views/agent/GroupDesigner.vue'), meta: { title: '智能体群编排', icon: 'Connection', roles: ['user', 'admin'] } },

      // ── 模型与服务 ──
      { path: 'model', name: 'Model', component: () => import('@/views/model/Index.vue'), meta: { title: '模型管理' } },
      { path: 'function', name: 'Function', component: () => import('@/views/function/Index.vue'), meta: { title: 'Function 工具' } },
      { path: 'multimodal', name: 'Multimodal', component: () => import('@/views/multimodal/Index.vue'), meta: { title: '多模态' } },
      { path: 'multimodal/local', name: 'MultimodalLocal', component: () => import('@/views/multimodal/LocalOnnx.vue'), meta: { title: '本地多模态 (ONNX)' } },
      { path: 'training', name: 'Training', component: () => import('@/views/training/Console.vue'), meta: { title: '模型训练' } },
      { path: 'training/dashboard', name: 'TrainingDashboard', redirect: to => ({ path: '/training', query: { tab: 'dashboard' } }) },

      // ── 数据中心 (单页 tab) ──
      { path: 'analytics', name: 'Analytics', component: () => import('@/views/analytics/Index.vue'), meta: { title: '数据分析' } },
      { path: 'analytics/nlsql', name: 'Nl2Sql', redirect: to => ({ path: '/analytics', query: { tab: 'nlsql' } }) },
      { path: 'rule', name: 'RuleAssistant', component: () => import('@/views/rule/Index.vue'), meta: { title: 'NL 规则助手' } },

      // ── 工作流 (单页 tab) ──
      { path: 'pipeline', name: 'Pipeline', component: () => import('@/views/pipeline/Index.vue'), meta: { title: '工作流' } },
      { path: 'pipeline/designer', name: 'PipelineDesigner', redirect: to => ({ path: '/pipeline', query: { tab: 'designer' } }) },
      // /pipeline/designer/:id 也统一走 tab（id 由列表页带过去，不做 URL 持久化）
      { path: 'pipeline/designer/:id', redirect: '/pipeline' },
      { path: 'pipeline/runs', name: 'PipelineRuns', redirect: to => ({ path: '/pipeline', query: { tab: 'runs' } }) },

      // ── 应用中心 ──
      { path: 'prompts', name: 'Prompts', component: () => import('@/views/prompts/Index.vue'), meta: { title: 'Prompt 模板' } },
      { path: 'plugins', name: 'Plugins', component: () => import('@/views/plugins/Index.vue'), meta: { title: '插件市场' } },
      { path: 'notification', name: 'Notification', component: () => import('@/views/notification/Index.vue'), meta: { title: '通知中心' } },
      { path: 'collab', name: 'Collab', component: () => import('@/views/collab/Index.vue'), meta: { title: '协作空间' } },

      // ── 系统管理 (统一为 /settings) ──
      // 旧路由 → /settings + query tab 参数（向后兼容）
      { path: 'settings', name: 'Settings', component: () => import('@/views/settings/Index.vue'), meta: { title: '系统管理' } },
      { path: 'apikey', redirect: to => ({ path: '/settings', query: { tab: 'apikey' } }) },
      { path: 'admin', redirect: to => ({ path: '/settings', query: { tab: 'users' } }) },
      { path: 'super', redirect: to => ({ path: '/settings', query: { tab: 'system' } }) },
      { path: 'tenant', redirect: to => ({ path: '/settings', query: { tab: 'tenant' } }) },
      {
        path: 'monitor',
        component: () => import('@/views/monitor/Index.vue'),
        meta: { title: '监控中心' },
        children: [
          { path: '',         name: 'MonitorHome',    redirect: 'monitor/overview' },
          { path: 'overview', name: 'MonitorOverview', component: () => import('@/views/monitor/Overview.vue'), meta: { title: '概览' } },
          { path: 'alerts',   name: 'MonitorAlerts',   component: () => import('@/views/monitor/Alerts.vue'),   meta: { title: '告警' } },
          { path: 'config',   name: 'MonitorConfig',   component: () => import('@/views/monitor/Config.vue'),   meta: { title: '配置' } }
        ]
      },

      // ── 关于 ──
      { path: 'about', name: 'About', component: () => import('@/views/About.vue'), meta: { title: '关于' } },
    ]
  },

  // ─── 404 ───
  {
    path: '/:pathMatch(.*)*', name: 'Error',
    component: () => import('@/views/Error.vue'),
    meta: { public: true, title: '404 Not Found' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// ─── 导航守卫 ───
// T2+: 路由级 401 重定向 — store 中无 token 且目标非 /login 时强制跳 /login
//       (同时保留原有: 有 token 但 isLogin=false 时的 fetchProfile 拉取流程)
const LOGIN_PATH = '/login'
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title + ' - Liugl-AI'
  }

  const userStore = useUserStore()

  // 公开路由跳过校验
  if (to.meta.public) {
    // 已登录用户访问公开路由 → 重定向到首页
    if (userStore.isLogin) {
      return next('/chat')
    }
    return next()
  }

  // T2+ 路由级 401 重定向: 无 token 且不是登录页 → 跳登录
  if (!userStore.accessToken && to.path !== LOGIN_PATH) {
    return next({ path: LOGIN_PATH, query: { redirect: to.fullPath } })
  }

  // JWT 校验
  if (!userStore.isLogin) {
    // 有 token 但没登录态, 尝试刷新
    if (userStore.accessToken) {
      userStore.fetchProfile().then(() => next()).catch(() => {
        next({ path: LOGIN_PATH, query: { redirect: to.fullPath } })
      })
    } else {
      next({ path: LOGIN_PATH, query: { redirect: to.fullPath } })
    }
  } else {
    next()
  }
})

export default router
