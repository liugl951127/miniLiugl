-- =====================================================
-- MiniMax Platform V3.5.8 - 77 tables
-- Auto-generated from entities (snake_case)
-- Compatible: MySQL 8.0+ / MariaDB 10.6+ / H2 MODE=MySQL
-- =====================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- ====== AdminAuditLog (admin_audit_log) ======
CREATE TABLE IF NOT EXISTS `admin_audit_log` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `actor_id` BIGINT DEFAULT 0 COMMENT 'actor_id',
    `actor_name` VARCHAR(255) DEFAULT NULL COMMENT 'actor_name',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action',
    `resource_type` VARCHAR(255) DEFAULT NULL COMMENT 'resource_type',
    `resource_id` VARCHAR(255) DEFAULT NULL COMMENT 'resource_id',
    `detail` VARCHAR(255) DEFAULT NULL COMMENT 'detail',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AuditLogFull (audit_log_full) ======
CREATE TABLE IF NOT EXISTS `audit_log_full` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `trace_id` VARCHAR(255) DEFAULT NULL COMMENT 'trace_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `user_ip` VARCHAR(255) DEFAULT NULL COMMENT 'user_ip',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action',
    `resource_type` VARCHAR(255) DEFAULT NULL COMMENT 'resource_type',
    `resource_id` VARCHAR(255) DEFAULT NULL COMMENT 'resource_id',
    `method` VARCHAR(255) DEFAULT NULL COMMENT 'method',
    `path` VARCHAR(255) DEFAULT NULL COMMENT 'path',
    `request_body` VARCHAR(255) DEFAULT NULL COMMENT 'request_body',
    `response_status` INT DEFAULT 0 COMMENT 'response_status',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg',
    `duration_ms` INT DEFAULT 0 COMMENT 'duration_ms',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AgentTask (agent_task) ======
CREATE TABLE IF NOT EXISTS `agent_task` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `task_id` VARCHAR(255) DEFAULT NULL COMMENT 'task_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `goal` VARCHAR(255) DEFAULT NULL COMMENT 'goal',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `rounds` INT DEFAULT 0 COMMENT 'rounds',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result',
    `llm_calls` INT DEFAULT 0 COMMENT 'llm_calls',
    `tool_calls` INT DEFAULT 0 COMMENT 'tool_calls',
    `total_tokens` INT DEFAULT 0 COMMENT 'total_tokens',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg',
    `latency_ms` BIGINT DEFAULT 0 COMMENT 'latency_ms',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== CollabMember (collab_member) ======
CREATE TABLE IF NOT EXISTS `collab_member` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `collab_id` BIGINT DEFAULT 0 COMMENT 'collab_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role',
    `joined_at` DATETIME DEFAULT NULL COMMENT 'joined_at',
    `last_active_at` DATETIME DEFAULT NULL COMMENT 'last_active_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== CollabSession (collab_session) ======
CREATE TABLE IF NOT EXISTS `collab_session` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `session_id` VARCHAR(255) DEFAULT NULL COMMENT 'session_id',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title',
    `max_users` INT DEFAULT 0 COMMENT 'max_users',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== KgEntity (kg_entity) ======
CREATE TABLE IF NOT EXISTS `kg_entity` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `entity_type` VARCHAR(255) DEFAULT NULL COMMENT 'entity_type',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `aliases` VARCHAR(255) DEFAULT NULL COMMENT 'aliases',
    `importance` INT DEFAULT 0 COMMENT 'importance',
    `source` VARCHAR(255) DEFAULT NULL COMMENT 'source',
    `ref_count` INT DEFAULT 0 COMMENT 'ref_count',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== KgRelation (kg_relation) ======
CREATE TABLE IF NOT EXISTS `kg_relation` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `from_entity` BIGINT DEFAULT 0 COMMENT 'from_entity',
    `to_entity` BIGINT DEFAULT 0 COMMENT 'to_entity',
    `relation_type` VARCHAR(255) DEFAULT NULL COMMENT 'relation_type',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `weight` DECIMAL(20,4) DEFAULT 0 COMMENT 'weight',
    `source` VARCHAR(255) DEFAULT NULL COMMENT 'source',
    `ref_count` INT DEFAULT 0 COMMENT 'ref_count',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== Plugin (plugin) ======
CREATE TABLE IF NOT EXISTS `plugin` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `display_name` VARCHAR(255) DEFAULT NULL COMMENT 'display_name',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `version` VARCHAR(255) DEFAULT NULL COMMENT 'version',
    `author` VARCHAR(255) DEFAULT NULL COMMENT 'author',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon',
    `entry` VARCHAR(255) DEFAULT NULL COMMENT 'entry',
    `plugin_type` VARCHAR(255) DEFAULT NULL COMMENT 'plugin_type',
    `config` VARCHAR(255) DEFAULT NULL COMMENT 'config',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `downloads` INT DEFAULT 0 COMMENT 'downloads',
    `rating` DECIMAL(20,4) DEFAULT 0 COMMENT 'rating',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AgentGroup (agent_group) ======
