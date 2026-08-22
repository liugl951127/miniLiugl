/**
 * @file HTTP 客户端封装 (T2-frontend-auth-ux 重构版)
 *
 * 统一封装 axios, 处理:
 *  - JWT Token 自动注入 (Authorization: Bearer)
 *  - **X-User-Id 自动注入** (从 userStore 读取, 业务接口无需手填)
 *  - **401 自动续期**: 检测 401 → 自动调 /api/v1/auth/refresh → 用新 token 重发原请求
 *    - 防 401 风暴: 5s 内只跳一次登录
 *    - 防并发 refresh: refreshing Promise 共享, 多个 401 只 refresh 一次
 *  - **友好错误提示**: 401 / 403 / 500 / 404 / network 分场景文案
 *  - **开发模式 console.error** + 生产模式静默 (统一由 useClientLog 上报)
 *  - 业务码处理 (BizException, code != 0)
 *  - 请求/响应拦截 (TraceId 透传, Idempotency-Key)
 *  - 适配 Vite 代理 (开发模式 /api 直连后端)
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

// ============== 全局状态 ==============
// V3.7.5+ 防止 401 风暴 (多接口同时 401)
let last401At = 0
// T2+: 防并发 refresh — 多个 401 共享一个 in-flight Promise
let refreshingPromise = null

// V5.8: traceId 全局 (每次请求带同一 traceId, 便于排查)
let globalTraceId = null
function getTraceId() {
  if (!globalTraceId) {
    globalTraceId = 'fe-' + Math.random().toString(36).substring(2, 14)
  }
  return globalTraceId
}

// ============== 开发模式判断 ==============
// Vite 暴露: import.meta.env.DEV / PROD / MODE
const isDev = (() => {
  try {
    return !!import.meta.env?.DEV
  } catch {
    return false
  }
})()

// ============== 错误日志工具 (T2 重构) ==============
/**
 * 统一错误日志输出 — 仅开发模式打印详细, 生产模式静默 (由 useClientLog 上报)
 *
 * <h2>使用场景</h2>
 * - 所有 HTTP 请求失败 (网络错误 / 4xx / 5xx / 业务码 != 0)
 *
 * <h2>格式 (开发模式)</h2>
 * [HTTP 错误] {method} {url}
 *   status: 500
 *   code: 1001
 *   message: xxx
 *
 * <h2>生产模式</h2>
 * - 不打印 console (避免性能开销 + 信息泄露)
 * - useClientLog composable 已经劫持 console, 自动批量上报
 *
 * @param tag  错误类型标签 (request/response/business)
 * @param info 错误详情
 */
function logHttpError(tag, info) {
  if (!isDev) return // 生产模式: 完全静默
  const summary = `[HTTP ${tag}] ${info.method || ''} ${info.url || ''}`
  console.groupCollapsed(`%c ${summary}`, 'color: #f56c6c; font-weight: bold')
  console.table({
    status: info.status || '-',
    code: info.code || '-',
    message: info.message || '-',
    traceId: info.traceId || '-',
    duration: info.duration || '-',
    timestamp: new Date().toISOString()
  })
  if (info.data !== undefined) console.log('data:', info.data)
  if (info.config) console.log('config:', info.config)
  if (info.error) console.log('error:', info.error)
  console.groupEnd()
}

// ============== axios 实例 ==============
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 60000
})

