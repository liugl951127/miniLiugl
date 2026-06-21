# MiniMax Platform — 企业级大模型平台

[![CI](https://github.com/liugl951127/miniLiugl/actions/workflows/ci.yml/badge.svg)](https://github.com/liugl951127/miniLiugl/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-brightgreen.svg)](https://vuejs.org/)

> **V5.23 CI + 一键部署** · 13 个微服务 · Spring Cloud Gateway · Nacos 服务发现 · Prometheus 全链路监控 · TraceId 全链路追踪 · GitHub Actions 5-Job CI · 41+ 张数据表 · 11,454+ 行代码
>
> Java 17 + Spring Boot 3 + Spring Cloud Gateway + Nacos + Vue 3 + Element Plus + 大模型 + 向量库 + RAG + Function Calling + 多模态 + 监控 + 调优 + 可观测性

---

## 🎯 项目亮点

| 能力 | 落地方式 |
|------|----------|
| **多模型路由** | OpenAI 协议通用，6 个模型 (GPT-4o / MiniMax-Text / VL / Ollama / Qwen / Mock) |
| **真流式输出** | Java HttpClient + `BodyHandlers.ofLines` + 流式 SSE + 前端 ReadableStream |
| **短期记忆** | Redis + Caffeine 双层降级 + 自动摘要压缩 (35→10) |
| **长期记忆** | MySQL BLOB 存向量 + 余弦相似度 + 跨会话召回 + 偏好记忆 |
| **RAG 知识库** | DOCX/PDF/TXT 解析 + 智能分块 (500/50) + SHA-256 去重 + 引用来源 |
| **Function Calling** | 4 个内置工具 + LLM 工具循环 (5 轮) + 工具注册表 + 调用审计 |
| **多模态** | 图片上传 + 视觉模型 (gpt-4o / MiniMax-VL) + 格式探测 + Mock 降级 |
| **管理后台** | 跨服务 HTTP 聚合 + 统一审计 + 健康检查聚合 + ECharts 仪表盘 |
| **监控告警** | Micrometer + Prometheus + 5 类业务指标 + 5 条默认告警规则 |
| **调优** | Bucket4j 限流 + Caffeine 缓存 + 异步任务 + 请求日志 + 压测脚本 |

---

## 🏗️ 系统架构 (V5.12)

```
                        ┌────────────────────────┐
                        │  Browser / Mobile H5   │
                        └──────────┬─────────────┘
                                   │ :3000
                        ┌──────────▼─────────────┐
                        │  nginx (V5.8 优化)     │
                        │  gzip / br / sec hdr   │
                        └──────────┬─────────────┘
                                   │ /api/**
                        ┌──────────▼─────────────┐
                        │ Spring Cloud Gateway   │
                        │ :8080 (WebFlux)        │
                        │ - JwtAuthFilter        │
                        │ - Resilience4j         │
                        │ - TraceId (V5.8)       │
                        │ - 限流 (Redis 令牌桶)  │
                        └──────────┬─────────────┘
                                   │ lb://minimax-*
                        ┌──────────▼─────────────┐
                        │ Nacos 2.3.2  (V5.7)    │
                        │ :8848 服务发现          │
                        └──────────┬─────────────┘
                                   │
   ┌─────┬─────┬─────┬─────┬─────┬─┴────┬─────┬─────┬─────┬─────┬─────┐
   ▼     ▼     ▼     ▼     ▼     ▼      ▼     ▼     ▼     ▼     ▼     ▼
  auth  chat model memory rag function admin multi monitor agent prompt ws
  8081  8082 8083  8084  8085 8086   8087  8088 8089  8090  8091 8095
   │     │    │     │     │     │       │     │    │     │     │     │
   └─────┴────┴─────┴─────┴─────┴───────┴─────┴────┴─────┴─────┴─────┘
                                   │
                        ┌──────────▼──────────────┐
                        │ MariaDB / Redis / Nacos │
                        │ 3306    / 6379 / 8848   │
                        └─────────────────────────┘
```

**核心组件 (V5.5-V5.12)**:
- **Spring Cloud Gateway** (WebFlux): 13 路由 + 网关级 JWT + Resilience4j 降级 + TraceId
- **Nacos 2.3.2**: 服务发现 + 配置中心 (12 微服务 + gateway 自动注册)
- **nginx**: :3000 统一入口 + gzip/brotli 压缩 + security headers
- **Prometheus**: 13 服务 `/actuator/prometheus` + MetricsFilter 自动采点
- **WebSocket**: 双重路由 (`/ws/notifications` 直连 auth, 其他走 gateway)
```

| 模块 | 端口 | 端点数 | 测试 | 核心能力 |
|------|------|--------|------|----------|
| **gateway** | 8080 | 1 | - | 反向代理 + 路由 |
| **auth** | 8081 | 8 | 4 | JWT 双 token + Spring Security + RBAC |
| **chat** | 8082 | 8 | 3 | 会话/消息 + SSE 流式 + 取消 + 重试 |
| **memory** | 8084 | 16 | 26 | 短期/长期/偏好/摘要 + 跨会话 |
| **model** | 8083 | 6 | 10 | Provider 路由 + Bucket4j 限流 + 6 模型 |
| **rag** | 8085 | 11 | 19 | 文档解析 + 智能分块 + 检索 + 引用 |
| **function** | 8086 | 10 | 23 | 工具注册 + LLM 循环 + 审计 |
| **admin** | 8087 | 14 | 11 | 跨服务聚合 + 审计 + Dashboard |
| **multimodal** | 8088 | 3 | 7 | 图片理解 + 视觉模型 + 格式探测 |
| **monitor** | 8089 | 15 | 11 | Prometheus + 告警 + 健康详情 |
| **common** | - | 0 | 11 | 限流/缓存/异步/请求日志/JWT |
| **总计** | 11 | **92+** | **125** | 14 天闭环 |

---

## 🚀 5 分钟快速启动 (V5.12)

### 方式 A: 一键部署 (生产, 推荐)
```bash
git clone https://github.com/liugl951127/miniLiugl.git
cd miniLiugl
sudo ./scripts/deploy-linux.sh install    # 装 Java/Maven/Node/MariaDB/Redis/Nacos + 编译 + 启 systemd
sudo ./scripts/deploy-linux.sh e2e        # 一键健康检查 13 服务
```

**自动部署**: Nacos → Gateway → 12 微服务 → nginx, 端口 3000 统一入口

### 方式 B: Docker Compose (开发)
```bash
docker compose up -d
# 前端:  http://localhost:3000
# API:   http://localhost:3000/api/v1/<module>/...
# Nacos: http://localhost:8848/nacos  (nacos/nacos)
# API 文档: http://localhost:3000/api-docs
# 监控:  http://localhost:3000/admin/metrics
```

默认账号: `adminLiugl / Liugl@2026`

### 方式 C: 本地 jar (开发调试)
```bash
cd backend
mvn -B clean install -DskipTests
# 启 Nacos + 12 微服务 + gateway
java -jar minimax-nacos/target/...     # Nacos (用 docker 跑更简单)
java -jar minimax-gateway/target/...  # Gateway :8080
java -jar minimax-auth/target/...     # Auth :8081
# ... 其他 11 个

# 前端
cd ../frontend
npm install && npm run dev
# http://localhost:5173
```

**架构入口** (V5.5-V5.12):
| 入口 | URL | 用途 |
|------|-----|------|
| 前端 | http://localhost:3000 | 统一入口 (V5.8 优化) |
| API | http://localhost:3000/api/v1/... | 走 gateway (lb://minimax-*) |
| Nacos | http://localhost:8848/nacos | 服务发现 + 配置 |
| API 文档 | http://localhost:3000/api-docs | 13 服务聚合 (V5.11) |
| 监控 | http://localhost:3000/admin/metrics | Prometheus 数据 (V5.10) |

---

## 🧪 验证 (125 个测试用例)

```bash
cd backend
mvn -B test
# Tests run: 125, Failures: 0, Errors: 0, Skipped: 0
```

### 压测
```bash
bash scripts/benchmark.sh http://localhost:8081 /api/v1/auth/health 50 1000
# 输出: QPS / p50 / p95 / p99
```

---

## 🆕 V5 架构升级 (2026-06-21)

V5 系列 8 个版本聚焦**生产级架构能力**:

| 版本 | 关键能力 | 提交 |
|------|---------|------|
| **V5.5** | Spring Cloud Gateway (WebFlux) + 13 路由 + JwtAuthFilter | `2d3c9e7` |
| **V5.6** | Dashboard 真实数据 + KG ECharts + 监控面板 | `fd14fdf` |
| **V5.7** | Nacos 服务发现 + Resilience4j 熔断降级 + lb:// | `f91a70a` |
| **V5.8** | TraceId 全链路追踪 + 智能分包 + nginx gzip/br | `c49ec80` |
| **V5.9** | Dashboard 真实图表 + 告警规则 CRUD UI + WS 精确分流 | `97f73b7` |
| **V5.10** | Prometheus 全链路监控 + MetricsFilter + BaseController | `c5d93fd` |
| **V5.11** | API 文档聚合中心 (13 服务) + knife4j 统一配置 | `e9db693` |
| **V5.12** | 部署脚本集成 Nacos + Gateway + E2E 健康检查 | `5a06932` |
| **V5.13** | 架构文档完善 (README + ARCHITECTURE 11KB) | `0f95507` |
| **V5.14** | OpenTelemetry 分布式追踪 (W3C traceparent + Jaeger) | `a9c456c` |
| **V5.15** | E2E 自动化测试脚本 (35+ 用例 7 Phase) | `509d1f7` |
| **V5.16** | Agent 增强 (流式 SSE + Plan 模式 + 记忆集成) | `2564e5b` |
| **V5.17** | Multi-Agent 多智能体 (Planner + Executor + Critic) | `5381c78` |
| **V5.18** | 真实 LLM (Claude + Gemini + 多 Key 轮询) | `0914689` |
| **V5.19** | WebSocket 双向流 (pause/resume/steer/feedback) | `b812499` |
| **V5.20** | Docker Compose 全栈中间件 + PWA + 后端 i18n | `4704604` |
| **V5.21** | 统一 SQL (单文件 1448 行) + 一键部署脚本 | `ae5294d` |
| **V5.22** | deploy-minimax.sh 生产可用 (747 行 9 子命令) | `da1f4f1` |
| **V5.23** | GitHub Actions CI (5 Job) + check 命令 + 27/27 通过 | `pending` |

**V5 累计**: +7,500 行 / -3,800 行, 19 个新文档, 13 个 systemd 服务, **5 个 CI Job 自动验证**

---

## 🔄 CI/CD (V5.23 新增)

```yaml
# .github/workflows/ci.yml - 5 个并行 Job
jobs:
  backend:        # Maven 编译 + 测试 (MariaDB + Redis service)
  frontend:       # Node 22 构建 (npm registry.npmmirror.com)
  deploy-scripts: # bash -n + shellcheck + YAML lint
  sql-check:      # 数 CREATE TABLE/INSERT, 实导入验证
  summary:        # 汇总 4 个 job 结果到 GitHub Step Summary
```

**触发**: push main / PR / workflow_dispatch

**快速验证**:
```bash
./scripts/deploy-minimax.sh check    # 静态检查 (27 项)
```

---

## 🎨 14 天路线图 (全部完成 ✅)

| Day | 模块 | 端点 | 测试 | 报告 |
|-----|------|------|------|------|
| 1 | 项目骨架 | - | - | [report](reports/day-1-report.md) |
| 2 | 用户鉴权 (JWT) | 8 | 4 | [report](reports/day-2-report.md) |
| 3 | 会话消息 | 8 | 3 | [report](reports/day-3-report.md) |
| 4 | 模型路由 (6 模型) | 6 | 10 | [report](reports/day-4-report.md) |
| 5 | SSE 真流式 | - | 3 | [report](reports/day-5-report.md) |
| 6 | 短期记忆 (Redis+Caffeine) | 6 | - | [report](reports/day-6-report.md) |
| 7 | 长期记忆 (向量) | 16 | 12 | [report](reports/day-7-report.md) |
| 8 | RAG (知识库) | 11 | 19 | [report](reports/day-8-report.md) |
| 9 | Function Calling (4 工具) | 10 | 23 | [report](reports/day-9-report.md) |
| 10 | 管理后台 (跨服务) | 14 | 11 | [report](reports/day-10-report.md) |
| 11 | 多模态 + 醒目 UI | 3 | 7 | [report](reports/day-11-report.md) |
| 12 | 监控 (Prometheus) | 15 | 11 | [report](reports/day-12-report.md) |
| 13 | 调优 (限流/缓存/异步) | - | 11 | [report](reports/day-13-report.md) |
| 14 | 交付 (本文档) | - | - | [report](reports/day-14-report.md) |

---

## 💼 3 大典型业务场景

### 场景 1: 智能客服
```
用户: 我们的产品支持哪些支付方式?
   ↓ chat 模块
短期记忆: 上下文
   ↓ RAG
检索产品手册: 命中 3 段 (微信/支付宝/银联/...)
   ↓ LLM + 引用
AI: 支持微信、支付宝、银联 [来源 1] [来源 2] [来源 3]
```

### 场景 2: 工具增强助手
```
用户: 上海现在几点? 帮我算 (123+456)*789
   ↓ Function Calling
Round 1: get_current_time(timezone="Asia/Shanghai")
         calculator(expression="(123+456)*789")
   ↓ 工具执行
Round 2: LLM 整合
AI: 上海 23:00, (123+456)*789 = 456897
```

### 场景 3: 多模态理解
```
用户: [上传截图] 这张图里的 bug 怎么修?
   ↓ 多模态
视觉模型描述: "看到 LoginController 第 42 行的 if 判断逻辑..."
   ↓ LLM 整合
AI: 截图显示您的 NPE 是因为 user 可能为 null,
   建议: 加 if (user == null) throw new BizException(...)
```

---

## 🛡️ 关键技术亮点

### 1. 限流 — Bucket4j
- 多维度: IP / User / Global
- 可配置: yml 调阈值
- 业务侧自动集成

### 2. 缓存 — Caffeine
- 防击穿: `getOrLoad` loader 仅调一次
- TTL 自动过期
- 命中率统计

### 3. 异步 — UUID 任务
- 状态机: pending/running/done/failed
- 失败重试 (3 次)
- 结果回调 + Future

### 4. 监控 — Prometheus
- 5 类业务指标自动暴露
- 5 条默认告警规则
- 自动恢复 (firing → resolved)

### 5. 部署 — 一键
- Windows / Linux 单机 / K8s 集群
- Docker Compose 全栈
- Helm Chart (Day 14 交付)

---

## 🔒 安全清单 (生产)

- [x] JWT 双 token (30min access + 7d refresh)
- [x] BCrypt 密码编码
- [x] Spring Security 6 RBAC
- [x] SQL 注入防护 (MyBatis-Plus 参数化)
- [x] SSRF 防护 (HttpGetTool 阻止内网)
- [x] 字符白名单 (CalculatorTool 阻止注入)
- [x] SHA-256 去重 (RAG 文档)
- [ ] HTTPS (生产部署)
- [ ] WAF (应用层防火墙)
- [ ] RBAC 资源级 (生产细化)

---

## 📊 资源需求

| 规模 | CPU | RAM | Disk | 部署 |
|------|-----|-----|------|------|
| **开发** | 4 核 | 8 GB | 50 GB | docker compose |
| **小规模** | 8 核 | 16 GB | 200 GB | 单机多副本 |
| **中规模** | 32 核 | 64 GB | 500 GB | K8s 3 节点 |
| **大规模** | 128+ 核 | 256+ GB | 1 TB+ | K8s 多节点 + ES |

---

## 📞 运维

### 启动
```bash
# Docker
docker compose up -d

# 单机 jar
./deploy/linux-single/deploy-linux-single.sh jar

# K8s
./deploy/linux-cluster/deploy-linux-cluster.sh k8s
```

### 健康
```bash
curl http://localhost:8081/api/v1/auth/health
curl http://localhost:8089/api/v1/monitor/health
```

### 压测
```bash
bash scripts/benchmark.sh http://localhost:8081 /api/v1/auth/health 50 1000
```

### 备份
```bash
docker exec minimax-mysql mysqldump -uroot -pMinMax2026! minimax | gzip > backup_$(date +%F).sql.gz
```

---

## 📜 License

MIT

---

## 🏆 总结 (Day 1-14 + V5.5-V5.12)

| 指标 | 值 |
|------|---|
| **后端模块** | **13** (V5.5 加 gateway) |
| **微服务** | **12 + gateway + common** |
| Java 文件 | 191+ |
| Java 行数 | 11,454+ |
| SQL 文件 | 9 (含 20_notification.sql) |
| SQL 行数 | 963+ |
| YAML/XML | 2,800+ |
| 单元/集成测试 | 125 (0 失败) |
| HTTP 端点 | 120+ |
| 数据表 (MySQL) | 41+ (含 wechat/oauth/notification/alert) |
| systemd 服务 | 16 (含 nacos/gateway) |
| 文档 | 16 份 (Day 1-14 + V5.5-V5.12 升级) |
| 部署脚本 | 3 (deploy-linux.sh + docker-compose + windows) |
| Git commits | 30+ (含 V5 系列 8 个) |
| **可观测性** | **Prometheus + TraceId + 告警规则 + Dashboard** |
| **服务发现** | **Nacos 2.3.2** |
| **API 网关** | **Spring Cloud Gateway (WebFlux)** |
| **API 文档** | **knife4j 聚合 (V5.11)** |

**V5 关键创新**:
- 网关级 JWT 鉴权 (所有路由统一拦截, 业务模块零感知)
- lb:// 服务发现 (业务模块无需硬编码 URL, 扩缩容自动发现)
- TraceId 全链路 (gateway 注入 → 日志/响应头 → 前端 5xx 错误带 traceId)
- Prometheus 自动采点 (13 服务统一指标 + MetricsFilter URI 归一化)
- 告警规则 CRUD (UI 弹窗编辑阈值/服务/通知渠道, 5 条预置规则)
- WebSocket 精确分流 (`/ws/notifications` 直连 auth 绕过 gateway)
- 部署一键化 (`./deploy-linux.sh install` + `./deploy-linux.sh e2e`)

**这是一个完整可投产的企业级大模型平台。**
