# Agent Forge V4.0 — 设计重构版

> V4.0 是对 V2.0 (规则引擎 + 模拟部署) 和 V3.0 (假 LLM + 假 ArgoCD) 的**重做**。把"占位包装成真实"全部清掉, 只保留**真的**。

## 🎯 核心改动

| 维度 | V2.0 / V3.0 (废) | V4.0 (新) |
|------|------------------|------------|
| **数据库** | 6 列 `JSON/TEXT` 字符串塞在主表 | 3 主表 + 4 子表 (`forge_agent` / `forge_workflow_step` / `forge_manifest` / `forge_deployment_log`) |
| **状态管理** | 3 个 service 各自 `setStatus(String)` | 单一 `ReleaseStateMachine` 状态机, 7 状态 6 事件白名单转换 |
| **部署** | `DeploymentOrchestrator` (V2.0) + `ArgoCdService` (V3.0) 重复 90% | 单 `DeploymentService.deploy(id)`, 按 `deploy_target` 路由 k8s/gitops/edge/docker |
| **LLM** | `LlmClientService` 3 层 fallback + 5 模型 | 单 `LlmClient` 1 次调用, 失败明确 `usedFallback=true` |
| **前端** | 5 个模型假下拉 + "GitOps ⭐"假按钮 | 3 个真实模型 + `usedFallback` 显式提示 |

## 🚮 删除的垃圾 (3 service + 1 endpoint + 1 SSE)

- ❌ `service/DeploymentOrchestrator.java` (V2.0 模拟 8 阶段)
- ❌ `service/ArgoCdService.java` (V3.0 假 GitOps)
- ❌ `service/LlmClientService.java` (V3.0 3 层 fallback)
- ❌ `POST /api/v1/forge/releases/{id}/deploy-gitops` (V3.0 假)
- ❌ `GET /api/v1/forge/argocd/applications/{name}` (V3.0 假)
- ❌ `GET /api/v1/forge/deployments/{id}/stream` SSE (V2.0 模拟推送)
- ❌ `config/RestTemplateConfig.java` → 改 `HttpClientConfig.java`

## ✅ V4.0 新增 (5 entity + 4 mapper + 1 state machine)

### 数据模型 (3 主 + 4 子)

```
forge_project (主)
  ├── forge_agent         1对多 (智能体, V4.0 独立行, 可索引)
  ├── forge_workflow_step 1对多 (流程步骤, V4.0 独立行)
  └── forge_release (主) 
        ├── forge_agent         1对多
        ├── forge_workflow_step 1对多
        ├── forge_manifest      1对多 (K8s yaml, Dockerfile, ArgoCD app, 每个独立行)
        └── forge_deployment (主) 
              └── forge_deployment_log  1对多 (每条日志独立行, 分页查询)
```

### 状态机 (单入口)

```java
ReleaseStateMachine.fire(releaseId, Event.START_BUILD)
  DRAFT     → BUILDING     (build manifest)
  BUILDING  → DEPLOYING    (push image, k8s apply)
  DEPLOYING → HEALTHY      (pods ready)
  HEALTHY   → ACTIVE       (traffic shifted)
  任何状态   → FAILED       (with reason)
  终态       → ARCHIVED
```

非法转换直接抛 `IllegalStateException`, 不能随便改 status。

### 部署路由

```java
DeploymentService.deploy(releaseId) {
  switch (release.deployTarget) {
    case "k8s"    -> runK8sSimPipeline()   // 真实 K8s manifest 模拟部署
    case "gitops" -> runGitOpsPipeline()   // 渲染 ArgoCD CRD, 真实持久化 forge_manifest
    case "edge"   -> runEdgePipeline()     // 渲染 edge 部署脚本
    case "docker" -> runDockerPipeline()   // 本地 docker
  }
}
```

`gitops` 模式**不假装** git push / ArgoCD sync 成功, 日志里写明:
```
WARN  Git push 需配置 agent-forge.gitops.repo-url + 凭证
WARN  ArgoCD sync 需配置 agent-forge.argocd.server + token
```

## 📂 V4.0 文件结构

```
backend/minimax-deployer/src/main/
├── java/com/minimax/deployer/
│   ├── DeployerApplication.java
│   ├── config/HttpClientConfig.java          (新)
│   ├── controller/
│   │   ├── AgentTemplateController.java      (V2.0 保留)
│   │   ├── ForgeProjectController.java       (V4.0 重写: +agents /workflow)
│   │   └── ForgeReleaseController.java       (V4.0 重写: 删 gitops/argocd/sse, +manifests/+logs)
│   ├── dto/
│   │   ├── CreateReleaseRequest.java         (V4.0 +workflow)
│   │   ├── ParseRequirementsRequest.java     (V4.0 +llmModel)
│   │   └── ParseRequirementsResponse.java    (V4.0 +usedFallback)
│   ├── entity/
│   │   ├── AgentTemplate.java
│   │   ├── ForgeProject.java                 (V4.0: 删 parsed_requirements/recommended_agents)
│   │   ├── ForgeRelease.java                 (V4.0: 删 agent_definitions/deploy_config/manifests)
│   │   ├── ForgeDeployment.java              (V4.0: 删 stages/logs, +current_stage)
│   │   ├── ForgeAgent.java                   (新)
│   │   ├── ForgeWorkflowStep.java            (新)
│   │   ├── ForgeManifest.java                (新)
│   │   └── ForgeDeploymentLog.java           (新)
│   ├── mapper/
│   │   ├── AgentTemplateMapper.java
│   │   ├── ForgeProjectMapper.java
│   │   ├── ForgeReleaseMapper.java
│   │   ├── ForgeDeploymentMapper.java
│   │   ├── ForgeAgentMapper.java             (新)
│   │   ├── ForgeWorkflowStepMapper.java      (新)
│   │   ├── ForgeManifestMapper.java          (新)
│   │   └── ForgeDeploymentLogMapper.java     (新)
│   ├── service/
│   │   ├── AgentTemplateService.java         (V2.0 保留)
│   │   ├── ManifestGeneratorService.java     (V2.0 保留, FreeMarker 渲染)
│   │   ├── ForgeProjectService.java          (V4.0: 持久化到子表)
│   │   ├── ForgeReleaseService.java          (V4.0: 持久化到子表)
│   │   ├── RequirementsParserService.java    (V4.0: 单 LlmClient, 显式 usedFallback)
│   │   ├── LlmClient.java                    (新, V4.0 简化版, 替代 V3.0 LlmClientService)
│   │   └── DeploymentService.java            (新, V4.0 统一部署, 替代 V2/V3 两个 service)
│   └── state/
│       └── ReleaseStateMachine.java          (新, V4.0 单一状态入口)
└── resources/
    ├── application.yml                       (V4.0 简化)
    ├── schema.sql                            (V4.0 +4 张子表)
    └── data.sql
```

