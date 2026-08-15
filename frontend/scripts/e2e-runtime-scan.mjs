/**
 * V3.7.38+ 深度 e2e 跑每个路由, 模拟点击交互, 抓所有 console 错
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

const allErrors = new Map()

page.on('console', msg => {
  if (msg.type() === 'error') {
    const text = msg.text()
    if (text.includes('Outdated Optimize Dep') || text.includes('504')) return
    if (text.includes('runtime.lastError')) return  // chrome extension
    const url = page.url().replace(BASE, '')
    if (!allErrors.has(url)) allErrors.set(url, [])
    allErrors.get(url).push(text)
  }
})
page.on('pageerror', err => {
  const url = page.url().replace(BASE, '')
  if (!allErrors.has(url)) allErrors.set(url, [])
  allErrors.get(url).push('PAGEERROR: ' + err.message)
})

// preheat
console.log('[preheat]')
await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(10000)

// 测试每个路由 + 模拟点击第一个按钮
for (const r of routes) {
  try {
    await page.goto(`${BASE}${r}`, { waitUntil: 'networkidle', timeout: 8000 }).catch(() => {})
    await page.waitForTimeout(2000)
    // 试点击第一个可见按钮
    const btn = await page.$('button:not([disabled])')
    if (btn) {
      try { await btn.click({ timeout: 1000 }) } catch (e) {}
      await page.waitForTimeout(500)
    }
  } catch (e) {
    // ignore
  }
}

await browser.close()

console.log(`\n=== 错路由 ===`)
if (allErrors.size === 0) {
  console.log('  ✓ 0 错')
} else {
  for (const [url, errs] of allErrors) {
    console.log(`\n  ${url}: ${errs.length} errors`)
    for (const e of errs.slice(0, 3)) {
      console.log(`    ${e.slice(0, 400)}`)
    }
  }
}
