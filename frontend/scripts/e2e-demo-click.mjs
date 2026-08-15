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
    errors.push(t)
  }
})
page.on('pageerror', err => errors.push('PAGE: ' + err.message))

console.log('=== 1. /login?demo=1 ===')
await page.goto(`${BASE}/login?demo=1`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(5000)
errors.length = 0

console.log('\n=== 2. 点 adminLiugl 演示卡 ===')
try {
  const card = await page.$('.demo-card.role-super, .demo-card:has-text("adminLiugl")')
  if (card) {
    await card.click()
    await page.waitForTimeout(5000)
    console.log(`  current URL: ${page.url()}`)
    console.log(`  errors: ${errors.length}`)
    for (const e of errors.slice(0, 10)) console.log(`    ${e.slice(0, 300)}`)
  } else {
    console.log('  no card found')
  }
} catch (e) {
  console.log('  click fail:', e.message.slice(0, 200))
}

await browser.close()
