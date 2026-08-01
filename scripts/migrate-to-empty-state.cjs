#!/usr/bin/env node
// V3.6.12+ EmptyState 渐进迁移
const fs = require('fs')
const TARGETS = process.argv.slice(2)
if (TARGETS.length === 0) { console.log('用法: node migrate-to-empty-state.cjs <file>'); process.exit(1) }
let total = 0
for (const file of TARGETS) {
  if (!fs.existsSync(file)) { console.log(`  - ${file} 不存在`); continue }
  let content = fs.readFileSync(file, 'utf-8')
  if (!content.includes('<el-empty')) { console.log(`  - ${file} 无 el-empty`); continue }
  const m = content.match(/<script setup>([\s\S]*?)<\/script>/)
  if (!m) continue
  if (!m[1].includes('import EmptyState from')) {
    const lines = m[1].split('\n')
    let last = -1
    for (let i = 0; i < lines.length; i++) if (lines[i].trim().startsWith('import ')) last = i
    if (last >= 0) {
      lines.splice(last + 1, 0, "import EmptyState from '@/components/EmptyState.vue'")
      content = content.replace(m[0], '<script setup>' + lines.join('\n') + '</script>')
    }
  }
  let count = 0
  content = content.replace(/<el-empty\s+([^/]*?)\/>/g, (m, attrs) => {
    count++
    const dm = attrs.match(/:description="([^"]+)"/)
    const desc = dm ? dm[1] : "'暂无数据'"
    return `<EmptyState :description="${desc}" />`
  })
  if (count > 0) { fs.writeFileSync(file, content); console.log(`  ✓ ${file} 替换 ${count}`); total += count }
}
console.log(`--- 总替换 ${total} ---`)
