/**
 * @file notification.js - Pinia 状态管理 (V3.5.12+)
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useUserStore } from './user'
import { createNotificationWS } from '@/utils/ws'
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
  const wsConnected = ref(false)
  let wsInstance = null

  // ── REST API ────────────────────────────────────────────────────────────

  async function fetchList(page = 1, size = 20) {
    const res = await listNotifications({ page, size })
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

  // ── WebSocket (V5.22 重构: 使用 ws.js 工具类) ─────────────────────────

  function connect() {
    const userStore = useUserStore()
    if (!userStore.accessToken) return
    if (wsInstance) {
      wsInstance.close(1000, 'reconnect')
      wsInstance = null
    }

    wsInstance = createNotificationWS(userStore.accessToken, {
      onOpen: () => {
        wsConnected.value = true
        wsInstance.send({ type: 'subscribe' })
      },
      onMessage: (msg) => {
        if (msg.type === 'notification' && msg.data) {
          notifications.value.unshift(msg.data)
          if (msg.data.isRead === 0) unreadCount.value++
        }
      },
      onClose: () => {
        wsConnected.value = false
        wsInstance = null
      },
      onError: () => {
        wsConnected.value = false
        wsInstance = null
      },
    })

    wsInstance.connect()
  }

  function disconnect() {
    if (wsInstance) {
      wsInstance.close(1000, 'logout')
      wsInstance = null
      wsConnected.value = false
    }
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