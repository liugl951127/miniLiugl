/**
 * @file HTTP 客户端封装 (V6.2+ 错误日志增强)
 *
 * 统一封装 axios, 处理:
 *  - JWT Token 自动注入 (Authorization: Bearer)
 *  - 错误统一处理 (BizException, 401 跳转登录)
 *  - 请求/响应拦截 (TraceId 透传)
 *  - 适配 Vite 代理 (开发模式 /api 直连后端)
 *  - **V6.2+ 错误日志**: 所有网络/接口/业务错误自动 console.log
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

// V3.7.5+ 防止 401 风暴 (多接口同时 401)
let last401At = 0

// V5.8: traceId 全局 (每次请求带同一 traceId, 便于排查)
let globalTraceId = null
function getTraceId() {
  if (!globalTraceId) {
    globalTraceId = 'fe-' + Math.random().toString(36).substring(2, 14)
  }
  return globalTraceId
}

// ============== V6.2+ 错误日志工具 ==============
/**
 * 统一错误日志输出
 *
 * <h2>使用场景</h2>
 * - 所有 HTTP 请求失败 (网络错误 / 4xx / 5xx / 业务码 != 0)
 * - 控制台可看到完整请求/响应信息, 方便 debug
 *
 * <h2>格式</h2>
 * [HTTP 错误] {method} {url}
 *   status: 500
 *   code: 1001
 *   message: xxx
 *   traceId: fe-xxx
 *   data: { ... }
 *
 * @param tag  错误类型标签 (request/response/business)
 * @param info 错误详情
 */
function logHttpError(tag, info) {
  // 简洁格式, 折叠 1 行
  const summary = `[HTTP ${tag}] ${info.method || ''} ${info.url || ''}`
  console.groupCollapsed(`%c ${summary}`, 'color: #f56c6c; font-weight: bold')
  // 详情表
  console.table({
    status: info.status || '-',
    code: info.code || '-',
    message: info.message || '-',
    traceId: info.traceId || '-',
    duration: info.duration || '-',
    timestamp: new Date().toISOString()
  })
  // 原始数据
  if (info.data !== undefined) console.log('data:', info.data)
  if (info.config) console.log('config:', info.config)
  if (info.error) console.log('error:', info.error)
  console.groupEnd()
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 60000
})

// 请求拦截器: 带 token + traceId + 计时
http.interceptors.request.use(
  (config) => {
    // V3.0.0: 路径前缀补齐
    // 不补: 绝对 URL (http://, https://, //) 和已经 /api 开头 / /ws / /sse / /actuator 的
    const url = config.url || ''
    const isAbsolute = /^https?:\/\//i.test(url) || url.startsWith('//')
    const isPrefixed = url.startsWith('/api/') || url.startsWith('/ws') || url.startsWith('/sse') ||
                       url.startsWith('/actuator') || url.startsWith('/fallback') || url.startsWith('/api-docs')
    if (!isAbsolute && !isPrefixed && url.startsWith('/')) {
      config.url = '/api/v1' + url
    }
    const userStore = useUserStore()
    if (userStore.accessToken) {
      config.headers.Authorization = `Bearer ${userStore.accessToken}`
    }
    // V5.8: 每次请求新 traceId (单次请求全程追踪)
    config.headers['X-Trace-Id'] = 'fe-' + Date.now().toString(36) + Math.random().toString(36).substring(2, 8)
    // 幂等键: 写操作 (POST/PUT/DELETE) 防止重复提交
    if (['post', 'put', 'delete', 'patch'].includes((config.method || '').toLowerCase())) {
      // 客户端生成 Idempotency-Key (gateway 不强制, 后端业务可读)
      if (!config.headers['Idempotency-Key']) {
        config.headers['Idempotency-Key'] = getTraceId() + '-' + Date.now()
      }
    }
    // V6.2+ 请求开始时间 (用于计算 duration)
    config.metadata = { startTime: Date.now() }
    return config
  },
  // V6.2+ 请求发送失败 (网络层错)
  (err) => {
    logHttpError('请求失败', {
      url: err.config?.url,
      method: err.config?.method,
      message: err.message,
      error: err
    })
    return Promise.reject(err)
  }
)

