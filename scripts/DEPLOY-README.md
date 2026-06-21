# 🐧 MiniMax Linux 一键部署 (非 Docker)

> 完整 systemd 化部署方案,适合生产环境.

## 📦 一行命令

```bash
curl -fsSL https://raw.githubusercontent.com/liugl951127/miniLiugl/main/scripts/deploy-linux.sh -o deploy-linux.sh
chmod +x deploy-linux.sh
sudo ./deploy-linux.sh install
```

完成后访问:
- **前端**: http://your-server-ip
- **管理账号**: `adminLiugl` / `Liugl@2026`

## 🎯 部署架构

```
                            ┌─────────────────────────────────┐
                            │         nginx :80                │
                            │   (反向代理 + 静态文件)           │
                            └────────────┬────────────────────┘
                                         │
       ┌──────────────┬──────────────────┼──────────────────┬──────────────┐
       ▼              ▼                  ▼                  ▼              ▼
   minimax-auth   minimax-chat    minimax-model      minimax-agent  minimax-ws
   :8081          :8082           :8083              :8090          :8095
       │              │                  │                  │              │
       └──────────────┴──────────────────┴──────────────────┴──────────────┘
                                         │
                            ┌────────────▼────────────┐
                            │  MariaDB 10.5           │
                            │  minimax_platform       │
                            │  (38 张表)              │
                            └─────────────────────────┘
```

## 🛠️ 部署清单

| 组件 | 版本 | 来源 |
|------|------|------|
| Java | OpenJDK 17 | apt / dnf |
| Maven | 3.8.7 | Apache 归档 |
| Node.js | 22 LTS | NodeSource |
| MariaDB | 10.5+ | 系统包 |
| nginx | 1.18+ | 系统包 |

## 📁 目录结构

部署完成后:
```
/opt/minimax/
├── apps/                      # 所有 jar 包
│   ├── minimax-auth.jar
│   ├── minimax-chat.jar
│   ├── minimax-model.jar
│   ├── ...
│   └── minimax-ws.jar
├── frontend/
│   └── dist/                  # 前端静态文件
├── data/                      # 数据目录 (预留)
├── backups/                   # 自动备份
└── ...

/var/log/minimax/              # 所有服务日志
├── auth.log
├── chat.log
├── model.log
├── ...
└── frontend.log

/etc/systemd/system/
├── minimax-auth.service
├── minimax-chat.service
├── ...
├── minimax-frontend.service
└── minimax-nginx.service

/etc/nginx/conf.d/
└── minimax.conf
```

## 🔧 systemd 服务管理

### 查看状态

```bash
sudo ./deploy-linux.sh status
```

输出示例:
```
SERVICE                   STATE      UPTIME  PORT
-------                   -----      ------  ----
minimax-auth              active     01:23:45 8081
minimax-chat              active     01:23:42 8082
minimax-model             active     01:23:39 8083
...
```

### 单个服务管理

```bash
# 重启 auth
sudo systemctl restart minimax-auth

# 查看日志 (实时)
sudo journalctl -u minimax-auth -f

# 看最后 200 行
sudo journalctl -u minimax-auth -n 200 --no-pager

# 开机自启
sudo systemctl enable minimax-auth

# 禁止开机启动
sudo systemctl disable minimax-auth
```

### 全部服务

```bash
sudo ./deploy-linux.sh start     # 启动
sudo ./deploy-linux.sh stop      # 停止
sudo ./deploy-linux.sh restart   # 重启
sudo ./deploy-linux.sh status    # 状态
```

## 📋 部署模块 (12 个后端 + 1 个前端)

