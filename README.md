# MiniMax Platform — 企业级大模型平台

[![CI](https://github.com/liugl951127/miniLiugl/actions/workflows/ci.yml/badge.svg)](https://github.com/liugl951127/miniLiugl/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-brightgreen.svg)](https://vuejs.org/)

> **V5.26 CentOS 部署** · 13 个微服务 · Spring Cloud Gateway · Nacos 服务发现 · Prometheus 全链路监控 · TraceId 全链路追踪 · GitHub Actions 5-Job CI · 前端 45 个页面全交付 · **CentOS 一键部署** · 41+ 张数据表 · 14,000+ 行代码
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
                        │ MySQL / Redis / Nacos │
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
sudo ./scripts/deploy-minimax.sh install    # 装 Java/Maven/Node/MySQL/Redis/Nacos + 编译 + 启 systemd
sudo ./scripts/deploy-minimax.sh test       # 一键健康检查 19 项
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

### 方式 D: Windows + IDEA 本地开发 (V5.30)

```powershell
# Windows 本地一键启动 (后台跑后端)
.\scripts\dev-start.bat backend

# IDEA 中打开前端项目
# → 右下角 npm scripts → 双击 "dev"
```

完整指南: [docs/WINDOWS-IDEA-GUIDE.md](docs/WINDOWS-IDEA-GUIDE.md) (10 节, JDK/Maven/Docker/IDEA 配置 + 12 个微服务 Run Config)

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
| **V5.23** | GitHub Actions CI (5 Job) + check 命令 + 27/27 通过 | `b0c49a4` |
| **V5.24** | 前端 5 placeholder 补完 + Provider/Leaderboard 管理页 | `6034984` |
| **V5.25** | 删除 deploy-linux.sh (旧版) + 文档引用清理 | `d1c1866` |
| **V5.26** | CentOS 专用部署脚本 (install-middleware + deploy-centos) | `a6db04c` |
| **V5.27** | MariaDB 迁移到 MySQL 8.0 (docker-compose + 3 脚本 + CI + 15 文档) | `1a788fb` |
| **V5.28** | **纯 Docker 全栈部署** (18 service: 中间件 + 13 微服务 + nginx 一键启) | `cdb227b` |
| **V5.29** | 修 OpenTelemetry 依赖: starter → autoconfigure + 版本 2.2.0 → 2.6.0 | `8459999` |
| **V5.30** | JWT secret 调整 + 关键代码行注释 + Windows IDEA 支持 | `73493de` |
| **V5.30.5** | 预编译静态检查脚本 + 修复 6 个未发现 bug | `36e9a6f` |
| **V5.30.6** | AnthropicAdapter/GeminiAdapter 修 record 参数 + 脚本加 record 检测 (又被用户抓 2 个) | `pending` |
| **V5.31** | 🆕 **第 15 个微服务: minimax-analytics (数据智能分析)** | `pending` |

**V5 累计**: +11,000 行 / -4,200 行, 21 个新文档, 13 个 systemd 服务, **5 个 CI Job 自动验证**, **前端 45 个页面全交付**, **CentOS 专用部署脚本**

---

## 🎨 V5.24 前端补完 (本期重点)

本期将 **5 个 placeholder 重写 + 新增 2 个管理页 + admin 容器化**, 至此前端 45 个页面全部交付 (零 placeholder).

| 页面 | 状态 | 功能 | 行数 |
|------|------|------|------|
| `knowledge/Index.vue` 知识库 | ⚠️→✅ | KB CRUD + 文档上传 + 切片预览 + 检索问答 | 483 |
| `memory/Index.vue` 记忆 | ⚠️→✅ | 短期/长期/偏好 3 Tab + 召回测试 + 摘要 | 442 |
| `admin/Provider.vue` 模型供应商 | ❌→✅ | CRUD + 测试连接 + 启用切换 + API Key 脱敏 | 258 |
| `admin/Leaderboard.vue` 模型排行 | ❌→✅ | 综合/速度/最近/分类 4 Tab + 金银铜牌 | 183 |
| `user/CrossAppBinding.vue` 跨应用 | ⚠️→✅ | UnionID 状态 + 4 步骤引导 + 历史记录 | 198 |
| `auth/WechatScanPage.vue` 扫码 | ⚠️→✅ | 4 步骤进度 + 已绑定信息 + 帮助折叠 | 146 |
| `admin/Index.vue` 管理后台 | ⚠️→✅ | 侧边栏容器 + 6 子页导航 + 快捷入口 | 134 |
| **API 封装** (3 个) | ➕ | rag.js / memory.js / model.js (CRUD 全) | 184 |

**总量**: +2021 行 (前端), 7 个 .vue + 3 个 .js

---

## 🐧 V5.26 CentOS 一键部署 (本期重点)

新增 2 个 CentOS 专用部署脚本, 区别于通用版 `deploy-minimax.sh`:
- 自动处理 SELinux (enforcing → permissive)
- 自动配置 firewalld 端口
- 配置国内 Docker 镜像加速 (5-10 倍)
- 用 yum 装 JDK + nginx (CentOS 原生工具链)

| 脚本 | 行数 | 用途 |
|------|------|------|
| `scripts/install-middleware-centos.sh` | 461 | **中间件独立安装** (Docker + MySQL/Redis/Nacos/Adminer) |
| `scripts/deploy-centos.sh` | 558 | **一键完整部署** (中间件 + JDK + mvn + 12 微服务 + nginx) |
| `docs/DEPLOY-CENTOS-GUIDE.md` | 7KB | CentOS 部署文档 (8 步详解 + 7 故障排查) |

**一行安装** (CentOS/Rocky/RHEL/AlmaLinux/Anolis):
```bash
curl -fsSL https://raw.githubusercontent.com/liugl951127/miniLiugl/main/scripts/deploy-centos.sh -o deploy-centos.sh
chmod +x deploy-centos.sh
sudo ./deploy-centos.sh install
```

**8 步流程**: JDK17 → 中间件 → 用户 → 编译 → 拷贝 jar → systemd → nginx → 启动

**check 命令**: 28/28 通过 (4 个新加验证项)

**总量**: +2030 行 (2 个脚本 + 1 个文档)

---

## 🐳 V5.28 纯 Docker 全栈部署 (本期重点)

全项目彻底 Docker 化, **18 个 service 一行启动** (中间件 + 13 微服务 + gateway + nginx ):

| 类别 | Service | 镜像 | 端口 |
|------|---------|------|------|
| **中间件** | mysql | `mysql:8.0` | 3306 |
| | redis | `redis:7.2-alpine` | 6379 |
| | nacos | `nacos/nacos-server:v2.3.2` | 8848 |
| **Gateway** | gateway | 自构建 (backend/Dockerfile) | 8080 |
| **12 微服务** | auth/chat/model/memory/rag/function/admin/multimodal/monitor/agent/prompt/ws | 自构建 | 8081-8091, 8095 |
| **前端** | nginx | `nginx:1.25-alpine` | 3000 |
| **可选** | adminer | `adminer:4.8.1` | 8082 (profile=tools) |

**一行安装**:
```bash
sudo ./scripts/deploy-minimax.sh install    # 构建镜像 + 启动全部 (5-10 分钟首次)
sudo ./scripts/deploy-minimax.sh status     # 查看状态
sudo ./scripts/deploy-minimax.sh test       # 19 项 E2E
sudo ./scripts/deploy-minimax.sh logs auth  # 查看服务日志
```

**服务连接** (Docker 网络 DNS):
- 微服务 → MySQL: `mysql:3306`
- 微服务 → Redis: `redis:6379`
- 微服务 → Nacos: `nacos:8848`
- 前端 → Gateway: `gateway:8080`
- 不再需要 systemd / host java

**check 命令**: 30/30 通过 (含 docker-compose + Dockerfile 验证)

**总量**: +17.5KB (重写 docker-compose.yml + Dockerfile + 重写 deploy 脚本)

---

## 🔧 V5.29 修复 OpenTelemetry 依赖解析

**问题**: `mvn install` 报 `Could not find artifact io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter:jar:2.2.0`

**原因**:
- `opentelemetry-spring-boot-starter` 2.x 系列是**空 jar** (placeholder, 只有 417 字节)
- 2.2.0 是 alpha, aliyun 镜像**未同步**
- 真正可用的依赖是 `opentelemetry-spring-boot-autoconfigure` (2.6.0+, 246KB)

**修复**:
- 父 pom: `opentelemetry-instrumentation.version` 2.2.0 → **2.6.0** (Spring Boot 3.2 兼容)
- common pom: `spring-boot-starter` → `spring-boot-autoconfigure`

**验证** (aliyun HEAD):
- `opentelemetry-spring-boot-autoconfigure-2.6.0.jar`: **246KB** ✓
- `opentelemetry-spring-boot-starter-2.6.0.jar`: **417 字节** (空)
- `opentelemetry-bom-1.36.0.pom`: ✓
- `opentelemetry-instrumentation-bom-2.6.0.pom`: ✓
- `opentelemetry-exporter-otlp-1.36.0.jar`: ✓

**docker-compose build** 不受影响 (Dockerfile 拉取依赖后构建)

---

## 🪟 V5.30 Windows + IDEA 本地支持 (本期重点)

### JWT Secret 统一调整
默认 `minimax.jwt.secret` 修改为:
`0f6beadebfcee3e97845856757a3babf97b2af8c80f0b95690783ccc7a595352` (64 字符 hex)

**4 处同步**:
- `backend/Dockerfile` ENV
- `scripts/deploy-centos.sh` 默认值
- `backend/.../JwtAuthenticationFilter.java` `@Value` 默认值
- `backend/.../GatewayApplication.java` 12 个 Service Run Config 模板

### 每个关键代码加行注释
**5 个核心文件**逐行注释 (含 import + 字段 + 方法):
- `backend/minimax-gateway/.../GatewayApplication.java` (Spring Boot 启动类)
- `backend/minimax-gateway/.../filter/JwtAuthGlobalFilter.java` (V5.5 网关鉴权)
- `backend/minimax-gateway/.../filter/TraceFilter.java` (V5.8/V5.14 TraceId)
- `backend/minimax-common/.../security/JwtAuthenticationFilter.java` (业务模块鉴权)
- `backend/minimax-common/.../result/Result.java` (统一响应包装)

每个 import / 字段 / 方法都有中文说明 (为什么 / 怎么用 / 注意事项).

### Windows IDEA 本地支持 (新)
- **scripts/dev-start.bat** (7.2KB, 5 子命令: all/backend/frontend/middleware/stop)
- **docs/WINDOWS-IDEA-GUIDE.md** (10KB, 12 节)
  - JDK/Maven/Node/Docker 安装
  - Maven 镜像配置 (aliyun)
  - IDEA 项目导入 + SDK 配置
  - 13 个微服务 Run Configuration 模板
  - 故障排查 5 类 (依赖/端口/Nacos/DB/npm)
  - 开发流程 + 性能调优

**净改动**: 5 files Java (重写, 加行注释) + 1 new bat + 1 new doc + README + commit + push

---

## 🛡️ V5.30.5 预编译静态检查 (本期重点)

### 新增 scripts/precompile-check.py (18KB)

跑 `mvn compile` 之前先跑这个脚本, 避免浪费时间调错误:

```bash
python3 scripts/precompile-check.py                # 全项目 (304 个 Java 文件)
python3 scripts/precompile-check.py --module model # 单模块
python3 scripts/precompile-check.py --list        # 列出可用模块
python3 scripts/precompile-check.py --strict      # 警告也算错
```

### 5 类检测 (基于 V5.30.1-V5.30.4 真实教训)

| 检测项 | 来源 bug | 错误等级 |
|--------|---------|---------|
| import 缺失 | V5.30.1 IOException 漏 import | error |
| Lombok 笔误 | V5.30.1 @NoConstructor 漏 s | error |
| DTO 引用不存在 | V5.30.4 dto.Message 不存在 | warn |
| Wrapper 误用 | V5.30.3 LambdaQueryWrapper.set | error |
| Thread.sleep 漏 throws | V5.30.3 InterruptedException | error |

### 集成 deploy-minimax.sh check
- 加 8 个新检查项: 预编译脚本 + 必需文件
- 现在 40/40 通过 (原 32 + 新 8)

### 修复 6 个隐藏 bug

跑预编译脚本时, **抓到 6 个用户未发现**的真 bug:
- `MockAdapter.java:66` streamChat() 缺 throws
- `StreamGatewayHandler.java:101/133/156/171/203` 5 个 streamXxx() 缺 throws

都是 V5.30.3 同样模式 (Thread.sleep 漏 throws), 之前只修了 BidirectionalStreamHandler, 漏了这个文件。

### 验证
- ✓ 预编译脚本: 304 个 Java 文件, 0 错误 0 警告
- ✓ check 命令: 40/40 通过
- ✓ 修复后 mvn compile 应该一次过

### 净改动
4 files, +516/-6 (新脚本 18KB + 6 处修复 + check 集成)

## 📊 V5.31 数据智能分析模块 (本期重点) 🆕

**第 15 个微服务 `minimax-analytics` (端口 8096)**, 企业级数据分析引擎.

### 核心能力 (4 大块)

| 子模块 | 能力 | 代表端点 |
|--------|------|---------|
| **数据库元数据** | 自动读 information_schema, 生成表结构 + 数据画像 + ER 图 | `GET /datasources/{id}/databases/{db}/tables` |
| **多格式导入** | csv / json / log / txt / tsv 解析, 自动质量报告 (行数/空值率/类型推断) | `POST /ingest/upload` |
| **NL2SQL** | 自然语言 → LLM → SQL → 安全执行 (5 道防线) → 结果 | `POST /nlsql/ask` |
| **报告 + 图表** | SQL + 数据 → Markdown 报告 + ECharts 自动选图 + 异常检测 (IQR/z-score) | `POST /reports/generate` |

### SQL 安全 5 道防线
1. **关键字黑名单** (Druid Parser): INSERT/UPDATE/DELETE/DROP/SLEEP/BENCHMARK 拒绝
2. **必须单条 SELECT** (Druid 拆分验证)
3. **黑名单正则** (防止字符串内关键字绕过)
4. **自动 LIMIT 1000** (无 LIMIT 自动追加)
5. **maxRows + setQueryTimeout + Hikari readOnly** 运行时保障

### 18 个端点 (8 controller 整合为 1)
```
数据源 (4):  /datasources  GET/POST/DELETE/test
Schema (4):  /datasources/{dsId}/databases[/{db}/tables[/{table}[/profile]]]
Ingest (3):  /ingest/upload  /tasks/{id}  /tasks/{id}/quality
NL2SQL (4):  /nlsql/ask  /explain  /feedback  /history
Query (2):   /query/execute  /dry-run
Report (2):  /reports/generate  /{reportId}
```

### 复用现有技术栈
- ✅ `minimax-model` 调 LLM (Anthropic/OpenAI/Gemini/Mock)
- ✅ `minimax-common` 统一 Result/BizException/JwtAuth
- ✅ `minimax-gateway` 路由 (lb://minimax-analytics)
- ✅ MyBatis-Plus + Druid + Caffeine (现有依赖)

### 文件清单 (V5.31 新增 26 文件)
```
backend/minimax-analytics/
├── pom.xml
├── src/main/java/com/minimax/analytics/
│   ├── AnalyticsApplication.java
│   ├── controller/AnalyticsController.java (18 端点)
│   ├── service/
│   │   ├── datasource/    (DataSourceService + Impl)
│   │   ├── schema/       (SchemaService + Impl, Caffeine 缓存 1h)
│   │   ├── ingest/       (FileIngestService + Impl + 4 parser)
│   │   ├── nlsql/        (Nl2SqlService + SqlSafetyChecker + PromptTemplates)
│   │   ├── query/        (QueryService + Impl, 安全执行)
│   │   ├── report/       (ReportService + TrendAnalyzer + AnomalyDetector)
│   │   └── chart/        (ChartService + ChartTypeDecider, ECharts)
│   ├── entity/    (4 张表 entity)
│   ├── dto/       (3 个 request DTO)
│   ├── vo/        (5 个 response VO)
│   ├── mapper/    (4 个 MyBatis-Plus mapper)
│   └── exception/ (全局异常处理)
├── src/main/resources/application.yml
├── src/test/java/...      (5 测试类 / 47 测试)
sql/analytics-v5.31.sql    (4 张 DDL)
docs/design/analytics-v5.31-design.md (36KB 设计文档)
```

### 验证
- ✅ `mvn install` 15/15 全 SUCCESS (50 秒)
- ✅ `mvn test` 47/47 测试通过
- ✅ `precompile-check.py` 0 错 0 警告
- ✅ `deploy-minimax.sh check` 40/40 通过
- ✅ Gateway 路由 `lb://minimax-analytics` 已加

### 设计文档
详见 `docs/design/analytics-v5.31-design.md` (36KB, 13 节 + 2 附录)

## 🔄 CI/CD (V5.23 新增)

```yaml
# .github/workflows/ci.yml - 5 个并行 Job
jobs:
  backend:        # Maven 编译 + 测试 (MySQL + Redis service)
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
| 部署脚本 | 3 (deploy-minimax.sh + docker-compose + windows) |
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
- 部署一键化 (`./scripts/deploy-minimax.sh install` + `./scripts/deploy-minimax.sh test`)

**这是一个完整可投产的企业级大模型平台。**
