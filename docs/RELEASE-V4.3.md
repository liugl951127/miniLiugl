# MiniMax Platform V4.3 Release Notes
**发布日期：** 2026-08-08
**版本号：** V4.3
**构建：** 14 模块全部通过 | npm build ✅ | 静态检查 0 错误

---

## 🎯 核心亮点

| 功能 | 描述 |
|------|------|
| **SSE 智能重连** | 网络断开自动重连（指数退避，最多 2 次），重连中显示状态条 |
| **批量文档上传** | 知识库支持同时上传多个文件，队列管理 + 逐文件进度 + 取消/重试 |
| **投票一致率图表** | 新增 ECharts 图表页（折线/饼/柱图），展示一致率趋势 + 策略分布 |
| **静默告警** | 告警可静默（30m/1h/4h/1d/1w），静默期内不重复通知 |
| **ErrorBoundary 静默通知** | 组件错误捕获后右下角静默提示，不打断用户操作 |

---

## 🛠 新增 / 改动详情

### 前端改动

#### `frontend/src/views/knowledge/Index.vue` ⭐
- **批量上传**：单文件 → 多文件（`multiple`），完整上传队列 UI
  - 逐文件进度条 + 状态（上传中/成功/失败/已取消）
  - 单个取消 + 全量取消 + 单个重试
  - 超出 50MB 的文件自动跳过

#### `frontend/src/views/analytics/VoteStats.vue` ✨ NEW
- 投票一致率分析图表页（Day 37 新增）
- 4 个 ECharts 图表：
  - 📈 折线图：一致率趋势（近14天，可按策略过滤）
  - 🥧 饼图：投票策略分布（多数投票/加权投票/全票通过）
  - 🤖 柱图：各模型参与次数 TOP 10
  - ⏱ 柱图：响应延迟分布
- KPI 卡片：总投票数 / 平均一致率 / 平均模型数 / 平均延迟
- 投票记录分页表格 + 详情弹窗
- 新增路由：`/analytics/vote-stats`

#### `frontend/src/api/analytics.js`
- 新增 3 个投票统计 API：`getVoteStatsSummary` / `getVoteTrend` / `getVoteRecords`

#### `frontend/src/components/ErrorBoundary.vue` ⭐
- V6.3 → V7.0：恢复 `onErrorCaptured` 错误捕获
- 静默通知：`ElNotification` 右上角弹出，duration=0 不自动关闭
- 3 秒去重机制，防止同类错误重复通知
- 返回 `false` 阻止错误传播，保持 UI 降级可用

#### `frontend/src/views/ai/AiChat.vue`
- 流式中"发送"按钮变为"⏹ 停止"按钮
- `AbortController` 替代无效的闭包 `cancel()`
- 重连时顶部显示橙黄色 reconnecting-bar
- 自动重连 2 次，指数退避 1s→2s
- 重连成功后追加"🔄 第 N 次重连中"文字消息

#### `frontend/src/views/admin/Alerts.vue`
- 静默对话框：时长选择（30m/1h/4h/1d/1w）+ 截止时间选择器
- firing 卡片新增「静默/取消静默」按钮
- 规则表格新增静默状态列
- 字段映射修复：`alert.name` → `alert.ruleName`，`alert.service` → `alert.metricName`

#### `frontend/index.html` + `public/`
- SEO meta 标签（description/keywords/robots）
- Open Graph + Twitter Card
- `<link rel="canonical">` 规范 URL
- `public/sitemap.xml`：11 个路由，优先级 0.5-1.0
- `public/robots.txt`：允许所有爬虫

#### `frontend/src/views/chat/ChatMessage.vue`
- 打字光标颜色优化：`#6366f1` → `#818cf8`（更柔和）
- 字体放大 1.2em + text-shadow 晕光
- blink 动画优化：`0.8s ease-in-out`

### 后端改动

#### `minimax-monitor` 模块
- `MonitorController`：新增 4 个静默端点
  - `POST /alerts/{id}/silence` — 实例级静默
  - `POST /alerts/{id}/unsilence` — 取消静默
  - `POST /alerts/rules/{id}/silence` — 规则级静默
  - `POST /alerts/rules/{id}/unsilence` — 取消静默
- `AlertEngine.java`：`evaluateRule` / `fireAnomalyAlert` 加静默期检查
- 静默参数支持 minutes（默认 60）或 endTime（毫秒时间戳）

#### `minimax-ai` 模块
- `VotingChatController`：投票结果含 `agreementScore` / `modelAnswers` / `votingStrategy`
- `MultiModelVotingService`：多模型投票 + 一致率计算

#### `minimax-rag` 模块
- `TextChunker`：滑动窗口分块（512 字符 / 50 重叠）
- `DocumentService`：SHA-256 去重 + 状态机

#### SQL 改动
- `alert_event`：新增 `silenced_until TIMESTAMP`
- `alert_rule`：新增 `silenced_until TIMESTAMP`

### 测试文件（新增）
- `AlertSilenceTest.java`：3 用例（正常触发 / 实例静默 / 规则静默过期后恢复）
- `RagUploadProgressTest.java`：3 用例（TXT 切片 / 长文本多 chunk / TextChunker 边界）

---

## 📊 统计数据

| 维度 | 数量 |
|------|------|
| Java 文件 | 247 |
| Vue 文件 | 35 |
| 后端模块 | 14 |
| API 端点 | 92+ |
| 单元测试 | 125+ |
| 自检通过 | 14/14 ✅ |

---

## 🔗 相关资源

- **架构文档**：[ARCHITECTURE.md](./ARCHITECTURE.md)
- **API 文档**：[API.md](./API.md) / [docs/openapi.yaml](./docs/openapi.yaml)
- **部署指南**：[README-DOCKER.md](./README-DOCKER.md)
- **操作手册**：[OPERATIONS.md](./OPERATIONS.md)
