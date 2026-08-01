/**
 * Liugl-AI PWA Service Worker (V2.8.9 完整版)
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
 * @author Liugl-AI
 * @since V2.8.9
 */

const CACHE_VERSION = 'v3.5.74'
const CACHE_NAME = `minimax-${CACHE_VERSION}`
const RUNTIME_CACHE = 'liugl-runtime'
const API_CACHE = 'liugl-api'
const OFFLINE_URL = '/offline.html'

// 静态资源预缓存 (构建时由 vite-plugin-pwa 注入, 这里手工维护核心)
const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/offline.html',
  '/manifest.json',
  '/favicon.svg',
  // '/icons/icon-192.png'  // V3.5.41: dist/icons/ 只有 svg, 无 png
  // '/icons/icon-512.png'  // V3.5.41: dist/icons/ 只有 svg, 无 png
  '/icons/icon-192.svg'
]

// V3.5.71+ /assets/* 缓存 (NetworkFirst, 永远拿最新, 离线兜底)
const ASSETS_CACHE = 'minimax-assets-runtime'

// API 路径模式 (NetworkFirst, 可离线读缓存)
const API_GET_PATTERNS = [
  /\/api\/v\d+\/auth\/me/,
  /\/api\/v\d+\/ai\/tools/,
  /\/api\/v\d+\/ai\/framework\/(agents|permission)/,
  /\/api\/v\d+\/collab\/rooms/,
  /\/api\/v\d+\/tensorboard\//
]

// V3.5.73+ Background Sync 队列 (IndexedDB)
const QUEUE_DB = 'minimax-bg-sync'
const QUEUE_STORE = 'pending-requests'
const SYNC_TAG = 'minimax-bg-sync'

// 永不缓存
const NEVER_CACHE_PATTERNS = [
  /\/api\/v\d+\/auth\/(login|logout|refresh)/,
  /\/api\/v\d+\/ws\//,
  /\/sockjs-node\//,
  /\/api\/v\d+\/admin\//,        // 管理操作
  /\/api\/v\d+\/chat\/send/,     // 发消息
  /\/api\/v\d+\/collab\/rooms\/[^/]+\/doc\/ops/  // CRDT op 写
]

// V3.5.73+ Background Sync event handler
// 浏览器从离线恢复时触发, 重发 IndexedDB 队列
self.addEventListener('sync', (event) => {
  if (event.tag !== SYNC_TAG) return
  console.log('[SW] Background Sync 触发, 重发离线队列...')
  event.waitUntil(replayQueuedRequests(event))
})

async function replayQueuedRequests(event) {
  const queued = await getQueuedRequests()
  console.log(`[SW] 队列里有 ${queued.length} 个待发请求`)
  let success = 0, failed = 0
  for (const entry of queued) {
    try {
      const resp = await fetch(entry.url, { method: entry.method, headers: entry.headers, body: entry.body })
      if (resp.ok) {
        await deleteQueuedRequest(entry.id)
        success++
        console.log('[SW] 重发成功:', entry.url, '->', resp.status)
        await notifyClientsOfSyncResult(entry, resp)
      } else if (resp.status >= 400 && resp.status < 500) {
        await deleteQueuedRequest(entry.id)
        failed++
        console.warn('[SW] 4xx 客户端错, 丢弃:', entry.url, '->', resp.status)
        await notifyClientsOfSyncResult(entry, resp, 'client-error')
      } else {
        await incrementRetry(entry.id)
        failed++
        console.warn('[SW] 5xx 服务端错, 留待重试:', entry.url, '->', resp.status)
      }
    } catch (e) {
      await incrementRetry(entry.id)
      failed++
      console.warn('[SW] 网络仍失败, 留待重试:', entry.url, e.message)
    }
  }
  console.log(`[SW] Background Sync 完成: ${success} 成功, ${failed} 失败`)
  const remaining = await getQueuedRequests()
  if (remaining.length > 0 && event && event.again) {
    event.again()
    console.log('[SW] 请求浏览器再次 sync (还有', remaining.length, '个待发)')
  }
}

async function notifyClientsOfSyncResult(entry, response, errorTag) {
  try {
    const allClients = await self.clients.matchAll({ includeUncontrolled: true })
    for (const client of allClients) {
      client.postMessage({
        type: 'bg-sync-result',
        url: entry.url, method: entry.method, status: response.status,
        ok: response.ok, errorTag: errorTag || null, timestamp: Date.now()
      })
    }
  } catch (e) {
    console.warn('[SW] notifyClients 失败:', e.message)
  }
}

// ============= Lifecycle =============

