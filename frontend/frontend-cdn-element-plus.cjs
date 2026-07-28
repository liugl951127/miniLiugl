// 简单 plugin: 把 'element-plus' import 转成 CDN 全局
// 改业务代码 import { ElButton } from 'element-plus' 为 'window.ElementPlus.ElButton'
import { readFileSync } from 'fs'
import { resolve } from 'path'

export default function cdnElementPlus(options = {}) {
  const {
    version = '2.6.2',
    cssHash = 'element-plus',
  } = options
  return {
    name: 'vite-plugin-cdn-element-plus',
    enforce: 'pre',
    resolveId(id) {
      if (id === 'element-plus' || id.startsWith('element-plus/') || id.startsWith('@element-plus/')) {
        return { id, external: true }
      }
    },
    transformIndexHtml(html) {
      return html.replace(
        '</head>',
        `<link rel="stylesheet" href="https://unpkg.com/element-plus@${version}/dist/index.css">
        <script src="https://unpkg.com/element-plus@${version}/dist/index.full.min.js"></script>
        <script src="https://unpkg.com/element-plus@${version}/dist/locale/zh-cn.min.js"></script>
        </head>`
      )
    },
  }
}
