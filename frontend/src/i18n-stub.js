/**
 * V6.2+ 简化 i18n stub - 彻底去除 vue-i18n 依赖
 * 
 * 特点:
 * 1. 不依赖任何外部模块 (locale 文件)
 * 2. t(key) 永远返回 key 本身 (无翻译状态)
 * 3. useI18n() / i18n / setLang / currentLang 全部兼容
 * 4. 体积 0 KB
 * 
 * 后续: 真实中文文案需要单独写, 这里是占位
 */

const STUB = '__STUB__'

// 简单翻译函数 - 永远返回 key
export function t(key, ...args) {
  if (!key) return ''
  return key
}

// 兼容 useI18n
export function useI18n() {
  return {
    t,
    te: () => false,
    locale: { value: 'zh' },
    locales: { value: ['zh'] }
  }
}

// 兼容 setLang / currentLang
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

// i18n 实例 - 给 main.js app.use 兼容
export const i18n = {
  t,
  setLang,
  currentLang,
  locale: 'zh',
  mode: 'composition',
  install: (app) => {
    // noop: 不需要真正 install
    if (app && app.config && app.config.globalProperties) {
      app.config.globalProperties.$t = t
    }
  }
}

export function initI18n() {
  return i18n
}

export default { t, useI18n, setLang, currentLang, i18n, initI18n }
