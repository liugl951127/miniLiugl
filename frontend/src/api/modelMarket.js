// V2.9.1 AI 模型市场 SDK
import http from './http'

export const stats = () => http.get('/ai/model-market/stats')
export const browse = (params) => http.get('/ai/model-market/models', { params })
export const detail = (key) => http.get(`/ai/model-market/models/${key}`)
export const upload = (formData) => http.post('/ai/model-market/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const publish = (data) => http.post('/ai/model-market/publish', data)
export const rate = (key, data) => http.post(`/ai/model-market/models/${key}/rate`, data)
export const ratings = (key) => http.get(`/ai/model-market/models/${key}/ratings`)
export const myModels = (authorId) => http.get('/ai/model-market/my', { params: { authorId } })
export const changeStatus = (key, status) => http.post(`/ai/model-market/models/${key}/status`, { status })

export const modelMarketApi = { stats, browse, detail, upload, publish, rate, ratings, myModels, changeStatus }