CREATE TABLE IF NOT EXISTS `agent_group` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `group_id` VARCHAR(255) DEFAULT NULL COMMENT 'group_id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `strategy` VARCHAR(255) DEFAULT NULL COMMENT 'strategy',
    `members_json` VARCHAR(255) DEFAULT NULL COMMENT 'members_json',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `last_run_at` DATETIME DEFAULT NULL COMMENT 'last_run_at',
    `run_count` INT DEFAULT 0 COMMENT 'run_count',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AiChatMessage (ai_chat_message) ======
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `session_id` VARCHAR(255) DEFAULT NULL COMMENT 'session_id',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content',
    `tool_code` VARCHAR(255) DEFAULT NULL COMMENT 'tool_code',
    `tool_input` VARCHAR(255) DEFAULT NULL COMMENT 'tool_input',
    `tool_output` VARCHAR(255) DEFAULT NULL COMMENT 'tool_output',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AiChatSession (ai_chat_session) ======
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `session_id` VARCHAR(255) DEFAULT NULL COMMENT 'session_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AiGenerationLog (ai_generation_log) ======
CREATE TABLE IF NOT EXISTS `ai_generation_log` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `generation_id` VARCHAR(255) DEFAULT NULL COMMENT 'generation_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `user_ip` VARCHAR(255) DEFAULT NULL COMMENT 'user_ip',
    `modality` VARCHAR(255) DEFAULT NULL COMMENT 'modality',
    `model_name` VARCHAR(255) DEFAULT NULL COMMENT 'model_name',
    `model_version` VARCHAR(255) DEFAULT NULL COMMENT 'model_version',
    `prompt` VARCHAR(255) DEFAULT NULL COMMENT 'prompt',
    `negative_prompt` VARCHAR(255) DEFAULT NULL COMMENT 'negative_prompt',
    `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters',
    `output_url` VARCHAR(255) DEFAULT NULL COMMENT 'output_url',
    `output_size` BIGINT DEFAULT 0 COMMENT 'output_size',
    `output_hash` VARCHAR(255) DEFAULT NULL COMMENT 'output_hash',
    `watermarked` INT DEFAULT 0 COMMENT 'watermarked',
    `watermark_text` VARCHAR(255) DEFAULT NULL COMMENT 'watermark_text',
    `duration_ms` INT DEFAULT 0 COMMENT 'duration_ms',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AiIntentKeyword (ai_intent_keyword) ======
CREATE TABLE IF NOT EXISTS `ai_intent_keyword` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `intent` VARCHAR(255) DEFAULT NULL COMMENT 'intent',
    `keyword` VARCHAR(255) DEFAULT NULL COMMENT 'keyword',
    `weight` INT DEFAULT 0 COMMENT 'weight',
    `is_regex` INT DEFAULT 0 COMMENT 'is_regex',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `language` VARCHAR(255) DEFAULT NULL COMMENT 'language',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AiTool (ai_tool) ======
CREATE TABLE IF NOT EXISTS `ai_tool` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `code` VARCHAR(255) DEFAULT NULL COMMENT 'code',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `builtin` INT DEFAULT 0 COMMENT 'builtin',
    `input_schema` VARCHAR(255) DEFAULT NULL COMMENT 'input_schema',
    `output_schema` VARCHAR(255) DEFAULT NULL COMMENT 'output_schema',
    `default_config` VARCHAR(255) DEFAULT NULL COMMENT 'default_config',
    `impl_type` VARCHAR(255) DEFAULT NULL COMMENT 'impl_type',
    `impl_value` VARCHAR(255) DEFAULT NULL COMMENT 'impl_value',
    `rate_limit` INT DEFAULT 0 COMMENT 'rate_limit',
    `timeout_seconds` INT DEFAULT 0 COMMENT 'timeout_seconds',
    `role_required` VARCHAR(255) DEFAULT NULL COMMENT 'role_required',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `version` VARCHAR(255) DEFAULT NULL COMMENT 'version',
    `author` VARCHAR(255) DEFAULT NULL COMMENT 'author',
    `created_by` BIGINT DEFAULT 0 COMMENT 'created_by',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AiToolInvocation (ai_tool_invocation) ======
CREATE TABLE IF NOT EXISTS `ai_tool_invocation` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `tool_code` VARCHAR(255) DEFAULT NULL COMMENT 'tool_code',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `input_json` VARCHAR(255) DEFAULT NULL COMMENT 'input_json',
    `output_json` VARCHAR(255) DEFAULT NULL COMMENT 'output_json',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message',
    `duration_ms` INT DEFAULT 0 COMMENT 'duration_ms',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent',
    `data_source_id` BIGINT DEFAULT 0 COMMENT 'data_source_id',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AuditLog (audit_log) ======
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `trace_id` VARCHAR(255) DEFAULT NULL COMMENT 'trace_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `user_ip` VARCHAR(255) DEFAULT NULL COMMENT 'user_ip',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action',
    `resource_type` VARCHAR(255) DEFAULT NULL COMMENT 'resource_type',
    `resource_id` VARCHAR(255) DEFAULT NULL COMMENT 'resource_id',
    `method` VARCHAR(255) DEFAULT NULL COMMENT 'method',
    `path` VARCHAR(255) DEFAULT NULL COMMENT 'path',
    `request_body` VARCHAR(255) DEFAULT NULL COMMENT 'request_body',
    `response_status` INT DEFAULT 0 COMMENT 'response_status',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg',
    `duration_ms` INT DEFAULT 0 COMMENT 'duration_ms',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== BillingRecord (billing_record) ======
CREATE TABLE IF NOT EXISTS `billing_record` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `record_id` VARCHAR(255) DEFAULT NULL COMMENT 'record_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `license_id` BIGINT DEFAULT 0 COMMENT 'license_id',
    `model_entry_id` BIGINT DEFAULT 0 COMMENT 'model_entry_id',
    `record_type` VARCHAR(255) DEFAULT NULL COMMENT 'record_type',
    `amount_cents` BIGINT DEFAULT 0 COMMENT 'amount_cents',
    `currency` VARCHAR(255) DEFAULT NULL COMMENT 'currency',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `payment_method` VARCHAR(255) DEFAULT NULL COMMENT 'payment_method',
    `external_transaction_id` VARCHAR(255) DEFAULT NULL COMMENT 'external_transaction_id',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== ClusterNode (cluster_node) ======
CREATE TABLE IF NOT EXISTS `cluster_node` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `node_id` VARCHAR(255) DEFAULT NULL COMMENT 'node_id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `address` VARCHAR(255) DEFAULT NULL COMMENT 'address',
    `region` VARCHAR(255) DEFAULT NULL COMMENT 'region',
    `zone` VARCHAR(255) DEFAULT NULL COMMENT 'zone',
    `capabilities` VARCHAR(255) DEFAULT NULL COMMENT 'capabilities',
    `total_cores` INT DEFAULT 0 COMMENT 'total_cores',
    `total_memory_mb` BIGINT DEFAULT 0 COMMENT 'total_memory_mb',
    `total_gpus` INT DEFAULT 0 COMMENT 'total_gpus',
    `cpu_usage` DOUBLE DEFAULT 0 COMMENT 'cpu_usage',
    `memory_usage` DOUBLE DEFAULT 0 COMMENT 'memory_usage',
    `gpu_usage` DOUBLE DEFAULT 0 COMMENT 'gpu_usage',
    `active_tasks` INT DEFAULT 0 COMMENT 'active_tasks',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `is_leader` TINYINT DEFAULT 0 COMMENT 'is_leader',
    `last_heartbeat` DATETIME DEFAULT NULL COMMENT 'last_heartbeat',
    `started_at` DATETIME DEFAULT NULL COMMENT 'started_at',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== DashboardMetric (dashboard_metric) ======
CREATE TABLE IF NOT EXISTS `dashboard_metric` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `metric` VARCHAR(255) DEFAULT NULL COMMENT 'metric',
    `dimension` VARCHAR(255) DEFAULT NULL COMMENT 'dimension',
    `value` DOUBLE DEFAULT 0 COMMENT 'value',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `timestamp` DATETIME DEFAULT NULL COMMENT 'timestamp',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== DbDataSource (data_source) ======
CREATE TABLE IF NOT EXISTS `data_source` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `type` VARCHAR(255) DEFAULT NULL COMMENT 'type',
    `jdbc_url` VARCHAR(255) DEFAULT NULL COMMENT 'jdbc_url',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `password` VARCHAR(255) DEFAULT NULL COMMENT 'password',
    `driver_class` VARCHAR(255) DEFAULT NULL COMMENT 'driver_class',
    `pool_size` INT DEFAULT 0 COMMENT 'pool_size',
    `min_idle` INT DEFAULT 0 COMMENT 'min_idle',
    `max_lifetime` INT DEFAULT 0 COMMENT 'max_lifetime',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `test_status` VARCHAR(255) DEFAULT NULL COMMENT 'test_status',
    `test_message` VARCHAR(255) DEFAULT NULL COMMENT 'test_message',
    `last_test_at` DATETIME DEFAULT NULL COMMENT 'last_test_at',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `created_by` BIGINT DEFAULT 0 COMMENT 'created_by',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== KbChunk (kb_chunk) ======
CREATE TABLE IF NOT EXISTS `kb_chunk` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `chunk_id` VARCHAR(255) DEFAULT NULL COMMENT 'chunk_id',
    `doc_id` VARCHAR(255) DEFAULT NULL COMMENT 'doc_id',
    `kb_id` VARCHAR(255) DEFAULT NULL COMMENT 'kb_id',
    `seq` INT DEFAULT 0 COMMENT 'seq',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content',
    `char_count` INT DEFAULT 0 COMMENT 'char_count',
    `token_count` INT DEFAULT 0 COMMENT 'token_count',
    `embedding` VARCHAR(255) DEFAULT NULL COMMENT 'embedding',
    `embedding_model` VARCHAR(255) DEFAULT NULL COMMENT 'embedding_model',
    `keywords` VARCHAR(255) DEFAULT NULL COMMENT 'keywords',
    `summary` VARCHAR(255) DEFAULT NULL COMMENT 'summary',
    `location` VARCHAR(255) DEFAULT NULL COMMENT 'location',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== KbDocument (kb_document) ======
CREATE TABLE IF NOT EXISTS `kb_document` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `doc_id` VARCHAR(255) DEFAULT NULL COMMENT 'doc_id',
    `kb_id` VARCHAR(255) DEFAULT NULL COMMENT 'kb_id',
    `filename` VARCHAR(255) DEFAULT NULL COMMENT 'filename',
    `mime_type` VARCHAR(255) DEFAULT NULL COMMENT 'mime_type',
    `size_bytes` BIGINT DEFAULT 0 COMMENT 'size_bytes',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256',
    `file_path` VARCHAR(255) DEFAULT NULL COMMENT 'file_path',
    `source` VARCHAR(255) DEFAULT NULL COMMENT 'source',
    `source_url` VARCHAR(255) DEFAULT NULL COMMENT 'source_url',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `chunk_count` INT DEFAULT 0 COMMENT 'chunk_count',
    `embedding_count` INT DEFAULT 0 COMMENT 'embedding_count',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id',
    `is_public` TINYINT DEFAULT 0 COMMENT 'is_public',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== KbPermission (kb_permission) ======
CREATE TABLE IF NOT EXISTS `kb_permission` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `kb_id` VARCHAR(255) DEFAULT NULL COMMENT 'kb_id',
    `subject_type` VARCHAR(255) DEFAULT NULL COMMENT 'subject_type',
    `subject_id` BIGINT DEFAULT 0 COMMENT 'subject_id',
    `permission` VARCHAR(255) DEFAULT NULL COMMENT 'permission',
    `grant_by` BIGINT DEFAULT 0 COMMENT 'grant_by',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== LogEntry (raft_log) ======
CREATE TABLE IF NOT EXISTS `raft_log` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `term` BIGINT DEFAULT 0 COMMENT 'term',
    `log_index` BIGINT DEFAULT 0 COMMENT 'log_index',
    `node_id` VARCHAR(255) DEFAULT NULL COMMENT 'node_id',
    `command` VARCHAR(255) DEFAULT NULL COMMENT 'command',
    `committed` TINYINT DEFAULT 0 COMMENT 'committed',
    `committed_at` DATETIME DEFAULT NULL COMMENT 'committed_at',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== ModelLicense (model_license) ======
CREATE TABLE IF NOT EXISTS `model_license` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `license_key` VARCHAR(255) DEFAULT NULL COMMENT 'license_key',
    `model_entry_id` BIGINT DEFAULT 0 COMMENT 'model_entry_id',
    `model_version_id` BIGINT DEFAULT 0 COMMENT 'model_version_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `license_type` VARCHAR(255) DEFAULT NULL COMMENT 'license_type',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `quota_calls` BIGINT DEFAULT 0 COMMENT 'quota_calls',
    `used_calls` BIGINT DEFAULT 0 COMMENT 'used_calls',
    `start_at` DATETIME DEFAULT NULL COMMENT 'start_at',
    `expire_at` DATETIME DEFAULT NULL COMMENT 'expire_at',
    `price_cents` BIGINT DEFAULT 0 COMMENT 'price_cents',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== ModelVersion (model_version) ======
CREATE TABLE IF NOT EXISTS `model_version` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `version_id` VARCHAR(255) DEFAULT NULL COMMENT 'version_id',
    `model_entry_id` BIGINT DEFAULT 0 COMMENT 'model_entry_id',
    `version` VARCHAR(255) DEFAULT NULL COMMENT 'version',
    `changelog` VARCHAR(255) DEFAULT NULL COMMENT 'changelog',
    `file_path` VARCHAR(255) DEFAULT NULL COMMENT 'file_path',
    `size_bytes` BIGINT DEFAULT 0 COMMENT 'size_bytes',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256',
    `input_schema` VARCHAR(255) DEFAULT NULL COMMENT 'input_schema',
    `output_schema` VARCHAR(255) DEFAULT NULL COMMENT 'output_schema',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `is_latest` TINYINT DEFAULT 0 COMMENT 'is_latest',
    `uploader_id` BIGINT DEFAULT 0 COMMENT 'uploader_id',
    `backward_compatible` VARCHAR(255) DEFAULT NULL COMMENT 'backward_compatible',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== ModerationRecord (moderation_record) ======
CREATE TABLE IF NOT EXISTS `moderation_record` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `trace_id` VARCHAR(255) DEFAULT NULL COMMENT 'trace_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `content_type` VARCHAR(255) DEFAULT NULL COMMENT 'content_type',
    `content_hash` VARCHAR(255) DEFAULT NULL COMMENT 'content_hash',
    `content_size` BIGINT DEFAULT 0 COMMENT 'content_size',
    `content_url` VARCHAR(255) DEFAULT NULL COMMENT 'content_url',
    `moderation_status` VARCHAR(255) DEFAULT NULL COMMENT 'moderation_status',
    `risk_level` VARCHAR(255) DEFAULT NULL COMMENT 'risk_level',
    `risk_labels` VARCHAR(255) DEFAULT NULL COMMENT 'risk_labels',
    `risk_score` DECIMAL(20,4) DEFAULT 0 COMMENT 'risk_score',
    `moderator` VARCHAR(255) DEFAULT NULL COMMENT 'moderator',
    `rejection_reason` VARCHAR(255) DEFAULT NULL COMMENT 'rejection_reason',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== MultimediaFile (multimedia_file) ======
CREATE TABLE IF NOT EXISTS `multimedia_file` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `file_id` VARCHAR(255) DEFAULT NULL COMMENT 'file_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `file_name` VARCHAR(255) DEFAULT NULL COMMENT 'file_name',
    `original_name` VARCHAR(255) DEFAULT NULL COMMENT 'original_name',
    `file_type` VARCHAR(255) DEFAULT NULL COMMENT 'file_type',
    `mime_type` VARCHAR(255) DEFAULT NULL COMMENT 'mime_type',
    `file_size` BIGINT DEFAULT 0 COMMENT 'file_size',
    `file_hash` VARCHAR(255) DEFAULT NULL COMMENT 'file_hash',
    `storage_path` VARCHAR(255) DEFAULT NULL COMMENT 'storage_path',
    `storage_type` VARCHAR(255) DEFAULT NULL COMMENT 'storage_type',
    `encrypted` INT DEFAULT 0 COMMENT 'encrypted',
    `duration_ms` BIGINT DEFAULT 0 COMMENT 'duration_ms',
    `width` INT DEFAULT 0 COMMENT 'width',
    `height` INT DEFAULT 0 COMMENT 'height',
    `bitrate` INT DEFAULT 0 COMMENT 'bitrate',
    `sample_rate` INT DEFAULT 0 COMMENT 'sample_rate',
    `channels` INT DEFAULT 0 COMMENT 'channels',
    `codec` VARCHAR(255) DEFAULT NULL COMMENT 'codec',
    `exif` VARCHAR(255) DEFAULT NULL COMMENT 'exif',
    `moderation_status` VARCHAR(255) DEFAULT NULL COMMENT 'moderation_status',
    `moderation_id` BIGINT DEFAULT 0 COMMENT 'moderation_id',
    `watermarked` INT DEFAULT 0 COMMENT 'watermarked',
    `is_public` INT DEFAULT 0 COMMENT 'is_public',
    `access_count` INT DEFAULT 0 COMMENT 'access_count',
    `expire_at` DATETIME DEFAULT NULL COMMENT 'expire_at',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== PipelineLog (pipeline_log) ======
CREATE TABLE IF NOT EXISTS `pipeline_log` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `session_id` VARCHAR(255) DEFAULT NULL COMMENT 'session_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `client_ip` VARCHAR(255) DEFAULT NULL COMMENT 'client_ip',
    `input_text` VARCHAR(255) DEFAULT NULL COMMENT 'input_text',
    `input_modality` VARCHAR(255) DEFAULT NULL COMMENT 'input_modality',
    `intent` VARCHAR(255) DEFAULT NULL COMMENT 'intent',
    `output_text` VARCHAR(255) DEFAULT NULL COMMENT 'output_text',
    `output_tokens` INT DEFAULT 0 COMMENT 'output_tokens',
    `compute_device` VARCHAR(255) DEFAULT NULL COMMENT 'compute_device',
    `compute_mode` VARCHAR(255) DEFAULT NULL COMMENT 'compute_mode',
    `total_cost_ms` BIGINT DEFAULT 0 COMMENT 'total_cost_ms',
    `stage_costs` VARCHAR(255) DEFAULT NULL COMMENT 'stage_costs',
    `risk_level` VARCHAR(255) DEFAULT NULL COMMENT 'risk_level',
    `needs_review` TINYINT DEFAULT 0 COMMENT 'needs_review',
    `rag_hits` INT DEFAULT 0 COMMENT 'rag_hits',
    `tool_calls` INT DEFAULT 0 COMMENT 'tool_calls',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== PushMessage (push_message) ======
CREATE TABLE IF NOT EXISTS `push_message` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `message_id` VARCHAR(255) DEFAULT NULL COMMENT 'message_id',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title',
    `body` VARCHAR(255) DEFAULT NULL COMMENT 'body',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon',
    `click_action` VARCHAR(255) DEFAULT NULL COMMENT 'click_action',
    `data` VARCHAR(255) DEFAULT NULL COMMENT 'data',
    `target_type` VARCHAR(255) DEFAULT NULL COMMENT 'target_type',
    `target_value` VARCHAR(255) DEFAULT NULL COMMENT 'target_value',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `success_count` INT DEFAULT 0 COMMENT 'success_count',
    `failure_count` INT DEFAULT 0 COMMENT 'failure_count',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== PushSubscription (push_subscription) ======
CREATE TABLE IF NOT EXISTS `push_subscription` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `subscription_id` VARCHAR(255) DEFAULT NULL COMMENT 'subscription_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    `endpoint` VARCHAR(255) DEFAULT NULL COMMENT 'endpoint',
    `p256dh_key` VARCHAR(255) DEFAULT NULL COMMENT 'p256dh_key',
    `auth_key` VARCHAR(255) DEFAULT NULL COMMENT 'auth_key',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `last_active_at` DATETIME DEFAULT NULL COMMENT 'last_active_at',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== SensitiveWord (sensitive_word) ======
CREATE TABLE IF NOT EXISTS `sensitive_word` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `word` VARCHAR(255) DEFAULT NULL COMMENT 'word',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category',
    `level` VARCHAR(255) DEFAULT NULL COMMENT 'level',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== TrainingCheckpoint (training_checkpoint) ======
CREATE TABLE IF NOT EXISTS `training_checkpoint` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `task_id` VARCHAR(255) DEFAULT NULL COMMENT 'task_id',
    `checkpoint_id` VARCHAR(255) DEFAULT NULL COMMENT 'checkpoint_id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `epoch` INT DEFAULT 0 COMMENT 'epoch',
    `step` INT DEFAULT 0 COMMENT 'step',
    `file_path` VARCHAR(255) DEFAULT NULL COMMENT 'file_path',
    `size_bytes` BIGINT DEFAULT 0 COMMENT 'size_bytes',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256',
    `val_loss` DOUBLE DEFAULT 0 COMMENT 'val_loss',
    `accuracy` DOUBLE DEFAULT 0 COMMENT 'accuracy',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== TrainingJob (training_job) ======
CREATE TABLE IF NOT EXISTS `training_job` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `task_id` VARCHAR(255) DEFAULT NULL COMMENT 'task_id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `model` VARCHAR(255) DEFAULT NULL COMMENT 'model',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `total_epochs` INT DEFAULT 0 COMMENT 'total_epochs',
    `current_epoch` INT DEFAULT 0 COMMENT 'current_epoch',
    `current_step` INT DEFAULT 0 COMMENT 'current_step',
    `start_time_ms` BIGINT DEFAULT 0 COMMENT 'start_time_ms',
    `end_time_ms` BIGINT DEFAULT 0 COMMENT 'end_time_ms',
    `config` VARCHAR(255) DEFAULT NULL COMMENT 'config',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `last_loss` DOUBLE DEFAULT 0 COMMENT 'last_loss',
    `last_val_loss` DOUBLE DEFAULT 0 COMMENT 'last_val_loss',
    `last_accuracy` DOUBLE DEFAULT 0 COMMENT 'last_accuracy',
    `total_steps` INT DEFAULT 0 COMMENT 'total_steps',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== TrainingMetric (training_metric) ======
CREATE TABLE IF NOT EXISTS `training_metric` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `task_id` VARCHAR(255) DEFAULT NULL COMMENT 'task_id',
    `epoch` INT DEFAULT 0 COMMENT 'epoch',
    `step` INT DEFAULT 0 COMMENT 'step',
    `loss` DOUBLE DEFAULT 0 COMMENT 'loss',
    `val_loss` DOUBLE DEFAULT 0 COMMENT 'val_loss',
    `accuracy` DOUBLE DEFAULT 0 COMMENT 'accuracy',
    `learning_rate` DOUBLE DEFAULT 0 COMMENT 'learning_rate',
    `elapsed_ms` BIGINT DEFAULT 0 COMMENT 'elapsed_ms',
    `timestamp` DATETIME DEFAULT NULL COMMENT 'timestamp',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== DataSource (analytics_datasource) ======
CREATE TABLE IF NOT EXISTS `analytics_datasource` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `type` VARCHAR(255) DEFAULT NULL COMMENT 'type',
    `jdbc_url` VARCHAR(255) DEFAULT NULL COMMENT 'jdbc_url',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `password_enc` VARCHAR(255) DEFAULT NULL COMMENT 'password_enc',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== IngestTask (analytics_ingest_task) ======
CREATE TABLE IF NOT EXISTS `analytics_ingest_task` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `task_id` VARCHAR(255) DEFAULT NULL COMMENT 'task_id',
    `filename` VARCHAR(255) DEFAULT NULL COMMENT 'filename',
    `file_type` VARCHAR(255) DEFAULT NULL COMMENT 'file_type',
    `encoding` VARCHAR(255) DEFAULT NULL COMMENT 'encoding',
    `separator` VARCHAR(255) DEFAULT NULL COMMENT 'separator',
    `file_size` BIGINT DEFAULT 0 COMMENT 'file_size',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message',
    `quality_json` VARCHAR(255) DEFAULT NULL COMMENT 'quality_json',
    `total_rows` BIGINT DEFAULT 0 COMMENT 'total_rows',
    `total_columns` BIGINT DEFAULT 0 COMMENT 'total_columns',
    `columns_json` VARCHAR(255) DEFAULT NULL COMMENT 'columns_json',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `finished_at` DATETIME DEFAULT NULL COMMENT 'finished_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== Nl2SqlHistory (analytics_nlsql_history) ======
CREATE TABLE IF NOT EXISTS `analytics_nlsql_history` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `data_source_id` BIGINT DEFAULT 0 COMMENT 'data_source_id',
    `question` VARCHAR(255) DEFAULT NULL COMMENT 'question',
    `generated_sql` VARCHAR(255) DEFAULT NULL COMMENT 'generated_sql',
    `corrected_sql` VARCHAR(255) DEFAULT NULL COMMENT 'corrected_sql',
    `model` VARCHAR(255) DEFAULT NULL COMMENT 'model',
    `prompt_tokens` INT DEFAULT 0 COMMENT 'prompt_tokens',
    `completion_tokens` INT DEFAULT 0 COMMENT 'completion_tokens',
    `duration_ms` BIGINT DEFAULT 0 COMMENT 'duration_ms',
    `success` TINYINT DEFAULT 0 COMMENT 'success',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message',
    `feedback_rating` INT DEFAULT 0 COMMENT 'feedback_rating',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== Report (analytics_report) ======
CREATE TABLE IF NOT EXISTS `analytics_report` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `report_id` VARCHAR(255) DEFAULT NULL COMMENT 'report_id',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title',
    `question` VARCHAR(255) DEFAULT NULL COMMENT 'question',
    `sql_text` VARCHAR(255) DEFAULT NULL COMMENT 'sql_text',
    `markdown` VARCHAR(255) DEFAULT NULL COMMENT 'markdown',
    `chart_options_json` VARCHAR(255) DEFAULT NULL COMMENT 'chart_options_json',
    `row_count` BIGINT DEFAULT 0 COMMENT 'row_count',
    `duration_ms` BIGINT DEFAULT 0 COMMENT 'duration_ms',
    `format` VARCHAR(255) DEFAULT NULL COMMENT 'format',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AuthLoginLog (auth_login_log) ======
CREATE TABLE IF NOT EXISTS `auth_login_log` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent',
    `status` INT DEFAULT 0 COMMENT 'status',
    `message` VARCHAR(255) DEFAULT NULL COMMENT 'message',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AuthRefreshToken (auth_refresh_token) ======
CREATE TABLE IF NOT EXISTS `auth_refresh_token` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `token` VARCHAR(255) DEFAULT NULL COMMENT 'token',
    `expires_at` DATETIME DEFAULT NULL COMMENT 'expires_at',
    `revoked` INT DEFAULT 0 COMMENT 'revoked',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== Notification (notification) ======
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `type` VARCHAR(255) DEFAULT NULL COMMENT 'type',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content',
    `is_read` INT DEFAULT 0 COMMENT 'is_read',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== OAuthAppConfig (oauth_app_config) ======
CREATE TABLE IF NOT EXISTS `oauth_app_config` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    `app_type` VARCHAR(255) DEFAULT NULL COMMENT 'app_type',
    `app_id` VARCHAR(255) DEFAULT NULL COMMENT 'app_id',
    `app_secret` VARCHAR(255) DEFAULT NULL COMMENT 'app_secret',
    `public_key` VARCHAR(255) DEFAULT NULL COMMENT 'public_key',
    `redirect_uri` VARCHAR(255) DEFAULT NULL COMMENT 'redirect_uri',
    `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `extra_config` VARCHAR(255) DEFAULT NULL COMMENT 'extra_config',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== OAuthBinding (oauth_binding) ======
