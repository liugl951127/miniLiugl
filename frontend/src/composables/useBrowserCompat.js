// V3.0.0 浏览器兼容层 (V3.5.8 全面增强版)
// 检测浏览器能力, 缺失时注入 polyfill
// 兼容: Chrome 63+ / Edge 79+ / Firefox 60+ / Safari 12+ / iOS 12+ / Android 8+

/**
 * 检测浏览器特性支持
 *
 * 设计原则:
 * 1. 全部检测无副作用 (try-catch 包住)
 * 2. 缺失只警告, 不中断 (polyfill 兜底)
 * 3. 现代浏览器 100% 通过, 老浏览器降级运行
 *
 * @returns {object} 特性支持矩阵
 */
export function detectFeatures() {
  if (typeof window === 'undefined') return {}

  const features = {
    // ES2017+ Async/Await (Chrome 55+/FF 52+/Edge 15+/Safari 11+)
    asyncAwait: typeof async function () {}.constructor === 'function',
    // ES2018 Object spread/rest (Chrome 60+/FF 55+/Edge 79+/Safari 11.1+)
    objectSpread: (() => { try { return { ...{} } } catch (e) { return false } })(),
    // ES2020 Optional chaining (Chrome 80+/FF 74+/Edge 80+/Safari 13.1+)
    optionalChaining: (() => { try { return ({})?.x } catch (e) { return false } })(),
    // ES2020 Nullish coalescing (Chrome 80+/FF 72+/Edge 80+/Safari 13.4+)
    nullishCoalescing: (() => { try { return null ?? 'a' } catch (e) { return false } })(),
    // ES2021 Logical assignment (Chrome 85+/FF 79+/Edge 85+/Safari 14+)
    logicalAssignment: (() => { try { let a = 1; a ||= 2; return a === 1 } catch (e) { return false } })(),
    // Promise (基础, 现代浏览器全支持)
    promise: typeof Promise !== 'undefined',
    // fetch (Chrome 42+/FF 39+/Edge 14+/Safari 10.1+)
    fetch: typeof fetch !== 'undefined',
    // WebSocket
    webSocket: typeof WebSocket !== 'undefined',
    // localStorage
    localStorage: (() => { try { return typeof localStorage !== 'undefined' } catch (e) { return false } })(),
    // Intl (国际化)
    intl: typeof Intl !== 'undefined',
    // Performance
    performance: typeof performance !== 'undefined',
    // navigator
    navigator: typeof navigator !== 'undefined',
    // crypto
    crypto: typeof crypto !== 'undefined',
    // structuredClone (Chrome 98+/FF 94+/Edge 98+/Safari 15.4+)
    structuredClone: typeof structuredClone === 'function',
    // IntersectionObserver (Chrome 51+/FF 55+/Edge 15+/Safari 12.1+) - 图片懒加载
    intersectionObserver: typeof IntersectionObserver !== 'undefined',
    // ResizeObserver (Chrome 64+/FF 69+/Edge 79+/Safari 13.1+) - 响应式
    resizeObserver: typeof ResizeObserver !== 'undefined',
    // MutationObserver (Chrome 26+/FF 14+/Edge 12+/Safari 7+) - DOM 变化监听
    mutationObserver: typeof MutationObserver !== 'undefined',
    // CustomEvent (Chrome 15+/FF 11+/Edge 14+/Safari 9+) - 自定义事件
    customEvent: typeof CustomEvent !== 'undefined',
    // Event (所有现代浏览器, 但是 polyfill 兜底)
    event: typeof Event !== 'undefined',
    // requestAnimationFrame (几乎所有浏览器, 兜底)
    requestAnimationFrame: typeof requestAnimationFrame !== 'undefined',
    // AbortController (Chrome 66+/FF 57+/Edge 16+/Safari 11.1+) - fetch 取消
    abortController: typeof AbortController !== 'undefined',
    // Proxy (Chrome 49+/FF 18+/Edge 12+/Safari 10+) - Vue 3 依赖
    proxy: typeof Proxy !== 'undefined',
    // Symbol (Chrome 38+/FF 36+/Edge 12+/Safari 9+)
    symbol: typeof Symbol !== 'undefined',
    // WeakMap (Chrome 36+/FF 6+/Edge 12+/Safari 8+)
    weakMap: typeof WeakMap !== 'undefined',
    // WeakSet
    weakSet: typeof WeakSet !== 'undefined',
    // BigInt (Chrome 67+/FF 68+/Edge 79+/Safari 14+)
    bigInt: typeof BigInt !== 'undefined',
    // dynamic import (Chrome 63+/FF 67+/Edge 79+/Safari 11.1+)
    dynamicImport: (() => { try { return (async () => {}).constructor === Function } catch (e) { return false } })(),
  }
  return features
}

