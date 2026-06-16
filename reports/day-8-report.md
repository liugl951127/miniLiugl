# Day 8 报告 - RAG (检索增强生成)

**日期**: 2026-06-16
**目标**: 完整落地 RAG 模块 — 文档上传 / 智能分块 / 向量化 / 检索 / 问答 + 引用
**Commit**: pending

---

## ✅ 完成项

### 1. 数据模型 (3 张表)
- ✅ `knowledge_base` — 知识库：owner/tenant/visibility/doc_count/chunk_count/tags
- ✅ `document` — 文档：title/source_type/source_uri/content/status/chunk_count/checksum
- ✅ `document_chunk` — 切片：chunk_index/embedding/position/access_count
- ✅ H2 兼容 schema (schema-h2.sql)

### 2. 文档解析 (3 种格式 + 智能探测)
- ✅ `PlainTextParser` — TXT/MD (UTF-8/UTF-16 + BOM 探测)
- ✅ `DocxParser` — Apache POI 5.2.5
- ✅ `PdfParser` — Apache PDFBox 3.0.2
- ✅ `ParserRegistry` — 按 sourceType 路由

### 3. 智能分块
- ✅ `TextChunker` — 滑动窗口
  - chunkSize = 500 字符 (可配)
  - overlap = 50 字符 (相邻块重叠)
  - 段落优先 → 超长段强制切 → 累计合并
  - **位置跟踪** (startPos / endPos) — 引用可定位原文

### 4. 向量化
- ✅ `EmbeddingClient` 接口 (RAG 自有, 与 memory 解耦)
- ✅ `MockEmbeddingClient` (offline, 确定性, 64 维) — 默认
- ✅ `OpenAiEmbeddingClient` (生产, 启用条件: `minimax.rag.embedding.provider=openai`)
- ✅ 复用 Day 7 思路: MySQL LONGBLOB 存向量 + 余弦相似度

### 5. 业务 Service
- ✅ `KnowledgeBaseService` — 建/查/列/删 + 可见性控制 (private/public)
- ✅ `DocumentService.upload()` — 全流程:
  1. SHA-256 去重 (同 owner + kb + checksum = 已存在)
  2. 解析 → 纯文本
  3. 分块 → List<Chunk>
  4. embed → 入库
  5. 状态机: pending → parsing → chunked / failed
  6. KB 计数 (docCount +1, chunkCount +N)
- ✅ `DocumentService.delete()` — 删 doc + 删 chunks + KB 计数回退
- ✅ **去重**: 同内容文件不会被重复处理

### 6. 检索器
- ✅ `Retriever.retrieve(kbId, query, topK)`:
  1. query → embedding
  2. 拉 KB 内全部 chunks (含向量)
  3. cosine 相似度
  4. 过滤 + 排序 → topK
  5. touchAccess (access_count++) + 回填 docTitle/docSource

### 7. RAG 增强问答 (核心价值)
- ✅ `RagService.ask(kbId, question, history, topK)`:
  1. 检索 → topK chunks
  2. 拼 system prompt: "你是基于知识库回答的助手...引用处标注 [来源 N]..."
  3. 调 model 服务 (OpenAI 兼容)
  4. 返回: `RagAnswer(answer, sources[])`
- ✅ 降级链路:
  - 检索为空 → 走普通 chat
  - LLM 失败 → 返回检索内容 + 提示
  - 全失败 → 友好提示

### 8. 暴露的 HTTP 端点 (11 个)
| 方法 | 路径 | 功能 |
|---|---|---|
| POST   | `/rag/kb` | 建知识库 |
| GET    | `/rag/kb` | 列我的 KB |
| GET    | `/rag/kb/public` | 列公开 KB |
| GET    | `/rag/kb/{id}` | 详情 |
| DELETE | `/rag/kb/{id}` | 删除 |
| POST   | `/rag/doc/upload` | **上传文档 (multipart)** |
| GET    | `/rag/doc` | 列 KB 内文档 |
| GET    | `/rag/doc/{id}/chunks` | 文档切片 |
| DELETE | `/rag/doc/{id}` | 删除 |
| POST   | `/rag/retrieve` | 纯检索 |
| POST   | `/rag/ask` | **RAG 问答 + 引用** |

