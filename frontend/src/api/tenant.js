/**
 * V3.1: 多租户 API
 * 路径: /api/v1/auth/tenants/*
 * 权限: adminLiugl (SUPER_ADMIN) 专属
 */
import http from './http'

// 列出所有租户
export function listTenants() {
  return http.get('/auth/tenants')
}

// 租户详情
export function getTenant(id) {
  return http.get(`/auth/tenants/${id}`)
}

// 创建租户
export function createTenant(data) {
  return http.post('/auth/tenants', data)
}

// 更新租户状态 (0=禁用 1=启用)
export function setTenantStatus(id, status) {
  return http.post(`/auth/tenants/${id}/status`, null, { params: { status } })
}

// 调整月度配额
export function updateTenantQuota(id, quota) {
  return http.post(`/auth/tenants/${id}/quota`, null, { params: { quota } })
}

// 删除租户 (不能删 default)
export function deleteTenant(id) {
  return http.delete(`/auth/tenants/${id}`)
}

// 租户下用户列表
export function listTenantUsers(id) {
  return http.get(`/auth/tenants/${id}/users`)
}

// 当前用户租户信息
export function myTenant() {
  return http.get('/auth/me/tenant')
}

export default {
  listTenants,
  getTenant,
  createTenant,
  setTenantStatus,
  updateTenantQuota,
  deleteTenant,
  listTenantUsers,
  myTenant,
}
