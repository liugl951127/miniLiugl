# V3.6.14 前端错误检查 + 修 sw.js 图标

## 1. V3.6.13 之后

V3.6.13 sw.js 移除版本控制 + swBuildTime plugin。V3.6.14 继续前端错误排查:
- **`scripts/frontend-error-check.sh`** (新) - 综合检查 21 路由 + 关键 asset + SW 内容
- **修 sw.js icon-192.png → .svg** (公共目录只有 .svg, .png 404)
- **0 错误 0 警告 21/21 路由** ✅

## 2. V3.6.14 改

### 2.1 scripts/frontend-error-check.sh (新)

5 段检查:
1. **21 路由** - 全部 200 验证 (`?demo=1` 演示模式)
2. **关键 asset** - sw.js / favicon.svg / offline.html / manifest.json / icons/icon-192.svg
3. **Service Worker 内容** - SKIP_WAITING / GET_VERSION / Background Sync / Periodic / icon-192.svg
4. **HTML script 引用** - 抓首页所有 `<script src="...">` 验证 200
5. **总结** - $ERRORS 错误 / $WARNINGS 警告

```bash
$ bash scripts/frontend-error-check.sh
═══════════════════════════════════════════════════════════
  V3.6.14+ 前端错误检查
═══════════════════════════════════════════════════════════

--- 21 路由 ---
  ✓ 21/21 通过

--- 关键 asset ---
  ✓ /sw.js
  ✓ /favicon.svg
  ✓ /offline.html
  ✓ /manifest.json
  ✓ /icons/icon-192.svg

--- Service Worker 内容检查 ---
  ✓ SKIP_WAITING 消息协议
  ✓ GET_VERSION 消息协议
  ✓ Background Sync
  ✓ Periodic Background Sync
  ✓ 图标引用 .svg (PWA 标准)

═══════════════════════════════════════════════════════════
  总结: 0 错误, 0 警告, 21/21 路由
═══════════════════════════════════════════════════════════
  🎉 无致命错误
```

### 2.2 sw.js icon-192.png → .svg (修复 404)

**问题**: V3.5.84+ sw.js 写死 `icon: '/icons/icon-192.png'`, 但 `public/icons/` 只有 `.svg` 文件, 导致 PWA 通知图标 404

**修法**:
```diff
- icon: '/icons/icon-192.png'
- badge: '/icons/badge-72.png'
+ icon: '/icons/icon-192.svg'
+ badge: '/icons/icon-192.svg'  // 沙箱用同一个
```

**PWA 标准**: 用 `.svg` 而非 `.png` 更现代, 适配高 DPI 屏

## 3. 验证

| 测试 | 结果 |
|------|------|
| **frontend-error-check.sh** | ✅ 0 错误 0 警告 |
| 21 路由 21/21 200 | ✅ |
| 5 关键 asset 200 | ✅ |
| sw.js 消息协议 5/5 | ✅ |
| 演示模式 `?demo=1` | ✅ 跳过 auth |
| vite dev server | ✅ 1.6s 启动 |
| ci-check 11/11 | ✅ < 3s |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |

## 4. 累计 69 个版本 (V3.5.46-V3.6.14)
