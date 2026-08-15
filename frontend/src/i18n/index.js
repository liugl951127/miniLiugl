/**
 * @file i18n/index.js - 极简 i18n (V6.3+ 完整版)
 * 不用 vue-i18n 9.x, 自己实现
 * 支持 zh (默认) + en (V6.3+ 新增)
 */
import zhModule from './locales/zh'
import enModule from './locales/en'
const zh = zhModule.default || zhModule
const en = enModule.default || enModule

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

function getLocale() {
  if (typeof localStorage !== 'undefined') {
    return localStorage.getItem('minimax_lang') || 'zh'
  }
  return 'zh'
}

function getDict() {
  return getLocale() === 'en' ? en : zh
}

export function t(key, ...args) {
  if (!key) return ''
  const dict = getDict()
  let found = lookup(dict, key)
  if (found === undefined) {
    // 兜底查 zh
    found = lookup(zh, key)
  }
  if (found !== undefined) {
    if (typeof found === 'function') return found(...args)
    return found
  }
  return key
}

export function useI18n() {
  return {
    t,
    te: (key) => lookup(getDict(), key) !== undefined,
    locale: { value: getLocale() },
    locales: { value: ['zh', 'en'] }
  }
}

export function setLang(lang) {
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('minimax_lang', lang)
  }
}

export function currentLang() {
  return getLocale()
}

export const i18n = {
  t, useI18n, setLang, currentLang,
  locale: getLocale(), mode: 'composition',
  install: (app) => {
    if (app?.config?.globalProperties) {
      app.config.globalProperties.$t = t
    }
  }
}

export function initI18n() { return i18n }

export default { t, useI18n, setLang, currentLang, i18n, initI18n }
