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

// 登录
await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)

// 找 input (不依赖 autocomplete)
const inputs = await page.$$('input[type="text"], input[type="password"], input:not([type])')
console.log(`  found ${inputs.length} inputs`)
for (let i = 0; i < inputs.length; i++) {
  const ph = await inputs[i].getAttribute('placeholder')
  const visible = await inputs[i].isVisible()
  console.log(`    [${i}] placeholder=${ph} visible=${visible}`)
}

// 选 username/password 输入
const usernameInput = await page.locator('input').first()
const pwdInput = await page.locator('input[type="password"]').first()
await usernameInput.fill('adminLiugl')
await pwdInput.fill('Liugl@2026')

// 找登录按钮
const submitBtn = page.locator('button:has-text("登录")').first()
await submitBtn.click()
await page.waitForTimeout(10000)

console.log(`\n  current URL: ${page.url()}`)
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 25)) {
  console.log(`    ERR: ${e.text.slice(0, 500)}`)
  if (e.location) console.log(`      at ${e.location.url}:${e.location.lineNumber}`)
  if (e.stack) console.log(`      ${e.stack.split('\n').slice(0, 8).join('\n      ')}`)
}

await browser.close()
