# MiniMax 分布式追踪指南 (V5.14 OpenTelemetry)

> V5.14 在 V5.8 TraceId 基础上升级 — 从"单点 trace 字符串"到"全链路 OpenTelemetry span"

## 1. 升级路径

```
V5.8                              V5.14
gateway 生成 traceId (16 位)   →  + W3C traceparent 头 (32+16 hex)
logback pattern [traceId=...]  →  + OTel SDK 自动生成 span
                                →  + OTLP 导出到 Jaeger / Tempo
                                →  + 跨服务 span 关联 (parent-child)
```

## 2. 架构

```
┌─────────┐  /api/v1/auth/login     ┌─────────────┐
│ Browser ├───── X-Trace-Id ────────► Spring Cloud│
└─────────┘                          │  Gateway    │
                                     │  :8080      │
                                     │  TraceFilter│
                                     │  注入 traceparent
                                     │  (W3C 32+16)│
                                     └──────┬──────┘
                                            │ lb://minimax-auth
                                            │ + traceparent header
                                            ▼
                                    ┌──────────────┐
                                    │ minimax-auth │
                                    │  :8081       │
                                    │  OTel auto   │
                                    │  instrumentation│
                                    │  检测 traceparent│
                                    │  创建 child span  │
                                    └──────┬───────┘
                                           │ OTel SDK
                                           │ OTLP/HTTP
                                           ▼
                                    ┌──────────────┐
                                    │  Jaeger      │
                                    │  :16686 UI   │
                                    │  :4318  OTLP │
                                    └──────────────┘
```

## 3. 部署

### 3.1 启动 Jaeger (Docker)

```bash
# V5.14: 一行启动 (all-in-one 含 OTLP + UI)
docker run -d --name jaeger \
  -p 16686:16686  \   # Jaeger UI
  -p 4318:4318    \   # OTLP HTTP
  -p 4317:4317    \   # OTLP gRPC
  jaegertracing/all-in-one:latest

# UI: http://localhost:16686
```

### 3.2 启动 MiniMax 服务

无需特殊配置 — OTel starter 自动启用, 默认 OTLP endpoint = `http://localhost:4318`

```bash
# 启动 15 微服务 (V1.9.1)
sudo ./deploy-simple/docker-deploy.sh up
# 或开发模式
mvn spring-boot:run
```

### 3.3 自定义 OTLP endpoint

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://tempo:4318
java -jar minimax-auth.jar
```

## 4. 关键改动 (V5.14)

### 4.1 依赖 (root pom + common pom)

```xml
<!-- root pom: BOM -->
<opentelemetry.version>1.36.0</opentelemetry.version>
<opentelemetry-instrumentation.version>2.2.0</opentelemetry-instrumentation.version>

<!-- common pom -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

### 4.2 配置 (application-common.yml)

```yaml
otel:
  service:
    name: ${spring.application.name}
  exporter:
    otlp:
      protocol: http/protobuf
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318}
  traces:
    sampler: parentbased_traceidratio
    sampler.arg: 1.0   # 生产 0.1 (10% 采样)
  propagation:
    - tracecontext
    - baggage
```

### 4.3 TraceFilter (V5.14 升级)

```java
// V5.8: 只生成 X-Trace-Id (16 位)
// V5.14: 同时生成 W3C traceparent (32+16 hex)
final String traceIdPadded = (traceId + "0000...0000").substring(0, 32);
final String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
final String traceparent = "00-" + traceIdPadded + "-" + spanId + "-01";
exchange.getRequest().mutate()
    .header("traceparent", traceparent)
    .build();
```

**W3C traceparent 格式**:
```
00-{trace-id: 32 hex}-{parent-id: 16 hex}-{flags: 2 hex}
```

## 5. 自动 instrumentation (零代码)

OTel Spring Boot starter 自动检测以下场景并创建 span:

| 场景 | instrumentation |
|------|----------------|
| HTTP Server (servlet) | spring-webmvc |
| HTTP Client | httpclient, okhttp |
| JDBC | jdbc |
| Kafka | kafka |
| RabbitMQ | rabbitmq |
| gRPC | grpc |
| Spring Scheduling | @Scheduled |
| Spring WebFlux | spring-webflux |
| Spring Cloud Gateway | spring-cloud-gateway |

**好处**: 业务代码 0 改动, 13 个服务自动有 span

## 6. 前端 Traces Dashboard (V5.14)

路径: `/admin/traces`

**功能**:
- 服务名 + Trace ID 搜索
- 概览卡片 (Traces / Spans / 平均耗时 / 错误率)
- Trace 列表 (按时间倒序)
- Span 树展开 (层级 + service + 耗时)
- 一键跳转 Jaeger UI
- 10s 自动刷新

**数据来源**: Jaeger Query API (`/api/traces`)

**未部署 Jaeger 时**: 友好提示启动命令

## 7. Jaeger UI 使用

访问 `http://localhost:16686`:

1. **Service**: 选择 `minimax-auth` (或其他微服务)
2. **Lookback**: `Last 1 hour`
3. **Search**: 找到 root span
4. **展开**: 看完整 span 树 (跨服务 + DB + HTTP)
5. **Time**: 火焰图时间轴

## 8. 高级用法

### 8.1 自定义 span

```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MyService {
    @Autowired
    private Tracer tracer;

    public void doWork() {
        Span span = tracer.spanBuilder("do-work").startSpan();
        try (var scope = span.makeCurrent()) {
            span.setAttribute("user.id", "12345");
            // ... 业务逻辑
        } finally {
            span.end();
        }
    }
}
```

### 8.2 自定义事件

```java
span.addEvent("cache-miss", Attributes.of(
    AttributeKey.stringKey("cache.key"), "user:123",
    AttributeKey.longKey("cache.ttl"), 300
));
```

### 8.3 错误标记

```java
span.setStatus(StatusCode.ERROR, "user not found");
span.recordException(new BizException("user not found"));
```

## 9. 与 V5.10 Prometheus 配合

| 数据 | 工具 | 用途 |
|------|------|------|
| 指标 (counter/gauge/histogram) | Prometheus + Grafana | 趋势 + 告警 |
| 追踪 (span tree) | OpenTelemetry + Jaeger | 单请求排查 |

**关联**: Jaeger UI 显示 trace, 通过 trace_id 在 Prometheus 查询:
```promql
sum by (trace_id) (minimax_http_requests_total{uri="/api/v1/auth/login"})
```

## 10. V5.14 新增文件

| 文件 | 用途 |
|------|------|
| `backend/pom.xml` | OTel BOM (1.36.0 + 2.2.0) |
| `backend/minimax-common/pom.xml` | OTel Spring Boot starter + OTLP exporter |
| `backend/minimax-common/src/main/resources/application-common.yml` | otel.* 配置 (13 模块自动继承) |
| `backend/minimax-gateway/src/main/java/com/minimax/gateway/filter/TraceFilter.java` | V5.14 加 W3C traceparent |
| `frontend/src/views/admin/Traces.vue` | 前端 Traces Dashboard (7.7KB) |
| `frontend/src/router/index.js` | `/admin/traces` 路由 |
| `docs/TRACES-GUIDE.md` | 本文档 |
