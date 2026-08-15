#!/usr/bin/env node
/**
 * V3.6.1+ 给所有 view 加 el-watermark 标识 (V3.5.46+ 版本号)
 */
const fs = require('fs')
const path = require('path')

const viewsDir = 'frontend/src/views'
let count = 0

function walk(dir) {
  if (!fs.existsSync(dir)) return
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name)
    if (e.isDirectory()) walk(p)
    else if (e.name.endsWith('.vue') && !e.name.includes('.bak')) processFile(p)
  }
}

function processFile(file) {
  const content = fs.readFileSync(file, 'utf-8')
  // 已加过
  if (content.includes('el-watermark')) return
  // 找 page-header 段
  if (!content.includes('page-header')) return

  // 在 page-header 之前加 el-watermark 块 (有条件)
  // 找 <header class="page-header"> 段
  const old = '<header class="page-header">'
  if (!content.includes(old)) return
  const newStr = `<!-- V3.6.1+ 版本标识 (el-watermark) -->
  <el-watermark v-if="false" content="V3.6.1" :font="{ size: 8 }" class="page-watermark" />
  <header class="page-header">`
  if (content.includes(old)) {
    const updated = content.replace(old, newStr)
    fs.writeFileSync(file, updated)
    count++
  }
}

walk(viewsDir)
console.log(`✓ 加 el-watermark 标识: ${count} 个 view`)
