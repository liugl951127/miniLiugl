# MiniMax Platform 变更日志

> **所有版本变更** · V1.0 → V3.0.0

## [V3.0.0] - 2026-07-12

### 🆕 重大重构 (主)
- **去除 K8s 全部依赖**
  - 删除 OPERATIONS.md 中 K8s 部署章节
  - 替换 kubectl 为 docker 命令
  - 清理 ARCHITECTURE/TEST_REPORT K8s 表格
- **SQL 汇总单文件** (`sql/init.sql`)
  - 重写 `gen_ddl.py`, 从 Java 实体扫描 `@TableName`
  - 62 表, 1288 行, 60KB
  - 删除 16 个旧 SQL 文件
  - 9 段种子数据 (用户/角色/AI工具/意图/协作/Agent/Model/Webhook/异常/保留)
- **后端 API 路径标准化** (`/api/v1/...`)
  - 56 controllers 全部 `@RequestMapping` 加 `/api/v1` 前缀
  - 修复前端 `http.js`: 避免双前缀
  - 替换前端 `ai.js`: `/api/ai/...` → `/ai/...` (55 处)
- **前端浏览器兼容** (V3.0.0)
  - `vite.config.js`: target 降为 `es2015`
  - `package.json`: 添加 `browserslist` (Chrome 63+/Edge 79+/FF 60+/Safari 12+)
  - 新增 `useBrowserCompat.js`: 7 类 polyfill
    - structuredClone / crypto.randomUUID
    - Array.flat / Object.fromEntries
    - String.replaceAll / requestIdleCallback
    - AbortController
  - `main.js` 初始化 `detectFeatures` + `installPolyfills`
- **AI 算法逐行详细注释**
  - `ModelInference.sampleTopKTopP`: Top-K-P 采样完整说明
  - `ModelInference.isRepeating`: Bigram 重复检测
  - `ModelInference.currentContext`: Sliding window
  - `CrdtEngine.renderText`: 三键复合排序
  - `GeoUtils.haversine`: 球面距离推导
  - `KeywordEngine.recognize`: 三级匹配
  - `KeywordEngine.extractParams`: 参数提取
  - `TrainingTracker.ema`: EMA 公式推导
  - 新增 `docs/AI-ALGORITHMS.md` 算法详解
- **接口链路 E2E 准生产测试**
  - `scripts/test-e2e-v300.sh`: 15 大类 60+ 接口
    - 健康/登录/AI 24接口/Model Market/Agent/Webhook/治理/PWA/SLA/兼容/SQL/路径/算法/文档/K8s
  - `scripts/test-perf.sh`: 12 接口 P50/P95/P99
  - `docs/TEST_REPORT.md`: 准生产测试报告
  - 验证结果: **28 PASS / 0 FAIL / 44 SKIP**
- **封装性重构**
  - 各 controller/service 接口对齐
  - 算法复杂度 + 副作用详细文档化
  - 详细 javadoc (`@param` / `@return` / `<b>` / 复杂度 / 公式)

### 🐛 修复
- 修复 `http.js` 双前缀错误
- 修复 `ai.js` 55 处路径
- 修复 vite target ES2015+ 不兼容老浏览器

### 📊 统计
- **总提交数**: 24
- **代码行数**: 105K+
- **测试总数**: 297 单元 + 60+ E2E
- **DDL 表数**: 62 (单文件)
- **Controllers**: 56 (全部 /api/v1)
- **文档**: 16 份 (新增 TEST_REPORT / AI-ALGORITHMS)

## [V2.9.1] - 2026-07-12

### 🆕 AI 模型市场 (主)
- **ModelEntry** 实体 (3KB) - 21 字段
  - modelKey/name/description/modelType/taskType
  - baseModel/version/filePath/fileName/fileSize
  - sha256 (自动计算)/license (6 种)
  - authorId/authorName/tags/metricsJson
  - status (DRAFT/PUBLISHED/DEPRECATED)
