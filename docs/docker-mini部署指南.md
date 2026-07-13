# MiniMax Platform 精简部署指南 (V3.5.5+)

## 两种部署模式

| 模式 | 容器数 | 内存 | 适用环境 | 启动时间 |
|------|--------|------|----------|----------|
| **精简版 (mini)** | 6 | ~1.5GB | 沙箱 / 树莓派 / 2GB VPS / 开发 | 60s |
| **完整版 (full)** | 21 | ~6GB | 8GB+ VPS / 生产环境 | 180s |

## 精简版 6 容器

```
┌─────────────────────────────────────────┐
│  Nginx :80                              │
│  ├─ /                  → 前端 dist      │
│  ├─ /api/auth/         → auth :8081     │
│  ├─ /api/v1/ai/        → ai :8094       │
│  └─ /api/              → gateway :7080  │
└─────────────────────────────────────────┘
         │
         ├─ MariaDB :3306 (89 表 + 59 INSERT)
         └─ Redis :6379
```

**包含的功能**:
- ✅ 用户登录 / 鉴权 (auth)
- ✅ AI 核心: 知识库 / 意图预测 / 智能体群 / Raft / 推送 / License / Spark
- ✅ Nginx 反代 + 前端
- ✅ MariaDB + Redis 基础设施

**未包含的 12 个服务** (按需启动):
- chat / memory / model / rag / function / multimodal
- agent / monitor / admin / prompt / analytics / pipeline

## 快速开始

### 一键启动

```bash
# 1. 进入项目根目录
cd /path/to/miniLiugl

# 2. 启动 (首次会 build 镜像, 约 5 分钟)
./deploy-mini.sh up

# 3. 查看状态
./deploy-mini.sh status

# 4. 查看日志
./deploy-mini.sh logs

# 5. 停止
./deploy-mini.sh down
```

### 手动启动

```bash
docker compose -f docker-compose.mini.yml up -d --build
```

### 访问入口

| 入口 | URL |
|------|-----|
| 前端 | http://<server-ip>/ |
| 网关 | http://<server-ip>:7080 |
| 鉴权 | http://<server-ip>:8081/actuator/health |
| AI | http://<server-ip>:8094/actuator/health |
| 健康检查 | http://<server-ip>/healthz |

默认账号: `adminLiugl / Liugl@2026`

## 扩展: 按需添加服务

在 `docker-compose.mini.yml` 里追加其他模块, 例如 `chat`:

```yaml
  chat:
    build:
      context: ./backend
      dockerfile: Dockerfile
      args:
        MODULE: chat
    image: minimax-chat:latest
    container_name: minimax-chat
    restart: unless-stopped
    depends_on:
      mariadb:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      SERVER_PORT: 8082
      SPRING_PROFILES_ACTIVE: mysql
      MYSQL_HOST: mariadb
      MYSQL_PORT: 3306
      MYSQL_DB: minimax_platform
      MYSQL_USER: root
      MYSQL_PASSWORD: root123456
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASS: minimax_redis_2024
      JAVA_OPTS: "-Xms192m -Xmx384m -XX:+UseG1GC -XX:MaxRAMPercentage=70"
    ports:
      - "8082:8082"
```

## 升级到完整版

```bash
# 停止精简版
./deploy-mini.sh down

# 启动完整版 (需要 8GB+ 内存)
./deploy.sh up
```

## 常见问题

### Q: AI 服务启动很慢?
A: 首次启动需要加载 Transformer 模型 (vocab=8192, hidden=128) + 训练数据,
约 30-60 秒。后续启动会快很多 (有 JVM 缓存)。

### Q: 端口 80 被占用?
A: 修改 `docker-compose.mini.yml` 的 `nginx.ports`, 例如 `"8080:80"`。

### Q: 内存不够 OOM?
A: 调小 `JAVA_OPTS`:
- auth: `-Xmx256m`
- ai: `-Xmx512m`
- gateway: `-Xmx256m`

### Q: MariaDB 初始化失败?
A: 删除 volume 重新初始化:
```bash
docker compose -f docker-compose.mini.yml down -v
./deploy-mini.sh up
```
