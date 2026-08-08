/**
 * @file i18n/index.js - 极简 i18n (V6.3+ 合并版)
 * 
 * 彻底去除 vue-i18n 9.x 依赖, 体积 0 KB
 * - t(key) 直接返回 key 本身 (无翻译状态)
 * - useI18n() 兼容 vue-i18n API
 * - i18n.install() 模拟 vue-i18n plugin install
 * - setLang() / currentLang() 持久化到 localStorage
 * 
 * 用法:
 *   import { useI18n, t, i18n, setLang } from '@/i18n'
 *   const { t } = useI18n()
 *   t('login.title')  // → 'login.title' (无翻译)
 *   
 *   app.use(i18n)  // 模拟 vue-i18n 插件
 */
export function t(key, ...args) { return key || '' }

export function useI18n() { 
  return { 
    t, 
    te: () => false,
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
  t,
  useI18n,
  setLang,
  currentLang,
  locale: 'zh',
  mode: 'composition',
  install: (app) => {
    if (app && app.config && app.config.globalProperties) {
      app.config.globalProperties.$t = t
    }
  }
}