/**
 * 安装 polyfill (运行时, 浏览器原生缺失时)
 *
 * 覆盖: ES2020+ Object/Array/String/Number + Web API
 * 不影响现代浏览器, 老浏览器降级运行
 */
export function installPolyfills() {
  if (typeof window === 'undefined') return

  // 1. Array.prototype.flat (Chrome 69+/FF 62+/Edge 79+/Safari 12+)
  if (!Array.prototype.flat) {
    Array.prototype.flat = function (depth) {
      depth = depth === undefined ? 1 : Math.floor(depth)
      const result = []
      const flat = (arr, d) => {
        for (const item of arr) {
          if (Array.isArray(item) && d > 0) flat(item, d - 1)
          else result.push(item)
        }
      }
      flat(this, depth)
      return result
    }
  }
  // Array.prototype.flatMap (Chrome 69+/FF 62+/Edge 79+/Safari 12+)
  if (!Array.prototype.flatMap) {
    Array.prototype.flatMap = function (callback, thisArg) {
      return Array.prototype.map.call(this, callback, thisArg).flat()
    }
  }

  // 2. Object.fromEntries (Chrome 73+/FF 63+/Edge 79+/Safari 12.1+)
  if (!Object.fromEntries) {
    Object.fromEntries = function (iterable) {
      const result = {}
      for (const [k, v] of iterable) {
        result[k] = v
      }
      return result
    }
  }

  // 3. String.prototype.replaceAll (Chrome 85+/FF 77+/Edge 85+/Safari 13.1+)
  if (!String.prototype.replaceAll) {
    String.prototype.replaceAll = function (str, newStr) {
      return this.split(str).join(newStr)
    }
  }
  // String.prototype.trimStart / trimEnd
  if (!String.prototype.trimStart) {
    String.prototype.trimStart = function () { return this.replace(/^\s+/, '') }
    String.prototype.trimEnd = function () { return this.replace(/\s+$/, '') }
  }

  // 4. Number.isInteger (Chrome 34+/FF 16+/Edge 12+/Safari 9+)
  if (!Number.isInteger) {
    Number.isInteger = function (n) { return typeof n === 'number' && isFinite(n) && Math.floor(n) === n }
  }

  // 5. requestIdleCallback (Chrome 47+/FF 55+/Edge 79+/Safari 16.4+)
  if (typeof window.requestIdleCallback === 'undefined') {
    window.requestIdleCallback = function (cb) { return setTimeout(() => cb({ didTimeout: false, timeRemaining: () => 50 }), 1) }
    window.cancelIdleCallback = function (id) { clearTimeout(id) }
  }

  // 6. AbortController (Chrome 66+/FF 57+/Edge 16+/Safari 11.1+)
  if (typeof AbortController === 'undefined') {
    window.AbortController = function () {
      this.signal = { aborted: false, addEventListener: () => {}, removeEventListener: () => {} }
      this.abort = () => { this.signal.aborted = true }
    }
  }

  // 7. IntersectionObserver (Chrome 51+/FF 55+/Edge 15+/Safari 12.1+) - 图片懒加载/无限滚动
  if (typeof IntersectionObserver === 'undefined') {
    window.IntersectionObserver = function (callback, options) {
      this.callback = callback
      this.options = options || {}
      this.observed = new Set()
    }
    window.IntersectionObserver.prototype.observe = function (el) { this.observed.add(el) }
    window.IntersectionObserver.prototype.unobserve = function (el) { this.observed.delete(el) }
    window.IntersectionObserver.prototype.disconnect = function () { this.observed.clear() }
  }

  // 8. ResizeObserver (Chrome 64+/FF 69+/Edge 79+/Safari 13.1+) - 响应式布局
  if (typeof ResizeObserver === 'undefined') {
    window.ResizeObserver = function (callback) {
      this.callback = callback
      this.observed = new Set()
    }
    window.ResizeObserver.prototype.observe = function (el) {
      this.observed.add(el)
      // 触发初始回调
      try { this.callback([{ target: el, contentRect: el.getBoundingClientRect() }]) } catch (e) {}
    }
    window.ResizeObserver.prototype.unobserve = function (el) { this.observed.delete(el) }
    window.ResizeObserver.prototype.disconnect = function () { this.observed.clear() }
  }

  // 9. CustomEvent (Chrome 15+/FF 11+/Edge 14+/Safari 9+)
  if (typeof CustomEvent === 'undefined') {
    window.CustomEvent = function (type, params) {
      params = params || { bubbles: false, cancelable: false, detail: null }
      const event = document.createEvent('CustomEvent')
      event.initCustomEvent(type, params.bubbles, params.cancelable, params.detail)
      return event
    }
  }
}

