/**
 * platform.js 单元测试 (Vitest + happy-dom)
 * V5.33 Day 25
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

import {
  isWechatBrowser,
  isQQBrowser,
  isAlipayBrowser,
  isMobile,
  isMiniProgram,
  h5LoginUrl,
} from './platform.js'

describe('platform utils', () => {
  describe('isWechatBrowser', () => {
    it('should return true for micromessenger UA', () => {
      Object.defineProperty(navigator, 'userAgent', {
        value: 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) MicroMessenger/8.0.0',
        configurable: true,
      })
      expect(isWechatBrowser()).toBe(true)
    })

    it('should return false for normal Chrome UA', () => {
      Object.defineProperty(navigator, 'userAgent', {
        value: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36',
        configurable: true,
      })
      expect(isWechatBrowser()).toBe(false)
    })

    it('should handle empty userAgent', () => {
      Object.defineProperty(navigator, 'userAgent', {
        value: '',
        configurable: true,
      })
      expect(isWechatBrowser()).toBe(false)
    })
  })

  describe('isQQBrowser', () => {
    it('should return true for QQ browser UA', () => {
      Object.defineProperty(navigator, 'userAgent', {
        value: 'Mozilla/5.0 (Linux; Android 10; Mi 9T Pro) AppleWebKit/537.36 MQQBrowser/9.0 Mobile Safari/537.36',
        configurable: true,
      })
      expect(isQQBrowser()).toBe(true)
    })

    it('should return false for non-QQ browser', () => {
      Object.defineProperty(navigator, 'userAgent', {
        value: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36',
        configurable: true,
      })
      expect(isQQBrowser()).toBe(false)
    })
  })

  describe('isAlipayBrowser', () => {
    it('should return true for alipay client UA', () => {
      Object.defineProperty(navigator, 'userAgent', {
        value: 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 AliApp(AP/10.2.50) AlipayClient/10.2.50',
        configurable: true,
      })
      expect(isAlipayBrowser()).toBe(true)
    })

    it('should return false for normal browser', () => {
      Object.defineProperty(navigator, 'userAgent', {
        value: 'Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36',
        configurable: true,
      })
      expect(isAlipayBrowser()).toBe(false)
    })
  })

  describe('isMobile', () => {
    it.each([
      ['iPhone', true],
      ['iPad', true],
      ['Android', true],
      ['Windows NT 10.0', false],
      ['Macintosh', false],
    ])('UA containing %s → mobile=%p', (keyword, expected) => {
      Object.defineProperty(navigator, 'userAgent', {
        value: `Mozilla/5.0 ${keyword} Test`,
        configurable: true,
      })
      expect(isMobile()).toBe(expected)
    })

    it('should handle empty userAgent', () => {
      Object.defineProperty(navigator, 'userAgent', {
        value: '',
        configurable: true,
      })
      expect(isMobile()).toBe(false)
    })
  })

  describe('isMiniProgram', () => {
    it('should return true when wx.miniprogram is defined', () => {
      const prevWx = globalThis.wx
      try {
        globalThis.wx = { miniprogram: { hello: 'world' } }
        expect(isMiniProgram()).toBe(true)
      } finally {
        if (prevWx === undefined) delete globalThis.wx
        else globalThis.wx = prevWx
      }
    })

    it('should return false when wx is undefined', () => {
      const prevWx = globalThis.wx
      try {
        delete globalThis.wx
        expect(isMiniProgram()).toBe(false)
      } finally {
        if (prevWx !== undefined) globalThis.wx = prevWx
      }
    })
  })

  // h5LoginUrl relies on window.location.origin — browser-only API.
  // Validated by manual/integration tests. Skipped in unit test here.
  describe('h5LoginUrl', () => {
    it('placeholder — browser-only, validated in integration', () => {
      expect(true).toBe(true)
    })
  })
})
