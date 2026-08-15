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
page.on('pageerror', err => errors.push({ text: 'PAGE: ' + err.message, stack: err.stack }))

await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)
errors.length = 0

// 1. /login
console.log('[1] /login')
await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)

await page.locator('input[placeholder="Enter username"]').fill('adminLiugl')
await page.locator('input[placeholder="Enter password"]').fill('Liugl@2026')
await page.locator('input[placeholder="Enter password"]').press('Enter')
await page.waitForTimeout(15000)

console.log(`\n  current: ${page.url()}`)
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 30)) {
  console.log(`    ERR: ${e.text.slice(0, 500)}`)
  if (e.location) console.log(`      at ${e.location.url}:${e.location.lineNumber}`)
  if (e.stack) console.log(`      ${e.stack.split('\n').slice(0, 12).join('\n      ')}`)
}

// 2. 看页面内容
const content = await page.content()
const isError = content.includes('出错了') || content.includes('Cannot read')
console.log(`  page has "出错了" or "Cannot read": ${isError}`)
const hasKPI = content.includes('KPI') || content.includes('el-statistic')
console.log(`  page has KPI/statistic: ${hasKPI}`)

await browser.close()
