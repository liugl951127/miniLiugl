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
  errors.push({ text: 'PAGE: ' + err.message, stack: err.stack })
})

console.log('=== 1. /login?demo=1 (V3.5.93+ 演示模式) ===')
await page.goto(`${BASE}/login?demo=1`, { waitUntil: 'networkidle' }).catch(e => console.log('  goto:', e.message))
await page.waitForTimeout(5000)
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 10)) {
  console.log(`    ${e.text.slice(0, 400)}`)
  if (e.location) console.log(`      at ${e.location.url}:${e.location.lineNumber}`)
}
errors.length = 0

console.log('\n=== 2. 模拟点 "一键登录" 按钮 ===')
try {
  // 找 demo 按钮
  const demoBtn = await page.$('button:has-text("演示"), button:has-text("Demo"), button:has-text("adminLiugl")')
  if (demoBtn) {
    await demoBtn.click()
    await page.waitForTimeout(5000)
    console.log(`  current URL: ${page.url()}`)
  } else {
    console.log('  no demo button found')
  }
} catch (e) {
  console.log(`  click fail: ${e.message.slice(0, 200)}`)
}
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 10)) {
  console.log(`    ${e.text.slice(0, 400)}`)
  if (e.location) console.log(`      at ${e.location.url}:${e.location.lineNumber}`)
}
errors.length = 0

console.log('\n=== 3. 访问 /admin/dashboard ===')
await page.goto(`${BASE}/admin/dashboard`, { waitUntil: 'networkidle' }).catch(e => console.log('  goto:', e.message))
await page.waitForTimeout(5000)
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 15)) {
  console.log(`    ${e.text.slice(0, 400)}`)
  if (e.location) console.log(`      at ${e.location.url}:${e.location.lineNumber}`)
}

await browser.close()
