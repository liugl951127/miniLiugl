# Day 58 Report — 2026-09-01

## 自检结果

| 检查项 | 结果 |
|--------|------|
| self-check (13/13) | ✅ |
| java-static-check (5/5) | ✅ (1 warning: test @Autowired 字段) |
| vite build (frontend) | ✅ 48.55s（pnpm install + build） |

---

## 今日完成

### 1. RCA 分析结果一键转知识库条目 ✅

**目标**: 在 RCA 详情抽屉中增加「保存到知识库」按钮，将 RCA 分析结果保存为可检索知识条目。

**SQL 新增**（`minimax-v681-schema.sql` 末尾）：
- 新建 `alert_rca_knowledge` 表，字段：id / alert_id / metric_name / rule_name / severity / category / cause / suggested_actions / confidence / method / historical_knowledge / saved_by / created_at

**后端新增**：
- `AlertRcaKnowledge.java`（V6.9）：实体类，对应 `alert_rca_knowledge` 表
- `AlertRcaKnowledgeMapper.java`：MyBatis-Plus BaseMapper，支持 `selectByMetricName()` / `selectBySavedBy()` 定制方法
- `AlertRcaKnowledgeMapper.xml`：两个查询的 SQL 映射
- `AlertRcaService.java`（V6.9）：新增 `saveRcaKnowledge(event, rca, savedBy)` 方法，将 RCA 结果序列化为 JSON 写入数据库
- `MonitorController.java`（V6.9）：
  - `POST /alerts/rca/save-to-knowledge`：保存 RCA 到知识库（接收 alertId，分析后写入）
  - `GET /alerts/rca/knowledge/list`：查询已保存的 RCA 知识列表（按 metricName / savedBy 筛选）

**前端新增**（`Alerts.vue` V6.9）：
- RCA 抽屉底部新增「保存到知识库」按钮（绿色，`Collection` 图标）
- `saveToKbLoading` 状态防止重复提交
- `saveToKnowledgeBase()` 函数：调用 `monitorApi.saveRcaToKnowledge()`，成功后弹出 ElMessage.success

**前端新增**（`monitor.js` V6.9）：
- `saveRcaToKnowledge(alertId)` → `POST /monitor/alerts/rca/save-to-knowledge`
- `listRcaKnowledge(params)` → `GET /monitor/alerts/rca/knowledge/list`
- 已加入 `createMonitorApi()` 导出对象

**改动文件**: SQL 1 文件、后端 4 文件、前端 2 文件

---

### 2. RAG 检索结果文档类型筛选 ✅

**目标**: KbList.vue 检索面板增加文档类型筛选（PDF / Word / Markdown / TXT），按 source_type 字段过滤。

**后端新增**（`DocumentChunkMapper.java` V6.9）：
- 新增 `selectEmbeddingsByKbAndFileType(kbId, fileType, limit)` 方法，按文档类型 JOIN 过滤

**后端新增**（`DocumentChunkMapper.xml` V6.9）：
- 新增 `<select id="selectEmbeddingsByKbAndFileType">`，INNER JOIN `document` 表过滤 `source_type`

**后端更新**（`Retriever.java` V6.9）：
- 主方法 `retrieve(kbId, query, topK, useTimeliness, sortBy, fileType)` 增加 `fileType` 参数
- `fileType` 有值时调用 `selectEmbeddingsByKbAndFileType`，否则调用原方法
- `retrieveMultiKb` 同步支持 `fileType` 参数
- 所有旧版重载方法自动向后兼容

**后端更新**（`RagController.java` V6.9）：
- `/retrieve`、`/retrieve/multi`、`/retrieve/rerank`、`/ask/multi` 四个 endpoint 全部提取 `fileType` 参数并透传

**前端更新**（`KbList.vue` V6.9）：
- 检索面板新增「文档类型」下拉选择（el-select，位于排序维度下方）
- `retrieveFileType` 状态变量，`fileTypeOptions` 包含：不限类型 / PDF / Word / Markdown / 纯文本
- `doRetrieve()` 将 `fileType` 注入请求 body（仅当有值时）
- 检索结果头部：有筛选时显示文档类型标签（如 📄 PDF）

**改动文件**: 后端 3 文件、前端 1 文件

---

## 前后端一致性扫描

| 检查项 | 结果 |
|--------|------|
| saveRcaToKnowledge API 路径匹配 | ✅ `/monitor/alerts/rca/save-to-knowledge` → `/api/v1/monitor/alerts/rca/save-to-knowledge` |
| listRcaKnowledge API 路径匹配 | ✅ `/monitor/alerts/rca/knowledge/list` → `/api/v1/monitor/alerts/rca/knowledge/list` |
| Alerts.vue import monitorApi | ✅ monitor.js 函数正确引用 |
| KbList.vue fileType body 注入 | ✅ `...(retrieveFileType.value ? { fileType } : {})` |
| Retriever 重载方法签名一致性 | ✅ 6 个重载版本全部向后兼容 |
| DocumentChunkMapper XML namespace | ✅ 正确引用 `com.minimax.rag.mapper.DocumentChunkMapper` |

---

## 关键数据

| 指标 | 值 |
|------|-----|
| 新增 Java 类/方法 | 1 个表 + 2 个 entity/mapper + 2 个 service 方法 + 2 个 endpoint |
| 新增前端组件 | 1 个抽屉按钮 + 1 个下拉选择器 |
| 自检通过率 | 13/13 ✅ |
| Java 静态检查 | 5/5 ✅ |
| vite build | 48.55s ✅ |

---

## 明日计划 Day 59

- [ ] RCA 知识库管理界面（查看/删除已保存的 RCA 知识条目）
- [ ] RAG 检索结果来源标注（显示文档 MIME 类型标签）
- [ ] MiniMax 大模型平台日常维护（前端语法检查 / API 路径一致性）
