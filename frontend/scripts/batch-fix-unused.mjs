/**
 * V3.7.38+ 批量删未用的 import + var
 * 
 * 跳过:
 * - vite.config / scripts/ 工具
 * - 测试文件
 * - 真实业务文件保留 (手审)
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

// 已知可删的 import (unused in 80% files)
const SAFE_REMOVE = new Set([
  'ElMessage', 'ElMessageBox', 'ElNotification',  // 用了 toast 替代
])

// 已知不能删的 (动态用了, 或在 template 用了)
const KEEP = new Set([
  'ref', 'reactive', 'computed', 'watch', 'watchEffect', 'onMounted', 'onUnmounted', 'nextTick',
  'useRoute', 'useRouter', 'useStore', 'useI18n',
  'defineProps', 'defineEmits', 'defineExpose', 'withDefaults',
  'defineComponent', 'createApp', 'h', 'inject', 'provide',
])

let removed = 0
let files = 0

for (const f of walk('src', ['.vue', '.js'])) {
  if (f.endsWith('.test.js')) continue
  let content = fs.readFileSync(f, 'utf8')
  let changed = false
  
  // 1. 找 import 块
  // 找 import { x, y, z } from 'path'
  const importRegex = /import\s+\{([^}]+)\}\s+from\s+['"][^'"]+['"]/g
  
  content = content.replace(importRegex, (match, names) => {
    // 解析 names
    const items = names.split(',').map(n => n.trim()).filter(Boolean)
    const removedItems = []
    const keptItems = []
    
    for (const item of items) {
      // 提取 as 别名
      const m = item.match(/^(\w+)(?:\s+as\s+(\w+))?$/)
      if (!m) { keptItems.push(item); continue }
      const origName = m[1]
      const asName = m[2] || m[1]
      
      // 跳过 KEEP
      if (KEEP.has(origName)) { keptItems.push(item); continue }
      
      // 跳过 SAFE_REMOVE 之外 (非白名单的保留 - 风险高)
      if (!SAFE_REMOVE.has(origName)) { keptItems.push(item); continue }
      
      // 检查是否在文件其他地方用 (排除 import 自己)
      const contentWithoutImport = content.replace(match, '')
      const usageCount = (contentWithoutImport.match(new RegExp(`\\b${asName}\\b`, 'g')) || []).length
      
      if (usageCount === 0) {
        removedItems.push(item)
        removed++
        changed = true
      } else {
        keptItems.push(item)
      }
    }
    
    if (removedItems.length === items.length) {
      // 整行删
      return ''
    } else if (removedItems.length > 0) {
      return `import { ${keptItems.join(', ')} } from ${match.match(/from\s+(['"][^'"]+['"])/)[1]}`
    }
    return match
  })
  
  // 2. 单独的 import X from '...'
  const defaultRegex = /import\s+(\w+)\s+from\s+['"][^'"]+['"];?\n/g
  content = content.replace(defaultRegex, (match, name) => {
    if (KEEP.has(name)) return match
    // 检查是否使用
    const contentWithoutImport = content.replace(match, '')
    const usageCount = (contentWithoutImport.match(new RegExp(`\\b${name}\\b`, 'g')) || []).length
    if (usageCount === 0) {
      removed++
      changed = true
      return ''
    }
    return match
  })
  
  if (changed) {
    fs.writeFileSync(f, content)
    files++
  }
}

console.log(`Removed ${removed} unused imports from ${files} files`)
