/**
 * API Key 管理 API (V5.33 Day 18)
 */
import http from './http'

export const apiKeyApi = {
  /** 列出我的 API Key */
  list: () => http.get('/auth/apikeys'),

  /** 创建 API Key（返回 rawKey，仅此次可见） */
  create: (data) => http.post('/auth/apikeys', data),

  /** 禁用 / 启用 Key */
  toggle: (id, enable) => http.patch(`/auth/apikeys/${id}/toggle?enable=${enable}`),

  /** 删除 Key */
  remove: (id) => http.delete(`/auth/apikeys/${id}`),

  /** 轮换 Key（删旧创新） */
  rotate: (id, data) => http.post(`/auth/apikeys/${id}/rotate`, data || {})
}
