// V3.7.2+ 演示模式 2.0 扩展 - 角色 mock 数据 + 权限 dashboard
import { ref, computed } from 'vue'
import { useUserStore } from '@/store/user'

// === 1. 5 测试账号 (V3.5.97 H5Login 演示账号) ===
const DEMO_USERS = {
  admin: {
    id: 1, username: 'admin', nickname: '超级管理员',
    email: 'admin@liugl.ai', roles: ['SUPER_ADMIN'],
    avatar: 'https://api.dicebear.com/7.x/initials/svg?seed=admin',
    color: '#ef4444',
    description: '拥有所有权限, 可管理用户/角色/系统',
    permissions: ['*'],
    loginTime: '2026-08-02 09:00:00',
  },
  adminLiugl: {
    id: 2, username: 'adminLiugl', nickname: '刘广礼 (创始)',
    email: 'liugl@liugl.ai', roles: ['SUPER_ADMIN'],
    avatar: 'https://api.dicebear.com/7.x/initials/svg?seed=adminLiugl',
    color: '#a855f7',
    description: '项目创始账号, 全局管理',
    permissions: ['*'],
    loginTime: '2026-08-02 08:30:00',
  },
  operator: {
    id: 3, username: 'operator', nickname: '运营专员',
    email: 'operator@liugl.ai', roles: ['OPERATOR'],
    avatar: 'https://api.dicebear.com/7.x/initials/svg?seed=operator',
    color: '#06b6d4',
    description: '日常运营: 内容审核/数据分析/客服',
    permissions: ['chat:read', 'chat:write', 'data:read', 'data:export', 'user:read'],
    loginTime: '2026-08-02 10:15:00',
  },
  auditor: {
    id: 4, username: 'auditor', nickname: '审计员',
    email: 'auditor@liugl.ai', roles: ['AUDITOR'],
    avatar: 'https://api.dicebear.com/7.x/initials/svg?seed=auditor',
    color: '#f59e0b',
    description: '审计日志/合规检查 (只读)',
    permissions: ['audit:read', 'data:read', 'logs:read', 'logs:export'],
    loginTime: '2026-08-02 11:00:00',
  },
  user: {
    id: 5, username: 'user', nickname: '普通用户',
    email: 'user@liugl.ai', roles: ['USER'],
    avatar: 'https://api.dicebear.com/7.x/initials/svg?seed=user',
    color: '#10b981',
    description: '普通用户: 聊天/Agent/RAG',
    permissions: ['chat:read', 'chat:write', 'agent:read', 'agent:write', 'rag:read'],
    loginTime: '2026-08-02 12:30:00',
  },
}

// === 2. 角色 dashboard mock 数据 (V3.7.2+) ===
// 每个角色看到的 KPI / 图表 / 菜单 都不同
const DEMO_DASHBOARDS = {
  SUPER_ADMIN: {
    kpis: [
      { key: 'users', label: '注册用户', value: 1248, trend: 12, color: '#6366f1' },
      { key: 'sessions', label: '活跃会话', value: 89, trend: 8, color: '#06b6d4' },
      { key: 'calls', label: '今日调用', value: 12450, trend: 24, color: '#10b981' },
      { key: 'tools', label: '工具调用', value: 32, trend: 5, color: '#f59e0b' },
    ],
    menuAccess: ['*'],
    visiblePages: ['admin', 'kg', 'agent', 'model', 'audit', 'cluster', 'metrics', 'traces', 'alerts'],
    description: '管理所有模块',
  },
  OPERATOR: {
    kpis: [
      { key: 'todayChats', label: '今日对话', value: 342, trend: 15, color: '#06b6d4' },
      { key: 'activeUsers', label: '活跃用户', value: 56, trend: 3, color: '#10b981' },
      { key: 'pendingReview', label: '待审核', value: 8, trend: -2, color: '#f59e0b' },
      { key: 'dataExports', label: '导出次数', value: 23, trend: 18, color: '#6366f1' },
    ],
    menuAccess: ['chat', 'kg', 'agent', 'data'],
    visiblePages: ['chat', 'kg', 'agent', 'data', 'analytics', 'review'],
    description: '运营: 日常审核/数据分析',
  },
  AUDITOR: {
    kpis: [
      { key: 'totalLogs', label: '日志总数', value: 89432, trend: 124, color: '#f59e0b' },
      { key: 'securityEvents', label: '安全事件', value: 3, trend: 0, color: '#ef4444' },
      { key: 'complianceScore', label: '合规分', value: 92, trend: 2, color: '#10b981' },
      { key: 'auditReports', label: '审计报告', value: 12, trend: 1, color: '#6366f1' },
    ],
    menuAccess: ['audit', 'logs', 'compliance'],
    visiblePages: ['audit', 'logs', 'compliance', 'reports'],
    description: '只读审计 + 合规检查',
  },
  USER: {
    kpis: [
      { key: 'myChats', label: '我的对话', value: 87, trend: 5, color: '#10b981' },
      { key: 'myAgents', label: '我的 Agent', value: 6, trend: 1, color: '#6366f1' },
      { key: 'ragQueries', label: 'RAG 查询', value: 34, trend: 12, color: '#06b6d4' },
      { key: 'tools', label: '可用工具', value: 28, trend: 0, color: '#f59e0b' },
    ],
    menuAccess: ['chat', 'agent', 'rag', 'kg'],
    visiblePages: ['chat', 'agent', 'kg', 'rag', 'tools'],
    description: '个人使用: 聊天/Agent/RAG',
  },
}

