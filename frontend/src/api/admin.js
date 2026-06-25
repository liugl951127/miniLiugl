// Admin API 封装
import http from './http'

export const getAdminHealth = () => http.get('/admin/health')
export const getAdminPing = () => http.get('/admin/ping')
export const getOpsStats = () => http.get('/admin/stats/ops')
export const getDashboard = () => http.get('/admin/stats/dashboard')

export const listAdminUsers = (page = 1, size = 20) =>
  http.get(`/admin/users?page=${page}&size=${size}`)

export const getAdminUser = (id) => http.get(`/admin/users/${id}`)

export const createAdminUser = (actorId, body) =>
  http.post(`/admin/users?actorId=${actorId}`, body)

export const resetAdminUserPassword = (id, actorId, newPassword) =>
  http.post(`/admin/users/${id}/reset-password?actorId=${actorId}`, { newPassword })

export const toggleAdminUser = (id, actorId, enabled) =>
  http.put(`/admin/users/${id}/status?actorId=${actorId}&enabled=${enabled}`)

export const listModelProviders = () => http.get('/admin/models/providers')
export const listModelConfigs = () => http.get('/admin/models')
export const updateRateLimit = (code, actorId, body) =>
  http.put(`/admin/models/${code}/rate-limit?actorId=${actorId}`, body)

export const getRecentAudit = (limit = 50) =>
  http.get(`/admin/audit/recent?limit=${limit}`)

export const getApiKeyStats = () => http.get('/admin/apikey/stats')

export const getAuditByActor = (actorId, limit = 20) =>
  http.get(`/admin/audit/by-actor/${actorId}?limit=${limit}`)

// V5.9: 按天审计统计 (Dashboard 折线图)
export const getAuditByDay = (days = 7, action) =>
  http.get(`/admin/audit/by-day?days=${days}${action ? `&action=${action}` : ''}`)
