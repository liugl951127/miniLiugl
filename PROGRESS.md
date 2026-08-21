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

## Day 24 - 2026-06-28 ✅ 告警渠道管理前端 + E2E CI 修复 + 性能报告更新

**今日完成：**
- 告警通知渠道管理 UI（监控页面内嵌）：3 种类型（EMAIL/DINGTALK/WEBHOOK）+ 配置预览 + CRUD
- monitor.js 新增 5 个 alert channel API 调用
- E2E CI Job 修复：JUnit 解析逻辑 + Summary 表格 + 失败检测条件
- 性能压测报告 bench/README.md 新增 E2E 测试章节

**自检：** self-check 5/5 ✅ | java-static 0 错误 ✅ | 前端构建 1m 37s ✅

**明日计划 Day 25：** 告警渠道 UI 联调 / Vitest 单元测试 / API 文档导出

## Day 25 - 2026-07-02 ✅ Vitest 单元测试 + API 文档导出

**今日完成：**
- Vitest 框架搭建: vitest + @vitest/ui + jsdom + vitest.config.js
- 32 个单元测试用例 (monitor.test.js 24 + auth.test.js 8) 全部通过 ✅
- scripts/gen-api-docs.js: openapi.yaml → docs/API.md (20KB)
- 修复 openapi.yaml 重复键问题

**自检：** self-check 5/5 ✅ | java-static 0 错误 ✅ | npm build 1m 14s ✅

**明日计划 Day 26：** 告警渠道 E2E / CI Vitest job / 监控图表懒加载 / E2E 补充

## Day 26 - 2026-07-14 ✅ 告警渠道端到端联调 + CI Vitest Job + Dashboard 懒加载 + Playwright E2E 补充

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

## Day 27 - 2026-07-14 ✅ 告警渠道真发送 / history查真实表 / E2E CI优化 / WebSocket实时推送

## Day 28 - 2026-07-14 ✅ 告警通知模板+变量替换 / Ack写库 / 审计日志资源类型筛选

## Day 29 - 2026-07-14 ✅ 智能化提升: QueryComplexity + SmartModelRouter + IntentConfidenceScorer

## Day 30 - 2026-07-27 ✅ 智能化提升 V2: 多模型投票/RAG查询重写/告警RCA/日志异常检测

**今日完成：**
- [x] **MultiModelVotingService** (`minimax-ai`): confidence < 0.50 触发多模型并行推理，3 种投票策略 (MAJORITY/CONFIDENCE_WEIGHTED/LLM_JUDGE)，CompletableFuture 并发，LLM 生成简洁答案方便比较
- [x] **QueryExpander** (`minimax-rag`): MiniMax-Text-03 查询展开，3 种策略 (SYNTACTIC/SEMANTIC_LLM/HYBRID)，并发多展开检索 + chunkId 去重合并 topK
- [x] **AlertRcaService** (`minimax-monitor`): LLM 推理告警根因，7 类分类 (RESOURCE/CONFIG/EXTERNAL/CODE/TRAFFIC/NETWORK/UNKNOWN)，含规则预分类快路径 + 4 条优先级建议操作
- [x] **LogAnomalyDetector** (`minimax-monitor`): 无监督异常检测，4 种算法加权 (Z-Score 35%/EWMA 30%/IQR 20%/Spike 15%)，滑动窗口 + 环形缓冲区，支持批量检测 + 指标摘要
- [x] `scripts/self-check.sh` + `scripts/java-static-check.sh` 新增
- [x] 前端 npm build 1m 22s ✅ | 自检 13/13 ✅ | 静态检查 5/5 ✅

**代码量:** +4 服务类 (~57KB) + 2 脚本

**明日计划 Day 31：**
- [ ] 多模型投票集成到 ChatController
- [ ] QueryExpander 默认启用到 RagService
- [ ] AlertRcaService 与 AlertEngine 联动
- [ ] LogAnomalyDetector 与告警规则绑定

## Day 31 - 2026-08-02 ✅ 智能化集成落地（投票对话 / RAG展开检索 / 告警RCA联动 / 异常检测告警）

