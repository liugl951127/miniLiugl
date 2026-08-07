/**
 * @file i18n/index.js - 国际化 (i18n) 配置 (V6.2+ 终极修复)
 *
 * V6.2+ 修复 e.t is not a function:
 *   之前用 Proxy 包装 createI18n() 实例
 *   vue-i18n 9.x 的 install 期望真实实例, Proxy 在某些路径下访问 .t 失败
 *
 * V3.5.55 修: createI18n 改成 lazy init
 *   之前 V3.5.50-54: 顶部 export const i18n = createI18n() 触发 ESM 循环 import TDZ
 *
 * V6.2+ 终极方案:
 *   1. 直接 let _i18n = null (模块级, 懒加载)
 *   2. initI18n() 函数, main.js 顶部先调
 *   3. i18n 用 Proxy 但只在 initI18n() 后访问 .t 等
 *   4. 真实例, 不是包装
 */
import { createI18n } from 'vue-i18n'
import zh from './locales/zh'
import en from './locales/en'

let _i18n = null
let _lang = null

function detectLang() {
  if (typeof navigator === 'undefined') return 'zh'
  return navigator.language.toLowerCase().startsWith('zh') ? 'zh' : 'en'
}

function ensureI18n() {
  if (_i18n) return _i18n
  const savedLang = (typeof localStorage !== 'undefined' && localStorage.getItem('minimax_lang')) || detectLang()
  _lang = savedLang
  _i18n = createI18n({
    legacy: false,
    locale: savedLang,
    fallbackLocale: 'zh',
    messages: { zh, en }
  })
  return _i18n
}

// V6.2+ 导出初始化函数, main.js 顶部先调用
export function initI18n() {
  return ensureI18n()
}

// 真正暴露的 i18n 实例
// 关键: 这是一个真 i18n 实例 (createI18n 返回值)
// 不再用 Proxy 包装, 避免 vue-i18n 9.x install 时拿不到 .t 等
// getI18n() 在 main.js app 创建前调用, 此时 _i18n 已创建
export const i18n = ensureI18n()

/** 全局便捷函数 */
export const t = (key, ...args) => i18n.global.t(key, ...args)

/** 切换语言 */
export function setLang(lang) {
  const inst = ensureI18n()
  inst.global.locale.value = lang
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('minimax_lang', lang)
  }
  if (typeof document !== 'undefined') {
    document.documentElement.lang = lang
  }
}

/** 当前语言 */
export const currentLang = () => {
  if (!_i18n) return _lang || 'zh'
  return _i18n.global.locale.value
}
