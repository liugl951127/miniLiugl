-- =============================================================
-- MiniMax Platform V3.5.19 Schema (auto-gen from 77 entity)
-- 重新生成时间: 2026-07-25
-- 目标: MySQL 8.0+ / MariaDB 10.4+
-- 用法: mysql -uroot -proot123456 < sql/v3.5.19-schema.sql
-- =============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
CREATE DATABASE IF NOT EXISTS minimax_platform DEFAULT CHARACTER SET utf8mb4;
USE minimax_platform;

-- ----------------------------
-- admin_audit_log (12 fields)
-- ----------------------------
DROP TABLE IF EXISTS `admin_audit_log`;
CREATE TABLE `admin_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `actor_id` BIGINT NOT NULL DEFAULT '' COMMENT 'actorId',
  `actor_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'actorName',
  `action` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'action',
  `resource_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'resourceType',
  `resource_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'resourceId',
  `detail` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'detail',
  `result` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'result',
  `error_msg` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMsg',
  `ip` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'ip',
  `user_agent` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'userAgent',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- agent_group (13 fields)
-- ----------------------------
DROP TABLE IF EXISTS `agent_group`;
CREATE TABLE `agent_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `group_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'groupId',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `strategy` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'strategy',
  `members_json` TEXT NOT NULL DEFAULT '' COMMENT 'membersJson',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `owner_id` BIGINT NOT NULL DEFAULT '' COMMENT 'ownerId',
  `tags` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'tags',
  `last_run_at` DATETIME NULL DEFAULT NULL COMMENT 'lastRunAt',
  `run_count` INT NOT NULL DEFAULT '' COMMENT 'runCount',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- agent_task (15 fields)
-- ----------------------------
DROP TABLE IF EXISTS `agent_task`;
CREATE TABLE `agent_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `task_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'taskId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `goal` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'goal',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `rounds` INT NOT NULL DEFAULT '' COMMENT 'rounds',
  `result` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'result',
  `llm_calls` INT NOT NULL DEFAULT '' COMMENT 'llmCalls',
  `tool_calls` INT NOT NULL DEFAULT '' COMMENT 'toolCalls',
  `total_tokens` INT NOT NULL DEFAULT '' COMMENT 'totalTokens',
  `error_msg` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMsg',
  `latency_ms` BIGINT NOT NULL DEFAULT '' COMMENT 'latencyMs',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- ai_chat_message (8 fields)
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `session_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'sessionId',
  `role` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'role',
  `content` TEXT NOT NULL DEFAULT '' COMMENT 'content',
  `tool_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'toolCode',
  `tool_input` TEXT NOT NULL DEFAULT '' COMMENT 'toolInput',
  `tool_output` TEXT NOT NULL DEFAULT '' COMMENT 'toolOutput',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- ai_chat_session (8 fields)
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `session_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'sessionId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'username',
  `title` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'title',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- ai_generation_log (20 fields)
-- ----------------------------
DROP TABLE IF EXISTS `ai_generation_log`;
CREATE TABLE `ai_generation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `generation_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'generationId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'username',
  `user_ip` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'userIp',
  `modality` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'modality',
  `model_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'modelName',
  `model_version` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'modelVersion',
  `prompt` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'prompt',
  `negative_prompt` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'negativePrompt',
  `parameters` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'parameters',
  `output_url` TEXT NOT NULL DEFAULT '' COMMENT 'outputUrl',
  `output_size` BIGINT NOT NULL DEFAULT '' COMMENT 'outputSize',
  `output_hash` TEXT NOT NULL DEFAULT '' COMMENT 'outputHash',
  `watermarked` INT NOT NULL DEFAULT '' COMMENT 'watermarked',
  `watermark_text` TEXT NOT NULL DEFAULT '' COMMENT 'watermarkText',
  `duration_ms` INT NOT NULL DEFAULT '' COMMENT 'durationMs',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `error_msg` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMsg',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- ai_intent_keyword (10 fields)
-- ----------------------------
DROP TABLE IF EXISTS `ai_intent_keyword`;
CREATE TABLE `ai_intent_keyword` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `intent` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'intent',
  `keyword` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'keyword',
  `weight` INT NOT NULL DEFAULT '' COMMENT 'weight',
  `is_regex` INT NOT NULL DEFAULT '' COMMENT 'isRegex',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `language` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'language',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'remark',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- ai_tool (23 fields)
-- ----------------------------
DROP TABLE IF EXISTS `ai_tool`;
CREATE TABLE `ai_tool` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'code',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `category` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'category',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `icon` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'icon',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `builtin` INT NOT NULL DEFAULT '' COMMENT 'builtin',
  `input_schema` TEXT NOT NULL DEFAULT '' COMMENT 'inputSchema',
  `output_schema` TEXT NOT NULL DEFAULT '' COMMENT 'outputSchema',
  `default_config` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'defaultConfig',
  `impl_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'implType',
  `impl_value` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'implValue',
  `rate_limit` INT NOT NULL DEFAULT '' COMMENT 'rateLimit',
  `timeout_seconds` INT NOT NULL DEFAULT '' COMMENT 'timeoutSeconds',
  `role_required` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'roleRequired',
  `tags` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'tags',
  `version` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'version',
  `author` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'author',
  `created_by` BIGINT NOT NULL DEFAULT '' COMMENT 'createdBy',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- ai_tool_invocation (13 fields)
-- ----------------------------
DROP TABLE IF EXISTS `ai_tool_invocation`;
CREATE TABLE `ai_tool_invocation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `tool_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'toolCode',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'username',
  `input_json` TEXT NOT NULL DEFAULT '' COMMENT 'inputJson',
  `output_json` TEXT NOT NULL DEFAULT '' COMMENT 'outputJson',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `error_message` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMessage',
  `duration_ms` INT NOT NULL DEFAULT '' COMMENT 'durationMs',
  `ip` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'ip',
  `user_agent` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'userAgent',
  `data_source_id` BIGINT NOT NULL DEFAULT '' COMMENT 'dataSourceId',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- alert_channel (12 fields)
-- ----------------------------
DROP TABLE IF EXISTS `alert_channel`;
CREATE TABLE `alert_channel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `channel_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'channelType',
  `type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'type',
  `target` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'target',
  `config` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'config',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `priority` INT NOT NULL DEFAULT '' COMMENT 'priority',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `template` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'template',
  `created_by` BIGINT NOT NULL DEFAULT '' COMMENT 'createdBy',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- alert_event (14 fields)
