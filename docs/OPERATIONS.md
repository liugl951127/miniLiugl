# MiniMax Platform 运维操作手册 (V2.8.6)

> **完整运维指南** · 部署 / 监控 / 备份 / 扩容 / 故障排查 / 性能调优

## 一、部署架构总览

### 1.1 部署模式

| 模式 | 适用 | 复杂度 | 高可用 |
|------|------|--------|--------|
| **单机 Docker Compose** | 开发/测试/小规模 | ⭐ | ❌ |
| **多机 Docker Compose** | 中小企业生产 | ⭐⭐ | ⚠️ 手动 |
| **集群 Docker Compose + LB** | 中大型企业 | ⭐⭐⭐ | ✅ 半自动 |

### 1.2 推荐配置

| 规模 | 用户数 | QPS | 服务器 | 配置 |
|------|--------|-----|--------|------|
| 小 | < 100 | < 50 | 1 台 | 8C16G 500G |
| 中 | < 1000 | < 500 | 3 台 | 8C16G 500G × 3 |
| 大 | < 10000 | < 5000 | 10+ 台 | 16C32G 1T × 10 |
| 超大 | > 10000 | > 5000 | 50+ 台 | 32C64G 2T × 50 |

## 二、单机部署 (1 台主机)

### 2.1 硬件要求

| 资源 | 最小 | 推荐 |
|------|------|------|
| CPU | 4 核 | 8 核 (含 AI 推理) |
| 内存 | 8 GB | 16 GB |
| 磁盘 | 100 GB SSD | 500 GB SSD |
| 网络 | 100 Mbps | 1 Gbps |
| 操作系统 | CentOS Stream 9 / Ubuntu 20+ | CentOS 9 |

### 2.2 安装 Docker

**CentOS 9**:
```bash
# 1. 卸载旧版
sudo dnf remove -y docker docker-client docker-client-latest \
    docker-common docker-latest docker-engine

# 2. 安装 yum-utils
sudo dnf install -y yum-utils

# 3. 添加 repo
sudo yum-config-manager --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo

# 4. 安装
sudo dnf install -y docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin

# 5. 启动
sudo systemctl enable --now docker
sudo systemctl status docker

# 6. 验证
docker --version
docker compose version
```

### 2.3 一键部署

**下载代码**:
```bash
cd /opt
sudo git clone https://github.com/liugl951127/miniLiugl.git
cd miniLiugl
```

**配置环境变量**:
```bash
# 生成 JWT 密钥
JWT_SECRET=$(openssl rand -hex 32)
echo "JWT_SECRET=$JWT_SECRET" > .env

# 数据库密码
echo "MYSQL_ROOT_PASSWORD=root123456" >> .env
echo "REDIS_PASSWORD=minimax_redis_2024" >> .env

# 域名
echo "DOMAIN=liugeliang.com" >> .env
```

**启动所有服务**:
```bash
# 启动 (首次会下载镜像, 约 5-10 分钟)
docker compose up -d

# 查看状态
docker compose ps

# 查看日志
docker compose logs -f minimax-ai
```

**访问**:
```
http://your-ip
默认账户: adminLiugl / Liugl@2026
```

### 2.4 HTTPS 配置 (Let's Encrypt)

```bash
# 1. 安装 certbot
sudo dnf install -y certbot

# 2. 申请证书
sudo certbot certonly --standalone -d liugeliang.com -d www.liugeliang.com

# 3. 复制到 nginx 目录
sudo cp /etc/letsencrypt/live/liugeliang.com/fullchain.pem /opt/miniLiugl/certs/
sudo cp /etc/letsencrypt/live/liugeliang.com/privkey.pem /opt/miniLiugl/certs/

# 4. 自动续期 (cron)
echo "0 3 * * * certbot renew --quiet && cp /etc/letsencrypt/live/liugeliang.com/*.pem /opt/miniLiugl/certs/ && docker compose restart nginx" | sudo crontab -
```

