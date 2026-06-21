# Changelog

所有重要的项目变更都记录在此。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

## [1.0.0] - 2026-06-16

### 14 天路线图全部完成 🎉

### Day 13 - 2026-06-16
#### Added
- 限流: Bucket4j 多维度 (IP/User/Global) + RateLimitService 配置化
- 缓存: Caffeine CacheService (防击穿 + TTL + 统计)
- 异步: AsyncTaskService (UUID + 状态机 + 重试 + 回调 + Future)
- 请求日志: RequestLogFilter (traceId + 慢/错采点)
- 压测: benchmark.sh (Bash 并发: QPS + p50/p95/p99)
- 配置: minimax-optimized.yml (生产调优模板)
- SQL: request_log / async_task / rate_limit_rule (3 表)
- 测试: 11 用例 (限流/缓存/异步)
- 累计: **125 用例 0 失败**

### Day 12 - 2026-06-16
#### Added
- 监控模块: 11th microservice (8089)
- MetricsCollector: 5 Counter + 4 Gauge + 2 Timer (Micrometer)
- AlertEngine: 30s 评估 + 6 运算符 + 冷却 + 自动恢复
- HealthDetailService: 5 维度 (DB/JVM/Disk/Thread/System)
- SnapshotService: 60s 落库 + 30d 清理
- 5 默认告警规则 (CPU/JVM/磁盘/LLM延迟/错误率)
- Prometheus 集成: `/actuator/prometheus`
- SQL: metric_snapshot / alert_rule / alert_event (3 表)
- 测试: 11 用例 (HealthDetail + AlertEngine)
- 累计: **114 用例**

### Day 11 - 2026-06-16
#### Added
- 多模态模块: 10th microservice (8088)
- VisionService: OpenAI vision 协议 + Mock 降级
- 图片信息识别: PNG/JPEG/GIF/WebP magic number
- 醒目前端升级:
  - MarkdownView: Markdown + 代码高亮 + 复制按钮
  - ChatMessage: Markdown + 工具调用 + RAG 引用 + 拖拽图片
  - chat/Index.vue: 快速提问 + 拖拽上传 + 流式 + 工具展示
  - admin/Dashboard.vue: 健康 pill + KPI 卡片 + ECharts 折线/饼图
  - layout/Index.vue: 顶部服务健康 pill (30s 自动刷新)
- 测试: 7 用例 (VisionService)

### Day 10 - 2026-06-16
#### Added
- 管理后台模块: 9th microservice (8087)
- ServiceClient: Java 11+ HttpClient 跨服务客户端 (无 Feign 依赖)
- UserMgmtService: 代理 auth + 自动审计
- ModelMgmtService: 代理 model + 调限流审计
- StatsService: 业务统计 + dashboard 聚合
- HealthAggregator: 并发 ping 6 服务
- AuditService: 统一操作审计
- SQL: admin_audit_log
- 测试: 11 用例

### Day 9 - 2026-06-16
#### Added
- Function Calling 模块: 8th microservice (8086)
- 4 个内置工具: get_current_time / calculator / http_get / random_number
- 自实现表达式求值器 (不依赖 Nashorn, Java 17 headless 兼容)
- ToolExecutor: 工具路由器 (内置 + HTTP)
- FunctionCallService: LLM + tool 循环 (最多 5 轮)
- SSRF 防护 + 字符白名单
- SQL: function_tool + function_call_log
- 测试: 23 用例

### Day 8 - 2026-06-16
#### Added
- RAG 模块完整版: 7th microservice (8085)
- 3 种文档解析器: TXT/MD (BOM 探测) / DOCX (POI) / PDF (PDFBox)
- 智能分块器 TextChunker (滑动窗口 500/50 + 位置跟踪)
- DocumentService: SHA-256 去重 + 状态机
- Retriever: 向量检索 topK + 引用填充
- RagService: 检索 + LLM 增强 + 3 级降级
- SQL: knowledge_base / document / document_chunk
- 测试: 19 用例

