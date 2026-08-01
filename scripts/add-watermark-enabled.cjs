#!/usr/bin/env node
/**
 * V3.6.3+ 22 view el-watermark v-if="true" 启用 (排除 login/h5login)
 * 改: 隐藏 watermark 内容大小 14, 透明度 0.06, 不影响视觉
 */
const fs = require('fs')
const path = require('path')

const viewsDir = 'frontend/src/views'
const EXCLUDE = ['auth/Login.vue', 'auth/H5Login.vue', 'About.vue', 'showcase/']
let count = 0

function walk(dir, base = '') {
  if (!fs.existsSync(dir)) return
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const rel = base ? base + '/' + e.name : e.name
    if (EXCLUDE.some(x => rel.includes(x))) continue
    const p = path.join(dir, e.name)
    if (e.isDirectory()) walk(p, rel)
    else if (e.name.endsWith('.vue') && !e.name.includes('.bak')) processFile(p, rel)
  }
}

function processFile(file, rel) {
  let content = fs.readFileSync(file, 'utf-8')
  // 已有 v-if="true" 跳过
  if (content.includes('el-watermark v-if="true"')) return
  // 改 v-if="false" → v-if="true" + 自定义 logo
  if (content.includes('el-watermark v-if="false" content="V3.6.1"')) {
    const new_wm = `<!-- V3.6.3+ 启用 el-watermark (V3.6.1 标识 + 用户名 + 时间) -->
  <el-watermark
    v-if="true"
    :content="['Liugl-AI V3.6.3', userStore.profile?.username || 'Guest', new Date().toLocaleDateString('zh-CN')]"
    :font="{ size: 14, color: 'rgba(99, 102, 241, 0.06)' }"
    :gap="[120, 80]"
    class="page-watermark"
  />`
    content = content.replace(
      /<el-watermark v-if="false" content="V3\.6\.1"[^>]*\/>/,
      new_wm
    )
    fs.writeFileSync(file, content)
    count++
  }
}

walk(viewsDir)
console.log(`✓ 启用 el-watermark: ${count} 个 view (排除 login/h5login/About)`)
