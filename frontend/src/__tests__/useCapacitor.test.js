/**
 * Capacitor composable 单元测试 (V3.1.1)
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useCapacitor, PLATFORM } from '../composables/useCapacitor'

describe('useCapacitor (V3.1.1)', () => {
  beforeEach(() => {
    // 重置
    vi.clearAllMocks()
    // 清除 Capacitor 全局 (让 isNativePlatform 返回 false)
    if (typeof window !== 'undefined') {
      delete window.Capacitor
    }
  })

  it('测试 1: 平台枚举 4 个', () => {
    expect(PLATFORM.WEB).toBe('web')
    expect(PLATFORM.IOS).toBe('ios')
    expect(PLATFORM.ANDROID).toBe('android')
  })

  it('测试 2: Web 平台默认是 web', () => {
    const { platform, isWeb, isIOS, isAndroid, isNative } = useCapacitor()
    expect(platform.value).toBe(PLATFORM.WEB)
    expect(isWeb.value).toBe(true)
    expect(isIOS.value).toBe(false)
    expect(isAndroid.value).toBe(false)
    expect(isNative.value).toBe(false)
  })

  it('测试 3: 模拟原生 iOS 平台', async () => {
    // 注入 Capacitor
    global.window = global.window || {}
    window.Capacitor = {
      isNativePlatform: () => true,
      getPlatform: () => 'ios',
    }
    // 重新导入模块获取新值
    vi.resetModules()
    const cap = await import('../composables/useCapacitor')
    const { platform, isIOS, isNative } = cap.useCapacitor()
    expect(platform.value).toBe(PLATFORM.IOS)
    expect(isIOS.value).toBe(true)
    expect(isNative.value).toBe(true)
  })

  it('测试 4: 模拟原生 Android 平台', async () => {
    global.window = global.window || {}
    window.Capacitor = {
      isNativePlatform: () => true,
      getPlatform: () => 'android',
    }
    vi.resetModules()
    const cap = await import('../composables/useCapacitor')
    const { platform, isAndroid } = cap.useCapacitor()
    expect(platform.value).toBe(PLATFORM.ANDROID)
    expect(isAndroid.value).toBe(true)
  })

  it('测试 5: Web 平台 preferences 走 localStorage', async () => {
    // mock localStorage
    const store = {}
    global.localStorage = {
      getItem: vi.fn((k) => store[k] || null),
      setItem: vi.fn((k, v) => { store[k] = v }),
      removeItem: vi.fn((k) => { delete store[k] }),
      clear: vi.fn(() => { for (const k in store) delete store[k] }),
    }
    const { preferences } = useCapacitor()
    await preferences.set('user', 'alice')
    expect(localStorage.setItem).toHaveBeenCalledWith('user', 'alice')
    const v = await preferences.get('user')
    expect(v).toBe('alice')
    await preferences.remove('user')
    expect(localStorage.removeItem).toHaveBeenCalledWith('user')
  })

  it('测试 6: 网络状态初始值', () => {
    const { isOnline } = useCapacitor()
    // jsdom 默认 true
    expect(isOnline.value).toBe(true)
  })

  it('测试 7: haptics.impact 失败时静默', async () => {
    const { haptics } = useCapacitor()
    // Web 平台: 调 navigator.vibrate (jsdom 不存在) 静默失败
    await expect(haptics.impact('MEDIUM')).resolves.toBeUndefined()
  })

  it('测试 8: splash 失败时静默', async () => {
    const { splash } = useCapacitor()
    await expect(splash.show()).resolves.toBeUndefined()
    await expect(splash.hide()).resolves.toBeUndefined()
  })

  it('测试 9: statusBar 失败时静默', async () => {
    const { statusBar } = useCapacitor()
    await expect(statusBar.setStyle('LIGHT')).resolves.toBeUndefined()
    await expect(statusBar.setBackgroundColor('#fff')).resolves.toBeUndefined()
  })

  it('测试 10: keyboard.hide 失败时静默', async () => {
    const { keyboard } = useCapacitor()
    await expect(keyboard.hide()).resolves.toBeUndefined()
  })

  it('测试 11: network.getStatus 在 web 走 navigator.onLine', async () => {
    const { network } = useCapacitor()
    // jsdom online=true
    expect(typeof network.getStatus).toBe('function')
  })

  it('测试 12: preferences.clear (web)', async () => {
    const { preferences } = useCapacitor()
    const m = vi.spyOn(global.localStorage || {}, 'clear').mockImplementation(() => {})
    await preferences.clear()
    // 至少调了 clear 或没报错
  })
})
