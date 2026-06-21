# MiniMax Platform 架构总览 (V5.12)

> 给开发者 / 架构师 / 运维 / 产品经理看的完整架构说明
>
> 涵盖: 分层架构、数据流、技术选型、关键决策、扩展路径

## 1. 一句话定位

> **V5.12** — 基于 Spring Cloud Gateway + Nacos 的企业级 LLM 应用平台
>
> = 14 天极限构建的 12 个微服务 + V5.5-V5.12 的 8 个生产级架构升级

## 2. 分层架构

### 2.1 顶层视图

```
┌─────────────────────────────────────────────────────────────┐
│  Client Layer  (浏览器 / 移动端 H5 / 第三方 API)             │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│  Edge Layer  nginx :3000                                    │
│    - 静态资源 (Vite dist)                                    │
│    - 反向代理 /api/** → gateway :8080                        │
│    - WebSocket Upgrade (V5.8 + V5.9 精确分流)                │
│    - gzip/brotli 压缩 + security headers                     │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│  Gateway Layer  Spring Cloud Gateway :8080 (WebFlux)        │
│    - 13 路由 (auth/chat/model/memory/... + monitor)         │
│    - JwtAuthGlobalFilter (网关级鉴权)                        │
│    - UserKeyResolver / IpKeyResolver (限流)                  │
│    - Resilience4j CircuitBreaker (下游故障降级)              │
│    - TraceFilter (traceId 注入, V5.8)                        │
│    - lb://minimax-* 负载均衡 (Nacos 服务发现)                │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│  Microservice Layer  (12 个 Spring Boot 应用)                │
│    auth chat model memory rag function                       │
│    admin multimodal monitor agent prompt ws                  │
│    每个:                                                  │
│    - MyBatis-Plus + MariaDB                                 │
│    - Spring Security 6 + JWT 双 token                       │
│    - /actuator/prometheus (V5.10)                           │
│    - Swagger / knife4j (V5.11 聚合)                         │
│    - MetricsFilter (V5.10 自动采点)                          │
└──────┬──────────────────────┬────────────────────┬──────────┘
       │                      │                    │
┌──────▼──────┐    ┌──────────▼──────┐    ┌────────▼────────┐
│ MariaDB     │    │ Redis            │    │ Nacos 2.3.2     │
│ :3306       │    │ :6379            │    │ :8848           │
│ - 用户/会话  │    │ - 限流令牌桶     │    │ - 服务发现       │
│ - 消息/记忆  │    │ - 短期记忆      │    │ - 配置中心       │
│ - 文档/向量  │    │ - 缓存          │    │ - 命名空间        │
│ - 审计/告警  │    │                 │    │   minimax-dev    │
└─────────────┘    └─────────────────┘    └─────────────────┘
```

### 2.2 微服务职责矩阵

| 模块 | 端口 | 数据库表 | 核心职责 | 关键依赖 |
|------|------|----------|----------|----------|
| **gateway** | 8080 | 无 | 路由 + 鉴权 + 限流 + TraceId | spring-cloud-gateway, nacos, resilience4j |
| **auth** | 8081 | 10+ (user/role/tenant/oauth/wechat/unionid) | 认证授权 + 多租户 + 微信/QQ/支付宝 OAuth | spring-security, jjwt, wechat-sdk |
| **chat** | 8082 | session/message | 会话消息 + SSE 流式 + 取消 + 重试 | SSE, Java HttpClient |
| **model** | 8083 | provider/config/battle_log | LLM 路由 + 6 模型 + 限流 + OpenAI 兼容 | Bucket4j, OpenAI SDK |
| **memory** | 8084 | short/long/preference | 短期 (Redis+Caffeine) + 长期 (向量) + 偏好 | Redis, MySQL BLOB 向量 |
| **rag** | 8085 | kb/doc/chunk | 文档解析 + 智能分块 + 检索 + 引用 | PDFBox, Apache POI |
| **function** | 8086 | tool/log | 4 工具 + LLM 循环 (5 轮) + 审计 | Java HttpClient, SSRF 防护 |
| **admin** | 8087 | audit_log | 跨服务聚合 + 审计 + Dashboard | Java HttpClient |
| **multimodal** | 8088 | upload | 图片理解 + 视觉模型 | OpenAI Vision API |
| **monitor** | 8089 | alert_rule/event/snapshot | Prometheus + 告警引擎 + 健康详情 | Micrometer, Spring Scheduling |
| **agent** | 8090 | session/step | 智能体编排 + 工具循环 + KG | Function Calling |
| **prompt** | 8091 | template | Prompt 模板管理 | MyBatis |
| **ws** | 8095 | 无 | WebSocket 流式网关 | Spring WebSocket, Reactor |

