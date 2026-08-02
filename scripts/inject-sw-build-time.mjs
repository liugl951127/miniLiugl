#!/usr/bin/env node
/**
 * V3.7.19+ postbuild 脚本: 注入 SW_BUILD_TIME 到 dist/sw.js
 * 替代 vite plugin (Vite 5+ hook 行为变化导致不可靠)
 * 用法: node scripts/inject-sw-build-time.mjs
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)
const FRONTEND_DIR = join(__dirname, '..', 'frontend')
const DIST = join(FRONTEND_DIR, 'dist', 'sw.js')
const JSON_OUT = join(FRONTEND_DIR, '.sw-build-time.json')

const ts = new Date().toISOString()
console.log(`[inject-sw-build-time] timestamp=${ts}`)

try {
  const content = readFileSync(DIST, 'utf-8')
  const updated = content.replace(/__SW_BUILD_TIME__/g, ts)
  if (updated !== content) {
    writeFileSync(DIST, updated)
    console.log(`[inject-sw-build-time] Injected ${ts} into dist/sw.js (${content.length} -> ${updated.length} bytes)`)
  } else {
    console.log(`[inject-sw-build-time] No __SW_BUILD_TIME__ placeholder found`)
  }
  writeFileSync(JSON_OUT, JSON.stringify({ buildTime: ts }, null, 2))
  console.log(`[inject-sw-build-time] Wrote ${JSON_OUT}`)
} catch (e) {
  console.error(`[inject-sw-build-time] Error: ${e.message}`)
  process.exit(1)
}