### Day 7 - 2026-06-16
#### Added
- 长期记忆 (向量库)
- Embedding 抽象: OpenAI 兼容 + Mock 离线
- 长期记忆 Service: store / recall (余弦) / recent / delete
- 用户偏好 Service: set/get/list/delete
- 跨会话 Context Builder: 短+长+偏好+摘要
- 真实 LLM 摘要升级 (调 model 服务)
- SQL: memory_long_term + memory_user_pref
- 测试: 12 用例 (新增)

### Day 6 - 2026-06-16
#### Added
- 短期记忆 (Redis + Caffeine 双层降级)
- ContextBuilder: 按 maxContext 智能裁剪
- Summarizer: 35→10 条触发压缩
- MemoryController 6 端点
- chat 模块独立 SessionContextCache
- 累计: 43 用例

### Day 5 - 2026-06-16
#### Added
- 真流式 (HttpClient BodyHandlers.ofLines)
- 取消机制 (streamId + stopFlag + POST /cancel)
- 打字机 + 停止按钮
- 前端 fetch + ReadableStream
- 累计: 33 用例

### Day 4 - 2026-06-16
#### Added
- 模型路由 (model_provider / model_config / model_quota)
- OpenAI 兼容 + Bucket4j 限流 (10/60s)
- 6 个模型: gpt-4o / MiniMax-Text-01 / VL-01 / ollama / qwen / mock
- Mock 模式开关 (无 key 也能演示)
- SSE StreamingResponseBody
- 累计: 30 用例

### Day 3 - 2026-06-16
#### Added
- 会话消息 (chat_session / chat_message)
- SessionController + MessageController
- 前端侧边栏
- 架构重构: JwtAuthenticationFilter 抽到 common
- H2 test profile
- 累计: 20 用例

### Day 2 - 2026-06-16
#### Added
- 用户鉴权: 5 表 + JWT 双 token (30min/7d)
- Spring Security 6 + BCrypt
- AuthController 5 端点
- Pinia user store + 401 自动 refresh
- 累计: 8 用例

### Day 1 - 2026-06-16
#### Added
- 项目骨架: 7 模块 Spring Boot 3 多模块
- Vue 3 + Element Plus + Pinia 前端
- docker-compose: MySQL + Redis + ES + MinIO
- 定时任务安装脚本
- 网关健康检查 + 平台介绍 API

---

## 整体数据

| 指标 | 14 天累计 |
|------|-----------|
| 后端模块 | 13 (v4.3 新增 minimax-prompt 8091) |
| Java 文件 | 241+ |
| SQL 文件 | 8+ |
| SQL 行数 | 1,000+ |
| 单元/集成测试 | 135+ (0 失败) |
| HTTP 端点 | 110+ |
| 数据表 (MySQL) | 22+ |
| 前端组件/视图 | 34+ |
| 部署脚本 | 4 |
| Git commits | 14+ |
| 当前版本 | V4.3 (Prompt 模板系统) |

## [V2.0] - 2026-06-16 — 4 大新功能

### Added
- **V2.1 Agent 自主任务**: 12th microservice (8090), ReAct 循环
  - Thought/Action/Observation 三段式 + `<final>` 包裹最终答案
  - 最多 8 轮, 每轮可视化
  - 区别于 Function Calling: Agent 是目标驱动, Function 是轮次驱动
- **V2.2 知识图谱**: 实体-关系存储 + 1跳/2跳 + 最短路径
  - 实体类型: person/place/org/concept/event
  - 关系类型: works_at/located_in/friend_of/owns/...
  - 重要性评分 1-10 + 别名 + 描述
- **V2.3 实时协作**: WebSocket 多人编辑会话
  - 消息广播 / typing 指示 / cursor 位置 / edit 同步
  - 房间创建/加入/关闭, owner/editor/viewer 角色
  - 持久化: collab_session + collab_member
- **V2.4 插件市场**: 系统插件 + 用户发布
  - 4 类型: Java class / HTTP / JS / WASM
  - 评分/下载量/启停
  - 4 个内置系统插件: weather-widget / markdown-export / code-formatter / translation
