import { chromium } from 'playwright'
const BASE = 'http://localhost:5173'
const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext()
const page = await ctx.newPage()

page.on('request', req => {
  if (req.url().includes('/api/v1/auth/')) {
    const h = req.headers()
    console.log(`  REQ ${req.method()} ${req.url().slice(0, 80)}`)
    console.log(`     Authorization: ${(h.authorization || 'NONE').slice(0, 60)}`)
  }
})
page.on('response', async resp => {
  if (resp.url().includes('/api/v1/auth/login') && resp.status() === 200) {
    const body = await resp.text().catch(() => '')
    console.log(`  LOGIN 200: ${body.slice(0, 300)}`)
  }
})

await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)

await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)

await page.locator('input[placeholder="Enter username"]').fill('adminLiugl')
await page.locator('input[placeholder="Enter password"]').fill('Liugl@2026')
await page.locator('input[placeholder="Enter password"]').press('Enter')
await page.waitForTimeout(8000)

console.log(`\n  final URL: ${page.url()}`)
await browser.close()
