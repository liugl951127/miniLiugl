# Day 38 Runbook — 2026-08-09

## 任务来源
Day 37 明日计划遗留

## 今日任务

### Task 1 ⭐ VoteStats.vue 接入真实 API
**问题**：`loadChartData()` / `loadRecords()` 全用 `generateMockData()` 假数据
**改动**：
- `api/analytics.js` 已定义 `getVoteStatsSummary` / `getVoteTrend` / `getVoteRecords`
- `VoteStats.vue` 的 `loadChartData()` 调 `getVoteStatsSummary()` 填充 KPI 卡片 + 调 `getVoteTrend()` 填充折线图
- `loadRecords()` 调 `getVoteRecords()` 填充表格
- 删除 `generateMockData()` 函数

### Task 2 Monitor Alerts 静默状态实时显示
**问题**：静默按钮有，但 firing 卡片无静默徽章提示
**改动**：
- Monitor Index.vue / Alerts.vue 中 firing 卡片每条 alert 显示 `isSilenced` 状态
- 若 alert.silencedUntil > now，显示 🔇 徽章 + 剩余时间

### Task 3 全链路 API 路径一致性扫描
- 前端 http.js baseURL
- 各 api/*.js 是否统一前缀
- 重点：ai.js / monitor.js / analytics.js

### Task 4 自检 + push
- self-check.sh ≥ 14/14
- java-static-check.sh 0 错误
- npm run build 通过
- git push

## 执行顺序
1. Task 1（前端改动）→ 立即可验证
2. Task 2（前端改动）
3. Task 3（扫描）
4. 自检
5. push

## 交付
- Day 38 report
- git commit: `feat(day-38): voting stats real API + monitor silence badge + api audit`
