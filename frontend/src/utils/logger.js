/**
 * 通用前端日志工具 (V6.8.1+)
 *
 * 设计目标:
 * 1. 4 级: debug / info / warn / error
 * 2. 时间戳 + 模块前缀 + 颜色
 * 3. 历史记录 (localStorage 保留 100 条, 跨刷新)
 * 4. 远程上报 (可选, 默认关闭)
 * 5. 一键导出 (download 日志)
 *
 * 使用:
 *   import logger, { useLogger } from '@/utils/logger'
 *   logger.info('User', '登录成功', { userId: 1 })
 *   logger.error('API', '请求失败', err)
 *   const log = useLogger()
 *   log.warn('Order', '订单超时', { orderId: '123' })
 *
 *   // 一键导出
 *   import { downloadLogs } from '@/utils/logger'
 *   downloadLogs()
 */

// ============== 颜色 ==============
const COLORS = {
  debug:   'color: #909399; font-weight: normal',     // 灰
  info:    'color: #409eff; font-weight: bold',       // 蓝
  warn:    'color: #e6a23c; font-weight: bold',       // 橙
  error:   'color: #f56c6c; font-weight: bold',       // 红
  success: 'color: #67c23a; font-weight: bold',       // 绿
}

const LEVELS = ['debug', 'info', 'warn', 'error', 'success']
const LEVEL_VALUE = { debug: 0, info: 1, warn: 2, error: 3, success: 1 }

// ============== 配置 ==============
const LS_KEY = 'minimax_log'
const MAX_LOG = 100
const config = {
  level: 'debug',           // 当前启用的最低级别
  console: true,            // 是否输出到 console
  storage: true,            // 是否存到 localStorage
  report: false,            // 是否远程上报
  reportUrl: '/api/v1/ai/log/collect',
  reportInterval: 30000,    // 上报间隔 (ms)
  reportBuffer: [],         // 待上报队列
  reportTimer: null,
}

try {
  const saved = localStorage.getItem(LS_KEY + '_config')
  if (saved) Object.assign(config, JSON.parse(saved))
} catch (e) { /* */ }

function saveConfig() {
  try {
    const { reportTimer: _t, reportBuffer: _b, ...persist } = config
    localStorage.setItem(LS_KEY + '_config', JSON.stringify(persist))
  } catch (e) { /* */ }
}

// ============== 历史 ==============
const _history = []

try {
  const saved = localStorage.getItem(LS_KEY)
  if (saved) _history.push(...JSON.parse(saved))
} catch (e) { /* */ }

function persist() {
  if (!config.storage) return
  try {
    localStorage.setItem(LS_KEY, JSON.stringify(_history.slice(-MAX_LOG)))
  } catch (e) { /* quota */ }
}

function timestamp() {
  const d = new Date()
  return d.toISOString().replace('T', ' ').replace('Z', '')
}

// ============== 核心 ==============
function log(level, module, ...args) {
  if (LEVEL_VALUE[level] < LEVEL_VALUE[config.level]) return
  const entry = {
    ts: Date.now(),
    iso: new Date().toISOString(),
    level,
    module,
    args: args.map(a => {
      try { return a instanceof Error ? { name: a.name, message: a.message, stack: a.stack } : a }
      catch { return String(a) }
    })
  }
  _history.push(entry)
  if (_history.length > MAX_LOG * 2) _history.splice(0, _history.length - MAX_LOG)
  persist()

  if (config.console) {
    const style = COLORS[level] || COLORS.info
    const prefix = `%c[${timestamp()}] [${level.toUpperCase()}] [${module}]`
    const consoleMethod = level === 'error' ? console.error
                       : level === 'warn'  ? console.warn
                       : level === 'debug' ? console.debug
                       : console.log
    consoleMethod(prefix, style, ...args)
  }

  if (config.report && level === 'error') {
    config.reportBuffer.push(entry)
    scheduleReport()
  }
}

function scheduleReport() {
  if (config.reportTimer) return
  config.reportTimer = setTimeout(() => {
    flushReport()
  }, config.reportInterval)
}

async function flushReport() {
  if (!config.report || config.reportBuffer.length === 0) return
  const buffer = config.reportBuffer.splice(0, config.reportBuffer.length)
  config.reportTimer = null
  try {
    await fetch(config.reportUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ logs: buffer, ua: navigator.userAgent, url: location.href })
    })
  } catch (e) {
    // 上报失败 - 重新入队 (避免丢失)
    config.reportBuffer.unshift(...buffer)
  }
}

// ============== 公开 API ==============
const logger = {
  debug:   (module, ...args) => log('debug', module, ...args),
  info:    (module, ...args) => log('info', module, ...args),
  warn:    (module, ...args) => log('warn', module, ...args),
  error:   (module, ...args) => log('error', module, ...args),
  success: (module, ...args) => log('success', module, ...args),
  log,

  // 配置
  setLevel(level) {
    if (LEVELS.includes(level)) {
      config.level = level
      saveConfig()
    }
  },
  getLevel: () => config.level,
  enableReport(url) {
    config.report = true
    if (url) config.reportUrl = url
    saveConfig()
  },
  disableReport() { config.report = false; saveConfig() },

  // 历史
  getHistory: (filter = {}) => {
    let h = _history.slice()
    if (filter.level) h = h.filter(e => e.level === filter.level)
    if (filter.module) h = h.filter(e => e.module === filter.module)
    if (filter.since) h = h.filter(e => e.ts > filter.since)
    return h
  },
  clearHistory() {
    _history.length = 0
    persist()
  },
  flush: flushReport,
}

// ============== 一键导出 ==============
export function downloadLogs() {
  const content = _history
    .map(e => `[${e.iso}] [${e.level.toUpperCase()}] [${e.module}] ${JSON.stringify(e.args)}`)
    .join('\n')
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `minimax-log-${new Date().toISOString().replace(/[:.]/g, '-')}.log`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

// ============== Vue composable ==============
export function useLogger() {
  return logger
}

// 自动挂载到 window 方便调试
if (typeof window !== 'undefined') {
  window.__minimax_log = logger
  window.__minimax_downloadLogs = downloadLogs
}

export default logger
