/**
 * V3.7.38+ 抓 Vite prebundle 错
 * 浏览器控制台会报 "Failed to resolve ... optimized dep" 等
 */
import { chromium } from 'playwright'
import fs from 'node:fs'

const BASE = 'http://localhost:5173'
const routerContent = fs.readFileSync('src/router/index.js', 'utf8')
const routes = [...routerContent.matchAll(/path:\s*['"]([^'"]+)['"]/g)].map(m => m[1])
  .filter(p => !p.includes(':') && !p.startsWith('/mobile'))

const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext()
const page = await ctx.newPage()

const allErrors = []
page.on('console', msg => {
  if (msg.type() === 'error') {
    const t = msg.text()
    allErrors.push(t)
  }
})
page.on('pageerror', err => {
  allErrors.push('PAGE: ' + err.message)
})

// 预热
console.log('[preheat]')
await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(10000)
allErrors.length = 0

// 访问所有路由
console.log(`[scan] ${routes.length} routes`)
for (const r of routes) {
  try {
    await page.goto(`${BASE}${r}`, { waitUntil: 'networkidle', timeout: 8000 }).catch(() => {})
    await page.waitForTimeout(1500)
  } catch (e) {}
}

await browser.close()

// 找 prebundle / dep 错
const prebundle = allErrors.filter(e => 
  /optimized dep|pre-bundle|dep-.*\.js|optimizeDeps|cannot resolve|is not exported|does not provide|@vite\/client/i.test(e)
)
const other = allErrors.filter(e => !prebundle.includes(e))

console.log(`\n=== ${allErrors.length} errors total ===`)
if (prebundle.length > 0) {
  console.log(`\n--- prebundle/dep 错 (${prebundle.length}) ---`)
  for (const e of [...new Set(prebundle)].slice(0, 20)) {
    console.log(`  ${e.slice(0, 400)}`)
  }
}
if (other.length > 0) {
  console.log(`\n--- 其他错 (${other.length}) ---`)
  for (const e of [...new Set(other)].slice(0, 10)) {
    console.log(`  ${e.slice(0, 400)}`)
  }
}