self.addEventListener('install, (event) => {
  console.log('[SW] Installing v' + CACHE_VERSION)
  event.waitUntil(
    (async () => {
      const cache = await caches.open(CACHE_NAME)
      // V3.5.43: 每个 URL 用 try-catch, cache.put 失败也不阻塞 install
      for (const url of PRECACHE_URLS) {
        try {
          // 用 fetch + put 代替 cache.add (避免 add 内部 opaque response 错)
          const resp = await fetch(url, { cache: 'no-cache' })
          if (resp.ok) {
            await cache.put(url, resp.clone())
          } else {
            console.warn('[SW] skip pre-cache (non-2xx):', url, resp.status)
          }
        } catch (e) {
          console.warn('[SW] pre-cache failed:', url, e.message)
        }
      }
      await self.skipWaiting()
    })()
  )
})

self.addEventListener('activate', (event) => {
  console.log('[SW] Activating v' + CACHE_VERSION)
  event.waitUntil(
    (async () => {
      // 删除旧版本缓存
      // V3.5.51 修: CACHE_NAME 用 minimax- 前缀, 但旧版 activate 只删 liugl- 前缀, 老 minimax-v3.5.45 等残留导致旧 chunk 反复报错
      // 现在: liugl- 跟 minimax- 前缀都清, 但保留 RUNTIME_CACHE/API_CACHE (运行时缓存) 跟当前 CACHE_NAME
      const keys = await caches.keys()
      await Promise.all(
        keys
          .filter((key) =>
            (key.startsWith('liugl-') || key.startsWith('minimax-')) &&
            key !== CACHE_NAME &&
            key !== API_CACHE
            // V3.5.70 修: RUNTIME_CACHE 不再豁免, 老 /assets/*.js 可能 import 'vue' 裸 specifier
            // 浏览器报 "Failed to resolve module specifier 'vue'", 强制清空老 runtime cache
            // V3.5.71+ /assets/* 走独立 ASSETS_CACHE (NetworkFirst), 升版本也一并清
          )
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

  // 7. V3.5.71+ V3.5.72 扩展: NetworkFirst 静态资源
  //   - /assets/* JS/CSS chunk (vite build 产物, hash 命名)
  //   - /icons/* PWA 图标 (PWA 部署时常更新)
  //   - /favicon.svg 站点图标
  // 老 cache 仅作 offline fallback
  if (shouldNetworkFirst(url.pathname)) {
    event.respondWith(handleStaticNetworkFirst(req))
    return
  }

  // 8. 其它静态资源 (images/fonts) - CacheFirst with revalidate
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

/**
 * V3.5.72+: 判断路径是否走 NetworkFirst 策略
 *
 * @param {string} pathname - URL pathname
 * @returns {boolean}
 */
function shouldNetworkFirst(pathname) {
  return (
    pathname.startsWith('/assets/') ||      // V3.5.71: vite build JS/CSS chunk
    pathname.startsWith('/icons/') ||       // V3.5.72: PWA 图标
    pathname === '/favicon.svg'             // V3.5.72: 站点 favicon
  )
}

/**
 * V3.5.71+ NetworkFirst 策略
 *
 * 适用范围: /assets/* JS/CSS (V3.5.71) + /icons/* + /favicon.svg (V3.5.72)
 *
 * 背景: V3.5.70 用户浏览器报错 "Failed to resolve module specifier 'vue'"
 * 原因: 老 /assets/*.js 用 import 'vue' 裸 specifier (V3.5.62-63 时期 externalGlobals 方案)
 * 浏览器 sw 缓存了老 chunk, CacheFirst 命中就返回, 跑老代码报错
 *
 * 修法: 这些静态资源永远 NetworkFirst
 *   1. 5s 内从网络拿最新
 *   2. 成功 → 返回 + 更新 ASSETS_CACHE
 *   3. 失败/超时 → 走 ASSETS_CACHE 老版本 (offline fallback)
 *   4. 完全没缓存 → 503
 *
 * 这样新版本代码永远从网络拿, 老 cache 只在断网时用
 */
async function handleStaticNetworkFirst(req) {
  const cache = await caches.open(ASSETS_CACHE)
  try {
    const controller = new AbortController()
    const timeout = setTimeout(() => controller.abort(), 5000)
    const network = await fetch(req, { cache: 'no-cache', signal: controller.signal })
    clearTimeout(timeout)
    if (network.ok) {
      // 限制运行时缓存大小 (50 资源)
      limitCacheSize(ASSETS_CACHE, 50)
      cache.put(req, network.clone())
      return network
    }
    // 非 2xx: 走缓存
    const cached = await cache.match(req)
    if (cached) return cached
    return network
  } catch (e) {
    // 网络失败/超时: 走缓存 (offline fallback)
    const cached = await cache.match(req)
    if (cached) {
      console.log('[SW] /assets/* 离线返回缓存:', new URL(req.url).pathname)
      return cached
    }
    return new Response('Offline + no cache: ' + new URL(req.url).pathname, {
      status: 503,
      headers: { 'Content-Type': 'text/plain' }
    })
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
      self.registration.showNotification(data.title || 'Liugl-AI', {
        body: data.body || '',
        icon: '/icons/icon-192.svg',
        badge: '/icons/icon-192.svg',
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
