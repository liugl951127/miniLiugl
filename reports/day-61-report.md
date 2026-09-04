# Day 61 Report — 2026-09-04

## 自检结果

| 检查项 | 结果 |
|--------|------|
| self-check (13/13) | ✅ |
| java-static-check (5/5) | ✅ (1 warning: test @Autowired 字段) |
| vite build (frontend) | ✅ 41.43s |

---

## 今日完成

### 1. RCA 知识库导出 CSV 功能 ✅

**目标**: 在「已保存 RCA」Tab 提供导出按钮，将知识条目批量导出为 CSV 文件。

**后端新增**（`MonitorController.java` V Day 61）：
- `GET /monitor/alerts/rca/knowledge/export` — 查询 RCA 知识条目，按 CSV 格式输出
- CSV 列：ID / 告警ID / 指标名称 / 规则名 / 级别 / 根因分类 / 根因 / 置信度 / 分析方法 / 保存人 / 保存时间 / 建议操作 / 历史经验
- `escapeCsv()` 方法处理含逗号/双引号/换行的字段，防止 CSV 格式破坏
- `Content-Disposition: attachment; filename=rca-knowledge.csv` 触发浏览器下载

**前端新增**（`monitor.js` V Day 61）：
- `exportRcaKnowledgeCsv(params)` → `GET /monitor/alerts/rca/knowledge/export`，`responseType: 'blob'`
- 已加入 `createMonitorApi()` 导出对象

**前端新增**（`Alerts.vue` V7.11）：
- 「已保存 RCA」工具栏新增「导出 CSV」按钮（`Download` 图标）
- `exportRcaCsv()` 函数：调用 API → `URL.createObjectURL()` → `<a download>` 触发下载
- `exportCsvLoading` 状态防止重复点击

---

### 2. 告警批量确认/解决 ✅

**目标**: 在「活跃告警」Tab 支持 checkbox 多选 + 批量确认/批量解决。

**后端新增**（`MonitorController.java` V Day 61）：
- `POST /monitor/alerts/batch/ack` — 批量确认，接收 `List<Long>`，返回 `{ total, succeeded }`
- `POST /monitor/alerts/batch/resolve` — 批量解决，同上

**后端补全**（`MonitorController.java` V Day 61）：
- `POST /monitor/alerts/{id}/resolve` — 单个告警解决（之前缺失，前端已有调用）

**前端新增**（`monitor.js` V Day 61）：
- `resolveAlert(id)` → `POST /monitor/alerts/${id}/resolve`
- `batchAcknowledge(ids)` → `POST /monitor/alerts/batch/ack`
- `batchResolve(ids)` → `POST /monitor/alerts/batch/resolve`
- 已加入 `createMonitorApi()` 导出对象

**前端新增**（`Alerts.vue` V7.11）：
- `<el-table-column type="selection" width="40" />` — 多选列
- 工具栏显示选中数量 + 「批量确认」/「批量解决」按钮（`selectedActiveAlerts` 长度 > 0 时显示）
- `@selection-change="onActiveSelectionChange"` → 更新 `selectedActiveAlerts`
- `doBatchAck()` / `doBatchResolve()` — 调用 API，成功后清空选择并刷新

---

### 3. Bug 修复：ackAlert API 调用 ✅

**问题**: Alerts.vue 调用 `monitorApi.ackAlert()` 但 `monitor.js` 中函数名为 `acknowledgeAlert`，导致「确认」按钮失效。

**修复**: `monitorApi.ackAlert(row.id)` → `monitorApi.acknowledgeAlert(row.id)`

---

## 改动文件

| 模块 | 文件 | 改动 |
|------|------|------|
| 后端 | `MonitorController.java` | CSV导出 + 单个resolve + 批量ack/resolve (Day 61) |
| 前端 | `api/monitor.js` | 新增4个API函数，修复ackAlert引用 |
| 前端 | `views/monitor/Alerts.vue` | 批量操作UI + CSV导出按钮 + 修复ackAlert (V7.11) |

---

## 明日计划 Day 62

- [ ] MiniMax 大模型平台日常维护（前端语法检查 / API 路径一致性）
- [ ] 前端 API 路径与后端 Controller 一致性专项扫描
- [ ] 持续集成脚本完善