**今日完成：**
- [x] VotingChatController（多模型投票对话，置信度自动触发 / 强制投票，3 端点）
- [x] RagService 深度集成 QueryExpander（默认启用展开检索，RagAnswer 新增 strategy/elapsedMs）
- [x] AlertEngine 接入 AlertRcaService（告警触发时自动 RCA 分析，结果写库并追加到消息）
- [x] AlertEngine 接入 LogAnomalyDetector（异常分触发独立告警事件，5min 冷却）
- [x] MonitorController 新增 RCA + 异常检测 4 个端点
- [x] 自检通过：13/13 ✅ | 静态检查 5/5 ✅ | 前端 dist ✅

**关键文件数：** +4 文件修改
**报告：** `reports/day-31-report.md`

**明日计划 Day 32：**
- [ ] 告警前端 UI 接入 RCA 结果展示
- [ ] 异常检测历史趋势图（前端图表接入 anomaly API）
- [ ] 前端投票对话 UI（展示多模型答案 + 一致率）
- [ ] V4.3 收尾文档更新

---

## Day 32 - 2026-08-03 ✅ 前端智能化落地（RCA 弹窗 / 异常检测图表 / 投票对话 UI）

**今日完成：**
- [x] 前端 API 补全：monitor.js 新增 rcaAnalysis/anomalySummary/activeAnomalyMetrics；ai.js 新增 votingChat/forceVotingChat/votingInfo
- [x] Monitor Index.vue：RCA 分析弹窗（根因类别/置信度/建议操作）+ 异常检测 Z-Score ECharts 仪表图 + 指标选择下拉框
- [x] AiChat.vue：投票模式切换 + 投票结果面板（策略/一致率/各模型答案）+ handleSend 重构（连接 VotingChatController）
- [x] 自检 13/13 ✅ | Java 静态 5/5 ✅ | 前端 dist 构建 ✅

**明日计划 Day 33：**
- [ ] 投票对话完整联调
- [ ] RCA 弹窗联调
- [ ] V4.3 文档收尾

---

## Day 33 - 2026-08-04 ✅ 前端 API 路径修复 + 投票对话联调验证

**今日完成：**
- [x] **monitor.js API 路径修复**：4 个 RCA/异常检测 API 补 `/api/v1` 前缀（`rcaAnalysis`、`anomalyDetect`、`anomalySummary`、`activeAnomalyMetrics`）
- [x] **后端投票对话端点验证**：VotingChatController 3 端点 + MultiModelVotingService 419 行完整
- [x] **自检 13/13 + Java 静态检查 5/5 + 前端 npm build ✅**
- [x] **前端 npm install --legacy-peer-deps + build（NODE_OPTIONS=1536MB，54.87s）**

**明日计划 Day 34：**
- [x] 前端构建集成到 self-check.sh
- [x] V4.3 文档收尾（API.md / CHANGELOG.md）
- [x] Monitor 告警确认弹窗完善
- [x] AiChat.vue SSE 流式响应

---

## Day 34 - 2026-08-05 ✅ 前端构建自动化 + Monitor 确认弹窗 + SSE 流式聊天

**今日完成：**
- [x] **self-check.sh**：`check_build()` 函数，`npm run build` 实际执行（14/14 ✅）
- [x] **文档收尾**：`docs/CHANGELOG.md` / `docs/API.md` 与根目录同步；README.md 版本号 V5.26 → V4.3
- [x] **Monitor 确认弹窗**：SQL `notes` 列 → Entity → Controller → API → Alerts.vue 弹窗（确认人/确认时间/备注）
- [x] **AiChat SSE**：新增 `chatStream()` API，`handleSend` 分流（SSE 流式 / REST 投票），ChatBubble 打字光标

**明日计划 Day 35：**
- [ ] 前端错误边界（ErrorBoundary 组件）
- [ ] Monitor 静默功能后端 API
- [ ] AiChat.vue 流式光标 CSS 优化
- [ ] 平台首页 SEO / Sitemap

## Day 35 - 2026-08-06 ✅ 静默功能全链路 + SEO 增强

