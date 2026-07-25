# MiniMax Platform V3.5.21 架构图 (增量)

> V3.5.18 合并 16→13 微服务 + V3.5.19 SQL 重生 + V3.5.20 一致性 + V3.5.21 文档
> 完整架构见 `docs/ARCHITECTURE.md`, 本文档是 V3.5.21 增量

## 一、整体架构 (V3.5.21)

```mermaid
graph TB
    User([用户/客户端])
    
    subgraph "客户端层"
        FE[Vue 3 + Element Plus<br/>45+ 页面]
        Mobile[移动端 H5]
    end
    
    subgraph "接入层"
        Nginx[Host Nginx :80<br/>13 upstream + 14 location]
    end
    
    subgraph "网关层"
        Gateway[Spring Cloud Gateway :7080<br/>Nacos lb:// 路由]
    end
    
    subgraph "微服务层 (13)"
        Auth[auth :8081<br/>5 账号 BCrypt]
        Chat[chat :8082<br/>含 memory_ext]
        Model[model :8084<br/>含 prompt]
        Rag[rag :8085<br/>RAG 知识库]
        MM[multimodal :8087<br/>ONNX 多模态]
        Agent[agent :8088<br/>Plan/Executor/Memory]
        Monitor[monitor :8089<br/>服务健康]
        Admin[admin :8090<br/>后台管理]
        Analytics[analytics :8092<br/>NL2SQL]
        Pipeline[pipeline :8093<br/>含 function_ext]
        AI[ai :8094<br/>4 模型加权]
        WS[ws :8095<br/>WebSocket]
    end
    
    subgraph "公共层"
        Common[common<br/>Result/Exception/Security/Tenant]
        Nacos[Nacos :8848<br/>服务发现 + 配置]
    end
    
    subgraph "数据层"
        MySQL[(MySQL 8.0+<br/>77 表 1652 行)]
        H2[(H2 沙箱模式<br/>AdminDataInitializer 兜底)]
        Redis[(Redis 7<br/>Caffeine 替代)]
    end
    
    subgraph "AI 引擎层"
        LLM[自研 MiniTransformer<br/>4 模型加权投票]
        ONNX[ONNX Runtime 1.17.1]
        Tik[Tika 文档解析]
    end
    
    subgraph "可观测性"
        OTel[OpenTelemetry :4317]
        Prom[Prometheus :9090]
        Grafana[Grafana :3000]
    end
    
    User --> FE
    User --> Mobile
    FE --> Nginx
    Mobile --> Nginx
    Nginx --> Gateway
    Nginx -.直连.-> Auth
    Nginx -.直连.-> Chat
    Nginx -.直连.-> AI
    Nginx -.直连.-> Pipeline
    
    Gateway --> Auth
    Gateway --> Chat
    Gateway --> Model
    Gateway --> Rag
    Gateway --> Agent
    Gateway --> Admin
    Gateway --> AI
    
    Auth --> MySQL
    Chat --> MySQL
    Chat -.h2.-> H2
    Model --> MySQL
    Pipeline --> MySQL
    AI --> LLM
    AI --> ONNX
    Rag --> Tik
    
    Auth -.注册.-> Nacos
    Chat -.注册.-> Nacos
    Model -.注册.-> Nacos
    Gateway -.发现.-> Nacos
    
    Auth -.metrics.-> OTel
    Chat -.metrics.-> OTel
    AI -.metrics.-> OTel
    OTel --> Prom
    Prom --> Grafana
    
    style Gateway fill:#f96
    style AI fill:#9cf
    style Nginx fill:#ff9
    style Nacos fill:#fc6
```

## 二、微服务调用链 (V3.5.21)

### 2.1 用户登录流程

```mermaid
sequenceDiagram
    participant U as 用户
    participant N as Nginx :80
    participant G as Gateway :7080
    participant A as auth :8081
    participant I as AdminDataInitializer
    participant DB as MySQL/H2
    
    U->>N: POST /api/v1/auth/login
    N->>G: 转发
    G->>A: lb://minimax-auth
    A->>I: @PostConstruct (启动时)
    I->>DB: 查 sys_user (BCrypt 验密)
    DB-->>I: user info
    I-->>A: ok
    A->>A: 签 JWT (access 30min + refresh 7d)
    A-->>G: {accessToken, refreshToken}
    G-->>N: 200
    N-->>U: 200
```