## 3. 关键数据流

### 3.1 用户请求流 (HTTP)

```
Browser → nginx:3000/api/v1/auth/login
         ↓
gateway:8080/api/v1/auth/login
         ↓ TraceFilter 注入 X-Trace-Id
JwtAuthGlobalFilter (login 路径白名单)
         ↓
lb://minimax-auth/api/login
         ↓ Nacos 查询可用实例
minimax-auth:8081
         ↓ MetricsFilter 记录请求指标
AuthController.login()
         ↓
Result.ok(token)
         ↓
Response: X-Trace-Id + token + Result<...>
         ↓
nginx → Browser
```

### 3.2 流式聊天 (SSE)

```
Browser → POST /api/v1/chat/message (Accept: text/event-stream)
         ↓
gateway 路由 → lb://minimax-chat
         ↓
ChatController.streamMessage()
         ↓
OpenAIGateway.streamCompletion() (HttpClient + BodyHandlers.ofLines)
         ↓
SSE chunk: data: {"content": "..."}
         ↓ (多次)
data: [DONE]
         ↓
Browser ReadableStream 渲染
```

### 3.3 工具调用循环 (Agent)

```
User: 上海现在几点? 算 (123+456)*789
         ↓
agent/run
         ↓
Step 1: LLM 决策
  - get_current_time(timezone="Asia/Shanghai")
  - calculator(expression="(123+456)*789")
         ↓
Step 2: function/invoke/{name} (lb://minimax-function)
         ↓ 返回工具结果
Step 3: LLM 整合
  - 上海 23:00
  - (123+456)*789 = 456897
         ↓
Step 4: 终态返回
```

## 4. 关键技术决策 (V5.5-V5.12)

| 决策 | 时机 | 原因 | 后果 |
|------|------|------|------|
| **Spring Cloud Gateway (WebFlux)** | V5.5 | 12 微服务需要统一鉴权 / 限流 / 路由 | 业务模块零感知, 升级灵活 |
| **Nacos 服务发现** | V5.7 | 硬编码 URL 难维护, 需要弹性扩缩 | lb://minimax-* 自动发现 |
| **Resilience4j (不用 Hystrix)** | V5.7 | Hystrix 已停止维护, Resilience4j 是 Spring 官方推荐 | 熔断 + 重试 + 超时 |
| **nginx 端口 3000 (不用 80)** | V5.12 | 避免 root 权限, 与开发端口分离 | 部署简单, 调试方便 |
| **TraceId 在 gateway 注入** | V5.8 | 业务模块不需要关心 | 全链路可追踪, 5xx 错误带 traceId |
| **MetricsFilter URI 归一化** | V5.10 | `/api/v1/user/123` 会产生高基数 label | 防 Prometheus 内存爆炸 |
| **WebSocket 精确分流** | V5.9 | Jakarta `@ServerEndpoint` 与 WebFlux 不兼容 | `/ws/notifications` 走直连, 其他走 gateway |
| **knife4j 静态聚合** | V5.11 | 13 个独立 doc.html 用户体验差 | 单入口 + tab 切换 + iframe |
| **Nacos 2.3.2 standalone** | V5.12 | 集群模式复杂度高, 单机足够 | 部署简单, MySQL 持久化配置 |

