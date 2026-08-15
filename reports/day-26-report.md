# Day 26 Report — 2026-07-14

## ✅ Day 26 - 告警渠道端到端联调 / CI Vitest / Dashboard 懒加载 / E2E

**今日完成：**

### 1. Alerts.vue 告警渠道端到端联调修复 (Day 26)

**核心问题修复：**
- `deleteChannel()` 此前只在前端 filter，未调 API → 修复为调用 `monitorApi.deleteAlertChannel(id)`
- `newChannel()` 旧版只有简陋的 `ElMessageBox.prompt` → 替换为完整的渠道对话框（名称/类型/目标地址/收件人）
- `testChannel()` 加了 `testingId` loading 状态防重复点击
- 新增 `openChannelDialog(channel)` / `saveChannel()` / `editChannel()` 完整 CRUD
- 渠道表格新增「编辑」按钮
- 渠道类型下拉支持：dingtalk / email / feishu / wechat / webhook，对应占位符自动切换

**新增响应式字段：**
- `channelDialogVisible` / `editingChannel` / `savingChannel` / `testingId`
- `CHANNEL_TYPE_MAP` + `CHANNEL_TARGET_PLACEHOLDER` 常量映射

### 2. CI/CD 添加 Vitest Job (Day 26)

**新增 `frontend-unit` job (ci.yml)：**
- 依赖 `[frontend]`，需要前端先构建
- `npm run test:unit` → Vitest 单元测试
- `npm run test:unit:coverage` → 覆盖率报告
- 上传 `coverage/` 到 artifact（`vitest-coverage`）

**新增 `frontend-e2e` job (ci.yml)：**
- Playwright Chromium E2E 测试（E2E_BASE_URL: http://localhost:5173）
- 先 `npm run build` + `npx serve dist` 启动静态服务
- `npx playwright test --reporter=line,junit`
- 上传 `e2e-report/` 到 artifact（`playwright-report`）

**notify job 更新**：needs 追加 `frontend-unit` + `frontend-e2e`

### 3. Dashboard 图表懒加载 (Day 26)

**IntersectionObserver 懒加载：**
- 新增 `chartSectionRef` + `isChartsVisible` ref
- 图表默认显示 `<el-skeleton animated>` 骨架屏
- 进入可视区后 `isChartsVisible = true`，ECharts 才开始渲染
- `chartObserver.disconnect()` 避免重复触发
- `onUnmounted` 清理 observer，防止内存泄漏
- 非浏览器环境（SSR）fallback：直接 `isChartsVisible = true`

### 4. Playwright E2E 补充 channel CRUD 场景 (Day 26)

**新增文件：** `frontend/e2e/alerts-channel.spec.js`

| 测试用例 | 描述 |
|---|---|
| 告警页 4 个 Tab 正常显示 | 触发中/告警规则/通知渠道/历史记录 |
| 切换到通知渠道 Tab | 新建按钮 + 表格可见性 |
| 新建渠道 - 完整表单保存 | 名称/类型/目标地址 → 保存 |
| 编辑渠道 - 打开对话框并关闭 | 编辑按钮 → 编辑渠道对话框 → 取消 |
| 新建规则 - 完整表单 | 规则名 → 保存 |
| 告警规则 Tab 切换 | 表头列验证 |
| 历史记录 Tab 切换 | 表头列验证 |
| 触发中 Tab 空状态 | alert-card 或 el-empty 显示 |

---

**自检结果：**
- 前端构建 (`npm run build`): ✅ 1m 15s
- Java 静态检查：4 TODO 历史遗留 + 5 System.out 历史遗留（非今日引入）
- CI/CD Vitest/E2E job: ✅ 已加入 ci.yml

**代码量：**
- `Alerts.vue` 重构 (14KB)
- `Dashboard.vue` 懒加载 (IntersectionObserver)
- `ci.yml` 新增 2 个 job
- `frontend/e2e/alerts-channel.spec.js` 新增 (4.6KB)

---

## Day 27 - 待开始

**待做：**
- [ ] 告警触发引擎 (AlertEngine) 接入真实指标源
- [ ] 告警渠道的 test 端点后端实现
- [ ] 告警历史记录 API (`/monitor/alerts/history`)
- [ ] Playwright E2E CI: 前端 dev server 启动脚本优化
- [ ] WebSocket 告警实时推送 (告警触发后前端即时通知)
