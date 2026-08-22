/**
 * @file usePwa.js - PWA 组合式函数 (SW 注册/版本检查)
 * @version V3.5.12+ (前端注释补全)
 */
// V2.8.9 PWA composable
// 提供: 注册 SW / 安装提示 / 离线检测 / 更新提示 / 缓存控制

import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * PWA 能力封装
 *
 * 用法:
 *   const { isInstallable, isOffline, install, update, clearCache, swVersion } = usePwa()
 */
export function usePwa() {
  const isInstallable = ref(false)
  const isOffline = ref(!navigator.onLine)
  const needRefresh = ref(false)
  const swVersion = ref('unknown')
  const swRegistered = ref(false)
  const cacheInfo = ref({ static: 0, api: 0, runtime: 0 })

  let deferredPrompt = null
  let registration = null
  let cachePoller = null
  let updateCheckInterval = null
  let beforeInstallHandler = null
  let appInstalledHandler = null

  const registerSw = async () => {
    if (!('serviceWorker' in navigator)) {
      console.warn('[PWA] Service Worker 不支持')
      return
    }
    if (!import.meta.env.PROD) {
      console.log('[PWA] 开发模式跳过 SW 注册')
      return
    }
    try {
      // V3.7.19+ 动态版本号 (vite plugin 编译时注入, dev 模式用 Date.now)
      // 之前硬编码 v3.5.92, 浏览器缓存了老 SW, 新 SW install 后没法触发 needRefresh
      // 改用动态 __SW_BUILD_TIME__ (vite plugin 替换) 或 build time (dev fallback)
      const buildVersion = (typeof __SW_BUILD_TIME__ !== 'undefined') 
        ? __SW_BUILD_TIME__ 
        : new Date().toISOString()
      registration = await navigator.serviceWorker.register(`/sw.js?v=${encodeURIComponent(buildVersion)}`, {
        scope: '/'
      })
      swRegistered.value = true
      console.log('[PWA] SW registered, scope:', registration.scope)

      // 检测更新
      registration.addEventListener('updatefound', () => {
        const newSw = registration.installing
        if (!newSw) return
        newSw.addEventListener('statechange', () => {
          if (newSw.state === 'installed' && navigator.serviceWorker.controller) {
            needRefresh.value = true
            // V3.7.12+ 通知带"立即更新"按钮 (ElMessageBox)
            ElMessageBox.confirm(
              '新版本已就绪, 立即更新加载?',
              '🔄 新版本可用',
              { confirmButtonText: '立即更新', cancelButtonText: '稍后', type: 'info' }
            ).then(() => update()).catch(() => { ElMessage.info('稍后更新, 刷新页面即可') })
          }
        })
      })

      // 拿版本号
      navigator.serviceWorker.controller?.postMessage({ type: 'GET_VERSION' })
      navigator.serviceWorker.addEventListener('message', (e) => {
        if (e.data?.type === 'SW_VERSION') {
          swVersion.value = e.data.buildTime || e.data.version
        }
      })
    } catch (e) {
      console.error('[PWA] SW 注册失败:', e)
    }
  }

  const install = async () => {
    if (!deferredPrompt) {
      ElMessage.warning('当前浏览器不支持 PWA 安装, 或已被安装')
      return
    }
    deferredPrompt.prompt()
    const { outcome } = await deferredPrompt.userChoice
    if (outcome === 'accepted') {
      ElMessage.success('✅ Liugl-AI 已添加到主屏幕')
    }
    deferredPrompt = null
    isInstallable.value = false
  }

  const update = async () => {
    if (!registration?.waiting) return
    registration.waiting.postMessage({ type: 'SKIP_WAITING' })
    // 等待 controller 变更
    await new Promise((resolve) => {
      navigator.serviceWorker.addEventListener('controllerchange', resolve, { once: true })
    })
    location.reload()
  }

  // V3.7.18+ 主动检查更新 (用户点"检查更新"或 5min 周期)
  const checkForUpdate = async () => {
    if (!('serviceWorker' in navigator)) {
      ElMessage.warning('当前浏览器不支持 Service Worker')
      return false
    }
    if (!registration) {
      // 重新注册
      await registerSw()
      ElMessage.info('Service Worker 已重新注册')
      return true
    }
    try {
      await registration.update()
      // 检查有没有 waiting worker
      if (registration.waiting) {
        ElMessage.success('发现新版本, 点击"立即更新"加载')
        return true
      } else {
        ElMessage.success('当前已是最新版本 (SW ' + swVersion.value + ')')
        return false
      }
    } catch (e) {
      ElMessage.error('检查更新失败: ' + e.message)
      return false
    }
  }

  const clearCache = async () => {
    if (!navigator.serviceWorker.controller) {
      ElMessage.warning('Service Worker 未激活')
      return
    }
    navigator.serviceWorker.controller.postMessage({ type: 'CLEAR_CACHE' })
    ElMessage.success('缓存已清空, 3s 后刷新...')
    setTimeout(() => location.reload(), 3000)
  }

  const updateCacheInfo = async () => {
    if (!('caches' in window)) return
    try {
      const names = await caches.keys()
      const info = { static: 0, api: 0, runtime: 0 }
      for (const name of names) {
        const cache = await caches.open(name)
        const keys = await cache.keys()
        if (name.startsWith('minimax-api')) info.api = keys.length
        else if (name.startsWith('minimax-runtime')) info.runtime = keys.length
        else if (name.startsWith('minimax-')) info.static = keys.length
      }
      cacheInfo.value = info
    } catch (e) {
      console.warn('[PWA] cache info error:', e)
    }
  }

  const onOnline = () => {
    isOffline.value = false
    ElMessage.success('✅ 网络已恢复')
  }
  const onOffline = () => {
    isOffline.value = true
    ElMessage.warning('📡 网络已断开, 切换到离线模式')
  }

  onMounted(() => {
    // 安装提示事件
    beforeInstallHandler = (e) => {
      e.preventDefault()
      deferredPrompt = e
      isInstallable.value = true
    }
    window.addEventListener('beforeinstallprompt', beforeInstallHandler)

    // 监听安装成功
    appInstalledHandler = () => {
      ElMessage.success('🎉 Liugl-AI PWA 已安装')
      isInstallable.value = false
      deferredPrompt = null
    }
    window.addEventListener('appinstalled', appInstalledHandler)

    // 网络状态
    window.addEventListener('online', onOnline)
    window.addEventListener('offline', onOffline)

    // 注册 SW
    registerSw()

    // 缓存统计
    updateCacheInfo()
    cachePoller = setInterval(updateCacheInfo, 10000)
    
    // V3.7.18+ 5min 周期检查更新
    updateCheckInterval = setInterval(() => {
      if (registration && navigator.onLine) {
        registration.update().catch(() => {})
      }
    }, 5 * 60 * 1000)
  })

  onUnmounted(() => {
    if (beforeInstallHandler) {
      window.removeEventListener('beforeinstallprompt', beforeInstallHandler)
      beforeInstallHandler = null
    }
    if (appInstalledHandler) {
      window.removeEventListener('appinstalled', appInstalledHandler)
      appInstalledHandler = null
    }
    window.removeEventListener('online', onOnline)
    window.removeEventListener('offline', onOffline)
    if (cachePoller) { clearInterval(cachePoller); cachePoller = null }
    if (updateCheckInterval) { clearInterval(updateCheckInterval); updateCheckInterval = null }
  })

  return {
    isInstallable,
    isOffline,
    needRefresh,
    swVersion,
    swRegistered,
    cacheInfo,
    install,
    update,
    checkForUpdate,  // V3.7.18+
    clearCache,
    updateCacheInfo
  }
}
