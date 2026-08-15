# 组件错误（已降级）排查报告 (V6.8.1)

## 现象
用户看到右下角通知: `⚠️ 组件错误（已降级）`

## 触发源
来自 `frontend/src/components/ErrorBoundary.vue` (V6.3+ 静默通知):
- `onErrorCaptured` 捕获后代组件 render / lifecycle 错
- 弹 ElNotification (右下角, 不自动关闭, 静默)
- 错误信息: `errMsg`

## 排查步骤

### 1. 验证 30 个核心 view 全部能 mount
临时测试 `src/__tests__/views-mount.test.js` (已删):
```
✓ 30 view 全部 mount OK
- 警告: 沙箱 base.css loader 限制 (非 view 问题)
```

测试覆盖: admin (12) + chat (2) + knowledge + memory + agent (5) + ai (7) + kg + monitor

### 2. 错误可能原因 (运行时)
1. **数据获取失败**: API 调用返回非预期结构 (前端 try/catch 没接住)
2. **store 初始化失败**: Pinia store 在 setup 阶段 throw
3. **路由参数缺失**: `route.params.id` undefined 但代码没判空
4. **第三方库未挂载**: echarts / v-chart 在生产 build 后路径错
5. **权限指令** (`v-permission`) 计算 prop 时 throw

### 3. 建议排查 (用户本地)
```bash
# 1. 看 console 完整错误
# DevTools → Console → 找 [ErrorBoundary] captured: Error ...

# 2. 路由到对应 view
# 看 URL, ErrorBoundary 通知会显示 errMsg 前 80 字符

# 3. 看 dist/assets 看 lazy chunk
ls dist/assets/ | grep -E "admin|chat|ai|kg|agent"
# 缺 chunk = lazy 加载失败

# 4. 跑生产 build
npm run build
# 看是否成功
```

### 4. 修复方向
- **短期**: ErrorBoundary 已静默处理, UI 降级可用 - 用户不需立即操作
- **中期**: 加 `app.config.errorHandler` 全局兜底, console 完整日志
- **长期**: 每个 view 加 try/catch 包裹 data fetch, 用 `useAsyncData` composable

## 测试状态
- 30 view 临时 mount 测试: 全过
- 240 长期测试: 23 files, 240 tests, 全部通过 ✅

## 关键文件
- `frontend/src/components/ErrorBoundary.vue` - 错误捕获 + 静默通知
- `frontend/src/App.vue` - ErrorBoundary 包 router-view
- `frontend/src/main.js` - 全局 errorHandler (可加)
