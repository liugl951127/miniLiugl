# V3.5.90+ OpenTelemetry 14 module 全栈接入

## 1. 范围

V3.5.89 只示范 `minimax-auth` 1 个 module 接入 OpenTelemetry. V3.5.90 推到所有 14 module.

## 2. 改动统计

| 维度 | 数量 |
|------|------|
| 后端 module | 14 (admin / agent / ai / analytics / auth / chat / common / gateway / model / monitor / multimodal / pipeline / rag / ws) |
| pom.xml 加 otel 依赖 | 14 (V3.5.89 auth + V3.5.90 13) |
| application*.yml 加 otel 段 | 32 (默认 + mysql + mariadb + dev + intent + standalone + h2local) |
| 父 pom otel-bom 1.36.0 | 已存在 (V3.5.89 之前 import-bom) |
| 父 pom instrumentation-bom 2.6.0 | 已存在 (V3.5.89 之前 import-bom) |

## 3. pom.xml 模板

每个 module (除 common 是库) 加:
```xml
<!-- V3.5.90+ OpenTelemetry trace (W3C traceparent, OTLP exporter) -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

父 pom 已 import-bom 1.36.0 + 2.6.0, 子 module 不用写 `<version>`.

`minimax-common` 也加 (它是库, 多个 module 依赖它), 子 module 继承, 自动有 otel 能力.

## 4. application*.yml 模板

每个 yml 文件末尾加:
```yaml
# V3.5.90+ OpenTelemetry trace (W3C traceparent, OTLP → collector → jaeger)
# jvm-env (docker-compose) 注入 OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
otel:
  metrics:
    export:
      interval: 30s
  java:
    global-autoconfigure:
      enabled: true
```

`global-autoconfigure.enabled: true` 让 Spring Boot 启动时自动配置 OpenTelemetry.

`metrics.export.interval: 30s` 控制 metric 上报间隔, 避免高频.

## 5. 14 module 自动 instrument 范围

`opentelemetry-spring-boot-starter` 自动给以下组件打 trace:
- Spring Web (`@Controller` / `@RestController` / `Filter` / `Interceptor`)
- Spring WebFlux (响应式)
- JDBC (DataSource / PreparedStatement)
- Redis (Lettuce / Jedis)
- HTTP Client (RestTemplate / WebClient / OkHttp)
- gRPC
- Kafka
- RabbitMQ
- R2DBC
- Logback (MDC 注入 trace_id / span_id)

14 module 都会自动获得这些 trace, 无需改业务代码.

## 6. 环境变量 (docker-compose jvm-env anchor)

```yaml
OTEL_SDK_DISABLED: "false"
OTEL_SERVICE_NAME: minimax-platform
OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4317
OTEL_EXPORTER_OTLP_PROTOCOL: grpc
OTEL_TRACES_EXPORTER: otlp
OTEL_METRICS_EXPORTER: otlp
OTEL_LOGS_EXPORTER: otlp
OTEL_RESOURCE_ATTRIBUTES: service.namespace=minimax,deployment.environment=production
```

`OTEL_SERVICE_NAME` 全部 14 module 共用 (统一服务名), 区分靠 `service.instance.id` (默认 hostname 注入).

## 7. 端到端架构

```
[浏览器 5 matrix (V3.5.85)] (chromium/webkit/firefox/mobile-safari/mobile-chrome)
   ↓ sw.js V3.5.89 自动加 W3C traceparent
   ↓ HTTP fetch
[后端 14 module] (OpenTelemetry SDK auto-instrumentation)
   ├─ Spring Web (Controller/Filter)
   ├─ JDBC (MyBatis-Plus 自动打 SQL trace)
   ├─ Redis (Lettuce 自动)
   ├─ HTTP Client (RestTemplate / OpenFeign 跨服务)
   └─ Logback MDC (日志带 trace_id)
   ↓ OTLP gRPC :4317
[OpenTelemetry Collector]
   ↓ batch (5s/1000) + resource
[Jaeger] ← :16686 UI 查 trace
```

## 8. 验证

| 测试 | 结果 |
|------|------|
| 14/14 module pom 有 otel-spring-boot-starter | ✅ |
| 14/14 module pom 有 opentelemetry-exporter-otlp | ✅ |
| 32 application*.yml 有 otel.java.global-autoconfigure.enabled: true | ✅ |
| 父 pom otel-bom 1.36.0 | ✅ V3.5.89 之前已配 |
| 父 pom instrumentation-bom 2.6.0 | ✅ V3.5.89 之前已配 |
| docker-compose jvm-env 9 OTEL 环境变量 | ✅ V3.5.89 |
| sw.js V3.5.89 traceparent 3 handler | ✅ V3.5.89 |
| otel-collector + jaeger 服务 | ✅ V3.5.89 |
| Round 6 90 轮 100% pass | ✅ |
| Round 7 5 browser 全 pass | ✅ |
| Round 8 5 browser traceparent 全 pass | ✅ |

## 9. 累计 45 个版本 (V3.5.46-V3.5.90)
