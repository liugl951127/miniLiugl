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

await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)

// 用 placeholder 定位
const usernameInput = page.locator('input[placeholder="Enter username"]')
const pwdInput = page.locator('input[placeholder="Enter password"]')
await usernameInput.fill('adminLiugl')
await pwdInput.fill('Liugl@2026')

// 找登录按钮
const submitBtn = page.locator('button:has-text("登录")').first()
await submitBtn.click()
await page.waitForTimeout(12000)

console.log(`\n  current URL: ${page.url()}`)
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 30)) {
  console.log(`    ERR: ${e.text.slice(0, 500)}`)
  if (e.location) console.log(`      at ${e.location.url}:${e.location.lineNumber}`)
  if (e.stack) console.log(`      ${e.stack.split('\n').slice(0, 10).join('\n      ')}`)
}

await browser.close()