-- ----------------------------
DROP TABLE IF EXISTS `alert_event`;
CREATE TABLE `alert_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `rule_id` BIGINT NOT NULL DEFAULT '' COMMENT 'ruleId',
  `rule_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'ruleName',
  `severity` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'severity',
  `metric_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'metricName',
  `metric_value` DECIMAL(18,2) NOT NULL DEFAULT '' COMMENT 'metricValue',
  `threshold` DECIMAL(18,2) NOT NULL DEFAULT '' COMMENT 'threshold',
  `message` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'message',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `fired_at` DATETIME NOT NULL DEFAULT '' COMMENT 'firedAt',
  `resolved_at` DATETIME NOT NULL DEFAULT '' COMMENT 'resolvedAt',
  `acked_at` DATETIME NOT NULL DEFAULT '' COMMENT 'ackedAt',
  `acked_by` BIGINT NOT NULL DEFAULT '' COMMENT 'ackedBy',
  `duration` BIGINT NOT NULL DEFAULT '' COMMENT 'duration',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- alert_rule (14 fields)
-- ----------------------------
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE `alert_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `metric_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'metricName',
  `service` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'service',
  `operator` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'operator',
  `threshold` DECIMAL(18,2) NOT NULL DEFAULT '' COMMENT 'threshold',
  `severity` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'severity',
  `cooldown_minutes` INT NOT NULL DEFAULT '' COMMENT 'cooldownMinutes',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `tags` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'tags',
  `notify_channel` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'notifyChannel',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- analytics_datasource (11 fields)
-- ----------------------------
DROP TABLE IF EXISTS `analytics_datasource`;
CREATE TABLE `analytics_datasource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'type',
  `jdbc_url` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'jdbcUrl',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'username',
  `password_enc` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'passwordEnc',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- analytics_ingest_task (16 fields)
-- ----------------------------
DROP TABLE IF EXISTS `analytics_ingest_task`;
CREATE TABLE `analytics_ingest_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `task_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'taskId',
  `filename` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'filename',
  `file_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'fileType',
  `encoding` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'encoding',
  `separator` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'separator',
  `file_size` BIGINT NOT NULL DEFAULT '' COMMENT 'fileSize',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `error_message` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMessage',
  `quality_json` TEXT NOT NULL DEFAULT '' COMMENT 'qualityJson',
  `total_rows` BIGINT NOT NULL DEFAULT '' COMMENT 'totalRows',
  `total_columns` BIGINT NOT NULL DEFAULT '' COMMENT 'totalColumns',
  `columns_json` TEXT NOT NULL DEFAULT '' COMMENT 'columnsJson',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `finished_at` DATETIME NOT NULL DEFAULT '' COMMENT 'finishedAt',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- analytics_nlsql_history (14 fields)
-- ----------------------------
DROP TABLE IF EXISTS `analytics_nlsql_history`;
CREATE TABLE `analytics_nlsql_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `data_source_id` BIGINT NOT NULL DEFAULT '' COMMENT 'dataSourceId',
  `question` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'question',
  `generated_sql` TEXT NOT NULL DEFAULT '' COMMENT 'generatedSql',
  `corrected_sql` TEXT NOT NULL DEFAULT '' COMMENT 'correctedSql',
  `model` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'model',
  `prompt_tokens` INT NOT NULL DEFAULT '' COMMENT 'promptTokens',
  `completion_tokens` INT NOT NULL DEFAULT '' COMMENT 'completionTokens',
  `duration_ms` BIGINT NOT NULL DEFAULT '' COMMENT 'durationMs',
  `success` TINYINT(1) NOT NULL DEFAULT '' COMMENT 'success',
  `error_message` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMessage',
  `feedback_rating` INT NOT NULL DEFAULT '' COMMENT 'feedbackRating',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- analytics_report (12 fields)
-- ----------------------------
DROP TABLE IF EXISTS `analytics_report`;
CREATE TABLE `analytics_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `report_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'reportId',
  `title` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'title',
  `question` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'question',
  `sql_text` TEXT NOT NULL DEFAULT '' COMMENT 'sqlText',
  `markdown` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'markdown',
  `chart_options_json` TEXT NOT NULL DEFAULT '' COMMENT 'chartOptionsJson',
  `row_count` BIGINT NOT NULL DEFAULT '' COMMENT 'rowCount',
  `duration_ms` BIGINT NOT NULL DEFAULT '' COMMENT 'durationMs',
  `format` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'format',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- audit_log (17 fields)
-- ----------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `trace_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'traceId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'username',
  `user_ip` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'userIp',
  `user_agent` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'userAgent',
  `action` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'action',
  `resource_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'resourceType',
  `resource_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'resourceId',
  `method` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'method',
  `path` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'path',
  `request_body` TEXT NOT NULL DEFAULT '' COMMENT 'requestBody',
  `response_status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'responseStatus',
  `result` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'result',
  `error_msg` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMsg',
  `duration_ms` INT NOT NULL DEFAULT '' COMMENT 'durationMs',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- audit_log_full (17 fields)
-- ----------------------------
DROP TABLE IF EXISTS `audit_log_full`;
CREATE TABLE `audit_log_full` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `trace_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'traceId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'username',
  `user_ip` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'userIp',
  `user_agent` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'userAgent',
  `action` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'action',
  `resource_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'resourceType',
  `resource_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'resourceId',
  `method` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'method',
  `path` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'path',
  `request_body` TEXT NOT NULL DEFAULT '' COMMENT 'requestBody',
  `response_status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'responseStatus',
  `result` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'result',
  `error_msg` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMsg',
  `duration_ms` INT NOT NULL DEFAULT '' COMMENT 'durationMs',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- auth_login_log (8 fields)
-- ----------------------------
DROP TABLE IF EXISTS `auth_login_log`;
CREATE TABLE `auth_login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'username',
  `ip` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'ip',
  `user_agent` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'userAgent',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `message` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'message',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- auth_refresh_token (6 fields)
-- ----------------------------
DROP TABLE IF EXISTS `auth_refresh_token`;
CREATE TABLE `auth_refresh_token` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `token` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'token',
  `expires_at` DATETIME NULL DEFAULT NULL COMMENT 'expiresAt',
  `revoked` INT NOT NULL DEFAULT '' COMMENT 'revoked',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- billing_record (14 fields)
-- ----------------------------
DROP TABLE IF EXISTS `billing_record`;
CREATE TABLE `billing_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `record_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'recordId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `license_id` BIGINT NOT NULL DEFAULT '' COMMENT 'licenseId',
  `model_entry_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'modelEntryId',
  `record_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'recordType',
  `amount_cents` BIGINT NOT NULL DEFAULT '' COMMENT 'amountCents',
  `currency` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'currency',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `payment_method` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'paymentMethod',
  `external_transaction_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'externalTransactionId',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- chat_message (10 fields)
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `session_id` BIGINT NOT NULL DEFAULT '' COMMENT 'sessionId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `role` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'role',
  `content` TEXT NOT NULL DEFAULT '' COMMENT 'content',
  `tokens` INT NOT NULL DEFAULT '' COMMENT 'tokens',
  `finish_reason` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'finishReason',
  `error_message` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMessage',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- chat_session (13 fields)
