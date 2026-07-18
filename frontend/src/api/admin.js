/**
 * @file admin API 调用层 (V3.5.12+)
 *
 * 对应后端模块: minimax-admin
 * 接口数: 30
 *
 *   GET    /api/v1/admin/users
 *   GET    /api/v1/admin/users/{id}
 *   POST   /api/v1/admin/users
 *   POST   /api/v1/admin/users/{id}/reset-password
 *   PUT    /api/v1/admin/users/{id}/status
 *   GET    /api/v1/admin/models/providers
 *   GET    /api/v1/admin/models
 *   PUT    /api/v1/admin/models/{code}/rate-limit
 *   ... 共 30 个
 */
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
  http.get(`/api/v1/admin/audit-ops/recent?limit=${limit}`)

export const getApiKeyStats = () => http.get('/api/v1/admin/apikey/stats')

export const getAuditByActor = (actorId, limit = 20) =>
  http.get(`/api/v1/admin/audit-ops/by-actor/${actorId}?limit=${limit}`)

// V5.9: 按天审计统计 (Dashboard 折线图)
export const getAuditByDay = (days = 7, action) =>
  http.get(`/api/v1/admin/audit-ops/by-day?days=${days}${action ? `&action=${action}` : ''}`)

// V2.9.0: 治理后台 API
export const governance = {
  /**
   * overview - 查询 /api/v1/admin/governance/overview
   * @returns GET /api/v1/admin/governance/overview 的响应 Promise
   */
  overview: (params) => http.get('/api/v1/admin/governance/overview', { params }),
  /**
   * timeline - 查询 /api/v1/admin/governance/timeline
   * @returns GET /api/v1/admin/governance/timeline 的响应 Promise
   */
  timeline: (params) => http.get('/api/v1/admin/governance/timeline', { params }),
  /**
   * anomalies - 查询 /api/v1/admin/governance/anomalies
   * @returns GET /api/v1/admin/governance/anomalies 的响应 Promise
   */
  anomalies: (params) => http.get('/api/v1/admin/governance/anomalies', { params }),
  /**
   * compliance - 查询 /api/v1/admin/governance/compliance
   * @returns GET /api/v1/admin/governance/compliance 的响应 Promise
   */
  compliance: () => http.get('/api/v1/admin/governance/compliance'),
  /**
   * retention - 查询 /api/v1/admin/governance/retention
   * @returns GET /api/v1/admin/governance/retention 的响应 Promise
   */
  retention: () => http.get('/api/v1/admin/governance/retention')
}
