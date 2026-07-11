# 更新日志 (CHANGELOG)

> 完整版本演进记录, 适用 V2.5+ V2.8

## V2.8.2 (2026-07-12) - UX 增强 + 文档化

### 新增
- 📄 `docs/ARCHITECTURE.md` (9.5KB) - 架构白皮书
- 📄 `docs/USER_GUIDE.md` (4.4KB) - 用户手册
- 📄 `docs/API.md` (6.9KB) - API 参考
- 📄 `docs/DEPLOYMENT.md` (7.5KB) - 部署指南
- 📄 `docs/OPERATIONS.md` (5.4KB) - 运维手册
- 🧩 `components/PageContainer.vue` (1.6KB) - 通用页头+面包屑
- 🧩 `components/StatCard.vue` (2.3KB) - 统计卡片
- 🧩 `components/StateBlock.vue` (1.3KB) - 加载/空/错误状态
- 🆕 AI Chat 会话管理 (CRUD 4 端点 + Mapper 增强)
- 🆕 Login 现代化 (左功能面板 + 快速登录 + 记住我)
- 🆕 Admin Dashboard 快捷操作 (8 个 AI 模块入口)
- 🆕 KeywordEngine 增强测试 (9 用例)
- 🆕 Nl2Chart 增强测试 (4 用例)
- 🐛 修复 monitor 测试 (setResolved → setStatus)
- 🐛 修复 i18n zh.js/en.js (缺 export default 闭合)

### 改进
- 13 个意图识别 (新增 IMAGE/AUDIO/VIDEO_ANALYZE)
- 前端组件复用, 减少代码重复
- 详细算法注释, 业务人员可读

## V2.8.1 (2026-07-12) - 音乐流式生成

### 新增
- 🎵 `StreamingMusicGen` (9.6KB) - 音乐流式生成器
- 🎵 `MusicStreamController` (2.8KB) - 4 端点
- 🎵 `MusicStream.vue` (10.2KB) - 实时 MIDI 块展示
- 🧪 StreamingMusicGenTest (5 用例)

## V2.8.0 (2026-07-12) - CI/CD

### 新增
- 🚀 `.github/workflows/ci.yml` (5.7KB) - 4 job 流水线
- 🚀 `deploy/docker/Dockerfile.module` (1.1KB) - Multi-stage
- 🚀 `docker-compose.yml` (2.7KB) - 一键编排
- 🚀 `scripts/local-ci.sh` - 本地 CI 脚本

## V2.7.9 (2026-07-12) - RBAC

### 新增
- 🔐 `PermissionService` (2.4KB) - 4 角色权限
- 🔐 `PermissionAspect` (AOP) - 注解拦截
- 🔐 `SecurityContext` (1.3KB) - ThreadLocal
- 🔐 `RequiresPermission` 注解
- 🔐 `v-permission` 指令 (前端)
- 🧪 PermissionServiceTest (11 用例)

## V2.7.8 (2026-07-12) - i18n

### 新增
- 🌐 `I18nUtil` + `LocaleConfig`
- 🌐 `LangSwitcher.vue` (1.2KB)
- 🌐 80 个新 i18n keys
- 🌐 messages_zh_CN.properties + en_US.properties
- 🧪 I18nUtilTest (5 用例)

## V2.7.7 (2026-07-12) - 文档解析

### 新增
- 📄 `DocumentParser` (11.9KB) - PDF/Word/Excel
- 📄 `DocumentController` (1.7KB)
- 📄 Tika + POI 依赖
- 🧪 DocumentParserTest (9 用例)

## V2.7.6 (2026-07-12) - 视频流式生成

### 新增
- 🎬 `StreamingVideoGen` (10.6KB) - SSE 实时推送
- 🎬 `VideoStreamController` (2.6KB)
- 🎬 `VideoStream.vue` (9.8KB)
- 🧪 StreamingVideoGenTest (7 用例)

## V2.7.5 (2026-07-12) - 训练+AIGC+移动端

