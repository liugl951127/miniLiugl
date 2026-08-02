#!/usr/bin/env node
/**
 * V3.7.20+ postbuild 脚本: 注入 SW_BUILD_TIME
 * 1. dist/sw.js 替换 __SW_BUILD_TIME__ → ISO 时间
 * 2. dist/index.html 替换 ?v=__SW_BUILD_TIME__ → ?v={ISO}
 * 3. frontend/index.html 源文件也替换
 * 4. 写 .sw-build-time.json 给 dev fallback
 *
 * 路径处理: 兼容从 scripts/ 或 frontend/scripts/ 调用
 */
import { readFileSync, writeFileSync, existsSync } from 'node:fs'
import { join, dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

// V3.7.20+ 智能定位 FRONTEND_DIR
// - 优先: __dirname/.. (scripts/inject-sw-build-time.mjs → frontend/)
// - 备选: cwd 是 frontend
// - 备选: __dirname/../.. (frontend/scripts/inject-sw-build-time.mjs → frontend/)
function findFrontendDir() {
  // 优先: 同级 frontend/ 目录
  const candidates = [
    resolve(__dirname, '../frontend'),  // scripts/ → frontend/
    resolve(__dirname, '..'),           // frontend/scripts/ → frontend/
    resolve(process.cwd(), 'frontend'),  // cwd 是 miniLiugl/
    resolve(process.cwd()),              // cwd 是 frontend/
  ]
  for (const dir of candidates) {
    if (existsSync(join(dir, 'vite.config.js'))) return dir
  }
  return resolve(__dirname, '../frontend')  // fallback
}

const FRONTEND_DIR = findFrontendDir()
const FILES = [
  { path: join(FRONTEND_DIR, 'dist', 'sw.js'), label: 'dist/sw.js' },
  { path: join(FRONTEND_DIR, 'dist', 'index.html'), label: 'dist/index.html' },
  // V3.7.20+ 不再修改源文件 frontend/index.html (保持 __SW_BUILD_TIME__ 占位符)
]
const JSON_OUT = join(FRONTEND_DIR, '.sw-build-time.json')

const ts = new Date().toISOString()
console.log(`[inject-sw-build-time] FRONTEND_DIR=${FRONTEND_DIR}`)
console.log(`[inject-sw-build-time] timestamp=${ts}`)

let replaced = 0

for (const { path: filePath, label } of FILES) {
  try {
    if (!existsSync(filePath)) continue
    const content = readFileSync(filePath, 'utf-8')
    let updated = content.replace(/__SW_BUILD_TIME__/g, ts)
    // V3.7.20+ 同时处理 ?v=3.5.79 (旧硬编码) → ?v={ts}
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