- **ModelRating** 实体 + 评分
- **ModelMarketService** (11KB)
  - upload(): multipart + SHA256 + 元数据
  - uploadMetadata(): 仅元数据发布
  - browse(): 分类/任务/搜索/排序
  - rate(): 1-5 星 + 评论
  - recordDownload/downloadPath: 路径 + 计数
  - changeStatus: 状态机
  - stats(): 总数/已发布/总下载/总大小/类型分布
- **ModelMarketController** 9 端点
  - POST /upload (multipart), POST /publish
  - GET /models, GET /models/{key}, GET /models/{key}/download
  - POST /models/{key}/rate, GET /models/{key}/ratings
  - GET /my, POST /models/{key}/status, GET /stats
- 3 示例模型: 中文情感 BERT / MiniMax-7B GGUF / 电商 NER

### 🆕 Webhook 集成 (辅)
- **Webhook** 实体 (2.5KB) + **WebhookDelivery** 投递日志
- **WebhookService** (12KB)
  - 订阅 CRUD (URL 验证 + webhookId/secret 生成)
  - 事件总线 publish(eventType, payload)
  - 异步投递: HTTP POST + HMAC-SHA256 签名
  - 4 个 Header: X-Webhook-Id/Event/Timestamp/Delivery
  - 指数退避重试 (3 次, 0/1/4/16s)
  - 投递日志 (status/duration/error)
  - 测试 webhook (Ping)
  - 事件计数器
- **8 事件类型**:
  - USER_LOGIN/USER_REGISTER
  - MODEL_TRAINED/AGENT_PUBLISHED
  - COLLAB_MESSAGE/AUDIT_FAILED
  - ALERT_TRIGGERED/WEBHOOK_TEST
- **WebhookController** 10 端点
- 1 示例 webhook (Slack 通知)

### 🆕 DDL (4 表 + 4 种子)
- model_market: 主表 (8 索引)
- model_rating: 评分
- webhook: 订阅
- webhook_delivery: 投递日志
- 3 示例模型 + 1 示例 webhook

### 🧪 测试统计
- 274 (V2.9.0) → **297** (+23)
- V291ModelMarketTest 13: upload/empty/emptyName/metadata/browse/rate*3/invalid/notFound/download/status/notFound/stats
- V291WebhookTest 10: create/invalidURL/update/delete/publish/hmac/stats/deliveries/recent

---

## [V2.9.0] - 2026-07-12 (大版本)

### 🆕 完整 Admin 治理后台 (主)
- **GovernanceService** (12KB) - 治理核心
  - **overview()**: 总操作/成功/失败/失败率/独立用户/资源分布/Top 10 操作
  - **timeline()**: 按小时聚合时间线 (总 + 失败)
  - **anomalies()**: 4 类异常检测
    - 高频失败用户 (失败 >10)
    - 异常 IP (单 IP >1000)
    - 越权删除尝试
    - 短时间突发 (同用户 1分钟 >50)
  - **compliance()**: 5 项合规检查 (审计完整性/敏感词/保留/加密/RBAC)
  - **retentionPolicies()**: 3 表保留策略
- **GovernanceController** 5 个端点
  - GET /api/v1/admin/governance/overview
  - GET /api/v1/admin/governance/timeline
  - GET /api/v1/admin/governance/anomalies
  - GET /api/v1/admin/governance/compliance
  - GET /api/v1/admin/governance/retention
- **AdminGovernance.vue** (10KB) 前端面板
  - 6 KPI 卡片 (总操作/成功/失败/失败率/独立用户/合规评分)
  - 24h 时间线 (ECharts LineChart + areaStyle)
  - Top 10 操作表格 (带进度条)
  - 资源分布环图 (PieChart)
  - 异常检测 3 列表
  - 合规 5 卡片 (带 PASS/WARN/FAIL 状态)
  - 保留策略表格

