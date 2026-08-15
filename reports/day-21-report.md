# Day 21 Report — 2026-06-26

## ✅ Day 21 - API 文档体系 + 压测模板

**今日完成：**

### 1. OpenAPI 3.0 规范（完整 92+ 端点）
- `docs/openapi.yaml`（~42KB）：完整的 OpenAPI 3.0.3 规范，涵盖全部 10 个模块
  - Auth / Chat / Model / Memory / RAG / Function / Admin / Multimodal / Monitor / Actuator
  - 包含统一的 Schema（Result / UserInfo / TokenResponse / Session / Message 等）
  - 每个端点含 summary / description / 请求示例 / 响应示例
  - 支持 JWT Bearer 鉴权配置
- `docs/openapi.json`：同步生成 JSON 版本（便于程序读取）

### 2. Postman Collection
- `docs/postman/MiniMax-Platform.postman_collection.json`（~22KB）
  - 10 个模块分组，45+ 个请求
  - 全局变量：baseUrl / accessToken / refreshToken / userId / sessionId 等
  - 登录请求自动提取 Token 注入后续请求
  - 含测试脚本（验证 code=0 / Token 提取）
  - 支持无 Token 公开端点（noauth）

### 3. E2E 健康检查脚本（14 服务全覆盖）
- `scripts/health-check.sh`（~8.8KB）
  - 18 项检查：全部 14 个微服务 + JWT 鉴权 + CORS + 响应时延
  - 自动登录获取 Token（无需手动配置）
  - HTTP 200/401/404 均视为 PASS（按端点类型判断）
  - 环境变量可覆盖：`GATEWAY_HOST / GATEWAY_PORT / TIMEOUT / TOKEN`

### 4. 性能压测模板
- `bench/wrk/login.lua`：wrk Lua 脚本（登录 API，keepalive）
- `bench/wrk/chat.lua`：wrk Lua 脚本（聊天 API，支持 Token 环境变量）
- `bench/jmeter/minimax-api-test.jmx`：JMeter 测试计划
  - 3 个 Thread Group：基准 10 并发 / 负载 50 并发 / 压力 200 并发
  - 6 个核心采样器：登录 / 创建会话 / 模型列表 / RAG / Admin / Monitor
  - 自带 Aggregate Report + Table Visualizer
- `bench/run.sh`：一键执行脚本（支持 wrk / ab / JMeter / all）
- `bench/README.md`：压测报告模板 + 指标解读 + 填写指南

### 5. 自检脚本增强
- `scripts/self-check.sh`：升级为环境感知版
  - mvn 未安装时跳过（不误报失败）
  - 前端 npm install + build 完整支持

**关键文件数：** +7 新文件（不含文档）/ 修改 1 脚本
**代码量：** +~85KB 新文档资源（YAML/JSON/脚本）

**自检结果：** 5/5 PASS ✅ | Java 静态体检 0 错误 ✅ | 前端构建 1m 20s ✅

**明日计划 Day 22：**
- [ ] WebSocket 端到端联调（前端连接 + 消息推送验证）
- [ ] RAG 文档上传 + 切片 + 检索完整链路测试
- [ ] API Key 鉴权中间件单元测试
- [ ] CI/CD 流水线增强（GitHub Actions 压测 stage）
