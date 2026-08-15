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

// preheat
await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(10000)
errors.length = 0

// 模拟登录 (admin)
await page.goto(`${BASE}/login?demo=1`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)
errors.length = 0

// kg 路由
console.log('[1] /kg')
await page.goto(`${BASE}/kg`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(5000)
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 10)) {
  console.log(`    ${e.text.slice(0, 500)}`)
}
errors.length = 0

// 模拟点击节点
console.log('\n[2] 模拟点击图节点')
try {
  const node = await page.$('.kg-canvas')  // chart container
  if (node) {
    const box = await node.boundingBox()
    if (box) {
      await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
      await page.waitForTimeout(2000)
    }
  }
} catch (e) {}
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 10)) {
  console.log(`    ${e.text.slice(0, 500)}`)
}

await browser.close()
