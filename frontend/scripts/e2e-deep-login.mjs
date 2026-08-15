/**
 * V3.7.38+ 模拟登录后所有页面 + 抓 SW / message 错
 */
import { chromium } from 'playwright'

const BASE = 'http://localhost:5173'

const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext()
const page = await ctx.newPage()

const errors = []
page.on('console', msg => {
  if (msg.type() === 'error') {
    errors.push({ text: msg.text() })
  }
})
page.on('pageerror', err => {
  errors.push({ text: 'PAGE: ' + err.message })
})
page.on('crash', () => errors.push({ text: 'PAGE CRASH' }))

console.log('=== 1. 访问 /login ===')
await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 10)) {
  console.log(`    ${e.text.slice(0, 300)}`)
}
errors.length = 0

console.log('\n=== 2. 登录 ===')
await page.goto(`${BASE}/login?demo=1`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)
console.log(`  errors after demo: ${errors.length}`)
for (const e of errors.slice(0, 10)) {
  console.log(`    ${e.text.slice(0, 300)}`)
}
errors.length = 0

console.log('\n=== 3. 访问 /admin/dashboard ===')
await page.goto(`${BASE}/admin/dashboard`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 10)) {
  console.log(`    ${e.text.slice(0, 300)}`)
}

await browser.close()
