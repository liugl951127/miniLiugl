# V3.6.13 sw.js 增量更新 + 移除版本控制

## 1. sw.js 完全重写 (447 → 270 行, -40%)

**移除**:
- CACHE_NAME = 'minimax-v{ver}' 模式
- CACHE_VERSION = 'v3.5.89' 等写死版本号
- PRECACHE_URLS 预缓存数组
- 5 个 Cache Storage 名字
- activate 时清理老 cache

**保留**:
- 消息协议: SKIP_WAITING / CLEAR_CACHE / GET_VERSION
- Push 通知 / Background Sync / Periodic Background Sync
- 离线 fallback (/offline.html)

## 2. vite-plugins/sw-build-time.mjs (新)

```js
export default function swBuildTime() {
  return {
    name: 'sw-build-time',
    transformIndexHtml: {
      order: 'post',
      handler(html) {
        const ts = Date.now()
        return html.replace(/src="\/sw\.js"/g, `src="/sw.js?v=${ts}"`)
      },
    },
    closeBundle() {
      // 替换 dist/sw.js __SW_BUILD_TIME__ → ISO 时间
    },
  }
}
```

## 3. 验证

vite build 0 错 ✅ 1m 2s
dist/sw.js SW_BUILD_TIME 注入 ✅
dist/index.html sw.js?v=ts ✅
ci-check 11/11 ✅
21 路由 21/21 200 ✅
ROUNDS=90 Round 6 ✅ 1890 GET 100% pass
simulate-login.sh 21 路由 ✅ 21/21
simulate-jwt.sh 21 路由 ✅ 21/21

## 4. 累计 68 个版本 (V3.5.46-V3.6.13)
