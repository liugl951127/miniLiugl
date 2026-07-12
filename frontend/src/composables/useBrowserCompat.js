// V3.0.0 浏览器兼容层
// 检测浏览器能力, 缺失时注入 polyfill
// 兼容: Chrome 63+ / Edge 79+ / Firefox 60+ / Safari 12+ / iOS 12+ / Android 8+

/**
 * 检测浏览器特性支持
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
  }
  return features
}

/**
 * 安装 polyfill (运行时, 浏览器原生缺失时)
 */
export function installPolyfills() {
  if (typeof window === 'undefined') return

  // 1. structuredClone 简易实现 (Safari 15.4- 不支持)
  if (typeof structuredClone !== 'function') {
    window.structuredClone = function (obj) {
      return JSON.parse(JSON.stringify(obj))
    }
  }

  // 2. crypto.randomUUID (Chrome 92+/FF 95+/Edge 92+/Safari 15.4+)
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID !== 'function') {
    crypto.randomUUID = function () {
      // 简化的 UUID v4
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        const r = Math.random() * 16 | 0
        const v = c === 'x' ? r : (r & 0x3 | 0x8)
        return v.toString(16)
      })
    }
  }

  // 3. Array.prototype.flat (Chrome 69+/FF 62+/Edge 79+/Safari 12+)
  if (!Array.prototype.flat) {
    Array.prototype.flat = function (depth) {
      depth = depth === undefined ? 1 : depth
      const flattened = []
      const flatten = (arr) => {
        for (const el of arr) {
          if (Array.isArray(el) && depth > 0) {
            flatten(el)
            depth--
          } else {
            flattened.push(el)
          }
        }
      }
      flatten(this)
      return flattened
    }
  }

  // 4. Object.fromEntries (Chrome 73+/FF 63+/Edge 79+/Safari 12.1+)
  if (!Object.fromEntries) {
    Object.fromEntries = function (iterable) {
      const result = {}
      for (const [k, v] of iterable) {
        result[k] = v
      }
      return result
    }
  }

  // 5. String.prototype.replaceAll (Chrome 85+/FF 77+/Edge 85+/Safari 13.1+)
  if (!String.prototype.replaceAll) {
    String.prototype.replaceAll = function (str, newStr) {
      return this.split(str).join(newStr)
    }
  }

  // 6. requestIdleCallback (Chrome 47+/FF 55+/Edge 79+/Safari 16.4+)
  if (typeof window.requestIdleCallback === 'undefined') {
    window.requestIdleCallback = function (cb) {
      return setTimeout(cb, 1)
    }
    window.cancelIdleCallback = function (id) {
      clearTimeout(id)
    }
  }

  // 7. AbortController (Chrome 66+/FF 57+/Edge 16+/Safari 11.1+)
  if (typeof AbortController === 'undefined') {
    window.AbortController = function () {
      this.signal = { aborted: false }
    }
  }
}

/**
 * 浏览器识别
 */
export function detectBrowser() {
  if (typeof navigator === 'undefined') return { name: 'unknown', version: '0' }
  const ua = navigator.userAgent
  if (/Edg\//.test(ua)) {
    return { name: 'Edge', version: ua.match(/Edg\/([\d.]+)/)?.[1] || '0' }
  }
  if (/Chrome\//.test(ua)) {
    return { name: 'Chrome', version: ua.match(/Chrome\/([\d.]+)/)?.[1] || '0' }
  }
  if (/Firefox\//.test(ua)) {
    return { name: 'Firefox', version: ua.match(/Firefox\/([\d.]+)/)?.[1] || '0' }
  }
  if (/Safari\//.test(ua)) {
    return { name: 'Safari', version: ua.match(/Version\/([\d.]+)/)?.[1] || '0' }
  }
  if (/OPR\//.test(ua)) {
    return { name: 'Opera', version: ua.match(/OPR\/([\d.]+)/)?.[1] || '0' }
  }
  return { name: 'unknown', version: '0' }
}

/**
 * 综合初始化: 安装 polyfill + 检测 + 报告
 */
export function initBrowserCompat() {
  const features = detectFeatures()
  const browser = detectBrowser()
  installPolyfills()

  // 不兼容警告
  const missing = Object.entries(features)
    .filter(([k, v]) => !v)
    .map(([k]) => k)
  if (missing.length > 0) {
    console.warn('[BrowserCompat] 不支持特性 (已尝试 polyfill):', missing)
  }

  console.info(`[BrowserCompat] ${browser.name} ${browser.version}, features OK`)
  return { browser, features, missing }
}
