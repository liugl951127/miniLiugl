# 宿主机 Nginx 部署 (V3.5.5+)

## 核心变更
- **nginx 不再在 Docker 中运行**，改为宿主机直接装
- 后端 5 容器只跑业务，nginx 在宿主负责 80 端口入口

## 架构对比

### 优化前（nginx 在容器）
```
Browser ──→ nginx:80 (容器) ──┬─→ auth:8081
                              ├─→ ai:8094
                              └─→ gateway:7080
                                 ↑
                              mariadb + redis
```

### 优化后（nginx 走宿主）
```
Browser ──→ nginx (宿主 /etc/nginx) ──┬─→ 127.0.0.1:8081 → auth (容器)
                                       ├─→ 127.0.0.1:8094 → ai (容器)
                                       └─→ 127.0.0.1:7080 → gateway (容器)
                                          ↑
                                       mariadb + redis (容器)
```

## 优势
1. **少一个容器层** — 减少 1 个容器、1 份 nginx 镜像
2. **宿主 nginx 性能更好** — 直通 OS，无容器网络开销
3. **HTTPS 灵活** — certbot / OpenSSL / 自签证书都好搞
4. **systemd 统一管理** — `systemctl status nginx` / `journalctl -u nginx`
5. **防火墙简单** — 只开 80/443 端口即可

## 一键安装

```bash
# CentOS 9 / RHEL
sudo dnf install -y nginx
sudo systemctl enable nginx
sudo cp nginx/nginx.conf /etc/nginx/conf.d/minimax.conf
sudo cp nginx/upstream.conf /etc/nginx/conf.d/minimax-upstream.conf
sudo cp -r frontend/dist/* /usr/share/nginx/html/
sudo nginx -t && sudo systemctl restart nginx

# Ubuntu 20+ / Debian 11+
sudo apt install -y nginx
sudo systemctl enable nginx
# (同上的 cp 操作)
```

## 推荐：使用 install-nginx.sh

```bash
sudo ./nginx/install-nginx.sh install   # 装 + 配置 + 启动 (全自动)
sudo ./nginx/install-nginx.sh status    # 看状态
sudo ./nginx/install-nginx.sh restart   # 重启
sudo ./nginx/install-nginx.sh reload    # 重新加载配置
sudo ./nginx/install-nginx.sh logs      # 实时日志
sudo ./nginx/install-nginx.sh uninstall # 卸载
```

### install-nginx.sh 做的事
1. 检测系统 (CentOS / Debian) 用对应包管理器装 nginx
2. `systemctl enable nginx` 开机自启
3. 部署 `nginx/nginx.conf` → `/etc/nginx/conf.d/minimax.conf`
4. 部署 `nginx/upstream.conf` → `/etc/nginx/conf.d/minimax-upstream.conf`
5. 复制 `frontend/dist/*` → `/usr/share/nginx/html/`
6. `nginx -t` 验证配置 + `systemctl restart nginx`
7. 检查健康 (`curl http://localhost/healthz`)

## 端口说明

| 端口 | 谁监听 | 谁访问 | 说明 |
|------|--------|--------|------|
| **80** | **宿主 nginx** | 浏览器/客户端 | **单端口入口** |
| 443 | 宿主 nginx | 浏览器/客户端 | HTTPS (用 certbot 申请) |
| 3306 | 容器 mariadb | 宿主运维工具 | DB 调试用 |
| 6379 | 容器 redis | 宿主运维工具 | Cache 调试用 |
| 8081 | 容器 auth | **宿主 nginx** | 反向代理 127.0.0.1:8081 |
| 8094 | 容器 ai | **宿主 nginx** | 反向代理 127.0.0.1:8094 |
| 7080 | 容器 gateway | **宿主 nginx** | 反向代理 127.0.0.1:7080 |

**关键**: 后端容器端口暴露 `0.0.0.0:port:port` (而不是 `127.0.0.1`)，让宿主 nginx 通过 localhost 访问到。

## nginx 配置详解

### upstream.conf
```nginx
upstream auth_service   { server 127.0.0.1:8081; keepalive 16; }
upstream ai_service     { server 127.0.0.1:8094; keepalive 16; }
upstream gateway_service{ server 127.0.0.1:7080; keepalive 16; }
```

### 主配置 (nginx.conf)
```nginx
server {
    listen 80;

    # 健康检查
    location = /healthz { return 200 "ok\n"; }

    # 前端 SPA (vue-router history fallback)
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    # API 反代
    location /api/v1/auth/  { proxy_pass http://auth_service; }
    location /api/v1/ai/    { proxy_pass http://ai_service; }
    location /api/v1/       { proxy_pass http://gateway_service; }
}
```

## 完整部署流程

```bash
# 1. 启动后端 5 容器 (nginx 走宿主, 不在容器中)
./deploy-mini.sh up
# 自动检测: nginx 未装 → 调用 ./nginx/install-nginx.sh install
# 自动检测: nginx 已装 → 跳过

# 2. 验证
curl http://localhost/healthz        # → ok
curl http://localhost/                # → HTML
curl http://localhost/api/v1/ai/intent/predict -X POST \
    -H "Content-Type: application/json" \
    -d '{"text":"我要退款"}'
```

## 防火墙建议

```bash
# CentOS
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload

# Ubuntu
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
```

只暴露 80/443，**不要**直接暴露 3306/6379/8081/8094/7080（这些由 nginx 反代）。

## HTTPS 配置（可选）

```bash
# 1. 安装 certbot
sudo dnf install -y certbot python3-certbot-nginx
# 或
sudo apt install -y certbot python3-certbot-nginx

# 2. 申请证书 (自动改 nginx 配置)
sudo certbot --nginx -d your-domain.com

# 3. 自动续期
echo "0 3 * * * certbot renew --quiet" | sudo crontab -
```

## 故障排查

| 现象 | 排查 |
|------|------|
| 502 Bad Gateway | 容器未启动或端口没暴露：`docker ps` + `curl 127.0.0.1:8081/actuator/health` |
| 404 Not Found | 前端 dist 没复制到 `/usr/share/nginx/html/` |
| 502 + 容器都在 | upstream 端口错：看 `cat /etc/nginx/conf.d/minimax-upstream.conf` |
| 配置文件改了不生效 | `sudo nginx -t && sudo systemctl reload nginx` |
| nginx 启动失败 | `sudo journalctl -u nginx -n 30` 看错误 |

---

**文档版本**: V3.5.5+ (2026-07-13)
