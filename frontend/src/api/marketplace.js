/**
 * @file marketplace API 调用层 (V3.5.12+)
 *
 */
// V2.9.0 Agent Marketplace SDK
import http from './http'

// V6.8.1 fix: 对齐 AiMarketplaceRealController 路由
// 后端: /ai/marketplace/list → browse()
// 后端: /ai/marketplace/{id}/install → installAgent()
// 后端: /ai/marketplace/search?q= → search()
// 后端: /ai/marketplace/categories → categories()
export const browse = (params) => http.get('/ai/marketplace/list', { params })
export const detail = (id) => http.get(`/ai/marketplace/${id}`)
export const upload = (data) => http.post('/ai/marketplace/list', data)
export const rate = (id, data) => http.post(`/ai/marketplace/${id}/rate`, data)
export const ratings = (id) => http.get(`/ai/marketplace/${id}`)
export const installAgent = (id) => http.post(`/ai/marketplace/${id}/install`)
export const uninstallAgent = (id) => http.delete(`/ai/marketplace/${id}/install`)
export const search = (q) => http.get('/ai/marketplace/search', { params: { q } })
export const categories = () => http.get('/ai/marketplace/categories')

// 兼容旧调用（别名）
export const useAgent = installAgent
export const approve = installAgent

export const marketplaceApi = {
  browse, detail, upload, rate, ratings,
  installAgent, uninstallAgent, search, categories,
  useAgent, approve
}
