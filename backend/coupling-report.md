# 微服务耦合诊断报告

## 严重程度：🔴 架构违规

**问题本质**：当前14个服务并非独立部署单元，而是通过Maven编译依赖直接共享彼此的 domain 类（entity/mapper/service/vo/dto）。这违反了微服务"独立部署、独立扩展"的核心原则。

---

## 违规依赖明细

### 1. minimax-agent → minimax-pipeline 🔴 最严重

**62 处**跨服务 import，包括：

```
PipelineRun / PipelineWorkflow / PipelineWorkflowVersion  ← 实体类
PipelineNodeLog / PipelineRunMapper / PipelineWorkflowMapper  ← Mapper
NodeStatus / NodeType / RunStatus  ← 枚举
NodeExecutor / NodeExecutorFactory / ExecutionContext  ← 核心执行器
ToolExecutor / ToolFunction / FunctionCallLog  ← 函数扩展
WorkflowService / DagValidator  ← 服务类
```

**后果**：`pipeline` 模块改一个字段，`agent` 必须重新编译。任何 pipeline 的发布都连带影响 agent。

---

### 2. minimax-pipeline → minimax-analytics 🔴 严重

**30+ 处**跨服务 import，包括：

```
DataSourceService / ChartService / FileIngestService  ← analytics 业务服务
Nl2SqlService / SqlSafetyChecker / PromptTemplates  ← NL2SQL 核心
SchemaService / QueryService / ReportService  ← 查询/报表服务
DataSource / IngestTask / Nl2SqlHistory / Report  ← analytics 实体
ChartTypeDecider / AnomalyDetector / TrendAnalyzer  ← 分析工具类
```

**后果**：`analytics` 模块重启，pipeline 的执行行为可能静默改变（两个模块绑定在同一 JVM）。

---

### 3. minimax-analytics → minimax-model 🔴 中等

**5 处**跨服务 import：

```
ModelProviderAdapter / ModelProviderFactory  ← 模型工厂
ChatRequest / ChatResponse  ← DTO
```

**后果**：analytics 调用 LLM 推理必须引入整个 model 模块，实际上是把 model 作为内部包而非独立服务。

---

### 4. minimax-chat → minimax-auth 🟡 轻微

```
UserApiKeyService  ← auth 的业务服务
```

**后果**：chat 不能独立部署，auth 重启会影响 chat。

---

### 5. minimax-agent → minimax-auth 🟡 轻微

```
UserApiKeyService  ← 同上
```

---

## 正确做法对照

| 当前（错误） | 应该（正确） |
|-------------|-------------|
| Maven 依赖 `minimax-pipeline` | 通过 HTTP 调用 `/api/v1/pipeline/**` |
| 直接 import `PipelineRun` | 通过 HTTP 返回 JSON，agent 自己解析 |
| 编译时绑定 | 运行时解耦 |
| pipeline 改字段 agent 必须重编译 | pipeline 改字段 agent 无感知 |
| 不能独立扩缩容 | 各服务独立扩缩 |

---

## 推荐的拆解方案

### 方案 A：HTTP 化（推荐）

所有跨服务调用改为 REST API 调用，通过 gateway 路由：

```
minimax-agent        →  HTTP GET /api/v1/pipeline/workflows/{id}
                       →  HTTP POST /api/v1/pipeline/runs
minimax-pipeline     →  HTTP GET /api/v1/analytics/datasources
                       →  HTTP POST /api/v1/analytics/nlsql
minimax-analytics    →  HTTP POST /api/v1/model/chat
minimax-chat/agent   →  HTTP GET /api/v1/auth/apikey/verify
```

**改动量**：中。需为每个跨服务调用新建 `FeignClient` 接口（仅接口，无实现），调用方只依赖接口而非实现。

### 方案 B：提取 shared-lib（过渡）

把跨服务共享的实体/DTO/枚举提取到 `minimax-common`：

```
minimax-common/
  shared/
    pipeline/    ← PipelineRun, NodeStatus, NodeType, ...
    analytics/   ← DataSource, Nl2SqlRequest, ChartType, ...
    model/       ← ChatRequest, ChatResponse, ...
```

**优点**：改动小。**缺点**：仍然是编译时耦合，只是换了个包；共享的 model 实体改字段依然要全部重编译。

---

## 推荐行动计划

**Phase 1（立即）**：将 `minimax-common` 里的内容全部打上 `<!-- 合理依赖 -->`，明确"common只允许放什么"（工具类、Result包装、多租户上下文、通用枚举），禁止放入业务实体。

**Phase 2（短期）**：agent → pipeline、pipeline → analytics 改 Feign HTTP 调用。analytics → model 也同样处理。

**Phase 3（长期）**：所有服务两两之间通过 API 通信，pom.xml 中完全移除 `minimax-*` 的业务模块依赖，仅保留 `minimax-common` 和第三方库。

---

## 依赖现状总结

```
当前（耦合）                          应该（独立）
─────────────────────────────────────────────────────────────
minimax-agent  ──maven──▶ minimax-pipeline    agent  ──HTTP──▶ pipeline
minimax-pipeline──maven──▶ minimax-analytics  pipeline ──HTTP──▶ analytics
minimax-analytics──maven──▶ minimax-model     analytics ──HTTP──▶ model
minimax-chat/agent ─maven──▶ minimax-auth     chat/agent ──HTTP──▶ auth
                                            (以及 minimax-common 作为纯工具库)
```
