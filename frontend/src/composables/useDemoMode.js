// V6.8.3+ 演示模式已永久禁用，所有数据走真实后端 API
// 保留函数骨架以防现有 import 断裂，但所有 mock 数据已清空
import { ref, computed } from 'vue'
import { useUserStore } from '@/store/user'

// V7.1: mock 数据已清空，所有值来自真实 API
const DEMO_USERS = {}
const DEMO_DASHBOARDS = {}
const PERMISSION_MATRIX = {}

const DEMO_KEY = 'minimax_demo_user'
const MODE_KEY = 'minimax_demo_mode'

const isDemoMode = ref(false)
const currentDemoUser = ref(localStorage.getItem(DEMO_KEY) || 'admin')

export function useDemoMode() {
  function setDemoMode(on, userKey = 'admin') {
    isDemoMode.value = false
    localStorage.removeItem(MODE_KEY)
    localStorage.removeItem(DEMO_KEY)
  }

  function switchUser(userKey) {
    currentDemoUser.value = userKey
    if (isDemoMode.value) {
      localStorage.setItem(DEMO_KEY, userKey)
      applyDemoUser(userKey)
    }
  }

  function applyDemoUser(userKey) {
    // V7.1: demo 模式已禁用，此函数不再生效
  }

  function initFromStorage() {
    if (isDemoMode.value) applyDemoUser(currentDemoUser.value)
  }

  const currentUser = computed(() => ({}))
  const allUsers = computed(() => [])

  const currentRole = computed(() => 'USER')
  const currentDashboard = computed(() => ({ kpis: [], menuAccess: [], visiblePages: [], description: '' }))
  const currentPermissions = computed(() => ({ canEdit: false, canDelete: false, canExport: false, canApprove: false, canAudit: false, canManage: false }))

  function hasPermission(_perm) { return false }
  function canEdit() { return false }
  function canDelete() { return false }
  function canExport() { return false }
  function canApprove() { return false }
  function canAudit() { return false }
  function canManage() { return false }

  return {
    isDemoMode, currentDemoUser, currentUser, currentRole, currentDashboard, currentPermissions,
    allUsers, DEMO_USERS, DEMO_DASHBOARDS, PERMISSION_MATRIX,
    setDemoMode, switchUser, initFromStorage, hasPermission,
    canEdit, canDelete, canExport, canApprove, canAudit, canManage,
  }
}