## 5. 可观测性 (V5.10)

### 5.1 指标分层

```
┌──────────────────────────────────────────────┐
│  HTTP 层 (公共, 13 服务自动)                  │
│  - minimax.http.requests.total               │
│  - minimax.http.requests.duration            │
│  - minimax.http.4xx/5xx.errors.total         │
└──────────────────────────────────────────────┘
                    +
┌──────────────────────────────────────────────┐
│  业务层 (minimax-monitor)                     │
│  - minimax.chat.messages.total               │
│  - minimax.tool.calls.total                  │
│  - minimax.rag.queries.total                 │
│  - minimax.llm.tokens.total                  │
│  - minimax.llm.latency (histogram)           │
└──────────────────────────────────────────────┘
                    +
┌──────────────────────────────────────────────┐
│  JVM 层 (Spring Boot Actuator)               │
│  - jvm.memory.used                           │
│  - jvm.gc.pause                              │
│  - hikaricp.connections.active               │
│  - process.cpu.usage                         │
└──────────────────────────────────────────────┘
```

### 5.2 告警规则 (V5.9)

| 默认规则 | 指标 | 阈值 | 严重度 | 冷却 |
|---------|------|------|--------|------|
| CPU 高 | cpu_usage | > 80% | warning | 5min |
| JVM Heap 高 | jvm_heap_usage | > 85% | critical | 5min |
| 磁盘高 | disk_usage | > 90% | critical | 10min |
| LLM 延迟高 | llm_avg_latency | > 3000ms | warning | 3min |
| 5xx 错误率 | http_5xx_rate | > 5% | critical | 5min |

### 5.3 前端可视化

- `/admin/metrics` (V5.10): 实时 metrics dashboard, Top10 URI / 状态码饼图 / 10s 自动刷新
- `/admin/dashboard` (V5.6+V5.9): 真实折线图 (3 条: 全部/用户/工具)
- `/admin/monitor` (V5.9): 健康/JVM/DB/磁盘/告警, 10s 自动刷新
- `/api-docs` (V5.11): 13 服务 API 文档聚合

## 6. 部署架构 (V5.12)

### 6.1 单机一键部署

```bash
sudo ./scripts/deploy-minimax.sh install
# 自动: Java/Maven/Node + MariaDB/Redis/Nacos + 编译 + 16 systemd + nginx
```

### 6.2 systemd 服务清单 (16 个)

```
基础设施层:   minimax-nacos / mariadb / redis-server
应用网关层:   minimax-gateway
微服务层:     minimax-{auth,chat,model,memory,rag,function,
                          admin,multimodal,monitor,
                          agent,prompt,ws}  (12 个)
前端层:       minimax-frontend (Vite preview)
边缘层:       minimax-nginx
```

### 6.3 启动顺序

```
mariadb → redis → nacos (sleep 25s) → gateway (sleep 12s)
        → 12 微服务 → frontend + nginx
```

## 7. 扩展路径

### 7.1 添加新微服务

```bash
# 1. 在 backend/ 下创建新模块
mkdir backend/minimax-foo
# 2. 继承 common 父 POM
# 3. application.yml 加 minimax: 顶层配置 + nacos discovery
# 4. gateway yml 加 route
# 5. deploy-minimax.sh MODULES 数组加 "foo:8xxx"
# 6. 重跑 install
```

### 7.2 添加新告警规则

```bash
# 方式 1: UI 弹窗 (V5.9)
/admin/monitor → 告警规则 → 新增

# 方式 2: SQL
INSERT INTO alert_rule (name, metric_name, operator, threshold, severity, cooldown_minutes) 
VALUES ('自定义', 'http_5xx_rate', '>', 0.05, 'critical', 5);
```

### 7.3 添加新 LLM Provider