-- ----------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `title` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'title',
  `model` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'model',
  `system_prompt` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'systemPrompt',
  `temperature` DECIMAL(18,2) NOT NULL DEFAULT '' COMMENT 'temperature',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `message_count` INT NOT NULL DEFAULT '' COMMENT 'messageCount',
  `last_message_at` DATETIME NULL DEFAULT NULL COMMENT 'lastMessageAt',
  `tenant_id` BIGINT NOT NULL DEFAULT '' COMMENT 'tenantId',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- cluster_node (20 fields)
-- ----------------------------
DROP TABLE IF EXISTS `cluster_node`;
CREATE TABLE `cluster_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `node_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'nodeId',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `address` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'address',
  `region` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'region',
  `zone` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'zone',
  `capabilities` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'capabilities',
  `total_cores` INT NOT NULL DEFAULT '' COMMENT 'totalCores',
  `total_memory_mb` BIGINT NOT NULL DEFAULT '' COMMENT 'totalMemoryMb',
  `total_gpus` INT NOT NULL DEFAULT '' COMMENT 'totalGpus',
  `cpu_usage` DOUBLE NOT NULL DEFAULT '' COMMENT 'cpuUsage',
  `memory_usage` DOUBLE NOT NULL DEFAULT '' COMMENT 'memoryUsage',
  `gpu_usage` DOUBLE NOT NULL DEFAULT '' COMMENT 'gpuUsage',
  `active_tasks` INT NOT NULL DEFAULT '' COMMENT 'activeTasks',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `is_leader` TINYINT(1) NOT NULL DEFAULT '' COMMENT 'isLeader',
  `last_heartbeat` DATETIME NULL DEFAULT NULL COMMENT 'lastHeartbeat',
  `started_at` DATETIME NOT NULL DEFAULT '' COMMENT 'startedAt',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- collab_member (6 fields)
-- ----------------------------
DROP TABLE IF EXISTS `collab_member`;
CREATE TABLE `collab_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `collab_id` BIGINT NOT NULL DEFAULT '' COMMENT 'collabId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `role` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'role',
  `joined_at` DATETIME NOT NULL DEFAULT '' COMMENT 'joinedAt',
  `last_active_at` DATETIME NULL DEFAULT NULL COMMENT 'lastActiveAt',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- collab_message (11 fields)
-- ----------------------------
DROP TABLE IF EXISTS `collab_message`;
CREATE TABLE `collab_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `room_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'roomId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'username',
  `nickname` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'nickname',
  `type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'type',
  `content` TEXT NOT NULL DEFAULT '' COMMENT 'content',
  `metadata` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'metadata',
  `client_msg_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'clientMsgId',
  `broadcast` INT NOT NULL DEFAULT '' COMMENT 'broadcast',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- collab_participant (14 fields)
-- ----------------------------
DROP TABLE IF EXISTS `collab_participant`;
CREATE TABLE `collab_participant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `room_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'roomId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'username',
  `nickname` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'nickname',
  `avatar` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'avatar',
  `role` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'role',
  `cursor_x` INT NOT NULL DEFAULT '' COMMENT 'cursorX',
  `cursor_y` INT NOT NULL DEFAULT '' COMMENT 'cursorY',
  `selection_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'selectionId',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `joined_at` DATETIME NOT NULL DEFAULT '' COMMENT 'joinedAt',
  `left_at` DATETIME NOT NULL DEFAULT '' COMMENT 'leftAt',
  `last_heartbeat` DATETIME NULL DEFAULT NULL COMMENT 'lastHeartbeat',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- collab_room (14 fields)
-- ----------------------------
DROP TABLE IF EXISTS `collab_room`;
CREATE TABLE `collab_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `room_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'roomId',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'type',
  `owner_id` BIGINT NOT NULL DEFAULT '' COMMENT 'ownerId',
  `owner_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'ownerName',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `is_public` INT NOT NULL DEFAULT '' COMMENT 'isPublic',
  `max_participants` INT NOT NULL DEFAULT '' COMMENT 'maxParticipants',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `current_participants` INT NOT NULL DEFAULT '' COMMENT 'currentParticipants',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `last_activity_at` DATETIME NULL DEFAULT NULL COMMENT 'lastActivityAt',
  `closed_at` DATETIME NOT NULL DEFAULT '' COMMENT 'closedAt',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- collab_session (9 fields)
-- ----------------------------
DROP TABLE IF EXISTS `collab_session`;
CREATE TABLE `collab_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `session_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'sessionId',
  `owner_id` BIGINT NOT NULL DEFAULT '' COMMENT 'ownerId',
  `title` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'title',
  `max_users` INT NOT NULL DEFAULT '' COMMENT 'maxUsers',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- dashboard_metric (6 fields)
-- ----------------------------
DROP TABLE IF EXISTS `dashboard_metric`;
CREATE TABLE `dashboard_metric` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `metric` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'metric',
  `dimension` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'dimension',
  `value` DOUBLE NOT NULL DEFAULT '' COMMENT 'value',
  `tags` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'tags',
  `timestamp` DATETIME NOT NULL DEFAULT '' COMMENT 'timestamp',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- data_source (20 fields)
-- ----------------------------
DROP TABLE IF EXISTS `data_source`;
CREATE TABLE `data_source` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'type',
  `jdbc_url` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'jdbcUrl',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'username',
  `password` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'password',
  `driver_class` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'driverClass',
  `pool_size` INT NOT NULL DEFAULT '' COMMENT 'poolSize',
  `min_idle` INT NOT NULL DEFAULT '' COMMENT 'minIdle',
  `max_lifetime` INT NOT NULL DEFAULT '' COMMENT 'maxLifetime',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `test_status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'testStatus',
  `test_message` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'testMessage',
  `last_test_at` DATETIME NULL DEFAULT NULL COMMENT 'lastTestAt',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `tags` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'tags',
  `created_by` BIGINT NOT NULL DEFAULT '' COMMENT 'createdBy',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- document (16 fields)
