# SQL 数据字典 (V3.5.8)

> 本文档详细描述 MiniMax Platform 数据库的 77 张表结构
> 生成时间: 2026-07-14
> 来源: sql/complete.sql

---

## 📊 总览

| 指标 | 数量 |
|---|---|
| **总表数** | **77** |
| **种子数据覆盖** | **53** (68.8%) |
| **种子数据条数** | **~180** |
| **模块数** | **13** |

## 🗂️ 模块分布

| 模块 | 表数 | 已覆盖 | 占比 |
|---|---|---|---|
| **auth** | 9 | 2 | 22% |
| **ai** | 14 | 8 | 57% |
| **admin** | 5 | 1 | 20% |
| **analytics** | 4 | 4 | 100% |
| **chat** | 3 | 2 | 67% |
| **monitor** | 3 | 2 | 67% |
| **agent** | 6 | 2 | 33% |
| **training** | 4 | 1 | 25% |
| **pipeline** | 5 | 2 | 40% |
| **function** | 2 | 1 | 50% |
| **multimodal** | 2 | 1 | 50% |
| **model** | 6 | 2 | 33% |
| **knowledge** | 4 | 2 | 50% |
| **collab** | 5 | 1 | 20% |
| **kg** | 2 | 2 | 100% |
| **其他** (用户/角色/租户/通知/审计) | 3 | 3 | 100% |

---

## 📋 表清单 (按模块)

### 🔐 auth 模块 (9 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `auth_login_log` | 登录日志 | user_id, ip, status, created_at | ✓ |
| `auth_refresh_token` | 刷新令牌 | user_id, token, expires_at, revoked | ✓ |
| `oauth_app_config` | OAuth 应用配置 | app_id, app_secret, redirect_uri | ✗ |
| `oauth_binding` | OAuth 绑定 | user_id, oauth_app, openid | ✗ |
| `unionid_relations` | UnionID 关系 | unionid, user_id | ✗ |
| `wechat_config` | 微信配置 | app_id, app_secret, mch_id | ✗ |
| `wechat_scan_session` | 扫码会话 | session_id, status, scanned_at | ✗ |
| `wechat_user_binding` | 微信用户绑定 | user_id, openid, unionid | ✗ |
| (其他 1 张) | | | ✗ |

### 🤖 ai 模块 (14 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `ai_chat_session` | AI 对话会话 | session_id, user_id, title | ✓ |
| `ai_chat_message` | AI 对话消息 | session_id, role, content | ✓ |
| `ai_tool_invocation` | AI 工具调用 | tool_code, input_json, output_json | ✓ |
| `ai_intent_keyword` | 意图关键词 | intent, keyword, weight | ✓ |
| `ai_generation_log` | AI 生成日志 | prompt, response, tokens | ✓ |
| `model_provider` | 模型提供商 | code, name, type, api_key | ✓ |
| `ai_tool` | AI 工具 | code, name, type, schema | ✓ |
| `pipeline_workflow` | 工作流定义 | name, definition, status | ✓ |
| (其他 6 张) | | | ✗ |

### 🛠 admin 模块 (5 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `admin_audit_log` | 管理审计 | actor_id, action, target_id | ✗ |
| `audit_log` | 审计日志 | user_id, action, resource | ✓ |
| `audit_log_full` | 完整审计 | (冗余, 保留兼容) | ✗ |
| `dashboard_metric` | 仪表盘指标 | metric, value, timestamp | ✓ |
| (其他 1 张) | | | ✗ |

### 📊 analytics 模块 (4 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `analytics_datasource` | 数据源 | name, type, jdbc_url | ✓ |
| `analytics_ingest_task` | 导入任务 | source_id, status, row_count | ✗ |
| `analytics_nlsql_history` | NL2SQL 历史 | question, generated_sql | ✓ |
| `analytics_report` | 分析报告 | name, type, format, status | ✓ |

### 💬 chat 模块 (3 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `chat_session` | 聊天会话 | user_id, model, title, status | ✓ |
| `chat_message` | 聊天消息 | session_id, role, content, tokens | ✓ |
| `function_call_log` | 函数调用日志 | user_id, tool_name, status | ✓ |

### 📈 monitor 模块 (3 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `alert_rule` | 告警规则 | name, metric, threshold | ✓ |
| `alert_channel` | 告警通道 | type, config, enabled | ✓ |
| `alert_event` | 告警事件 | rule_id, severity, status | ✓ |
| `metric_snapshot` | 指标快照 | metric, value, timestamp | ✗ |

### 🎯 agent 模块 (6 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `agent_task` | Agent 任务 | task_id, goal, status, rounds | ✓ |
| `agent_group` | Agent 群组 | name, owner_id, member_count | ✓ |
| `plugin` | 插件 | code, name, type, status | ✓ |
| `kg_entity` | 知识图谱实体 | name, type, properties | ✓ |
| `kg_relation` | 知识图谱关系 | from, to, relation_type | ✓ |
| (其他 1 张) | | | ✗ |

### 🎓 training 模块 (4 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `training_job` | 训练任务 | model_name, dataset, status | ✓ |
| `training_checkpoint` | 训练检查点 | job_id, step, metrics | ✗ |
| `training_metric` | 训练指标 | job_id, step, loss, acc | ✗ |
| `training_task` | 训练子任务 | parent_id, config | ✗ |

