/**
 * V3.7.38+ 用 Playwright 主动跑每个路由, 抓 console 错
 * 
 * 用法: 跑 vite dev server, 然后:
 *   node scripts/e2e-console-scan.mjs http://localhost:5173
 */
import { chromium } from 'playwright'

const BASE = process.argv[2] || 'http://localhost:5173'

// 从 router 抓所有路由
import fs from 'node:fs'
const routerContent = fs.readFileSync('src/router/index.js', 'utf8')
const routes = [...routerContent.matchAll(/path:\s*['"]([^'"]+)['"]/g)].map(m => m[1])
  .filter(p => !p.includes(':') && !p.startsWith('/mobile'))

const errors = []
const warnings = []

const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext()
const page = await ctx.newPage()

page.on('console', msg => {
  if (msg.type() === 'error') {
    errors.push({ text: msg.text(), location: msg.location() })
  } else if (msg.type() === 'warning') {
    warnings.push({ text: msg.text() })
  }
})
page.on('pageerror', err => {
  errors.push({ text: err.message, stack: err.stack })
})

// 先访问 login 拿 demo
console.log('=== 访问 /login ===')
await page.goto(`${BASE}/login`, { waitUntil: 'networkidle', timeout: 15000 }).catch(e => {
  console.log(`  goto fail: ${e.message}`)
})
await page.waitForTimeout(2000)
console.log(`  Errors: ${errors.length}`)

// 访问所有路由
for (const r of routes.slice(0, 30)) {
  errors.length = 0
  warnings.length = 0
  try {
    await page.goto(`${BASE}${r}`, { waitUntil: 'networkidle', timeout: 8000 }).catch(() => {})
    await page.waitForTimeout(1500)
    if (errors.length > 0) {
      console.log(`\n  ${r}:`)
      for (const e of errors.slice(0, 5)) {
        console.log(`    ERROR: ${e.text.slice(0, 200)}`)
      }
    } else {
      console.log(`  ✓ ${r}`)
    }
  } catch (e) {
    console.log(`  ✗ ${r}: ${e.message.slice(0, 100)}`)
  }
}

await browser.close()
