#!/usr/bin/env node
/**
 * V3.7.21+ postbuild 脚本: 注入 SW_BUILD_TIME
 *
 * 1. dist/sw.js 替换 __SW_BUILD_TIME__ → ISO 时间
 * 2. dist/index.html 替换 ?v=__SW_BUILD_TIME__ → ?v={ISO}
 * 3. 写 .sw-build-time.json 给 dev fallback
 *
 * 路径:
 * - 位置: frontend/scripts/inject-sw-build-time.mjs
 * - FRONTEND_DIR 自动定位到 ../ (即 frontend/)
 * - 调用: package.json postbuild
 */
import { readFileSync, writeFileSync, existsSync } from 'node:fs'
import { join, dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

// V3.7.21+ FRONTEND_DIR = __dirname/.. (frontend/scripts/ → frontend/)
const FRONTEND_DIR = resolve(__dirname, '..')
const FILES = [
  { path: join(FRONTEND_DIR, 'dist', 'sw.js'), label: 'dist/sw.js' },
  { path: join(FRONTEND_DIR, 'dist', 'index.html'), label: 'dist/index.html' },
]
const JSON_OUT = join(FRONTEND_DIR, '.sw-build-time.json')

const ts = new Date().toISOString()
console.log(`[inject-sw-build-time] FRONTEND_DIR=${FRONTEND_DIR}`)
console.log(`[inject-sw-build-time] timestamp=${ts}`)

let replaced = 0

for (const { path: filePath, label } of FILES) {
  try {
    if (!existsSync(filePath)) {
      console.log(`[inject-sw-build-time] ${label} not found, skip`)
      continue
    }
    const content = readFileSync(filePath, 'utf-8')
    let updated = content.replace(/__SW_BUILD_TIME__/g, ts)
    // 兼容旧硬编码 ?v=3.5.x
    updated = updated.replace(/\?v=3\.5\.\d+/g, `?v=${encodeURIComponent(ts)}`)
    if (updated !== content) {
      writeFileSync(filePath, updated)
      console.log(`[inject-sw-build-time] Injected into ${label} (${content.length} -> ${updated.length} bytes)`)
      replaced++
    }
  } catch (e) {
    console.warn(`[inject-sw-build-time] ${label} error: ${e.message}`)
  }
}

writeFileSync(JSON_OUT, JSON.stringify({ buildTime: ts }, null, 2))
console.log(`[inject-sw-build-time] Wrote ${JSON_OUT} (${replaced} files updated)`)
