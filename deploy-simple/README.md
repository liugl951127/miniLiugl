# MiniMax Platform — 一体化部署 (V1)

> **目标: 一份脚本, 一个端口 (80), 前后端一体化访问**
> **不需要 Docker / K8s, 不需要 3000 端口映射**

```
                    ┌─────────────────────────┐
                    │   浏览器                │
                    │   http://<server-ip>/   │
                    └────────────┬────────────┘
                                 │
                                 ▼  80 端口
                    ┌─────────────────────────┐
                    │  nginx (反代 + 静态)    │
                    │  /opt/minimax/nginx     │
                    └────────────┬────────────┘
                                 │
       ┌─────────────────────────┼─────────────────────────┐
       ▼                         ▼                         ▼
  / (SPA 静态)             /api/**                       /ws/**
  frontend/dist            gateway :8080                 gateway/auth
       │                         │                         │
       ▼                         ▼                         ▼
   index.html             12 个微服务 (jar)             WebSocket
                          + ws/function
```

---

## 📁 目录结构

```
/opt/minimax/
├── nginx/                          # nginx 配置 + 日志
│   ├── conf.d/minimax.conf         # 入口配置 (端口 80)
│   └── html/                       # 前端 dist (软链或拷贝)
├── backend/                        # 后端 jar + 启动脚本
│   ├── minimax-gateway.jar         # :8080  (入口)
│   ├── minimax-auth.jar            # :8081
│   ├── minimax-chat.jar            # :8082
│   ├── minimax-memory.jar          # :8083
│   ├── minimax-model.jar           # :8084
│   ├── minimax-rag.jar             # :8085
│   ├── minimax-function.jar        # :8086
│   ├── minimax-multimodal.jar      # :8087
│   ├── minimax-agent.jar           # :8088
│   ├── minimax-monitor.jar         # :8089
│   ├── minimax-admin.jar           # :8090
│   ├── minimax-ws.jar              # :8095
│   ├── logs/                       # 每个服务的日志
│   ├── pids/                       # 每个服务的 pid
│   └── start-all.sh / stop-all.sh  # 启停脚本
└── README                          # (本文件)
```

---

## 🚀 一键部署 (3 步)

### 前提
- Linux (CentOS 7+ / Ubuntu 20+ / Debian 11+)
- JDK 17 (`java -version` 验证)
- nginx 已装 (`nginx -v` 验证, 1.18+ 推荐)
- MySQL 8.0 已运行 (`init-db.sh` 初始化)
- Nacos 2.x 已运行 (`docker run -d -p 8848:8848 nacos/nacos-server`)
- Redis 已运行 (可选, 短期记忆)
- 至少 4 GB 内存 (12 个 jar 进程)

### 步骤

```bash
# 1. 在项目根目录执行 (Mac/Linux/WSL)
chmod +x deploy-simple/deploy.sh
./deploy-simple/deploy.sh

# 2. 等待 3-5 分钟 (mvn install + npm install + 打包)
# 输出:
#   ✓ 后端 jar: 13/13
#   ✓ 前端 dist 已生成
#   ✓ nginx 配置已部署
#   ✓ 所有服务已启动 (1/13 ... 13/13)

# 3. 浏览器访问
http://<server-ip>/
# 默认账号: admin / admin123
```

---

## 🛠️ 常用命令

```bash
# 启动所有服务
/opt/minimax/backend/start-all.sh

# 停止所有服务
/opt/minimax/backend/stop-all.sh

# 查看某个服务日志
tail -f /opt/minimax/backend/logs/minimax-gateway.log

# 重启单个服务
/opt/minimax/backend/restart.sh minimax-chat

# nginx 重载
sudo nginx -t && sudo systemctl reload nginx

# 查看所有服务状态
/opt/minimax/backend/status.sh
```

---

## ⚙️ 关键配置

### 端口规划 (V1)
| 端口 | 角色 | 暴露? |
|------|------|-------|
| **80** | nginx (前后端统一入口) | ✅ 公网 |
| 8080 | gateway (内部) | ❌ 仅 127.0.0.1 |
| 8081-8090 | 业务微服务 | ❌ 仅 127.0.0.1 |
| 8095 | WebSocket | ❌ 仅 127.0.0.1 |
| 8848 | Nacos | ❌ 仅 127.0.0.1 |

### 环境变量
```bash
# /opt/minimax/backend/env.sh (自动生成)
export NACOS_SERVER=127.0.0.1:8848
export MYSQL_URL=jdbc:mysql://127.0.0.1:3306/minimax?...
export REDIS_HOST=127.0.0.1
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export SERVER_PORT=8081  # 每个服务独立
```

---

## 🔄 与 V5.30 原方案的区别

| 维度 | V5.30 (旧) | V1 (本方案) |
|------|------------|-------------|
| 入口端口 | 3000 (nginx) + 8080 (gateway) | **80 (统一)** |
| 部署方式 | Docker / K8s / Compose | **纯 jar + nginx** |
| 前端服务 | vite dev (3000) 或 nginx (3000) | **打包进 nginx html** |
| CORS | 需要 (跨端口) | **不需要 (同源)** |
| 用户访问 | http://host:3000 | **http://host** |
| 资源占用 | Docker 多层镜像 | **直接 jar, 内存省 30%** |

---

## ⚠️ 已知问题

1. **首次启动慢** — 13 个 jar 同时启, 大约 30-60s, 健康检查有 ready check
2. **日志清理** — 建议加 logrotate, 见 `deploy-simple/logrotate-minimax`
3. **HTTPS** — 当前是 HTTP, 生产建议加 certbot + Let's Encrypt

---

## 📝 更新记录

- **V1.0** (2026-06-22): 初版, 一键部署 + 单端口 80