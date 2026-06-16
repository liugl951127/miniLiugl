# 🧩 MiniMax Platform — 模块说明 + 关联性

> 12 后端模块 + 15+ 前端页面 + 完整关联图

---

## 📊 1. 模块总览

| # | 模块 | 端口 | 类型 | 主要职责 | 端点数 |
|---|------|------|------|----------|--------|
| 1 | **auth** | 8081 | 胖 | 登录/注册/JWT/SUPER/租户 | 22 |
| 2 | **chat** | 8082 | 胖 | 会话/消息/流式输出 | 11 |
| 3 | **model** | 8083 | 胖 | 6 大模型/调用网关/OpenAI 兼容 | 18 |
| 4 | **memory** | 8084 | 胖 | 短/长/偏好/摘要 4 维记忆 | 14 |
| 5 | **rag** | 8085 | 胖 | 3 文档解析/3 级降级/引用 | 13 |
| 6 | **function** | 8086 | 瘦 | 4 工具/LLM 工具循环 | 9 |
| 7 | **admin** | 8087 | 瘦 | 跨服务聚合 HTTP 代理 | 12 |
| 8 | **multimodal** | 8088 | 瘦 | 图片理解 (Vision) | 6 |
| 9 | **monitor** | 8089 | 瘦 | Prometheus 指标/告警 | 11 |
| 10 | **agent** | 8090 | 瘦 | ReAct/KG/协作/插件 (4 合 1) | 19 |
| 11 | **common** | - | 共享 | 工具类/安全/JWT/限流/缓存 | 0 |
| 12 | **gateway** | 8080 | (未启) | 统一网关 (未来) | 0 |
| | | | | **合计** | **135+** |

---

## 📦 2. 后端模块详解

### 2.1 minimax-common (共享库)

**职责**: 所有其他模块复用的工具, 不启动服务

**关键类**:
```
com.minimax.common
├── jwt
│   ├── JwtTokenProvider      # JWT 签发/解析
│   ├── JwtAuthenticationFilter  # 鉴权拦截器
│   └── JwtProperties         # 配置 (密钥/过期)
├── security
│   ├── SecurityConfig        # Spring Security 6 配置
│   ├── AuthenticatedUser     # 已登录用户信息
│   ├── SuperAdminGuard       # 超级管理员检查
│   └── UserContext           # ThreadLocal
├── rate
│   ├── RateLimitService      # Bucket4j
│   └── RateLimitFilter       # 限流拦截器
├── cache
│   ├── CacheService          # Caffeine + Redis 双层
│   └── CacheNames
├── tenant
│   ├── TenantContext         # ThreadLocal tenantId
│   ├── TenantInterceptor     # 拦截器
│   ├── TenantResolver        # 用户→租户
│   └── TenantInfo
├── result
│   ├── Result<T>             # 统一响应 {code, msg, data}
│   ├── ResultCode            # 业务码
│   └── PageResult
├── exception
│   ├── BizException          # 业务异常
│   ├── GlobalExceptionHandler # @ControllerAdvice
│   └── ErrorCode
├── util
│   ├── HttpClientUtil        # Java 11 HttpClient 封装
│   ├── JsonUtil              # Jackson 包装
│   ├── SnowflakeIdWorker     # 雪花 ID
│   └── DateUtil
└── mybatis
    ├── MybatisPlusConfig     # 分页 + 乐观锁
    ├── MetaObjectHandlerImpl # 自动填充 createTime
    └── TenantLineInnerInterceptor  # 多租户拦截
```

**被谁依赖**:
- 10 个业务模块 (auth/chat/model/memory/rag/function/admin/multimodal/monitor/agent)

**关键设计**:
- 不放业务逻辑, 只放通用
- starter 模式 (其他模块引依赖即生效)
- 多租户拦截器自动注入所有 Mapper

---

### 2.2 minimax-auth (身份认证, 8081) — 胖模块

**职责**: 登录/注册/JWT/SUPER/租户管理