---

## 📊 关键数据

| 指标 | Day 7 | Day 8 | 增量 |
|------|-------|-------|------|
| Java 文件 | 109 | **135** | +26 |
| Java 行数 | 5457 | **6995** | +1538 |
| SQL 行数 | 274 | **411** | +137 |
| 单元/集成测试 | 43 | **62** | +19 |
| 端点 (rag 模块) | 0 | **11** | +11 |
| 数据表 (rag) | 0 | **3** | +3 |
| 文档解析器 | 0 | **3 种格式** | +3 |

### Day 8 新增测试 (19 用例)
- `TextChunkerTest` (6 cases) — 空/单段/多段/超长/重叠/位置
- `VectorUtilsTest` (5 cases) — roundTrip + cosine
- `RagIntegrationTest` (8 cases) — 端到端:
  - createAndListKb
  - publicKbVisibleToAll
  - privateKbHiddenFromOthers
  - uploadAndChunkTextDoc
  - dedupSameContent
  - kbCounters
  - retrieveFindsRelevantDoc
  - deleteDocDecrementsCounters

---

## 🏗️ 架构改进

### Day 7 → Day 8 变化
```
Day 7: 短期 + 长期记忆 (向量化)
  └─ 用于"对话上下文"

Day 8: RAG (知识库)
  ├─ 知识库容器 (KB)
  ├─ 文档 (上传 → 解析 → 分块 → 向量化)
  └─ 检索 (query → 向量 → topK → 引用)
       ↓
  RAG 问答: 检索结果 + 用户问题 → LLM → 答案 + 来源标注
```

### 关键设计
1. **不引外部向量库** — 复用 Day 7 思路 (MySQL BLOB)，1000 文档内完全 OK
2. **解析器可插拔** — ParserRegistry + 接口，新增格式 (e.g. HTML/EPUB) 只需一个 Component
3. **SHA-256 去重** — 同内容不重复处理，节省 embedding API 成本
4. **chunkSize/overlap 可调** — 不同业务场景 (技术文档/对话) 调不同参数
5. **位置跟踪** — startPos/endPos 可在 UI 高亮原文出处
6. **降级完整** — LLM 不可用也能用 (返回检索内容)
7. **多 KB 隔离** — retrieval 严格限定在指定 kbId 内

### 上下文拼接示例 (RAG 实际发送给 LLM 的 messages)
```json
[
  {"role": "system", "content": "你是基于知识库回答问题的助手...引用处标注 [来源 N]...\n\n参考资料:\n[来源 1] Spring Boot 实战 - 片段 #2 (相似度 0.85)\nSpring Boot 是基于 Spring 框架的..."},
  {"role": "user", "content": "Spring Boot 是什么？"}
]
```

---

## 🔍 验证

### 单元/集成测试 (62 用例全过)
```
RagIntegrationTest ....... 8/8  ← Day 8 新增
TextChunkerTest .......... 6/6  ← Day 8 新增
VectorUtilsTest .......... 5/5  ← Day 8 新增
ContextBuilderTest ....... 4/4
MockEmbeddingClientTest .. 5/5
ShortTermMemoryTest ...... 4/4
VectorUtilsTest (memory) . 7/7
JwtTokenProviderTest ..... 4/4
MessageRoleTest .......... 3/3
MockAdapterTest .......... 3/3
ModelProviderFactoryTest . 4/4
StreamingTest ............ 3/3
                         -----
                          62/62 ✅
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
minimax-rag ............ SUCCESS  ← Day 8 升级
=====================
BUILD SUCCESS · 38.7s
```