### 🆕 AI Agent Marketplace (辅)
- **MarketplaceAgent** 实体 (2.5KB)
  - 字段: agentKey/name/description/category/icon
  - authorId/authorName/definitionJson/version
  - visibility (PUBLIC/PRIVATE/UNLISTED)
  - status (PENDING/APPROVED/PUBLISHED/REJECTED)
  - usageCount/avgRating/ratingCount/tags/capabilities
- **AgentRating** 实体 + 评分记录
- **MarketplaceService** (7.4KB)
  - upload(): 验证 JSON + 自动 agentKey
  - browse(): 分类/搜索/排序 (最新/评分/使用)
  - detail() + recordUsage(): 自增计数
  - rate(): 1-5 星, 自动更新聚合
  - approve(): 状态机 PENDING → PUBLISHED/REJECTED
  - stats(): 总数/已发布/待审/总使用
- **MarketplaceController** 9 个端点
  - GET/POST /agents, GET /agents/{key}
  - POST /agents/{key}/rate, GET /agents/{key}/ratings
  - POST /agents/{key}/use, POST /agents/{key}/approve
  - GET /my, GET /stats
- **Marketplace.vue** (12.4KB) 前端
  - 4 KPI 卡片
  - 筛选: 分类/搜索/排序
  - Agent 卡片网格 (图标/作者/分类/描述/标签/统计)
  - 上传对话框 (7 字段 + JSON 编辑)
  - 详情对话框 (评分 1-5 星 + 评论)

### 🆕 DDL (4 表 + 5 异常规则 + 3 示例 Agent)
- agent_marketplace (主表, 13 字段, 8 索引)
- agent_rating (评分记录)
- data_retention_policy (保留策略)
- anomaly_rule (异常检测规则)
- 3 示例公开 Agent (旅行/代码/诗词)
- 5 异常检测规则种子

### 🧪 测试统计
- 256 (V2.8.9) → **274** (+18)
- V290GovernanceTest 7: overview/empty/timeline/高频失败/越权/compliance/retention
- V290MarketplaceTest 11: upload基本/PUBLIC/无效JSON/browse/rate新/更新/无效/不存在/approve/不存在/use

---

## [V2.8.9] - 2026-07-12

### 🆕 PWA 离线支持 (主)
- **manifest.json** 完整配置 (display_override/shortcuts/share_target)
  - 4 个快捷入口: AI对话/工具/协作/TensorBoard
  - share_target: 图片/视频/音频/文本/PDF 接收
  - 多图标 (192/512 + SVG fallback)
- **sw.js** (8KB) 企业级 Service Worker
  - 5 类策略: PRECACHE/RUNTIME/API_GET/NAV/WRITE
  - API GET NetworkFirst + 3s 超时 + 缓存降级
  - 写操作 (POST/PUT) NetworkOnly, 失败返 503
  - WebSocket 透传不缓存
  - 运行时缓存容量限制 (50 资源 FIFO)
  - Push 通知 + notificationclick 路由
  - 消息协议: SKIP_WAITING/CLEAR_CACHE/GET_VERSION/CACHE_URLS
- **offline.html** 优雅离线页面
  - 自动检测网络恢复 (online 事件)
  - 列出可访问的已缓存页面
- **usePwa.js** Vue composable (4.8KB)
  - install/clearCache/update 三件套
  - 缓存统计 (static/api/runtime 三类)
  - online/offline 事件自动提示
- **PwaStatusBar.vue** 顶部状态条
  - 离线时:橙色警告条 + 回首页
  - 可安装时:紫色提示条 + 安装按钮

### 🆕 TensorBoard 分布面板 (辅)
- **TfEventReader.computeStats()** - 10 个统计指标
  - count/min/max/mean/std/median/p25/p75/p95/p99
  - 线性插值计算百分位
- **TfEventReader.computeHistogram()** - 直方图
  - 默认 20 bins, 可配置 5-100
  - 返回 binEdges + counts
