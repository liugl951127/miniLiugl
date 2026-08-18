# Runbook Day 47 — 2026-08-18

## 今日目标
RAG 批量删除 + Monitor 统计 Dashboard + 前端搜索高亮优化 + API 限流完善

---

## Task 1 ⭐⭐⭐ RAG 文档批量删除

### 后端（minimax-rag）
- `DocumentService.batchDeleteDocs(List<Long> docIds, Long ownerId)`
  - 校验归属
  - 删除 chunks（先删切片再删文档）
  - 删除文档记录
  - 返回 `{ succeeded: N, failed: [...] }`
- `RagController`: `DELETE /api/v1/rag/doc/batch`
  - body: `{ "docIds": [1,2,3] }`
  - 返回 BatchResult

### 前端（knowledge/Index.vue）
- 工具栏新增「批量删除」按钮（显示选中数量）
- 批量删除弹窗：二次确认 + 文档名称列表
- 删除后刷新表格（重新 fetchList）
- 前端 API（rag.js）：`batchDeleteDocs(ownerId, docIds)`

### SQL
- 如需新字段，检查 schema，无需则跳过

---

## Task 2 ⭐⭐ Monitor 告警统计 Dashboard

### 后端（minimax-monitor）
- `AlertStatisticsController`: `GET /api/v1/monitor/alerts/statistics`
  - 返回：{ total, critical, warning, info, resolved, firing, avgDurationMinutes, topRules }
- `AlertEventMapper`: 聚合查询支持（按 severity 统计、按日聚合）
- `AlertEventService.getStatistics(days)` 业务方法

### 前端（monitor/Index.vue）
- 新增「统计」Tab 或卡片区
- el-card 展示：总数 / CRITICAL / WARNING / INFO / 已恢复 / 进行中
- 或 ECharts 饼图 + 数字卡片

---

## Task 3 ⭐ 前端搜索高亮优化

### 后端（minimax-rag）
- `DocumentService.search(query, ownerId, topK)` 已支持 highlight，返回高亮片段

### 前端（knowledge/Index.vue）
- 搜索结果 v-html + `<mark>` 标签渲染高亮
- 已有 highlight，验证是否正确渲染

---

## Task 4 ⭐ API 限流中间件完善

### 后端（minimax-common）
- 检查现有 `RateLimitInterceptor` 实现
- 补充注释 / 配置化（从配置文件读取限流阈值）
- 扩展支持不同端点不同阈值

### 配置
- `application.yml`: `rate-limit.enabled: true`, `rate-limit.default-limit: 100`
