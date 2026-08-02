import { chromium } from 'playwright'
const BASE = 'http://localhost:5173'
const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext()
const page = await ctx.newPage()

page.on('console', msg => {
  if (msg.text().includes('UserStore') || msg.text().includes('token')) {
    console.log(`  [console] ${msg.text()}`)
  }
})

await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)

await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)

// 注入 store 调试
await page.evaluate(() => {
  const checkStore = () => {
    const data = localStorage.getItem('minimax-user')
    if (data) {
      console.log('UserStore persisted:', data.slice(0, 200))
    } else {
      console.log('UserStore not persisted yet')
    }
  }
  setInterval(checkStore, 500)
})

await page.locator('input[placeholder="Enter username"]').fill('adminLiugl')
await page.locator('input[placeholder="Enter password"]').fill('Liugl@2026')
await page.locator('input[placeholder="Enter password"]').press('Enter')

for (let i = 0; i < 5; i++) {
  await page.waitForTimeout(1500)
  const data = await page.evaluate(() => {
    const d = localStorage.getItem('minimax-user')
    return d ? d.slice(0, 200) : 'null'
  })
  console.log(`  T+${(i+1)*1.5}s URL: ${page.url()}`)
  console.log(`    localStorage.minimax-user: ${data}`)
}

await browser.close()
