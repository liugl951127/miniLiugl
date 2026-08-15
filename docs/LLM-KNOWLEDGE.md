# MiniMax Platform — 大模型知识详解

> 涵盖 LLM 原理、训练流程、核心算法、平台实现细节

---

## 目录

1. [大模型基础原理](#1-大模型基础原理)
2. [Transformer 架构](#2-transformer-架构)
3. [注意力机制详解](#3-注意力机制详解)
4. [大模型训练流程](#4-大模型训练流程)
5. [RAG 检索增强生成](#5-rag-检索增强生成)
6. [Agent 智能体编排](#6-agent-智能体编排)
7. [Function Calling 工具调用](#7-function-calling-工具调用)
8. [向量嵌入与相似度检索](#8-向量嵌入与相似度检索)
9. [Prompt 工程](#9-prompt-工程)
10. [平台实现细节](#10-平台实现细节)

---

## 1. 大模型基础原理

### 1.1 什么是大语言模型 (LLM)

大语言模型是基于深度学习的自然语言处理模型，通过海量文本数据学习语言的统计规律，具备：

- **文本生成**：给定前缀续写下文
- **文本理解**：分类、摘要、情感分析
- **指令遵循**：理解自然语言指令并执行
- **知识推理**：多步逻辑推理、数学解题
- **代码生成**：根据描述生成可执行代码

### 1.2 Scaling Law (规模定律)

模型能力随参数量、数据量、计算量的增长而提升：

```
Loss(N, D) = (N_c / N)^α + (D_c / D)^β + const

N: 参数量
D: 训练 token 数
α ≈ 0.076, β ≈ 0.103
```

**关键阈值**（涌现能力 Emergent Abilities）：

| 参数量 | 涌现能力 |
|--------|---------|
| < 7B | 基础对话、简单推理 |
| 7B~13B | 链式推理、多步计算 |
| 70B+ | 复杂推理、代码生成、长上下文 |
| 100B+ | 接近人类水平的复杂任务 |

### 1.3 Tokenization (分词)

将文本转为模型可处理的 token 序列：

```
文本: "你好，世界！"
     ↓ Tokenize
Tokens: [ 你好 , ， , 世界 , ！ ]
Token IDs: [ 342, 12, 4563, 888 ]
```

**常见分词器**：
- Byte-Pair Encoding (BPE)：GPT 系列
- WordPiece：BERT、XLNet
- SentencePiece：多语言统一
- Tiktoken：OpenAI 专用（快速、精确）

**Token 计算**：
```
中文字符 ≈ 1.3~2.0 tokens/字
英文单词 ≈ 0.75 tokens/词
```

---

## 2. Transformer 架构

### 2.1 整体结构

```
Input Tokens
     ↓
Embedding Layer (+ Positional Encoding)
     ↓
┌─────────────────────────────────┐
│     N × Encoder Layer           │
│  ┌─────────────────────────┐   │
│  │ Multi-Head Self-Attention│   │
│  │     + Add & Norm        │   │
│  │ Feed-Forward Network     │   │
│  │     + Add & Norm        │   │
│  └─────────────────────────┘   │
└─────────────────────────────────┘
     ↓
Output Probabilities
```

### 2.2 Encoder vs Decoder

| 特性 | Encoder-only (BERT) | Decoder-only (GPT) | Encoder-Decoder (T5) |
|------|---------------------|--------------------|---------------------|
| 注意力 | 双向 (bidirectional) | 单向 (causal) | 混合 |
| 典型任务 | 分类、NER、QA | 文本生成 | 翻译、摘要 |
| 优势 | 理解能力强 | 生成能力强 | 通用性强 |
| 代表模型 | BERT, RoBERTa | GPT, LLaMA, ChatGLM | T5, BART |

### 2.3 Positional Encoding (位置编码)

Transformer 本身无序列顺序感，通过位置编码注入位置信息：

**正弦/余弦编码（原始 Transformer）**：
```python
PE(pos, 2i)   = sin(pos / 10000^(2i/d_model))
PE(pos, 2i+1) = cos(pos / 10000^(2i/d_model))
```

**旋转位置编码 RoPE（LLaMA 等）**：
```python
# 将绝对位置编码为旋转矩阵
Q' = R(θ, m) · Q
K' = R(θ, m) · K
Attention(Q', K', V) = softmax(Q'K'^T / √d) · V
```

**ALiBi（不需要训练的位置编码）**：
```python
# 位置偏差加到 attention score
score = (Q[i] · K[j]) - λ * |i - j|
```

---

## 3. 注意力机制详解

### 3.1 Scaled Dot-Product Attention

核心公式：
```
Attention(Q, K, V) = softmax(QK^T / √d_k) · V
```

**为什么除以 √d_k？**
- 当 d_k 较大时，QK^T 的方差会随维度增大 → softmax 梯度消失
- 除以 √d_k 使方差稳定

### 3.2 Multi-Head Attention (多头注意力)

```
Q, K, V 分别经过线性变换:
  Q_i = Q · W_i^Q
  K_i = K · W_i^K
  V_i = V · W_i^V

每个头的注意力:
  head_i = Attention(Q_i, K_i, V_i)

拼接所有头:
  MultiHead = Concat(head_1, ..., head_h) · W^O
```

**各头学习不同表示**：
- 头1：句法结构
- 头2：语义关系
- 头3：位置信息
- 头4：长距离依赖

### 3.3 Flash Attention

标准 attention 的复杂度是 O(N²) 显存，Flash Attention 通过 **分块计算** 降低到 O(N)：

```
标准: 一次性加载整个 Q, K, V 矩阵
Flash: 分块加载 (block_size=64/128)
       SRAM 计算 → HBM 存储
       减少 HBM 访问次数
```

**平台实现**：
```java
// MiniMax Agent Engine 使用 ONNX Runtime
// ONNX Runtime 内置 Flash Attention 优化
onnxSession.run(new String[]{outputName}, inputs)
```

### 3.4 KV Cache (键值缓存)

自回归生成时，已计算的历史 token 的 K 和 V 可以缓存：

```
生成第 t+1 个 token:
  当前 Q_t+1 + 缓存的 K_1...t + V_1...t
  = 只需计算当前 token 的 attention
```

**问题**：KV Cache 随序列长度线性增长，占用大量显存
**解决**：Grouped Query Attention (GQA)、Paged Attention (vLLM)

---

## 4. 大模型训练流程

### 4.1 三阶段训练范式

```
┌─────────────────────────────────────────────────┐
│ 阶段 1: 预训练 (Pre-training)                    │
│ 大规模无标注语料 → 基座模型                       │
│ 下一个 token 预测                               │
│ 计算量: 10^23 FLOPs+                           │
└─────────────────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────┐
│ 阶段 2: 监督微调 (SFT)                          │
│ 人工标注的问答对 → 对话模型                       │
│ 计算量: ~10^22 FLOPs                            │
└─────────────────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────┐
│ 阶段 3: 对齐训练 (RLHF / DPO)                   │
│ 人类反馈 → 符合人类偏好                          │
│ 计算量: ~10^21 FLOPs                            │
└─────────────────────────────────────────────────┘
```

### 4.2 预训练 (Pre-training)

**数据处理**：
1. **数据收集**：网页、书籍、代码、论文、对话
2. **质量过滤**：规则过滤 + 分类器过滤低质量内容
3. **去重**：MinHash / SimHash 去除重复文档
4. **分词**：SentencePiece / Tiktoken
5. **数据配比**：中英混合、多领域平衡

**训练目标**：下一个 token 预测（Next Token Prediction）
```python
loss = -Σ log P(token_t | token_1, ..., token_t-1)
```

**混合精度训练**：
- FP16/BF16 前向+反向传播
- FP32 优化器状态（AdamW）
- 梯度检查点（Gradient Checkpointing）节省显存

### 4.3 监督微调 (SFT)

**数据格式**（ChatML）：
```json
[
  {"role": "system", "content": "你是 MiniMax AI 助手"},
  {"role": "user", "content": "什么是 RAG？"},
  {"role": "assistant", "content": "RAG 是检索增强生成..."}
]
```

**格式化为 token 序列**：
- system + user + assistant 拼接
- 只在 assistant 部分计算 loss（其他为 context）

**LoRA / QLoRA 高效微调**：
```
原始权重: W ∈ R^(d×k)
LoRA 更新: ΔW = A · B (A ∈ R^(r×k), B ∈ R^(d×r), r << min(d,k))
训练: 只更新 A 和 B，冻结 W
显存节省: ~60-70%
```

### 4.4 RLHF (人类反馈强化学习)

**三步流程**：

```
Step 1: 训练 Reward Model (奖励模型)
  人类偏好排序 → 二分类器
  loss = -log(σ(r_pos - r_neg))

Step 2: PPO 微调
  策略模型生成回复
  Reward = RM_score - β·KL(π_new || π_ref)
  PPO-clip 防止策略过大变化

Step 3: 迭代优化
  多次 RM 训练 + PPO
```

**问题**：RLHF 训练不稳定、计算成本高

### 4.5 DPO (Direct Preference Optimization)

简化版对齐算法，不需要 Reward Model 和 PPO：

```python
# DPO Loss
pos_logprob = log_prob(π_θ, chosen)
neg_logprob = log_prob(π_θ, rejected)

loss = -log(σ(β * (pos_logprob - neg_logprob)))
```

**优势**：无需额外 RM，训练更稳定，效果与 RLHF 相当

---

## 5. RAG 检索增强生成

### 5.1 为什么需要 RAG

LLM 的局限性：
- **知识截止**：训练数据有时效，过期知识无法更新
- **幻觉**：生成看似合理但错误的内容
- **私有知识**：企业数据、用户数据不在模型中

RAG 解决方案：
```
用户问题 + 检索到的外部知识 → LLM → 准确回答
```

### 5.2 RAG 完整流程

```
┌──────────────────────────────────────────────────────────┐
│                     RAG Pipeline                          │
│                                                          │
│  ┌────────────┐    ┌────────────┐    ┌────────────┐   │
│  │  文档上传   │ → │   分块      │ → │  Embedding  │   │
│  │  PDF/Word  │    │  512-1024  │    │  向量化     │   │
│  │  Markdown   │    │  tokens    │    │  1536 dim  │   │
│  └────────────┘    └────────────┘    └─────┬──────┘   │
│                                            ↓             │
│  ┌────────────┐    ┌────────────┐    ┌────────────┐   │
│  │  回答生成  │ ← │ Prompt 组装 │ ← │ Top-K 检索  │   │
│  │  LLM      │    │ + 上下文   │    │  相似度   │   │
│  │  Streaming│    │ 注入       │    │  > 0.75   │   │
│  └────────────┘    └────────────┘    └────────────┘   │
└──────────────────────────────────────────────────────────┘
```

### 5.3 文档分块 (Chunking) 策略

| 策略 | 块大小 | 适用场景 |
|------|--------|---------|
| 固定大小 | 512 tokens | 通用场景 |
| 滑动窗口 | 512 + 128 重叠 | 跨块语义连贯 |
| 语义分块 | 按段落/sentence | 问答系统 |
| 层级分块 | 章节→段落 | 长文档 |

**重叠策略**：
```
[块1: tokens 1-512]  [块2: tokens 385-897]
      ↑ 重叠 128 tokens ↑
```

### 5.4 Embedding 模型

**常用模型**：
- `text-embedding-ada-002` (OpenAI)
- `bge-large-zh` (BAAI, 中文优化)
- `m3e-large` (MokaAI)
- `jina-embeddings-v2` (Jina)

**Embedding 质量指标**：
```python
# Cosine Similarity
similarity = dot(a, b) / (||a|| * ||b||)
# 范围: [-1, 1], 越接近 1 表示越相似
```

**批量化 Embedding**：
```java
// MiniMax Platform 实现
List<float[]> batchEmbed(List<String> texts) {
    // 批量调用 embedding 服务
    // 节省网络开销: 100 条 / batch vs 100 次单条
}
```

### 5.5 向量检索

**HNSW (Hierarchical Navigable Small World)**：
```python
# 构建: O(N log N)
# 查询: O(log N)
# 内存占用较高，但查询速度极快
index = hnswlib.Index(space='cosine', dim=1536)
index.add_items(vectors, ids=range(n))
results = index.knn_query(query_vector, k=10)
```

**向量数据库选型**：

| 数据库 | 优势 | 适用场景 |
|--------|------|---------|
| Milvus | 性能最强、分布式 | 大规模生产 |
| Qdrant | Rust 实现、过滤强 | 实时系统 |
| ChromaDB | 轻量、易用 | 原型/小规模 |
| Pinecone | 托管服务、云原生 | SaaS |
| FAISS | Facebook 开源、免费 | 自托管 |

**平台实现**：
```yaml
# MiniMax Platform H2 Fallback (无 Milvus 时)
# 使用内存索引模拟向量检索
rag:
  use-milvus: false
  fallback: memory  # H2 内存模式
```

### 5.6 Hybrid Search (混合检索)

单一检索的局限性 → 混合检索：

```
BM25 稀疏检索 (关键词匹配)
    +
向量相似度检索 (语义理解)
    ↓
RRF (Reciprocal Rank Fusion) 融合
    ↓
Top-K 最终结果
```

```python
# RRF 公式
score(doc) = Σ 1 / (k + rank_i(doc))
# k: 平滑因子 (通常 k=60)
# rank_i: 检索方法 i 中 doc 的排名
```

### 5.7 Re-ranking (重排序)

初检结果可能不精准 → Cross-Encoder 重排序：

```
初检 (Bi-Encoder): 1000 条
    ↓
Cross-Encoder 打分: cross_encode(query, doc) → score
    ↓
Top-20 精排结果
    ↓
LLM 生成回答
```

**BGE-Reranker**：
```python
from sentence_transformers import CrossEncoder
reranker = CrossEncoder('BAAI/bge-reranker-large')
scores = reranker.predict([(query, doc) for doc in candidates])
```

---

## 6. Agent 智能体编排

### 6.1 Agent 定义

Agent = LLM + Planning + Memory + Tools

```
Agent
├── Planning (规划)
│   ├── 任务分解 (Task Decomposition)
│   ├── 计划执行 (Plan Execution)
│   └── 自我反思 (Self-Reflection)
├── Memory (记忆)
│   ├── 短期记忆 (Short-term, 当前会话)
│   └── 长期记忆 (Long-term, 持久化知识)
├── Tools (工具)
│   ├── RAG (知识检索)
│   ├── Web Search (网络搜索)
│   ├── Calculator (计算器)
│   └── API Call (接口调用)
└── Action (执行)
```

### 6.2 规划策略

**ReAct (Reason + Act)**：
```python
thought = "用户想知道今天的天气，我需要先调用天气 API"
action = "call_weather(tool)"
observation = "北京：25°C，晴"
# 下一步思考基于 observation 继续...
```

**CoT (Chain of Thought)**：
```python
# 不直接回答，分步推理
answer = """
1. 首先，我需要理解这道数学题
2. 已知条件：x + y = 10，x = 6
3. 代入：6 + y = 10
4. 解得：y = 4
"""
```

**ToT (Tree of Thought)**：
```
        问题
       /    \
    方案A    方案B    ← 分支探索
    / \      / \
  合理 差  合理  差    ← 评估各分支
    \    /    /
      最佳路径              ← 选择最优
```

### 6.3 MiniMax Agent Orchestrator

**平台架构**：
```java
// AgentOrchestrator.java - 核心编排器
public class AgentOrchestrator {
    Planner planner;      // 计划生成
    Executor executor;     // 执行引擎
    Memory memory;         // 记忆管理
    ToolRegistry tools;     // 工具注册表
}

// Plan → PlanStep[] → 拓扑排序 → Kahn 算法
public class PlanExecutor {
    public ExecutionResult execute(Plan plan) {
        List<PlanStep> sorted = topologicalSort(plan.steps);
        for (PlanStep step : sorted) {
            // 占位符替换: ${stepId.output}
            String resolved = replacePlaceholders(step.prompt);
            // 执行 step
            ToolResult result = toolBus.execute(step.tool, resolved);
            // 缓存结果供后续步骤使用
            context.put(step.id + ".output", result);
        }
    }
}
```

### 6.4 Plan & PlanStep 契约

```java
public class Plan {
    String id;
    String task;
    List<PlanStep> steps;    // 有向无环图
    PlanStatus status;
}

public class PlanStep {
    String id;                // 唯一标识
    String tool;             // 工具名
    String prompt;            // 工具输入（支持 ${otherStep.output } 占位符）
    ToolResult result;       // 执行结果
    RiskLevel risk;           // LOW / MEDIUM / HIGH / CRITICAL
}

// Risk 拦截
if (step.risk == RiskLevel.CRITICAL) {
    // 需要人工审批
    pendingApproval(step);
}
```

---

## 7. Function Calling 工具调用

### 7.1 什么是 Function Calling

让 LLM 调用外部工具/函数的能力：

```json
用户: "北京今天多少度？"

LLM 返回:
{
  "function_call": {
    "name": "get_weather",
    "arguments": {
      "city": "北京",
      "date": "2026-08-11"
    }
  }
}
```

### 7.2 Function Calling 流程

```
用户问题
  ↓
Function Calling 定义注入 Prompt
  ↓
LLM 识别意图 → 调用函数
  ↓
函数执行 (外部 API / 数据库 / 计算)
  ↓
函数结果注入上下文
  ↓
LLM 生成最终回答
```

### 7.3 MiniMax 平台实现

```java
// FunctionCallService.java
public class FunctionCallService {
    public FunctionCallResult execute(FunctionCall call) {
        // 1. 解析函数名和参数
        String functionName = call.getName();
        Map<String, Object> args = call.getArguments();

        // 2. 查找注册的函数
        RegisteredFunction func = registry.get(functionName);

        // 3. 权限检查
        if (!permissionService.canInvoke(userId, functionName)) {
            throw new AccessDeniedException(functionName);
        }

        // 4. 执行
        Object result = func.invoke(args);

        // 5. 结果格式化
        return new FunctionCallResult(functionName, result);
    }
}
```

### 7.4 函数定义格式（JSON Schema）

```json
{
  "name": "get_weather",
  "description": "获取指定城市和日期的天气",
  "parameters": {
    "type": "object",
    "properties": {
      "city": {
        "type": "string",
        "description": "城市名，如 北京、上海"
      },
      "date": {
        "type": "string",
        "description": "日期，格式 YYYY-MM-DD"
      }
    },
    "required": ["city"]
  }
}
```

---

## 8. 向量嵌入与相似度检索

### 8.1 Embedding 原理

将文本映射到高维向量空间：
```
"你好"    → [0.12, -0.34, 0.56, ...] (1536维)
"hello"  → [0.13, -0.33, 0.58, ...]  (相似)
"飞机"   → [-0.45, 0.21, -0.12, ...]  (不相似)
```

**训练目标**：语义相近的文本向量距离近

### 8.2 常用 Embedding 模型对比

| 模型 | 维度 | 中文 | MTEB 得分 | 备注 |
|------|------|------|----------|------|
| text-embedding-ada-002 | 1536 | 一般 | ~60 | OpenAI 官方 |
| bge-large-zh-v1.5 | 1024 | 强 | ~64 | BAAI |
| m3e-large | 1024 | 强 | ~63 | MokaAI |
| jina-embeddings-v2 | 1024 | 一般 | ~64 | Jina |
| BGE-M3 | 1024 | 强 | ~66 | 多语言 |

### 8.3 相似度度量

```python
# Cosine Similarity (余弦相似度，最常用)
cos_sim(a, b) = dot(a, b) / (||a|| * ||b||)

# Dot Product (点积，未归一化时使用)
dot_sim(a, b) = dot(a, b)

# Euclidean Distance (欧氏距离)
euclidean(a, b) = ||a - b||

# MNR (Maximum Marginal Relevance) - 多样性检索
MMR = argmax [λ * sim(d_i, q) - (1-λ) * max_j sim(d_i, d_j)]
```

### 8.4 ANN 索引算法

**HNSW 原理**：
```
Layer 2:    A ─────────── E        (稀疏，粗粒度搜索)
Layer 1:  A ─ B ─ C ─ D ─ E      (中等密度)
Layer 0:  A─B─C─D─E─F─G─H─I─J    (最密，全量数据)

搜索：从顶层开始贪心搜索最近邻 → 下降到下一层 → 直到最底层
```

### 8.5 平台 Embedding 服务

```java
// EmbeddingService.java
@Service
public class EmbeddingService {
    public List<float[]> batchEmbed(List<String> texts) {
        // 支持多后端:
        // 1. OpenAI API (openai-api-key)
        // 2. DeepSeek API (成本低)
        // 3. MiniMax 自研模型 (on-premise)
        // 4. Mock (沙箱模式)
        if (useMock) return mockEmbed(texts);
        return openAI.embed(texts);
    }
}
```

---

## 9. Prompt 工程

### 9.1 Prompt 结构

```
┌─────────────────────────────────────────────────┐
│ <system>                                         │
│ 角色设定：你是一个专业的 Java 后端工程师         │
│ 写作风格：简洁、注重实践、避免废话               │
│ </system>                                       │
│                                                  │
│ <user>                                          │
│ 问题：Spring Boot 如何实现异步？                  │
│                                                  │
│ 要求：                                          │
│ 1. 给出完整代码示例                              │
│ 2. 解释关键注解                                   │
│ 3. 注意事项                                       │
│ </user>                                         │
└─────────────────────────────────────────────────┘
```

### 9.2 常用策略

**Few-shot (少样本)**：
```
任务：情感分类

示例：
"今天心情很好" → 正面
"考试没考好"  → 负面

"吃了火锅"    → ?
```

**CoT (思维链)**：
```
请分步骤回答：123 × 456 = ?
答案格式：
Step 1: ...
Step 2: ...
Final: ...
```

**ICL (In-Context Learning)**：
- 无需梯度更新，通过示例学习新任务
- 示例数量和排列顺序影响效果

### 9.3 温度 (Temperature) 参数

| Temperature | 行为 | 适用场景 |
|-------------|------|---------|
| 0.0 | 确定性输出，总选最高概率 | 代码生成、数学 |
| 0.3 | 轻微随机 | 分类、提取 |
| 0.7 | 有创意但不离谱 | 对话、内容创作 |
| 1.0+ | 高随机性 | 头脑风暴 |

```python
# Temperature 控制 softmax 分布
probs = softmax(logits / T)
# T 高 → 分布平滑 → 随机性大
# T 低 → 分布尖锐 → 确定性大
```

### 9.4 平台 Prompt 模板

```java
// MiniMax ChatRequest.java
public class ChatRequest {
    String systemPrompt;    // 角色 + 写作风格
    List<Message> history;   // 对话历史 (含 System/User/Assistant)
    String currentQuery;    // 当前问题

    // 构建最终 Prompt
    public String buildPrompt() {
        return format("""
            <system>
            {systemPrompt}
            当前时间: {now}
            用户信息: {userProfile}
            </system>
            {history}
            <user>
            {currentQuery}
            </user>
            """);
    }
}
```

---

## 10. 平台实现细节

### 10.1 模型路由 (Model Router)

```java
// ModelRouterService.java
public class ModelSelector selectModel(TaskRequest request) {
    // 1. 任务分类
    if (request.isCodeGen()) return model("code-agent");    // 代码专用模型
    if (request.isLongContext()) return model("chatglm-32k"); // 长上下文
    if (request.isSimple()) return model("deepseek-chat");  // 简单任务用便宜模型

    // 2. 成本优化
    if (budget < 0.01) return model("deepseek-chat");       // 优先便宜模型

    // 3. 默认兜底
    return model("gpt-4o-mini");
}
```

### 10.2 Streaming SSE 实现

```java
// AiController.java - SSE 流式响应
@GetMapping(value = "/ai/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(ChatRequest request) {
    return modelClient.stream(request)
        .map(token -> "data: " + token + "\n\n")
        .concatWith(Flux.just("data: [DONE]\n\n"));
}
```

### 10.3 ONNX Runtime 自研推理

```java
// OnnxInferenceEngine.java
public class OnnxInferenceEngine {
    // 加载 ONNX 模型 (如 ChatGLM, Qwen)
    private OrtEnvironment env;
    private OrtSession session;

    public List<String> generate(String prompt, int maxTokens) {
        // 1. Tokenize
        long[] inputIds = tokenizer.encode(prompt);

        // 2. ONNX 推理 (batch=1)
        float[][] logits = session.run(new float[][]{inputIds});

        // 3. Sampling (temperature, top_p)
        int nextToken = sample(logits[logits.length-1]);

        // 4. 解码 → token → string
        return greedy_decode(nextToken);
    }
}
```

### 10.4 多轮对话上下文管理

```java
// ChatSessionService.java
public class AiChatSession {
    // 存储对话历史
    List<Message> messages = new ArrayList<>();

    // 上下文窗口管理
    int MAX_TOKENS = 128000;
    void addMessage(Message msg) {
        messages.add(msg);
        // 超过窗口大小时，压缩或截断
        while (estimateTokens(messages) > MAX_TOKENS) {
            messages.remove(1); // 保留 system prompt
        }
    }
}
```

### 10.5 MiniMax Agent Skill Engine

```java
// SkillEngine.java - 可扩展的工具集
public class SkillEngine {
    // 注册技能
    Map<String, Skill> skills = new HashMap<>();

    // 技能定义
    public class Skill {
        String name;
        String description;       // LLM 用于理解何时调用
        String category;          // LOW / MEDIUM / HIGH / CRITICAL
        Function<JSONObject, Object> handler;
    }

    // 内置技能
    // - calculator: 数学计算
    // - web_search: 网络搜索
    // - file_reader: 读取文件
    // - code_executor: 执行代码
    // - api_caller: 调用外部 API
}
```

---

## 附录 A: 数学符号表

| 符号 | 含义 |
|------|------|
| LLM | Large Language Model, 大语言模型 |
| Transformer | 注意力机制驱动的序列模型 |
| Attention | QK^T / √d → softmax → V |
| RLHF | Reinforcement Learning from Human Feedback |
| DPO | Direct Preference Optimization |
| RAG | Retrieval-Augmented Generation, 检索增强生成 |
| LoRA | Low-Rank Adaptation, 低秩适配 |
| GQA | Grouped Query Attention |
| HNSW | Hierarchical Navigable Small World |
| ReAct | Reasoning + Acting |
| CoT | Chain of Thought |
| ToT | Tree of Thought |
| ICL | In-Context Learning |
| MTEB | Massive Text Embedding Benchmark |
| SSE | Server-Sent Events |
| ONNX | Open Neural Network Exchange |

---

## 附录 B: 平台 API 路径速查

| 功能 | 路径 | 方法 |
|------|------|------|
| AI 对话 | `/api/v1/ai/chat` | POST |
| AI 对话流式 | `/api/v1/ai/chat/stream` | GET |
| RAG 检索 | `/api/v1/rag/retrieve` | POST |
| RAG 问答 | `/api/v1/rag/ask` | POST |
| 文档上传 | `/api/v1/rag/doc/upload` | POST |
| Embedding | `/api/v1/ai/embed` | POST |
| 模型列表 | `/api/v1/model/providers` | GET |
| Agent 执行 | `/api/v1/agent/run` | POST |
| Agent Plan | `/api/v1/agent/plan` | POST |
| 工具调用 | `/api/v1/ai/tools/{code}/invoke` | POST |

---

*文档版本: V6.8.10 | 更新: 2026-08-11*
