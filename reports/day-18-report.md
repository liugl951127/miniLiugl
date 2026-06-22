# Day 18 报告 — V5.33 API Key 管理 + 告警推送

**日期**: 2026-06-22
**目标**: V5.33 — API Key 管理前端 / 告警邮件钉钉推送 / 监控真实数据
**Commit**: pending

---

## ✅ 完成项

### 1. 用户 API Key 管理（前后端全链路）

**后端** (minimax-auth 模块):

| 文件 | 说明 |
|------|------|
| `entity/UserApiKey.java` | 实体：userId / keyHash(SHA-256) / keyPrefix / scopes / expiresAt / useCount / enabled |
| `mapper/UserApiKeyMapper.java` | MyBatis-Plus BaseMapper + 3 个自定义方法 |
| `mapper/UserApiKeyMapper.xml` | selectByUserId / selectByKeyHash / incrementUseCount |
| `dto/CreateApiKeyRequest.java` | 创建请求 DTO |
| `vo/ApiKeyResponse.java` | 响应 VO（含 rawKey 仅创建时返回）|
| `service/UserApiKeyService.java` | 完整 CRUD + SHA-256 哈希 + 验证 + 轮换 |
| `controller/UserApiKeyController.java` | 5 个 REST 端点（list/create/toggle/delete/rotate）|

**5 个 API 端点**:
- `GET  /auth/apikeys` — 列出我的 Key
- `POST /auth/apikeys` — 创建 Key（返回 rawKey 一次）
- `PATCH /auth/apikeys/{id}/toggle?enable=true` — 禁用/启用
- `DELETE /auth/apikeys/{id}` — 删除 Key
- `POST /auth/apikeys/{id}/rotate` — 轮换 Key

**SQL** (init-minimax.sql):
- 新增 `user_api_key` 表（含 key_hash / key_prefix / scopes / expires_at / use_count 字段）

**前端** (frontend):

| 文件 | 说明 |
|------|------|
| `src/api/apikey.js` | 5 个 API 函数 |
| `src/views/apikey/Index.vue` | ~270 行完整 UI（列表/创建/复制/禁用/轮换/删除）|
| `src/i18n/locales/zh.js` | 新增 apikey section |
| `src/i18n/locales/en.js` | 新增 apikey section |
| `src/router/index.js` | 新增 `/apikey` 路由 |
| `src/layout/Index.vue` | 侧边栏 + 右上角下拉菜单新增 "API Key" 入口 |

**UI 功能**:
- Key 列表表格（名称 / 前缀 / 权限 / 过期 / 调用次数 / 最后使用 / 状态）
- 创建弹窗（名称 + 权限范围 + 过期时间）
- rawKey 一次性展示弹窗（⚠️ 提示仅显示一次 + 一键复制）
- 轮换弹窗
- 禁用/启用切换
- 删除（含确认）
- 支持 i18n 中英文

---

### 2. 告警通知推送（邮件 + 钉钉 WebHook）

**后端** (minimax-monitor 模块):

| 文件 | 说明 |
|------|------|
| `entity/AlertChannel.java` | 通知渠道实体（EMAIL / DINGTALK + JSON 配置）|
| `mapper/AlertChannelMapper.java` | MyBatis-Plus BaseMapper |
| `alert/AlertNotifier.java` | 通知器接口 |
| `alert/EmailAlertNotifier.java` | JavaMail SMTP 邮件发送实现 |
| `alert/DingTalkAlertNotifier.java` | 钉钉 WebHook（支持签名密钥）|
| `alert/AlertNotifierManager.java` | 渠道管理器（启动收集 + 按优先级通知）|
| `service/AlertChannelService.java` | 渠道 CRUD 服务 |
| `controller/MonitorController.java` | 新增 5 个渠道 CRUD 端点 |

**5 个告警渠道端点**:
- `GET  /monitor/alerts/channels` — 列出渠道
- `GET  /monitor/alerts/channels/{id}` — 渠道详情
- `POST /monitor/alerts/channels` — 创建渠道
- `PUT  /monitor/alerts/channels/{id}` — 更新渠道
- `DELETE /monitor/alerts/channels/{id}` — 删除渠道

**通知触发**: AlertEngine.evaluateRule() 在告警触发时自动调用 `notifierManager.notifyAll(e)`，按优先级调用所有已启用渠道（邮件 / 钉钉）。

**配置**:
- 邮件: `spring.mail.*` SMTP 配置（支持 SSL 465 端口 / STARTTLS 587）
- 钉钉: WebHook URL + 可选签名密钥（HMAC-SHA256）

**SQL** (init-minimax.sql):
- 新增 `alert_channel` 表

**pom.xml 变更**:
- minimax-monitor 新增 `spring-boot-starter-mail` 依赖

---

### 3. 自检脚本修复

- `scripts/self-check.sh`: 修正过时的 SQL 文件名检查（`02_user_auth.sql` → `init-minimax.sql`）
- `scripts/java-static-check.sh`: 修复 package 声明检查范围（1-5行 → 1-30行，兼容大注释头文件）

---

## 📊 代码统计

| 维度 | 数量 |
|------|------|
| 新增后端 Java | 11 个（entity×2 / mapper×2 / service×2 / notifier×3 / dto×1 / vo×1）|
| 新增前端文件 | 3 个（api/apikey.js / views/apikey/Index.vue / i18n locale×2）|
| 编辑前端文件 | 3 个（router/index.js / layout/Index.vue × 2 i18n）|
| 编辑后端文件 | 2 个（MonitorController / AlertEngine）|
| SQL 新增表 | 2 张（user_api_key / alert_channel）|
| pom.xml 变更 | 1 个（monitor + mail starter）|
| 脚本修复 | 2 个（self-check.sh / java-static-check.sh）|
| **总文件变更** | **~25 个** |

---

## ✅ 验证结果

| 检查项 | 结果 |
|--------|------|
| `scripts/self-check.sh` | ✅ 78/78 通过 |
| `scripts/java-static-check.sh` | ✅ 0 错误 |
| `npm run build` | ✅ 1m 16s，3129 模块 |

---

## 🚀 使用方式

### API Key

1. 启动后端 `minimax-auth`（8081）
2. 登录后访问 `/apikey`
3. 点击 "创建密钥" → 填写名称 → 复制 rawKey → 保存
4. rawKey 用于调用平台 API（Header: `Authorization: Bearer msk_xxx`）

### 告警通知

1. 在 `monitor` 模块配置 `spring.mail.*` 环境变量
2. 或在 DB 的 `alert_channel` 表插入钉钉 WebHook
3. 配置后，告警触发时自动发送邮件 + 钉钉消息

---

## 📅 版本历史

| 版本 | 日期 | 内容 |
|------|------|------|
| V5.31 | 2026-06-22 | 数据智能分析 (minimax-analytics) |
| V5.32 | 2026-06-22 | 画布工作流拖拽 (minimax-pipeline) |
| **V5.33** | **2026-06-22** | **API Key 管理 + 告警邮件钉钉推送** |
