/**
 * WebSocket 工具类 (V5.22 Day 22)
 *
 * 提供统一的 WebSocket 连接管理:
 * - 自动重连 (exponential backoff)
 * - 心跳保活
 * - 统一消息解析
 * - 生命周期钩子
 *
 * 用法:
 *   import { createWS } from '@/utils/ws'
 *
 *   const ws = createWS({
 *     url: '/ws/bidi',
 *     token: userStore.accessToken,
 *     onOpen: () => console.log('connected'),
 *     onMessage: (msg) => handle(msg),
 *     onClose: () => cleanup(),
 *     onError: (err) => report(err),
 *   })
 *   ws.connect()
 *   ws.send({ action: 'pause' })
 *   ws.close()
 */

/**
 * @param {Object} opts
 * @param {string}  opts.url       - WebSocket 路径 (不含 host，自动用当前页面的 ws:// 或 wss://)
 * @param {string}  [opts.token]   - JWT token (加到 query.token)
 * @param {Object}  [opts.params]  - 额外 query 参数
 * @param {Function} [opts.onOpen]
 * @param {Function} [opts.onMessage]  - 接收已解析的 JS 对象
 * @param {Function} [opts.onClose]    - 接收 CloseEvent
 * @param {Function} [opts.onError]
 * @param {number}   [opts.reconnectDelay=1000]   - 初始重连延迟 ms
 * @param {number}   [opts.reconnectMaxDelay=30000] - 最大重连延迟 ms
 * @param {number}   [opts.heartbeatInterval=25000] - 心跳间隔 ms
 * @param {string}   [opts.logPrefix='[WS]']       - 日志前缀
 * @returns {{ connect, close, send, isConnected: boolean }}
 */
export function createWS(opts) {
  const {
    url,
    token = '',
    params = {},
    onOpen,
    onMessage,
    onClose,
    onError,
    reconnectDelay = 1000,
    reconnectMaxDelay = 30000,
    heartbeatInterval = 25000,
    logPrefix = '[WS]',
  } = opts

  let socket = null
  let reconnectTimer = null
  let heartbeatTimer = null
  let shouldReconnect = true
  let attempt = 0

  const isConnected = () => socket && socket.readyState === WebSocket.OPEN

  function buildUrl() {
    const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = location.host
    let q = new URLSearchParams()
    if (token) q.set('token', token)
    if (params) {
      for (const [k, v] of Object.entries(params)) {
        if (v !== undefined && v !== null) q.set(k, String(v))
      }
    }
    return `${proto}//${host}${url}?${q.toString()}`
  }

  function clearTimers() {
    clearTimeout(reconnectTimer)
    clearInterval(heartbeatTimer)
    reconnectTimer = null
    heartbeatTimer = null
  }

  function scheduleReconnect() {
    if (!shouldReconnect) return
    const delay = Math.min(reconnectDelay * Math.pow(2, attempt), reconnectMaxDelay)
    attempt++
    console.warn(`${logPrefix} Reconnecting in ${delay}ms (attempt ${attempt})`)
    reconnectTimer = setTimeout(connect, delay)
  }

  function startHeartbeat() {
    clearInterval(heartbeatTimer)
    heartbeatTimer = setInterval(() => {
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type: 'ping' }))
      }
    }, heartbeatInterval)
  }

  function connect() {
    if (socket) {
      socket.onopen = null
      socket.onmessage = null
      socket.onclose = null
      socket.onerror = null
      if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
        socket.close()
      }
    }

    const wsUrl = buildUrl()
    console.info(`${logPrefix} Connecting → ${wsUrl.replace(/token=[^&]+/, 'token=***')}`)
    socket = new WebSocket(wsUrl)

    socket.onopen = (ev) => {
      attempt = 0
      startHeartbeat()
      console.info(`${logPrefix} Connected`)
      onOpen?.(ev)
    }

    socket.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data)
        onMessage?.(msg)
      } catch (e) {
        // 非 JSON 原文直接透传
        onMessage?.({ _raw: ev.data })
      }
    }

    socket.onclose = (ev) => {
      clearTimers()
      console.info(`${logPrefix} Closed: ${ev.code} ${ev.reason || ''}`)
      onClose?.(ev)
      if (shouldReconnect && !ev.wasClean) {
        scheduleReconnect()
      }
    }

    socket.onerror = (ev) => {
      console.error(`${logPrefix} Error`, ev)
      onError?.(ev)
    }
  }

  function send(data) {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      console.warn(`${logPrefix} Cannot send: not connected`)
      return false
    }
    const payload = typeof data === 'string' ? data : JSON.stringify(data)
    socket.send(payload)
    return true
  }

  function close(code = 1000, reason = 'normal') {
    shouldReconnect = false
    clearTimers()
    if (socket) {
      socket.close(code, reason)
    }
  }

  return { connect, close, send, isConnected }
}

/**
 * 快捷: 建立通知 WS (复用 notification store 的逻辑)
 */
export function createNotificationWS(token, { onMessage, onOpen, onClose, onError } = {}) {
  return createWS({
    url: '/ws/notifications',
    token,
    params: {},
    onOpen,
    onMessage,
    onClose,
    onError,
    heartbeatInterval: 25000,
    logPrefix: '[WS.Notif]',
  })
}
