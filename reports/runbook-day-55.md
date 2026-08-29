# Day 55 Runbook — 2026-08-29

## 任务清单

### P0 — Monitor RCA 前端展示
- **目标**: Alerts.vue 活跃告警列表增加「根因分析」按钮，点击弹出详情抽屉
- **内容**: RCA category 标签 / cause 文本 / suggestedActions 列表 / historicalKnowledge 历史经验 / method 标识 / confidence 置信度
- **后端已有**: `RcaResult` 已含 `historicalKnowledge` 字段（Day 54），前端只需调用 `monitorApi.rcaAnalysis(alertId)`
- **文件**: `frontend/src/views/monitor/Alerts.vue`

### P0 — RAG 置信度展示
- **目标**: KbList.vue 检索结果卡片显示 `rankScore`（Cross-Encoder 综合分）
- **现状**: 已有 `score`（向量相似度）展示，`rankScore` 字段已存在于后端 Hit 对象但前端未展示
- **改动**: 模板中 `retrieve-item-header` 增加 rankScore vs score 对比展示；结果头部加「精排」标签
- **文件**: `frontend/src/views/knowledge/KbList.vue`

### 验证
- [ ] 自检 13/13 ✅
- [ ] Java 静态 5/5 ✅
- [ ] vite build ✅
- [ ] git push
