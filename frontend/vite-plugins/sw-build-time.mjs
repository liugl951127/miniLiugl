/**
 * V3.6.13+ Vite plugin: HTML sw.js ?v={ts}
 * V3.7.19+ 简化: dist/sw.js 注入移到 scripts/inject-sw-build-time.mjs
 * (Vite 5+ writeBundle 不可靠)
 *
 * 保留能力:
 * 1. HTML 引用加 ?v={ts} 强制浏览器拉新
 */
export default function swBuildTime() {
  return {
    name: 'sw-build-time',
    transformIndexHtml: {
      order: 'post',
      handler(html) {
        const ts = Date.now()
        const updated = html.replace(/src="\/sw\.js"/g, `src="/sw.js?v=${ts}"`)
        if (updated !== html) {
          process.stderr.write(`[sw-build-time] Injected ?v=${ts} into index.html\n`)
        }
        return updated
      },
    },
  }
}
