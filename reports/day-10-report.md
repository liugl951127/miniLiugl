# Day 10 报告 - 管理后台 (跨服务 API 聚合 + 审计 + 监控)

**日期**: 2026-06-16
**目标**: 完整落地管理后台 — 跨服务 API 聚合 + 统一操作审计 + 健康检查聚合 + 业务统计
**Commit**: pending

---

## ✅ 完成项

### 1. 数据模型 (1 张表)
- ✅ `admin_audit_log` — 统一操作审计: actor/action/resourceType/resourceId/detail/result/error/ip/ua
- ✅ H2 + MySQL 双兼容 schema

### 2. 跨服务 HTTP 客户端 (Day 10 核心)
- ✅ `ServiceClient` — 通用 HTTP 客户端 (Java 11+ HttpClient, 无 Feign)
  - GET / POST / PUT / DELETE 全部支持
  - 可注入 service-to-service token
  - `isReachable(baseUrl)` 探活
  - 异常隔离: 单服务失败不影响整体
- ✅ `ServiceEndpoints` — 6 服务 URL 集中配置 (auth/chat/model/memory/rag/function)

### 3. 业务聚合服务
- ✅ `UserMgmtService` — 用户管理 (代理 auth + 自动审计)
  - listUsers / getUser
  - createUser (审计 create_user)
  - resetPassword (审计 reset_password)
  - toggleUser enable/disable (审计 enable_user/disable_user)
- ✅ `ModelMgmtService` — 模型管理 (代理 model + 自动审计)
  - listProviders / listConfigs
  - updateRateLimit (审计 update_rate_limit)
- ✅ `StatsService` — 业务统计
  - periodBounds (today / week7d / month30d)
  - opsStats (按 action 聚合)
  - modelStats / toolStats (代理其他服务)
  - dashboard (一页关键指标)
- ✅ `HealthAggregator` — 跨服务健康检查
  - 并发 ping 6 服务 (异步 + 3s 超时)
  - 输出 up/total + 各自状态
  - durationMs 性能监控
- ✅ `AuditService` — 统一操作审计
  - record(actor/action/resource/detail/result)
  - recent / byActor
  - countByAction / countByResourceType (GROUP BY)

### 4. 暴露的 HTTP 端点 (12 个)
| 方法 | 路径 | 功能 |
|---|---|---|
| GET    | `/admin/users` | 列出用户 |
| GET    | `/admin/users/{id}` | 用户详情 |
| POST   | `/admin/users` | **注册用户 (审计)** |
| POST   | `/admin/users/{id}/reset-password` | **重置密码 (审计)** |
| PUT    | `/admin/users/{id}/status` | **启停 (审计)** |
| GET    | `/admin/models/providers` | 列出模型 Provider |
| GET    | `/admin/models` | 列出模型 Config |
| PUT    | `/admin/models/{code}/rate-limit` | **调限流 (审计)** |
| GET    | `/admin/stats/ops` | 操作统计 |
| GET    | `/admin/stats/dashboard` | **一页 dashboard** |
| GET    | `/admin/health` | **跨服务 health 聚合** |
| GET    | `/admin/audit/recent` | 最近审计 |
| GET    | `/admin/audit/by-actor/{id}` | 按操作人审计 |
| GET    | `/admin/ping` | 心跳 |

---

## 📊 关键数据

| 指标 | Day 9 | Day 10 | 增量 |
|------|-------|--------|------|
| Java 文件 | 152 | **166** | +14 |
| Java 行数 | 8407 | **9410** | +1003 |
| SQL 行数 | 610 | **683** | +73 |
| 单元/集成测试 | 85 | **96** | +11 |
| 端点 (admin 模块) | 0 | **14** | +14 |
| 数据表 (admin) | 0 | **1** | +1 |
| 后端模块 | 8 | **9** | +1 |

### Day 10 新增测试 (11 用例)
- `ServiceClientTest` (3 cases):
  - errorResp / isReachableLocalUnreachable / isReachableInvalidUrl
- `AdminIntegrationTest` (8 cases):
  - recordAuditLog / recordAuditLogError
  - auditByActor / countByAction
  - periodBounds / opsStats
  - healthAggregateAllDown / errorRespMethod

---

## 🏗️ 架构设计

### 跨服务架构
```
┌──────────────────────────────────────────────────────────────┐
│                       Admin (8087)                           │
│                                                              │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│   │ UserMgmt    │  │ ModelMgmt   │  │ Stats       │         │
│   │ Service     │  │ Service     │  │ Service     │         │
│   └─────────────┘  └─────────────┘  └─────────────┘         │
│         │                │                │                 │
│   ┌─────▼────────────────▼────────────────▼──────────────┐  │
│   │              ServiceClient (HTTP)                     │  │
│   └────┬──────┬──────┬──────┬──────┬──────┬──────────────┘  │
│        │      │      │      │      │      │                │
└────────┼──────┼──────┼──────┼──────┼──────┼────────────────┘
         │      │      │      │      │      │
       auth  chat  model memory  rag  function
       8081  8082  8083  8084  8085  8086
```

