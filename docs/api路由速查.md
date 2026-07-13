# API 路由速查表 (V3.5.5+)

> nginx 统一代理 16 个后端服务, 通过路径前缀区分, 单端口 80 入口

## 服务端口映射

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
| `/api/v1/prompt/` | prompt | 8091 | 提示词 |
| `/api/v1/analytics/` | analytics | 8092 | 数据分析 |
| `/api/v1/pipeline/` | pipeline | 8093 | 训练流水线 |
| `/api/v1/ws/` | ws | 8095 | WebSocket |
| `/api/v1/gateway/` | gateway | 7080 | 业务网关 |
| `/api/v1/` (兜底) | gateway | 7080 | 业务聚合 |

## 特殊路径

| 路径 | 目标 | 说明 |
|------|------|------|
| `/` | `/usr/share/nginx/html` | 前端 SPA (Vue3) |
| `/healthz` | nginx 直返 `ok` | 健康检查 |
| `/ws/` | ws:8095 | WebSocket 升级 |
| `/sse/` | ai:8094 | SSE 流式输出 |
| `/h2-console/` | ai:8094 | H2 控制台 (开发用) |
| `/sw.js` | `/usr/share/nginx/html` | Service Worker (PWA) |

## 路由匹配规则

nginx 用 `location /api/v1/auth/ { ... }` 模式, 路径前缀精确匹配。
未匹配上的 `/api/v1/xxx` 走兜底 `location /api/v1/ { proxy_pass http://gateway_service; }`。

```
GET /api/v1/auth/login           → auth:8081
GET /api/v1/ai/intent/predict    → ai:8094
GET /api/v1/chat/sessions        → chat:8082
GET /api/v1/rag/knowledge        → rag:8085
GET /api/v1/agent/tasks          → agent:8088
GET /api/v1/monitor/metrics      → monitor:8089
GET /api/v1/admin/users          → admin:8090
GET /api/v1/pipeline/stages      → pipeline:8093
GET /api/v1/analytics/reports    → analytics:8092
GET /api/v1/ws/messages          → ws:8095
GET /api/v1/unknown              → gateway:7080 (兜底)
```

## 添加新服务

1. 新建 `backend/minimax-newservice/`
2. `application.yml` 配 `server.port`
3. 跑 `bash scripts/gen-nginx-config.sh` 自动生成
4. 跑 `sudo ./nginx/install-nginx.sh config` 重载

## 验证

```bash
# 健康
curl http://localhost/healthz
# → ok

# 任意服务
curl http://localhost/api/v1/ai/intent/predict \
    -X POST -H "Content-Type: application/json" \
    -d '{"text":"我要退款"}'
# → {"code":0,"data":{"intent":"complaint",...}}
```

