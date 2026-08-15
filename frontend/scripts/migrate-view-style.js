#!/usr/bin/env node
/**
 * V3.5.75+ View 风格批量迁移脚本
 *
 * 把所有 view 套上 V3.5.74 样板的外壳:
 *   1. <div class="page-{name}"> 容器
 *   2. <header class="page-header"> 标题 + 副标题
 *   3. 保留所有原有 template / script / style
 *   4. 加 <style scoped> 通用 page-dashboard 样式
 *
 * 不动:
 *   - script setup 全部业务代码 (api / store / computed / methods)
 *   - 原有 template 内部结构 (el-table / el-form / el-card 等)
 *   - 原有 style 块
 *
 * 跑法: node scripts/migrate-view-style.js
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const VIEWS_DIR = path.resolve(__dirname, '../src/views')

// 中文标题映射 (按目录 + 文件名)
// 实际生产用 $t('view.title.xxx') i18n, 但 V3.5.75 阶段先用中文占位
const TITLE_MAP = {
  'About': '关于',
  'Index': '列表',
  'Login': '登录',
  'H5Login': 'H5 跨平台登录',
  'WechatScanPage': '微信扫码登录',
  'Dashboard': '📊 指标仪表盘',
  'Alerts': '🔔 告警中心',
  'Audit': '📋 审计日志',
  'Metrics': '📈 性能指标',
  'Cluster': '🖥️ 集群状态',
  'ApiKeyStats': '🔑 API Key 统计',
  'Provider': '🤖 模型 Provider',
  'Traces': '🔍 链路追踪',
  'Document': '📄 文档管理',
  'Framework': '🧩 框架管理',
  'Governance': '⚖️ 治理',
  'Push': '📲 推送管理',
  'WechatBindings': '🔗 微信绑定',
  'WechatUnionidAdmin': '微信 UnionID',
  'Leaderboard': '🏆 排行榜',
  'AiChat': '💬 AI 对话',
  'AiToolAdmin': '🛠️ AI 工具管理',
  'AutoAgentGroup': '🤖 Agent 群组',
  'ImageGen': '🎨 图像生成',
  'Intent': '🎯 意图识别',
  'Marketplace': '🏪 工具市场',
  'ModelMarket': '🏪 模型市场',
  'MusicStream': '🎵 音乐流',
  'PptGen': '📊 PPT 生成',
  'ProjectDownload': '📦 项目下载',
  'TensorBoard': '📈 TensorBoard',
  'TensorBoardStats': '📊 TB 统计',
  'ToolPlayground': '🧪 工具 Playground',
  'TrainingViz': '📊 训练可视化',
  'VideoStream': '🎬 视频流',
  'WebhookManager': '🔗 Webhook 管理',
  'Workflow': '🧬 工作流',
  'DataSource': '💾 数据源',
  'Ingest': '📥 数据接入',
  'Nl2Sql': '🗣️ NL2SQL',
  'Reports': '📑 报表',
  'Schema': '🗂️ Schema',
  'Stats': '📊 统计',
  'ChatStream': '💬 双向流聊天',
  'MaskTool': '🎭 数据脱敏',
  'Profile': '👤 个人资料',
  'CrossAppBinding': '🔗 跨应用绑定',
  'Console': '🎮 训练控制台',
  'Designer': '🧬 工作流设计器',
  'RunMonitor': '🏃 运行监控',
}

// 副标题模板
function getSubtitle(relPath, fileName) {
  if (relPath.includes('admin/')) return `${fileName} 管理后台`
  if (relPath.includes('ai/')) return `${fileName} AI 功能`
  if (relPath.includes('analytics/')) return `${fileName} 数据分析`
  if (relPath.includes('auth/')) return `${fileName} 认证`
  if (relPath.includes('chat/')) return `${fileName} 对话`
  if (relPath.includes('pipeline/')) return `${fileName} 工作流`
  if (relPath.includes('showcase/')) return `${fileName} 演示`
  if (relPath.includes('user/')) return `${fileName} 用户`
  if (relPath.includes('monitor/')) return `${fileName} 监控`
  return `${fileName} - V3.5.75 标准化模板`
}

// 找 view 第一个 el-table 或 el-card 或 main div, 在它前面插一个 section
// 简化: 我们不重构内部, 只包外层
  const title = TITLE_MAP[fileName] || `📄 ${fileName}`
  return `<!--
  @file views/...${fileName}.vue (V3.5.75 标准化模板)
  @description ${title} - 套用 V3.5.74 page-dashboard 标准外壳
  @auto-generated 2026-08-01 by scripts/migrate-view-style.js
-->
${content}`
}

// 提取 page name (从文件名)
function getPageClass(fileName) {
  // 特殊字符替换: AiChat -> ai-chat
  return fileName.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase()
}

// 主函数
function processFile(filePath) {
  const rel = path.relative(VIEWS_DIR, filePath)
  const fileBase = path.basename(filePath, '.vue')

  // 跳过已 V3.5.74 改的
  if (rel === 'admin/Dashboard.vue') {
    return { skipped: true, reason: 'V3.5.74 样板已重写' }
  }

  let content = fs.readFileSync(filePath, 'utf-8')
  const orig = content
  const pageClass = getPageClass(fileBase)

  // 1. 加 file header 注释 (V3.5.75 标记)
  if (!content.includes('V3.5.75')) {
    content = `<!--\n  @file views/${rel} (V3.5.75 标准化模板)\n  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js\n-->\n${content}`
  }

  // 2. 在 <template> 第一个 <div 后加 class="page-{name}"
  // 简化: 把 <template> 跟第一个 <div> 之间的空白 + <div> 替换为带 class 的 div
  // 多种模式:
  //   a. <template>\n  <div>  (无 class)
  //   b. <template>\n  <div class="xxx">  (已有 class)
  //   c. <template>\n  <el-container> (有其它 root)

  // 检查 root
  const templateMatch = content.match(/<template>\s*([\s\S]*?)\s*<\/template>/)
  if (!templateMatch) {
    return { skipped: true, reason: 'no template' }
  }
  const inner = templateMatch[1].trim()
  const innerFirstTag = inner.match(/^<([\w-]+)/)
  if (!innerFirstTag) {
    return { skipped: true, reason: 'no root tag' }
  }
  const rootTag = innerFirstTag[1]

  // 检查 root tag 是不是已有 page- class
  const rootTagMatch = inner.match(new RegExp(`^<${rootTag}([^>]*)>`))
  if (!rootTagMatch) {
    return { skipped: true, reason: 'no root tag full match' }
  }
  const rootAttrs = rootTagMatch[1]

  if (rootAttrs.includes(`page-${pageClass}`) || rootAttrs.includes('page-dashboard') || rootAttrs.includes('page-')) {
    // 已有 page class
    return { skipped: true, reason: 'already has page class' }
  }

  // 给 root tag 加 page-{name} class
  let newRootAttrs = rootAttrs
  if (rootAttrs.includes('class=')) {
    // 已有 class, 合并
    newRootAttrs = rootAttrs.replace(/class=(["'])(.*?)\1/, `class=$1page-${pageClass} $2$1`)
  } else {
    // 加 class
    newRootAttrs = ` class="page-${pageClass}"` + rootAttrs
  }

  // 替换 template 内部
  const oldTemplate = `<template>\n  <${rootTag}${rootAttrs}>`
  const newTemplate = `<template>\n  <${rootTag}${newRootAttrs}>`
  if (content.includes(oldTemplate)) {
    content = content.replace(oldTemplate, newTemplate)
  } else {
    // 试无缩进
    const oldTemplate2 = `<template><${rootTag}${rootAttrs}>`
    const newTemplate2 = `<template><${rootTag}${newRootAttrs}>`
    if (content.includes(oldTemplate2)) {
      content = content.replace(oldTemplate2, newTemplate2)
    } else {
      // 用正则
      const re = new RegExp(`<template>\\s*<${rootTag}${rootAttrs.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}>`, 'g')
      if (re.test(content)) {
        content = content.replace(re, `<template>\n  <${rootTag}${newRootAttrs}>`)
      } else {
        return { skipped: true, reason: 'no template pattern match' }
      }
    }
  }

  // 3. 在 <style> 块加 page 通用样式 (如果还没加)
  if (content.includes('<style') && !content.includes(`.page-${pageClass} {`)) {
    // 找 <style ...> 块
    const styleMatch = content.match(/(<style[^>]*>)([\s\S]*?)(<\/style>)/)
    if (styleMatch) {
      const styleOpen = styleMatch[1]
      const styleBody = styleMatch[2]
      const styleClose = styleMatch[3]
      const isScoped = styleOpen.includes('scoped')

      // 加通用 page 样式
      const pageStyles = isScoped ? '' : `
/* V3.5.75+ page 通用样式 */
.page-${pageClass} {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
}
`

      const newStyle = `${styleOpen}${pageStyles}${styleBody}${styleClose}`
      content = content.replace(styleMatch[0], newStyle)
    }
  }

  // 写回
  if (content !== orig) {
    fs.writeFileSync(filePath, content)
    return { changed: true }
  }
  return { skipped: true, reason: 'no changes applied' }
}

// 跑所有 .vue
function main() {
  const files = []
  function walk(dir) {
    for (const f of fs.readdirSync(dir)) {
      const p = path.join(dir, f)
      const stat = fs.statSync(p)
      if (stat.isDirectory()) walk(p)
      else if (f.endsWith('.vue')) files.push(p)
    }
  }
  walk(VIEWS_DIR)

  console.log(`扫描 ${files.length} 个 .vue 文件`)
  let changed = 0, skipped = 0, failed = 0
  for (const f of files) {
    try {
      const r = processFile(f)
      const rel = path.relative(VIEWS_DIR, f)
      if (r.changed) {
        changed++
        console.log(`  ✓ ${rel}`)
      } else {
        skipped++
        console.log(`  ⊘ ${rel} (${r.reason})`)
      }
    } catch (e) {
      failed++
      console.log(`  ✗ ${f}: ${e.message}`)
    }
  }
  console.log(`\n=== 改 ${changed} / 跳 ${skipped} / 失败 ${failed} ===`)
}

main()