- **V2.5 前端可视化**: 4 页面
  - /agent: Agent 思考时间线 (步骤卡片 + 高亮颜色)
  - /kg: 知识图谱 (实体搜索 + 1跳/2跳邻居 + 关系创建)
  - /collab: WebSocket 多人协作 (在线列表 + 消息流 + typing)
  - /plugins: 插件市场卡片 (评分 + 下载 + 一键发布)

### SQL
- 6 张新表: agent_task / kg_entity / kg_relation / collab_session / collab_member / plugin
- 4 个内置插件种子数据

### Test
- 5 个新单测 (AgentServiceTest 2 + KnowledgeGraphServiceTest 3)
- Agent 模块 BUILD SUCCESS

### Build
- 12 个后端模块 BUILD SUCCESS
- 前端 4 新页面集成到路由
- vite proxy 加 /api/v1/agent (8090) + /ws (WebSocket)

## [V3.0] - 2026-06-16 — adminLiugl 超级管理员

### Added
- **adminLiugl 超级管理员**: 唯一超级管理员 (独立于普通 admin)
  - 账号: `adminLiugl` / `Liugl@2026` / 邮箱 `liugl951127@gmail.com`
  - 角色: `SUPER_ADMIN` (独立于 `ADMIN`)
  - 启动时由 `AdminDataInitializer` BCrypt 编码
- **SuperAdminGuard**: 通用权限检查工具
  - `isSuperAdmin()`: 当前用户是超级管理员?
  - `requireSuperAdmin()`: 强制要求超级管理员 (否则 403)
  - `isAdminOrAbove()`: ADMIN 或 SUPER_ADMIN
- **SuperAdminController**: 专属 API
  - `GET  /auth/super/me`              - 当前超级管理员信息 + 能力列表
  - `GET  /auth/super/users`           - 列出所有用户
  - `POST /auth/super/users/{id}/disable` - 禁用用户 (不能禁 adminLiugl)
  - `POST /auth/super/users/{id}/enable`  - 启用用户
  - `POST /auth/super/users/{id}/reset-pwd` - 重置密码
- **UserInfo.superAdmin**: 登录/me 返回新增布尔字段
  - adminLiugl 登录: `superAdmin: true`
  - admin 登录: `superAdmin: false`
- **前端超级管理控制台** (`/super`)
  - 顶部 👑 SUPER 徽章 (仅 adminLiugl 可见)
  - 侧边栏菜单 "超级管理" (仅 adminLiugl 可见)
  - 用户表格: 禁用/启用/重置密码
  - 平台统计: 总用户/活跃/禁用
  - 路由 guard: 强制要求 super admin
- **登录页提示**: adminLiugl 账号说明
- **SQL 16_super_admin.sql**: SUPER_ADMIN 角色 + 独立密码 (兼容已有库)

### Security
- adminLiugl 唯一超级管理员
- 不能被自己禁用 (`super admin 不能禁自己`)
- 普通 admin 无法访问 `/auth/super/*` (403)
- 前端路由 guard: 普通用户访问 `/super` → 跳首页

### Test
- 5 个 SuperAdminGuardTest (isSuperAdmin / requireSuperAdmin / admin 拒绝 等)
- 端到端验证:
  - adminLiugl 登录 → superAdmin:true ✅
  - admin 登录 → superAdmin:false ✅
  - adminLiugl 访问 /auth/super/me → success ✅
  - admin 访问 /auth/super/me → 403 ✅
  - adminLiugl 禁用 admin → success ✅
  - adminLiugl 禁用自己 → 异常 “禁止禁用超级管理员” ✅

### Build
- 12 个后端模块 BUILD SUCCESS
- 135 个测试 0 失败 (从 130 + 5)
- 前端 BUILD SUCCESS (含新增 super 页面)
- 端到端验证: H2 内存模式启动 auth 模块, 登录 + 权限检查全过

