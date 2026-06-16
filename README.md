# MiniMax 大模型平台

一个企业级、可运行、可二次开发的大模型应用平台，对标 Minimax / OpenAI Web / Cursor 等产品形态。

## 架构概览

```
┌──────────────────────────────────────────────────────────────┐
│                        Frontend (Vue 3)                      │
│   Element Plus + Vite + Pinia + Vue Router + Axios + SSE     │
└──────────────────────────────────────────────────────────────┘
                              │  HTTPS / SSE
                              ▼
┌──────────────────────────────────────────────────────────────┐
│                  Gateway (Spring Cloud Gateway)              │
│         限流 / 鉴权 / 日志 / 路由 / WebFlux                  │
└──────────────────────────────────────────────────────────────┘
                              │
   ┌──────────────┬───────────┼────────────┬──────────────┐
   ▼              ▼           ▼            ▼              ▼
┌────────┐  ┌──────────┐  ┌────────┐  ┌─────────┐  ┌──────────┐
│  Auth  │  │   Chat   │  │ Memory │  │   RAG   │  │  Model   │
│  JWT   │  │  SSE     │  │ Redis  │  │ Vector  │  │  Router  │
│  RBAC  │  │  Stream  │  │ + ES   │  │  + LLM  │  │ OpenAI   │
└────────┘  └──────────┘  └────────┘  └─────────┘  └──────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│           MySQL 8  +  Redis 7  +  Elasticsearch 8           │
│           MinIO (对象存储)  +  Prometheus + Grafana          │
└──────────────────────────────────────────────────────────────┘
```

## 模块清单（14 天路线图）

- [x] **Day 1** 项目骨架 + Docker 一键启动
- [ ] **Day 2** 用户体系 + JWT 鉴权
- [ ] **Day 3** 会话模块 CRUD
- [ ] **Day 4** 模型路由层（OpenAI 兼容）
- [ ] **Day 5** 流式对话（SSE）⭐ 核心
- [ ] **Day 6** 短期记忆（Redis）
- [ ] **Day 7** 长期记忆（向量库 + 摘要）
- [ ] **Day 8** 知识库 RAG
- [ ] **Day 9** 工具调用 Function Calling
- [ ] **Day 10** 管理后台
- [ ] **Day 11** 多模态上传
- [ ] **Day 12** 监控埋点
- [ ] **Day 13** 自检 + Bug 修复
- [ ] **Day 14** 部署文档

## 快速开始

### 1. 启动基础设施

```bash
docker compose up -d mysql redis minio
```

### 2. 启动后端

```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run -pl minimax-gateway
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173` 即可。

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios |
| 后端 | Spring Boot 3 + Java 17 + MyBatis Plus + Spring Security |
| 流式 | Spring WebFlux + SSE |
| 数据库 | MySQL 8 + Redis 7 + Elasticsearch 8 |
| 存储 | MinIO |
| 监控 | Prometheus + Grafana + Micrometer |
| 部署 | Docker + Docker Compose + Nginx |

## License

Apache 2.0
