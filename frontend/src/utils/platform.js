/**
 * 平台检测工具 (V5.2).
 */

export function isWechatBrowser() {
  const ua = (navigator.userAgent || '').toLowerCase()
  return ua.indexOf('micromessenger') !== -1
}

export function isQQBrowser() {
  const ua = (navigator.userAgent || '').toLowerCase()
  return ua.indexOf('qq/') !== -1 || ua.indexOf('mqqbrowser') !== -1
}

export function isAlipayBrowser() {
  const ua = (navigator.userAgent || '').toLowerCase()
  return ua.indexOf('alipayclient') !== -1
}

export function isMobile() {
  const ua = navigator.userAgent || ''
  return /Mobile|iPhone|iPad|Android/i.test(ua)
}

export function isMiniProgram() {
  // 小程序标识
  return typeof wx !== 'undefined' && wx.miniprogram
}

/**
 * 移动端 H5 登录页 URL (供其他页面跳转)
 */
export function h5LoginUrl(redirect = '/') {
  const base = window.location.origin
  return `${base}/h5-login?redirect=${encodeURIComponent(redirect)}`
}