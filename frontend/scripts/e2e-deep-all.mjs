/**
 * V3.7.38+ 深度 e2e 跑所有路由, 模拟交互, 抓所有 console 错
 */
import { chromium } from 'playwright'
import fs from 'node:fs'

const BASE = 'http://localhost:5173'
const routerContent = fs.readFileSync('src/router/index.js', 'utf8')
const routes = [...routerContent.matchAll(/path:\s*['"]([^'"]+)['"]/g)].map(m => m[1])
  .filter(p => !p.includes(':') && !p.startsWith('/mobile'))

const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext()

// 预置 demo mode
await ctx.addInitScript(() => {
  localStorage.setItem('minimax_demo_mode', 'true')
  localStorage.setItem('minimax_demo_user', 'adminLiugl')
})

const page = await ctx.newPage()

const errors = []
page.on('console', msg => {
  if (msg.type() === 'error') {
    const t = msg.text()
    if (t.includes('Outdated Optimize Dep') || t.includes('504') || t.includes('runtime.lastError')) return
    errors.push(t)
  }
})
page.on('pageerror', err => {
  errors.push('PAGE: ' + err.message)
})

// preheat
await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)
errors.length = 0

// 访问每个路由 + 点击第一个 button
console.log(`[scan] ${routes.length} routes`)
for (const r of routes) {
  try {
    await page.goto(`${BASE}${r}`, { waitUntil: 'networkidle', timeout: 8000 }).catch(() => {})
    await page.waitForTimeout(2000)
    // 模拟点击第一个可点 button
    const btn = await page.$('button:not([disabled])')
    if (btn) {
      try { await btn.click({ timeout: 500 }); await page.waitForTimeout(500) } catch (e) {}
    }
  } catch (e) {}
}

await browser.close()

// 报 unique 错
const unique = [...new Set(errors)]
console.log(`\n=== ${unique.length} unique errors ===`)
for (const e of unique.slice(0, 30)) {
  console.log(`  ${e.slice(0, 500)}`)
}
