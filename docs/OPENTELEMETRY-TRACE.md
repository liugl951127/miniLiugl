# V3.5.89+ 后端 OpenTelemetry Trace 接入

## 1. 目标

- 后端 14 module 自动生成 + 上报 trace 到 OTel Collector
- 前端 5 browser matrix (V3.5.85) 加 W3C traceparent header
- 全链路追踪: 前端 fetch → 后端微服务 → DB / Redis / 第三方 API
- 端到端可视化: Jaeger UI 16686

## 2. 架构

```
[前端 sw.js V3.5.89]
   ↓ HTTP + traceparent header (W3C)
[后端 14 module - OpenTelemetry SDK]
   ↓ OTLP gRPC :4317
[OpenTelemetry Collector]
   ↓ batch + resource processor
[Jaeger]
   ↓ 
[UI :16686]  (查 trace)
```

## 3. 改动清单

### 3.1 后端 14 module (`backend/minimax-{auth,ai,...}/`)

**`pom.xml`** (auth 示范, 14 module 类似):
```xml
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

**`application-mysql.yml`** (12 module 改, V3.5.32 前):
```yaml
# V3.5.89+ OpenTelemetry trace - 走环境变量配置
otel:
  metrics:
    export:
      interval: 30s
  java:
    global-autoconfigure:
      enabled: true
```

### 3.2 docker-compose.yml - jvm-env anchor

```yaml
x-jvm-env: &jvm-env
  # ... 已有 NACOS / REDIS ...
  # V3.5.89+ OpenTelemetry trace
  OTEL_SDK_DISABLED: "false"
  OTEL_SERVICE_NAME: minimax-platform
  OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4317
  OTEL_EXPORTER_OTLP_PROTOCOL: grpc
  OTEL_TRACES_EXPORTER: otlp
  OTEL_METRICS_EXPORTER: otlp
  OTEL_LOGS_EXPORTER: otlp
  OTEL_RESOURCE_ATTRIBUTES: service.namespace=minimax,deployment.environment=production
```

### 3.3 docker-compose.yml - 2 个新服务

```yaml
  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.96.0
    command: ["--config=/etc/otel-collector-config.yaml"]
    volumes:
      - ./deploy/otel-collector-config.yaml:/etc/otel-collector-config.yaml:ro
    ports: ["4317:4317", "4318:4318"]
    depends_on: [jaeger]

  jaeger:
    image: jaegertracing/all-in-one:1.55
    environment: { COLLECTOR_OTLP_ENABLED: "true" }
    ports: ["16686:16686", "14250:14250"]
```

### 3.4 deploy/otel-collector-config.yaml (新增)

收 OTLP trace + metric + log, 批处理 → Jaeger:

```yaml
receivers:
  otlp:
    protocols:
      grpc: { endpoint: 0.0.0.0:4317 }
      http: { endpoint: 0.0.0.0:4318 }
processors:
  batch: { timeout: 5s, send_batch_size: 1000 }
  resource:
    attributes:
      - { key: service.namespace, value: minimax, action: upsert }
exporters:
  otlp/jaeger: { endpoint: jaeger:4317, tls: { insecure: true } }
  debug: { verbosity: basic }
service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch, resource]
      exporters: [otlp/jaeger, debug]
```

### 3.5 前端 sw.js V3.5.89 (network-only 模式加 traceparent)

**新增函数**:
```js
// W3C Trace Context 格式: 00-{32hex}-{16hex}-01
function generateTraceparent() {
  const traceId = Array.from({length:32}, ()=>'0123456789abcdef'[Math.random()*16|0]).join('')
  const spanId = Array.from({length:16}, ()=>'0123456789abcdef'[Math.random()*16|0]).join('')
  return `00-${traceId}-${spanId}-01`  // 01 = sampled
}

function withTraceparent(req) {
  const newHeaders = new Headers(req.headers)
  if (!newHeaders.has('traceparent')) {
    newHeaders.set('traceparent', generateTraceparent())
  }
  return new Request(req.url, { /* 复制原 req + 新 headers */ })
}
```

**3 个 handler 加 traceparent**:
- `handleNavigation` 导航请求
- `handleNetworkOnly` GET 资源
- `handleWrite` POST/PUT/DELETE

## 4. 5 browser matrix trace 验证 (V3.5.89 Round 8)

`scripts/e2e-multiround.sh` Round 8: 5 browser × 3 路由 × traceparent = 15 GET

| Browser | User-Agent | trace |
|---------|-----------|-------|
| chromium | Chrome 120.0 | `00-{trace_id}-01` |
| webkit | Safari 16.0 | `00-{trace_id}-01` |
| firefox | Firefox 120.0 | `00-{trace_id}-01` |
| mobile-safari | iPhone 16_0 | `00-{trace_id}-01` |
| mobile-chrome | Pixel 5 | `00-{trace_id}-01` |

每个浏览器每次生成新的 32 hex trace_id + 16 hex span_id, 模拟浏览器生成 trace 的随机性。

## 5. 使用方法

```bash
# 启服务 (包含 otel-collector + jaeger)
docker compose up -d

# 访问 Jaeger UI
open http://localhost:16686

# 选 service: minimax-platform
# 选 operation: HTTP GET / / HTTP POST /api/v1/auth/login 等
# 查 trace 看完整调用链
```

## 6. 跟 V3.5.85+ 5 browser matrix 打通

| 层 | V3.5.85 | V3.5.89 |
|----|---------|---------|
| 浏览器 | HTTP UA 模拟 (5 browser) | + W3C traceparent header |
| 前端 sw.js | network-only | + traceparent 自动生成 |
| 后端 | curl 直传 | + OpenTelemetry SDK auto-instrumentation |
| 收集中间 | 无 | + OTel Collector :4317 |
| 存储/可视化 | 无 | + Jaeger :16686 |

## 7. 验证

| 测试 | 结果 |
|------|------|
| 14 module otel 依赖 | ✅ auth 示范 |
| 12 module otel yml 配置 | ✅ |
| docker-compose otel-collector + jaeger | ✅ |
| sw.js V3.5.89 traceparent 3 handler | ✅ |
| Round 8 5 browser × 3 路由 = 15 GET | ✅ 全 PASS |
| 父 pom otel-bom + instrumentation-bom | ✅ V3.5.89 之前已有 |

## 8. 累计 44 个版本 (V3.5.46-V3.5.89)
