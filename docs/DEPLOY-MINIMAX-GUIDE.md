# MiniMax V5.22 - Linux 一键部署指南 (生产可用)

> **架构**: Docker 中间件 + systemd 微服务 + nginx 统一入口

## 📋 部署架构

```
                            ┌─────────────────────────────────┐
                            │     nginx :3000 (统一入口)        │
                            │   /api → gateway :8080            │
                            │   /actuator → gateway :8080       │
                            │   /ws/bidi → ws :8095             │
                            │   / → 静态文件 / API 文档聚合      │
                            └────────────┬────────────────────┘
                                         │
                            ┌────────────▼────────────┐
                            │   Gateway :8080         │
                            │   (Spring Cloud Gateway) │
                            │   13 routes + JwtAuth   │
                            └────────────┬────────────┘
                                         │ lb://minimax-*
                  ┌──────────────────────┼──────────────────────┐
                  ▼                      ▼                      ▼
          12 个微服务 (8081-8095)    Nacos :8848 (Docker)    监控 (可选)
            - auth:8081              MariaDB :3306 (Docker)   Prometheus
            - chat:8082              Redis :6379 (Docker)      Grafana
            - model:8083             Adminer :8082 (Docker)    Jaeger
            - memory:8084                                     ES + Kibana
            - rag:8085
            - function:8086
            - admin:8087
            - multimodal:8088
            - monitor:8089
            - agent:8090
            - prompt:8091
            - ws:8095
```

## 🚀 一行命令 (推荐)

```bash
curl -fsSL https://raw.githubusercontent.com/liugl951127/miniLiugl/main/scripts/deploy-minimax.sh -o deploy-minimax.sh
chmod +x deploy-minimax.sh
sudo ./deploy-minimax.sh install
```

完成后:
- **前端**: http://your-server-ip:3000
- **API 文档**: http://your-server-ip:3000/api-docs
- **Nacos**: http://your-server-ip:8848/nacos (nacos/nacos)
- **Adminer**: http://your-server-ip:8082 (minimax/minimax_pass_2024)
- **监控** (需 `--profile monitoring`): http://your-server-ip:3001
- **追踪** (需 `--profile tracing`): http://your-server-ip:16686

账号: `adminLiugl` / `Liugl@2026`

## 🎯 9 个子命令

```bash
sudo ./deploy-minimax.sh install      # 一键安装 (中间件+微服务+nginx)
sudo ./deploy-minimax.sh start        # 启动所有服务
sudo ./deploy-minimax.sh stop         # 停止所有服务
sudo ./deploy-minimax.sh restart      # 重启
sudo ./deploy-minimax.sh status       # 状态
sudo ./deploy-minimax.sh test         # E2E 健康检查 (19 项)
sudo ./deploy-minimax.sh uninstall    # 卸载 (保留数据)
sudo ./deploy-minimax.sh logs auth    # 查看日志
sudo ./deploy-minimax.sh help         # 帮助
```

## 📦 install 步骤详解 (7 步)

| 步骤 | 内容 | 耗时 |
|------|------|------|
| 1. 检查依赖 | 装 Docker + Compose + Java 17 + nginx | 1-3 分钟 |
| 2. 创建用户 | minimax 用户 (UID 999, no login) | < 1s |
| 3. 启动中间件 | MariaDB + Redis + Nacos + Adminer (docker) | 1-2 分钟 |
| 4. SQL 导入 | docker 自动执行 sql/init/init-minimax.sql | 30s |
| 5. 拷贝 jar | 12 微服务 + gateway → /opt/minimax/apps/ | < 5s |
| 6. systemd | 生成 13 个 service 文件 | < 1s |
| 7. nginx | 部署反代配置 (含 gzip / WS / api-docs) | < 5s |
| 8. 启动顺序 | mariadb → nacos → gateway → 12 微服务 → nginx | 30s |

## 🌐 网络拓扑

### 容器 ↔ 宿主

| 服务 | 容器端口 | 宿主端口 | 通信 |
|------|---------|---------|------|
| mariadb | 3306 | 127.0.0.1:3306 | 宿主微服务 → 127.0.0.1:3306 |
| redis | 6379 | 127.0.0.1:6379 | 同上 |
| nacos | 8848 | 127.0.0.1:8848 | 宿主微服务 → 127.0.0.1:8848 |
| adminer | 8080 (内) | 127.0.0.1:8082 | 浏览器直连 |

