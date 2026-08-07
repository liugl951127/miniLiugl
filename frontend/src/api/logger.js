/**
 * API 调试日志工具 (V6.2+)
 *
 * 使用:
 *   import apiLog from './logger'
 *   apiLog.request('POST', '/api/ai/generate', payload)
 *   apiLog.response('POST', '/api/ai/generate', 200, data, 123)
 *   apiLog.error('POST', '/api/ai/generate', error)
 *   apiLog.businessError('POST', '/api/ai/generate', { code: 1, message: 'xx' })
 *
 * 效果: 控制台可看到分组日志 (可折叠)
 *   [API 请求] POST /api/ai/generate
 *   [API 响应] 200 (123ms)  ← 绿色
 *   [API 错误] HTTP 500      ← 红色
 *   [API 业务错误] code=1    ← 橙色
 */

// ============== 颜色常量 ==============
const COLORS = {
  request: 'color: #409eff; font-weight: bold',     // 蓝
  response: 'color: #67c23a; font-weight: bold',    // 绿
  error: 'color: #f56c6c; font-weight: bold',       // 红
  business: 'color: #e6a23c; font-weight: bold',    // 橙
  warn: 'color: #909399'                             // 灰
}

// ============== 工具函数 ==============
function formatDuration(ms) {
  if (ms === undefined || ms === null) return '-'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(2)}s`
}

function getTime() {
  return new Date().toISOString().split('T')[1].replace('Z', '')
}

/**
 * 请求日志
 * @param {string} method  HTTP 方法
 * @param {string} url     请求 URL
 * @param {object} [data]  请求数据
 * @param {object} [config] axios config
 */
export function request(method, url, data, config) {
  const summary = `[API 请求] ${method} ${url}`
  console.groupCollapsed(`%c${summary}`, COLORS.request)
  console.log('time:', getTime())
  if (data !== undefined) console.log('data:', data)
  if (config?.headers) console.log('headers:', config.headers)
  console.groupEnd()
}

/**
 * 响应日志 (成功)
 * @param {string} method
 * @param {string} url
 * @param {number} status
 * @param {object} data
 * @param {number} duration 耗时 (ms)
 */
export function response(method, url, status, data, duration) {
  const summary = `[API 响应] ${method} ${url} → ${status} (${formatDuration(duration)})`
  console.groupCollapsed(`%c${summary}`, COLORS.response)
  console.log('time:', getTime())
  console.log('duration:', formatDuration(duration))
  if (data !== undefined) console.log('data:', data)
  console.groupEnd()
}

/**
 * 错误日志 (HTTP 4xx/5xx 或网络错误)
 * @param {string} method
 * @param {string} url
 * @param {object} error axios error
 */
export function error(method, url, error) {
  const status = error.response?.status
  const code = error.response?.data?.code
  const message = error.response?.data?.message || error.message
  const traceId = error.response?.headers?.['x-trace-id']
  const summary = `[API 错误] ${method} ${url} → HTTP ${status || 'NETWORK'}` + (code ? ` (code=${code})` : '')
  console.groupCollapsed(`%c${summary}`, COLORS.error)
  console.log('time:', getTime())
  console.log('status:', status || '-')
  console.log('code:', code || '-')
  console.log('message:', message)
  console.log('traceId:', traceId || '-')
  if (error.config) {
    console.log('request config:', {
      url: error.config.url,
      method: error.config.method,
      data: error.config.data
    })
  }
  if (error.response?.data) console.log('response data:', error.response.data)
  console.groupEnd()
}

/**
 * 业务错误日志 (HTTP 200 但 code != 0)
 * @param {string} method
 * @param {string} url
 * @param {object} result 业务 Result {code, message, data}
 */
export function businessError(method, url, result) {
  const summary = `[API 业务错误] ${method} ${url} → code=${result.code}`
  console.groupCollapsed(`%c${summary}`, COLORS.business)
  console.log('time:', getTime())
  console.log('code:', result.code)
  console.log('message:', result.message)
  console.log('data:', result)
  console.groupEnd()
}

/**
 * 警告日志 (黄色)
 */
export function warn(message, ...args) {
  console.warn(`%c${message}`, COLORS.warn, ...args)
}

/**
 * 调试日志 (灰)
 */
export function debug(message, ...args) {
  console.log(`%c${message}`, COLORS.warn, ...args)
}

// 默认导出 (对象)
export default { request, response, error, businessError, warn, debug }
