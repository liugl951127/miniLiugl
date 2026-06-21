# MiniMax 全栈中间件 Docker Compose 指南 (V5.20)

> V5.20 重写 docker-compose.yml — 一行启动所有中间件, 含可选 profile 分组

## 1. 一行启动

```bash
# 必选 (MariaDB + Redis + Nacos + Adminer)
docker compose up -d

# + 监控 (Prometheus + Grafana)
docker compose --profile monitoring up -d

# + 追踪 (Jaeger)
docker compose --profile tracing up -d

# + 搜索 (ES + Kibana)
docker compose --profile search up -d

# 全部
docker compose --profile monitoring --profile tracing --profile search up -d
```

## 2. 中间件清单

### 必选 (default profile)

| 服务 | 镜像 | 端口 | 用途 | V5.x 集成 |
|------|------|------|------|-----------|
| **MariaDB 10.5** | `mariadb:10.5` | 3306 | 关系数据库 | 12 微服务共用 |
| **Redis 7.2** | `redis:7.2-alpine` | 6379 | 限流/缓存/短期记忆 | Bucket4j + V5.12 |
| **Nacos 2.3.2** | `nacos/nacos-server:v2.3.2` | 8848, 9848 | 服务发现 + 配置中心 | V5.7 lb:// |
| **Adminer** | `adminer:latest` | 8082 | Web DB 管理界面 | 开发调试 |

### 可选 profile=monitoring

| 服务 | 镜像 | 端口 | 用途 |
|------|------|------|------|
| **Prometheus 2.50** | `prom/prometheus:v2.50.0` | 9090 | 指标抓取 |
| **Grafana 10.2** | `grafana/grafana:10.2.2` | 3001 | Dashboard 可视化 |

### 可选 profile=tracing

| 服务 | 镜像 | 端口 | 用途 |
|------|------|------|------|
| **Jaeger 1.55** | `jaegertracing/all-in-one:1.55` | 16686, 4317, 4318 | 分布式追踪 UI + OTLP |

### 可选 profile=search

| 服务 | 镜像 | 端口 | 用途 |
|------|------|------|------|
| **Elasticsearch 8.11** | `docker.elastic.co/elasticsearch/elasticsearch:8.11.0` | 9200 | 全文搜索 |
| **Kibana 8.11** | `docker.elastic.co/kibana/kibana:8.11.0` | 5601 | ES 可视化 |

## 3. 启动后接入

### 3.1 数据库

```bash
# Adminer Web GUI
open http://localhost:8082
# Server: mariadb
# User: minimax
# Password: minimax_pass_2024
# Database: minimax_platform

# CLI
docker exec -it minimax-mariadb mysql -uminimax -pminimax_pass_2024 minimax_platform

# 初始化 SQL
docker exec -i minimax-mariadb mysql -uroot -pminimax_root_2024 < sql/01-database.sql
```

### 3.2 Nacos

```bash
# Console
open http://localhost:8848/nacos
# nacos / nacos

# 注册的 13 个微服务 + gateway 应自动出现在服务列表
```

### 3.3 Prometheus

```bash
# 验证 scrape targets
open http://localhost:9090/targets
# 应看到 minimax-auth / minimax-chat / ... / minimax-gateway
```

### 3.4 Grafana

```bash
# 登录
open http://localhost:3001
# admin / minimax_grafana_2024

# 加 Prometheus 数据源: http://prometheus:9090
# 导入 dashboard: dashboards/minimax-platform.json
```

### 3.5 Jaeger

```bash
# UI
open http://localhost:16686

# V5.14 OTel 自动导出 span 到 http://jaeger:4318 (OTLP HTTP)
```

## 4. 升级路径

```
V5.0 之前:
  手动 apt install mariadb + redis + nacos
  各种 root 权限 + 配置冲突

V5.20 现在:
  docker compose up -d  (一行启动)
  数据持久化 (./data/)
  profile 分组 (按需启用)
  健康检查 (depends_on + healthcheck)
```

## 5. 端口冲突解决

如果本地已占用某些端口, 修改 `docker-compose.yml`:

```yaml
ports:
  - "3307:3306"   # host:container
```

常用冲突:
- 3306: 本地 MariaDB
- 6379: 本地 Redis
- 8080: 本地 Gateway
- 8848: 本地 Nacos

## 6. 数据持久化

数据存在 `./data/` 目录:

```
data/
├── mariadb/       # MariaDB data
├── redis/         # Redis AOF
├── nacos/         # Nacos logs + data
├── prometheus/    # Prometheus TSDB
├── grafana/       # Grafana dashboards
├── jaeger/        # Jaeger badger
└── es/            # ES indices
```

完全清理: `docker compose down -v` (会删数据)

## 7. V5.20 新增/修改文件

| 文件 | 变更 |
|------|------|
| `docker-compose.yml` | 重写 125 → 200 行, 4 个 profile |
| `frontend/public/sw.js` | 重写 78 → 110 行, API + 静态分别策略 |
| `backend/minimax-common/.../i18n/LocaleConfig.java` | 新 1.5KB |
| `docs/INFRA-DOCKER-GUIDE.md` | 本文档 |

## 8. 与 deploy-minimax.sh 关系

| 场景 | 推荐方式 |
|------|---------|
| 开发 (单机) | `docker compose up -d` + `mvn spring-boot:run` |
| 测试 (CI) | `docker compose up -d` + jar 启动 |
| 生产 (单机) | `sudo ./deploy-minimax.sh install` (apt 装 mariadb/redis/nacos) |
| 生产 (集群) | Kubernetes + Helm Chart (V6.x 计划) |

## 9. Troubleshooting

### 9.1 Nacos 启动慢

```bash
docker logs -f minimax-nacos
# 看启动日志, 通常 20-30s 才 ready
```

### 9.2 MariaDB 数据迁移

```bash
# 导出
docker exec minimax-mariadb mysqldump -uroot -pminimax_root_2024 minimax_platform > backup.sql
# 导入
docker exec -i minimax-mariadb mysql -uroot -pminimax_root_2024 minimax_platform < backup.sql
```

### 9.3 Prometheus 抓取不到

检查:
1. `http://localhost:9090/targets` 看 targets 健康状态
2. gateway / 各微服务 `/actuator/prometheus` 是否暴露
3. 是否在同一个 `minimax-net` 网络