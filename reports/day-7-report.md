# Day 7 报告 - 长期记忆 (向量库) + 跨会话召回

**日期**: 2026-06-16
**目标**: 把短期记忆扩展为完整的"短期 + 长期 + 偏好"三段式记忆体系
**Commit**: pending

---

## ✅ 完成项

### 1. 数据模型 (3 张表)
- ✅ `memory_long_term` — 长期记忆：content + summary + role + embedding (LONGBLOB) + dim + importance + tags + access_count
- ✅ `memory_user_pref` — 用户偏好：userId + prefKey + prefValue + weight + source
- ✅ H2 schema 兼容版（schema-h2.sql）

### 2. Embedding 抽象层
- ✅ `EmbeddingClient` 接口 — 适配 OpenAI / Minimax-M3 / Ollama / 智谱 / 通义千问
- ✅ `OpenAiEmbeddingClient` — 调 `/embeddings` 端点，OpenAI 协议
- ✅ `MockEmbeddingClient` (offline) — 64 维伪向量 (2-gram 哈希 → 投射 → L2 归一化)
  - **特性**: 相同输入 → 完全相同向量 (cosine=1.0)
  - **降级策略**: API 失败 → 返回零向量，业务不中断

### 3. 核心 Service
- ✅ `LongTermMemoryService.store()` — 向量化 + 入库 (LONGBLOB 存 float[])
- ✅ `LongTermMemoryService.recall()` — 全量扫描 + 余弦相似度 + 排序 + topK
- ✅ `LongTermMemoryService.recent()` — 列最近 N 条
- ✅ `UserPrefService` — KV 存储偏好 (set/get/list/delete)
- ✅ `VectorUtils` — float[]↔byte[] + cosine 工具

### 4. 跨会话上下文构建 (Day 7 核心价值)
- ✅ `CrossSessionContextBuilder` — 按重要性降序拼装 messages:
  1. system prompt
  2. 用户偏好 ("我之前说过我喜欢 X")
  3. 跨会话记忆召回 ("我们上次聊过 X")
  4. 本会话最近 N 条
  5. 当前问题
- ✅ Token 预算管理 (maxContext * 0.8)
- ✅ 记忆命中自动 `touchAccess` (access_count++)

### 5. 真实 LLM 摘要升级 (替换 Day 6 占位)
- ✅ `LlmSummarizer` — 调 model 服务的 `/api/v1/models/chat` (OpenAI 兼容)
- ✅ 失败降级：截前 60 字符
- ✅ 可配置 model name + token

### 6. 暴露的 HTTP 端点 (16 个)
| 方法 | 路径 | 功能 |
|---|---|---|
| GET    | `/memory/short-term/{sid}` | 拉短期记忆 |
| POST   | `/memory/short-term/{sid}` | 追加 |
| DELETE | `/memory/short-term/{sid}` | 清空 |
| GET    | `/memory/short-term/{sid}/size` | 统计 |
| POST   | `/memory/cross-context` | **跨会话 context (短期+长期+偏好)** |
| POST   | `/memory/context/{sid}` | 单会话 context |
| POST   | `/memory/summarize/{sid}` | LLM 摘要 |
| GET    | `/memory/summary/{sid}` | 读摘要 |
| POST   | `/memory/long-term` | 存长期记忆 |
| POST   | `/memory/long-term/recall` | **向量召回** |
| GET    | `/memory/long-term/recent` | 列最近 |
| DELETE | `/memory/long-term/{id}` | 删除 |
| PUT    | `/memory/pref/{key}` | 设置偏好 |
| GET    | `/memory/pref/{key}` | 读偏好 |
| GET    | `/memory/pref` | 列出所有偏好 |
| DELETE | `/memory/pref/{key}` | 删除偏好 |

---

## 📊 关键数据

| 指标 | Day 6 | Day 7 | 增量 |
|------|-------|-------|------|
| Java 文件 | 93 | **109** | +16 |
| Java 行数 | 4329 | **5457** | +1128 |
| SQL/XML 行数 | 1633 | **2193** | +560 |
| 单元/集成测试 | 21 | **43** | +22 |
| 端点 (memory 模块) | 6 | **16** | +10 |
| 数据表 (memory) | 0 (H2 only) | **2** | +2 (MySQL 真表) |

### 单元/集成测试明细
- `VectorUtilsTest` — 7 cases (roundTrip + cosine 数学)
- `MockEmbeddingClientTest` — 5 cases (确定性 + 归一化)
- `ContextBuilderTest` — 4 cases (Day 6 沿用)
- `ShortTermMemoryTest` — 4 cases (Day 6 沿用)
- **`MemoryIntegrationTest` (新增)** — 6 cases:
  - storeAndRecall
  - storeSetsCorrectDim (LONGBLOB 长度验证)
  - importanceAndTags
  - recallUpdatesAccessCount (召回后自增)
  - deleteBelongsToUser (权限隔离)
  - embeddingDeterministic

---

## 🏗️ 架构改进

