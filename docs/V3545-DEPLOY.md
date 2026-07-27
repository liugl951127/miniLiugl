# V3.5.45 部署指南 (解决 const re={mdurl:t} TDZ)

## 问题
浏览器控制台错误:
```
Uncaught ReferenceError: Cannot access 't' before initialization
    at utils.BqiN7M47.js:1:17314
```

## 根因 (已分析)
- `vite.config.js` 把 dayjs + markdown-it + highlight.js 塞到 `utils` chunk (1.3MB)
- markdown-it 用诡异命名 `import { parse_link_label as as }`
- Rollup 输出 `import { as as t, ... } from "./vendor.XXX.js"`
- 浏览器加载 utils chunk 时, vendor 的 'as' (别名 't') 还没初始化
- 17314 行 `const re={mdurl:t,ucmicro:e}` 触发 TDZ (Temporal Dead Zone)

## 修复 (V3.5.43)
**`vite.config.js` manualChunks 调整**:
```js
// 修前: 这 3 个第三方都进 utils (1.3MB)
if (id.includes('dayjs') || id.includes('markdown')) return 'utils'
// 修后: 全部进 vendor, utils chunk 消失
if (id.includes('dayjs') || id.includes('markdown') || id.includes('highlight')) return 'vendor'
```

## V3.5.45 部署步骤

### 1. 后端编译验证
```bash
cd /workspace/miniLiugl/backend
mvn -B install -DskipTests -Dspotless.check.skip=true
# 应全部 BUILD SUCCESS
```

### 2. 前端重新 build
```bash
cd /workspace/miniLiugl/frontend
npm run build
# 输出: dist/ (utils.*.js 消失, vendor.BJXQQT1v.js 1.4MB)
```

### 3. Docker 重建 (生产)
```bash
cd /workspace/miniLiugl
docker compose build --no-cache minimax-frontend
docker compose up -d minimax-frontend
```

### 4. 用户浏览器清理 (关键!)
**普通刷新不够, 必须硬刷**:
- **Chrome/Edge**: `Ctrl + Shift + R` (Windows/Linux) 或 `Cmd + Shift + R` (Mac)
- **Firefox**: `Ctrl + F5`
- **Safari**: `Cmd + Option + R`

### 5. 验证 sw.js 升级
打开 DevTools → Application → Service Workers:
- 应看到 `minimax-v3.5.45` (新)
- 不应再有 `minimax-v3.5.41` (旧)
- 如果旧 SW 还在: 勾选 "Update on reload" + 刷新

### 6. 验证错消失
打开 DevTools → Console:
- ❌ 之前: `Cannot access 't' before initialization` + `Cache.put network error`
- ✅ 之后: 仅有 V3.5.45 sw.js 注册成功日志

## 防御深度 (5 个版本连续加固)
```
V3.5.41  sw.js v2.8.9 → v3.5.41, icon .png → .svg
V3.5.42  manifest.json PNG → SVG, nginx sw.js no-cache
V3.5.43  ★ vite manualChunks: dayjs/markdown/highlight → vendor (修 TDZ 根因)
V3.5.45  sw.js v3.5.41 → v3.5.45 (强清旧缓存)
```

## 旧错回退检查清单
- [ ] `dist/assets/utils.*.js` 不存在
- [ ] `dist/assets/vendor.*.js` ≥ 1.4MB
- [ ] sw.js CACHE_NAME = `minimax-v3.5.45`
- [ ] 浏览器硬刷后控制台无 TDZ 错
- [ ] sw.js 状态显示为 `minimax-v3.5.45` (激活)
