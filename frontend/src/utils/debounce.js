/**
 * @file utils/debounce.js (P0 竞态修复)
 * @description 通用防抖 / 节流 / AbortController 工具
 */

/**
 * 通用防抖: 多次连续调用只执行最后一次
 * @param {Function} fn 要执行的函数
 * @param {number} delay 延迟毫秒
 * @returns {Function} 防抖后的函数 + cancel 方法
 */
export function debounce(fn, delay = 300) {
  let timer = null
  const wrapped = function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      timer = null
      fn.apply(this, args)
    }, delay)
  }
  wrapped.cancel = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }
  wrapped.flush = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
      fn()
    }
  }
  return wrapped
}

/**
 * 通用节流: 在 delay 时间内最多执行一次
 * @param {Function} fn
 * @param {number} delay
 */
export function throttle(fn, delay = 300) {
  let last = 0
  let timer = null
  return function (...args) {
    const now = Date.now()
    const remaining = delay - (now - last)
    if (remaining <= 0) {
      if (timer) { clearTimeout(timer); timer = null }
      last = now
      fn.apply(this, args)
    } else if (!timer) {
      timer = setTimeout(() => {
        last = Date.now()
        timer = null
        fn.apply(this, args)
      }, remaining)
    }
  }
}

/**
 * 可取消的 fetch 包装器
 * - 内部维护最新请求的 AbortController
 * - 新请求会取消上一次未完成的请求, 避免乱序
 *
 * 用法:
 *   const fetcher = createCancellableFetcher()
 *   await fetcher.fetch(() => api.list(params))
 */
export function createCancellableFetcher() {
  let currentController = null

  async function fetch(fn) {
    // 取消上一次未完成请求
    if (currentController) {
      try { currentController.abort() } catch (e) { /* ignore */ }
    }
    currentController = new AbortController()
    const signal = currentController.signal
    try {
      return await fn(signal)
    } finally {
      if (currentController && currentController.signal === signal) {
        currentController = null
      }
    }
  }

  function cancel() {
    if (currentController) {
      try { currentController.abort() } catch (e) { /* ignore */ }
      currentController = null
    }
  }

  return { fetch, cancel }
}

export default { debounce, throttle, createCancellableFetcher }
