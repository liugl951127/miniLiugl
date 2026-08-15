import { chromium } from 'playwright'
const BASE = 'http://localhost:5173'
const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext()
const page = await ctx.newPage()

page.on('response', resp => {
  if (resp.url().includes('/api/v1/auth/login')) {
    console.log(`  [login] ${resp.status()}`)
  }
  if (resp.url().includes('/api/v1/auth/me')) {
    console.log(`  [me] ${resp.status()}`)
  }
})

await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)

await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)

await page.locator('input[placeholder="Enter username"]').fill('adminLiugl')
await page.locator('input[placeholder="Enter password"]').fill('Liugl@2026')
await page.locator('input[placeholder="Enter password"]').press('Enter')

// 5s 间隔
for (let i = 0; i < 6; i++) {
  await page.waitForTimeout(2000)
  console.log(`  T+${(i+1)*2}s URL: ${page.url()}`)
  // 检查 localStorage
  const token = await page.evaluate(() => localStorage.getItem('user') || '')
  console.log(`    localStorage.user: ${token.slice(0, 100)}`)
}

await browser.close()
