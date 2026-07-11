# MiniMax Platform 架构设计 (V2.8.2)

> 企业级 AI 平台架构白皮书 · 适用版本 V2.5+ V2.8

## 1. 概述

MiniMax Platform 是基于 Spring Cloud + Spring Boot 3 + JDK 17 的**自研 AI 平台**, 包含 **17 个微服务**, 完整实现从聊天、知识库、Agent 到多模态生成、流程编排的 AI 应用全栈.

### 1.1 核心特性
- 🤖 **自研 AI 引擎**: 不依赖任何外部 LLM (OpenAI/Claude/DeepSeek), MiniTransformer + 教学级训练
- 🎨 **多模态生成**: 图表 (7 种) / 音乐 (6 风格) / 动画 (GIF) / 视频 (SSE 流式) / 图片
- 🔐 **企业级合规**: 审计日志 / 数据脱敏 / 文件加密 (AES-256-GCM) / 内容审核
- 📊 **数据智能**: NL2SQL / 数据分析 / 趋势 / 异常检测 / 分布
- 🌐 **i18n**: 中英双语
- 🔌 **插件化**: 工具注册 + 动态调用 + 限流 + 审计

### 1.2 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.2.x |
| 微服务 | Spring Cloud | 2023.x |
| 服务发现 | Nacos | 2.3.x |
| 网关 | Spring Cloud Gateway | 4.x |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | MariaDB | 10.11 |
| 缓存 | Redis | 7.x |
| 链路追踪 | OpenTelemetry | 1.x |
| 前端 | Vue 3 + Element Plus | 3.4 / 2.5 |
| 构建 | Vite | 5.x |
| 容器 | Docker + Docker Compose | 24+ |
| CI/CD | GitHub Actions | - |

## 2. 系统架构

### 2.1 微服务拓扑

```
                            ┌────────────────┐
                            │   Nginx :80    │
                            │  (Host)        │
                            └────────┬───────┘
                                     │
                            ┌────────▼───────┐
                            │  Gateway :7080 │  (Spring Cloud Gateway)
                            │  StripPrefix=2 │
                            └────────┬───────┘
                                     │
        ┌─────────────┬──────────────┼──────────────┬──────────────┐
        │             │              │              │              │
   ┌────▼────┐   ┌───▼────┐   ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐
   │ Auth    │   │  Chat  │   │  Memory   │  │  Agent    │  │  Model    │
   │ :8081   │   │  :8082 │   │  :8083    │  │  :8088    │  │  :8084    │
   └────┬────┘   └───┬────┘   └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
        │             │              │              │              │
        └─────────────┴──────────────┴──────────────┴──────────────┘
                                     │
                              ┌──────▼──────┐
                              │   Nacos     │  (服务发现/配置)
                              │   :8848     │
                              └─────────────┘

   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
   │ Function    │ │ Multimodal  │ │  Monitor    │ │   Admin     │
   │  :8086      │ │  :8087      │ │  :8089      │ │  :8090      │
   └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘

   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
   │  Prompt     │ │  Analytics  │ │  RAG        │
   │  :8091      │ │  :8092      │ │  :8085      │
   └─────────────┘ └─────────────┘ └─────────────┘

   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
   │  Pipeline   │ │ **AI V2.5** │ │     WS      │
   │  :8093      │ │  **:8094**  │ │  :8095      │
   └─────────────┘ └─────────────┘ └─────────────┘

                              ┌─────────────┐
                              │   MariaDB   │
                              │   :3306     │
                              └─────────────┘
                              ┌─────────────┐
                              │   Redis     │
                              │   :6379     │
                              └─────────────┘
```

### 2.2 请求流转

```
Client
  │
  ├─→ HTTP  ──→ Nginx:80 ──→ Gateway:7080 (/api/**)
  │                              │
  │                              ├─ JwtFilter (验证 token)
  │                              ├─ RateLimit (Bucket4j)
  │                              ├─ TraceFilter (OTel traceId)
  │                              └─ LoadBalance (随机)
  │                                       │
  │                                       ▼
  │                                 AuthService / ChatService / AIService
  │                                       │
  │                                       ├─ 业务逻辑
  │                                       ├─ MyBatis-Plus ──→ MariaDB
  │                                       └─ RedisCache ──→ Redis
  │
  ├─→ WebSocket /queue/events
  │       (Chat 实时消息)
  │
  └─→ SSE /api/ai/video/stream/sse
          /api/ai/music/stream/sse
          (流式生成推送)
```

