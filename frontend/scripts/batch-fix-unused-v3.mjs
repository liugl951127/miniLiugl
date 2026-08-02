/**
 * V3.7.38+ 安全批量删 unused imports - v3
 * 
 * 排除: dist/, scripts/, *.d.ts, auto-imports, components.d.ts, capacitor.config.ts
 * 限制: src/ 下的 .vue/.js/.ts
 */
import { execSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

function* walk(dir, exts) {
  if (!fs.existsSync(dir)) return
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.name === '__tests__' || e.name === 'node_modules' || e.name === 'dist' || e.name === 'scripts' || e.name.startsWith('.')) continue
    const p = path.join(dir, e.name)
    if (e.isDirectory()) yield* walk(p, exts)
    else if (exts.some(ext => p.endsWith(ext))) yield p
  }
}

// 收集所有 .vue/.js/.ts in src/
const files = []
for (const f of walk('src', ['.vue', '.js', '.ts'])) files.push(f)
console.log(`[1/4] 扫 ${files.length} 个文件 (src/ only)`)

let removed = 0
let changedFiles = 0

// 跑 eslint 单个文件 (避免一次性太大输出)
function lintFile(f) {
  try {
    return execSync(`node_modules/.bin/eslint "${f}" --format json 2>/dev/null`, { encoding: 'utf8' })
  } catch (e) {
    // eslint 返非0 也算成功
    return e.stdout?.toString() || '[]'
  }
}

for (const f of files) {
  const out = lintFile(f)
  let results
  try { results = JSON.parse(out) } catch { continue }
  
  const r = results[0]
  if (!r || !r.messages) continue
  
  // 找 unused imports
  const unusedNames = new Set()
  for (const msg of r.messages) {
    if (msg.severity !== 1) continue
    if (msg.ruleId !== 'no-unused-vars' && msg.ruleId !== 'vue/no-unused-vars') continue
    const varName = msg.message.match(/'([A-Za-z_][A-Za-z0-9_]*)'/)?.[1]
    if (varName) unusedNames.add(varName)
  }
  
  if (unusedNames.size === 0) continue
  
  // 改文件
  let content = fs.readFileSync(f, 'utf8')
  const original = content
  
  // 1. import { a, b, c } from '...'
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
        removed++
      } else {
        kept.push(item)
      }
    }
    if (removedThisImport === items.length) return ''
    if (removedThisImport > 0) return `import { ${kept.join(', ')} } from '${fromPath}'`
    return match
  })
  
  // 2. import X from '...'
  content = content.replace(/import\s+(\w+)\s+from\s+['"][^'"]+['"]\s*;?\n?/g, (match, name) => {
    if (unusedNames.has(name)) { removed++; return '' }
    return match
  })
  
  // 3. import * as X from '...'
  content = content.replace(/import\s+\*\s+as\s+(\w+)\s+from\s+['"][^'"]+['"]\s*;?\n?/g, (match, name) => {
    if (unusedNames.has(name)) { removed++; return '' }
    return match
  })
  
  if (content !== original) {
    fs.writeFileSync(f, content)
    changedFiles++
  }
}

console.log(`[2/4] 删了 ${removed} 个 unused imports, 改了 ${changedFiles} 个文件`)

// 验证
console.log('[3/4] 跑全 eslint 验证 (单线程, 限时 60s)...')
try {
  const out = execSync('timeout 60 node_modules/.bin/eslint src --ext .vue,.js,.ts 2>&1 | tail -5', { encoding: 'utf8' })
  console.log(out)
} catch (e) {
  console.log(e.stdout?.toString() || e.message)
}
