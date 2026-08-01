-- ============================================================
-- 双录一体化平台 - 运维脚本(常用查询)
-- ============================================================

-- ============================================================
-- 1. 驾驶舱:实时订单状态分布
-- ============================================================
SELECT
    s.state_name                                              AS '状态',
    COUNT(o.order_id)                                         AS '订单数',
    ROUND(SUM(o.amount) / 100, 2)                             AS '总金额(元)',
    ROUND(AVG(TIMESTAMPDIFF(SECOND, o.started_at, o.completed_at)) / 60, 1) AS '平均耗时(分钟)'
FROM t_order o
LEFT JOIN t_dict_order_state s ON o.state = s.state_code
WHERE o.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
  AND o.deleted_at IS NULL
GROUP BY o.state, s.state_name
ORDER BY o.state;

-- ============================================================
-- 2. 异常监控:过去 1 小时失败订单
-- ============================================================
SELECT
    o.order_no, o.product_name, o.channel, o.amount,
    o.sales_user_id, o.terminal_id, o.created_at
FROM t_order o
WHERE o.state IN (-1, -2)
  AND o.created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
  AND o.deleted_at IS NULL
ORDER BY o.created_at DESC;

-- ============================================================
-- 3. 质检通过率统计(按天)
-- ============================================================
SELECT
    DATE(q.created_at)            AS '日期',
    COUNT(*)                       AS '总数',
    SUM(CASE WHEN q.qa_status = 1 THEN 1 ELSE 0 END) AS '通过数',
    SUM(CASE WHEN q.qa_status = 2 THEN 1 ELSE 0 END) AS '失败数',
    SUM(CASE WHEN q.qa_status = 3 THEN 1 ELSE 0 END) AS '复检数',
    ROUND(SUM(CASE WHEN q.qa_status = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS '通过率(%)',
    ROUND(AVG(q.total_score), 2)  AS '平均分'
FROM t_quality q
WHERE q.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(q.created_at)
ORDER BY DATE(q.created_at) DESC;

-- ============================================================
-- 4. 话术模板执行统计
-- ============================================================
SELECT
    s.script_code, s.script_name, s.version,
    COUNT(se.session_id) AS '使用次数',
    SUM(CASE WHEN se.state = 2 THEN 1 ELSE 0 END) AS '完成数',
    SUM(CASE WHEN se.state = 3 THEN 1 ELSE 0 END) AS '中断数',
    SUM(CASE WHEN se.state = 4 THEN 1 ELSE 0 END) AS '失败数',
    ROUND(AVG(se.video_duration), 0) AS '平均视频时长(秒)'
FROM t_script s
LEFT JOIN t_session se ON s.script_id = se.script_id
WHERE se.start_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY s.script_id, s.script_code, s.script_name, s.version
ORDER BY COUNT(se.session_id) DESC;

-- ============================================================
-- 5. 客户经理产能排名
-- ============================================================
SELECT
    o.sales_user_id,
    COUNT(o.order_id)              AS '订单数',
    ROUND(SUM(o.amount) / 100, 2)  AS '总销售额(元)',
    SUM(CASE WHEN o.state = 6 THEN 1 ELSE 0 END) AS '完成数',
    ROUND(SUM(CASE WHEN o.state = 6 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS '完成率(%)'
FROM t_order o
WHERE o.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
  AND o.sales_user_id IS NOT NULL
  AND o.deleted_at IS NULL
GROUP BY o.sales_user_id
ORDER BY COUNT(o.order_id) DESC
LIMIT 20;

-- ============================================================
-- 6. 风评过期预警(7 天内过期)
-- ============================================================
SELECT
    c.customer_id, c.name, c.mobile,
    c.risk_level, c.risk_expire_at,
    DATEDIFF(c.risk_expire_at, NOW()) AS '剩余天数'
FROM t_customer c
WHERE c.risk_expire_at BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 7 DAY)
  AND c.deleted_at IS NULL
ORDER BY c.risk_expire_at ASC;

-- ============================================================
-- 7. 数据归档(10 年前的双录视频移至冷存储)
-- ============================================================
-- 注意: 实际执行前请备份,确认无误后再执行
-- UPDATE t_session
-- SET video_url = REPLACE(video_url, 'oss-hot', 'oss-cold')
-- WHERE end_at < DATE_SUB(NOW(), INTERVAL 10 YEAR)
--   AND state = 2;

-- ============================================================
-- 8. 软删除数据清理(90 天前已删除的订单)
-- ============================================================
-- 注意: 实际执行前请备份
-- DELETE FROM t_order
-- WHERE deleted_at IS NOT NULL
--   AND deleted_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- ============================================================
-- 9. 慢查询监控(执行时间 > 3 秒的查询)
-- ============================================================
-- SET GLOBAL slow_query_log = 'ON';
-- SET GLOBAL long_query_time = 3;
-- SHOW VARIABLES LIKE 'slow_query_log%';

-- ============================================================
-- 10. 表空间使用情况
-- ============================================================
SELECT
    table_name                                    AS '表名',
    ROUND(data_length / 1024 / 1024, 2)          AS '数据大小(MB)',
    ROUND(index_length / 1024 / 1024, 2)         AS '索引大小(MB)',
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS '总大小(MB)',
    table_rows                                     AS '行数(估算)'
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name LIKE 't_%'
ORDER BY (data_length + index_length) DESC;
