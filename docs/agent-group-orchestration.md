# 智能体群编排 (Agent Group Orchestration) — 使用文档

> 一句话把单个智能体编排成群，按 workflow 协作执行。后端编排器 + 前端拖拽式设计器。

## 概述

平台支持把若干个**已注册或可描述的智能体**（每个有 prompt + tools）按**位置 (position) + 角色 (role)** 编排成**一个智能体群**，群按选定的 **strategy** 协作完成用户目标。运行过程以 **SSE 流式** 推回前端。

- **PIPELINE**（顺序）：上一步的输出 → 下一步的输入，流水线式。
- **PARALLEL**（并行）：所有成员同时跑，结果合并。
- **DEBATE**（辩论）：MANAGER 提问题 → N WORKER 各自答 → CRITIC 评分 → 选最高。

## 数据模型

`agent_group_member`（minimax-common/src/main/resources/sql/minimax-schema.sql）：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| group_id | BIGINT | 关联 AgentGroup.id |
| agent_code | VARCHAR(64) | 智能体编码（如 `writer`、`analyst`） |
| role | VARCHAR(32) | MANAGER / WORKER / CRITIC |
| position | INT | 顺序位（0=首位） |
| config_json | VARCHAR(2000) | 个性化配置（JSON 字符串，可选） |
| enabled | TINYINT | 1=启用，0=禁用 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

## API 列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/agent-group/{groupId}/members` | 列出群所有成员（按 position 升序） |
| POST | `/api/v1/agent-group/{groupId}/members` | 添加成员（body: `{agentCode, role, position, configJson}`） |
| PUT | `/api/v1/agent-group/{groupId}/members/{memberId}` | 更新成员 |
| DELETE | `/api/v1/agent-group/{groupId}/members/{memberId}` | 删除成员 |
| PUT | `/api/v1/agent-group/{groupId}/members/reorder` | 批量重排（body: `[{memberId, position}, ...]`） |
| POST | `/api/v1/agent-group/{groupId}/run` | 流式执行（body: `{goal, strategy, tools}`） → SSE |
| GET | `/api/v1/agent-group/strategies` | 可用策略列表 |

## 三种 Strategy 详解

### PIPELINE（顺序执行）
- 按 position 升序依次执行每个成员
- 第 N 个成员的 prompt 包含前 N-1 个成员的输出
- 适用：写作链（大纲 → 扩写 → 润色）、分析链（数据收集 → 清洗 → 统计 → 报告）

### PARALLEL（并行执行）
- 用 `CompletableFuture.allOf` 并行调用所有 WORKER
- MANAGER 角色单独先跑（确定分解子任务）
- 最后由 MANAGER 合并所有 WORKER 输出
- 适用：调研、并行分析、并行搜索

### DEBATE（辩论）
- MANAGER 先提问题（可选）
- 每个 WORKER 独立回答
- CRITIC（若有）对每个回答打分（0-1）
- 选最高分作为 final；返回所有候选
- 适用：方案对比、决策建议

## SSE 事件协议

`POST /api/v1/agent-group/{groupId}/run` 返回 `text/event-stream`：

| 事件 | data 字段 | 说明 |
|------|-----------|------|
| `step-start` | `{agentCode, role, position, groupStrategy}` | 一个成员开始执行 |
| `step-token` | `{agentCode, content}` | 该成员输出增量 token |
| `step-end` | `{agentCode, output, durationMs}` | 成员完成 |
| `final` | `{success, finalAnswer, totalSteps, durationMs, error?}` | 全部完成 |
| `error` | `{message, agentCode?}` | 执行出错 |

## 前端 GroupDesigner 操作步骤

1. 菜单 → 智能体 → **智能体群编排**
2. 顶部下拉选群（默认 1 号群）
3. 选 strategy（PIPELINE / PARALLEL / DEBATE）
4. 左侧候选池拖入右侧成员区，或在右侧添加
5. 调整 position（拖拽）和 role（MANAGER / WORKER / CRITIC）
6. 点 **保存** → 调 addMember/updateMember/removeMember 同步
7. 底部输入目标 (goal)，点 **运行**
8. 右侧抽屉实时显示各 step 输出，step-token 增量追加

## 后端实现要点

- `AgentGroupOrchestrator`（`minimax-ai`）：核心入口，注入 `ApplicationContext` 反射取 `GroupStrategy` bean
- `GroupStrategy` 接口：`String name()` + `void execute(userId, members, goal, emitter)`
- 3 个实现：PipelineStrategy / ParallelStrategy / DebateStrategy（都在 `marketplace/orchestrator/`）
- `SseEmitterUtil`：统一封装事件推送（带 try-catch + 完整事件关闭）
- `AgentInvoker`：抽象 agent 调用层（可接 LLM / Mock / 已有 Agent）
- MyBatis-Plus 实体 + Mapper 完整 CRUD
- Gateway 路由：`/api/v1/agent-group/**` → `lb://minimax-ai`

## 后续可扩展

- [ ] DAG 编辑器（按依赖关系编排，而不只是顺序）
- [ ] 循环 / 条件分支 strategy
- [ ] 群运行历史 + 重放
- [ ] 群模板（保存/克隆）
- [ ] 多模态输出（图片/音频嵌入 step-token）
