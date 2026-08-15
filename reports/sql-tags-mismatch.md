# SQL tags 列错修复方案 (V6.8.1+)

## 现象
`ERROR 1054 (42S22) at line 2156: Unknown column 'tags' in 'INSERT INTO'`

## 根因
你本地 `sql/minimax-mysql-final.sql` 是 V6.7+ 版本 (2249 行), 但用了 V6.8.1 的种子。

V6.7+ `sys_user` 表有 `tags` 列 (V3.5.46 旧字段)。
V6.8.1 `sys_user` 表**没有** `tags` 列 (Entity 解析时已剔除)。

`line 2156` 是 V6.7+ 旧 sql 的 `INSERT IGNORE INTO sys_user (..., tags, ...)`, 但 V6.8.1 schema 没 `tags` 列。

## 3 个解决方案

### 方案 A: 用 V6.8.1 完整 sql (推荐)
```bash
git pull origin main
mysql -u root -p < sql/minimax-mysql-final.sql
mysql -u root -p < sql/minimax-seed-data.sql
```
我 V6.8.1 sql 1854 行 (含 78 表 + 4 函数 + 4 视图 + 2 触发器), 沙箱 MariaDB 10.11 验证 0 错。

### 方案 B: 给 V6.8.1 schema 加 tags 列 (兼容旧 seed)
如果你有 V6.7+ 的种子数据想保留, 让我加 `tags` 列到 `sys_user` 等表。

### 方案 C: 我给你加 tags 列回去 + 你的旧 seed 也能跑
1. 我加 `tags VARCHAR(255) DEFAULT NULL` 到 sys_user 等
2. 重生成 schema
3. 你的旧 V6.7+ seed 也能跑

## 我建议: 方案 A (直接拉最新)

```bash
# 1. 备份你本地
cp sql/minimax-mysql-final.sql /tmp/old-final.sql

# 2. 拉最新
git pull origin main

# 3. 用新 sql 跑
mysql -u root -p < sql/minimax-mysql-final.sql
mysql -u root -p < sql/minimax-seed-data.sql
```

如果用 docker:
```bash
docker compose down
docker compose up -d
```

## 如果你一定要保留 tags 列 (业务需要)

让我加 tags 列回去, 重跑:
```bash
# 加 tags 列到 sys_user
ALTER TABLE sys_user ADD COLUMN tags VARCHAR(255) DEFAULT NULL AFTER status;
```

我可以加更多表, 看你要哪些。