-- ----------------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `kb_id` BIGINT NOT NULL DEFAULT '' COMMENT 'kbId',
  `owner_id` BIGINT NOT NULL DEFAULT '' COMMENT 'ownerId',
  `title` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'title',
  `source_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'sourceType',
  `source_uri` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'sourceUri',
  `content` TEXT NOT NULL DEFAULT '' COMMENT 'content',
  `size_bytes` BIGINT NOT NULL DEFAULT '' COMMENT 'sizeBytes',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `error_msg` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMsg',
  `chunk_count` INT NOT NULL DEFAULT '' COMMENT 'chunkCount',
  `checksum` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'checksum',
  `tags` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'tags',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- document_chunk (16 fields)
-- ----------------------------
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `doc_id` BIGINT NOT NULL DEFAULT '' COMMENT 'docId',
  `kb_id` BIGINT NOT NULL DEFAULT '' COMMENT 'kbId',
  `owner_id` BIGINT NOT NULL DEFAULT '' COMMENT 'ownerId',
  `chunk_index` INT NOT NULL DEFAULT '' COMMENT 'chunkIndex',
  `content` TEXT NOT NULL DEFAULT '' COMMENT 'content',
  `embedding` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'embedding',
  `dim` INT NOT NULL DEFAULT '' COMMENT 'dim',
  `char_count` INT NOT NULL DEFAULT '' COMMENT 'charCount',
  `start_pos` INT NOT NULL DEFAULT '' COMMENT 'startPos',
  `end_pos` INT NOT NULL DEFAULT '' COMMENT 'endPos',
  `access_count` INT NOT NULL DEFAULT '' COMMENT 'accessCount',
  `last_access_at` DATETIME NULL DEFAULT NULL COMMENT 'lastAccessAt',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- function_call_log (12 fields)
-- ----------------------------
DROP TABLE IF EXISTS `function_call_log`;
CREATE TABLE `function_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `session_id` BIGINT NOT NULL DEFAULT '' COMMENT 'sessionId',
  `tool_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'toolName',
  `arguments` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'arguments',
  `result` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'result',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `error_msg` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMsg',
  `duration_ms` INT NOT NULL DEFAULT '' COMMENT 'durationMs',
  `ip` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'ip',
  `user_agent` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'userAgent',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- function_tool (15 fields)
-- ----------------------------
DROP TABLE IF EXISTS `function_tool`;
CREATE TABLE `function_tool` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `display_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'displayName',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `category` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'category',
  `scope` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'scope',
  `owner_id` BIGINT NOT NULL DEFAULT '' COMMENT 'ownerId',
  `parameters` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'parameters',
  `endpoint` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'endpoint',
  `http_method` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'httpMethod',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `tags` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'tags',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- kb_chunk (14 fields)
-- ----------------------------
DROP TABLE IF EXISTS `kb_chunk`;
CREATE TABLE `kb_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `chunk_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'chunkId',
  `doc_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'docId',
  `kb_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'kbId',
  `seq` INT NOT NULL DEFAULT '' COMMENT 'seq',
  `content` TEXT NOT NULL DEFAULT '' COMMENT 'content',
  `char_count` INT NOT NULL DEFAULT '' COMMENT 'charCount',
  `token_count` INT NOT NULL DEFAULT '' COMMENT 'tokenCount',
  `embedding` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'embedding',
  `embedding_model` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'embeddingModel',
  `keywords` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'keywords',
  `summary` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'summary',
  `location` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'location',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- kb_document (19 fields)
-- ----------------------------
DROP TABLE IF EXISTS `kb_document`;
CREATE TABLE `kb_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `doc_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'docId',
  `kb_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'kbId',
  `filename` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'filename',
  `mime_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'mimeType',
  `size_bytes` BIGINT NOT NULL DEFAULT '' COMMENT 'sizeBytes',
  `sha256` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'sha256',
  `file_path` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'filePath',
  `source` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'source',
  `source_url` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'sourceUrl',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `chunk_count` INT NOT NULL DEFAULT '' COMMENT 'chunkCount',
  `embedding_count` INT NOT NULL DEFAULT '' COMMENT 'embeddingCount',
  `error` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'error',
  `tags` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'tags',
  `owner_id` BIGINT NOT NULL DEFAULT '' COMMENT 'ownerId',
  `is_public` TINYINT(1) NOT NULL DEFAULT '' COMMENT 'isPublic',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- kb_permission (7 fields)
