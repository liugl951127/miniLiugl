# MiniMax 部署指南 (V5.12 架构升级)

> 本指南说明生产级部署 — V5.12 集成 Nacos + Spring Cloud Gateway 后的完整架构

## 1. 架构总览

```
                    ┌────────────────────────┐
                    │  Browser / Mobile H5   │
                    └──────────┬─────────────┘
                               │
                               ▼ :3000
                    ┌────────────────────────┐
                    │  nginx (V5.8 优化)     │
                    │  - gzip / br 压缩      │
                    │  - /api/** → gateway   │
                    │  - /ws → gateway       │
                    │  - /doc.html → monitor │
                    └──────────┬─────────────┘
                               │
                               ▼ :8080
                    ┌────────────────────────┐
                    │ Spring Cloud Gateway   │
                    │  - JwtAuthGlobalFilter │
                    │  - 限流 (Redis 令牌桶) │
                    │  - Resilience4j 降级   │
                    │  - TraceFilter         │
                    └──────────┬─────────────┘
                               │ lb://minimax-*
                               │ (服务发现, V5.7)
                               ▼
            ┌──────────────────────────────────┐
            │ 12 个业务微服务 (8081-8095)      │
            │  - auth chat model memory rag    │
            │  - function admin multimodal     │
            │  - monitor agent prompt ws       │
            └────┬──────────┬──────────┬───────┘
                 │          │          │
                 ▼          ▼          ▼
            ┌────────┐ ┌────────┐ ┌────────┐
            │MariaDB │ │ Redis  │ │ Nacos  │ 8848
            │ 3306   │ │ 6379   │ │ (V5.12)│
            └────────┘ └────────┘ └────────┘
```

## 2. 端口分配

| 端口 | 用途 | 服务 |
|------|------|------|
| 3000 | 统一入口 | nginx |
| 8080 | API 网关 | Spring Cloud Gateway |
| 8848 | 服务发现 + 配置 | Nacos (V5.7+) |
| 3306 | 关系数据库 | MariaDB |
| 6379 | 限流/缓存 | Redis (V5.12) |
| 8081 | 认证授权 | minimax-auth |
| 8082 | 聊天会话 | minimax-chat |
| 8083 | 模型路由 | minimax-model |
| 8084 | 记忆 | minimax-memory |
| 8085 | RAG 知识库 | minimax-rag |
| 8086 | 函数工具 | minimax-function |
| 8087 | 后台管理 | minimax-admin |
| 8088 | 多模态 | minimax-multimodal |
| 8089 | 监控 | minimax-monitor |
| 8090 | 智能体 | minimax-agent |
| 8091 | Prompt 模板 | minimax-prompt |
| 8095 | WebSocket | minimax-ws |

## 3. 一键部署

```bash
# 1. 下载脚本
curl -O https://raw.githubusercontent.com/liugl951127/miniLiugl/main/scripts/deploy-linux.sh
chmod +x deploy-linux.sh

# 2. 一键安装 (含 Java/Maven/Node/MariaDB/Redis/Nacos 全部依赖)
sudo ./deploy-linux.sh install

# 3. 启动顺序 (V5.12):
#    nacos (8848) → gateway (8080) → 12 微服务 (8081-8095) → nginx (3000)
sudo ./deploy-linux.sh start

# 4. 健康检查 (V5.12 新增)
sudo ./deploy-linux.sh e2e
```

## 4. 子命令 (V5.12)

| 子命令 | 用途 | V5.12 增强 |
|--------|------|-----------|
| `install` | 一键安装 (Java/Maven/Node/MariaDB/Redis/Nacos) | 新增 Nacos + Redis |
| `start` | 启动服务 (顺序: nacos→gateway→微服务→nginx) | 启动逻辑重写 |
| `stop` | 停止服务 (倒序) | 加 nacos/gateway |
| `restart` | 重启 | - |
| `status` | systemctl 状态 | 加 nacos/gateway/redis |
| **`e2e`** | **V5.12 HTTP 健康检查 13 服务** | **新增** |
| `logs [M]` | 跟踪日志 | 加 nacos/redis 支持 |
| `backup` | 备份数据库 + jar | - |
| `update` | git pull + 重打包 + 重启 | - |
| `uninstall` | 完全卸载 (保留数据) | - |

## 5. E2E 健康检查示例

