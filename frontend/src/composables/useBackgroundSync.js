/**
 * useBackgroundSync (V3.5.73+)
 *
 * 让 UI 监听 SW 的 background sync 结果, 用户提交表单后:
 * - 在线: 立即成功
 * - 离线: SW 入队 IndexedDB, 返回 202 '已保存, 联网后自动发送'
 * - 恢复网络: SW 触发 sync 事件, 重发队列, postMessage 给客户端通知结果
 *
 * 用法:
 *   const { onSyncResult, getPendingCount } = useBackgroundSync()
 *   onSyncResult((result) => toast(`已发送: ${result.url}`))
 *   const pending = await getPendingCount()  // 当前队列数
 */
import { ref, onMounted, onBeforeUnmount, readonly } from 'vue'

const syncResults = ref([])
const pendingCount = ref(0)
const isSupported = ref(false)
const isOnline = ref(navigator.onLine)

let messageHandler = null
let onlineHandler = null
let offlineHandler = null
let pendingCheckInterval = null

export function useBackgroundSync() {
  /**
   * 监听 sync 事件结果
   * @param {(result: { url, method, status, ok, errorTag, timestamp }) => void} callback
   */
  function onSyncResult(callback) {
    if (!messageHandler) {
      messageHandler = (event) => {
        if (event.data && event.data.type === 'bg-sync-result') {
          const result = event.data
          syncResults.value = [result, ...syncResults.value.slice(0, 49)]
          if (callback) callback(result)
          // 重新查队列
          getPendingCount()
        }
      }
      navigator.serviceWorker?.addEventListener('message', messageHandler)
    }
  }

  /**
   * 查当前 IndexedDB 队列数
   */
  async function getPendingCount() {
    if (!('serviceWorker' in navigator) || !('indexedDB' in window)) {
      pendingCount.value = 0
      return 0
    }
    return new Promise((resolve) => {
      const req = indexedDB.open('minimax-bg-sync', 1)
      req.onsuccess = () => {
        const db = req.result
        if (!db.objectStoreNames.contains('pending-requests')) {
          pendingCount.value = 0
          db.close()
          return resolve(0)
        }
        const tx = db.transaction('pending-requests', 'readonly')
        const store = tx.objectStore('pending-requests')
        const countReq = store.count()
        countReq.onsuccess = () => {
          pendingCount.value = countReq.result
          db.close()
          resolve(countReq.result)
        }
        countReq.onerror = () => {
          pendingCount.value = 0
          db.close()
          resolve(0)
        }
      }
      req.onerror = () => {
        pendingCount.value = 0
        resolve(0)
      }
    })
  }

  /**
   * 检测 Background Sync API 支持 (Chrome/Edge 支持, Safari/Firefox 不支持)
   */
  async function checkSupport() {
    if (!('serviceWorker' in navigator)) {
      isSupported.value = false
      return false
    }
    try {
      const reg = await navigator.serviceWorker.ready
      isSupported.value = 'sync' in reg
    } catch (e) {
      isSupported.value = false
    }
    return isSupported.value
  }

  // 监听网络状态
  onMounted(() => {
    checkSupport()
    getPendingCount()
    onlineHandler = () => { isOnline.value = true; getPendingCount() }
    offlineHandler = () => { isOnline.value = false }
    window.addEventListener('online', onlineHandler)
    window.addEventListener('offline', offlineHandler)
    // 每 30s 刷新 pending count (保险起见)
    pendingCheckInterval = setInterval(getPendingCount, 30000)
  })

  onBeforeUnmount(() => {
    if (messageHandler) {
      navigator.serviceWorker?.removeEventListener('message', messageHandler)
      messageHandler = null
    }
    if (onlineHandler) window.removeEventListener('online', onlineHandler)
    if (offlineHandler) window.removeEventListener('offline', offlineHandler)
    if (pendingCheckInterval) clearInterval(pendingCheckInterval)
  })

  return {
    syncResults,
    pendingCount,
    isSupported,
    isOnline,
    onSyncResult,
    getPendingCount,
    checkSupport
  }
}

export default useBackgroundSync
