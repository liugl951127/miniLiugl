import { chromium } from 'playwright'
const BASE = 'http://localhost:5173'
const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext()
const page = await ctx.newPage()

const errors = []
page.on('console', msg => {
  if (msg.type() === 'error') {
    const t = msg.text()
    if (t.includes('Outdated Optimize Dep') || t.includes('504') || t.includes('runtime.lastError')) return
    errors.push({ text: t, location: msg.location() })
  }
})
page.on('pageerror', err => {
  errors.push({ text: 'PAGEERROR: ' + err.message, stack: err.stack })
})

// preheat
await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)
errors.length = 0

// demo 模式登录 (自动跳 /admin/dashboard)
console.log('[1] /login?demo=1 + 触发 demo')
await page.goto(`${BASE}/login?demo=1`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)
errors.length = 0

// 手动跳 dashboard
console.log('\n[2] 访问 /admin/dashboard')
await page.goto(`${BASE}/admin/dashboard`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)
console.log(`  current: ${page.url()}`)
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 20)) {
  console.log(`    ERR: ${e.text.slice(0, 500)}`)
  if (e.location) console.log(`      at ${e.location.url}:${e.location.lineNumber}`)
}

await browser.close()
