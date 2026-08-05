/**
 * @file HTTP 客户端封装 (V3.5.12+)
 *
 * 统一封装 axios, 处理:
 *  - JWT Token 自动注入 (Authorization: Bearer)
 *  - 错误统一处理 (BizException, 401 跳转登录)
 *  - 请求/响应拦截 (TraceId 透传)
 *  - 适配 Vite 代理 (开发模式 /api 直连后端)
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'
let last401At = 0  // V3.7.5+ 防止 401 风暴 (多接口同时 401)


// V5.8: traceId 全局 (每次请求带同一 traceId, 便于排查)
let globalTraceId = null
function getTraceId() {
  if (!globalTraceId) {
    globalTraceId = 'fe-' + Math.random().toString(36).substring(2, 14)
  }
  return globalTraceId
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 60000
})

// 请求拦截器: 带 token + traceId
http.interceptors.request.use(
  (config) => {
    // V3.0.0: 路径前缀补齐
    // 不补: 绝对 URL (http://, https://, //) 和已经 /api 开头 / /ws / /sse / /actuator 的
    // 目的: 前端 API 调用可省略 /api/v1 前缀, http.js 自动补
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
    return config
  },
  (err) => Promise.reject(err)
)

// 响应拦截器: 业务码处理 + 401 自动 refresh
http.interceptors.response.use(
  (resp) => {
    let data = resp.data
    // V5.8: 把 traceId 暴露到全局, 错误提示可显示
    const respTraceId = resp.headers['x-trace-id']
    // V3.7.22+ 业务码处理 (Result 包装)
    if (data && typeof data === 'object' && data.code !== undefined && data.code !== 0) {
      ElMessage.error({
        message: data.message || '请求失败',
        duration: 3500,
        showClose: true,
      })
      const err = new Error(data.message || 'Request failed')
      err.code = data.code
      err.traceId = respTraceId
      // V3.7.24+ 成功/失败统一 __result 字段 (业务 e.__result 始终可用)
      err.__result = data
      return Promise.reject(err)
    }
    // V3.7.22+ 自动剥 Result.data (成功时)
    // 业务代码 res.data 直接拿到业务数据, 不用 res.data.data
    // 兼容: data 不是 Result 包装 (没 code 字段) 时, 直接返回原 data
    if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
      // 保留 code/message/timestamp 在 result 字段, 方便业务需要时访问
      const original = data
      data = data.data
      // V3.7.22+ 兼容老代码 res.data.data.data 模式 (双层嵌套)
      if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
        data = data.data
      }
      // V3.7.24+ 挂载 __result 给业务 (成功/失败统一字段名)
      if (data && typeof data === 'object') {
        data.__result = original
      }
      return data
    }
    return data
  },
  async (err) => {
    const status = err.response?.status
    const code = err.response?.data?.code
    const msg = err.response?.data?.message || err.message
    const respTraceId = err.response?.headers?.['x-trace-id']

    // V5.8: 401 / 1002 业务码 → 尝试 refresh 后重放
    if ((status === 401 || code === 1002) && !err.config?._retry) {
      // V3.7.4: 登录页 401 不跳走, 让 Login.vue 显示错误
      const isLoginRequest = err.config?.url?.includes('/auth/login') || err.config?.url?.includes('/sessions')
      // V3.7.4: 登录页 401 不跳走, 让 Login.vue 显示错误
      if (isLoginRequest) {
        return Promise.reject(err)
      }
      // V3.7.10+ silent 模式: 401 不跳走, 让调用方 catch 处理
      if (err.config?._silent) {
        return Promise.reject(err)
      }
      const userStore = useUserStore()
      if (userStore.refreshToken) {
        try {
          err.config._retry = true
          const newToken = await userStore.refreshAccessToken()
          err.config.headers.Authorization = `Bearer ${newToken}`
          return http(err.config)
        } catch (_) {
          // refresh 失败 → 走登出
        }
      }
      // V3.7.5+ 5s 内只跳一次 (防风暴)
      const now = Date.now()
      if (now - last401At < 10000) {
        return Promise.reject(err)  // V3.7.8+ 10s 防风暴
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