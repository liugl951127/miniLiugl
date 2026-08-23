# Agent Forge V5.0 — 真实 GitOps 集成 (JGit + ArgoCD API)

> V4.1 把设计清理干净, V5.0 在 V4.1 基础上**真做** GitOps: 真实 git push + 真实 ArgoCD REST API 调用, 不再 "WARN 未集成"。

## 🎯 V5.0 vs V4.1 行为对比

| 阶段 | V4.1 (V4.0 修复) | V5.0 (本版本) |
|------|------------------|---------------|
| **生成 manifest** | ✅ 真生成 (K8s yaml + Dockerfile) | ✅ 同上 |
| **落 forge_manifest** | ✅ 真持久化 | ✅ 同上 |
| **Git push** | ❌ WARN "需配置" | ✅ **JGit 真 clone+commit+push** |
| **触发 ArgoCD sync** | ❌ WARN "需配置" | ✅ **真调 POST /api/v1/applications/{name}/sync** |
| **查询 ArgoCD 状态** | ❌ 无 | ✅ **真调 GET /api/v1/applications/{name}** |
| **轮询健康** | ❌ 无 | ✅ **同步阻塞 60s 轮询 + 状态机推进** |

## 🏗️ V5.0 架构

```
DeploymentService.deploy(id) 
  → GitOpsStrategy.execute()  (按 deploy_target='gitops' 路由)
    ├─ ManifestGenerator.generateAll(agents, cfg, Target.GITOPS, version)  ← V4.1 已类型化
    ├─ renderArgoApp() → CRD yaml
    ├─ GitOpsClient.pushManifests(repo, branch, path, files)   ← V5.0 NEW
    │   └─ JGit: clone → 写文件 → add → commit → push
    ├─ stateMachine.fire(DEPLOY)  (BUILDING → DEPLOYING)
    ├─ ArgoCdClient.triggerSync(appName)                       ← V5.0 NEW
    │   └─ POST {argoServer}/api/v1/applications/{name}/sync
    ├─ pollArgoCdStatus(appName, timeout=60s)                  ← V5.0 NEW
    │   └─ 每 3s GET /api/v1/applications/{name} (最多 20 次)
    └─ stateMachine.fire(READY) → fire(ACTIVATE)  (Health 状态)
```

## 📦 V5.0 新增文件 (3 个)

### 1. `gitops/GitOpsClient.java` (114 行)
JGit 真实 Git 操作:
- `clone` 或 `pull` (已有 workDir 时)
- 写 manifests 到 `${path}/${version}/`
- `add` + `commit` + `push`
- 凭证: HTTPS Basic Auth (`${GITOPS_USER}` / `${GITOPS_PASS}`)
- 失败抛 `GitOpsException` (不假装成功)

### 2. `gitops/ArgoCdClient.java` (114 行)
ArgoCD REST API 真实调用:
- `queryStatus(appName)` → `ApplicationStatus { name, syncStatus, health, revision }`
- `triggerSync(appName)` → POST `/api/v1/applications/{name}/sync`
- 凭证: Bearer Token (`${ARGOCD_TOKEN}`)
- 失败抛 `ArgoCdException`

### 3. `deploy/GitOpsStrategy.java` (重写)
集成真客户端, 9 阶段:
1. 加载 agents
2. 生成 K8s manifests (V4.1 ManifestGenerator)
3. 渲染 ArgoCD Application CRD
4. 落 forge_manifest 子表
5. **真 Git push** (V5.0)
6. 状态机 DEPLOY 事件
7. **真 ArgoCD sync 触发** (V5.0)
8. **真 ArgoCD 状态轮询** (V5.0, 最多 60s)
9. 根据 health 状态 fire(READY) 或 fire(FAIL)

## ⚙️ V5.0 配置

```yaml
# application.yml 新增
agent-forge:
  gitops:
    username: ${GITOPS_USER:}
    password: ${GITOPS_PASS:}
    local-path: ${GITOPS_LOCAL_PATH:/tmp/agent-forge-gitops}
  argocd:
    token: ${ARGOCD_TOKEN:}
```

环境变量 (生产用 secret 注入):
- `GITOPS_USER` / `GITOPS_PASS` — Git 仓库 HTTPS 凭证
- `GITOPS_LOCAL_PATH` — 本地 clone 目录 (默认 /tmp/agent-forge-gitops)
- `ARGOCD_TOKEN` — ArgoCD Service Account Token (RBAC: applications:get,sync)

## 🌐 V5.0 端点 (2 个新增)

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/v1/forge/argocd/applications/{appName}` | GET | **V5.0**: 查询 ArgoCD Application 真实状态 |
| `/api/v1/forge/argocd/applications/{appName}/sync` | POST | **V5.0**: 触发 ArgoCD 同步 |

## 🎨 V5.0 前端

- `Deploy.vue` 加 `gitops` 部署目标 (V5.0 ⭐ 标识)
- 选中 GitOps 时, 走 `triggerDeploy` 真 API + `pollArgoCdStatus` 轮询
- 5s 间隔, 最多 60s, 显示 `health=Healthy sync=Synced` 才算成功

## 📦 V5.0 依赖 (新增)

```xml
<dependency>
    <groupId>org.eclipse.jgit</groupId>
    <artifactId>org.eclipse.jgit</artifactId>
    <version>6.10.0.202406032230-r</version>
</dependency>
```

## 🚦 启动前置条件

| 模式 | 必需配置 |
|------|----------|
| `k8s` | 无 (模拟部署) |
| `gitops` | `GITOPS_REPO` + `GITOPS_USER` + `GITOPS_PASS` + `ARGOCD_SERVER` + `ARGOCD_TOKEN` |
| `edge` | `EDGE_TARGET` |
| `docker` | 无 (模拟) |

## 📜 提交

- `479cb665` V4.1 (上一版, 5 个真问题修复)
- 本次 V5.0 (JGit + ArgoCD API 真实集成)
- 后续 V6.0 计划: K8s API (Kubectl/Fabric8) 真调, 多集群蓝绿/金丝雀

## ⚠️ V5.0 限制

- 沙箱无 Java/Maven, 没法跑集成测试
- JGit 库依赖需要在生产构建环境装, 沙箱里没装 Maven
- 实际跑需要先有 Git 仓库 + ArgoCD 服务, 否则会报配置错
- 没 SSH key 支持 (仅 HTTPS Basic Auth)