CREATE TABLE IF NOT EXISTS `oauth_binding` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    `app_type` VARCHAR(255) DEFAULT NULL COMMENT 'app_type',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar',
    `access_token` VARCHAR(255) DEFAULT NULL COMMENT 'access_token',
    `refresh_token` VARCHAR(255) DEFAULT NULL COMMENT 'refresh_token',
    `token_expires_at` DATETIME DEFAULT NULL COMMENT 'token_expires_at',
    `raw_data` VARCHAR(255) DEFAULT NULL COMMENT 'raw_data',
    `bound_at` DATETIME DEFAULT NULL COMMENT 'bound_at',
    `last_login_at` DATETIME DEFAULT NULL COMMENT 'last_login_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== SysRole (sys_role) ======
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `code` VARCHAR(255) DEFAULT NULL COMMENT 'code',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `sort` INT DEFAULT 0 COMMENT 'sort',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== SysUser (sys_user) ======
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `password` VARCHAR(255) DEFAULT NULL COMMENT 'password',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname',
    `email` VARCHAR(255) DEFAULT NULL COMMENT 'email',
    `phone` VARCHAR(255) DEFAULT NULL COMMENT 'phone',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar',
    `gender` INT DEFAULT 0 COMMENT 'gender',
    `status` INT DEFAULT 0 COMMENT 'status',
    `last_login_ip` VARCHAR(255) DEFAULT NULL COMMENT 'last_login_ip',
    `last_login_at` DATETIME DEFAULT NULL COMMENT 'last_login_at',
    `tenant_id` BIGINT DEFAULT 0 COMMENT 'tenant_id',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark',
    `wechat_openid` VARCHAR(255) DEFAULT NULL COMMENT 'wechat_openid',
    `wechat_unionid` VARCHAR(255) DEFAULT NULL COMMENT 'wechat_unionid',
    `wechat_nickname` VARCHAR(255) DEFAULT NULL COMMENT 'wechat_nickname',
    `wechat_avatar` VARCHAR(255) DEFAULT NULL COMMENT 'wechat_avatar',
    `wechat_bound_at` DATETIME DEFAULT NULL COMMENT 'wechat_bound_at',
    `qq_openid` VARCHAR(255) DEFAULT NULL COMMENT 'qq_openid',
    `qq_unionid` VARCHAR(255) DEFAULT NULL COMMENT 'qq_unionid',
    `qq_nickname` VARCHAR(255) DEFAULT NULL COMMENT 'qq_nickname',
    `qq_avatar` VARCHAR(255) DEFAULT NULL COMMENT 'qq_avatar',
    `qq_bound_at` DATETIME DEFAULT NULL COMMENT 'qq_bound_at',
    `alipay_openid` VARCHAR(255) DEFAULT NULL COMMENT 'alipay_openid',
    `alipay_user_id` VARCHAR(255) DEFAULT NULL COMMENT 'alipay_user_id',
    `alipay_nickname` VARCHAR(255) DEFAULT NULL COMMENT 'alipay_nickname',
    `alipay_avatar` VARCHAR(255) DEFAULT NULL COMMENT 'alipay_avatar',
    `alipay_bound_at` DATETIME DEFAULT NULL COMMENT 'alipay_bound_at',
    `created_by` BIGINT DEFAULT 0 COMMENT 'created_by',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_by` BIGINT DEFAULT 0 COMMENT 'updated_by',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== SysUserRole (sys_user_role) ======
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `role_id` BIGINT DEFAULT 0 COMMENT 'role_id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== Tenant (tenant) ======
CREATE TABLE IF NOT EXISTS `tenant` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `code` VARCHAR(255) DEFAULT NULL COMMENT 'code',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `plan` VARCHAR(255) DEFAULT NULL COMMENT 'plan',
    `status` INT DEFAULT 0 COMMENT 'status',
    `max_users` INT DEFAULT 0 COMMENT 'max_users',
    `max_models` INT DEFAULT 0 COMMENT 'max_models',
    `qps_limit` INT DEFAULT 0 COMMENT 'qps_limit',
    `monthly_quota` BIGINT DEFAULT 0 COMMENT 'monthly_quota',
    `used_quota` BIGINT DEFAULT 0 COMMENT 'used_quota',
    `expire_at` DATETIME DEFAULT NULL COMMENT 'expire_at',
    `contact_email` VARCHAR(255) DEFAULT NULL COMMENT 'contact_email',
    `contact_phone` VARCHAR(255) DEFAULT NULL COMMENT 'contact_phone',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== UnionidRelations (unionid_relations) ======
CREATE TABLE IF NOT EXISTS `unionid_relations` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    `first_seen_at` DATETIME DEFAULT NULL COMMENT 'first_seen_at',
    `last_seen_at` DATETIME DEFAULT NULL COMMENT 'last_seen_at',
    `binding_count` INT DEFAULT 0 COMMENT 'binding_count',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== UserApiKey (user_api_key) ======
CREATE TABLE IF NOT EXISTS `user_api_key` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `key_hash` VARCHAR(255) DEFAULT NULL COMMENT 'key_hash',
    `key_prefix` VARCHAR(255) DEFAULT NULL COMMENT 'key_prefix',
    `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes',
    `expires_at` DATETIME DEFAULT NULL COMMENT 'expires_at',
    `last_used_at` DATETIME DEFAULT NULL COMMENT 'last_used_at',
    `use_count` BIGINT DEFAULT 0 COMMENT 'use_count',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== WechatConfig (wechat_config) ======