**今日完成：**
- [x] **Monitor 静默 API**：4 端点（alert 实例 + rule 规则级），支持 minutes/endTime 参数，AlertEngine 规则级检查
- [x] **SQL 双表**：alert_event + alert_rule 加 `silenced_until` 列
- [x] **前端静默 UI**：时长选择对话框（30m-1w），firing 卡片 + rules 表格静默/解除按钮，字段映射修复
- [x] **ErrorBoundary**：验收通过（App.vue 全局嵌入，ErrorState 支持 7 类错误）
- [x] **流式光标 CSS**：ChatMessage 优化（柔和色+晕光+smooth blink），AiChat typing fade 动画
- [x] **SEO**：index.html 加 OG/Twitter/canonical meta；新增 sitemap.xml（11 路由）+ robots.txt

**明日计划 Day 36：**
- [ ] Monitor 静默功能前端联调测试
- [ ] RAG 上传切片端到端测试
- [ ] 前端 AiChat SSE reconnect 逻辑
- [ ] 前端知识库上传进度条

## Day 36 - 2026-08-07 ✅ SSE Reconnect + Monitor 静默验证 + RAG E2E

**今日完成：**
- [x] **AiChat SSE Reconnect**：`chatStream()` 全面重构（AbortController / 指数退避 / buffer 跨 chunk 拼接 / 最多 2 次重连）
- [x] **AiChat.vue 增强**：isStreaming / reconnectingStatus / stopStream / 停止按钮 / 重连状态条
- [x] **Monitor 静默联调验证**：API 路径前后端一致（4 端点全对）；AlertEngine 规则/实例级 silencedUntil 检查 ✅
- [x] **AlertSilenceTest**：3 用例（正常触发 / 实例静默持久化 / 规则静默跳过 evaluateRule）
- [x] **RagUploadProgressTest**：3 用例（TXT 切片 / 长文本多 chunk / TextChunker 边界）
- [x] **自检**：14/14 ✅ | 静态检查 5/5 ✅ | npm build ✅

**明日计划 Day 37：**
- [ ] 前端知识库批量上传 UI
- [ ] 投票对话一致率 ECharts 图表
- [ ] ErrorBoundary 与静默通知联动
- [ ] V4.3 release notes

## Day 37 - 2026-08-08 ✅ 批量上传 + 投票图表 + ErrorBoundary 静默 + V4.3 Release

**今日完成：**
- [x] **知识库批量上传**：`el-upload` multiple + 上传队列面板（逐文件进度/单个取消/全量取消/单个重试）
- [x] **VoteStats.vue**：4 个 ECharts 图表（折线/饼/柱图）+ KPI 卡片 + 分页表格 + 详情弹窗；新路由 `/analytics/vote-stats`
- [x] **analytics.js**：新增 3 个投票统计 API（`getVoteStatsSummary` / `getVoteTrend` / `getVoteRecords`）
- [x] **ErrorBoundary.vue V7.0**：`onErrorCaptured` + `ElNotification` 静默通知（duration=0 / 3秒去重 / 堆叠偏移）
- [x] **docs/RELEASE-V4.3.md**：完整 Release Notes（5 核心亮点 + 前后端改动清单）
- [x] **自检**：14/14 ✅ | 静态检查 5/5 ✅ | npm build ✅（yarn install 解决 npm 10.x peer dep 冲突）

**关键数据：** +1 新 Vue 文件（VoteStats.vue 17KB）+ 1 新 MD + 多文件改动

**明日计划 Day 38：**
- [ ] 投票统计后端 API 实现（GET /ai/voting/stats）
- [ ] 投票统计页接入真实 API
- [ ] 批量上传 KB 级压力测试
- [ ] 静默状态实时监控面板

## Day 38 - 2026-08-09 ✅ 投票统计真实 API + Monitor 静默徽章 + 自检 14/14

