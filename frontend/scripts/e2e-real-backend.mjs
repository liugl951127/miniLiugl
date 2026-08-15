import { chromium } from 'playwright'
const BASE = 'http://localhost:5173'
const AUTH = 'http://localhost:8081'
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

console.log('[1] /login + 填 adminLiugl + 提交')
await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)
try {
  const inputs = await page.$$('input')
  if (inputs.length >= 2) {
    await inputs[0].fill('adminLiugl')
    await inputs[1].fill('Liugl@2026')
    // 找登录按钮
    const submitBtn = await page.$('button[type="submit"]') || await page.$('button.el-button--primary')
    if (submitBtn) {
      await submitBtn.click()
      await page.waitForTimeout(8000)
    }
  }
} catch (e) {
  console.log('  fail:', e.message.slice(0, 200))
}

console.log(`\n  current URL: ${page.url()}`)
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 20)) {
  console.log(`    ERR: ${e.text.slice(0, 500)}`)
  if (e.location) console.log(`      at ${e.location.url}:${e.location.lineNumber}`)
  if (e.stack) console.log(`      ${e.stack.split('\n').slice(0, 8).join('\n      ')}`)
}

await browser.close()