### 2.2 AI 意图识别流程 (V3.5.15+ 4 模型加权)

```mermaid
sequenceDiagram
    participant U as 用户
    participant AI as ai :8094
    participant C as Caffeine LRU
    participant TF as TF 模型
    participant NG as N-gram 模型
    participant SY as 同义词模型
    participant CT as 上下文模型
    participant DB as MySQL
    
    U->>AI: "生成柱状图"
    AI->>C: 查缓存
    alt 命中
        C-->>AI: GENERATE_CHART (0.1ms)
    else 未命中
        AI->>TF: 子串匹配 → 0.95
        AI->>NG: Bigram 搭配 → 0.85
        AI->>SY: 同义扩展 → 0.70
        AI->>CT: 上下文 → 0.50
        AI->>AI: 加权投票: 0.4*0.95 + 0.3*0.85 + 0.2*0.70 + 0.1*0.50 = 0.83
        AI->>C: 写缓存
    end
    AI->>AI: 路由到 ChartGenerator
    AI-->>U: PNG 图片
```

### 2.3 Agent 编排 (V3.5.16+ Planner/Executor/Memory)

```mermaid
graph LR
    Q[用户 query] --> P[Planner<br/>关键词规则]
    P --> Plan[Plan: 3 steps]
    Plan --> E1[Step 1: 查知识库]
    E1 --> RAG[RAG]
    RAG --> O1[output]
    O1 --> E2[Step 2: 生成代码]
    E2 --> CG[CodeGenerator]
    CG --> O2[output]
    O2 --> E3[Step 3: 渲染图表]
    E3 --> ChG[ChartGenerator]
    ChG --> Result[最终结果]
    
    M[AgentMemory<br/>短期 + 长期] -.持久化.-> DB[(MySQL)]
    P -.上下文.-> M
    E1 -.上下文.-> M
    E2 -.上下文.-> M
```

## 三、端口与模块映射 (V3.5.20 100% 一致)

```mermaid
graph LR
    subgraph "Host"
        H80[":80<br/>Nginx"]
    end
    
    subgraph "Gateway"
        G7080[":7080<br/>gateway"]
    end
    
    subgraph "13 微服务"
        A8081[":8081<br/>auth"]
        C8082[":8082<br/>chat<br/>含 memory_ext"]
        M8084[":8084<br/>model<br/>含 prompt"]
        R8085[":8085<br/>rag"]
        M8087[":8087<br/>multimodal"]
        Ag8088[":8088<br/>agent"]
        Mo8089[":8089<br/>monitor"]
        Ad8090[":8090<br/>admin"]
        An8092[":8092<br/>analytics"]
        P8093[":8093<br/>pipeline<br/>含 function_ext"]
        AI8094[":8094<br/>ai"]
        W8095[":8095<br/>ws"]
    end
    
    H80 --> G7080
    H80 -.直连.-> A8081
    H80 -.直连.-> C8082
    H80 -.直连.-> M8084
    H80 -.直连.-> R8085
    H80 -.直连.-> M8087
    H80 -.直连.-> Ag8088
    H80 -.直连.-> Mo8089
    H80 -.直连.-> Ad8090
    H80 -.直连.-> An8092
    H80 -.直连.-> P8093
    H80 -.直连.-> AI8094
    H80 -.直连.-> W8095
    
    G7080 --> A8081
    G7080 --> C8082
    G7080 --> M8084
    G7080 --> R8085
    G7080 --> Ag8088
    G7080 --> Ad8090
    G7080 --> AI8094
```

## 四、数据流 (V3.5.21 一致性)

