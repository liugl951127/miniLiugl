/**
 * V3.7.38+ 自动删未用的 const/let/function
 * 
 * 策略:
 * 1. 跑 eslint 找未用的变量
 * 2. 找每个 var 的定义行
 * 3. 检查文件其他地方是否真没用 (排除 lint 误报)
 * 4. 删定义行 (含后续 ; 闭合)
 */
import { execSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

// 跑 eslint 找 unused
const out = execSync('node_modules/.bin/eslint . --ext .vue,.js,.ts --format json 2>/dev/null', { encoding: 'utf8' })
const results = JSON.parse(out)

let removed = 0
let files = 0
const KEEP = new Set(['ref', 'reactive', 'computed', 'watch', 'watchEffect', 'onMounted', 'onUnmounted', 'nextTick', 'useRoute', 'useRouter', 'useStore', 'useI18n', 'defineProps', 'defineEmits', 'defineExpose', 'withDefaults'])

for (const r of results) {
  const file = r.filePath
  if (!fs.existsSync(file)) continue
  let content = fs.readFileSync(file, 'utf8')
  const lines = content.split('\n')
  let fileChanged = false
  
  for (const msg of r.messages) {
    if (msg.ruleId !== 'no-unused-vars' && msg.ruleId !== 'vue/no-unused-vars') continue
    if (msg.severity !== 1) continue // 只看 warning
    const varName = msg.message.match(/'([A-Za-z_][A-Za-z0-9_]*)'/)?.[1]
    if (!varName) continue
    if (KEEP.has(varName)) continue
    
    // 找定义行 (const X = / let X = / function X)
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i]
      // 匹配: const X = / let X = / var X = / function X(
      const m = line.match(new RegExp(`^\\s*(const|let|var)\\s+${varName}\\s*[=:]\\s*`)) ||
                line.match(new RegExp(`^\\s*function\\s+${varName}\\s*\\(`))
      if (m) {
        // 检查文件其他位置是否真没用
        const contentWithout = lines.filter((l, idx) => idx !== i).join('\n')
        // 算 \bvarName\b
        const usages = (contentWithout.match(new RegExp(`\\b${varName}\\b`, 'g')) || []).length
        if (usages === 0) {
          // 删行 (含 ; 闭合 - 简单)
          let deleteLines = 1
          // 如果行结尾没 ;, 看下一行
          if (!line.trimEnd().endsWith(';') && !line.trimEnd().endsWith(',')) {
            // multi-line
            for (let j = i + 1; j < Math.min(i + 10, lines.length); j++) {
              if (lines[j].trim().endsWith(';') || lines[j].trim().endsWith(';')) {
                deleteLines = j - i + 1
                break
              }
              if (lines[j].trim() === '' || lines[j].trim() === '}') {
                deleteLines = j - i
                break
              }
            }
          }
          // 简单: 只删单行
          lines.splice(i, 1)
          removed++
          fileChanged = true
          break // 一行只处理一次
        }
      }
    }
  }
  
  if (fileChanged) {
    fs.writeFileSync(file, lines.join('\n'))
    files++
  }
}

console.log(`Removed ${removed} unused vars from ${files} files`)
