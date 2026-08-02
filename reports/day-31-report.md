# Day 31 Report — 2026-08-02

## ✅ Day 31 - 智能化集成落地：投票对话 / RAG 展开检索 / 告警 RCA 联动 / 异常检测告警

**今日完成：**

### 1. VotingChatController — 多模型投票集成到对话流程 (Day 31)

**模块**: `minimax-ai/controller/VotingChatController.java` (新文件)

**核心设计**：在 `minimax-ai` 模块内新增高级对话 Controller，同时依赖 `MultiModelVotingService`（投票引擎）和 `RestTemplate`（调 minimax-model 的 HTTP 端点），实现跨模块投票路由。

**端点**：
- `POST /api/v1/ai/chat` — 智能对话：先单模型预览置信度，低则自动触发多模型投票
- `POST /api/v1/ai/chat/voting` — 强制多模型投票（绕过置信度预判）
- `GET  /api/v1/ai/chat/voting-info` — 查询投票配置

**流程**：
```
请求 → shouldVote(text) 预判置信度
    → 高置信 → 直接 callSingleModel() → 返回
    → 低置信 → votingService.vote() → 多模型并行推理 → 共识答案
响应 → ChatResponse + votingMeta (策略/耗时/一致率/各模型答案)
```

**响应结构**：
```json
{
  "response": { "content": "共识答案", "finishReason": "..." },
  "meta": {
    "confidence": 0.85,
    "votingTriggered": true,
    "totalElapsedMs": 1240,
    "votingStrategy": "CONFIDENCE_WEIGHTED",
    "votingElapsedMs": 890,
    "agreementScore": 0.85,
    "modelCount": 3,
    "modelAnswers": [
      { "model": "MiniMax-Text-01", "provider": "minimax", "answer": "...", "latencyMs": 312 },
      ...
    ]
  }
}
```

---

### 2. RagService — QueryExpander 深度集成 (Day 31)

**模块**: `minimax-rag/service/RagService.java`

**改动**：将 `retriever.retrieve()` 替换为 `queryExpander.expandRetrieve()`，默认启用展开检索。

**改动前**：
```java
List<Retriever.Hit> hits = retriever.retrieve(kbId, question, topK);
```

**改动后**：
```java
// 默认启用 QueryExpander 展开检索（Day 31）
QueryExpander.ExpansionResult expansionResult = queryExpander.expandRetrieve(kbId, question, topK);
List<Retriever.Hit> hits = expansionResult.getHits();
String expStrategy = expansionResult.getStrategy();
```

**新字段**：`RagAnswer` record 新增 `strategy`（展开策略名）和 `elapsedMs`（端到端耗时），响应更透明。

**端到端耗时** = 检索（含展开）+ LLM，分别记录并在日志中输出。

---

### 3. AlertEngine — AlertRcaService 联动 (Day 31)

**模块**: `minimax-monitor/alert/AlertEngine.java`

**改动**：告警触发时自动调用 `AlertRcaService.analyze()`，RCA 结果记录到日志并将根因追加到告警消息。

```java
// Day 31: 自动触发 RCA 根因分析
try {
    RcaResult rca = rcaService.analyze(e);
    if (rca != null && rca.isAnalyzed()) {
        log.info("[RCA] alertId={} category={} cause='{}' actions={}ms (method={})",
                e.getId(), rca.getCategory(), rca.getCause(), rca.getAnalysisMs(), rca.getMethod());
        // 将 RCA 原因追加到告警消息（方便前端展示）
        if (e.getMessage() != null && rca.getCause() != null) {
            e.setMessage(e.getMessage() + " | RCA: " + rca.getCause());
            eventMapper.updateById(e);
        }
    }
} catch (Exception rcaEx) {
    log.warn("[RCA] alertId={} analysis error: {}", e.getId(), rcaEx.getMessage());
}
```

**RCA 覆盖范围**：
- 短时告警（<1min）且规则命中 → 规则预分类（快路径，不调 LLM）
- 长时/复杂告警 → MiniMax-Text-03 深度推理

---

### 4. AlertEngine — LogAnomalyDetector 告警绑定 (Day 31)

**模块**: `minimax-monitor/alert/AlertEngine.java` + `MonitorController.java`

**改动**：每次 `evaluateRule()` 读取指标后，自动送入 `LogAnomalyDetector.detect()`，异常分 ≥ WARNING 时触发独立的异常检测告警。

```java
// Day 31: 异常检测 — 指标值送入 LogAnomalyDetector
if (anomalyDetector.isEnabled()) {
    try {
        AnomalyResult ar = anomalyDetector.detect(r.getMetricName(), v, r.getService());
        if (ar.needsAlert()) {
            fireAnomalyAlert(r, v, ar);  // 独立事件 + 通知 + 推送
        }
    } catch (Exception anomEx) {
        log.debug("[Anomaly] {} detection error: {}", r.getMetricName(), anomEx.getMessage());
    }
}
```

**异常检测告警**：
- 5 分钟冷却（防止告警风暴）
- severity = WARNING（score 0.75~0.90）或 CRITICAL（score > 0.90）
- 独立事件写入 `alert_event` 表，触发通知 + SSE 推送

**新增端点**（`MonitorController`）：
- `POST /monitor/alerts/{id}/rca` — 手动对告警做 RCA 分析
- `POST /monitor/anomaly/detect` — 手动触发异常检测
- `GET  /monitor/anomaly/summary` — 异常检测摘要（均值/标准差/Z-Score）
- `GET  /monitor/anomaly/active-metrics` — 活跃检测指标列表

---

### 5. 自检结果

| 检查项 | 结果 |
|--------|------|
| self-check.sh (13/13) | ✅ |
| java-static-check.sh (5/5, 0 错误) | ✅ |
| 前端 dist 产物 | ✅ |

---

**代码量**：
- `VotingChatController.java` 新增 (~10KB, ~310 行)
- `RagService.java` 修改 (~+30 行)
- `AlertEngine.java` 修改 (~+80 行)
- `MonitorController.java` 修改 (~+70 行)

**报告：** `reports/day-31-report.md`

---

## Day 32 - 待开始

**待做**：
- [ ] 告警前端 UI 接入 RCA 结果展示（前端 Monitor 页面显示根因分析）
- [ ] 异常检测历史趋势图（前端图表接入 anomaly API）
- [ ] 前端投票对话 UI（展示多模型答案 + 一致率）
- [ ] V4.3 收尾文档（API.md 更新 + README.md 更新）
