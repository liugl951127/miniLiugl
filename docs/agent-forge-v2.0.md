# 🔥 Agent Forge V2.0 — 后端 deployer 服务

> 从客户需求文档到生产部署, 真实后端支撑, 端到端可运行

## 📖 V2.0 增量

V1.0 完成了纯前端 5 步流水线 (mock 数据)。V2.0 新增:
- **后端 deployer 服务** (`minimax-deployer` Spring Boot 模块)
- **真实数据库持久化** (4 张表: H2 沙箱 / MySQL 生产)
- **LLM 驱动需求解析** (规则引擎, 生产接 Qwen2.5-72B)
- **Manifest 自动生成** (Dockerfile + K8s YAML, Freemarker 模板)
- **SSE 实时部署推送** (Server-Sent Events, 8 阶段状态)
- **语义化版本管理** (SemVer, 一键回滚)
- **6 个预置行业模板** (教育/电商/代码/金融/医疗/自定义)

## 🏗️ 架构

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Frontend     │ →  │  minimax-     │ →  │  MySQL/H2    │
│  (Vue 3)      │    │  deployer     │    │  forge_*     │
│  /builder/*   │ ←  │  (9010)       │ ←  │              │
└──────────────┘    └──────────────┘    └──────────────┘
       │ SSE
       ↓
   实时部署状态
```

## 📦 模块结构

```
backend/minimax-deployer/
├── pom.xml                          # Maven 配置 (parent + 15 依赖)
├── src/main/java/com/minimax/deployer/
│   ├── DeployerApplication.java     # 启动类 (9010)
│   ├── controller/
│   │   ├── ForgeProjectController.java   # /projects CRUD
│   │   ├── ForgeReleaseController.java   # /releases + SSE
│   │   └── AgentTemplateController.java  # /templates
│   ├── service/
│   │   ├── RequirementsParserService.java  # LLM 解析 (mock)
│   │   ├── ManifestGeneratorService.java   # Freemarker 模板
│   │   ├── DeploymentOrchestrator.java     # 8 阶段异步部署 + SSE
│   │   ├── ForgeProjectService.java        # 项目 CRUD
│   │   ├── ForgeReleaseService.java        # Release + 回滚
│   │   └── AgentTemplateService.java       # 模板查询
│   ├── entity/
│   │   ├── ForgeProject.java
│   │   ├── ForgeRelease.java
│   │   ├── ForgeDeployment.java
│   │   └── AgentTemplate.java
│   ├── mapper/                          # MyBatis-Plus 4 个
│   └── dto/
│       ├── ParseRequirementsRequest.java
│       ├── ParseRequirementsResponse.java
│       └── CreateReleaseRequest.java
└── src/main/resources/
    ├── application.yml                 # H2 沙箱配置
    ├── bootstrap.yml                   # Nacos 关闭
    ├── schema.sql                      # 4 张表
    └── data.sql                        # 6 个模板
```

## 🗄️ 数据库设计

### forge_project (项目)
- id, name, industry, scenario
- raw_requirements (原始文本)
- parsed_requirements (LLM 解析 JSON)
- recommended_agents (智能体 JSON)
- current_release_id (冗余)
- status: DRAFT/ANALYZED/DEPLOYED/ARCHIVED
- owner_id, created_at, updated_at

### forge_release (版本)
- id, project_id, version (SemVer), title, changelog
- agent_definitions, deploy_config, manifests (YAML/JSON)
- status: DRAFT/BUILDING/DEPLOYING/ACTIVE/FAILED/ROLLED_BACK
- deploy_target: DOCKER/K8S/CLOUD/EDGE
- replicas, image_registry, image_tag
- deploy_duration, created_by, deployed_at

### forge_deployment (部署实例)
- id, release_id, instance_name
- stages (JSON 数组, 8 阶段状态)
- logs (累积日志, TEXT)
- status: PENDING/BUILDING/PUSHING/DEPLOYING/RUNNING/FAILED
- target, namespace, running_replicas, desired_replicas
- current_qps, error_message
- started_at, finished_at

### agent_template (预置模板)
- id, code, name, industry, description
- emoji, color (CSS gradient)
- agents, workflow, tools, recommended_model
- usage_count, status

## 🔌 API 接口 (12 个)

| Method | Path | 描述 |
|--------|------|------|
| POST   | /api/v1/forge/projects | 创建项目 (含需求解析) |
| GET    | /api/v1/forge/projects | 列出我的项目 |
| GET    | /api/v1/forge/projects/{id} | 项目详情 |
| DELETE | /api/v1/forge/projects/{id} | 删除项目 |
| POST   | /api/v1/forge/releases | 创建 release |
| GET    | /api/v1/forge/releases/{id} | release 详情 |
| GET    | /api/v1/forge/releases?projectId=X | 项目的所有 release |
| POST   | /api/v1/forge/releases/{id}/deploy | 触发部署 |
| POST   | /api/v1/forge/releases/{id}/rollback/{targetId} | 回滚 |
| GET    | /api/v1/forge/releases/{from}/diff/{to} | 差异 |
| GET    | /api/v1/forge/templates | 全部模板 (按 industry 筛选) |
| GET    | /api/v1/forge/deployments/{id}/stream | **SSE 实时状态** |

## 🚀 8 阶段部署流程

1. **代码校验** (2s) - 检查智能体配置合法性
2. **构建镜像** (35s) - Docker build, N 个镜像并行
3. **镜像推送** (22s) - 推送到镜像仓库
4. **创建命名空间** (2s) - namespace 创建
5. **应用配置** (5s) - ConfigMap / Secret
6. **部署 Pod** (30s) - Deployment 创建
7. **健康检查** (10s) - Liveness / Readiness probe
8. **流量接入** (5s) - Service / Ingress

总耗时: 约 111s (V2.0 模拟), 真实环境取决于镜像大小

## 🔄 Manifest 自动生成

`ManifestGeneratorService` 基于 Freemarker 模板, 为每个智能体生成:

### Dockerfile
```dockerfile
FROM minimax/base-agent:v6.8 AS base
LABEL maintainer="agent-forge@minimax.io" version="1.0.0" agent.name="小课"
COPY prompts/ /app/prompts/
COPY config/ /app/config/
ENV AGENT_NAME=小课 AGENT_MODEL=Qwen2.5-7B AGENT_VERSION=1.0.0
HEALTHCHECK --interval=30s CMD curl -f http://localhost:8080/health
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/agent.jar"]
```

### K8s Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: xiaoke
  labels:
    app: xiaoke
    app.kubernetes.io/version: "1.0.0"
    managed-by: agent-forge
spec:
  replicas: 2
  selector:
    matchLabels:
      app: xiaoke
  template:
    metadata:
      labels:
        app: xiaoke
    spec:
      containers:
        - name: xiaoke
          image: registry.minimax.io/agent-forge/xiaoke:1.0.0
          ports: [{ containerPort: 8080 }]
          resources:
            requests: { cpu: "500m", memory: "1024Mi" }
            limits:   { cpu: "1000m", memory: "2048Mi" }
          livenessProbe: { ... }
          readinessProbe: { ... }
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
...
```

## 📡 SSE 实时推送

前端通过 `EventSource` 订阅部署状态:

```javascript
import { subscribeDeployment } from '@/api/forge'

const unsubscribe = subscribeDeployment(deploymentId, (event, data) => {
  switch (event) {
    case 'stage_start': /* 更新阶段状态 */ break
    case 'stage_done':  /* 标记阶段完成 */ break
    case 'log':         /* 追加日志 */ break
    case 'done':        /* 全部完成 */ break
  }
})
```

后端使用 `SseEmitter` 推送, 4 种事件:
- `stage_start` - 阶段开始 (含 stage 详情)
- `stage_done` - 阶段完成 (含 duration)
- `log` - 实时日志 (含 time/level/text)
- `done` - 全部完成 (含 status/duration/message)

## 🧠 需求解析 (LLM 驱动)

`RequirementsParserService` 接收用户需求, 输出:

### 输入
```json
{
  "source": "DOCUMENT",
  "content": "在线教育平台 7×24 小时智能客服...",
  "documentName": "requirements.pdf"
}
```

### 输出
```json
{
  "extracted": {
    "projectType": "教育 · 在线教育客服",
    "scenario": "在线教育平台 7×24 小时智能客服",
    "features": ["咨询", "推荐", "查询", "退款", "审核"],
    "scale": "日均 5000+ 会话, 峰值 200 并发",
    "compliance": ["个人信息保护法", "未成年保护"],
    "integrations": ["CRM", "工单系统", "支付系统"]
  },
  "agents": [
    { "name": "小课", "role": "课程顾问", "emoji": "📚", "tools": ["课程搜索"], "model": "Qwen2.5-7B" },
    { "name": "小助", "role": "退费专员", "emoji": "💰", "tools": ["订单查询"], "model": "Qwen2.5-7B" },
    ...
  ],
  "workflow": [
    { "step": 1, "name": "用户提问" },
    { "step": 2, "name": "意图识别" },
    ...
  ]
}
```

V2.0 实现: 规则 + 关键词提取 (mock LLM)
V3.0 规划: 接入 Qwen2.5-72B, 支持更复杂需求理解

## 📊 6 预置行业模板

| Code | 名称 | 智能体数 | 使用次数 |
|------|------|----------|----------|
| `edu-cs` | 在线教育客服 | 4 (小课/小助/小导/小审) | 1,247 |
| `ecom-cs` | 电商客服系统 | 3 (小购/小售/小评) | 892 |
| `code-review` | 代码评审助手 | 3 (小审/小测/小规) | 654 |
| `finance-risk` | 金融风控平台 | 3 (小风/小投/小审) | 421 |
| `medical-triage` | 医疗问诊机器人 | 3 (小医/小护/小顾) | 318 |
| `custom` | 自定义项目 | 0 (用户自由组合) | 256 |

## 🔧 本地启动

```bash
# 1. 编译 (需 Maven)
cd backend
mvn clean install -pl minimax-deployer -am -DskipTests