## 3. 模块划分

### 3.1 核心服务 (12)

| 模块 | 端口 | 职责 |
|------|------|------|
| gateway | 7080 | API 网关, 路由 + 鉴权 + 限流 |
| auth | 8081 | 用户认证 / JWT / 第三方登录 |
| chat | 8082 | 聊天会话 / WebSocket 推送 |
| memory | 8083 | 短期记忆 / 长期记忆 / 摘要 |
| model | 8084 | 模型管理 / Provider 配置 |
| rag | 8085 | 知识库 / 文档索引 / 检索 |
| function | 8086 | 函数调用 / 工具市场 |
| multimodal | 8087 | 图像/音频/视频分析 |
| agent | 8088 | ReAct Agent / 多步推理 |
| monitor | 8089 | 监控指标 / 告警 / 审计 |
| admin | 8090 | 后台管理 / 仪表盘 |
| prompt | 8091 | Prompt 模板 |

### 3.2 业务服务 (5)

| 模块 | 端口 | 职责 |
|------|------|------|
| analytics | 8092 | 数据分析 / 报表 |
| pipeline | 8093 | 工作流编排 / 调度 |
| **ai** | **8094** | **自研 AI 引擎 (V2.5+)** |
| ws | 8095 | WebSocket 网关 |

## 4. AI 引擎架构 (V2.5+ V2.7+)

```
                       ┌─────────────────┐
                       │  用户提示词      │
                       │  "画一个饼图"   │
                       └────────┬────────┘
                                │
                       ┌────────▼─────────┐
                       │  KeywordEngine   │  (13 意图 + 关键词 + regex)
                       │  recognize()     │
                       └────────┬─────────┘
                                │
            ┌───────────────────┼───────────────────┐
            │                   │                   │
   ┌────────▼────────┐ ┌────────▼────────┐ ┌────────▼────────┐
   │ GENERATE_CHART  │ │ GENERATE_MUSIC  │ │   QUERY_DATA    │
   │ → ChartGenerator│ │ → MusicGenerator│ │   → NL2SQL      │
   └────────┬────────┘ └────────┬────────┘ └────────┬────────┘
            │                   │                   │
   ┌────────▼──────────────────▼───────────────────▼────────┐
   │           AWT 渲染 / MIDI 编码 / SQL 执行                 │
   └────────┬────────────────────────────────────────────────┘
            │
   ┌────────▼────────┐
   │  返回 PNG/MIDI  │  或 SSE 流式推 chunk
   │  Base64/File    │
   └─────────────────┘
```

### 4.1 算法选择 (为什么不依赖外部 LLM)
- **成本**: OpenAI/Claude 按 token 计费, 大规模用户成本高
- **数据隐私**: 客户数据不能出网
- **可定制**: 自研模型可针对业务调优
- **教学价值**: 团队 AI 能力建设

### 4.2 自研模型
- **MiniTransformer**: 简化版 Transformer, 1-2M 参数, CPU 训练
- **ChineseTokenizer**: 中文 BPE 分词
- **MiniTrainer**: 教学级训练循环 (SGD + 简化 BPTT)
- **TextGenerator**: 混合策略 (70% n-gram 统计 + 30% transformer logits)

### 4.3 工具市场
- DB 存储 9 内置工具 + 无限扩展
- 工具自动发现 (Java SPI)
- 调用审计 + 限流
- 智能参数表单 (前端 SDK)

## 5. 流式生成 (V2.7.6+ V2.8.1)

### 5.1 SSE 协议
```
GET /api/ai/video/stream/sse
Accept: text/event-stream

→ event: start
  data: {"taskId":"stream-123","totalFrames":60}

→ event: frame
  data: {"taskId":"stream-123","index":0,"data":"<base64 png>"}

→ event: progress
  data: {"taskId":"stream-123","percent":50}

→ event: complete
  data: {"taskId":"stream-123","durationMs":4500}

→ event: heartbeat (每 5s)
  data: "ping"
```

