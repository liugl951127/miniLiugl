import { chromium } from 'playwright'
const BASE = 'http://localhost:5173'
const browser = await chromium.launch({ headless: true })
const ctx = await browser.newContext()
const page = await ctx.newPage()

const allReq = []
page.on('request', req => {
  if (req.url().includes('/api/')) {
    allReq.push({ method: req.method(), url: req.url() })
  }
})
const allResp = []
page.on('response', resp => {
  if (resp.url().includes('/api/')) {
    allResp.push({ status: resp.status(), url: resp.url() })
  }
})

const errors = []
page.on('console', msg => {
  if (msg.type() === 'error') {
    const t = msg.text()
    if (t.includes('Outdated Optimize Dep') || t.includes('504') || t.includes('runtime.lastError')) return
    errors.push({ text: t, location: msg.location() })
  }
})
page.on('pageerror', err => errors.push({ text: 'PAGE: ' + err.message, stack: err.stack }))

await page.goto(`${BASE}/`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(8000)
allReq.length = 0
allResp.length = 0
errors.length = 0

await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' }).catch(() => {})
await page.waitForTimeout(3000)

await page.locator('input[placeholder="Enter username"]').fill('adminLiugl')
await page.locator('input[placeholder="Enter password"]').fill('Liugl@2026')
await page.locator('input[placeholder="Enter password"]').press('Enter')
await page.waitForTimeout(12000)

console.log(`\n  current URL: ${page.url()}`)
console.log(`  errors: ${errors.length}`)
for (const e of errors.slice(0, 30)) {
  console.log(`    ERR: ${e.text.slice(0, 500)}`)
  if (e.location) console.log(`      at ${e.location.url}:${e.location.lineNumber}`)
  if (e.stack) console.log(`      ${e.stack.split('\n').slice(0, 10).join('\n      ')}`)
}

console.log(`\n  requests:`)
for (const r of allReq.slice(0, 10)) console.log(`    ${r.method} ${r.url.slice(0, 100)}`)
console.log(`\n  responses:`)
for (const r of allResp.slice(0, 10)) console.log(`    ${r.status} ${r.url.slice(0, 100)}`)

await browser.close()
