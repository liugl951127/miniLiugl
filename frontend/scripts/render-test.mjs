/**
 * 真实 server-side Vue render, 抓 _ctx.X is not a function
 * 
 * 思路:
 * 1. 用 @vue/compiler-sfc 编译 .vue
 * 2. 用 esbuild 解析 import (vite alias, vue, element-plus)
 * 3. 用 Vue runtime + jsdom 模拟
 */
import { parse, compileScript, compileTemplate } from 'vue/compiler-sfc'
import { createSSRApp, h, defineComponent, ref, computed, watch, onMounted } from 'vue'
import * as vue from 'vue'
import * as elementPlus from 'element-plus'
import { createPinia } from 'pinia'
import * as pinia from 'pinia'
import * as vueI18n from 'vue-i18n'
import fs from 'node:fs'
import path from 'node:path'

const ROOT = path.resolve('src')

// 自定义 resolver: 解析 @ 别名 + node_modules
async function resolveImport(spec, importer) {
  if (spec.startsWith('@/')) {
    return path.join(ROOT, spec.slice(2))
  }
  if (spec.startsWith('.')) {
    return path.resolve(path.dirname(importer), spec)
  }
  // node_modules
  return path.resolve('node_modules', spec)
}

// 加载模块 (mock 第三方)
async function loadModule(spec, importer) {
  const resolved = await resolveImport(spec, importer)
  
  // 第三方
  if (resolved.includes('node_modules')) {
    // vue / element-plus / pinia / vue-i18n
    if (spec.startsWith('vue')) return vue
    if (spec.startsWith('element-plus')) return elementPlus
    if (spec.startsWith('pinia')) return pinia
    if (spec.startsWith('vue-i18n')) return vueI18n
    return {}
  }
  
  // 本地文件
  if (!fs.existsSync(resolved)) {
    // 试 .vue / .ts / .js
    for (const ext of ['.vue', '.ts', '.js', '/index.ts', '/index.js']) {
      const p = resolved + ext
      if (fs.existsSync(p)) return await loadFile(p, importer)
    }
    return {}
  }
  
  return await loadFile(resolved, importer)
}

const cache = new Map()
async function loadFile(f, importer) {
  if (cache.has(f)) return cache.get(f)
  if (importer) cache.set(f, null)
  
  let content = fs.readFileSync(f, 'utf8')
  let mod
  
  if (f.endsWith('.vue')) {
    const { descriptor } = parse(content, { filename: f })
    if (descriptor.scriptSetup || descriptor.script) {
      // 编译 script
      const result = compileScript(descriptor, { id: f })
      content = result.content
    } else {
      content = 'export default {}'
    }
  } else if (f.endsWith('.ts')) {
    // 简化: 跳过 TS type, 直接转 JS
    content = content.replace(/^import\s+type.*$/gm, '')
    content = content.replace(/^export\s+type.*$/gm, '')
  }
  
  // 解析 import
  const imports = []
  content = content.replace(/^import\s+(?:\*\s+as\s+(\w+)|\{([^}]+)\}|(\w+))\s+from\s+['"]([^'"]+)['"];?$/gm, (m, ns, names, def, src) => {
    if (ns) imports.push({ type: 'ns', name: ns, src })
    if (names) {
      for (const n of names.matchAll(/(\w+)(?:\s+as\s+(\w+))?/g)) {
        imports.push({ type: 'named', name: n[1], as: n[2] || n[1], src })
      }
    }
    if (def) imports.push({ type: 'default', name: def, src })
    return ''
  })
  
  // 解析 import 'side-effect'
  content = content.replace(/^import\s+['"]([^'"]+)['"];?$/gm, '')
  
  // 解析 export
  let exportDefault = null
  content = content.replace(/export\s+default\s+([\s\S]+?)(?=\n\nexport|\n$|$)/m, (m, expr) => {
    exportDefault = expr.trim()
    return ''
  })
  
  // 异步加载所有 import
  const exports = {}
  for (const imp of imports) {
    const dep = await loadModule(imp.src, f)
    if (imp.type === 'ns') {
      exports[imp.name] = dep
    } else if (imp.type === 'named') {
      exports[imp.as] = dep[imp.name]
    } else if (imp.type === 'default') {
      exports[imp.name] = dep.default || dep
    }
  }
  
  // 跑剩余代码
  const argNames = Object.keys(exports)
  const argVals = Object.values(exports)
  
  try {
    const fn = new Function(...argNames, content)
    const result = fn(...argVals)
    if (exportDefault) {
      mod = result[exportDefault.trim()] || result
    } else {
      mod = result
    }
  } catch (e) {
    mod = { __error: e.message }
  }
  
  cache.set(f, mod)
  return mod
}

function* walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.name === '__tests__' || e.name === 'node_modules') continue
    const p = path.join(dir, e.name)
    if (e.isDirectory()) yield* walk(p)
    else if (e.name.endsWith('.vue')) yield p
  }
}

let total = 0
const reports = []

// 跑每个 .vue
for (const f of walk('src')) {
  cache.clear()  // 重置避免循环
  try {
    const mod = await loadFile(f, null)
    if (mod && mod.__error) {
      reports.push({ f, error: mod.__error })
    }
  } catch (e) {
    reports.push({ f, error: e.message })
  }
  total++
}

for (const r of reports.slice(0, 30)) {
  console.log(`  ${r.f}: ${r.error}`)
}
console.log(`\nTotal: ${reports.length}/${total} files have errors`)
