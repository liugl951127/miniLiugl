# Day 44 Report — 2026-08-15

## ✅ Day 44 - RAG 全文阅读 + Monitor 告警趋势图 + 投票详情弹窗

### 自检全部通过

| 检查项 | 结果 |
|--------|------|
| self-check.sh | **13/13 ✅** |
| java-static-check.sh | **5/5 ✅**（0 错误，仅测试文件建议） |
| vite build | ✅（49.10s，1928+ 个模块） |

---

### Task 1 ⭐⭐⭐ RAG 文档全文阅读

**后端（`minimax-rag`）：**
- `DocumentService.getById(Long docId)`：新增方法，返回完整 Document 实体（含 `content` 字段）
- `RagController`：新增 `GET /api/v1/rag/doc/{id}/content` 端点
  - 校验文档存在性
  - 调用 `kbService.verifyAccess()` 做归属校验（防 IDOR）
  - 返回完整 `Document` 对象（含 `content`、`sizeBytes`、`createdAt`、`status` 等）

**前端（`knowledge/Index.vue`）：**
- `rag.js` 新增 `getDocContent(docId)` API 函数
- 检索结果卡片新增「**阅读全文**」按钮（`item.docId`）
- 新增**文档全文弹窗**（800px）：
  - 文档元信息（名称/类型/大小/切片数/创建时间/状态）
  - 正文内容（pre-wrap 换行，最大高度 60vh 滚动）
  - Loading 态 + 错误提示

**API 路径：** `GET /api/v1/rag/doc/{id}/content`

---

### Task 2 ⭐⭐⭐ Monitor 告警趋势图

**后端（`minimax-monitor`）：**
- `MonitorController` 新增 `GET /api/v1/monitor/alerts/trend` 端点
  - 按天聚合近 N 天（默认 30 天）告警数量
  - 按严重级别分组：CRITICAL / WARNING / INFO
  - 返回：`[{ date, CRITICAL, WARNING, INFO, total }]`

**前端（`monitor.js`）：**
- 新增 `getAlertTrend(params)` API 函数
- 导出列表补充 `getAlertTrend`

**前端（`monitor/Index.vue`）：**
- 新增「**告警趋势**」Tab（懒加载，切到 Tab 才请求）
- 时间范围切换：7天 / 14天 / 30天
- **ECharts 折线+柱状混合图**：CRITICAL/WARNING/INFO 三条线 + 总数柱状
- 底部 4 个统计卡片：告警总数 / 日均 / 最高单日 / 峰值日期

---

### Task 3 ⭐⭐ 投票历史详情弹窗

**前端（`analytics/Index.vue`）：**
- `recentVotes` 映射补全：`agreementRate`、`modelVotes`、`createdAt`、`strategy`
- 新增**投票详情弹窗**（680px）：
  - 投票信息（问题/策略/总票数/一致率进度条/投票时间）
  - **各模型答案表格**（模型名 / 答案 / 置信度进度条）
  - 一致率颜色分级：≥80% 绿色 / ≥50% 橙色 / <50% 红色
  - 置信度颜色分级：≥0.8 绿色 / ≥0.5 橙色 / <0.5 红色
- 投票历史列表新增「**查看详情**」按钮

---

## 明日计划 Day 45

- [ ] RAG 文档在线编辑（修改文档内容 + 重新切片 + 重新索引）
- [ ] Monitor 告警升级策略（CRITICAL 自动通知 + 自动恢复）
- [ ] 前端性能优化（虚拟滚动 / 大表格分页 / 路由懒加载）
- [ ] V4.4 Release 打包准备
