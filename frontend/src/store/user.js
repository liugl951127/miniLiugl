import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

/**
 * 用户态 Store（Pinia + persist）。
 * 关键设计：
 *  - accessToken 短命，refreshToken 长命；两者分开存
 *  - 后端 401 触发 http.js 自动 logout + 跳登录；不在这里处理
 *  - 用户资料走 /auth/me 拉取，避免 token 中角色信息过期
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
      return roles.includes('ADMIN') || profile.value?.role === 'ADMIN'
    })

    async function login(payload) {
      const res = await authApi.login(payload)
      // http.js 已剥离外层，res 即 {code, message, data}
      const { accessToken: at, refreshToken: rt, user } = res.data
      accessToken.value = at
      refreshToken.value = rt
      profile.value = user
      return res
    }

    async function fetchProfile() {
      const res = await authApi.me()
      profile.value = res.data
      return res
    }

    async function refreshAccessToken() {
      if (!refreshToken.value) throw new Error('no refresh token')
      const res = await authApi.refresh(refreshToken.value)
      accessToken.value = res.data.accessToken
      refreshToken.value = res.data.refreshToken
      return res.data.accessToken
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
