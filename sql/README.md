# MiniMax Platform V6.8 SQL Schema

**全面重构版** — 80 表 / 全字段中文注释 / 表级 COMMENT / 驼峰命名。

## 文件

| 文件 | 行数 | 说明 |
|------|------|------|
| `minimax-v681-schema-comments.sql` | ~1977 | **推荐使用** DDL + 字段 COMMENT + 表 COMMENT + 种子数据 |
| `minimax-v681-schema.sql` | ~1963 | 原始无注释版（保留对照） |
| `minimax-mysql-final.sql` | ~1705 | 历史版本（仅供参考） |
| `gen_sql.py` | - | 自动生成脚本 |
| `sql_refactor.py` | - | 注释增强脚本 |

## 规范

- **字段命名**: 全小写 + 下划线（驼峰已全部规范化）
- **表级 COMMENT**: `COMMENT='中文表名'`
- **字段 COMMENT**: `COMMENT '中文含义'`
- **id 主键**: `COMMENT '主键ID - {中文表名}'`
- **字符集**: utf8mb4 / COLLATE utf8mb4_unicode_ci / InnoDB

## 模块覆盖

11 个模块，80 张表：

| 模块 | 包路径 | 表数 |
|------|--------|------|
| auth | minimax-auth | 12 |
| ai | minimax-ai | 19 |
| model | minimax-model | 6 |
| agent | minimax-agent | 6 |
| monitor | minimax-monitor | 4 |
| pipeline | minimax-pipeline | 5 |
| rag | minimax-rag | 3 |
| analytics | minimax-analytics | 4 |
| admin | minimax-admin | 2 |
| chat | minimax-chat | 2 |
| ws | minimax-ws | 3 |

## 表清单

| 表名 | 中文名 | 核心字段 |
|------|--------|----------|
| sys_user | 系统用户表 | username, email, phone, wechat_*, qq_*, tenant_id |
| sys_role | 系统角色表 | code, name, description |
| sys_user_role | 用户角色关联表 | user_id, role_id (复合主键) |
| auth_login_log | 登录日志表 | user_id, username, ip, status |
| auth_refresh_token | 刷新令牌表 | user_id, token, expires_at |
| notification | 系统通知表 | user_id, type, title, is_read |
| oauth_app_config | OAuth应用配置表 | platform, app_id, app_secret |
| oauth_binding | OAuth第三方绑定表 | user_id, platform, openid, unionid |
| agent_group | 智能体群组表 | name, strategy, members_json |
| ai_tool | AI工具定义表 | code, name, input_schema, output_schema |
| ai_chat_session | AI对话会话表 | session_id, intent, model |
| training_job | 训练任务表 | task_id, model, status, config |
| model_provider | 模型供应商表 | code, base_url, protocol |
| model_config | 模型配置表 | model_code, max_context, input_price |
| pipeline_workflow | 流水线工作流表 | name, definition, version |
| kb_document | 知识库文档表 | doc_id, kb_id, status, chunk_count |
| kg_entity | 知识图谱实体表 | name, entity_type, importance |
| kg_relation | 知识图谱关系表 | from_entity, to_entity, relation_type |
| alert_rule | 告警规则表 | metric_name, threshold, severity |
| collab_room | 协作房间表 | room_id, owner_id, max_participants |

## 部署

```bash
# 全新部署
mysql -uroot -p minimax_platform < minimax-v681-schema-comments.sql
```

## 验证

```sql
-- 1. 表数 & 总 COMMENT 数
SELECT COUNT(*) AS total_tables FROM information_schema.tables
WHERE table_schema = 'minimax_platform';
-- 期望: 80

-- 2. 字段 COMMENT 覆盖率
SELECT
  TABLE_NAME,
  COUNT(*) AS field_count,
  SUM(COLUMN_COMMENT != '') AS commented
FROM information_schema.columns
WHERE table_schema = 'minimax_platform'
GROUP BY TABLE_NAME
ORDER BY commented / field_count;
-- 期望: 所有表 100% 覆盖

-- 3. 测试账号
SELECT id, username, role, status FROM sys_user;
-- 期望: admin / user01 / operator

-- 4. 角色
SELECT * FROM sys_role;
-- 期望: SUPER_ADMIN / USER / OPERATOR
```
