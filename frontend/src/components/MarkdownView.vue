<!--
  MarkdownView.vue
  Markdown 渲染 + 代码高亮 + 复制按钮 + 链接
  醒目特性: 代码块带语言标签 + 复制按钮 / 表格渲染 / 任务列表
-->
<!--
  @file components/MarkdownView.vue (Markdown 渲染)
  @version V3.5.12+ (前端注释补全)
  @description Markdown 渲染
-->
<template>
  <div class="md-body" v-html="rendered"></div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { computed, onMounted } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

const props = defineProps({
  source: { type: String, default: '' },
})

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        const out = hljs.highlight(str, { language: lang, ignoreIllegals: true }).value
        return `<pre class="hljs-pre"><div class="hljs-header"><span class="hljs-lang">${lang}</span><button class="hljs-copy" data-code="${encodeURIComponent(str)}">复制</button></div><code class="hljs language-${lang}">${out}</code></pre>`
      } catch (_) {}
    }
    const enc = encodeURIComponent(str)
    return `<pre class="hljs-pre"><div class="hljs-header"><span class="hljs-lang">text</span><button class="hljs-copy" data-code="${enc}">复制</button></div><code class="hljs">${md.utils.escapeHtml(str)}</code></pre>`
  },
})

const rendered = computed(() => {
  if (!props.source) return ''
  let html = md.render(props.source)
  // 引用块特殊样式
  html = html.replace(
    /<blockquote>/g,
    '<blockquote class="md-blockquote">'
  )
  return html
})

onMounted(() => {
  // 绑定复制按钮
  document.addEventListener('click', (e) => {
    const t = e.target
    if (t && t.classList && t.classList.contains('hljs-copy')) {
      const code = decodeURIComponent(t.getAttribute('data-code') || '')
      navigator.clipboard.writeText(code).then(() => {
        const orig = t.textContent
        t.textContent = '✓ 已复制'
        t.classList.add('copied')
        setTimeout(() => {
          t.textContent = orig
          t.classList.remove('copied')
        }, 1500)
      })
    }
  })
})
</script>

<style lang="scss" scoped>
.md-body {
  font-size: 14px;
  line-height: 1.65;
  color: #1f2937;
  word-wrap: break-word;

  :deep(p) { margin: 8px 0; }
  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    margin: 16px 0 8px;
    font-weight: 600;
    line-height: 1.3;
  }
  :deep(h1) { font-size: 20px; }
  :deep(h2) { font-size: 18px; }
  :deep(h3) { font-size: 16px; }
  :deep(h4) { font-size: 15px; }

  :deep(ul), :deep(ol) { margin: 8px 0; padding-left: 24px; }
  :deep(li) { margin: 4px 0; }
  :deep(li > input[type=checkbox]) { margin-right: 6px; }

  :deep(a) {
    color: #2563eb;
    text-decoration: none;
    border-bottom: 1px solid transparent;
    transition: border-color .2s;
  }
  :deep(a:hover) { border-bottom-color: #2563eb; }

  :deep(.md-blockquote) {
    margin: 10px 0;
    padding: 8px 14px;
    border-left: 3px solid #6366f1;
    background: #eef2ff;
    border-radius: 0 6px 6px 0;
    color: #4338ca;
    font-style: italic;
  }
  :deep(.md-blockquote p) { margin: 4px 0; }

  :deep(table) {
    border-collapse: collapse;
    margin: 12px 0;
    width: 100%;
    font-size: 13px;
  }
  :deep(th), :deep(td) {
    border: 1px solid #e5e7eb;
    padding: 6px 10px;
    text-align: left;
  }
  :deep(th) {
    background: #f9fafb;
    font-weight: 600;
  }
  :deep(tr:nth-child(even)) { background: #fafafa; }

  :deep(img) {
    max-width: 100%;
    border-radius: 6px;
    margin: 8px 0;
  }

  :deep(.hljs-pre) {
    position: relative;
    margin: 12px 0;
    padding: 0;
    background: #0d1117;
    border-radius: 8px;
    overflow: hidden;
  }
  :deep(.hljs-header) {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 4px 12px;
    background: #161b22;
    color: #8b949e;
    font-size: 12px;
    border-bottom: 1px solid #21262d;
  }
  :deep(.hljs-lang) {
    text-transform: uppercase;
    letter-spacing: 0.5px;
    font-weight: 600;
  }
  :deep(.hljs-copy) {
    background: transparent;
    border: 1px solid #30363d;
    color: #8b949e;
    padding: 2px 8px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 11px;
    transition: all .2s;
  }
  :deep(.hljs-copy:hover) {
    background: #21262d;
    color: #c9d1d9;
  }
  :deep(.hljs-copy.copied) {
    background: #238636;
    color: white;
    border-color: #238636;
  }
  :deep(.hljs) {
    display: block;
    padding: 14px 16px;
    overflow-x: auto;
    background: #0d1117;
    color: #c9d1d9;
    font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
    font-size: 13px;
    line-height: 1.5;
  }
  :deep(.hljs code) {
    background: transparent;
    padding: 0;
    color: inherit;
  }
  :deep(pre) {
    margin: 0;
  }
  :deep(strong) { font-weight: 600; color: #111827; }
  :deep(em) { font-style: italic; }
  :deep(hr) {
    border: none;
    border-top: 1px dashed #d1d5db;
    margin: 16px 0;
  }
}
</style>
