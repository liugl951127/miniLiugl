/**
 * @file session.js - Pinia 状态管理 (V3.5.12+)
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { sessionApi, messageApi } from '@/api/session'

/**
 * 会话 Store：
 * - sessions: 当前用户的会话列表
 * - currentSessionId: 当前选中的会话
 * - messages: 当前会话的消息
 *
 * 关键设计：
 *  1. 列表 / 详情懒加载
 *  2. 切会话不清消息数组（避免闪烁），新会话前清空
 *  3. append 后立刻 UI 推一条（不等服务器）
 */
export const useSessionStore = defineStore(
  'session',
  () => {
    const sessions = ref([])
    const currentSessionId = ref(null)
    const messages = ref([])
    const loading = ref(false)

    const currentSession = computed(() =>
        sessions.value.find(s => s.id === currentSessionId.value) || null
    )

    async function loadSessions(status = null) {
      loading.value = true
      try {
        const res = await sessionApi.list(status ? { status } : {})
        sessions.value = res.data || []
      } finally {
        loading.value = false
      }
    }

    async function createSession(payload) {
      const res = await sessionApi.create(payload)
      const s = res.data
      sessions.value.unshift(s)
      currentSessionId.value = s.id
      messages.value = []
      return s
    }

    async function selectSession(id) {
      currentSessionId.value = id
      await loadMessages(id, 0, 50)
    }

    async function loadMessages(sessionId, page = 0, size = 50) {
      const res = await messageApi.list(sessionId, { page, size })
      messages.value = res.data || []
    }

    async function appendMessage(sessionId, payload) {
      // 立即推 UI
      const tempId = -Date.now()
      const temp = {
        id: tempId,
        sessionId,
        role: payload.role,
        content: payload.content,
        createdAt: new Date().toISOString()
      }
      messages.value.push(temp)
      // 服务器持久化
      const res = await messageApi.append(sessionId, payload)
      // 替换临时
      const idx = messages.value.findIndex(m => m.id === tempId)
      if (idx >= 0) messages.value[idx] = res.data
      return res.data
    }

    async function removeSession(id) {
      await sessionApi.remove(id)
      sessions.value = sessions.value.filter(s => s.id !== id)
      if (currentSessionId.value === id) {
        currentSessionId.value = null
        messages.value = []
      }
    }

    function clear() {
      sessions.value = []
      currentSessionId.value = null
      messages.value = []
    }

    return {
      sessions, currentSessionId, messages, loading, currentSession,
      loadSessions, createSession, selectSession, loadMessages,
      appendMessage, removeSession, clear
    }
  },
  { persist: { key: 'minimax-session', storage: localStorage } }
)