// === 3. 角色权限矩阵 (V3.7.2+) ===
const PERMISSION_MATRIX = {
  SUPER_ADMIN: { canEdit: true, canDelete: true, canExport: true, canApprove: true, canAudit: true, canManage: true },
  OPERATOR: { canEdit: true, canDelete: false, canExport: true, canApprove: false, canAudit: false, canManage: false },
  AUDITOR: { canEdit: false, canDelete: false, canExport: true, canApprove: false, canAudit: true, canManage: false },
  USER: { canEdit: true, canDelete: false, canExport: false, canApprove: false, canAudit: false, canManage: false },
}

const DEMO_KEY = 'minimax_demo_user'
const MODE_KEY = 'minimax_demo_mode'

const isDemoMode = ref(localStorage.getItem(MODE_KEY) === 'true' || new URLSearchParams(window.location.search).get('demo') === '1')
const currentDemoUser = ref(localStorage.getItem(DEMO_KEY) || 'admin')

export function useDemoMode() {
  function setDemoMode(on, userKey = 'admin') {
    isDemoMode.value = on
    currentDemoUser.value = userKey
    if (on) {
      localStorage.setItem(MODE_KEY, 'true')
      localStorage.setItem(DEMO_KEY, userKey)
      applyDemoUser(userKey)
    } else {
      localStorage.removeItem(MODE_KEY)
      localStorage.removeItem(DEMO_KEY)
    }
  }

  function switchUser(userKey) {
    if (!DEMO_USERS[userKey]) {
      console.warn(`[useDemoMode] 未知用户: ${userKey}`)
      return
    }
    currentDemoUser.value = userKey
    if (isDemoMode.value) {
      localStorage.setItem(DEMO_KEY, userKey)
      applyDemoUser(userKey)
    }
  }

  function applyDemoUser(userKey) {
    const user = DEMO_USERS[userKey]
    if (!user) return
    const userStore = useUserStore()
    userStore.accessToken = `demo-token-${userKey}-${Date.now()}`
    userStore.userInfo = user
    userStore.profile = user
    userStore.roles = user.roles
    userStore.permissions = user.permissions
    localStorage.setItem('minimax_user', JSON.stringify(user))
  }

  function initFromStorage() {
    if (isDemoMode.value) {
      applyDemoUser(currentDemoUser.value)
    }
  }

  const currentUser = computed(() => DEMO_USERS[currentDemoUser.value] || DEMO_USERS.admin)
  const allUsers = computed(() => Object.entries(DEMO_USERS).map(([k, v]) => ({ key: k, ...v })))

  // V3.7.2+ 角色相关 computed
  const currentRole = computed(() => currentUser.value?.roles?.[0] || 'USER')
  const currentDashboard = computed(() => DEMO_DASHBOARDS[currentRole.value] || DEMO_DASHBOARDS.USER)
  const currentPermissions = computed(() => PERMISSION_MATRIX[currentRole.value] || PERMISSION_MATRIX.USER)

  // 权限检查 helper
  function hasPermission(perm) {
    const perms = currentUser.value?.permissions || []
    if (perms.includes('*')) return true
    return perms.includes(perm)
  }

  function canEdit() { return currentPermissions.value.canEdit }
  function canDelete() { return currentPermissions.value.canDelete }
  function canExport() { return currentPermissions.value.canExport }
  function canApprove() { return currentPermissions.value.canApprove }
  function canAudit() { return currentPermissions.value.canAudit }
  function canManage() { return currentPermissions.value.canManage }

  return {
    isDemoMode, currentDemoUser, currentUser, currentRole, currentDashboard, currentPermissions,
    allUsers, DEMO_USERS, DEMO_DASHBOARDS, PERMISSION_MATRIX,
    setDemoMode, switchUser, initFromStorage, hasPermission,
    canEdit, canDelete, canExport, canApprove, canAudit, canManage,
  }
}
