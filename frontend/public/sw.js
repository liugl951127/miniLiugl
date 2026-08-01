/**
 * Liugl-AI PWA Service Worker (V3.6.13+ 移除版本控制)
 *
 * <h3>V3.6.13+ 策略</h3>
 * <ul>
 *   <li>✅ 不再使用 CACHE_NAME 版本控制</li>
 *   <li>✅ network-only 模式 - 不缓存任何东西</li>
 *   <li>✅ 浏览器 HTTP 缓存兜底 (Cache-Control / ETag)</li>
 *   <li>✅ SW_BUILD_TIME 由 vite plugin 编译时注入 (__SW_BUILD_TIME__)</li>
 *   <li>✅ 发版时 vite plugin 自动给 HTML sw.js 引用加 ?v={ts}</li>
 *   <li>✅ 离线 fallback: 导航失败返 /offline.html</li>
 * </ul>
 *
 * <h3>保留能力</h3>
 * <ul>
 *   <li>消息协议: SKIP_WAITING / CLEAR_CACHE / GET_VERSION (含 buildTime)</li>
 *   <li>Push 通知 (P1 占位)</li>
 *   <li>Background Sync (V3.5.73+ 离线写排队)</li>
 *   <li>Periodic Background Sync (V3.5.79+ 定时同步)</li>
 * </ul>
 *
 * <h3>移除/简化</h3>
 * <ul>
 *   <li>❌ CACHE_NAME 版本管理 (V3.5.41+ 弃用)</li>
 *   <li>❌ CACHE_VERSION = 'v3.5.89' 等 (V3.6.13+ 改 'auto')</li>
 *   <li>❌ PRECACHE_URLS (V3.5.84+ 删)</li>
 *   <li>❌ 5 个 Cache Storage 名字 (V3.5.84+ 删)</li>
 *   <li>❌ activate 时清理老 cache (无需, 根本没建)</li>
 * </ul>
 *
 * @author Liugl-AI
 * @since V2.8.9
 * @updated V3.6.13 移除版本控制
 */

const SW_BUILD_TIME = '__SW_BUILD_TIME__'  // vite plugin 编译时替换
const OFFLINE_URL = '/offline.html'

// IndexedDB 队列 (Background Sync 用) - 不受版本控制
const QUEUE_DB = 'minimax-bg-sync'
const QUEUE_STORE = 'pending-requests'
const SYNC_TAG = 'minimax-bg-sync'

// Periodic Background Sync
const PERIODIC_TAG = 'minimax-periodic-sync'
const PERIODIC_MIN_INTERVAL = 60 * 60 * 1000  // 1 小时

// ========== Install / Activate (no cache) ==========

self.addEventListener('install', (event) => {
  console.log('[SW] Installing (no cache, network-only)')
  // 立即激活新版本 (跳过 waiting)
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  console.log('[SW] Activating (no cache cleanup needed)')
  // 接管所有客户端
  event.waitUntil(self.clients.claim())
})

// ========== Fetch (network-only + 离线 fallback) ==========

self.addEventListener('fetch', (event) => {
  const { request } = event

  // WebSocket: 直连
  if (request.headers.get('upgrade') === 'websocket') {
    return  // 浏览器原生处理
  }

  // 导航请求: network-only + 离线 fallback
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request).catch(async () => {
        const cache = await caches.open('minimax-offline')
        const cached = await cache.match(OFFLINE_URL)
        return cached || new Response('Offline', { status: 503 })
      })
    )
    return
  }

  // 其他请求: 全部 network-only
  // 不干预 - 让浏览器走 HTTP 缓存 (Cache-Control / ETag)
})

// ========== 消息协议 (页面 ↔ SW 通信) ==========

self.addEventListener('message', (event) => {
  const { type, payload } = event.data || {}

  switch (type) {
    case 'SKIP_WAITING':
      self.skipWaiting()
      break

    case 'CLEAR_CACHE':
      // 清空所有 cache (用户主动清缓存)
      event.waitUntil(
        caches.keys().then((names) =>
          Promise.all(names.map((name) => caches.delete(name)))
        ).then(() => {
          event.ports[0]?.postMessage({ type: 'CACHE_CLEARED' })
        })
      )
      break

    case 'GET_VERSION':
      // V3.6.13+ 不返回 CACHE_VERSION, 只返回 build time
      event.ports[0]?.postMessage({
        type: 'SW_VERSION',
        buildTime: SW_BUILD_TIME,
        mode: 'network-only',
      })
      break

    case 'bg-sync-enqueue':
    case 'bg-sync-get-queue':
    case 'bg-sync-clear':
      // V3.6.9+ Background Sync 通信
      handleBgSyncMessage(event, type, payload)
      break
  }
})

