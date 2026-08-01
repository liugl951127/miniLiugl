#!/usr/bin/env node
/**
 * V3.6.12+ EmptyState 渐进迁移脚本
 * 安全: 先找 import 段结束位置, 在其后插入 import, 而非简单 append
 * 局限: 只处理 1 行 <el-empty /> / <el-empty description="..." />
 */
const fs = require('fs')
const path = require('path')

const TARGETS = process.argv.slice(2)
if (TARGETS.length === 0) {
  console.log('用法: node scripts/migrate-to-empty-state.cjs <file1.vue> [file2.vue] ...')
  process.exit(1)
}

let totalReplaced = 0
for (const file of TARGETS) {
  if (!fs.existsSync(file)) {
    console.log(`  ⚠ ${file} 不存在`)
    continue
  }
  
  let content = fs.readFileSync(file, 'utf-8')
  if (!content.includes('<el-empty')) {
    console.log(`  - ${file} 无 el-empty, 跳过`)
    continue
  }
  
  // 1. 找 <script setup> 段
  const scriptMatch = content.match(/<script setup>([\s\S]*?)<\/script>/)
  if (!scriptMatch) {
    console.log(`  ⚠ ${file} 无 <script setup>`)
    continue
  }
  
  // 2. 找最后一个 import 行 (在 script 段内)
  const script = scriptMatch[1]
  const importLines = script.split('\n').filter(l => l.trim().startsWith('import '))
  if (importLines.length === 0) {
    console.log(`  ⚠ ${file} 无 import 行`)
    continue
  }
  
  // 3. 找 import EmptyState 现有 - 不重复加
  if (script.includes('import EmptyState from')) {
    console.log(`  - ${file} 已引入 EmptyState`)
  } else {
    // 找最后一个 import 行位置 (在 script 段)
    let lastImportIdx = -1
    const scriptLines = script.split('\n')
    for (let i = 0; i < scriptLines.length; i++) {
      if (scriptLines[i].trim().startsWith('import ')) lastImportIdx = i
    }
    
    if (lastImportIdx >= 0) {
      // 在最后 import 行后插入
      const newScript = [
        ...scriptLines.slice(0, lastImportIdx + 1),
        "import EmptyState from '@/components/EmptyState.vue'",
        ...scriptLines.slice(lastImportIdx + 1)
      ].join('\n')
      
      content = content.replace(
        scriptMatch[0],
        `<script setup>${newScript}</script>`
      )
    }
  }
  
  // 4. 替换 <el-empty ... />
  let count = 0
  content = content.replace(
    /<el-empty\s+([^/]*?)\/>/g,
    (match, attrs) => {
      count++
      // 提取 description
      const descMatch = attrs.match(/:description="([^"]+)"/)
      const desc = descMatch ? descMatch[1] : "'暂无数据'"
      return `<EmptyState :description="${desc}" />`
    }
  )
  
  if (count > 0) {
    fs.writeFileSync(file, content)
    console.log(`  ✓ ${file} 替换 ${count} 个 el-empty`)
    totalReplaced += count
  } else {
    console.log(`  - ${file} 无可替换 el-empty`)
  }
}

console.log(`--- 总替换: ${totalReplaced} ---`)
