# MiniMax Platform 架构规范

## 一、微服务设计原则

**核心规则**：每个微服务是独立部署单元，服务间通信只能通过 HTTP API（REST/Feign），禁止通过 Maven 依赖共享业务类。

```
✅ 正确：agent 通过 Feign HTTP 调用 pipeline 的 /api/v1/function/** 接口
❌ 错误：agent 在 pom.xml 中引入 minimax-pipeline，直接 import 对方类
```

**minimax-common 的边界**（唯一允许跨服务共享的包）：

| 包路径 | 允许内容 | 禁止内容 |
|--------|---------|---------|
| `common.result` | `Result<T>`, `ResultCode` | 业务 DTO |
| `common.exception` | `BizException`, `GlobalExceptionHandler` | 业务异常 |
| `common.security` | JWT 过滤器、权限注解、上下文工具 | 业务鉴权逻辑 |
| `common.tenant` | `TenantContext`, 租户拦截器 | 租户业务逻辑 |
| `common.audit` | 审计切面、记录器 | 业务审计 |
| `common.sse` | SSE 工具类 | 业务事件 |
| `common.web` | `BaseController`, `PageRequest` | 业务 Controller |
| `common.config` | CORS、通用配置 | 模块配置 |
| `common.constants` | 通用常量 | 业务枚举 |
| `common.utils` | IP 工具等通用工具 | 业务工具 |

**minimax-common 禁止放入**：
- 业务实体（`Entity`, `DO`）
- 业务 Mapper
- 业务 Service 实现
- 业务枚举（`NodeStatus`, `RunStatus` 等属于各自模块）
- 业务 DTO / VO（属于各自模块）

---

## 二、正确 vs 错误依赖对照

### agent ↔ pipeline（V6.8.1 当前 → 应该）

```
❌ 当前（编译时耦合）
agent/pom.xml → minimax-pipeline
agent 代码: import com.minimax.pipeline.function_ext.entity.FunctionTool;

✅ 应该（运行时解耦）
agent/pom.xml → minimax-common（仅）
agent 代码: import com.minimax.common.feign.pipeline.* (Feign 接口)
调用: pipelineFeign.listTools() → HTTP GET /api/v1/function/tools
```

### pipeline ↔ analytics（当前 → 应该）

```
❌ 当前
pipeline/pom.xml → minimax-analytics
pipeline 代码: import com.minimax.analytics.service.datasource.DataSourceService;

✅ 应该
pipeline/pom.xml → minimax-common（仅）
pipeline 代码: DataSourceFeignClient.getDataSource(id) → HTTP GET /api/v1/analytics/datasources/{id}
```

### analytics ↔ model（当前 → 应该）

```
❌ 当前
analytics/pom.xml → minimax-model
analytics 代码: import com.minimax.model.provider.ModelProviderFactory;

✅ 应该
analytics/pom.xml → minimax-common（仅）
analytics 代码: ModelFeignClient.chat(ChatRequest) → HTTP POST /api/v1/model/chat
```

---

## 三、Feign 客户端规范

### 1. Feign 接口定义位置

调用方模块的 `src/main/java/com/minimax/{module}/feign/` 包下定义接口：

```
minimax-agent/src/main/java/com/minimax/agent/
  feign/
    PipelineFunctionClient.java    ← 列工具/调用工具
    SkillApprovalClient.java       ← 审批 CRUD

minimax-pipeline/src/main/java/com/minimax/pipeline/
  feign/
    AnalyticsDataSourceClient.java  ← 拿 DataSource

minimax-analytics/src/main/java/com/minimax/analytics/
  feign/
    ModelClient.java               ← LLM 对话
```

### 2. Feign 接口写法

```java
@FeignClient(name = "minimax-pipeline", contextId = "functionClient")
public interface PipelineFunctionClient {

    @GetMapping("/api/v1/function/tools")
    Result<List<FunctionToolDTO>> listTools();

    @GetMapping("/api/v1/function/tools/by-name/{name}")
    Result<FunctionToolDTO> getByName(@PathVariable String name);

    @PostMapping("/api/v1/function/invoke/{name}")
    Result<ToolResult> invoke(@PathVariable String name,
                               @RequestParam Long userId,
                               @RequestParam(required = false) Long sessionId,
                               @RequestBody Map<String, Object> body,
                               HttpServletRequest req);
}
```

### 3. 共享 DTO 放 common

