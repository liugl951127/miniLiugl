# MiniMax Platform - Prometheus Metrics 指南 (V5.10)

> 本指南说明 V5.10 引入的可观测性能力 — Prometheus + Grafana 接入路径

## 1. 架构总览

```
┌─────────┐         ┌────────────────────┐         ┌─────────────┐
│ nginx   │ ──────► │ Spring Cloud       │ ──────► │ minimax-*   │
│ :3000   │  /api/  │ Gateway (WebFlux)  │  lb://   │ microservice│
└─────────┘         │ :8080              │         │ :8081-8095  │
                    └─────────┬──────────┘         └──────┬──────┘
                              │                          │
                              │ /actuator/prometheus     │ /actuator/prometheus
                              ▼                          ▼
                    ┌─────────────────────────────────────────┐
                    │ Prometheus Server (10s scrape)          │
                    └─────────────────┬───────────────────────┘
                                      │
                                      ▼
                    ┌─────────────────────────────────────────┐
                    │ Grafana Dashboard                       │
                    │   - HTTP Request Rate / 4xx / 5xx       │
                    │   - HTTP Duration p50/p95/p99           │
                    │   - LLM Latency                         │
                    │   - 业务指标: chat/tool/rag/llm_tokens  │
                    └─────────────────────────────────────────┘
```

## 2. 自动采集的指标

### 2.1 HTTP 层 (公共, 通过 `MetricsFilter`)

| 指标 | 类型 | 标签 | 说明 |
|------|------|------|------|
| `minimax.http.requests.total` | Counter | method, uri, status | HTTP 请求计数 |
| `minimax.http.requests.duration` | Timer | method, uri, status | 请求耗时 (含 p50/p95/p99) |
| `minimax.http.4xx.errors.total` | Counter | method, uri, status | 4xx 错误数 |
| `minimax.http.5xx.errors.total` | Counter | method, uri, status | 5xx 错误数 |

**URI 归一化**: `/api/v1/user/123` → `/api/v1/user/{id}` (避免高基数标签)

### 2.2 业务层 (minimax-monitor 模块, 通过 `MetricsCollector`)

| 指标 | 类型 | 说明 |
|------|------|------|
| `minimax.chat.messages.total` | Counter | 聊天消息数 |
| `minimax.tool.calls.total` | Counter | 工具调用数 |
| `minimax.rag.queries.total` | Counter | RAG 查询数 |
| `minimax.llm.tokens.total` | Counter | LLM token 用量 |
| `minimax.http.5xx.total` | Counter | 5xx 错误 |
| `minimax.http.4xx.total` | Counter | 4xx 错误 |
| `minimax.uploads.total` | Counter | 文件上传数 |
| `minimax.active.sessions` | Gauge | 活跃会话 |
| `minimax.kb.count` | Gauge | 知识库数 |
| `minimax.user.count` | Gauge | 用户数 |
| `minimax.memory.count` | Gauge | 记忆数 |
| `minimax.llm.latency` | Timer (histogram) | LLM 调用延迟 |
| `minimax.tool.duration` | Timer (tag: tool) | 工具调用延迟 |

### 2.3 自动 JVM 指标 (Spring Boot Actuator 内置)

| 指标 | 说明 |
|------|------|
| `jvm.memory.used` | JVM 堆/非堆内存 |
| `jvm.gc.pause` | GC 暂停时间 |
| `jvm.threads.live` | 活跃线程数 |
| `process.cpu.usage` | CPU 使用率 |
| `hikaricp.connections.active` | DB 连接池 |

## 3. 暴露的端点

每个微服务 (含 gateway) 自动暴露:
- `GET /actuator/health` — 健康检查
- `GET /actuator/metrics` — JSON 格式 metrics 索引
- `GET /actuator/prometheus` — **Prometheus 文本格式** (主要抓取点)
- `GET /actuator/info` — 服务信息

**鉴权**: 已在 `SecurityConfig` 白名单 (`/actuator/**`), 无需 JWT

## 4. 前端 Metrics Dashboard (V5.10)

路径: `/admin/metrics` (在 admin 菜单下)

**功能**:
- 服务选择下拉 (12 微服务 + gateway)
- 概览卡片: 总请求 / 4xx / 5xx / 平均延迟
- Top 10 高频 URI (表格 + 进度条占比)
- Top 10 慢 URI (平均耗时)
- 状态码分布 (饼图)
- Top 5 慢 URI (横向柱图)
- 10s 自动刷新 (可关闭)
- 折叠的原始 Prometheus 文本 (调试)

**数据来源**: 通过 monitor 模块的转发端点 `GET /monitor/forward-prometheus?service=xxx`

## 5. Prometheus 部署示例

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'minimax-platform'
    scrape_interval: 10s
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - '127.0.0.1:8080'   # gateway
          - '127.0.0.1:8081'   # auth
          - '127.0.0.1:8082'   # chat
          # ... 12 microservice ports
        labels:
          environment: 'dev'
```

## 6. 常用 PromQL 示例

```promql
# QPS (每秒请求数)
sum(rate(minimax_http_requests_total[1m]))

# 错误率
sum(rate(minimax_http_5xx_errors_total[5m])) / sum(rate(minimax_http_requests_total[5m]))

# p95 延迟
histogram_quantile(0.95, sum(rate(minimax_http_requests_duration_seconds_bucket[5m])) by (le, uri))

# Top 5 慢 URI
topk(5, histogram_quantile(0.95, sum(rate(minimax_http_requests_duration_seconds_bucket[5m])) by (le, uri)))
```

## 7. Grafana 面板建议

| 面板 | 类型 | PromQL |
|------|------|--------|
| HTTP QPS by Service | Time Series | `sum by (application) (rate(minimax_http_requests_total[1m]))` |
| HTTP p95 Latency | Time Series | `histogram_quantile(0.95, ...)` |
| Error Rate | Stat / Gauge | `sum(rate(minimax_http_5xx_errors_total[5m])) / sum(rate(minimax_http_requests_total[5m]))` |
| LLM Token 用量 | Bar Gauge | `sum by (application) (increase(minimax_llm_tokens_total[1h]))` |
| JVM Heap | Time Series | `sum by (application) (jvm_memory_used_bytes{area="heap"})` |
| DB 连接池 | Time Series | `hikaricp_connections_active` |

## 8. V5.10 新增文件

| 文件 | 用途 |
|------|------|
| `backend/minimax-common/src/main/java/com/minimax/common/web/MetricsFilter.java` | HTTP 通用指标自动拦截 |
| `backend/minimax-common/src/main/resources/application-common.yml` | 公共 management 配置 (Prometheus 启用) |
| `backend/minimax-common/pom.xml` | 加 spring-boot-starter-actuator + micrometer-registry-prometheus 依赖 |
| `backend/minimax-monitor/src/main/java/com/minimax/monitor/client/ServiceEndpoints.java` | 跨服务 URL 解析 (for 转发) |
| `backend/minimax-monitor/src/main/java/com/minimax/monitor/controller/MonitorController.java` | 新增 `/monitor/forward-prometheus` |
| `backend/minimax-model/src/main/java/com/minimax/model/controller/ProviderController.java` | V5.10 BaseController 模式落地演示 |
| `frontend/src/views/admin/Metrics.vue` | 前端实时 Metrics Dashboard |
| `frontend/src/router/index.js` | 加 `/admin/metrics` 路由 |
