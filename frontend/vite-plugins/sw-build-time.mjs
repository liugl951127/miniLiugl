/**
 * V3.7.20+ 简化: vite plugin 已弃用
 * 之前 transformIndexHtml 改 src="/sw.js" (但实际是 inline script, 改不到)
 * 现在所有注入统一在 scripts/inject-sw-build-time.mjs (postbuild)
 *
 * 保留: 空 plugin 占位 (config 引用存在)
 */
export default function swBuildTime() {
  return {
    name: 'sw-build-time-noop',
  }
}
