#!/usr/bin/env node
/**
 * V3.6.6+: 校验 <el-menu-item> / router.push 路径跟 router 一致 (Node 提速版)
 * 之前 Python 版卡顿, V3.6.6 改 Node .cjs (3-5x 加速)
 * V3.6.6: 支持 redirect 路径 + 尾段匹配 + 父子级匹配
 */
const fs = require('fs')
const path = require('path')

const FRONTEND = 'frontend/src'

function walk(dir, files = []) {
  if (!fs.existsSync(dir)) return files
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name)
    if (e.isDirectory()) walk(p, files)
    else if (e.name.endsWith('.vue') && !e.name.includes('.bak')) files.push(p)
  }
  return files
}

function readFile(file) {
  try { return fs.readFileSync(file, 'utf-8') } catch { return '' }
}

// 提取 router 路径 + redirect
const routerFile = path.join(FRONTEND, 'router/index.js')
const routerContent = readFile(routerFile)
const routerPaths = new Set()
const redirectPaths = new Set()

const pathRe = /path:\s*['"]([^'"]+)['"]/g
let m
while ((m = pathRe.exec(routerContent)) !== null) routerPaths.add(m[1])

const redRe = /redirect:\s*['"]([^'"]+)['"]/g
while ((m = redRe.exec(routerContent)) !== null) redirectPaths.add(m[1])

function extractFromFiles(files, regex) {
  const out = []
  for (const f of files) {
    const content = readFile(f)
    const re = new RegExp(regex, 'g')
    let m
    while ((m = re.exec(content)) !== null) out.push({ file: f, path: m[1] })
  }
  return out
}

const files = walk(path.join(FRONTEND, 'views'))
const menuPaths = extractFromFiles(files, "<el-menu-item[^>]*index=['\"]([^'\"]+)['\"]")
const routerPushPaths = extractFromFiles(files, "router\\.push\\(\\s*['\"]([^'\"]+)['\"]")

// 智能匹配: 绝对路径 / 相对路径 / 父级 redirect / 父子级 / 尾段
function pathExists(p) {
  if (routerPaths.has(p)) return true
  if (redirectPaths.has(p)) return true
  const clean = p.replace(/^\//, '')
  if (routerPaths.has(clean)) return true
  if (redirectPaths.has(clean)) return true
  // 尾段匹配 (admin/alerts -> alerts)
  const lastSeg = clean.split('/').pop()
  if (lastSeg && routerPaths.has(lastSeg)) return true
  // 父子级 (/admin/wechat/unionid -> admin/wechat/unionid)
  for (let i = 1; i < clean.split('/').length; i++) {
    const subPath = clean.split('/').slice(i).join('/')
    if (subPath && routerPaths.has(subPath)) return true
  }
  return false
}

let errors = 0
console.log('═══════════════════════════════════════════════════════════')
console.log(`  V3.6.6+ Check 7: menu 路径跟 router 路径一致 (Node 提速版)`)
console.log('═══════════════════════════════════════════════════════════')
console.log(`  Router paths: ${routerPaths.size}, redirects: ${redirectPaths.size}, Menu: ${menuPaths.length}, Push: ${routerPushPaths.length}`)

for (const { file, path: p } of menuPaths) {
  if (!pathExists(p)) {
    console.log(`  ❌ ${file.replace(FRONTEND + '/', '')}: menu "${p}" 不在 router`)
    errors++
  }
}
for (const { file, path: p } of routerPushPaths) {
  if (!pathExists(p)) {
    console.log(`  ❌ ${file.replace(FRONTEND + '/', '')}: push "${p}" 不在 router`)
    errors++
  }
}

console.log('═══════════════════════════════════════════════════════════')
console.log(`  错误: ${errors}`)
console.log(`  状态: ${errors === 0 ? '✅ ALL PASS' : '❌ 有 menu 路径缺失'}`)
console.log('═══════════════════════════════════════════════════════════')
process.exit(errors > 0 ? 1 : 0)