## [V4.3] - 2026-06-18 — Prompt 模板系统

### Added
- **minimax-prompt 模块**: 14th microservice (8091), Prompt 模板管理
  - PromptTemplate 实体: id / name / description / category / content / variables(JSON) / creatorId / isPublic / useCount / createdAt
  - PromptTemplateController 7 端点:
    - `GET  /prompts`            - 列表 (分页 + 分类过滤 + 搜索)
    - `GET  /prompts/{id}`       - 详情
    - `POST /prompts`            - 创建 (支持变量占位符 `{{variable}}`)
    - `PUT  /prompts/{id}`       - 更新
    - `DELETE /prompts/{id}`     - 删除 (软删)
    - `POST /prompts/{id}/use`   - 使用计数 +1
    - `GET  /prompts/categories` - 全部分类
  - PromptTemplateService: CRUD + 变量解析 (正则提取 `{{...}}`) + 分类聚合
  - 5 个系统内置模板: 翻译助手 / 代码审查 / 会议纪要 / 营销文案 / 故障排查
  - SQL: `prompt_template` 表
- **前端模板管理页面** (`/prompts`):
  - 卡片列表 + 搜索 + 分类筛选
  - 模板编辑器 (textarea + 变量高亮)
  - 变量填值弹窗 + 预览效果
  - 快速使用 (一键填入 chat 输入框)
  - 创建/编辑/删除交互
- **集成到 Chat 输入框**:
  - chat/Index.vue 顶部加 "📝 用模板" 快捷入口
  - 点击模板自动展开变量填写，填完直接填入消息框

### Build
- 13 个后端模块 BUILD SUCCESS (新增 minimax-prompt)
- 前端 BUILD SUCCESS

---

## [V4.2] - 2026-06-18 — WebSocket 流式 + PWA + i18n + 视频生成 + Agent DAG

### Added
- **minimax-ws 模块**: 13th microservice (8092), WebSocket 统一流式网关
  - StreamGatewayHandler 5 类型流式协议:
    - `chat`  / `vision`  / `audio`  / `agent`  / `battle`
    - 客户端: `cancel`  / `ping`  控制帧
    - 服务端: `ready`  / `chunk`  / `done`  / `error`  推送帧
  - WsApplication (Spring Boot 3, 独立端口)
  - WebSocketConfig 注册 `/ws/stream` 端点
  - SecurityConfig 全公开 (业务内 token 校验)
- **前端新页面**:
  - `StreamShowcase.vue` WebSocket 流式演示台 (5 类型 + 事件日志面板)
  - `VideoGenShowcase.vue` 文生视频 (6 模型: Sora/可灵/CogVideoX/万相/AnimateDiff/Mock)
  - `DagShowcase.vue` Agent DAG 工作流 (6 节点类型 + 拖拽 + 3 内置模板)
- **PWA 离线支持**: manifest.json + service worker
- **i18n 国际化**: 中文 + 英文双语 (vue-i18n)

### Build
- 13 个后端模块 BUILD SUCCESS (新增 minimax-ws)
- 前端 BUILD SUCCESS

---

## [V4.1] - 2026-06-18 — 文生图 + ASR/TTS + 排行榜 + Plugin SDK

### Added
- **ImageGenController** (minimax-model, 5 模型):
  - FLUX / SDXL / Kolors / 通义万相 / DALL-E
  - Mock 模式: prompt 哈希生成 SVG 渐变图 (data URI, 离线可用)
  - 真实模式: SILICONFLOW_API_KEY 调用
  - 端点: `GET /imagegen/models` , `POST /imagegen/generate`
- **AudioController** 语音能力 (ASR + TTS):
  - ASR: Whisper V3 / SenseVoice Small / Mock
  - TTS: Edge-TTS 5 个音色 (晓晓/云希/云扬/Jenny/Mock)
  - 端点: `/audio/asr/{models,transcribe}` , `/audio/tts/{voices,synthesize}`
  - Mock: 生成 1 秒静音 WAV (标准 RIFF/WAVE 头)
