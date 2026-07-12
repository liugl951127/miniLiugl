/**
 * 移动端安全区域管理 (V3.1.1)
 *
 * <p>处理:
 *   - iOS 顶部刘海 / 底部 Home Indicator
 *   - Android 状态栏 / 导航栏
 *   - 横屏 / 竖屏切换
 *
 * <p>原理: 用 CSS env(safe-area-inset-*) + 动态设置 padding
 */
import { ref, onMounted, onUnmounted } from 'vue'

export function useSafeArea() {
  // 当前 inset 值
  const insets = ref({
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
  })

  /**
   * 读取 CSS env() 值
   */
  function readInsets() {
    if (typeof window === 'undefined') return
    const style = getComputedStyle(document.documentElement)
    // 解析 env(safe-area-inset-*)
    function read(name) {
      const v = style.getPropertyValue(name).trim()
      if (v.endsWith('px')) return parseFloat(v) || 0
      return 0
    }
    insets.value = {
      top: read('--sat') || 0,
      right: read('--sar') || 0,
      bottom: read('--sab') || 0,
      left: read('--sal') || 0,
    }
  }

  /**
   * 设置 CSS 变量 (给全局样式用)
   */
  function setCssVars() {
    if (typeof document === 'undefined') return
    const root = document.documentElement
    root.style.setProperty('--safe-area-top', `${insets.value.top}px`)
    root.style.setProperty('--safe-area-bottom', `${insets.value.bottom}px`)
    root.style.setProperty('--safe-area-left', `${insets.value.left}px`)
    root.style.setProperty('--safe-area-right', `${insets.value.right}px`)
  }

  // 屏幕方向
  const orientation = ref('portrait')

  function readOrientation() {
    if (typeof window === 'undefined') return
    // matchMedia 优先
    if (window.matchMedia?.('(orientation: landscape)').matches) {
      orientation.value = 'landscape'
    } else {
      orientation.value = 'portrait'
    }
  }

  // 监听
  function onResize() {
    readInsets()
    readOrientation()
    setCssVars()
  }

  onMounted(() => {
    onResize()
    window.addEventListener('resize', onResize)
    window.addEventListener('orientationchange', onResize)
  })

  onUnmounted(() => {
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', onResize)
      window.removeEventListener('orientationchange', onResize)
    }
  })

  return {
    insets,
    orientation,
    // 计算属性: 应用 padding 时减去 safe area
    paddingStyle: () => ({
      paddingTop: `env(safe-area-inset-top, ${insets.value.top}px)`,
      paddingBottom: `env(safe-area-inset-bottom, ${insets.value.bottom}px)`,
      paddingLeft: `env(safe-area-inset-left, ${insets.value.left}px)`,
      paddingRight: `env(safe-area-inset-right, ${insets.value.right}px)`,
    }),
  }
}

export default useSafeArea
