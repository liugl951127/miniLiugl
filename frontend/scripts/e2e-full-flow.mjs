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
console.log('[1] preheat /')
await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)
errors.length = 0

// 登录
console.log('\n[2] /login?demo=1')
await page.goto(`${BASE}/login?demo=1`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)
errors.length = 0

// 填账号
console.log('\n[3] fill + submit')
try {
  // 找表单
  const usernameInput = await page.$('input[autocomplete="username"]')
  const pwdInput = await page.$('input[autocomplete="current-password"]')
  if (usernameInput && pwdInput) {
    await usernameInput.fill('adminLiugl')
    await pwdInput.fill('Liugl@2026')
    await page.click('button[type="submit"]')
    await page.waitForTimeout(8000)
  } else {
    console.log('  no input found, trying alt')
    // alt: 用 el-form
    const inputs = await page.$$('input')
    if (inputs.length >= 2) {
      await inputs[0].fill('adminLiugl')
      await inputs[1].fill('Liugl@2026')
      await page.click('button:has-text("登录"), button:has-text("Login"), button.el-button--primary')
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
  if (e.stack) console.log(`      ${e.stack.split('\n').slice(0, 5).join('\n      ')}`)
}

await browser.close()
