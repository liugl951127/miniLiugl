# Day 17 Runbook — V4 基础设施补全

**日期**: 2026-06-21
**目标**: V4 基础设施补全 — Swagger文档 / i18n / 移动端H5 / WebSocket通知

---

## 背景

Day 16 完成 V3.1 多租户前端管理，遗留4个基础设施任务待推进：
- 移动端 H5 适配优化
- OpenAPI 3.0 / Swagger 文档生成
- WebSocket 实时通知前端
- 国际化 (i18n) 补全

---

## 任务清单

### 1. Swagger/OpenAPI (knife4j) — 全模块激活
**依赖**: knife4j 4.4.0 已在 root pom

**操作**:
- [ ] minimax-auth: AuthController, UserController 加 @Tag/@Operation
- [ ] minimax-chat: SessionController, MessageController 加 @Tag/@Operation
- [ ] minimax-model: ModelController 加 @Tag/@Operation
- [ ] minimax-memory: MemoryController 加 @Tag/@Operation
- [ ] minimax-rag: RagController, DocumentController, KnowledgeBaseController 加 @Tag/@Operation
- [ ] minimax-function: FunctionController 加 @Tag/@Operation
- [ ] minimax-agent: AgentController 加 @Tag/@Operation
- [ ] minimax-admin: AdminController 加 @Tag/@Operation
- [ ] minimax-prompt: PromptTemplateController 加 @Tag/@Operation
- [ ] minimax-monitor: MonitorController 加 @Tag/@Operation
- [ ] 各模块 application.yml 激活 springdoc + knife4j

### 2. i18n 国际化补全
**现状**: vue-i18n 框架已搭，zh/en 各70行，但17个页面未使用 t() 函数

**操作**:
- [ ] 读取 zh.js + en.js 了解结构
- [ ] 追加缺失 section: tenant/admin/monitor/prompt/agent/kg/collab/plugins/about
- [ ] 17个页面文件添加 import { t } from '@/i18n' 并替换硬编码

**17个待处理文件**:
```
views/admin/Index.vue
views/agent/Index.vue
views/auth/Login.vue
views/auth/WechatScanPage.vue
views/collab/Index.vue
views/kg/Index.vue
views/knowledge/Index.vue
views/memory/Index.vue
views/mobile/Index.vue
views/mobile/Me.vue
views/plugins/Index.vue
views/prompts/Index.vue
views/showcase/PluginShowcase.vue
views/showcase/VideoGenShowcase.vue
views/tenant/Index.vue
views/user/CrossAppBinding.vue
```

### 3. 移动端 H5 适配优化
**现状**: 6个页面已有基础（Agent/Chat/Index/Kg/Me/Plugins），使用 Vant

**操作**:
- [ ] Index.vue: 完善卡片式导航（Vant Cell Group）
- [ ] Chat.vue: 完善聊天界面（消息气泡/输入框/会话列表）
- [ ] Agent.vue: 完善任务输入/结果展示
- [ ] Kg.vue: 完善 ECharts 迷你图
- [ ] Plugins.vue: 完善插件卡片
- [ ] Me.vue: 完善个人中心
- [ ] router/index.js: 确认移动端路由守卫

### 4. WebSocket 实时通知中心
**后端**:
- [ ] sql/20_notification.sql: notification 表
- [ ] Notification 实体/Mapper/Service/Controller
- [ ] WebSocket endpoint: /ws/notifications
- [ ] 通知触发场景（会话创建/Agent完成/文档审核）

**前端**:
- [ ] src/api/notification.js
- [ ] src/store/notification.js
- [ ] src/views/notification/Index.vue
- [ ] layout/Index.vue: 🔔图标 + 红点

---

## 验收标准

- self-check.sh ≥ 78/78
- java-static-check.sh 0 错误
- npm run build 通过
- 所有修改 git push 成功

---

## 预计代码量

- Swagger 注解: ~50 个 @Operation + 10 个 @Tag
- i18n: 新增 ~80 行 locale key + 17 个文件改动
- Mobile: 6 个页面优化
- WebSocket通知: ~10 个后端文件 + 4 个前端文件
