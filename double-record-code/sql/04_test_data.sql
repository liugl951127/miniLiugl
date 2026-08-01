-- ============================================================
-- 双录一体化平台 - 测试数据脚本
-- 仅供开发测试环境使用,生产环境请勿执行
-- ============================================================

-- ============================================================
-- 1. 测试客户数据
-- ============================================================
INSERT INTO t_customer
    (customer_id, customer_no, name, id_type, id_no, mobile, mobile_hash, risk_level, risk_score, kyc_status, customer_type, vip_level, created_by)
VALUES
    (1000001, 'C20260801001', '张三', 1, '110101199001011234', '13800138001', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'C3', 65.5, 1, 1, 1, 10001),
    (1000002, 'C20260801002', '李四', 1, '310101198505051234', '13800138002', 'b2e182d4d201b0c93b1f7c0d8a0a4d4e3b1d4f3b8a4b3c0d3a4b3c0d3a4b3c0d', 'C1', 18.0, 1, 1, 0, 10001),
    (1000003, 'C20260801003', '王五', 1, '440101199203031234', '13800138003', 'c3f293e5e312c1d04c2f8d1e9b1b5e5f4c2e5f4c2e5f4c2e5f4c2e5f4c2e5f4c', 'C4', 72.0, 1, 1, 2, 10001),
    (1000004, 'C20260801004', '赵六', 1, '500101198801081234', '13800138004', 'd4a3a4f6f423d2e15d3a9e2f0c2c6f6f5d3f6f5d3f6f5d3f6f5d3f6f5d3f6f5d', 'C5', 88.0, 1, 1, 3, 10001);

-- ============================================================
-- 2. 测试订单数据
-- ============================================================
INSERT INTO t_order
    (order_id, order_no, customer_id, product_id, product_type, product_name, amount, currency, state, channel, sales_user_id, branch_id, reserve_at, started_at, completed_at, created_by)
VALUES
    (2000001, 'ORD20260801001', 1000001, 101, 1, 'XX 终身寿险(分红型)', 50000.00, 'CNY', 6, 1, 10001, 2001, '2026-08-01 09:00:00', '2026-08-01 09:05:00', '2026-08-01 09:25:00', 10001),
    (2000002, 'ORD20260801002', 1000002, 201, 2, 'XX 封闭式理财 365 天', 100000.00, 'CNY', 5, 3, 10001, 2001, '2026-08-01 10:00:00', '2026-08-01 10:05:00', NULL, 10001),
    (2000003, 'ORD20260801003', 1000003, 301, 3, 'XX 股票型基金', 80000.00, 'CNY', 3, 4, 10001, 2001, '2026-08-01 11:00:00', '2026-08-01 11:05:00', NULL, 10001),
    (2000004, 'ORD20260801004', 1000004, 101, 1, 'XX 终身寿险(分红型)', 200000.00, 'CNY', 0, 1, 10001, 2001, '2026-08-01 14:00:00', NULL, NULL, 10001);

-- ============================================================
-- 3. 测试双录会话数据
-- ============================================================
INSERT INTO t_session
    (session_id, order_id, session_seq, channel, script_id, script_version, state, video_url, video_hash, video_size, video_duration, start_at, end_at, trust_time, block_chain_tx, node_results)
VALUES
    (3000001, 2000001, 1, 1, 1001, 'V3.2', 2,
     'https://oss.example.com/records/2000001/3000001.mp4',
     'e5b3c4d5e6f7890123456789012345678901234567890123456789012345678',
     15728640, 1320,
     '2026-08-01 09:05:00', '2026-08-01 09:27:00',
     '2026-08-01 09:27:00',
     '0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef',
     JSON_ARRAY(
        JSON_OBJECT('node', 'N1', 'result', 'PASS', 'duration', 8),
        JSON_OBJECT('node', 'N2', 'result', 'PASS', 'duration', 12),
        JSON_OBJECT('node', 'N3', 'result', 'PASS', 'duration', 15),
        JSON_OBJECT('node', 'N4', 'result', 'PASS', 'duration', 10),
        JSON_OBJECT('node', 'N5', 'result', 'PASS', 'duration', 10),
        JSON_OBJECT('node', 'N6', 'result', 'PASS', 'duration', 8)
     )),
    (3000002, 2000002, 1, 3, 1002, 'V5.0', 1,
     NULL, NULL, NULL, NULL,
     '2026-08-01 10:05:00', NULL,
     NULL, NULL, NULL);

-- ============================================================
-- 4. 测试风评数据
-- ============================================================
INSERT INTO t_risk_assess
    (assess_id, order_id, customer_id, session_id, assess_type, answers, basic_score, experience_score, preference_score, liquidity_score, total_score, risk_level, product_match, valid_until, assess_mode)
VALUES
    (4000001, 2000001, 1000001, 3000001, 1,
     JSON_OBJECT(
        'Q1', '31-50', 'Q2', '30-100 万', 'Q3', JSON_ARRAY('股票', '基金', '银行理财'),
        'Q4', '焦虑但持有', 'Q5', '3-5 年'
     ),
     20.00, 25.00, 15.00, 15.00, 75.00, 'C3', 1, '2027-08-01 09:25:00', 1);

-- ============================================================
-- 5. 测试质检数据
-- ============================================================
INSERT INTO t_quality
    (qa_id, session_id, order_id, qa_type, qa_status, total_score, script_score, risk_score, confirm_score, av_score, flow_score, issues, keywords_missed, asr_text, sentiment_score, verdict, reviewer_id, reviewed_at)
VALUES
    (5000001, 3000001, 2000001, 2, 1, 92.50, 30.00, 25.00, 20.00, 14.50, 3.00,
     JSON_ARRAY(),
     JSON_ARRAY(),
     '您好我是客户经理本次双录...',
     8.50, 'HIGH_PASS', 10002, '2026-08-01 09:30:00');

-- ============================================================
-- 6. 测试合同数据
-- ============================================================
INSERT INTO t_contract
    (contract_id, order_id, customer_id, contract_no, contract_type, template_id, file_url, file_hash, file_size, sign_method, sign_ca_cert, sign_serial, sign_time, trust_time, sign_image_url, face_image_url, status, signed_at)
VALUES
    (6000001, 2000001, 1000001, 'CT20260801001', 1, 5001,
     'https://oss.example.com/contracts/CT20260801001.pdf',
     'f6c4d5e6f7890123456789012345678901234567890123456789012345678901',
     524288, 1,
     '-----BEGIN CERTIFICATE-----\nMIIDdzCCAl+gAwIBAgI...\n-----END CERTIFICATE-----',
     '2026080109250001',
     '2026-08-01 09:25:00', '2026-08-01 09:25:00',
     'https://oss.example.com/signatures/6000001.png',
     'https://oss.example.com/faces/6000001.jpg',
     1, '2026-08-01 09:25:00');