**关键类**:
```
com.minimax.auth
├── controller
│   ├── AuthController        # 登录/登出/注册
│   ├── SuperAdminController  # 👑 5 端点 (adminLiugl 专属)
│   ├── TenantController      # 🏢 8 端点 (V3.1)
│   └── UserController        # 普通用户管理
├── service
│   ├── AuthService
│   ├── UserService
│   ├── RoleService
│   ├── PermissionService
│   ├── TenantService         # V3.1
│   └── SuperAdminService
├── entity
│   ├── SysUser
│   ├── SysRole
│   ├── SysPermission
│   ├── SysTenant             # V3.1
│   ├── SysUserRole
│   └── SysRolePermission
├── mapper                    # 10 个
├── config
│   ├── SecurityConfig        # permitAll 公开 / JWT 过滤
│   ├── WebMvcConfig          # 注册 TenantInterceptor
│   ├── AdminDataInitializer  # 创建 adminLiugl + roles
│   └── OpenAIGatewayConfig
├── guard
│   └── SuperAdminGuard       # @Component 公共类
└── util
```

**关键表**:
```sql
sys_user (id, username, password, email, real_name, status, tenant_id, super_admin)
sys_role (id, code, name)  -- ROLE_USER / ROLE_ADMIN / ROLE_SUPER_ADMIN
sys_permission (id, code, name)
sys_user_role (user_id, role_id)
sys_role_permission (role_id, permission_id)
sys_tenant (id, code, name, plan, max_users, max_qps, monthly_quota)
```

**对外 HTTP**:
```
POST /auth/login                          → 公开
POST /auth/register                       → 公开
POST /auth/refresh                        → 公开
GET  /auth/me                             → 已登录
POST /auth/logout                         → 已登录
GET  /auth/super/me                       → SUPER_ADMIN (5 端点)
GET  /auth/super/users                    → SUPER_ADMIN
POST /auth/super/users                    → SUPER_ADMIN
PUT  /auth/super/users/{id}               → SUPER_ADMIN
POST /auth/super/users/{id}/reset-password → SUPER_ADMIN
GET  /auth/tenants                        → 已登录 (自己租户)
POST /auth/tenants                        → SUPER_ADMIN
PUT  /auth/tenants/{id}                   → SUPER_ADMIN
DELETE /auth/tenants/{id}                 → SUPER_ADMIN
POST /auth/tenants/{id}/suspend           → SUPER_ADMIN
GET  /auth/tenants/{id}/quota             → 已登录
```

**调用关系**:
- **入站**: 前端 `/login` 调
- **入站**: admin 8087 跨服务调
- **出站**: 不调其他服务 (独立)

---

### 2.3 minimax-chat (对话, 8082) — 胖模块

**职责**: 会话/消息/流式输出

**关键类**:
```
com.minimax.chat
├── controller
│   ├── ChatController        # 会话/消息
│   ├── SessionController     # 会话列表/CRUD
│   └── StreamController      # SSE 流式
├── service
│   ├── ChatService           # 主服务
│   ├── SessionService        # 会话管理
│   ├── MessageService        # 消息历史
│   └── PromptBuilder         # 构造 prompt
├── entity
│   ├── ChatSession (id, user_id, title, model_code, kb_id, agent_id)
│   └── ChatMessage (id, session_id, role, content, reasoning, tool_calls, tokens)
├── mapper
└── sse
    ├── SseEvent              # 事件对象
    └── SseEmitterFactory
```

**关键表**:
```sql
chat_session (id, user_id, title, model_code, kb_id, agent_id, status, created_at)
chat_message (id, session_id, role, content, content_md, reasoning, tool_calls, tokens, latency_ms)
```

**SSE 事件**:
```
event: message
data: {"id":1, "content":"Hello", "delta":true}

event: tool
data: {"id":2, "name":"calculator", "args":{...}}

event: error
data: {"code":401, "msg":"..."}

event: done
data: {}
```

**调用关系**:
- **入站**: 前端 `/chat`
- **出站**: 
  - → `model 8083` (调 LLM)
  - → `memory 8084` (拉长期记忆)
  - → `rag 8085` (拉上下文)
  - → `function 8086` (调工具, ReAct)
  - → `monitor 8089` (上报 metrics)

**强关联**:
- 几乎所有模块都跟它有关 (聚合作用)

---

### 2.4 minimax-model (模型, 8083) — 胖模块

**职责**: 6 大模型 provider / OpenAI 兼容网关

