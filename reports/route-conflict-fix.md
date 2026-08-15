# 路由冲突修复 (V6.8.1)

## 冲突
- `AiAgentGroupAutoRealController` (类级 @RequestMapping("/api/v1/ai/agent-group/auto"))
  - L35: `@PostMapping("/execute")` → `/api/v1/ai/agent-group/auto/execute`
- `AiAgentGroupRealController` (类级 @RequestMapping("/api/v1/ai/agent-group"))
  - `@PostMapping("/auto/execute")` → `/api/v1/ai/agent-group/auto/execute`
  - `@PostMapping("/auto/generate")` → `/api/v1/ai/agent-group/auto/generate`

## 冲突
- `POST /api/v1/ai/agent-group/auto/execute` ← 2 个 Controller 重复
- `POST /api/v1/ai/agent-group/auto/generate` ← 2 个 Controller 重复

## 修法
- `AiAgentGroupAutoRealController`:
  - `/generate` → `/generate-auto` (避免 /auto/generate 冲突)
  - `/execute` → `/run` (避免 /auto/execute 冲突)

## 当前最终路径
**AutoRealController**:
- POST /api/v1/ai/agent-group/auto/generate-auto
- POST /api/v1/ai/agent-group/auto/run

**RealController**:
- GET  /api/v1/ai/agent-group/auto/templates
- POST /api/v1/ai/agent-group/auto/templates
- POST /api/v1/ai/agent-group/auto/execute
- POST /api/v1/ai/agent-group/auto/generate
- GET  /api/v1/ai/agent-group/auto/template/{id}
- GET  /api/v1/ai/agent-group/groups

## 前端影响
- ✅ 无前端代码引用 `agent-group/auto/execute` 或 `agent-group/auto/generate`
- ✅ 改名安全

## 编译验证
```
mvn compile -pl minimax-ai -am
[INFO] BUILD SUCCESS
```