**已集成** (Spring Boot Actuator + Micrometer):
- `jvm_memory_used_bytes` - JVM 堆内存
- `jvm_gc_pause_seconds` - GC 暂停
- `http_server_requests_seconds_count` - HTTP 请求数
- `http_server_requests_seconds_sum` - 总耗时
- `hikaricp_connections_active` - DB 连接池
- `tomcat_threads_busy` - Tomcat 线程
- 自定义业务指标: AI 调用次数 / Token 数 / 风控拦截数

**抓取配置** (`ops/prometheus.yml`):
```yaml
scrape_configs:
  - job_name: 'minimax-ai'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['minimax-ai:8094']
```

### 4.2 告警规则 (Prometheus Alertmanager)

**位置**: `ops/alerts.yml`

```yaml
groups:
- name: minimax-alerts
  rules:
  - alert: HighErrorRate
    expr: |
      sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
      / sum(rate(http_server_requests_seconds_count[5m])) > 0.05
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "高错误率 ({{ $value | humanizePercentage }})"
  
  - alert: HighMemory
    expr: jvm_memory_used_bytes{area="heap"} > 1.5e9
    for: 5m
    labels:
      severity: warning
  
  - alert: HighLatency
    expr: |
      histogram_quantile(0.99,
        rate(http_server_requests_seconds_bucket[5m])
      ) > 1
    for: 5m
    labels:
      severity: warning
  
  - alert: ServiceDown
    expr: up{job=~"minimax-.*"} == 0
    for: 1m
    labels:
      severity: critical
```

### 4.3 Grafana 仪表盘

**导入**: `ops/grafana/dashboard.json`

**4 个核心仪表盘**:
1. **总览**: CPU/内存/磁盘/QPS/错误率
2. **JVM**: 堆/GC/线程/类加载
3. **HTTP**: 状态码/P99/Top 10 慢请求
4. **业务**: AI 调用/Token/风控/订单

**告警渠道**:
```yaml
# alertmanager.yml
receivers:
- name: ops-team
  email_configs:
  - to: ops@liugeliang.com
    send_resolved: true
  webhook_configs:
  - url: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx'
    send_resolved: true
```

### 4.4 链路追踪 (OpenTelemetry)

**配置** (`application.yml`):
```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 采样 10%
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
```

**Tempo 查询**:
```
{service.name="minimax-ai"} && trace_id="abc123"
```

**前端 traceId 透传**:
```js
config.headers['X-Trace-Id'] = 'fe-' + Date.now().toString(36)
```

## 五、备份与恢复

### 5.1 自动备份脚本

**MySQL 全量备份** (`scripts/backup.sh`):
```bash
#!/usr/bin/env bash
set -euo pipefail

BACKUP_ROOT="/var/backups/minimax"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${BACKUP_ROOT}/daily/${TIMESTAMP}"
MYSQL_HOST=${MYSQL_HOST:-127.0.0.1}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASS=${MYSQL_PASS:-root123456}
DB_NAME=${DB_NAME:-minimax_platform}

mkdir -p "${BACKUP_DIR}"

# 1. MySQL 全量备份
mysqldump -h "${MYSQL_HOST}" -P 3306 \
          -u "${MYSQL_USER}" -p"${MYSQL_PASS}" \
          --single-transaction --routines --triggers --events \
          "${DB_NAME}" | gzip > "${BACKUP_DIR}/${DB_NAME}.sql.gz"

# 2. Redis 备份 (RDB)
redis-cli -h ${REDIS_HOST:-127.0.0.1} -a "${REDIS_PASS}" BGSAVE
cp /var/lib/redis/dump.rdb "${BACKUP_DIR}/redis.rdb"

# 3. 上传到 MinIO/S3
mc cp "${BACKUP_DIR}/${DB_NAME}.sql.gz" minio/backups/$(date +%Y/%m/%d)/

# 4. 清理 30 天前的备份
find "${BACKUP_ROOT}" -mindepth 2 -maxdepth 2 -type d \
    -mtime +30 -exec rm -rf {} \;

echo "[backup] OK, size=$(du -h ${BACKUP_DIR} | tail -1)"
```

