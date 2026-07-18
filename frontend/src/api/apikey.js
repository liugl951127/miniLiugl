/**
 * @file apikey API 调用层 (V3.5.12+)
 *
 */
/**
 * API Key 管理 API (V5.33 Day 18)
 */
import http from './http'

export const apiKeyApi = {
  /** 列出我的 API Key */
  /**
   * list - 查询 /auth/apikeys
   * @returns GET /auth/apikeys 的响应 Promise
   */
  list: () => http.get('/auth/apikeys'),

  /** 创建 API Key（返回 rawKey，仅此次可见） */
  /**
   * create - 创建/更新 /auth/apikeys
   * @returns POST /auth/apikeys 的响应 Promise
   */
  create: (data) => http.post('/auth/apikeys', data),

  /** 禁用 / 启用 Key */
  /**
   * toggle - 部分更新 
   * @returns PATCH  的响应 Promise
   */
  toggle: (id, enable) => http.patch(`/auth/apikeys/${id}/toggle?enable=${enable}`),

  /** 删除 Key */
  /**
   * remove - 删除 
   * @returns DELETE  的响应 Promise
   */
  remove: (id) => http.delete(`/auth/apikeys/${id}`),

  /** 轮换 Key（删旧创新） */
  /**
   * rotate - 创建/更新 
   * @returns POST  的响应 Promise
   */
  rotate: (id, data) => http.post(`/auth/apikeys/${id}/rotate`, data || {}),

  // V5.9 Day 20: 管理员 API Key 统计
  /** 全局统计摘要 (admin) */
  /**
   * adminSummary - 查询 /api/v1/admin/stats/apikey
   * @returns GET /api/v1/admin/stats/apikey 的响应 Promise
   */
  adminSummary: () => http.get('/api/v1/admin/stats/apikey'),

  /** 新增趋势 (admin) */
  /**
   * adminTrend - 查询 
   * @returns GET  的响应 Promise
   */
  adminTrend: (days = 7) => http.get(`/api/v1/admin/stats/apikey/trend?days=${days}`)
}