-- ----------------------------
DROP TABLE IF EXISTS `kb_permission`;
CREATE TABLE `kb_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `kb_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'kbId',
  `subject_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'subjectType',
  `subject_id` BIGINT NOT NULL DEFAULT '' COMMENT 'subjectId',
  `permission` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'permission',
  `grant_by` BIGINT NOT NULL DEFAULT '' COMMENT 'grantBy',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- kg_entity (12 fields)
-- ----------------------------
DROP TABLE IF EXISTS `kg_entity`;
CREATE TABLE `kg_entity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `entity_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'entityType',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `aliases` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'aliases',
  `importance` INT NOT NULL DEFAULT '' COMMENT 'importance',
  `source` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'source',
  `ref_count` INT NOT NULL DEFAULT '' COMMENT 'refCount',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- kg_relation (12 fields)
-- ----------------------------
DROP TABLE IF EXISTS `kg_relation`;
CREATE TABLE `kg_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `from_entity` BIGINT NOT NULL DEFAULT '' COMMENT 'fromEntity',
  `to_entity` BIGINT NOT NULL DEFAULT '' COMMENT 'toEntity',
  `relation_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'relationType',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `weight` DECIMAL(18,2) NOT NULL DEFAULT '' COMMENT 'weight',
  `source` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'source',
  `ref_count` INT NOT NULL DEFAULT '' COMMENT 'refCount',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- knowledge_base (12 fields)
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `owner_id` BIGINT NOT NULL DEFAULT '' COMMENT 'ownerId',
  `tenant_id` BIGINT NOT NULL DEFAULT '' COMMENT 'tenantId',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `visibility` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'visibility',
  `doc_count` INT NOT NULL DEFAULT '' COMMENT 'docCount',
  `chunk_count` INT NOT NULL DEFAULT '' COMMENT 'chunkCount',
  `tags` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'tags',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- metric_snapshot (6 fields)
-- ----------------------------
DROP TABLE IF EXISTS `metric_snapshot`;
CREATE TABLE `metric_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `service` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'service',
  `metric_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'metricName',
  `metric_value` DECIMAL(18,2) NOT NULL DEFAULT '' COMMENT 'metricValue',
  `tags` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'tags',
  `recorded_at` DATETIME NOT NULL DEFAULT '' COMMENT 'recordedAt',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- model_battle_log (16 fields)
-- ----------------------------
DROP TABLE IF EXISTS `model_battle_log`;
CREATE TABLE `model_battle_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `battle_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'battleId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `model_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'modelId',
  `model_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'modelCode',
  `prompt` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'prompt',
  `response` TEXT NOT NULL DEFAULT '' COMMENT 'response',
  `prompt_tokens` INT NOT NULL DEFAULT '' COMMENT 'promptTokens',
  `completion_tokens` INT NOT NULL DEFAULT '' COMMENT 'completionTokens',
  `latency_ms` INT NOT NULL DEFAULT '' COMMENT 'latencyMs',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `error_msg` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMsg',
  `score` INT NOT NULL DEFAULT '' COMMENT 'score',
  `judge_model` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'judgeModel',
  `judge_reason` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'judgeReason',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- model_config (17 fields)
-- ----------------------------
DROP TABLE IF EXISTS `model_config`;
CREATE TABLE `model_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `provider_id` BIGINT NOT NULL DEFAULT '' COMMENT 'providerId',
  `model_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'modelCode',
  `display_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'displayName',
  `max_context` INT NOT NULL DEFAULT '' COMMENT 'maxContext',
  `max_output` INT NOT NULL DEFAULT '' COMMENT 'maxOutput',
  `input_price` DECIMAL(18,2) NOT NULL DEFAULT '' COMMENT 'inputPrice',
  `output_price` DECIMAL(18,2) NOT NULL DEFAULT '' COMMENT 'outputPrice',
  `supports_vision` INT NOT NULL DEFAULT '' COMMENT 'supportsVision',
  `supports_tools` INT NOT NULL DEFAULT '' COMMENT 'supportsTools',
  `supports_stream` INT NOT NULL DEFAULT '' COMMENT 'supportsStream',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `sort` INT NOT NULL DEFAULT '' COMMENT 'sort',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- model_license (14 fields)
-- ----------------------------
DROP TABLE IF EXISTS `model_license`;
CREATE TABLE `model_license` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `license_key` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'licenseKey',
  `model_entry_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'modelEntryId',
  `model_version_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'modelVersionId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `license_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'licenseType',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `quota_calls` BIGINT NOT NULL DEFAULT '' COMMENT 'quotaCalls',
  `used_calls` BIGINT NOT NULL DEFAULT '' COMMENT 'usedCalls',
  `start_at` DATETIME NOT NULL DEFAULT '' COMMENT 'startAt',
  `expire_at` DATETIME NULL DEFAULT NULL COMMENT 'expireAt',
  `price_cents` BIGINT NOT NULL DEFAULT '' COMMENT 'priceCents',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- model_provider (12 fields)
-- ----------------------------
DROP TABLE IF EXISTS `model_provider`;
CREATE TABLE `model_provider` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'code',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `base_url` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'baseUrl',
  `api_key` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'apiKey',
  `protocol` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'protocol',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `sort` INT NOT NULL DEFAULT '' COMMENT 'sort',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- model_quota (10 fields)
-- ----------------------------
DROP TABLE IF EXISTS `model_quota`;
CREATE TABLE `model_quota` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `model_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'modelId',
  `quota_date` DATETIME NOT NULL DEFAULT '' COMMENT 'quotaDate',
  `used_tokens` BIGINT NOT NULL DEFAULT '' COMMENT 'usedTokens',
  `used_requests` INT NOT NULL DEFAULT '' COMMENT 'usedRequests',
  `limit_tokens` BIGINT NOT NULL DEFAULT '' COMMENT 'limitTokens',
  `limit_requests` INT NOT NULL DEFAULT '' COMMENT 'limitRequests',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- model_version (17 fields)
-- ----------------------------
DROP TABLE IF EXISTS `model_version`;
CREATE TABLE `model_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `version_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'versionId',
  `model_entry_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'modelEntryId',
  `version` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'version',
  `changelog` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'changelog',
  `file_path` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'filePath',
  `size_bytes` BIGINT NOT NULL DEFAULT '' COMMENT 'sizeBytes',
  `sha256` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'sha256',
  `input_schema` TEXT NOT NULL DEFAULT '' COMMENT 'inputSchema',
  `output_schema` TEXT NOT NULL DEFAULT '' COMMENT 'outputSchema',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `is_latest` TINYINT(1) NOT NULL DEFAULT '' COMMENT 'isLatest',
  `uploader_id` BIGINT NOT NULL DEFAULT '' COMMENT 'uploaderId',
  `backward_compatible` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'backwardCompatible',
  `metadata` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'metadata',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- moderation_record (15 fields)
-- ----------------------------
DROP TABLE IF EXISTS `moderation_record`;
CREATE TABLE `moderation_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `trace_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'traceId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'username',
  `content_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'contentType',
  `content_hash` TEXT NOT NULL DEFAULT '' COMMENT 'contentHash',
  `content_size` BIGINT NOT NULL DEFAULT '' COMMENT 'contentSize',
  `content_url` TEXT NOT NULL DEFAULT '' COMMENT 'contentUrl',
  `moderation_status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'moderationStatus',
  `risk_level` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'riskLevel',
  `risk_labels` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'riskLabels',
  `risk_score` DECIMAL(18,2) NOT NULL DEFAULT '' COMMENT 'riskScore',
  `moderator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'moderator',
  `rejection_reason` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'rejectionReason',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- multimedia_file (28 fields)
-- ----------------------------
DROP TABLE IF EXISTS `multimedia_file`;
CREATE TABLE `multimedia_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `file_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'fileId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'username',
  `file_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'fileName',
  `original_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'originalName',
  `file_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'fileType',
  `mime_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'mimeType',
  `file_size` BIGINT NOT NULL DEFAULT '' COMMENT 'fileSize',
  `file_hash` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'fileHash',
  `storage_path` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'storagePath',
  `storage_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'storageType',
  `encrypted` INT NOT NULL DEFAULT '' COMMENT 'encrypted',
  `duration_ms` BIGINT NOT NULL DEFAULT '' COMMENT 'durationMs',
  `width` INT NOT NULL DEFAULT '' COMMENT 'width',
  `height` INT NOT NULL DEFAULT '' COMMENT 'height',
  `bitrate` INT NOT NULL DEFAULT '' COMMENT 'bitrate',
  `sample_rate` INT NOT NULL DEFAULT '' COMMENT 'sampleRate',
  `channels` INT NOT NULL DEFAULT '' COMMENT 'channels',
  `codec` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'codec',
  `exif` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'exif',
  `moderation_status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'moderationStatus',
  `moderation_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'moderationId',
  `watermarked` INT NOT NULL DEFAULT '' COMMENT 'watermarked',
  `is_public` INT NOT NULL DEFAULT '' COMMENT 'isPublic',
  `access_count` INT NOT NULL DEFAULT '' COMMENT 'accessCount',
  `expire_at` DATETIME NULL DEFAULT NULL COMMENT 'expireAt',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- notification (8 fields)
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'type',
  `title` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'title',
  `content` TEXT NOT NULL DEFAULT '' COMMENT 'content',
  `is_read` INT NOT NULL DEFAULT '' COMMENT 'isRead',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- oauth_app_config (12 fields)
