/**
 * @file useCapacitor.js - useCapacitor 组合式 API
 * @version V3.5.12+ (前端注释补全)
 */
/**
 * Capacitor 移动端能力集成 (V3.1.1)
 *
 * <p>提供:
 *   - 平台检测 (web / ios / android)
 *   - 偏好存储 (跨平台, 替代 localStorage)
 *   - 网络状态监听
 *   - 触觉反馈
 *   - 启动屏控制
 *   - 键盘事件
 *
 * <h3>降级策略</h3>
 * - Web 平台: 用 localStorage / navigator.onLine / Vibration API
 * - 原生平台: 用 @capacitor/* 插件
 *
 * 业务代码统一调 useCapacitor(), 无需关心平台.
 */
import { ref, computed } from 'vue'

// 平台枚举
export const PLATFORM = {
  WEB: 'web',
  IOS: 'ios',
  ANDROID: 'android',
}

// Capacitor 是否可用 (原生平台)
const isNative = typeof window !== 'undefined' &&
                  window.Capacitor !== undefined &&
                  window.Capacitor.isNativePlatform?.() === true

// 当前平台
const platform = ref(
  isNative
    ? (window.Capacitor.getPlatform?.() || PLATFORM.WEB)
    : PLATFORM.WEB
)

// 网络状态
const isOnline = ref(typeof navigator !== 'undefined' ? navigator.onLine : true)

// 偏好存储 (跨平台)
class PreferencesService {
  constructor() {
    this.isNative = isNative
  }

  async get(key) {
    try {
      if (this.isNative) {
        const { Preferences } = await import('@capacitor/preferences')
        const { value } = await Preferences.get({ key })
        return value
      } else {
        return localStorage.getItem(key)
      }
    } catch (e) {
      console.warn('[prefs] get failed:', e)
      return null
    }
  }

  async set(key, value) {
    try {
      if (this.isNative) {
        const { Preferences } = await import('@capacitor/preferences')
        await Preferences.set({ key, value: String(value) })
      } else {
        localStorage.setItem(key, String(value))
      }
      return true
    } catch (e) {
      console.warn('[prefs] set failed:', e)
      return false
    }
  }

  async remove(key) {
    try {
      if (this.isNative) {
        const { Preferences } = await import('@capacitor/preferences')
        await Preferences.remove({ key })
      } else {
        localStorage.removeItem(key)
      }
    } catch (e) {
      console.warn('[prefs] remove failed:', e)
    }
  }

  async clear() {
    try {
      if (this.isNative) {
        const { Preferences } = await import('@capacitor/preferences')
        await Preferences.clear()
      } else {
        localStorage.clear()
      }
    } catch (e) {
      console.warn('[prefs] clear failed:', e)
    }
  }
}

// 触觉反馈
class HapticsService {
  async impact(style = 'MEDIUM') {
    try {
      if (isNative) {
        const { Haptics, ImpactStyle } = await import('@capacitor/haptics')
        await Haptics.impact({ style: ImpactStyle[style] || ImpactStyle.Medium })
      } else if (navigator.vibrate) {
        navigator.vibrate(20)
      }
    } catch (e) {
      console.warn('[haptics] impact failed:', e)
    }
  }

  async notify(type = 'SUCCESS') {
    try {
      if (isNative) {
        const { Haptics, NotificationType } = await import('@capacitor/haptics')
        await Haptics.notification({ type: NotificationType[type] || NotificationType.Success })
      } else if (navigator.vibrate) {
        navigator.vibrate([20, 50, 20])
      }
    } catch (e) {
      console.warn('[haptics] notify failed:', e)
    }
  }
}

// 启动屏
class SplashService {
  async show() {
    try {
      if (isNative) {
        const { SplashScreen } = await import('@capacitor/splash-screen')
        await SplashScreen.show()
      }
    } catch (e) {
      console.warn('[splash] show failed:', e)
    }
  }

  async hide() {
    try {
      if (isNative) {
        const { SplashScreen } = await import('@capacitor/splash-screen')
        await SplashScreen.hide()
      }
    } catch (e) {
      console.warn('[splash] hide failed:', e)
    }
  }
}

// 状态栏
class StatusBarService {
  async setStyle(style = 'LIGHT') {
    try {
      if (isNative) {
        const { StatusBar, Style } = await import('@capacitor/status-bar')
        await StatusBar.setStyle({ style: Style[style] || Style.Light })
      }
    } catch (e) {
      console.warn('[status-bar] setStyle failed:', e)
    }
  }

  async setBackgroundColor(color = '#409EFF') {
    try {
      if (isNative) {
        const { StatusBar } = await import('@capacitor/status-bar')
        await StatusBar.setBackgroundColor({ color })
      }
    } catch (e) {
      console.warn('[status-bar] setBackgroundColor failed:', e)
    }
  }
}

// 网络监听
class NetworkService {
  constructor() {
    this.isOnline = isOnline
    if (typeof window !== 'undefined') {
      window.addEventListener('online', () => { isOnline.value = true })
      window.addEventListener('offline', () => { isOnline.value = false })
    }
  }

  async getStatus() {
    try {
      if (isNative) {
        const { Network } = await import('@capacitor/network')
        const status = await Network.getStatus()
        return status.connected
      }
      return navigator.onLine
    } catch (e) {
      return navigator.onLine
    }
  }
}

// 键盘
class KeyboardService {
  async hide() {
    try {
      if (isNative) {
        const { Keyboard } = await import('@capacitor/keyboard')
        await Keyboard.hide()
      }
    } catch (e) {
      console.warn('[keyboard] hide failed:', e)
    }
  }
}

// 单例
const preferences = new PreferencesService()
const haptics = new HapticsService()
const splash = new SplashService()
const statusBar = new StatusBarService()
const network = new NetworkService()
const keyboard = new KeyboardService()

/**
 * 主 composable
 */
export function useCapacitor() {
  return {
    // 平台信息
    platform: computed(() => platform.value),
    isNative: computed(() => isNative),
    isWeb: computed(() => platform.value === PLATFORM.WEB),
    isIOS: computed(() => platform.value === PLATFORM.IOS),
    isAndroid: computed(() => platform.value === PLATFORM.ANDROID),
    // 网络
    isOnline: computed(() => isOnline.value),
    // 服务
    preferences,
    haptics,
    splash,
    statusBar,
    network,
    keyboard,
  }
}

/**
 * App 初始化 (在 main.js 调一次)
 */
export async function initCapacitor() {
  if (!isNative) {
    console.log('[capacitor] Web 平台, 跳过原生初始化')
    return
  }
  console.log('[capacitor] 原生平台:', platform.value)
  try {
    // 1. 配置状态栏
    await statusBar.setStyle('LIGHT')
    await statusBar.setBackgroundColor('#409EFF')
    // 2. 隐藏启动屏 (延迟 1s, 让 App 加载完)
    setTimeout(() => splash.hide(), 1000)
  } catch (e) {
    console.warn('[capacitor] init failed:', e)
  }
}

export default useCapacitor
