# MiniMax Platform 部署指南 (V2.8.2)

> 完整部署文档, 覆盖开发/测试/生产

## 1. 系统要求

### 1.1 最低配置
- CPU: 2 核
- 内存: 4 GB
- 磁盘: 20 GB
- OS: CentOS 9 / Ubuntu 20+ / Debian 11+

### 1.2 推荐配置 (生产)
- CPU: 8 核+
- 内存: 16 GB+
- 磁盘: 100 GB+ SSD
- OS: CentOS Stream 9 / Ubuntu 22.04 LTS

### 1.3 软件依赖
- Docker 24+
- Docker Compose v2
- Nginx 1.20+
- OpenSSL 1.1+

## 2. 一键部署 (开发/测试)

```bash
# 1. 克隆代码
git clone https://github.com/liugl951127/miniLiugl.git
cd miniLiugl

# 2. 一键启动
./scripts/local-ci.sh --docker
docker compose up -d

# 3. 等待 60s, 访问
curl http://localhost
```

## 3. 详细部署步骤 (生产)

### 3.1 环境准备

#### CentOS Stream 9
```bash
# 1. 更新
sudo dnf update -y

# 2. 安装 Docker
sudo dnf config-manager --add-repo=https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io

# 3. 启动 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 4. 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 5. 安装 Nginx
sudo dnf install -y nginx

# 6. 关闭 SELinux (或配置允许)
sudo setenforce 0
```

#### Ubuntu 22.04
```bash
sudo apt update
sudo apt install -y docker.io docker-compose nginx
sudo systemctl enable docker nginx
```

### 3.2 部署服务

```bash
# 1. 创建数据目录
sudo mkdir -p /opt/minimax/data/{mariadb,redis,nacos,gateway,auth,chat,ai}
sudo chown -R 999:999 /opt/minimax/data

# 2. 启动基础服务
cd /opt/minimax
docker compose up -d mariadb redis nacos

# 3. 等待健康 (约 30s)
docker compose ps
# STATUS 应都是 healthy 或 running

# 4. 启动业务服务
docker compose up -d

# 5. 检查
curl http://localhost:7080/actuator/health
# {"status":"UP"}
```

### 3.3 配置 Nginx

`/etc/nginx/nginx.conf` (替换或 include):
```nginx
user nginx;
worker_processes auto;
events { worker_connections 1024; }

http {
    include /etc/nginx/mime.types;
    sendfile on;
    keepalive_timeout 65;
    client_max_body_size 100M;

    # 强制 HTTPS (生产)
    server {
        listen 80;
        server_name your-domain.com;
        return 301 https://$server_name$request_uri;
    }

    server {
        listen 443 ssl http2;
        server_name your-domain.com;

        ssl_certificate /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;

        # 前端
        location / {
            root /opt/minimax/dist;
            try_files $uri $uri/ /index.html;
        }

        # API (SSE 需关闭缓冲)
        location /api/ {
            proxy_pass http://127.0.0.1:7080/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_buffering off;          # 关键: SSE 必需
            proxy_cache off;
            proxy_read_timeout 86400s;    # 长连接
        }

        # WebSocket
        location /ws/ {
            proxy_pass http://127.0.0.1:7080/ws/;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
        }
    }
}
```

### 3.4 HTTPS 证书 (Let's Encrypt)

```bash
sudo dnf install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
# 自动续期
sudo systemctl enable certbot.timer
```

## 4. 数据持久化

### 4.1 数据卷挂载
`/opt/minimax/data/` 是所有服务的持久化目录:

```
/opt/minimax/data/
├── mariadb/      # MySQL 数据
├── redis/        # Redis 数据
├── nacos/        # Nacos 配置
├── gateway/      # 网关日志
├── auth/
├── chat/
├── ai/
└── uploads/      # 上传文件 (合规加密后存这里)
```

### 4.2 备份脚本

`/opt/minimax/scripts/backup.sh`:
```bash
#!/bin/bash
BACKUP_DIR=/opt/minimax/backups/$(date +%Y%m%d)
mkdir -p $BACKUP_DIR

# MySQL
docker exec minimax-mariadb mariadb-dump -uroot -proot123456 minimax | gzip > $BACKUP_DIR/db.sql.gz

# Redis
docker exec minimax-redis redis-cli -a minimax_redis_2024 --no-auth-warning BGSAVE
docker cp minimax-redis:/data/dump.rdb $BACKUP_DIR/

# 上传文件
tar czf $BACKUP_DIR/uploads.tar.gz /opt/minimax/data/uploads

# 清理 30 天前
find /opt/minimax/backups -mtime +30 -delete
```

定时任务:
```bash
crontab -e
0 2 * * * /opt/minimax/scripts/backup.sh
```

