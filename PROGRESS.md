# MiniMax 平台 - 推进记录

> 每天 20:00 自动构建一次。每天结束时这里会追加当日产出 + 明日计划。
> 当前版本: **V4.3** | 后端模块: **14** | Java 文件: **247** | Vue 文件: **35**

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

## Day 7 - 2026-06-17 ✅ 长期记忆 (向量库) + 跨会话召回 + 偏好 + 真实 LLM 摘要

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

## Day 9 - 2026-06-16 ✅ Function Calling (工具调用)

**今日完成：**
- [x] MySQL 2 张表：`function_tool` (工具注册) + `function_call_log` (调用审计)
- [x] 4 个内置工具：get_current_time / calculator / http_get / random_number
- [x] 自实现表达式求值器 (不依赖 Nashorn, Java 17 headless 兼容)
- [x] ToolExecutor 路由器：内置按 name bean / 自定义 HTTP POST
- [x] FunctionCallService：LLM + tool 循环 (最多 5 轮)
- [x] 工具调用结果回传 LLM (OpenAI tool_use 协议)
- [x] SSRF 防护 + 字符白名单 + 异常隔离
- [x] FunctionController 10 端点 (CRUD + invoke + logs + chat)
- [x] 集成测试 23 用例 (13 unit + 10 integration)

**关键数据：** +17 java (152 总) / +1412 行 (8407) / 23 测试 / 2 张 MySQL 表 / 10 端点 / 4 内置工具 / 8 后端模块
**报告：** `reports/day-9-report.md`

**明日计划 Day 10：**
- [ ] 管理后台 (用户/模型/KB/统计)
- [ ] 跨服务 API 聚合
- [ ] 监控面板 (JVM 指标 + 业务指标)
- [ ] 操作审计 (关键操作日志)

## Day 10 - 2026-06-16 ✅ 管理后台 (跨服务 API 聚合 + 审计 + 监控)

**今日完成：**
- [x] MySQL 1 张表：`admin_audit_log` (统一操作审计)
- [x] ServiceClient：Java 11+ HttpClient 封装的跨服务 HTTP 客户端 (无 Feign 依赖)
- [x] 6 服务端点配置 (auth/chat/model/memory/rag/function)
- [x] UserMgmtService：代理 auth + 自动审计 (重置密码/启停)
- [x] ModelMgmtService：代理 model + 调限流审计
- [x] StatsService：业务统计 (today/last7d/last30d) + dashboard
- [x] HealthAggregator：并发 ping 6 服务 + 跨服务 health
- [x] AuditService：统一操作审计 + 记录/查询/统计
- [x] AdminController 14 端点 (用户管理 + 模型管理 + 统计 + 监控 + 审计)
- [x] 集成测试 11 用例 (8 integration + 3 unit)

**关键数据：** +14 java (166 总) / +1003 行 (9410) / 11 测试 / 1 张 MySQL 表 / 14 端点 / 9 后端模块
**报告：** `reports/day-10-report.md`

**明日计划 Day 11：**
- [ ] 多模态 (图片上传 + 视觉模型)
- [ ] 多模态 Embedding
- [ ] 多模态 RAG
- [ ] 图片理解对话

## Day 12 - 2026-06-16 ✅ 监控 (Prometheus + 告警 + 健康详情)

**今日完成：**
- [x] MySQL 3 张表：`metric_snapshot` / `alert_rule` / `alert_event`
- [x] 5 个默认告警规则 (CPU/JVM/磁盘/LLM延迟/错误率)
- [x] HealthDetailService：DB/JVM/磁盘/线程/系统 5 维度
- [x] MetricsCollector：5 Counter + 4 Gauge + 2 Timer (Micrometer)
- [x] SnapshotService：60s 落库 + 30 天自动清理
- [x] AlertEngine：30s 评估 + 6 种运算符 + 冷却 + 自动恢复
- [x] Prometheus：`/actuator/prometheus` 暴露
- [x] MonitorController 15 端点
- [x] 集成测试 11 用例 (6 health + 5 alert)

