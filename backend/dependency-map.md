# MiniMax Platform 微服务依赖图

## 概览

```
                         ┌──────────────┐
                         │   外 部 流 量   │
                         └──────┬───────┘
                                │
                         ┌──────▼───────┐
                         │  Nacos Gateway │  (注册中心 / 路由)
                         └──────┬───────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          │         负载均衡 lb://  + WebSocket          │
    ┌─────▼──────┐  ┌──────▼──┐  ┌─────▼────┐  ┌──────▼─────┐
    │ minimax-ai │  │  gateway │  │ minimax- │  │minimax-ws  │
    │  (AI核心)   │  │  (路由)  │  │  admin   │  │ (实时协作)  │
    └─────┬──────┘  └─────────┘  └──────────┘  └─────┬──────┘
          │                                           │
 ┌────────┼───────────────────────────────────────┐   │
 │        │        Maven 依赖层（构建时）            │   │
 │  ┌─────▼──────────────┐    ┌──────────────────▼─┐ │
 │  │  minimax-common    │◄───│  minimax-gateway  │ │
 │  │  (通用实体/枚举/工具) │    └─────────────────────┘ │
 │  └────────┬───────────┘                            │
 │           │                                        │
 │  ┌────────┼────────────────────────────────────┐   │
 │  │        │      所有业务服务的 Base            │   │
 │  │  ┌─────▼──────┐   ┌────────▼──────┐        │   │
 │  │  │minimax-auth│   │minimax-model  │        │   │
 │  │  │ (认证鉴权)  │   │  (模型/推理)   │        │   │
 │  │  └───┬────┬───┘   └────┬─────┬────┘        │   │
 │  │      │    │             │     │              │   │
 │  │  ┌───▼┐ ┌─▼──────┐  ┌──▼──┐ ┌▼──────────┐  │   │
 │  │  │chat│ │ agent  │  │pipel│ │ analytics │  │   │
 │  │  │    │ │(Agent) │  │ine  │  │(NL2SQL)  │  │   │
 │  │  └───┘ └───┬────┘  └──┬──┘ └─┬────────┘  │   │
 │  │            │          │       │           │   │
 │  └────────────┼──────────┼───────┼───────────┘   │
 └───────────────┼──────────┼───────┼────────────────┘
                  │          │       │
           ┌──────▼───┐ ┌───▼──┐ ┌─▼─────┐  ┌──────▼──────┐
           │ minimax- │ │ mini- │ │ mini- │  │  minimax-   │
           │  agent   │ │max-pip│ │max-ana│  │  rag/model/  │
           │          │ │eline  │ │lytics │  │  multimodal/ │
           │          │ │       │ │       │  │  monitor/   │
           └────┬─────┘ └──┬────┘ └──┬────┘  │  chat       │
                │          │        │        └─────────────┘
                └──────────┴────────┘
```

## 基础设施依赖

| 服务 | MySQL | Nacos 注册 | Redis | 说明 |
|------|-------|-----------|-------|------|
| minimax-gateway | — | ✅ | — | 无 DB，只做路由 |
| minimax-ws | ✅ Shanghai | ✅ | — | WebSocket 协作 |
| minimax-auth | ✅ Shanghai | ✅ | — | 用户/RBAC |
| minimax-chat | ✅ Shanghai | ✅ | — | 聊天消息 |
| minimax-model | ✅ Shanghai | ✅ | — | 模型配置/配额 |
| minimax-rag | ✅ Shanghai | ✅ | — | 知识库/RAG |
| minimax-admin | ✅ Shanghai | ✅ | — | 系统管理 |
| minimax-multimodal | ✅ Shanghai | ✅ | — | 多模态 |
| minimax-monitor | ✅ Shanghai | ✅ | — | 告警监控 |
| minimax-agent | ✅ Shanghai | ✅ | — | Agent 编排 |
| minimax-analytics | ✅ Shanghai | ✅ | — | NL2SQL 分析 |
| minimax-pipeline | ✅ Shanghai | ✅ | — | 流水线 |
| minimax-ai | ✅ Shanghai | ✅ | — | AI 推理核心 |
| minimax-common | — | — | — | 纯工具库 |

## Maven 依赖矩阵