**关键类**:
```
com.minimax.model
├── controller
│   ├── ModelController        # 模型列表/详情
│   ├── ProviderController     # Provider CRUD
│   ├── OpenAIGatewayController # OpenAI 兼容 API (V3.3)
│   └── ModelConfigController
├── service
│   ├── ModelService
│   ├── ProviderService
│   ├── ModelConfigService
│   ├── ChatCompletionService  # 主调用
│   └── OpenAICompatService    # OpenAI 协议转换
├── adapter
│   ├── ModelAdapter           # interface
│   ├── OpenAiCompatibleAdapter  # 实现 6 个 provider
│   ├── MockAdapter            # 沙箱模式
│   └── AdapterFactory
├── entity
│   ├── ModelProvider (id, name, type, base_url, api_key)
│   ├── ModelConfig (id, code, name, provider_id, model_code, ...)
│   └── ModelCallLog (id, user_id, model_id, prompt_tokens, completion_tokens, latency)
└── dto
    ├── ChatRequest
    ├── ChatResponse
    └── OpenAIRequest/Response
```

**Provider**:
| Code | Type | BaseURL |
|------|------|---------|
| openai | openai-compat | https://api.openai.com/v1 |
| minimax | openai-compat | https://api.minimaxi.com/v1 |
| ollama | openai-compat | http://localhost:11434/v1 |
| qwen | openai-compat | https://dashscope.aliyuncs.com/compatible-mode/v1 |
| deepseek | openai-compat | https://api.deepseek.com/v1 |
| zhipu | openai-compat | https://open.bigmodel.cn/api/paas/v4 |

**调用关系**:
- **入站**: chat 8082 (每次对话)
- **入站**: agent 8090 (ReAct LLM call)
- **入站**: memory 8084 (摘要)
- **入站**: admin 8087 (test 端点)
- **出站**: 6 个 provider (OpenAI 协议)

**OpenAI 兼容端点 (V3.3)**:
```
GET  /api/v1/openai/models                 # 列出模型
POST /api/v1/openai/chat/completions       # 聊天 (含 stream)
```

---

### 2.5 minimax-memory (记忆, 8084) — 胖模块

**职责**: 4 维记忆 (短/长/偏好/摘要)

**关键类**:
```
com.minimax.memory
├── controller
│   ├── MemoryController
│   ├── ShortTermController
│   ├── LongTermController
│   ├── PreferenceController
│   └── SummaryController
├── service
│   ├── MemoryService
│   ├── ShortTermService   # Redis + Caffeine
│   ├── LongTermService    # MySQL BLOB + 余弦
│   ├── PreferenceService  # 用户显式偏好
│   └── SummaryService     # LLM 摘要
├── entity
│   ├── MemoryShortTerm (id, user_id, session_id, role, content, ttl)
│   ├── MemoryLongTerm (id, user_id, content, embedding BLOB, importance)
│   ├── MemoryPreference (id, user_id, key, value)
│   └── MemorySummary (id, user_id, period, summary, source_msg_count)
└── embedding
    ├── EmbeddingService    # 调 model 8083
    └── CosineUtil          # 余弦相似度
```

**关键表**:
```sql
memory_short_term (id, user_id, session_id, role, content, created_at, expires_at)
memory_long_term (id, user_id, content, embedding BLOB(3072), importance, last_accessed)
memory_preference (id, user_id, pref_key, pref_value, weight)
memory_summary (id, user_id, period_start, period_end, summary, source_count)
```

**调用关系**:
- **入站**: chat 8082 (recall)
- **入站**: 前端 /memory
- **出站**: → model 8083 (embedding / 摘要)

**记忆召回**:
```
[短记忆]  L1: Redis (TTL 30min) + Caffeine (60s LRU)
   ↓ (5 轮)
[长记忆]  L2: MySQL BLOB + cosine(768 dim) → top 5
   ↓
[偏好]    L3: 显式 + 隐式 → 注入 prompt
   ↓
[摘要]    L4: LLM 压缩长对话 → 1000 字以内
```

---

### 2.6 minimax-rag (知识库, 8085) — 胖模块

**职责**: 3 文档解析 / 3 级降级检索