Feign 接口使用的 DTO/常量必须来自 `minimax-common` 或 common 的 `feign/` 子包：

```
minimax-common/src/main/java/com/minimax/common/feign/
  pipeline/
    FunctionToolDTO.java      ← pipeline → agent 的共享 Tool DTO
    SkillApprovalDTO.java     ← pipeline → agent 的共享 Approval DTO
    ToolResultDTO.java
  analytics/
    DataSourceDTO.java        ← analytics → pipeline 的共享 DataSource DTO
  model/
    ChatRequestDTO.java       ← model → analytics 的共享请求 DTO
    ChatResponseDTO.java      ← model → analytics 的共享响应 DTO
```

### 4. Pipeline 控制器返回 DTO（不是实体）

```java
// ❌ 原来：返回 pipeline 实体
@GetMapping("/tools/{id}")
public FunctionTool getTool(@PathVariable Long id) {
    return toolService.get(id);
}

// ✅ 改成：返回 common DTO
@GetMapping("/tools/{id}")
public Result<FunctionToolDTO> getTool(@PathVariable Long id) {
    FunctionTool tool = toolService.get(id);
    return Result.ok(FunctionToolDTO.fromEntity(tool));
}
```

### 5. application.yml 配置

被调用方（pipeline）需要添加 CORS + 鉴权白名单：

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allow-credentials: true
```

---

## 四、依赖矩阵（V6.8.1 重构后）

```
              common  auth  model  pipeline  analytics
minimax-common   —      —     —       —         —
minimax-gateway  ✅     —     —       —         —
minimax-ws       ✅     —     —       —         —
minimax-auth     ✅     —     —       —         —
minimax-chat     ✅     —     —       —         —
minimax-model    ✅     —     —       —         —
minimax-rag      ✅     —     —       —         —
minimax-admin    ✅     —     —       —         —
minimax-multi    ✅     —     —       —         —
minimax-monitor  ✅     —     —       —         —
minimax-agent    ✅     —     —       —         —   (Feign: pipeline + auth)
minimax-analytics✅     —     —       —         —   (Feign: model)
minimax-pipeline ✅     —     —       —         —   (Feign: analytics)
minimax-ai       ✅     —     —       —         —   (HTTP: model)
```

**✅ 所有 14 个服务的 Maven pom.xml 已完全解耦，无跨服务编译依赖。**

---

## 五、服务间 Feign/HTTP 调用映射

| 调用方 | 被调用方 | 方式 | 路由 |
|--------|---------|------|------|
| agent | pipeline (function tools) | Feign | `/api/v1/function/**` → `lb://minimax-pipeline` |
| agent | pipeline (skill approval) | Feign | `/api/v1/skill-approval/**` → `lb://minimax-pipeline` |
| agent | auth (API key validate) | Feign | `/internal/apikey/validate` → `lb://minimax-auth` |
| analytics | model (LLM chat) | Feign | `/api/v1/models/internal/chat` → `lb://minimax-model` |
| pipeline | analytics (datasource) | MyBatis 直接查 `analytics_datasource` 表 | — |
| ai | model (LLM chat) | RestTemplate HTTP | `http://minimax-model:8084/api/v1/models/chat` |

---

## 六、共享 DTO 位置（minimax-common）

```
minimax-common/src/main/java/com/minimax/common/feign/
  pipeline/
    FunctionToolDTO.java      ← pipeline → agent 共享工具定义
    SkillApprovalDTO.java     ← pipeline → agent 共享审批记录
    ToolResultDTO.java        ← pipeline → agent 共享工具执行结果
  analytics/
    DataSourceDTO.java       ← analytics → pipeline 共享数据源配置
  model/
    ChatRequestDTO.java      ← model ↔ analytics/ai 共享对话请求
    ChatResponseDTO.java     ← model ↔ analytics/ai 共享对话响应
```

---

## 五、实施路线图

| 阶段 | 任务 | 状态 |
|------|------|------|
| Phase 1 | 定义 common 边界，清理架构文档 | ✅ 完成 |
| Phase 2a | agent → pipeline Feign 化 | ✅ 完成 |
| Phase 2b | pipeline → analytics Feign 化 | ✅ 完成 |
| Phase 2c | analytics → model Feign 化 | ✅ 完成 |
| Phase 3 | chat/agent → auth 解耦 + ai → model 解耦 | ✅ 完成 |
| Phase 4 | Git commit + 推送 | ⬜ |