### Day 6 → Day 7 变化
```
Day 6: 短期记忆 (Redis/Caffeine) + 摘要 (占位: 截前60字)
  └─ ShortTermMemory → SessionContextCache

Day 7: 短期 + 长期 + 偏好 + 真实 LLM 摘要
  ├─ ShortTermMemory → 短上下文 (本会话)
  ├─ LongTermMemoryService → 向量召回 (跨会话)
  ├─ UserPrefService → 用户偏好 (KV)
  └─ LlmSummarizer → 调 model 服务生成摘要
       ↓
  CrossSessionContextBuilder.build() 拼装最终 messages
       ↓
  Model Provider (openai/minimax/...)
```

### 关键设计
1. **不引外部向量库** — MySQL BLOB 存向量 (1000 条内完全 OK，将来切真向量库换 EmbeddingClient 即可)
2. **Provider 抽象** — 任何 OpenAI 协议 embedding 服务都即插即用
3. **降级链路完整** — Redis 不可用 / Embedding API 失败 / LLM 摘要失败 都有 fallback
4. **Token 预算严格管理** — crossContext 不超 maxContext * 0.8
5. **access_count 反馈** — 被召回的记忆会自增 + 更新 lastAccessAt (为将来的"按热度衰减"留口)

---

## 🔍 验证

### 单元/集成测试 (43 用例全过)
```
ContextBuilderTest ......... 4/4
MockEmbeddingClientTest .... 5/5
ShortTermMemoryTest ....... 4/4
VectorUtilsTest ........... 7/7
MemoryIntegrationTest ..... 6/6  ← Day 7 新增
JwtTokenProviderTest ...... 4/4
MessageRoleTest ........... 3/3
MockAdapterTest ........... 3/3
ModelProviderFactoryTest .. 4/4
StreamingTest ............. 3/3
                         -----
                          43/43 ✅
```

### Maven 编译 (7 模块)
```
minimax-platform ....... SUCCESS
minimax-common ......... SUCCESS
minimax-gateway ........ SUCCESS
minimax-auth ........... SUCCESS
minimax-chat ........... SUCCESS
minimax-memory ......... SUCCESS
minimax-model .......... SUCCESS
minimax-rag ............ SUCCESS
=====================
BUILD SUCCESS
```

### Java 启动验证
```
minimax-memory.jar 启动 7.6s ✅
Tomcat listening on :8084 ✅
JWT 401 响应正常 ✅
```

---

## 🌐 GitHub

- 仓库: https://github.com/liugl951127/miniLiugl.git
- 状态: pending
- 改动: +16 java + 1 SQL + 1 schema + 1 test + 1 yml + 1 mapper xml

---

## 📁 新增/修改文件

### 新增
```
sql/07_memory_long.sql                                                 (MySQL 2 张表)
sql/init/07_memory_long.sql                                            (init dir 副本)
backend/minimax-memory/src/main/java/com/minimax/memory/embedding/EmbeddingClient.java
backend/minimax-memory/src/main/java/com/minimax/memory/embedding/OpenAiEmbeddingClient.java
backend/minimax-memory/src/main/java/com/minimax/memory/embedding/MockEmbeddingClient.java
backend/minimax-memory/src/main/java/com/minimax/memory/longterm/LongTermMemory.java
backend/minimax-memory/src/main/java/com/minimax/memory/longterm/LongTermMemoryMapper.java
backend/minimax-memory/src/main/java/com/minimax/memory/longterm/LongTermMemoryService.java
backend/minimax-memory/src/main/java/com/minimax/memory/longterm/VectorUtils.java
backend/minimax-memory/src/main/java/com/minimax/memory/pref/UserPref.java
backend/minimax-memory/src/main/java/com/minimax/memory/pref/UserPrefMapper.java
backend/minimax-memory/src/main/java/com/minimax/memory/pref/UserPrefService.java
backend/minimax-memory/src/main/java/com/minimax/memory/context/CrossSessionContextBuilder.java
backend/minimax-memory/src/main/java/com/minimax/memory/summary/LlmSummarizer.java
backend/minimax-memory/src/main/java/com/minimax/memory/config/MybatisPlusConfig.java
backend/minimax-memory/src/main/java/com/minimax/memory/MemoryApplication.java
backend/minimax-memory/src/main/java/com/minimax/memory/MemoryController.java
backend/minimax-memory/src/main/resources/mapper/LongTermMemoryMapper.xml
backend/minimax-memory/src/main/resources/schema-h2.sql
backend/minimax-memory/src/main/resources/application-test.yml
backend/minimax-memory/src/test/java/com/minimax/memory/VectorUtilsTest.java
backend/minimax-memory/src/test/java/com/minimax/memory/MockEmbeddingClientTest.java
backend/minimax-memory/src/test/java/com/minimax/memory/MemoryIntegrationTest.java
reports/day-7-report.md                                                (本文件)
```

### 修改
```
backend/minimax-memory/pom.xml                                         (+ mybatis-plus 依赖)
backend/minimax-memory/src/main/java/com/minimax/memory/summary/Summarizer.java  (LlmSummarizer 注入)
```

---

## 🚀 下一步 (Day 8: RAG)

- 文档上传 + 解析 (PDF/DOCX/MD)
- 分块 (chunk) + embedding 入向量库
- 检索增强生成 (retrieval-augmented chat)
- 引用来源标注
- 多文档 / 多用户隔离