### 新增
- 📈 `TrainingTracker` (5.7KB) - 训练指标追踪
- 📈 `TrainerService` + `TrainingController`
- 📈 `TrainingViz.vue` (8.7KB) - TensorBoard 风格曲线
- 🎨 `ImageGenerator` (11.3KB) - 7 类型 AIGC
- 🎨 `ImageGen.vue` (5.8KB)
- 📱 `Discover.vue` + `Market.vue` - 移动端
- 🧪 15 用例 (Training + ImageGenerator)

## V2.7.4 (2026-07-12) - AI 工具完整 + 工作流

### 新增
- 🔗 `WorkflowEngine` (6.7KB) - DAG 拓扑排序
- 🔗 `WorkflowController` + `Workflow.vue`
- 🔗 3 工具 (DeduplicateTool, DistributionTool, SchemaCodeGenTool)
- 🔗 AiToolAdmin 智能参数表单
- 🧪 17 用例

## V2.7.3 (2026-07-12) - 工具补全

### 新增
- 3 工具: Deduplicate / Distribution / SchemaCodeGen
- 智能参数表单
- AiToolAdmin 增强

## V2.7.2 (2026-07-12) - 前端完整

### 新增
- Alerts.vue / Audit.vue / MaskTool.vue
- monitor.js SDK (30+ 方法)
- ai.js 完整

## V2.7.1 (2026-07-12) - 前后端集成

### 新增
- `AiPlatformController` (18+ 端点)
- `frontend/src/api/ai.js` (6KB)
- `AiChat.vue` (7KB) 智能路由 UI

## V2.7 (2026-07-12) - 多模态 AI 平台

### 新增
- `ChartGenerator` (26KB) - 7 图表
- `MusicGenerator` (13KB) - MIDI
- `AnimationGenerator` (14KB) - GIF
- `VideoComposer` (20KB) - 视频
- `DashboardBuilder` (16KB) - 看板
- `KeywordEngine` (10KB) - 13 意图
- `Nl2Chart` - NL→图表
- `DynamicDataSource` (12KB) - 真实数据库
- `RealTimeDataStream`

## V2.6 (2026-07-12) - 多模态 + 合规

### 新增
- `DataMasker` (9 脱敏类型)
- `FileEncryptor` (AES-256-GCM)
- `ContentModerator` (5min 缓存)
- `AuditLogger` (@Async 异步)
- `ImageAnalyzer` (pHash + 64-d embedding)
- `AudioAnalyzer` (RMS + 频谱)
- `VideoAnalyzer` (MP4 解析)

## V2.5 (2026-07-12) - 自研 AI 引擎

### 新增
- `MiniTransformer` / `ChineseTokenizer` / `MiniTrainer`
- `TextGenerator`
- `SimpleEmbedding`
- `MultiDataSourceManager` (7 数据库)
- `ProjectCodeGenerator` (6 类型)
- `DataAnalyzer`
- 4 内置工具
- `AiToolAdminController` + `AiToolAdmin.vue`

## V2.4 (2026-07-12) - JWT 规范化

- 统一 JWT Secret 生成
- 环境变量配置

## V2.3 (2026-07-12) - 编译修复

- 修复 7 编译 bug
- 完整 accept test

## V2.2 (2026-07-12) - 开箱即用增强

- upgrade.sh / tail-logs.sh / seed-data.sh
- .env.example
- 完整运维脚本

## V2.1 (2026-07-12) - 运维脚本

- status.sh / backup.sh
- OPERATIONS.md
- Knife4j 集成

## V2.0 (2026-07-12) - 性能优化

- G1GC + MaxRAMPercentage=70
- Tomcat 100 线程
- JVM 调优

## V1.9.x (2026-07-12) - 路由修复 + 部署

- 14 controller 路径修正
- 端口 8081-8093
- Nginx host 模式
- docker-nginx profile
- SKIP_NGINX_CHECK 选项

---

**当前版本**: V2.8.2
**总测试数**: 158+ 单元测试
**总微服务**: 17
**总前端页面**: 62
**总 i18n keys**: 532
