// V3.7.1+ 演示模式 2.0 - 多账号切换 + 角色 mock 数据
import { ref, computed, watch } from 'vue'
import { useUserStore } from '@/store/user'

const DEMO_USERS = {
  admin: {
    id: 1, username: 'admin', nickname: '超级管理员',
    email: 'admin@liugl.ai', roles: ['SUPER_ADMIN'],
    avatar: 'https://api.dicebear.com/7.x/initials/svg?seed=admin',
    color: '#ef4444',
    description: '拥有所有权限, 可管理用户/角色/系统',
    permissions: ['*'],
  },
  adminLiugl: {
    id: 2, username: 'adminLiugl', nickname: '刘广礼 (创始)',
    email: 'liugl@liugl.ai', roles: ['SUPER_ADMIN'],
    avatar: 'https://api.dicebear.com/7.x/initials/svg?seed=adminLiugl',
    color: '#a855f7',
    description: '项目创始账号, 全局管理',
    permissions: ['*'],
  },
  operator: {
    id: 3, username: 'operator', nickname: '运营专员',
    email: 'operator@liugl.ai', roles: ['OPERATOR'],
    avatar: 'https://api.dicebear.com/7.x/initials/svg?seed=operator',
    color: '#06b6d4',
    description: '日常运营: 内容审核/数据分析/客服',
    permissions: ['chat:read', 'chat:write', 'data:read', 'data:export'],
  },
  auditor: {
    id: 4, username: 'auditor', nickname: '审计员',
    email: 'auditor@liugl.ai', roles: ['AUDITOR'],
    avatar: 'https://api.dicebear.com/7.x/initials/svg?seed=auditor',
    color: '#f59e0b',
    description: '审计日志/合规检查 (只读)',
    permissions: ['audit:read', 'data:read', 'logs:read'],
  },
  user: {
    id: 5, username: 'user', nickname: '普通用户',
    email: 'user@liugl.ai', roles: ['USER'],
    avatar: 'https://api.dicebear.com/7.x/initials/svg?seed=user',
    color: '#10b981',
    description: '普通用户: 聊天/Agent/RAG',
    permissions: ['chat:read', 'chat:write', 'agent:read', 'agent:write'],
  },
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

  return {
    isDemoMode,
    currentDemoUser,
    currentUser,
    allUsers,
    DEMO_USERS,
    setDemoMode,
    switchUser,
    initFromStorage,
  }
}
