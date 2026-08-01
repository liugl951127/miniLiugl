/**
 * V3.6.13+ Vite plugin: sw.js build time 注入 + HTML ?v={ts}
 * 1. HTML sw.js 引用加 ?v={ts} 强制浏览器拉新
 * 2. dist/sw.js 的 __SW_BUILD_TIME__ 替换为实际 ISO 时间
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

export default function swBuildTime() {
  let config
  return {
    name: 'sw-build-time',
    configResolved(c) { config = c },
    transformIndexHtml: {
      order: 'post',
      handler(html) {
        const ts = Date.now()
        return html.replace(/src="\/sw\.js"/g, `src="/sw.js?v=${ts}"`)
      },
    },
    closeBundle() {
      const distDir = config?.build?.outDir || 'dist'
      const swPath = join(distDir, 'sw.js')
      try {
        const content = readFileSync(swPath, 'utf-8')
        const ts = new Date().toISOString()
        const updated = content.replace(/__SW_BUILD_TIME__/g, ts)
        if (updated !== content) {
          writeFileSync(swPath, updated)
          console.log(`[sw-build-time] Injected ${ts} into sw.js`)
        }
      } catch (e) { /* dev 模式忽略 */ }
    },
  }
}
