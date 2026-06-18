// MiniMax PWA Service Worker
const CACHE_NAME = 'minimax-v4.2'
const RUNTIME_CACHE = 'minimax-runtime'

const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/manifest.json',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
]

// 安装
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(PRECACHE_URLS).catch(() => {}))
      .then(() => self.skipWaiting())
  )
})

// 激活 - 清旧缓存
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((key) => key !== CACHE_NAME && key !== RUNTIME_CACHE)
          .map((key) => caches.delete(key))
      )
    ).then(() => self.clients.claim())
  )
})

// fetch - 网络优先, 离线 fallback
self.addEventListener('fetch', (event) => {
  const { request } = event

  // 跳过非 GET / WebSocket / POST (API)
  if (request.method !== 'GET') return
  if (request.url.includes('/api/')) return
  if (request.url.includes('/ws/')) return

  // 静态资源 - cache-first
  if (request.url.match(/\.(js|css|png|jpg|jpeg|gif|webp|svg|woff2?|ttf)$/)) {
    event.respondWith(
      caches.match(request).then((cached) => {
        if (cached) return cached
        return fetch(request).then((resp) => {
          if (resp.ok) {
            const clone = resp.clone()
            caches.open(RUNTIME_CACHE).then((c) => c.put(request, clone))
          }
          return resp
        }).catch(() => cached)
      })
    )
    return
  }

  // HTML - 网络优先, 离线用 index.html
  event.respondWith(
    fetch(request)
      .then((resp) => {
        if (resp.ok) {
          const clone = resp.clone()
          caches.open(RUNTIME_CACHE).then((c) => c.put(request, clone))
        }
        return resp
      })
      .catch(() => caches.match(request).then((c) => c || caches.match('/index.html')))
  )
})

// 消息 - 可强制更新
self.addEventListener('message', (event) => {
  if (event.data?.type === 'SKIP_WAITING') self.skipWaiting()
})