### 12 微服务 → 中间件

每个微服务的 application.yml 通过 **127.0.0.1** 连接:
- `spring.datasource.url=jdbc:mariadb://127.0.0.1:3306/minimax_platform`
- `spring.redis.host=127.0.0.1`
- `spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848`

(由 systemd 的 `Environment` 参数注入, 见脚本 `step_generate_systemd`)

## 🛠️ 故障排查

### 1. install 卡在 "Docker 启动 MariaDB"

```bash
# 检查 docker
systemctl status docker
docker ps

# 手动试 mariadb
cd /path/to/minimax-platform
docker compose up -d mariadb
docker logs minimax-mariadb
```

### 2. SQL 导入失败

```bash
# 看 docker 启动日志
docker logs minimax-mariadb 2>&1 | tail -20

# 手动重导入
docker exec -i minimax-mariadb mysql -uroot -p"minimax_root_2024" < sql/init-minimax.sql
```

### 3. Nacos 起不来 (端口 8848 占用)

```bash
# 看端口
lsof -i :8848
ss -tlnp | grep 8848

# 改 Nacos 端口 (docker-compose.yml)
sed -i 's/8848:8848/18848:8848/g' docker-compose.yml
```

### 4. 微服务起不来

```bash
# 看日志
sudo tail -f /var/log/minimax/auth.log

# 看 java jar 是否存在
ls -la /opt/minimax/apps/

# 看 systemd 状态
systemctl status minimax-auth
journalctl -u minimax-auth -n 50
```

### 5. 502 Bad Gateway (nginx → gateway)

```bash
# gateway 是否在跑
systemctl status minimax-gateway

# 端口监听
ss -tlnp | grep 8080

# 重启 gateway
systemctl restart minimax-gateway
```

## 🔄 升级

```bash
# 1. 备份
sudo ./deploy-minimax.sh backup

# 2. 拉新代码
git pull

# 3. 重新编译
cd backend && mvn clean install -DskipTests

# 4. 重装 jar
sudo cp backend/minimax-{auth,chat,...}/target/*.jar /opt/minimax/apps/
sudo chown minimax:minimax /opt/minimax/apps/*.jar

# 5. 重启
sudo ./deploy-minimax.sh restart
```

## 🗑️ 卸载

```bash
# 保留数据: 数据库 / Redis / 日志
sudo ./deploy-minimax.sh uninstall

# 完全删除
sudo rm -rf /opt/minimax /var/log/minimax
cd /path/to/source && docker compose down -v
```

## 📊 资源占用

| 组件 | 内存 | CPU | 磁盘 |
|------|------|-----|------|
| MariaDB | 256MB | 0.5 | 1GB |
| Redis | 64MB | 0.2 | 100MB |
| Nacos | 512MB | 0.5 | 500MB |
| Adminer | 16MB | 0.1 | 10MB |
| Gateway | 256MB | 0.3 | 200MB |
| 12 微服务 | 256MB × 12 = 3GB | 0.3 × 12 | 200MB × 12 |
| nginx | 16MB | 0.1 | 50MB |
| **总计** | **~4.2GB** | **~5** | **~5GB** |

推荐: **8GB RAM / 4 CPU / 20GB 磁盘** 生产环境

## 🔐 默认凭证 (生产请改)

| 服务 | 账号 | 密码 |
|------|------|------|
| Adminer | minimax | minimax_pass_2024 |
| Nacos | nacos | nacos |
| Web 后台 | adminLiugl | Liugl@2026 |
| Redis | - | minimax_redis_2024 |
| MariaDB | minimax | minimax_pass_2024 |
| MariaDB (root) | root | minimax_root_2024 |

通过环境变量改: `DB_PASS=xxx REDIS_PASS=yyy sudo ./deploy-minimax.sh install`

## 📚 相关文档

- [DEVELOPER-GUIDE.md](DEVELOPER-GUIDE.md) - 开发者指南
- [DEPLOY-README.md](../scripts/DEPLOY-README.md) - 旧版部署脚本
- [ARCHITECTURE.md](ARCHITECTURE.md) - 架构文档
- [INFRA-DOCKER-GUIDE.md](INFRA-DOCKER-GUIDE.md) - Docker 详解