// ============== 请求拦截器 ==============
http.interceptors.request.use(
  (config) => {
    // V3.0.0: 路径前缀补齐
    const url = config.url || ''
    const isAbsolute = /^https?:\/\//i.test(url) || url.startsWith('//')
    const isPrefixed = url.startsWith('/api/') || url.startsWith('/ws') || url.startsWith('/sse') ||
                       url.startsWith('/actuator') || url.startsWith('/fallback') || url.startsWith('/api-docs')
    if (!isAbsolute && !isPrefixed && url.startsWith('/')) {
      config.url = '/api/v1' + url
    }
    const userStore = useUserStore()
    // V6.8.9+: 主动续期: token 剩余 < 5 分钟时静默刷新
    if (userStore.accessToken) {
      userStore.silentRefreshIfNeeded(false).catch(() => {})
    }
    // V6.8+: _skipAuth=true 时跳过 JWT 注入（外部 API Key 调用专用）
    if (!config._skipAuth && userStore.accessToken) {
      config.headers.Authorization = `Bearer ${userStore.accessToken}`
    }
    // T2+: 自动注入 X-User-Id (从 userStore 读; 业务接口无需手填)
    // - 优先级: 已显式设置 > store.profile.id > localStorage.userId > 'anonymous'
    // - _skipXUserId=true 时跳过 (外部系统调用专用)
    if (!config._skipXUserId && !config.headers['X-User-Id']) {
      const profileId = userStore.profile?.userId ?? userStore.profile?.id
      const lsUserId = (() => {
        try { return localStorage.getItem('userId') } catch { return null }
      })()
      const uid = profileId || lsUserId || 'anonymous'
      config.headers['X-User-Id'] = String(uid)
    }
    // V5.8: 每次请求新 traceId (单次请求全程追踪)
    config.headers['X-Trace-Id'] = 'fe-' + Date.now().toString(36) + Math.random().toString(36).substring(2, 8)
    // 幂等键: 写操作 (POST/PUT/DELETE) 防止重复提交
    if (['post', 'put', 'delete', 'patch'].includes((config.method || '').toLowerCase())) {
      if (!config.headers['Idempotency-Key']) {
        config.headers['Idempotency-Key'] = getTraceId() + '-' + Date.now()
      }
    }
    // V6.2+ 请求开始时间 (用于计算 duration)
    config.metadata = { startTime: Date.now() }
    return config
  },
  // 请求发送失败 (网络层错)
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

// ============== 401 智能续期: 共享 in-flight Promise ==============
/**
 * T2+: 多个 401 同时发生时, 共享一个 refresh 请求
 *  - 如果正在 refresh, 等待它完成
 *  - 完成后用新 token 继续重放
 *  - 失败: 返回 null (调用方走登出)
 */
async function doRefresh() {
  if (refreshingPromise) return refreshingPromise
  const userStore = useUserStore()
  if (!userStore.refreshToken) return null
  refreshingPromise = (async () => {
    try {
      const newToken = await userStore.refreshAccessToken()
      if (isDev) console.log('%c[HTTP] 401 → refresh 成功', 'color: #67c23a')
      return newToken
    } catch (e) {
      if (isDev) console.warn('[HTTP] refresh 失败:', e?.message || e)
      return null
    } finally {
      refreshingPromise = null
    }
  })()
  return refreshingPromise
}

// ============== 响应拦截器 ==============
http.interceptors.response.use(
  async (resp) => {
    // V6.8.9+: 检测服务端 X-Token-Refresh 提示, 静默刷新 token
    if (resp.headers['x-token-refresh'] === 'true') {
      const userStore = useUserStore()
      userStore.silentRefreshIfNeeded(true).catch(() => {})
    }

    let data = resp.data
    const respTraceId = resp.headers['x-trace-id']
    // V3.7.22+ 业务码处理 (Result 包装)
    if (data && typeof data === 'object' && data.code !== undefined && data.code !== 0) {
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
      err.__result = data
      return Promise.reject(err)
    }
    // V3.7.22+ 自动剥 Result.data (成功时)
    if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
      const original = data
      data = data.data
      if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
        data = data.data
      }
      if (data && typeof data === 'object') {
        data.__result = original
      }
      return data
    }
    return data
  },
  async (err) => {
    // ============== 1. 网络错误 (无 response, 如断网/超时/CORS) ==============
    if (!err.response) {
      const duration = err.config?.metadata?.startTime
        ? Date.now() - err.config.metadata.startTime
        : undefined
      logHttpError('网络错误', {
        url: err.config?.url,
        method: err.config?.method,
        message: err.message,
        duration: duration ? `${duration}ms` : '-',
        code: err.code,
        error: err
      })
      // T2+: 网络错误友好提示
      ElMessage.error({
        message: '网络连接失败, 请检查后端是否启动',
        duration: 4000,
        showClose: true,
      })
      return Promise.reject(err)
    }

    // ============== 2. HTTP 错误 (4xx/5xx) ==============
    const status = err.response?.status
    const code = err.response?.data?.code
    const msg = err.response?.data?.message || err.message
    const respTraceId = err.response?.headers?.['x-trace-id']
    const duration = err.config?.metadata?.startTime
      ? Date.now() - err.config.metadata.startTime
      : undefined

    if (status && status >= 400) {
      logHttpError(`HTTP ${status}`, {
        url: err.config?.url,
        method: err.config?.method,
        status,
        code,
        message: msg,
        traceId: respTraceId,
        duration: duration ? `${duration}ms` : '-',
        data: err.response?.data
      })
    }

    // ============== 3. 401 / 1002: 自动 refresh + 重放 ==============
    if ((status === 401 || code === 1002) && !err.config?._retry) {
      // 登录页 401 不跳走
      const isLoginRequest = err.config?.url?.includes('/auth/login') ||
                              err.config?.url?.includes('/auth/refresh') ||
                              err.config?.url?.includes('/auth/register') ||
                              err.config?.url?.includes('/sessions')
      if (isLoginRequest) {
        // 登录接口失败: 弹友好提示
        if (status === 401) {
          ElMessage.error('登录失败: ' + (msg || '账号或密码错误'))
        }
        return Promise.reject(err)
      }
      // V3.7.10+ silent 模式: 401 不跳走
      if (err.config?._silent) {
        return Promise.reject(err)
      }
      // T2+: 用共享 Promise 防并发 refresh
      const newToken = await doRefresh()
      if (newToken) {
        err.config._retry = true
        err.config.headers.Authorization = `Bearer ${newToken}`
        if (isDev) console.log(`[HTTP] 401 → refresh → 重放 ${err.config.method?.toUpperCase()} ${err.config.url}`)
        return http(err.config)
      }
      // refresh 失败 → 5s 防风暴 + 跳登录
      const now = Date.now()
      if (now - last401At < 10000) {
        return Promise.reject(err)
      }
      last401At = now
      ElMessage.warning('登录已过期, 正在跳转登录页...')
      try { await useUserStore().logout() } catch (_) {}
      // 跳登录 (用 replace 避免回退又触发 401)
      if (router.currentRoute.value.path !== '/login') {
        router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      }
      return Promise.reject(err)
    }

    // ============== 4. 403: 权限不足 — 不跳转, 友好提示 ==============
    if (status === 403) {
      const userStore = useUserStore()
      const roles = userStore.profile?.roles || []
      const needRole = roles.includes('ADMIN') || roles.includes('SUPER_ADMIN') ? '' : ' (需要 ADMIN 角色)'
      ElMessage.error({
        message: '无权限执行此操作' + needRole,
        duration: 4000,
        showClose: true,
      })
      return Promise.reject(err)
    }

    // ============== 5. 404: 接口不存在 — 静默 + console.warn ==============
    if (status === 404) {
      if (isDev) console.warn(`[HTTP 404] ${err.config?.method?.toUpperCase()} ${err.config?.url} (接口不存在或已下线)`)
      // 不弹 toast, 避免误打扰; 调用方自己处理
      return Promise.reject(err)
    }

    // ============== 6. 5xx: 服务器错误 — 友好提示 + traceId ==============
    if (status >= 500) {
      let specificMsg = msg
      if (status === 500) specificMsg = msg || '服务内部错误'
      else if (status === 502) specificMsg = msg || '网关错误 (后端不可用)'
      else if (status === 503) specificMsg = msg || '服务暂不可用 (维护中或过载)'
      else if (status === 504) specificMsg = msg || '网关超时 (后端响应慢)'
      else specificMsg = msg || '服务异常, 请稍后重试'

      const fullMsg = respTraceId ? `${specificMsg} [traceId: ${respTraceId}]` : specificMsg
      ElMessage.error({
        message: fullMsg,
        duration: 5000,
        showClose: true,
      })
      return Promise.reject(err)
    }

    // ============== 7. 其他 4xx (400/422 等): 用后端 message ==============
    if (status >= 400) {
      ElMessage.error({
        message: msg || `请求错误 (${status})`,
        duration: 4000,
        showClose: true,
      })
    }
    return Promise.reject(err)
  }
)

export default http
