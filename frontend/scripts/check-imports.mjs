/**
 * V3.7.38+ 检查所有 .vue/.js 的 import 依赖是否在 package.json
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

// 找所有外部 import (非 ./ @/)
const imports = new Set()
for (const f of walk('src', ['.vue', '.js', '.ts'])) {
  const content = fs.readFileSync(f, 'utf8')
  for (const m of content.matchAll(/(?:import|export)\s+[^'"]*from\s+['"]([^'"]+)['"]/g)) {
    const spec = m[1]
    if (!spec.startsWith('.') && !spec.startsWith('@/') && !spec.startsWith('virtual:')) {
      // 提取包名
      const pkg = spec.startsWith('@') 
        ? spec.split('/').slice(0, 2).join('/')
        : spec.split('/')[0]
      imports.add(pkg)
    }
  }
}

// 读 package.json
const pkg = JSON.parse(fs.readFileSync('package.json', 'utf8'))
const declared = new Set([
  ...Object.keys(pkg.dependencies || {}),
  ...Object.keys(pkg.devDependencies || {}),
])

console.log('=== Imported packages ===')
const missing = []
for (const imp of [...imports].sort()) {
  if (declared.has(imp)) {
    // console.log(`  ✓ ${imp}`)
  } else {
    console.log(`  ✗ MISSING: ${imp}`)
    missing.push(imp)
  }
}
console.log(`\nTotal imported: ${imports.size}`)
console.log(`Total declared: ${declared.size}`)
console.log(`Missing: ${missing.length}`)
if (missing.length > 0) {
  console.log('Missing list:')
  for (const m of missing) console.log(`  - ${m}`)
}
