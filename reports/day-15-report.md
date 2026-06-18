# Day 15 报告 - V4.3 Prompt 模板系统

**日期**: 2026-06-18
**目标**: V4.3 — Prompt 模板系统 (后端模块 + 前端 UI)
**Commit**: pending

---

## ✅ 完成项

### 1. CHANGELOG 大补全
- 补全 [V4.0] — 真实 AI 对接 + 多模型对决 + PlayGround
- 补全 [V4.1] — 文生图 + ASR/TTS + 排行榜 + Plugin SDK
- 补全 [V4.2] — WebSocket 流式 + PWA + i18n + 视频生成 + Agent DAG
- 新增 [V4.3] — Prompt 模板系统 (本文档内容)
- 更新整体数据: 13 模块 / 247 Java / 35 Vue / 27 SQL / 29 MD

### 2. minimax-prompt 模块 (第 14 个微服务, 8091 端口)
- **PromptApplication** — Spring Boot 3 启动类 (scanBasePackages 含 common)
- **PromptTemplate 实体** — id/name/description/category/content/variables(JSON)/creatorId/creatorName/isPublic/useCount
- **PromptTemplateMapper** — MyBatis-Plus BaseMapper
- **PromptTemplateService** — 完整 CRUD + 变量提取/解析 + 内置模板初始化 + 使用计数
- **PromptController** — 7 个 REST 端点:
  - `GET  /prompts`              — 分页列表 (分类+搜索过滤)
  - `GET  /prompts/{id}`         — 模板详情
  - `POST /prompts`              — 创建模板
  - `PUT  /prompts/{id}`         — 更新 (仅创建者)
  - `DELETE /prompts/{id}`       — 删除 (仅创建者, 软删)
  - `POST /prompts/{id}/use`    — 使用计数 +1
  - `GET  /prompts/categories`  — 全部分类
  - `POST /prompts/resolve`      — 变量填值生成最终 prompt
- **5 个内置系统模板**:
  1. 翻译助手 (变量: 目标语言, 原文)
  2. 代码审查 (变量: 语言, 代码)
  3. 会议纪要 (变量: 会议内容)
  4. 营销文案 (变量: 公司名, 产品名称, 产品特点, 目标受众, 推广渠道)
  5. 故障排查助手 (变量: 问题描述, 环境信息, 相关日志)
- **SecurityConfig** — JWT 鉴权 + CORS 配置
- **application.yml** — 8091 端口, context-path /api/v1

### 3. SQL 脚本
- `sql/19_prompt_template.sql` — prompt_template 表 + 5 个内置模板种子数据

### 4. 后端 pom 集成
- `backend/pom.xml` — 新增 `<module>minimax-prompt</module>`

### 5. 前端 (V4.3)
- `src/api/prompt.js` — promptApi (list/get/create/update/remove/use/categories/resolve)
- `src/store/prompt.js` — usePromptStore (Pinia)
- `src/views/prompts/Index.vue` — 完整模板管理页面:
  - 卡片视图 + 列表视图切换
  - 分类筛选 + 关键词搜索
  - 模板创建/编辑/删除 (仅创建者)
  - 变量填值弹窗 (textarea + 实时预览)
  - "🚀 填入对话" 一键跳转到 chat 页面
- `vite.config.js` — 新增 `/api/v1/prompts` → `http://localhost:8091` proxy
- `src/router/index.js` — 新增 `/prompts` 路由
- `src/layout/Index.vue` — 侧边栏新增 "Prompt 模板" 菜单
- `src/views/chat/Index.vue` — 支持 `?prompt=` query 参数自动填入消息框

### 6. 修复既有 bug
- `AudioShowcase.vue` — 修复 2 处 HTML 属性中的双引号语法错误 (description)
- `DagShowcase.vue` — 修复缺失 `</el-form>` 闭合标签
- `prompt.js` — 修复 `import { http }` → `import http` (default export)
- `Prompts/Index.vue` — 修复模板变量显示 `'{{' + v + '}}'` 解析错误

---

## 📊 代码统计

| 维度 | 数量 |
|------|------|
| 新增后端 Java | 7 (PromptApplication + SecurityConfig + Entity + Mapper + Service + Controller + ResolveRequest) |
| 新增 SQL 文件 | 1 (`sql/19_prompt_template.sql`, 80 行含种子数据) |
| 新增前端 Vue | 1 (`views/prompts/Index.vue`, ~350 行) |
| 新增前端 JS | 2 (`api/prompt.js`, `store/prompt.js`) |
| 修复既有文件 | 5 (CHANGELOG + backend/pom + vite.config + router + chat/Index + AudioShowcase + DagShowcase + prompt.js) |
| **总文件变更** | **~16 个** |

---

## ✅ 验证结果

| 检查项 | 结果 |
|--------|------|
| `scripts/self-check.sh` | ✅ 79/79 通过 |
| `scripts/java-static-check.sh` | ✅ 0 错误 |
| `npm run build` | ✅ 34.15s, 2949 模块 |
| 模块数 | 13 → **14** |
| Vue 文件 | 34 → **35** |

---

## 🚀 使用方式

1. **启动服务**:
   ```bash
   # 初始化数据库
   mysql -u minimax -p minimax < sql/19_prompt_template.sql

   # 启动后端 (8091)
   cd backend && mvn spring-boot:run -pl minimax-prompt

   # 启动前端
   cd frontend && npm run dev
   ```

2. **访问**: http://localhost:5173/prompts

3. **使用模板**:
   - 选择一个模板 → 点击 "▶ 使用"
   - 填写变量 → 点击 "🚀 填入对话"
   - 自动跳转到 chat 页面，预填消息框，直接发送

---

## 📅 版本历史

| 版本 | 日期 | 内容 |
|------|------|------|
| V4.0 | 2026-06-17 | 真实 AI 对接 + 多模型对决 + PlayGround |
| V4.1 | 2026-06-18 | 文生图 + ASR/TTS + 排行榜 + Plugin SDK |
| V4.2 | 2026-06-18 | WebSocket 流式 + PWA + i18n + 视频生成 + Agent DAG |
| **V4.3** | **2026-06-18** | **Prompt 模板系统 (本文档)** |