| 模块 | 端口 | 上下文 | 说明 |
|------|------|--------|------|
| auth | 8081 | `/` | 认证 + 微信扫码 |
| chat | 8082 | `/chat` | 对话 |
| model | 8083 | `/model` | AI 模型调用 |
| memory | 8084 | `/memory` | 记忆 |
| rag | 8085 | `/rag` | RAG 检索 |
| function | 8086 | `/function` | 函数调用 |
| admin | 8087 | `/admin` | 后台管理 |
| multimodal | 8088 | `/multi` | 多模态 |
| monitor | 8089 | `/monitor` | 监控 |
| agent | 8090 | `/agent` | Agent |
| prompt | 8091 | `/prompt` | Prompt 模板 |
| ws | 8095 | `/ws` | WebSocket |
| frontend | 5173 | `/` | Vite (经 nginx 80 暴露) |

## 🌐 nginx 反向代理

nginx 监听 80 端口, 按路径前缀分流:

```nginx
location /                  →  frontend (5173)
location /api/v1/auth/      →  auth (8081)
location /api/v1/chat/      →  chat (8082)
location /api/v1/model/     →  model (8083)
location /api/v1/memory/    →  memory (8084)
location /api/v1/rag/       →  rag (8085)
location /api/v1/function/  →  function (8086)
location /api/v1/admin/     →  admin (8087)
location /api/v1/multi/     →  multimodal (8088)
location /api/v1/monitor/   →  monitor (8089)
location /api/v1/agent/     →  agent (8090)
location /api/v1/prompt/    →  prompt (8091)
location /ws/               →  ws (8095)  [WebSocket]
location /health            →  "OK"
```

## 💾 数据库

- **DB 名**: `minimax_platform`
- **DB 用户**: `minimax` / `minimax_pass_2024`
- **表数量**: 38 (含 4 张微信扫码表)
- **初始化**: `sql/init-minimax.sql` (1258 行)

```bash
# 手动初始化
mysql -uroot -p"${DB_ROOT_PASS}" < /opt/minimax/src/sql/init-minimax.sql

# 或重置
mysql -uroot -p"${DB_ROOT_PASS}" -e "DROP DATABASE minimax_platform; CREATE DATABASE minimax_platform DEFAULT CHARACTER SET utf8mb4;"
mysql -uroot -p"${DB_ROOT_PASS}" < /opt/minimax/src/sql/init-minimax.sql
```

## 🔄 更新流程

```bash
sudo ./deploy-linux.sh update
```

执行:
1. `git pull --rebase` 拉最新代码
2. `mvn clean install` 重编译
3. `npm run build` 重构建前端
4. `systemctl restart` 重启所有服务

## 💼 备份

```bash
sudo ./deploy-linux.sh backup
```

输出:
- `/opt/minimax/backups/db_20260620_153000.sql.gz` - 数据库
- `/opt/minimax/backups/apps_20260620_153000.tar.gz` - jar 包

仅保留最近 5 个备份, 老的自动删除.

## 🔐 安全建议

### 1. 改默认密码

```sql
UPDATE sys_user SET password='$2a$10$新bcrypt哈希'
WHERE username='adminLiugl';
```

### 2. 改 JWT 密钥

修改 `/opt/minimax/apps/*.jar` 内的 `application.yml`:

```yaml
minimax:
  jwt:
    secret: '你的新密钥 (base64 编码, 32+ 字节)'
```

### 3. 改 DB 密码

```bash
mysql -uroot -p
ALTER USER 'minimax'@'localhost' IDENTIFIED BY '新密码';
```

然后修改所有 jar 内的 `application.yml`:
```yaml
spring:
  datasource:
    password: '新密码'
```

### 4. 启用 HTTPS

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

## 🚨 故障排查

### 服务起不来

```bash
# 看日志
sudo journalctl -u minimax-auth -n 200 --no-pager

# 端口占用
sudo ss -tlnp | grep 8081

# 手动启动调试
sudo -u minimax java -jar /opt/minimax/apps/minimax-auth.jar
```

### 数据库连接失败

```bash
# 测试连接
mysql -uminimax -pminimax_pass_2024 minimax_platform

# 看 MariaDB 状态
sudo systemctl status mariadb
```

### 前端 502

