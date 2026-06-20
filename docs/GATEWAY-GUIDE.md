# MiniMax Gateway 网关使用指南 (V5.5)

## 1. 概述

MiniMax Gateway 是基于 **Spring Cloud Gateway** (响应式 WebFlux) 的统一网关入口，
将 12 个微服务统一通过 `http://localhost:8080/api/v1/{module}/**` 暴露。

### 1.1 架构图

```
                                  ┌─────────────────────────────────┐
                                  │  nginx 端口 3000 (前端静态+反代)  │
                                  └─────────────┬───────────────────┘
                                                │
                                                ▼
                          ┌──────────────────────────────────────┐
                          │  MiniMax Gateway  端口 8080          │
                          │  ─ Spring Cloud Gateway (WebFlux)    │
                          │  ─ JwtAuthGlobalFilter (网关级鉴权)  │
                          │  ─ RequestRateLimiter (Redis 限流)   │
                          │  ─ CORS 统一处理                      │
                          └──────┬───────┬──────┬──────┬────────┘
                                 │       │      │      │
                  ┌──────────────┘       │      │      └──────────────┐
                  ▼                      ▼      ▼                      ▼
            ┌──────────┐           ┌──────────┐ ┌──────────┐    ┌──────────┐
            │ auth     │           │ chat     │ │ model    │    │ ... 12   │
            │ 8081     │           │ 8082     │ │ 8083     │    │ 微服务   │
            └──────────┘           └──────────┘ └──────────┘    └──────────┘
```

### 1.2 12 微服务路由表

