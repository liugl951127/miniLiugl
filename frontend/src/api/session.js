/**
 * @file session API 调用层 (V3.7.26+ 委托 useBusinessStream)
 *
 * 之前 V3.7.3+ session.js 自己写 SSE 解析 (重复实现)
 * V3.7.26+ 改用 useBusinessStream, 统一 chat/agent/rag SSE
 */
import http from './http'
import { useBusinessStream } from '@/composables/useBusinessStream'

/** 会话管理 */
export const listSessions = (params) => http.get('/sessions', { params })
export const createSession = (data) => http.post('/sessions', data)
export const getSession = (id) => http.get(`/sessions/${id}`)
export const updateSession = (id, data) => http.put(`/sessions/${id}`, data)
export const deleteSession = (id) => http.delete(`/sessions/${id}`)

/** 消息管理 */
export const listMessages = (sessionId, params) => http.get(`/sessions/${sessionId}/messages`, { params })
export const appendMessage = (sessionId, data) => http.post(`/sessions/${sessionId}/messages`, data)

/**
 * V3.7.26+ 流式发送消息 (委托 useBusinessStream)
 *
 * 协议:
 *   - HTTP: POST /sessions/{id}/messages/stream
 *   - 返: text/event-stream
 *   - 5 type: start / content / tool_call / source / done / error
 *   - Result 包装: {code, message, data, timestamp}
 *
 * @param {number} sessionId
 * @param {object} body
 * @param {object} opts { onStart, onContent, onToolCall, onSource, onDone, onError, signal, streamId }
 */
export async function sendMessageStream(sessionId, body, opts = {}) {
  const { streamId, onStart, onContent, onToolCall, onSource, onDone, onError, signal } = opts
  const url = `/sessions/${sessionId}/messages/stream`
  const payload = { ...body, streamId }

  // V3.7.26+ 委托 useBusinessStream (统一 5 type + Result 兼容)
  const stream = useBusinessStream()
  
  // 转换: 业务 onXxx → useBusinessStream onXxx
  const wrappedOpts = {
    onContent: (c) => { onContent && onContent(c) },
    onToolCall: (tc) => { onToolCall && onToolCall(tc) },
    onSource: (s) => { onSource && onSource(s) },
    onDone: () => { onDone && onDone() },
    onError: (e) => { onError && onError(e) },
    signal,
  }
  
  return stream.send(url, payload, wrappedOpts)
}

/** 停止流式生成 */
export const stopMessageStream = (streamId) =>
  http.post(`/sessions/stop-stream`, { streamId })