CREATE TABLE IF NOT EXISTS `wechat_config` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `app_type` VARCHAR(255) DEFAULT NULL COMMENT 'app_type',
    `app_id` VARCHAR(255) DEFAULT NULL COMMENT 'app_id',
    `app_secret` VARCHAR(255) DEFAULT NULL COMMENT 'app_secret',
    `token` VARCHAR(255) DEFAULT NULL COMMENT 'token',
    `aes_key` VARCHAR(255) DEFAULT NULL COMMENT 'aes_key',
    `redirect_uri` VARCHAR(255) DEFAULT NULL COMMENT 'redirect_uri',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== WechatScanSession (wechat_scan_session) ======
CREATE TABLE IF NOT EXISTS `wechat_scan_session` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `ticket` VARCHAR(255) DEFAULT NULL COMMENT 'ticket',
    `scene_id` VARCHAR(255) DEFAULT NULL COMMENT 'scene_id',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `access_token` VARCHAR(255) DEFAULT NULL COMMENT 'access_token',
    `refresh_token` VARCHAR(255) DEFAULT NULL COMMENT 'refresh_token',
    `client_ip` VARCHAR(255) DEFAULT NULL COMMENT 'client_ip',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent',
    `expires_at` DATETIME DEFAULT NULL COMMENT 'expires_at',
    `confirmed_at` DATETIME DEFAULT NULL COMMENT 'confirmed_at',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== WechatUserBinding (wechat_user_binding) ======