## 5. 升级部署

```bash
# 1. 拉新代码
cd /opt/minimax
git pull origin main

# 2. 重新构建镜像
docker compose build --pull

# 3. 滚动重启 (零停机)
docker compose up -d --no-deps --build gateway
# 验证 OK 后继续其他服务
docker compose up -d --no-deps --build auth chat ai

# 4. 旧镜像清理
docker image prune -f
```

## 6. 监控与告警

### 6.1 Prometheus
`/opt/minimax/prometheus.yml`:
```yaml
global:
  scrape_interval: 15s
scrape_configs:
  - job_name: 'minimax'
    static_configs:
      - targets: ['gateway:7080', 'auth:8081', 'ai:8094']
    metrics_path: /actuator/prometheus
```

### 6.2 告警规则
- CPU > 80% 持续 5min → 钉钉告警
- 内存 > 90% → 钉钉告警
- API 错误率 > 5% → 钉钉告警
- 磁盘 > 80% → 邮件告警

### 6.3 日志聚合
```bash
# 查看实时日志
docker compose logs -f --tail=100

# 某服务日志
docker logs -f minimax-gateway

# 错误过滤
docker logs minimax-gateway 2>&1 | grep -i error
```

## 7. 性能调优

### 7.1 JVM 参数
`docker-compose.yml`:
```yaml
environment:
  JAVA_OPTS: >-
    -XX:+UseG1GC
    -XX:MaxRAMPercentage=70
    -XX:+UseStringDeduplication
    -Xss512k
```

### 7.2 MySQL 调优
`/etc/mysql/my.cnf`:
```ini
[mysqld]
innodb_buffer_pool_size = 4G
innodb_log_file_size = 256M
max_connections = 500
query_cache_type = 1
query_cache_size = 64M
```

### 7.3 Redis 调优
`redis.conf`:
```
maxmemory 2gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
```

### 7.4 Nginx 调优
```nginx
worker_processes auto;
worker_rlimit_nofile 65535;

events {
    worker_connections 4096;
    use epoll;
    multi_accept on;
}

http {
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 30;
    types_hash_max_size 2048;
    server_tokens off;

    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
}
```

## 8. 安全清单

- [x] 改默认密码 (`admin/admin@123` → 强密码)
- [x] JWT Secret 使用环境变量, 不硬编码
- [x] HTTPS (Let's Encrypt 自动续期)
- [x] 数据库密码加密存储
- [x] 上传文件 AES-256-GCM 加密
- [x] 敏感信息脱敏 (DataMasker)
- [x] RBAC 按钮级权限
- [x] 审计日志 (6 个月保留)
- [x] 内容审核 (敏感词过滤)
- [x] SQL 注入防护 (MyBatis-Plus 参数化)
- [x] XSS 防护 (Vue 自动转义)
- [x] CSRF Token (前后端校验)
- [x] 限流 (Bucket4j, 100 req/s)
- [x] 防火墙 (仅暴露 80/443)

## 9. 故障排查

### 9.1 服务无法启动
```bash
# 查看日志
docker compose logs minimax-gateway

# 检查端口
netstat -tlnp | grep 7080

# 健康检查
curl http://localhost:7080/actuator/health
```

### 9.2 数据库连接失败
```bash
# 测连接
docker exec minimax-mariadb mariadb -uroot -proot123456 -e "SELECT 1"

# 检查 Nacos 配置
docker exec minimax-nacos curl http://localhost:8848/nacos/v1/cs/configs?dataId=minimax-common.yml&group=DEFAULT_GROUP
```

### 9.3 SSE 不工作
检查 Nginx:
```nginx
proxy_buffering off;       # 必须!
proxy_cache off;           # 必须!
proxy_read_timeout 86400;  # 长连接
```

### 9.4 内存溢出
```bash
# 查看堆
docker exec minimax-gateway jcmd 1 GC.heap_info

# 临时加内存
docker update --memory=4g minimax-gateway
```

## 10. 备份与恢复

### 10.1 备份
参见 4.2

### 10.2 恢复
```bash
# 停止服务
docker compose down

# 恢复 MySQL
gunzip < /opt/minimax/backups/20260712/db.sql.gz | \
  docker exec -i minimax-mariadb mariadb -uroot -proot123456 minimax

# 恢复上传文件
tar xzf /opt/minimax/backups/20260712/uploads.tar.gz -C /

# 启动
docker compose up -d
```

## 11. 联系

- 📧 部署支持: ops@minimax.com
- 🐛 Bug 报告: https://github.com/liugl951127/miniLiugl/issues
- 📖 文档: [docs/](.)

---

**版本**: V2.8.2
**最后更新**: 2026-07-12
