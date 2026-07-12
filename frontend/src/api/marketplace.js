// V2.9.0 Agent Marketplace SDK
import http from './http'

export const browse = (params) => http.get('/ai/marketplace/agents', { params })
export const detail = (key) => http.get(`/ai/marketplace/agents/${key}`)
export const upload = (data) => http.post('/ai/marketplace/agents', data)
export const rate = (key, data) => http.post(`/ai/marketplace/agents/${key}/rate`, data)
export const ratings = (key) => http.get(`/ai/marketplace/agents/${key}/ratings`)
export const useAgent = (key) => http.post(`/ai/marketplace/agents/${key}/use`)
export const approve = (key, data) => http.post(`/ai/marketplace/agents/${key}/approve`, data)
export const myAgents = (authorId) => http.get('/ai/marketplace/my', { params: { authorId } })
export const stats = () => http.get('/ai/marketplace/stats')

export const marketplaceApi = { browse, detail, upload, rate, ratings, useAgent, approve, myAgents, stats }