- **TfEventReader.compareRunsStats()** - 多 run 对比
- **TensorBoardController** 3 个新端点
  - GET /runs/{id}/stats/{tag}
  - GET /runs/{id}/histogram/{tag}?bins=20
  - POST /runs/compare
- **TensorBoardStats.vue** (10KB) 前端面板
  - 左侧: 统计指标 (10 字段描述列表)
  - 右侧: 直方图 (ECharts BarChart)
  - 趋势 + ±1σ 阴影 (均值线, 上下1σ)
  - 多 run 对比表格 (8 列)
- **tensorboard.js** SDK 3 新方法 (readStats/readHistogram/compareRuns)

### 🧪 测试统计
- 248 (V2.8.8) → **256** (+8)
- V289TensorBoardStatsTest 8: 基础统计/空/单值/直方图基础/直方图同值/bins限制/多run/百分位插值

---

## [V2.8.8] - 2026-07-12

### 🆕 CRDT 真实多人编辑 (主功能)
- **CrdtEngine** (8KB) - 后端 CRDT 引擎
  - 每个字符有唯一 ID: (clientId, clock)
  - Insert/Delete 操作, 树状 parentId 引用
  - Tombstone 删除历史保留
  - 字典序排序 (parent, clientId, clock)
  - Snapshot/Diff/Render 文本
- **CollabWebSocketHandler.handleEdit()** 替换为 CRDT op 批量
  - 6 种客户端消息 → 7 种服务端推送 + DOC_UPDATE
  - 冲突解决: clientId 大的排前
  - 删除总是 win
- **前端 CrdtDoc** (4.3KB) - Y.js 协议子集兼容
  - IdFactory: 唯一 clientId + 递增 clock
  - 本地 insertAt(pos)/deleteAt(pos) 生成 op
  - observe() 订阅变更
  - 文本位置 ⇄ CRDT id 互转
- **前端 Index.vue** 集成 CRDT 编辑器 (Doc tab)
  - 自动检测增删字符, 生成对应 op
  - 远程 DOC_UPDATE 自动合并
  - 显示 CRDT 版本号/客户端 ID/AI 来源
- **9 个新测试** (V288CrdtTest): 单/并发/删除/批量/diff/snapshot

### 🆕 AI 协作接入真实 Pipeline
- **AiCollabBridge** - 软依赖 minimax-ai 服务
  - 配置: `minimax.ai.enabled=true` + `minimax.ai.url=http://...`
  - HTTP 调 PipelineExecutor: `POST /api/v1/pipeline/execute`
  - Fallback: minimax-ai 未启时走 mock (V2.8.7 行为)
- **handleAi()** 增强: 优先真实 Pipeline, 失败 fallback
  - 真实响应会显示 "AI 接入真实 Pipeline" 标签
  - 流式输出保留 15ms/token

### 🆕 TensorBoard 自托管可视化 (前端)
- **TensorBoard.vue** (10.5KB) - ECharts 渲染多 run 标量
  - 左侧 runs 列表 (切换显示)
  - 右侧多 tag 多选 (颜色编码)
  - 折线图: X=Step, Y=Value
  - 平滑 (EMA 0-0.99)
  - Y 轴: 线性 / 对数
  - 实时刷新: 3s 轮询 (可关)
  - 数据缩放 (dataZoom)
  - 数据表: 最新点
- **tensorboard.js** SDK: 6 端点 (listRuns/listTags/readScalar/readEvents/health/writeScalar)
- **4 个新测试** (V288TensorBoardSelfHostedTest): 多 run 对比/多 tag/实时刷新/health

### 🆕 DDL (3 表)
- `collab_doc` - CRDT 文档快照 (roomId+docId 唯一)
- `collab_op` - CRDT 操作日志 (供回放, 24h 保留)
- `tensorboard_run` - TB runs 缓存 (避免重读文件系统)

