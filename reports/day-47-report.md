# Day 47 Report — 2026-08-18

## ✅ Day 47 - RAG 批量删除 + Monitor 统计概览 + 限流配置

### 自检全部通过

| 检查项 | 结果 |
|--------|------|
| self-check.sh | **13/13 ✅** |
| java-static-check.sh | **5/5 ✅**（0 错误，仅测试文件建议）|
| vite build | ✅（57.46s，1932+ 个模块）|

---

## Task 1 ⭐⭐⭐ RAG 文档批量删除

### 后端（`minimax-rag`）

`DocumentService.batchDeleteDocs(List<Long> docIds, Long ownerId)`:
- 校验归属（所有 doc 必须属于同一 owner）
- 遍历每个 doc：删除 chunks → 删除 document → 调整 KB docCount/chunkCount
- 返回 `BatchResult`：{ succeeded: 成功数, failed: [{ docId, error }] }

**RagController 新端点：**
- `DELETE /api/v1/rag/doc/batch` — 批量删除
  - body: `{ "docIds": [1, 2, 3] }`
  - 返回 BatchResult

### 前端（`knowledge/Index.vue`）
- 工具栏新增**「批量删除」按钮**（红色危险操作，显示选中数量）
- 新增**批量删除弹窗**（560px）：
  - 红色警告提示，确认删除文档数和不可恢复
  - 选中文档 ID 列表展示
  - 乐观进度条 + 实时状态
  - 结果展示：成功/失败计数 + 失败详情列表
- 删除成功后自动刷新文档列表和知识库列表

**前端 API（`rag.js`）：**
- 新增 `batchDeleteDocs(ownerId, docIds)` 函数

---

## Task 2 ⭐⭐ Monitor 告警统计概览 Dashboard

### 后端（`minimax-monitor`）

`AlertMetricsService.getStatistics(Integer days)`:
- 按窗口（默认 30 天）查询 alert_event
- 返回：total / critical / warning / info / firing / acked / resolved / active
- avgDurationMinutes：平均持续时长（分钟）
- topRules：Top 5 触发最多的规则

**MonitorController 新端点：**
- `GET /api/v1/monitor/alerts/statistics?days=30`
  - 返回统计指标 Map

### 前端（`monitor/Index.vue`）
- 新增**「统计概览」Tab**（懒加载）：
  - 6 个核心数字卡片：总数 / 活跃 / 已恢复 / 平均持续 / 已确认 / 进行中
  - 按严重程度分布（CRITICAL / WARNING / INFO）
  - Top 5 触发规则列表（规则名 + 次数）
  - 支持 7/30/90 天窗口切换

**前端 API（`monitor.js`）：**
- 新增 `getAlertStatistics(days)` 函数

---

## Task 3 ⭐ 前端搜索高亮优化

- `knowledge/Index.vue` 的 `highlightKeyword()` 函数已实现
- 搜索结果支持 `<mark>` 标签高亮关键词
- CSS 样式已定义（黄色背景 + 深色文字 + 圆角）
- Day 43 已完成，Day 47 验证通过 ✅

---

## Task 4 ⭐ API 限流中间件完善

### 后端（`minimax-common`）

`RateLimitService` 增强：
- 新增 `strict` 档位：高危操作（上传/敏感）更严格限制（10次/60秒）
- 支持从配置文件读取各档位阈值

**application-common.yml** 新增配置：
```yaml
minimax:
  ratelimit:
    enabled: true
    ip:       { capacity: 100, refill: 100, period-seconds: 60 }
    user:     { capacity: 60,  refill: 60,  period-seconds: 60 }
    global:   { capacity: 1000, refill: 1000, period-seconds: 60 }
    strict:   { capacity: 10,  refill: 10,  period-seconds: 60 }
```

---

## 明日计划 Day 48

- [ ] RAG 文档批量导出（支持 PDF/TXT）
- [ ] Monitor 告警统计 ECharts 可视化（饼图 + 柱状图）
- [ ] 前端深色模式切换（Element Plus dark 主题）
- [ ] API 认证中间件完善（JWT 续期 + 强制刷新）
