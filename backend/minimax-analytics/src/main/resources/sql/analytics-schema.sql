-- Analytics 模块自身表 (V5.31, H2 MySQL mode)
-- 驼峰字段 + MyBatis-Plus underline-to-camel-case 自动映射
-- 所有字段与实体类严格对齐，TIMESTAMP 使用 DEFAULT CURRENT_TIMESTAMP

SET MODE MySQL;
SET NAMES utf8mb4;

-- ============================================================
-- analytics_datasource  — 数据源配置表
-- 实体字段: id, userId, name, type, jdbcUrl, username, passwordEnc, description, deleted, createdAt, updatedAt
-- ============================================================
CREATE TABLE IF NOT EXISTS analytics_datasource (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL COMMENT '数据源名称',
    type            VARCHAR(32) NOT NULL DEFAULT 'h2' COMMENT 'mysql/h2/postgresql',
    jdbc_url        VARCHAR(512) COMMENT 'JDBC 连接 URL',
    username        VARCHAR(128) COMMENT '用户名',
    password_enc    VARCHAR(256) COMMENT 'AES-256 加密存储',
    description     VARCHAR(512) COMMENT '描述',
    deleted         TINYINT DEFAULT 0 COMMENT '软删除: 0=正常, 1=删除',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

CREATE INDEX idx_ds_user ON analytics_datasource(user_id);
CREATE INDEX idx_ds_deleted ON analytics_datasource(deleted);

-- ============================================================
-- analytics_nlsql_history  — NL2SQL 调用历史
-- 实体字段: id, userId, dataSourceId, question, generatedSql, correctedSql, model,
--          promptTokens, completionTokens, durationMs, success, errorMessage, feedbackRating, createdAt
-- ============================================================
CREATE TABLE IF NOT EXISTS analytics_nlsql_history (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    data_source_id      BIGINT NOT NULL COMMENT '关联数据源ID',
    question            VARCHAR(1024) NOT NULL COMMENT '用户自然语言问题',
    generated_sql       TEXT COMMENT 'LLM 生成的 SQL',
    corrected_sql       TEXT COMMENT '用户修正后的 SQL',
    model               VARCHAR(64) COMMENT '调用的模型名称',
    prompt_tokens       INT COMMENT 'Prompt token 数量',
    completion_tokens   INT COMMENT 'Completion token 数量',
    duration_ms         BIGINT COMMENT '执行耗时(毫秒)',
    success             TINYINT DEFAULT 1 COMMENT '是否执行成功: 1=成功, 0=失败',
    error_message       VARCHAR(512) COMMENT '错误信息',
    feedback_rating     INT COMMENT '用户反馈评分 1-5 星',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

CREATE INDEX idx_nl_user ON analytics_nlsql_history(user_id);
CREATE INDEX idx_nl_ds ON analytics_nlsql_history(data_source_id);
CREATE INDEX idx_nl_created ON analytics_nlsql_history(created_at DESC);
CREATE INDEX idx_nl_success ON analytics_nlsql_history(success);

-- ============================================================
-- analytics_ingest_task  — 文件导入任务
-- 实体字段: id, userId, taskId, filename, fileType, encoding, separator,
--          fileSize, status, errorMessage, qualityJson, totalRows, totalColumns, columnsJson, createdAt, finishedAt
-- ============================================================
CREATE TABLE IF NOT EXISTS analytics_ingest_task (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    task_id         VARCHAR(64) NOT NULL UNIQUE COMMENT 'UUID 对外ID',
    filename        VARCHAR(256) NOT NULL COMMENT '原始文件名',
    file_type       VARCHAR(16) COMMENT 'csv/json/log/tsv',
    encoding        VARCHAR(32) DEFAULT 'UTF-8' COMMENT '文件编码',
    separator       VARCHAR(8) COMMENT '列分隔符',
    file_size       BIGINT COMMENT '文件字节数',
    status          VARCHAR(32) DEFAULT 'PENDING' COMMENT '状态: PENDING/PARSING/READY/FAILED',
    error_message   VARCHAR(512) COMMENT '失败原因',
    quality_json    TEXT COMMENT '质量报告 JSON',
    total_rows      BIGINT COMMENT '解析后行数',
    total_columns   INT COMMENT '列数',
    columns_json    TEXT COMMENT '列名 JSON 数组',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    finished_at     TIMESTAMP NULL COMMENT '完成时间'
);

CREATE INDEX idx_ingest_user ON analytics_ingest_task(user_id);
CREATE INDEX idx_ingest_status ON analytics_ingest_task(status);
CREATE INDEX idx_ingest_task_id ON analytics_ingest_task(task_id);

-- ============================================================
-- analytics_report  — 分析报告
-- 实体字段: id, userId, reportId, title, question, sqlText, markdown,
--          chartOptionsJson, rowCount, durationMs, format, createdAt
-- ============================================================
CREATE TABLE IF NOT EXISTS analytics_report (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    report_id           VARCHAR(64) NOT NULL UNIQUE COMMENT 'UUID 对外ID',
    title               VARCHAR(256) COMMENT '报告标题',
    question            VARCHAR(1024) COMMENT '原始 NL2SQL 问题',
    sql_text            TEXT COMMENT '实际执行的 SQL',
    markdown            TEXT COMMENT '渲染后的 markdown',
    chart_options_json  TEXT COMMENT 'ECharts option JSON',
    row_count           BIGINT COMMENT '结果行数',
    duration_ms         BIGINT COMMENT '执行耗时(毫秒)',
    format              VARCHAR(32) DEFAULT 'markdown' COMMENT '格式: markdown/html',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

CREATE INDEX idx_report_user ON analytics_report(user_id);
CREATE INDEX idx_report_created ON analytics_report(created_at DESC);
CREATE INDEX idx_report_report_id ON analytics_report(report_id);
