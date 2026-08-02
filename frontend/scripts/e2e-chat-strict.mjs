import { chromium } from 'playwright'
const BASE = 'http://localhost:5173'
const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext()

// 预置 demo
await ctx.addInitScript(() => {
  localStorage.setItem('minimax_demo_mode', 'true')
  localStorage.setItem('minimax_demo_user', 'adminLiugl')
})

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
  errors.push({ text: 'PAGE: ' + err.message, stack: err.stack })
})

await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)
errors.length = 0

console.log('[1] /chat (demo mode)')
await page.goto(`${BASE}/chat`, { waitUntil: 'networkidle', timeout: 10000 }).catch(() => {})
await page.waitForTimeout(5000)
console.log(`  URL: ${page.url()}`)
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 5)) {
  console.log(`    ${e.text.slice(0, 500)}`)
  if (e.location) console.log(`      at ${e.location.url}:${e.location.lineNumber}`)
  if (e.stack) console.log(`      ${e.stack.split('\n').slice(0, 8).join('\n      ')}`)
}

await browser.close()
