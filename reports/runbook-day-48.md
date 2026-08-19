# Day 48 Runbook — 2026-08-19

## 今日任务

### Task 1 ⭐⭐⭐ RAG 文档批量导出（PDF/TXT）

**后端（`minimax-rag`）**
- `DocumentService.exportDocs(List<Long> docIds, String format, Long ownerId)`
  - format: `pdf` | `txt`
  - 校验归属
  - PDF：用开源库（iText / OpenPDF）或纯文本拼接
  - TXT：拼接 chunk 文本，UTF-8
  - 返回 byte[] 或文件路径
- `RagController` 新端点：
  - `POST /api/v1/rag/doc/export`
  - body: `{ "docIds": [1,2], "format": "pdf" }`
  - 返回文件流（application/pdf / text/plain）

**前端（`knowledge/Index.vue`）**
- 工具栏新增「导出」按钮（绿色）
- 导出弹窗（500px）：
  - 格式选择：PDF / TXT（Radio）
  - 选中文档数提示
  - 下载进度（axios responseType: 'blob'）
  - 成功后触发浏览器下载

**前端 API（`rag.js`）**
- 新增 `exportDocs(ownerId, docIds, format)` 函数

---

### Task 2 ⭐⭐ Monitor 告警统计 ECharts 可视化

**后端**
- `AlertMetricsService.getTimeSeries(Integer days)`：
  - 返回每日告警数量趋势（date + count，按 severity 分组）
- `MonitorController`：
  - `GET /api/v1/monitor/alerts/timeseries?days=30`
  - 返回 List<Map> 时间序列数据

**前端（`monitor/Index.vue` 统计概览 Tab）**
- 引入 ECharts（已通过 CDN 或 npm）
- 左侧：饼图（按 severity 分布：CRITICAL/WARNING/INFO）
- 右侧：柱状图（近 30 天告警趋势，按日聚合）
- 切换 7/30/90 天时图表联动刷新
- 导出统计图片按钮（echarts export）

**前端 API（`monitor.js`）**
- 新增 `getAlertTimeSeries(days)` 函数

---

### Task 3 ⭐ 前端深色模式切换

**后端（`minimax-common`）**
- UserPreference 实体新增 `theme: String` 字段（默认 `light`）
- UserPreferenceMapper / Service / Controller
- `PATCH /api/v1/user/preferences/theme`
  - body: `{ "theme": "dark" | "light" }`

**前端**
- `stores/preferences.js`：Pinia store 管理 theme
- `App.vue`：监听 theme 动态切换 Element Plus `el-theme-dark` class
- 顶部导航右侧新增主题切换图标按钮（太阳/月亮图标）
- 主题偏好持久化到 localStorage + 后端同步

---

### Task 4 ⭐ API 认证中间件完善（JWT 续期）

**后端（`minimax-auth` / `minimax-common`）**
- `JwtAuthenticationFilter` 增强：
  - access token 剩余有效期 < 5 分钟时，在响应 header 注入 `X-Token-Refresh: true`
  - 前端检测到此 header，自动调 `/auth/refresh`
- `AuthController`：
  - `/auth/refresh` 支持 body 传 refreshToken（不只是 cookie）
  - 新增 `/auth/validate` 端点：校验 token 是否有效
  - 返回：`{ valid: boolean, userId: Long, expiresAt: Instant }`

**前端（`http.js`）**
- Axios 响应拦截器：检测到 `X-Token-Refresh` header 时静默刷新 token
- 刷新期间请求排队，刷新完成后重放

---

## 执行顺序

1. Task 4（JWT 续期）→ 基础设施，后面前端改动依赖
2. Task 3（深色模式）→ 后端实体先跑，前端 UI
3. Task 1（RAG 导出）→ 核心功能
4. Task 2（Monitor ECharts）→ 可视化

## 自检清单

- [ ] `bash /workspace/minimax-platform/scripts/self-check.sh` → 13/13 ✅
- [ ] `bash /workspace/minimax-platform/scripts/java-static-check.sh` → 0 错误
- [ ] `cd /workspace/minimax-platform/frontend && npm run build` → ✅

## 预期 commit

`feat(day-48): RAG批量导出 + Monitor ECharts可视化 + 深色模式 + JWT续期增强`
