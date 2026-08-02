/**
 * V3.7.26+ useBusinessStream 单元测试
 *
 * 覆盖 4 个核心场景:
 * 1. 5 type 统一解析
 * 2. Result 包装自动剥
 * 3. 业务错误处理
 * 4. 5 type 别名兼容 (chunk/toolcall/src/finish/err)
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useBusinessStream } from '@/composables/useBusinessStream'

// mock fetch
global.fetch = vi.fn()

function mockSSEResponse(events) {
  const body = events.map(e => {
    if (e === '[DONE]') return 'data: [DONE]\n\n'
    return `data: ${JSON.stringify(e)}\n\n`
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
            const chunk = body.slice(i, i + 100)
            i += 100
            return { done: false, value: encoder.encode(chunk) }
          },
          cancel: async () => {},
        }
      },
    },
  }
}

describe('useBusinessStream', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('1. 5 type 统一解析: content/tool_call/source/done', async () => {
    fetch.mockResolvedValueOnce(mockSSEResponse([
      { code: 0, data: { type: 'content', content: 'hello ' } },
      { code: 0, data: { type: 'content', content: 'world' } },
      { code: 0, data: { type: 'done' } },
    ]))

    const { messages, send } = useBusinessStream()
    const onContent = vi.fn()
    const onDone = vi.fn()
    await send('/api/chat/stream', {}, { onContent, onDone })

    expect(messages.value).toEqual(['hello ', 'world'])
    expect(onContent).toHaveBeenCalledTimes(2)
    expect(onDone).toHaveBeenCalledTimes(1)
  })

  it('2. Result 包装自动剥: {code:0, data:{type,content}}', async () => {
    fetch.mockResolvedValueOnce(mockSSEResponse([
      { code: 0, data: { type: 'content', content: '剥包装成功' } },
    ]))

    const { messages, send } = useBusinessStream()
    await send('/api/chat/stream', {}, { onContent: vi.fn() })
    expect(messages.value).toEqual(['剥包装成功'])
  })

  it('3. 业务错误: code !== 0 触发 onError + __result 标记', async () => {
    fetch.mockResolvedValueOnce(mockSSEResponse([
      { code: 1, message: 'stream failed', data: { message: 'detail' } },
    ]))

    const { send, errors } = useBusinessStream()
    const onError = vi.fn()
    await send('/api/chat/stream', {}, { onError })

    expect(onError).toHaveBeenCalledTimes(1)
    const err = onError.mock.calls[0][0]
    expect(err.message).toBe('stream failed')
    expect(err.__result).toEqual({ code: 1, message: 'stream failed', data: { message: 'detail' } })
    expect(errors.value).toHaveLength(1)
  })

  it('4. 5 type 别名兼容: chunk/toolcall/src/finish/err', async () => {
    fetch.mockResolvedValueOnce(mockSSEResponse([
      { type: 'chunk', content: '老别名' },
      { type: 'toolcall', toolCall: { name: 'x' } },
      { type: 'src', source: { url: 'a' } },
      { type: 'finish' },
    ]))

    const { messages, toolCalls, sources, send } = useBusinessStream()
    const onToolCall = vi.fn()
    const onSource = vi.fn()
    const onDone = vi.fn()
    await send('/api/chat/stream', {}, { onContent: vi.fn(), onToolCall, onSource, onDone })

    expect(messages.value).toEqual(['老别名'])
    expect(toolCalls.value).toEqual([{ name: 'x' }])
    expect(sources.value).toEqual([{ url: 'a' }])
    expect(onToolCall).toHaveBeenCalledWith({ name: 'x' })
    expect(onSource).toHaveBeenCalledWith({ url: 'a' })
    expect(onDone).toHaveBeenCalledTimes(1)
  })
})
