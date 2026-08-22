/**
 * @file useClientLog.js — 前端日志上报 composable (V6.8+)
 *
 * 功能:
 *   1. 劫持 console.log / warn / error / info / debug
 *   2. 批量缓存到内存队列 (最多 50 条)
 *   3. 每 3s 自动 flush 到后端 /api/v1/logs/client
 *   4. 上报失败不阻塞主线程，静默重试
 *
 * 使用: 在 main.js / App.vue 顶部 import 即可自动生效
 *   import '@/composables/useClientLog'
 */
import { ref } from 'vue'

// ============== 配置 ==============
/** V6.8.1: 改用 /logs/save 直接 append 写文件，不过队列 */
const LOG_API = '/api/v1/logs/save'
const FLUSH_INTERVAL_MS = 3000
const BATCH_SIZE = 50
const LOG_LEVELS = ['log', 'warn', 'error', 'info', 'debug']

// ============== 全局状态 ==============
const isEnabled = ref(true)
const logBuffer = []
let flushTimer = null

// ============== 用户上下文 (动态获取) ==============
function getUserContext() {
  try {
    const stored = localStorage.getItem('minimax-user')
    const user = stored ? JSON.parse(stored) : null
    return {
      userId: user?.state?.userId || user?.userId || null,
      username: user?.state?.username || user?.username || null,
      token: user?.state?.accessToken || user?.accessToken || null,
    }
  } catch {
    return { userId: null, username: null, token: null }
  }
}

function getTraceId() {
  try {
    return sessionStorage.getItem('traceId') || ''
  } catch {
    return ''
  }
}

// ============== 单条日志构建 ==============
function buildEntry(level, args) {
  let msg = ''
  // 格式化: 支持 %c 样式占位 (只取第一段纯文本)
  for (const arg of args) {
    if (arg === undefined) msg += 'undefined'
    else if (arg === null) msg += 'null'
    else if (typeof arg === 'object') {
      try { msg += JSON.stringify(arg, null, 2) } catch { msg += String(arg) }
    } else {
      msg += String(arg)
    }
    msg += ' '
  }
  msg = msg.trim()

  const entry = {
    level,
    msg,
    url: window.location.href,
    userId: getUserContext().userId,
    traceId: getTraceId(),
    time: new Date().toISOString(),
    ua: navigator.userAgent,
  }

  // error/warn 额外捕获 stack
  if ((level === 'error' || level === 'warn') && args[0] instanceof Error) {
    entry.stack = args[0].stack || args[0].message
    entry.msg = args[0].message || msg
  }

  return entry
}

// ============== 发送批次到后端 ==============
async function flushLogs() {
  if (!isEnabled.value || logBuffer.length === 0) return
  const batch = logBuffer.splice(0, BATCH_SIZE)
  try {
    const ctx = getUserContext()
    // V6.8.1: 直接写文件端点，不过队列，立即落盘
    // _skipAuth: true 告诉 gateway 安全过滤器跳过 JWT 校验
    await fetch(LOG_API, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': ctx.userId ? String(ctx.userId) : 'anonymous',
        'X-User-Name': ctx.username || 'anonymous',
        '_skipAuth': 'true',        // gateway bypass JWT
      },
      body: JSON.stringify(batch),
      // 日志上报不阻塞：失败静默，不影响业务
    })
  } catch (_) {
    // 上报失败，放回 buffer 等待下次 flush
    logBuffer.unshift(...batch)
  }
}

// ============== 劫持 console ==============
function patchConsole() {
  const original = {}
  for (const level of LOG_LEVELS) {
    original[level] = console[level].bind(console)
    console[level] = (...args) => {
      // 1. 保留原始行为 (控制台仍可见)
      original[level](...args)
      // 2. 缓存到 buffer
      if (isEnabled.value) {
        const entry = buildEntry(level, args)
        logBuffer.push(entry)
        // buffer 满则立即 flush
        if (logBuffer.length >= BATCH_SIZE) {
          flushLogs()
        }
      }
    }
  }
  return original
}

// ============== 劫持 window.onerror ==============
function patchOnError() {
  const original = window.onerror
  window.onerror = (msg, src, line, col, err) => {
    if (err) {
      const entry = buildEntry('error', [err])
      logBuffer.push(entry)
    }
    if (typeof original === 'function') return original(msg, src, line, col, err)
    return false
  }
}

// ============== 劫持 unhandledrejection ==============
function patchUnhandledRejection() {
  window.addEventListener('unhandledrejection', (e) => {
    const entry = buildEntry('error', [e.reason instanceof Error ? e.reason : String(e.reason)])
    entry.type = 'unhandledrejection'
    logBuffer.push(entry)
    // 高优先级: 立即 flush (不等 3s)
    if (logBuffer.length >= 5) {
      flushLogs()
    }
  })
}

// ============== Vue 错误处理 ==============
let vueApp = null
function patchVueErrorHandler(app) {
  if (!app) return
  vueApp = app
  app.config.errorHandler = (err, instance, info) => {
    const entry = buildEntry('error', [
      err instanceof Error ? err : String(err),
      '[Vue Info]', info,
    ])
    entry.vue = true
    logBuffer.push(entry)
    // 仍抛给原 handler
    throw err
  }
}

// ============== 启动/停止 ==============
function start() {
  if (flushTimer) return
  patchConsole()
  patchOnError()
  patchUnhandledRejection()
  flushTimer = setInterval(flushLogs, FLUSH_INTERVAL_MS)
}

function stop() {
  if (flushTimer) {
    clearInterval(flushTimer)
    flushTimer = null
  }
  flushLogs()
  isEnabled.value = false
}

function setEnabled(enabled) {
  isEnabled.value = enabled
}

// 自动启动 (模块 import 时生效)
if (typeof window !== 'undefined') {
  start()
}

// ============== 导出 ==============
export { start, stop, setEnabled, isEnabled, flushLogs, patchVueErrorHandler }
export default { start, stop, setEnabled, isEnabled, flushLogs, patchVueErrorHandler }