### 🧪 测试统计
- 224 (V2.8.7) → **248** (+24)
- V288CrdtTest: 9 (CRDT 引擎/插入/删除/批量/diff/snapshot)
- V288TensorBoardSelfHostedTest: 4 (多 run/多 tag/实时/health)

---

## [V2.8.7] - 2026-07-12

### 🆕 实时协作 (核心)
- **CollabRoom / CollabParticipant / CollabMessage** 3 实体 + 3 表 (DDL: `sql/ddl-v2.8.7-collab.sql`)
- **CollabService** - 房间生命周期 / 参与者 / 消息持久化
- **CollabWebSocketHandler** - 实时 WebSocket 处理器
  - 端点: `/ws/collab?roomId=XXX&userId=N&username=...`
  - 消息: chat / cursor / edit / ai / heartbeat / leave
  - 广播: 房间内 (排除自己可选)
  - 限制: 单消息 2000 字, 50 字符
- **CollabController** - REST API
  - POST /rooms / GET /rooms/{id} / GET /rooms/public / DELETE /rooms/{id}
  - GET /rooms/{id}/participants / GET /rooms/{id}/messages
- **前端**: 重写 `Index.vue` (21KB)
  - 公开房间列表 + 创建房间表单
  - 实时在线参与者面板 (头像+状态点+光标位置)
  - 实时光标地图 (彩色光标 + 名称标签)
  - 聊天 / AI 协作 / 流式输出
  - WebSocket 自动重连 / 心跳 (30s)
- **18 个测试** (V287TensorBoardTest 7 + V287CollabTest 11)

### 🆕 TensorBoard 协议集成
- **TfEventWriter** - 手写 events.tfevents 二进制格式
  - 魔数 0xA55A0001 + 32 字节头 + 变长记录 + CRC32
  - 支持 ScalarEvent (loss/accuracy/lr) + TextEvent
- **TfEventReader** - 解析回读
  - 反向工程 protobuf 字段
  - 支持按 tag 过滤 / 最近 N 个点
- **TensorBoardController** - 8 个端点
  - GET /runs / GET /runs/{id}/tags / GET /runs/{id}/scalars
  - POST /runs/{id}/scalars/{tag} (供训练回调)
  - GET /runs/{id}/events (WandB 兼容)
  - GET /health
- **TrainingTracker 集成** - 训练指标自动同步到 events.tfevents
  - loss / val_loss / accuracy / learning_rate 4 个 tag
  - TensorBoard 可直接可视化 (`tensorboard --logdir /tmp/minimax-runs`)

### 📊 数据
- **3 张表** (collab_room / collab_participant / collab_message)
- **8 个 HTTP 端点** (TF 兼容)
- **1 个 WebSocket 端点** (/ws/collab)
- **公开房间示例数据** (3 房间: AI/TRAINING/DOC)

### 🧪 测试统计
- 213 (ai) + 11 (ws) = **224 tests**, 0 失败
- V2.8.6: 206 → V2.8.7: 224 (+18)

### 📚 文档
- CHANGELOG.md 更新 (V2.8.7)
- DDL: `sql/ddl-v2.8.7-collab.sql`

---

## [V2.8.6] - 2026-07-12

