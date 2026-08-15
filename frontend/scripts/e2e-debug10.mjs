import { chromium } from 'playwright'
const BASE = 'http://localhost:5173'
const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext()
const page = await ctx.newPage()

page.on('console', msg => {
  if (msg.text().includes('USER.LOGIN')) {
    console.log(`  [browser] ${msg.text()}`)
  }
})
page.on('response', resp => {
  if (resp.url().includes('/api/v1/auth/')) {
    console.log(`  ${resp.status()} ${resp.url().slice(0, 80)}`)
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
await browser.close()
