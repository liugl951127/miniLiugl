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
| 后端模块 | 11 |
| Java 文件 | 191 |
| Java 行数 | 11,454 |
| SQL 文件 | 8 |
| SQL 行数 | 963 |
| 单元/集成测试 | 125 (0 失败) |
| HTTP 端点 | 92+ |
| 数据表 (MySQL) | 18+ |
| 前端组件/视图 | 12+ |
| 部署脚本 | 4 |
| Git commits | 14+ |

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

