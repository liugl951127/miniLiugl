# MiniMax Platform - 运维手册 (V2.0)

> 一切从这里开始 🚀

## 📋 目录

1. [快速启动](#快速启动)
2. [服务管理](#服务管理)
3. [故障排查](#故障排查)
4. [性能优化](#性能优化)
5. [备份恢复](#备份恢复)
6. [监控告警](#监控告警)
7. [升级迁移](#升级迁移)

---

## 🚀 快速启动

### 第一次部署

```bash
# 1. 克隆
git clone https://github.com/liugl951127/miniLiugl.git /opt/miniLiugl
cd /opt/miniLiugl

# 2. 一键启动 (默认用宿主机 nginx, 不强制域名)
sudo ./deploy-simple/docker-deploy.sh up

# 3. 验证 (等 5-10 分钟首次编译)
curl -I http://localhost/
# 期望: HTTP/1.1 200 OK
```

### 配置域名 + HTTPS

```bash
# DNS 解析 OK 后 (5-30 分钟生效):
sudo ./deploy-simple/docker-deploy.sh frontend liugeliang.com admin@liugeliang.com

# 验证
sudo ./deploy-simple/docker-deploy.sh verify liugeliang.com
```

### 默认账号

| 账号 | 密码 | 说明 |
|------|------|------|
| `adminLiugl` | `Liugl@2026` | **超级管理员** (创建即有) |
| `admin` | `admin@123` | 普通管理员 (需手动 INSERT) |

---

## 🛠️ 服务管理

### 查看所有服务状态

```bash
# 方式 1: docker compose
cd /opt/miniLiugl
docker compose ps

# 方式 2: 一键脚本
./deploy-simple/docker-deploy.sh ps

# 方式 3: 看资源占用
docker stats --no-stream
```

### 启动 / 停止

```bash
# 启动全部
./deploy-simple/docker-deploy.sh up

# 停止全部 (数据保留)
./deploy-simple/docker-deploy.sh down

# 启动单个
docker compose up -d gateway

# 重启单个
docker compose restart auth

# 强制重新构建 (代码改动后)
./deploy-simple/docker-deploy.sh rebuild gateway
```

### 看日志

```bash
# 所有服务 (混合)
./deploy-simple/docker-deploy.sh logs

# 单个服务
./deploy-simple/docker-deploy.sh logs gateway

# 最近 100 行 + 跟踪
docker compose logs -f --tail=100 gateway
```

### 进入容器调试

```bash
# gateway 容器
docker exec -it minimax-gateway bash

# 看 JVM 实时状态
docker exec minimax-gateway jcmd 1 VM.flags
docker exec minimax-gateway jcmd 1 GC.heap_info

# 看应用配置
docker exec minimax-gateway cat /app/config/application.yml
```

---

## 🔧 故障排查

### 80 端口被占用

```bash
# 自动修复
sudo ./deploy-simple/docker-deploy.sh fix-80

# 手动
sudo systemctl stop nginx       # 宿主机 nginx
sudo lsof -i :80                # 找占用进程
docker compose restart nginx    # 重启 docker nginx
```

### 服务启动失败

```bash
# 看具体错误
docker compose logs gateway --tail=50

# 常见原因:
# 1. nacos 没起来 → docker compose up -d nacos
# 2. mysql 没起来 → docker compose logs mysql
# 3. JVM OOM → 调大 memory limit
# 4. 端口冲突 → ss -tlnp | grep :7080
```

### 登录失败

```bash
# 1. auth 服务日志
docker compose logs auth --tail=30

# 2. 测试直接访问 (绕过 nginx)
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"adminLiugl","password":"Liugl@2026"}'

# 3. 看 JWT secret 是否一致
docker exec minimax-auth cat /app/config/application.yml | grep -A 3 jwt:
docker exec minimax-gateway cat /app/config/application.yml | grep -A 3 jwt:
```

### 内存持续增长 (内存泄漏)

```bash
# 1. 看哪个容器占用最多
docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}"

# 2. 触发 Heap Dump
docker exec minimax-gateway jcmd 1 GC.heap_dump /tmp/heap.hprof
docker cp minimax-gateway:/tmp/heap.hprof /tmp/

# 3. 用 VisualVM 打开分析 (本地)
# scp /tmp/heap.hprof local:~/Desktop/
```

### 数据库连接池耗尽

```bash
# 看 mysql 连接数
docker exec minimax-mysql mysql -uroot -proot123456 -e \
  "SHOW PROCESSLIST; SELECT COUNT(*) FROM information_schema.processlist;"

# 重启服务清空连接
docker compose restart gateway auth chat
```

---

## ⚡ 性能优化

### JVM 内存 (V2.0 已优化)

每个微服务运行内存: **~256MB** (之前 ~512MB)

关键参数 (`backend/parent-jvm-args.txt`):
```
-XX:MaxRAMPercentage=70.0     # Docker 内存感知
-XX:+UseG1GC                  # G1 垃圾回收
-XX:+UseStringDeduplication   # 字符串去重
-XX:+UseCompressedOops        # 压缩指针
```

### 编译加速

```bash
# 用阿里云镜像 + 4 线程并行
mvn clean install -s .mvn/settings.xml -DskipTests -T 4
```

### Redis 连接池

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 16      # 默认 8, 提高
          max-idle: 8
          min-idle: 2
```

### 数据库连接池

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 8    # V2.0 精简, 可按需调整
      minimum-idle: 2
```

---

## 💾 备份恢复

### 自动备份脚本

```bash
# 创建 /root/backup-minimax.sh
cat > /root/backup-minimax.sh << 'EOF'
#!/usr/bin/env bash
BACKUP_DIR=/opt/minimax/backup
DATA_DIR=/opt/minimax/data
DATE=$(date +%Y%m%d-%H%M%S)

mkdir -p $BACKUP_DIR

# 1. MySQL
docker exec minimax-mysql mysqldump -uroot -proot123456 \
  --all-databases --single-transaction --routines --triggers \
  > $BACKUP_DIR/mysql-$DATE.sql

# 2. 压缩数据目录
tar czf $BACKUP_DIR/data-$DATE.tar.gz -C $DATA_DIR .

# 3. 删除 7 天前
find $BACKUP_DIR -type f -mtime +7 -delete

echo "✓ Backup done: $BACKUP_DIR"
ls -lah $BACKUP_DIR/
EOF
chmod +x /root/backup-minimax.sh

# 加 cron 每天 3 点
echo "0 3 * * * root /root/backup-minimax.sh >> /var/log/backup.log 2>&1" | \
  sudo tee /etc/cron.d/minimax-backup
```

### 恢复

```bash
# 1. MySQL
docker exec -i minimax-mysql mysql -uroot -proot123456 < backup.sql

# 2. 数据目录
tar xzf data.tar.gz -C /opt/minimax/data/

# 3. 重启服务
docker compose restart
```

---

## 📊 监控告警

### 健康检查

```bash
# gateway
curl http://localhost:7080/actuator/health/liveness
curl http://localhost:7080/actuator/health/readiness

# Prometheus 指标
curl http://localhost:7080/actuator/prometheus
```

### 看 OpenTelemetry 链路

```bash
# OTEL Collector 端口
# 4317 (gRPC)
# 4318 (HTTP)
# 8888 (Prometheus)

# 看 traces (需要 jaeger/tempo)
# 配置 OTEL Collector 把 traces 导到 jaeger
```

### 日志位置

| 类型 | 路径 |
|------|------|
| **nginx 访问** | `/var/log/nginx/access.log` |
| **nginx 错误** | `/var/log/nginx/error.log` |
| **应用 stdout** | `docker logs <container>` |
| **JVM OOM dump** | `/var/log/minimax/oom/` |
| **Nginx 自动续期** | `/var/log/certbot-renew.log` |

---

## 🔄 升级迁移

### 升级到新版本

```bash
# 1. 备份
/root/backup-minimax.sh

# 2. 拉最新代码
cd /opt/miniLiugl
git pull

# 3. 强制重建镜像
./deploy-simple/docker-deploy.sh rebuild

# 4. 重启
./deploy-simple/docker-deploy.sh down
./deploy-simple/docker-deploy.sh up

# 5. 验证
./deploy-simple/docker-deploy.sh verify liugeliang.com
```

### 迁移到新服务器

```bash
# 老服务器: 打包数据
cd /opt/miniLiugl
git pull
/root/backup-minimax.sh
scp -r /opt/miniLiugl root@<new-server>:/opt/
scp /opt/minimax/backup/mysql-*.sql root@<new-server>:/tmp/

# 新服务器: 恢复
cd /opt/miniLiugl
./deploy-simple/docker-deploy.sh up
docker exec -i minimax-mysql mysql -uroot -proot123456 < /tmp/mysql-*.sql
```

---

## 📞 常用命令速查

| 需求 | 命令 |
|------|------|
| 一键启动 | `sudo ./deploy-simple/docker-deploy.sh up` |
| 配 HTTPS | `sudo ./deploy-simple/docker-deploy.sh frontend DOMAIN EMAIL` |
| 看状态 | `./deploy-simple/docker-deploy.sh ps` |
| 看日志 | `./deploy-simple/docker-deploy.sh logs gateway` |
| 重启服务 | `./deploy-simple/docker-deploy.sh` 然后 up |
| 修 80 端口 | `sudo ./deploy-simple/docker-deploy.sh fix-80` |
| 验证链路 | `sudo ./deploy-simple/docker-deploy.sh verify DOMAIN` |
| 强制重建 | `./deploy-simple/docker-deploy.sh rebuild` |
| 完全停止 | `./deploy-simple/docker-deploy.sh down` |
| 看内存 | `docker stats --no-stream` |
| 看磁盘 | `df -h /opt/minimax` |
| 进入容器 | `docker exec -it minimax-gateway bash` |

---

## 🆘 联系支持

- GitHub: https://github.com/liugl951127/miniLiugl/issues
- 文档: 本目录的 `README.md`
- 部署: `deploy-simple/README.md`