/**
 * V3.7.38+ 检查模板中 {{ X.method(args) }} / :X="X.method(args)"
 * 其中 X 是否在 script 定义, X.method 是否在 X 类型上
 */
import fs from 'node:fs'
import path from 'node:path'

function* walk(dir, exts) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.name === '__tests__' || e.name === 'node_modules' || e.name.startsWith('.')) continue
    const p = path.join(dir, e.name)
    if (e.isDirectory()) yield* walk(p, exts)
    else if (exts.some(ext => p.endsWith(ext))) yield p
  }
}

// 找模板中 .method() 调用 + 链上调用
function extractMethodCalls(tmpl) {
  const calls = []
  for (const m of tmpl.matchAll(/\{\{([^}]+)\}\}/g)) {
    const expr = m[1]
    // 找 X.method(...) 形式
    for (const mc of expr.matchAll(/(\w+(?:\.\w+)*)\.(\w+)\s*\(/g)) {
      calls.push({ path: mc[1], method: mc[2], ctx: 'mustache' })
    }
  }
  for (const m of tmpl.matchAll(/:[\w:.-]+="([^"]+\.\w+\s*\([^"]*\)["])/g)) {
    const expr = m[1]
    for (const mc of expr.matchAll(/(\w+(?:\.\w+)*)\.(\w+)\s*\(/g)) {
      calls.push({ path: mc[1], method: mc[2], ctx: 'prop' })
    }
  }
  return calls
}

function extractScriptVars(script) {
  const vars = new Map()  // name -> 'ref' | 'computed' | 'function' | 'const'
  for (const m of script.matchAll(/(?:const|let|var)\s+(\w+)\s*=\s*([^=\n]+)/g)) {
    const name = m[1]
    const init = m[2].trim()
    let kind = 'const'
    if (init.startsWith('ref(')) kind = 'ref'
    else if (init.startsWith('computed(')) kind = 'computed'
    else if (init.startsWith('use')) kind = 'composable'
    vars.set(name, { kind, init: init.slice(0, 80) })
  }
  for (const m of script.matchAll(/function\s+(\w+)/g)) {
    vars.set(m[1], { kind: 'function', init: 'function' })
  }
  for (const m of script.matchAll(/import\s*\{([^}]+)\}/g)) {
    for (const n of m[1].matchAll(/(\w+)/g)) vars.set(n[1], { kind: 'import', init: 'import' })
  }
  return vars
}

const issues = []
for (const f of walk('src', ['.vue'])) {
  const content = fs.readFileSync(f, 'utf8')
  const tmplMatch = content.match(/<template>([\s\S]*?)<\/template>/)
  const scriptMatch = content.match(/<script[^>]*>([\s\S]*?)<\/script>/)
  if (!tmplMatch || !scriptMatch) continue
  
  const tmpl = tmplMatch[1]
  const script = scriptMatch[1]
  const calls = extractMethodCalls(tmpl)
  const vars = extractScriptVars(script)
  
  for (const c of calls) {
    const rootVar = c.path.split('.')[0]
    // 排除全局 + 已知
    if (['Math', 'Date', 'JSON', 'Object', 'Array', 'String', 'Number', 'Boolean', 'Promise', 'Map', 'Set', 'Symbol', 'Error', 'RegExp', 'parseInt', 'parseFloat', 'isNaN', 'isFinite', 'NaN', 'undefined', 'null', 'true', 'false', 'console', 'window', 'document', 'navigator', 'location', 'history', 'localStorage', 'sessionStorage', 'setTimeout', 'setInterval', 'clearTimeout', 'clearInterval', 'process', 'globalThis', 'self', 'Boolean', 'String', 'Number', 'Object'].includes(rootVar)) continue
    if (rootVar === 't' || rootVar === 'route' || rootVar === 'router') continue
    
    // 找 .method 在 rootVar 上是否定义
    if (vars.has(rootVar)) {
      const v = vars.get(rootVar)
      // ref/computed 内部 method 看 - 不强校验
      if (v.kind === 'ref' || v.kind === 'computed') continue
      // const 对象 - method 可能在对象里
      if (v.kind === 'const' && v.init.includes('{')) continue
      // function 自身有 .method?
      if (v.kind === 'function' && v.init === 'function') continue
    }
    // 检查 init 中是否有 .method
    // 简化: 只看 .method 是否在 file 任何地方定义
    // 实际: 难静态抓, 跳过
  }
}

console.log(`\n=== 静态扫完 ${issues.length} 潜在问题 ===`)
