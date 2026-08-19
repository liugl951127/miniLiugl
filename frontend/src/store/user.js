/**
 * @file user.js - Pinia 状态管理 (V3.5.12+)
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

// V3.5.93+ 演示模式 (无后端时模拟)
function isDemoMode() {
  return localStorage.getItem('minimax_demo_mode') === 'true' || 
         (typeof window !== 'undefined' && window.location.search.includes('demo=1'))
}

/**
 * 用户态 Store（Pinia + persist）。
 * 关键设计：
 *  - accessToken 短命，refreshToken 长命；两者分开存
 *  - 后端 401 触发 http.js 自动 logout + 跳登录；不在这里处理
 *  - 用户资料走 /auth/me 拉取，避免 token 中角色信息过期
 *
 * 角色:
 *  - admin       → ADMIN       (普通管理员)
 *  - adminLiugl  → SUPER_ADMIN (平台所有者, 唯一超级管理员)
 */
export const useUserStore = defineStore(
  'user',
  () => {
    const accessToken = ref('')
    const refreshToken = ref('')
    const profile = ref(null)
    // V6.8.9+: token 过期时间戳 (毫秒)，用于提前续期
    const tokenExpiry = ref(0)
    // V6.8.9+: 上次静默刷新时间戳，防止频繁刷新
    let lastSilentRefresh = 0
    // V6.8.9+: 刷新锁，防止并发重复刷新
    let isRefreshing = false

    const isLogin = computed(() => !!accessToken.value)
    const isAdmin = computed(() => {
      const roles = profile.value?.roles || []
      return roles.includes('ADMIN') || roles.includes('SUPER_ADMIN')
    })
    /** ⭐ adminLiugl 独有 */
    const isSuperAdmin = computed(() => {
      return profile.value?.superAdmin === true ||
             (profile.value?.roles || []).includes('SUPER_ADMIN')
    })

    async function login(payload) {
      const res = await authApi.login(payload)
      // V3.7.38+ http.js 自动剥, res 是业务数据; 兼容老 res.data 双层 + 各种 mock
      // 1. res = {accessToken, ...} (V3.7.22+ 剥后)
      // 2. res = {data: {accessToken, ...}} (老代码)
      // 3. res = {data: {data: {accessToken, ...}}} (双层)
      let data = res
      if (data && data.data && (data.data.accessToken || data.data.user)) {
        data = data.data
        if (data && data.data && (data.data.accessToken || data.data.user)) {
          data = data.data
        }
      }
      const { accessToken: at, refreshToken: rt, user, expiresIn } = data || {}
      accessToken.value = at || ''
      refreshToken.value = rt || ''
      profile.value = user || null
      // V6.8.9+: 记录 token 过期时间 (服务端返回 expiresIn 秒数)
      tokenExpiry.value = expiresIn ? (Date.now() + expiresIn * 1000) : 0
      return res
    }

    async function fetchProfile() {
      try {
        const res = await authApi.me()
        // V3.7.22+ http.js 自动剥, res 本身已经是 UserInfo
        profile.value = res.data || res || null
        return res
      } catch (e) {
        // V3.5.93: 失败时设空 profile, 避免 layout 空白
        // - 后端 5xx/网络错: profile = null (下游 layout 显示空态)
        // - 后端 401: useErrorHandler 自动清 token + 跳登录
        if (!profile.value) {
          profile.value = { username: accessToken.value ? 'unknown' : '', roles: [] }
        }
        // V6.8.3: 演示模式已永久禁用，直接抛出错误
        throw e
      }
    }

    async function refreshAccessToken() {
      if (!refreshToken.value) throw new Error('no refresh token')
      try {
      const res = await authApi.refresh(refreshToken.value)
      // V3.7.22+ res 本身是剥后数据
      const data = res.data || res || {}
      accessToken.value = data.accessToken || ''
      refreshToken.value = data.refreshToken || ''
      return data.accessToken
      } catch (e) {
        // V3.7.11+ refreshToken 401 → 标记 invalid, 跳 login
        if (e?.response?.status === 401) {
          const err = new Error('refresh token invalid')
          err.refreshTokenInvalid = true
          throw err
        }
        throw e
      }
    }

    async function logout() {
      try {
        if (refreshToken.value) await authApi.logout(refreshToken.value)
      } catch (_) {
        // 忽略错误，前端照样清空
      }
      accessToken.value = ''
      refreshToken.value = ''
      profile.value = null
      tokenExpiry.value = 0
    }

    /**
     * V6.8.9+: 静默刷新（如果 token 快过期或收到服务端 X-Token-Refresh 提示）
     * @param {boolean} forceServerHint - 收到 X-Token-Refresh header 时为 true
     */
    async function silentRefreshIfNeeded(forceServerHint = false) {
      if (!refreshToken.value || isRefreshing) return
      const now = Date.now()
      // 防止并发：60 秒内最多刷新一次
      if (!forceServerHint && now - lastSilentRefresh < 60_000) return
      // 条件：forceServerHint（服务端提示）或 token 剩余 < 5 分钟
      const needsRefresh = forceServerHint ||
        (tokenExpiry.value > 0 && now > tokenExpiry.value - 5 * 60 * 1000)
      if (!needsRefresh) return
      isRefreshing = true
      try {
        lastSilentRefresh = now
        const res = await authApi.refresh(refreshToken.value)
        const d = res.data || res || {}
        accessToken.value = d.accessToken || ''
        refreshToken.value = d.refreshToken || ''
        tokenExpiry.value = d.expiresIn ? (Date.now() + d.expiresIn * 1000) : 0
      } catch (_) {
        // 静默失败不影响业务，下次请求再试
      } finally {
        isRefreshing = false
      }
    }

    return {
      accessToken,
      refreshToken,
      profile,
      tokenExpiry,
      isLogin,
      isAdmin,
      isSuperAdmin,
      login,
      logout,
      fetchProfile,
      refreshAccessToken,
      silentRefreshIfNeeded
    }
  },
  {
    persist: {
      key: 'minimax-user',
      storage: localStorage
    }
  }
)
