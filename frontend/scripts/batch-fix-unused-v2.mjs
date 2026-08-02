/**
 * V3.7.38+ 安全批量删 unused imports
 * 
 * 策略: 用 ESLint JSON 报告, 只删 import 块 (风险低)
 * 跳过: const/let/function (易误删)
 */
import { execSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

// 跑 eslint
console.log('[1/4] eslint 扫描...')
const out = execSync('node_modules/.bin/eslint . --ext .vue,.js,.ts --format json 2>/dev/null', { encoding: 'utf8' })
const results = JSON.parse(out)

// 收集 unused imports
const unusedImportsByFile = new Map()
for (const r of results) {
  for (const msg of r.messages) {
    if (msg.ruleId !== 'no-unused-vars' && msg.ruleId !== 'vue/no-unused-vars') continue
    if (msg.severity !== 1) continue
    const varName = msg.message.match(/'([A-Za-z_][A-Za-z0-9_]*)'/)?.[1]
    if (!varName) continue
    if (!unusedImportsByFile.has(r.filePath)) unusedImportsByFile.set(r.filePath, new Set())
    unusedImportsByFile.get(r.filePath).add(varName)
  }
}

console.log(`[2/4] 找到 ${unusedImportsByFile.size} 个文件有 unused imports`)

let removed = 0
let files = 0

// 遍历文件, 删 import 中的 unused
for (const [file, names] of unusedImportsByFile) {
  if (!fs.existsSync(file)) continue
  let content = fs.readFileSync(file, 'utf8')
  const original = content
  
  // 1. import { a, b, c } from '...'  - 删 a/b/c
  const importRegex = /import\s+\{([^}]+)\}\s+from\s+['"][^'"]+['"]\s*;?/g
  content = content.replace(importRegex, (match, namesStr) => {
    const items = namesStr.split(',').map(n => n.trim()).filter(Boolean)
    const kept = []
    let removedThisImport = 0
    for (const item of items) {
      const m = item.match(/^(\w+)(?:\s+as\s+(\w+))?$/)
      if (!m) { kept.push(item); continue }
      const orig = m[1]
      const as = m[2] || m[1]
      if (names.has(as) || names.has(orig)) {
        removedThisImport++
        removed++
      } else {
        kept.push(item)
      }
    }
    if (removedThisImport === items.length) {
      // 整行删
      return ''
    } else if (removedThisImport > 0) {
      return `import { ${kept.join(', ')} } from ${match.match(/from\s+(['"][^'"]+['"])/)[1]}`
    }
    return match
  })
  
  // 2. import X from '...' (default) - 删整个
  const defaultRegex = /import\s+(\w+)\s+from\s+['"][^'"]+['"]\s*;?\n?/g
  content = content.replace(defaultRegex, (match, name) => {
    if (names.has(name)) {
      removed++
      return ''
    }
    return match
  })
  
  // 3. import * as X from '...'
  const namespaceRegex = /import\s+\*\s+as\s+(\w+)\s+from\s+['"][^'"]+['"]\s*;?\n?/g
  content = content.replace(namespaceRegex, (match, name) => {
    if (names.has(name)) {
      removed++
      return ''
    }
    return match
  })
  
  if (content !== original) {
    fs.writeFileSync(file, content)
    files++
  }
}

console.log(`[3/4] 删了 ${removed} 个 unused imports, 改了 ${files} 个文件`)

// 验证
console.log('[4/4] 验证...')
const out2 = execSync('node_modules/.bin/eslint . --ext .vue,.js,.ts 2>/dev/null', { encoding: 'utf8' })
const errors = (out2.match(/error/g) || []).length
const warnings = (out2.match(/warning/g) || []).length
console.log(`  errors: ${errors}`)
console.log(`  warnings: ${warnings}`)
