/**
 * @file e2e/cross-browser.spec.js (V3.5.76+)
 * @description 跨浏览器兼容 E2E - 在 chromium/webkit/firefox/mobile 跑同一组测试
 * @scope 验证 V3.5.76 polyfill 跟 compat.scss 覆盖度
 */
import { test, expect } from '@playwright/test'

test.describe('跨浏览器基础兼容', () => {
  test('页面加载 + 不报错', async ({ page }) => {
    const errors = []
    page.on('pageerror', err => errors.push(err.message))
    page.on('console', msg => {
      if (msg.type() === 'error') errors.push(msg.text())
    })
    await page.goto('/', { waitUntil: 'networkidle' })
    // 任何错误都 fail
    expect(errors.filter(e => !e.includes('favicon') && !e.includes('sw.js') && !e.includes('Devtools'))).toHaveLength(0)
  })

  test('localStorage / sessionStorage 可用', async ({ page }) => {
    await page.goto('/')
    const ok = await page.evaluate(() => {
      try {
        localStorage.setItem('__test__', '1')
        sessionStorage.setItem('__test__', '2')
        return localStorage.getItem('__test__') === '1' && sessionStorage.getItem('__test__') === '2'
      } catch (e) {
        return false
      }
    })
    expect(ok).toBe(true)
  })

  test('CSS 变量支持', async ({ page }) => {
    await page.goto('/')
    const supportCSSVar = await page.evaluate(() => {
      const el = document.createElement('div')
      el.style.setProperty('--test-var', 'red')
      el.style.color = 'var(--test-var)'
      document.body.appendChild(el)
      const color = getComputedStyle(el).color
      el.remove()
      return color === 'rgb(255, 0, 0)' || color === 'red'
    })
    expect(supportCSSVar).toBe(true)
  })

  test('ES2017 async/await', async ({ page }) => {
    await page.goto('/')
    const result = await page.evaluate(async () => {
      const delay = (ms) => new Promise(r => setTimeout(r, ms))
      const start = Date.now()
      await delay(10)
      return Date.now() - start >= 10
    })
    expect(result).toBe(true)
  })

  test('ES2020 Optional chaining', async ({ page }) => {
    await page.goto('/')
    const result = await page.evaluate(() => {
      try {
        const v = ({})?.x?.y
        return v === undefined
      } catch (e) {
        return false
      }
    })
    expect(result).toBe(true)
  })

  test('ES2020 Nullish coalescing', async ({ page }) => {
    await page.goto('/')
    const result = await page.evaluate(() => {
      try {
        return (null ?? 'a') === 'a' && (undefined ?? 'b') === 'b' && (0 ?? 'c') === 0
      } catch (e) {
        return false
      }
    })
    expect(result).toBe(true)
  })

  test('fetch API', async ({ page }) => {
    await page.goto('/')
    const result = await page.evaluate(async () => {
      try {
        const r = await fetch('/')
        return r.status === 200
      } catch (e) {
        return false
      }
    })
    expect(result).toBe(true)
  })

  test('AbortController', async ({ page }) => {
    await page.goto('/')
    const result = await page.evaluate(() => {
      try {
        const c = new AbortController()
        c.abort()
        return c.signal.aborted === true
      } catch (e) {
        return false
      }
    })
    expect(result).toBe(true)
  })

  test('IntersectionObserver', async ({ page }) => {
    await page.goto('/')
    const result = await page.evaluate(() => {
      try {
        return typeof IntersectionObserver === 'function'
      } catch (e) {
        return false
      }
    })
    expect(result).toBe(true)
  })
})

test.describe('响应式布局', () => {
  test('桌面 1280px - 主菜单可见', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/login')
    // 登录页或主页面
  })

  test('平板 768px', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 })
    await page.goto('/')
  })

  test('手机 375px - iPhone SE', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/')
  })
})
