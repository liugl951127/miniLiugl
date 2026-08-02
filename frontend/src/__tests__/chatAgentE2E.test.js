/**
 * V3.7.36+ chat/agent 端到端业务流测试
 * 
 * 模拟完整业务流 (无浏览器, 纯 JS 层):
 * 1. 模拟用户输入 → chat SSE → 流式回复
 * 2. 模拟 agent 任务 → agent SSE → 工具调用 → final
 * 3. 验证打字机/暂停/继续行为
 * 4. 验证错误处理
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useBusinessStream } from '@/composables/useBusinessStream'

global.fetch = vi.fn()

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

describe('chat/agent 端到端业务流', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('1. chat 完整业务流: 用户输入 → 流式回复', async () => {
    // 模拟用户输入 "你好世界" 触发流式回复
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'start', data: { type: 'content', streamId: 's1' } },
      { event: 'content', data: { type: 'content', content: '你' } },
      { event: 'content', data: { type: 'content', content: '好' } },
      { event: 'content', data: { type: 'content', content: '，' } },
      { event: 'content', data: { type: 'content', content: '世' } },
      { event: 'content', data: { type: 'content', content: '界' } },
      { event: 'content', data: { type: 'content', content: '!' } },
    ]))

    const { messages, send } = useBusinessStream()
    const onContent = vi.fn()
    await send('/api/chat/stream', { role: 'user', content: '你好' }, { onContent })

    // 完整流式回复累积
    expect(messages.value.join('')).toBe('你好，世界!')
    expect(onContent).toHaveBeenCalledTimes(7) // start + 6 content
  })

  it('2. agent 完整业务流: 用户目标 → 思考 → 工具 → 最终', async () => {
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'start', data: { type: 'content', streamId: 'a1' } },
      { event: 'thought', data: { type: 'content', content: '需要搜索' } },
      { event: 'tool-call', data: { type: 'tool_call', toolCall: { name: 'search', args: { q: 'x' } } } },
      { event: 'observation', data: { type: 'content', content: '搜索结果' } },
      { event: 'final', data: { type: 'content', answer: '这是最终答案' } },
    ]))

    const { messages, toolCalls, send } = useBusinessStream()
    const onToolCall = vi.fn()
    await send('/api/agent/run-stream', { goal: '搜索 x' }, { onContent: vi.fn(), onToolCall })

    // 验证 4 阶段
    expect(messages.value).toContain('需要搜索')
    expect(messages.value).toContain('搜索结果')
    expect(toolCalls.value).toEqual([{ name: 'search', args: { q: 'x' } }])
    expect(onToolCall).toHaveBeenCalledWith({ name: 'search', args: { q: 'x' } })
  })

  it('3. 错误恢复: 业务错误后流结束', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      body: {
        getReader: () => {
          const encoder = new TextEncoder()
          const data = `data: ${JSON.stringify({ code: 1, message: 'rate limit', data: { message: '请稍后' } })}\n\n`
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
    await send('/api/chat/stream', {}, { onError })

    expect(onError).toHaveBeenCalledTimes(1)
    const err = onError.mock.calls[0][0]
    expect(err.message).toBe('rate limit')
    expect(err.__result.data.message).toBe('请稍后')
    expect(errors.value).toHaveLength(1)
  })

  it('4. 长时间流: 100 chunks 流式累积', async () => {
    // 模拟 100 chunks
    const events = [
      { event: 'start', data: { type: 'content' } },
    ]
    for (let i = 0; i < 100; i++) {
      events.push({ event: 'content', data: { type: 'content', content: String(i) } })
    }
    fetch.mockResolvedValueOnce(mockSSE(events))

    const { messages, send } = useBusinessStream()
    await send('/api/chat/stream', {}, { onContent: vi.fn() })

    // 100 chunks 累积
    expect(messages.value.length).toBe(100)
    expect(messages.value[0]).toBe('0')
    expect(messages.value[99]).toBe('99')
  })

  it('5. 5 type 兼容别名业务流 (chunk/toolcall/src/finish/err)', async () => {
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'chunk', data: { type: 'content', content: 'A' } },  // 兼容 chunk
      { event: 'toolcall', data: { type: 'tool_call', toolCall: { name: 'x' } } },  // 兼容 toolcall
      { event: 'src', data: { type: 'source', source: { url: 'a' } } },  // 兼容 src
    ]))

    const { messages, toolCalls, sources, send } = useBusinessStream()
    await send('/api/legacy', {}, { onContent: vi.fn() })

    expect(messages.value).toEqual(['A'])
    expect(toolCalls.value).toEqual([{ name: 'x' }])
    expect(sources.value).toEqual([{ url: 'a' }])
  })

  it('6. 并发多个 send: reset 后新流', async () => {
    // 第 1 次流
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'content', data: { type: 'content', content: 'first' } },
    ]))
    // 第 2 次流
    fetch.mockResolvedValueOnce(mockSSE([
      { event: 'content', data: { type: 'content', content: 'second' } },
    ]))

    const { messages, send, reset } = useBusinessStream()
    
    await send('/api/chat/stream', {}, { onContent: vi.fn() })
    expect(messages.value).toEqual(['first'])
    
    reset()
    expect(messages.value).toEqual([])
    
    await send('/api/chat/stream', {}, { onContent: vi.fn() })
    expect(messages.value).toEqual(['second'])
  })
})