- **LeaderboardController** 模型对决排行榜:
  - `GET /leaderboard/overall` 综合评分 (按 avg_score 降序)
  - `POST /leaderboard/battle` 发起对决
  - `GET /leaderboard/history` 对决历史
  - model_battle_log 表 (新增)
- **Plugin SDK** (minimax-agent):
  - Plugin 接口 + PluginContext + PluginExecutor
  - 插件注册表热加载 (动态发现)

### Build
- 12 个后端模块 BUILD SUCCESS
- 前端 BUILD SUCCESS (含 ASR/TTS/ImageGen UI)

---

## [V4.0] - 2026-06-17 — 真实 AI 对接 + 多模型对决 + 视觉对决 + PlayGround

### Added
- **RealAiTestController** (minimax-model, 3 端点):
  - `GET  /api/v1/test/ping`       健康检查
  - `POST /api/v1/test/single`     单次非流式 (真 OpenAI 协议)
  - `POST /api/v1/test/battle`     多模型并发对决 (8 线程池, 120s 超时)
- **真实 AI 对接**:
  - siliconflow / dashscope / deepseek 3 个新 provider
  - 10 个新模型: Qwen2-VL / InternVL / GLM-4V / Qwen-Max / DeepSeek V3 / R1 等
  - SecurityConfig 开放 `/api/v1/test/**` 和 `/openai/**`
- **PlayGround** 前端页面 (`/playground`):
  - 单次对话 / 流式对话 / 对决 3 种模式
  - 模型选择 + 参数调节 (temperature / top_p / max_tokens)
  - 视觉对决: 图片上传 + 双模型并发理解
- **model_battle_log** SQL 表 (对决日志)

### Build
- 12 个后端模块 BUILD SUCCESS
- 前端 BUILD SUCCESS (含 playground 页面)
- 真实 API key 验证 (需要 SILICONFLOW_API_KEY / DASHSCOPE_API_KEY / DEEPSEEK_API_KEY)


## [V5.9] - 2026-06-21 — Dashboard 真实图表 + 告警规则 CRUD + WebSocket 精确分流

### Added
- **Dashboard 真实折线图** (admin 模块):
  - 新增 `countByDay(since, action)` mapper 方法 (MySQL DATE GROUP BY)
  - 新增 `GET /admin/audit/by-day?days=7&action=user_op` 端点
  - Dashboard.vue 折线图从 mock `[12, 28, 18, ...]` 改为接 3 条 API 真实数据:
    - 全部操作 / 用户类 / 工具调用 (各 fetch 一次 by-day)
- **告警规则 CRUD UI** (monitor 模块, V5.9.2):
  - 后端 4 个新端点: `GET/POST/PUT/DELETE /monitor/alerts/rules` + `GET /monitor/alerts/rules/all`
  - AlertEngine 新增 allRules/createRule/updateRule/deleteRule 方法
  - 前端 monitor/Index.vue 新增告警规则管理卡片 + 编辑弹窗 (含 9 个表单字段: 名称/服务/指标/运算符/阈值/级别/冷却/通知渠道/启用)
  - 13 个服务选项下拉 (gateway + 12 业务模块)
  - 支持新建/编辑/删除/刷新
- **WebSocket 精确分流** (V5.9.3 nginx):
  - 解决: `@ServerEndpoint` (Jakarta WS) vs WebFlux Gateway 协议不兼容
  - nginx location 拆分:
    - `location = /ws/notifications` → 直连 auth:8081 (Jakarta WS 绕过 gateway)
    - `location /ws/` 和 `location /ws` → gateway :8080 → lb:ws://minimax-ws (Spring WebSocketHandler)

### Changed
- Dashboard.vue `loadAll()` 增加 `loadTrend()` 并行调用, 新增 `dailyOps/dailyUserOps/dailyToolOps` 3 个 ref
- monitor.js 新增 5 个 API 函数 (getMonitorAlertRules / create / update / delete / summary)
- frontend api/admin.js 新增 `getAuditByDay(days, action)` 函数

