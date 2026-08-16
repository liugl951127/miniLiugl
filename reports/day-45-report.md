# Day 45 Report — 2026-08-16

## ✅ Day 45 - RAG 文档在线编辑 + Monitor 告警升级策略 + 前端性能优化

### 自检全部通过

| 检查项 | 结果 |
|--------|------|
| self-check.sh | **13/13 ✅** |
| java-static-check.sh | **5/5 ✅**（0 错误，仅测试文件建议）|
| vite build | ✅（1m 7s，1928+ 个模块）|

---

### Task 1 ⭐⭐⭐ RAG 文档在线编辑（修改内容 + 重新切片 + 重新索引）

**后端（`minimax-rag`）：**

`DocumentService.updateDocContent(Long docId, Long ownerId, String newContent)`:
- 归属校验（ownerId 匹配）
- 更新 `document.content` 和 `document.sizeBytes`
- 删除旧切片（`chunkMapper.deleteByDoc`）
- 重新切片（`TextChunker.chunk`）
- 重新向量化（`EmbeddingClient.embed`）并写入新 chunk
- 更新 document 状态 + chunkCount
- 调整所属 KB 的 chunk 计数

**RagController 新端点：**
- `PUT /api/v1/rag/doc/{id}/content` — 在线编辑（Day 45）
  - body: `{ "content": "新的文档正文..." }`
  - 返回更新后的 Document 实体

**前端（`knowledge/Index.vue`）：**
- 文档列表新增「**EditPen**」按钮（编辑内容）
- 新增**文档在线编辑弹窗**（860px）：
  - 元信息展示（名称/当前切片数/原大小）
  - Warning Alert 提示：修改后将重新切片索引
  - 可编辑 textarea（18行，等宽字体）
  - 字数统计 + 预估切片数
  - 进度条 + 保存按钮

**前端（`rag.js`）：**
- 新增 `updateDocContent(docId, ownerId, newContent)` API 函数

---

### Task 2 ⭐⭐⭐ Monitor 告警升级策略

**后端（`minimax-monitor`）：**

**实体新增字段：**
- `AlertRule`: `escalateAfterMinutes`（升级等待分钟）/ `escalationChannel`（升级渠道）/ `autoResolveMinutes`（自动恢复分钟）
- `AlertEvent`: `escalated`（是否已升级）/ `escalatedAt`（升级时间）

**AlertEngine 新增定时任务（每 60s）：**
- `checkEscalation()`: 遍历所有 firing 告警
  - CRITICAL 级别 + 未升级 + 触发时间超过 `escalateAfterMinutes` → 升级
  - 标记 `escalated=true` + 更新消息含 `⚠️【已升级】`
  - 触发升级通知（支持单独配置升级渠道）
  - SSE 广播升级事件

**updateRule() 方法扩展：**
- 支持 `escalateAfterMinutes` / `escalationChannel` / `autoResolveMinutes` 字段更新

**SQL Schema（`minimax-v681-schema.sql`）：**
- `alert_rule`: 新增 `escalate_after_minutes` / `escalation_channel` / `auto_resolve_minutes` 列
- `alert_event`: 新增 `escalated` (TINYINT) / `escalated_at` (TIMESTAMP) 列

**前端（`monitor/Index.vue`）：**
- 新增「**告警规则**」Tab（紧接在通知渠道之后）
  - 规则列表（ID/名称/指标/条件/阈值/级别/升级策略/状态/操作）
  - 支持新建 / 编辑 / 启用禁用 / 删除规则
- **规则编辑弹窗**（580px）新增升级配置区：
  - 升级等待时间（分钟，0=不升级）
  - 升级通知渠道（多选）
  - 自动恢复时间（分钟，0=不自动恢复）

**前端 API（`monitor.js`）：**
- 新增 `getAllAlertRules()` → `GET /api/v1/monitor/alerts/rules/all`（含禁用规则）

---

### Task 3 ⭐ 前端性能优化

**路由懒加载：** 已确认所有路由均使用 `() => import('@/views/...')` 动态导入，无需修改 ✅

**NL2SQL 结果表格分页（`analytics/Index.vue`）：**
- 新增 `nlPage` / `nlPageSize=20` / `nlPaginatedRows` computed
- 表格数据改为 `nlPaginatedRows`（分页切片）
- 结果超过 10 条时显示 `el-pagination` 控件

---

## 明日计划 Day 46

- [ ] RAG 多文档批量编辑 + 批量重新索引
- [ ] Monitor 告警自动恢复（auto-resolve）定时任务完善
- [ ] 前端图片懒加载优化
- [ ] V4.4 Release 打包 + CHANGELOG 更新