// ========== Background Sync (离线写排队) ==========

self.addEventListener('sync', (event) => {
  if (event.tag !== SYNC_TAG) return
  console.log('[SW] Background Sync 触发, 重发离线队列...')
  event.waitUntil(replayQueuedRequests(event))
})

async function handleBgSyncMessage(event, type, payload) {
  switch (type) {
    case 'bg-sync-enqueue':
      event.waitUntil(replayQueuedRequests({ tag: SYNC_TAG }))
      break
    case 'bg-sync-get-queue':
      const items = await getQueuedRequests()
      event.ports[0]?.postMessage({ type: 'queue-list', items })
      break
    case 'bg-sync-clear':
      await clearQueuedRequests(payload?.ids || [])
      event.ports[0]?.postMessage({ type: 'cleared' })
      break
  }
}

function openQueueDB() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(QUEUE_DB, 1)
    req.onupgradeneeded = (e) => {
      const db = e.target.result
      if (!db.objectStoreNames.contains(QUEUE_STORE)) {
        db.createObjectStore(QUEUE_STORE, { keyPath: 'id', autoIncrement: true })
      }
    }
    req.onsuccess = (e) => resolve(e.target.result)
    req.onerror = (e) => reject(e.target.error)
  })
}

async function getQueuedRequests() {
  const db = await openQueueDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(QUEUE_STORE, 'readonly')
    const store = tx.objectStore(QUEUE_STORE)
    const req = store.getAll()
    req.onsuccess = () => resolve(req.result || [])
    req.onerror = () => reject(req.error)
  })
}

async function clearQueuedRequests(ids) {
  const db = await openQueueDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(QUEUE_STORE, 'readwrite')
    const store = tx.objectStore(QUEUE_STORE)
    for (const id of ids) store.delete(id)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

async function replayQueuedRequests(event) {
  const items = await getQueuedRequests()
  if (!items.length) return
  const succeeded = []
  for (const item of items) {
    try {
      const res = await fetch(item.url, {
        method: item.method,
        headers: item.headers,
        body: item.body,
      })
      if (res.ok) {
        succeeded.push(item.id)
        const clients = await self.clients.matchAll()
        clients.forEach((c) =>
          c.postMessage({ type: 'syncSuccess', payload: item })
        )
      }
    } catch (e) {
      console.warn('[SW] Replay failed:', e)
    }
  }
  if (succeeded.length) await clearQueuedRequests(succeeded)
}

// ========== Periodic Background Sync (定时同步) ==========

self.addEventListener('periodicsync', (event) => {
  if (event.tag !== PERIODIC_TAG) return
  console.log('[SW] Periodic Background Sync 触发, 拉新数据...')
  event.waitUntil(performPeriodicSync())
})

async function performPeriodicSync() {
  try {
    const res = await fetch('/api/v1/notifications/recent', { credentials: 'include' })
    if (!res.ok) return
    const data = await res.json()
    const items = data.items || []
    for (const item of items) {
      if (item.unread) {
        await self.registration.showNotification(item.title || '新通知', {
          body: item.body,
          tag: 'periodic-sync-' + (item.id || 'default'),
          data: { url: item.url || '/' },
        })
      }
    }
  } catch (e) {
    console.warn('[SW] Periodic sync failed:', e)
  }
}

// ========== Push 通知 (P1 占位) ==========

self.addEventListener('push', (event) => {
  if (!event.data) return
  try {
    const payload = event.data.json()
    event.waitUntil(
      self.registration.showNotification(payload.title || '新消息', {
        body: payload.body,
        icon: '/icons/icon-192.png',
        badge: '/icons/badge-72.png',
        data: { url: payload.url || '/' },
      })
    )
  } catch (e) {
    console.warn('[SW] Push failed:', e)
  }
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = event.notification.data?.url || '/'
  event.waitUntil(
    self.clients.matchAll({ type: 'window' }).then((clients) => {
      for (const c of clients) {
        if (c.url.endsWith(url) && 'focus' in c) return c.focus()
      }
      return self.clients.openWindow(url)
    })
  )
})
