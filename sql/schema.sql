-- ============================================================
-- MiniMax Platform V2.8.2 数据库初始化入口
-- ============================================================
-- 用法:
--   1. 创建数据库: CREATE DATABASE minimax DEFAULT CHARSET utf8mb4;
--   2. 导入 DDL:     source /opt/minimax/sql/schema-v2.8.2.sql;
--   3. 导入种子:     source /opt/minimax/sql/seed-v2.8.2.sql;
--
-- 包含:
--   - schema-v2.8.2.sql: 60+ 表结构 (从 Java 实体自动生成)
--   - seed-v2.8.2.sql:    默认数据 (用户/角色/工具/规则/敏感词)
--
-- 重置数据库: DROP DATABASE minimax; CREATE DATABASE minimax;
-- ============================================================

SOURCE /opt/minimax/sql/schema-v2.8.2.sql;
SOURCE /opt/minimax/sql/seed-v2.8.2.sql;

-- 确认
SELECT 'MiniMax V2.8.2 初始化完成' AS status;
SHOW TABLES;
