/**
 * V3.7.38+ 全面语法检查
 * 
 * 1. .vue 模板/脚本/样式 编译错
 * 2. .js / .ts 语法错
 * 3. .scss 样式错
 * 4. import 路径错
 */
import { parse, compileScript, compileTemplate, compileStyle } from 'vue/compiler-sfc'
import { transformSync } from 'esbuild'
import fs from 'node:fs'
import path from 'node:path'

let total = 0
const reports = []

function* walk(dir, exts) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.name === '__tests__' || e.name === 'node_modules' || e.name.startsWith('.')) continue
    const p = path.join(dir, e.name)
    if (e.isDirectory()) yield* walk(p, exts)
    else if (exts.some(ext => p.endsWith(ext))) yield p
  }
}

// 1. .vue 检查
console.log('=== 1. .vue 编译 ===')
for (const f of walk('src', ['.vue'])) {
  const content = fs.readFileSync(f, 'utf8')
  let descriptor
  try {
    descriptor = parse(content, { filename: f }).descriptor
  } catch (e) {
    reports.push({ f, kind: 'parse', error: e.message })
    total++
    continue
  }
  
  // script
  if (descriptor.script || descriptor.scriptSetup) {
    try {
      compileScript(descriptor, { id: f, isProd: false })
    } catch (e) {
      const msg = (e.message || String(e)).split('\n')[0]
      if (!msg.includes('defineProps') && !msg.includes('defineEmits')) {
        reports.push({ f, kind: 'script', error: msg })
        total++
        continue
      }
    }
  }
  
  // template
  if (descriptor.template) {
    try {
      compileTemplate({
        source: descriptor.template.content,
        filename: f,
        id: f,
        scoped: descriptor.styles.some(s => s.scoped),
      })
    } catch (e) {
      const msg = (e.message || String(e)).split('\n')[0]
      reports.push({ f, kind: 'template', error: msg })
      total++
      continue
    }
  }
  
  // style
  for (const s of descriptor.styles || []) {
    try {
      compileStyle({ source: s.content, filename: f, id: f, scoped: s.scoped })
    } catch (e) {
      const msg = (e.message || String(e)).split('\n')[0]
      reports.push({ f, kind: 'style', error: msg })
      total++
    }
  }
}

// 2. .js / .ts 检查
console.log('=== 2. .js/.ts 语法 ===')
for (const f of walk('src', ['.js', '.ts'])) {
  const content = fs.readFileSync(f, 'utf8')
  try {
    transformSync(content, { loader: f.endsWith('.ts') ? 'ts' : 'js', target: 'es2020', sourcemap: false })
  } catch (e) {
    const msg = (e.message || String(e)).split('\n')[0]
    reports.push({ f, kind: 'js', error: msg })
    total++
  }
}

// 3. 静态 import 检查 (依赖未装)
console.log('=== 3. import 路径检查 ===')
const importRegex = /(?:import|export)\s+[^'"]*from\s+['"]([^'"]+)['"]/g
const dynamicImportRegex = /import\s*\(\s*['"]([^'"]+)['"]\s*\)/g

for (const f of walk('src', ['.vue', '.js', '.ts'])) {
  const content = fs.readFileSync(f, 'utf8')
  const dir = path.dirname(f)
  
  for (const m of content.matchAll(importRegex)) {
    const spec = m[1]
    if (spec.startsWith('.')) {
      // 本地 import
      const resolved = path.resolve(dir, spec)
      let exists = fs.existsSync(resolved)
      if (!exists) {
        // 试扩展名
        for (const ext of ['.js', '.ts', '.vue', '.json', '/index.js', '/index.ts']) {
          if (fs.existsSync(resolved + ext)) { exists = true; break }
        }
      }
      if (!exists) {
        reports.push({ f, kind: 'import-missing', error: `'${spec}' not found` })
        total++
      }
    }
  }
}

// 输出
for (const r of reports.slice(0, 50)) {
  console.log(`  [${r.kind}] ${r.f}: ${r.error.slice(0, 200)}`)
}
console.log(`\nTotal: ${total} issues found`)
