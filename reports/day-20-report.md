# Day 20 Report — V5.9 Day 20 (2026-06-24)

## 今日完成

### 1. API Key 用量统计 (前后端全链路)

**后端: ApiKeyStatsService + AdminController 扩展**
- 新增 `ApiKeyStatsService.java`：聚合 auth 服务的 `/auth/apikeys` 数据
  - `summary()`: 全局 Key 总数 / 启用 / 禁用 / 总调用 / 平均调用 / Top 用户
  - `newKeysTrend(int days)`: 近 N 天新增 Key 趋势
  - 降级策略: auth 服务不可达时返回 `status: unavailable` 而不抛异常
- `AdminController` 新增 2 端点:
  - `GET /admin/stats/apikey` — API Key 全局统计摘要
  - `GET /admin/stats/apikey/trend?days=7` — 新增趋势

**前端: API Key 统计页面**
- 新建 `src/views/apikey/Stats.vue` (~300 行):
  - 4 个 KPI 卡片: 总 Key / 启用 / 禁用 / 总调用
  - ECharts 饼图: Key 状态分布 (启用 vs 禁用)
  - Top 用户调用量排行榜 (rank badge + 次数)
  - 配额概览: 独立用户 / 平均调用 / 启用率
- 扩展 `src/api/apikey.js`: 新增 `adminSummary()` + `adminTrend()`
- 路由: `/apikey-stats` (需超级管理员权限)
- 侧边栏: 超级管理员下新增「📊 Key 统计」入口

### 2. E2E 健康检查脚本

- 新建 `scripts/e2e-health-check.sh`:
  - 检查 12 个微服务: Gateway / Auth / Chat / Model / Memory / RAG / Function / Agent / Monitor / Admin / Analytics / Prompt
  - 每个服务检查: `/actuator/health` + `/api/v1/health`
  - Gateway 额外检查: `/actuator/prometheus`
  - WebSocket 端口 TCP 检测
  - 环境变量覆盖端口: `GATEWAY_PORT=3000 AUTH_PORT=8081 ...`
  - 彩色输出: `[PASS]` / `[FAIL]` / `[WARN]` / `[INFO]`
  - 汇总结果: `N 通过 / N 失败`，失败时 `exit 1`

### 3. Postman API Collection

- 新建 `docs/minimax-api.postman_collection.json`:
  - 13 个分组: 认证 / 聊天 / 模型 / 记忆 / 知识库 / 工具 / Agent / 监控 / 管理 / 分析 / 提示词 / 系统
  - 50+ 个请求: 覆盖所有 REST 端点
  - Collection Variables: `{{baseUrl}}` / `{{accessToken}}` / `{{userId}}`
  - 自动脚本: 登录后自动填充 `accessToken`
  - 支持 Bearer Token 和 API Key 两种认证

### 4. 自检通过

- SQL 语法检查: ✅
- Maven 编译: ✅ (19 个模块)
- 前端构建: ✅ (1m, 0 错误)
- 关键配置文件: ✅
- Java 静态体检: ✅ (0 TODO / 0 System.out / 0 e.printStackTrace)

## 关键文件

| 文件 | 变更 |
|------|------|
| `backend/minimax-admin/.../service/ApiKeyStatsService.java` | 新增 |
| `backend/minimax-admin/.../controller/AdminController.java` | 扩展 |
| `frontend/src/views/apikey/Stats.vue` | 新增 |
| `frontend/src/api/apikey.js` | 扩展 |
| `frontend/src/router/index.js` | 扩展 |
| `frontend/src/layout/Index.vue` | 扩展 |
| `scripts/e2e-health-check.sh` | 新增 |
| `docs/minimax-api.postman_collection.json` | 新增 |

## 代码量

- Java: +1 文件 (~120 行)
- Vue: +1 文件 (~300 行)
- JS: +1 文件 (扩展)
- Shell: +1 文件 (~150 行)
- JSON: +1 文件 (~400 行)

## 明日计划 Day 21

- [ ] 性能压测脚本 (wrk / Bash 并发压测)
- [ ] API 文档导出为 Markdown / PDF
- [ ] API Key 配额前端告警提示
- [ ] 外部 API 限流规则 CRUD 页面
