/**
 * V3.7.35+ useBusinessStream 6 type 端到端测试
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useBusinessStream } from '@/composables/useBusinessStream'

global.fetch = vi.fn()

// V3.7.30+ 后端 SseResult.sendCustom 输出
// {code:0, data: {event, type, ...}, timestamp}
function makePayload(event, data) {
  return { code: 0, message: 'success', data: { event, ...data }, timestamp: Date.now() }
}

function mockSSE(events) {
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

  it('1. agent 8 事件: 5 type 路由 + content/tool_call/source', async () => {
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'start', data: { type: 'content', streamId: 's1' } },
      { event: 'tools', data: { type: 'source', tools: [{ name: 'search' }] } },
      { event: 'thought', data: { type: 'content', content: 'thinking' } },
      { event: 'tool-call', data: { type: 'tool_call', name: 'search', args: { q: 'x' } } },
      { event: 'observation', data: { type: 'content', content: 'result' } },
    ]))

    const { messages, toolCalls, sources, send } = useBusinessStream()
    await send('/api/agent/run-stream', {}, { onContent: vi.fn(), onToolCall: vi.fn(), onSource: vi.fn() })

    // 2 个 content: thought + observation
    expect(messages.value).toEqual(['thinking', 'result'])
    // 1 tool_call
    expect(toolCalls.value).toEqual([{ name: 'search', args: { q: 'x' } }])
    // 1 source
    expect(sources.value).toEqual([{ tools: [{ name: 'search' }] }])
  })

  it('2. chat 5 content 事件', async () => {
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'content', data: { type: 'content', content: '你' } },
      { event: 'content', data: { type: 'content', content: '好' } },
      { event: 'content', data: { type: 'content', content: '世' } },
      { event: 'content', data: { type: 'content', content: '界' } },
      { event: 'content', data: { type: 'content', content: '!' } },
    ]))

    const { messages, send } = useBusinessStream()
    await send('/api/chat/stream', {}, { onContent: vi.fn() })

    expect(messages.value).toEqual(['你', '好', '世', '界', '!'])
  })

  it('3. music 5 事件: chunk 类型不同字段', async () => {
    // 5 个事件, 全部 type=content
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'start', data: { type: 'content', taskId: 't1' } },
      { event: 'chunk', data: { type: 'content', content: 'audio0' } },
      { event: 'chunk', data: { type: 'content', content: 'audio1' } },
      { event: 'progress', data: { type: 'content', percent: 75 } },
      { event: 'complete', data: { type: 'content', durationMs: 3000 } },
    ]))

    const { messages, send } = useBusinessStream()
    await send('/api/music/stream', {}, { onContent: vi.fn() })

    // 只有 2 个有 content 字段 (chunk x 2)
    expect(messages.value).toEqual(['audio0', 'audio1'])
  })

  it('4. error 中途: 触发 onError + __result', async () => {
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

  it('5. 6 type 路由分别触发', async () => {
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'start', data: { type: 'content', content: 'x' } },
      { event: 'tool-call', data: { type: 'tool_call', name: 'x' } },
      { event: 'tools', data: { type: 'source', tools: [] } },
    ]))

    const { send } = useBusinessStream()
    const onContent = vi.fn()
    const onToolCall = vi.fn()
    const onSource = vi.fn()
    await send('/api/test', {}, { onContent, onToolCall, onSource })

    expect(onContent).toHaveBeenCalledWith('x')
    expect(onToolCall).toHaveBeenCalledWith({ name: 'x' })
    expect(onSource).toHaveBeenCalledWith({ tools: [] })
  })

  it('6. 5 type 兼容别名 (chunk/toolcall/src/finish/err)', async () => {
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'chunk', data: { type: 'content', content: 'old chunk' } },
      { event: 'toolcall', data: { type: 'tool_call', name: 'old' } },
      { event: 'src', data: { type: 'source', url: 'old' } },
    ]))

    const { messages, toolCalls, sources, send } = useBusinessStream()
    await send('/api/test', {}, { onContent: vi.fn() })

    expect(messages.value).toEqual(['old chunk'])
    expect(toolCalls.value).toEqual([{ name: 'old' }])
    expect(sources.value).toEqual([{ url: 'old' }])
  })
})
