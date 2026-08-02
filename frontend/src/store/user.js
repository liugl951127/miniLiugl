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
      // V3.7.21+ 后端用 Result<LoginResponse> 包装, 前端剥 data.data
      // 之前 res.data = { code, message, data: LoginResponse, timestamp } 整个 Result
      // 解构顶层 accessToken/refreshToken/user 全是 undefined → profile.value = undefined → 渲染错
      const { accessToken: at, refreshToken: rt, user } = res.data.data || res.data || {}
      accessToken.value = at || ''
      refreshToken.value = rt || ''
      profile.value = user || null
      return res
    }

    async function fetchProfile() {
      try {
        const res = await authApi.me()
        // V3.7.21+ 剥 Result.data
        profile.value = res.data.data || res.data
        return res
      } catch (e) {
        // V3.5.93: 失败时设空 profile, 避免 layout 空白
        // - 后端 5xx/网络错: profile = null (下游 layout 显示空态)
        // - 后端 401: useErrorHandler 自动清 token + 跳登录
        if (!profile.value) {
          profile.value = { username: accessToken.value ? 'unknown' : '', roles: [] }
        }
        // V3.5.93: 调试模式 (无后端) 直接返 mock profile
        if (isDemoMode()) {
          profile.value = {
            username: localStorage.getItem('minimax_demo_user') || 'demo',
            nickname: '演示用户',
            email: 'demo@minimax.io',
            roles: ['ADMIN', 'USER'],
            avatar: '🎭'
          }
          return { data: profile.value }
        }
        throw e
      }
    }

    async function refreshAccessToken() {
      if (!refreshToken.value) throw new Error('no refresh token')
      try {
      const res = await authApi.refresh(refreshToken.value)
      // V3.7.21+ 剥 Result.data
      const data = res.data.data || res.data || {}
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
    }

    return {
      accessToken,
      refreshToken,
      profile,
      isLogin,
      isAdmin,
      isSuperAdmin,
      login,
      logout,
      fetchProfile,
      refreshAccessToken
    }
  },
  {
    persist: {
      key: 'minimax-user',
      storage: localStorage
    }
  }
)
