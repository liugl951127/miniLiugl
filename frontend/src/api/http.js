import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 60000
})

http.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.accessToken) {
      config.headers.Authorization = `Bearer ${userStore.accessToken}`
    }
    return config
  },
  (err) => Promise.reject(err)
)

http.interceptors.response.use(
  (resp) => {
    const data = resp.data
    if (data && typeof data === 'object' && data.code !== undefined && data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || 'Request failed'))
    }
    return data
  },
  async (err) => {
    const status = err.response?.status
    const code = err.response?.data?.code
    const msg = err.response?.data?.message || err.message

    // 401 或业务码 1002（未登录/过期）→ 尝试 refresh 后重放一次
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

    ElMessage.error(msg || '网络异常')
    return Promise.reject(err)
  }
)

export default http