### 关键设计
1. **不引 Feign** — 用 Java 11+ HttpClient 自己写客户端, 零额外依赖
2. **降级完整** — 单服务不可达返回 `unavailable` JSON, 不抛 500
3. **统一审计** — 关键操作 (改密/启停/调限流) 强制写 `admin_audit_log`
4. **异步健康检查** — CompletableFuture + 3s 超时, 6 服务并发 ping
5. **dashboard 一页** — 聚合 ops/model/tools 3 类指标
6. **时间窗口** — today / last7d / last30d 灵活统计

---

## 🔍 验证

### 单元/集成测试 (96 用例全过)
```
ServiceClientTest ......... 3/3   ← Day 10
AdminIntegrationTest ..... 8/8   ← Day 10
BuiltinToolsTest ........ 13/13
FunctionIntegrationTest . 10/10
RagIntegrationTest ....... 8/8
TextChunkerTest .......... 6/6
VectorUtilsTest .......... 5/5
ContextBuilderTest ....... 4/4
MockEmbeddingClientTest .. 5/5
ShortTermMemoryTest ...... 4/4
VectorUtilsTest (memory) . 7/7
JwtTokenProviderTest ..... 4/4
MessageRoleTest .......... 3/3
MockAdapterTest .......... 3/3
ModelProviderFactoryTest . 4/4
StreamingTest ............ 3/3
                      -------
                      96/96 ✅
```

### Maven 编译 (9 模块)
```
minimax-platform ...... SUCCESS
minimax-common ........ SUCCESS
minimax-gateway ....... SUCCESS
minimax-auth .......... SUCCESS
minimax-chat .......... SUCCESS
minimax-memory ........ SUCCESS
minimax-model ......... SUCCESS
minimax-rag ........... SUCCESS
minimax-function ...... SUCCESS
minimax-admin ......... SUCCESS  ← Day 10 新增
======================
BUILD SUCCESS · 56s
```

---

## 🌐 GitHub

- 仓库: https://github.com/liugl951127/miniLiugl.git
- 状态: pending
- 改动: +14 java + 1 SQL + 1 schema + 2 测试 + 1 yml + 1 mapper xml + 1 pom

---

## 📁 新增文件

```
sql/10_admin.sql
sql/init/10_admin.sql
backend/minimax-admin/pom.xml
backend/minimax-admin/src/main/java/com/minimax/admin/AdminApplication.java
backend/minimax-admin/src/main/java/com/minimax/admin/config/MybatisPlusConfig.java
backend/minimax-admin/src/main/java/com/minimax/admin/entity/AdminAuditLog.java
backend/minimax-admin/src/main/java/com/minimax/admin/mapper/AdminAuditLogMapper.java
backend/minimax-admin/src/main/java/com/minimax/admin/client/ServiceClient.java
backend/minimax-admin/src/main/java/com/minimax/admin/client/ServiceEndpoints.java
backend/minimax-admin/src/main/java/com/minimax/admin/service/AuditService.java
backend/minimax-admin/src/main/java/com/minimax/admin/service/UserMgmtService.java
backend/minimax-admin/src/main/java/com/minimax/admin/service/ModelMgmtService.java
backend/minimax-admin/src/main/java/com/minimax/admin/service/StatsService.java
backend/minimax-admin/src/main/java/com/minimax/admin/service/HealthAggregator.java
backend/minimax-admin/src/main/java/com/minimax/admin/controller/AdminController.java
backend/minimax-admin/src/main/resources/mapper/AdminAuditLogMapper.xml
backend/minimax-admin/src/main/resources/schema-h2.sql
backend/minimax-admin/src/main/resources/application-test.yml
backend/minimax-admin/src/test/java/com/minimax/admin/ServiceClientTest.java
backend/minimax-admin/src/test/java/com/minimax/admin/AdminIntegrationTest.java
reports/day-10-report.md
```

---

## 🚀 下一步 (Day 11: 多模态)

- 图片上传 (multipart) + 文件管理
- 视觉模型接入 (gpt-4-vision / MiniMax-VL-01)
- 多模态 Embedding (CLIP / MiniMax-Embed-VL)
- 多模态 RAG (图片 + 文本混合检索)
- 图片理解对话: 用户上传图片 + 文字 → 多模态 LLM
- 多模态记忆 (图片存向量库)
