# MiniMax Platform - Nginx 完整配置 (V3.5.5+)

## 📁 文件清单

| 文件 | 作用 | 大小 |
|------|------|------|
| `install-nginx.sh` | 一键部署 (装 + 配置 + 启动) | 7KB |
| `nginx.conf` | 主配置 (SPA + 16 API + WS + SSE) | 11KB |
| `upstream.conf` | 16 个后端 upstream | 2.3KB |
| `security-headers.conf` | CSP / XSS / CORS 安全 headers | 2KB |
| `README.md` | 本文档 | 4KB |

## 🎯 覆盖的 16 个微服务

| 路径前缀 | 服务 | 端口 | 用途 |
|----------|------|------|------|
| `/api/v1/auth/` | auth | 8081 | 用户鉴权 / RBAC |
| `/api/v1/ai/` | ai | 8094 | AI 核心 (意图/知识库/Spark) |
| `/api/v1/chat/` | chat | 8082 | 会话 + 消息 |
| `/api/v1/memory/` | memory | 8083 | 长期记忆 |
| `/api/v1/model/` | model | 8084 | 模型管理 + Battle |
| `/api/v1/rag/` | rag | 8085 | RAG 检索 |
| `/api/v1/function/` | function | 8086 | Function Call |
| `/api/v1/multimodal/` | multimodal | 8087 | 多模态 |
| `/api/v1/agent/` | agent | 8088 | Agent 自主任务 |
| `/api/v1/monitor/` | monitor | 8089 | 监控 + 告警 |
| `/api/v1/admin/` | admin | 8090 | 后台管理 |
| `/api/v1/prompt/` | prompt | 8091 | 提示词管理 |
| `/api/v1/analytics/` | analytics | 8092 | 数据分析 |
| `/api/v1/pipeline/` | pipeline | 8093 | 训练流水线 |
| `/api/v1/ws/` | ws | 8095 | WebSocket |
| `/api/v1/gateway/` | gateway | 7080 | 业务网关 |

## 🚀 部署

```bash
# 一键部署 (装 nginx + 配置 + 启动)
sudo ./nginx/install-nginx.sh install

# 重新加载配置
sudo ./nginx/install-nginx.sh config

# 状态
sudo ./nginx/install-nginx.sh status

# 实时日志
sudo ./nginx/install-nginx.sh logs

# HTTPS (certbot)
sudo ./nginx/install-nginx.sh https example.com
```

## 🌐 特殊路径

| 路径 | 目标 | 说明 |
|------|------|------|
| `/` | `/usr/share/nginx/html` | 前端 SPA (Vue3) |
| `/healthz` | nginx 直返 `ok` | 健康检查 |
| `/api/v1/{module}/*` | 对应 upstream | 16 个微服务 |
| `/ws/` | ws:8095 | WebSocket 升级 |
| `/sse/` | ai:8094 | SSE 流式输出 |
| `/h2-console/` | ai:8094 | H2 数据库控制台 (开发) |
| `/sw.js` | 前端 dist | Service Worker (PWA) |
| `/favicon.ico` | 前端 dist | 网站图标 |

## 🔒 安全 headers (security-headers.conf)

- **X-Frame-Options: SAMEORIGIN** - 防止点击劫持
- **X-Content-Type-Options: nosniff** - 防止 MIME 嗅探
- **X-XSS-Protection** - 浏览器 XSS 过滤
- **Referrer-Policy** - Referrer 策略
- **Permissions-Policy** - 限制浏览器特性
- **Content-Security-Policy** - CSP 内容安全
- **HSTS** (HTTPS 启用) - 强制 HTTPS

## 📊 性能优化

- **Gzip 压缩** (level 6) - 文本/JSON/JS/CSS/SVG
- **静态资源 1 天缓存** (js/css/png/svg/woff 等)
- **immutable 标记** - 浏览器不会重新验证
- **keepalive 32** - upstream 连接复用
- **server_tokens off** - 隐藏版本号

## 🔧 故障排查

| 现象 | 排查 |
|------|------|
| 502 Bad Gateway | 容器未启动：`docker ps`，`curl 127.0.0.1:8081/actuator/health` |
| 404 Not Found | 前端 dist 没复制：`ls /usr/share/nginx/html/` |
| SPA 路由 404 | SPA fallback 没生效：看 nginx.conf location / |
| WebSocket 断连 | `Upgrade` / `Connection` 头被过滤：检查 `proxy_set_header` |
| SSE 没流式输出 | `proxy_buffering off` 没设：检查 SSE location |
| HTTPS 证书失败 | DNS 未解析 / 80 端口被占：`certbot renew --dry-run` |
