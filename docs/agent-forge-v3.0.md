# Agent Forge V3.0 — [已废弃, 历史版本]

> ⚠️ **本版本已废弃**, V4.0 重做设计后, V3.0 引入的"假 LLM 多层 fallback"和"假 ArgoCD GitOps"被删除。
> 请查看 [agent-forge-v4.0.md](./agent-forge-v4.0.md) 了解当前设计。

## 历史背景

V3.0 在 V2.0 基础上加:
- `LlmClientService` 调 minimax-ai 的 Qwen2.5 ONNX (3 层 fallback + 5 个模型)
- `ArgoCdService` 渲染 ArgoCD Application CRD + 模拟 Git push + 模拟 ArgoCD sync
- 5 个 LLM 模型选择下拉框
- "GitOps ⭐" 部署按钮

## V3.0 真实问题 (V4.0 修复)

| 问题 | 修复 (V4.0) |
|------|--------------|
| `@EnableAsync` 缺失 → `@Async` 失效 | 不再用 `@Async`, 改同步阻塞 + 日志 |
| 前端 `llmModel` 选择后端忽略 (DTO 没字段) | DTO 加 `llmModel` 字段 + 后端 LlmClient 接收 |
| `simulateGitPush` 返回 UUID 字符串 | 删, 改 `gitops` 模式显式 WARN |
| `simulateArgoCdSync` 仅 sleep 2s | 删, 改显式 WARN |
| `queryApplicationStatus` 返回 hardcoded Map | 删, 状态走 `/deployments/{id}` 真实反映 |
| 5 个模型 4 个不能调 | 简化为 3 个真实可用 (含 `rule-engine`) |

## 启动

V3.0 文件已被 V4.0 替代, 不再使用。