CREATE TABLE IF NOT EXISTS `wechat_user_binding` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid',
    `app_type` VARCHAR(255) DEFAULT NULL COMMENT 'app_type',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar',
    `bound_at` DATETIME DEFAULT NULL COMMENT 'bound_at',
    `last_login_at` DATETIME DEFAULT NULL COMMENT 'last_login_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== ChatMessage (chat_message) ======
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `session_id` BIGINT DEFAULT 0 COMMENT 'session_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content',
    `tokens` INT DEFAULT 0 COMMENT 'tokens',
    `finish_reason` VARCHAR(255) DEFAULT NULL COMMENT 'finish_reason',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== ChatSession (chat_session) ======
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title',
    `model` VARCHAR(255) DEFAULT NULL COMMENT 'model',
    `system_prompt` VARCHAR(255) DEFAULT NULL COMMENT 'system_prompt',
    `temperature` DECIMAL(20,4) DEFAULT 0 COMMENT 'temperature',
    `status` INT DEFAULT 0 COMMENT 'status',
    `message_count` INT DEFAULT 0 COMMENT 'message_count',
    `last_message_at` DATETIME DEFAULT NULL COMMENT 'last_message_at',
    `tenant_id` BIGINT DEFAULT 0 COMMENT 'tenant_id',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== FunctionCallLog (function_call_log) ======
