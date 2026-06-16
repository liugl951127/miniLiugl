# MiniMax 平台 - 14 天推进记录

> 每天 20:00 自动构建一次。每天结束时这里会追加当日产出 + 明日计划。

## Day 1 - 2026-06-15 ✅ 项目骨架

**今日完成：**
- [x] 顶层目录结构（backend / frontend / sql / scripts / deploy）
- [x] Spring Boot 3 多模块 Maven 工程（7 个子模块）
- [x] 统一响应包装 `Result<T>` + 业务异常体系 + 全局异常处理
- [x] 前端 Vue 3 + Vite + Element Plus + Pinia 骨架
- [x] 前端路由（登录/对话/知识库/记忆/管理后台/关于）
- [x] 布局：左侧菜单 + 顶部导航 + 用户下拉
- [x] `docker-compose.yml`：MySQL 8 + Redis 7 + ES 8 + MinIO + 监控
- [x] 网关健康检查 + 平台介绍 API
- [x] 每日构建脚本 `scripts/daily-build.sh`（自检 + 打包）
- [x] 定时任务安装脚本 `scripts/setup-cron.sh`

**关键文件数：** ~40 个源文件 + 1 个 docker-compose + 完整文档
**压缩包大小：** ~60KB（不含 node_modules/target）

**明日计划 Day 2：**
- [ ] User 实体 + MyBatis-Plus 持久层
- [ ] JWT 工具类（生成/解析/刷新）
- [ ] Spring Security 6 配置（无状态 + 自定义过滤器）
- [ ] AuthController：`/auth/register` `/auth/login` `/auth/me` `/auth/refresh` `/auth/logout`
- [ ] 前端：真实登录页 + Token 持久化 + 路由守卫完善
- [ ] 接入 MySQL 真实建表

## Day 2 - 2026-06-16 ✅ 用户体系 + JWT 鉴权

**今日完成：**
- [x] SQL 建表脚本（sys_user/role/refresh_token/login_log）
- [x] User/Role/UserRole/RefreshToken/LoginLog 5 个实体
- [x] MyBatis-Plus 5 个 Mapper + 2 个 XML
- [x] JWT 工具（access 30min + refresh 7d 双 token + SHA-256 哈希刷新）
- [x] Spring Security 6 + JwtAuthenticationFilter + 双 JSON 入口点
- [x] AuthService（注册/登录/刷新/登出/me）
- [x] AuthController 5 个 REST 接口
- [x] 启动类 AuthApplication（独立可跑）
- [x] 前端真实登录页（登录/注册切换）
- [x] Pinia user store 双 token 持久化
- [x] Axios http.js 401 自动 refresh + 重放
- [x] 路由守卫完整版
- [x] Vite proxy /api/v1/auth → 8081
- [x] 单元测试 JwtTokenProviderTest（4 用例）
- [x] 自检脚本 daily-build.sh + java-static-check.sh
- [x] 修复 Day 1 破窗：Element Plus 图标改名 (Memory→Cpu) / Result.toJsonString / UA 长度限制

**关键文件数：** 39 Java + 9 Vue + 9 JS + 5 SQL = 62 个源文件
**代码量：** Java 1458 行 + XML 606 行 + Vue 609 行 + JS 291 行 + SQL 132 行 = 3096 行
**前端构建：** 19.74s ✅
**Java 静态体检：** ✅（含 5 通配符 import / 1 嵌套类 / 0 TODO）

**明日计划 Day 3：**
- [ ] Session 实体 + CRUD
- [ ] Message 实体 + 增删改查
- [ ] 会话侧边栏 UI
- [ ] 多会话切换
- [ ] 历史消息分页加载

## Day 3 - 2026-06-16 ✅ 会话模块 (CRUD + 侧边栏) + 实跳验训