-- ----------------------------
DROP TABLE IF EXISTS `oauth_app_config`;
CREATE TABLE `oauth_app_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `platform` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'platform',
  `app_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'appType',
  `app_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'appId',
  `app_secret` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'appSecret',
  `public_key` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'publicKey',
  `redirect_uri` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'redirectUri',
  `scopes` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'scopes',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `extra_config` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'extraConfig',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- oauth_binding (14 fields)
-- ----------------------------
DROP TABLE IF EXISTS `oauth_binding`;
CREATE TABLE `oauth_binding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `platform` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'platform',
  `app_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'appType',
  `openid` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'openid',
  `unionid` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'unionid',
  `nickname` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'nickname',
  `avatar` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'avatar',
  `access_token` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'accessToken',
  `refresh_token` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'refreshToken',
  `token_expires_at` DATETIME NULL DEFAULT NULL COMMENT 'tokenExpiresAt',
  `raw_data` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'rawData',
  `bound_at` DATETIME NOT NULL DEFAULT '' COMMENT 'boundAt',
  `last_login_at` DATETIME NULL DEFAULT NULL COMMENT 'lastLoginAt',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- pipeline_log (19 fields)
-- ----------------------------
DROP TABLE IF EXISTS `pipeline_log`;
CREATE TABLE `pipeline_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `session_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'sessionId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `client_ip` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'clientIp',
  `input_text` TEXT NOT NULL DEFAULT '' COMMENT 'inputText',
  `input_modality` TEXT NOT NULL DEFAULT '' COMMENT 'inputModality',
  `intent` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'intent',
  `output_text` TEXT NOT NULL DEFAULT '' COMMENT 'outputText',
  `output_tokens` INT NOT NULL DEFAULT '' COMMENT 'outputTokens',
  `compute_device` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'computeDevice',
  `compute_mode` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'computeMode',
  `total_cost_ms` BIGINT NOT NULL DEFAULT '' COMMENT 'totalCostMs',
  `stage_costs` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'stageCosts',
  `risk_level` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'riskLevel',
  `needs_review` TINYINT(1) NOT NULL DEFAULT '' COMMENT 'needsReview',
  `rag_hits` INT NOT NULL DEFAULT '' COMMENT 'ragHits',
  `tool_calls` INT NOT NULL DEFAULT '' COMMENT 'toolCalls',
  `error_message` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMessage',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- pipeline_node_log (14 fields)
-- ----------------------------
DROP TABLE IF EXISTS `pipeline_node_log`;
CREATE TABLE `pipeline_node_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `run_id` BIGINT NOT NULL DEFAULT '' COMMENT 'runId',
  `node_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'nodeId',
  `node_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'nodeType',
  `node_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'nodeName',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `start_time` DATETIME NOT NULL DEFAULT '' COMMENT 'startTime',
  `end_time` DATETIME NOT NULL DEFAULT '' COMMENT 'endTime',
  `duration_ms` BIGINT NOT NULL DEFAULT '' COMMENT 'durationMs',
  `input_rows` INT NOT NULL DEFAULT '' COMMENT 'inputRows',
  `output_rows` INT NOT NULL DEFAULT '' COMMENT 'outputRows',
  `output_preview` TEXT NOT NULL DEFAULT '' COMMENT 'outputPreview',
  `error_message` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMessage',
  `config_snapshot` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'configSnapshot',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- pipeline_run (13 fields)
-- ----------------------------
DROP TABLE IF EXISTS `pipeline_run`;
CREATE TABLE `pipeline_run` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `workflow_id` BIGINT NOT NULL DEFAULT '' COMMENT 'workflowId',
  `workflow_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'workflowName',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `trigger_by` BIGINT NOT NULL DEFAULT '' COMMENT 'triggerBy',
  `trigger_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'triggerType',
  `definition_snapshot` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'definitionSnapshot',
  `start_time` DATETIME NOT NULL DEFAULT '' COMMENT 'startTime',
  `end_time` DATETIME NOT NULL DEFAULT '' COMMENT 'endTime',
  `duration_ms` BIGINT NOT NULL DEFAULT '' COMMENT 'durationMs',
  `error_message` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMessage',
  `result_summary` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'resultSummary',
  `create_time` DATETIME NOT NULL DEFAULT '' COMMENT 'createTime',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- pipeline_workflow (11 fields)
-- ----------------------------
DROP TABLE IF EXISTS `pipeline_workflow`;
CREATE TABLE `pipeline_workflow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `definition` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'definition',
  `version` INT NOT NULL DEFAULT '' COMMENT 'version',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `create_by` BIGINT NOT NULL DEFAULT '' COMMENT 'createBy',
  `update_by` BIGINT NOT NULL DEFAULT '' COMMENT 'updateBy',
  `create_time` DATETIME NOT NULL DEFAULT '' COMMENT 'createTime',
  `update_time` DATETIME NOT NULL DEFAULT '' COMMENT 'updateTime',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- pipeline_workflow_version (7 fields)
