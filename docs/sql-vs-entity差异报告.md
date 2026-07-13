# SQL 脚本 vs Java 实体类 差异报告 (V3.5.5)

**生成时间**: 2026-07-13
**比对工具**: `scripts/diff_sql_entity.py`
**SQL 范围**: `sql/init.sql` (89 表) + 16 个 `schema-h2.sql`
**Entity 范围**: 77 个 `*Entity.java`

## 📊 总览

| 指标 | 数量 |
|------|------|
| SQL 总表数 | 89 |
| Entity 总类数 | 77 |
| 两边共有 | 77 |
| **SQL 孤儿表** (有表无 entity) | **12** |
| **Entity 孤儿表** (有 entity 无表) | **0** |
| **字段缺失** (entity 有 SQL 缺) | **713 字段 / 75 张表** |

## ⚠️ SQL 孤儿表 (12 张)

以下表在 SQL 中建了，但没有对应的 Java Entity (没人用):

| 表名 | 字段数 | 可能原因 |
|------|--------|----------|
| `agent_marketplace` | 2 | 待开发 |
| `agent_rating` | 2 | 待开发 |
| `async_task` | 6 | 待开发 |
| `license_template` | 4 | 已用 (V3.5.2 在 minimax-ai, 路径可能不同) |
| `memory_long_term` | 7 | 待开发 |
| `memory_user_pref` | 5 | 待开发 |
| `model_market` | 2 | 待开发 |
| `model_rating` | 2 | 待开发 |
| `rate_limit_rule` | 1 | 待开发 |
| `request_log` | 3 | 待开发 |
| `webhook` | 2 | 已用 (V3.5.x) |
| `webhook_delivery` | 2 | 已用 (V3.5.x) |

**建议**: 
- 短期: 保留 SQL 表 (避免破坏现有查询)
- 中期: 补充对应 Entity (匹配业务需求)

## ⚠️ 字段缺失 (75 张表, 713 字段)

**根因**: Entity 类比 SQL 表"先行", 新增字段只加了 Java 类没改 DDL。

**典型案例**:
- `sys_user` 缺 27 字段 (微信/QQ/支付宝 登录字段)
- `multimedia_file` 缺 25 字段
- `training_task` 缺 14 字段
- `plugin` 缺 15 字段
- `model_battle_log` 缺 15 字段
- `cluster_node` 缺 16 字段
- `pipeline_log` 缺 17 字段
- `ai_generation_log` 缺 17 字段
- `ai_tool` 缺 18 字段
- `audit_log` 缺 14 字段

**影响**: 
- 运行时: MyBatis-Plus `select *` 会因列不存在而 SQL 异常
- 写操作: 字段为 null, 实际落库字段不全

## ✅ 修复方案

### 1. 自动生成的修复 SQL

```
sql/fix_missing.sql  (871 行)
```

包含 75 张表的 `ALTER TABLE ... ADD COLUMN` 语句, 共 713 字段。

### 2. 应用方式

**Docker 部署**:
```bash
# 容器内执行
docker exec -i minimax-mariadb mysql -uroot -proot123456 minimax_platform < sql/fix_missing.sql
```

**本地 MySQL**:
```bash
mysql -uroot -proot123456 minimax_platform < sql/fix_missing.sql
```

**V3.5.6 集成**: 把 `fix_missing.sql` 内容合并到 `init.sql`, 后续部署自动建全。

### 3. 字段类型映射

脚本根据 Java 类型推断 SQL 类型:

| Java | MySQL |
|------|-------|
| String | VARCHAR(255) |
| Long | BIGINT |
| Integer | INT |
| Boolean | TINYINT(1) |
| Double | DOUBLE |
| BigDecimal | DECIMAL(20,4) |
| LocalDateTime | DATETIME |
| LocalDate | DATE |
| JSONObject/JSONArray | TEXT |
| byte[] | BLOB |

**注**: 复杂字段 (`parameters`, `config`, `metadata` JSON 等) 实际可能应是 `TEXT`/`JSON`, 需人工 review 调整。

## 🛠️ 复测工具

```bash
# 重新跑对比
python3 scripts/diff_sql_entity.py

# 重新生成修复 SQL
python3 scripts/gen_fix_sql.py
```

## 长期改进

1. **建表统一化**: 新增 Entity 时同步 DDL, 加 CI 检查
2. **Schema 校验**: 用 Liquibase / Flyway 管理 DDL 版本
3. **Entity 必填**: 字段加 `@TableField(value = "col_name")` 显式映射
4. **字段类型优化**: 字符串太长应 `VARCHAR(1024)`/`TEXT`, JSON 用 `JSON` 类型