```bash
$ sudo ./deploy-linux.sh e2e

══════════ V5.12 E2E 健康检查 (13 服务 + nginx + nacos) ══════════

[1] 基础设施
  nacos (8848)                    200 (code=200)
  redis (6379)                    timeout (code=N/A)
  mariadb (3306)                  timeout (code=N/A)

[2] 入口 (nginx :3000 -> gateway :8080)
  nginx /                         200 (code=200)
  api-docs                        302 (code=302)
  actuator/health                 200 (code=200)

[3] 网关 + 微服务 (走 lb://minimax-* 转发)
  gateway :8080                   200 (code=200)
  auth                            200 (code=200)
  chat                            200 (code=200)
  model                           200 (code=200)
  ... (12 项)

══════════ 结果: 15 通过 / 0 失败 ══════════
✅ 所有服务健康
```

## 6. systemd 服务清单 (V5.12)

```bash
# 基础设施
minimax-nacos.service       # Nacos 2.3.2 (8848)
mariadb.service             # MariaDB (3306, 系统自带)
redis-server.service        # Redis (6379, 系统自带)

# 应用层
minimax-gateway.service     # Spring Cloud Gateway (8080)
minimax-auth.service        # 12 个业务微服务
minimax-chat.service
minimax-model.service
minimax-memory.service
minimax-rag.service
minimax-function.service
minimax-admin.service
minimax-multimodal.service
minimax-monitor.service
minimax-agent.service
minimax-prompt.service
minimax-ws.service

# 前端层
minimax-frontend.service    # Vite preview (5173)
minimax-nginx.service       # nginx 入口 (3000)
```

## 7. 配置文件位置

| 文件 | 用途 |
|------|------|
| `/etc/systemd/system/minimax-*.service` | 16 个 systemd 服务 |
| `/etc/nginx/conf.d/minimax.conf` | nginx 入口配置 |
| `/opt/minimax/apps/*.jar` | 12 业务 jar + gateway.jar |
| `/opt/minimax/frontend/dist/` | 前端构建产物 |
| `/opt/minimax/nacos/conf/application.properties` | Nacos 配置 (V5.12) |
| `/opt/minimax/backups/` | 数据库 + jar 备份 |
| `/var/log/minimax/` | 13 个服务日志 (含 nacos/gateway) |
| `/var/log/redis/` | Redis 日志 |

## 8. 升级路径 (历史)

| 版本 | 日期 | 关键变更 |
|------|------|---------|
| V5.5 | 2026-06-21 | Spring Cloud Gateway 替换 12 直连 |
| V5.6 | 2026-06-21 | Dashboard 真实数据 + KG ECharts + 监控面板 |
| V5.7 | 2026-06-21 | Nacos 服务发现 + Resilience4j |
| V5.8 | 2026-06-21 | TraceId + 智能分包 + nginx gzip/br |
| V5.9 | 2026-06-21 | Dashboard 真实图表 + 告警规则 CRUD + WS 精确分流 |
| V5.10 | 2026-06-21 | Prometheus 全链路监控 + MetricsFilter |
| V5.11 | 2026-06-21 | API 文档聚合 + knife4j 统一配置 |
| V5.12 | 2026-06-21 | 部署脚本集成 Nacos + Gateway + E2E 健康检查 |

## 9. 故障排查

### 9.1 Nacos 启动慢
Nacos 启动需 20-30s, 部署脚本已 sleep 25. 如需更长:
```bash
sudo ./deploy-linux.sh stop
sudo systemctl stop minimax-nacos
sudo -u minimax bash /opt/minimax/nacos/bin/startup-standalone.sh &
# 观察启动日志
```

### 9.2 Gateway 找不到微服务
检查:
1. Nacos console (`http://localhost:8848/nacos`, nacos/nacos) 服务列表
2. 12 微服务是否注册成功 (看 nacos UI)
3. gateway 日志: `sudo ./deploy-linux.sh logs gateway`

### 9.3 端口冲突
- 80/3000: nginx
- 8080: gateway
- 8848: nacos
- 3306: mariadb
- 6379: redis
修改 deploy 脚本顶部变量即可。

## 10. 安全建议

1. **改默认密码**: Nacos (nacos/nacos), adminLiugl/Liugl@2026, Redis (minimax_redis_2024)
2. **JWT_SECRET 替换**: 用 `openssl rand -base64 32` 生成
3. **nginx 白名单**: 生产环境限制 IP
4. **HTTPS**: 加 Let's Encrypt 证书
5. **数据库**: 启用 MariaDB SSL

## 11. 备份策略

```bash
# 每日凌晨 3 点自动备份
echo "0 3 * * * root /opt/minimax/deploy-linux.sh backup" >> /etc/crontab

# 备份内容:
# - MariaDB 全库 (mysqldump)
# - 13 个 jar
# - Nacos 配置 (nacos/conf)
# - nginx 配置
```
