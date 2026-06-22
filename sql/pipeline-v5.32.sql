-- V5.32 画布工作流 DDL
-- 4 张表: workflow / version / run / node_log

CREATE TABLE IF NOT EXISTS pipeline_workflow (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(200) NOT NULL COMMENT '工作流名称',
    description  VARCHAR(500) DEFAULT NULL,
    definition   LONGTEXT     NOT NULL COMMENT 'JSON: nodes + edges + viewport',
    version      INT          NOT NULL DEFAULT 1 COMMENT '当前版本号',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1-启用 0-禁用',
    create_by    BIGINT       DEFAULT NULL,
    update_by    BIGINT       DEFAULT NULL,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '0-未删 1-已删',
    KEY idx_create_by (create_by),
    KEY idx_status_deleted (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='画布工作流定义';

CREATE TABLE IF NOT EXISTS pipeline_workflow_version (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    workflow_id  BIGINT       NOT NULL,
    version      INT          NOT NULL,
    definition   LONGTEXT     NOT NULL,
    change_log   VARCHAR(500) DEFAULT NULL,
    create_by    BIGINT       DEFAULT NULL,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wf_version (workflow_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流版本历史';

CREATE TABLE IF NOT EXISTS pipeline_run (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    workflow_id         BIGINT       NOT NULL,
    workflow_name       VARCHAR(200) DEFAULT NULL,
    status              VARCHAR(20)  NOT NULL COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    trigger_by          BIGINT       DEFAULT NULL,
    trigger_type        VARCHAR(20)  DEFAULT 'MANUAL' COMMENT 'MANUAL/CRON/API',
    definition_snapshot LONGTEXT     NOT NULL,
    start_time          DATETIME     DEFAULT NULL,
    end_time            DATETIME     DEFAULT NULL,
    duration_ms         BIGINT       DEFAULT NULL,
    error_message       TEXT         DEFAULT NULL,
    result_summary      JSON         DEFAULT NULL,
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_workflow_id (workflow_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流执行历史';

CREATE TABLE IF NOT EXISTS pipeline_node_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    run_id          BIGINT       NOT NULL,
    node_id         VARCHAR(64)  NOT NULL,
    node_type       VARCHAR(40)  NOT NULL,
    node_name       VARCHAR(200) DEFAULT NULL,
    status          VARCHAR(20)  NOT NULL COMMENT 'PENDING/RUNNING/SUCCESS/FAILED/SKIPPED',
    start_time      DATETIME     DEFAULT NULL,
    end_time        DATETIME     DEFAULT NULL,
    duration_ms     BIGINT       DEFAULT NULL,
    input_rows      INT          DEFAULT NULL,
    output_rows     INT          DEFAULT NULL,
    output_preview  LONGTEXT     DEFAULT NULL,
    error_message   TEXT         DEFAULT NULL,
    config_snapshot JSON         DEFAULT NULL,
    KEY idx_run_id (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点执行日志';
