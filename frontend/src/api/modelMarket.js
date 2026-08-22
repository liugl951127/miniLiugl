/**
 * @file modelMarket API 调用层 (V3.5.12+, V6.8.1 修复)
 *
 */
// V2.9.1 AI 模型市场 SDK
import http from './http'

export const stats = () => {
  return http.get('/ai/model-market/stats');
}
export const browse = (params) => http.get('/ai/model-market/models', { params })
export const detail = (key) => http.get(`/ai/model-market/models/${key}`)
export const upload = (formData) => http.post('/ai/model-market/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const publish = (data) => {
  return http.post('/ai/model-market/publish', data);
}
export const rate = (key, data) => http.post(`/ai/model-market/models/${key}/rate`, data)
export const ratings = (key) => http.get(`/ai/model-market/models/${key}/ratings`)
export const myModels = (authorId) => http.get('/ai/model-market/my', { params: { authorId } })
// V6.8.1 fix: 对齐 ProviderController /api/v1/model/providers/toggle (按 code 查找)
export const changeStatus = (code, status) => http.post(`/model/providers/toggle?code=${code}`)

export const modelMarketApi = { stats, browse, detail, upload, publish, rate, ratings, myModels, changeStatus }
