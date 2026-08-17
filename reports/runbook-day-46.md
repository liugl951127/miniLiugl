# Day 46 Runbook — 2026-08-17

## 今日任务

### Task 1 RAG 多文档批量编辑 + 批量重新索引
- **目标**: 支持一次选中多个文档，批量修改内容 + 批量重新切片索引
- **后端**: `BatchReindexService` 或在 `DocumentService` 新增 `batchUpdateContent(Map<Long, String>)`；新增 `POST /api/v1/rag/doc/batch/reindex` 端点；异步处理大文档
- **前端**: `knowledge/Index.vue` 批量勾选 + 批量编辑弹窗 + 批量重新索引按钮；进度条展示
- **文件**: backend/rag-service/..., frontend/views/knowledge/Index.vue, frontend/api/rag.js

### Task 2 Monitor 告警自动恢复（auto-resolve）定时任务完善
- **目标**: AlertEngine 中 `autoResolve()` 完善：条件满足时自动将 firing 告警标记为 resolved
- **后端**: `AlertEngine` 新增 `checkAutoResolve()`：遍历 firing 告警，条件满足超过 `autoResolveMinutes` → 自动恢复；记录 `resolvedAt` / `resolvedBy=SYSTEM`
- **前端**: `monitor/Index.vue` 告警规则编辑弹窗已有 `autoResolveMinutes` 字段，确保与后端一致
- **文件**: backend/monitor-service/..., frontend/views/monitor/Index.vue

### Task 3 前端图片懒加载优化
- **目标**: 对用户头像、文档缩略图、Kb 封面图做 `loading="lazy"` 懒加载，减少首屏体积
- **前端**: 扫描 `<img` 标签，补全 `loading="lazy"`；对大图加 `v-lazy` 或自定义指令
- **文件**: frontend/views/**/*.vue

### Task 4 V4.4 Release 打包 + CHANGELOG 更新
- **目标**: 生成 V4.4 Release，打标签，更新 CHANGELOG.md
- **操作**: 更新版本号 → 生成 CHANGELOG → mvn clean package → git tag v4.4 → commit

## 执行顺序
Task 1 (RAG 批量编辑) -> Task 2 (Monitor auto-resolve) -> Task 3 (图片懒加载) -> 自检 -> Task 4 (Release) -> 自检 -> push