**今日完成：**
- [x] VoteStats.vue：删除 mock 数据，3 个真实 API 接入（getVoteStatsSummary/getVoteTrend/getVoteRecords）
- [x] Alerts.vue：修复 firingCount/currentTabLabel/totalAlerts 未定义 bug，新增 silencedCount + 🔇 静默徽章 + is-silenced 视觉区分
- [x] 全链路 API 路径扫描（http.js 前缀统一 / 投票 API 路径验证 / monitor 静默 API 路径验证）
- [x] self-check.sh SQL 条件修复（>= 2 → >= 1，反映 SQL 已合并现状）
- [x] yarn install + npm build ✅ 59.74s

**明日计划 Day 40：**
- [ ] 投票详情弹窗接入真实 model_votes 数据
- [ ] Monitor 静默告警自动刷新
- [ ] RAG 上传多阶段进度（上传/解析/切片/索引）
- [ ] AiVotingService 异常回退逻辑完善

## Day 39 - 2026-08-10 ✅ 投票真实DB + 详情弹窗 + Monitor仪表盘 + RAG进度精确化

**今日完成：**
- [x] **投票后端真实 DB**：新增 `ai_voting_record` 表 + 10条种子数据 + Entity/Mapper/Service 重构 Controller（去掉全部 mock）
- [x] **投票详情弹窗**：el-dialog 展示各模型答案 + 一致率进度条 + 置信度条；列对齐真实 API（text/answer/totalVotes/agreementRate）
- [x] **Monitor Dashboard**：全新 ~280 行，含 🔇 静默告警圆形仪表盘 + 系统健康 + CPU/内存/JVM/会话实时指标
- [x] **RAG 进度精确化**：显示字节数 `2.4 MB / 10.5 MB` + 上传阶段标签 + stroke-width 增强
- [x] 自检：14/14 ✅ | 静态检查 5/5 ✅ | yarn + npm build ✅（1m 19s）
- [x] 修复 2 个 stub 文件 package 声明被注释（MissingAiController / GlobalMissingController）

**关键数据：** +3 后端文件（Entity/Mapper/Service）/ 1 新 SQL 表 / 3 Vue 文件改动 / 4 项自检全过

**明日计划 Day 40：**
- [ ] 投票详情弹窗接入真实 model_votes 数据
- [ ] Monitor 静默告警自动刷新
- [ ] RAG 上传多阶段进度（上传/解析/切片/索引）
- [ ] AiVotingService 异常回退逻辑完善

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


## Day N+1 (2026-07-13) - V3.5.4 ✅
- [x] Spark-style 多机并行训练框架 (3 worker, RDD-like, DAG topo, Shuffle)
- [x] 15 单元测试全过 (parallelize/map/filter/reduce/groupByKey/multiStage/shuffle)
- [x] Bug 修复: root stage shuffleId 必设, parentShuffleId 链式读取, groupByKey 跨 partition 合并
- [x] 89 表 + 59 INSERT DDL H2 验证 0 错误
- [x] 前端 npm build 4.7M dist (vite build)
- [x] 15 服务 H2 沙箱启动 + 6 个 V3.4-V3.5 API 实测全过
- [x] 企业文档第 13 章 (Spark 多机并行)
- [x] push e38138b

## Day 40 - 2026-08-11 ✅ 投票详情真实数据 + Monitor 自动刷新 + RAG 4阶段进度

**今日完成：**
- [x] 投票详情弹窗接入真实 model_votes 数据（后端 modelVotes 完整详情 + activeModels 真实计数）
- [x] Monitor 静默告警自动刷新（30s 轮询 + onUnmounted cleanup）
- [x] RAG 上传 4 阶段进度条（上传→解析→切片→索引，带编号圆点指示器）
- [x] AiVotingService.saveVotingRecord() 异常回退（DB 不可用静默忽略）
- [x] 自检 14/14 + 静态 5/5 + npm build 54.69s

**关键数据：** 后端 1 文件 / 前端 3 文件 / yarn install 113s

**明日计划 Day 41：**
- [ ] RAG 后端 SSE 进度事件（真正的解析/切片/索引阶段）
- [ ] 投票历史「重新投票」入口
- [ ] Monitor 告警详情页面（RCA + 日志上下文）

## Day 41 - 2026-08-12 ✅ RAG SSE 真实进度 + 投票重新投票 + Monitor 告警详情

