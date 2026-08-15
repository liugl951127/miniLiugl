/**
 * @file router/index.js - Vue Router 配置 (V6.8 重构版)
 *
 * 所有路由使用懒加载，path 和组件一一对应
 * 守卫: JWT token 校验 (meta.public = true 跳过)
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
      // AI 对话
      { path: 'chat', name: 'Chat', component: () => import('@/views/chat/Index.vue'), meta: { title: '智能对话' } },
      { path: 'chat/stream', name: 'ChatStream', component: () => import('@/views/chat/Stream.vue'), meta: { title: '流式对话' } },
      { path: 'chat/:sessionId', name: 'ChatSession', component: () => import('@/views/chat/Index.vue'), meta: { title: '对话' } },

      // 知识中心
      { path: 'knowledge', name: 'Knowledge', component: () => import('@/views/knowledge/Index.vue'), meta: { title: '知识库管理' } },
      { path: 'kg', name: 'Kg', component: () => import('@/views/kg/Index.vue'), meta: { title: '知识图谱' } },
      { path: 'memory', name: 'Memory', component: () => import('@/views/memory/Index.vue'), meta: { title: '记忆中心' } },

      // Agent
      { path: 'agent', name: 'Agent', component: () => import('@/views/agent/Index.vue'), meta: { title: 'Agent 编排' } },
      { path: 'agent/stream', name: 'AgentStream', component: () => import('@/views/agent/Stream.vue'), meta: { title: 'Agent 流式' } },
      { path: 'agent/canvas', name: 'AgentCanvas', component: () => import('@/views/agent/Canvas.vue'), meta: { title: 'Agent 画布' } },
      { path: 'agent/multi', name: 'AgentMulti', component: () => import('@/views/agent/Multi.vue'), meta: { title: '多智能体' } },
      { path: 'agent/training', name: 'AgentTraining', component: () => import('@/views/agent/Training.vue'), meta: { title: '训练可视化' } },
      { path: 'agent/approval', name: 'AgentApproval', component: () => import('@/views/agent/Approval.vue'), meta: { title: 'Skill 审批' } },
      { path: 'agent-auto', name: 'AgentAuto', component: () => import('@/views/agent/Auto.vue'), meta: { title: '智能体群生成 (V3.4.2)', icon: 'MagicStick' } },

      // 模型与服务
      { path: 'model', name: 'Model', component: () => import('@/views/model/Index.vue'), meta: { title: '模型管理' } },
      { path: 'function', name: 'Function', component: () => import('@/views/function/Index.vue'), meta: { title: 'Function 工具' } },
      { path: 'multimodal', name: 'Multimodal', component: () => import('@/views/multimodal/Index.vue'), meta: { title: '多模态' } },
      { path: 'training', name: 'Training', component: () => import('@/views/training/Console.vue'), meta: { title: '模型训练' } },
      { path: 'training/dashboard', name: 'TrainingDashboard', component: () => import('@/views/training/Dashboard.vue'), meta: { title: '训练总览' } },

      // 数据与工作流
      { path: 'analytics', name: 'Analytics', component: () => import('@/views/analytics/Index.vue'), meta: { title: '数据分析' } },
      { path: 'analytics/nlsql', name: 'Nl2Sql', component: () => import('@/views/analytics/Index.vue'), meta: { title: 'NL2SQL' } },
      { path: 'pipeline', name: 'Pipeline', component: () => import('@/views/pipeline/Index.vue'), meta: { title: '工作流' } },
      { path: 'pipeline/designer', name: 'PipelineDesigner', component: () => import('@/views/pipeline/Designer.vue'), meta: { title: '画布设计器' } },
      { path: 'pipeline/designer/:id', name: 'PipelineDesignerEdit', component: () => import('@/views/pipeline/Designer.vue'), meta: { title: '编辑工作流' } },
      { path: 'pipeline/runs', name: 'PipelineRuns', component: () => import('@/views/pipeline/RunMonitor.vue'), meta: { title: '运行监控' } },

      // 规则引擎
      { path: 'rule', name: 'RuleAssistant', component: () => import('@/views/rule/Index.vue'), meta: { title: 'NL 规则助手', icon: 'MagicStick' } },

      // 应用中心
      { path: 'prompts', name: 'Prompts', component: () => import('@/views/prompts/Index.vue'), meta: { title: 'Prompt 模板' } },
      { path: 'plugins', name: 'Plugins', component: () => import('@/views/plugins/Index.vue'), meta: { title: '插件市场' } },
      { path: 'notification', name: 'Notification', component: () => import('@/views/notification/Index.vue'), meta: { title: '通知中心' } },
      { path: 'collab', name: 'Collab', component: () => import('@/views/collab/Index.vue'), meta: { title: '协作空间' } },

      // 系统管理
      { path: 'apikey', name: 'ApiKey', component: () => import('@/views/apikey/Index.vue'), meta: { title: 'API Key' } },
      { path: 'admin', name: 'Admin', component: () => import('@/views/admin/Index.vue'), meta: { title: '管理后台' } },
      { path: 'super', name: 'Super', component: () => import('@/views/super/Index.vue'), meta: { title: '超级管理' } },
      { path: 'tenant', name: 'Tenant', component: () => import('@/views/tenant/Index.vue'), meta: { title: '租户管理' } },
      { path: 'monitor', name: 'Monitor', component: () => import('@/views/monitor/Index.vue'), meta: { title: '系统监控' } },

      // 关于
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
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title + ' - Liugl-AI'
  }

  // 公开路由跳过校验
  if (to.meta.public) {
    return next()
  }

  // JWT 校验
  const userStore = useUserStore()
  if (!userStore.isLogin) {
    // 有 token 但没登录态，尝试刷新
    if (userStore.accessToken) {
      userStore.fetchProfile().then(() => next()).catch(() => {
        next({ path: '/login', query: { redirect: to.fullPath } })
      })
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
  } else {
    next()
  }
})

export default router
