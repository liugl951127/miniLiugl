#!/usr/bin/env node
/**
 * V3.6.8+ 22 view el-watermark 自定义 (用户名 + 角色 + 时间)
 * 改 v-if="true" 内容: ['Liugl-AI V3.6.8', userStore.profile?.username, roles[0], date]
 */
const fs = require('fs')
const path = require('path')

const viewsDir = 'frontend/src/views'
const EXCLUDE = ['auth/Login.vue', 'auth/H5Login.vue', 'About.vue']
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
  // 找 V3.6.3+ 启用 el-watermark 段
  if (content.includes('el-watermark') && content.includes('V3.6.3+ 启用')) {
    const new_wm = `<!-- V3.6.8+ 增强 el-watermark (用户名 + 角色 + 时间) -->
  <el-watermark
    v-if="true"
    :content="[
      'Liugl-AI V3.6.8',
      userStore.profile?.username || 'Guest',
      (userStore.profile?.roles || ['USER'])[0],
      new Date().toLocaleString('zh-CN')
    ]"
    :font="{ size: 12, color: 'rgba(99, 102, 241, 0.05)' }"
    :gap="[160, 100]"
    class="page-watermark"
  />`
    content = content.replace(
      /<!-- V3\.6\.3\+ 启用 el-watermark[\s\S]*?\/>/,
      new_wm
    )
    fs.writeFileSync(file, content)
    count++
  }
}

walk(viewsDir)
console.log(`✓ V3.6.8+ 增强 el-watermark: ${count} 个 view`)
