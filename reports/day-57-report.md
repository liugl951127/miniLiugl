# Day 57 Report — 2026-08-31

## 自检结果

| 检查项 | 结果 |
|--------|------|
| self-check (13/13) | ✅ |
| java-static-check (5/5) | ✅ (1 warning: test @Autowired 字段) |
| vite build (frontend) | ✅ 45.57s（pnpm install + build） |

---

## 今日完成

### 1. Monitor 知识库 → RCA 分析联动 ✅

**目标**: Alerts.vue 知识库 Tab 每行增加「触发 RCA」按钮，点击自动触发同类告警 RCA 根因分析并打开 RCA 抽屉。

**后端新增**（`MonitorController.java` V7.8）：
- 新增 `GET /api/v1/monitor/alerts/rca/by-metric` endpoint
- 接收参数：`metricName`（必填）、`severity`（可选）、`historyDays`（默认 30）
- 逻辑：优先找最近一条 `firing` 态同类告警 → 无则找最近任意态 → 调用现有 RCA 分析引擎
- 返回格式与 `/alerts/{id}/rca` 完全一致，前端无需额外适配

**后端新增**（`AlertEventMapper.java`）：
- 新增 `selectLatestByMetric(metricName, severity, status)` default 方法，查找最近一条指定条件的告警

**前端新增**（`monitor.js` V7.8）：
- `rcaAnalysisByMetric(metricName, severity, historyDays)` → `GET /monitor/alerts/rca/by-metric`
- 已加入 `createMonitorApi()` 导出对象

**前端新增**（`Alerts.vue` V7.8）：
- 知识库 Tab 表格新增「操作」列（100px），每行显示「触发 RCA」按钮
- `triggerRcaFromKb(entry)` 函数：调用 `rcaAnalysisByMetric` → 填充 `rcaResult` → 打开 RCA 抽屉
- `rcaFromKbLoading` 状态（key = metricName，防止多按钮同时 loading）
- RCA 抽屉复用已有组件，来源标识为 `by-metric`

**改动文件**: 后端 2 文件（MonitorController.java / AlertEventMapper.java）、前端 2 文件（monitor.js / Alerts.vue）

---

### 2. RAG 检索结果排序维度切换 ✅

**目标**: KbList.vue 检索测试 tab 增加「相关性 / 时效性 / 权威性」三档排序维度切换。

**后端新增**（`Retriever.java` V7.8）：
- `retrieve()` 方法新增 `sortBy` 参数（`String sortBy = "relevance"`）
- 三种排序模式：
  - `relevance`（默认）：相关性分数 + 时效性加权 `(1-γ)*sim + γ*recency`
  - `timeliness`：`recencyScore` 单独排序（越新越前）
  - `authority`：文档大小归一化 + 相关性加权 `sizeBytes/10MB` 作为权威分
- `retrieveMultiKb()` 同步支持 `sortBy` 参数
- `retrieveRerank()` 首轮候选检索也传入 `sortBy`

**后端新增**（`RagController.java` V7.8）：
- `/retrieve`、`/retrieve/multi`、`/retrieve/rerank` 三个 endpoint 全部接受 `sortBy` 参数
- 参数默认值 `"relevance"`，向后完全兼容

**前端新增**（`KbList.vue` V7.8）：
- 检索面板新增「排序维度」行（`el-radio-group`，位于提示模板下方）
- 三个选项：相关性 / 时效性 / 权威性
- `retrieveSortBy` 状态变量，`doRetrieve()` 自动将 `sortBy` 传入请求 body
- 检索结果头部：非 `relevance` 模式时显示对应彩色标签（⏰ 时效性 / 🏆 权威性）

**改动文件**: 后端 2 文件（Retriever.java / RagController.java）、前端 1 文件（KbList.vue）

---

## 前后端一致性扫描

| 检查项 | 结果 |
|--------|------|
| monitor API 路径匹配 | ✅ `/monitor/*` → `/api/v1/monitor/*`（http.js 自动前缀） |
| rag API 路径匹配 | ✅ `/rag/*` → `/api/v1/rag/*` |
| 新增 rcaAnalysisByMetric 导出 | ✅ 已加入 createMonitorApi() |
| Alerts.vue import | ✅ monitorApi 正确引用 |
| KbList.vue import | ✅ rag.js 函数正确引用 |

---

## 关键数据

| 指标 | 值 |
|------|-----|
| 新增 Java 类/方法 | 1 个 endpoint + 1 个 mapper default 方法 |
| 新增前端组件 | 1 个操作列 + 1 个排序维度选择器 |
| 自检通过率 | 13/13 ✅ |
| Java 静态检查 | 5/5 ✅ |
| vite build | 45.57s ✅ |

---

## 明日计划 Day 58

- [ ] Monitor RCA 分析结果一键转知识库条目（将 RCA 分析结果写入告警知识库）
- [ ] RAG 检索结果支持按文档类型筛选（PDF/Word/TXT/MD）
- [ ] MiniMax 大模型平台日常维护（前端语法检查 / API 路径一致性）
