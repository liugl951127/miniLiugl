-- V5.31 数据智能分析模块 DDL
-- 4 张表: 数据源 / 文件任务 / NL2SQL 历史 / 报告

CREATE TABLE IF NOT EXISTS analytics_datasource (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    name        VARCHAR(128) NOT NULL,
    type        VARCHAR(16)  NOT NULL COMMENT 'mysql/h2/postgresql',
    jdbc_url    VARCHAR(512) NOT NULL,
    username    VARCHAR(64)  NOT NULL,
    password_enc VARCHAR(512) COMMENT 'AES 加密',
    description VARCHAR(512),
    deleted     TINYINT      DEFAULT 0,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析数据源';

CREATE TABLE IF NOT EXISTS analytics_ingest_task (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    task_id       VARCHAR(32)  NOT NULL,
    filename      VARCHAR(256) NOT NULL,
    file_type     VARCHAR(16)  NOT NULL,
    encoding      VARCHAR(16),
    separator     VARCHAR(8),
    file_size     BIGINT,
    status        VARCHAR(16)  DEFAULT 'PENDING' COMMENT 'PENDING/PARSING/READY/FAILED',
    error_message VARCHAR(1024),
    quality_json  TEXT,
    total_rows    BIGINT,
    total_columns BIGINT,
    columns_json  TEXT,
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    finished_at   DATETIME,
    UNIQUE KEY uk_task (task_id),
    INDEX idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件导入任务';

CREATE TABLE IF NOT EXISTS analytics_nlsql_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    data_source_id  BIGINT,
    question        TEXT         NOT NULL,
    generated_sql   TEXT,
    corrected_sql   TEXT         COMMENT '用户修改后, 用于训练',
    model           VARCHAR(64),
    prompt_tokens   INT,
    completion_tokens INT,
    duration_ms     BIGINT,
    success         TINYINT      DEFAULT 1,
    error_message   VARCHAR(1024),
    feedback_rating INT          COMMENT '1-5',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NL2SQL 历史';

CREATE TABLE IF NOT EXISTS analytics_report (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    report_id         VARCHAR(32)  NOT NULL,
    title             VARCHAR(256),
    question          TEXT,
    sql_text          TEXT,
    markdown          LONGTEXT,
    chart_options_json TEXT,
    row_count         BIGINT,
    duration_ms       BIGINT,
    format            VARCHAR(16) DEFAULT 'markdown',
    created_at        DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_report (report_id),
    INDEX idx_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析报告';
