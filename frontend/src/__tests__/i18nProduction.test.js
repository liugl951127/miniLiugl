/**
 * V6.2+ 模拟生产环境 useI18n 行为
 * 这是 e.t is not a function 错误的真凶测试
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createApp, h, defineComponent } from 'vue'
import { useI18n } from '@/i18n'
import zh from '@/i18n/locales/zh'
import en from '@/i18n/locales/en'

describe('生产环境 useI18n (V6.2+ e.t 修复验证)', () => {
  let i18nInstance

  beforeEach(() => {
    // 真实创建, 与生产环境一致
    const { createI18n } = require('vue-i18n')
    i18nInstance = createI18n({
      legacy: false,
      locale: 'zh',
      fallbackLocale: 'zh',
      messages: { zh, en }
    })
  })

  it('场景 1: 真实 createI18n + app.use + useI18n (生产标准流程)', () => {
    // 模拟 main.js
    const app = createApp({ render: () => h('div', 'root') })
    app.use(i18nInstance)  // ← 真实 install

    // 模拟 Login.vue 内的 useI18n
    const TestComp = defineComponent({
      setup() {
        const { t } = useI18n()
        return { t }
      },
      template: '<div class="i18n-test">{{ t("login.title") }}</div>'
    })

    let err = null
    try {
      const wrapper = mount(TestComp, {
        global: { plugins: [i18nInstance] }
      })
      const html = wrapper.html()
      expect(html).toContain('登录')
    } catch (e) {
      err = e
    }

    if (err) {
      console.error('e.t 错误:', err.message)
    }
    expect(err).toBe(null)
  })

  it('场景 2: V6.2+ 包装后 i18n - 模拟我刚做的修复', () => {
    // 模拟 i18n/index.js 包装后
    const wrapped = {
      get mode() { return i18nInstance.mode },
      get global() { return i18nInstance.global },
      get install() { return i18nInstance.install },
      t(key, ...args) { return i18nInstance.global.t(key, ...args) }
    }

    const app = createApp({ render: () => h('div', 'root') })
    app.use(wrapped)  // ← 用包装后的 i18n

    const TestComp = defineComponent({
      setup() {
        const { t } = useI18n()
        return { t }
      },
      template: '<div>{{ t("login.title") }}</div>'
    })

    let err = null
    try {
      const wrapper = mount(TestComp, {
        global: { plugins: [wrapped] }
      })
      expect(wrapper.html()).toContain('登录')
    } catch (e) {
      err = e
    }
    if (err) {
      console.error('包装后 e.t 错误:', err.message)
    }
    expect(err).toBe(null)
  })

  it('场景 3: 之前的 Proxy 包装 - 模拟原来的 bug', () => {
    // 模拟 i18n/index.js 之前的 Proxy 包装
    const proxied = new Proxy({}, {
      get(_, prop) {
        const v = i18nInstance[prop]
        return typeof v === 'function' ? v.bind(i18nInstance) : v
      }
    })

    const app = createApp({ render: () => h('div', 'root') })
    
    let installErr = null
    try {
      app.use(proxied)  // ← Proxy 包
    } catch (e) {
      installErr = e
    }
    
    if (installErr) {
      console.error('Proxy install 失败 (原 bug):', installErr.message)
    }
    // 关键: Proxy 应该 install 失败, 因为 vue-i18n 检查 i18n.mode 等
    // 如果这里不报错, 说明 Proxy 也能用 - 那原 bug 在别处
  })
})