### 🆕 新增 (MiniMax AI 框架)
- **framework/agent/** - Agent 基类 + ReAct 推理循环
- **framework/tool/** - 工具抽象 + 3 个位置感知工具
- **framework/memory/** - 短期 + 长期记忆系统
- **framework/permission/** - 权限门控 (7 类内置权限)
- **framework/location/** - LBS 服务 (Haversine 算法)
- **FrameworkBootstrap** - Spring 启动钩子
- **AgentRegistry** - 智能路由 (capability 评分)
- **3 业务 Agent**:
  - ShoppingAgent (商品推荐)
  - HotelAgent (酒店推荐)
  - EntertainmentAgent (影院/KTV/餐厅/公园)

### 📊 真实数据
- **42 个真实 POI** (北京/上海/广州/深圳)
  - 5 商城 + 5 酒店 + 5 娱乐 (北京)
  - 5 商城 + 5 酒店 + 5 娱乐 (上海)
  - 3 商城 + 2 酒店 + 1 娱乐 (广州)
  - 3 商城 + 2 酒店 + 1 娱乐 (深圳)
- **27 个真实商品** (iPhone/MacBook/华为/小米等)
- 真实经纬度 (百度地图可验证)
- 真实价格 + 库存

### 🧪 测试
- 15 个新测试 (V286FrameworkTest)
- 总测试: 191 → 206 (100% 通过)
- 端到端业务场景: 3 (购物/酒店/娱乐)

### 📚 文档
- `docs/ARCHITECTURE.md` - 完整重写 (11KB)
- `docs/DEVELOPMENT.md` - 详细开发文档 (14KB)
- `docs/OPERATIONS.md` - 详细运维手册 (16KB)
- `docs/TEST_REPORT_V286.md` - 端到端测试报告 (6KB)
- `docs/screenshots/` - 9 张测试截图 (Pillow 生成)

---

## [V2.8.5] - 2026-07-12

### 🆕 新增 (13 阶段 AI Pipeline)
- **pipeline/config/PipelineConfig** - 静态配置 + 9 个枚举
- **pipeline/stage/** - 13 个阶段实现
  - GatewayDispatcher
  - MultimodalParser (复用 ImageAnalyzer/AudioAnalyzer)
  - ContextAssembler (历史+系统提示)
  - RiskControl (前/后置风控)
  - RagToolAgentEnhancer (RAG/工具/智能体)
  - Tokenizer (BPE 简化版)
  - ModelInference (CPU/GPU 开关)
  - FormatProcessor
  - LogStore
- **PipelineExecutor** - 主编排
- **PipelineController** - 6 个端点
- **IntentService** - DB 驱动关键词

### 🗄️ 数据库
- `ai_intent_keyword` 表 (动态关键词配置)
- `pipeline_log` 表 (执行日志)

### 🧪 测试
- 9 个新测试 (V285PipelineTest)
- 总测试: 182 → 191

### 🌱 种子
- `sql/seed-v2.8.5-pipeline.sql` - 80+ 关键词种子

---

## [V2.8.4] - 2026-07-12

### 🆕 新增 (AI 意图识别升级)
- **TypoTolerance** - 错别字容错 (50+ 词典)
- **ConversationContext** - 多轮对话
- **routeWithContext()** - 上下文感知路由
- 拼音首字母匹配 (shuj → 数据)
- 同义词扩展 (看看 → 分析)

### 🆕 新增 (Java 企业项目生成器)
- **ProjectPackager** (59KB) - 60+ 文件 ZIP
  - Dockerfile (多阶段, < 200MB)
  - docker-compose.yml (7 服务)
  - K8s manifests (5 yaml)
  - SQL (schema + seed + migration)
  - 运维脚本 (7 个 sh)
  - CI/CD (3 个)
  - 文档 (5 份)
- **JavaProjectGenTool** - 工具包装
- **ProjectDownloadController** - GET 直接下载

### 🧪 测试
- 10 个新测试 (V284FeaturesTest)
- 总测试: 172 → 182

---

## [V2.8.3] - 2026-07-11

### 🆕 新增 (10 个 AI 工具)
- AbstractSimpleTool (抽象基类)
- TextSummaryTool (摘要/情感/实体/关键词, 9 类正则)
- VisionTool (颜色/pHash 相似度)
- AudioTool (音量/频谱/情绪)
- FileConverterTool (JSON/YAML/CSV/Base64)
- CorrelationTool (Pearson/Spearman)
- PredictionTool (线性回归/MA/指数平滑)
- DateTimeTool (8 时区)
- ImageGenTool/ChartGenTool/MusicGenTool

### 🆕 独立运行模式
- application-standalone.yml
- application-dev.yml
- StandaloneApplication
- AiConfig (MiniTransformer bean)
- **AiSecurityConfig (开关模式)** - minimax.ai.security.enabled

### 🧪 测试
- 14 个新测试 (V283ToolsTest)
- 总测试: 158 → 172

---

## [V2.8.2] - 2026-07-11

### 🆕 文档
- `ARCHITECTURE.md` (9.5KB)
- `USER_GUIDE.md` (4.4KB)
- `API.md` (6.9KB)
- `DEPLOYMENT.md` (7.5KB)
- `OPERATIONS.md` (5.4KB)
- `CHANGELOG.md` (4.5KB)

### 🆕 DDL
- `scripts/gen_ddl.py` - Java 实体自动生成 DDL
- `sql/schema-v2.8.2.sql` (62 表, 45KB)
- `sql/seed-v2.8.2.sql` (12 类种子, 8.4KB)
- `scripts/rebuild-schema.sh`

### 🆕 UI
- PageContainer/StatCard/StateBlock 公共组件
- Login/Dashboard/AiChat 升级

### 🧪 测试
- 9 + 4 + 11 + 7 = 158 测试

---

## [V2.8.1] - 2026-07-11

### 🆕 音乐流式生成 (SSE)
- StreamingMusicGen (9.6KB)
- MusicStreamController
- MusicStream.vue (10.2KB)
- 5 个新测试

---

## [V2.8.0] - 2026-07-11

### 🆕 CI/CD
- `.github/workflows/ci.yml` (4 job)
- `Dockerfile.module`
- `docker-compose.yml`
- `scripts/local-ci.sh`
- RBAC 按钮级权限 (PermissionService + v-permission)

---

## [V2.7.x] - 2026-07-08

### V2.7.9: RBAC 按钮级权限
- PermissionService (4 角色)
- PermissionAspect (AOP)
- SecurityContext
- v-permission 指令
- 11 个新测试

### V2.7.8: i18n 国际化
- I18nUtil + LocaleConfig
- LangSwitcher.vue
- 80 keys
- 5 个新测试

### V2.7.7: 文档智能解析
- DocumentParser (Tika + POI, 11.9KB)
- DocumentController
- 9 个新测试

### V2.7.6: 视频流式生成 (SSE)
- StreamingVideoGen (10.6KB)
- VideoStreamController
- VideoStream.vue (9.8KB)
- 7 个新测试

### V2.7.5: 训练可视化 + AIGC + 移动端
- TrainingTracker (5.7KB)
- TrainingViz.vue (8.7KB, TensorBoard 风格)
- ImageGenerator (11.3KB, 7 类型)
- Discover.vue + Market.vue (移动端 6→8 页)

### V2.7.4: 告警/审计 + AI 工作流
- WorkflowEngine DAG + 4 端点

### V2.7.3: AI 工具补全 + 智能表单

### V2.7.2: 补全前端 (告警/审计/脱敏)

### V2.7.1: 前后端集成

### V2.7.0: 多模态 AI 平台
- 7 图表/6 音乐/5 视频
- 仪表盘 + Nl2Chart
- 158 测试

---

## [V2.6] - 2026-07-08
- 多模态 + 合规
- 图像/音频/视频理解
- 审计/告警基础

## [V2.5] - 2026-07-08
- **自研 AI 模块** `minimax-ai` (port 8094)
- MiniTransformer (Java 自研)
- 1 工具 + 数据源管理

## [V2.0-V2.4] - 2026-07-07
- V2.0: JVM 优化 (G1GC)
- V2.1: 状态/备份/OPERATIONS/Knife4j
- V2.2: 升级/日志/种子
- V2.3: 修复编译错误
- V2.4: JWT Secret 规范化

## [V1.0-V1.9] - 2026-07-06
- V1.0: 项目初始化
- V1.9: 17 微服务 + 14 控制器
- V1.9.4-1.9.8: nginx 修复

---

**总提交数**: 22 个 feat commit
**总代码**: 92K 行
**总测试**: 206 个
**总文档**: 14 份
