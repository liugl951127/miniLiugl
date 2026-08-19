/**
 * @file preferences.js - Pinia 主题/语言偏好 (V6.8.9)
 */

import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { authApi } from '@/api/auth'

export const usePreferencesStore = defineStore(
  'preferences',
  () => {
    /** 主题: 'light' | 'dark' */
    const theme = ref(localStorage.getItem('minimax-theme') || 'light')

    /** 语言: 'zh-CN' | 'en' */
    const language = ref(localStorage.getItem('minimax-language') || 'zh-CN')

    // 初始化：从 localStorage 恢复 + 同步到 DOM
    function init() {
      applyTheme(theme.value)
    }

    // 应用主题到 DOM
    function applyTheme(t) {
      document.documentElement.setAttribute('data-theme', t)
      if (t === 'dark') {
        document.documentElement.classList.add('el-theme-dark')
      } else {
        document.documentElement.classList.remove('el-theme-dark')
      }
      localStorage.setItem('minimax-theme', t)
    }

    // 切换主题
    async function toggleTheme() {
      const next = theme.value === 'light' ? 'dark' : 'light'
      theme.value = next
      applyTheme(next)
      // 异步同步到后端（失败不打断）
      syncToBackend(next).catch(() => {})
    }

    // 设置主题（指定值）
    async function setTheme(t) {
      theme.value = t
      applyTheme(t)
      syncToBackend(t).catch(() => {})
    }

    // 同步到后端
    async function syncToBackend(t) {
      try {
        await authApi.setTheme(t)
      } catch (_) {}
    }

    // 初始化时从后端拉取最新偏好
    async function fetchFromBackend() {
      try {
        const res = await authApi.getPreferences()
        const data = res.data || res || {}
        theme.value = data.theme || 'light'
        language.value = data.language || 'zh-CN'
        applyTheme(theme.value)
        localStorage.setItem('minimax-language', language.value)
      } catch (_) {
        // 后端拉失败就用本地缓存
      }
    }

    watch(theme, (t) => applyTheme(t))

    return { theme, language, init, toggleTheme, setTheme, fetchFromBackend }
  },
  { persist: { key: 'minimax-prefs', storage: localStorage } }
)
