# MiniMax Multi-Agent 协作指南 (V5.17)

> V5.17 引入 3 角色多智能体协作 — Planner 规划 + Executor 执行 + Critic 评估, 失败自动重规划

## 1. 架构

```
                    ┌──────────────────────┐
                    │   用户目标 (goal)    │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  Planner 规划师      │  LLM 直调
                    │  - 拆解目标 3-7 步  │  (低温度 0.3)
                    │  - 接受 critic 反馈  │
                    └──────────┬───────────┘
                               │ planSteps[]
                               ▼
                    ┌──────────────────────┐
                    │  Executor 执行者     │  复用 AgentService.run
                    │  - 逐个执行子任务    │  (ReAct + 工具循环)
                    │  - 调用工具          │
                    └──────────┬───────────┘
                               │ results[]
                               ▼
                    ┌──────────────────────┐
                    │  Critic 评估者       │  LLM 直调
                    │  - 是否达成目标      │  (低温度 0.2)
                    │  - 评分 0-10         │
                    │  - 改进建议          │
                    └──────────┬───────────┘
                               │
                ┌──────────────┴──────────────┐
                │ ✓ 通过                       │ ✗ 不通过
                ▼                              ▼
        ┌──────────────┐               ┌──────────────────┐
        │ final answer │               │ feedback → Planner│
        │ done event   │               │ 重规划 (max 3 轮) │
        └──────────────┘               └──────────────────┘
```

## 2. 端点

### 2.1 同步多智能体

```bash
curl -X POST http://localhost:3000/api/v1/agent/multi/run \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "userId": 1,
    "goal": "给我公司生成一份 MiniMax 平台的竞品分析报告",
    "tools": ["get_current_time", "http_get"]
  }'
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "success": true,
    "finalAnswer": "✅ 竞品分析报告 ...",
    "rounds": 1,
    "criticPassed": true,
    "steps": [
      {"criticRound": 1, "stepIndex": 1, "goal": "...", "observation": "...", "durationMs": 1234}
    ],
    "criticEvals": [
      {"round": 1, "passed": true, "feedback": "报告完整, 评分 8/10"}
    ],
    "totalDurationMs": 5432
  }
}
```

### 2.2 流式多智能体 (SSE)

```bash
curl -N -X POST http://localhost:3000/api/v1/agent/multi/stream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userId":1, "goal":"...", "tools":[]}'
```

**SSE 事件类型**:
| 事件 | 触发时机 | data |
|------|---------|------|
| `multi-agent-start` | 任务开始 | goal, maxCriticRounds |
| `planner-start` | Planner 开始 | round, feedback |
| `planner-plan` | Planner 完成 | round, steps[] |
| `executor-step` | Executor 开始 1 步 | round, step, goal |
| `executor-result` | Executor 完成 1 步 | round, step, observation, durationMs |
| `critic-eval` | Critic 评估中 | round, plan, results |
| `critic-result` | Critic 评估完 | round, passed, score, feedback |
| `critic-retry` | Critic 不通过, 重规划 | round, feedback |
| `final` | 最终答案 | answer, rounds |
| `done` | 任务结束 | success, answer, rounds, criticPassed, totalDurationMs |
| `error` | 出错 | message |

### 2.3 单独 Planner

```bash
POST /api/v1/agent/multi/plan
{
  "goal": "查明天上海天气",
  "feedback": "上次漏了上海徐汇区"   // 可选
}
```

### 2.4 单独 Critic

```bash
POST /api/v1/agent/multi/critic
{
  "goal": "查明天上海天气",
  "plan": ["调 get_current_time", "调 http_get 查天气"],
  "results": "【1】调 get_current_time\n→ 当前时间 2026-06-21\n【2】调 http_get 查天气\n→ 明天上海 25°C 晴"
}
```

## 3. 前端可视化

路径: `/agent/multi`

**功能**:
- 流式/同步双模式
- 3 角色实时展示 (颜色编码: 蓝=Planner, 绿=Executor, 黄=Critic)
- Critic 通过/不通过状态 (绿/红背景)
- 失败自动重规划 (最多 3 轮)
- 原始事件流 (JSON 调试)

## 4. 与单智能体对比

| 维度 | V5.16 单 Agent | V5.17 Multi-Agent |
|------|---------------|-------------------|
| 角色数 | 1 (ReAct) | 3 (Planner+Executor+Critic) |
| 评估 | 无 | Critic 评估 (passed/score) |
| 重规划 | 无 | 失败自动重规划 (max 3 轮) |
| 质量保证 | 取决于 prompt | Critic 反馈循环保证 |
| 适用 | 简单任务 | 复杂任务 (报告/分析/多步) |
| 延迟 | 10-30s | 30-90s (3 角色串行) |

## 5. 关键设计

### 5.1 Critic 反馈循环

```
Round 1: Planner → plan1
         Executor → results1
         Critic → ✗ (feedback1)
            ↓
Round 2: Planner(plan1 + feedback1) → plan2
         Executor → results2
         Critic → ✗ (feedback2)
            ↓
Round 3: Planner(plan2 + feedback2) → plan3
         Executor → results3
         Critic → ✓
            ↓
         final answer
```

### 5.2 角色模型

- **Planner**: 温度 0.3 (稳定 + 一定变化)
- **Executor**: 复用 V5.16 AgentService.run (ReAct + tools, 温度 0.4)
- **Critic**: 温度 0.2 (稳定 + 严格评估)

### 5.3 超时

- 整体 3 分钟 (`SseEmitter(180_000L)`)
- 单次 LLM 调用 60s
- maxCriticRounds = 3 (可调)

## 6. V5.17 新增文件

| 文件 | 用途 |
|------|------|
| `backend/minimax-agent/src/main/java/com/minimax/agent/service/MultiAgentService.java` | 14KB, 3 角色协作核心 |
| `backend/minimax-agent/src/main/java/com/minimax/agent/controller/AgentController.java` | 加 4 端点 |
| `frontend/src/views/agent/Multi.vue` | 12KB, 3 角色可视化 |
| `frontend/src/router/index.js` | `/agent/multi` 路由 |
| `docs/MULTI-AGENT-GUIDE.md` | 本文档 |
