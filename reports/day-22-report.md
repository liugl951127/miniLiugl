# Day 22 Report — 2026-06-27

## ✅ Day 22 - WebSocket 联调 + 鉴权测试 + RAG 链路 + CI 压测

**今日完成：**

### 1. WebSocket 端到端联调（V5.22）
- **Stream.vue JWT Token 支持**：更新 `/api/v1/ws/bidi` WebSocket URL，加入 JWT Bearer token 参数（`token=<jwt>`），未登录时显示友好提示
- **WebSocket 工具类** `frontend/src/utils/ws.js`（~4.7KB）：
  - `createWS()` 工厂函数，统一管理连接生命周期
  - 自动重连（指数退避，最多 30s）
  - 心跳保活（25s 间隔）
  - 统一消息解析（JSON 自动 parse，原始数据透传 `_raw`）
  - 可配置 `reconnectDelay` / `heartbeatInterval` / `logPrefix`
- **notification store 重构**：使用 `createNotificationWS()` 替换手写 WS 逻辑，代码更简洁可靠

### 2. API Key 鉴权过滤器单元测试
- **ApiKeyAuthGlobalFilterTest** `backend/minimax-gateway/src/test/java/.../filter/`（~7.4KB）：
  - 无 Authorization 头 → 放行
  - 普通 JWT Bearer Token（非 `mmx_`）→ 放行
  - `mmx_` 格式 + Redis 命中 → 注入 `X-User-Id` 头 + `X-User-Source: apikey` 放行
  - `getOrder() = -200`（早于 JwtAuthFilter 的 -100）
  - SHA-256 cache key 格式验证（`apikey:` + 64 字符 hex）
- **ApiKeyAuthGlobalFilter** 重构：新增测试用 3-参数构造函数（`WebClient` + `StringRedisTemplate` + `authServiceUrl`），同时保留 Spring 注解注入方式

### 3. RAG 完整链路测试（5 个新用例）
- `uploadChunkRetrieve_fullFlow`：上传 Python/Rust/Go 三类文档 → 检索"并发编程"/"数据分析" → 验证 chunk 计数 + docId 注入
- `multiKbIsolation`：金融库/医疗库隔离检索，跨 KB 不泄露内容
- `ragService_emptyQuestion`：空问题返回"问题不能为空"
- `ragService_emptyRetrievalFallsBackToPlainChat`：空检索降级普通 chat
- `parserRegistry_routesCorrectly`：TXT/PDF/DOCX 三格式路由上传成功

### 4. CI/CD 压测 Stage（V5.22 Day 22）
- `.github/workflows/ci.yml` 新增 `perf-test` job（Job 5）：
  - 依赖 backend job（使用其构建产物）
  - GitHub Actions service: MySQL 8 + Redis 7
  - 启动 minimax-gateway + minimax-auth JAR
  - 安装 wrk（源码编译）+ Apache Bench
  - 运行 30s / 50 并发 / 4 线程 登录 API 压测
  - 上传 `.txt` / `.tsv` 压测结果为 GitHub Artifacts
  - 非阻塞（不影响 PR 合并）
- Summary job 新增 Perf Test 行

**关键文件数：** +6 新文件（test × 2 + ws.js + ci.yml + filter 重构）
**代码量：** +~16KB

**自检结果：** 自检 5/5 ✅ | Java 静态 0 错误 ✅ | 前端构建 51s ✅

**明日计划 Day 23：**
- [ ] 前端 E2E 测试（Playwright）集成到 CI
- [ ] 监控告警系统完整链路测试
- [ ] 前端知识库管理 UI 完善
- [ ] API Key 管理前端 UI
