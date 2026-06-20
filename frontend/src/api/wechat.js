import http from './http'

/** 微信扫码登录 API */
export const wechatApi = {
  // 生成二维码
  createQrCode: () => http.get('/api/v1/auth/wechat/qrcode'),
  // 轮询扫码状态
  getStatus: (ticket) => http.get('/api/v1/auth/wechat/status', { params: { ticket } }),
  // mock 模式点 "模拟扫码" (仅 mock 模式可见)
  mockScan: (ticket) => http.get('/api/v1/auth/wechat/mock-scan', { params: { ticket } }),
  // 移动端 (公众号/小程序) 静默登录
  mobileLogin: (code, appType = 'mini') => http.post('/api/v1/auth/wechat/mobile-login', { code, appType }),
  // 微信回调地址 (前端展示用)
  callbackUrl: () => `${location.origin}/api/v1/auth/wechat/callback`,
}
