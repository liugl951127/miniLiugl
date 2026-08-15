# 路由冲突全模块扫描修复 (V6.8.1)

## 扫描范围
全 14 module, 排除 GlobalMissingController + MissingAiController 兜底, 找真实 Controller 跟 Controller 冲突。

## 修复前: 13 个真实冲突
| 冲突端点 | Controller A | Controller B |
|---|---|---|
| GET /api/v1/ai/agent-group/auto/templates | AiAgentGroupRealController | framework/group/AutoAgentGroupController |
| POST /api/v1/ai/agent-group/auto/generate | AiAgentGroupRealController | framework/group/AutoAgentGroupController |
| POST /api/v1/ai/animation/text-fade | AiAnimationsRealController | AiPlatformController |
| POST /api/v1/ai/animation/progress | AiAnimationsRealController | AiPlatformController |
| POST /api/v1/ai/chat/voting | AiChatRealController | VotingChatController |
| GET /api/v1/ai/chat/voting-info | AiChatRealController | VotingChatController |
| GET /api/v1/ai/dashboard/health | AiDashboardRealController | DashboardController |
| GET /api/v1/ai/raft/log | AiRaftRealController | RaftController |
| GET /api/v1/ai/training/tasks | AiTrainingRealController | TrainingController |
| GET /api/v1/ai/training/tasks/{id} | AiTrainingRealController | TrainingController |
| GET /api/v1/ai/training/tasks/{id}/history | AiTrainingRealController | TrainingController |
| DELETE /api/v1/ai/training/tasks/{id} | AiTrainingRealController | TrainingController |
| DELETE /api/v1/ai/webhooks/{id} | AiWebhookRealController | WebhookController |

## 修法
保留 V6.6 *RealController, 改 V2.x 老的 framework/* + controller/* 路径加 -impl 后缀:

| Controller | 改前 | 改后 |
|---|---|---|
| framework/group/AutoAgentGroupController | /api/v1/ai/agent-group/auto | /api/v1/ai/agent-group/auto-impl |
| AiAnimationsRealController | /api/v1/ai/animation | /api/v1/ai/animation-impl |
| VotingChatController | /api/v1/ai/chat | /api/v1/ai/chat-impl |
| DashboardController | /api/v1/ai/dashboard | /api/v1/ai/dashboard-impl |
| RaftController | /api/v1/ai/raft | /api/v1/ai/raft-impl |
| controller/TrainingController | /api/v1/ai/training | /api/v1/ai/training-impl |
| WebhookController | /api/v1/ai/webhooks | /api/v1/ai/webhooks-impl |

## 修复后: 0 个真实冲突
```
$ python3 scripts/check-real-conflicts.py
无真实 Controller 冲突!
```

## 编译验证
```
mvn compile -pl minimax-ai -am
[INFO] BUILD SUCCESS

mvn compile -fae (全 14 module)
[INFO] BUILD SUCCESS (0 错)
```

## 前端影响 - 零
- 前端 `/ai/training/tasks*` → AiTrainingRealController (保留) ✅
- 前端 `/ai/agent-group/auto/*` → AiAgentGroupRealController (保留) ✅
- 前端 `/ai/raft/append` → AiRaftRealController (保留) ✅
- 前端 `/ai/chat/sessions*` → AiChatRealController (保留, 之前修过) ✅

## 剩余冲突 (37 个)
全为 GlobalMissingController 兜底跟真实 Controller 的重复 — 设计保留 (兜底优先级低, 真实 Controller 优先匹配)

## V6.8.1 累计路由冲突修复
| Commit | 冲突数 | 涉及 Controller |
|---|---|---|
| 809cc49 | 2 | AiAgentGroupAutoRealController |
| 27b8164 | 4 | AiPlatformController + AiChatRealController |
| 40ec6a8 | 7 | AiAdminRealController + AiToolAdminController |
| (本次) | 13 | 7 个 framework/* + controller/* 老 Controller |
| **累计** | **26** | **0 真实冲突** |
