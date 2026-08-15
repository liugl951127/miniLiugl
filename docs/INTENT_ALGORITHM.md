# 意图识别算法完整链路 (V3.5.8)

> 本文档详细描述 MiniMax Platform 意图识别系统的完整代码链路
> 包括: 4 模型加权投票 + 数据分析引擎 + 问题分类 + 智能推荐

---

## 📋 目录

1. [架构概览](#架构概览)
2. [完整数据流](#完整数据流)
3. [核心组件详解](#核心组件详解)
4. [API 接口](#api-接口)
5. [YAML 配置](#yaml-配置)
6. [性能 & 准确度](#性能--准确度)

---

## 🏗️ 架构概览

### 4 层架构

```
┌────────────────────────────────────────────────────────────┐
│ Layer 1: HTTP 入口层 (IntentController)                   │
│  - POST /api/v1/ai/intent/predict                         │
│  - GET  /api/v1/ai/intent/config                          │
│  - PUT  /api/v1/ai/intent/config                          │
│  - POST /api/v1/ai/intent/benchmark                       │
└────────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────────┐
│ Layer 2: 业务服务层 (IntentPredictionService)             │
│  - 编排 4 模型融合                                         │
│  - 调用 V3.5.8 新增 3 组件                                 │
│  - 输出 9 维度 IntentPrediction                            │
└────────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────────┐
│ Layer 3: 核心算法层 (4 + 3 组件)                          │
│  - TextNormalizer   文本归一化                            │
│  - NgramExtractor   短语提取                              │
│  - NegationHandler  否定处理                              │
│  - WeightedVotingEnsemble  4 模型融合                     │
│  - DataAnalysisEngine  数据分析意图 (V3.5.8)              │
│  - QuestionClassifier 问题分类 (V3.5.8)                  │
│  - SuggestionEngine  智能推荐 (V3.5.8)                   │
└────────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────────┐
│ Layer 4: 配置层 (IntentConfig)                            │
│  - @ConfigurationProperties 绑 yml                        │
│  - 9 意图 38 同义词 16 简繁 20 benchmark                  │
│  - 支持热更新 (PUT /config)                               │
└────────────────────────────────────────────────────────────┘
```

### 文件清单 (10 文件)

| 文件 | 行数 | 角色 |
|------|------|------|
| `IntentController.java` | 179 | HTTP 入口, 5 端点 |
| `IntentPredictionService.java` | 610 | 业务服务, 编排 |
| `IntentConfig.java` | 341 | 配置模型, 绑 yml |
| `TextNormalizer.java` | 175 | 文本归一化 (7 步) |
| `NgramExtractor.java` | 90 | 短语提取 (2-3 gram) |
| `NegationHandler.java` | 130 | 否定作用域 (5 字) |
| `WeightedVotingEnsemble.java` | 125 | 4 模型融合 + 置信度 |
| `DataAnalysisEngine.java` | 295 | 6 意图 + 5 实体 (V3.5.8) |
| `QuestionClassifier.java` | 240 | 12 类型 + 3 维复杂度 (V3.5.8) |
| `SuggestionEngine.java` | 220 | 11 路由 + 25 工具 + 36 追问 (V3.5.8) |
| **合计** | **2,405** | |

---

## 🔄 完整数据流

### predict() 主链路 (12 步)

```java
// 入口: IntentPredictionService.predict(text, sessionId)
IntentPrediction predict(String text, String sessionId) {

    // ① 空入参兜底 → 直接返回空结果
    if (text == null || text.isBlank()) return IntentPrediction.builder()...build();

    // ② 文本归一化 (5 步)
    NormalizedResult norm = TextNormalizer.normalize(text);
    //    - trim
    //    - 全角 → 半角
    //    - lowercase
    //    - 繁 → 简 (LinkedHashMap 长词优先)
    //    - 同义词扩展 (输出到 norm.expansions())
    String work = norm.normalized();

    // ③ 否定作用域检测
    Map<Integer, Integer> negScopes = NegationHandler.detectNegationScopes(work);
    //    - 扫描 "不" "没" "别" 等否定词
    //    - 每个否定词向后延伸 5 字符
    //    - 作用域内关键词应用 0.3 惩罚

    // ④ 4 模型分别打分
    Map<String, Double> tfScores     = scoreKeywords(work, negScopes);   // 关键词 TF
    Map<String, Double> ngramScores  = scoreNgrams(work);                // N-gram 短语
    Map<String, Double> expandScores = scoreExpansions(norm.expansions()); // 同义词扩展
    String ctxIntent = getContextIntent(sessionId);                      // 上下文 (上轮)

    // ⑤ 4 模型融合 (加权求和)
    Map<String, Double> fused = WeightedVotingEnsemble.fuse(
        tfScores, ngramScores, expandScores, ctxIntent, weights
    );
    //    - TF 权重 0.4 (基础分)
    //    - N-gram 权重 0.3 (精度分)
    //    - 同义词 0.2 (召回补充)
    //    - 上下文 0.1 (兜底)

    // ⑥ 排序取 top1
    String topIntent = sorted.get(0).getKey();
    double confidence = confidenceSigmoid(sorted);  // tanh 平滑

    // ⑦ V3.5.8 新增: 数据分析引擎覆盖意图
    DataAnalysisEngine.Result daResult = DataAnalysisEngine.recognize(work);
    if (daResult.matched()) {
        topIntent = daResult.intent();
        confidence = (confidence + daResult.confidence()) / 2;
    }

    // ⑧ V3.5.8 新增: 问题分类 (12 类型)
    List<Match> qMatches = QuestionClassifier.classify(work);
    String questionType = qMatches.isEmpty() ? "general" : qMatches.get(0).type();

    // ⑨ V3.5.8 新增: 复杂度评估
    Complexity complexity = QuestionClassifier.assess(work);
    //    - simple:   total ≤ 1
    //    - medium:   total 2~3
    //    - complex:  total > 3

    // ⑩ V3.5.8 新增: 智能推荐
    Suggestion suggestion = SuggestionEngine.suggest(topIntent, questionType, complexity, confidence);
    //    - 选 Agent (chat/rag/analytics/function)
    //    - 选工具 (25 个)
    //    - 选追问 (36 个)
    //    - 选流程 (DIRECT_QUERY/FILTER_AGG/MULTI_JOIN)
    //    - 选图表 (line/bar/pie/area/table)

    // ⑪ 实体抽取 + 槽位填充
    List<ExtractedEntity> entities = extractEntities(text);
    Map<String, String> slots = extractSlots(text, topIntent, entities);

    // ⑫ 输出 IntentPrediction (16 字段)
    return IntentPrediction.builder()
        .originalText(text)                  // 原文
        .normalizedText(work)                // 归一化
        .intent(topIntent)                   // 意图
        .confidence(confidence)              // 置信度
        .intentScores(probs)                 // 全意图概率
        .entities(entities)                  // 实体
        .slots(slots)                        // 槽位
        .urgency(urgency)                    // 紧急度
        .sentiment(sentiment)                // 情感
        .recommendedAgent(suggestion.agent)  // 推荐 Agent
        .alternatives(alternatives)          // 备选
        .algorithm(config.getAlgorithm())    // 算法版本
        .questionType(questionType)          // 12 类型 (V3.5.8)
        .queryComplexity(complexity.level()) // 3 复杂度 (V3.5.8)
        .suggestedTools(suggestion.tools)    // 推荐工具 (V3.5.8)
        .processSuggestion(suggestion.process) // 流程 (V3.5.8)
        .chartTypeSuggestion(suggestion.chart) // 图表 (V3.5.8)
        .followupQuestions(suggestion.followups) // 追问 (V3.5.8)
        .dataEntities(daResult.entities)     // 数据实体 (V3.5.8)
        .build();
}
```

---

## 🧩 核心组件详解

### 1. TextNormalizer (文本归一化)

**解决问题**: 用户输入千变万化, 关键词匹配需要规范输入

**7 步流水线** (顺序敏感):
```
输入: "我要退  訂！！！"
  ↓ ① trim
"我要退  訂！！！"
  ↓ ② 全角 → 半角
"我要退  訂!!!"
  ↓ ③ lowercase
"我要退  訂!!!"
  ↓ ④ 繁 → 简 (LinkedHashMap 长词优先)
"我要退  订!!!"
  ↓ ⑤ 同义词扩展 (输出 expansions)
expansions: ["退钱", "退货", "申请退款"]
  ↓ ⑥ 空白合并
"我要退 订!!!"
  ↓ ⑦ 重复字压缩
"我要退 订!!!"
```

**关键设计**:
- `LinkedHashMap` 强保序: 简繁表必须长词在前 (`退訂單` → `退订单`)
- `AtomicReference` 包装: 支持热更新

### 2. NgramExtractor (短语提取)

**解决问题**: 单字匹配漏掉关键短语

**算法**:
```java
for (int n = 2; n <= 3; n++) {              // 2-gram 和 3-gram
    for (int i = 0; i + n <= len; i++) {     // 滑动窗口
        String gram = text.substring(i, i + n);
        if (candidates.containsKey(gram)) {
            double score = candidates.get(gram);
            score *= positionWeight(i, n, len);  // 句首尾 +0.5x
            score *= (n == 3 ? 1.2 : 1.0);      // 3-gram 1.2x
            out.add(new NgramMatch(gram, i, i + n, score));
        }
    }
}
```

**复杂度**: O(N × W × K) = 100 字 × 3 × 10 候选 ≈ 1ms

### 3. NegationHandler (否定处理)

**解决问题**: "我不要退款" 被误判为投诉

**作用域算法**:
```
文本: "我不要退款"
位置:  0123456
        ^
        "不" 在位置 1
作用域 = [1, 1+1+5) = [1, 7)
        ↓
位置 3 的 "退" 在作用域内, 分数 × 0.3
```

### 4. WeightedVotingEnsemble (4 模型融合)

**解决问题**: 单模型偏差大, 融合提升准确度

**融合公式**:
```
finalScore(intent) = 0.4 × tf + 0.3 × ngram + 0.2 × expand + 0.1 × context
```

**置信度算法**:
```
diff = top1 - top2
confidence = 0.5 + 0.5 × tanh(diff / scale)  // scale=5.0
```

**Softmax 归一化**:
```
P(i) = exp(s_i - max) / Σ exp(s_j - max)  // 数值稳定
```

### 5. DataAnalysisEngine (数据分析引擎) (V3.5.8)

**解决问题**: 通用 Intent 难以表达数据查询场景

**6 意图打分**:
```java
score(intent) = Σ (trigger.length > 2 ? 0.4 : 0.2)  // 长词加权
matched      = confidence > 0.15                      // 低阈值
```

**5 类实体抽取**:
- `metric`: 销售额/订单数/用户数/转化率/库存
- `dimension`: 地区/产品/渠道/用户/部门
- `timeRange`: 今天/上周/上月/近 7 天/2024 年
- `aggregation`: 总数/平均/最大/最小/Top N
- `dataSource`: 订单/用户/产品/库存

### 6. QuestionClassifier (问题分类) (V3.5.8)

**12 问题类型**:
- 5W1H: what/how/why/when/where/who
- 6 数据查询: query/compare/analyze/predict/recommend/create

**3 维复杂度**:
```java
total = timeRanges + aggregations + dimensions + Math.min(numbers, 3)
if (total <= 1)      level = "simple"
else if (total <= 3) level = "medium"
else                 level = "complex"
```

**图表推荐**:
- `line`: 时间 + 聚合 (折线趋势)
- `bar`: 多维 + 聚合 (柱状对比)
- `pie`: 仅聚合 (占比)
- `area`: 仅时间
- `table`: 其他

### 7. SuggestionEngine (智能推荐) (V3.5.8)

**4 维推荐**:
- Agent 路由: 11 意图 → 6 Agent
- 工具推荐: 11 意图 → 25 工具
- 追问模板: 12 类型 → 36 追问
- 流程推荐: 复杂度 → DIRECT_QUERY / FILTER_AGG / MULTI_JOIN

**路由示例**:
```
greeting       → chat
question (5W)  → rag
data_query     → analytics.sql_query
data_analyze   → analytics.analyze + chart_gen
data_predict   → analytics.predict (ML 推理)
data_visualize → analytics.chart
data_report    → analytics.report_export
```

---

## 🌐 API 接口

### 1. POST /api/v1/ai/intent/predict

**请求**:
```json
{
  "text": "对比上季度各产品的销售趋势",
  "sessionId": "user-123"
}
```

**响应** (16 字段):
```json
{
  "code": 0,
  "data": {
    "originalText": "对比上季度各产品的销售趋势",
    "normalizedText": "对比上季度各产品的销售趋势",
    "intent": "data_compare",
    "confidence": 0.65,
    "intentScores": {"data_compare": 0.62, "data_analyze": 0.25, ...},
    "entities": [...],
    "slots": {...},
    "urgency": 0.0,
    "sentiment": "neutral",
    "recommendedAgent": "analytics",
    "alternatives": [
      {"intent": "data_analyze", "confidence": 0.25},
      {"intent": "data_query", "confidence": 0.10}
    ],
    "algorithm": "v3.5.6-weighted-voting",
    "questionType": "compare",         // V3.5.8
    "queryComplexity": "simple",       // V3.5.8
    "complexityDetail": {              // V3.5.8
      "timeRanges": 1,
      "aggregations": 0,
      "dimensions": 0
    },
    "suggestedTools": ["data_query", "diff_calc"],  // V3.5.8
    "processSuggestion": "DIRECT_QUERY",            // V3.5.8
    "chartTypeSuggestion": "area",                  // V3.5.8
    "followupQuestions": [                          // V3.5.8
      "需要按什么维度对比?",
      "需要看趋势变化吗?",
      "需要生成对比图吗?"
    ],
    "suggestionConfidence": 0.74,       // V3.5.8
    "dataEntities": {                   // V3.5.8
      "metric": [],
      "dimension": ["产品"],
      "timeRange": ["季度", "上季度"],
      "aggregation": [],
      "dataSource": ["产品"],
      "trend": ["趋势"]
    }
  }
}
```

### 2. GET /api/v1/ai/intent/config

返回当前 IntentConfig (YAML 全部内容)

### 3. PUT /api/v1/ai/intent/config

热更新配置 (不重启服务)

### 4. POST /api/v1/ai/intent/config/reset

重置为 V3.5.6 默认值

### 5. POST /api/v1/ai/intent/benchmark

跑 20 个 benchmark 测试用例, 返回准确度

---

## ⚙️ YAML 配置

### application-intent.yml 路径

`backend/minimax-ai/src/main/resources/application-intent.yml`

### 配置段 (5 大段)

```yaml
intent:
  algorithm: v3.5.6-weighted-voting
  weights: [0.4, 0.3, 0.2, 0.1]   # 4 模型权重
  confidenceScale: 5.0              # tanh 缩放
  
  negation:                         # 否定处理
    scope: 5
    prefixes: [不, 没, 未, 别, ...]
  
  urgent:                           # 紧急度
    words: [急, 紧急, 马上, ...]
    degree: [非常, 特别, 极其, ...]
  
  sentiment:                        # 情感分析
    positive: [好, 棒, 赞, ...]
    negative: [差, 烂, 垃圾, ...]
    degreeWords: {非常: 1.5, 极其: 1.8, ...}
  
  agents:                           # Agent 路由
    query: echo-analyzer
    order: echo-writer
    complaint: echo-reviewer
    ...
  
  keywords:                         # 关键词 (intent -> {word -> weight})
    greeting: {你好: 10, 嗨: 8, ...}
    order: {下单: 10, 买: 8, ...}
    ...
  
  phrases:                          # 关键短语
    complaint: {我要退款: 12, 申请退款: 10, ...}
    ...
  
  synonyms:                         # 同义词
    退款: 退钱,退货
    差: 烂,垃圾
    ...
  
  traditional:                      # 简繁
    訂: 订
    訂單: 订单
    ...
  
  benchmark:                        # 测试用例
    - {text: "我要退款", expected: "complaint"}
    - {text: "你好", expected: "greeting"}
    ...
  
  # V3.5.8 新增
  dataAnalysis:                     # 数据分析
    enabled: true
    intents: {data_query: [...], ...}
    entities: {metric: [...], ...}
    confidenceThreshold: 0.15
  
  questionTypes:                    # 问题类型
    enabled: true
    triggers: {what: [...], how: [...], ...}
  
  suggestion:                       # 智能推荐
    enabled: true
    routing: {greeting: chat, ...}
    tools: {data_query: [sql_query, data_export], ...}
```

---

## 📊 性能 & 准确度

### 性能 (单次 predict)

| 步骤 | 耗时 | 说明 |
|---|---|---|
| 文本归一化 | < 1ms | O(N) |
| 否定作用域 | < 1ms | O(N × M) |
| 4 模型打分 | < 2ms | O(N × K) |
| 模型融合 | < 1ms | O(K) |
| V3.5.8 三组件 | < 1ms | O(N) |
| **合计** | **< 5ms** | 100 字文本 |

### 准确度 (E2E 测试 9 句)

| 版本 | 准确度 | 提升 |
|---|---|---|
| V3.5.5 (单关键词) | 50% | 基准 |
| V3.5.6 (4 模型加权) | 70% | +20pt |
| V3.5.7 (外部化) | 70% | 配置灵活 |
| **V3.5.8 (数据分析)** | **89%** | **+19pt** |

### 9 句测试结果

| 输入 | 意图 | 类型 | 复杂度 | 图表 | 工具 |
|---|---|---|---|---|---|
| 查询上个月的销售数据 | data_query | query | simple | table | sql_query |
| 对比上季度各产品的销售趋势 | data_compare | compare | simple | area | data_query, diff_calc |
| 分析用户增长趋势 | data_analyze | analyze | simple | table | data_query, statistic |
| 预测下月销量 | data_predict | predict | simple | table | data_query, ml_forecast |
| 把上周销售数据画成折线图 | data_visualize | create | simple | area | data_query, chart_generate |
| 生成上月销售报告 | data_report | create | simple | area | data_query, report_template |
| 什么是RAG? | consult | what | simple | table | rag_search |
| 我要投诉 | complaint | general | simple | table | complaint_create |
| 你好 | other (待补) | general | simple | table | - |

**8/9 = 89% 准确度**

---

## 🔧 调试 & 排查

### 启用 DEBUG 日志

```yaml
logging:
  level:
    com.minimax.ai.intent: DEBUG
```

输出示例:
```
[intent] in='对比上季度各产品销售' normalized='对比上季度各产品销售' expansions=[]
[intent] tf scores: {data_compare=0.4, data_analyze=0.2}
[intent] ngram scores: {data_compare=0.6}
[intent] fused: {data_compare=0.58, data_analyze=0.16}
[intent] top: data_compare, conf: 0.65
[intent] dataAnalysis: {data_compare=0.4}
[intent] questionType: compare, complexity: simple
[intent] suggestion: agent=analytics, tools=[data_query, diff_calc], chart=area
```

### 跑 Benchmark

```bash
curl -X POST http://localhost:8094/api/v1/ai/intent/benchmark
```

返回:
```json
{
  "code": 0,
  "data": {
    "total": 20,
    "passed": 14,
    "accuracy": 0.70
  }
}
```

---

## 📚 版本演进

| 版本 | 关键能力 | 准确度 |
|---|---|---|
| V3.5.0 | 单关键词匹配 | 50% |
| V3.5.5 | + 同义词 + 简繁 | 60% |
| V3.5.6 | 4 模型加权投票 (TF+N-gram+同义+上下文) | 70% |
| V3.5.7 | yml 外部化配置 + 热更新 | 70% |
| **V3.5.8** | **+ 数据分析引擎 + 问题分类 + 智能推荐** | **89%** |

---

**最后更新**: 2026-07-14
**作者**: MiniMax Agent Team
