-- ============================================================
-- V2.8.5 Pipeline 流水线 DDL
-- 2 张新表: ai_intent_keyword, pipeline_log
-- ============================================================

-- 1. AI 意图关键词表
CREATE TABLE IF NOT EXISTS `ai_intent_keyword` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `intent` VARCHAR(64) NOT NULL COMMENT '意图枚举名 (GENERATE_CHART / GENERATE_MUSIC / ...)',
    `keyword` VARCHAR(255) NOT NULL COMMENT '关键词或正则表达式',
    `weight` INT NOT NULL DEFAULT 1 COMMENT '权重 1-10',
    `is_regex` TINYINT NOT NULL DEFAULT 0 COMMENT '是否正则 0=否 1=是',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 0=否 1=是',
    `language` VARCHAR(8) DEFAULT 'zh' COMMENT '语言 (zh/en)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_intent_kw_lang` (`intent`, `keyword`, `language`),
    KEY `idx_intent` (`intent`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 意图关键词 (V2.8.5 替代硬编码)';

-- 2. Pipeline 执行日志表
CREATE TABLE IF NOT EXISTS `pipeline_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id` VARCHAR(64) DEFAULT NULL COMMENT '会话 ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户 ID',
    `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端 IP',
    `input_text` TEXT COMMENT '用户输入 (截断 2000)',
    `input_modality` VARCHAR(16) DEFAULT NULL COMMENT '输入模态 (TEXT/IMAGE/AUDIO/VIDEO/FILE)',
    `intent` VARCHAR(64) DEFAULT NULL COMMENT '识别意图',
    `output_text` TEXT COMMENT '模型输出 (截断 4000)',
    `output_tokens` INT DEFAULT NULL COMMENT '生成 token 数',
    `compute_device` VARCHAR(16) DEFAULT NULL COMMENT '实际设备 (CPU/GPU)',
    `compute_mode` VARCHAR(16) DEFAULT NULL COMMENT '计算模式 (CPU/GPU/AUTO)',
    `total_cost_ms` BIGINT DEFAULT NULL COMMENT '总耗时 ms',
    `stage_costs` TEXT COMMENT '各阶段耗时 (JSON)',
    `risk_level` VARCHAR(16) DEFAULT NULL COMMENT '风控等级',
    `needs_review` TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要人工复审 0/1',
    `rag_hits` INT DEFAULT NULL COMMENT 'RAG 命中数',
    `tool_calls` INT DEFAULT NULL COMMENT '工具调用数',
    `error_message` VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_intent` (`intent`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_risk_level` (`risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Pipeline 执行日志 (V2.8.5)';
