# Day 48 Report — 2026-08-19

## ✅ Day 48 - RAG批量导出 + Monitor ECharts可视化 + 深色模式 + JWT续期

### 自检全部通过

| 检查项 | 结果 |
|--------|------|
| self-check.sh | **13/13 ✅** |
| java-static-check.sh | **5/5 ✅**（仅测试文件建议）|
| vite build | ✅（59.77s，ECharts 1.1MB chunk + echarts 1.14MB）|

---

## Task 1 ⭐⭐⭐ RAG 文档批量导出（PDF/TXT）

### 后端（`minimax-rag`）

`DocumentService.exportDocs(List<Long> docIds, Long ownerId, String format)`:
- 校验归属（所有 doc 必须属于同一 owner）
- TXT：拼接所有文档内容（标题 + 元信息 + 正文），UTF-8 编码
- PDF：使用 PDFBox 3.0.2 生成多页 PDF，支持标题/正文样式
- 返回 `ExportResult(byte[], filename, contentType)`

**RagController 新端点：**
- `POST /api/v1/rag/doc/export`
  - body: `{ "docIds": [1, 2], "format": "pdf" | "txt" }`
  - 返回文件流（application/pdf / text/plain）

### 前端（`knowledge/Index.vue`）
- 工具栏新增**「导出」按钮**（绿色，与批量删除并排）
- 导出弹窗（500px）：
  - 格式选择：PDF / TXT（Radio）
  - 选中文档数 + IDs 预览
  - 下载进度条 + 完成后自动触发浏览器下载
  - 结果展示（成功提示）

**前端 API（`rag.js`）：**
- 新增 `exportDocs(ownerId, docIds, format)` 函数（原生 fetch，responseType: blob）

---

## Task 2 ⭐⭐ Monitor 告警统计 ECharts 可视化

### 后端（`minimax-monitor`）

`AlertMetricsService.getTimeSeries(Integer days)`：
- 按日期聚合告警数据（total / critical / warning / info）
- 自动补齐无告警的日期（填 0）
- 返回 List<Map> 时间序列数据

**MonitorController 新端点：**
- `GET /api/v1/monitor/alerts/timeseries?days=30`

### 前端（`monitor/Index.vue` 统计概览 Tab）

在现有数字卡片和 Top 规则区域**下方**新增两图并排展示：
- **左侧：饼图**（按严重程度分布：CRITICAL/WARNING/INFO，颜色 #f56c6c/#e6a23c/#909399）
- **右侧：柱状图**（近 N 天告警趋势，按日聚合，堆叠 CRITICAL/WARNING）
- 7/30/90 天窗口切换时图表联动刷新
- 每个图表支持「导出图片」按钮（ECharts `getDataURL` → PNG 下载）
- 图表随窗口 resize 自适应（`addEventListener('resize')`）

**前端 API（`monitor.js`）：**
- 新增 `getAlertTimeSeries(days)` 函数

---

## Task 3 ⭐ 前端深色模式切换

### 后端（`minimax-auth`）

**SQL**：新增 `user_preferences` 表
- `user_id`（唯一约束）、`theme`（light/dark）、`language`

**后端实体/Service/Controller：**
- `UserPreference` 实体（createdAt/updatedAt 自动填充）
- `UserPreferenceMapper`（MyBatis-Plus BaseMapper）
- `UserPreferenceService.getOrCreate(userId)` + `updateTheme(userId, theme)`
- `AuthController` 新端点：
  - `GET /api/v1/auth/preferences` — 获取偏好（theme + language）
  - `PATCH /api/v1/auth/preferences/theme` — 更新主题

### 前端

**`stores/preferences.js`**（Pinia + persist）：
- `theme`：'light' | 'dark'，localStorage 持久化
- `applyTheme(t)`：设置 `data-theme` + `el-theme-dark` class 到 document root
- `toggleTheme()`：切换并同步后端
- `fetchFromBackend()`：登录后从后端拉最新偏好

**`App.vue`**：挂载时初始化 preferences + 登录后拉后端偏好

**`layout/Index.vue`**：
- 头部右侧新增主题切换按钮（Moon/Sunny 图标切换）
- 深色模式 CSS 覆盖：`layout-header`、`layout-main`、`header-title`、`user-name`

**`App.vue` CSS 变量**：
```css
[data-theme="dark"] {
  --el-bg-color: #1a1a2e;
  --el-bg-color-overlay: #16213e;
  --el-text-color-primary: #e4e7ed;
  --el-border-color: #3a3f5c;
}
```

---

## Task 4 ⭐ API 认证中间件完善（JWT 续期）

### 后端（`minimax-common`）

**JwtAuthenticationFilter 增强：**
- 解析 JWT 成功后，检查 `剩余有效期 < 5 分钟`
- 满足条件时注入响应 header：`X-Token-Refresh: true`

**AuthController 新端点：**
- `GET /api/v1/auth/validate` — 校验 token 有效性（返回 valid + userId + username）

### 前端

**`stores/user.js` 新增：**
- `tokenExpiry`：记录 access token 过期时间戳
- `isRefreshing`：`true` 时防止并发重复刷新
- `silentRefreshIfNeeded(forceServerHint)`：
  - 条件：服务端提示 `forceServerHint=true` 或 token 剩余 < 5 分钟
  - 60 秒内最多刷新一次（防抖）
  - 静默失败不打断业务

**`api/http.js`：**
- 请求拦截器：发请求前主动检查 token 过期，提前静默刷新
- 响应拦截器：检测 `X-Token-Refresh: true` header → 调用 `silentRefreshIfNeeded(true)`

---

## 明日计划 Day 49

- [ ] RAG 文档在线预览（预览 Word/PDF，支持移动端）
- [ ] Monitor 告警实时推送（WebSocket SSE）
- [ ] 前端通知中心增加深色模式适配
- [ ] RAG 检索结果可高亮标注 + 复制片段
