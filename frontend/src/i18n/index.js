/**
 * @file i18n/index.js - 极简 i18n (V6.3+ 带真实中文翻译)
 * 
 * 不用 vue-i18n 9.x, 自己实现
 * 翻译查 zh.js, 找不到返回 key
 */
import zhModule from './locales/zh'
const zh = zhModule.default || zhModule

// 嵌套 key 查找
function lookup(obj, key) {
  if (!obj || !key) return undefined
  if (key in obj) return obj[key]
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
  const found = lookup(zh, key)
  if (found !== undefined) {
    if (typeof found === 'function') return found(...args)
    return found
  }
  return key  // 兜底
}

export function useI18n() {
  return {
    t,
    te: (key) => lookup(zh, key) !== undefined,
    locale: { value: 'zh' },
    locales: { value: ['zh'] }
  }
}

export function setLang(lang) {
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('minimax_lang', lang)
  }
}

export function currentLang() {
  if (typeof localStorage !== 'undefined') {
    return localStorage.getItem('minimax_lang') || 'zh'
  }
  return 'zh'
}

export const i18n = {
  t, useI18n, setLang, currentLang,
  locale: 'zh', mode: 'composition',
  install: (app) => {
    if (app?.config?.globalProperties) {
      app.config.globalProperties.$t = t
    }
  }
}

export function initI18n() { return i18n }

export default { t, useI18n, setLang, currentLang, i18n, initI18n }
