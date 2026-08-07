/**
 * @file i18n/index.js - 简化版 i18n (V6.2+)
 *
 * 决定: 去掉 vue-i18n 依赖, 用最简单的本地实现
 * - 直接返回 zh 翻译 (硬编码)
 * - t('xxx') 永远返回 'xxx' 作为兜底, 或 '默认中文'
 * - 完全不依赖 vue-i18n 9.x, 避免 e.t is not a function
 *
 * 使用: import { useI18n } from '@/i18n' 兼容旧代码
 *       const { t } = useI18n()
 *       t('login.title') // → 'login.title' 或对应中文
 */

import zh from './locales/zh.js'

// 翻译函数 - 简单直接, 不用 Proxy, 不用 vue-i18n
// 支持嵌套 key: t('nav.chat') → 查 zh.nav.chat
function lookup(obj, key) {
  if (!obj || !key) return undefined
  if (key in obj) return obj[key]
  // 嵌套查找: 'nav.chat' → obj.nav?.chat
  const parts = key.split('.')
  let cur = obj
  for (const p of parts) {
    if (cur && typeof cur === 'object' && p in cur) {
      cur = cur[p]
    } else {
      return undefined
    }
  }
  return cur
}

export function t(key, ...args) {
  if (!key) return ''
  // 1. 先查 zh 翻译
  const found = lookup(zh, key)
  if (found !== undefined) {
    if (typeof found === 'function') {
      return found(...args)
    }
    return found
  }
  // 2. 兜底: 返回 key 本身
  return key
}

// useI18n 兼容 - 返回 t 函数
export function useI18n() {
  return {
    t,
    te: (key) => zh[key] !== undefined,
    locale: { value: 'zh' },
    locales: { value: ['zh'] }
  }
}

// 切换语言 - 简化为 reload
export function setLang(lang) {
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('minimax_lang', lang)
  }
  if (typeof document !== 'undefined') {
    document.documentElement.lang = lang
  }
}

// 当前语言 - 永远返回 zh
export function currentLang() {
  return 'zh'
}

// i18n 实例 - 给 main.js app.use 兼容
// 不再是 vue-i18n 实例, 只是个对象
export const i18n = {
  t,
  setLang,
  currentLang,
  locale: 'zh',
  mode: 'composition'
}

// 初始化函数 (兼容 main.js)
export function initI18n() {
  return i18n
}
