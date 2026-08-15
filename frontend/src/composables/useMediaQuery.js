/**
 * V6.7+ 响应式断点检测
 * - 移动端 < 768px
 * - 平板 768-1024px
 * - 桌面 > 1024px
 */
import { ref, onMounted, onUnmounted } from 'vue'

export const BREAKPOINTS = {
  xs: 480,
  sm: 640,
  md: 768,
  lg: 1024,
  xl: 1280,
  xxl: 1536
}

export function useMediaQuery() {
  const width = ref(typeof window !== 'undefined' ? window.innerWidth : 1280)
  const height = ref(typeof window !== 'undefined' ? window.innerHeight : 720)
  
  const device = ref('desktop') // mobile | tablet | desktop
  const isMobile = ref(false)
  const isTablet = ref(false)
  const isDesktop = ref(true)
  const orientation = ref('landscape') // portrait | landscape
  const isTouch = ref(false)
  
  function update() {
    if (typeof window === 'undefined') return
    width.value = window.innerWidth
    height.value = window.innerHeight
    
    if (width.value < BREAKPOINTS.md) {
      device.value = 'mobile'
      isMobile.value = true
      isTablet.value = false
      isDesktop.value = false
    } else if (width.value < BREAKPOINTS.lg) {
      device.value = 'tablet'
      isMobile.value = false
      isTablet.value = true
      isDesktop.value = false
    } else {
      device.value = 'desktop'
      isMobile.value = false
      isTablet.value = false
      isDesktop.value = true
    }
    
    orientation.value = width.value > height.value ? 'landscape' : 'portrait'
  }
  
  function onTouch() {
    isTouch.value = 'ontouchstart' in window || navigator.maxTouchPoints > 0
  }
  
  onMounted(() => {
    update()
    onTouch()
    window.addEventListener('resize', update, { passive: true })
    window.addEventListener('orientationchange', update)
  })
  
  onUnmounted(() => {
    window.removeEventListener('resize', update)
    window.removeEventListener('orientationchange', update)
  })
  
  return {
    width,
    height,
    device,
    isMobile,
    isTablet,
    isDesktop,
    orientation,
    isTouch,
    update
  }
}

export function useBreakpoint(name) {
  const { width } = useMediaQuery()
  return computed(() => {
    const bp = BREAKPOINTS[name]
    return width.value >= bp
  })
}

// 用 Vue computed (需 import)
import { computed } from 'vue'
