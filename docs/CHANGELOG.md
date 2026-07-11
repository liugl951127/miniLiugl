# MiniMax Platform 变更日志

> **所有版本变更** · V1.0 → V2.8.6

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
