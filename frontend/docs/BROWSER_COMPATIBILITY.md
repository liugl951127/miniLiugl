# V3.5.76+ 浏览器兼容矩阵

## 1. 目标覆盖

| 浏览器 | 最低版本 | 当前状态 |
|--------|---------|---------|
| Chrome | 63+ | ✓ 完全支持 |
| Edge (Chromium) | 79+ | ✓ 完全支持 |
| Firefox | 60+ | ✓ 完全支持 |
| Safari (macOS) | 12+ | ✓ 完全支持 |
| Safari (iOS) | 12+ | ✓ 完全支持 |
| Android Chrome | 8+ | ✓ 完全支持 |
| Android WebView | 8+ | ✓ 完全支持 |
| 微信内置浏览器 (X5) | iOS 12+ / Android 8+ | ✓ 完全支持 |
| IE 11 | — | ✘ 不支持 (`not IE 11` in browserslist) |

## 2. 兼容层架构

```
src/styles/compat.scss   (281 行, 14 节)
  - CSS 变量 (design token)
  - Dark mode
  - Reset + Base
  - iOS Safari 100vh 修复
  - 滚动行为
  - 触摸优化
  - 响应式断点
  - CSS Grid / Flexbox
  - backdrop-filter
  - prefers-reduced-motion
  - 高对比度
  - 打印样式
  - 工具类
  - Vue Transition 兼容

src/composables/useBrowserCompat.js   (377 行)
  - detectFeatures() - 特性支持矩阵
  - loadPolyfills() - 9 个 polyfill 懒加载
  - detectBrowser() - UA 嗅探
  - initBrowserCompat() - 入口
```

## 3. build 配置

```js
// vite.config.js
build: {
  target: 'es2015',   // Chrome 63+/Edge 79+/FF 60+/Safari 12+
  // browserslist 覆盖: Chrome >= 63, Safari >= 12, iOS >= 12
}

esbuild: {
  target: 'es2018',   // esbuild 转换降级
}
```

## 4. 跨浏览器 E2E (Playwright)

`e2e/cross-browser.spec.js` 9 个 case:
- 页面加载无错
- localStorage / sessionStorage
- CSS 变量
- ES2017 async/await
- ES2020 optional chaining
- ES2020 nullish coalescing
- fetch API
- AbortController
- IntersectionObserver

3 个响应式 case:
- 桌面 1280px
- 平板 768px
- 手机 375px

跑: `npx playwright test --project=webkit` / `--project=firefox` / `--project=chromium` / `--project=mobile-safari` / `--project=mobile-chrome`

## 5. 已知兼容问题 + 修法

| 问题 | 浏览器 | 修法 |
|------|--------|------|
| 100vh 包含地址栏 | iOS Safari | `100dvh` + `100vh` fallback |
| 滚动卡顿 | iOS Safari | `-webkit-overflow-scrolling: touch` |
| 触摸延迟 300ms | 老 Android | `touch-action: manipulation` |
| CSS Grid 不支持 | IE 11 (我们不支持) | Flexbox 兜底 |
| backdrop-filter | 老 Edge | 背景色 fallback |
| Service Worker | 旧 Android | 仅支持 Chrome 40+ |
| ES2020 语法 | 旧 Safari | esbuild target es2015 降级 |

## 6. 验证方法

```bash
# 1. 单元测试
cd frontend && npm run test:unit

# 2. 跨浏览器 E2E
cd frontend
npx playwright test --project=chromium
npx playwright test --project=webkit      # Safari
npx playwright test --project=firefox
npx playwright test --project=mobile-safari

# 3. 手动测试
# - Chrome DevTools: iPhone 13 emulator
# - BrowserStack: 真机测试 (iOS Safari / Android Chrome)
# - 微信开发者工具: 微信内置浏览器

# 4. build target 验证
cd frontend && npx vite build
# 看 dist/assets/*.js 是否包含 es2015 语法 (const/let/async/...)
```
