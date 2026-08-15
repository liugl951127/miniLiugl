/**
 * V3.6.9+ Background Sync API
 * 离线消息队列: 用户发消息时如果离线, 缓存到 IndexedDB
 * 网络恢复时 sync 事件触发, 自动重发
 */

const DB_NAME = 'minimax-bg-sync'
const STORE_NAME = 'message-queue'
const TAG = 'minimax-message-sync'

// 打开 IndexedDB
function openDB() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1)
    req.onupgradeneeded = (e) => {
      const db = e.target.result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'id', autoIncrement: true })
      }
    }
    req.onsuccess = (e) => resolve(e.target.result)
    req.onerror = (e) => reject(e.target.error)
  })
}

// 加入队列
async function enqueueMessage(message) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    const req = store.add({ ...message, queuedAt: Date.now() })
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

// 取队列
async function getQueue() {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const store = tx.objectStore(STORE_NAME)
    const req = store.getAll()
    req.onsuccess = () => resolve(req.result || [])
    req.onerror = () => reject(req.error)
  })
}

// 清队列
async function clearQueue(ids) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    for (const id of ids) {
      store.delete(id)
    }
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

// 注册 sync
async function registerSync() {
  if ('serviceWorker' in navigator && 'SyncManager' in window) {
    const reg = await navigator.serviceWorker.ready
    try {
      await reg.sync.register(TAG)
      console.log('[BG Sync] Registered:', TAG)
      return true
    } catch (e) {
      console.warn('[BG Sync] register failed:', e)
      return false
    }
  }
  return false
}

// 暴露给页面用
self.addEventListener('message', (event) => {
  if (event.data?.type === 'enqueue') {
    enqueueMessage(event.data.payload).then(id => {
      event.ports[0]?.postMessage({ ok: true, id })
    })
  } else if (event.data?.type === 'getQueue') {
    getQueue().then(items => {
      event.ports[0]?.postMessage({ ok: true, items })
    })
  } else if (event.data?.type === 'clear') {
    clearQueue(event.data.ids).then(() => {
      event.ports[0]?.postMessage({ ok: true })
    })
  }
})

// Sync 事件: 重新发送消息
self.addEventListener('sync', (event) => {
  if (event.tag === TAG) {
    event.waitUntil(replayMessages())
  }
})

async function replayMessages() {
  const items = await getQueue()
  if (!items.length) return
  const succeeded = []
  for (const item of items) {
    try {
      const res = await fetch('/api/chat/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(item),
      })
      if (res.ok) {
        succeeded.push(item.id)
        // 通知所有客户端
        const clients = await self.clients.matchAll()
        clients.forEach(c => c.postMessage({ type: 'syncSuccess', payload: item }))
      }
    } catch (e) {
      console.warn('[BG Sync] Replay failed:', e)
    }
  }
  if (succeeded.length) {
    await clearQueue(succeeded)
  }
}
