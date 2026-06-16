# Day 9 报告 - Function Calling (工具调用)

**日期**: 2026-06-16
**目标**: 完整落地 Function Calling — 工具注册表 + 4 个内置工具 + LLM 工具循环 + 调用审计
**Commit**: pending

---

## ✅ 完成项

### 1. 数据模型 (2 张表)
- ✅ `function_tool` — 工具注册表: name/displayName/description/category/scope/ownerId/parameters/endpoint/enabled
- ✅ `function_call_log` — 调用审计: who/when/which tool/args/result/status/error/durationMs
- ✅ H2 兼容 schema (含 4 个内置工具种子数据)
- ✅ MySQL schema + 4 个内置工具 INSERT

### 2. 4 个内置工具
- ✅ `get_current_time` — 返回当前时间，支持时区
- ✅ `calculator` — **自实现表达式求值器** (Java 17 headless 缺 Nashorn, 不依赖 ScriptEngine)
  - 支持 + - * / % 括号
  - 数学函数: sin/cos/tan/sqrt/log/abs/exp/pow/max/min/floor/ceil/round
  - 一元负号
  - 字符白名单安全过滤
- ✅ `http_get` — HTTP GET, 含 SSRF 防护 (阻止 localhost/内网/FTP)
- ✅ `random_number` — 线程安全随机数 (ThreadLocalRandom)

### 3. 核心架构
- ✅ `ToolFunction` 接口 — 统一工具抽象
- ✅ `ToolExecutor` — 工具路由器 + 异常隔离 + 审计
  - 内置: 按 name 找 Spring bean
  - 自定义: HTTP POST JSON 到 endpoint
- ✅ `FunctionToolService` — 工具 CRUD + 权限 (builtin 只读 / user 仅 owner)
- ✅ `FunctionCallService` — **核心价值: LLM + tool 循环**
  - 工具定义转 OpenAI `tools` 格式
  - 调 LLM → 检测 `tool_calls` → 执行 → 结果回传 → 再次 LLM
  - 最多 N 轮 (默认 5)
  - 每轮都写审计
  - 降级: LLM 失败 / 工具失败都能跑

### 4. 暴露的 HTTP 端点 (10 个)
| 方法 | 路径 | 功能 |
|---|---|---|
| GET    | `/function/tools` | 列所有启用工具 |
| GET    | `/function/tools/category/{cat}` | 按分类 |
| GET    | `/function/tools/{id}` | 详情 |
| GET    | `/function/tools/by-name/{name}` | 按 name |
| POST   | `/function/tools` | 注册自定义工具 |
| PUT    | `/function/tools/{id}` | 更新 |
| DELETE | `/function/tools/{id}` | 删除 |
| POST   | `/function/invoke/{name}` | **直接调用 (无 LLM)** |
| GET    | `/function/logs` | 我的调用历史 |
| POST   | `/function/chat` | **chat + tool 循环** |

---

## 📊 关键数据

| 指标 | Day 8 | Day 9 | 增量 |
|------|-------|-------|------|
| Java 文件 | 135 | **152** | +17 |
| Java 行数 | 6995 | **8407** | +1412 |
| SQL 行数 | 411 | **610** | +199 |
| 单元/集成测试 | 62 | **85** | +23 |
| 端点 (function 模块) | 0 | **10** | +10 |
| 数据表 (function) | 0 | **2** | +2 |
| 内置工具 | 0 | **4** | +4 |
| 后端模块 | 7 | **8** | +1 |

### Day 9 新增测试 (23 用例)
- `BuiltinToolsTest` (13 cases):
  - timeTool / timeToolInvalidTimezone
  - calculatorAdd / calculatorWithParens / calculatorSqrt / calculatorPow
  - calculatorEmpty / calculatorInvalidChars
  - randomInRange / randomDefault
  - httpGetBlocksLocalhost / httpGetInvalidScheme / httpGetMissingUrl
- `FunctionIntegrationTest` (10 cases):
  - builtinToolsLoaded / registerUserTool / registerDuplicateNameFails
  - updateUserTool / updateOtherUsersToolDenied / deleteUserTool
  - invokeBuiltinByApi / invokeUnknownTool / invokeBadJson
  - callLogCreated

---

## 🏗️ 架构设计

### LLM tool 循环 (Day 9 核心)
```
User: "上海现在几点？明天 5+3*2 是多少？"
  ↓
[Round 1] LLM 看到 2 个 tool, 返回 tool_calls:
  → get_current_time(timezone="Asia/Shanghai")
  → calculator(expression="5+3*2")
  ↓ 执行 + 审计
[Round 2] 把 tool 结果回传给 LLM, LLM 整合回答
  ↓
Assistant: "上海现在是 2026-06-16 23:00, 5+3*2 = 11"
```