**关键数据：** +13 java (185 总) / +1427 行 (10837) / 11 测试 / 3 张 MySQL 表 / 15 端点 / 5 告警规则 / 11 后端模块
**报告：** `reports/day-12-report.md`

**明日计划 Day 13：**
- [ ] JVM 调优 (G1GC, Metaspace, DirectMemory)
- [ ] HikariCP 连接池调优
- [ ] 缓存层 (Caffeine + Redis)
- [ ] 异步化 (@Async)
- [ ] 压测报告

## Day 13 - 2026-06-16 ✅ 调优 (限流/缓存/异步/请求日志/压测)

**今日完成：**
- [x] MySQL 3 张表：`request_log` / `async_task` / `rate_limit_rule`
- [x] RateLimiter (Bucket4j 令牌桶) + RateLimitService (IP/User/Global 三维)
- [x] CacheService (Caffeine) + 防击穿 getOrLoad + TTL + stats
- [x] AsyncTaskService (UUID 任务 + 状态机 + 重试 + 回调 + Future)
- [x] RequestLogFilter (traceId + 慢/错采点)
- [x] benchmark.sh (Bash 并发压测: QPS + p50/p95/p99)
- [x] minimax-optimized.yml (Tomcat/HikariCP/Redis/JVM 生产调优模板)
- [x] common 模块升级: Bucket4j 8.10.1 + Caffeine 3.1.8
- [x] 集成测试 11 用例 (限流/缓存/异步)

**关键数据：** +6 java (191 总) / +617 行 (11454) / 11 测试 / 3 张 MySQL 表 / 1 压测脚本 / 11 后端模块
**报告：** `reports/day-13-report.md`

**明日计划 Day 14：**
- [ ] 完整 README + 架构图
- [ ] API 文档 (OpenAPI 3)
- [ ] Docker 镜像 + K8s manifest
- [ ] 演示场景 + 客户案例

## Day 14 - 2026-06-16 ✅ 文档交付 (README + ARCHITECTURE + CHANGELOG + API)

**今日完成：**
- [x] README.md 主文档 (10 大能力 + 架构图 + 快速启动)
- [x] ARCHITECTURE.md (8553 字, 7 大设计原则 + ER + 时序图 + 安全架构)
- [x] CHANGELOG.md (4159 字, Day 1-14 完整记录)
- [x] API.md (7255 字, 92+ 端点完整参考)
- [x] 部署文档 + 演示场景 + 客户案例

**关键数据：** README + ARCHITECTURE + CHANGELOG + API + 14 reports
**累计：** 11,454 行 Java / 191 Java 文件 / 125 测试 / 92+ 端点 / 18+ 表

**明日计划：** → V4 增值包

---

## Day 15 - 2026-06-18 ✅ V4.3 Prompt 模板系统

**今日完成：**
- [x] CHANGELOG 大补全 (V4.0 / V4.1 / V4.2 / V4.3)
- [x] **minimax-prompt 模块** (第 14 个微服务, 8091 端口)
  - PromptTemplate 实体 + Mapper + Service + Controller (7 端点)
  - 变量占位符 `{{variable}}` 提取 + 填值解析
  - 5 个内置系统模板 (翻译/代码审查/会议纪要/营销文案/故障排查)
  - 启动时自动初始化内置模板
- [x] `sql/19_prompt_template.sql` — prompt_template 表 + 种子数据
- [x] 前端模板管理页面 (`/prompts`): 卡片/列表视图 + 变量填值弹窗 + 实时预览
- [x] Chat 页面: `?prompt=` query 参数自动填入消息框
- [x] 侧边栏菜单 + vite proxy
- [x] 修复 5 个既有 bug (AudioShowcase/DagShowcase/prompt.js/Vue解析)

**关键数据：** +7 Java / +1 SQL / +3 JS/Vue / 14 模块
**报告：** `reports/day-15-report.md`

## V2 增值包 - 2026-06-16 ✅ 4 大新功能