```mermaid
graph TB
    subgraph "前端 (Vue 3)"
        Login[Login.vue]
        Chat[Chat.vue]
        Admin[Admin.vue]
        RAGUI[RAG.vue]
        AgentUI[Agent.vue]
    end
    
    subgraph "网关 (Nginx → Gateway)"
        Static[静态资源<br/>/opt/minimax/frontend/dist]
        API[API 路由<br/>13 upstream]
        WS[WebSocket<br/>Upgrade]
    end
    
    subgraph "业务逻辑层 (13)"
        Auth[auth: 鉴权 + BCrypt]
        Chat[chat: chat + memory]
        Model[model: 模型 + prompt]
        AI[ai: 4 模型 + MiniTransformer]
        RAG[rag: 知识库]
        Agent[agent: Plan/Executor]
        Pipeline[pipeline: 流水线 + function]
    end
    
    subgraph "数据层"
        MySQL[(MySQL 8.0+<br/>77 表)]
        H2[(H2 沙箱<br/>自动建表)]
        FS[(文件系统<br/>文件上传)]
    end
    
    subgraph "AI 引擎"
        LLM[MiniTransformer<br/>4 模型加权]
        ONNX[ONNX Runtime<br/>多模态]
    end
    
    Login --> API
    Chat --> API
    Chat --> WS
    Admin --> API
    RAGUI --> API
    AgentUI --> API
    
    API --> Auth
    API --> Chat
    API --> Model
    API --> AI
    API --> RAG
    API --> Agent
    API --> Pipeline
    
    Auth --> MySQL
    Chat --> MySQL
    Chat -.沙箱.-> H2
    Model --> MySQL
    RAG --> MySQL
    RAG --> FS
    Agent --> MySQL
    Pipeline --> MySQL
    
    AI --> LLM
    AI --> ONNX
    RAG --> ONNX
    
    style API fill:#ff9
    style Auth fill:#9cf
    style AI fill:#9cf
    style LLM fill:#f96
```

## 五、AI 算法架构 (V3.5.16+)

```mermaid
graph TB
    Input[用户 query] --> Cache{缓存?<br/>Caffeine LRU 1000}
    Cache -->|命中| Out[直接返回<br/>0.1ms]
    Cache -->|未命中| Pipeline
    
    subgraph "4 模型加权 (V3.5.15+)"
        Pipeline --> TF[TF 模型<br/>子串匹配<br/>权重 0.4]
        Pipeline --> Ngram[N-gram 模型<br/>Bigram 搭配<br/>权重 0.3]
        Pipeline --> Syn[同义词模型<br/>149 同义词组<br/>权重 0.2]
        Pipeline --> Ctx[上下文模型<br/>代词/承接<br/>权重 0.1]
    end
    
    TF --> Vote[加权投票]
    Ngram --> Vote
    Syn --> Vote
    Ctx --> Vote
    
    Vote --> Neural{Neural<br/>权重 0?}
    Neural -->|启用| NE[NeuralIntentModel<br/>MiniTransformer 召回]
    Neural -->|默认关闭| Route
    NE --> Route
    
    Route[意图路由] --> Chart[ChartGenerator]
    Route --> Music[MusicGenerator]
    Route --> Video[VideoComposer]
    Route --> Text[TextGenerator]
    Route --> Dash[DashboardBuilder]
    
    Chart --> Online[OnlineLearning<br/>反馈学习]
    Music --> Online
    Text --> Online
    
    Online --> Cache2[写回缓存]
    Online --> DB[(MySQL<br/>ai_intent_keyword)]
    
    style Pipeline fill:#9cf
    style Vote fill:#f96
    style Online fill:#fc6
```

## 六、部署架构 (V3.5.21)

```mermaid
graph TB
    subgraph "开发机"
        Dev[开发 IDE<br/>IntelliJ + VSCode]
        Mvn[mvn package<br/>14 module]
    end
    
    subgraph "沙箱环境"
        SB[沙箱 h2local<br/>AdminDataInitializer 5 账号兜底]
        E2E[e2e-multiround.sh<br/>23 case]
    end
    
    subgraph "生产环境 (CentOS 9 / Ubuntu 20+)"
        Host[Host Nginx :80]
        Docker[Docker Compose<br/>16 容器]
    end
    
    subgraph "可观测性"
        Prom[Prometheus]
        Graf[Grafana]
        Trace[Jaeger / Tempo]
    end
    
    Dev --> Mvn
    Mvn --> SB
    Mvn --> Docker
    SB --> E2E
    
    Docker --> Host
    Docker --> Prom
    Prom --> Graf
    Docker --> Trace
    
    style Dev fill:#9cf
    style Docker fill:#fc6
    style Host fill:#ff9
```