| 模块       | 端口 | 路由 (Path)                                              | 限流 (rps) | 鉴权 |
|-----------|------|--------------------------------------------------------|-----------|------|
| auth      | 8081 | /api/v1/auth/**, /api/auth/**                          | 100       | 部分公开 |
| chat      | 8082 | /api/v1/chat/**, /api/v1/sessions/**, /api/v1/messages/** | 50    | 需要 |
| model     | 8083 | /api/v1/model/**, /api/v1/test/**, /api/v1/openai/**   | -         | 需要 |
| memory    | 8084 | /api/v1/memory/**                                       | -         | 需要 |
| rag       | 8085 | /api/v1/rag/**                                          | -         | 需要 |
| function  | 8086 | /api/v1/function/**                                     | -         | 需要 |
| admin     | 8087 | /api/v1/admin/**, /admin/**                             | 20        | SUPER_ADMIN |
| multimodal| 8088 | /api/v1/multimodal/**, /api/v1/multi/**                 | -         | 需要 |
| monitor   | 8089 | /api/v1/monitor/**                                      | -         | 需要 |
| agent     | 8090 | /api/v1/agent/**                                        | 10        | 需要 |
| prompt    | 8091 | /api/v1/prompt/**, /api/v1/prompts/**                   | -         | 需要 |
| ws        | 8095 | /api/v1/ws/**, /ws/**                                  | -         | 需要 |

---

## 2. 启动 Gateway

### 2.1 编译

```bash
cd backend
mvn -B -DskipTests -T 1C -pl minimax-common install
mvn -B -DskipTests -T 1C -pl minimax-gateway package
```

### 2.2 启动

```bash
cd backend/minimax-gateway/target
nohup java -Xmx256m -jar -Dserver.port=8080 minimax-gateway.jar > /workspace/minimax-platform/logs/gateway.log 2>&1 &
```

### 2.3 健康检查

```bash
curl http://localhost:8080/actuator/health           # 总体健康
curl http://localhost:8080/actuator/health/liveness  # 进程存活
curl http://localhost:8080/actuator/info              # 应用信息
```

---

## 3. JWT 网关鉴权

### 3.1 公开路径 (无需 token)

```yaml
minimax.jwt.gateway-auth.public-paths:
  - /api/v1/auth/login        # 登录
  - /api/v1/auth/register     # 注册
  - /api/v1/auth/refresh      # 刷新 token
  - /api/v1/auth/oauth/**     # 跨平台 OAuth
  - /api/v1/auth/wechat/**    # 微信扫码
  - /api/v1/health            # 业务健康
  - /health
  - /v3/api-docs              # OpenAPI
  - /swagger-ui/**
  - /doc.html                 # Swagger UI
  - /actuator/**              # 网关管理
```

### 3.2 鉴权流程

1. 客户端调用 `Authorization: Bearer <token>`
2. `JwtAuthGlobalFilter` 解析 JWT
3. 验证签名 + 有效期
4. 通过则注入下游请求头:
   - `X-User-Id`: 用户 ID
   - `X-User-Name`: 用户名
   - `X-User-Roles`: 角色 (逗号分隔)
5. 业务模块通过 `@RequestHeader("X-User-Id")` 直接获取

### 3.3 错误响应

未带 token / token 过期 / 签名错误统一返回:

```json
{
  "code": 401,
  "message": "未授权",
  "data": null
}
```

---

## 4. Redis 限流

### 4.1 限流策略

- **令牌桶算法** (Token Bucket)
- **Key 维度**: 优先用 `X-User-Id` (登录用户), 未登录回退到 IP

### 4.2 路由限流配置

```yaml
- id: chat
  uri: http://127.0.0.1:8082
  predicates:
    - Path=/api/v1/chat/**
  filters:
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 50     # 每秒 50 个
        redis-rate-limiter.burstCapacity: 100    # 突发 100 个
        key-resolver: "#{@userKeyResolver}"
```

### 4.3 限流响应

超过限流返回 `429 Too Many Requests`.

---

## 5. CORS 跨域

### 5.1 配置

```yaml
spring.cloud.gateway.globalcors.cors-configurations:
  '[/**]':
    allowed-origin-patterns:
      - http://localhost:*
      - http://127.0.0.1:*
      - https://*.your-domain.com
    allowed-methods: "*"
    allowed-headers: "*"
    exposed-headers:
      - X-Trace-Id
      - X-User-Id
    allow-credentials: true
```

### 5.2 实际行为

- 浏览器跨域访问 → 网关自动加 CORS header
- OPTIONS 预检 → 网关直接返回 200

---

## 6. 配置项

### 6.1 application.yml (网关主配置)

| 配置项 | 默认 | 说明 |
|-------|------|------|
| `server.port` | 8080 | 网关端口 |
| `spring.data.redis.host` | 127.0.0.1 | Redis 地址 |
| `spring.data.redis.port` | 6379 | Redis 端口 |
| `minimax.jwt.expire-minutes` | 10080 (7天) | Token 有效期 |
| `minimax.jwt.gateway-auth.enabled` | true | 网关鉴权开关 |

### 6.2 application-common.yml (公共配置)

`backend/minimax-common/src/main/resources/application-common.yml` 包含
所有微服务共享的配置 (server tomcat, jackson, servlet multipart, etc).
Gateway 通过 `spring.config.import: classpath:application-common.yml` 引用.

### 6.3 环境变量

| 变量 | 默认 | 说明 |
|------|------|------|
| `REDIS_HOST` | 127.0.0.1 | Redis 地址 |
| `REDIS_PORT` | 6379 | Redis 端口 |
| `REDIS_PASS` | minimax_redis_2024 | Redis 密码 |
| `UPLOAD_DIR` | ./data/upload | 上传目录 |

---

## 7. 与 nginx (端口 3000) 配合

完整生产架构:

```
用户 → nginx:3000
        ├── /              → 前端 SPA 静态文件
        ├── /api/v1/**     → gateway:8080 (网关鉴权 + 限流)
        └── /ws/**         → ws:8095 (WebSocket)
```

`scripts/nginx-minimax-3000.conf` 关键配置:

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

---

## 8. 故障排查

### 8.1 启动失败

```bash
# 1. 看 yml 是否有重复 key
grep -nE "^[a-z].*:" backend/minimax-gateway/src/main/resources/application.yml

# 2. 看 application-common.yml 是否重复 server 段
grep -n "^server:" backend/minimax-common/src/main/resources/application-common.yml

# 3. 强制重打
mvn -B -DskipTests -pl minimax-common install
mvn -B -DskipTests -pl minimax-gateway clean package
```

### 8.2 路由 404

```bash
# 看路由是否被注册
curl http://localhost:8080/actuator/gateway/routes  # (需要 management.endpoint.gateway.enabled=true)
```

### 8.3 业务模块连不上 (502/500)

```bash
# 检查下游微服务是否启动
ps -ef | grep "minimax-{module}.jar" | grep -v grep
# 检查端口
netstat -lnp | grep {port}
```

### 8.4 限流 429

临时调整 `replenishRate` (每秒) 和 `burstCapacity` (突发).

### 8.5 鉴权 401

```bash
# 解码 JWT 看是否过期
echo "{token}" | cut -d. -f2 | base64 -d | jq .
```

---

## 9. 性能指标

### 9.1 资源占用

- **启动内存**: 200-300MB (`-Xmx256m`)
- **启动时间**: 9-12s
- **QPS (令牌桶)**: 1000+ (取决于下游微服务)
- **延迟**: 5-10ms (路由 + 限流)

### 9.2 关键指标

```bash
# 网关 metrics
curl http://localhost:8080/actuator/metrics

# 路由命中统计
curl http://localhost:8080/actuator/metrics/gateway.requests
```

---

## 10. 升级到 Nacos 服务发现 (可选)

当前用 **path 路由 + 硬编码 URL**, 生产可升级到 **服务发现**:

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
spring.cloud.gateway:
  discovery:
    locator:
      enabled: true
      lower-case-service-id: true
```

然后路由 URI 从 `http://127.0.0.1:8081` 改成 `lb://minimax-auth`.

---

## 11. V5.5 vs V5.3 对比

| 维度 | V5.3 (nginx gateway) | V5.5 (Spring Cloud Gateway) |
|------|---------------------|----------------------------|
| 入口 | nginx 3000 | nginx 3000 → gateway 8080 → 微服务 |
| 鉴权 | 各微服务自检 | **网关级** (一次验证, 注入 X-User-Id) |
| 限流 | 简单 (nginx limit_req) | **Redis 令牌桶** (按用户/IP) |
| CORS | 12 微服务各自配 | **网关统一** (一处配置) |
| 路由 | nginx try_files | **Spring Path** + 动态添加 |
| 监控 | nginx 日志 | **actuator metrics** (Prometheus 可集成) |
| 性能 | 高 (nginx C) | 中 (Java WebFlux, ~5ms 路由延迟) |
| 部署 | 简单 | 多一层 (gateway jar) |

**选择建议**:
- 中小项目 (QPS < 500) → V5.3 (nginx 简单稳定)
- 中大项目 (QPS > 1000, 多团队协作) → **V5.5 (Spring Cloud Gateway 可观测 + 动态路由)**

---

## 12. 相关文件

```
backend/
├── minimax-gateway/
│   ├── pom.xml                                          # Gateway 依赖
│   ├── src/main/java/com/minimax/gateway/
│   │   ├── GatewayApplication.java                      # 入口
│   │   ├── filter/
│   │   │   ├── JwtAuthGlobalFilter.java                 # 网关级 JWT 鉴权
│   │   │   ├── UserKeyResolver.java                     # 用户限流 Key
│   │   │   └── IpKeyResolver.java                       # IP 限流 Key
│   │   └── config/
│   │       └── SecurityConfig.java                      # WebFlux Security
│   └── src/main/resources/
│       ├── application.yml                              # 主配置 (13 routes)
│       └── application-dev.yml                         # 开发环境
└── minimax-common/src/main/resources/
    └── application-common.yml                           # 公共配置

docs/
└── GATEWAY-GUIDE.md                                     # 本文档
```

---

**V5.5 升级完成**: Spring Cloud Gateway 替换原空壳 gateway, 实现网关级 JWT 鉴权 + 限流 + CORS,
统一 12 微服务入口, 前后台分流管理.
