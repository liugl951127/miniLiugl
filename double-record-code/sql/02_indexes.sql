-- ============================================================
-- 双录一体化平台 - 索引与约束
-- 在 01_schema.sql 之后执行
-- ============================================================

-- ============================================================
-- t_customer 索引
-- ============================================================
ALTER TABLE t_customer ADD INDEX idx_mobile_hash (mobile_hash) COMMENT '手机号哈希查询';
ALTER TABLE t_customer ADD INDEX idx_risk_level (risk_level) COMMENT '风险等级筛选';
ALTER TABLE t_customer ADD INDEX idx_created_at (created_at) COMMENT '创建时间排序';
ALTER TABLE t_customer ADD INDEX idx_deleted_at (deleted_at) COMMENT '软删除索引';

-- ============================================================
-- t_order 索引(高频查询场景)
-- ============================================================
ALTER TABLE t_order ADD INDEX idx_customer_id (customer_id) COMMENT '按客户查订单';
ALTER TABLE t_order ADD INDEX idx_product_id (product_id) COMMENT '按产品查订单';
ALTER TABLE t_order ADD INDEX idx_state (state) COMMENT '按状态筛选';
ALTER TABLE t_order ADD INDEX idx_sales_user (sales_user_id) COMMENT '按客户经理查';
ALTER TABLE t_order ADD INDEX idx_branch_id (branch_id) COMMENT '按网点查';
ALTER TABLE t_order ADD INDEX idx_channel (channel) COMMENT '按渠道统计';
ALTER TABLE t_order ADD INDEX idx_created_at (created_at) COMMENT '按时间排序';
ALTER TABLE t_order ADD INDEX idx_completed_at (completed_at) COMMENT '完成时间查询';
ALTER TABLE t_order ADD INDEX idx_customer_state (customer_id, state) COMMENT '客户+状态联合查询';
ALTER TABLE t_order ADD INDEX idx_state_created (state, created_at) COMMENT '状态+时间(用于管理驾驶舱)';

-- ============================================================
-- t_session 索引
-- ============================================================
ALTER TABLE t_session ADD INDEX idx_order_id (order_id) COMMENT '按订单查会话';
ALTER TABLE t_session ADD INDEX idx_state (state) COMMENT '按状态筛选';
ALTER TABLE t_session ADD INDEX idx_trust_time (trust_time) COMMENT '按时间戳查';
ALTER TABLE t_session ADD INDEX idx_video_hash (video_hash) COMMENT '按视频哈希查重';

-- ============================================================
-- t_script 索引
-- ============================================================
ALTER TABLE t_script ADD INDEX idx_product_type (product_type) COMMENT '按产品类型';
ALTER TABLE t_script ADD INDEX idx_is_active (is_active) COMMENT '按启用状态';
ALTER TABLE t_script ADD INDEX idx_effective_date (effective_date) COMMENT '按生效日期';
ALTER TABLE t_script ADD INDEX idx_active_effective (is_active, effective_date) COMMENT '活跃+生效联合';

-- ============================================================
-- t_script_node 索引
-- ============================================================
ALTER TABLE t_script_node ADD INDEX idx_script_id (script_id) COMMENT '按话术查节点';
ALTER TABLE t_script_node ADD INDEX idx_node_type (node_type) COMMENT '按节点类型';

-- ============================================================
-- t_risk_assess 索引
-- ============================================================
ALTER TABLE t_risk_assess ADD INDEX idx_order_id (order_id) COMMENT '按订单查评估';
ALTER TABLE t_risk_assess ADD INDEX idx_customer_id (customer_id) COMMENT '按客户查评估历史';
ALTER TABLE t_risk_assess ADD INDEX idx_risk_level (risk_level) COMMENT '按风险等级';
ALTER TABLE t_risk_assess ADD INDEX idx_valid_until (valid_until) COMMENT '按有效期(用于过期清理)';
ALTER TABLE t_risk_assess ADD INDEX idx_customer_valid (customer_id, valid_until) COMMENT '客户最新评估查询';

-- ============================================================
-- t_quality 索引
-- ============================================================
ALTER TABLE t_quality ADD INDEX idx_session_id (session_id) COMMENT '按会话查质检';
ALTER TABLE t_quality ADD INDEX idx_order_id (order_id) COMMENT '按订单查质检';
ALTER TABLE t_quality ADD INDEX idx_qa_status (qa_status) COMMENT '按状态筛选';
ALTER TABLE t_quality ADD INDEX idx_verdict (verdict) COMMENT '按结论查';
ALTER TABLE t_quality ADD INDEX idx_total_score (total_score) COMMENT '按分数排序';
ALTER TABLE t_quality ADD INDEX idx_reviewer (reviewer_id) COMMENT '按复核人查';
ALTER TABLE t_quality ADD INDEX idx_status_score (qa_status, total_score) COMMENT '状态+分数联合';

-- ============================================================
-- t_contract 索引
-- ============================================================
ALTER TABLE t_contract ADD INDEX idx_order_id (order_id) COMMENT '按订单查合同';
ALTER TABLE t_contract ADD INDEX idx_customer_id (customer_id) COMMENT '按客户查合同';
ALTER TABLE t_contract ADD INDEX idx_status (status) COMMENT '按状态筛选';
ALTER TABLE t_contract ADD INDEX idx_signed_at (signed_at) COMMENT '按签署时间';
ALTER TABLE t_contract ADD INDEX idx_file_hash (file_hash) COMMENT '按哈希查重';
ALTER TABLE t_contract ADD INDEX idx_block_chain_tx (block_chain_tx) COMMENT '按区块链交易号查';

-- ============================================================
-- 分区表(订单表,按月分区,可选)
-- 适用于订单量大的场景(>1 亿行)
-- ============================================================
-- 注意: 启用分区前需要先删除主键和外键约束
-- ALTER TABLE t_order PARTITION BY RANGE (TO_DAYS(created_at)) (
--     PARTITION p202601 VALUES LESS THAN (TO_DAYS('2026-02-01')),
--     PARTITION p202602 VALUES LESS THAN (TO_DAYS('2026-03-01')),
--     PARTITION p202603 VALUES LESS THAN (TO_DAYS('2026-04-01')),
--     ...
--     PARTITION p_max VALUES LESS THAN MAXVALUE
-- );
