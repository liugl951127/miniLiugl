/**
 * V6.7+ 性能优化
 * - 图片懒加载
 * - 路由预加载
 * - 组件缓存
 * - 长列表虚拟滚动
 */
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

const observers = new WeakMap()

export function useLazyImage() {
  const loaded = ref(false)
  const error = ref(false)
  
  function onImageLoad() { loaded.value = true }
  function onImageError() { error.value = true }
  
  return { loaded, error, onImageLoad, onImageError }
}

export function useLazyVisible(options = {}) {
  const { threshold = 0.1, rootMargin = '50px' } = options
  const target = ref(null)
  const visible = ref(false)
  
  onMounted(() => {
    if (!target.value || !('IntersectionObserver' in window)) {
      visible.value = true
      return
    }
    
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            visible.value = true
            observer.unobserve(entry.target)
          }
        })
      },
      { threshold, rootMargin }
    )
    observer.observe(target.value)
    observers.set(target.value, observer)
  })
  
  onUnmounted(() => {
    if (target.value) {
      const o = observers.get(target.value)
      if (o) o.disconnect()
    }
  })
  
  return { target, visible }
}

export function useDebounce(fn, delay = 300) {
  let timer
  function debounced(...args) {
    clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), delay)
  }
  debounced.cancel = () => clearTimeout(timer)
  return debounced
}

export function useThrottle(fn, delay = 200) {
  let last = 0
  let timer
  function throttled(...args) {
    const now = Date.now()
    if (now - last >= delay) {
      fn.apply(this, args)
      last = now
    } else {
      clearTimeout(timer)
      timer = setTimeout(() => {
        fn.apply(this, args)
        last = Date.now()
      }, delay - (now - last))
    }
  }
  throttled.cancel = () => clearTimeout(timer)
  return throttled
}

export function useAsyncData(loader, options = {}) {
  const { immediate = true, watch: watchSources = [] } = options
  const data = ref(null)
  const error = ref(null)
  const loading = ref(false)
  
  async function load(...args) {
    loading.value = true
    error.value = null
    try {
      data.value = await loader(...args)
    } catch (e) {
      error.value = e
    } finally {
      loading.value = false
    }
  }
  
  if (immediate) load()
  
  return { data, error, loading, reload: load }
}