-- ----------------------------
DROP TABLE IF EXISTS `pipeline_workflow_version`;
CREATE TABLE `pipeline_workflow_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `workflow_id` BIGINT NOT NULL DEFAULT '' COMMENT 'workflowId',
  `version` INT NOT NULL DEFAULT '' COMMENT 'version',
  `definition` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'definition',
  `change_log` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'changeLog',
  `create_by` BIGINT NOT NULL DEFAULT '' COMMENT 'createBy',
  `create_time` DATETIME NOT NULL DEFAULT '' COMMENT 'createTime',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- plugin (19 fields)
-- ----------------------------
DROP TABLE IF EXISTS `plugin`;
CREATE TABLE `plugin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `display_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'displayName',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `version` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'version',
  `author` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'author',
  `category` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'category',
  `scope` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'scope',
  `owner_id` BIGINT NOT NULL DEFAULT '' COMMENT 'ownerId',
  `icon` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'icon',
  `entry` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'entry',
  `plugin_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'pluginType',
  `config` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'config',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `downloads` INT NOT NULL DEFAULT '' COMMENT 'downloads',
  `rating` DECIMAL(18,2) NOT NULL DEFAULT '' COMMENT 'rating',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- prompt_template (13 fields)
-- ----------------------------
DROP TABLE IF EXISTS `prompt_template`;
CREATE TABLE `prompt_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `category` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'category',
  `content` TEXT NOT NULL DEFAULT '' COMMENT 'content',
  `variables` TEXT NOT NULL DEFAULT '' COMMENT 'variables',
  `creator_id` BIGINT NOT NULL DEFAULT '' COMMENT 'creatorId',
  `creator_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'creatorName',
  `is_public` TINYINT(1) NOT NULL DEFAULT '' COMMENT 'isPublic',
  `use_count` INT NOT NULL DEFAULT '' COMMENT 'useCount',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- push_message (15 fields)
-- ----------------------------
DROP TABLE IF EXISTS `push_message`;
CREATE TABLE `push_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `message_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'messageId',
  `title` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'title',
  `body` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'body',
  `icon` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'icon',
  `click_action` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'clickAction',
  `data` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'data',
  `target_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'targetType',
  `target_value` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'targetValue',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `success_count` INT NOT NULL DEFAULT '' COMMENT 'successCount',
  `failure_count` INT NOT NULL DEFAULT '' COMMENT 'failureCount',
  `error` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'error',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- push_subscription (12 fields)
-- ----------------------------
DROP TABLE IF EXISTS `push_subscription`;
CREATE TABLE `push_subscription` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `subscription_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'subscriptionId',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `platform` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'platform',
  `endpoint` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'endpoint',
  `p256dh_key` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'p256dhKey',
  `auth_key` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'authKey',
  `user_agent` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'userAgent',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `last_active_at` DATETIME NULL DEFAULT NULL COMMENT 'lastActiveAt',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- raft_log (8 fields)
-- ----------------------------
DROP TABLE IF EXISTS `raft_log`;
CREATE TABLE `raft_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `term` BIGINT NOT NULL DEFAULT '' COMMENT 'term',
  `log_index` BIGINT NOT NULL DEFAULT '' COMMENT 'logIndex',
  `node_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'nodeId',
  `command` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'command',
  `committed` TINYINT(1) NOT NULL DEFAULT '' COMMENT 'committed',
  `committed_at` DATETIME NOT NULL DEFAULT '' COMMENT 'committedAt',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- sensitive_word (7 fields)
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `word` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'word',
  `category` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'category',
  `level` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'level',
  `action` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'action',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- sys_role (9 fields)
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'code',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `description` TEXT NOT NULL DEFAULT '' COMMENT 'description',
  `sort` INT NOT NULL DEFAULT '' COMMENT 'sort',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- sys_user (33 fields)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `username` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'username',
  `password` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'password',
  `nickname` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'nickname',
  `email` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'email',
  `phone` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'phone',
  `avatar` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'avatar',
  `gender` INT NOT NULL DEFAULT '' COMMENT 'gender',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `last_login_ip` VARCHAR(255) NULL DEFAULT NULL COMMENT 'lastLoginIp',
  `last_login_at` DATETIME NULL DEFAULT NULL COMMENT 'lastLoginAt',
  `tenant_id` BIGINT NOT NULL DEFAULT '' COMMENT 'tenantId',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'remark',
  `wechat_openid` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'wechatOpenid',
  `wechat_unionid` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'wechatUnionid',
  `wechat_nickname` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'wechatNickname',
  `wechat_avatar` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'wechatAvatar',
  `wechat_bound_at` DATETIME NOT NULL DEFAULT '' COMMENT 'wechatBoundAt',
  `qq_openid` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'qqOpenid',
  `qq_unionid` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'qqUnionid',
  `qq_nickname` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'qqNickname',
  `qq_avatar` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'qqAvatar',
  `qq_bound_at` DATETIME NOT NULL DEFAULT '' COMMENT 'qqBoundAt',
  `alipay_openid` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'alipayOpenid',
  `alipay_user_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'alipayUserId',
  `alipay_nickname` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'alipayNickname',
  `alipay_avatar` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'alipayAvatar',
  `alipay_bound_at` DATETIME NOT NULL DEFAULT '' COMMENT 'alipayBoundAt',
  `created_by` BIGINT NOT NULL DEFAULT '' COMMENT 'createdBy',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NOT NULL DEFAULT '' COMMENT 'updatedBy',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- sys_user_role (2 fields)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `role_id` BIGINT NOT NULL DEFAULT '' COMMENT 'roleId',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- tenant (17 fields)
-- ----------------------------
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'code',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `plan` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'plan',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `max_users` INT NOT NULL DEFAULT '' COMMENT 'maxUsers',
  `max_models` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'maxModels',
  `qps_limit` INT NOT NULL DEFAULT '' COMMENT 'qpsLimit',
  `monthly_quota` BIGINT NOT NULL DEFAULT '' COMMENT 'monthlyQuota',
  `used_quota` BIGINT NOT NULL DEFAULT '' COMMENT 'usedQuota',
  `expire_at` DATETIME NULL DEFAULT NULL COMMENT 'expireAt',
  `contact_email` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'contactEmail',
  `contact_phone` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'contactPhone',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'remark',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- training_checkpoint (14 fields)
-- ----------------------------
DROP TABLE IF EXISTS `training_checkpoint`;
CREATE TABLE `training_checkpoint` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `task_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'taskId',
  `checkpoint_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'checkpointId',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `epoch` INT NOT NULL DEFAULT '' COMMENT 'epoch',
  `step` INT NOT NULL DEFAULT '' COMMENT 'step',
  `file_path` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'filePath',
  `size_bytes` BIGINT NOT NULL DEFAULT '' COMMENT 'sizeBytes',
  `sha256` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'sha256',
  `val_loss` DOUBLE NOT NULL DEFAULT '' COMMENT 'valLoss',
  `accuracy` DOUBLE NOT NULL DEFAULT '' COMMENT 'accuracy',
  `tags` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'tags',
  `metadata` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'metadata',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- training_job (20 fields)
-- ----------------------------
DROP TABLE IF EXISTS `training_job`;
CREATE TABLE `training_job` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `task_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'taskId',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `model` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'model',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `total_epochs` INT NOT NULL DEFAULT '' COMMENT 'totalEpochs',
  `current_epoch` INT NOT NULL DEFAULT '' COMMENT 'currentEpoch',
  `current_step` INT NOT NULL DEFAULT '' COMMENT 'currentStep',
  `start_time_ms` BIGINT NOT NULL DEFAULT '' COMMENT 'startTimeMs',
  `end_time_ms` BIGINT NOT NULL DEFAULT '' COMMENT 'endTimeMs',
  `config` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'config',
  `error` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'error',
  `owner_id` BIGINT NOT NULL DEFAULT '' COMMENT 'ownerId',
  `tags` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'tags',
  `last_loss` DOUBLE NULL DEFAULT NULL COMMENT 'lastLoss',
  `last_val_loss` DOUBLE NULL DEFAULT NULL COMMENT 'lastValLoss',
  `last_accuracy` DOUBLE NULL DEFAULT NULL COMMENT 'lastAccuracy',
  `total_steps` INT NOT NULL DEFAULT '' COMMENT 'totalSteps',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- training_metric (10 fields)
