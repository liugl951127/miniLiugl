import { createI18n } from 'vue-i18n'
import zh from './locales/zh'
import en from './locales/en'

// 探测浏览器语言
const browserLang = navigator.language.toLowerCase().startsWith('zh') ? 'zh' : 'en'
const savedLang = localStorage.getItem('minimax_lang') || browserLang

export const i18n = createI18n({
  legacy: false,
  locale: savedLang,
  fallbackLocale: 'zh',
  messages: { zh, en }
})

/** 全局便捷函数 */
export const t = (key) => i18n.global.t(key)

/** 切换语言 */
export function setLang(lang) {
  i18n.global.locale.value = lang
  localStorage.setItem('minimax_lang', lang)
  document.documentElement.lang = lang
}

/** 当前语言 */
export const currentLang = () => i18n.global.locale.value