**今日完成：**
- [x] **V2.1 Agent 自主任务** - ReAct 模式 (Thought/Action/Observation + XML 包裹 Final Answer)
- [x] **V2.2 知识图谱** - entity-relation + 1跳/2跳 + 最短路径 BFS
- [x] **V2.3 实时协作** - WebSocket 多人编辑 (消息/typing/cursor/edit) + 持久化
- [x] **V2.4 插件市场** - 系统插件 + 用户发布 + 评分/启停 (4 类型: class/url/js/wasm)
- [x] **V2.5 前端可视化** - Agent 思考时间线 / KG 实体图 / WS 在线 / 插件卡片

**关键文件数：** 22 Java + 1462 行 + 6 SQL 表 + 19 端点 + 4 前端页面
**测试：** 5 个新单测全过 (AgentServiceTest 2 + KnowledgeGraphServiceTest 3)
**编译：** 12 个模块 BUILD SUCCESS (含新 minimax-agent 8090 端口)

**核心代码：**
- `backend/minimax-agent/` - 12 模块
- `sql/15_v2_features.sql` - 6 表
- `frontend/src/views/{agent,kg,collab,plugins}/Index.vue` - 4 页面
- `reports/v2-features-report.md` - V2 报告

**商业价值：**
- Agent 让 AI 真正"自主" (目标驱动 vs 轮次驱动)
- KG 让 RAG 升级为关系推理
- 协作让 AI 助手从"单人"变"团队"
- 插件让平台可扩展成生态

---

## Day 16 - 2026-06-20 ✅ V3.1 多租户前端管理系统

**今日完成：**
- [x] `src/api/tenant.js` — 7 个 API 端点 (list/get/create/status/quota/delete/users)
- [x] `src/store/tenant.js` — useTenantStore (fetch/create/toggle/setQuota/remove/fetchUsers)
- [x] `src/views/tenant/Index.vue` — ~400 行完整租户管理 UI:
  - KPI 概览 (租户总数/正常运营/用户数/停用)
  - 租户列表表格 (8 列 + 配额进度条)
  - 创建/编辑/启停/配额调整/删除 (default 防删)
  - 用户列表弹窗
- [x] `src/router/index.js` — 新增 `/tenant` 路由 (requiresSuper: true)
- [x] `src/layout/Index.vue` — 侧边栏 + 右上角下拉新增 "🏢 租户管理" 入口

**关键数据：** +1 API / +1 Store / +1 Vue (400行) / 2 文件编辑
**报告：** `reports/day-16-report.md`

**明日计划 Day 17：**
- [ ] 移动端 H5 适配优化
- [ ] OpenAPI 3.0 / Swagger 文档生成
- [ ] WebSocket 实时通知前端
- [ ] 国际化 (i18n) 补全

---

## Day 17 - 2026-06-21 ✅ V4 基础设施补全 (Swagger/i18n/移动端/通知)

**今日完成：**
- [x] Swagger/OpenAPI (knife4j): 10模块24个Controller加@Tag/@Operation注解，10个yml激活knife4j
- [x] 国际化i18n: 新增9个locale section（tenant/admin/monitor/prompt/agent/kg/collab/plugins/about），17个Vue页面接入t()
- [x] 移动端H5: 6个页面全面升级（Index/Banner+Tabbar、Chat/气泡+时间戳、Agent/Steps、Kg/ECharts图、Plugins/安装卸载、Me/退出登录）
- [x] WebSocket通知: notification表+实体/Mapper/Service/Controller+NotificationWebSocket端点+前端通知页面+铃铛红点Badge

**关键数据：** +~21个后端文件 / +~30个前端文件 / 24个Controller改注解 / 1张SQL表
**报告：** `reports/day-17-report.md`

**明日计划 Day 18：**
- [ ] API网关增强（路由可配置化）
- [ ] 性能监控面板（真实数据接入）
- [ ] 告警通知推送（邮件/钉钉）
- [ ] API Key 管理界面

---

## Day 18 - 2026-06-22 ✅ V5.33 API Key 管理 + 告警邮件钉钉推送

**今日完成：**
- [x] **用户 API Key 管理**：前后端全链路（SHA-256 哈希 / rawKey 一次性展示 / 轮换 / 禁用启用）
  - 后端：UserApiKey 实体 + Mapper + Service + Controller（5 端点）
  - 前端：`/apikey` 页面（列表/创建/复制/轮换/删除）
  - SQL：新增 `user_api_key` 表
