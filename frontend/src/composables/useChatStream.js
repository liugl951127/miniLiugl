// V3.7.3+ chat 流式发送 (基于 useSSEStream)
import { ref, onUnmounted } from 'vue'
import http from '@/api/http'
import { useSSEStream } from './useSSEStream'

export function useChatStream() {
  const sse = useSSEStream()
  const messages = ref([])
  const toolCalls = ref([])
  const sources = ref([])

  function reset() {
    messages.value = []
    toolCalls.value = []
    sources.value = []
    sse.reset()
  }

  /**
   * 发送消息 + 接收流式响应
   * @param {string} url - SSE URL
   * @param {object} payload - 请求体
   * @param {object} callbacks - { onMessage, onToolCall, onSource, onDone, onError }
   */
  async function send(url, payload, callbacks = {}) {
    const { onMessage, onToolCall, onSource, onDone, onError } = callbacks
    messages.value = []
    toolCalls.value = []
    sources.value = []

    try {
      // 用 fetch + ReadableStream (V3.7.3 真实实现)
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
        },
        body: JSON.stringify(payload),
      })

      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      if (!response.body) throw new Error('Response body is null')

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        if (sse.isPaused.value) {
          await new Promise(r => setTimeout(r, 100))
          continue
        }

        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data === '[DONE]') {
              if (onDone) onDone()
              return
            }
            try {
              let json = JSON.parse(data)
              // V3.7.24+ chat SSE 也支持 Result 包装: {code, data: {type, content, toolCall, ...}}
              // 自动剥 data 字段 (跟 http.js 拦截器逻辑一致)
              if (json && typeof json === 'object' && 'code' in json && 'data' in json && json.code === 0) {
                json = json.data
              }
              // V3.7.24+ 业务错误 (code !== 0)
              if (json && typeof json === 'object' && 'code' in json && json.code !== 0) {
                const err = new Error(json.message || 'Chat SSE 业务错误')
                err.__result = json
                if (onError) onError(err)
                return
              }
              if (json.type === 'content' && json.content) {
                messages.value.push(json.content)
                if (onMessage) onMessage(json.content)
              } else if (json.type === 'tool_call' && json.toolCall) {
                toolCalls.value.push(json.toolCall)
                if (onToolCall) onToolCall(json.toolCall)
              } else if (json.type === 'source' && json.source) {
                sources.value.push(json.source)
                if (onSource) onSource(json.source)
              } else if (json.type === 'done') {
                if (onDone) onDone()
                return
              } else if (json.type === 'error') {
                const err = new Error(json.error || '业务错误')
                err.__result = json
                if (onError) onError(err)
                return
              }
            } catch (e) {
              // 纯文本 fallback
              messages.value.push(data)
              if (onMessage) onMessage(data)
            }
          }
        }
      }

      if (onDone) onDone()
    } catch (e) {
      if (onError) onError(e)
    }
  }

  function pause() { sse.pause() }
  function resume() { sse.resume() }
  function stop() { sse.stop() }

  onUnmounted(() => stop())

  return { messages, toolCalls, sources, ...sse, send, pause, resume, stop, reset }
}
