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
  errors.push({ text: 'PAGE: ' + err.message + '\n' + (err.stack || '').slice(0, 500) })
})

// preheat
await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)
errors.length = 0

// 访问 kg 路由
console.log('[kg] visit /kg')
await page.goto(`${BASE}/kg`, { waitUntil: 'networkidle', timeout: 15000 }).catch(e => console.log('  goto:', e.message))
await page.waitForTimeout(5000)
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 10)) {
  console.log(`    ${e.text.slice(0, 500)}`)
}

await browser.close()
