/**
 * Liugl-AI PWA Service Worker (V3.5.84 简化版 - network-only)
 *
 * <h3>背景</h3>
 * V3.5.84 改: 去掉所有离线缓存, 改为 network-only 策略
 *
 * 之前版本 (V2.8.9 - V3.5.79) 用 7 类缓存策略 (PRECACHE/RUNTIME/API/ASSETS 等):
 *   - CacheFirst / NetworkFirst / CacheFirst with revalidate
 *   - 5 个 Cache Storage 名字 (minimax-v3.5.79, liugl-runtime, liugl-api, minimax-assets-runtime, etc.)
 *   - PRECACHE_URLS (9 个静态资源)
 *   - 50 资源 FIFO 限制
 *   - IndexedDB Background Sync 队列
 *
 * 改 network-only 的原因:
 *   1. 版本迭代加载错误: 老 /assets/*.js (V3.5.62-63 CDN 方案残留) 在 sw 缓存里
 *      浏览器报 "Failed to resolve module specifier 'vue'", 新版本用户跑老代码
 *   2. 缓存策略复杂度: 7 类策略 + 5 个 cache 名 + 3 个同步机制 = 维护成本高
 *   3. 缓存大小: 老 cache 占用几十 MB 空间, 用户清理不便
 *   4. 用户反馈: 离线缓存影响新版本加载, 期望发版立刻生效
 *
 * 简化后策略:
 *   - 所有 fetch 直接走网络, 不缓存
 *   - 仅保留离线 fallback: 导航请求失败时返回 /offline.html
 *   - 保留消息协议 (SKIP_WAITING / CLEAR_CACHE / GET_VERSION)
 *   - 保留 Push 通知 + Background Sync + Periodic Background Sync (写操作 / 通知用)
 *   - 浏览器 HTTP 缓存天然处理资源缓存, sw 不再干预
 *
 * <h3>新策略</h3>
 * <ul>
 *   <li>导航请求 (HTML): NetworkOnly + 离线 /offline.html fallback</li>
 *   <li>所有静态资源 (/assets/* /icons/* /favicon.svg): NetworkOnly (HTTP 缓存兜底)</li>
 *   <li>API GET: NetworkOnly (HTTP 缓存兜底)</li>
 *   <li>API POST/PUT/DELETE: NetworkOnly + 失败 503 (写操作不能错)</li>
 *   <li>WebSocket: NetworkOnly (直连)</li>
 *   <li>消息协议: SKIP_WAITING / CLEAR_CACHE / GET_VERSION / CACHE_URLS (保留)</li>
 *   <li>Push 通知: 保留 (P1 占位)</li>
 *   <li>Background Sync: 保留 (V3.5.73+ 离线写排队)</li>
 *   <li>Periodic Background Sync: 保留 (V3.5.79+ 定时同步)</li>
 * </ul>
 *
 * <h3>发版流程</h3>
 * <ul>
 *   <li>发版时 sw.js URL 加 ?v={ver} 强制浏览器拉新 (PRECACHE_URLS 已无意义)</li>
 *   <li>CACHE_VERSION 仅作消息协议返回用, 不再触发缓存</li>
 *   <li>activate 时删除所有老 cache, 保证浏览器空间释放</li>
 * </ul>
 *
 * @author Liugl-AI
 * @since V2.8.9
 * @updated V3.5.84 简化
 */

const CACHE_VERSION = 'v3.5.89'
const OFFLINE_URL = '/offline.html'

// V3.5.84+ 删 PRECACHE_URLS - 不再预缓存, 浏览器 HTTP 缓存处理
// const PRECACHE_URLS = []  // 保留注释, 标记删了什么

// V3.5.84+ 删所有 cache 名 - 不再使用
// - minimax-${CACHE_VERSION}: 预缓存 (9 URL)
// - liugl-runtime: 图片/字体
// - liugl-api: API GET
// - minimax-assets-runtime: /assets/* NetworkFirst
// - minimax-bg-sync: IndexedDB (保留, Background Sync 用)