**关键类**:
```
com.minimax.rag
├── controller
│   ├── KbController           # 知识库 CRUD
│   ├── DocumentController     # 文档上传/解析
│   ├── ChunkController        # chunk CRUD
│   └── RetrieveController     # 检索
├── service
│   ├── KbService
│   ├── DocumentService
│   ├── ChunkService
│   ├── ParseService           # 文档解析
│   ├── EmbedService           # 调 model 算 embedding
│   ├── RetrieveService        # 3 级降级
│   └── DegradeStrategy        # 降级策略
├── parser
│   ├── DocParser (interface)
│   ├── TxtParser
│   ├── DocxParser (Apache POI)
│   ├── PdfParser (PDFBox)
│   └── ParserFactory
├── chunker
│   ├── Chunker (interface)
│   ├── FixedChunker          # 500 字 / 50 滑动
│   └── SentenceChunker       # 句分割
└── entity
    ├── Kb (id, name, owner_id, embedding_model)
    ├── KbDocument (id, kb_id, filename, sha256, status, parse_msg)
    └── KbChunk (id, doc_id, content, embedding BLOB, position)
```

**3 级降级**:
```
L1: 向量检索 (内存) → top 10, threshold 0.5
    ↓ (< 3 results)
L2: 关键词检索 (MySQL FULLTEXT) → top 10
    ↓ (< 1 result)
L3: 全量扫描 → top 5
```

**调用关系**:
- **入站**: chat 8082 (RAG 开关)
- **入站**: 前端 /knowledge
- **出站**: → model 8083 (embedding)

---

### 2.7 minimax-function (工具, 8086) — 瘦模块

**职责**: 4 内置工具 / LLM 工具循环

**关键类**:
```
com.minimax.function
├── controller
│   ├── FunctionController
│   └── ToolCallController     # 调试用
├── service
│   ├── FunctionService
│   ├── ToolCallService
│   └── ToolRegistry
├── tool
│   ├── Tool (interface)        # 统一接口
│   ├── CalculatorTool          # 四则
│   ├── TimeTool                # 时间
│   ├── HttpTool                # HTTP 调用
│   ├── RandomTool              # 随机数
│   └── WeatherTool (扩展)
└── schema
    ├── ToolSchema              # OpenAI 工具定义
    └── ToolCall
```

**4 内置工具**:
| 名称 | 描述 | 参数 |
|------|------|------|
| calculator | 四则运算 | {expr: string} |
| time | 当前时间 | {tz: string} |
| http_get | HTTP GET | {url: string} |
| random | 随机数 | {min, max} |

**调用关系**:
- **入站**: chat 8082 (ReAct)
- **入站**: agent 8090 (ReAct)
- **出站**: 不调其他服务 (本地工具)

---

### 2.8 minimax-admin (管理后台, 8087) — 瘦模块

**职责**: 跨服务聚合 HTTP 代理

**关键类**:
```
com.minimax.admin
├── controller
│   ├── DashboardController   # 仪表盘 KPI
│   ├── UserManageController  # 跨服务用户管理
│   ├── ModelAdminController  # 模型配额
│   ├── AuditController       # 审计日志
│   ├── HealthController      # 服务健康
│   └── StatsController       # 业务统计
├── service
│   ├── DashboardService      # 聚合多服务
│   ├── HealthCheckService    # 轮询 10 服务
│   ├── UserManageService     # 代理 auth 8081
│   ├── ModelAdminService     # 代理 model 8083
│   └── AuditService
├── client                    # OpenFeign/HTTP 客户端
│   ├── AuthClient
│   ├── ModelClient
│   ├── ChatClient
│   └── ... 9 个 client
└── config
    ├── AdminSecurityConfig
    └── ServiceUrlConfig      # 服务地址配置
```

**调用关系**:
- **入站**: 前端 /admin
- **出站**: → 9 个服务 (聚合)
- 自身**不写业务数据**, 只聚合

**仪表盘数据流**:
```
[前端] → admin/dashboard
        ↓
        ├─ → auth: 用户数
        ├─ → chat: 调用数
        ├─ → model: token 用量
        ├─ → monitor: 健康状态
        └─ → memory: 长期记忆数
        ↓
[前端] ECharts 渲染
```

---

### 2.9 minimax-multimodal (多模态, 8088) — 瘦模块

**职责**: 图片理解 (Vision)

**关键类**:
```
com.minimax.multimodal
├── controller
│   ├── ImageController       # 上传/分析
│   └── VisionController
├── service
│   ├── ImageService
│   ├── VisionService
│   └── OcrService
├── vision
│   ├── VisionAdapter
│   └── VisionProvider        # VL-01 / GPT-4V
└── entity
    ├── ImageAsset (id, user_id, url, sha256, size, mime)
    └── VisionResult (asset_id, content, tags)
```