**定时任务** (crontab):
```bash
# 每天凌晨 2 点备份
echo "0 2 * * * /opt/miniLiugl/scripts/backup.sh" | sudo crontab -
```

### 5.2 恢复

**MySQL 恢复** (`scripts/restore.sh`):
```bash
#!/usr/bin/env bash
set -euo pipefail

BACKUP_FILE=${1:?"usage: restore.sh <backup-file.sql.gz>"}
gunzip -c "${BACKUP_FILE}" | mysql -h ${MYSQL_HOST} \
                                   -u ${MYSQL_USER} -p"${MYSQL_PASS}" \
                                   ${DB_NAME}
echo "[restore] done"
```

**使用**:
```bash
# 1. 停止应用
docker compose stop minimax-gateway minimax-chat

# 2. 恢复数据库
./scripts/restore.sh /var/backups/minimax/daily/20260712_020000/minimax_platform.sql.gz

# 3. 启动应用
docker compose start minimax-gateway minimax-chat
```

### 5.3 Redis 备份

Redis 持久化 (RDB + AOF):
```bash
# 配置 (/etc/redis/redis.conf)
save 900 1
save 300 10
save 60 10000
appendonly yes
```

恢复: 启动 Redis 自动加载 `dump.rdb`

### 5.4 灾备演练

**每季度 1 次**:
1. 模拟数据库故障
2. 切换到从库
3. 验证应用可用
4. 回切
5. 记录 RTO / RPO

## 六、扩容

### 6.1 垂直扩容 (Scale Up)

**步骤**:
1. 停服: `docker compose stop`
2. 调整资源: 修改 `docker-compose.yml` 的 `mem_limit` / `cpus`
3. 启动: `docker compose up -d`
4. 验证: `docker stats`

**示例** (minimax-ai 从 2G 升到 4G):
```yaml
minimax-ai:
  mem_limit: 4g
  cpus: '4'
  environment:
    JAVA_OPTS: "-Xmx3g -XX:MaxRAMPercentage=70"
```

### 6.2 水平扩容 (Scale Out)

**Docker Compose** (多机):
```bash
# 在第 2 台机器上
DOCKER_HOST=tcp://node1:2376 docker compose up -d minimax-ai

# Nginx upstream 自动加入
```

**Docker Compose (扩缩容)**:
**MySQL 主从**:
```bash
# 1. 主库配置 (my.cnf)
[mysqld]
server-id=1
log-bin=mysql-bin
binlog-format=ROW

# 2. 从库配置
[mysqld]
server-id=2
relay-log=mysql-relay-bin
read-only=1

# 3. 从库同步
CHANGE MASTER TO
  MASTER_HOST='master.minimax.local',
  MASTER_USER='repl',
  MASTER_PASSWORD='replpass',
  MASTER_LOG_FILE='mysql-bin.000001',
  MASTER_LOG_POS=4;
START SLAVE;
```

**Redis Cluster** (6 节点):
```bash
redis-cli --cluster create \
  10.0.1.1:6379 10.0.1.2:6379 10.0.1.3:6379 \
  10.0.2.1:6379 10.0.2.2:6379 10.0.2.3:6379 \
  --cluster-replicas 1
```

## 七、故障排查

### 7.1 常见故障

#### 故障 1: 服务无法启动

**症状**: `docker compose up -d` 后 `minimax-ai` 一直 Restarting

**排查**:
```bash
# 1. 查看日志
docker compose logs minimax-ai

# 2. 常见原因
# - 数据库连不上: 检查 DB_HOST, ping $DB_HOST
# - 端口被占用: netstat -lntp | grep 8094
# - 内存不足: docker stats
# - 配置错误: 检查 application.yml
```

