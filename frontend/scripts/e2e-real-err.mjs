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

// 等 vite optimize dep 完成
console.log('[1] preheat...')
await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)
errors.length = 0

// 真实登录
console.log('\n[2] /login + 填表 + 提交')
await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(2000)
// 填账号
try {
  await page.fill('input[autocomplete="username"]', 'adminLiugl')
  await page.fill('input[autocomplete="current-password"]', 'Liugl@2026')
  await page.click('button[type="submit"]')
} catch (e) {
  console.log('  fill fail:', e.message.slice(0, 200))
}
await page.waitForTimeout(5000)
console.log(`  errors after submit: ${errors.length}`)
for (const e of errors.slice(0, 30)) {
  console.log(`    ${e.text.slice(0, 400)}`)
}

await browser.close()