**今日完成：**
- [x] RAG 后端 SSE 进度事件（DocumentService.uploadWithProgress + RagController /doc/upload-stream）
- [x] 投票历史「重新投票」入口（AiVotingService.duplicateVote + 前端按钮）
- [x] Monitor 告警详情页面（后端 /alerts/{id} + 前端详情弹窗含 RCA）
- [x] 自检 14/14 + 静态 5/5 + yarn build ✅

**关键数据：** 后端 6 文件 / 前端 4 文件 / yarn install 151s / build ✅

**明日计划 Day 42：**
- [ ] RAG 上传失败自动重试机制
- [ ] 投票历史导出 CSV
- [ ] Monitor 告警通知渠道管理（邮件/钉钉 Webhook 测试）

## Day 42 - 2026-08-13 ✅ RAG 自动重试 + 投票 CSV 导出 + Monitor 通知渠道管理

**今日完成：**
- [x] RAG 上传失败自动重试机制（指数退避 1s→2s→4s→8s，4次，onRetry 回调）
- [x] 投票历史导出 CSV（UTF-8 BOM + 500条记录 + 实时 loading）
- [x] Monitor 通知渠道管理（新增 Tab：邮件/钉钉/企微/Webhook/SMS + CRUD + 测试）
- [x] 自检 14/14 + 静态 5/5 + vite build 689 modules 49.65s ✅

**关键数据：** 前端 4 文件改动 / self-check 修复 vite fallback 逻辑

**明日计划 Day 43：**
- [ ] RAG 文档全文检索高亮（搜索结果片段定位原文）
- [ ] Monitor 告警 SLA 统计（MTBF / MTTR / 可用率）
- [ ] 投票结果邮件通知

## Day 43 - 2026-08-14 ✅ RAG 检索高亮 + Monitor SLA 统计 + 投票邮件通知

**今日完成：**
- [x] RAG 检索结果高亮（后端 highlight() + 前端 v-html <mark> 标签渲染）
- [x] Monitor SLA 统计 Dashboard（MTBF / MTTR / 可用率 / SLA 等级，7/30/90 天窗口）
- [x] 投票结果邮件通知（后端 notifyVotingResult() + 前端邮箱输入框 + SQL notify_email 字段）
- [x] 自检 13/13 ✅ + 静态 5/5 ✅ + vite build 56.64s ✅

**关键数据：** 后端 6 文件 / 前端 3 文件 / SQL 1 文件

**明日计划 Day 44：**
- [ ] RAG 文档全文阅读（点击搜索结果展开完整文档内容）
- [ ] Monitor 历史告警趋势图（折线图展示近 30 天告警趋势）
- [ ] 投票历史详情弹窗（展示各模型答案 + 置信度）

## Day 44 - 2026-08-15 ✅ RAG全文阅读 + Monitor趋势图 + 投票详情弹窗

**今日完成：**
- [x] **RAG 文档全文阅读**：后端 `GET /api/v1/rag/doc/{id}/content` + `DocumentService.getById()` + 前端「阅读全文」按钮 + 800px 全文弹窗（名称/类型/大小/内容/pre-wrap）
- [x] **Monitor 告警趋势图**：后端 `GET /api/v1/monitor/alerts/trend` 按天聚合 + 前端新 Tab（ECharts 折线+柱状混合图，7/14/30天切换，CRITICAL/WARNING/INFO 三线 + 统计卡片）
- [x] **投票详情弹窗**：后端 `modelVotes` 映射补全 + 前端详情弹窗（问题/策略/一致率/各模型答案+置信度进度条）+ 列表「查看详情」按钮
- [x] 自检 13/13 ✅ + 静态 5/5 ✅ + vite build 49.10s ✅

**关键数据：** 后端 3 文件 / 前端 5 文件 / API 2 函数

**明日计划 Day 45：**
- [ ] RAG 文档在线编辑（修改内容 + 重新切片 + 重新索引）
- [ ] Monitor 告警升级策略（CRITICAL 自动通知）
- [ ] 前端性能优化（虚拟滚动 / 路由懒加载）
- [ ] V4.4 Release 打包准备

