/**
 * session.js API 单元测试 (Vitest, mock fetch + http)
 * V5.33 Day 25
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// Mock global fetch
const mockFetch = vi.fn()
globalThis.fetch = mockFetch

// Mock http for non-stream functions
vi.mock('./http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  }
}))

import http from './http'
import {
  listSessions,
  createSession,
  getSession,
  updateSession,
  deleteSession,
  listMessages,
  appendMessage,
  sendMessageStream,
  stopMessageStream,
} from './session.js'

beforeEach(() => {
  vi.clearAllMocks()
})

// ─── 会话 CRUD ───────────────────────────────────────────────

describe('listSessions', () => {
  it('should call http.get with correct params', async () => {
    http.get.mockResolvedValue({ data: [{ id: 1, title: 'Test Session' }] })
    const result = await listSessions({ page: 1, size: 20 })
    expect(http.get).toHaveBeenCalledWith('/sessions', { params: { page: 1, size: 20 } })
    expect(result.data).toHaveLength(1)
  })
})

describe('createSession', () => {
  it('should POST session data', async () => {
    const payload = { title: 'New Chat', modelCode: 'gpt-4o' }
    http.post.mockResolvedValue({ data: { id: 99, ...payload } })
    const result = await createSession(payload)
    expect(http.post).toHaveBeenCalledWith('/sessions', payload)
    expect(result.data.id).toBe(99)
  })
})

describe('getSession', () => {
  it('should GET single session', async () => {
    http.get.mockResolvedValue({ data: { id: 5, title: 'Session 5' } })
    const result = await getSession(5)
    expect(http.get).toHaveBeenCalledWith('/sessions/5')
    expect(result.data.id).toBe(5)
  })
})

describe('updateSession', () => {
  it('should PUT updated session', async () => {
    const payload = { title: 'Updated Title' }
    http.put.mockResolvedValue({ data: { id: 3, title: 'Updated Title' } })
    const result = await updateSession(3, payload)
    expect(http.put).toHaveBeenCalledWith('/sessions/3', payload)
    expect(result.data.title).toBe('Updated Title')
  })
})

describe('deleteSession', () => {
  it('should DELETE session', async () => {
    http.delete.mockResolvedValue({ data: null })
    await deleteSession(8)
    expect(http.delete).toHaveBeenCalledWith('/sessions/8')
  })
})

// ─── 消息 ────────────────────────────────────────────────────

describe('listMessages', () => {
  it('should GET messages for session with pagination', async () => {
    http.get.mockResolvedValue({ data: { messages: [], total: 0 } })
    await listMessages(5, { page: 1, size: 50 })
    expect(http.get).toHaveBeenCalledWith('/sessions/5/messages', { params: { page: 1, size: 50 } })
  })
})

describe('appendMessage', () => {
  it('should POST message to session', async () => {
    const msg = { role: 'user', content: 'Hello' }
    http.post.mockResolvedValue({ data: { id: 100, ...msg } })
    const result = await appendMessage(5, msg)
    expect(http.post).toHaveBeenCalledWith('/sessions/5/messages', msg)
    expect(result.data.id).toBe(100)
  })
})

// ─── 流式发送 ────────────────────────────────────────────────

describe('sendMessageStream', () => {
  let mockReader, mockResp

  beforeEach(() => {
    mockReader = {
      read: vi.fn(),
      releaseLock: vi.fn(),
    }
    mockResp = {
      ok: true,
      body: { getReader: () => mockReader },
    }
    mockFetch.mockResolvedValue(mockResp)
  })

  it('should call fetch with correct headers and body', async () => {
    mockReader.read.mockResolvedValueOnce({ done: true, value: new Uint8Array(0) })

    const onChunk = vi.fn()
    const onDone = vi.fn()
    await sendMessageStream(5, { role: 'user', content: 'hi' }, { onChunk, onDone })

    expect(mockFetch).toHaveBeenCalledOnce()
    const [url, opts] = mockFetch.mock.calls[0]
    expect(url).toContain('/sessions/5/messages/stream')
    expect(opts.method).toBe('POST')
    expect(opts.headers['Content-Type']).toBe('application/json')
    expect(opts.headers['Accept']).toBe('text/event-stream')
  })

  it('should parse SSE data: chunk event → call onChunk', async () => {
    const encoder = new TextEncoder()
    const chunkData = JSON.stringify({ type: 'chunk', content: 'Hello' })
    mockReader.read
      .mockResolvedValueOnce({ done: false, value: encoder.encode(`data: ${chunkData}\n\n`) })
      .mockResolvedValueOnce({ done: true, value: new Uint8Array(0) })

    const onChunk = vi.fn()
    await sendMessageStream(5, { role: 'user', content: 'hi' }, { onChunk })
    expect(onChunk).toHaveBeenCalledWith('Hello')
  })

  it('should parse SSE data: done event → call onDone', async () => {
    const encoder = new TextEncoder()
    const doneData = JSON.stringify({ type: 'done' })
    mockReader.read
      .mockResolvedValueOnce({ done: false, value: encoder.encode(`data: ${doneData}\n\n`) })
      .mockResolvedValueOnce({ done: true, value: new Uint8Array(0) })

    const onDone = vi.fn()
    await sendMessageStream(5, { role: 'user', content: 'hi' }, { onDone })
    expect(onDone).toHaveBeenCalled()
  })

  it('should handle [DONE] SSE marker', async () => {
    const encoder = new TextEncoder()
    mockReader.read
      .mockResolvedValueOnce({ done: false, value: encoder.encode('data: [DONE]\n\n') })
      .mockResolvedValueOnce({ done: true, value: new Uint8Array(0) })

    const onDone = vi.fn()
    await sendMessageStream(5, { role: 'user', content: 'hi' }, { onDone })
    expect(onDone).toHaveBeenCalled()
  })

  it('should call onError on non-200 response', async () => {
    mockFetch.mockResolvedValueOnce({ ok: false, status: 401, body: null })

    const onError = vi.fn()
    await sendMessageStream(5, { role: 'user', content: 'hi' }, { onError })
    expect(onError).toHaveBeenCalled()
    expect(onError.mock.calls[0][0].message).toBe('HTTP 401')
  })

  it('should call onError on stream error', async () => {
    mockReader.read.mockRejectedValueOnce(new Error('network interrupted'))

    const onError = vi.fn()
    await sendMessageStream(5, { role: 'user', content: 'hi' }, { onError })
    expect(onError).toHaveBeenCalled()
    expect(onError.mock.calls[0][0].message).toBe('network interrupted')
  })

  it('should call onToolCall on tool_call type event', async () => {
    const encoder = new TextEncoder()
    const toolData = JSON.stringify({ type: 'tool_call', toolCall: { name: 'get_weather', arguments: '{}' } })
    mockReader.read
      .mockResolvedValueOnce({ done: false, value: encoder.encode(`data: ${toolData}\n\n`) })
      .mockResolvedValueOnce({ done: true, value: new Uint8Array(0) })

    const onToolCall = vi.fn()
    await sendMessageStream(5, { role: 'user', content: 'weather' }, { onToolCall })
    expect(onToolCall).toHaveBeenCalledWith({ name: 'get_weather', arguments: '{}' })
  })
})

// ─── 停止流式 ───────────────────────────────────────────────

describe('stopMessageStream', () => {
  it('should POST streamId to stop-stream endpoint', async () => {
    http.post.mockResolvedValue({ data: null })
    await stopMessageStream('stream-abc-123')
    expect(http.post).toHaveBeenCalledWith('/sessions/stop-stream', { streamId: 'stream-abc-123' })
  })
})
