/**
 * @file auth API 调用层 (V3.5.12+)
 *
 * 对应后端模块: minimax-auth
 * 接口数: 47
 *
 *   POST   /internal/apikey/validate
 *   GET    /internal/apikey/stats
 *   POST   /api/v1/auth/register
 *   POST   /api/v1/auth/login
 *   POST   /api/v1/auth/refresh
 *   POST   /api/v1/auth/logout
 *   GET    /api/v1/auth/me
 *   GET    /api/v1/auth/notifications/unread-count
 *   ... 共 47 个
 */
import http from './http'

export const authApi = {
  /**
   * login - 创建/更新 /api/v1/auth/login
   * @returns POST /api/v1/auth/login 的响应 Promise
   */
  login: (data) => http.post('/api/v1/auth/login', data),
  /**
   * register - 创建/更新 /api/v1/auth/register
   * @returns POST /api/v1/auth/register 的响应 Promise
   */
  register: (data) => http.post('/api/v1/auth/register', data),
  /**
   * me - 查询 /api/v1/auth/me
   * @returns GET /api/v1/auth/me 的响应 Promise
   */
  me: () => http.get('/api/v1/auth/me'),
  /**
   * refresh - 创建/更新 /api/v1/auth/refresh
   * @returns POST /api/v1/auth/refresh 的响应 Promise
   */
  refresh: (refreshToken) => http.post('/api/v1/auth/refresh', { refreshToken }),
  /**
   * logout - 创建/更新 /api/v1/auth/logout
   * @returns POST /api/v1/auth/logout 的响应 Promise
   */
  logout: (refreshToken) => http.post('/api/v1/auth/logout', { refreshToken })
}