## Day 45 - 2026-08-16 ✅ RAG文档在线编辑 + Monitor告警升级策略 + 前端性能优化

**今日完成：**
- [x] **RAG 文档在线编辑**：后端 `PUT /api/v1/rag/doc/{id}/content` + `updateDocContent()`（修改内容 + 删除旧chunk + 重新切片 + 重新向量化 + 更新KB计数）+ 前端 EditPen 按钮 + 860px 编辑弹窗（字数统计 + 预估切片数 + 进度条）
- [x] **Monitor 告警升级策略**：AlertRule 新增 `escalateAfterMinutes` / `escalationChannel` / `autoResolveMinutes` + AlertEvent 新增 `escalated` / `escalatedAt` + `checkEscalation()` 定时任务（每60s检查CRITICAL超时事件）+ SQL schema 更新
- [x] **Monitor 前端升级配置**：新增「告警规则」Tab（规则列表 + 新建/编辑/启用禁用/删除）+ 规则编辑弹窗（升级等待时间/升级渠道/自动恢复时间）+ `getAllAlertRules()` API
- [x] **前端性能优化**：路由懒加载确认已有 ✅ + analytics NL2SQL 结果表格分页（每页20条，el-pagination）
- [x] 自检 13/13 ✅ + 静态 5/5 ✅ + vite build 1m7s ✅

**关键数据：** 后端 5 文件 / 前端 3 文件 / SQL 1 文件 / API 1 函数

**明日计划 Day 46：**
- [ ] RAG 多文档批量编辑 + 批量重新索引
- [ ] Monitor 告警自动恢复（auto-resolve）定时任务完善
- [ ] 前端图片懒加载优化
- [ ] V4.4 Release 打包 + CHANGELOG 更新

## Day 46 - 2026-08-17 ✅ RAG批量重索引 + Monitor自动恢复 + 图片懒加载

**今日完成：**
- [x] **RAG 批量重索引**：`POST /api/v1/rag/doc/batch/reindex` + `batchReindexDocs()` + 前端勾选+弹窗+结果展示
- [x] **Monitor 自动恢复**：`checkAutoResolve()` 每60s定时任务 + `AlertEvent.resolvedBy=SYSTEM` + SQL schema
- [x] **图片懒加载**：multimodal/Index.vue 三个 img 标签加 `loading="lazy"`
- [x] **V6.8.3 Release**：CHANGELOG.md 更新
- [x] 自检 13/13 ✅ + 静态 5/5 ✅ + vite build 1m8s ✅

**明日计划 Day 47：**
- [ ] RAG 文档批量删除
- [ ] Monitor 告警统计 Dashboard（历史趋势）
- [ ] 前端搜索高亮优化
- [ ] API 限流中间件完善

## Day 47 - 2026-08-18 ✅ RAG批量删除 + Monitor统计概览 + 限流配置

**今日完成：**
- [x] **RAG 批量删除**：`DELETE /api/v1/rag/doc/batch` + `batchDeleteDocs()` + 前端红色危险按钮 + 二次确认弹窗 + 成功/失败结果展示
- [x] **Monitor 统计概览**：`GET /api/v1/monitor/alerts/statistics` + `getStatistics()` + 新增「统计概览」Tab（6个数字卡片 + 严重程度分布 + Top 5 触发规则）
- [x] **限流配置完善**：`RateLimitService` 新增 `strict` 档位（10次/60秒）+ `application-common.yml` 完整配置
- [x] 自检 13/13 ✅ + 静态 5/5 ✅ + vite build 57.46s ✅

**关键数据：** 后端 4 文件 / 前端 2 文件 / 配置 1 文件

## Day 48 - 2026-08-19 ✅ RAG批量导出 + Monitor ECharts可视化 + 深色模式 + JWT续期

