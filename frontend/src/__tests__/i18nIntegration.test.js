/**
 * V6.2+ i18n 集成测试 - 模拟 Login.vue 实际 useI18n()
 * 这是 e.t is not a function 错误的真实测试
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createApp, h } from 'vue'
import { createI18n } from 'vue-i18n'
import zh from '@/i18n/locales/zh'
import en from '@/i18n/locales/en'

describe('i18n 集成测试 (V6.2+ 修复 e.t)', () => {
  let i18n
  let app

  beforeEach(() => {
    i18n = createI18n({
      legacy: false,
      locale: 'zh',
      fallbackLocale: 'zh',
      messages: { zh, en }
    })
    app = createApp({ render: () => h('div', 'test') })
  })

  it('✓ 直接用真实 createI18n app.use 应该 OK', () => {
    app.use(i18n)
    expect(i18n.mode).toBe('composition')
  })

  it('✓ 用对象包装 i18n 后, 还能正常 install?', () => {
    // 模拟 V6.2+ 修复: 用对象包装
    const wrapped = {
      get mode() { return i18n.mode },
      get global() { return i18n.global },
      get install() { return i18n.install },
      t(key, ...args) { return i18n.global.t(key, ...args) }
    }
    app.use(wrapped)
    // install 触发后, i18n 实例被 vue 内部接管
    // 现在测试 useI18n
    expect(wrapped.mode).toBe('composition')
    expect(typeof wrapped.t).toBe('function')
    expect(wrapped.t('login.title')).toBeTruthy()
  })

  it('✓ 模拟 Login.vue: useI18n + t() 在模板中能正常用', async () => {
    // 模拟 main.js: app.use(i18n)
    app.use(i18n)
    
    // 模拟 Login.vue 内的 useI18n
    const { useI18n } = await import('vue-i18n')
    const TestComp = {
      setup() {
        const { t } = useI18n()
        return { t }
      },
      template: '<div>{{ t("login.title") }}</div>'
    }
    
    let err = null
    try {
      const wrapper = mount(TestComp, {
        global: { plugins: [i18n] }
      })
      expect(wrapper.html()).toContain('登录')
    } catch (e) {
      err = e
    }
    
    if (err) {
      console.error('e.t 错误重现:', err.message)
    }
    expect(err).toBe(null)
  })
})
