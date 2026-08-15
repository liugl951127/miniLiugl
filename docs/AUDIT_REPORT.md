# API 端到端审计报告 (V3.5.8)

> 本报告基于 2026-07-14 的代码快照, 审计脚本: `scripts/audit-api.py`

## 📊 总体规模

| 维度 | 数量 |
|---|---|
| **后端 Controller** | 52 个 (17 模块) |
| **后端 API 端点** | **506** 个 (去重) |
| **前端 Vue 视图** | 71 个 |
| **前端路由** | 79 个 (70 已注册) |
| **前端 API 调用** | 55 个去重 |
| **未调用端点** | 477 个 (内部/admin/AI 引擎) |
| **未注册视图** | 0 (V3.5.8 修复) |

### HTTP 方法分布

| 方法 | 后端 | 前端调用 |
|---|---|---|
| GET | 244 | 26 |
| POST | 217 | 21 |
| PUT | 19 | 4 |
| DELETE | 25 | 4 |

## ✅ V3.5.8 修复的 6 个真问题

| # | 问题 | 状态 | 修复 |
|---|---|---|---|
| 1 | `POST /api/v1/agent/plugins/{id}/call` 后端无 | ✅ | 后端实现 `PluginService.call()` |
| 2 | 前端 `/api/v1/models` 后端只在 `/admin` | ✅ | 前端改 `/api/v1/admin/models` |
| 3 | 前端 `/api/v1/monitor/health/all` 后端无 `/all` | ✅ | 前端改 `/api/v1/monitor/health` |
| 4 | `adminApi.governance.*` 不存在 | ✅ | 前端改用 `http.get()` 5 个端点 |
| 5 | Governance.vue 模板表达式编译错 | ✅ | 用 `computed: failRatePercent` |
| 6 | AgentController 缺 `@Slf4j` | ✅ | 加 `@Slf4j` + `log.error` |

## ✅ V3.5.8 注册的 7 个未引用视图

| 视图 | 路由 | 说明 |
|---|---|---|
| `admin/Governance.vue` | `/admin/governance` | 治理后台 (V2.9.0) |
| `ai/ModelMarket.vue` | `/admin/ai-market` | AI 模型市场 (V2.9.1) |
| `ai/Marketplace.vue` | `/admin/marketplace` | AI Agent 市场 (V2.9.0) |
| `ai/TensorBoard.vue` | `/admin/tensorboard` | 训练可视化 |
| `ai/TensorBoardStats.vue` | `/admin/tensorboard-stats` | 统计分布 |
| `ai/WebhookManager.vue` | `/admin/webhooks` | Webhook 集成 (V2.9.1) |

## 🔧 V3.5.8 命名空间统一

### 修复前
```javascript
// 写 /admin/... 依赖 http.js 自动加前缀 (隐式)
export const getAdminHealth = () => http.get('/admin/health')

// 写 /api/v1/admin/... 显式 (与其它模块不一致)
list: () => http.get('/api/v1/admin/models')
```

### 修复后
```javascript
// 所有 admin 端点统一显式 /api/v1/admin/...
export const getAdminHealth = () => http.get('/api/v1/admin/health')
list: () => http.get('/api/v1/admin/models')
```

### 改的模块

`admin.js` / `model.js` / `marketplace.js` / `monitor.js` / `tenant.js` 等 25 个 API 文件

## 📦 后端未调用端点 (477 个, V3.5.8 已分析)

### 分类

| 类别 | 数量 | 说明 |
|---|---|---|
| **Admin 后台** | 16 | 限管理员, UI 暂未集成全部 |
| **AI 引擎** | ~80 | `push / dashboard / model-market` 等内部端点 |
| **Agent 内部** | 30+ | `collab 协作 / kg 知识图谱` |
| **微信集成** | 8 | 微信扫码/UnionID |
| **其它** | ~340 | 辅助/管理/调试端点 |

### 主要未调用端点样例

```
GET    /api/ai/seed/check                            (V3.5.8 新增, debug 用)
GET    /api/v1/admin/audit/by-actor/{id}             (审计, 计划 Day 25 上线)
GET    /api/v1/admin/audit/by-day                    (审计, 计划 Day 25 上线)
GET    /api/v1/admin/audit/export                    (审计, 计划 Day 25 上线)
GET    /api/v1/ai/dashboard/tools/top                (管理仪表盘)
POST   /api/v1/ai/raft/trigger-election              (集群管理)
GET    /api/v1/ai/cluster/raft/leader               (集群管理)
POST   /api/v1/ai/datasources/{id}/query             (数据源, Day 22)
GET    /api/v1/ai/training/ema                       (训练, Day 24)
GET    /api/v1/ai/marketplace/agents/{key}/ratings   (市场, 已注册页面)
```

## 🔄 CI 集成 (V3.5.8)

`.github/workflows/audit.yml` 配置:

```yaml
on:
  push:    [main, develop]
  pull_request: [main]
  schedule: '0 9 * * 1'  # 每周一 9 点
  workflow_dispatch:       # 手动
```

### CI 行为

1. **每次 push/PR**: 跑审计, 警告未匹配端点
2. **每周一**: 定期审计, 生成报告
3. **手动**: 开发者主动跑

### 审计输出

- `audit-output.txt` (控制台日志)
- `report.json` (结构化数据)
- PR 评论 (自动)

## 🎯 后续建议

1. **Day 25**: 集成审计端点 (governance / audit)
2. **Day 26**: 集群管理 UI (raft / leader)
3. **Day 27**: 数据源管理 (datasources)
4. **持续**: CI 审计防止新增未对接接口

## 📚 文档

- [审计脚本](../scripts/audit-api.py)
- [CI 配置](../.github/workflows/audit.yml)
- [脚本 README](../scripts/README.md)
