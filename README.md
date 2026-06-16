# MiniMax Platform — 企业级大模型平台

> **14 天极限构建 · 11 个微服务 · 191 个 Java 文件 · 11,454 行代码 · 125 个测试用例 · 0 失败**
>
> Java 17 + Spring Boot 3 + Vue 3 + Element Plus + 大模型 + 向量库 + RAG + Function Calling + 多模态 + 监控 + 调优

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

## 🏗️ 系统架构

```
┌──────────────────────────────────────────────────────────────────────────┐
│                       Frontend  (Vue 3 + Element Plus)                  │
│  Markdown · 代码高亮 · 拖拽上传 · 流式打字机 · Admin Dashboard          │
└────────────────────────┬─────────────────────────────────────────────────┘
                         │  /api/v1/*
┌────────────────────────▼─────────────────────────────────────────────────┐
│                  Gateway  :8080  (反向代理 / 限流)                       │
└──┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬─────────┘
   │      │      │      │      │      │      │      │      │      │
   ▼      ▼      ▼      ▼      ▼      ▼      ▼      ▼      ▼      ▼
 auth  chat  memory  model   rag   func   admin  multi  moni  common
8081  8082  8084   8083  8085  8086  8087   8088   8089   shared
                                                              + gateway
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

## 🚀 5 分钟快速启动

### 前置
- Docker 20.10+ (推荐) 或 JDK 17 + Maven 3.8+

### 方式 A: Docker Compose (推荐)
```bash
git clone https://github.com/liugl951127/miniLiugl.git
cd miniLiugl

# 启动基础设施 + 应用
docker compose up -d

# 访问
# 前端:  http://localhost
# API:   http://localhost:8080
# 监控:  http://localhost:8089/actuator/prometheus
```

默认账号: `admin / admin@123`

### 方式 B: 本地 jar (开发模式)
```bash
# 装 JDK 17 + Maven
# Ubuntu: apt install openjdk-17-jdk maven
# Mac:    brew install openjdk@17 maven

cd backend
mvn -B clean install -DskipTests
java -jar minimax-auth/target/minimax-auth.jar --spring.profiles.active=test --server.port=8081
# 重复启其他 6 个服务...

# 启动前端
cd ../frontend
npm install && npm run dev
# http://localhost:5173
```

### 方式 C: 一键部署 (生产)
参考 [`deploy/`](deploy/) 目录:
- `deploy/windows/deploy-windows.bat` — Windows 单机
- `deploy/linux-single/deploy-linux-single.sh` — Linux 单机
- `deploy/linux-cluster/deploy-linux-cluster.sh` — K8s 集群

```bash
# Linux 单机
bash deploy/linux-single/deploy-linux-single.sh docker

# K8s 集群
bash deploy/linux-cluster/deploy-linux-cluster.sh k8s minimax
```

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

## 🏆 14 天总结

| 指标 | 值 |
|------|---|
| 后端模块 | 11 |
| Java 文件 | 191 |
| Java 行数 | 11,454 |
| SQL 文件 | 8 |
| SQL 行数 | 963 |
| YAML/XML | 2,500+ |
| 单元/集成测试 | 125 (0 失败) |
| HTTP 端点 | 92+ |
| 数据表 (MySQL) | 18+ |
| 部署脚本 | 4 (Win/Linux-Single/Linux-Cluster/DB-Init) |
| Dockerfiles | 6 |
| K8s manifests | 10 |
| 报告 | 14 份 (Day 1-14) |
| Git commits | 14+ |

**这是一个完整可投产的企业级大模型平台。**
