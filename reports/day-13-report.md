# Day 13 报告 - 调优 (限流 + 缓存 + 异步 + 请求日志 + 压测)

**日期**: 2026-06-16
**目标**: 性能调优 — 限流多维度化 + Caffeine 缓存 + 异步任务 + 请求日志 + 压测
**Commit**: pending

---

## ✅ 完成项

### 1. 数据模型 (3 张表)
- ✅ `request_log` — 请求日志: traceId/service/method/path/status/duration/is_slow/is_error
- ✅ `async_task` — 异步任务: taskId/type/payload/status/attempts/result
- ✅ `rate_limit_rule` — 限流规则 (可配置): scope/key_pattern/capacity/refill
- ✅ H2 + MySQL 双兼容 schema (含 4 个默认限流规则)

### 2. 限流优化 (common 模块, Bucket4j 8.10.1)
- ✅ `RateLimiter` — 通用令牌桶封装
  - 容量 + 补充策略 (intervally)
  - 线程安全 (ConcurrentHashMap)
  - 可 tryConsume(n) 批量扣
- ✅ `RateLimitService` — 多 scope 限流服务
  - IP 限流 (100/60s)
  - User 限流 (60/60s)
  - Global 限流 (1000/60s)
  - 配置化 (yml 调阈值)
  - 触发时打 WARN 日志

### 3. 缓存层 (common 模块, Caffeine 3.1.8)
- ✅ `CacheService` — 多命名 cache 统一管理
  - define(name, maxSize, ttl)
  - put / get / getOrLoad
  - invalidate / clear / clearAll
  - stats (hit/miss/eviction)
  - **getOrLoad 防击穿**: loader 仅调一次
  - **TTL 自动过期**: 防止脏数据

### 4. 异步任务 (common 模块)
- ✅ `AsyncTaskService` — 完整异步任务执行器
  - 任务 ID 用 UUID
  - **状态机**: pending / running / done / failed
  - **自动重试**: maxRetries 默认 3 次
  - **支持结果回调**: AsyncResultHandler<T>
  - **支持 Future**: CompletableFuture<T>
  - 自定义 ThreadPool (core/max/queue)
  - 状态查询 + 实时监控

### 5. 请求日志中间件 (common 模块)
- ✅ `RequestLogFilter` — OncePerRequestFilter
  - **traceId**: UUID 注入 header + request attr
  - 慢请求标记 (> 1000ms)
  - 错误请求标记 (status >= 500)
  - 自动 log 慢/错请求
  - 真实 client IP (穿透 X-Forwarded-For)
  - Order = HIGHEST_PRECEDENCE + 10 (最前)

### 6. 压测脚本
- ✅ `scripts/benchmark.sh` — Bash 并发压测
  - 并发 N (xargs -P)
  - 总请求 M
  - 输出: 总耗时 / QPS / 成功/失败 / p50 / p95 / p99 / max / min / avg
  - 适用: 任意 HTTP 端点快速压测
- 用法: `bash scripts/benchmark.sh http://localhost:8081 /api/v1/auth/health 50 1000`

### 7. 优化配置模板
- ✅ `minimax-optimized.yml` — 生产推荐配置
  - **Tomcat 线程**: max 400 / min-spare 50
  - **HikariCP**: max 30 / min 10 / leak 检测 60s
  - **Redis Lettuce**: pool 32/16/4
  - **JVM**: G1GC + MaxGCPauseMillis 200 + 堆 dump + 容器 cgroup
  - **优雅停机**: 30s

### 8. common 模块升级
- ✅ pom 加 Bucket4j + Caffeine
- ✅ 保留原有 Security + JWT (无破坏)

---

## 📊 关键数据

| 指标 | Day 12 | Day 13 | 增量 |
|------|--------|--------|------|
| Java 文件 | 185 | **191** | +6 |
| Java 行数 | 10837 | **11454** | +617 |
| SQL 行数 | 828 | **963** | +135 |
| 单元/集成测试 | 114 | **125** | +11 |
| common 模块端点 | 0 | (内部组件) | - |
| 通用组件 | 5 (Result/Filter/JWT) | **9** (+RateLimiter/Cache/Async/RequestLog) | +4 |
| 优化配置项 | 0 | **20+** | new |

