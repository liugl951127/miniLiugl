import { transformSync } from 'esbuild'
import fs from 'node:fs'
import path from 'node:path'

// 找 .vue 拆 script, esbuild transform
function* walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.name === '__tests__' || e.name === 'node_modules') continue
    const p = path.join(dir, e.name)
    if (e.isDirectory()) yield* walk(p)
    else if (e.name.endsWith('.vue')) yield p
    else if (e.name.endsWith('.ts') || e.name.endsWith('.js')) yield p
  }
}

let errors = 0
for (const f of walk('src')) {
  let content
  try {
    content = fs.readFileSync(f, 'utf8')
  } catch (e) {
    console.log(`  ! ${f}: ${e.message}`)
    continue
  }
  
  if (f.endsWith('.vue')) {
    const scriptMatch = content.match(/<script[^>]*>([\s\S]*?)<\/script>/)
    if (!scriptMatch) continue
    content = scriptMatch[1]
  }
  
  try {
    transformSync(content, { loader: 'ts', target: 'es2020' })
  } catch (e) {
    console.log(`  ✗ ${f}: ${e.message.split('\n')[0]}`)
    errors++
  }
}

console.log(`\nTotal parse errors: ${errors}`)
process.exit(errors > 0 ? 1 : 0)
