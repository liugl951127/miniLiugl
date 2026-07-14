# SQL 脚本目录 (V3.5.8)

> MiniMax Platform 数据库脚本, 含 DDL + DML + 字典

## 📁 文件清单

| 文件 | 大小 | 说明 |
|---|---|---|
| `complete.sql` | 89 KB | **MySQL** DDL, 77 张表 (生产) |
| `complete-h2.sql` | 89 KB | **H2** DDL, 77 张表 (沙箱, MODE=MySQL) |
| `seed-data.sql` | 27 KB | 种子数据, **53 表 178+ 条** (H2 自动加载) |

## 🚀 快速使用

### 全新部署 (MySQL)

```bash
# 1. 创建数据库 + 77 张表
mysql -uroot -proot123456 < sql/complete.sql

# 2. 加载种子数据 (53 表 178+ 条)
mysql -uroot -proot123456 minimax_platform < sql/seed-data.sql
```

### H2 沙箱 (自动)

```yaml
# application-h2local.yml
spring:
  sql:
    init:
      mode: always
      schema-locations: classpath:complete-h2.sql
      data-locations: classpath:seed-data.sql
      encoding: UTF-8
      continue-on-error: false
```

## 📊 种子数据分布

| 模块 | 表数 | 已覆盖 | 占比 |
|---|---|---|---|
| **auth** | 9 | 2 | 22% |
| **ai** | 14 | 8 | 57% |
| **admin** | 5 | 1 | 20% |
| **analytics** | 4 | 4 | **100%** |
| **chat** | 3 | 2 | 67% |
| **monitor** | 3 | 3 | **100%** |
| **agent** | 6 | 2 | 33% |
| **training** | 4 | 1 | 25% |
| **pipeline** | 5 | 2 | 40% |
| **function** | 2 | 2 | **100%** |
| **multimodal** | 2 | 1 | 50% |
| **model** | 6 | 4 | 67% |
| **knowledge** | 4 | 3 | 75% |
| **collab** | 5 | 1 | 20% |
| **kg** | 2 | 2 | **100%** |
| **other** (用户/角色/租户/通知/计费/审核) | 6 | 6 | **100%** |
| **总计** | **77** | **53** | **68.8%** |

## 🔑 4 个测试账号 (BCrypt 加密)

| 用户名 | 密码 | 角色 | 租户 |
|---|---|---|---|
| `adminLiugl` | `Liugl@2026` | super_admin | default |
| `admin_user` | `admin123` | admin | default |
| `test_user` | `user123` | user | default |
| `demo_user` | `demo1234` | user | demo |

## 🛠️ 维护工具

| 工具 | 说明 |
|---|---|
| `scripts/verify-seed-data.py` | 字段一致性检查 |
| `scripts/sync-seed-data.sh` | (已废弃, 用单文件) |

## 📚 文档

- **数据字典**: [docs/SQL_DICTIONARY.md](../docs/SQL_DICTIONARY.md) (77 表详细字段说明)
- **审计报告**: [docs/AUDIT_REPORT.md](../docs/AUDIT_REPORT.md)
- **意图算法**: [docs/INTENT_ALGORITHM.md](../docs/INTENT_ALGORITHM.md)

## 🔄 版本演进

| 版本 | 关键能力 |
|---|---|
| V3.5.5 | complete.sql 自动生成 (Entity → DDL) |
| V3.5.6 | seed-data 23 表 128 条 |
| V3.5.7 | 种子数据单文件 (sql/seed-data.sql) |
| **V3.5.8** | **种子数据扩展到 53 表 178+ 条, 加 SQL 字典** |

## ⚠️ 字符编码

- 文件编码: **UTF-8**
- 启动参数: `-Dfile.encoding=UTF-8`
- MySQL: `SET NAMES utf8mb4`
- H2: `MODE=MySQL` 自动处理
