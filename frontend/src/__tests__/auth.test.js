/**
 * auth.js API 单元测试 (Vitest)
 * 覆盖: authApi 对象 — login/register/me/refresh/logout
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock axios instance
const mockHttp = {
  get: vi.fn(),
  post: vi.fn(),
}

// Mock http.js
vi.mock('../api/http.js', () => ({
  default: mockHttp,
}))

function mockResponse(data) {
  return { data: { code: 0, ...data } }
}

describe('Auth API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('authApi.login', () => {
    it('应调用 POST /api/v1/auth/login', async () => {
      mockHttp.post.mockResolvedValueOnce(mockResponse({
        data: { accessToken: 'at_xxx', refreshToken: 'rt_yyy' }
      }))
      const { authApi } = await import('../api/auth.js')
      await authApi.login({ username: 'admin', password: 'password' })
      expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/auth/login', { username: 'admin', password: 'password' })
    })

    it('返回 accessToken 和 refreshToken', async () => {
      mockHttp.post.mockResolvedValueOnce(mockResponse({
        data: { accessToken: 'at_new', refreshToken: 'rt_new' }
      }))
      const { authApi } = await import('../api/auth.js')
      const result = await authApi.login({ username: 'u', password: 'p' })
      expect(result.data.data.accessToken).toBe('at_new')
    })
  })

  describe('authApi.register', () => {
    it('应调用 POST /api/v1/auth/register', async () => {
      mockHttp.post.mockResolvedValueOnce(mockResponse({ data: { id: 1 } }))
      const { authApi } = await import('../api/auth.js')
      await authApi.register({ username: 'newuser', email: 'new@test.com', password: 'pass123' })
      expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/auth/register', { username: 'newuser', email: 'new@test.com', password: 'pass123' })
    })
  })

  describe('authApi.me', () => {
    it('应调用 GET /api/v1/auth/me', async () => {
      mockHttp.get.mockResolvedValueOnce(mockResponse({ data: { id: 1, username: 'admin' } }))
      const { authApi } = await import('../api/auth.js')
      const result = await authApi.me()
      expect(mockHttp.get).toHaveBeenCalledWith('/api/v1/auth/me')
      expect(result.data.data.username).toBe('admin')
    })
  })

  describe('authApi.refresh', () => {
    it('应调用 POST /api/v1/auth/refresh', async () => {
      mockHttp.post.mockResolvedValueOnce(mockResponse({ data: { accessToken: 'at_refreshed' } }))
      const { authApi } = await import('../api/auth.js')
      await authApi.refresh('rt_old')
      expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/auth/refresh', { refreshToken: 'rt_old' })
    })
  })

  describe('authApi.logout', () => {
    it('应调用 POST /api/v1/auth/logout', async () => {
      mockHttp.post.mockResolvedValueOnce(mockResponse({ data: {} }))
      const { authApi } = await import('../api/auth.js')
      await authApi.logout('rt_token')
      expect(mockHttp.post).toHaveBeenCalledWith('/api/v1/auth/logout', { refreshToken: 'rt_token' })
    })
  })

  describe('错误处理', () => {
    it('login 在网络错误时应抛出', async () => {
      mockHttp.post.mockRejectedValueOnce(new Error('Network Error'))
      const { authApi } = await import('../api/auth.js')
      await expect(authApi.login({ username: 'u', password: 'p' })).rejects.toThrow('Network Error')
    })

    it('me 在 401 时应抛出', async () => {
      mockHttp.get.mockRejectedValueOnce({ response: { status: 401, data: { msg: '未登录' } } })
      const { authApi } = await import('../api/auth.js')
      await expect(authApi.me()).rejects.toBeDefined()
    })
  })
})