-- ----------------------------
DROP TABLE IF EXISTS `training_metric`;
CREATE TABLE `training_metric` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `task_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'taskId',
  `epoch` INT NOT NULL DEFAULT '' COMMENT 'epoch',
  `step` INT NOT NULL DEFAULT '' COMMENT 'step',
  `loss` DOUBLE NOT NULL DEFAULT '' COMMENT 'loss',
  `val_loss` DOUBLE NOT NULL DEFAULT '' COMMENT 'valLoss',
  `accuracy` DOUBLE NOT NULL DEFAULT '' COMMENT 'accuracy',
  `learning_rate` DOUBLE NOT NULL DEFAULT '' COMMENT 'learningRate',
  `elapsed_ms` BIGINT NOT NULL DEFAULT '' COMMENT 'elapsedMs',
  `timestamp` DATETIME NOT NULL DEFAULT '' COMMENT 'timestamp',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- training_task (19 fields)
-- ----------------------------
DROP TABLE IF EXISTS `training_task`;
CREATE TABLE `training_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `model_name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'modelName',
  `corpus_path` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'corpusPath',
  `n_layer` INT NOT NULL DEFAULT '' COMMENT 'nLayer',
  `n_head` INT NOT NULL DEFAULT '' COMMENT 'nHead',
  `n_embd` INT NOT NULL DEFAULT '' COMMENT 'nEmbd',
  `block_size` INT NOT NULL DEFAULT '' COMMENT 'blockSize',
  `max_iters` INT NOT NULL DEFAULT '' COMMENT 'maxIters',
  `batch_size` INT NOT NULL DEFAULT '' COMMENT 'batchSize',
  `learning_rate` DOUBLE NOT NULL DEFAULT '' COMMENT 'learningRate',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `progress` INT NOT NULL DEFAULT '' COMMENT 'progress',
  `current_loss` DOUBLE NOT NULL DEFAULT '' COMMENT 'currentLoss',
  `current_iter` INT NOT NULL DEFAULT '' COMMENT 'currentIter',
  `error_message` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'errorMessage',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `completed_at` DATETIME NOT NULL DEFAULT '' COMMENT 'completedAt',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- unionid_relations (7 fields)
-- ----------------------------
DROP TABLE IF EXISTS `unionid_relations`;
CREATE TABLE `unionid_relations` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `unionid` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'unionid',
  `platform` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'platform',
  `first_seen_at` DATETIME NOT NULL DEFAULT '' COMMENT 'firstSeenAt',
  `last_seen_at` DATETIME NULL DEFAULT NULL COMMENT 'lastSeenAt',
  `binding_count` INT NOT NULL DEFAULT '' COMMENT 'bindingCount',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- user_api_key (13 fields)
-- ----------------------------
DROP TABLE IF EXISTS `user_api_key`;
CREATE TABLE `user_api_key` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'name',
  `key_hash` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'keyHash',
  `key_prefix` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'keyPrefix',
  `scopes` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'scopes',
  `expires_at` DATETIME NULL DEFAULT NULL COMMENT 'expiresAt',
  `last_used_at` DATETIME NULL DEFAULT NULL COMMENT 'lastUsedAt',
  `use_count` BIGINT NOT NULL DEFAULT '' COMMENT 'useCount',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- wechat_config (12 fields)
-- ----------------------------
DROP TABLE IF EXISTS `wechat_config`;
CREATE TABLE `wechat_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `app_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'appType',
  `app_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'appId',
  `app_secret` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'appSecret',
  `token` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'token',
  `aes_key` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'aesKey',
  `redirect_uri` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'redirectUri',
  `scope` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'scope',
  `enabled` INT NOT NULL DEFAULT '' COMMENT 'enabled',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'remark',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- wechat_scan_session (17 fields)
-- ----------------------------
DROP TABLE IF EXISTS `wechat_scan_session`;
CREATE TABLE `wechat_scan_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `ticket` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'ticket',
  `scene_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'sceneId',
  `status` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'status',
  `openid` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'openid',
  `unionid` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'unionid',
  `nickname` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'nickname',
  `avatar` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'avatar',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `access_token` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'accessToken',
  `refresh_token` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'refreshToken',
  `client_ip` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'clientIp',
  `user_agent` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'userAgent',
  `expires_at` DATETIME NULL DEFAULT NULL COMMENT 'expiresAt',
  `confirmed_at` DATETIME NOT NULL DEFAULT '' COMMENT 'confirmedAt',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

-- ----------------------------
-- wechat_user_binding (9 fields)
-- ----------------------------
DROP TABLE IF EXISTS `wechat_user_binding`;
CREATE TABLE `wechat_user_binding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` BIGINT NOT NULL DEFAULT '' COMMENT 'userId',
  `openid` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'openid',
  `unionid` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'unionid',
  `app_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'appType',
  `nickname` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'nickname',
  `avatar` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'avatar',
  `bound_at` DATETIME NOT NULL DEFAULT '' COMMENT 'boundAt',
  `last_login_at` DATETIME NULL DEFAULT NULL COMMENT 'lastLoginAt',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V3.5.19 auto';

