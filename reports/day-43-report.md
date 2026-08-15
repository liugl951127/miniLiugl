# Day 43 Report — 2026-08-14

## ✅ Day 43 - RAG 检索高亮 + Monitor SLA 统计 + 投票邮件通知

**今日完成：**

### Task 1 ⭐⭐⭐ RAG 文档全文检索高亮

**后端改动（`backend/minimax-rag/src/main/java/.../retriever/Retriever.java`）：**
- 新增 `highlight(String content, String query, int window)` 静态方法：正则匹配 query 关键词，用 `<mark>` 标签包裹，窗口截取 240 字符
- `Hit` 类新增 `highlight` 字段 + `setHighlight(query)` 方法
- `retrieve()` 方法遍历 topK 结果时自动调用 `setHighlight(query)` 填充高亮字段

**前端改动（`frontend/src/views/knowledge/Index.vue`）：**
- 检索结果片段优先显示 `item.highlight`（含 v-html 渲染 `<mark>` 标签）
- 新增 `:deep(mark)` CSS：高亮词 `#fff3bf` 底色 + `#d48806` 橙色文字 + 粗体

### Task 2 ⭐⭐⭐ Monitor 告警 SLA 统计

**后端新增（`backend/minimax-monitor/src/main/java/.../service/AlertMetricsService.java`）：**
- 全新 SLA 统计服务，计算窗口可配置（默认 30 天）
- MTBF（Mean Time Between Failures）：平均故障间隔
- MTTR（Mean Time To Recover）：平均恢复时间（resolved 告警平均持续时长）
- 可用率：1 - (MTTR × 总告警数 / 窗口总时长)
- SLA 等级：A+ / A / B / C / D / F 按可用率分档
- 按严重程度分类统计（CRITICAL / WARNING / INFO）

**后端改动（`backend/minimax-monitor/src/main/java/.../controller/MonitorController.java`）：**
- 新增 `GET /monitor/alerts/sla?windowDays=30` 端点，返回 SLA 指标
- 新增 `AlertMetricsService` 依赖注入

**前端改动（`frontend/src/views/monitor/Index.vue`）：**
- 新增「SLA 统计」Tab，支持 7/30/90 天窗口切换
- 4 个指标卡片：SLA 等级（颜色分档）/ 可用率（含进度条）/ MTBF（小时）/ MTTR（分钟）
- 3 个统计卡片：总告警数 / 活跃告警 / 已恢复
- 按严重程度分布表格（CRITICAL/WARNING/INFO）
- 懒加载：切到 SLA tab 才请求数据

**前端 API（`frontend/src/api/monitor.js`）：**
- 新增 `getAlertSla(windowDays)` 函数 → `GET /monitor/alerts/sla`

### Task 3 ⭐⭐ 投票结果邮件通知

**后端实体（`backend/minimax-ai/src/main/java/.../entity/AiVotingRecord.java`）：**
- 新增 `notifyEmail` 字段（VARCHAR 255，可空）

**后端服务（`backend/minimax-ai/src/main/java/.../service/AiVotingService.java`）：**
- `saveVotingRecord()` 保存记录后，若 `notifyEmail` 非空则自动调用 `notifyVotingResult()`
- 新增 `notifyVotingResult(AiVotingRecord)` 方法：通过 RestTemplate HTTP 调用 notification-service 发邮件
- 新增 `buildVotingEmailBody(AiVotingRecord)` 构建邮件正文（问题/答案/策略/一致率/耗时）

**后端控制器（`backend/minimax-ai/src/main/java/.../controller/AiVotingRealController.java`）：**
- `POST /api/v1/ai/voting` 新增 `notifyEmail` 参数，透传给 `AiVotingRecord`
- 日志同时输出 `notifyEmail`

**SQL（`sql/minimax-v681-schema.sql`）：**
- `ai_voting_record` 表新增 `notify_email VARCHAR(255) NULL COMMENT '投票结束通知邮箱 (Day 43)'`
- INSERT 种子数据补 NULL 占位

**前端（`frontend/src/views/analytics/Index.vue`）：**
- 实时投票卡片新增「投票结束后通知邮箱」输入框
- `onRevote(vote)` 重新投票时若填了邮箱，POST /ai/voting 带 `notifyEmail` 参数提交新投票

### 自检全部通过

| 检查项 | 结果 |
|--------|------|
| self-check.sh | **13/13 ✅** |
| java-static-check.sh | **5/5 ✅**（0 错误，仅测试文件建议） |
| vite build | ✅（56.64s，1928 个模块） |

---

## 明日计划 Day 44

- [ ] RAG 文档全文阅读（点击搜索结果展开完整文档内容）
- [ ] Monitor 历史告警趋势图（折线图展示近 30 天告警趋势）
- [ ] 投票历史详情弹窗（展示各模型答案 + 置信度）
