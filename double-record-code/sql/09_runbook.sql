-- ============================================================
-- 双录一体化平台 - 完整初始化运行手册
-- 执行顺序: 01 -> 02 -> 03 -> 04 -> 06 -> 07 -> 08(自动验证)
-- ============================================================

-- 1. 建库
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS dual_record DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dual_record;

-- 2. 建表(8 张核心)
SOURCE /path/to/sql/01_schema.sql;

-- 3. 索引(单列)
SOURCE /path/to/sql/02_indexes.sql;

-- 4. 种子数据(网点/用户/角色/产品/话术)
SOURCE /path/to/sql/03_init_data.sql;

-- 5. 测试数据(客户/订单/会话)
SOURCE /path/to/sql/04_test_data.sql;

-- 6. 扩展表(审计/事件/节点结果/公钥/用户/角色/网点/异常)
SOURCE /path/to/sql/06_audit_log.sql;

-- 7. 外键 + 复合索引 + 物化视图
SOURCE /path/to/sql/07_foreign_keys.sql;

-- 8. 自动验证
SOURCE /path/to/sql/08_verify.sql;

SELECT '== 全部脚本执行完毕 ==' AS done;