**今日完成：**
- [x] SQL 脚本：chat_session + chat_message（132 行 + 索引）
- [x] 实体：ChatSession、ChatMessage + MessageRole 枚举
- [x] Mapper：2 个 + 2 个 XML
- [x] Service：ChatSessionService、ChatMessageService（鉴权 + 软删 + 分页 + 自动计数）
- [x] Controller：SessionController（5 端点）+ MessageController（嵌套）
- [x] ChatApplication 启动类
- [x] 前端 API/Store/视图（侧边栏 + 会话管理 + 消息流）
- [x] 架构重构：JwtAuthenticationFilter 从 auth 移到 common
- [x] H2 测试 profile：本地一键启动验证
- [x] AdminDataInitializer：启动时 BCrypt 编码 admin 密码
- [x] Maven 编译：4 个模块全部 BUILD SUCCESS（25.6s）
- [x] 单元测试：7 用例全过
- [x] **java -jar 真实启动 auth + chat 跨服务运行** （两个端口 8081/8082 都起起来了）
- [x] **跨服务 JWT 鉴权全链路验证**：登录→拿 token→ chat 创建会话→添加消息→自动 messageCount +1
- [x] 前端构建：20.48s 通过
- [x] 父 pom 增强 aliyun 镜像（spring / spring-plugin / google）
- [x] ~/.m2/settings.xml 配置 mirror 走 aliyun

**关键文件数：** 60 Java + 8 Vue + 8 JS + 4 SQL = 80 个源文件
**代码量：** Java 2251 行 + XML 928 行 + SQL 264 行 = 3443 行
**静态体检：** 60 Java / 7 测试 / 0 TODO

**明日计划 Day 4：**
- [ ] Model 路由层（OpenAI 兼容）
- [ ] 多 provider 支持
- [ ] 限流 + 配额
- [ ] 真实模型调用

## Day 4 - 2026-06-16 ✅ 模型路由层 (OpenAI 兼容 + 限流 + 配额) + 跨服务验证

**今日完成：**
- [x] SQL：model_provider + model_config + model_quota (3 表 + 3 provider + 6 模型初始数据)
- [x] 实体：ModelProvider、ModelConfig、ModelQuota
- [x] Mapper：3 个 + 2 个 XML
- [x] Provider 适配器：ModelProviderAdapter 接口 + OpenAiCompatibleAdapter + MockAdapter + Factory
- [x] 限流：Bucket4j 60/min 突发 10
- [x] 配额：QuotaService 原子 upsert
- [x] Service + Controller（4 端点：list/providers/chat/stream）
- [x] H2 test profile + schema
- [x] 单元测试：10 用例全过
- [x] Maven 编译：5 模块全过
- [x] 3 服务跨跳 E2E 验证（auth + chat + model 同步运行）
- [x] 限流触发：1006 RATE_LIMIT
- [x] SSE 流式：真起推送字符
- [x] 前端构建：19.26s

**代码量：** Java 3293 行 + XML 1278 行 + SQL 396 行

**明日计划 Day 5：**
- [ ] 流式对话 SSE 真实接入
- [ ] 实时打字机效果
- [ ] 取消按钮 + 中断流式

## Day 5 - 2026-06-16 ✅ 流式对话 SSE (核心) + 取消机制 + 打字机

**今日完成：**
- [x] OpenAI 真实流式调用 (HttpClient + BodyHandlers.ofLines)
- [x] Mock 流式 30ms/字符
- [x] 流式控制器（按 provider 选 adapter + SSE 推 chunk）
- [x] 取消机制（后端 stopFlag + streamId 跟踪）
- [x] /models/chat/cancel 端点
- [x] 前端 fetch + ReadableStream 实时处理
- [x] 打字机效果（光标 blink 动画）
- [x] 取消按钮（流式时 "发送" 变 "停止"）
- [x] Token 计数 + finishReason UI
- [x] 单元测试 13 用例全过
- [x] Maven 编译：全过
- [x] 3 服务跳 E2E：流式 + 取消 都验证过

**关键代码量：** Java 3593 行 + XML 1336 行

**明日计划 Day 6：**
- [ ] 短期记忆 (Redis) - 多轮上下文管理
- [ ] 上下文窗口截断
- [ ] 系统提示词模板

## Day 6 - 2026-06-16 ✅ 短期记忆 (Redis) + 摘要压缩 + 上下文管理

