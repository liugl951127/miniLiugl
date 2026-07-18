/**
 * @file wechat API 调用层 (V3.5.12+)
 *
 */
import http from './http'

/** 微信扫码登录 API */
export const wechatApi = {
  // 生成二维码
  /**
   * createQrCode - 查询 /api/v1/auth/wechat/qrcode
   * @returns GET /api/v1/auth/wechat/qrcode 的响应 Promise
   */
  createQrCode: () => http.get('/api/v1/auth/wechat/qrcode'),
  // 轮询扫码状态
  /**
   * getStatus - 查询 /api/v1/auth/wechat/status
   * @returns GET /api/v1/auth/wechat/status 的响应 Promise
   */
  getStatus: (ticket) => http.get('/api/v1/auth/wechat/status', { params: { ticket } }),
  // mock 模式点 "模拟扫码" (仅 mock 模式可见)
  /**
   * mockScan - 查询 /api/v1/auth/wechat/mock-scan
   * @returns GET /api/v1/auth/wechat/mock-scan 的响应 Promise
   */
  mockScan: (ticket) => http.get('/api/v1/auth/wechat/mock-scan', { params: { ticket } }),
  // 移动端 (公众号/小程序) 静默登录
  /**
   * mobileLogin - 创建/更新 mini
   * @returns POST mini 的响应 Promise
   */
  mobileLogin: (code, appType = 'mini') => http.post('/api/v1/auth/wechat/mobile-login', { code, appType }),
  // 微信回调地址 (前端展示用)
  callbackUrl: () => `${location.origin}/api/v1/auth/wechat/callback`,
}

/** 微信绑定管理 (V5.24) — 跨应用 unionid 打通 */
export const getMyBinding = () =>
  http.get('/auth/wechat/binding/me')

export const unbindMyself = () =>
  http.delete('/auth/wechat/binding/me')

export const listAllBindings = (limit = 100, keyword) =>
  http.get(`/auth/admin/wechat/bindings?limit=${limit}${keyword ? `&keyword=${encodeURIComponent(keyword)}` : ''}`)

export const findByOpenid = (openid) =>
  http.get(`/auth/admin/wechat/find?openid=${encodeURIComponent(openid)}`)

export const bindByAdmin = (body) =>
  http.post('/auth/admin/wechat/bind', body)

export const unbindByAdmin = (userId) =>
  http.delete(`/auth/admin/wechat/bind/${userId}`)
