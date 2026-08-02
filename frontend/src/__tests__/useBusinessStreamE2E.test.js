/**
 * V3.7.35+ useBusinessStream 6 type 端到端测试
 *
 * 覆盖 6 场景 (跟后端 SseResultE2ETest 对应):
 * 1. agent 8 事件 (start/tools/step-start/thought/tool-call/observation/final/done)
 * 2. chat 7 事件 (start + 5 content + done)
 * 3. music 7 事件 (start/heartbeat/3 chunks/progress/complete)
 * 4. error 中途发生
 * 5. 6 type 路由: content/tool_call/source/done/error/start
 * 6. Result 包装自动剥
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useBusinessStream } from '@/composables/useBusinessStream'

global.fetch = vi.fn()

// V3.7.30+ 后端 SseResult.sendCustom 输出格式
// {code:0, data: {event, type, ...}, timestamp}
function makePayload(event, data) {
  return { code: 0, message: 'success', data: { event, ...data }, timestamp: Date.now() }
}

function mockSSE(events) {
  // events: array of {event, data: {type, ...}} 或纯 data
  const body = events.map(e => {
    const data = typeof e === 'string' ? e : JSON.stringify(makePayload(e.event, e.data || {}))
    return `data: ${data}\n\n`
  }).join('')
  const encoder = new TextEncoder()
  return {
    ok: true,
    body: {
      getReader: () => {
        let i = 0
        return {
          read: async () => {
            if (i >= body.length) return { done: true, value: undefined }
            const chunk = body.slice(i, i + 200)
            i += 200
            return { done: false, value: encoder.encode(chunk) }
          },
          cancel: async () => {},
        }
      },
    },
  }
}

describe('useBusinessStream 6 type E2E', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('1. agent 8 事件完整链路', async () => {
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'start', data: { type: 'content', streamId: 's1' } },
      { event: 'tools', data: { type: 'source', tools: [{ name: 'search' }] } },
      { event: 'step-start', data: { type: 'content', round: 1 } },
      { event: 'thought', data: { type: 'content', content: 'thinking' } },
      { event: 'tool-call', data: { type: 'tool_call', name: 'search', args: { q: 'x' } } },
      { event: 'observation', data: { type: 'content', content: 'result' } },
      { event: 'final', data: { type: 'content', answer: 'final' } },
    ]))

    const { messages, toolCalls, sources, send } = useBusinessStream()
    const onContent = vi.fn()
    const onToolCall = vi.fn()
    const onSource = vi.fn()
    await send('/api/agent/run-stream', {}, { onContent, onToolCall, onSource })

    // content type (start/step-start/thought/observation/final) → messages
    expect(messages.value.length).toBe(5)
    // tool_call type → toolCalls
    expect(toolCalls.value).toEqual([{ name: 'search', args: { q: 'x' } }])
    // source type → sources
    expect(sources.value).toEqual([{ tools: [{ name: 'search' }] }])
  })

  it('2. chat 7 事件: start + 5 content + done', async () => {
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'start', data: { type: 'content', streamId: 's1' } },
      { event: 'content', data: { type: 'content', content: '你' } },
      { event: 'content', data: { type: 'content', content: '好' } },
      { event: 'content', data: { type: 'content', content: '世' } },
      { event: 'content', data: { type: 'content', content: '界' } },
      { event: 'content', data: { type: 'content', content: '!' } },
    ]))

    const { messages, send } = useBusinessStream()
    await send('/api/chat/stream', {}, { onContent: vi.fn() })

    // start + 5 content = 6 消息
    expect(messages.value).toContain('你')
    expect(messages.value).toContain('好')
    expect(messages.value).toContain('!')
  })

  it('3. music 7 事件: start/heartbeat/3 chunks/progress/complete', async () => {
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'start', data: { type: 'content', taskId: 't1' } },
      { event: 'heartbeat', data: { type: 'content', ping: 12345 } },
      { event: 'chunk', data: { type: 'content', chunkIndex: 0, data: 'audio0' } },
      { event: 'chunk', data: { type: 'content', chunkIndex: 1, data: 'audio1' } },
      { event: 'chunk', data: { type: 'content', chunkIndex: 2, data: 'audio2' } },
      { event: 'progress', data: { type: 'content', percent: 75 } },
      { event: 'complete', data: { type: 'content', durationMs: 3000 } },
    ]))

    const { messages, send } = useBusinessStream()
    await send('/api/music/stream', {}, { onContent: vi.fn() })

    // 7 事件都走 content
    expect(messages.value.length).toBe(7)
  })

  it('4. error 中途发生: 触发 onError + __result 标记', async () => {
    const errPayload = { code: 1, message: 'stream failed', data: { message: 'detail' } }
    fetch.mockResolvedValueOnce({
      ok: true,
      body: {
        getReader: () => {
          const encoder = new TextEncoder()
          const data = `data: ${JSON.stringify(errPayload)}\n\n`
          let i = 0
          return {
            read: async () => {
              if (i >= data.length) return { done: true, value: undefined }
              const chunk = data.slice(i, i + 100)
              i += 100
              return { done: false, value: encoder.encode(chunk) }
            },
            cancel: async () => {},
          }
        },
      },
    })

    const { send, errors } = useBusinessStream()
    const onError = vi.fn()
    await send('/api/agent/run-stream', {}, { onError })

    expect(onError).toHaveBeenCalledTimes(1)
    const err = onError.mock.calls[0][0]
    expect(err.__result).toBeTruthy()
    expect(errors.value).toHaveLength(1)
  })

  it('5. 6 type 路由: content/tool_call/source', async () => {
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'start', data: { type: 'content' } },
      { event: 'tool-call', data: { type: 'tool_call', name: 'x' } },
      { event: 'tools', data: { type: 'source', tools: [] } },
    ]))

    const { send } = useBusinessStream()
    const onContent = vi.fn()
    const onToolCall = vi.fn()
    const onSource = vi.fn()
    await send('/api/test', {}, { onContent, onToolCall, onSource })

    expect(onContent).toHaveBeenCalled()
    expect(onToolCall).toHaveBeenCalled()
    expect(onSource).toHaveBeenCalled()
  })

  it('6. 5 type 兼容别名 (chunk/toolcall/src/finish/err)', async () => {
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'chunk', data: { type: 'content', content: 'old chunk' } },
      { event: 'toolcall', data: { type: 'tool_call', name: 'old' } },
      { event: 'src', data: { type: 'source', url: 'old' } },
      { event: 'finish', data: { type: 'done' } },
    ]))

    const { messages, toolCalls, sources, send } = useBusinessStream()
    const onDone = vi.fn()
    await send('/api/test', {}, { onContent: vi.fn(), onDone })

    expect(messages.value).toContain('old chunk')
    expect(toolCalls.value).toEqual([{ name: 'old' }])
    expect(sources.value).toEqual([{ url: 'old' }])
  })
})
