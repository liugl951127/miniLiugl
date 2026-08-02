import { chromium } from 'playwright'
import fs from 'node:fs'

const BASE = process.argv[2] || 'http://localhost:5173'
const routerContent = fs.readFileSync('src/router/index.js', 'utf8')
const routes = [...routerContent.matchAll(/path:\s*['"]([^'"]+)['"]/g)].map(m => m[1])
  .filter(p => !p.includes(':') && !p.startsWith('/mobile'))

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
  errors.push({ text: 'PAGEERROR: ' + err.message })
})

console.log(`[scan] ${routes.length} routes to test`)

let totalWithErrors = 0
for (const r of routes) {
  errors.length = 0
  try {
    await page.goto(`${BASE}${r}`, { waitUntil: 'networkidle', timeout: 10000 }).catch(e => {
      // ignore
    })
    await page.waitForTimeout(2000)
    if (errors.length > 0) {
      console.log(`\n  [${r}]: ${errors.length} errors`)
      for (const e of errors.slice(0, 5)) {
        console.log(`    ERR: ${e.text.slice(0, 250)}`)
      }
      totalWithErrors++
    } else {
      process.stdout.write('.')
    }
  } catch (e) {
    console.log(`\n  [${r}]: navigation failed: ${e.message.slice(0, 100)}`)
  }
}

console.log(`\n[scan] done: ${totalWithErrors}/${routes.length} routes have errors`)
await browser.close()