**今日完成：**
- [x] **RAG 批量导出 PDF/TXT**：`POST /api/v1/rag/doc/export` + PDFBox 3.0.2 生成 PDF + 绿色导出按钮 + 弹窗选格式 + fetch blob 下载
- [x] **Monitor ECharts 可视化**：`GET /api/v1/monitor/alerts/timeseries` + 统计概览 Tab 新增饼图（严重程度分布）+ 柱状图（每日趋势）+ 导出图片按钮 + resize 自适应
- [x] **深色模式切换**：user_preferences 表 + UserPreference 实体/Mapper/Service + AuthController PATCH /preferences/theme + preferences store + layout 主题按钮 + CSS 变量覆盖
- [x] **JWT 续期增强**：JwtAuthenticationFilter 注入 X-Token-Refresh header + AuthController /validate 端点 + user store silentRefreshIfNeeded + http.js 主动续期检测
- [x] 自检 13/13 ✅ + 静态 5/5 ✅ + vite build 59.77s ✅

**关键数据：** 后端 8 文件 / 前端 5 文件 / SQL 1 表

## Day 49 - 2026-08-20 ✅ 文档预览 + Monitor SSE实时推送 + 通知中心深色模式 + RAG高亮复制

**今日完成：**
- [x] **RAG 文档在线预览**：文档抽屉表格新增预览按钮（👁 View）+ 预览弹窗大改版（90vw移动端适配 + 字符数统计 + 复制全文按钮 + doc-preview-body 深色模式 CSS 变量）
- [x] **Monitor 告警实时推送 SSE**：前端 `EventSource` 连接 `/api/v1/monitor/alerts/stream` + 监听 `alert` 事件插入列表 + 5秒重连 + 告警历史标签页实时状态指示器（●绿/○红）+ 新告警计数 badge + `ElMessage.warning` 弹窗提醒
- [x] **通知中心深色模式适配**：全部硬编码颜色替换为 CSS 变量（`--el-color-primary / --el-fill-color / --el-text-color-*`）+ `.notif-item` hover/unread 深色模式嵌套样式
- [x] **RAG 检索高亮 + 复制片段**：检索结果展开状态新增「复制片段」按钮 + `copyChunk()` 函数 + CSS 变量化改造检索样式（背景/边框/文字）
- [x] **echarts 版本冲突修复**：`echarts ^6.1.0` → `^5.5.1`（vue-echarts@7.x 兼容）+ yarn install 替代 npm（npm 崩溃修复）+ 自检 13/13 ✅ + 静态 5/5 ✅ + vite build 49.97s ✅

**关键数据：** 前端 3 文件修改（knowledge/Index.vue + monitor/Index.vue + notification/Index.vue）

**明日计划 Day 50：**
- [ ] 知识库文档预览支持 Word/PDF 渲染（pdf.js / mammoth.js）
- [ ] 前端全站深色模式一致性审查（重点页面扫一遍）
- [ ] RAG 检索结果来源标注（doc name + chunk id）
- [ ] Monitor 新增 SLA 达标率计算与展示

## Day 50 - 2026-08-21 ✅ 文档渲染增强 + 检索来源标注 + SLA 达标率 + 深色模式补全

**今日完成：**
- [x] **RAG 文档预览增强**：文件类型语义化标签（PDF/DOCX/MD/TXT）+ mammoth.js CDN 动态加载（DOCX 转 HTML）+ markdown-it 渲染（MD 文件）+ 完整 CSS 样式
- [x] **RAG 检索结果来源标注**：检索结果卡片新增文档名 badge + 类型 tag + 切片编号 tag（docTitle/docSource/chunkIndex）
- [x] **Monitor SLA 达标率**：新增达标率卡片（✅/❌ 达标状态 + 目标阈值 + 偏差值 + 距离下个等级 + 进度条）
- [x] **深色模式一致性**：About / admin/Alerts / admin/Dashboard / agent/Canvas / agent/Auto 共 5 个文件，30+ 处硬编码颜色替换为 CSS 变量
- [x] 自检 13/13 + 静态 5/5 + vite build 48.46s ✅

**明日计划 Day 51：**
- [ ] Admin 管理后台深色模式补全
- [ ] RAG 检索结果排序优化（相关性 + 时效性加权）
- [ ] Monitor 历史告警高级筛选
- [ ] 前端骨架屏 + 首屏优化

