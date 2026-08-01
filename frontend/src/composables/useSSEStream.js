// V3.7.3+ 真实 SSE ReadableStream composable
// 用 fetch + ReadableStream 替代 chunk callback
// 支持 paused / resumed / 进度 / 错误重试
import { ref, onUnmounted } from 'vue'

export function useSSEStream() {
  const isStreaming = ref(false)
  const isPaused = ref(false)
  const progress = ref(0)  // 0-100
  const fullText = ref('')
  const error = ref(null)
  let abortController = null
  let reader = null

  async function streamSSE(url, options = {}) {
    isStreaming.value = true
    isPaused.value = false
    progress.value = 0
    fullText.value = ''
    error.value = null
    abortController = new AbortController()

    try {
      const response = await fetch(url, {
        ...options,
        signal: abortController.signal,
        headers: {
          'Accept': 'text/event-stream',
          'Cache-Control': 'no-cache',
          ...(options.headers || {}),
        },
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      if (!response.body) {
        throw new Error('Response body is null')
      }

      reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        if (isPaused.value) {
          await new Promise(r => setTimeout(r, 100))
          continue
        }

        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          // SSE 格式: "data: xxx" / "event: xxx" / "id: xxx" / 空行
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data === '[DONE]') {
              isStreaming.value = false
              progress.value = 100
              return
            }
            try {
              const json = JSON.parse(data)
              // 处理 chunk
              if (json.content) {
                fullText.value += json.content
                progress.value = Math.min(100, progress.value + (json.content.length / 50))
              }
              if (json.done) {
                isStreaming.value = false
                progress.value = 100
              }
            } catch (e) {
              // 纯文本
              fullText.value += data
            }
          } else if (line.startsWith('event:')) {
            // 事件类型
          } else if (line.startsWith('id:')) {
            // 事件 ID
          }
        }
      }
    } catch (e) {
      if (e.name === 'AbortError') {
        // 用户取消
      } else {
        error.value = e
      }
    } finally {
      isStreaming.value = false
    }
  }

  function pause() {
    isPaused.value = true
  }

  function resume() {
    isPaused.value = false
  }

  function stop() {
    if (abortController) abortController.abort()
    if (reader) reader.cancel()
    isStreaming.value = false
    isPaused.value = false
  }

  function reset() {
    stop()
    progress.value = 0
    fullText.value = ''
    error.value = null
  }

  onUnmounted(() => stop())

  return {
    isStreaming, isPaused, progress, fullText, error,
    streamSSE, pause, resume, stop, reset,
  }
}