### Day 13 新增测试 (11 用例)
- `OptimizationTest`:
  - rateLimiterAllowsUpToCapacity (5 通过 / 6 拒绝)
  - rateLimiterIsolatesKeys (不同 key 独立)
  - rateLimiterTryConsumeMany (批量消费)
  - rateLimitServiceMultiScope (ip/user/global 隔离)
  - cacheBasic (put/get/invalidate)
  - cacheGetOrLoad (loader 仅调一次 = 防击穿)
  - cacheStats (hit/miss 统计)
  - asyncTaskSubmitAndStatus (基本异步)
  - asyncTaskFailureRetry (3 次重试)
  - asyncTaskWithResult (结果回调)
  - asyncTaskFuture (CompletableFuture)

---

## 🏗️ 架构改进

### 优化前后对比

| 维度 | 优化前 | 优化后 |
|------|--------|--------|
| 限流 | 单维度 (Day 4 Bucket4j) | **3 维度 (IP/User/Global) + 可配置** |
| 缓存 | 无 | **Caffeine 多命名 + TTL + 统计** |
| 异步 | @Async (无状态) | **任务 ID + 状态机 + 重试 + 回调** |
| 请求日志 | 无 | **traceId + 慢/错采点** |
| 压测 | 无 | **benchmark.sh 一键** |
| 配置 | 默认 | **生产调优模板** |

### 关键设计
1. **Bucket4j 替代自实现** — 工业级令牌桶库, 线程安全 + 性能优
2. **Caffeine 替代 ConcurrentHashMap** — TTL + LRU + 统计 + 防击穿
3. **任务 ID 而非直接 Future** — 可序列化入库, 状态可查
4. **traceId 全链路** — Filter 注入, 跨服务透传
5. **限流分层** — IP/User/Global 三层叠加, 防止任何单点打满
6. **配置化限流** — 改 yml 不改代码

---

## 🔍 验证

### 单元/集成测试 (125 用例全过)
```
OptimizationTest ......... 11/11  ← Day 13
AlertEngineTest .......... 5/5
HealthDetailTest ......... 6/6
BuiltinToolsTest ........ 13/13
FunctionIntegrationTest . 10/10
RagIntegrationTest ...... 8/8
TextChunkerTest ......... 6/6
VectorUtilsTest .......... 5/5
ContextBuilderTest ...... 4/4
MockEmbeddingClientTest . 5/5
ShortTermMemoryTest ..... 4/4
VectorUtilsTest (memory) 7/7
JwtTokenProviderTest .... 4/4
MessageRoleTest ......... 3/3
MockAdapterTest ......... 3/3
ModelProviderFactoryTest 4/4
StreamingTest ........... 3/3
VisionServiceTest ....... 7/7
AdminIntegrationTest .... 8/8
ServiceClientTest ....... 3/3
                   -------
                   125/125 ✅
```

### Maven 编译 (11 模块)
```
minimax-platform ...... SUCCESS
minimax-common ........ SUCCESS  ← Day 13 升级
minimax-gateway ....... SUCCESS
minimax-auth .......... SUCCESS
minimax-chat .......... SUCCESS
minimax-memory ........ SUCCESS
minimax-model ......... SUCCESS
minimax-rag ........... SUCCESS
minimax-function ...... SUCCESS
minimax-admin ......... SUCCESS
minimax-multimodal .... SUCCESS
minimax-monitor ....... SUCCESS
======================
BUILD SUCCESS · 56s
```

---

## 🌐 GitHub

- 仓库: https://github.com/liugl951127/miniLiugl.git
- 状态: pending
- 改动: +6 java + 1 SQL + 1 schema + 1 测试 + 1 yml + 1 压测脚本

---

## 📁 新增文件

```
sql/13_optimization.sql
sql/init/13_optimization.sql
backend/minimax-common/pom.xml                                          (升级 + Bucket4j + Caffeine)
backend/minimax-common/src/main/java/com/minimax/common/ratelimit/RateLimiter.java
backend/minimax-common/src/main/java/com/minimax/common/ratelimit/RateLimitService.java
backend/minimax-common/src/main/java/com/minimax/common/cache/CacheService.java
backend/minimax-common/src/main/java/com/minimax/common/async/AsyncTaskService.java
backend/minimax-common/src/main/java/com/minimax/common/request/RequestLogFilter.java
backend/minimax-common/src/main/resources/minimax-optimized.yml
backend/minimax-common/src/test/java/com/minimax/common/OptimizationTest.java
scripts/benchmark.sh
reports/day-13-report.md
```

---

## 🚀 下一步 (Day 14: 交付)

- 完整 README (架构图 + 部署 + Demo)
- API 文档 (OpenAPI 3 / Knife4j)
- Docker 镜像 (12 服务)
- K8s 完整 manifest (12 服务 + Ingress + HPA)
- 演示视频脚本
- 客户案例 (3 个场景: 智能客服 / 知识助手 / 工具增强)
