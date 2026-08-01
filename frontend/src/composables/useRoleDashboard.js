// V3.7.2+ 角色 dashboard composable
// 根据当前用户角色返回不同的 KPI / 图表数据
import { computed } from 'vue'
import { useDemoMode } from './useDemoMode'

export function useRoleDashboard() {
  const { currentRole, currentDashboard, currentUser } = useDemoMode()

  // 4 角色 4 套 KPI (已在 DEMO_DASHBOARDS 里)
  const kpis = computed(() => currentDashboard.value.kpis)

  // 角色色卡
  const roleColor = computed(() => {
    const map = { SUPER_ADMIN: '#ef4444', OPERATOR: '#06b6d4', AUDITOR: '#f59e0b', USER: '#10b981' }
    return map[currentRole.value] || '#6366f1'
  })

  const roleLabel = computed(() => {
    const map = { SUPER_ADMIN: '超级管理员', OPERATOR: '运营专员', AUDITOR: '审计员', USER: '普通用户' }
    return map[currentRole.value] || '访客'
  })

  return { kpis, roleColor, roleLabel, currentRole, currentUser, currentDashboard }
}