**解决**:
```bash
# 内存不足 → 调整 limit
vim docker-compose.yml  # mem_limit: 2g
docker compose up -d minimax-ai

# 端口占用 → 杀掉旧进程
lsof -ti:8094 | xargs kill -9
```

#### 故障 2: API 响应 502

**症状**: 前端调接口返回 502 Bad Gateway

**排查**:
```bash
# 1. Gateway 日志
docker compose logs minimax-gateway

# 2. 后端服务状态
docker compose ps

# 3. 服务是否健康
curl http://localhost:8081/actuator/health
```

**解决**:
```bash
# 重启后端
docker compose restart minimax-auth

# 检查 Nacos 服务列表
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=minimax-auth
```

#### 故障 3: 登录慢 / 失败

**症状**: 登录接口 5 秒超时

**排查**:
```bash
# 1. 检查 Auth 服务
docker compose logs minimax-auth | tail -100

# 2. 检查数据库
docker exec minimax-mysql mysqladmin ping

# 3. 检查慢 SQL
docker exec minimax-mysql mysql -uroot -proot123456 -e "
  SELECT * FROM information_schema.PROCESSLIST
  WHERE COMMAND='Query' AND TIME > 5;
"
```

**解决**:
```sql
-- 杀掉慢查询
KILL <id>;

-- 加索引
CREATE INDEX idx_username ON user(username);
```

#### 故障 4: AI 响应慢

**症状**: chat 接口 P99 > 5s

**排查**:
```bash
# 1. AI 服务日志
docker compose logs minimax-ai | grep "cost"

# 2. 模型是否加载
curl http://localhost:8094/api/ai/info

# 3. GPU 状态
nvidia-smi
```

**优化**:
```java
// 1. 启用 KV cache
PipelineConfig.ENABLE_KV_CACHE = true;

// 2. 减少生成 token
PipelineConfig.MAX_GENERATE_TOKENS = 100;

// 3. 批处理
PipelineConfig.BATCH_SIZE = 4;
```

#### 故障 5: 数据库连接耗尽

**症状**: `HikariPool-1 - Connection is not available`

**排查**:
```bash
docker exec minimax-mysql mysql -uroot -proot123456 -e "SHOW PROCESSLIST;" | wc -l
```

**解决**:
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### 7.2 应急响应 SOP

#### 服务整体宕机

**5 分钟内恢复**:
```bash
# 1. 确认故障
curl http://localhost:7080/actuator/health

# 2. 重启所有
docker compose restart

# 3. 等待 30s
sleep 30

# 4. 验证
for svc in gateway auth chat memory model rag function multimodal \
           agent monitor admin prompt analytics pipeline ai ws; do
    curl -fsS http://localhost:8080/actuator/health/$svc 2>&1 | head -1
done

# 5. 通知用户
```

#### 数据损坏

**30 分钟内恢复**:
```bash
# 1. 停止应用
docker compose stop minimax-gateway minimax-chat minimax-ai

# 2. 恢复最新备份
ls -t /var/backups/minimax/daily/*/minimax_platform.sql.gz | head -1 | xargs \
  ./scripts/restore.sh

# 3. 重启应用
docker compose start minimax-gateway minimax-chat minimax-ai

# 4. 验证数据
docker exec minimax-mysql mysql -uroot -proot123456 -e "
  SELECT COUNT(*) FROM chat_message;
  SELECT COUNT(*) FROM ai_tool;
"

# 5. 通知用户
```

#### 安全事件

**立即响应**:
```bash
# 1. 隔离
docker network disconnect minimax-net minimax-ai

# 2. 收集证据
docker logs minimax-ai > /tmp/ai-logs.txt
tar czf /tmp/evidence.tgz /opt/miniLiugl/logs/ /var/log/

# 3. 改密钥
NEW_JWT=$(openssl rand -hex 32)
echo "JWT_SECRET=$NEW_JWT" > .env
docker compose restart

# 4. 报告
```