**调用关系**:
- **入站**: chat 8082 (拖拽图片)
- **出站**: → model 8083 (VL-01 视觉模型)

---

### 2.10 minimax-monitor (监控告警, 8089) — 瘦模块

**职责**: Prometheus 指标 / 5 告警规则

**关键类**:
```
com.minimax.monitor
├── controller
│   ├── MetricsController     # /actuator/prometheus
│   ├── HealthController      # 5 维健康
│   ├── AlertController       # 告警列表/确认
│   └── SnapshotController
├── service
│   ├── MetricsService
│   ├── AlertRuleService
│   ├── AlertEventService
│   ├── AlertEngine           # 后台线程 60s 扫描
│   ├── HealthCheckService    # 5 维健康
│   └── SnapshotService
├── metrics                   # Micrometer 指标
│   ├── BusinessMetrics       # 5 类
│   ├── GatewayMetrics        # 4 类
│   └── TechMetrics           # 2 类
├── rule
│   ├── AlertRule (interface)
│   ├── HighErrorRateRule
│   ├── SlowResponseRule
│   ├── ServiceDownRule
│   ├── DiskFullRule
│   ├── MemoryHighRule
│   └── RuleRegistry
└── entity
    ├── AlertRule (id, code, name, condition, severity, cooldown_min)
    ├── AlertEvent (id, rule_id, fired_at, resolved_at, status)
    └── HealthSnapshot (id, cpu, mem, disk, net, proc, taken_at)
```

**5 类业务指标**:
```
minimax_chat_total{model=...}              # chat 调用
minimax_tokens_total{model=...}            # token 用量
minimax_active_users                       # 活跃用户
minimax_error_rate{endpoint=...}           # 错误率
minimax_latency_p99{endpoint=...}          # P99 延迟
```

**5 默认告警**:
| Code | 条件 | 严重度 | 冷却 |
|------|------|--------|------|
| HighErrorRate | error_rate > 5% 5min | HIGH | 15min |
| SlowResponse | p99 > 3s 5min | MEDIUM | 15min |
| ServiceDown | service down 1min | CRITICAL | 5min |
| DiskFull | disk > 90% | MEDIUM | 30min |
| MemoryHigh | heap > 85% 5min | HIGH | 15min |

**调用关系**:
- **入站**: 其他 9 服务 (上报 metrics HTTP)
- **入站**: 前端 /admin/dashboard
- **出站**: 不调其他服务

---

### 2.11 minimax-agent (智能体, 8090) — 瘦模块

**职责**: 4 大新功能 (V2)

**关键类**:
```
com.minimax.agent
├── controller
│   ├── AgentController        # ReAct 自主任务
│   ├── KgController           # 知识图谱
│   ├── CollabController       # 协作
│   ├── CollabWsHandler        # WebSocket
│   └── PluginController       # 插件
├── service
│   ├── AgentService           # ReAct 主循环
│   ├── KnowledgeGraphService  # 实体/关系/路径
│   ├── CollabService          # 消息广播
│   ├── CollabSessionManager   # 在线用户
│   └── PluginService          # 插件注册
├── kg
│   ├── Entity (id, name, type, alias, importance, desc)
│   ├── Relation (from, to, type, weight)
│   ├── KgNode
│   └── BfsPathFinder
├── plugin
│   ├── Plugin (interface)
│   ├── PluginType {CLASS, URL, JS, WASM}
│   ├── ClassPlugin
│   ├── UrlPlugin
│   ├── JsPlugin
│   └── WasmPlugin (预留)
└── collab
    ├── CollabMessage
    ├── TypingEvent
    ├── CursorEvent
    └── EditEvent
```

**ReAct 主循环**:
```
[Agent 8090]
    ↓
[model 8083] → LLM (with tool schema)
    ↓
[function 8086] → 执行 tool_call
    ↓
[model 8083] → LLM + tool result
    ↓
循环 8 轮
    ↓
[用户]  ← final answer
```

**知识图谱算法**:
- 1 跳邻居: 直接查 relation 表
- 2 跳邻居: join 两次
- 最短路径: BFS (Queue)
- 重要性: 关系权重 + 实体重要性

**协作 WebSocket**:
```
WS: /ws/collab/{sessionId}?userId=N
事件类型:
  - JOIN
  - LEAVE
  - MESSAGE
  - TYPING
  - CURSOR
  - EDIT
```

