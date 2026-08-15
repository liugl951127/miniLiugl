# Day 30 Report — 2026-07-27

## ✅ Day 30 - 智能化提升 V2：多模型投票 / RAG 查询重写 / 智能告警 RCA / 日志异常检测

**今日完成：**

### 1. MultiModelVotingService — 多模型投票 (Day 30)

**模块**: `minimax-ai/intent/MultiModelVotingService.java`

**触发条件**: `IntentConfidenceScorer.confidence < 0.50` → 自动触发多模型并行推理

**投票策略（可配置）**:
- `MAJORITY`: 多数投票，相同答案超过半数
- `CONFIDENCE_WEIGHTED`（默认）: 各模型能力分加权，累加得分最高者
- `LLM_JUDGE`: 各模型答案交给 MiniMax-Text-03 最终裁决（最可靠但成本最高）

**并行策略**:
- 多模型同时发起 HTTP 调用 (`CompletableFuture.allOf`)
- 单模型超时: 10s（可配置）
- 允许部分模型失败（至少 2 个成功才投票）

**集成点**: 复用 `SmartModelRouter` 的模型池（自研 MiniMax-Text-01/02 + 外部 GPT4O/DeepSeek）

**API**:
```java
// 判断是否需要投票
boolean shouldVote(String text, String sessionId)

// 执行投票
VotingResult vote(String text, String sessionId)

// 快捷方法：需要则投票，否则返回单模型答案
VotingResult voteIfNeeded(String text, String sessionId, String singleAnswer)
```

**配置**:
```yaml
minimax.ai.voting.enabled=true
minimax.ai.voting.threshold=0.50
minimax.ai.voting.model-count=3
minimax.ai.voting.strategy=CONFIDENCE_WEIGHTED
minimax.ai.voting.timeout-ms=10000
```

---

### 2. QueryExpander — RAG 查询重写 (Day 30)

**模块**: `minimax-rag/service/QueryExpander.java`

**问题**: 原始查询往往简短模糊，直接检索效果差

**展开策略**:
- `SYNTACTIC`: 基于规则的同义词/句式变换（无需 LLM 调用）
  - 40+ 同义词映射（查询→检索/如何→怎么/API→接口等）
  - 中英混合扩展（SQL→数据库查询、JWT→身份验证 token）
  - 疑问词扩展（加"？"、"怎么做"、"关于..."）
- `SEMANTIC_LLM`（默认）: MiniMax-Text-03 生成多个语义等价表述
- `HYBRID`: 规则 + LLM 混合（最全面）

**检索流程**:
```
原始查询 → LLM 生成 N 个展开查询
        → 并发向量检索（每个展开查询独立检索）
        → 按 chunkId 去重（保留最高分）
        → 取 topK
```

**配置**:
```yaml
minimax.rag.query-expansion.enabled=true
minimax.rag.query-expansion.strategy=SEMANTIC_LLM
minimax.rag.query-expansion.expanded-count=3
minimax.rag.query-expansion.model=MiniMax-Text-03
```

**API**:
```java
// 扩展 + 并发检索
ExpansionResult expandRetrieve(Long kbId, String query, int topK)

// 仅展开（不检索）
List<String> expand(String query)
```

---

### 3. AlertRcaService — 智能告警根因分析 (Day 30)

**模块**: `minimax-monitor/service/AlertRcaService.java`

**目标**: 告警触发后 LLM 推理可能根因 + 建议操作，大幅缩短 MTTR

**两阶段分析**:
1. **规则预分类**（快路径，不调 LLM）: 短时告警（<1min）+ 规则命中 → 直接返回
2. **LLM 深度推理**: 长时/复杂告警 → MiniMax-Text-03 综合历史告警上下文推理

**根因分类（7 类）**:
| 类别 | 关键词 |
|------|--------|
| RESOURCE_BOTTLENECK | CPU/Memory/Disk 使用率高 |
| CONFIG_ERROR | 超时/连接池满/配置缺失 |
| EXTERNAL_DEPENDENCY | DB/Redis/外部 API 超时 |
| CODE_BUG | OOM/NPE/StackOverflow/死循环 |
| TRAFFIC_SPIKE | QPS 突增/并发过高 |
| NETWORK | 超时/DNS/连接拒绝 |
| UNKNOWN | 无法分类 |

