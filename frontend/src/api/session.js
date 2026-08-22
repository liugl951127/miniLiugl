/**
 * @file session API 调用层 (V3.7.26+ 委托 useBusinessStream)
 *
 * 之前 V3.7.3+ session.js 自己写 SSE 解析 (重复实现)
 * V3.7.26+ 改用 useBusinessStream, 统一 chat/agent/rag SSE
 */
import http from './http'
import { useBusinessStream } from '@/composables/useBusinessStream'

/** 会话管理 */
export const listSessions = (params) => http.get('/ai/chat/sessions', { params })
export const createSession = (data) => {
  return http.post('/ai/chat/sessions', data);
}
// V6.8.1 fix: 所有路径补 /ai/chat 前缀 (对齐 AiChatRealController)
export const getSession = (id) => http.get(`/ai/chat/sessions/${id}`)
export const updateSession = (id, data) => http.put(`/ai/chat/sessions/${id}`, data)
export const deleteSession = (id) => http.delete(`/ai/chat/sessions/${id}`)

/** 消息管理 */
export const listMessages = (sessionId, params) => http.get(`/ai/chat/sessions/${sessionId}/messages`, { params })
export const appendMessage = (sessionId, data) => http.post(`/ai/chat/sessions/${sessionId}/messages`, data)

/**
 * V3.7.26+ 流式发送消息 (委托 useBusinessStream)
 *
 * 协议:
 *   - HTTP: POST /ai/chat/stream (V6.8.1 fix: 原来 /sessions/{id}/messages/stream 不存在)
 *   - 返: text/event-stream
 *   - 5 type: start / content / tool_call / source / done / error
 *   - Result 包装: {code, message, data, timestamp}
 *
 * @param {number} sessionId
 * @param {object} body
 * @param {object} opts { onStart, onContent, onToolCall, onSource, onDone, onError, signal, streamId }
 */
export async function sendMessageStream(sessionId, body, opts = {}) {
  // V7.0: 支持 Flow② Agent SSE 回调
  const { streamId, _onStart, onContent, onToolCall, onSource, onDone, onError,
          onAgentResult, onAgentStatus, signal } = opts
  // V6.8.1 fix: 后端只有 POST /ai/chat/stream (不在 /sessions/{id}/messages/stream)
  const url = `/ai/chat/stream`
  const payload = { ...body, sessionId, streamId }

  // V3.7.26+ 委托 useBusinessStream (统一 5 type + Result 兼容)
  const stream = useBusinessStream()

  // 转换: 业务 onXxx → useBusinessStream onXxx
  const wrappedOpts = {
    onContent: (c) => { onContent && onContent(c) },
    onToolCall: (tc) => { onToolCall && onToolCall(tc) },
    onSource: (s) => { onSource && onSource(s) },
    onDone: () => { onDone && onDone() },
    onError: (e) => { onError && onError(e) },
    onAgentResult: (r) => { onAgentResult && onAgentResult(r) },
    onAgentStatus: (s) => { onAgentStatus && onAgentStatus(s) },
    signal,
  }
  
  return stream.send(url, payload, wrappedOpts)
}

/** 停止流式生成 */
export const stopMessageStream = (streamId) =>
  // V6.8.1 fix: 路径对齐 AiChatRealController /api/v1/ai/chat/stop
  http.post(`/ai/chat/stop`, { streamId })

// ============ ONNX 推理 API (V7.1) ============
/**
 * POST /ai/chat/onnx/generate
 * 同步推理（阻塞直到生成完毕），返回完整文本
 */
export const onnxGenerate = (params) => {
  const { prompt, model, temperature = 0.7, maxTokens = 512, topP = 0.9 } = params
  return http.post('/ai/chat/onnx/generate', { prompt, model, temperature, maxTokens, topP })
}

/**
 * GET /ai/chat/onnx/status
 * 查询 ONNX 模型加载状态
 */
export const onnxStatus = () => http.get('/ai/chat/onnx/status')

/**
 * V6.8+ 聚合 API (兼容旧 store)
 */
export const sessionApi = {
  list: listSessions,
  create: createSession,
  get: getSession,
  update: updateSession,
  remove: deleteSession,
  listMessages,
  appendMessage,
  sendMessageStream,
  stopMessageStream,
  onnxGenerate,
  onnxStatus
}

export const messageApi = {
  list: listMessages,
  append: appendMessage,
  sendStream: sendMessageStream,
  stopStream: stopMessageStream
}
