# Docker Compose 一键部署指南 (V3.5.3+)

> **面向企业级生产部署** · 17 微服务 + 3 基础设施 + Nginx · 单命令启动

---

## 📋 服务清单

| 类别 | 服务 | 端口 | 镜像 |
|------|------|------|------|
| **基础设施** | mariadb | 3306 | mariadb:10.11 |
| | redis | 6379 | redis:7-alpine |
| | nacos | 8848/9848 | nacos/nacos-server:v2.3.2 |
| **微服务 (17)** | minimax-gateway | 7080 | minimax/gateway:1.0 |
| | minimax-auth | 8081 | minimax/auth:1.0 |
| | minimax-chat | 8082 | minimax/chat:1.0 |
| | minimax-memory | 8083 | minimax/memory:1.0 |
| | minimax-model | 8084 | minimax/model:1.0 |
| | minimax-rag | 8085 | minimax/rag:1.0 |
| | minimax-function | 8086 | minimax/function:1.0 |
| | minimax-multimodal | 8087 | minimax/multimodal:1.0 |
| | minimax-agent | 8088 | minimax/agent:1.0 |
| | minimax-monitor | 8089 | minimax/monitor:1.0 |
| | minimax-admin | 8090 | minimax/admin:1.0 |
| | minimax-prompt | 8091 | minimax/prompt:1.0 |
| | minimax-analytics | 8092 | minimax/analytics:1.0 |
| | minimax-pipeline | 8093 | minimax/pipeline:1.0 |
| | minimax-ai | 8094 | minimax/ai:1.0 |
| | minimax-ws | 8095 | minimax/ws:1.0 |
| **反向代理** | nginx | 80/443 | nginx:1.25-alpine |

**总计: 21 个服务, 19 个对外端口, 1 个 Docker bridge 网络**

---

## 🚀 一键部署

```bash
# 1. 上传项目代码
scp -r minimax-ai-platform.tar.gz root@your-server:/root/

# 2. 解压
tar -xzf minimax-ai-platform.tar.gz
cd minimax-ai-platform

# 3. 启动
./deploy.sh up
# 等价于:
#   docker compose build
#   docker compose up -d

# 4. 验证
./deploy.sh status
```

启动后访问:
- 前端: http://your-server/  (经 nginx 80 端口)
- Gateway: http://your-server:7080/
- Nacos: http://your-server:8848/nacos/  (nacos/nacos)

---

## 🔧 常用命令

```bash
./deploy.sh up         # 启动 (build + up -d)
./deploy.sh down       # 停止
./deploy.sh build      # 重新构建镜像
./deploy.sh status     # 状态 + 端口健康检查
./deploy.sh logs                    # 所有服务日志
./deploy.sh logs minimax-ai         # 单个服务日志
./deploy.sh restart minimax-ai     # 重启单个服务
```

---

## 🏗️ Dockerfile 模板

所有微服务用同一个 `Dockerfile.module` (multi-stage):

```bash
# 阶段 1: 构建
FROM maven:3.9-eclipse-temurin-17 AS builder
ARG MODULE=minimax-gateway
WORKDIR /build
COPY backend/pom.xml backend/.mvn ./
COPY backend/$MODULE ./backend/$MODULE
RUN cd backend && mvn -s .mvn/settings.xml package \
    -pl $MODULE -am -DskipTests -B -T 4

# 阶段 2: 运行时
FROM eclipse-temurin:17-jre-alpine
ARG MODULE=minimax-gateway
ENV MODULE_NAME=$MODULE
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=70.0 -Dfile.encoding=UTF-8"
ENV TZ=Asia/Shanghai

# 健康检查
RUN apk add --no-cache curl tini && \
    mkdir -p /app/logs /app/data

# 复制构建产物
COPY --from=builder /build/backend/$MODULE/target/$MODULE-spring-boot.jar /app/app.jar

# 启动
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["/sbin/tini", "--", "sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
```

构建示例:
```bash
docker build --build-arg MODULE=minimax-ai -t minimax/ai:1.0 .
```

---

## 🌐 端口规划

| 端口 | 服务 | 备注 |
|------|------|------|
| 80 | nginx | 唯一对外 HTTP |
| 443 | nginx | HTTPS (需挂证书) |
| 3306 | mariadb | 数据库 |
| 6379 | redis | 缓存 |
| 7080 | gateway | 内部 API 网关 |
| 8848 | nacos | 服务发现 + 配置 |
| 8081-8095 | 15 微服务 | 直连端口 (仅内网) |

**生产建议**: 8081-8095 不对外, 全部走 nginx 80 端口.

---

## 🛢️ 数据库初始化

`./sql/complete.sql` 自动挂载到 mariadb `/docker-entrypoint-initdb.d/`:
- 87 张表
- 59 条种子数据 (含超管 admin/admin123)
- 单文件, 启动时自动导入

如需重置:
```bash
docker compose down -v  # 删除 volume
./deploy.sh up
```

---

## 🔍 验证清单

启动后跑:
```bash
./deploy.sh status  # 21 个服务 + 端口
curl http://localhost/actuator/health  # 经 nginx
```

应看到:
- 21 个 container Up
- 19 个端口 OK
- nginx 200

---

## 📊 资源规划

| 资源 | 最小 | 推荐 |
|------|------|------|
| CPU | 8 核 | 16 核 |
| RAM | 16 GB | 32 GB |
| Disk | 50 GB | 100 GB SSD |
| Network | 100 Mbps | 1 Gbps |

每个 Spring Boot 服务默认 384MB, 17 个 ≈ 6.5GB. + 基础设施 1GB. + OS 2GB. 建议 16GB 起步.

---

## 🆚 沙箱 vs 生产

| 模式 | 沙箱 (V3.5.2+) | 生产 (本指南) |
|------|--------------|--------------|
| 数据库 | H2 内存 | MariaDB 10.11 |
| 缓存 | 禁用 (timeout 50ms) | Redis 7 |
| 配置 | -D 参数 | Nacos 2.3 |
| 启动 | `java -jar --spring.profiles.active=h2local` | `./deploy.sh up` |
| 数据 | 重启丢失 | 持久化 volume |
| 网络 | 本机端口 | Docker bridge + 主机端口 |

---

## 🛠️ 故障排查

```bash
# 1. 看容器日志
docker compose logs -f minimax-ai

# 2. 进容器调试
docker compose exec minimax-ai sh

# 3. 查表
docker compose exec mariadb mariadb -uroot -proot123456 minimax_platform \
    -e "SHOW TABLES"

# 4. 单独重启某服务
./deploy.sh restart minimax-auth

# 5. 完全重置 (删数据!)
docker compose down -v
./deploy.sh up
```

---

## 📦 镜像推送 (生产前)

```bash
# 给所有镜像打 tag
for svc in common gateway auth chat memory model rag function multimodal agent monitor admin prompt analytics pipeline ai ws; do
    docker tag minimax/$svc:1.0 registry.minimax.ai/minimax-platform/$svc:1.0
done

# 推送
docker push registry.minimax.ai/minimax-platform/minimax-ai:1.0
# ...
```

---

**版本**: V3.5.3+ · **更新**: 2026-07-13 · **作者**: MiniMax Team
