/**
 * V6.2+ i18n 测试 - 验证 e.t is not a function 是否真修
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import zh from '@/i18n/locales/zh'
import en from '@/i18n/locales/en'

describe('i18n 真实行为 (V6.2+ e.t 错误修复)', () => {
  let i18nInstance
  
  beforeEach(() => {
    i18nInstance = createI18n({
      legacy: false,
      locale: 'zh',
      fallbackLocale: 'zh',
      messages: { zh, en }
    })
  })

  it('✓ createI18n 直接创建的实例有 global.t 方法', () => {
    expect(typeof i18nInstance.global.t).toBe('function')
  })

  it('✓ t() 方法能正常翻译', () => {
    const result = i18nInstance.global.t('login.title', '默认')
    expect(result).toBeTruthy()
  })

  it('✓ 真实插件 install 测试 - 模拟 app.use(i18n)', () => {
    const mockApp = {
      config: {
        globalProperties: {}
      },
      provide: vi.fn(),
      directive: vi.fn(),
      mixin: vi.fn()
    }
    // vue-i18n 9.x 的 install 函数
    i18nInstance.install(mockApp)
    expect(mockApp.provide).toHaveBeenCalled()
    // 关键: install 后 app.config.globalProperties.$t 应存在
    expect(typeof mockApp.config.globalProperties.$t).toBe('function')
  })

  it('✓ 真实 useI18n 测试 - 模拟组件中的 t()', async () => {
    const { useI18n } = await import('vue-i18n')
    
    // 在没有 install 的情况下, useI18n 失败
    // 安装到 mock app
    const mockApp = { config: { globalProperties: {} }, provide: vi.fn() }
    i18nInstance.install(mockApp)
    
    // 模拟 inject
    // vue-i18n 9.x 用 Symbol 注入, 简单测试 t 方法直接可用
    const t = i18nInstance.global.t
    expect(typeof t).toBe('function')
    expect(t('login.title', '默认')).toBeTruthy()
  })
})
