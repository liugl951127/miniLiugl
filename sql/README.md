# MiniMax Platform V6.8.1 MySQL 建表脚本

**单文件一站式部署** - 自动从实体类生成，与 Java 实体严格对齐。

## 文件

| 文件 | 行 | 用途 |
|------|------|------|
| `minimax-v681-schema.sql` | 1900+ | DDL (80 表) + 种子数据 |
| `gen_sql.py` | - | 自动生成脚本 |
| `README.md` | - | 本说明 |

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

## 部署

```bash
# 全新部署 (1 步)
mysql -uroot -p minimax_platform < minimax-v681-schema.sql
```

## 验证

```sql
-- 1. 表数
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'minimax_platform';
-- 期望: 80

-- 2. 测试账号
SELECT id, username, role, status FROM sys_user;
-- 期望: 3 个 (admin/user01/operator)

-- 3. 角色
SELECT * FROM sys_role;
-- 期望: 3 个 (SUPER_ADMIN/USER/OPERATOR)
```

## 重新生成

```bash
# 从实体类重新生成（实体变更后）
python3 gen_sql.py
```