## 📜 端点 (V4.0 简化清单)

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/v1/forge/projects` | POST | 创建项目 (含 LLM 解析) |
| `/api/v1/forge/projects` | GET | 列表 |
| `/api/v1/forge/projects/{id}` | GET | 详情 |
| `/api/v1/forge/projects/{id}/agents` | GET | **V4.0** agents 子表 |
| `/api/v1/forge/projects/{id}/workflow` | GET | **V4.0** workflow 子表 |
| `/api/v1/forge/projects/{id}` | DELETE | 删除 |
| `/api/v1/forge/releases` | POST | 创建 release (落 agent/workflow 子表) |
| `/api/v1/forge/releases` | GET | 列表 (按 projectId) |
| `/api/v1/forge/releases/{id}` | GET | 详情 |
| `/api/v1/forge/releases/{id}/manifests` | GET | **V4.0** manifest 子表 |
| `/api/v1/forge/releases/{id}/deploy` | POST | 触发部署 (按 deploy_target 路由) |
| `/api/v1/forge/releases/{id}/rollback/{targetId}` | POST | 回滚 |
| `/api/v1/forge/releases/{from}/diff/{to}` | GET | 差异 |
| `/api/v1/forge/deployments/{id}` | GET | 部署详情 |
| `/api/v1/forge/deployments/{id}/logs` | GET | **V4.0** 部署日志 (分页) |
| `/api/v1/forge/templates` | GET | 模板列表 |
| `/api/v1/forge/templates/{id}` | GET | 模板详情 |
| `/api/v1/forge/templates/code/{code}` | GET | 按 code |

**总端点**: 17 个 (V2.0 12 + V3.0 +3 = 15 → 删假 5 加真 4 = 14, 加 agents/workflow/manifests/logs 4 = 17)

## 🧠 LLM 集成 (V4.0 简化)

```java
@Value("${agent-forge.llm.url}")    private String aiUrl;        // minimax-ai:8090
@Value("${agent-forge.llm.model}")  private String model;        // qwen2.5-0.5b-instruct

Optional<String> chat(prompt, system) {
  POST minimax-ai/api/v1/multimodal/chat-qwen
  → 返回 content
}
```

**不再有**:
- ❌ 多层 fallback (主用/备选/兜底)
- ❌ 5 个模型选择 (V3.0 假切换)
- ❌ 假装不同模型返回不同结果

**有**:
- ✅ 1 次 HTTP 调用
- ✅ 失败返回 `Optional.empty()`
- ✅ 响应里 `usedFallback=true` 明示 (前端可显示 warning)

## 🔄 状态机 (V4.0)

```
DRAFT ──START_BUILD──> BUILDING ──START_DEPLOY──> DEPLOYING ──DEPLOY_HEALTHY──> HEALTHY ──MARK_ACTIVE──> ACTIVE
  │                       │                          │                            │
  └─ARCHIVE──> ARCHIVED   └─FAIL──> FAILED ──ARCHIVE──> ARCHIVED                  
                                                                                    
终态 (ACTIVE / ARCHIVED) 不可再转, 任何非法转换抛 IllegalStateException
```

## 📊 启动方式

```bash
# 1. 启动 minimax-ai (Qwen2.5 ONNX)
mvn -pl minimax-ai -am spring-boot:run

# 2. 启动 minimax-deployer (V4.0)
mvn -pl minimax-deployer spring-boot:run

# 3. 启动前端
cd frontend && npm run dev

# 4. 访问 http://localhost:3000/builder/requirements
```

## 📜 V4.0 提交

- `xxx` (本次) — refactor(deployer): V4.0 设计重构 (删 3 service + 5 假端点 + 6 JSON 列, 加 1 状态机 + 4 子表)
- `cc375be8` (V3.0, 即将被 V4.0 替代) — feat(agent-forge): V3.0 真实 LLM 接入 + ArgoCD GitOps
- `9b86ca42` (V2.0) — feat(agent-forge): V2.0 后端 deployer 服务模块

## 🎯 V5.0 计划 (下一步)

V4.0 把设计清理干净了, V5.0 才真正接 JGit 库做 git push + 真调 ArgoCD REST API。当前 V4.0 在 gitops 模式下会**明确**说"需配置", 不再假装成功。
