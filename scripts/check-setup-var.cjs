#!/usr/bin/env node
/**
 * V3.5.95 防御性自检: 扫所有 .vue 找模板用变量但 setup 未定义
 */
const fs = require('fs')
const path = require('path')

const viewsDir = 'frontend/src/views'
const BUILTIN = new Set([
  'Math','JSON','Date','Object','Array','String','Number','Boolean','Promise',
  'console','window','document','localStorage','sessionStorage','navigator',
  'location','history','fetch','setTimeout','setInterval','clearTimeout','clearInterval',
  'RegExp','Map','Set','Symbol','Error','TypeError','RangeError',
  'el','Element','HTMLElement','Node','Event','MouseEvent','KeyboardEvent','PointerEvent',
])
const EL_PREFIX = ['ElMessage','ElButton','ElInput','ElSelect','ElOption','ElForm','ElFormItem','ElTable','ElTableColumn','ElTag','ElCard','ElRow','ElCol','ElDialog','ElDrawer','ElEmpty','ElCollapse','ElCollapseItem','ElCheckbox','ElIcon','ElTooltip','ElPopover','ElMenu','ElMenuItem','ElSubMenu','ElAside','ElHeader','ElMain','ElContainer','ElPagination','ElSwitch','ElSlider','ElRadio','ElRadioGroup','ElCheckboxGroup','ElOptionGroup','ElCascader','ElTree','ElTabs','ElTabPane','ElUpload','ElDropdown','ElDropdownMenu','ElDropdownItem']

function isElComponent(name) {
  if (EL_PREFIX.includes(name)) return true
  if (name.startsWith('El') && name.length > 2 && name[2] === name[2].toUpperCase()) return true
  return false
}

function findVueFiles(dir, files = []) {
  if (!fs.existsSync(dir)) return files
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name)
    if (e.isDirectory()) findVueFiles(p, files)
    else if (e.name.endsWith('.vue') && !e.name.includes('.bak')) files.push(p)
  }
  return files
}

const files = findVueFiles(viewsDir)
let errors = 0
const log = []

for (const f of files) {
  const content = fs.readFileSync(f, 'utf-8')
  const tmplMatch = content.match(/<template>([\s\S]*?)<\/template>/)?.[1] || ''

  // 提取 v-for 局部变量
  const definedSet = new Set()
  const vforRe = /v-for=(?:["'])([^"']+)(?:["'])/g
  let m
  while ((m = vforRe.exec(tmplMatch)) !== null) {
    const expr = m[1]
    const inIdx = expr.indexOf(' in ')
    if (inIdx < 0) continue
    const left = expr.substring(0, inIdx).replace(/[()]/g, '').trim()
    left.split(',').forEach(n => {
      const name = n.trim().split(/\s+as\s+/)[0].trim()
      if (name && /^\w+$/.test(name)) definedSet.add(name)
    })
  }
  // v-slot 局部: <template #default="{ row }">
  const slotRe = /<template\s+#[\w-]+\s*=\s*["']\{([^}]+)\}["']/g
  while ((m = slotRe.exec(tmplMatch)) !== null) {
    const expr = m[1]
    expr.split(',').forEach(n => {
      const name = n.trim().split(/[:=]/)[0].trim()
      if (name && /^\w+$/.test(name)) definedSet.add(name)
    })
  }

  // 清理模板: 移除属性值字符串
  let cleaned = tmplMatch
  // 移除 ="...", ='...'
  cleaned = cleaned.replace(/=\s*"[^"]*"/g, '=""')
  cleaned = cleaned.replace(/=\s*'[^']*'/g, "=''")
  // 移除 JS 字符串字面量 (避免 prop value 里的 'chat.start' 误识)
  // 单引号
  cleaned = cleaned.replace(/'[^'\n]*?'/g, "''")
  // 双引号
  cleaned = cleaned.replace(/"[^"\n]*?"/g, '""')
  // 反引号 (模板字符串)
  cleaned = cleaned.replace(/`[^`]*`/g, '``')
  // 移除 {{ }} 插值
  cleaned = cleaned.replace(/\{\{[\s\S]*?\}\}/g, '')
  // 移除 v-on/@ 后面的修饰符: .stop .prevent .enter .keydown
  cleaned = cleaned.replace(/@[\w-]+(\.\w+)+/g, '@EVT')
  cleaned = cleaned.replace(/v-on:[\w-]+(\.\w+)+/g, 'v-on:EVT')
  // 函数调用
  cleaned = cleaned.replace(/\b[a-zA-Z_]\w*\s*\(/g, 'FN(')

  // 提取 .xxx
  const varInTmpl = new Set()
  ;(cleaned.match(/\b([a-z]\w{2,})\.\w+/g) || []).forEach(x => {
    const v = x.split('.')[0]
    if (!BUILTIN.has(v) && !isElComponent(v) && v.length > 2) {
      varInTmpl.add(v)
    }
  })

  // 提取 script setup 顶层定义
  const scriptMatch = content.match(/<script setup>([\s\S]*?)<\/script>/)?.[1] || ''
  ;(scriptMatch.matchAll(/^import\s+\{([^}]+)\}/gm)).forEach(m => {
    m[1].split(',').forEach(s => {
      const name = s.trim().split(/\s+as\s+/)[0].trim()
      if (name) definedSet.add(name)
    })
  })
  ;(scriptMatch.matchAll(/^(?:const|let|var|function)\s+(\w+)/gm)).forEach(m => {
    definedSet.add(m[1])
  })
  ;(scriptMatch.matchAll(/defineProps\s*\(\s*\{([^}]+)\}/g) || []).forEach(m => {
    m[1].split(/[,\n]/).forEach(s => {
      const name = s.split(':')[0].trim()
      if (name && /^\w+$/.test(name)) definedSet.add(name)
    })
  })

  // 对比
  for (const v of varInTmpl) {
    if (!definedSet.has(v)) {
      log.push(`❌ ${f}: 模板用 \`${v}.xxx\` 但 setup 未定义`)
      errors++
    }
  }
}

console.log('═══════════════════════════════════════════════════════════')
console.log('  V3.5.95 <script setup> 防御性自检 (22 view)')
console.log('═══════════════════════════════════════════════════════════')
for (const l of log) console.log('  ' + l)
console.log('═══════════════════════════════════════════════════════════')
console.log(`  扫描: ${files.length} .vue 文件`)
console.log(`  错误: ${errors}`)
console.log(`  状态: ${errors === 0 ? '✅ ALL PASS' : '❌ 有未定义变量'}`)
console.log('═══════════════════════════════════════════════════════════')
process.exit(errors > 0 ? 1 : 0)