### 关键设计
1. **OpenAI tool_use 协议兼容** — 任何支持 function calling 的 LLM 都能用
2. **工具可插拔** — 新增内置工具只需 @Component implements ToolFunction
3. **安全过滤**
   - Calculator: 字符白名单 + 自实现求值 (无 ScriptEngine 注入风险)
   - HttpGet: 阻止 localhost/内网/非 http(s) (防 SSRF)
4. **降级完整**
   - LLM 不可用 → 返回 `[function unavailable] 你的问题: ...`
   - 工具失败 → 错误结果回传 LLM, 继续循环
   - 工具不存在 → 显式 error 不抛 500
5. **审计完整** — args/result/status/duration/ip/ua 全留痕
6. **无需额外依赖** — 复用 minimax-common (Result/异常) + minimax-model 协议
7. **多轮限流** — max-rounds=5 防 LLM 死循环

---

## 🔍 验证

### 单元/集成测试 (85 用例全过)
```
BuiltinToolsTest ........... 13/13  ← Day 9
FunctionIntegrationTest .... 10/10  ← Day 9
RagIntegrationTest ......... 8/8
TextChunkerTest ............ 6/6
VectorUtilsTest ............ 5/5
ContextBuilderTest ......... 4/4
MockEmbeddingClientTest .... 5/5
ShortTermMemoryTest ........ 4/4
VectorUtilsTest (memory) ... 7/7
JwtTokenProviderTest ....... 4/4
MessageRoleTest ............ 3/3
MockAdapterTest ............ 3/3
ModelProviderFactoryTest ... 4/4
StreamingTest .............. 3/3
                          ------
                          85/85 ✅
```

### Maven 编译 (8 模块)
```
minimax-platform ...... SUCCESS
minimax-common ........ SUCCESS
minimax-gateway ....... SUCCESS
minimax-auth .......... SUCCESS
minimax-chat .......... SUCCESS
minimax-memory ........ SUCCESS
minimax-model ......... SUCCESS
minimax-rag ........... SUCCESS
minimax-function ...... SUCCESS  ← Day 9 新增
======================
BUILD SUCCESS · 49s
```

---

## 🌐 GitHub

- 仓库: https://github.com/liugl951127/miniLiugl.git
- 状态: pending
- 改动: +17 java + 1 SQL + 1 schema + 2 测试 + 1 yml + 1 mapper xml + 1 pom

---

## 📁 新增文件

```
sql/09_function_calling.sql
sql/init/09_function_calling.sql
backend/minimax-function/pom.xml
backend/minimax-function/src/main/java/com/minimax/function/FunctionApplication.java
backend/minimax-function/src/main/java/com/minimax/function/config/MybatisPlusConfig.java
backend/minimax-function/src/main/java/com/minimax/function/entity/FunctionTool.java
backend/minimax-function/src/main/java/com/minimax/function/entity/FunctionCallLog.java
backend/minimax-function/src/main/java/com/minimax/function/mapper/FunctionToolMapper.java
backend/minimax-function/src/main/java/com/minimax/function/mapper/FunctionCallLogMapper.java
backend/minimax-function/src/main/java/com/minimax/function/executor/ToolFunction.java
backend/minimax-function/src/main/java/com/minimax/function/executor/ToolExecutor.java
backend/minimax-function/src/main/java/com/minimax/function/builtin/TimeTool.java
backend/minimax-function/src/main/java/com/minimax/function/builtin/CalculatorTool.java
backend/minimax-function/src/main/java/com/minimax/function/builtin/HttpGetTool.java
backend/minimax-function/src/main/java/com/minimax/function/builtin/RandomNumberTool.java
backend/minimax-function/src/main/java/com/minimax/function/service/FunctionToolService.java
backend/minimax-function/src/main/java/com/minimax/function/service/FunctionCallService.java
backend/minimax-function/src/main/java/com/minimax/function/controller/FunctionController.java
backend/minimax-function/src/main/resources/mapper/FunctionToolMapper.xml
backend/minimax-function/src/main/resources/mapper/FunctionCallLogMapper.xml
backend/minimax-function/src/main/resources/schema-h2.sql
backend/minimax-function/src/main/resources/application-test.yml
backend/minimax-function/src/test/java/com/minimax/function/BuiltinToolsTest.java
backend/minimax-function/src/test/java/com/minimax/function/FunctionIntegrationTest.java
reports/day-9-report.md
```

---

## 🚀 下一步 (Day 10: 管理后台)

- 用户管理 (CRUD, 启停, 重置密码, 角色分配)
- 模型管理 (Provider/Config 增删改, 限流调整)
- KB 管理 (列出所有 KB, 文档管理, 容量统计)
- 调用统计 (按日/周/月, 按用户/工具/模型)
- 系统监控 (服务状态, 健康检查, JVM 指标)
- 操作审计 (关键操作日志)
