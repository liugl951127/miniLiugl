# Day 41 Runbook — 2026-08-12

## 今日目标
基于 Day 40 的明日计划，推进 3 项功能：
1. RAG 后端 SSE 进度事件（真正的解析/切片/索引阶段）
2. 投票历史「重新投票」入口
3. Monitor 告警详情页面（RCA + 日志上下文）

---

## Task 1: RAG 后端 SSE 进度事件（⭐⭐⭐）

### 目标
RAG 上传时，后端真实推送 SSE 事件：文件上传完成 → 解析开始 → 切片完成 → 索引完成

### 后端改动
**`backend/modules/ai-service/src/main/java/com/minimax/rag/service/RagDocumentService.java`**
- 新增 `RagProgressEvent` 事件类：`{ stage: String, progress: int, message: String }`
- `uploadDocument()` 方法中各阶段调用 `emitProgress(stage, progress, message)`
- 使用 `SseEmitter` 推送（Spring WebFlux 的 `SseEmitter` 来自 spring-web，不是 webflux）
- 参考现有的 SSE 工具类或直接用 Controller 返回 `SseEmitter`

**`backend/modules/ai-service/src/main/java/com/minimax/rag/controller/RagController.java`**
- `POST /api/rag/upload` 改为返回 `SseEmitter`，推送进度事件
- 或者新增 `POST /api/rag/upload-stream` 返回 SseEmitter

### 前端改动
**`frontend/src/views/knowledge/Index.vue`**
- 已有 `uploadStage` 状态，需改为接收 SSE 事件驱动（而不是 JS setTimeout 模拟）
- 使用 `EventSource` 或 `fetch` + `ReadableStream` 接收 SSE
- 4 阶段进度真实化：upload→parse→chunk→index

---

## Task 2: 投票历史「重新投票」入口（⭐⭐）

### 目标
在投票历史列表中，每条记录加一个「重新投票」按钮，点击后跳转/弹窗发起新投票

### 前端改动
**`frontend/src/views/voting/Index.vue` 或 `VoteHistory.vue`**
- 每行加「重新投票」按钮
- 点击触发投票发起逻辑（可复用现有发起投票逻辑）
- 确认弹窗：「重新投票将使用当前模型配置」

### 后端改动
如果需要后端支持（如复制历史投票参数），新增接口：
**`backend/modules/ai-service/src/main/java/com/minimax/controller/AiVotingController.java`**
- `POST /api/ai/voting/duplicate/{recordId}`：根据历史记录复制参数发起新投票

---

## Task 3: Monitor 告警详情页面（⭐⭐⭐）

### 目标
点击 Monitor 的告警条目，弹出/跳转详情页面，显示 RCA + 日志上下文

### 前端改动
**`frontend/src/views/monitor/Index.vue`**
- 点击告警行打开详情弹窗
- 新增 `AlertDetail.vue` 或内嵌详情面板
- 详情内容：告警名称 / 触发时间 / 持续时间 / 当前值 / 阈值 / RCA 建议 / 最近相关日志

### 后端改动
**`backend/modules/monitor-service/src/main/java/com/minimax/monitor/controller/AlertController.java`**
- `GET /api/monitor/alerts/{alertId}`：获取告警详情（含关联日志上下文）
- `GET /api/monitor/alerts/{alertId}/logs`：获取该告警触发前后的日志片段

---

## 自检清单（Task 4）
- [ ] `bash scripts/self-check.sh` → ≥78/78
- [ ] `bash scripts/java-static-check.sh` → 0 错误
- [ ] `cd frontend && npm run build` → 通过
- [ ] API 路径前后端一致性检查
- [ ] SQL 脚本同步检查

---

## 输出文件
- `/workspace/minimax-platform/reports/day-41-report.md`
- 更新 `PROGRESS.md`（Day 41 ✅ + Day 42 待开始）
- `reports/next-day.txt` → 42
