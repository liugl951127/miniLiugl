import http from './http'

export const modelApi = {
  list: () => http.get('/api/v1/models'),
  providers: () => http.get('/api/v1/models/providers'),
  chat: (data) => http.post('/api/v1/models/chat', data),
  cancel: (streamId) => http.post(`/api/v1/models/chat/cancel?streamId=${streamId}`)
}

/**
 * 直接用 fetch 处理 SSE（不走 axios 拦截器，避免被解析成 JSON）。
 * 返回 { abort(), streamId }：调用方可以 abort() 中断。
 */
export function streamChat(messages, model, onChunk) {
  const ctrl = new AbortController()
  const streamId = 'web_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
  const baseURL = import.meta.env.VITE_API_BASE || ''
  const accessToken = JSON.parse(localStorage.getItem('minimax-user') || '{}')?.accessToken || ''

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
        if (!line.startsWith('data:')) continue
        const data = line.slice(5).trim()
        if (data === '[DONE]') {
          onChunk({ done: true })
          return
        }
        try {
          const json = JSON.parse(data)
          const content = json.choices?.[0]?.delta?.content || ''
          if (content) onChunk({ content })
        } catch (e) { /* ignore parse error */ }
      }
    }
  }).catch(err => {
    if (err.name !== 'AbortError') onChunk({ error: err.message })
  })

  return {
    streamId,
    abort: () => ctrl.abort()
  }
}
