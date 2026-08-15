# SQL 重生成 + 沙箱验证 (V6.8.1+)

## 沙箱安装
```bash
apt-get install -y mariadb-server mariadb-client
mariadb-install-db --user=mysql --datadir=/var/lib/mysql
mariadbd --user=mysql --port=3306 &
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root123456';
```

## 最终交付

### 1. `sql/minimax-mysql-final.sql` (1774 行)
- **78 CREATE TABLE** (77 业务表 + 1 schema_version)
- **4 函数** (token 成本/意图匹配/活跃天数/配额百分比)
- **4 视图** (用户画像/聊天统计/租户使用/工具统计)
- **2 触发器** (用户审计/租户配额日志)
- **1028 字段** (从 77 Entity 扫出)
- **业务注释** (从 Javadoc 提取, 无注释用字段名 fallback)
- **MariaDB 10.11 验证通过**

### 2. `sql/minimax-seed-data.sql` (180+ 行)
- **11 模块全覆盖**: auth/ai/agent/analytics/chat/model/monitor/pipeline/rag/admin/ws
- **5 测试账号**: admin/liugl/guest/dev/tester (密码: minimax123)
- **5 角色 + 5 角色绑定**
- **3 租户**
- **15 AI 意图关键词**
- **5 AI 工具**
- **5 Agent 分组**
- **3 KG 实体 + 2 关系**
- **2 数据源**
- **3 模型提供商 + 4 模型配置**
- **3 提示词模板**
- **3 通知**
- **2 审计日志**
- 等等

## 真错修复过程 (沙箱跑出)

### Schema 阶段
1. `DECIMAL((20,4))` 嵌套括号 → 修生成器 size
2. `sys_user.id` 缺失 → 修 parser, 只在 class 体内找字段
3. 视图 `v_chat_session_stats` 引用不存在的 `s.status` → 改用 `s.intent`
4. 视图引用不存在的 `m.input_tokens` → 改用 CHAR_LENGTH
5. 视图 `v_active_tools` 引用不存在的 `i.success_count` → 改用 CASE WHEN
6. 视图 JOIN 字段 `i.tool_id` 不存在 → 改用 `i.tool_code = t.code`
7. 视图子查询 user 引用冲突 → 加 alias

### Seed 阶段
1. 缺 `USE minimax_platform` → 加
2. `sys_role.sort_order` 改 `sort`, `status` 改 `enabled`
3. `ai_intent_keyword.weight` 是 INT 不是 DOUBLE, 补 8 个列
4. `agent_group` 改用 `group_id/strategy/members_json` 等实际列
5. `ai_chat_session` 11 列不只 5 列
6. `plugin` 19 列只传 7 列
7. `kg_entity/relation` 12 列只传 5 列
8. `data_source` 20 列用实际 jdbc_url 字段
9. `model_config` 16 列用 model_code/max_context 等
10. `prompt_template` 13 列用 description/variables 等
11. `pipeline_workflow` 用 definition/create_time 等
12. `knowledge_base` 用 owner_id/tenant_id/visibility 等
13. `notification.status` 改 `is_read`
14. 视图子查询别名冲突

## 验证结果
```
=== Tables ===       78
=== Views ===         4
=== Functions ===     4
=== Triggers ===      2
=== Test Functions ===
  fn_calculate_token_cost(1000, 500, 'gpt-4') = 0.06
  fn_tenant_usage_percent(1) = 0.00
=== Test Views ===
  v_user_profile (3 rows) - OK
=== Test Trigger ===
  sys_user deleted=1 → admin_audit_log +1 row - OK
```

## 关键经验

1. **沙箱装 mariadb 真跑才能发现真错** - 静态检查 (8 类常见错) 不够
2. **Entity parser 必须限制在 class 体内** - @TableName/@Data 等类注解会被吞
3. **视图子查询必须用 alias** - 否则子表和主表 user/role 冲突
4. **INSERT 列数必须严格匹配** - 即使 INSERT IGNORE 也会 1136 错
5. **BCrypt 占位 hash** - 真实部署用后端启动重置
6. **触发器是审计日志好办法** - 业务表无侵入审计
7. **视图里不要用不存在的字段** - 子查询用 EXISTS 比 COUNT 快
8. **MariaDB 10.11 strict mode 默认严格** - 跟 MySQL 8 行为接近
