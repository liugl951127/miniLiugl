# Day 53 Report — 2026-08-24

## 自检结果

| 检查项 | 结果 |
|--------|------|
| self-check (13/13) | ✅ |
| java-static-check (5/5) | ✅ (1 warning: test file @Autowired) |
| vite build (frontend) | ✅ 43.17s |
| scan-frontend-syntax.py | ✅ (0 真错) |

---

## 今日完成

### 1. Monitor 告警趋势预测 ✅

**新增 AlertPredictionService** (`minimax-monitor/.../service/AlertPredictionService.java`)

算法：
- 取近 N 天历史告警数据，按日聚合
- 指数加权移动平均 (EWMA, alpha=0.3) 平滑序列
- 线性回归斜率判断趋势方向 (RISING / FALLING / STABLE)
- 周模式因子（工作日 vs 周末权重）
- 预测未来 N 天告警数 + 置信度 + 风险等级 (HIGH/MEDIUM/LOW)
- 风险预警：预测值显著高于近期均值时给出警告

**新增 API：**
```
GET /api/v1/monitor/alerts/predict
  ?historyDays=30      历史窗口（默认 30）
  ?forecastDays=7     预测天数（默认 7）
  ?severity=CRITICAL   按级别过滤（可选）

GET /api/v1/monitor/alerts/predict/by-severity
  返回 CRITICAL / WARNING / INFO 分别预测结果
```

返回结构：
```json
{
  "trend": "RISING",
  "trendSlope": 0.135,
  "recentAvgDaily": 4,
  "ewmaSmoothed": 3.82,
  "historicalSeries": [{ date, count, dayOfWeek }],
  "forecasts": [{ date, predicted, confidence, riskLevel }],
  "warnings": ["⚠️ 2026-08-25 预测告警数 (8) 显著高于近期均值 (4)..."],
  "confidence": 0.75
}
```

### 2. 前端全站深色模式一致性审查 ✅

**compat.scss CSS 变量扩展** — 新增：
- `--stat-blue-bg/fg`、`--stat-green-bg/fg`、`--stat-amber-bg/fg`、`--stat-red-bg/fg`（亮色模式）
- Dark mode 覆盖：半透明深色背景 + 高亮文字（`rgba(59,130,246,0.2)` 等）
- `--text-muted`、`--text-faint`、`--text-strong`（通用文字色）

**修复文件（16 处硬编码 → CSS 变量）：**
- `Dashboard.vue`：stat-icon 颜色类 + stat-value/label/notice-text/notice-date 深色兼容
- `agent/Canvas.vue`：6 处 `#909399`/`#c0c4cc` → `var(--text-muted/faint)`
- `agent/CanvasToolbar.vue`：`#303133` → `var(--text-strong)`
- `analytics/Vote.vue`：4 个 stat 数字颜色 → `--liugl-primary/success/warning/danger`
- `builder/Analysis.vue`：`#64748b` → `var(--liugl-text-secondary)`
- `builder/Deploy.vue`：2 处资源标签颜色 → `var(--liugl-text-secondary)`
- `chat/Stream.vue`：空状态颜色 → `var(--liugl-text-secondary)`
- `prompts/Index.vue`：模板预览区背景/文字 → `var(--liugl-bg/text/border)`

### 3. RAG 跨知识库联合检索 ✅

**Retriever.java 新增方法：**
```java
List<Hit> retrieveMultiKb(List<Long> kbIds, String query, int topK, boolean useTimeliness)
List<Hit> retrieveMultiKb(List<Long> kbIds, String query, int topK)
```

特性：
- 多 KB 并行检索（每个 KB 取 topK×2 候选）
- chunk 级别去重（同一 doc 保留最高分 chunk）
- KB 间均衡（每个 KB 至少保留 topK/N 个）
- 综合分排序（相关性 + 时效性加权）

**新增 API：**
```
POST /api/v1/rag/retrieve/multi
  body: { "kbIds": [1, 2, 3], "query": "...", "topK": 5 }

POST /api/v1/rag/ask/multi
  body: { "kbIds": [1, 2], "question": "...", "history": "...", "topK": 5 }
```

返回结构（ask/multi）：
```json
{
  "question": "...",
  "kbCount": 2,
  "kbNames": ["知识库A", "知识库B"],
  "hitCount": 5,
  "sources": [{ chunkId, docId, kbId, docTitle, score, rankScore, highlight }],
  "answer": "LLM 生成答案...",
  "strategy": "HYBRID",
  "elapsedMs": 1243
}
```

---

## 关键文件变更

| 文件 | 变更 |
|------|------|
| `minimax-monitor/.../AlertPredictionService.java` | 新增 260 行 |
| `minimax-monitor/.../MonitorController.java` | 新增 2 个端点 |
| `minimax-rag/.../Retriever.java` | 新增 `retrieveMultiKb` 方法 |
| `minimax-rag/.../RagController.java` | 新增 `/retrieve/multi` + `/ask/multi` 端点 |
| `frontend/src/styles/compat.scss` | 新增 stat 颜色变量 + dark mode 覆盖 |
| `frontend/src/views/Dashboard.vue` | 16 处硬编码颜色 → CSS 变量 |
| `frontend/src/views/agent/Canvas.vue` | 6 处文字颜色修复 |
| `frontend/src/views/agent/CanvasToolbar.vue` | 1 处文字颜色修复 |
| `frontend/src/views/analytics/Vote.vue` | 4 处 stat 颜色修复 |
| `frontend/src/views/builder/Analysis.vue` | 1 处文字颜色修复 |
| `frontend/src/views/builder/Deploy.vue` | 2 处资源标签颜色修复 |
| `frontend/src/views/chat/Stream.vue` | 1 处空状态颜色修复 |
| `frontend/src/views/prompts/Index.vue` | 模板预览区全量修复 |

---

## 明日计划 Day 54

- [ ] Monitor 告警根因知识库（同类告警自动关联历史处理记录）
- [ ] 前端性能优化（Code Splitting / 路由预加载 / 图片压缩）
- [ ] RAG 语义重排序（Cross-Encoder rerank 优化 top-K 准确率）