**输出**:
- 根因分类 + 详细分析（3-5 句话，引用具体数据）
- 4 条按优先级排序的建议操作
- 分析耗时 + 置信度

**配置**:
```yaml
minimax.monitor.rca.enabled=true
minimax.monitor.rca.model=MiniMax-Text-03
minimax.monitor.rca.service-url=http://localhost:8083
```

**API**:
```java
// 单事件分析
RcaResult analyze(AlertEvent event, List<AlertEvent> recentEvents)

// 批量分析
List<RcaResult> analyzeBatch(List<AlertEvent> events)
```

---

### 4. LogAnomalyDetector — 无监督日志异常检测 (Day 30)

**模块**: `minimax-monitor/service/LogAnomalyDetector.java`

**检测算法（5 种加权组合）**:

| 算法 | 权重 | 说明 |
|------|------|------|
| Z-Score | 35% | 值偏离均值超过 N 个标准差 |
| EWMA | 30% | 指数加权移动平均，快速响应突变 |
| IQR | 20% | 四分位距，极端值检测 |
| Spike Detection | 15% | 相邻差值突变检测（>200% 变化） |

**告警级别**:
```
score > 0.90  → CRITICAL (红色)
score > 0.75  → WARNING  (橙色)
score > 0.60  → INFO     (黄色)
score <= 0.60 → NORMAL   (绿色)
```

**特性**:
- 无需训练样本，纯统计方法
- 滑动窗口（环形缓冲区），自动淘汰旧数据
- 低波动指标自动放大敏感度（std < mean*5%）
- 支持批量检测 + 指标摘要查询

**配置**:
```yaml
minimax.monitor.anomaly.z-threshold=3.0
minimax.monitor.anomaly.window-size=60
minimax.monitor.anomaly.alpha=0.3
minimax.monitor.anomaly.enabled=true
minimax.monitor.anomaly.min-samples=10
```

**API**:
```java
// 实时检测
AnomalyResult detect(String metric, double value, String instanceId)

// 批量检测
List<AnomalyResult> detectBatch(Map<String, Double> metrics, String instanceId)

// 指标摘要
MetricSummary getSummary(String metric, String instanceId)

// 重置
void reset(String metric, String instanceId)
```

---

### 5. 自检脚本 (Day 30)

**新增 `scripts/self-check.sh`**:
- 检查目录结构 / SQL 文件数 / Maven 模块数
- 验证 Day 30 的 4 个关键文件存在
- 前端构建产物检查

**新增 `scripts/java-static-check.sh`**:
- package 声明完整性
- TODO 残留检查（允许 gpt 相关）
- System.out 检查（用 log 替代）
- class 块匹配
- @Autowired 字段检查

---

**自检结果**:
- `self-check.sh`: ✅ 13/13 通过
- `java-static-check.sh`: ✅ 5/5 通过（0 错误）
- `npm run build`: ✅ 1m 22s 通过

**代码量**:
- `MultiModelVotingService.java` 新增 (~14KB, ~450 行) — 多模型投票
- `QueryExpander.java` 新增 (~11KB, ~380 行) — RAG 查询重写
- `AlertRcaService.java` 新增 (~19KB, ~650 行) — 告警 RCA
- `LogAnomalyDetector.java` 新增 (~13KB, ~450 行) — 日志异常检测
- `self-check.sh` 新增
- `java-static-check.sh` 新增

---

## Day 31 - 待开始

**待做**:
- [ ] 多模型投票集成到实际对话流程（ChatController 接入 VotingService）
- [ ] RAG QueryExpander 与 RagService 的深度集成（默认启用展开检索）
- [ ] AlertRcaService 与 AlertEngine 联动（告警触发时自动触发 RCA）
- [ ] LogAnomalyDetector 与告警规则绑定（异常分超过阈值触发告警）
