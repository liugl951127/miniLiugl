-- ============================================================
-- 双录一体化平台 - 数据库建表脚本
-- 适用数据库: MySQL 8.0+ / MariaDB 10.6+
-- 字符集: utf8mb4 / 排序: utf8mb4_unicode_ci
-- 引擎: InnoDB (事务 + 行锁 + MVCC)
-- 命名规范: t_xxx (业务表), idx_xxx (索引), uk_xxx (唯一键)
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 客户主数据表
-- 说明: 全行统一客户主数据,跨业务线打通
-- ============================================================
DROP TABLE IF EXISTS t_customer;
CREATE TABLE t_customer (
    customer_id        BIGINT          NOT NULL                COMMENT '客户唯一标识(雪花 ID)',
    customer_no        VARCHAR(32)     NOT NULL                COMMENT '客户编号(ECIF 系统)',
    name               VARCHAR(64)     NOT NULL                COMMENT '客户姓名(加密存储)',
    id_type            TINYINT         NOT NULL DEFAULT 1      COMMENT '证件类型 1-身份证 2-护照 3-军官证 4-港澳台',
    id_no              VARCHAR(32)     NOT NULL                COMMENT '证件号码(SM4 加密)',
    id_expire_date     DATE            DEFAULT NULL            COMMENT '证件有效期',
    mobile             VARCHAR(20)     DEFAULT NULL            COMMENT '手机号(加密)',
    mobile_hash        CHAR(64)        DEFAULT NULL            COMMENT '手机号 SHA-256 哈希(用于查询索引)',
    risk_level         CHAR(2)         NOT NULL DEFAULT 'C1'   COMMENT '风险等级 C1-C5',
    risk_score         DECIMAL(5,2)    DEFAULT NULL            COMMENT '风险评分 0-100',
    risk_expire_at     DATETIME        DEFAULT NULL            COMMENT '风险评估过期时间(12 个月有效)',
    kyc_status         TINYINT         NOT NULL DEFAULT 0      COMMENT 'KYC 状态 0-未完成 1-已完成 2-过期',
    customer_type      TINYINT         NOT NULL DEFAULT 1      COMMENT '客户类型 1-个人 2-机构',
    vip_level          TINYINT         DEFAULT 0               COMMENT 'VIP 等级 0-普通 1-银卡 2-金卡 3-钻石',
    created_by         BIGINT          DEFAULT NULL            COMMENT '创建人',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    updated_by         BIGINT          DEFAULT NULL            COMMENT '最后修改人',
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    deleted_at         DATETIME        DEFAULT NULL            COMMENT '软删除时间',
    version            INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (customer_id),
    UNIQUE KEY uk_customer_no (customer_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户主数据表';

-- ============================================================
-- 2. 订单主表
-- 说明: 双录业务订单,关联客户、产品、风评、合同
-- ============================================================
DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
    order_id           BIGINT          NOT NULL                COMMENT '订单唯一标识(雪花 ID)',
    order_no           VARCHAR(32)     NOT NULL                COMMENT '订单编号(业务可见)',
    customer_id        BIGINT          NOT NULL                COMMENT '客户 ID',
    product_id         BIGINT          NOT NULL                COMMENT '产品 ID',
    product_type       TINYINT         NOT NULL                COMMENT '产品类型 1-保险 2-理财 3-基金 4-信托 5-贵金属',
    product_name       VARCHAR(128)    NOT NULL                COMMENT '产品名称(冗余,便于查询)',
    amount             DECIMAL(18,2)   NOT NULL                COMMENT '购买金额(分单位)',
    currency           CHAR(3)         NOT NULL DEFAULT 'CNY'  COMMENT '币种',
    state              TINYINT         NOT NULL DEFAULT 0      COMMENT '订单状态 0-已预约 1-已核验 2-话术执行中 3-视频录制中 4-电子签约 5-质检通过 6-订单完成 -1-已取消 -2-已失败',
    state_history      JSON            DEFAULT NULL            COMMENT '状态流转历史(JSON 数组)',
    channel            TINYINT         NOT NULL                COMMENT '渠道 1-H5 2-小程序 3-线下一体机 4-PAD 5-网点 PC',
    sales_user_id      BIGINT          DEFAULT NULL            COMMENT '客户经理 ID',
    branch_id          BIGINT          DEFAULT NULL            COMMENT '网点 ID',
    terminal_id        VARCHAR(64)     DEFAULT NULL            COMMENT '终端设备 ID',
    ip_address         VARCHAR(64)     DEFAULT NULL            COMMENT '客户 IP 地址',
    location           VARCHAR(128)    DEFAULT NULL            COMMENT '地理位置(GPS)',
    reserve_at         DATETIME        DEFAULT NULL            COMMENT '预约时间',
    started_at         DATETIME        DEFAULT NULL            COMMENT '开始时间',
    completed_at       DATETIME        DEFAULT NULL            COMMENT '完成时间',
    expire_at          DATETIME        DEFAULT NULL            COMMENT '订单过期时间',
    remark             VARCHAR(512)    DEFAULT NULL            COMMENT '备注',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at         DATETIME        DEFAULT NULL,
    version            INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

-- ============================================================
-- 3. 双录会话表
-- 说明: 一笔订单可能对应多次双录会话(如重做)
-- ============================================================
DROP TABLE IF EXISTS t_session;
CREATE TABLE t_session (
    session_id         BIGINT          NOT NULL                COMMENT '会话唯一标识',
    order_id           BIGINT          NOT NULL                COMMENT '订单 ID',
    session_seq        TINYINT         NOT NULL DEFAULT 1      COMMENT '会话序号(1=首次 2=重做)',
    channel            TINYINT         NOT NULL                COMMENT '渠道',
    terminal_id        VARCHAR(64)     DEFAULT NULL            COMMENT '终端设备 ID',
    script_id          BIGINT          NOT NULL                COMMENT '使用的话术模板 ID',
    script_version     VARCHAR(16)     NOT NULL                COMMENT '话术版本',
    state              TINYINT         NOT NULL DEFAULT 0      COMMENT '会话状态 0-未开始 1-进行中 2-已完成 3-中断 4-失败',
    video_url          VARCHAR(512)    DEFAULT NULL            COMMENT '录制视频 OSS 地址',
    video_hash         CHAR(64)        DEFAULT NULL            COMMENT '视频 SHA-256 哈希',
    video_size         BIGINT          DEFAULT NULL            COMMENT '视频文件大小(字节)',
    video_duration     INT             DEFAULT NULL            COMMENT '视频时长(秒)',
    audio_url          VARCHAR(512)    DEFAULT NULL            COMMENT '音频地址(独立存储时)',
    start_at           DATETIME        DEFAULT NULL            COMMENT '开始时间',
    end_at             DATETIME        DEFAULT NULL            COMMENT '结束时间',
    pause_count        INT             NOT NULL DEFAULT 0      COMMENT '暂停次数',
    interrupt_reason   VARCHAR(256)    DEFAULT NULL            COMMENT '中断原因',
    node_results       JSON            DEFAULT NULL            COMMENT '各节点执行结果(JSON)',
    trust_time         DATETIME        DEFAULT NULL            COMMENT '可信时间戳(国家授时)',
    block_chain_tx     VARCHAR(128)    DEFAULT NULL            COMMENT '区块链存证交易号',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='双录会话表';

-- ============================================================
-- 4. 话术模板表(主版本)
-- 说明: 原子化话术模板,一个产品对应一个活跃版本
-- ============================================================
DROP TABLE IF EXISTS t_script;
CREATE TABLE t_script (
    script_id          BIGINT          NOT NULL                COMMENT '话术模板 ID',
    script_code        VARCHAR(64)     NOT NULL                COMMENT '模板编码(如 INS-LIFE-V3)',
    script_name        VARCHAR(128)    NOT NULL                COMMENT '模板名称',
    product_type       TINYINT         NOT NULL                COMMENT '产品类型',
    product_ids        JSON            DEFAULT NULL            COMMENT '适用的产品 ID 列表',
    version            VARCHAR(16)     NOT NULL                COMMENT '版本号(如 V3.2)',
    is_active          TINYINT         NOT NULL DEFAULT 0      COMMENT '是否启用 0-否 1-是',
    is_gray            TINYINT         NOT NULL DEFAULT 0      COMMENT '是否灰度 0-否 1-是',
    gray_ratio         DECIMAL(3,2)    DEFAULT 0.00           COMMENT '灰度比例 0.00-1.00',
    effective_date     DATE            NOT NULL                COMMENT '生效日期',
    expire_date        DATE            DEFAULT NULL            COMMENT '失效日期',
    total_nodes        INT             NOT NULL DEFAULT 0      COMMENT '节点总数',
    estimated_minutes  INT             NOT NULL DEFAULT 0      COMMENT '预计耗时(分钟)',
    file_url           VARCHAR(512)    DEFAULT NULL            COMMENT '话术文件 URL',
    file_hash          CHAR(64)        DEFAULT NULL            COMMENT '话术文件 SHA-256',
    approved_by        BIGINT          DEFAULT NULL            COMMENT '审批人',
    approved_at        DATETIME        DEFAULT NULL            COMMENT '审批时间',
    created_by         BIGINT          NOT NULL                COMMENT '创建人',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         BIGINT          DEFAULT NULL,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at         DATETIME        DEFAULT NULL,
    version_lock       INT             NOT NULL DEFAULT 0      COMMENT '乐观锁',
    PRIMARY KEY (script_id),
    UNIQUE KEY uk_script_code_version (script_code, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话术模板表';

-- ============================================================
-- 5. 话术节点表(原子化拆解)
-- 说明: 模板拆分为 N 个独立节点,每个节点单独配置和版本管理
-- ============================================================
DROP TABLE IF EXISTS t_script_node;
CREATE TABLE t_script_node (
    node_id            BIGINT          NOT NULL                COMMENT '节点 ID',
    script_id          BIGINT          NOT NULL                COMMENT '所属话术 ID',
    node_seq           INT             NOT NULL                COMMENT '节点顺序(从 1 开始)',
    node_code          VARCHAR(32)     NOT NULL                COMMENT '节点编码(N1, N2...)',
    node_type          VARCHAR(32)     NOT NULL                COMMENT '节点类型: GREETING/PRODUCT/RISK_DISCLOSURE/SUITABILITY/COOLING_PERIOD/CONFIRMATION',
    node_name          VARCHAR(64)     NOT NULL                COMMENT '节点名称',
    content            TEXT            NOT NULL                COMMENT '节点内容(标准话术文本)',
    content_audio_url  VARCHAR(512)    DEFAULT NULL            COMMENT 'TTS 音频 URL',
    is_required        TINYINT         NOT NULL DEFAULT 1      COMMENT '是否必读 0-否 1-是',
    require_confirm    TINYINT         NOT NULL DEFAULT 0      COMMENT '是否需客户确认 0-否 1-是',
    min_read_seconds   INT             NOT NULL DEFAULT 3      COMMENT '最少阅读时长(秒)',
    keywords           JSON            DEFAULT NULL            COMMENT '必须包含的关键词列表',
    skip_allowed       TINYINT         NOT NULL DEFAULT 0      COMMENT '是否允许跳过 0-否 1-是',
    risk_level         TINYINT         DEFAULT NULL            COMMENT '风险等级(R1-R5),用于质检加权',
    next_node_rule     VARCHAR(256)    DEFAULT NULL            COMMENT '下一节点跳转规则',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (node_id),
    UNIQUE KEY uk_script_seq (script_id, node_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话术节点表';

-- ============================================================
-- 6. 风评问卷表
-- 说明: KYC 风险评估问卷
-- ============================================================
DROP TABLE IF EXISTS t_risk_assess;
CREATE TABLE t_risk_assess (
    assess_id          BIGINT          NOT NULL                COMMENT '评估 ID',
    order_id           BIGINT          NOT NULL                COMMENT '订单 ID',
    customer_id        BIGINT          NOT NULL                COMMENT '客户 ID',
    session_id         BIGINT          NOT NULL                COMMENT '双录会话 ID',
    assess_type        TINYINT         NOT NULL DEFAULT 1      COMMENT '评估类型 1-新评估 2-重新评估',
    answers            JSON            NOT NULL                COMMENT '问卷答案(JSON)',
    basic_score        DECIMAL(5,2)    DEFAULT NULL            COMMENT '基本信息得分',
    experience_score   DECIMAL(5,2)    DEFAULT NULL            COMMENT '投资经验得分',
    preference_score   DECIMAL(5,2)    DEFAULT NULL            COMMENT '风险偏好得分',
    liquidity_score    DECIMAL(5,2)    DEFAULT NULL            COMMENT '流动性得分',
    total_score        DECIMAL(5,2)    NOT NULL                COMMENT '总分',
    risk_level         CHAR(2)         NOT NULL                COMMENT '评估等级 C1-C5',
    product_match      TINYINT         NOT NULL DEFAULT 1      COMMENT '产品匹配 0-不匹配 1-匹配',
    valid_until        DATETIME        NOT NULL                COMMENT '有效期至',
    assessor_id        BIGINT          DEFAULT NULL            COMMENT '评估人(自动/人工)',
    assess_mode        TINYINT         NOT NULL DEFAULT 1      COMMENT '评估方式 1-自助 2-客户经理协助',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (assess_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风险评估表';

-- ============================================================
-- 7. 智能质检结果表
-- 说明: AI 质检 + 人工复核结果
-- ============================================================
DROP TABLE IF EXISTS t_quality;
CREATE TABLE t_quality (
    qa_id              BIGINT          NOT NULL                COMMENT '质检 ID',
    session_id         BIGINT          NOT NULL                COMMENT '会话 ID',
    order_id           BIGINT          NOT NULL                COMMENT '订单 ID',
    qa_type            TINYINT         NOT NULL                COMMENT '质检类型 1-L1 规则 2-L2 AI 3-L3 人工',
    qa_status          TINYINT         NOT NULL DEFAULT 0      COMMENT '质检状态 0-待处理 1-通过 2-失败 3-需复检',
    total_score        DECIMAL(5,2)    DEFAULT NULL            COMMENT '总分(0-100)',
    script_score       DECIMAL(5,2)    DEFAULT NULL            COMMENT '话术完整度得分',
    risk_score         DECIMAL(5,2)    DEFAULT NULL            COMMENT '风险揭示得分',
    confirm_score      DECIMAL(5,2)    DEFAULT NULL            COMMENT '客户确认得分',
    av_score           DECIMAL(5,2)    DEFAULT NULL            COMMENT '音视频合规得分',
    flow_score         DECIMAL(5,2)    DEFAULT NULL            COMMENT '流程完整度得分',
    issues             JSON            DEFAULT NULL            COMMENT '问题列表(JSON)',
    keywords_missed    JSON            DEFAULT NULL            COMMENT '缺失关键词',
    asr_text           MEDIUMTEXT      DEFAULT NULL            COMMENT 'ASR 转写文本',
    sentiment_score    DECIMAL(5,2)    DEFAULT NULL            COMMENT '情感分析得分',
    verdict            VARCHAR(64)     DEFAULT NULL            COMMENT '质检结论: HIGH_PASS/PASS/REVIEW/FAIL',
    reviewer_id        BIGINT          DEFAULT NULL            COMMENT '复核人 ID',
    review_remark      TEXT            DEFAULT NULL            COMMENT '复核意见',
    reviewed_at        DATETIME        DEFAULT NULL            COMMENT '复核时间',
    appeal_status      TINYINT         DEFAULT 0               COMMENT '申诉状态 0-无 1-申诉中 2-已处理',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (qa_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能质检结果表';

-- ============================================================
-- 8. 电子合同表
-- 说明: 客户签署的电子合同,哈希上链
-- ============================================================
DROP TABLE IF EXISTS t_contract;
CREATE TABLE t_contract (
    contract_id        BIGINT          NOT NULL                COMMENT '合同 ID',
    order_id           BIGINT          NOT NULL                COMMENT '订单 ID',
    customer_id        BIGINT          NOT NULL                COMMENT '客户 ID',
    contract_no        VARCHAR(64)     NOT NULL                COMMENT '合同编号',
    contract_type      TINYINT         NOT NULL                COMMENT '合同类型 1-主合同 2-附加协议 3-风险揭示书',
    template_id        BIGINT          NOT NULL                COMMENT '合同模板 ID',
    file_url           VARCHAR(512)    NOT NULL                COMMENT '合同 PDF 地址',
    file_hash          CHAR(64)        NOT NULL                COMMENT '合同 SHA-256 哈希',
    file_size          INT             DEFAULT NULL            COMMENT '文件大小(字节)',
    sign_method        TINYINT         NOT NULL DEFAULT 1      COMMENT '签署方式 1-CA 数字证书 2-手写电子签名 3-短信验证',
    sign_ca_cert       VARCHAR(2048)   DEFAULT NULL            COMMENT 'CA 数字证书(PEM)',
    sign_serial        VARCHAR(64)     DEFAULT NULL            COMMENT '签名序列号',
    sign_time          DATETIME        DEFAULT NULL            COMMENT '签署时间(国家授时)',
    trust_time         DATETIME        DEFAULT NULL            COMMENT '可信时间戳',
    sign_image_url     VARCHAR(512)    DEFAULT NULL            COMMENT '手写签名图 URL',
    face_image_url     VARCHAR(512)    DEFAULT NULL            COMMENT '人脸核身图 URL',
    sms_code           VARCHAR(10)     DEFAULT NULL            COMMENT '短信验证码(脱敏)',
    block_chain_tx     VARCHAR(128)    DEFAULT NULL            COMMENT '区块链存证交易号',
    block_chain_addr   VARCHAR(128)    DEFAULT NULL            COMMENT '区块链合约地址',
    status             TINYINT         NOT NULL DEFAULT 0      COMMENT '合同状态 0-待签 1-已签 2-已撤销 3-已过期',
    signed_at          DATETIME        DEFAULT NULL            COMMENT '签署完成时间',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (contract_id),
    UNIQUE KEY uk_contract_no (contract_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子合同表';

SET FOREIGN_KEY_CHECKS = 1;
