# MiniMax Platform — 生产外网部署指南 (V1.9.2)

> **V1.9.2 新增: 完整支持 CentOS Stream 9 / RHEL 9 (dnf + firewalld + SELinux)**
> 其他 OS: Ubuntu 20+ / Debian 11+ / CentOS 7-8

---

## 🐧 CentOS Stream 9 快速开始

```bash
# 1. 装 docker (CentOS Stream 9 默认仓库没 docker)
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
newgrp docker    # 或重新登录

# 2. 装 git + 拉代码
sudo dnf install -y git
git clone https://github.com/liugl951127/miniLiugl.git
cd miniLiugl

# 3. 一键启动 (自动适配 OS)
chmod +x deploy-simple/docker-deploy.sh
./deploy-simple/docker-deploy.sh up

# 4. (可选) 配置 HTTPS + 域名
sudo ./deploy-simple/setup-domain.sh your-domain.com admin@example.com
```

**CentOS Stream 9 特性自动处理**:
- ✅ dnf 包管理器 (而非 yum)
- ✅ firewalld 防火墙 (而非 ufw)
- ✅ SELinux 自动调整 (httpd_read_user_content, httpd_can_network_connect)
- ✅ nginx 通过 EPEL 仓库安装
- ✅ certbot 通过 EPEL 安装
- ✅ 证书自动续期通过 systemd timer 或 cron.d
- ✅ Docker CE 仓库配置提示

---

## 🐧 其他系统 (Ubuntu / Debian / CentOS 7-8)

### 🚀 一键部署 (5 分钟)

#### 前提
- 一台公网 VPS (1 核 2G 起, 推荐 2 核 4G)
- CentOS 7+ / Ubuntu 20+ / Debian 11+
- 一个**已解析**到 VPS 公网 IP 的域名 (例如 `minimax.example.com`)

---

## 📋 完整部署架构

```
外网用户
  │ https://your-domain.com/
  ▼
┌─────────────────────────────────────────────┐
│  公网服务器 (你的 VPS)                       │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │  nginx :443 (HTTPS) + :80 (重定向)   │  │
│  │  - Let's Encrypt 证书 (90 天自动续)  │  │
│  │  - HSTS / TLS 1.2 / 1.3             │  │
│  │  - gzip / 安全头 / CORS             │  │
│  └──────────────────────────────────────┘  │
│           │              │                  │
│           ▼              ▼                  │
│  ┌────────────────┐ ┌────────────────┐   │
│  │ gateway:7080   │ │ auth:8081      │   │
│  │ (Spring Cloud) │ │ (WebSocket)    │   │
│  └────────────────┘ └────────────────┘   │
│           │                               │
│           ▼ (lb:// 找 nacos)               │
│  ┌──────────────────────────────────────┐  │
│  │  nacos:8848 (服务发现)               │  │
│  └──────────────────────────────────────┘  │
│           │                               │
│  ┌────────┴────────┐                      │
│  ▼                 ▼                      │
│  mysql:3306      redis:6379               │
│  (数据)          (缓存/短期记忆)          │
│                                             │
│  15 个微服务 (jar 在 /opt/minimax/backend)│
└─────────────────────────────────────────────┘
```

---

## 🚀 一键部署 (5 分钟)

### 前提
- 一台公网 VPS (1 核 2G 起, 推荐 2 核 4G)
- **支持的操作系统** (V1.9.2 自动适配):
  - **CentOS Stream 9** / RHEL 9 / Rocky Linux 9 / AlmaLinux 9 (dnf + firewalld + SELinux)
  - **CentOS Stream 8** / RHEL 8 (dnf + firewalld + SELinux)
  - **CentOS 7** / RHEL 7 (yum 兼容)
  - **Ubuntu 20+** / **Debian 11+** (apt + ufw)
- 一个**已解析**到 VPS 公网 IP 的域名 (例如 `minimax.example.com`)

### 步骤

