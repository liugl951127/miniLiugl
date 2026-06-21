import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useUserStore } from './user'
import {
  listNotifications,
  unreadCount as apiUnreadCount,
  markRead as apiMarkRead,
  markAllRead as apiMarkAllRead,
  clearNotifications as apiClear
} from '@/api/notification'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const unreadCount = ref(0)
  const ws = ref(null)
  const wsConnected = ref(false)
  let wsReconnectTimer = null

  // ── REST API ────────────────────────────────────────────────────────────

  async function fetchList(page = 1, size = 20) {
    const res = await listNotifications({ page, size })
    // 分页返回 data.records + data.total
    const records = res.data?.records || res.data || []
    notifications.value = records
    return records
  }

  async function fetchUnread() {
    try {
      const res = await apiUnreadCount()
      unreadCount.value = Number(res.data) || 0
    } catch (_) {}
  }

  async function markRead(id) {
    await apiMarkRead(id)
    const n = notifications.value.find(n => n.id === id)
    if (n && n.isRead === 0) {
      n.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  async function markAllRead() {
    await apiMarkAllRead()
    notifications.value.forEach(n => { n.isRead = 1 })
    unreadCount.value = 0
  }

  async function clear() {
    await apiClear()
    notifications.value = []
    unreadCount.value = 0
  }

  // ── WebSocket ───────────────────────────────────────────────────────────

  function connect() {
    const userStore = useUserStore()
    if (!userStore.accessToken || ws.value) return

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const url = `${protocol}//${location.host}/ws/notifications?token=${userStore.accessToken}`

    try {
      const socket = new WebSocket(url)
      socket.onopen = () => {
        wsConnected.value = true
        clearTimeout(wsReconnectTimer)
        console.info('[WS] Notification connected')
        socket.send(JSON.stringify({ type: 'subscribe' }))
        // 启动心跳
        startHeartbeat(socket)
      }

      socket.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data)
          if (msg.type === 'notification' && msg.data) {
            notifications.value.unshift(msg.data)
            if (msg.data.isRead === 0) {
              unreadCount.value++
            }
          }
        } catch (e) {
          // ignore parse error
        }
      }

      socket.onclose = (event) => {
        wsConnected.value = false
        ws.value = null
        console.info(`[WS] Notification closed: ${event.code} ${event.reason}`)
        // 自动重连
        if (!event.wasClean) {
          wsReconnectTimer = setTimeout(() => connect(), 3000)
        }
      }

      socket.onerror = (err) => {
        console.warn('[WS] Notification error:', err)
        wsConnected.value = false
        ws.value = null
      }

      ws.value = socket
    } catch (e) {
      console.warn('[WS] Failed to connect:', e)
    }
  }

  function disconnect() {
    clearTimeout(wsReconnectTimer)
    if (ws.value) {
      ws.value.close(1000, 'logout')
      ws.value = null
      wsConnected.value = false
    }
  }

  let heartbeatTimer = null

  function startHeartbeat(socket) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = setInterval(() => {
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type: 'ping' }))
      }
    }, 25000)
  }

  // 初始化：连接 WS + 拉取未读数
  async function init() {
    await fetchUnread()
    connect()
  }

  return {
    notifications,
    unreadCount,
    wsConnected,
    init,
    fetchList,
    fetchUnread,
    markRead,
    markAllRead,
    clear,
    connect,
    disconnect
  }
})