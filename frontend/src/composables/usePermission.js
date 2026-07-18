/**
 * @file usePermission.js - usePermission 组合式 API
 * @version V3.5.12+ (前端注释补全)
 */
import { ref, computed } from 'vue'

/**
 * 权限 composable (V2.7.9)
 *
 * 简化版: 角色从 userStore.profile.roles 取
 * (生产应从后端 /api/ai/permission/me 拉)
 */
const ROLE_PERMS = {
  SUPER_ADMIN: ['*'],
  ADMIN: ['ai.use', 'ai.admin', 'ai.tool.*', 'user.manage', 'system.config', 'alert.*', 'audit.read', 'doc.parse', 'training.run'],
  USER: ['ai.use', 'ai.chat', 'ai.image', 'ai.workflow', 'doc.parse', 'training.view'],
  GUEST: ['ai.chat', 'ai.read']
}

const currentRole = ref(localStorage.getItem('minimax_role') || 'USER')

function has(perm) {
  const perms = ROLE_PERMS[currentRole.value] || []
  if (perms.includes('*')) return true
  if (perms.includes(perm)) return true
  return perms.some(p => p.endsWith('.*') && perm.startsWith(p.slice(0, -2) + '.'))
}

function hasAll(...perms) { return perms.every(has) }
function hasAny(...perms) { return perms.some(has) }
function setRole(role) {
  currentRole.value = role
  localStorage.setItem('minimax_role', role)
}

export function usePermission() {
  return {
    role: currentRole,
    permissions: computed(() => ROLE_PERMS[currentRole.value] || []),
    has,
    hasAll,
    hasAny,
    setRole
  }
}