```bash
# 1. minimax-model/.../provider/ 加适配器
class FooProvider implements ModelProviderAdapter { ... }
# 2. ModelProviderFactory 注册
# 3. model_config 表插入新模型
# 4. 前端 ModelSelect 下拉自动显示
```

### 7.4 水平扩展

```bash
# 1. 启动第 2 个 minimax-chat 实例
java -jar minimax-chat.jar --server.port=8082-2
# 2. 自动注册到 Nacos
# 3. gateway 通过 lb://minimax-chat 自动负载均衡
# 4. Redis 限流令牌桶跨实例共享 (一致性)
```

## 8. 故障转移 / 灾备

| 故障 | 表现 | 恢复 |
|------|------|------|
| 某微服务挂 | gateway 503 | Resilience4j CircuitBreaker 自动 fallback |
| Nacos 挂 | gateway 用本地缓存路由 | Nacos 重启后自动恢复 |
| MariaDB 挂 | 所有写操作失败 | Redis 缓存兜底, MySQL 重启后恢复 |
| Redis 挂 | 限流失效 / 短期记忆丢 | Bucket4j 内存降级 / 长期记忆兜底 |
| nginx 挂 | 入口不可达 | 客户端直连 gateway :8080 兜底 |
| gateway 挂 | API 全部 502 | 客户端直连各微服务端口兜底 |

## 9. 技术选型对照

| 类别 | 选型 | 对比 | 理由 |
|------|------|------|------|
| 后端 | Spring Boot 3 + JDK 17 | vs Spring Cloud完整套件 | 学习成本低, 社区活跃 |
| 网关 | Spring Cloud Gateway (WebFlux) | vs Zuul / Kong | 官方推荐, 性能高, WebFlux |
| 服务发现 | Nacos 2.3.2 | vs Eureka / Consul | 国人开发, 中文文档, 易部署 |
| 限流 | Bucket4j | vs Guava RateLimiter | 分布式支持, 持久化 |
| 缓存 | Caffeine + Redis | vs Guava Cache | Caffeine 性能高, Redis 跨实例 |
| 向量库 | MySQL BLOB + 余弦相似度 | vs Milvus / Qdrant | 业务量小, 减少依赖 |
| ORM | MyBatis-Plus | vs Spring Data JPA | 中文友好, 灵活 |
| 监控 | Micrometer + Prometheus | vs Spring Boot Admin | 工业标准, Grafana 生态 |
| 前端 | Vue 3 + Element Plus | vs React + Antd | 中文文档, 组件丰富 |
| 文档 | knife4j 4.4.0 | vs Swagger UI | 国人开发, 增强 UI |
| 部署 | Linux systemd | vs Docker / K8s | 单机场景简单, 见 [K8s 路径] |

## 10. 进阶阅读

- [DEPLOY-GUIDE.md](DEPLOY-GUIDE.md): 完整部署步骤
- [GATEWAY-GUIDE.md](GATEWAY-GUIDE.md): Spring Cloud Gateway 配置详解
- [METRICS-GUIDE.md](METRICS-GUIDE.md): Prometheus + Grafana 接入
- [API-DOCS-GUIDE.md](API-DOCS-GUIDE.md): 文档聚合中心
- [DEVELOPER-GUIDE.md](DEVELOPER-GUIDE.md): 开发者上手手册
- [MODULES.md](MODULES.md): 13 个微服务详细说明

## 11. 演进路线

```
V5.5  ─┐
V5.6   │  Spring Cloud Gateway 化
V5.7   │  + Nacos + Resilience4j
V5.8   │  + TraceId + nginx 优化
V5.9   │  + 真实图表 + 告警规则 CRUD
V5.10  │  + Prometheus 全链路
V5.11  │  + 文档聚合
V5.12 ─┘  + 部署一键化
         ↓
V5.13  本次: 架构文档完善 (README + ARCHITECTURE)
         ↓
V6.x  待定:  (候选) K8s 集群 / OpenTelemetry 分布式追踪
              / 真实 LLM API 接入 / 多租户数据隔离
```
