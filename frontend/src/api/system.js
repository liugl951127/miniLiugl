/**
 * @file system API 调用层 (V3.5.12+)
 *
 */
// 系统公共 API (V3.5.7 修正路径)
// 修复: 原 systemApi.health/intro/chat.send/chat.stream 路径不匹配后端
// 现在统一指向正确的后端端点
import http from './http'

export const systemApi = {
  /** 平台介绍 (About.vue 用) - minimax-ai /api/ai/intro */
  /**
   * intro - 查询 /api/ai/intro
   * @returns GET /api/ai/intro 的响应 Promise
   */
  intro: () => http.get('/api/ai/intro'),
  /** 平台健康检查 - minimax-monitor /api/v1/monitor/health */
  /**
   * health - 查询 /api/v1/monitor/health
   * @returns GET /api/v1/monitor/health 的响应 Promise
   */
  health: () => http.get('/api/v1/monitor/health'),
  /** 平台完整健康检查 (所有 16 模块) - minimax-monitor */
  /**
   * healthAll - 查询 /api/v1/monitor/health
   * @returns GET /api/v1/monitor/health 的响应 Promise
   */
  healthAll: () => http.get('/api/v1/monitor/health')
}

export const authApi = {
  /**
   * login - 创建/更新 /api/v1/auth/login
   * @returns POST /api/v1/auth/login 的响应 Promise
   */
  login: (data) => http.post('/api/v1/auth/login', data),
  /**
   * register - 创建/更新 /api/v1/auth/register
   * @returns POST /api/v1/auth/register 的响应 Promise
   */
  register: (data) => http.post('/api/v1/auth/register', data),
  /**
   * me - 查询 /api/v1/auth/me
   * @returns GET /api/v1/auth/me 的响应 Promise
   */
  me: () => http.get('/api/v1/auth/me'),
  /**
   * refresh - 创建/更新 /api/v1/auth/refresh
   * @returns POST /api/v1/auth/refresh 的响应 Promise
   */
  refresh: (data) => http.post('/api/v1/auth/refresh', data),
  /**
   * logout - 创建/更新 /api/v1/auth/logout
   * @returns POST /api/v1/auth/logout 的响应 Promise
   */
  logout: () => http.post('/api/v1/auth/logout')
}

export const sessionApi = {
  /**
   * list - 查询 /api/v1/sessions
   * @returns GET /api/v1/sessions 的响应 Promise
   */
  list: (params) => http.get('/api/v1/sessions', { params }),
  /**
   * create - 创建/更新 /api/v1/sessions
   * @returns POST /api/v1/sessions 的响应 Promise
   */
  create: (data) => http.post('/api/v1/sessions', data),
  /**
   * detail - 查询 
   * @returns GET  的响应 Promise
   */
  detail: (id) => http.get(`/api/v1/sessions/${id}`),
  /**
   * update - 替换 
   * @returns PUT  的响应 Promise
   */
  update: (id, data) => http.put(`/api/v1/sessions/${id}`, data),
  /**
   * remove - 删除 
   * @returns DELETE  的响应 Promise
   */
  remove: (id) => http.delete(`/api/v1/sessions/${id}`),
  /**
   * messages - 查询 
   * @returns GET  的响应 Promise
   */
  messages: (id, params) => http.get(`/api/v1/sessions/${id}/messages`, { params }),
  /** 发送消息 (POST) - minimax-chat */
  /**
   * sendMessage - 创建/更新 
   * @returns POST  的响应 Promise
   */
  sendMessage: (id, data) => http.post(`/api/v1/sessions/${id}/messages`, data),
  /** 流式发送 (SSE) - minimax-chat */
  streamUrl: (id) => `/api/v1/sessions/${id}/messages/stream`,
  /** 流式状态查询 */
  /**
   * streamStatus - 查询 
   * @returns GET  的响应 Promise
   */
  streamStatus: (streamId) => http.get(`/api/v1/sessions/stream-status/${streamId}`),
  /** 停止流 */
  /**
   * stopStream - 创建/更新 
   * @returns POST  的响应 Promise
   */
  stopStream: (streamId) => http.post(`/api/v1/sessions/stop-stream?streamId=${streamId}`)
}

// 旧 chatApi 兼容 (前端 views/chat/Index.vue 可能还在用)
export const chatApi = {
  /** 已废弃: 用 sessionApi.sendMessage(id, data) */
  /**
   * send - 创建/更新 
   * @returns POST  的响应 Promise
   */
  send: (id, data) => http.post(`/api/v1/sessions/${id}/messages`, data),
  /** 已废弃: 用 sessionApi.streamUrl(id) */
  streamUrl: (id) => `/api/v1/sessions/${id}/messages/stream`
}

/** 知识库 (前端用 knowledge-bases 别名, 指向 minimax-rag /api/v1/rag/kb) */
export const knowledgeApi = {
  /**
   * list - 查询 /api/v1/rag/kb
   * @returns GET /api/v1/rag/kb 的响应 Promise
   */
  list: (params) => http.get('/api/v1/rag/kb', { params }),
  /**
   * create - 创建/更新 /api/v1/rag/kb
   * @returns POST /api/v1/rag/kb 的响应 Promise
   */
  create: (data) => http.post('/api/v1/rag/kb', data),
  /**
   * detail - 查询 
   * @returns GET  的响应 Promise
   */
  detail: (id) => http.get(`/api/v1/rag/kb/${id}`),
  /**
   * update - 替换 
   * @returns PUT  的响应 Promise
   */
  update: (id, data) => http.put(`/api/v1/rag/kb/${id}`, data),
  /**
   * remove - 删除 
   * @returns DELETE  的响应 Promise
   */
  remove: (id) => http.delete(`/api/v1/rag/kb/${id}`),
  /**
   * public - 查询 /api/v1/rag/kb/public
   * @returns GET /api/v1/rag/kb/public 的响应 Promise
   */
  public: () => http.get('/api/v1/rag/kb/public')
}
