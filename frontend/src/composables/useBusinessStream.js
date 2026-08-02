/**
 * V3.7.25+ 通用业务 SSE 流 (统一 5 type)
 *
 * 之前: useChatStream 写死 type: 'chunk' (跟 useSSEStream 不一致)
 * 现在: 5 type 统一 (content/tool_call/source/done/error)
 * 兼容: V3.7.3+ useChatStream 用的 'chunk' 改成 'content' (但旧 'chunk' 也兼容)
 *
 * 5 种 type:
 *   - content: { type: 'content', content: '...' }
 *   - tool_call: { type: 'tool_call', toolCall: {...} }
 *   - source: { type: 'source', source: {...} }
 *   - done: { type: 'done' }
 *   - error: { type: 'error', message: '...', __result: {...} }
 *
 * Result 包装兼容:
 *   - {code:0, data: <5 type 之一>} → 自动剥
 *   - {code:1, message:'err'} → onError
 *
 * 用法 (chat/agent/rag 统一):
 *   const { messages, toolCalls, sources, send, ... } = useBusinessStream()
 *   await send('/api/chat/stream', payload, {
 *     onContent: (c) => ...,
 *     onToolCall: (tc) => ...,
 *     onSource: (s) => ...,
 *     onDone: () => ...,
 *     onError: (e) => ...,
 *   })
 */
import { ref, onUnmounted } from 'vue'
import { useSSEStream } from './useSSEStream'

// V3.7.25+ 5 type 统一映射
const TYPE_MAP = {
  // 新标准 (V3.7.25+)
  content: 'content',
  tool_call: 'tool_call',
  source: 'source',
  done: 'done',
  error: 'error',
  // 兼容老代码 (V3.7.3+ 用 'chunk')
  chunk: 'content',
  toolcall: 'tool_call',
  src: 'source',
  finish: 'done',
  err: 'error',
}

export function useBusinessStream() {
  const sse = useSSEStream()
  const messages = ref([])
  const toolCalls = ref([])
  const sources = ref([])
  const errors = ref([])

  function reset() {
    messages.value = []
    toolCalls.value = []
    sources.value = []
    errors.value = []
    sse.reset()
  }

  /**
   * 发送 + 接收流式响应
   * @param {string} url
   * @param {object} payload
   * @param {object} callbacks - { onContent, onToolCall, onSource, onDone, onError, signal }
   */
  async function send(url, payload, callbacks = {}) {
    const { onContent, onToolCall, onSource, onDone, onError, signal } = callbacks
    messages.value = []
    toolCalls.value = []
    sources.value = []
    errors.value = []

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
        },
        body: JSON.stringify(payload),
        signal: signal || sse.abortController?.signal,
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
        if (signal?.aborted) throw new DOMException('Aborted', 'AbortError')

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
              // V3.7.25+ Result 包装自动剥
              if (json && typeof json === 'object' && 'code' in json && 'data' in json && json.code === 0) {
                json = json.data
              }
              // V3.7.25+ 业务错误 (code !== 0)
              if (json && typeof json === 'object' && 'code' in json && json.code !== 0) {
                const err = new Error(json.message || 'SSE 业务错误')
                err.__result = json
                errors.value.push(err)
                if (onError) onError(err)
                return
              }
              // 5 type 统一处理 (兼容老 type 别名)
              const type = TYPE_MAP[json.type] || json.type
              if (type === 'content' && json.content !== undefined) {
                messages.value.push(json.content)
                if (onContent) onContent(json.content)
              } else if (type === 'tool_call' && json.toolCall) {
                toolCalls.value.push(json.toolCall)
                if (onToolCall) onToolCall(json.toolCall)
              } else if (type === 'source' && json.source) {
                sources.value.push(json.source)
                if (onSource) onSource(json.source)
              } else if (type === 'done') {
                if (onDone) onDone()
                return
              } else if (type === 'error') {
                const err = new Error(json.message || json.error || '业务错误')
                err.__result = json
                errors.value.push(err)
                if (onError) onError(err)
                return
              }
            } catch (e) {
              // 纯文本 fallback
              messages.value.push(data)
              if (onContent) onContent(data)
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

  return { 
    messages, toolCalls, sources, errors,
    isStreaming: sse.isStreaming,
    isPaused: sse.isPaused,
    progress: sse.progress,
    fullText: sse.fullText,
    error: sse.error,
    send, pause, resume, stop, reset 
  }
}
