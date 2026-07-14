# Day 29 Report — 2026-07-14

## ✅ Day 29 - 智能化提升：QueryComplexity / SmartModelRouter / IntentConfidenceScorer

**今日完成：**

### 1. QueryComplexityService — 查询复杂度自动分析 (Day 29)

**分析维度与权重：**
- **长度得分 (25%)**: `min(textLen/500, 1.0)`
- **技术术语密度 (30%)**: 命中 `TECH_TERMS`（SQL/代码/API/神经等 40+ 术语）越多越复杂
- **结构复杂度 (20%)**: 数字量 + 多步骤指示词（首先/然后/最后/first/then）
- **意图类型分 (15%)**: 根据 `intentHint` 预设分值（GENERATE_CODE=0.80, CHAT=0.05）
- **上下文复杂度 (10%)**: 含"再/它/好像"等指代词越多越复杂

**等级划分：**
```
score < 0.30  → SIMPLE   (闲聊、快速问答)
score < 0.60  → MEDIUM   (数据查询、统计分析)
score >= 0.60 → COMPLEX  (代码生成、多步分析、图表生成)
```

**额外规则：** 检测到代码片段（`{...}`/SELECT/FUNCTION）直接 `score ≥ 0.70`

### 2. SmartModelRouter — 成本-质量感知模型选择 (Day 29)

**模型能力矩阵：**
| 模型 | Provider | 输入成本 | 能力分 | 适用 |
|---|---|---|---|---|
| MiniMax-Text-01 | self | $0.10/1K | ⭐⭐⭐ | 简单问答 |
| MiniMax-Text-02 | self | $0.30/1K | ⭐⭐⭐⭐ | 数据分析 |
| MiniMax-Text-03 | self | $1.00/1K | ⭐⭐⭐⭐⭐ | 代码生成 |
| GPT4O-Mini | openai | $0.15/1K | ⭐⭐⭐⭐ | 降级 |
| GPT4O | openai | $2.00/1K | ⭐⭐⭐⭐⭐ | 高质量 |
| DeepSeek-Coder | deepseek | $0.50/1K | ⭐⭐⭐⭐⭐ | 代码 |

**路由策略：**
- `SIMPLE + BALANCED/COST_FIRST` → MiniMax-Text-01（快省）
- `SIMPLE + QUALITY_FIRST` → MiniMax-Text-02
- `MEDIUM + BALANCED` → MiniMax-Text-02
- `COMPLEX` → MiniMax-Text-03（高质量兜底：GPT4O-Mini）

**返回内容：** primaryModel + fallbackModel + strategy + estimatedCost + reasoning

### 3. IntentConfidenceScorer — 意图识别置信度评分 (Day 29)

**评分维度：**
- **匹配强度 (40%)**: 命中 `STRONG_SIGNALS`（强信号词）数量
- **意图一致性 (30%)**: 同时命中多种复杂意图 → 降分至 0.5
- **上下文符合度 (20%)**: 含"大概/也许/随便"等模糊词越多越低
- **文本清晰度 (10%)**: 无意义符号过多 → 降分

**置信度等级与降级策略：**
```
confidence ≥ 0.50  → HIGH    ✅ 直接使用识别结果
0.30 ≤ c < 0.50   → MEDIUM  ⚠️  记录但仍使用，建议复核
0.10 ≤ c < 0.30   → LOW     ❌ 考虑降级到 CHAT + 触发澄清
c < 0.10          → UNKNOWN 🚨 直接询问用户意图
```

**强信号词示例：**
- `TRANSFER_HUMAN`: "转人工" "真人" "人工客服"
- `GENERATE_CODE`: "生成代码" "spring boot项目" "create project"
- `GENERATE_MUSIC`: "作曲" "compose music"

**额外修复：** `KeywordEngine.java` 重复 import 清理（`Matcher` ×2 + `Pattern` ×2）

---

**自检结果：**
- 前端构建 (`npm run build`): ✅ 1m 14s

**代码量：**
- `QueryComplexityService.java` 新增 (6.5KB) — 查询复杂度分析
- `SmartModelRouter.java` 新增 (8.5KB) — 成本-质量感知路由
- `IntentConfidenceScorer.java` 新增 (8.4KB) — 置信度评分
- `KeywordEngine.java` 修复重复 import

---

## Day 30 - 待开始

**待做：**
- [ ] 多模型投票（confidence < 0.50 时多个模型同时推理，取共识）
- [ ] RAG 查询重写（用 MiniMax-Text-03 做 query expansion）
- [ ] 智能告警根因分析（告警触发后 LLM 推理可能原因 + 建议操作）
- [ ] 运维日志异常检测（无监督异常检测算法）