**调优命令**:
```bash
# 查看当前 GC
docker exec minimax-ai jstat -gc <pid>

# 查看堆转储
docker exec minimax-ai jmap -dump:live,format=b,file=/tmp/heap.hprof <pid>
docker cp minimax-ai:/tmp/heap.hprof ./

# MAT 分析: https://www.eclipse.org/mat/
```

### 8.2 数据库调优

**MySQL 关键参数**:
```ini
# /etc/my.cnf
[mysqld]
innodb_buffer_pool_size = 4G
innodb_log_file_size = 1G
max_connections = 500
query_cache_type = 0
slow_query_log = 1
long_query_time = 1
```

**SQL 优化**:
```sql
-- 1. 加索引
CREATE INDEX idx_session ON chat_message(session_id, created_at);

-- 2. 分析执行计划
EXPLAIN SELECT * FROM chat_message WHERE session_id = 'xxx' ORDER BY created_at DESC LIMIT 20;

-- 3. 慢查询统计
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;
```

### 8.3 Redis 调优

```bash
# 内存优化
maxmemory 4gb
maxmemory-policy allkeys-lru

# 持久化
save 900 1
save 300 10
appendonly yes
appendfsync everysec
```

### 8.4 Tomcat 调优

```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 50
    accept-count: 100
    max-connections: 10000
```

### 8.5 AI 模型调优

**CPU 模式**:
```java
PipelineConfig.BATCH_SIZE = 4;
PipelineConfig.ENABLE_KV_CACHE = true;
```

**GPU 模式** (需 NVIDIA 驱动):
```bash
# 1. 安装驱动
sudo dnf install -y nvidia-driver nvidia-container-toolkit

# 2. 重启 Docker
sudo systemctl restart docker

# 3. 验证
docker run --gpus all nvidia/cuda:11.8.0-base nvidia-smi

# 4. 启用
curl -X POST http://ai:8094/api/ai/pipeline/config/compute-mode?mode=GPU
```

## 九、运维工具脚本

| 脚本 | 用途 |
|------|------|
| `scripts/status.sh` | 查看所有服务状态 |
| `scripts/start.sh` | 启动服务 |
| `scripts/stop.sh` | 停止服务 |
| `scripts/backup.sh` | 备份 |
| `scripts/restore.sh` | 恢复 |
| `scripts/deploy.sh` | 部署 |
| `scripts/upgrade.sh` | 升级 |
| `scripts/tail-logs.sh` | 实时日志 |
| `scripts/seed-data.sh` | 初始化种子 |
| `scripts/local-ci.sh` | 本地 CI |
| `scripts/test-e2e.sh` | 端到端测试 |
| `scripts/gen-test-screenshots.py` | 生成测试截图 |
| `scripts/gen_ddl.py` | DDL 自动生成 |

## 十、SLA 承诺

| 指标 | 目标 |
|------|------|
| 可用性 | 99.9% (年宕机 < 8.76h) |
| P99 延迟 | < 1s (API), < 3s (AI Pipeline) |
| 数据持久性 | 99.999% (RPO < 1min) |
| 故障恢复 RTO | < 30min |
| 备份保留 | 30 天 |
| 升级窗口 | < 10min (滚动升级) |

## 十一、联系与升级

- **GitHub**: https://github.com/liugl951127/miniLiugl
- **Issues**: 提 issue
- **邮件**: ops@liugeliang.com
- **微信群**: 扫码加入

**版本升级检查清单**:
- [ ] 备份数据
- [ ] 查看 CHANGELOG.md
- [ ] 在测试环境验证
- [ ] 滚动升级 (先 1 副本)
- [ ] 监控 30 分钟
- [ ] 全部升级

---

**最后更新**: 2026-07-12 (V2.8.6)
