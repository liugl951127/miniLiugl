/**
 * V3.7.38+ 快速批量删 unused imports
 * 
 * 跳过 ESLint, 用 ast 解析 (vue-eslint-parser 太慢)
 * 改用简单 regex: 对 import 块逐个文件分析
 */
import fs from 'node:fs'
import path from 'node:path'
import { execSync } from 'node:child_process'

// 读 unused files
const files = fs.readFileSync('/tmp/unused-files.txt', 'utf8').trim().split('\n').map(f => f.trim()).filter(Boolean)
console.log(`[1/3] 目标 ${files.length} 个文件`)

// 跑 eslint 单文件, 找 unused
let totalRemoved = 0
let totalChanged = 0

for (const f of files) {
  if (!fs.existsSync(f)) continue
  let content
  try { content = fs.readFileSync(f, 'utf8') } catch { continue }
  const original = content
  
  // 跑 eslint 单文件
  let out
  try {
    out = execSync(`node_modules/.bin/eslint "${f}" --format json 2>/dev/null`, { encoding: 'utf8' })
  } catch (e) { out = e.stdout?.toString() || '[]' }
  
  let results
  try { results = JSON.parse(out) } catch { continue }
  const r = results[0]
  if (!r || !r.messages) continue
  
  const unusedNames = new Set()
  for (const msg of r.messages) {
    if (msg.severity !== 1) continue
    if (msg.ruleId !== 'no-unused-vars' && msg.ruleId !== 'vue/no-unused-vars') continue
    const varName = msg.message.match(/'([A-Za-z_][A-Za-z0-9_]*)'/)?.[1]
    if (varName) unusedNames.add(varName)
  }
  
  if (unusedNames.size === 0) continue
  
  // 改 import 块
  let removedThis = 0
  content = content.replace(/import\s+\{([^}]+)\}\s+from\s+['"]([^'"]+)['"]\s*;?/g, (match, namesStr, fromPath) => {
    const items = namesStr.split(',').map(n => n.trim()).filter(Boolean)
    const kept = []
    let removedThisImport = 0
    for (const item of items) {
      const m = item.match(/^(\w+)(?:\s+as\s+(\w+))?$/)
      if (!m) { kept.push(item); continue }
      const orig = m[1]
      const as = m[2] || m[1]
      if (unusedNames.has(as) || unusedNames.has(orig)) {
        removedThisImport++
      } else {
        kept.push(item)
      }
    }
    if (removedThisImport === items.length) { removedThis += removedThisImport; return '' }
    if (removedThisImport > 0) { removedThis += removedThisImport; return `import { ${kept.join(', ')} } from '${fromPath}'` }
    return match
  })
  
  content = content.replace(/import\s+(\w+)\s+from\s+['"][^'"]+['"]\s*;?\n?/g, (match, name) => {
    if (unusedNames.has(name)) { removedThis++; return '' }
    return match
  })
  
  content = content.replace(/import\s+\*\s+as\s+(\w+)\s+from\s+['"][^'"]+['"]\s*;?\n?/g, (match, name) => {
    if (unusedNames.has(name)) { removedThis++; return '' }
    return match
  })
  
  if (content !== original) {
    fs.writeFileSync(f, content)
    totalRemoved += removedThis
    totalChanged++
  }
}

console.log(`[2/3] 删了 ${totalRemoved} 个 unused imports, 改了 ${totalChanged} 个文件`)
console.log('[3/3] 完成')
