# Day 27 Report — 2026-07-14

## ✅ Day 27 - 告警渠道真发送 / history 查真实表 / E2E CI 优化 / WebSocket 实时推送

**今日完成：**

### 1. 告警渠道 test 端点真正发送 (Day 27)

**AlertNotifierManager 新增 `sendTest(AlertChannel)` 方法：**
- 构建测试告警事件 `AlertFiredEvent` (severity=info, message=测试消息)
- 根据 channelType 路由到对应 notifier (dingtalk/email/feishu/wechat/webhook)
- config 优先用 `channel.config`，否则用 `channel.target`

**MonitorController `testAlertChannel()` 重构：**
- 先查渠道是否存在 (`alertChannelService.getById`)
- 不存在返回 400 错误
- 存在则调用 `notifierManager.sendTest(ch)`
- 异常捕获并返回友好错误信息

### 2. 告警历史 API 查真实 alert_event 表 (Day 27)

**MonitorController `getAlertHistory()` 重构：**
- 旧版：返回 mock 循环数据
- 新版：调用 `alert.recentEvents()` 查真实 `alert_event` 表
- 过滤掉 `firing` 状态（只返回已解决的历史）
- 映射字段：id / ruleName / severity / status / message / firedAt / resolvedAt / ackedAt / duration

### 3. CI E2E 启动脚本优化 (Day 27)

**ci.yml `frontend-e2e` job 重写：**
- 移除错误的 `services` 块（GitHub Actions services 容器不能直接跑 steps）
- 改用 `npm run preview` 后台启动 + 端口就绪轮询（最多 30s）
- 用 `vite preview` 而非 `npx serve`（更轻量，无需额外依赖）
- 修正端口 4173（package.json preview 默认端口）
- E2E_BASE_URL 同步更新为 `http://localhost:4173`
- 测试结束后 `kill $PREVIEW_PID` 清理进程

### 4. WebSocket 告警实时推送 (Day 27)

**新增 `AlertStreamRegistry`（monitor/config）：**
- `SseEmitter` 列表管理（CopyOnWriteArrayList 线程安全）
- `register(SseEmitter)` / `unregister(SseEmitter)` 管理连接生命周期
- `broadcast(AlertEvent event)` 向所有在线前端推送 JSON 告警事件

**新增 `MonitorController /alerts/stream` (SSE 端点)：**
- `produces = MediaType.TEXT_EVENT_STREAM_VALUE`
- 超时时间 `Long.MAX_VALUE`（长连接）
- 自动清理断开的 emitter
- 首次连接发送 `ping` 心跳

**AlertEngine 触发时广播：**
- 告警触发后调用 `streamRegistry.broadcast(e)`
- 失败不阻断主流程（catch 吞异常）

**前端 Alerts.vue SSE 订阅：**
- `onMounted` 时创建 `EventSource('/api/v1/monitor/alerts/stream')`
- 监听 `alert` 类型事件，解析 JSON
- 新告警插入 `firing` 列表顶部（最多 50 条）
- 当前 Tab 非 firing 时顶部弹 `ElMessage.warning` 提示
- `onUnmounted` 自动 `close()` 清理连接

---

**自检结果：**
- 前端构建 (`npm run build`): ✅ 1m 14s

**代码量：**
- `AlertNotifierManager.java` 新增 `sendTest()` 方法
- `MonitorController.java` 重构 `testAlertChannel` + `getAlertHistory` + 新增 `/alerts/stream`
- `AlertStreamRegistry.java` 新增（配置类）
- `AlertFiredEvent.java` 新增（事件类）
- `AlertEngine.java` 注入 `AlertStreamRegistry` + 触发时广播
- `ci.yml` 重写 `frontend-e2e` job
- `Alerts.vue` 新增 SSE 订阅逻辑

---

## Day 28 - 待开始

**待做：**
- [ ] 告警规则关联具体服务（微服务下拉 + 标签筛选）
- [ ] 告警通知模板（支持变量替换：${ruleName} / ${metricValue} / ${threshold}）
- [ ] 告警恢复确认（acknowledge → resolved 手动确认流）
- [ ] 监控面板接入 Prometheus（/monitor/forward-prometheus 端点完善）
- [ ] 审计日志按资源类型筛选
