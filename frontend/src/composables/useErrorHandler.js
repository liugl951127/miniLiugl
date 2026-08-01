/**
 * @file composables/useErrorHandler.js (V3.5.92+)
 * 统一错误处理 composable
 *
 * 解决问题: 之前各 view 自己 try/catch 写 ElMessage.error, 不一致
 * 这个 composable 统一 401/403/404/500/网络错/超时 处理
 *
 * 关键设计:
 *  1. 401 (token 过期/无效) - 清 token + 跳登录 + 跳前路由
 *  2. 403 (无权限) - 提示 + 可选跳首页
 *  3. 404 (资源不存在) - 友好提示
 *  4. 500+ (服务端错) - 提示 + 建议重试
 *  5. 网络错/超时 - 提示 + 离线状态
 *  6. 业务错 (res.code !== 0) - 用 res.message
 */
import { ElMessage, ElNotification } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

// 模块级状态 - 跨 view 共享
let last401At = 0  // 防止 401 风暴 (多接口同时 401)
const isOnline = ref(typeof navigator !== 'undefined' ? navigator.onLine : true)

if (typeof window !== 'undefined') {
  window.addEventListener('online', () => {
    isOnline.value = true
    ElMessage.success('🌐 网络已恢复')
  })
  window.addEventListener('offline', () => {
    isOnline.value = false
    ElNotification({
      title: '📡 网络断开',
      message: '请检查网络连接, 操作将在恢复后自动重试',
      type: 'warning',
      duration: 0,
      position: 'top-right'
    })
  })
}

import { ref } from 'vue'

/**
 * 统一错误处理
 * @param {Error|object} err - axios error 或普通 Error
 * @param {object} options
 * @param {string} options.silent - 静默模式 (不弹窗, 自己处理)
 * @param {boolean} options.showMessage - 是否弹 ElMessage (默认 true)
 * @returns {{handled: boolean, type: string, message: string}}
 */
export function handleError(err, options = {}) {
  const { silent = false, showMessage = true } = options

  // 1. 提取错误信息
  let status = err?.response?.status
  let message = err?.response?.data?.message || err?.message || '未知错误'
  let type = 'unknown'
  let handled = false

  // 2. 分类
  if (status === 401) {
    // V3.7.4: 登录页 401 不跳走
    const isLoginRequest = router.currentRoute.value?.name === 'Login' || err?.config?.url?.includes('/auth/login')
    if (isLoginRequest) {
      return { type: 'login_error', handled: false, message }
    }

    type = 'unauthorized'
    // 防止风暴: 5s 内只处理一次
    const now = Date.now()
    if (now - last401At > 5000) {
      last401At = now
      const userStore = useUserStore()
      userStore.logout()
      if (!silent) {
        ElNotification({
          title: '🔒 登录已过期',
          message: '请重新登录后继续操作',
          type: 'warning',
          duration: 3000,
          position: 'top-right',
          onClick: () => router.push({ name: 'Login' })
        })
      }
      // 跳登录, 带 redirect
      const currentPath = router.currentRoute.value.fullPath
      if (currentPath !== '/login') {
        router.push({ name: 'Login', query: { redirect: currentPath } })
      }
    }
    handled = true
  } else if (status === 403) {
    type = 'forbidden'
    message = '权限不足, 请联系管理员'
    if (showMessage) ElMessage.error(message)
    handled = true
  } else if (status === 404) {
    type = 'notfound'
    message = '请求的资源不存在'
    if (showMessage) ElMessage.warning(message)
    handled = true
  } else if (status >= 500) {
    type = 'server'
    // V3.7.6+ 错误码细化
    if (status === 502) message = '网关错误 (后端不可用)'
    else if (status === 503) message = '服务暂不可用 (维护中或过载)'
    else if (status === 504) message = '网关超时 (后端响应慢)'
    else message = `服务异常 (${status}), 请稍后重试`
    if (showMessage) {
      // V3.7.7+ 错误码 toast 类型区分
      if (status === 502 || status === 504) ElMessage.warning(message)
      else if (status === 503) ElMessage({ message, type: 'info', duration: 5000 })
      else ElMessage.error(message)
    }
    handled = true
  } else if (err?.code === 'ECONNABORTED' || message.includes('timeout')) {
    type = 'timeout'
    message = '请求超时, 请检查网络后重试'
    if (showMessage) ElMessage.warning(message)
    handled = true
  } else if (err?.message === 'Network Error' || !navigator.onLine) {
    type = 'network'
    message = '网络不可用, 请检查连接'
    if (showMessage) ElMessage.error(message)
    handled = true
  } else {
    // 业务错 (BFF 返回 code != 0)
    if (showMessage && message) ElMessage.error(message)
    handled = false
  }

  return { handled, type, message, status }
}

/**
 * useErrorHandler composable
 * 返回 { handleError, isOnline }
 */
export function useErrorHandler() {
  return {
    handleError,
    isOnline
  }
}

export default useErrorHandler