- [x] **告警邮件 + 钉钉推送**：AlertNotifier 接口 + EmailAlertNotifier + DingTalkAlertNotifier
  - AlertNotifierManager 按优先级调用所有渠道
  - AlertEngine 触发时自动调用 notifierManager.notifyAll()
  - SMTP + WebHook 签名密钥支持
  - SQL：新增 `alert_channel` 表
- [x] **自检脚本修复**：self-check.sh（过时 SQL 文件名）+ java-static-check.sh（package 声明检查范围）

**明日计划 Day 19：**
- [ ] API Key 鉴权过滤器（网关拦截 Bearer Token）
- [ ] 完整 README + 架构图更新
- [ ] API Key 速率限制

## Day 19 - 2026-06-23 ✅ API Key 鉴权过滤 + API Key 限流

**今日完成：**
- [x] **API Key 鉴权过滤器** (`ApiKeyAuthGlobalFilter`): 网关拦截 `Bearer mmx_xxxx` → Redis 缓存验证结果(5min TTL) → WebClient 调用 auth 服务 `/internal/apikey/validate` → 注入 `X-User-Id` 头, 优先级 Order=-200 (早于 JWT Filter)
- [x] **API Key 内部验证接口** (`ApiKeyInternalController`): auth 模块新增 `POST /internal/apikey/validate`, 供网关内部调用
- [x] **API Key 限流解析器** (`ApiKeyRateLimitResolver`): 按 `userId > API Key SHA-256 > IP` 优先级限流, 覆盖 auth/chat/model/agent/admin 全部路由
- [x] **自检脚本**: 新建 `scripts/self-check.sh` (SQL/Maven/前端) + `scripts/java-static-check.sh` (package/TODO/System.out)
- [x] **静态体检修复**: IpUtils.java `System.out` → `log.info`, GatewayApplication.java `System.out` → `log.info`
- [x] **Gateway 配置更新**: 4 条路由切换为 `apiKeyRateLimitResolver`, model 路由新增限流配置
- [x] **文档更新**: README.md + ARCHITECTURE.md 同步 API Key 鉴权 + 限流说明

**关键数据：** +3 后端文件 (filter/config/controller) / +2 脚本 / 修改 4 文件 / 0 TODO 残留

**明日计划 Day 20：**
- [ ] 外部 API 文档 (Swagger 聚合 / Apifox / Postman collection)
- [ ] 端到端测试 (集成测试 + 健康检查脚本)
- [ ] API Key 配额/用量统计页面
- [ ] 性能压测报告 (wrk / JMeter)

## Day 20 - 2026-06-24 ✅ V5.9 API Key 统计 + E2E 健康检查 + Postman Collection

**今日完成：**
- [x] **API Key 用量统计** (前后端全链路)
  - 后端: `ApiKeyStatsService` (`summary` / `newKeysTrend`) + `AdminController` 新增 2 端点
  - 前端: `Stats.vue` KPI 卡片 + ECharts 饼图 + Top 用户排行榜
  - 侧边栏: 超级管理员新增「📊 Key 统计」入口
- [x] **E2E 健康检查脚本** (`scripts/e2e-health-check.sh`)
  - 12 个微服务: Gateway/Auth/Chat/Model/Memory/RAG/Function/Agent/Monitor/Admin/Analytics/Prompt
  - 彩色输出 + 环境变量覆盖端口 + WebSocket TCP 检测
- [x] **Postman API Collection** (`docs/minimax-api.postman_collection.json`)
  - 13 分组 50+ 请求，自动登录脚本 + Collection Variables
- [x] **自检通过**: Maven 19 模块 ✅ / 前端构建 ✅ / 静态体检 0 错误 ✅

**明日计划 Day 21：**
- [ ] 性能压测脚本 (wrk / Bash 并发压测)
- [ ] API 文档导出为 Markdown / PDF
- [ ] API Key 配额前端告警提示
- [ ] 外部 API 限流规则 CRUD 页面

