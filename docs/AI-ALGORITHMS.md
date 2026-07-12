# MiniMax AI 核心算法详解 (V3.0.0)

> **自研多模态 AI 平台** — 19 个 AI 工具 · 13 阶段推理管线 · ReAct Agent 框架

## 一、Top-K + Top-P 采样 (Token 解码)

**位置**: `minimax-ai/.../pipeline/stage/ModelInference.java#sampleTopKTopP`

### 1.1 算法背景

LLM 解码时, 朴素做法 (argmax) 会导致重复/退化. 随机采样增加多样性, 但完全随机会引入低质量 token. Top-K 和 Top-P 组合使用是当前主流方案.

| 参数 | 作用 | 范围 | 推荐值 |
|------|------|------|--------|
| `temperature` | 分布尖锐度 | (0, ∞) | 1.0 |
| `top_k` | 候选数 | [1, vocab] | 50 |
| `top_p` | 累积概率 | (0, 1] | 0.9 |

### 1.2 算法步骤

1. **温度缩放**: `logits ← logits / T`
   - T→0 近似贪心, T→∞ 趋近均匀
2. **Softmax (数值稳定)**: `logits ← softmax(logits - max)`
3. **排序**: 按概率降序排 `id`
4. **Top-K 截断**: 保留前 K 个, 其余置 0
5. **Top-P (Nucleus) 截断**: 累积到 p 截止
6. **重归一化**: 概率和 = 1
7. **多项式采样**: Inverse CDF 采样

### 1.3 复杂度

`O(N log N)`, N=vocab_size (通常 8K-32K)

### 1.4 参数效应

| 场景 | T | K | P |
|------|---|---|---|
| 通用对话 | 1.0 | 50 | 0.9 |
| 代码补全 | 0.3 | 10 | 0.5 |
| 创意写作 | 1.5 | 100 | 0.95 |

## 二、EMA 指数移动平均 (训练追踪)

**位置**: `minimax-ai/.../training/TrainingTracker.java#ema`

### 2.1 公式

```
EMA_t = α · value_t + (1 - α) · EMA_{t-1}
```

### 2.2 直觉

简单滑动平均对窗口内等权; EMA 对近期值赋高权重, 远期值指数衰减.

| α | 效果 | 用途 |
|---|------|------|
| 0.1 | 强平滑 | 训练 loss 曲线 (推荐) |
| 0.3 | 中等 | 实时监控 |
| 0.9 | 弱平滑 | 几乎跟原始 |

### 2.3 复杂度

`O(N)`, 一次扫描

## 三、Haversine 球面距离 (POI 检索)

**位置**: `minimax-ai/.../framework/location/GeoUtils.java#haversine`

### 3.1 公式

```
a = sin²(Δφ/2) + cos(φ1)·cos(φ2)·sin²(Δλ/2)
c = 2·atan2(√a, √(1−a))
d = R·c   (R = 6371 km)
```

### 3.2 为什么不用平面勾股?

平面勾股在 1km 距离误差 0.5%, 在 100km+ 跨城市误差 5%+. Haversine 考虑地球曲面, 误差 < 0.1%.

### 3.3 atan2 优势

`atan2(√a, √(1-a))` 比 `acos(1-a)` 数值稳定, 避免边界精度损失.

### 3.4 复杂度

`O(1)`

## 四、CRDT 协作编辑

**位置**: `minimax-ws/.../collab/CrdtEngine.java#renderText`

### 4.1 核心数据结构

```java
class CrdtId { String clientId; long clock; }
class CrdtItem { CrdtId id; String content; CrdtId parentId; }
class DocState { LinkedHashMap<String, CrdtItem> items;
                Set<String> tombstones; long version; }
```

### 4.2 三键复合排序

```java
sort(items, by(parentId).nullsFirst
            .thenBy(clientId)
            .thenBy(clock));
```

### 4.3 强最终一致性

两个客户端并发插入同一位置, 无论接收顺序, 重建后顺序一致 — CRDT 的核心保证.

### 4.4 Tombstone

已删除的 item 保留在 items (供 undo), 但不输出文本.

### 4.5 复杂度

`O(N log N)`, N=item 数

## 五、关键词 TF 意图识别

**位置**: `minimax-ai/.../generation/KeywordEngine.java#recognize`

### 5.1 三级匹配

1. **正则**: 高精度 (e.g. `生成.*?柱状图` → CHART)
2. **错别字纠正**: 50+ 中文错字 + 拼音首字母 + 编辑距离
3. **关键词 TF**: 命中数最多的意图

### 5.2 平手规则

`ANALYZE_DATA > QUERY_DATA` (分析优先于查询)

### 5.3 复杂度

`O(N × M)`, N=text 长度, M=关键词数

## 六、ReAct Agent 框架

**位置**: `minimax-ai/.../framework/agent/`

### 6.1 循环结构

```java
while (steps < maxSteps) {
    thought = model.generate(prompt + history);
    action = parseAction(thought);
    if (action.isFinish()) return action.result;
    observation = toolRegistry.invoke(action);
    history.add(thought, action, observation);
}
```

### 6.2 工具注册

```java
@Tool(name="weather", desc="查询天气")
public Result getWeather(@Param("city") String city) {
    return weatherService.query(city);
}
```

### 6.3 复杂度

`O(K × T)`, K=步数, T=工具调用时间

## 七、Pipeline 13 阶段

**位置**: `minimax-ai/.../pipeline/PipelineExecutor.java`

```
USER_INPUT → GATEWAY_DISPATCH → MULTIMODAL_PARSE → CONTEXT_ASSEMBLE
  → PRE_RISK → RAG_TOOL_AGENT → TOKENIZE → MODEL_GENERATE
  → TOKEN_DECODE → POST_RISK → FORMAT → LOG_STORE → RETURN
```

每个阶段独立可观测, 失败可重试.

## 八、性能基准 (V3.0.0)

| 算法 | 单次耗时 | CPU |
|------|----------|-----|
| Top-K-P 采样 | < 5ms | 8K vocab |
| EMA 100点 | < 0.1ms | - |
| Haversine | < 1μs | - |
| CRDT render 1000字 | < 10ms | - |
| Keyword 识别 | < 5ms | - |
| Pipeline (13阶段) | < 100ms | - |