**今日完成：**
- [x] memory 模块上线
- [x] ShortTermMemory (Redis LIST + Caffeine 兑底)
- [x] ContextBuilder (按 maxContext 智能裁剪)
- [x] Summarizer (摘要压缩、30 条触发)
- [x] MemoryController 6 端点
- [x] chat 模块加 SessionContextCache (本地独立)
- [x] chat appendMessage 自动同步短期记忆
- [x] chat recentContext 接口
- [x] 单元测试 21 用例全过 (memory 8 新)
- [x] Maven 编译 6 模块全过 (41.8s)
- [x] 4 服务跳 E2E (auth+chat+model+memory)
- [x] 摘要验证：35→10 条成功
- [x] 上下文构建验证
- [x] 集成验证：chat history → model 调成功

**代码量：** Java 4329 行 + XML 1633 行

**明日计划 Day 7：**
- [ ] 长期记忆 (向量库)
- [ ] 真实 LLM 摘要
- [ ] 跨会话记忆召回

## Day 7 - 2026-06-16 ✅ 长期记忆 (向量库) + 跨会话召回 + 偏好 + 真实 LLM 摘要

**今日完成：**
- [x] MySQL 2 张表：`memory_long_term`（向量 BLOB）+ `memory_user_pref`（偏好 KV）
- [x] Embedding 抽象层 (`EmbeddingClient`): OpenAI 兼容 + Mock (离线)
- [x] `LongTermMemoryService`: store / recall (余弦) / recent / delete
- [x] `UserPrefService`: 用户偏好 KV (set/get/list/delete)
- [x] `CrossSessionContextBuilder`: 跨会话 context (短+长+偏好+摘要)
- [x] `LlmSummarizer`: 调 model 服务做真实摘要（替换 Day 6 占位）
- [x] `VectorUtils`: float[]↔byte[] + cosine 数学
- [x] MemoryController 扩展到 16 端点
- [x] `MemoryIntegrationTest` (6 cases) 覆盖端到端
- [x] Maven 7 模块编译 + 43 用例测试 + Java 启动验证

**关键数据：** +16 java (109 总) / +1128 行 (5457) / 43 测试 / 2 张 MySQL 表
**报告：** `reports/day-7-report.md`

**明日计划 Day 8：**
- [ ] 文档上传 + 解析 (PDF/DOCX/MD)
- [ ] 分块 (chunk) + embedding 入向量库
- [ ] 检索增强生成 (retrieval-augmented chat)
- [ ] 引用来源标注

## Day 8 - 2026-06-16 ✅ RAG (知识库 + 文档上传 + 检索 + 问答 + 引用)

**今日完成：**
- [x] MySQL 3 张表：`knowledge_base` + `document` + `document_chunk`
- [x] 3 种文档解析器：TXT/MD (BOM探测) / DOCX (POI) / PDF (PDFBox)
- [x] 智能分块器 TextChunker (滑动窗口 500/50 + 位置跟踪)
- [x] DocumentService upload：SHA-256 去重 + 解析 + 分块 + 向量化 + 入库 + 状态机
- [x] Retriever：向量检索 topK + touchAccess + 引用填充
- [x] RagService：检索 → 拼 context → 调 LLM → 答案+来源 (3 级降级)
- [x] RagController 11 端点 (KB CRUD + Doc upload + retrieve + ask)
- [x] 集成测试 19 用例 (8 端到端 + 6 chunker + 5 vector)
- [x] Maven 7 模块编译 + 62 总测试全过

**关键数据：** +26 java (135 总) / +1538 行 (6995) / 19 测试 / 3 张 MySQL 表 / 11 端点
**报告：** `reports/day-8-report.md`

**明日计划 Day 9：**
- [ ] 工具注册表 (function registry)
- [ ] LLM 工具调用协议 (OpenAI functions)
- [ ] 内置工具: 时间/计算器/HTTP抓取
- [ ] 自定义工具 API
- [ ] 工具调用 + 聊天循环

## Day 9 - 待开始