### 5.2 心跳保活
每 5s 发 heartbeat, 防 nginx 默认 60s 断连. 客户端可选择忽略.

## 6. 数据安全 (V2.6+)

### 6.1 数据脱敏
```java
DataMasker.mask("13800138000", DataMasker.Type.MOBILE)
// → "138****8000"
```

### 6.2 文件加密 (AES-256-GCM)
文件格式: `[MAGIC(4)="MMX1"][IV(12)][ciphertext+tag(16)]`
密钥: `${MINIMAX_FILE_KEY:default32bytes1234567890abcdef}`

### 6.3 审计日志
- `audit_log_full` 表: 6 个月保留
- 异步落库 (`@Async AuditLogger`)
- DB 失败时降级到本地日志

## 7. RBAC (V2.7.9)

### 7.1 角色
- `SUPER_ADMIN` - 全部权限 (`*`)
- `ADMIN` - 除 super 外的所有
- `USER` - AI 使用 / 工具调用
- `GUEST` - 只读

### 7.2 注解
```java
@RequiresPermission("ai.admin")
public Result<?> deleteTool(@PathVariable String code) { ... }

@RequiresPermission(value = {"ai.use", "ai.admin"}, mode = Mode.ANY)
public Result<?> invoke() { ... }
```

### 7.3 前端指令
```vue
<el-button v-permission="'ai.admin'">删除</el-button>
<div v-permission="['ai.use', 'ai.admin']" mode.any>...</div>
```

## 8. 国际化 (V2.7.8)

- 后端: `AcceptHeaderLocaleResolver` + `ResourceBundleMessageSource`
- 前端: `vue-i18n` + 532 keys (中英)
- 切换: 顶部右上角 🇨🇳/🇺🇸

## 9. CI/CD (V2.8.0)

### 9.1 GitHub Actions 流水线
```
push to main
  ↓
  backend (compile + test) ─┐
                            ├──→ docker (build images)
  frontend (npm run build) ─┘
                            ↓
                            notify (汇总状态)
```

### 9.2 本地 CI
```bash
./scripts/local-ci.sh           # 编译 + 测试
./scripts/local-ci.sh --docker  # + Docker 镜像
```

## 10. 部署架构

### 10.1 单机部署 (开发)
```
一台 4C8G VPS
  ├─ Nginx (宿主机) :80
  ├─ Docker Compose
  │    ├─ MariaDB :3306
  │    ├─ Redis :6379
  │    ├─ Nacos :8848
  │    └─ 17 微服务 :8081-8095
  └─ /opt/minimax/data/ (数据卷)
```

### 10.2 生产部署 (K8s)
- 微服务 → K8s Deployment
- MariaDB/Redis → 云托管服务
- Nacos → 集群 3 节点
- Gateway → Ingress + Nginx
- AI 模块 → 独立节点池 (GPU 预留)

## 11. 性能指标 (V2.7+)

| 接口 | 平均响应 | 95 分位 |
|------|----------|---------|
| /api/auth/login | 80ms | 150ms |
| /api/chat/send | 120ms | 250ms |
| /api/ai/generate | 800ms | 1500ms |
| /api/ai/chart/render | 200ms | 400ms |
| /api/ai/music/generate | 300ms | 600ms |
| /api/ai/video/stream | (SSE 长连接) | - |
| /api/ai/document/parse | 1.5s | 3s |

JVM: G1GC + MaxRAMPercentage=70, 100 Tomcat 线程

## 12. 后续规划

- [ ] 训练可视化对接 TensorBoard 协议
- [ ] 移动端原生 App (UniApp)
- [ ] 实时协作 (Y.js)
- [ ] 多租户隔离
- [ ] 联邦学习 (跨企业训练)

---

更多文档:
- [DEPLOYMENT.md](DEPLOYMENT.md) - 部署指南
- [API.md](API.md) - API 参考
- [USER_GUIDE.md](USER_GUIDE.md) - 用户手册
- [OPERATIONS.md](OPERATIONS.md) - 运维手册
- [CHANGELOG.md](CHANGELOG.md) - 更新日志
