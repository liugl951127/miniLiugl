-- ============================================================
-- 双录一体化平台 - 审计日志表(业务侧)
-- 适用数据库: MySQL 8.0+ / MariaDB 10.6+
-- 字符集: utf8mb4 / 排序: utf8mb4_unicode_ci
-- 引擎: InnoDB
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 审计日志表(只增不改,与链上审计互补)
-- 说明: 记录业务系统侧的所有操作,司法举证合规
-- ============================================================
DROP TABLE IF EXISTS t_audit_log;
CREATE TABLE t_audit_log (
    id                 BIGINT          NOT NULL AUTO_INCREMENT   COMMENT '日志 ID(雪花)',
    order_id           BIGINT          DEFAULT NULL              COMMENT '关联订单',
    session_id         BIGINT          DEFAULT NULL              COMMENT '关联会话',
    action             VARCHAR(64)     NOT NULL                  COMMENT '操作类型',
    action_module      VARCHAR(32)     DEFAULT NULL              COMMENT '操作模块',
    operator_id        BIGINT          NOT NULL                  COMMENT '操作人 ID',
    operator_name      VARCHAR(64)     NOT NULL                  COMMENT '操作人姓名',
    operator_org       VARCHAR(64)     DEFAULT NULL              COMMENT '操作人所属机构',
    operator_ip        VARCHAR(64)     DEFAULT NULL              COMMENT '操作 IP',
    operator_device    VARCHAR(128)    DEFAULT NULL              COMMENT '操作设备',
    target_type        VARCHAR(32)     DEFAULT NULL              COMMENT '操作对象类型',
    target_id          VARCHAR(64)     DEFAULT NULL              COMMENT '操作对象 ID',
    old_value          JSON            DEFAULT NULL              COMMENT '变更前值',
    new_value          JSON            DEFAULT NULL              COMMENT '变更后值',
    reason             VARCHAR(512)    DEFAULT NULL              COMMENT '操作原因',
    risk_flag          TINYINT         NOT NULL DEFAULT 0        COMMENT '风险标识 0-正常 1-可疑 2-高危',
    block_chain_tx     VARCHAR(128)    DEFAULT NULL              COMMENT '链上对应交易号',
    created_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间(毫秒)',
    PRIMARY KEY (id),
    KEY idx_audit_order (order_id),
    KEY idx_audit_session (session_id),
    KEY idx_audit_operator (operator_id, created_at),
    KEY idx_audit_action (action, created_at),
    KEY idx_audit_risk (risk_flag, created_at),
    KEY idx_audit_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表(只增不改)';

-- ============================================================
-- 2. 链码事件落地表(Kafka 消费入库)
-- 说明: 链码 SetEvent 异步落库,用于风控/合规的实时分析
-- ============================================================
DROP TABLE IF EXISTS t_chain_event;
CREATE TABLE t_chain_event (
    id                 BIGINT          NOT NULL AUTO_INCREMENT   COMMENT '事件 ID',
    event_name         VARCHAR(64)     NOT NULL                  COMMENT '事件名(EvidenceSubmitted/StateChanged/...)',
    order_id           BIGINT          DEFAULT NULL              COMMENT '关联订单',
    session_id         BIGINT          DEFAULT NULL              COMMENT '关联会话',
    chain_tx_id        VARCHAR(128)    NOT NULL                  COMMENT '链上交易 ID',
    block_num          BIGINT          DEFAULT NULL              COMMENT '区块号',
    block_hash         VARCHAR(128)    DEFAULT NULL              COMMENT '区块哈希',
    channel_id         VARCHAR(64)     DEFAULT NULL              COMMENT '通道 ID',
    payload            JSON            DEFAULT NULL              COMMENT '事件载荷',
    processed          TINYINT         NOT NULL DEFAULT 0        COMMENT '已处理 0-否 1-是',
    process_remark     VARCHAR(512)    DEFAULT NULL              COMMENT '处理备注',
    event_time         DATETIME(3)     NOT NULL                  COMMENT '事件时间(从链码读取)',
    received_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '入库时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_chain_tx (chain_tx_id, event_name),
    KEY idx_event_name (event_name, event_time),
    KEY idx_event_order (order_id),
    KEY idx_event_unprocessed (processed, received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='链码事件落地表';

-- ============================================================
-- 3. 节点结果表(业务侧)
-- 说明: 业务侧存全量节点结果,链上只存 Merkle 根
-- ============================================================
DROP TABLE IF EXISTS t_node_result;
CREATE TABLE t_node_result (
    id                 BIGINT          NOT NULL AUTO_INCREMENT   COMMENT '结果 ID',
    session_id         BIGINT          NOT NULL                  COMMENT '会话 ID',
    order_id           BIGINT          NOT NULL                  COMMENT '订单 ID',
    node_seq           INT             NOT NULL                  COMMENT '节点序号',
    node_code          VARCHAR(32)     NOT NULL                  COMMENT '节点编码',
    node_name          VARCHAR(64)     DEFAULT NULL              COMMENT '节点名称',
    result             TINYINT         NOT NULL                  COMMENT '结果 1-通过 2-失败 3-跳过 4-超时 5-重做',
    duration_ms        INT             NOT NULL DEFAULT 0        COMMENT '耗时(毫秒)',
    customer_said      TEXT            DEFAULT NULL              COMMENT '客户实际回答',
    keywords_hit       JSON            DEFAULT NULL              COMMENT '命中关键词列表',
    keywords_missed    JSON            DEFAULT NULL              COMMENT '未命中关键词',
    asr_confidence     DECIMAL(5,4)    DEFAULT NULL              COMMENT 'ASR 置信度 0-1',
    audio_url          VARCHAR(512)    DEFAULT NULL              COMMENT '客户原声 OSS',
    audio_hash         CHAR(64)        DEFAULT NULL              COMMENT '客户原声 SHA-256',
    video_clip_url     VARCHAR(512)    DEFAULT NULL              COMMENT '本节点对应视频片段',
    video_clip_hash    CHAR(64)        DEFAULT NULL              COMMENT '视频片段 SHA-256',
    started_at         DATETIME(3)     DEFAULT NULL              COMMENT '开始时间',
    ended_at           DATETIME(3)     DEFAULT NULL              COMMENT '结束时间',
    result_hash        CHAR(64)        NOT NULL                  COMMENT '结果指纹(SM3)',
    retries            INT             NOT NULL DEFAULT 0        COMMENT '重做次数',
    error_message      VARCHAR(512)    DEFAULT NULL              COMMENT '错误信息',
    created_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_node (session_id, node_code),
    KEY idx_node_session (session_id, node_seq),
    KEY idx_node_order (order_id),
    KEY idx_node_result (result),
    KEY idx_node_hash (result_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点执行结果表';

-- ============================================================
-- 4. 公钥注册表(国密 SM2)
-- 说明: 链上公钥的链下副本,用于业务侧快速查询
-- ============================================================
DROP TABLE IF EXISTS t_public_key;
CREATE TABLE t_public_key (
    key_id             BIGINT          NOT NULL AUTO_INCREMENT   COMMENT '公钥 ID',
    party_type         VARCHAR(32)     NOT NULL                  COMMENT '参与方类型 CUSTOMER/MANAGER/WITNESS/BRANCH/SYSTEM',
    party_id           VARCHAR(64)     NOT NULL                  COMMENT '参与方 ID',
    party_name         VARCHAR(64)     DEFAULT NULL              COMMENT '参与方名称',
    public_key_hex     VARCHAR(512)    NOT NULL                  COMMENT '公钥(hex,SM2 X.509 编码)',
    algo               VARCHAR(16)     NOT NULL DEFAULT 'SM2'    COMMENT '算法 SM2/RSA',
    key_size           INT             NOT NULL DEFAULT 256      COMMENT '密钥长度',
    issuer             VARCHAR(64)     DEFAULT NULL              COMMENT 'CA 颁发机构',
    cert_serial        VARCHAR(128)    DEFAULT NULL              COMMENT '证书序列号',
    cert_expire_at     DATETIME        DEFAULT NULL              COMMENT '证书过期时间',
    status             TINYINT         NOT NULL DEFAULT 1        COMMENT '状态 0-禁用 1-启用 2-吊销',
    block_chain_tx     VARCHAR(128)    DEFAULT NULL              COMMENT '链上注册交易号',
    created_by         BIGINT          DEFAULT NULL              COMMENT '注册人',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (key_id),
    UNIQUE KEY uk_party (party_type, party_id, algo),
    KEY idx_key_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公钥注册表';

-- ============================================================
-- 5. 业务用户与权限表
-- 说明: 双录业务系统的 RBAC
-- ============================================================
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    user_id            BIGINT          NOT NULL                  COMMENT '用户 ID(雪花)',
    user_no            VARCHAR(32)     NOT NULL                  COMMENT '工号',
    name               VARCHAR(64)     NOT NULL                  COMMENT '姓名',
    mobile             VARCHAR(20)     DEFAULT NULL              COMMENT '手机号(加密)',
    email              VARCHAR(64)     DEFAULT NULL              COMMENT '邮箱',
    id_no              VARCHAR(32)     DEFAULT NULL              COMMENT '身份证(加密)',
    role_code          VARCHAR(32)     NOT NULL                  COMMENT '主角色编码',
    branch_id          BIGINT          DEFAULT NULL              COMMENT '所属网点',
    org_id             BIGINT          DEFAULT NULL              COMMENT '所属机构',
    msp_id             VARCHAR(32)     DEFAULT NULL              COMMENT 'Fabric MSP ID(链上身份)',
    public_key_id      BIGINT          DEFAULT NULL              COMMENT '关联公钥',
    is_active          TINYINT         NOT NULL DEFAULT 1        COMMENT '启用 0-否 1-是',
    last_login_at      DATETIME        DEFAULT NULL              COMMENT '最后登录',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at         DATETIME        DEFAULT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_no (user_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务用户表';

DROP TABLE IF EXISTS t_role;
CREATE TABLE t_role (
    role_id            BIGINT          NOT NULL AUTO_INCREMENT   COMMENT '角色 ID',
    role_code          VARCHAR(32)     NOT NULL                  COMMENT '角色编码',
    role_name          VARCHAR(64)     NOT NULL                  COMMENT '角色名称',
    description        VARCHAR(256)    DEFAULT NULL,
    permissions        JSON            NOT NULL                  COMMENT '权限点 JSON 数组',
    is_builtin         TINYINT         NOT NULL DEFAULT 0        COMMENT '是否内置 0-否 1-是',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

DROP TABLE IF EXISTS t_user_role;
CREATE TABLE t_user_role (
    id                 BIGINT          NOT NULL AUTO_INCREMENT   COMMENT 'ID',
    user_id            BIGINT          NOT NULL                  COMMENT '用户 ID',
    role_id            BIGINT          NOT NULL                  COMMENT '角色 ID',
    granted_by         BIGINT          DEFAULT NULL              COMMENT '授权人',
    granted_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at         DATETIME        DEFAULT NULL              COMMENT '过期时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_user (role_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ============================================================
-- 6. 网点表
-- ============================================================
DROP TABLE IF EXISTS t_branch;
CREATE TABLE t_branch (
    branch_id          BIGINT          NOT NULL                  COMMENT '网点 ID',
    branch_code        VARCHAR(32)     NOT NULL                  COMMENT '网点编号',
    branch_name        VARCHAR(128)    NOT NULL                  COMMENT '网点名称',
    parent_id          BIGINT          DEFAULT NULL              COMMENT '上级网点(分行/支行)',
    level              TINYINT         NOT NULL DEFAULT 1        COMMENT '层级 1-总行 2-省分行 3-市分行 4-支行 5-网点',
    region             VARCHAR(64)     DEFAULT NULL              COMMENT '所属区域',
    address            VARCHAR(256)    DEFAULT NULL              COMMENT '地址',
    msp_id             VARCHAR(32)     DEFAULT NULL              COMMENT 'Fabric MSP ID',
    status             TINYINT         NOT NULL DEFAULT 1        COMMENT '状态 0-禁用 1-启用',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (branch_id),
    UNIQUE KEY uk_branch_code (branch_code),
    KEY idx_branch_parent (parent_id),
    KEY idx_branch_msp (msp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网点表';

-- ============================================================
-- 7. 异常订单表(重做/申诉/争议)
-- ============================================================
DROP TABLE IF EXISTS t_order_exception;
CREATE TABLE t_order_exception (
    id                 BIGINT          NOT NULL AUTO_INCREMENT   COMMENT '异常 ID',
    order_id           BIGINT          NOT NULL                  COMMENT '订单 ID',
    exception_type     TINYINT         NOT NULL                  COMMENT '异常类型 1-话术重录 2-签字失败 3-ASR 失败 4-客户申诉 5-监管检查',
    exception_reason   VARCHAR(1024)   DEFAULT NULL              COMMENT '异常原因',
    handler_id         BIGINT          DEFAULT NULL              COMMENT '处理人',
    handler_remark     VARCHAR(1024)   DEFAULT NULL              COMMENT '处理意见',
    handle_status      TINYINT         NOT NULL DEFAULT 0        COMMENT '处理状态 0-待处理 1-处理中 2-已解决 3-已驳回',
    resolved_at        DATETIME        DEFAULT NULL              COMMENT '解决时间',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_exc_order (order_id),
    KEY idx_exc_status (handle_status, created_at),
    KEY idx_exc_type (exception_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单异常表';
