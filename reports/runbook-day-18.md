# Day 18 Runbook — API Key 管理 + 监控真实数据 + 告警通知推送

**日期**: 2026-06-22
**目标**: V5.33 — API Key 管理前端 / 监控真实数据 / 告警邮件钉钉推送

---

## 背景

Day 17 完成 Swagger/i18n/移动端/WS 通知。Day 18 计划三个基础设施：
- API Key 管理界面（用户自主管理 API Key）
- 监控面板真实数据（替换 mock）
- 告警通知推送（邮件 + 钉钉 WebHook）

---

## 任务清单

### 1. API Key 管理前端 UI
**后端现状**: 已存在 api_key 表 + ApiKeyController（5 端点）

**前端新增**:
- [ ] `src/api/apiKey.js`: listKeys / createKey / revokeKey / rotateKey / getQuota
- [ ] `src/store/apiKey.js`: useApiKeyStore
- [ ] `src/views/apikey/Index.vue`: 
  - 我的 API Key 列表（Key/名称/权限/期限/使用量）
  - 创建 Key 弹窗（名称 + 权限范围 + 过期时间）
  - 复制 Key 值（仅创建时显示完整）
  - 撤销 / 轮换操作
  - 配额使用进度条

### 2. 监控面板真实数据接入
**现状**: 监控页面有 UI，但数据可能 mock

**操作**:
- [ ] `src/api/monitor.js`: 确认已有 `getMetrics()` / `getAlerts()` / `getHealth()`
- [ ] `src/views/monitor/Index.vue`: 替换 mock 数据为真实 API 调用
- [ ] 确保 Chart 数据绑定到 `fetchDashboard()` 返回

### 3. 告警邮件/钉钉推送
**后端新增**:
- [ ] `AlertNotifier` 接口 + `EmailAlertNotifier` 实现（JavaMail）
- [ ] `DingTalkAlertNotifier` 实现（WebHook POST）
- [ ] `AlertConfig` 实体/Mapper/Service/Controller（通知渠道配置）
- [ ] AlertEngine 触发时调用 notifier

**通知渠道配置表**:
- id / tenant_id / channel (EMAIL/DINGTALK) / config (JSON: email 或 webhook url) / enabled / created_at

---

## 验收标准

- self-check.sh ≥ 78/78
- java-static-check.sh 0 错误
- npm run build 通过
- git push 成功

---

## 预计代码量

- API Key 前端: ~200 行 Vue + ~50 行 JS
- 监控真实数据: ~50 行 Vue
- 告警推送: ~10 个 Java + 1 SQL 表
