-- ============================================================
-- 双录一体化平台 - 初始化数据脚本
-- 包含: 状态机字典、风评问卷模板、话术模板样例
-- ============================================================

-- ============================================================
-- 1. 状态机字典(订单状态)
-- ============================================================
CREATE TABLE IF NOT EXISTS t_dict_order_state (
    state_code         TINYINT         NOT NULL,
    state_name         VARCHAR(32)     NOT NULL,
    state_desc         VARCHAR(128)    NOT NULL,
    can_rollback_to    VARCHAR(64)     DEFAULT NULL            COMMENT '可回退到的状态列表(逗号分隔)',
    is_terminal        TINYINT         NOT NULL DEFAULT 0      COMMENT '是否终态',
    sort_order         INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (state_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态字典';

INSERT INTO t_dict_order_state (state_code, state_name, state_desc, can_rollback_to, is_terminal, sort_order) VALUES
(0,  '已预约',     '客户已预约,等待开始核身',           '0',                       0, 1),
(1,  '已核验',     '身份核身通过,等待风险评估',          '0',                       0, 2),
(2,  '话术执行中', '话术节点正在执行,等待视频录制',      '1',                       0, 3),
(3,  '视频录制中', '正在录制音视频,等待签字',            '2',                       0, 4),
(4,  '电子签约',   '电子合同签署中,等待质检',            '3',                       0, 5),
(5,  '质检通过',   '智能质检+人工复核通过,等待归档',     '4',                       0, 6),
(6,  '订单完成',   '全流程完成,已归档',                 '',                        1, 7),
(-1, '已取消',     '用户主动取消或超时未支付',           '0',                       1, 99),
(-2, '已失败',     '关键节点失败,需人工介入',            '0,1,2,3,4',               1, 99);

-- ============================================================
-- 2. 会话状态字典
-- ============================================================
CREATE TABLE IF NOT EXISTS t_dict_session_state (
    state_code         TINYINT         NOT NULL,
    state_name         VARCHAR(32)     NOT NULL,
    state_desc         VARCHAR(128)    NOT NULL,
    PRIMARY KEY (state_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话状态字典';

INSERT INTO t_dict_session_state (state_code, state_name, state_desc) VALUES
(0, '未开始', '会话已创建,尚未开始录制'),
(1, '进行中', '正在录制或执行话术'),
(2, '已完成', '正常完成,视频已上传'),
(3, '中断',   '客户或系统主动中断,可续接'),
(4, '失败',   '录制或上传失败,需重做');

-- ============================================================
-- 3. 产品类型字典
-- ============================================================
CREATE TABLE IF NOT EXISTS t_dict_product_type (
    type_code          TINYINT         NOT NULL,
    type_name          VARCHAR(32)     NOT NULL,
    risk_level         TINYINT         NOT NULL                COMMENT '默认风险等级',
    primary_script     VARCHAR(64)     NOT NULL                COMMENT '默认话术编码',
    PRIMARY KEY (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品类型字典';

INSERT INTO t_dict_product_type (type_code, type_name, risk_level, primary_script) VALUES
(1, '保险', 3, 'INS-LIFE-V3'),
(2, '理财', 2, 'WEALTH-CLOSED-V5'),
(3, '基金', 4, 'FUND-EQ-V4'),
(4, '信托', 4, 'TRUST-V1'),
(5, '贵金属', 5, 'GOLD-V1');

-- ============================================================
-- 4. 话术模板:终身寿险(分红型)
-- ============================================================
INSERT INTO t_script
    (script_id, script_code, script_name, product_type, product_ids, version, is_active, is_gray, gray_ratio, effective_date, expire_date, total_nodes, estimated_minutes, file_url, file_hash, approved_by, approved_at, created_by)
VALUES
    (1001, 'INS-LIFE-V3', '终身寿险(分红型)话术 V3.2', 1, JSON_ARRAY(101, 102, 103), 'V3.2', 1, 0, 0.00, '2026-01-15', NULL, 6, 8, '/templates/INS-LIFE-V3.json', 'a1b2c3d4e5f6789012345678901234567890123456789012345678901234abcd', 10001, '2026-01-10 10:00:00', 10001);

INSERT INTO t_script_node (script_id, node_seq, node_code, node_type, node_name, content, content_audio_url, is_required, require_confirm, min_read_seconds, keywords, skip_allowed, risk_level, next_node_rule) VALUES
(1001, 1, 'N1', 'GREETING',         '问候核身', '您好,我是 XX 银行客户经理 XX,本次双录将全程录音录像,作为合规凭证保存 10 年,请问您同意吗?', NULL, 1, 1, 5, JSON_ARRAY('同意', '可以', '是'), 0, NULL, NULL),
(1001, 2, 'N2', 'PRODUCT',          '产品告知', '本产品为终身寿险(分红型),保险责任为身故或全残保障,具体以合同条款为准。', NULL, 1, 1, 8, JSON_ARRAY('终身寿险', '分红'), 0, NULL, NULL),
(1001, 3, 'N3', 'RISK_DISCLOSURE',  '风险揭示', '分红型保险收益不确定,演示利率非保证,可能为零,您是否充分理解?', NULL, 1, 1, 10, JSON_ARRAY('理解', '明白', '知道'), 0, 3, NULL),
(1001, 4, 'N4', 'SUITABILITY',      '适当性匹配', '根据您的风险评估结果,您为 C3 级稳健型投资者,本产品风险等级 R3,匹配通过。', NULL, 1, 1, 6, JSON_ARRAY('匹配', '确认'), 0, NULL, NULL),
(1001, 5, 'N5', 'COOLING_PERIOD',   '犹豫期告知', '您享有 15 天犹豫期,期内退保仅扣除不超过 10 元工本费,是否知晓?', NULL, 1, 1, 7, JSON_ARRAY('知晓', '明白', '是'), 0, NULL, NULL),
(1001, 6, 'N6', 'CONFIRMATION',     '签字确认', '以上内容均为您的真实意愿表达,请确认是否投保?', NULL, 1, 1, 5, JSON_ARRAY('确认', '是', '投保'), 0, NULL, 'END');

-- ============================================================
-- 5. 话术模板:封闭式净值型理财
-- ============================================================
INSERT INTO t_script
    (script_id, script_code, script_name, product_type, product_ids, version, is_active, is_gray, gray_ratio, effective_date, expire_date, total_nodes, estimated_minutes, file_url, file_hash, approved_by, approved_at, created_by)
VALUES
    (1002, 'WEALTH-CLOSED-V5', '封闭式净值型理财话术 V5.0', 2, JSON_ARRAY(201, 202, 203, 204), 'V5.0', 1, 0, 0.00, '2026-05-20', NULL, 6, 6, '/templates/WEALTH-CLOSED-V5.json', 'b2c3d4e5f6789012345678901234567890123456789012345678901234abcdef', 10002, '2026-05-15 10:00:00', 10002);

INSERT INTO t_script_node (script_id, node_seq, node_code, node_type, node_name, content, is_required, require_confirm, min_read_seconds, keywords, skip_allowed, risk_level) VALUES
(1002, 1, 'N1', 'SUITABILITY',      '风险匹配', '经评估您的风险等级为 C3(稳健型),本产品风险等级 C2,匹配通过,是否知晓?', 1, 1, 6, JSON_ARRAY('知晓', '明白', '是'), 0, NULL),
(1002, 2, 'N2', 'PRODUCT',          '产品要素', '本产品为封闭式净值型理财,期限 365 天,业绩比较基准 3.5%-4.2%。', 1, 1, 8, JSON_ARRAY('净值型', '365'), 0, NULL),
(1002, 3, 'N3', 'RISK_DISCLOSURE',  '收益说明', '业绩比较基准非保证收益,实际可能低于基准,本金不保证,您是否知晓?', 1, 1, 8, JSON_ARRAY('知晓', '明白'), 0, 2),
(1002, 4, 'N4', 'PRODUCT',          '资金用途', '本产品募集资金主要投资于债券和同业存单。', 1, 1, 5, JSON_ARRAY('债券', '同业存单'), 0, NULL),
(1002, 5, 'N5', 'RISK_DISCLOSURE',  '流动性告知', '产品封闭期内不可赎回,是否影响您的资金安排?', 1, 1, 6, JSON_ARRAY('不影响', '可以', '知晓'), 0, NULL),
(1002, 6, 'N6', 'CONFIRMATION',     '签字确认', '请确认知悉风险与产品要素,是否继续购买?', 1, 1, 5, JSON_ARRAY('确认', '是', '购买'), 0, NULL);

-- ============================================================
-- 6. 风评问卷模板(简化版,实际存为 JSON 字段)
-- ============================================================
CREATE TABLE IF NOT EXISTS t_risk_questionnaire (
    questionnaire_id   BIGINT          NOT NULL AUTO_INCREMENT,
    questionnaire_code VARCHAR(32)     NOT NULL,
    version            VARCHAR(16)     NOT NULL,
    title              VARCHAR(128)    NOT NULL,
    questions          JSON            NOT NULL                COMMENT '题目定义(JSON)',
    scoring_rules      JSON            NOT NULL                COMMENT '评分规则',
    level_mapping      JSON            NOT NULL                COMMENT '分值-等级映射',
    is_active          TINYINT         NOT NULL DEFAULT 1,
    effective_date     DATE            NOT NULL,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (questionnaire_id),
    UNIQUE KEY uk_code_version (questionnaire_code, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风评问卷模板';

INSERT INTO t_risk_questionnaire
    (questionnaire_code, version, title, questions, scoring_rules, level_mapping, is_active, effective_date)
VALUES
    ('KYC-V6', 'V6.0', '客户风险评估问卷 V6.0',
     JSON_ARRAY(
        JSON_OBJECT('qid', 'Q1', 'type', 'single', 'text', '您的年龄段', 'options', JSON_ARRAY('18-30', '31-50', '51-65', '65 以上'), 'weight', 1),
        JSON_OBJECT('qid', 'Q2', 'type', 'single', 'text', '您的家庭年收入(税前)', 'options', JSON_ARRAY('10 万以下', '10-30 万', '30-100 万', '100 万以上'), 'weight', 2),
        JSON_OBJECT('qid', 'Q3', 'type', 'multi', 'text', '您过往的投资经历', 'options', JSON_ARRAY('股票', '基金', '银行理财', '不动产', '信托', '无'), 'weight', 2),
        JSON_OBJECT('qid', 'Q4', 'type', 'single', 'text', '投资亏损 20% 时的反应', 'options', JSON_ARRAY('无法接受', '焦虑但持有', '可接受', '加仓抄底'), 'weight', 3),
        JSON_OBJECT('qid', 'Q5', 'type', 'single', 'text', '这笔资金的预计持有期限', 'options', JSON_ARRAY('1 年内', '1-3 年', '3-5 年', '5 年以上'), 'weight', 2)
     ),
     JSON_OBJECT('method', 'weighted_sum', 'max_score', 100, 'pass_score', 0),
     JSON_OBJECT(
        JSON_ARRAY('C1', 0, 20, '保守型'),
        JSON_ARRAY('C2', 21, 40, '稳健型'),
        JSON_ARRAY('C3', 41, 60, '平衡型'),
        JSON_ARRAY('C4', 61, 80, '成长型'),
        JSON_ARRAY('C5', 81, 100, '进取型')
     ),
     1, '2026-07-01');

-- ============================================================
-- 7. 渠道字典
-- ============================================================
CREATE TABLE IF NOT EXISTS t_dict_channel (
    channel_code       TINYINT         NOT NULL,
    channel_name       VARCHAR(32)     NOT NULL,
    terminal_type      VARCHAR(32)     NOT NULL                COMMENT '终端类型',
    enabled            TINYINT         NOT NULL DEFAULT 1,
    sort_order         INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道字典';

INSERT INTO t_dict_channel (channel_code, channel_name, terminal_type, enabled, sort_order) VALUES
(1, '线上 H5',     'mobile_h5',  1, 1),
(2, '线上小程序',  'wechat_mp',  1, 2),
(3, '线下一体机',  'atm',        1, 3),
(4, 'PAD 移动展业', 'pad',        1, 4),
(5, '网点 PC 端',  'pc',         1, 5);

-- ============================================================
-- 8. 质检结论字典
-- ============================================================
CREATE TABLE IF NOT EXISTS t_dict_verdict (
    verdict_code       VARCHAR(32)     NOT NULL,
    verdict_name       VARCHAR(32)     NOT NULL,
    score_min          DECIMAL(5,2)    NOT NULL,
    score_max          DECIMAL(5,2)    NOT NULL,
    action             VARCHAR(64)     NOT NULL                COMMENT '处理动作',
    PRIMARY KEY (verdict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检结论字典';

INSERT INTO t_dict_verdict (verdict_code, verdict_name, score_min, score_max, action) VALUES
('HIGH_PASS',  '高分通过',   90.00, 100.00, 'AUTO_ARCHIVE'),
('PASS',       '通过',       70.00, 89.99,  'SAMPLE_REVIEW'),
('REVIEW',     '需复检',     50.00, 69.99,  'MANUAL_REVIEW'),
('FAIL',       '不通过',     0.00,  49.99,  'BLOCK_AND_REDO');
