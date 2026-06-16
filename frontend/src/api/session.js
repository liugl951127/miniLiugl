import http from './http'

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
 * 流式发送消息 (SSE / fetch ReadableStream)
 * @param {number} sessionId
 * @param {object} body  { role, content, modelCode, images }
 * @param {object} opts  { streamId, onChunk, onToolCall, onSource, onDone, onError }
 */
export async function sendMessageStream(sessionId, body, opts = {}) {
  const { streamId, onChunk, onToolCall, onSource, onDone, onError } = opts
  const url = `/sessions/${sessionId}/messages/stream`
  const payload = { ...body, streamId }

  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'text/event-stream' },
      body: JSON.stringify(payload),
    })

    if (!resp.ok || !resp.body) {
      throw new Error(`HTTP ${resp.status}`)
    }

    const reader = resp.body.getReader()
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
          const data = line.substring(5).trim()
          if (data === '[DONE]') {
            onDone && onDone()
            return
          }
          try {
            const obj = JSON.parse(data)
            if (obj.type === 'chunk' && obj.content) {
              onChunk && onChunk(obj.content)
            } else if (obj.type === 'tool_call' && onToolCall) {
              onToolCall(obj.toolCall)
            } else if (obj.type === 'source' && onSource) {
              onSource(obj.source)
            } else if (obj.type === 'done') {
              onDone && onDone()
            } else if (obj.type === 'error') {
              onError && onError(new Error(obj.message))
            }
          } catch (e) {
            // 非 JSON 行忽略
          }
        }
      }
    }
    onDone && onDone()
  } catch (e) {
    onError && onError(e)
  }
}

/** 停止流式生成 */
export const stopMessageStream = (streamId) =>
  http.post(`/sessions/stop-stream`, { streamId })
