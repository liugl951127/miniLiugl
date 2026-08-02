/**
 * V3.7.26+ useChatStream - 委托 useBusinessStream (保持向后兼容)
 *
 * 之前 V3.7.3+ 重复实现 SSE 解析
 * V3.7.26+ 委托 useBusinessStream, 但保留 V3.7.3+ 的 onChunk/onMessage 接口
 *
 * V3.7.3+ 老接口: { onChunk, onMessage, onToolCall, onSource, onDone, onError }
 * V3.7.26+ 新接口: { onContent, onMessage, onToolCall, onSource, onDone, onError }
 *
 * 推荐: 新代码用 useBusinessStream
 */
import { onUnmounted } from 'vue'
import { useBusinessStream } from './useBusinessStream'

export function useChatStream() {
  const stream = useBusinessStream()
  
  // 兼容 V3.7.3+ 的 onChunk 别名
  const send = async (url, payload, callbacks = {}) => {
    const { onChunk, onContent, ...rest } = callbacks
    return stream.send(url, payload, {
      ...rest,
      onContent: onContent || onChunk,  // 兼容老 onChunk
    })
  }
  
  onUnmounted(() => stream.stop())
  
  return {
    messages: stream.messages,
    toolCalls: stream.toolCalls,
    sources: stream.sources,
    isStreaming: stream.isStreaming,
    isPaused: stream.isPaused,
    progress: stream.progress,
    fullText: stream.fullText,
    error: stream.error,
    send,
    pause: stream.pause,
    resume: stream.resume,
    stop: stream.stop,
    reset: stream.reset,
  }
}