---

## 🌐 GitHub

- 仓库: https://github.com/liugl951127/miniLiugl.git
- 状态: pending
- 改动: +26 java + 1 SQL + 1 schema + 3 测试 + 1 yml + 3 mapper xml

---

## 📁 新增/修改文件

### 新增
```
sql/08_rag.sql                                                              (MySQL 3 张表)
sql/init/08_rag.sql                                                         (init dir 副本)
backend/minimax-rag/pom.xml                                                 (升级 + POI/PDFBox/H2)
backend/minimax-rag/src/main/java/com/minimax/rag/RagApplication.java
backend/minimax-rag/src/main/java/com/minimax/rag/entity/KnowledgeBase.java
backend/minimax-rag/src/main/java/com/minimax/rag/entity/Document.java
backend/minimax-rag/src/main/java/com/minimax/rag/entity/DocumentChunk.java
backend/minimax-rag/src/main/java/com/minimax/rag/mapper/KnowledgeBaseMapper.java
backend/minimax-rag/src/main/java/com/minimax/rag/mapper/DocumentMapper.java
backend/minimax-rag/src/main/java/com/minimax/rag/mapper/DocumentChunkMapper.java
backend/minimax-rag/src/main/java/com/minimax/rag/embedding/EmbeddingClient.java
backend/minimax-rag/src/main/java/com/minimax/rag/embedding/MockEmbeddingClient.java
backend/minimax-rag/src/main/java/com/minimax/rag/embedding/OpenAiEmbeddingClient.java
backend/minimax-rag/src/main/java/com/minimax/rag/parser/DocumentParser.java
backend/minimax-rag/src/main/java/com/minimax/rag/parser/PlainTextParser.java
backend/minimax-rag/src/main/java/com/minimax/rag/parser/DocxParser.java
backend/minimax-rag/src/main/java/com/minimax/rag/parser/PdfParser.java
backend/minimax-rag/src/main/java/com/minimax/rag/parser/ParserRegistry.java
backend/minimax-rag/src/main/java/com/minimax/rag/chunker/TextChunker.java
backend/minimax-rag/src/main/java/com/minimax/rag/retriever/Retriever.java
backend/minimax-rag/src/main/java/com/minimax/rag/service/VectorUtils.java
backend/minimax-rag/src/main/java/com/minimax/rag/service/KnowledgeBaseService.java
backend/minimax-rag/src/main/java/com/minimax/rag/service/DocumentService.java
backend/minimax-rag/src/main/java/com/minimax/rag/service/RagService.java
backend/minimax-rag/src/main/java/com/minimax/rag/controller/RagController.java
backend/minimax-rag/src/main/java/com/minimax/rag/config/MybatisPlusConfig.java
backend/minimax-rag/src/main/resources/mapper/KnowledgeBaseMapper.xml
backend/minimax-rag/src/main/resources/mapper/DocumentMapper.xml
backend/minimax-rag/src/main/resources/mapper/DocumentChunkMapper.xml
backend/minimax-rag/src/main/resources/schema-h2.sql
backend/minimax-rag/src/main/resources/application-test.yml
backend/minimax-rag/src/test/java/com/minimax/rag/TextChunkerTest.java
backend/minimax-rag/src/test/java/com/minimax/rag/VectorUtilsTest.java
backend/minimax-rag/src/test/java/com/minimax/rag/RagIntegrationTest.java
reports/day-8-report.md                                                     (本文件)
```

---

## 🚀 下一步 (Day 9: Function Calling)

- 工具注册表 (function registry)
- LLM 工具调用协议 (OpenAI functions / MiniMax tool_use)
- 内置工具: 时间查询、计算器、HTTP 抓取
- 自定义工具 API (用户注册自己的 function)
- 工具调用 + 聊天循环 (function call → 结果回传 → 二次 LLM 调用)
- 调用审计 (谁/何时/哪个工具/什么参数)
