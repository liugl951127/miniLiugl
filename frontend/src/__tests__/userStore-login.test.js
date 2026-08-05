/**
 * V3.7.22+ userStore.login 数据格式单元测试
 *
 * 验证 3 层 fallback:
 * 1. http.js 自动剥 Result.data (后端 Result<LoginResponse> 包装)
 * 2. res.data 直接是 LoginResponse
 * 3. 字段缺失/格式异常时优雅降级
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/store/user'

// mock authApi
vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    me: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
  }
}))

import { authApi } from '@/api/auth'

// mock useDemoMode
vi.mock('@/composables/useDemoMode', () => ({
  isDemoMode: () => false,
}))

describe('userStore.login 数据格式验证', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('1. 标准 Result.data 自动剥: res.data 直接是 LoginResponse', async () => {
    // V3.7.22+ http.js 自动剥 Result.data
    // authApi.login 返 { code:0, data: LoginResponse, ... } (http 已剥)
    // store 看到 res.data = LoginResponse
    authApi.login.mockResolvedValue({
      data: {
        accessToken: 'at-1',
        refreshToken: 'rt-1',
        expiresIn: 1800,
        tokenType: 'Bearer',
        user: { id: 1, username: 'admin', roles: ['ADMIN'] }
      }
    })

    const store = useUserStore()
    await store.login({ username: 'admin', password: 'admin' })

    expect(store.accessToken).toBe('at-1')
    expect(store.refreshToken).toBe('rt-1')
    expect(store.profile).toEqual({ id: 1, username: 'admin', roles: ['ADMIN'] })
    expect(store.isLogin).toBe(true)
    expect(store.isAdmin).toBe(true)
  })

  it('2. 字段缺失: accessToken 缺失用空串', async () => {
    authApi.login.mockResolvedValue({
      data: {
        user: { id: 1, username: 'test' }
        // 缺 accessToken, refreshToken
      }
    })

    const store = useUserStore()
    await store.login({ username: 'test', password: 'test' })

    expect(store.accessToken).toBe('')
    expect(store.refreshToken).toBe('')
    expect(store.profile).toEqual({ id: 1, username: 'test' })
  })

  it('3. 整个 data 是 null: 全部 fallback 空值', async () => {
    authApi.login.mockResolvedValue({ data: null })

    const store = useUserStore()
    await store.login({ username: 'test', password: 'test' })

    expect(store.accessToken).toBe('')
    expect(store.refreshToken).toBe('')
    expect(store.profile).toBe(null)
    expect(store.isLogin).toBe(false)
  })

  it('4. fetchProfile 同样剥 Result.data', async () => {
    authApi.me.mockResolvedValue({
      data: { id: 1, username: 'admin', roles: ['ADMIN', 'USER'] }
    })

    const store = useUserStore()
    await store.fetchProfile()

    expect(store.profile).toEqual({ id: 1, username: 'admin', roles: ['ADMIN', 'USER'] })
  })

  it('5. refreshAccessToken 剥 Result.data', async () => {
    authApi.refresh.mockResolvedValue({
      data: { accessToken: 'new-at', refreshToken: 'new-rt' }
    })

    const store = useUserStore()
    store.refreshToken = 'old-rt'
    const at = await store.refreshAccessToken()

    expect(at).toBe('new-at')
    expect(store.accessToken).toBe('new-at')
    expect(store.refreshToken).toBe('new-rt')
  })

  it('6. 防御性: user 字段缺失时 profile 是 null 不崩', async () => {
    authApi.login.mockResolvedValue({
      data: { accessToken: 'at', refreshToken: 'rt' }  // 缺 user
    })

    const store = useUserStore()
    await store.login({ username: 'test', password: 'test' })

    expect(store.profile).toBe(null)
    expect(store.isAdmin).toBe(false)
  })

  it('7. 防御性: superAdmin 字段异常不崩', async () => {
    authApi.login.mockResolvedValue({
      data: {
        accessToken: 'at',
        refreshToken: 'rt',
        user: { id: 1, username: 'admin', superAdmin: 'true' }  // 字符串而不是 boolean
      }
    })

    const store = useUserStore()
    await store.login({ username: 'admin', password: 'admin' })

    // superAdmin === true 严格比较, 字符串 'true' 不等于 boolean true
    expect(store.isSuperAdmin).toBe(false)
  })
})