**插件 4 类型**:
| Type | 实现 | 用例 |
|------|------|------|
| class | Java 类 | 自定义业务逻辑 |
| url | HTTP 端点 | 调外部服务 |
| js | 沙箱 JS | 前端组件 |
| wasm | 二进制 (预留) | 高性能计算 |

**4 系统插件**:
| 名称 | 类型 | 功能 |
|------|------|------|
| weather-widget | url | 天气小组件 |
| markdown-export | class | 会话导出 |
| code-formatter | class | 代码格式化 |
| translation | url | 翻译 API |

**调用关系**:
- **入站**: 前端 /agent /kg /collab /plugins
- **出站**: → model 8083 (LLM)
- **出站**: → function 8086 (ReAct 调工具)

---

## 📱 3. 前端模块详解

### 3.1 目录结构

```
frontend/src/
├── main.ts                   # 入口
├── App.vue
├── router/
│   └── index.ts             # 路由 + 守卫
├── store/
│   ├── user.ts              # Pinia (用户/token)
│   └── index.ts
├── api/
│   ├── auth.ts              # 12 个后端的 API 客户端
│   ├── chat.ts
│   ├── model.ts
│   ├── memory.ts
│   ├── rag.ts
│   ├── function.ts
│   ├── admin.ts
│   ├── multimodal.ts
│   ├── monitor.ts
│   ├── agent.ts
│   ├── openai.ts            # V3.3
│   └── mobile.ts
├── views/
│   ├── Login.vue
│   ├── Dashboard.vue
│   ├── About.vue
│   ├── chat/Index.vue
│   ├── knowledge/Index.vue  # 知识库
│   ├── memory/Index.vue     # 记忆
│   ├── agent/Index.vue
│   ├── kg/Index.vue
│   ├── collab/Index.vue
│   ├── plugins/Index.vue
│   ├── admin/
│   │   ├── Index.vue
│   │   └── Dashboard.vue    # 监控面板
│   ├── super/Index.vue      # 👑 超级管理 (V3.0)
│   ├── auth/Index.vue
│   └── mobile/              # 📱 V3.2
│       ├── Chat.vue         # /m/chat
│       ├── Agent.vue        # /m/agent
│       ├── Kg.vue           # /m/kg
│       ├── Plugins.vue      # /m/plugins
│       └── Me.vue           # /m/me
├── components/              # 共用组件
│   ├── ChatBubble.vue
│   ├── CodeBlock.vue
│   ├── Markdown.vue
│   ├── Sidebar.vue
│   └── TopBar.vue
├── utils/
│   ├── request.ts           # axios 封装 (带 JWT)
│   ├── stream.ts            # SSE 客户端
│   ├── ws.ts                # WebSocket 客户端
│   └── ua.ts                # UA 检测 (移动端跳 /m/chat)
└── styles/
    ├── element.scss
    └── vant.scss
```

### 3.2 路由表

```typescript
const routes = [
  { path: '/login', component: Login },
  
  // 桌面
  { path: '/', redirect: '/chat' },
  { path: '/chat', component: ChatIndex, meta: { requiresAuth: true } },
  { path: '/knowledge', component: KnowledgeIndex },
  { path: '/memory', component: MemoryIndex },
  { path: '/agent', component: AgentIndex },
  { path: '/kg', component: KgIndex },
  { path: '/collab', component: CollabIndex },
  { path: '/plugins', component: PluginsIndex },
  { path: '/admin', component: AdminIndex, meta: { role: 'ADMIN' } },
  { path: '/admin/dashboard', component: AdminDashboard, meta: { role: 'ADMIN' } },
  { path: '/super', component: SuperIndex, meta: { role: 'SUPER_ADMIN' } },
  { path: '/about', component: About },
  
  // 移动端 (V3.2)
  { path: '/m', redirect: '/m/chat' },
  { path: '/m/chat', component: MobileChat },
  { path: '/m/agent', component: MobileAgent },
  { path: '/m/kg', component: MobileKg },
  { path: '/m/plugins', component: MobilePlugins },
  { path: '/m/me', component: MobileMe },
]
```

### 3.3 路由守卫

