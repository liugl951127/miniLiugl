/**
 * MiniMax PWA Service Worker (V2.8.9 完整版)
 *
 * <h3>缓存策略</h3>
 * <ul>
 *   <li>PRECACHE: 关键静态资源 (HTML/CSS/JS) - CacheFirst</li>
 *   <li>RUNTIME: 图片/字体 - CacheFirst (容量限制)</li>
 *   <li>API GET (用户数据/AI工具): NetworkFirst + 3s 超时, 失败走缓存</li>
 *   <li>API POST/PUT/DELETE (写操作): NetworkOnly (不缓存)</li>
 *   <li>WebSocket: 不缓存, 直连</li>
 *   <li>导航请求 (HTML): NetworkFirst, 失败返回 /offline.html</li>
 * </ul>
 *
 * <h3>消息协议</h3>
 * <ul>
 *   <li>SKIP_WAITING: 客户端强制激活新 SW</li>
 *   <li>CLEAR_CACHE: 清空所有缓存</li>
 *   <li>GET_VERSION: 返回当前 SW 版本</li>
 * </ul>
 *
 * @author MiniMax
 * @since V2.8.9
 */

const CACHE_VERSION = 'v2.8.9'
const CACHE_NAME = `minimax-${CACHE_VERSION}`
const RUNTIME_CACHE = 'minimax-runtime'
const API_CACHE = 'minimax-api'
const OFFLINE_URL = '/offline.html'

// 静态资源预缓存 (构建时由 vite-plugin-pwa 注入, 这里手工维护核心)
const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/offline.html',
  '/manifest.json',
  '/favicon.svg',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
  '/icons/icon-192.svg'
]

// API 路径模式 (NetworkFirst, 可离线读缓存)
const API_GET_PATTERNS = [
  /\/api\/v\d+\/auth\/me/,
  /\/api\/v\d+\/ai\/tools/,
  /\/api\/v\d+\/ai\/framework\/(agents|permission)/,
  /\/api\/v\d+\/collab\/rooms/,
  /\/api\/v\d+\/tensorboard\//
]

// 永不缓存
const NEVER_CACHE_PATTERNS = [
  /\/api\/v\d+\/auth\/(login|logout|refresh)/,
  /\/api\/v\d+\/ws\//,
  /\/sockjs-node\//,
  /\/api\/v\d+\/admin\//,        // 管理操作
  /\/api\/v\d+\/chat\/send/,     // 发消息
  /\/api\/v\d+\/collab\/rooms\/[^/]+\/doc\/ops/  // CRDT op 写
]

// ============= Lifecycle =============

self.addEventListener('install', (event) => {
  console.log('[SW] Installing v' + CACHE_VERSION)
  event.waitUntil(
    (async () => {
      const cache = await caches.open(CACHE_NAME)
      // 用 addAll 但容错 (某些资源可能 404, 不阻塞安装)
      await Promise.allSettled(
        PRECACHE_URLS.map((url) => cache.add(url).catch((e) => {
          console.warn('[SW] pre-cache failed:', url, e.message)
        }))
      )
      await self.skipWaiting()
    })()
  )
})

self.addEventListener('activate', (event) => {
  console.log('[SW] Activating v' + CACHE_VERSION)
  event.waitUntil(
    (async () => {
      // 删除旧版本缓存
      const keys = await caches.keys()
      await Promise.all(
        keys
          .filter((key) => key.startsWith('minimax-') && key !== CACHE_NAME && key !== RUNTIME_CACHE && key !== API_CACHE)
          .map((key) => caches.delete(key))
      )
      await self.clients.claim()
    })()
  )
})

// ============= Fetch Handler =============

self.addEventListener('fetch', (event) => {
  const req = event.request
  const url = new URL(req.url)

  // 1. 跨域直接放行
  if (url.origin !== location.origin) {
    return
  }

  // 2. WebSocket / SockJS 不缓存
  if (req.headers.get('Upgrade') === 'websocket' ||
      url.pathname.startsWith('/ws/') ||
      url.pathname.startsWith('/sockjs')) {
    return
  }

  // 3. 永不缓存的路径
  if (NEVER_CACHE_PATTERNS.some((p) => p.test(url.pathname))) {
    return
  }

  // 4. 写操作 (POST/PUT/DELETE/PATCH) 不缓存, 直传
  if (req.method !== 'GET') {
    event.respondWith(handleWrite(req))
    return
  }

  // 5. 导航请求 (HTML) - NetworkFirst with offline fallback
  if (req.mode === 'navigate') {
    event.respondWith(handleNavigation(req))
    return
  }

  // 6. API GET - NetworkFirst with 3s timeout
  if (API_GET_PATTERNS.some((p) => p.test(url.pathname))) {
    event.respondWith(handleApiGet(req))
    return
  }

  // 7. 静态资源 (JS/CSS/images/fonts) - CacheFirst with revalidate
  event.respondWith(handleStatic(req))
})