```bash
# 1. 拉代码
git clone https://github.com/liugl951127/miniLiugl.git
cd miniLiugl

# 2. 一键启动基础设施 (mysql + redis + nacos)
cd deploy-simple
./docker-deploy.sh up

# 3. 等基础设施就绪 (1-2 分钟)
docker compose ps    # 看 mysql/nacos/redis 都 healthy

# 4. 配置域名 + HTTPS (一键)
chmod +x setup-domain.sh
./setup-domain.sh your-domain.com your-email@example.com

# 完成!
# 访问: https://your-domain.com
```

---

## 📋 手动部署 (分步)

### Step 1: 启动基础设施 (Docker)

```bash
cd deploy-simple
./docker-deploy.sh up
```

启动后会自动跑:
- mysql:3306 (root/root123456, db=minimax_platform)
- redis:6379 (minimax_redis_2024)
- nacos:8848 (nacos/nacos)
- 15 个微服务容器
- nginx:80 (前端 + 反代)

### Step 2: 申请 HTTPS 证书

```bash
# 安装 certbot
apt install -y certbot   # Ubuntu/Debian
yum install -y certbot   # CentOS

# 申请证书 (standalone 模式, 自动用 80 端口验证)
certbot certonly --standalone \
  --preferred-challenges http \
  -d your-domain.com \
  -d www.your-domain.com \
  --email your-email@example.com \
  --agree-tos --no-eff-email
```

成功后证书在:
```
/etc/letsencrypt/live/your-domain.com/fullchain.pem
/etc/letsencrypt/live/your-domain.com/privkey.pem
```

### Step 3: 部署生产 nginx 配置

```bash
# 1. 复制配置
cp scripts/nginx-minimax-domain.conf /etc/nginx/conf.d/minimax.conf

# 2. 替换占位符 (你的域名)
sed -i "s|your-domain.com|your-domain.com|g" /etc/nginx/conf.d/minimax.conf

# 3. 备份默认配置
mv /etc/nginx/sites-enabled/default /etc/nginx/sites-enabled/default.bak 2>/dev/null

# 4. 测试 + 重载
nginx -t && systemctl reload nginx
```

### Step 4: 配置自动续期

```bash
cat > /etc/cron.d/certbot-renew <<EOF
0 3 * * * root certbot renew --quiet --post-hook "systemctl reload nginx"
EOF
```

---

## 🔍 验证清单

```bash
# 1. HTTPS 可访问
curl -I https://your-domain.com/
# 期望: HTTP/2 200

# 2. HTTP 自动跳 HTTPS
curl -I http://your-domain.com/
# 期望: 301 Location: https://your-domain.com/

# 3. API 可调用
curl -X POST https://your-domain.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"adminLiugl","password":"Liugl@2026"}'
# 期望: {"code":0,"data":{"accessToken":"..."}}

# 4. WebSocket 可连
wscat -c wss://your-domain.com/ws/notifications

# 5. SSL 评分 (A+)
https://www.ssllabs.com/ssltest/analyze.html?d=your-domain.com
```

---

## 🛡️ 安全加固建议

### 1. 防火墙
```bash
# 只开放 22 (ssh) + 80 (http) + 443 (https)
ufw default deny incoming
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
```

### 2. fail2ban (防 SSH 爆破)
```bash
apt install -y fail2ban
systemctl enable fail2ban
```

### 3. 限流 (防 CC / DDOS)

在 nginx 配置的 `http {}` 块加:
```nginx
# API 限流: 单 IP 每秒 30 个请求, 突发 50
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=30r/s;
# 登录端点更严: 单 IP 每分钟 10 次
limit_req_zone $binary_remote_addr zone=login_limit:10m rate=10r/m;

server {
    location /api/v1/auth/login {
        limit_req zone=login_limit burst=5 nodelay;
        limit_req_status 429;
        proxy_pass http://minimax_gateway;
    }
    location /api/ {
        limit_req zone=api_limit burst=50 nodelay;
        limit_req_status 429;
        proxy_pass http://minimax_gateway;
    }
}
```

### 4. nginx 隐藏版本号
```nginx
server_tokens off;
```

### 5. CSP 严格化
```nginx
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self' wss: https:;" always;
```

---

## 🔧 常见问题