### Files
- backend: 5 files (AdminController / AuditService / AdminAuditLogMapper + xml / AlertEngine / MonitorController)
- frontend: 3 files (api/admin.js, api/monitor.js, admin/Dashboard.vue, monitor/Index.vue)
- scripts: 1 file (nginx-minimax-3000.conf)


## [V5.10] - 2026-06-21 — Prometheus 全链路监控 + BaseController 落地

### Added
- **HTTP 自动指标** (common/MetricsFilter):
  - `minimax.http.requests.total` Counter (method, uri, status)
  - `minimax.http.requests.duration` Timer (含 p50/p95/p99 histogram)
  - `minimax.http.4xx.errors.total` / `minimax.http.5xx.errors.total` Counter
  - URI 归一化 (防高基数标签): `/api/v1/user/123` → `/api/v1/user/{id}`
- **Prometheus 端点统一启用** (application-common.yml):
  - management.endpoints.web.exposure.include: health,info,metrics,prometheus
  - management.metrics.tags.application = ${spring.application.name} (Grafana 按服务分组)
  - percentiles-histogram 启用 http.server.requests + minimax.llm.latency
- **依赖**: spring-boot-starter-actuator + micrometer-registry-prometheus (common pom)
- **跨服务 Prometheus 转发** (monitor 模块):
  - ServiceEndpoints: 12 微服务 + gateway URL 解析
  - `GET /monitor/forward-prometheus?service=minimax-auth` 透传 prometheus 文本
- **前端 Metrics Dashboard** (`/admin/metrics`):
  - 服务选择下拉 (12 微服务 + gateway)
  - 概览卡片 (总请求 / 4xx / 5xx / 平均延迟)
  - Top 10 高频 URI / Top 10 慢 URI / 状态码饼图 / 耗时 Top 5 柱图
  - 10s 自动刷新 + 原始 Prometheus 文本折叠
- **V5.10 BaseController 落地演示**:
  - ProviderController (minimax-model): 5 个标准 CRUD + 1 个 `/test` 业务专属端点
  - 模板: 直接使用 mapper + Result + Swagger 注解, 风格与 BaseController 一致

### Docs
- `docs/METRICS-GUIDE.md` (5.7KB): Prometheus + Grafana 接入指南, PromQL 示例

### Files (10)
- backend: 6 files (common/MetricsFilter + common/pom + common/yml + monitor/ServiceEndpoints + monitor/Controller + model/ProviderController)
- frontend: 2 files (views/admin/Metrics.vue + router/index.js)
- docs: 1 file (METRICS-GUIDE.md)
- config: 1 file (CHANGELOG.md)

## [V5.11] - 2026-06-21 — API 文档聚合中心 + knife4j 统一配置下沉重构

