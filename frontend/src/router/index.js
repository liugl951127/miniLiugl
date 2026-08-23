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
    redirect: '/dashboard',
    children: [
      // ── AI 对话 ──
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '工作台' } },
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
      {
        path: 'agent',
        component: () => import('@/views/agent/Index.vue'),
        meta: { title: 'Agent 编排' },
        children: [
          { path: '',          name: 'AgentHome',     redirect: 'agent/tasks' },
          { path: 'tasks',    name: 'AgentTasks',    component: () => import('@/views/agent/Tasks.vue'),    meta: { title: '任务' } },
          { path: 'canvas',   name: 'AgentCanvas',   component: () => import('@/views/agent/Canvas.vue'),   meta: { title: '画布' } },
          { path: 'multi',    name: 'AgentMulti',    component: () => import('@/views/agent/GroupDesigner.vue'), meta: { title: '多智能体' } },
          { path: 'training', name: 'AgentTraining', redirect: '/training' },
          { path: 'approval', name: 'AgentApproval', component: () => import('@/views/agent/Approval.vue'),  meta: { title: '审批' } }
        ]
      },
      { path: 'agent/stream', name: 'AgentStream', redirect: '/agent/canvas' },
      { path: 'agent-auto', name: 'AgentAuto', component: () => import('@/views/agent/Auto.vue'), meta: { title: '智能体群生成', icon: 'MagicStick' } },
      { path: 'agent/group-designer', name: 'AgentGroupDesigner', component: () => import('@/views/agent/GroupDesigner.vue'), meta: { title: '智能体群编排', icon: 'Connection', roles: ['user', 'admin'] } },

      // ── 模型与服务 ──
      {
        path: 'model',
        component: () => import('@/views/model/Index.vue'),
        meta: { title: '模型管理' },
        children: [
          { path: '',         name: 'ModelHome',  redirect: 'model/local' },
          { path: 'trained',  name: 'ModelTrained', component: () => import('@/views/model/Trained.vue'), meta: { title: '训练模型' } },
          { path: 'local',    name: 'ModelLocal',   component: () => import('@/views/model/Local.vue'),   meta: { title: '本地模型' } },
          { path: 'cloud',    name: 'ModelCloud',   component: () => import('@/views/model/Cloud.vue'),   meta: { title: '第三方模型' } }
        ]
      },
      { path: 'function', name: 'Function', component: () => import('@/views/function/Index.vue'), meta: { title: 'Function 工具' } },
      {
        path: 'multimodal',
        component: () => import('@/views/multimodal/Index.vue'),
        meta: { title: '多模态能力中心' },
        children: [
          { path: '',         name: 'MultimodalHome',    redirect: 'multimodal/overview' },
          { path: 'overview', name: 'MultimodalOverview', component: () => import('@/views/multimodal/Overview.vue'), meta: { title: '概览' } },
          { path: 'image',    name: 'MultimodalImage',    component: () => import('@/views/multimodal/Image.vue'),    meta: { title: '图像' } },
          { path: 'audio',    name: 'MultimodalAudio',    component: () => import('@/views/multimodal/Audio.vue'),    meta: { title: '语音' } },
          { path: 'video',    name: 'MultimodalVideo',    component: () => import('@/views/multimodal/Video.vue'),    meta: { title: '视频' } },
          { path: 'document', name: 'MultimodalDocument', component: () => import('@/views/multimodal/Document.vue'), meta: { title: '文档音乐' } }
        ]
      },
      {
        path: 'multimodal/local',
        component: () => import('@/views/multimodal/LocalOnnx.vue'),
        meta: { title: '本地多模态 (ONNX)' },
        children: [
          { path: '',       name: 'LocalOnnxHome', redirect: 'multimodal/local/image' },
          { path: 'image', name: 'LocalImage',    component: () => import('@/views/multimodal/LocalImage.vue'), meta: { title: '图片' } },
          { path: 'audio', name: 'LocalAudio',    component: () => import('@/views/multimodal/LocalAudio.vue'), meta: { title: '语音' } },
          { path: 'video', name: 'LocalVideo',    component: () => import('@/views/multimodal/LocalVideo.vue'), meta: { title: '视频' } },
          { path: 'llm',   name: 'LocalLlm',      component: () => import('@/views/multimodal/LocalLlm.vue'),   meta: { title: '语言' } }
        ]
      },
      { path: 'training', name: 'Training', component: () => import('@/views/training/Console.vue'), meta: { title: '模型训练' } },
      { path: 'training/dashboard', name: 'TrainingDashboard', redirect: to => ({ path: '/training', query: { tab: 'dashboard' } }) },

      // ── 数据中心 (单页 tab) ──
      {
        path: 'analytics',
        component: () => import('@/views/analytics/Index.vue'),
        meta: { title: '数据分析' },
        children: [
          { path: '',         name: 'AnalyticsHome',  redirect: 'analytics/overview' },
          { path: 'overview', name: 'AnalyticsOverview', component: () => import('@/views/analytics/Overview.vue'), meta: { title: '总览' } },
          { path: 'nlsql',    name: 'AnalyticsNlsql',    component: () => import('@/views/analytics/Nlsql.vue'),    meta: { title: 'NL2SQL' } },
          { path: 'vote',     name: 'AnalyticsVote',     component: () => import('@/views/analytics/Vote.vue'),     meta: { title: '多模型投票' } }
        ]
      },
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
      {
        path: 'builder',
        component: () => import('@/views/builder/Index.vue'),
        meta: { title: 'Agent Forge' },
        children: [
          { path: '',            name: 'BuilderHome',    redirect: 'builder/requirements' },
          { path: 'requirements',name: 'BuilderReqs',     component: () => import('@/views/builder/Requirements.vue'), meta: { title: '需求接收' } },
          { path: 'analysis',    name: 'BuilderAnalysis', component: () => import('@/views/builder/Analysis.vue'),     meta: { title: 'AI 解析' } },
          { path: 'designer',    name: 'BuilderDesigner', component: () => import('@/views/builder/Designer.vue'),     meta: { title: '团队设计' } },
          { path: 'deploy',      name: 'BuilderDeploy',   component: () => import('@/views/builder/Deploy.vue'),       meta: { title: '远程部署' } },
          { path: 'monitor',     name: 'BuilderMonitor',  component: () => import('@/views/builder/Monitor.vue'),      meta: { title: '实时监控' } },
          { path: 'releases',    name: 'BuilderReleases', component: () => import('@/views/builder/Releases.vue'),     meta: { title: '发布管理' } }
        ]
      },
      {
        path: 'settings',
        component: () => import('@/views/settings/Index.vue'),
        meta: { title: '系统管理' },
        children: [
          { path: '',         name: 'SettingsHome',  redirect: 'settings/users' },
          { path: 'users',    name: 'SettingsUsers',  component: () => import('@/views/settings/Users.vue'),  meta: { title: '用户租户' } },
          { path: 'apikey',   name: 'SettingsApikey', component: () => import('@/views/settings/Apikey.vue'), meta: { title: 'API Key' } },
          { path: 'audit',    name: 'SettingsAudit',  component: () => import('@/views/settings/Audit.vue'),  meta: { title: '审计' } },
          { path: 'system',   name: 'SettingsSystem', component: () => import('@/views/settings/System.vue'), meta: { title: '系统' } }
        ]
      },
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
