import { chromium } from 'playwright'
const BASE = 'http://localhost:5173'
const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext()
const page = await ctx.newPage()

page.on('console', msg => {
  if (msg.type() === 'error' || msg.text().includes('DEBUG')) {
    console.log(`  [${msg.type()}] ${msg.text().slice(0, 200)}`)
  }
})
page.on('response', resp => {
  if (resp.url().includes('/api/v1/auth/')) {
    console.log(`  [resp] ${resp.status()} ${resp.url().slice(0, 80)}`)
  }
})

await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)

await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)

await page.locator('input[placeholder="Enter username"]').fill('adminLiugl')
await page.locator('input[placeholder="Enter password"]').fill('Liugl@2026')
await page.locator('input[placeholder="Enter password"]').press('Enter')

for (let i = 0; i < 4; i++) {
  await page.waitForTimeout(2000)
  const data = await page.evaluate(() => localStorage.getItem('minimax-user') || 'null')
  console.log(`  T+${(i+1)*2}s URL: ${page.url()}`)
  console.log(`    LS: ${data.slice(0, 150)}`)
}
await browser.close()
