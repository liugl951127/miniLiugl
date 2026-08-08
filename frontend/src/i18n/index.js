/**
 * @file i18n/index.js - 极简 i18n (V6.3+)
 * 移除所有翻译逻辑，t() 直接返回 key 本身
 */
export function t(key) { return key || '' }
export function useI18n() { return { t, locale: { value: 'zh' } } }
export function setLang() {}
export function currentLang() { return 'zh' }
export const i18n = { install: (app) => { app.config.globalProperties.$t = t } }
