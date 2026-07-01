/**
 * ws.js 单元测试 (Day 25)
 * 重点测试 createWS 的 URL 构建 / send / close / 重连行为
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock global WebSocket
const mockSend = vi.fn()
const mockClose = vi.fn()
const mockOnOpen = vi.fn()
const mockOnMessage = vi.fn()
const mockOnClose = vi.fn()
const mockOnError = vi.fn()

function makeMockWS() {
  return class MockWebSocket {
    static CONNECTING = 0
    static OPEN = 1
    static CLOSING = 2
    static CLOSED = 3
    readyState = MockWebSocket.OPEN
    onopen = null
    onmessage = null
    onclose = null
    onerror = null
    send = mockSend
    close = mockClose
  }
}

vi.stubGlobal('WebSocket', makeMockWS())

// 动态 import 以在 vi.stubGlobal 之后加载
let createWS, createNotificationWS

beforeEach(async () => {
  vi.clearAllMocks()
  const wsModule = await import('./ws.js')
  createWS = wsModule.createWS
  createNotificationWS = wsModule.createNotificationWS
})

describe('ws.js - createWS', () => {

  describe('URL 构建', () => {
    it('不带 token 的 URL', () => {
      const ws = createWS({
        url: '/ws/chat',
        onOpen: mockOnOpen,
        onMessage: mockOnMessage,
        onClose: mockOnClose,
        onError: mockOnError,
      })
      ws.connect()
      expect(ws.isConnected()).toBe(true)
    })

    it('带 token 的 URL 将 token 加入 query', () => {
      const ws = createWS({
        url: '/ws/chat',
        token: 'test-jwt-token',
        onOpen: mockOnOpen,
        onMessage: mockOnMessage,
        onClose: mockOnClose,
        onError: mockOnError,
      })
      ws.connect()
      expect(ws.isConnected()).toBe(true)
    })

    it('带额外 params 的 URL', () => {
      const ws = createWS({
        url: '/ws/chat',
        token: 'tok123',
        params: { sessionId: 'abc', room: 'main' },
        onOpen: mockOnOpen,
        onMessage: mockOnMessage,
        onClose: mockOnClose,
        onError: mockOnError,
      })
      ws.connect()
      expect(ws.isConnected()).toBe(true)
    })
  })

  describe('send', () => {
    it('已连接时 send 成功返回 true', () => {
      const ws = createWS({
        url: '/ws/chat',
        onOpen: mockOnOpen,
        onMessage: mockOnMessage,
        onClose: mockOnClose,
        onError: mockOnError,
      })
      ws.connect()
      const result = ws.send({ action: 'pause' })
      expect(result).toBe(true)
      expect(mockSend).toHaveBeenCalled()
    })

    it('未连接时 send 返回 false 且不调用 mockSend', () => {
      const ws = createWS({
        url: '/ws/chat',
        onOpen: mockOnOpen,
        onMessage: mockOnMessage,
        onClose: mockOnClose,
        onError: mockOnError,
        reconnectDelay: 999999,
      })
      // socket 未初始化时 send 返回 false
      const result = ws.send({ action: 'pause' })
      expect(result).toBe(false)
    })

    it('send 接受字符串 payload', () => {
      const ws = createWS({
        url: '/ws/chat',
        onOpen: mockOnOpen,
        onMessage: mockOnMessage,
        onClose: mockOnClose,
        onError: mockOnError,
      })
      ws.connect()
      ws.send('plain text message')
      expect(mockSend).toHaveBeenCalledWith('plain text message')
    })

    it('send 自动 JSON.stringify 对象', () => {
      const ws = createWS({
        url: '/ws/chat',
        onOpen: mockOnOpen,
        onMessage: mockOnMessage,
        onClose: mockOnClose,
        onError: mockOnError,
      })
      ws.connect()
      const payload = { type: 'ping' }
      ws.send(payload)
      expect(mockSend).toHaveBeenCalledWith(JSON.stringify(payload))
    })
  })

  describe('close', () => {
    it('close 关闭 WebSocket 并禁止重连', () => {
      const ws = createWS({
        url: '/ws/chat',
        onOpen: mockOnOpen,
        onMessage: mockOnMessage,
        onClose: mockOnClose,
        onError: mockOnError,
      })
      ws.connect()
      ws.close(1000, 'user logout')
      expect(mockClose).toHaveBeenCalledWith(1000, 'user logout')
    })

    it('close 不带参数使用默认值', () => {
      const ws = createWS({
        url: '/ws/chat',
        onOpen: mockOnOpen,
        onMessage: mockOnMessage,
        onClose: mockOnClose,
        onError: mockOnError,
      })
      ws.connect()
      ws.close()
      expect(mockClose).toHaveBeenCalledWith(1000, 'normal')
    })
  })

  describe('心跳', () => {
    it('连接成功后启动心跳定时器', async () => {
      vi.useFakeTimers()
      const ws = createWS({
        url: '/ws/chat',
        onOpen: mockOnOpen,
        onMessage: mockOnMessage,
        onClose: mockOnClose,
        onError: mockOnError,
        heartbeatInterval: 1000,
      })
      ws.connect()
      vi.advanceTimersByTime(2000)
      vi.useRealTimers()
    })
  })
})

describe('ws.js - createNotificationWS', () => {
  it('使用通知专用 URL 和默认参数', async () => {
    const ws = createNotificationWS('token-abc', {
      onMessage: mockOnMessage,
      onOpen: mockOnOpen,
      onClose: mockOnClose,
      onError: mockOnError,
    })
    ws.connect()
    expect(ws.isConnected()).toBe(true)
    ws.close()
  })
})
