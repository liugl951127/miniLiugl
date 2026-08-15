#!/usr/bin/env node
/**
 * V3.6.21+ 安全的 import 修复
 * 改用 set 去重, 避免重复
 */
const fs = require('fs')
const path = require('path')

const VUE_APIS = [
  'ref', 'reactive', 'computed', 'watch', 'watchEffect',
  'onMounted', 'onUnmounted', 'onUpdated', 'onBeforeMount', 'onBeforeUnmount',
  'onBeforeUpdate', 'onActivated', 'onDeactivated', 'onErrorCaptured',
  'nextTick', 'defineProps', 'defineEmits', 'defineExpose', 'useSlots', 'useAttrs',
  'inject', 'provide', 'getCurrentInstance', 'shallowRef', 'shallowReactive',
  'triggerRef', 'customRef', 'readonly', 'toRaw', 'markRaw', 'unref', 'toRef', 'toRefs',
]

let totalFixed = 0
function walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name)
    if (e.isDirectory()) walk(p)
    else if (/\.vue$/.test(e.name) || /\.js$/.test(e.name)) check(p)
  }
}

function check(file) {
  const content = fs.readFileSync(file, 'utf-8')
  const m = content.match(/import\s*\{([^}]+)\}\s*from\s*['"]vue['"]/)
  if (!m) return
  
  // 用 Set 去重 + 保留原有顺序
  const seen = new Set()
  const parts = m[1].split(',').map(s => s.trim()).filter(Boolean).filter(api => {
    if (seen.has(api)) return false
    seen.add(api)
    return true
  })
  
  const missing = []
  for (const api of VUE_APIS) {
    if (parts.includes(api)) continue
    const re = new RegExp(`\\b${api}\\b`)
    if (re.test(content)) missing.push(api)
  }
  
  if (missing.length === 0) return
  
  const newImport = [...parts, ...missing].join(', ')
  const newContent = content.replace(m[0], `import { ${newImport} } from 'vue'`)
  fs.writeFileSync(file, newContent)
  console.log(`  ✓ ${file}: +${missing.join(', ')}`)
  totalFixed++
}

walk('src')
console.log(`--- 修 ${totalFixed} 文件 ---`)
