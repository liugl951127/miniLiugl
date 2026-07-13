# SQL 脚本目录 (V3.5.5+ 最终版)

## 📄 唯一 SQL 文件: `complete.sql`

- **大小**: ~87KB
- **表数**: 77 张
- **字段数**: 1020 个
- **行数**: ~1500 行 (含 59 条种子数据 INSERT)
- **生成方式**: 自动扫描 77 个 Java Entity, 100% 对齐
- **驼峰规则**: SQL 列名跟 Java 字段一致 (camelCase)

## 🎯 特点

- ✅ **100% 覆盖 Entity**: 跟 Java `*Entity.java` 字段一一对应
- ✅ **驼峰一致**: `userId` ↔ `userId`, `createdAt` ↔ `createdAt`
- ✅ **中文注释**: 每个字段带 `COMMENT '主键ID(id)'` 等
- ✅ **唯一基线**: V3.0.0 的 `init.sql` 已废弃, 增量 `fix_*.sql` 已合并

## 🚀 使用

### Docker 部署 (mariadb 自动跑)

```yaml
# docker-compose.yml
volumes:
  - ./sql/complete.sql:/docker-entrypoint-initdb.d/01-init.sql:ro
```

### 手动导入

```bash
mysql -uroot -proot123456 < sql/complete.sql
```

### 重新生成

```bash
python3 scripts/gen_complete_sql.py
```

## 📊 旧版对比

| 文件 | 来源 | 状态 |
|------|------|------|
| ~~init.sql~~ | V3.0.0 旧基线 (89 表) | ❌ 已废弃 |
| ~~fix_missing.sql~~ | init.sql 增量修复 | ❌ 已废弃 |
| ~~fix_by_module/*.sql~~ | 按模块拆分 | ❌ 已废弃 |
| ~~backend/*/schema-h2.sql~~ | H2 沙箱 schema | ❌ 已废弃 |
| **`complete.sql`** | V3.5.5 扫描 entity | ✅ **唯一基线** |

## 🔧 工具脚本

- `scripts/gen_complete_sql.py` - 重新生成 SQL
- `scripts/diff_sql_entity.py` - SQL vs Entity 字段比对
- `scripts/check_camelcase.py` - 驼峰规则核对