```
                    common  auth  model  pipeline  analytics  chat  其他
minimax-common        —      —     —       —         —        —     —
minimax-gateway       ✅     —     —       —         —        —     —
minimax-ws            ✅     —     —       —         —        —     —
minimax-auth          ✅     —     —       —         —        —     —
minimax-chat          ✅     ✅    —       —         —        —     —
minimax-model         ✅     —     —       —         —        —     —
minimax-rag           ✅     —     —       —         —        —     —
minimax-admin         ✅     —     —       —         —        —     —
minimax-multimodal    ✅     —     —       —         —        —     —
minimax-monitor       ✅     —     —       —         —        —     —
minimax-agent         ✅     ✅    —       ✅        —        —     —
minimax-analytics     ✅     —     ✅       —         —        —     —
minimax-pipeline      ✅     —     ✅       —         ✅       —     —
minimax-ai            ✅     —     ✅       —         —        —     —
```

## 服务职责

| 服务 | 端口 | 职责 | 核心能力 |
|------|------|------|----------|
| **gateway** | 8080 | 统一网关/路由 | 鉴权过滤、路由转发、限流 |
| **auth** | 8081 | 用户认证/RBAC | JWT、OAuth、登录日志 |
| **ws** | 8082 | 实时协作 | WebSocket、协作房间、权限广播 |
| **chat** | 8083 | 聊天服务 | 会话管理、消息收发 |
| **model** | 8084 | 模型管理 | LLM 路由、配额、许可证 |
| **rag** | 8085 | 知识库/RAG | 文档切片、向量检索 |
| **admin** | 8086 | 系统管理 | API Key、租户、审计 |
| **multimodal** | 8087 | 多模态 | 图片/音频/视频生成 |
| **monitor** | 8088 | 监控告警 | 告警规则、Prometheus 指标 |
| **agent** | 8089 | Agent 编排 | Planner/Executor/Memory |
| **analytics** | 8090 | 数据分析 | NL2SQL、报表、可视化 |
| **pipeline** | 8091 | 流水线 | 工作流编排、节点执行 |
| **ai** | 8092 | AI 核心 | AI Chat、训练、推理 |

## Gateway 路由表

| 路径前缀 | 目标服务 | 协议 |
|----------|----------|------|
| `/api/v1/auth/**` | minimax-auth | HTTP |
| `/api/v1/chat/**`, `/api/v1/sessions/**` | minimax-chat | HTTP |
| `/api/v1/model/**`, `/api/v1/openai/**`, `/api/v1/imagegen/**` | minimax-model | HTTP |
| `/api/v1/memory/**` | minimax-chat | HTTP |
| `/api/v1/rag/**` | minimax-rag | HTTP |
| `/api/v1/function/**` | minimax-pipeline | HTTP |
| `/api/v1/multimodal/**` | minimax-multimodal | HTTP |
| `/api/v1/monitor/**` | minimax-monitor | HTTP |
| `/api/v1/agent/**`, `/api/v1/skill-approval/**` | minimax-agent | HTTP |
| `/api/v1/analytics/**` | minimax-analytics | HTTP |
| `/api/v1/pipeline/**` | minimax-pipeline | HTTP |
| `/api/v1/training/**` | minimax-model | HTTP |
| `/api/v1/collab/**` | minimax-ws | HTTP |
| `/api/v1/ai/**`, `/api/ai/**` | minimax-ai | HTTP |
| `/ws/**`, `/api/v1/ws/**` | minimax-ws | WebSocket |
| `/admin/**`, `/api/v1/admin/**` | minimax-admin | HTTP |

## 依赖解读

### 核心依赖链

```
minimax-model ──┬── minimax-analytics  (NL2SQL 调用模型)
                ├── minimax-pipeline   (流水线节点调用模型)
                └── minimax-ai         (AI 推理调用模型)

minimax-auth ──┬── minimax-chat  (聊天鉴权)
               └── minimax-agent  (Agent 鉴权)

minimax-pipeline ── minimax-analytics  (流水线引用分析节点)

minimax-common ── 所有服务的基础依赖（通用实体/枚举/工具类）
```

### 说明
- **服务间通信**: 通过 Spring Cloud LoadBalancer (`lb://service-name`) 在 gateway 层转发，无 Feign/Ribbon 服务间直调
- **数据库**: 13/14 个服务共用同一个 MySQL (Shanghai)，gateway 本身无 DB
- **注册发现**: 全量接入 Nacos，所有服务通过 `spring.cloud.nacos.discovery` 自注册
- **Redis**: 目前代码中未启用 Redis 客户端（pom 中有 spring-boot-starter-data-redis 但未实际使用）
