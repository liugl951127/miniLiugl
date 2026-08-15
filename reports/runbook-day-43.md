# Day 43 Runbook — 2026-08-14

## 今日任务

### Task 1 RAG 文档全文检索高亮
- **目标**: 搜索结果点击后，定位到原文并高亮
- **后端**: RagDocument 增加高亮摘要方法，生成含 mark 标签的高亮片段
- **前端**: 知识库搜索列表点击后，展开文档内容，原文片段高亮显示
- **文件**: backend/rag-service/..., frontend/views/knowledge/Index.vue

### Task 2 Monitor 告警 SLA 统计
- **目标**: 新增 SLA 统计 Dashboard，展示 MTBF / MTTR / 可用率
- **后端**: AlertMetricService 计算 SLA 指标 (基于 alert_logs 表时间戳)
- **前端**: Monitor 新增「SLA 统计」Tab，卡片展示 MTBF / MTTR / 可用率 / 总告警数
- **文件**: backend/monitor-service, frontend/views/monitor/Index.vue

### Task 3 投票结果邮件通知
- **目标**: 投票结束（deadline 到或手动关闭）后，自动发送邮件给发起人
- **后端**: AiVotingService.votingEnded() 调用 NotificationService 发邮件
- **前端**: 投票创建时填写邮件地址，投票结束后触发通知
- **文件**: backend/ai-service, frontend/views/analytics/Index.vue

## 执行顺序
Task 1 (RAG 高亮) -> 自检 -> Task 2 (SLA) -> 自检 -> Task 3 (邮件通知) -> 自检 -> push
