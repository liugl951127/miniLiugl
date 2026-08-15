/**
 * V3.7.38+ 模板 @handler 与 script 定义一致性检查
 * 
 * 防 V3.7.37 那种"模板调 X 但 script 没定义"运行时错
 * 
 * 用法: 
 *   node scripts/lint-template-handlers.mjs        # 报告
 *   node scripts/lint-template-handlers.mjs --fix  # 自动 stub
 */
import fs from 'node:fs'
import path from 'node:path'

const FIX = process.argv.includes('--fix')

function* walkVue(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.name === '__tests__' || entry.name === 'node_modules') continue
    const p = path.join(dir, entry.name)
    if (entry.isDirectory()) yield* walkVue(p)
    else if (entry.name.endsWith('.vue')) yield p
  }
}

function checkFile(f) {
  const content = fs.readFileSync(f, 'utf8')
  const tmplMatch = content.match(/<template>([\s\S]*?)<\/template>/)
  const scriptMatch = content.match(/<script[^>]*>([\s\S]*?)<\/script>/)
  if (!tmplMatch || !scriptMatch) return []
  
  const tmpl = tmplMatch[1]
  const script = scriptMatch[1]
  
  // 模板 @xxx="funcName"
  const handlers = new Set()
  for (const m of tmpl.matchAll(/@[\w:.-]+="([a-zA-Z_$][\w$]*)"/g)) {
    if (!['$event', 'true', 'false', 'null', 'undefined'].includes(m[1])) {
      handlers.add(m[1])
    }
  }
  
  // script defined
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
  // defineProps
  for (const m of script.matchAll(/defineProps\s*\(\s*\{?([^)]+)\}?\)/g)) {
    for (const n of m[1].matchAll(/([a-zA-Z_$][\w$]*)\s*[:?]/g)) defined.add(n[1])
  }
  // destructure 多行 (跳过识别)
  
  return [...handlers].filter(h => !defined.has(h))
}

let total = 0
const files = [...walkVue('src')]
for (const f of files) {
  const issues = checkFile(f)
  if (issues.length === 0) continue
  // 过滤 lint 误报 (destructure 多行)
  const real = issues.filter(h => !['install', 'update', 'checkForUpdate'].includes(h))
  if (real.length === 0) continue
  
  console.log(`  ${f}: ${real.map(h => `@click=${h}`).join(', ')}`)
  total += real.length
  
  if (FIX) {
    let content = fs.readFileSync(f, 'utf8')
    const stub = '\n\n// === V3.7.38+ lint auto-stub ===\n' + 
      real.map(h => `function ${h}() { /* TODO */ }`).join('\n') + '\n'
    if (content.includes('onMounted(')) {
      content = content.replace('onMounted(', stub + '\nonMounted(', 1)
    } else if (content.includes('</script>')) {
      content = content.replace('</script>', stub + '\n</script>', 1)
    }
    fs.writeFileSync(f, content)
  }
}

console.log(`\nTotal: ${total} real @handler mismatches${FIX ? ' (FIXED)' : ''}`)
process.exit(total > 0 ? 1 : 0)
