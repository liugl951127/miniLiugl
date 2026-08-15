/**
 * @file model API 调用层 (V3.5.12+)
 *
 * 对应后端模块: minimax-model
 * 接口数: 26
 *
 *   GET    /api/v1/audio/asr/models
 *   POST   /api/v1/audio/asr/transcribe
 *   GET    /api/v1/audio/tts/voices
 *   POST   /api/v1/audio/tts/synthesize
 *   GET    /api/v1/imagegen/models
 *   POST   /api/v1/imagegen/generate
 *   GET    /api/v1/leaderboard/overall
 *   GET    /api/v1/leaderboard/latency
 *   ... 共 26 个
 */
// 模型管理 API (V5.24 扩展 Provider + Leaderboard, V5.23 修复 localStorage→Pinia)
import http from './http'
import { useUserStore } from '@/store/user'

export const modelApi = {
  // V3.5.8: 修正路径 - 后端 minimax-admin /api/v1/admin/models (限管理员)
  /**
   * list - 查询 /api/v1/admin/models
   * @returns GET /api/v1/admin/models 的响应 Promise
   */
  list: () => http.get('/admin/models'),
  /**
   * providers - 查询 /api/v1/admin/models/providers
   * @returns GET /api/v1/admin/models/providers 的响应 Promise
   */
  providers: () => http.get('/admin/models/providers'),
  // 聊天端点保持原样 (minimax-model 模块有 /api/v1/models/chat)
  /**
   * chat - 创建/更新 /api/v1/models/chat
   * @returns POST /api/v1/models/chat 的响应 Promise
   */
  chat: (data) => http.post('/models/chat', data),
  /**
   * cancel - 创建/更新 
   * @returns POST  的响应 Promise
   */
  cancel: (streamId) => http.post(`/models/chat/cancel?streamId=${streamId}`)
}

// Provider 管理 (V5.10 + V5.24 完整 CRUD)
export const listProviders = (page = 1, size = 20) =>
  http.get(`/model/providers/page?page=${page}&size=${size}`)

export const getProvider = (id) =>
  http.get(`/model/providers/${id}`)

export const createProvider = (data) =>
  http.post('/model/providers', data)

export const updateProvider = (id, data) =>
  http.put(`/model/providers/${id}`, data)

export const deleteProvider = (id) =>
  http.delete(`/model/providers/${id}`)

export const testProvider = (id) =>
  http.post(`/model/providers/${id}/test`)

// Leaderboard (V4.1)
export const leaderboardOverall = (limit = 50) =>
  http.get(`/leaderboard/overall?limit=${limit}`)

export const leaderboardLatency = (limit = 50) =>
  http.get(`/leaderboard/latency?limit=${limit}`)

export const leaderboardRecent = (limit = 50) =>
  http.get(`/leaderboard/recent?limit=${limit}`)

export const leaderboardCategories = () =>
  http.get('/leaderboard/categories')

// ==================== 模型列表 API (V6.8.2 修复) ====================
/** 列出所有可用模型 (含本地模型) — 前端选择器用这个 */
export const listEnabledModels = () => http.get('/models')

// ==================== 本地模型 API (V6.8.1) ====================
export const localModelApi = {
  /** 列出本地服务商 */
  listProviders: () => http.get('/models/local/providers'),
  /** 注册本地服务商 */
  registerProvider: (data) => http.post('/models/local/providers', data),
  /** 删除本地服务商 */
  deleteProvider: (id) => http.delete(`/models/local/providers/${id}`),
  /** 切换启用状态 */
  toggleProvider: (id) => http.post(`/models/local/providers/${id}/toggle`),
  /** 从本地服务器发现可用模型 */
  discoverModels: (providerId) => http.get(`/models/local/providers/${providerId}/discover`),
  /** 添加单个模型到 model_config */
  addModel: (providerId, data) => http.post(`/models/local/providers/${providerId}/models`, data),
  /** 一键同步：从服务器发现并添加所有模型 */
  syncModels: (providerId) => http.post(`/models/local/providers/${providerId}/sync`),
}

/**
 * SSE 流式 chat (V5.16+)
 * 返回 { abort(), streamId }
 */
export function streamChat(messages, model, onChunk) {
  const ctrl = new AbortController()
  const streamId = 'web_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
  const baseURL = import.meta.env.VITE_API_BASE || ''
  const userStore = useUserStore()
  const accessToken = userStore.accessToken || ''

  fetch(`${baseURL}/api/v1/models/chat/stream?streamId=${streamId}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`
    },
    body: JSON.stringify({ model, messages, stream: true }),
    signal: ctrl.signal
  }).then(async response => {
    if (!response.ok || !response.body) {
      onChunk({ error: `HTTP ${response.status}` })
      return
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const payload = line.slice(5).trim()
          if (payload && payload !== '[DONE]') {
            try { onChunk(JSON.parse(payload)) } catch { onChunk({ text: payload }) }
          }
        }
      }
    }
    onChunk({ done: true })
  }).catch(err => {
    if (err.name !== 'AbortError') onChunk({ error: err.message })
  })

  return { abort: () => ctrl.abort(), streamId }
}