### Q1: 证书申请失败 "Connection refused"
**原因**: 80 端口被占用或域名没解析
```bash
# 检查域名解析
nslookup your-domain.com
# 检查 80 端口
ss -tlnp | grep :80
# 临时停止 nginx
systemctl stop nginx
certbot certonly --standalone ...    # 再申请
```

### Q2: WebSocket 连接失败
**原因**: nginx 没正确转发 Upgrade 头
**解决**: 确保配置里有 `proxy_set_header Upgrade $http_upgrade; proxy_set_header Connection "upgrade";`

### Q3: 上传文件失败 (413 Request Entity Too Large)
**解决**: 配置里 `client_max_body_size 100M;` (已默认)

### Q4: 后端 gateway 502 Bad Gateway
**原因**: gateway:7080 没启动
```bash
# 检查
ss -tlnp | grep :7080
# 启动 (docker)
docker compose up -d gateway
```

### Q5: 跨域 CORS 报错
**原因**: 前端调 https://your-domain.com 但后端在 http://gateway:7080
**解决**: nginx 已经处理 (`Access-Control-Allow-Origin` 头),如果还有问题检查前端 `VITE_API_BASE` 是否为空 (相对路径)。

### Q6: 性能调优
```nginx
# /etc/nginx/nginx.conf 的 worker 调大
worker_processes auto;       # 默认 = CPU 核心数
worker_connections 4096;     # 默认 1024, 加大

# 启用 gzip 预压缩 (需要在 vite build 时生成 .gz / .br 文件)
# 我们的前端 dist 默认不带, nginx 运行时压缩也够用
```

---

## 📊 多域名 / 子域名配置

如果你有多个域名 (例如 `app.example.com` + `admin.example.com`):

```nginx
# app.example.com → 用户前端
server {
    listen 443 ssl http2;
    server_name app.example.com;
    ssl_certificate /etc/letsencrypt/live/app.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/app.example.com/privkey.pem;

    location / {
        # SPA 主入口
        root /opt/minimax/frontend/dist;
        try_files $uri $uri/ /index.html;
    }
    location /api/ {
        proxy_pass http://minimax_gateway;
    }
}

# admin.example.com → 管理后台 (同一套前端, 不同入口路径)
server {
    listen 443 ssl http2;
    server_name admin.example.com;
    ssl_certificate /etc/letsencrypt/live/admin.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/admin.example.com/privkey.pem;

    location / {
        root /opt/minimax/frontend/dist;
        try_files $uri $uri/ /index.html;
    }
    location /api/ {
        proxy_pass http://minimax_gateway;
    }
}
```

---

## 🌍 CDN 加速 (可选)

如果你用 Cloudflare / 阿里云 CDN:

1. 域名解析改到 CDN (CNAME)
2. CDN 回源到 `your-domain.com` (HTTPS, 启用 HTTP/2)
3. CDN 开启:
   - 静态资源缓存 (assets/ 1y)
   - HTML 不缓存 (no-cache)
   - WebSocket 不缓存 (透传)
   - TLS 1.3 + Brotli
4. 后端服务器只允许 CDN IP 段访问 (nginx allow/deny)

---

## 🎯 部署清单 (Checklist)

- [ ] 公网 VPS (2核4G+ 推荐)
- [ ] 域名 A 记录指向 VPS
- [ ] docker compose up (mysql + redis + nacos + 15 服务 + nginx)
- [ ] certbot 申请证书
- [ ] nginx 配置 + 重载
- [ ] 防火墙规则 (22/80/443)
- [ ] 自动续期 cron
- [ ] 备份策略 (mysql dump 定期)
- [ ] 监控告警 (可选: UptimeRobot + email)
- [ ] CDN 配置 (可选)

**部署完成,你的 MiniMax 平台已可被全世界访问! 🌍**

---

**最近更新: V1.9.1 (2026-06-23)**
- nginx 配置支持 HTTPS + WebSocket + CORS
- Let's Encrypt 自动签发 + 续期
- 一键脚本 setup-domain.sh
- 多域名支持
- CDN 友好配置