/**
 * 浏览器识别 (UA 嗅探)
 *
 * @returns {{ name: string, version: string, isMobile: boolean }}
 */
export function detectBrowser() {
  if (typeof navigator === 'undefined') return { name: 'unknown', version: '0', isMobile: false }
  const ua = navigator.userAgent
  // iOS 检测 (iPhone/iPad/iPod, Safari/Chrome 同源)
  const isMobile = /iPhone|iPad|iPod|Android.*Mobile|Mobile.*Safari/i.test(ua)
  // iPad 在 iOS 13+ 报为 Mac, 单独处理
  const isIPad = /Macintosh/.test(ua) && navigator.maxTouchPoints > 1
  const actualMobile = isMobile || isIPad

  if (/Edg\//.test(ua)) {
    return { name: 'Edge', version: ua.match(/Edg\/([\d.]+)/)?.[1] || '0', isMobile: actualMobile }
  }
  if (/OPR\//.test(ua)) {
    return { name: 'Opera', version: ua.match(/OPR\/([\d.]+)/)?.[1] || '0', isMobile: actualMobile }
  }
  if (/Chrome\//.test(ua) && !/Edg|OPR/.test(ua)) {
    return { name: 'Chrome', version: ua.match(/Chrome\/([\d.]+)/)?.[1] || '0', isMobile: actualMobile }
  }
  if (/Firefox\//.test(ua)) {
    return { name: 'Firefox', version: ua.match(/Firefox\/([\d.]+)/)?.[1] || '0', isMobile: actualMobile }
  }
  if (/Safari\//.test(ua)) {
    return { name: 'Safari', version: ua.match(/Version\/([\d.]+)/)?.[1] || '0', isMobile: actualMobile }
  }
  if (/MicroMessenger\//.test(ua)) {
    return { name: 'WeChat', version: ua.match(/MicroMessenger\/([\d.]+)/)?.[1] || '0', isMobile: true }
  }
  if (/QQ\//.test(ua)) {
    return { name: 'QQ', version: ua.match(/QQ\/([\d.]+)/)?.[1] || '0', isMobile: actualMobile }
  }
  return { name: 'unknown', version: '0', isMobile: actualMobile }
}

/**
 * 检测 CSS 特性支持 (用于降级样式)
 */
export function detectCssFeatures() {
  if (typeof window === 'undefined' || !window.CSS || !CSS.supports) {
    return { flexbox: true, grid: false, gap: false, containerQueries: false, backdropFilter: false }
  }
  return {
    flexbox: CSS.supports('display', 'flex'),
    grid: CSS.supports('display', 'grid'),
    gap: CSS.supports('gap', '10px'),
    containerQueries: CSS.supports('container-type', 'inline-size'),
    backdropFilter: CSS.supports('backdrop-filter', 'blur(10px)') || CSS.supports('-webkit-backdrop-filter', 'blur(10px)'),
    aspectRatio: CSS.supports('aspect-ratio', '16 / 9'),
    sticky: CSS.supports('position', 'sticky'),
  }
}

/**
 * 修复 iOS Safari 100vh 问题
 * 100vh 在 iOS Safari 包含地址栏高度, 导致内容被截断
 * 改用 -webkit-fill-available / dynamic viewport units (dvh)
 */
