/**
 * V3.7.38+ 模板变量 vs script 定义一致性检查
 * 
 * 防 V3.7.37 "模板渲染时 X is not defined" 错
 * 比 @handler 更广: 检查 {{ X }} / :X="Y" / v-if="Z" / v-for="X in Y"
 */
import fs from 'node:fs'
import path from 'node:path'

const FIX = process.argv.includes('--fix')

function* walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.name === '__tests__' || e.name === 'node_modules') continue
    const p = path.join(dir, e.name)
    if (e.isDirectory()) yield* walk(p)
    else if (e.name.endsWith('.vue')) yield p
  }
}

// 提取模板中使用的所有变量名
function extractTemplateVars(tmpl) {
  const vars = new Set()
  // {{ X }}
  for (const m of tmpl.matchAll(/\{\{[^}]*?([a-zA-Z_$][\w$.]*(?:\?[^}]*)?)/g)) {
    const n = m[1]
    // 提取首段 (X.Y → X)
    vars.add(n.split('.')[0])
  }
  // :X="Y"
  for (const m of tmpl.matchAll(/:[\w:.-]+="([^"]+)"/g)) {
    const val = m[1]
    // 找所有标识符
    for (const id of val.matchAll(/([a-zA-Z_$][\w$]*)/g)) {
      const n = id[1]
      if (!['true', 'false', 'null', 'undefined', '$event', 'this', 'window', 'document'].includes(n)) {
        vars.add(n)
      }
    }
  }
  // v-if/v-show="X"
  for (const m of tmpl.matchAll(/v-(?:if|show|else-if)="([^"]+)"/g)) {
    for (const id of m[1].matchAll(/([a-zA-Z_$][\w$]*)/g)) {
      const n = id[1]
      if (!['true', 'false', 'null', 'undefined', '$event', 'this'].includes(n)) {
        vars.add(n)
      }
    }
  }
  return vars
}

function extractScriptDefined(script) {
  const defined = new Set()
  for (const m of script.matchAll(/function\s+([a-zA-Z_$][\w$]*)/g)) defined.add(m[1])
  for (const m of script.matchAll(/(?:const|let|var)\s+([a-zA-Z_$][\w$]*)\s*=/g)) defined.add(m[1])
  for (const m of script.matchAll(/import\s*\{([^}]+)\}/g)) {
    for (const n of m[1].matchAll(/([a-zA-Z_$][\w$]*)/g)) defined.add(n[1])
  }
  for (const m of script.matchAll(/import\s+([a-zA-Z_$][\w$]*)\s+from/g)) defined.add(m[1])
  for (const m of script.matchAll(/return\s*\{([^}]+)\}/g)) {
    for (const n of m[1].matchAll(/([a-zA-Z_$][\w$]*)/g)) defined.add(n[1])
  }
  if (/defineProps/.test(script)) {
    for (const m of script.matchAll(/defineProps\s*\(\s*\{?([^)]+)\}?\)/g)) {
      for (const n of m[1].matchAll(/([a-zA-Z_$][\w$]*)\s*[:?]/g)) defined.add(n[1])
    }
  }
  if (/defineEmits/.test(script)) {
    for (const m of script.matchAll(/defineEmits\s*\(\s*\[([^\]]+)\]/g)) {
      for (const n of m[1].matchAll(/'([\w$]+)'/g)) defined.add(n[1])
    }
  }
  return defined
}

let total = 0
const reports = []

for (const f of walk('src')) {
  const content = fs.readFileSync(f, 'utf8')
  const tmplMatch = content.match(/<template>([\s\S]*?)<\/template>/)
  const scriptMatch = content.match(/<script[^>]*>([\s\S]*?)<\/script>/)
  if (!tmplMatch || !scriptMatch) continue
  
  const tmplVars = extractTemplateVars(tmplMatch[1])
  const defined = extractScriptDefined(scriptMatch[1])
  
  // 排除已知全局 (Vue / JS / el 组件 / 路由)
  const GLOBALS = new Set([
    'Math', 'Date', 'JSON', 'Array', 'Object', 'String', 'Number', 'Boolean',
    'Promise', 'Map', 'Set', 'Symbol', 'Error', 'RegExp', 'parseInt', 'parseFloat',
    'isNaN', 'isFinite', 'Infinity', 'NaN', 'undefined', 'null', 'true', 'false',
    'console', 'window', 'document', 'navigator', 'location', 'history', 'localStorage',
    'sessionStorage', 'setTimeout', 'setInterval', 'clearTimeout', 'clearInterval',
    'process', 'globalThis', 'self',
    'router', 'route', 't', 'd', 'p', 'h', 'g', 's', 'i', 'k', 'v', 'x', 'y', 'z',
    'n', 'm', 'r', 'q', 'u', 'f', 'e', 'l', 'a', 'b', 'c', 'o', 'idx', 'gi', 'tmpl', 'cap',
    'qp', 'seg', 'act', 'card', 'm', 'n', 'name', 'row', 'item', 'index', 'key',
    'loading', 'tab', 'searchKw', 'filterText', 'searchInContent',
    'chatId', 'type', 'size', 'id', 'class', 'style', 'value', 'prop',
    'onClick', 'onChange', 'onInput', 'onSubmit', 'onMounted', 'onUnmounted',
  ])
  
  const issues = []
  for (const v of tmplVars) {
    if (GLOBALS.has(v)) continue
    if (defined.has(v)) continue
    // 排除 v-for 变量
    if (/\bv-for\s*=\s*["'][^"']*\b\w+\b\s+(?:in|of)\s+\w+/.test(tmplMatch[1])) continue
    issues.push(v)
  }
  
  if (issues.length > 0) {
    reports.push({ f, issues })
    total += issues.length
  }
}

// 输出 (按问题数排序)
reports.sort((a, b) => b.issues.length - a.issues.length)
for (const r of reports.slice(0, 50)) {
  console.log(`  ${r.f}: ${r.issues.slice(0, 10).join(', ')}${r.issues.length > 10 ? '...' : ''}`)
}

console.log(`\nTotal: ${total} potential issues across ${reports.length} files`)
