# Day 36 Runbook — 2026-08-07

## 任务来源
Day 35 明日计划遗留 + V4.3 收尾

## 今日任务（按优先级）

### Task 1 ⭐ AiChat SSE Reconnect 逻辑
**问题**：当前 `chatStream()` 一旦网络断开就直接报错，没有重试逻辑，用户体验差
**改动**：
- `frontend/src/api/ai.js` 的 `chatStream()` 函数增强：
  - 自动重连最多 2 次（指数退避 1s/2s）
  - 流式中断时显示 "连接断开，正在重连..." UI 提示
  - 重连成功继续追加内容
  - 重连失败提示用户检查网络
- `AiChat.vue` 的 `handleSend()` 区分网络错误和业务错误

### Task 2 ⭐ Monitor 静默前端联调验证
**现状**：静默 UI 有（Alerts.vue + monitor.js），后端 API 有（MonitorController）
**需要验证**：
- 检查 `silenceAlert` / `unsilenceAlert` / `silenceRule` / `unsilenceRule` 4 个 API 路径是否正确（`/api/v1/...` vs `/monitor/...`）
- 检查 `AlertEngine.java` 规则级 `silencedUntil` 检查逻辑是否完整
- 补充一个单元测试 `AlertSilenceTest`（3 用例：实例静默/规则静默/静默期不触发）

### Task 3 ⭐ RAG 上传切片端到端测试
**现状**：`uploadDoc` API 已有，DocumentService 已有
**需要验证**：
- 检查 `DocumentService.upload()` 是否有 chunking 逻辑（512 字符滑动窗口）
- 补充 `RagUploadIntegrationTest`（3 用例：上传 TXT→chunk 数量、上传 DOCX→切片、上传 PDF→文本提取）
- 确认前端进度条调用了 `onProgress` 回调

### Task 4 RAG 知识库上传进度条（验收）
**现状**：knowledge/Index.vue 已有进度条，`uploadDocWithCancel` 有 `onUploadProgress`
**验收点**：进度条实际触发的百分比是否正常显示

## 执行顺序
1. Task 1（前端改动）→ 立即可验证
2. Task 2（后端测试 + 验证）→ 需要 Java 环境
3. Task 3（后端测试 + 验证）→ 需要 Java 环境
4. 自检（Task 4 是验收，不单独测试）

## 自检标准
- `self-check.sh` ≥ 78+
- `java-static-check.sh` 0 错误
- `npm run build` 通过
- `mvn compile` 通过

## 交付
- Day 36 report
- git commit: `feat(day-36): SSE reconnect + monitor silence test + RAG E2E`
