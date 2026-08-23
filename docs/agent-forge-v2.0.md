# Agent Forge V2.0 — [已废弃, 历史版本]

> ⚠️ **本版本已废弃**, V4.0 重做设计后, V2.0 的 6 处 JSON 字符串 + 散落状态管理被替换。
> V2.0 的核心 Service 已被 V4.0 替代, 代码不再被调用。
> 请查看 [agent-forge-v4.0.md](./agent-forge-v4.0.md) 了解当前设计。

## 历史背景

V2.0 是 Agent Forge 的第一版后端, 主要交付:
- `minimax-deployer` Spring Boot 模块 (12 Java, 9010 端口)
- 4 张 DDL 表 (forge_project / forge_release / forge_deployment / agent_template)
- 12 个 REST API + 1 个 SSE
- `ManifestGeneratorService` 用 FreeMarker 渲染 Dockerfile + K8s YAML + HPA + Edge 脚本
- `DeploymentOrchestrator` 跑 8 阶段模拟部署
- `RequirementsParserService` 用规则引擎 mock LLM

## V2.0 已知问题 (V4.0 修复)

| 问题 | 修复 (V4.0) |
|------|--------------|
| 6 处 JSON 字符串塞主表 | 拆 4 张子表 |
| 状态在 3 个 service 散落改 | 单一 `ReleaseStateMachine` |
| 模拟 8 阶段 | 单 `DeploymentService` 真实流程 |
| 规则 mock LLM | 真 `LlmClient` + `usedFallback` 显式标记 |

## 启动

V2.0 后端文件仍存在, 但已被 V4.0 替代。请参考 V4.0 文档启动。
