// V2.8.9 PWA composable
// 提供: 注册 SW / 安装提示 / 离线检测 / 更新提示 / 缓存控制

import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'

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
      registration = await navigator.serviceWorker.register('/sw.js', {
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
            ElNotification({
              title: '🔄 新版本可用',
              message: '点击"立即更新"加载新版本',
              type: 'info',
              duration: 0,
              position: 'bottom-right',
              onClick: update
            })
          }
        })
      })

      // 拿版本号
      navigator.serviceWorker.controller?.postMessage({ type: 'GET_VERSION' })
      navigator.serviceWorker.addEventListener('message', (e) => {
        if (e.data?.type === 'SW_VERSION') {
          swVersion.value = e.data.version
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
    window.addEventListener('beforeinstallprompt', (e) => {
      e.preventDefault()
      deferredPrompt = e
      isInstallable.value = true
    })

    // 监听安装成功
    window.addEventListener('appinstalled', () => {
      ElMessage.success('🎉 Liugl-AI PWA 已安装')
      isInstallable.value = false
      deferredPrompt = null
    })

    // 网络状态
    window.addEventListener('online', onOnline)
    window.addEventListener('offline', onOffline)

    // 注册 SW
    registerSw()

    // 缓存统计
    updateCacheInfo()
    cachePoller = setInterval(updateCacheInfo, 10000)
  })

  onUnmounted(() => {
    window.removeEventListener('online', onOnline)
    window.removeEventListener('offline', onOffline)
    if (cachePoller) clearInterval(cachePoller)
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
    clearCache,
    updateCacheInfo
  }
}
