import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

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
    const data = resp.data
    // V5.8: 把 traceId 暴露到全局, 错误提示可显示
    const respTraceId = resp.headers['x-trace-id']
    if (data && typeof data === 'object' && data.code !== undefined && data.code !== 0) {
      ElMessage.error({
        message: data.message || '请求失败',
        duration: 3500,
        showClose: true,
      })
      const err = new Error(data.message || 'Request failed')
      err.code = data.code
      err.traceId = respTraceId
      return Promise.reject(err)
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
      ElMessage.error('登录已过期，请重新登录')
      await useUserStore().logout()
      router.push('/login')
      return Promise.reject(err)
    }

    // V5.8: 5xx 错误带 traceId 提示 (方便排查)
    const fullMsg = status >= 500 && respTraceId
      ? `${msg} [traceId: ${respTraceId}]`
      : (msg || '网络异常')
    ElMessage.error({
      message: fullMsg,
      duration: 5000,
      showClose: true,
    })
    return Promise.reject(err)
  }
)

export default http