// ============= Handler 实现 =============

async function handleNavigation(req) {
  try {
    const network = await fetch(req)
    if (network.ok) {
      const cache = await caches.open(CACHE_NAME)
      cache.put(req, network.clone())
    }
    return network
  } catch (e) {
    // 离线: 返回缓存的 index.html 或 /offline.html
    const cache = await caches.open(CACHE_NAME)
    const cached = await cache.match('/index.html')
    if (cached) return cached
    const offline = await cache.match(OFFLINE_URL)
    if (offline) return offline
    return new Response(
      '<!DOCTYPE html><html><head><meta charset="UTF-8"><title>离线</title></head>' +
      '<body style="font-family:sans-serif;text-align:center;padding:60px;">' +
      '<h1>📡 网络不可用</h1><p>请检查网络连接后重试</p>' +
      '<button onclick="location.reload()">重试</button>' +
      '</body></html>',
      { status: 503, headers: { 'Content-Type': 'text/html; charset=utf-8' } }
    )
  }
}

async function handleApiGet(req) {
  const cache = await caches.open(API_CACHE)
  try {
    const controller = new AbortController()
    const timeout = setTimeout(() => controller.abort(), 3000)
    const network = await fetch(req, { signal: controller.signal })
    clearTimeout(timeout)
    if (network.ok) {
      // 只缓存 200, 不缓存 401/403/500
      cache.put(req, network.clone())
      return network
    }
    // 非 2xx: 走缓存
    const cached = await cache.match(req)
    if (cached) return cached
    return network
  } catch (e) {
    // 网络失败: 走缓存
    const cached = await cache.match(req)
    if (cached) {
      console.log('[SW] API 离线返回缓存:', url.pathname)
      return cached
    }
    return new Response(
      JSON.stringify({ code: -1, message: '离线 + 无缓存', data: null }),
      { status: 503, headers: { 'Content-Type': 'application/json' } }
    )
  }
}

async function handleStatic(req) {
  const cache = await caches.open(RUNTIME_CACHE)
  const cached = await cache.match(req)
  if (cached) {
    // 异步后台更新
    fetch(req).then((network) => {
      if (network.ok) cache.put(req, network.clone())
    }).catch(() => {})
    return cached
  }
  try {
    const network = await fetch(req)
    if (network.ok && req.url.startsWith(location.origin)) {
      // 限制运行时缓存大小 (50 资源)
      limitCacheSize(RUNTIME_CACHE, 50)
      cache.put(req, network.clone())
    }
    return network
  } catch (e) {
    // 找不到资源且无缓存
    return new Response('Not Found', { status: 404 })
  }
}

async function handleWrite(req) {
  // 写操作: 直传, 失败抛出 (让前端感知)
  try {
    return await fetch(req)
  } catch (e) {
    return new Response(
      JSON.stringify({ code: -1, message: '离线时无法执行写操作', data: null }),
      { status: 503, headers: { 'Content-Type': 'application/json' } }
    )
  }
}

async function limitCacheSize(name, maxItems) {
  const cache = await caches.open(name)
  const keys = await cache.keys()
  if (keys.length > maxItems) {
    // 删除最老的 (FIFO)
    for (let i = 0; i < keys.length - maxItems; i++) {
      await cache.delete(keys[i])
    }
  }
}

// ============= Push Notifications (P1 占位) =============

self.addEventListener('push', (event) => {
  if (!event.data) return
  try {
    const data = event.data.json()
    event.waitUntil(
      self.registration.showNotification(data.title || 'MiniMax', {
        body: data.body || '',
        icon: '/icons/icon-192.png',
        badge: '/icons/icon-192.png',
        data: data.url || '/'
      })
    )
  } catch (e) {
    console.warn('[SW] push data parse error:', e.message)
  }
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = event.notification.data || '/'
  event.waitUntil(clients.openWindow(url))
})

// ============= Message Handler =============

self.addEventListener('message', (event) => {
  const data = event.data || {}
  switch (data.type) {
    case 'SKIP_WAITING':
      self.skipWaiting()
      break
    case 'CLEAR_CACHE':
      event.waitUntil((async () => {
        const keys = await caches.keys()
        await Promise.all(keys.map((k) => caches.delete(k)))
        console.log('[SW] All caches cleared')
      })())
      break
    case 'GET_VERSION':
      event.source && event.source.postMessage({
        type: 'SW_VERSION',
        version: CACHE_VERSION
      })
      break
    case 'CACHE_URLS':
      // 手动预缓存新 URL
      event.waitUntil((async () => {
        const cache = await caches.open(CACHE_NAME)
        await cache.addAll(data.urls || [])
      })())
      break
    default:
      console.debug('[SW] unknown message:', data.type)
  }
})