### ⚙️ pipeline 模块 (5 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `pipeline_workflow` | 工作流定义 | name, definition, version | ✓ |
| `pipeline_run` | 工作流运行 | workflow_id, status, duration | ✓ |
| `pipeline_node_log` | 节点日志 | run_id, node_id, status | ✓ |
| `pipeline_log` | 工作流日志 | run_id, level, message | ✗ |
| `pipeline_workflow_version` | 工作流版本 | workflow_id, version | ✗ |

### 🔧 function 模块 (2 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `function_tool` | 函数工具 | code, name, schema, status | ✓ |
| `function_call_log` | 函数调用日志 | tool_name, args, result | ✓ |

### 🎨 multimodal 模块 (2 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `multimedia_file` | 多媒体文件 | file_id, type, size, hash | ✓ |
| (其他 1 张) | | | ✗ |

### 🧠 model 模块 (6 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `model_provider` | 模型提供商 | code, name, api_key | ✓ |
| `model_config` | 模型配置 | model_code, max_context, prices | ✓ |
| `model_quota` | 模型配额 | user_id, used_tokens, limits | ✓ |
| `model_battle_log` | 模型对决日志 | prompt, models, winner | ✓ |
| `model_license` | 模型许可证 | model_id, license_type | ✗ |
| `model_version` | 模型版本 | model_id, version, status | ✗ |

### 📚 knowledge 模块 (4 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `knowledge_base` | 知识库 | name, description, type | ✓ |
| `kb_document` | 知识库文档 | kb_id, filename, sha256 | ✓ |
| `kb_chunk` | 知识库块 | doc_id, content, embedding | ✓ |
| `kb_permission` | 知识库权限 | kb_id, user_id, permission | ✗ |

### 🤝 collab 模块 (5 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `collab_room` | 协作房间 | roomId, name, ownerId | ✓ |
| `collab_session` | 协作会话 | room_id, user_id, status | ✗ |
| `collab_member` | 协作成员 | session_id, user_id, role | ✗ |
| `collab_message` | 协作消息 | session_id, content | ✗ |
| `collab_participant` | 协作者 | room_id, user_id, status | ✗ |

### 🧩 kg 模块 (2 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `kg_entity` | 知识图谱实体 | name, type, properties | ✓ |
| `kg_relation` | 知识图谱关系 | from_entity, to_entity, type | ✓ |

### 📢 通知/通知中心 (3 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `notification` | 通知 | user_id, type, title, read | ✓ |
| `push_message` | 推送消息 | message_id, title, body, target | ✓ |
| `push_subscription` | 推送订阅 | user_id, endpoint, keys | ✗ |

### 👥 用户/角色/租户 (3 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `sys_user` | 用户 | username, password, email | ✓ |
| `sys_role` | 角色 | name, code, permissions | ✓ |
| `sys_user_role` | 用户-角色 | user_id, role_id | ✓ |

### 🏢 租户 (1 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `tenant` | 租户 | name, code, status | ✓ |

### 💰 计费 (1 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `billing_record` | 计费记录 | user_id, tokens, cost | ✓ |

### 🔍 审核 (1 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `moderation_record` | 审核记录 | trace_id, content, risk_level | ✓ |

### 🔐 集群/许可证 (2 张表)

| 表名 | 说明 | 关键字段 | 已覆盖 |
|---|---|---|---|
| `cluster_node` | 集群节点 | node_id, status, role | ✓ |
| `raft_log` | Raft 日志 | term, index, entry | ✗ |

---

## 📊 字段命名规范

所有表字段遵循统一规范:

```
主键:     id BIGINT AUTO_INCREMENT
外键:     xxx_id BIGINT (e.g. user_id, role_id)
布尔:     xxx_enabled / is_xxx / xxx (TINYINT 0/1)
时间:     created_at / updated_at / deleted_at
逻辑删除: deleted TINYINT DEFAULT 0
状态:     status INT/VARCHAR (0=正常, 1=禁用, 2=已删)
```

## 🔑 4 个测试账号

| 用户名 | 密码 | 角色 | BCrypt 哈希 |
|---|---|---|---|
| `adminLiugl` | `Liugl@2026` | super_admin | $2a$10$fyhH... |
| `admin_user` | `admin123` | admin | $2a$10$VqCU... |
| `test_user` | `user123` | user | $2a$10$AgpC... |
| `demo_user` | `demo1234` | user | $2a$10$/CTA... |

## 📦 文件清单

```
sql/
├── README.md               # 4 KB - 快速说明
├── complete.sql            # 89 KB - MySQL DDL (77 表)
├── complete-h2.sql         # 89 KB - H2 DDL (77 表, MODE=MySQL)
└── seed-data.sql           # 25 KB - 种子数据 (53 表, 178+ 条)
```

## 🚀 用法

### 全新部署

```bash
mysql -uroot -proot123456 < sql/complete.sql
mysql -uroot -proot123456 minimax_platform < sql/seed-data.sql
```

### H2 沙箱

```bash
# 自动加载 (Spring Boot 配置 spring.sql.init.data-locations)
# 文件: classpath:seed-data.sql
```

### 验证

```bash
mysql -uroot -proot123456 minimax_platform -e "SHOW TABLES" | wc -l
# 输出: 78 (含 77 表 + 1 行 header)

mysql -uroot -proot123456 minimax_platform -e "
  SELECT table_name, table_rows
  FROM information_schema.tables
  WHERE table_schema='minimax_platform' AND table_rows > 0
  ORDER BY table_rows DESC LIMIT 20
"
```
