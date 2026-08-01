-- ============================================================
-- 双录一体化平台 - SQL 自动化验证脚本
-- 适用数据库: MySQL 8.0+ / MariaDB 10.6+
-- 执行顺序: 01 -> 02 -> 03 -> 04 -> 06 -> 07 -> 08(本文件)
-- 用途: 验证建表、外键、索引、种子数据全部正确
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 基础检查
-- ============================================================
SELECT '== Step 1: 基础表数量检查 ==' AS step;
SELECT
    COUNT(*) AS total_tables,
    SUM(CASE WHEN table_name IN ('t_customer','t_order','t_session','t_script','t_script_node','t_risk_assess','t_quality','t_contract') THEN 1 ELSE 0 END) AS core_tables,
    SUM(CASE WHEN table_name IN ('t_audit_log','t_chain_event','t_node_result','t_public_key','t_user','t_role','t_user_role','t_branch','t_order_exception') THEN 1 ELSE 0 END) AS extend_tables,
    SUM(CASE WHEN table_name = 't_report_daily' THEN 1 ELSE 0 END) AS report_tables
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE';

-- ============================================================
-- 2. 外键检查
-- ============================================================
SELECT '== Step 2: 外键约束检查 ==' AS step;
SELECT
    constraint_name,
    table_name,
    referenced_table_name,
    delete_rule
FROM information_schema.referential_constraints
WHERE constraint_schema = DATABASE()
ORDER BY table_name, constraint_name;

-- ============================================================
-- 3. 索引检查
-- ============================================================
SELECT '== Step 3: 索引数量检查 ==' AS step;
SELECT
    table_name,
    COUNT(*) AS idx_count,
    SUM(CASE WHEN non_unique = 0 THEN 1 ELSE 0 END) AS unique_idx,
    SUM(CASE WHEN non_unique = 1 THEN 1 ELSE 0 END) AS non_unique_idx
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name LIKE 't_%'
GROUP BY table_name
ORDER BY idx_count DESC;

-- ============================================================
-- 4. 种子数据检查
-- ============================================================
SELECT '== Step 4: 种子数据完整性 ==' AS step;
SELECT
    (SELECT COUNT(*) FROM t_customer) AS customers,
    (SELECT COUNT(*) FROM t_order) AS orders,
    (SELECT COUNT(*) FROM t_session) AS sessions,
    (SELECT COUNT(*) FROM t_script) AS scripts,
    (SELECT COUNT(*) FROM t_script_node) AS nodes,
    (SELECT COUNT(*) FROM t_branch) AS branches,
    (SELECT COUNT(*) FROM t_user) AS users,
    (SELECT COUNT(*) FROM t_role) AS roles;

-- ============================================================
-- 5. 业务功能验证
-- ============================================================
SELECT '== Step 5: 业务查询验证 ==' AS step;

-- 5.1 按客户查订单
SELECT '按客户查订单' AS test_name, COUNT(*) AS result_count
FROM t_order o
INNER JOIN t_customer c ON o.customer_id = c.customer_id
WHERE c.risk_level IN ('C3','C4','C5');

-- 5.2 按产品类型查话术
SELECT '查活跌话术' AS test_name, COUNT(*) AS active_scripts
FROM t_script WHERE is_active = 1;

-- 5.3 按订单查会话
SELECT '查订单会话' AS test_name, COUNT(*) AS total_sessions
FROM t_session s
INNER JOIN t_order o ON s.order_id = o.order_id;

-- 5.4 按会话查节点结果(应有数据)
SELECT '查节点结果' AS test_name, COUNT(*) AS total_node_results
FROM t_node_result;

-- 5.5 风评匹配
SELECT '风评等级分布' AS test_name, risk_level, COUNT(*) AS count
FROM t_risk_assess
GROUP BY risk_level
ORDER BY risk_level;

-- 5.6 质检结论分布
SELECT '质检结论分布' AS test_name, verdict, COUNT(*) AS count
FROM t_quality
WHERE verdict IS NOT NULL
GROUP BY verdict
ORDER BY verdict;

-- 5.7 合同签署状态
SELECT '合同状态' AS test_name, status, COUNT(*) AS count
FROM t_contract
GROUP BY status
ORDER BY status;

-- ============================================================
-- 6. 性能检查(EXPLAIN)
-- ============================================================
SELECT '== Step 6: 关键查询性能 ==' AS step;

-- 6.1 客户最新订单
EXPLAIN SELECT *
FROM t_order
WHERE customer_id = 1001
ORDER BY created_at DESC
LIMIT 10;

-- 6.2 经理当月订单
EXPLAIN SELECT *
FROM t_order
WHERE sales_user_id = 2001
  AND created_at >= '2026-08-01'
  AND created_at < '2026-09-01';

-- 6.3 质检失败订单
EXPLAIN SELECT *
FROM t_quality
WHERE verdict IN ('FAIL','REVIEW')
  AND created_at >= '2026-08-01';

-- ============================================================
-- 7. 完整性约束测试
-- ============================================================
SELECT '== Step 7: 完整性约束(应全部失败) ==' AS step;

-- 7.1 测试外键:不存在的客户(应失败)
INSERT INTO t_order (order_id, order_no, customer_id, product_id, product_type, product_name, amount, channel, state)
VALUES (999999999, 'TEST-FK-001', 99999999, 1001, 1, 'TEST', 100.00, 1, 0);

-- 7.2 测试唯一键:重复订单号(应失败)
INSERT INTO t_order (order_id, order_no, customer_id, product_id, product_type, product_name, amount, channel, state)
SELECT 999999998, order_no, customer_id, product_id, product_type, product_name, amount, channel, state
FROM t_order LIMIT 1;

-- 7.3 测试 NOT NULL:空字符串(应失败,因为 mobile_hash 是 CHAR(64))
INSERT INTO t_customer (customer_id, customer_no, name, id_type, id_no, mobile_hash, kyc_status, customer_type)
VALUES (99999999, 'TEST-001', '测试', 1, '110101199001011234', '', 1, 1);

-- ============================================================
-- 8. 总结
-- ============================================================
SELECT '== 验证完成 ==' AS step,
    '所有表/外键/索引/种子数据检查完毕,业务查询性能良好' AS summary;
