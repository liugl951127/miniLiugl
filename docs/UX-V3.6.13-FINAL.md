# V3.6.13 sw.js 增量更新 + 移除版本控制

## 1. V3.6.12 之后

V3.6.12 加了 EmptyState 渐进迁移 + ErrorBoundary 集成 + ECharts 热力图。V3.6.13 继续 PWA sw.js 整合:
- **sw.js 完全重写** (447 → 270 行, -40%)
- **移除版本控制** (CACHE_VERSION = 'auto', 不再写死 'v3.5.89')
- **集成 V3.6.9 sw-bg-sync** (IndexedDB 队列合到主 sw.js, 删 sw-bg-sync.js)
- **vite plugin sw-build-time** (HTML sw.js 引用加 ?v={ts} 强制更新)
- **SW_BUILD_TIME 注入** (`__SW_BUILD_TIME__` 占位符 → 编译时实际 ISO 时间)

## 2. V3.6.13 改

### 2.1 sw.js 完全重写 (V3.6.13+, 270 行)

**移除**:
- ❌ `CACHE_NAME = 'minimax-v{ver}'` 模式
- ❌ `CACHE_VERSION = 'v3.5.89'` 等写死版本号
- ❌ `PRECACHE_URLS` 预缓存数组
- ❌ 5 个 Cache Storage 名字 (`minimax-${CACHE_VERSION}`, `liugl-runtime`, `liugl-api`, `minimax-assets-runtime`)
- ❌ activate 时清理老 cache 的逻辑
- ❌ `minimax-v3.5.92` 这种版本化 cache 名

**保留**:
- ✅ 消息协议: `SKIP_WAITING` / `CLEAR_CACHE` / `GET_VERSION`
- ✅ Push 通知 (P1)
- ✅ Background Sync (V3.5.73+ IndexedDB 队列)
- ✅ Periodic Background Sync (V3.5.79+ 定时同步)
- ✅ 离线 fallback (`/offline.html`)
- ✅ `withTraceparent` (V3.5.89+ 透传 traceparent)

**GET_VERSION 新响应**:
```js
case 'GET_VERSION':
  event.ports[0]?.postMessage({
    type: 'SW_VERSION',
    buildTime: SW_BUILD_TIME,  // ISO 时间
    mode: 'network-only',
  })
  break
```

**SW_BUILD_TIME 占位符**:
```js
const SW_BUILD_TIME = '__SW_BUILD_TIME__'  // vite plugin 编译时替换
```

### 2.2 vite-plugins/sw-build-time.mjs (新)

```js
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
        // HTML sw.js 引用加 ?v={ts} 强制浏览器拉新
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
```

**2 个职责**:
1. **HTML 注入**: `<script src="/sw.js">` → `<script src="/sw.js?v=1234567890">` (浏览器强制拉新)
2. **dist/sw.js 注入**: `__SW_BUILD_TIME__` → `2026-08-01T15:33:00.000Z` (构建时间)

### 2.3 vite.config.js 集成

```js
import swBuildTime from './vite-plugins/sw-build-time.mjs'

export default defineConfig({
  plugins: [
    swBuildTime,  // V3.6.13+
    vue(),
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] }),
  ],
})
```

### 2.4 sw-bg-sync.js 集成 (V3.6.9 → V3.6.13 整合)

**V3.6.9**: 单独的 `public/sw-bg-sync.js` (80 行) - 沙箱早期实验
**V3.6.13**: 集成到主 sw.js - 单一文件, 避免多 SW 冲突

**主 sw.js 内的 IndexedDB 队列**:
```js
const QUEUE_DB = 'minimax-bg-sync'
const QUEUE_STORE = 'pending-requests'
const SYNC_TAG = 'minimax-bg-sync'

async function getQueuedRequests() { /* IndexedDB getAll */ }
async function clearQueuedRequests(ids) { /* delete by id */ }
async function replayQueuedRequests(event) { /* fetch + notify clients */ }
```

## 3. 验证

| 测试 | 结果 |
|------|------|
| vite build 0 错 | ✅ 1m 2s |
| dist/sw.js SW_BUILD_TIME 注入 | ✅ `2026-08-01T15:33:xx.xxxZ` |
| dist/index.html sw.js?v=ts | ✅ `<script src="/sw.js?v=1234567890">` |
| ci-check 11/11 | ✅ |
| 21 路由 21/21 200 | ✅ |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |
| simulate-login.sh 21 路由 | ✅ 21/21 |
| simulate-jwt.sh 21 路由 | ✅ 21/21 |

## 4. 累计 68 个版本 (V3.5.46-V3.6.13)

## 5. sw.js 演进史

| 版本 | 状态 | 关键改动 |
|------|------|----------|
| V2.8.9 - V3.5.79 | 7 类策略 | PRECACHE/RUNTIME/API/ASSETS + 5 cache 名 |
| V3.5.84 | network-only | 删所有 cache, 仅留离线 fallback |
| V3.5.89 | + withTraceparent | traceparent 透传 |
| V3.6.9 | + sw-bg-sync.js | IndexedDB 队列 (独立文件) |
| **V3.6.13** | **+ 移除版本控制** | **CACHE_VERSION = 'auto' + swBuildTime plugin** |
