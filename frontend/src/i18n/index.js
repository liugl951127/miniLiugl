/**
 * @file i18n/index.js - 国际化 (i18n) 配置 (V3.5.55+)
 *
 * V3.5.55 修: createI18n 改成 lazy init (let i18n 代替 const, 第一次访问才创建)
 * 之前 V3.5.50-54: 顶部 export const i18n = createI18n() 触发 ESM 循环 import TDZ
 *  - i18n chunk import { defineComponent } from vue chunk
 *  - i18n chunk 顶部就调 createI18n -> 内部用 defineComponent
 *  - vue chunk 还没跑完, defineComponent 调 isFunction, isFunction TDZ
 *  - 错: 'Cannot access isFunction before initialization'
 *
 * 加载顺序 (lazy):
 *   1. 本地 locales/zh.json + en.json
 *   2. 首次访问 i18n 才创建 createI18n (延后到 main.js app.use(i18n) 之后)
 *   3. 合并到 vue-i18n 实例
 */
import { createI18n as _createI18n } from 'vue-i18n'
import zh from './locales/zh'
import en from './locales/en'

// 探测浏览器语言
function detectLang() {
  if (typeof navigator === 'undefined') return 'zh'
  return navigator.language.toLowerCase().startsWith('zh') ? 'zh' : 'en'
}

let _i18n = null
let _lang = null

function ensureI18n() {
  if (_i18n) return _i18n
  const savedLang = (typeof localStorage !== 'undefined' && localStorage.getItem('minimax_lang')) || detectLang()
  _lang = savedLang
  _i18n = _createI18n({
    legacy: false,
    locale: savedLang,
    fallbackLocale: 'zh',
    messages: { zh, en }
  })
  return _i18n
}

// 代理对象 - 第一次访问才创建 i18n 实例, 避开模块加载时的循环 import
export const i18n = new Proxy({}, {
  get(_, prop) {
    const inst = ensureI18n()
    const v = inst[prop]
    return typeof v === 'function' ? v.bind(inst) : v
  },
  set(_, prop, value) {
    const inst = ensureI18n()
    inst[prop] = value
    return true
  }
})

/** 全局便捷函数 */
export const t = (key) => ensureI18n().global.t(key)

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