```typescript
router.beforeEach((to, from, next) => {
  const user = useUserStore();
  
  if (to.meta.requiresAuth && !user.token) {
    return next('/login');
  }
  
  // 角色检查
  if (to.meta.role === 'SUPER_ADMIN' && !user.isSuperAdmin) {
    return next('/chat');  // 非超管跳首页
  }
  
  // 移动端 UA 跳 /m/chat
  if (to.path === '/login' && isMobile()) {
    return next('/m/chat');
  }
  
  next();
});
```

---

## 🔗 4. 模块关联图

### 4.1 高层视图

```
                        ┌──────────────┐
                        │   Frontend   │
                        │   (Vue 3)    │
                        └──────┬───────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
   ┌────▼────┐           ┌─────▼─────┐          ┌─────▼─────┐
   │  auth   │◀─────────▶│  chat 8082│◀─────────│  memory   │
   │  8081   │           │           │          │  8084     │
   │         │           │  (中心)   │          │           │
   │ JWT 登录│           │ 会话/消息 │          │  4 维记忆 │
   └────┬────┘           └─────┬─────┘          └─────┬─────┘
        │                      │                      │
        │                      │                      │
   ┌────▼────┐           ┌─────▼─────┐          ┌─────▼─────┐
   │  model  │◀──────────│   rag     │          │  agent    │
   │  8083   │           │  8085     │          │  8090     │
   │ 6 模型  │           │ 3级降级   │          │ ReAct/KG  │
   │ OpenAI  │           └─────┬─────┘          │ 协作/插件 │
   └────┬────┘                 │                └─────┬─────┘
        │                      │                      │
        │                ┌─────▼─────┐                │
        │                │ function  │◀───────────────┘
        │                │  8086     │
        │                │  4 工具   │
        │                └───────────┘
        │
   ┌────▼────┐
   │ monitor │◀────── 9 服务均上报 metrics
   │  8089   │
   └─────────┘
```

### 4.2 数据流向表

| From | To | 协议 | 用途 |
|------|----|------|------|
| Frontend | auth | HTTP/POST | 登录 |
| Frontend | chat | HTTP+SSE | 聊天流式 |
| Frontend | model | HTTP | 模型列表 |
| Frontend | memory | HTTP | 记忆管理 |
| Frontend | rag | HTTP | 知识库 |
| Frontend | agent | HTTP | 自主任务 |
| Frontend | agent | WebSocket | 协作 |
| Frontend | admin | HTTP | 管理 |
| Frontend | monitor | HTTP/Prometheus | 指标 |
| OpenAI-SDK | model (V3.3) | HTTP/SSE | OpenAI 兼容 |
| chat | model | HTTP | LLM 调用 |
| chat | memory | HTTP | 记忆召回 |
| chat | rag | HTTP | RAG |
| chat | function | HTTP | 工具调用 |
| agent | model | HTTP | LLM (ReAct) |
| agent | function | HTTP | 工具 |
| agent | agent | WebSocket | 协作 (内部) |
| memory | model | HTTP | Embedding |
| rag | model | HTTP | Embedding |
| multimodal | model | HTTP | VL-01 |
| admin | 9 服务 | HTTP | 聚合 |
| monitor | 9 服务 | HTTP (被动) | 健康检查 |
| **9 服务** | monitor | HTTP (主动) | 上报 metrics |

### 4.3 强耦合 vs 弱耦合

| 关系 | 强/弱 | 影响 |
|------|------|------|
| chat ↔ model | **强** | chat 每次都调 model |
| chat ↔ memory | 中 | 每次对话拉记忆 |
| chat ↔ rag | 弱 | 仅 RAG 开关开启 |
| chat ↔ function | 弱 | 仅工具调用 |
| agent ↔ model | **强** | ReAct 每轮 LLM |
| agent ↔ function | 中 | ReAct 调工具 |
| agent ↔ chat | 弱 | KG 引用 chat 历史 |
| admin ↔ 9 服务 | 弱 | 仪表盘数据 |
| monitor ↔ 9 服务 | 弱 | 被动接收 |
| auth ↔ 其他 | 无 | 完全独立 |

### 4.4 共享依赖

```
common
  │
  ├── jwt        (auth/chat/model/memory/rag/agent)
  ├── security   (auth/agent)
  ├── rate       (auth/chat)
  ├── cache      (memory/agent)
  ├── tenant     (auth/admin)
  ├── result     (10 个服务全用)
  ├── util       (10 个服务全用)
  └── mybatis    (10 个服务全用)
```

---

## 🎯 5. 场景关联示例

