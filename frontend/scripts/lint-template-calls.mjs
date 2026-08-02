/**
 * V3.7.38+ 模板函数调用 vs script 定义检查 (精确版)
 * 
 * 防 "_ctx.X is not a function" 错
 * 抓: {{ X( ) }} / @X="Y()" / :X="Y()"
 */
import fs from 'node:fs'
import path from 'node:path'

function* walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.name === '__tests__' || e.name === 'node_modules') continue
    const p = path.join(dir, e.name)
    if (e.isDirectory()) yield* walk(p)
    else if (e.name.endsWith('.vue')) yield p
  }
}

// String/Number/Array/Object/Map 原型方法
const PROTOTYPE_METHODS = new Set([
  'substring', 'substr', 'slice', 'split', 'replace', 'trim', 'toLowerCase',
  'toUpperCase', 'charAt', 'charCodeAt', 'indexOf', 'lastIndexOf', 'includes',
  'startsWith', 'endsWith', 'concat', 'repeat', 'padStart', 'padEnd',
  'toFixed', 'toPrecision', 'toExponential', 'toString', 'valueOf',
  'toLocaleString', 'toJSON',
  'push', 'pop', 'shift', 'unshift', 'splice', 'sort', 'reverse', 'join',
  'map', 'filter', 'reduce', 'reduceRight', 'forEach', 'every', 'some',
  'find', 'findIndex', 'flat', 'flatMap', 'fill', 'copyWithin',
  'has', 'get', 'set', 'delete', 'clear', 'add', 'size', 'click', 'focus', 'blur',
  'keys', 'values', 'entries', 'assign', 'freeze', 'fromEntries',
  'stringify', 'parse',
  'round', 'floor', 'ceil', 'abs', 'max', 'min', 'pow', 'sqrt', 'log',
  'sin', 'cos', 'tan', 'random', 'trunc',
  'now', 'getTime', 'getDate', 'getDay', 'getFullYear', 'getMonth', 'getYear',
  'getHours', 'getMinutes', 'getSeconds', 'getMilliseconds', 'getTimezoneOffset',
  'setDate', 'setFullYear', 'setMonth', 'setHours', 'setMinutes', 'setSeconds',
  'toISOString', 'toDateString', 'toTimeString',
  'substr', 'match', 'search', 'replace',
])

// 全局函数
const GLOBALS = new Set([
  'parseInt', 'parseFloat', 'isNaN', 'isFinite', 'Number', 'String',
  'JSON', 'Date', 'Math', 'Array', 'Object', 'Boolean', 'Promise',
  'Map', 'Set', 'Symbol', 'Error', 'RegExp', 'encodeURI', 'decodeURI',
  'encodeURIComponent', 'decodeURIComponent',
  't', '$emit', 'formatTime', 'truncate', 'formatDate', 'formatSize', 'formatTimestamp',
  'toast', 'router', 'route', 'var', 'rgba',
])

// defineEmits 里的事件名 (每个 emit 都是合法的)
const EMIT_PREFIX = 'emit'

function extractCallSites(tmpl) {
  const calls = new Set()
  for (const m of tmpl.matchAll(/\{\{[^}]*\b([a-zA-Z_$][\w$]*)\s*\(/g)) calls.add(m[1])
  for (const m of tmpl.matchAll(/@[\w:.-]+="[^"]*\b([a-zA-Z_$][\w$]*)\s*\(/g)) calls.add(m[1])
  for (const m of tmpl.matchAll(/:[\w:.-]+="[^"]*\b([a-zA-Z_$][\w$]*)\s*\(/g)) calls.add(m[1])
  return calls
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
  
  const calls = extractCallSites(tmplMatch[1])
  const defined = extractScriptDefined(scriptMatch[1])
  
  const issues = []
  for (const c of calls) {
    if (PROTOTYPE_METHODS.has(c)) continue
    if (GLOBALS.has(c)) continue
    if (defined.has(c)) continue
    issues.push(c)
  }
  
  if (issues.length > 0) {
    reports.push({ f, issues })
    total += issues.length
  }
}

reports.sort((a, b) => b.issues.length - a.issues.length)
for (const r of reports.slice(0, 30)) {
  console.log(`  ${r.f}: ${r.issues.slice(0, 8).join(', ')}${r.issues.length > 8 ? '...' : ''}`)
}

console.log(`\nTotal: ${total} real call mismatches across ${reports.length} files`)
process.exit(total > 0 ? 1 : 0)