CREATE TABLE IF NOT EXISTS `function_call_log` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `session_id` BIGINT DEFAULT 0 COMMENT 'session_id',
    `tool_name` VARCHAR(255) DEFAULT NULL COMMENT 'tool_name',
    `arguments` VARCHAR(255) DEFAULT NULL COMMENT 'arguments',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg',
    `duration_ms` INT DEFAULT 0 COMMENT 'duration_ms',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== FunctionTool (function_tool) ======
CREATE TABLE IF NOT EXISTS `function_tool` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `display_name` VARCHAR(255) DEFAULT NULL COMMENT 'display_name',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id',
    `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters',
    `endpoint` VARCHAR(255) DEFAULT NULL COMMENT 'endpoint',
    `http_method` VARCHAR(255) DEFAULT NULL COMMENT 'http_method',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== ModelBattleLog (model_battle_log) ======
CREATE TABLE IF NOT EXISTS `model_battle_log` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `battle_id` VARCHAR(255) DEFAULT NULL COMMENT 'battle_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `model_id` BIGINT DEFAULT 0 COMMENT 'model_id',
    `model_code` VARCHAR(255) DEFAULT NULL COMMENT 'model_code',
    `prompt` VARCHAR(255) DEFAULT NULL COMMENT 'prompt',
    `response` VARCHAR(255) DEFAULT NULL COMMENT 'response',
    `prompt_tokens` INT DEFAULT 0 COMMENT 'prompt_tokens',
    `completion_tokens` INT DEFAULT 0 COMMENT 'completion_tokens',
    `latency_ms` INT DEFAULT 0 COMMENT 'latency_ms',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg',
    `score` INT DEFAULT 0 COMMENT 'score',
    `judge_model` VARCHAR(255) DEFAULT NULL COMMENT 'judge_model',
    `judge_reason` VARCHAR(255) DEFAULT NULL COMMENT 'judge_reason',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== ModelConfig (model_config) ======
CREATE TABLE IF NOT EXISTS `model_config` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `provider_id` BIGINT DEFAULT 0 COMMENT 'provider_id',
    `model_code` VARCHAR(255) DEFAULT NULL COMMENT 'model_code',
    `display_name` VARCHAR(255) DEFAULT NULL COMMENT 'display_name',
    `max_context` INT DEFAULT 0 COMMENT 'max_context',
    `max_output` INT DEFAULT 0 COMMENT 'max_output',
    `input_price` DECIMAL(20,4) DEFAULT 0 COMMENT 'input_price',
    `output_price` DECIMAL(20,4) DEFAULT 0 COMMENT 'output_price',
    `supports_vision` INT DEFAULT 0 COMMENT 'supports_vision',
    `supports_tools` INT DEFAULT 0 COMMENT 'supports_tools',
    `supports_stream` INT DEFAULT 0 COMMENT 'supports_stream',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `sort` INT DEFAULT 0 COMMENT 'sort',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== ModelProvider (model_provider) ======
CREATE TABLE IF NOT EXISTS `model_provider` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `code` VARCHAR(255) DEFAULT NULL COMMENT 'code',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `base_url` VARCHAR(255) DEFAULT NULL COMMENT 'base_url',
    `api_key` VARCHAR(255) DEFAULT NULL COMMENT 'api_key',
    `protocol` VARCHAR(255) DEFAULT NULL COMMENT 'protocol',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `sort` INT DEFAULT 0 COMMENT 'sort',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== ModelQuota (model_quota) ======
CREATE TABLE IF NOT EXISTS `model_quota` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `model_id` BIGINT DEFAULT 0 COMMENT 'model_id',
    `quota_date` DATE DEFAULT NULL COMMENT 'quota_date',
    `used_tokens` BIGINT DEFAULT 0 COMMENT 'used_tokens',
    `used_requests` INT DEFAULT 0 COMMENT 'used_requests',
    `limit_tokens` BIGINT DEFAULT 0 COMMENT 'limit_tokens',
    `limit_requests` INT DEFAULT 0 COMMENT 'limit_requests',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== TrainingTask (training_task) ======
CREATE TABLE IF NOT EXISTS `training_task` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `model_name` VARCHAR(255) DEFAULT NULL COMMENT 'model_name',
    `corpus_path` VARCHAR(255) DEFAULT NULL COMMENT 'corpus_path',
    `n_layer` INT DEFAULT 0 COMMENT 'n_layer',
    `n_head` INT DEFAULT 0 COMMENT 'n_head',
    `n_embd` INT DEFAULT 0 COMMENT 'n_embd',
    `block_size` INT DEFAULT 0 COMMENT 'block_size',
    `max_iters` INT DEFAULT 0 COMMENT 'max_iters',
    `batch_size` INT DEFAULT 0 COMMENT 'batch_size',
    `learning_rate` DOUBLE DEFAULT 0 COMMENT 'learning_rate',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `progress` INT DEFAULT 0 COMMENT 'progress',
    `current_loss` DOUBLE DEFAULT 0 COMMENT 'current_loss',
    `current_iter` INT DEFAULT 0 COMMENT 'current_iter',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `completed_at` DATETIME DEFAULT NULL COMMENT 'completed_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AlertChannel (alert_channel) ======
CREATE TABLE IF NOT EXISTS `alert_channel` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `channel_type` VARCHAR(255) DEFAULT NULL COMMENT 'channel_type',
    `type` VARCHAR(255) DEFAULT NULL COMMENT 'type',
    `target` VARCHAR(255) DEFAULT NULL COMMENT 'target',
    `config` VARCHAR(255) DEFAULT NULL COMMENT 'config',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `priority` INT DEFAULT 0 COMMENT 'priority',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `template` VARCHAR(255) DEFAULT NULL COMMENT 'template',
    `created_by` BIGINT DEFAULT 0 COMMENT 'created_by',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AlertEvent (alert_event) ======
CREATE TABLE IF NOT EXISTS `alert_event` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `rule_id` BIGINT DEFAULT 0 COMMENT 'rule_id',
    `rule_name` VARCHAR(255) DEFAULT NULL COMMENT 'rule_name',
    `severity` VARCHAR(255) DEFAULT NULL COMMENT 'severity',
    `metric_name` VARCHAR(255) DEFAULT NULL COMMENT 'metric_name',
    `metric_value` VARCHAR(255) DEFAULT 0 COMMENT 'metric_value',
    `threshold` VARCHAR(255) DEFAULT 0 COMMENT 'threshold',
    `message` VARCHAR(255) DEFAULT NULL COMMENT 'message',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `fired_at` DATETIME DEFAULT NULL COMMENT 'fired_at',
    `resolved_at` DATETIME DEFAULT NULL COMMENT 'resolved_at',
    `acked_at` DATETIME DEFAULT NULL COMMENT 'acked_at',
    `acked_by` BIGINT DEFAULT 0 COMMENT 'acked_by',
    `duration` BIGINT DEFAULT 0 COMMENT 'duration',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== AlertRule (alert_rule) ======
CREATE TABLE IF NOT EXISTS `alert_rule` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `metric_name` VARCHAR(255) DEFAULT NULL COMMENT 'metric_name',
    `service` VARCHAR(255) DEFAULT NULL COMMENT 'service',
    `operator` VARCHAR(255) DEFAULT NULL COMMENT 'operator',
    `threshold` VARCHAR(255) DEFAULT 0 COMMENT 'threshold',
    `severity` VARCHAR(255) DEFAULT NULL COMMENT 'severity',
    `cooldown_minutes` INT DEFAULT 0 COMMENT 'cooldown_minutes',
    `enabled` INT DEFAULT 0 COMMENT 'enabled',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `notify_channel` VARCHAR(255) DEFAULT NULL COMMENT 'notify_channel',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== MetricSnapshot (metric_snapshot) ======
CREATE TABLE IF NOT EXISTS `metric_snapshot` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `service` VARCHAR(255) DEFAULT NULL COMMENT 'service',
    `metric_name` VARCHAR(255) DEFAULT NULL COMMENT 'metric_name',
    `metric_value` DECIMAL(20,4) DEFAULT 0 COMMENT 'metric_value',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `recorded_at` DATETIME DEFAULT NULL COMMENT 'recorded_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== PipelineNodeLog (pipeline_node_log) ======
CREATE TABLE IF NOT EXISTS `pipeline_node_log` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `run_id` BIGINT DEFAULT 0 COMMENT 'run_id',
    `node_id` VARCHAR(255) DEFAULT NULL COMMENT 'node_id',
    `node_type` VARCHAR(255) DEFAULT NULL COMMENT 'node_type',
    `node_name` VARCHAR(255) DEFAULT NULL COMMENT 'node_name',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `start_time` DATETIME DEFAULT NULL COMMENT 'start_time',
    `end_time` DATETIME DEFAULT NULL COMMENT 'end_time',
    `duration_ms` BIGINT DEFAULT 0 COMMENT 'duration_ms',
    `input_rows` INT DEFAULT 0 COMMENT 'input_rows',
    `output_rows` INT DEFAULT 0 COMMENT 'output_rows',
    `output_preview` VARCHAR(255) DEFAULT NULL COMMENT 'output_preview',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message',
    `config_snapshot` VARCHAR(255) DEFAULT NULL COMMENT 'config_snapshot',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== PipelineRun (pipeline_run) ======
CREATE TABLE IF NOT EXISTS `pipeline_run` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `workflow_id` BIGINT DEFAULT 0 COMMENT 'workflow_id',
    `workflow_name` VARCHAR(255) DEFAULT NULL COMMENT 'workflow_name',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `trigger_by` BIGINT DEFAULT 0 COMMENT 'trigger_by',
    `trigger_type` VARCHAR(255) DEFAULT NULL COMMENT 'trigger_type',
    `definition_snapshot` VARCHAR(255) DEFAULT NULL COMMENT 'definition_snapshot',
    `start_time` DATETIME DEFAULT NULL COMMENT 'start_time',
    `end_time` DATETIME DEFAULT NULL COMMENT 'end_time',
    `duration_ms` BIGINT DEFAULT 0 COMMENT 'duration_ms',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message',
    `result_summary` VARCHAR(255) DEFAULT NULL COMMENT 'result_summary',
    `create_time` DATETIME DEFAULT NULL COMMENT 'create_time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== PipelineWorkflow (pipeline_workflow) ======
CREATE TABLE IF NOT EXISTS `pipeline_workflow` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `definition` VARCHAR(255) DEFAULT NULL COMMENT 'definition',
    `version` INT DEFAULT 0 COMMENT 'version',
    `status` INT DEFAULT 0 COMMENT 'status',
    `create_by` BIGINT DEFAULT 0 COMMENT 'create_by',
    `update_by` BIGINT DEFAULT 0 COMMENT 'update_by',
    `create_time` DATETIME DEFAULT NULL COMMENT 'create_time',
    `update_time` DATETIME DEFAULT NULL COMMENT 'update_time',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== PipelineWorkflowVersion (pipeline_workflow_version) ======
CREATE TABLE IF NOT EXISTS `pipeline_workflow_version` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `workflow_id` BIGINT DEFAULT 0 COMMENT 'workflow_id',
    `version` INT DEFAULT 0 COMMENT 'version',
    `definition` VARCHAR(255) DEFAULT NULL COMMENT 'definition',
    `change_log` VARCHAR(255) DEFAULT NULL COMMENT 'change_log',
    `create_by` BIGINT DEFAULT 0 COMMENT 'create_by',
    `create_time` DATETIME DEFAULT NULL COMMENT 'create_time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== PromptTemplate (prompt_template) ======
CREATE TABLE IF NOT EXISTS `prompt_template` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content',
    `variables` VARCHAR(255) DEFAULT NULL COMMENT 'variables',
    `creator_id` BIGINT DEFAULT 0 COMMENT 'creator_id',
    `creator_name` VARCHAR(255) DEFAULT NULL COMMENT 'creator_name',
    `is_public` TINYINT DEFAULT 0 COMMENT 'is_public',
    `use_count` INT DEFAULT 0 COMMENT 'use_count',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== Document (document) ======
CREATE TABLE IF NOT EXISTS `document` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `kb_id` BIGINT DEFAULT 0 COMMENT 'kb_id',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title',
    `source_type` VARCHAR(255) DEFAULT NULL COMMENT 'source_type',
    `source_uri` VARCHAR(255) DEFAULT NULL COMMENT 'source_uri',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content',
    `size_bytes` BIGINT DEFAULT 0 COMMENT 'size_bytes',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg',
    `chunk_count` INT DEFAULT 0 COMMENT 'chunk_count',
    `checksum` VARCHAR(255) DEFAULT NULL COMMENT 'checksum',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== DocumentChunk (document_chunk) ======
CREATE TABLE IF NOT EXISTS `document_chunk` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `doc_id` BIGINT DEFAULT 0 COMMENT 'doc_id',
    `kb_id` BIGINT DEFAULT 0 COMMENT 'kb_id',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id',
    `chunk_index` INT DEFAULT 0 COMMENT 'chunk_index',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content',
    `embedding` BLOB DEFAULT 0 COMMENT 'embedding',
    `dim` INT DEFAULT 0 COMMENT 'dim',
    `char_count` INT DEFAULT 0 COMMENT 'char_count',
    `start_pos` INT DEFAULT 0 COMMENT 'start_pos',
    `end_pos` INT DEFAULT 0 COMMENT 'end_pos',
    `access_count` INT DEFAULT 0 COMMENT 'access_count',
    `last_access_at` DATETIME DEFAULT NULL COMMENT 'last_access_at',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== KnowledgeBase (knowledge_base) ======
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id',
    `tenant_id` BIGINT DEFAULT 0 COMMENT 'tenant_id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `visibility` VARCHAR(255) DEFAULT NULL COMMENT 'visibility',
    `doc_count` INT DEFAULT 0 COMMENT 'doc_count',
    `chunk_count` INT DEFAULT 0 COMMENT 'chunk_count',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at',
    `deleted` INT DEFAULT 0 COMMENT 'deleted',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== CollabMessage (collab_message) ======
CREATE TABLE IF NOT EXISTS `collab_message` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `room_id` VARCHAR(255) DEFAULT NULL COMMENT 'room_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname',
    `type` VARCHAR(255) DEFAULT NULL COMMENT 'type',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata',
    `client_msg_id` VARCHAR(255) DEFAULT NULL COMMENT 'client_msg_id',
    `broadcast` INT DEFAULT 0 COMMENT 'broadcast',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== CollabParticipant (collab_participant) ======
CREATE TABLE IF NOT EXISTS `collab_participant` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `room_id` VARCHAR(255) DEFAULT NULL COMMENT 'room_id',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role',
    `cursor_x` INT DEFAULT 0 COMMENT 'cursor_x',
    `cursor_y` INT DEFAULT 0 COMMENT 'cursor_y',
    `selection_id` VARCHAR(255) DEFAULT NULL COMMENT 'selection_id',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `joined_at` DATETIME DEFAULT NULL COMMENT 'joined_at',
    `left_at` DATETIME DEFAULT NULL COMMENT 'left_at',
    `last_heartbeat` DATETIME DEFAULT NULL COMMENT 'last_heartbeat',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== CollabRoom (collab_room) ======
CREATE TABLE IF NOT EXISTS `collab_room` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id',
    `room_id` VARCHAR(255) DEFAULT NULL COMMENT 'room_id',
    `name` VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `type` VARCHAR(255) DEFAULT NULL COMMENT 'type',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id',
    `owner_name` VARCHAR(255) DEFAULT NULL COMMENT 'owner_name',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'description',
    `is_public` INT DEFAULT 0 COMMENT 'is_public',
    `max_participants` INT DEFAULT 0 COMMENT 'max_participants',
    `status` VARCHAR(255) DEFAULT NULL COMMENT 'status',
    `current_participants` INT DEFAULT 0 COMMENT 'current_participants',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at',
    `last_activity_at` DATETIME DEFAULT NULL COMMENT 'last_activity_at',
    `closed_at` DATETIME DEFAULT NULL COMMENT 'closed_at',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS=1;