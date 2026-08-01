-- ============================================================
-- 双录一体化平台 - 外键约束 + 性能优化
-- 适用数据库: MySQL 8.0+ / MariaDB 10.6+
-- 重要: 必须在 01_schema.sql + 06_audit_log.sql 执行完后再执行
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 添加外键约束(数据完整性)
-- ============================================================

-- 订单 → 客户
ALTER TABLE t_order
    ADD CONSTRAINT fk_order_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer(customer_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- 会话 → 订单
ALTER TABLE t_session
    ADD CONSTRAINT fk_session_order
    FOREIGN KEY (order_id) REFERENCES t_order(order_id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- 话术节点 → 话术
ALTER TABLE t_script_node
    ADD CONSTRAINT fk_node_script
    FOREIGN KEY (script_id) REFERENCES t_script(script_id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- 风评 → 订单 / 客户 / 会话
ALTER TABLE t_risk_assess
    ADD CONSTRAINT fk_risk_order FOREIGN KEY (order_id) REFERENCES t_order(order_id),
    ADD CONSTRAINT fk_risk_customer FOREIGN KEY (customer_id) REFERENCES t_customer(customer_id),
    ADD CONSTRAINT fk_risk_session FOREIGN KEY (session_id) REFERENCES t_session(session_id);

-- 质检 → 会话 / 订单
ALTER TABLE t_quality
    ADD CONSTRAINT fk_qa_session FOREIGN KEY (session_id) REFERENCES t_session(session_id),
    ADD CONSTRAINT fk_qa_order FOREIGN KEY (order_id) REFERENCES t_order(order_id);

-- 合同 → 订单 / 客户
ALTER TABLE t_contract
    ADD CONSTRAINT fk_contract_order FOREIGN KEY (order_id) REFERENCES t_order(order_id),
    ADD CONSTRAINT fk_contract_customer FOREIGN KEY (customer_id) REFERENCES t_customer(customer_id);

-- 节点结果(06) → 会话 / 订单
ALTER TABLE t_node_result
    ADD CONSTRAINT fk_node_result_session FOREIGN KEY (session_id) REFERENCES t_session(session_id),
    ADD CONSTRAINT fk_node_result_order FOREIGN KEY (order_id) REFERENCES t_order(order_id);

-- 异常订单(06) → 订单
ALTER TABLE t_order_exception
    ADD CONSTRAINT fk_exception_order FOREIGN KEY (order_id) REFERENCES t_order(order_id);

-- 用户 → 网点
ALTER TABLE t_user
    ADD CONSTRAINT fk_user_branch FOREIGN KEY (branch_id) REFERENCES t_branch(branch_id),
    ADD CONSTRAINT fk_user_pubkey FOREIGN KEY (public_key_id) REFERENCES t_public_key(key_id);

-- 用户角色(06) → 用户 / 角色
ALTER TABLE t_user_role
    ADD CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES t_user(user_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES t_role(role_id) ON DELETE CASCADE;

-- 网点自引用(总行/分行/支行)
ALTER TABLE t_branch
    ADD CONSTRAINT fk_branch_parent FOREIGN KEY (parent_id) REFERENCES t_branch(branch_id);

-- 审计日志(06) → 订单 / 会话(弱关联,允许 NULL)
ALTER TABLE t_audit_log
    ADD CONSTRAINT fk_audit_order FOREIGN KEY (order_id) REFERENCES t_order(order_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_audit_session FOREIGN KEY (session_id) REFERENCES t_session(session_id) ON DELETE SET NULL;

-- 链码事件(06) → 订单(弱关联)
ALTER TABLE t_chain_event
    ADD CONSTRAINT fk_event_order FOREIGN KEY (order_id) REFERENCES t_order(order_id) ON DELETE SET NULL;

-- ============================================================
-- 2. 复合索引优化(高频查询)
-- ============================================================

-- 订单:按客户+状态+时间(风控大屏)
CREATE INDEX idx_order_customer_state_time ON t_order(customer_id, state, created_at);

-- 订单:按经理+时间(经理工作量统计)
CREATE INDEX idx_order_sales_time ON t_order(sales_user_id, created_at);

-- 订单:按网点+状态(网点运营报表)
CREATE INDEX idx_order_branch_state ON t_order(branch_id, state);

-- 会话:按订单+序号(查会话历史)
CREATE INDEX idx_session_order_seq ON t_session(order_id, session_seq);

-- 会话:按状态+开始时间(超时监控)
CREATE INDEX idx_session_state_start ON t_session(state, start_at);

-- 话术节点:按类型(批量查风险揭示节点)
CREATE INDEX idx_node_type ON t_script_node(node_type);

-- 质检:按会话+得分(质检大屏)
CREATE INDEX idx_qa_session_score ON t_quality(session_id, total_score);

-- 质检:按结论+时间(监管统计)
CREATE INDEX idx_qa_verdict_time ON t_quality(verdict, created_at);

-- 合同:按订单+类型(订单合同列表)
CREATE INDEX idx_contract_order_type ON t_contract(order_id, contract_type);

-- 合同:按状态+签署时间
CREATE INDEX idx_contract_status_time ON t_contract(status, sign_time);

-- ============================================================
-- 3. 表/库参数优化(MySQL 8+)
-- ============================================================

-- 启用严格模式
SET SESSION sql_mode = 'STRICT_ALL_TABLES,NO_ENGINE_SUBSTITUTION,ERROR_FOR_DIVISION_BY_ZERO';

-- 字符集与排序
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- 4. 物化视图(汇总表,业务侧直接查询)
-- ============================================================

DROP TABLE IF EXISTS t_report_daily;
CREATE TABLE t_report_daily (
    report_date        DATE            NOT NULL,
    branch_id          BIGINT          NOT NULL,
    product_type       TINYINT         NOT NULL,
    total_orders       INT             NOT NULL DEFAULT 0,
    completed_orders   INT             NOT NULL DEFAULT 0,
    failed_orders      INT             NOT NULL DEFAULT 0,
    cancelled_orders   INT             NOT NULL DEFAULT 0,
    total_amount       DECIMAL(18,2)   NOT NULL DEFAULT 0,
    avg_duration_sec   INT             NOT NULL DEFAULT 0,
    high_pass_count    INT             NOT NULL DEFAULT 0,
    pass_count         INT             NOT NULL DEFAULT 0,
    review_count       INT             NOT NULL DEFAULT 0,
    fail_count         INT             NOT NULL DEFAULT 0,
    chain_tx_count     INT             NOT NULL DEFAULT 0,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (report_date, branch_id, product_type),
    KEY idx_report_date (report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日报汇总表';

SET FOREIGN_KEY_CHECKS = 1;