// V3.5.73+ Background Sync 队列 (IndexedDB) - 保留
const QUEUE_DB = 'minimax-bg-sync'
const QUEUE_STORE = 'pending-requests'
const SYNC_TAG = 'minimax-bg-sync'

// V3.5.79+ Periodic Background Sync - 保留
const PERIODIC_TAG = 'minimax-periodic-sync'
const PERIODIC_MIN_INTERVAL = 60 * 60 * 1000   // 1 小时

// V3.5.73+ Background Sync event handler - 保留 (离线写排队)
self.addEventListener('sync', (event) => {
  if (event.tag !== SYNC_TAG) return
  console.log('[SW] Background Sync 触发, 重发离线队列...')
  event.waitUntil(replayQueuedRequests(event))
})

// V3.5.79+ Periodic Background Sync - 保留 (定时拉新)
self.addEventListener('periodicsync', (event) => {
  if (event.tag !== PERIODIC_TAG) return
  console.log('[SW] Periodic Background Sync 触发, 拉新数据...')
  event.waitUntil(performPeriodicSync())
})

async function performPeriodicSync() {
  // 1. 拉新通知
  try {
    const resp = await fetch('/api/v1/notification/unread', {
      cache: 'no-cache',
      credentials: 'include'
    })
    if (resp.ok) {
      const data = await resp.json()
      const unread = (data.data?.list || []).filter(n => !n.read)
      console.log(`[SW] Periodic 拉新 ${unread.length} 个未读通知`)
      const allClients = await self.clients.matchAll({ includeUncontrolled: true })
      for (const client of allClients) {
        client.postMessage({
          type: 'periodic-sync-notification',
          unreadCount: unread.length,
          notifications: unread.slice(0, 5),
          timestamp: Date.now()
        })
      }
      if (unread.length > 0 && 'showNotification' in self.registration) {
        const top = unread[0]
        await self.registration.showNotification(top.title || '新通知', {
          body: top.content || top.body || '',
          icon: '/icons/icon-192.svg',
          badge: '/icons/icon-192.svg',
          tag: 'periodic-sync-' + (top.id || 'default'),
          data: { url: top.url || '/notification' },
          requireInteraction: false,
          silent: false
        })
      }
    } else {
      console.log('[SW] Periodic 拉新 API 错:', resp.status)
    }
  } catch (e) {
    console.warn('[SW] Periodic 拉新网络失败:', e.message)
  }
}

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

// ============= IndexedDB Helpers (Background Sync 用) =============

