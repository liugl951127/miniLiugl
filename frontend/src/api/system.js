// 系统公共 API (V3.5.7 修正路径)
// 修复: 原 systemApi.health/intro/chat.send/chat.stream 路径不匹配后端
// 现在统一指向正确的后端端点
import http from './http'

export const systemApi = {
  /** 平台介绍 (About.vue 用) - minimax-ai /api/ai/intro */
  intro: () => http.get('/api/ai/intro'),
  /** 平台健康检查 - minimax-monitor /api/v1/monitor/health */
  health: () => http.get('/api/v1/monitor/health'),
  /** 平台完整健康检查 (所有 16 模块) - minimax-monitor */
  healthAll: () => http.get('/api/v1/monitor/health/all')
}

export const authApi = {
  login: (data) => http.post('/api/v1/auth/login', data),
  register: (data) => http.post('/api/v1/auth/register', data),
  me: () => http.get('/api/v1/auth/me'),
  refresh: (data) => http.post('/api/v1/auth/refresh', data),
  logout: () => http.post('/api/v1/auth/logout')
}

export const sessionApi = {
  list: (params) => http.get('/api/v1/sessions', { params }),
  create: (data) => http.post('/api/v1/sessions', data),
  detail: (id) => http.get(`/api/v1/sessions/${id}`),
  update: (id, data) => http.put(`/api/v1/sessions/${id}`, data),
  remove: (id) => http.delete(`/api/v1/sessions/${id}`),
  messages: (id, params) => http.get(`/api/v1/sessions/${id}/messages`, { params }),
  /** 发送消息 (POST) - minimax-chat */
  sendMessage: (id, data) => http.post(`/api/v1/sessions/${id}/messages`, data),
  /** 流式发送 (SSE) - minimax-chat */
  streamUrl: (id) => `/api/v1/sessions/${id}/messages/stream`,
  /** 流式状态查询 */
  streamStatus: (streamId) => http.get(`/api/v1/sessions/stream-status/${streamId}`),
  /** 停止流 */
  stopStream: (streamId) => http.post(`/api/v1/sessions/stop-stream?streamId=${streamId}`)
}

// 旧 chatApi 兼容 (前端 views/chat/Index.vue 可能还在用)
export const chatApi = {
  /** 已废弃: 用 sessionApi.sendMessage(id, data) */
  send: (id, data) => http.post(`/api/v1/sessions/${id}/messages`, data),
  /** 已废弃: 用 sessionApi.streamUrl(id) */
  streamUrl: (id) => `/api/v1/sessions/${id}/messages/stream`
}

/** 知识库 (前端用 knowledge-bases 别名, 指向 minimax-rag /api/v1/rag/kb) */
export const knowledgeApi = {
  list: (params) => http.get('/api/v1/rag/kb', { params }),
  create: (data) => http.post('/api/v1/rag/kb', data),
  detail: (id) => http.get(`/api/v1/rag/kb/${id}`),
  update: (id, data) => http.put(`/api/v1/rag/kb/${id}`, data),
  remove: (id) => http.delete(`/api/v1/rag/kb/${id}`),
  public: () => http.get('/api/v1/rag/kb/public')
}
