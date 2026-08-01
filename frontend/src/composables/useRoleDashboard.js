// V3.7.3+ 角色 dashboard composable - 加 emoji icon
import { computed } from 'vue'
import { useDemoMode } from './useDemoMode'

const ROLE_ICONS = {
  SUPER_ADMIN: '👑', OPERATOR: '🎯', AUDITOR: '🔍', USER: '👤',
}

const KPI_EMOJI = {
  // SUPER_ADMIN
  users: '👥', sessions: '💬', calls: '📞', tools: '🛠️',
  // OPERATOR
  todayChats: '💬', activeUsers: '🟢', pendingReview: '⏳', dataExports: '📊',
  // AUDITOR
  totalLogs: '📋', securityEvents: '🚨', complianceScore: '✅', auditReports: '📑',
  // USER
  myChats: '💬', myAgents: '🤖', ragQueries: '🔎', tools: '🛠️',
}

export function useRoleDashboard() {
  const { currentRole, currentDashboard, currentUser } = useDemoMode()

  // 加 emoji icon + role icon
  const kpis = computed(() => {
    const list = currentDashboard.value.kpis || []
    return list.map((kpi) => ({
      ...kpi,
      emoji: KPI_EMOJI[kpi.key] || '📊',
    }))
  })

  const roleColor = computed(() => {
    const map = { SUPER_ADMIN: '#ef4444', OPERATOR: '#06b6d4', AUDITOR: '#f59e0b', USER: '#10b981' }
    return map[currentRole.value] || '#6366f1'
  })

  const roleLabel = computed(() => {
    const map = { SUPER_ADMIN: '超级管理员', OPERATOR: '运营专员', AUDITOR: '审计员', USER: '普通用户' }
    return map[currentRole.value] || '访客'
  })

  const roleIcon = computed(() => ROLE_ICONS[currentRole.value] || '👤')

  return { kpis, roleColor, roleLabel, roleIcon, currentRole, currentUser, currentDashboard }
}