### Added
- **API 文档聚合中心** (`/api-docs`):
  - 新增 `static/api-docs.html` (7KB) — 13 服务 tab 切换 + iframe 嵌入
  - 新增 `ApiDocsController` (`/monitor/api-docs` → 静态资源)
  - 入口: `/api-docs` / `/doc.html` → 302 重定向到 monitor 聚合页
  - 单服务直访: `/api/v1/{module}/doc.html` (走 gateway lb:// 转发)
- **knife4j 统一配置下沉重构**:
  - 10 个业务 yml 的 knife4j/springdoc 重复块全部清理
  - 配置统一在 `application-common.yml` (V5.11 顺手修潜在 DuplicateKeyException)
  - knife4j 中文 UI + 实体类列表 + 多版本切换

### Changed
- `scripts/nginx-minimax-3000.conf` 加 3 个 location (302 重定向)
- `application-common.yml` 加 springdoc + knife4j 块 (13 模块自动继承)

### Docs
- `docs/API-DOCS-GUIDE.md` (3.6KB): 入口/架构/文件清单/PromQL

### Files (16)
- backend: 5 new (ApiDocsController + static/api-docs.html + common yml 增量 + 10 yml 去重)
- frontend: 0
- docs: 1 new (API-DOCS-GUIDE.md)
- config: 1 modified (nginx-minimax-3000.conf)

## [V5.12] - 2026-06-21 — 部署脚本集成 Nacos + Gateway + E2E 健康检查

### Added (deploy-linux.sh)
- **install_nacos**: 下载 Nacos 2.3.2, 配 MySQL 持久化, standalone 启动脚本
- **install_redis**: apt 装 Redis, 配密码 + bind 127.0.0.1
- **build_backend**: 拷贝 gateway.jar (Spring Cloud Gateway)
- **generate_systemd**: 加 minimax-nacos.service + minimax-gateway.service
- **start_services**: 启动顺序重写 (nacos→gateway→微服务→nginx), sleep 25+12 等依赖
- **stop_services**: 倒序停 (微服务→gateway→nacos)
- **show_status**: 加 nacos/gateway/redis 行
- **show_logs**: 加 nacos/redis 特殊路径
- **e2e_health_check (新子命令)**: 一键 HTTP 检查 13 服务 + nacos + redis + nginx
- **install_all**: 加 install_redis + install_nacos, 改 3000 端口 + 新提示

### Changed
- nginx listen 80 → 3000 (V5.12 统一入口)
- 子命令提示加 e2e
- 用法文档更新 (Nacos 启动等待 25s, gateway 12s)

### Docs
- `docs/DEPLOY-GUIDE.md` (6.6KB): 架构图/端口分配/E2E 示例/systemd 清单/升级路径/故障排查

### Files (3)
- scripts: 1 modified (deploy-linux.sh, 581→834 行)
- docs: 1 new (DEPLOY-GUIDE.md)
- config: 1 modified (CHANGELOG.md)

## [V5.13] - 2026-06-21 — 架构总览文档完善 (README + ARCHITECTURE)

### Added
- **README.md 全面升级** (V5.12 反映):
  - 标题行: 13 个微服务 / Spring Cloud Gateway / Nacos / Prometheus / TraceId
  - 架构图重画: 浏览器 → nginx :3000 → gateway :8080 → Nacos → 12 微服务 → MariaDB/Redis
  - 新增"V5 架构升级"章节 (8 个版本 + commit hash)
  - 启动方式更新: 推荐 deploy-linux.sh + e2e 健康检查
  - 总结表加 V5 关键创新 7 条
- **docs/ARCHITECTURE.md** (11KB, 新文档):
  - 一句话定位 + 顶层视图
  - 分层架构: Client/Edge/Gateway/Microservice/Infrastructure
  - 13 微服务职责矩阵 (端口/表/职责/依赖)
  - 3 大数据流: HTTP / SSE / Agent 工具循环
  - 9 个关键技术决策 (V5.5-V5.12)
  - 可观测性分层: HTTP/业务/JVM + 5 条告警规则
  - 部署架构 + 16 个 systemd 清单
  - 4 条扩展路径 (新微服务/告警/Provider/水平扩展)
  - 故障转移矩阵
  - 技术选型对照表
  - 演进路线 V5 → V6

### Files (3)
- docs: 1 new (ARCHITECTURE.md, 11KB)
- root: 1 modified (README.md)
- config: 1 modified (CHANGELOG.md)

## [V5.14] - 2026-06-21 — OpenTelemetry 分布式追踪 (从单点 trace 到全链路 span)

### Added
- **OpenTelemetry 接入** (root pom + common pom):
  - opentelemetry-bom:1.36.0 + opentelemetry-instrumentation-bom:2.2.0
  - opentelemetry-spring-boot-starter (auto-config 模式)
  - opentelemetry-exporter-otlp (OTLP/HTTP 协议)
- **W3C traceparent 注入** (gateway TraceFilter V5.14 升级):
  - 32+16 hex 格式, 复用 V5.8 的 16 位 traceId 填充
  - 13 个微服务自动识别, 创建 child span
- **零代码 instrumentation** (auto-detect):
  - HTTP Server/Client, JDBC, Kafka, RabbitMQ, gRPC
  - Spring WebFlux, Spring Cloud Gateway, @Scheduled
- **OTLP 配置** (application-common.yml):
  - 默认 endpoint: http://localhost:4318
  - W3C tracecontext + baggage 传播器
  - 采样率 1.0 (dev), 生产建议 0.1
- **前端 Traces Dashboard** (`/admin/traces`):
  - 服务名 + Trace ID 搜索
  - 概览 (Traces / Spans / 平均耗时 / 错误率)
  - Span 树展开 (层级 + service + 耗时)
  - 一键跳转 Jaeger UI
  - 10s 自动刷新
- **docs/TRACES-GUIDE.md** (6.5KB): 部署/配置/auto-instrumentation/自定义 span

### Files (7)
- backend: 4 (root pom + common pom + common yml + gateway TraceFilter)
- frontend: 2 (admin/Traces.vue + router)
- docs: 1 new (TRACES-GUIDE.md)

## [V5.15] - 2026-06-21 — 完整 E2E 自动化测试 (健康 + JWT + TraceId + Prometheus)

### Added
- **scripts/e2e-full-test.sh** (10KB, 新):
  - 7 个 Phase 自动化测试:
    1. 基础设施健康 (nginx/nacos/redis/mariadb)
    2. 13 服务健康检查 (gateway + 12 微服务)
    3. JWT 鉴权全链路 (401 → 登录 → 200)
    4. 跨服务调用 (admin/monitor/chat/model/rag)
    5. TraceId 透传验证 (V5.14 OTel W3C traceparent)
    6. Prometheus 指标验证 (V5.10)
    7. 错误码一致性
  - 支持 `--quick` (只跑 Phase 1+2) 和 `--full` (跑全部)
  - 支持自定义 BASE/GATEWAY/NACOS/账号 环境变量
  - 35+ 测试用例, 彩色输出 + 汇总表 + 退出码
- **deploy-linux.sh 加 e2e-full 子命令**:
  - `e2e` (V5.12 旧): 快速健康检查 (inline)
  - `e2e-full` (V5.15 新): 调用 e2e-full-test.sh 跑完整测试

### Docs
- **docs/E2E-GUIDE.md** (5KB): 用法/CI 集成/故障排查

### Files (4)
- scripts: 2 (e2e-full-test.sh + deploy-linux.sh 加子命令)
- docs: 1 new (E2E-GUIDE.md)
- config: 1 modified (CHANGELOG.md)

## [V5.16] - 2026-06-22 — Agent 增强 (流式 SSE + Plan 模式 + 记忆集成)

### Added (后端)
- **Agent 流式执行 (SSE)**: `runStream()` 用 SseEmitter 实时推送事件
  - 事件类型: start / tools / step-start / thought / tool-call / observation / final / done / error
  - 2 分钟超时, 异步执行不阻塞 Tomcat
  - 端点: `POST /agent/run-stream` (V5.16)
- **Plan 模式**: `plan()` LLM 拆解目标为 3-7 步骤, 用户确认后 `runPlan()` 串行执行
  - 端点: `POST /agent/plan` / `POST /agent/run-plan`
- **RAG 长期记忆集成**: `runWithMemory()` 调 RAG /retrieve 召回相关记忆, 拼入 system prompt
  - 端点: `POST /agent/run-with-memory`
- **ReAct 循环保留**: 6 个 event 类型 (thought/tool-call/observation/final/done/error)

### Added (前端)
- **`/agent/stream` 页面** (V5.16, 11KB):
  - 三模式: 流式执行 / Plan 模式 / 带记忆
  - SSE 事件流可视化 (timeline 渲染)
  - Plan 步骤可编辑 (textarea inline edit)
  - 工具列表 + 原始 JSON 调试面板
  - fetch + ReadableStream 替代 EventSource (支持 POST + JWT)

### Files (4)
- backend: 2 (AgentService + AgentController)
- frontend: 2 (Stream.vue + router)
- config: 1 modified (CHANGELOG.md)