### 5.1 完整一次对话

```
[用户输入 "查北京天气"]
   ↓
Frontend → chat 8082 (新消息)
   ↓
chat → memory 8084 (拉长期记忆 + 偏好)
   ↓
chat → rag 8085 (可选, 查知识库)
   ↓
chat → model 8083 (调 LLM)
   ↓
model → 6 个 provider (OpenAI / MiniMax / etc)
   ↓
LLM 响应: {tool_call: weather_get, args: {city: "北京"}}
   ↓
chat → function 8086 (执行工具)
   ↓
function → http_get (调天气 API)
   ↓
工具结果: {temp: 25, desc: "晴"}
   ↓
chat → model 8083 (第二轮 LLM + 工具结果)
   ↓
LLM 响应: "北京今天晴 25°C"
   ↓
Frontend SSE 流式显示
   ↓
chat → monitor 8089 (上报 metrics)
```

**调用的模块**: auth + chat + memory + model + function + monitor = 6 个

### 5.2 Agent 自主任务

```
[用户输入 "查竞品 A 官网, 提取 5 个 SKU 名字, 给我看"]
   ↓
Frontend → agent 8090 /agent/run
   ↓
agent → model 8083 (Round 1: LLM + tools)
   ↓
LLM 决定: tool_call http_get(url="https://a.com")
   ↓
agent → function 8086 → http_get
   ↓
Round 2: LLM + html 内容
   ↓
LLM 决定: tool_call random (模拟 SKU 抽取)
   ↓
Round 3: LLM + 结果
   ↓
LLM 输出: <final>SKU 列表: ...</final>
   ↓
agent → chat 8082 (可选: 保存为会话)
   ↓
Frontend 显示
```

**调用的模块**: agent + model + function + chat (可选) = 3-4 个

### 5.3 adminLiugl 重置用户密码

```
[adminLiugl 点击 "重置" → 输入新密码]
   ↓
Frontend → auth 8081 /auth/super/users/{id}/reset-password
   ↓
auth → SuperAdminGuard.requireSuperAdmin()  ✓
   ↓
auth → BCrypt.encode(新密码)
   ↓
auth → sys_user UPDATE password = ?
   ↓
返回成功
   ↓
Frontend 提示成功
```

**调用的模块**: auth 单模块

---

## 🛡️ 6. 权限矩阵

| 操作 | USER | ADMIN | SUPER_ADMIN |
|------|------|-------|-------------|
| 登录 | ✓ | ✓ | ✓ |
| 聊天 | ✓ | ✓ | ✓ |
| 上传文档 | ✓ | ✓ | ✓ |
| 创建会话 | ✓ | ✓ | ✓ |
| 删除自己会话 | ✓ | ✓ | ✓ |
| 删除他人会话 | ✗ | ✓ | ✓ |
| 管理插件 | ✗ | ✓ | ✓ |
| 查看监控 | ✗ | ✓ | ✓ |
| 跨服务用户管理 | ✗ | ✗ | ✓ |
| 跨租户 | ✗ | ✗ | ✓ |
| 模拟用户 | ✗ | ✗ | ✓ |
| 重启服务 | ✗ | ✗ | ✓ |
| 租户管理 | ✗ | ✗ | ✓ |
| 审计日志 | ✗ | ✗ | ✓ |

---

## 📈 7. 扩展性

| 需求 | 加模块 | 改文件 |
|------|--------|--------|
| 加新模型 | minimax-model | adapter 加一行 |
| 加新工具 | minimax-function | tool 包 + 注册 |
| 加新告警 | minimax-monitor | rule + RuleRegistry |
| 加新知识图谱 | minimax-agent | kg 包 |
| 加新页面 | frontend | views + api + router |
| 加新 API | 现有 | controller + service + mapper |
| 加新微服务 | new module | pom + common 依赖 |

---

## 🎓 8. 总结: 12 模块的角色

| 角色 | 模块 | 数量 |
|------|------|------|
| **入口** | auth | 1 |
| **中心** | chat | 1 |
| **能力** | model, memory, rag, function, multimodal | 5 |
| **增值** | agent | 1 |
| **管理** | admin, monitor | 2 |
| **共享** | common | 1 |
| **未来** | gateway | 1 |
| | **合计** | **12** |

**11 个微服务 + 1 个共享库 = 完整企业级 LLM 平台**