```bash
# 检查 frontend 服务
sudo systemctl status minimax-frontend
sudo journalctl -u minimax-frontend -n 100

# 检查 nginx
sudo nginx -t
sudo systemctl status nginx
```

### 内存不足

修改 `/etc/systemd/system/minimax-*.service`:

```ini
[Service]
Environment="JAVA_OPTS=-Xms256m -Xmx1024m"
ExecStart=/usr/bin/java $JAVA_OPTS -jar ...
```

```bash
sudo systemctl daemon-reload
sudo systemctl restart minimax-auth
```

## 📊 性能调优

### JVM 参数

```ini
ExecStart=/usr/bin/java \
  -Xms512m -Xmx2048m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/minimax/ \
  -jar ...
```

### 文件句柄

```ini
LimitNOFILE=65536
```

### nginx worker

```nginx
worker_processes auto;
worker_rlimit_nofile 65535;
```

## 📝 环境变量

```bash
export INSTALL_DIR=/data/minimax          # 安装目录
export DB_ROOT_PASS=minimax_pass_2024     # root 密码
export JWT_SECRET='你的密钥'              # JWT 密钥
export NODE_VERSION=22                    # Node 版本
```

## 🎯 卸载

```bash
sudo ./deploy-linux.sh uninstall
```

保留:
- `/opt/minimax/apps/` (jar 包)
- `/opt/minimax/data/`
- `/var/log/minimax/`

删除:
- systemd 服务文件
- nginx 配置

如需完全清理:
```bash
sudo rm -rf /opt/minimax /var/log/minimax
sudo systemctl stop mariadb && sudo systemctl disable mariadb
sudo apt remove --purge -y mariadb-server nginx
```

## 🔗 相关文档

- [BUILD.md](../docs/BUILD.md) - 跨平台打包
- [USER_GUIDE.md](../docs/USER_GUIDE.md) - 用户指南
- [MODULES.md](../docs/MODULES.md) - 模块清单
- [WECHAT-GUIDE.md](../docs/WECHAT-GUIDE.md) - 微信扫码登录
---

# 🆕 V5.21+: 两种部署脚本

V5.21 引入新脚本 `deploy-minimax.sh`, 与 `deploy-linux.sh` 区别:

| 维度 | `deploy-linux.sh` (旧) | `deploy-minimax.sh` (新) |
|------|----------------------|------------------------|
| 中间件安装 | apt 装 (需 root) | Docker 一行 / apt 二选一 |
| 默认模式 | --native | **--docker (一行启动)** |
| SQL 文件 | 旧 minimax-all.sql 路径 (V5.21 已修) | sql/init-minimax.sql |
| 启动顺序 | 手动 | 自动 (nacos→gateway→微服务) |
| 健康检查 | 5 项 | 16 项 |
| 升级路径 | 旧 (V5.12 之前) | **新 (V5.21 推荐)** |

**用法**:

```bash
# 方式 A: Docker (推荐, 一行启动所有中间件)
sudo ./scripts/deploy-minimax.sh install           # 默认 --docker
sudo ./scripts/deploy-minimax.sh install --docker  # 等价
sudo ./scripts/deploy-minimax.sh start
sudo ./scripts/deploy-minimax.sh test
sudo ./scripts/deploy-minimax.sh status

# 方式 B: Linux apt 原生 (无 Docker)
sudo ./scripts/deploy-minimax.sh install --native
# 自动: apt 装 Java/Maven/Node/MariaDB/Redis + 编译 + systemd

# 旧脚本 (V5.12 之前, 仍可用, 已修 SQL 路径)
sudo ./scripts/deploy-linux.sh install
```

**注意**:
- 两个脚本互不依赖, 任选一个
- 推荐: 新部署用 `deploy-minimax.sh --docker`, 老部署保留 `deploy-linux.sh` 不变
- 单独 SQL 导入: `mysql -uroot -p < sql/init-minimax.sql`