# 2. 启动 (端口 9010)
cd minimax-deployer
mvn spring-boot:run

# 3. 验证
curl http://localhost:9010/actuator/health

# 4. Swagger 文档
open http://localhost:9010/swagger-ui.html
```

## 🐳 Docker 启动

```bash
# 构建镜像
cd backend/minimax-deployer
docker build -t minimax-deployer:v2.0 .

# 运行
docker run -d \
  --name minimax-deployer \
  -p 9010:9010 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/minimax \
  minimax-deployer:v2.0
```

## 📈 监控指标

- `actuator/health` - 服务健康
- `actuator/metrics` - JVM / HTTP / 数据库指标
- `actuator/prometheus` - Prometheus 格式导出

## 🔜 V3.0 规划

- [ ] 接入 Qwen2.5-72B (真实 LLM 解析)
- [ ] ArgoCD GitOps 集成
- [ ] 多集群联邦部署
- [ ] 蓝绿发布 / 灰度发布
- [ ] 自动扩缩容 (HPA)
- [ ] Prometheus + Grafana 仪表盘
- [ ] Loki 日志聚合
- [ ] Jaeger 分布式追踪

## 📜 提交

- `minimax-deployer` 模块: 12 Java 文件 + 4 资源文件 + 1 pom.xml
- 1 个 API 层文件 (frontend/src/api/forge.js)
- 1 个 README 文档 (本文档)
