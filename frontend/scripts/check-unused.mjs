/**
 * V3.7.38+ 检测未使用的 export / import
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

let total = 0
const reports = []

// 1. 找 .vue 死代码
for (const f of walk('src', ['.vue'])) {
  const content = fs.readFileSync(f, 'utf8')
  const scriptMatch = content.match(/<script[^>]*>([\s\S]*?)<\/script>/)
  if (!scriptMatch) continue
  const script = scriptMatch[1]
  const tmplMatch = content.match(/<template>([\s\S]*?)<\/template>/)
  const tmpl = tmplMatch ? tmplMatch[1] : ''
  const styleMatch = content.match(/<style[^>]*>([\s\S]*?)<\/style>/)
  const style = styleMatch ? styleMatch[1] : ''
  
  // 找 const/let/var/function 声明
  const decls = []
  for (const m of script.matchAll(/(?:const|let|var)\s+([a-zA-Z_$][\w$]*)\s*=/g)) {
    decls.push(m[1])
  }
  for (const m of script.matchAll(/function\s+([a-zA-Z_$][\w$]*)/g)) {
    decls.push(m[1])
  }
  
  // 检查每个 decl 在 script/template/style 中是否被用
  const fullContent = script + tmpl + style
  for (const d of decls) {
    // 去掉 import / declaration, 找其他位置
    const stripped = fullContent.replace(new RegExp(`\\b${d}\\s*=\\s*`, 'g'), '')
    const usages = (stripped.match(new RegExp(`\\b${d}\\b`, 'g')) || []).length
    if (usages === 0) {
      reports.push({ f, kind: 'unused', name: d })
      total++
    }
  }
}

console.log(`\n=== 死代码 (unreferenced) ===`)
for (const r of reports.slice(0, 50)) {
  console.log(`  ${r.f}: ${r.name}`)
}
console.log(`\nTotal: ${total} unreferenced variables`)
