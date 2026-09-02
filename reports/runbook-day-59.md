# Day 59 Runbook — 2026-09-02

## 今日任务（来自 Day 58 明日计划）

1. **RCA 知识库管理界面** — 查看/删除已保存的 RCA 知识条目
2. **RAG 检索结果来源标注** — 显示文档 MIME 类型标签
3. **日常维护** — 前端语法检查 / API 路径一致性 / 后端静态检查

---

## 任务 1：RCA 知识库管理界面

### 目标
在 Alerts.vue 的 RCA 抽屉旁边（或通过「告警中心」页面）增加一个「RCA 知识库」管理面板，支持查看/删除已保存的 RCA 知识条目。

### 后端已有能力（Day 58 已完成）
- `GET /monitor/alerts/rca/knowledge/list` — 查询 RCA 知识列表
- `AlertRcaKnowledge` 实体类已存在
- `AlertRcaKnowledgeMapper` 有 `selectByMetricName()` / `selectBySavedBy()`

### 待确认/补充
- [ ] 是否已有 `DELETE /monitor/alerts/rca/knowledge/{id}` endpoint？若无需新增
- [ ] 前端需新建 `RcaKnowledgeDrawer.vue` 或在现有 Alerts.vue 中加 Tab

### 前端交付物
- `src/api/monitor.js`：新增 `deleteRcaKnowledge(id)` → `DELETE /monitor/alerts/rca/knowledge/{id}`
- `src/views/alert/RcaKnowledgeDrawer.vue`（新文件）：抽屉组件
  - 表格展示：metric_name / rule_name / severity / cause / confidence / saved_by / created_at
  - 支持按 metricName / savedBy 搜索
  - 支持删除操作（带确认）
- `Alerts.vue` 或 `MonitorIndex.vue`：加入入口按钮「RCA 知识库」

### 一致性检查
- 前端 API 路径必须与后端 `MonitorController` 路径一致
- `AlertRcaKnowledge` JSON 字段命名与前端 props 对齐

---

## 任务 2：RAG 检索结果来源标注

### 目标
在 KbList.vue 检索结果列表中，每个结果行显示文档 MIME 类型标签（PDF / DOCX / MD / TXT）。

### 分析现状
- `KbList.vue` 已有检索结果展示，模板中每条结果有 `source` 字段（来源文档名）
- `Retriever.java` 的 `retrieve()` 返回结果中包含 `DocumentChunk` 对象，有 `source_type` 字段
- 前端 `doRetrieve()` 请求返回的结果应已包含 `sourceType` 字段

### 前端修改
- `KbList.vue`：在每条检索结果行，增加文件类型标签显示
  - 根据 `chunk.sourceType` / `item.sourceType` 显示对应图标
  - PDF → 📄，Word → 📝，Markdown → 📋，TXT → 📃

### 一致性检查
- `sourceType` 字段必须存在于返回的 JSON 中
- 如字段名不一致（后端用 `source_type`），需检查 JSON 序列化配置

---

## 任务 3：日常维护

### 自检三板斧
```bash
bash /workspace/minimax-platform/scripts/self-check.sh
bash /workspace/minimax-platform/scripts/java-static-check.sh
cd /workspace/minimax-platform/frontend && npm run build
```

### 前端代码扫描
- 检查 `src/views/**/*.vue` 是否 import 了不存在的组件/API
- 检查 `src/api/*.js` 中的 axios 请求路径是否与后端 Controller `@RequestMapping` 一致
- 扫描 `src/` 下是否有 `console.error` / 未捕获的 Promise rejection

### 后端静态检查
- 检查 `src/main/java` 下是否有语法错误（已通过 java-static-check.sh）
- 检查 Mapper XML 是否 namespace 正确

---

## 成功标准
- [ ] RCA 知识库管理界面可查看和删除条目
- [ ] 检索结果行显示文档类型标签
- [ ] self-check ≥ 78/78
- [ ] java-static-check 0 错误
- [ ] vite build 通过
- [ ] git push 成功

---

## 输出文件
- `reports/day-59-report.md`
- `reports/next-day.txt` → 60
- `PROGRESS.md` 更新（若无则新建）
