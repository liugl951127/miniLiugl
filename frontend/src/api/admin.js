// Admin API 封装
import http from './http'

export const getAdminHealth = () => http.get('/api/v1/admin/health')
export const getAdminPing = () => http.get('/api/v1/admin/ping')
export const getOpsStats = () => http.get('/api/v1/admin/stats/ops')
export const getDashboard = () => http.get('/api/v1/admin/stats/dashboard')

export const listAdminUsers = (page = 1, size = 20) =>
  http.get(`/api/v1/admin/users?page=${page}&size=${size}`)

export const getAdminUser = (id) => http.get(`/api/v1/admin/users/${id}`)

export const createAdminUser = (actorId, body) =>
  http.post(`/api/v1/admin/users?actorId=${actorId}`, body)

export const resetAdminUserPassword = (id, actorId, newPassword) =>
  http.post(`/api/v1/admin/users/${id}/reset-password?actorId=${actorId}`, { newPassword })

export const toggleAdminUser = (id, actorId, enabled) =>
  http.put(`/api/v1/admin/users/${id}/status?actorId=${actorId}&enabled=${enabled}`)

export const listModelProviders = () => http.get('/api/v1/admin/models/providers')
export const listModelConfigs = () => http.get('/api/v1/admin/models')
export const updateRateLimit = (code, actorId, body) =>
  http.put(`/api/v1/admin/models/${code}/rate-limit?actorId=${actorId}`, body)

export const getRecentAudit = (limit = 50) =>
  http.get(`/api/v1/admin/audit/recent?limit=${limit}`)

export const getApiKeyStats = () => http.get('/api/v1/admin/apikey/stats')

export const getAuditByActor = (actorId, limit = 20) =>
  http.get(`/api/v1/admin/audit/by-actor/${actorId}?limit=${limit}`)

// V5.9: 按天审计统计 (Dashboard 折线图)
export const getAuditByDay = (days = 7, action) =>
  http.get(`/api/v1/admin/audit/by-day?days=${days}${action ? `&action=${action}` : ''}`)

// V2.9.0: 治理后台 API
export const governance = {
  overview: (params) => http.get('/api/v1/admin/governance/overview', { params }),
  timeline: (params) => http.get('/api/v1/admin/governance/timeline', { params }),
  anomalies: (params) => http.get('/api/v1/admin/governance/anomalies', { params }),
  compliance: () => http.get('/api/v1/admin/governance/compliance'),
  retention: () => http.get('/api/v1/admin/governance/retention')
}
