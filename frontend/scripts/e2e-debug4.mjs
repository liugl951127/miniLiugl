import { chromium } from 'playwright'
const BASE = 'http://localhost:5173'
const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext()
const page = await ctx.newPage()

// 拦截 res
page.on('response', async resp => {
  if (resp.url().includes('/api/v1/auth/login') && resp.status() === 200) {
    const body = await resp.text()
    const parsed = JSON.parse(body)
    console.log(`  login full response: ${JSON.stringify(parsed).slice(0, 500)}`)
  }
})

await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)

await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)

await page.locator('input[placeholder="Enter username"]').fill('adminLiugl')
await page.locator('input[placeholder="Enter password"]').fill('Liugl@2026')

// 拦截 res, 看 res.data 实际结构
await page.evaluate(() => {
  const orig = window.fetch
  window.fetch = async (...args) => {
    const resp = await orig(...args)
    const url = args[0]
    if (typeof url === 'string' && url.includes('/api/v1/auth/login')) {
      const cloned = resp.clone()
      const body = await cloned.text()
      console.log('FETCH login response body:', body.slice(0, 500))
    }
    return resp
  }
})

await page.locator('input[placeholder="Enter password"]').press('Enter')
await page.waitForTimeout(8000)

await browser.close()