// 响应拦截器: 业务码处理 + 401 自动 refresh
http.interceptors.response.use(
  (resp) => {
    let data = resp.data
    // V5.8: 把 traceId 暴露到全局
    const respTraceId = resp.headers['x-trace-id']
    // V3.7.22+ 业务码处理 (Result 包装)
    if (data && typeof data === 'object' && data.code !== undefined && data.code !== 0) {
      // V6.2+ 业务错误 console.log (code != 0 但 HTTP 200)
      const duration = resp.config?.metadata?.startTime
        ? Date.now() - resp.config.metadata.startTime
        : undefined
      logHttpError('业务错误', {
        url: resp.config?.url,
        method: resp.config?.method,
        code: data.code,
        message: data.message,
        traceId: respTraceId,
        duration: duration ? `${duration}ms` : '-',
        data: data
      })

      ElMessage.error({
        message: data.message || '请求失败',
        duration: 3500,
        showClose: true,
      })
      const err = new Error(data.message || 'Request failed')
      err.code = data.code
      err.traceId = respTraceId
      // V3.7.24+ 成功/失败统一 __result 字段
      err.__result = data
      return Promise.reject(err)
    }
    // V3.7.22+ 自动剥 Result.data (成功时)
    if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
      // 保留 code/message/timestamp 在 result 字段
      const original = data
      data = data.data
      // V3.7.22+ 兼容老代码 res.data.data.data 模式 (双层嵌套)
      if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
        data = data.data
      }
      // V3.7.24+ 挂载 __result 给业务
      if (data && typeof data === 'object') {
        data.__result = original
      }
      return data
    }
    return data
  },
  async (err) => {
    // ============== V6.2+ 错误日志 ==============
    // 1. 网络错误 (无 response, 如断网/超时/CORS)
    if (!err.response) {
      const duration = err.config?.metadata?.startTime
        ? Date.now() - err.config.metadata.startTime
        : undefined
      logHttpError('网络错误', {
        url: err.config?.url,
        method: err.config?.method,
        message: err.message,
        duration: duration ? `${duration}ms` : '-',
        code: err.code,  // ECONNABORTED, ENOTFOUND 等
        error: err
      })
    }

    // 2. 业务错误处理
    const status = err.response?.status
    const code = err.response?.data?.code
    const msg = err.response?.data?.message || err.message
    const respTraceId = err.response?.headers?.['x-trace-id']
    const duration = err.config?.metadata?.startTime
      ? Date.now() - err.config.metadata.startTime
      : undefined

    // V6.2+ HTTP 4xx/5xx 错误 console.log
    if (status && status >= 400) {
      logHttpError(`HTTP ${status}`, {
        url: err.config?.url,
        method: err.config?.method,
        status: status,
        code: code,
        message: msg,
        traceId: respTraceId,
        duration: duration ? `${duration}ms` : '-',
        data: err.response?.data
      })
    }

    // V5.8: 401 / 1002 业务码 → 尝试 refresh 后重放
    if ((status === 401 || code === 1002) && !err.config?._retry) {
      // V3.7.4: 登录页 401 不跳走
      const isLoginRequest = err.config?.url?.includes('/auth/login') || err.config?.url?.includes('/sessions')
      if (isLoginRequest) {
        return Promise.reject(err)
      }
      // V3.7.10+ silent 模式: 401 不跳走
      if (err.config?._silent) {
        return Promise.reject(err)
      }
      const userStore = useUserStore()
      if (userStore.refreshToken) {
        try {
          err.config._retry = true
          const newToken = await userStore.refreshAccessToken()
          err.config.headers.Authorization = `Bearer ${newToken}`
          console.log(`[HTTP] 401 → refresh → 重放 ${err.config.method?.toUpperCase()} ${err.config.url}`)
          return http(err.config)
        } catch (_) {
          console.warn('[HTTP] refresh 失败, 跳登录')
          // refresh 失败 → 走登出
        }
      }
      // V3.7.5+ 5s 内只跳一次 (防风暴)
      const now = Date.now()
      if (now - last401At < 10000) {
        return Promise.reject(err)
      }
      last401At = now
      ElMessage.error('登录已过期，请重新登录')
      await useUserStore().logout()
      router.push('/login')
      return Promise.reject(err)
    }

    // V3.7.6+ 错误码细化 (500/502/503/504 区分)
    let specificMsg = msg
    if (status === 500) specificMsg = msg || '服务内部错误'
    else if (status === 502) specificMsg = msg || '网关错误 (后端不可用)'
    else if (status === 503) specificMsg = msg || '服务暂不可用 (维护中或过载)'
    else if (status === 504) specificMsg = msg || '网关超时 (后端响应慢)'
    else if (status >= 500) specificMsg = msg || '服务异常'

    const fullMsg = status >= 500 && respTraceId
      ? `${specificMsg} [traceId: ${respTraceId}]`
      : (specificMsg || '网络异常')
    ElMessage.error({
      message: fullMsg,
      duration: 5000,
      showClose: true,
    })
    return Promise.reject(err)
  }
)

export default http