function openQueueDB() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(QUEUE_DB, 1)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(QUEUE_STORE)) {
        db.createObjectStore(QUEUE_STORE, { keyPath: 'id', autoIncrement: true })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function getQueuedRequests() {
  try {
    const db = await openQueueDB()
    return new Promise((resolve) => {
      const tx = db.transaction(QUEUE_STORE, 'readonly')
      const store = tx.objectStore(QUEUE_STORE)
      const req = store.getAll()
      req.onsuccess = () => resolve(req.result || [])
      req.onerror = () => resolve([])
    })
  } catch (e) {
    console.warn('[SW] getQueuedRequests 失败:', e.message)
    return []
  }
}

async function deleteQueuedRequest(id) {
  try {
    const db = await openQueueDB()
    return new Promise((resolve) => {
      const tx = db.transaction(QUEUE_STORE, 'readwrite')
      tx.objectStore(QUEUE_STORE).delete(id)
      tx.oncomplete = () => resolve()
      tx.onerror = () => resolve()
    })
  } catch (e) {
    console.warn('[SW] deleteQueuedRequest 失败:', e.message)
  }
}

async function incrementRetry(id) {
  try {
    const db = await openQueueDB()
    return new Promise((resolve) => {
      const tx = db.transaction(QUEUE_STORE, 'readwrite')
      const store = tx.objectStore(QUEUE_STORE)
      const req = store.get(id)
      req.onsuccess = () => {
        const entry = req.result
        if (entry) {
          entry.retries = (entry.retries || 0) + 1
          entry.lastRetry = Date.now()
          store.put(entry)
        }
        resolve()
      }
      req.onerror = () => resolve()
    })
  } catch (e) {
    console.warn('[SW] incrementRetry 失败:', e.message)
  }
}

// ============= Lifecycle =============

// V3.5.84+ install 简化: 不再预缓存
self.addEventListener('install', (event) => {
  console.log('[SW] Installing v' + CACHE_VERSION + ' (network-only mode)')
  // V3.5.84 删: PRECACHE_URLS fetch + cache.put
  // 不再调用 self.skipWaiting() - 让浏览器等所有 tab 关闭再激活, 避免新 SW 干扰老 tab
  // V3.5.84 修: 之前 skipWaiting() 导致新 SW 立即激活, 老 tab 还没关闭就拿新 SW
  //                现在发版后让老 tab 自己关闭, 新 tab 才会注册新 SW
})

// V3.5.84+ activate 简化: 删所有老缓存 (释放浏览器空间) + 不接管 client
self.addEventListener('activate', (event) => {
  console.log('[SW] Activating v' + CACHE_VERSION + ' (network-only mode)')
  event.waitUntil(
    (async () => {
      // 删所有老 cache, 包括 liugl- / minimax- 前缀, 释放浏览器空间
      const keys = await caches.keys()
      await Promise.all(
        keys.map((key) => {
          console.log('[SW] 删除老 cache:', key)
          return caches.delete(key)
        })
      )
      // V3.5.84 删: self.clients.claim() - 不再强制接管未受控 client
      // 原因: 接管会导致正在用的 tab 突然切到新 SW, 引发状态错乱
      //       新 tab 重新注册时自然会用新 SW
    })()
  )
})

// ============= Fetch Handler (V3.5.84 network-only) =============

self.addEventListener('fetch', (event) => {
  const req = event.request
  const url = new URL(req.url)

  // 1. 跨域直接放行 (CDN / 第三方 API)
  if (url.origin !== location.origin) {
    return
  }

  // 2. WebSocket / SockJS 不缓存
  if (req.headers.get('Upgrade') === 'websocket' ||
      url.pathname.startsWith('/ws/') ||
      url.pathname.startsWith('/sockjs')) {
    return
  }

  // 3. 导航请求 (HTML) - NetworkOnly + 离线 fallback
  //    V3.5.84 唯一保留的 fallback 行为: 离线时返回 /offline.html
  if (req.mode === 'navigate') {
    event.respondWith(handleNavigation(req))
    return
  }

  // 4. V3.5.84 改: 其他所有请求都直传 (NetworkOnly)
  //    - /assets/* JS/CSS chunk: 直传 (HTTP 缓存兜底)
  //    - /icons/* PWA 图标: 直传
  //    - API GET/POST/PUT/DELETE: 直传
  //    - 写操作失败时返回 503
  if (req.method !== 'GET') {
    event.respondWith(handleWrite(req))
    return
  }

  // GET 资源全部直传, 不缓存
  event.respondWith(handleNetworkOnly(req))
})

// ============= Handler 实现 (V3.5.84 简化版) =============

/**
 * V3.5.84+ 导航请求处理
 * 唯一保留的 fallback: 离线时返回 /offline.html
 *
 * 之前 V2.8.9-V3.5.79: NetworkFirst + 缓存命中返老版本
 * 现在 V3.5.84: NetworkOnly + 网络失败返 /offline.html
 *
 * @param {Request} req
 * @returns {Promise<Response>}
 */
async function handleNavigation(req) {
  try {
    // V3.5.89+: 加 traceparent header
    const tracedReq = withTraceparent(req)
    return await fetch(tracedReq, { cache: 'no-cache' })
  } catch (e) {
    // 离线: 返回 /offline.html (浏览器 HTTP 缓存会有, 实在没有用内联)
    try {
      const offline = await fetch(OFFLINE_URL, { cache: 'no-cache' })
      if (offline.ok) return offline
    } catch (e2) {
      // /offline.html 也拉不到, 用内联 HTML
    }
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

/**
 * V3.5.84+ GET 资源处理 (NetworkOnly)
 * 浏览器 HTTP 缓存天然处理资源复用, sw 不再干预
 *
 * @param {Request} req
 * @returns {Promise<Response>}
 */
// V3.5.89+ W3C Trace Context: 给 fetch 加 traceparent header
// 格式: '00-{trace_id(32 hex)}-{span_id(16 hex)}-{flags(2 hex)}'
// 浏览器 fetch 没法自动生成 (没 OpenTelemetry JS SDK), 手工实现
function generateTraceparent() {
  const traceId = Array.from({ length: 32 }, () => '0123456789abcdef'[Math.floor(Math.random() * 16)]).join('')
  const spanId = Array.from({ length: 16 }, () => '0123456789abcdef'[Math.floor(Math.random() * 16)]).join('')
  return `00-${traceId}-${spanId}-01`  // 01 = sampled
}

// 给 req 加 traceparent header (保留原 headers)
function withTraceparent(req) {
  const newHeaders = new Headers(req.headers)
  // 不要覆盖前端 / OTel SDK 已经设的 traceparent
  if (!newHeaders.has('traceparent')) {
    newHeaders.set('traceparent', generateTraceparent())
  }
  return new Request(req.url, {
    method: req.method,
    headers: newHeaders,
    body: req.method !== 'GET' && req.method !== 'HEAD' ? req.body : undefined,
    mode: req.mode,
    credentials: req.credentials,
    cache: req.cache,
    redirect: req.redirect,
    referrer: req.referrer,
    integrity: req.integrity
  })
}

async function handleNetworkOnly(req) {
  try {
    // V3.5.89+: 加 traceparent header (W3C Trace Context)
    const tracedReq = withTraceparent(req)
    return await fetch(tracedReq, { cache: 'no-cache' })
  } catch (e) {
    // 网络失败: 不返缓存, 直接 503
    return new Response(
      JSON.stringify({ code: -1, message: '网络不可用', data: null }),
      { status: 503, headers: { 'Content-Type': 'application/json' } }
    )
  }
}

/**
 * V3.5.84+ 写操作处理 (NetworkOnly)
 * 跟之前一样, 失败返 503 错误响应
 *
 * @param {Request} req
 * @returns {Promise<Response>}
 */
async function handleWrite(req) {
  try {
    // V3.5.89+: 加 traceparent header
    const tracedReq = withTraceparent(req)
    return await fetch(tracedReq)
  } catch (e) {
    return new Response(
      JSON.stringify({ code: -1, message: '离线时无法执行写操作', data: null }),
      { status: 503, headers: { 'Content-Type': 'application/json' } }
    )
  }
}

// ============= Push Notifications (保留) =============

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

// ============= Message Handler (保留) =============

self.addEventListener('message', (event) => {
  const data = event.data || {}
  switch (data.type) {
    case 'SKIP_WAITING':
      // V3.5.84 改: 不再 skipWaiting - 让浏览器等老 tab 关闭
      // 老 tab 关闭后, 新 tab 重新注册 sw.js 自然会用新版本
      console.log('[SW] 收到 SKIP_WAITING (V3.5.84 network-only, 已忽略)')
      break
    case 'CLEAR_CACHE':
      // V3.5.84 改: 仍可清缓存, 但主要是清 IndexedDB 队列
      event.waitUntil((async () => {
        const keys = await caches.keys()
        await Promise.all(keys.map((k) => caches.delete(k)))
        console.log('[SW] All caches cleared (V3.5.84)')
      })())
      break
    case 'GET_VERSION':
      event.source && event.source.postMessage({
        type: 'SW_VERSION',
        version: CACHE_VERSION
      })
      break
    case 'CACHE_URLS':
      // V3.5.84 改: 不再支持预缓存 URL
      console.log('[SW] CACHE_URLS 已禁用 (V3.5.84 network-only)')
      break
    default:
      console.debug('[SW] unknown message:', data.type)
  }
})
