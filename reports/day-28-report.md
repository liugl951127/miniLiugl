# Day 28 Report — 2026-07-14

## ✅ Day 28 - 告警通知模板 / Ack 写库 / 审计日志资源类型筛选

**今日完成：**

### 1. 告警通知模板 + 变量替换 (Day 28)

**新增 `AlertTemplateResolver`（monitor/service）：**
- 支持 8 个变量: `${ruleName}` `${severity}` `${metricName}` `${metricValue}` `${threshold}` `${message}` `${firedAt}` `${service}`
- 正则 `Pattern` 匹配 `${变量}` 并替换为实际值
- 未识别变量原样保留（不报错）
- 默认模板兜底（模板字段为空时使用）

**AlertChannel 新增 `template` 字段：**
- 允许用户自定义每条渠道的通知文本模板
- 支持钉钉/邮件/飞书/企微/自定义 Webhook 全部类型

**AlertNotifier 接口新增 `send(event, config, resolvedText)` 重载：**
- `default` 实现调用原 `send(event, config)`，向后兼容
- DingTalkAlertNotifier / EmailAlertNotifierOverride 此方法，resolvedText 非空时用模板替换默认格式
- DingTalk 将模板文本转为 Markdown 格式（加 `>` 引用前缀）

**AlertNotifierManager `sendTest()` 升级：**
- 调用 `templateResolver.resolve(testEvent, channel.getTemplate())` 生成模板文本
- 传入 notifier `send(event, config, resolvedText)`

**前端 Alerts.vue 渠道对话框新增模板编辑字段：**
- textarea（3行），placeholder 说明可用变量
- 底部变量提示：`${ruleName} ${severity} ${metricName} ${metricValue} ${threshold} ${message} ${firedAt}`
- 保存时同步 `template` 字段

### 2. Acknowledge 端点真正写数据库 (Day 28)

**MonitorController `acknowledgeAlert()` 重构：**
- 先查 `alertEventMapper.selectById(id)` 验证事件存在
- 不存在返回 `Result.fail()`
- 存在则设置 `status="acked"` + `ackedAt=now()` + `updateById()`
- 移除 mock 直接返回 true

### 3. 审计日志按资源类型筛选 (Day 28)

**AuditController `/recent` + `/export` 新增 `resourceType` 参数：**
- `QueryWrapper.eq("resource_type", resourceType)` 精确匹配
- `resourceType` 为空/空白时跳过筛选

**前端 Audit.vue 新增资源类型下拉筛选器：**
- 支持: user / chat / model / agent / file / config / api_key
- `filters.resourceType` 同步到 `loadLogs()` 参数
- `resetFilters()` 重置时包含 resourceType

---

**自检结果：**
- 前端构建 (`npm run build`): ✅ 1m 10s

**代码量：**
- `AlertTemplateResolver.java` 新增（模板解析器）
- `AlertChannel.java` 新增 `template` 字段
- `AlertNotifier.java` 新增 `send(event, config, text)` 重载
- `DingTalkAlertNotifier.java` 新增 `send(event, config, text)` + `buildBodyWithText()`
- `EmailAlertNotifier.java` 新增 `send(event, config, text)`
- `AlertNotifierManager.java` 注入 `AlertTemplateResolver` + `sendTest()` 使用模板
- `MonitorController.java` 重构 `acknowledgeAlert()` + 注入 `AlertEventMapper`
- `AuditController.java` 新增 `resourceType` 筛选参数
- `Alerts.vue` 渠道对话框新增模板字段
- `Audit.vue` 新增资源类型筛选下拉

---

## Day 29 - 待开始

**待做：**
- [ ] 告警规则关联具体服务（微服务下拉 + 标签筛选）
- [ ] 告警恢复通知（resolved 时再发一次通知）
- [ ] Prometheus 端点按指标类型过滤输出
- [ ] 告警统计面板（饼图：按 severity 分布 / 按 service 分布）
