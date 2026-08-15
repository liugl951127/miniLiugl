-- Analytics 模块自身表 (V5.31, H2 MySQL mode)
-- 驼峰字段 + MyBatis-Plus underline-to-camel-case 自动映射

SET MODE MySQL;
SET NAMES utf8mb4;

-- ============================================================
-- 数据源配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS analytics_datasource (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL COMMENT '数据源名称',
    type            VARCHAR(32) NOT NULL DEFAULT 'h2' COMMENT 'mysql/h2/postgresql',
    jdbc_url        VARCHAR(512) COMMENT 'JDBC 连接 URL',
    username        VARCHAR(128),
    password_enc    VARCHAR(256) COMMENT 'AES-256 加密存储',
    description     VARCHAR(512),
    deleted         TINYINT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_ds_user ON analytics_datasource(user_id);

-- ============================================================
-- NL2SQL 历史记录
-- ============================================================
CREATE TABLE IF NOT EXISTS analytics_nlsql_history (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    data_source_id      BIGINT NOT NULL,
    question            VARCHAR(1024) NOT NULL COMMENT '自然语言问题',
    generated_sql       TEXT COMMENT 'LLM 生成的 SQL',
    corrected_sql       TEXT COMMENT '用户修正后的 SQL',
    model               VARCHAR(64) COMMENT '调用的模型',
    prompt_tokens       INT,
    completion_tokens   INT,
    duration_ms         BIGINT,
    success             TINYINT DEFAULT 1 COMMENT '是否执行成功',
    error_message       VARCHAR(512),
    feedback_rating     INT COMMENT '用户反馈 1-5 星',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_nl_user ON analytics_nlsql_history(user_id);
CREATE INDEX idx_nl_ds ON analytics_nlsql_history(data_source_id);
CREATE INDEX idx_nl_created ON analytics_nlsql_history(created_at DESC);

-- ============================================================
-- 文件导入任务
-- ============================================================
CREATE TABLE IF NOT EXISTS analytics_ingest_task (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    task_id         VARCHAR(64) NOT NULL UNIQUE COMMENT 'UUID 对外 ID',
    filename        VARCHAR(256) NOT NULL COMMENT '原始文件名',
    file_type       VARCHAR(16) COMMENT 'csv/json/log/tsv',
    encoding        VARCHAR(32) DEFAULT 'UTF-8',
    separator       VARCHAR(8) COMMENT '列分隔符',
    file_size       BIGINT,
    status          VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/PARSING/READY/FAILED',
    error_message   VARCHAR(512),
    quality_json    TEXT COMMENT '质量报告 JSON',
    total_rows      BIGINT,
    total_columns   INT,
    columns_json    TEXT COMMENT '列名 JSON 数组',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at     TIMESTAMP
);

CREATE INDEX idx_ingest_user ON analytics_ingest_task(user_id);
CREATE INDEX idx_ingest_status ON analytics_ingest_task(status);

-- ============================================================
-- 分析报告
-- ============================================================
CREATE TABLE IF NOT EXISTS analytics_report (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    report_id           VARCHAR(64) NOT NULL UNIQUE COMMENT 'UUID',
    title               VARCHAR(256),
    question            VARCHAR(1024) COMMENT '原始 NL2SQL 问题',
    sql_text            TEXT COMMENT '实际执行的 SQL',
    markdown            TEXT COMMENT '渲染后的 markdown',
    chart_options_json  TEXT COMMENT 'ECharts option JSON',
    row_count           BIGINT,
    duration_ms         BIGINT,
    format              VARCHAR(32) DEFAULT 'markdown',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_report_user ON analytics_report(user_id);
CREATE INDEX idx_report_created ON analytics_report(created_at DESC);