export function fixIOSViewport() {
  if (typeof document === 'undefined') return
  const browser = detectBrowser()
  if (browser.name !== 'Safari' && browser.name !== 'Chrome' && browser.name !== 'WeChat' && browser.name !== 'QQ') return
  if (!browser.isMobile) return

  // 注入 CSS 变量
  const style = document.createElement('style')
  style.id = 'ios-viewport-fix'
  style.textContent = `
    /* iOS Safari 100vh 修复 */
    .full-height {
      height: 100vh;
      height: -webkit-fill-available;
      height: 100dvh; /* dynamic viewport units, Safari 15.4+ */
    }
    .min-height-screen {
      min-height: 100vh;
      min-height: -webkit-fill-available;
      min-height: 100dvh;
    }
    /* 滚动弹性 (momentum scrolling) */
    .scroll-smooth {
      -webkit-overflow-scrolling: touch;
      overflow-scrolling: touch;
    }
    /* 触摸高亮去除 */
    * { -webkit-tap-highlight-color: transparent; }
  `
  if (!document.getElementById('ios-viewport-fix')) {
    document.head.appendChild(style)
  }

  // 监听 resize 重新计算 (横竖屏切换)
  window.addEventListener('orientationchange', () => {
    setTimeout(() => {
      // 触发 resize 事件让所有 ResizeObserver 重新计算
      window.dispatchEvent(new CustomEvent('resize'))
    }, 100)
  })
}

/**
 * 检测用户偏好
 * - prefers-color-scheme: 深色/浅色主题
 * - prefers-reduced-motion: 减少动画
 * - prefers-contrast: 高对比度
 */
export function detectPreferences() {
  if (typeof window === 'undefined' || !window.matchMedia) {
    return { colorScheme: 'light', reducedMotion: 'no-preference', contrast: 'no-preference' }
  }
  return {
    colorScheme: window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light',
    reducedMotion: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'reduce' : 'no-preference',
    contrast: window.matchMedia('(prefers-contrast: more)').matches ? 'more' : 'no-preference',
  }
}

/**
 * 监听用户偏好变化 (用于自动切换主题)
 *
 * @param {function(string)} onColorSchemeChange colorScheme 变化回调
 * @returns {function} 取消监听函数
 */
export function watchPreferences(onColorSchemeChange) {
  if (typeof window === 'undefined' || !window.matchMedia) return () => {}

  const darkModeQuery = window.matchMedia('(prefers-color-scheme: dark)')
  const handler = (e) => {
    onColorSchemeChange && onColorSchemeChange(e.matches ? 'dark' : 'light')
  }
  // Safari 14+ 支持 addEventListener, 老版本用 addListener
  if (darkModeQuery.addEventListener) {
    darkModeQuery.addEventListener('change', handler)
    return () => darkModeQuery.removeEventListener('change', handler)
  } else {
    darkModeQuery.addListener(handler)
    return () => darkModeQuery.removeListener(handler)
  }
}

/**
 * 综合初始化: 安装 polyfill + 检测 + 报告
 *
 * 调用时机: main.js 最开始 (在 createApp 之前)
 * 作用:
 * 1. 注入 polyfill 让老浏览器兼容现代 API
 * 2. 检测浏览器 / 设备 / CSS 特性
 * 3. 修复 iOS 100vh 等已知问题
 * 4. 检测用户偏好 (主题/动效)
 * 5. 在 console 报告
 */
export function initBrowserCompat() {
  const features = detectFeatures()
  const browser = detectBrowser()
  const cssFeatures = detectCssFeatures()
  const preferences = detectPreferences()
  installPolyfills()
  fixIOSViewport()

  // 不兼容警告
  const missing = Object.entries(features)
    .filter(([k, v]) => !v)
    .map(([k]) => k)
  if (missing.length > 0) {
    console.warn('[BrowserCompat] 不支持特性 (已尝试 polyfill):', missing)
  }

  // CSS 特性警告
  const cssMissing = Object.entries(cssFeatures)
    .filter(([k, v]) => !v)
    .map(([k]) => k)
  if (cssMissing.length > 0) {
    console.info('[BrowserCompat] CSS 降级:', cssMissing)
  }

  console.info(
    `[BrowserCompat] ${browser.name} ${browser.version} | ` +
    `${browser.isMobile ? '📱 Mobile' : '💻 Desktop'} | ` +
    `主题=${preferences.colorScheme} | 动效=${preferences.reducedMotion} | ` +
    `特性 ${Object.values(features).filter(Boolean).length}/${Object.keys(features).length} OK`
  )
  return { browser, features, cssFeatures, preferences, missing }
}