## Day 22 - 2026-06-27 ✅ WebSocket 联调 + 鉴权测试 + RAG 链路 + CI 压测

**今日完成：**
- [x] **WebSocket 端到端联调**：Stream.vue 加 JWT token，`frontend/src/utils/ws.js` 通用 WS 工具类（重连/心跳/自动解析），notification store 重构
- [x] **API Key 鉴权单元测试**：`ApiKeyAuthGlobalFilterTest`（5 用例覆盖），`ApiKeyAuthGlobalFilter` 重构加测试用构造函数
- [x] **RAG 完整链路测试**：5 个新用例（多文档上传/切片/检索、多 KB 隔离、问答应答、格式路由）
- [x] **CI/CD 压测 Stage**：新增 `perf-test` job（wrk + ab，30s/50并发，GitHub Artifacts 上传）

**明日计划 Day 23：**
- [ ] 前端 E2E 测试（Playwright）集成到 CI
- [ ] 监控告警系统完整链路测试
- [ ] 前端知识库管理 UI 完善
- [ ] API Key 管理前端 UI

## Day 23 - 2026-06-28 ✅ E2E 测试 + 监控告警链路 + Playwright CI

**今日完成：**
- Playwright E2E 测试框架：3 个 spec (login/navigation/chat, ~22 用例) + config + npm scripts
- Playwright E2E CI Job：frontend 依赖 + serve 静态服务 + playwright test (non-blocking)
- 监控告警链路测试：AlertNotifierManager/Email/DingTalk/MonitorController 4 个测试类 (~30 新用例)
- 确认知识库管理 UI (560行) + API Key 管理 UI (299行) 完整无需额外完善

**自检：** self-check 5/5 ✅ | java-static 0 错误 ✅ | 前端构建 57.73s ✅

**明日计划 Day 24：** CI/CD E2E 测试 job 真实运行调试 / 告警渠道管理前端 UI / 性能基准测试报告更新

## Day 24 - 待开始

## V5.9 (2026-06-21) — Dashboard 真实图表 + 告警规则 CRUD + WS 精确分流

- ✅ Dashboard 折线图接 by-day API (admin/audit/by-day, 7天 3条线)
- ✅ Monitor 告警规则 CRUD UI (新增/编辑/删除 + 13服务下拉)
- ✅ nginx 拆分 WS: /ws/notifications → auth 直连, /ws/* → gateway
- ✅ CHANGELOG + 静态体检通过

## V5.5-V5.8 (历史)
- V5.5: Spring Cloud Gateway (WebFlux) + 12 routes + JwtAuth + 限流 + CORS
- V5.6: Dashboard 真实数据 + KG ECharts + 监控面板 + yml 清理
- V5.7: Nacos 服务发现 + Resilience4j + lb:// 转发
- V5.8: TraceFilter + 智能分包 + nginx gzip/br + http X-Trace-Id

## Day 21 - 2026-06-26 ✅ API 文档体系 + 压测模板

**今日完成：**
- [x] **OpenAPI 3.0 规范**：`docs/openapi.yaml`（~42KB，92+ 端点，10 模块全覆盖）+ `docs/openapi.json`
- [x] **Postman Collection**：`docs/postman/MiniMax-Platform.postman_collection.json`（45+ 请求，Token 自动注入）
- [x] **E2E 健康检查脚本**：`scripts/health-check.sh`（18 项检查，14 服务全覆盖，自动获取 Token）
- [x] **压测模板**：`bench/wrk/`（Lua 脚本）+ `bench/jmeter/minimax-api-test.jmx`（3 梯度）+ `bench/run.sh` + `bench/README.md`
- [x] **自检脚本升级**：环境感知版（mvn 缺失不误报）

**关键数据：** +7 新文件 / 前端构建 1m20s ✅ / 静态体检 0 错误 ✅

**明日计划 Day 22：**
- [ ] WebSocket 端到端联调
- [ ] RAG 完整链路测试（上传/切片/检索）
- [ ] API Key 鉴权单元测试
- [ ] CI/CD 压测 stage

