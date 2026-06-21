// MiniMax PWA Service Worker (V5.20)
const CACHE_VERSION = 'v5.20.0'
const CACHE_NAME = `minimax-${CACHE_VERSION}`
const RUNTIME_CACHE = 'minimax-runtime'
const API_CACHE = 'minimax-api'

// 静态资源预缓存
const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/manifest.json',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
  '/favicon.svg',
  '/api-docs',
]

// 安装: 预缓存 + skipWaiting
self.addEventListener('install', (event) => {
  console.log('[SW V5.20] install')
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(PRECACHE_URLS).catch((e) => console.warn('precache 部分失败:', e)))
      .then(() => self.skipWaiting())
  )
})

// 激活: 清旧缓存 + clients.claim
self.addEventListener('activate', (event) => {
  console.log('[SW V5.20] activate')
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((key) => !key.startsWith('minimax-') || (key !== CACHE_NAME && key !== RUNTIME_CACHE && key !== API_CACHE))
          .map((key) => {
            console.log('[SW] 删除旧缓存:', key)
            return caches.delete(key)
          })
      )
    ).then(() => self.clients.claim())
  )
})

// Fetch 策略:
//   - 静态资源 (HTML/CSS/JS/IMG/FONT): Cache First
//   - API GET 请求: Network First, 失败 fallback Cache
//   - API POST/PUT/DELETE: Network Only (不缓存写操作)
//   - 导航请求: Network First, 失败 fallback index.html
self.addEventListener('fetch', (event) => {
  const req = event.request
  const url = new URL(req.url)

  // 跳过非 GET
  if (req.method !== 'GET') return

  // 跳过 chrome-extension 等
  if (!url.protocol.startsWith('http')) return

  // API 请求: Network First
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(networkFirst(req, API_CACHE))
    return
  }

  // 导航请求: Network First, fallback index.html (SPA)
  if (req.mode === 'navigate') {
    event.respondWith(
      fetch(req)
        .then((resp) => {
          // 成功则缓存 + 返回
          const clone = resp.clone()
          caches.open(CACHE_NAME).then((cache) => cache.put(req, clone))
          return resp
        })
        .catch(() => caches.match('/index.html').then((r) => r || new Response('Offline', { status: 503 })))
    )
    return
  }

  // 静态资源: Cache First
  event.respondWith(cacheFirst(req))
})

async function cacheFirst(req) {
  const cached = await caches.match(req)
  if (cached) return cached
  try {
    const resp = await fetch(req)
    if (resp.ok) {
      const clone = resp.clone()
      caches.open(RUNTIME_CACHE).then((cache) => cache.put(req, clone))
    }
    return resp
  } catch (e) {
    console.warn('[SW] fetch failed:', req.url, e)
    return new Response('Offline', { status: 503 })
  }
}

async function networkFirst(req, cacheName) {
  try {
    const resp = await fetch(req)
    if (resp.ok) {
      const clone = resp.clone()
      caches.open(cacheName).then((cache) => cache.put(req, clone))
    }
    return resp
  } catch (e) {
    const cached = await caches.match(req)
    if (cached) return cached
    return new Response(JSON.stringify({
      code: -1,
      msg: '离线模式: 该请求未缓存',
      offline: true
    }), {
      status: 503,
      headers: { 'Content-Type': 'application/json' }
    })
  }
}

// 监听消息: skipWaiting
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting()
  }
})

// 后台同步 (V5.20 新增)
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-data') {
    console.log('[SW] background sync')
  }
})