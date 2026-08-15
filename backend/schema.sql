-- ============================================================
-- MiniMax Platform V6.8.2 数据库初始化脚本
-- 生成时间: 2026-08-12
-- 数据库: MySQL 8.0+ / utf8mb4
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 模块: minimax-auth
-- ============================================================
CREATE TABLE IF NOT EXISTS `mm_auth_login_log` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`username` VARCHAR(128),
`ip` VARCHAR(64),
`user_agent` VARCHAR(512),
`status` INT DEFAULT 0,
`message` TEXT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_auth_refresh_token` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`token` VARCHAR(255),
`expires_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`revoked` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_notification` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`type` VARCHAR(32),
`title` VARCHAR(255),
`content` TEXT,
`is_read` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_oauth_app_config` (
`id` BIGINT NOT NULL,
`platform` INT,
`app_type` VARCHAR(32),
`app_id` INT,
`app_secret` VARCHAR(255),
`public_key` VARCHAR(64),
`redirect_uri` VARCHAR(512),
`scopes` VARCHAR(32),
`enabled` INT DEFAULT 0,
`extra_config` TEXT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_oauth_binding` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`platform` INT,
`app_type` VARCHAR(32),
`openid` VARCHAR(128),
`unionid` VARCHAR(128),
`nickname` VARCHAR(128),
`avatar` VARCHAR(512),
`access_token` VARCHAR(255),
`refresh_token` VARCHAR(255),
`token_expires_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`raw_data` INT,
`bound_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`last_login_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_sys_role` (
`id` BIGINT NOT NULL,
`code` VARCHAR(64),
`name` VARCHAR(128),
`description` VARCHAR(64),
`sort` INT DEFAULT 0,
`enabled` INT DEFAULT 0,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_sys_user` (
`id` BIGINT NOT NULL,
`username` VARCHAR(128),
`password` VARCHAR(255),
`nickname` VARCHAR(128),
`email` VARCHAR(128),
`phone` VARCHAR(32),
`avatar` VARCHAR(512),
`gender` INT DEFAULT 0,
`status` INT DEFAULT 0,
`last_login_ip` VARCHAR(64),
`last_login_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`tenant_id` BIGINT NOT NULL,
`remark` VARCHAR(500),
`wechat_openid` VARCHAR(128),
`wechat_unionid` VARCHAR(128),
`wechat_nickname` VARCHAR(128),
`wechat_avatar` VARCHAR(512),
`wechat_bound_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`qq_openid` VARCHAR(128),
`qq_unionid` VARCHAR(128),
`qq_nickname` VARCHAR(128),
`qq_avatar` VARCHAR(512),
`qq_bound_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`alipay_openid` VARCHAR(64),
`alipay_user_id` VARCHAR(64),
`alipay_nickname` VARCHAR(64),
`alipay_avatar` VARCHAR(64),
`alipay_bound_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`created_by` BIGINT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_by` BIGINT,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_sys_user_role` (
`user_id` BIGINT NOT NULL,
`role_id` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_tenant` (
`id` BIGINT NOT NULL,
`code` VARCHAR(64),
`name` VARCHAR(128),
`plan` VARCHAR(64),
`status` INT DEFAULT 0,
`max_users` INT,
`max_models` INT DEFAULT 0,
`qps_limit` INT,
`monthly_quota` BIGINT,
`used_quota` BIGINT,
`expire_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`contact_email` VARCHAR(128),
`contact_phone` VARCHAR(32),
`remark` VARCHAR(500),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_unionid_relations` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`unionid` VARCHAR(128),
`platform` INT,
`first_seen_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`last_seen_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`binding_count` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_user_api_key` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`name` VARCHAR(128),
`key_hash` VARCHAR(64),
`key_prefix` VARCHAR(64),
`scopes` VARCHAR(32),
`expires_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`last_used_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`use_count` BIGINT,
`enabled` INT DEFAULT 0,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_wechat_config` (
`id` BIGINT NOT NULL,
`app_type` VARCHAR(32),
`app_id` INT,
`app_secret` VARCHAR(255),
`token` VARCHAR(255),
`aes_key` VARCHAR(64),
`redirect_uri` VARCHAR(512),
`scope` VARCHAR(32),
`enabled` INT DEFAULT 0,
`remark` VARCHAR(500),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_wechat_scan_session` (
`id` BIGINT NOT NULL,
`ticket` INT,
`scene_id` INT,
`status` VARCHAR(32),
`openid` VARCHAR(128),
`unionid` VARCHAR(128),
`nickname` VARCHAR(128),
`avatar` VARCHAR(512),
`user_id` BIGINT NOT NULL,
`access_token` VARCHAR(255),
`refresh_token` VARCHAR(255),
`client_ip` VARCHAR(64),
`user_agent` VARCHAR(512),
`expires_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`confirmed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_wechat_user_binding` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`openid` VARCHAR(128),
`unionid` VARCHAR(128),
`app_type` VARCHAR(32),
`nickname` VARCHAR(128),
`avatar` VARCHAR(512),
`bound_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`last_login_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 模块: minimax-chat
-- ============================================================
CREATE TABLE IF NOT EXISTS `mm_chat_message` (
`id` BIGINT NOT NULL,
`session_id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`role` INT,
`content` TEXT,
`tokens` INT,
`finish_reason` INT,
`error_message` TEXT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_chat_session` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`title` VARCHAR(255),
`model` VARCHAR(32),
`system_prompt` TEXT,
`temperature` DECIMAL(20,6),
`status` INT DEFAULT 0,
`message_count` INT DEFAULT 0,
`last_message_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`tenant_id` BIGINT NOT NULL,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 模块: minimax-model
-- ============================================================
CREATE TABLE IF NOT EXISTS `mm_model_battle_log` (
`id` BIGINT NOT NULL,
`battle_id` INT,
`user_id` BIGINT NOT NULL,
`model_id` BIGINT,
`model_code` VARCHAR(64),
`prompt` TEXT,
`response` INT,
`prompt_tokens` INT,
`completion_tokens` INT,
`latency_ms` INT,
`status` VARCHAR(32),
`error_msg` INT,
`score` INT,
`judge_model` VARCHAR(32),
`judge_reason` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_model_config` (
`id` BIGINT NOT NULL,
`provider_id` BIGINT,
`model_code` VARCHAR(64),
`display_name` VARCHAR(128),
`max_context` INT,
`max_output` INT,
`input_price` DECIMAL(20,6),
`output_price` DECIMAL(20,6),
`supports_vision` INT,
`supports_tools` INT,
`supports_stream` INT,
`enabled` INT DEFAULT 0,
`sort` INT DEFAULT 0,
`description` VARCHAR(64),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_model_provider` (
`id` BIGINT NOT NULL,
`code` VARCHAR(64),
`name` VARCHAR(128),
`base_url` VARCHAR(512),
`api_key` VARCHAR(255),
`protocol` VARCHAR(255),
`enabled` INT DEFAULT 0,
`sort` INT DEFAULT 0,
`description` VARCHAR(64),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_model_quota` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`model_id` BIGINT,
`quota_date` DATE,
`used_tokens` BIGINT,
`used_requests` INT,
`limit_tokens` BIGINT,
`limit_requests` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_training_metric` (
`id` BIGINT NOT NULL,
`task_id` BIGINT,
`iter` INT,
`loss` DOUBLE,
`accuracy` DOUBLE,
`progress` INT,
`lr` VARCHAR(32),
`gpu_util` INT,
`vram_gb` DOUBLE,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_training_task` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`model_name` VARCHAR(128),
`corpus_path` VARCHAR(255),
`n_layer` INT,
`n_head` INT,
`n_embd` INT,
`block_size` INT DEFAULT 0,
`max_iters` INT,
`batch_size` INT DEFAULT 0,
`learning_rate` DOUBLE,
`status` VARCHAR(32),
`progress` INT,
`current_loss` DOUBLE,
`current_iter` INT,
`error_message` TEXT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`completed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 模块: minimax-model-prompt
-- ============================================================
CREATE TABLE IF NOT EXISTS `mm_prompt_template` (
`id` BIGINT NOT NULL,
`name` VARCHAR(128),
`description` VARCHAR(64),
`category` VARCHAR(32),
`content` TEXT,
`variables` VARCHAR(512),
`creator_id` BIGINT,
`creator_name` VARCHAR(128),
`is_public` TINYINT(1),
`use_count` INT DEFAULT 0,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 模块: minimax-agent
-- ============================================================
CREATE TABLE IF NOT EXISTS `mm_agent_task` (
`id` BIGINT NOT NULL,
`task_id` INT,
`user_id` BIGINT NOT NULL,
`goal` TEXT,
`status` VARCHAR(32),
`rounds` INT,
`result` VARCHAR(32),
`llm_calls` INT,
`tool_calls` INT,
`total_tokens` INT DEFAULT 0,
`error_msg` INT,
`latency_ms` BIGINT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_collab_member` (
`id` BIGINT NOT NULL,
`collab_id` BIGINT,
`user_id` BIGINT NOT NULL,
`role` INT,
`joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`last_active_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_collab_session` (
`id` BIGINT NOT NULL,
`session_id` INT NOT NULL,
`owner_id` BIGINT NOT NULL,
`title` VARCHAR(255),
`max_users` INT,
`status` VARCHAR(32),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_kg_entity` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`name` VARCHAR(128),
`entity_type` VARCHAR(32),
`description` VARCHAR(64),
`aliases` VARCHAR(500),
`importance` INT,
`source` VARCHAR(255),
`ref_count` INT DEFAULT 0,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_kg_relation` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`from_entity` BIGINT NOT NULL,
`to_entity` BIGINT NOT NULL,
`relation_type` VARCHAR(32),
`description` VARCHAR(64),
`weight` DECIMAL(20,6),
`source` VARCHAR(255),
`ref_count` INT DEFAULT 0,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_plugin` (
`id` BIGINT NOT NULL,
`name` VARCHAR(128),
`display_name` VARCHAR(128),
`description` VARCHAR(64),
`version` INT,
`author` INT,
`category` VARCHAR(32),
`scope` VARCHAR(32),
`owner_id` BIGINT NOT NULL,
`icon` VARCHAR(512),
`entry` VARCHAR(64),
`plugin_type` VARCHAR(32),
`config` TEXT,
`enabled` INT DEFAULT 0,
`downloads` INT,
`rating` DECIMAL(20,6),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 模块: minimax-ai
-- ============================================================
CREATE TABLE IF NOT EXISTS `mm_agent_group` (
`id` BIGINT NOT NULL,
`group_id` INT,
`name` VARCHAR(128),
`description` VARCHAR(64),
`strategy` DECIMAL(20,6),
`members_json` INT,
`status` VARCHAR(32),
`owner_id` BIGINT NOT NULL,
`tags` VARCHAR(500),
`last_run_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`run_count` INT DEFAULT 0,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_ai_chat_message` (
`id` BIGINT NOT NULL,
`session_id` INT NOT NULL,
`role` INT,
`content` TEXT,
`tool_code` VARCHAR(64),
`tool_input` TEXT,
`tool_output` TEXT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_ai_chat_session` (
`id` BIGINT NOT NULL,
`session_id` INT NOT NULL,
`user_id` BIGINT NOT NULL,
`username` VARCHAR(128),
`title` VARCHAR(255),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`status` INT DEFAULT 0,
`intent` INT,
`confidence` DOUBLE,
`alternatives` INT,
`model` VARCHAR(32),
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_ai_generation_log` (
`id` BIGINT NOT NULL,
`generation_id` DOUBLE,
`user_id` BIGINT NOT NULL,
`username` VARCHAR(128),
`user_ip` VARCHAR(64),
`modality` INT,
`model_name` VARCHAR(128),
`model_version` VARCHAR(32),
`prompt` TEXT,
`negative_prompt` TEXT,
`parameters` INT,
`output_url` VARCHAR(512),
`output_size` BIGINT,
`output_hash` TEXT,
`watermarked` INT,
`watermark_text` TEXT,
`duration_ms` INT,
`status` VARCHAR(32),
`error_msg` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_ai_intent_keyword` (
`id` BIGINT NOT NULL,
`intent` INT,
`keyword` VARCHAR(64),
`weight` INT,
`is_regex` INT,
`enabled` INT DEFAULT 0,
`language` INT,
`remark` VARCHAR(500),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_ai_tool` (
`id` BIGINT NOT NULL,
`code` VARCHAR(64),
`name` VARCHAR(128),
`category` VARCHAR(32),
`description` VARCHAR(64),
`icon` VARCHAR(512),
`enabled` INT DEFAULT 0,
`builtin` INT,
`input_schema` TEXT,
`output_schema` TEXT,
`default_config` TEXT,
`impl_type` VARCHAR(32),
`impl_value` VARCHAR(64),
`rate_limit` INT,
`timeout_seconds` INT,
`role_required` INT,
`tags` VARCHAR(500),
`version` INT,
`author` INT,
`created_by` BIGINT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`status` INT DEFAULT 0,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_ai_tool_invocation` (
`id` BIGINT NOT NULL,
`tool_code` VARCHAR(64),
`user_id` BIGINT NOT NULL,
`username` VARCHAR(128),
`input_json` TEXT,
`output_json` TEXT,
`status` VARCHAR(32),
`error_message` TEXT,
`duration_ms` INT,
`ip` VARCHAR(64),
`user_agent` VARCHAR(512),
`data_source_id` BIGINT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_ai_voting_record` (
`id` BIGINT NOT NULL,
`session_id` INT NOT NULL,
`user_id` BIGINT NOT NULL,
`username` VARCHAR(128),
`question` INT,
`final_answer` INT,
`strategy` DECIMAL(20,6),
`total_votes` INT DEFAULT 0,
`agreement_rate` DECIMAL(20,6),
`model_votes` VARCHAR(32),
`duration_ms` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_audit_log` (
`id` BIGINT NOT NULL,
`trace_id` VARCHAR(64),
`user_id` BIGINT NOT NULL,
`username` VARCHAR(128),
`user_ip` VARCHAR(64),
`user_agent` VARCHAR(512),
`action` INT,
`resource_type` VARCHAR(32),
`resource_id` VARCHAR(255),
`method` VARCHAR(32),
`path` VARCHAR(255),
`request_body` TEXT,
`response_status` INT DEFAULT 0,
`result` VARCHAR(32),
`error_msg` INT,
`duration_ms` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_billing_record` (
`id` BIGINT NOT NULL,
`record_id` INT,
`user_id` BIGINT NOT NULL,
`license_id` BIGINT,
`model_entry_id` BIGINT,
`record_type` VARCHAR(32),
`amount_cents` BIGINT,
`currency` VARCHAR(8),
`status` VARCHAR(32),
`payment_method` VARCHAR(128),
`external_transaction_id` VARCHAR(128),
`description` VARCHAR(64),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_cluster_node` (
`id` BIGINT NOT NULL,
`node_id` INT,
`name` VARCHAR(128),
`address` INT,
`region` VARCHAR(64),
`zone` VARCHAR(64),
`capabilities` INT,
`total_cores` INT DEFAULT 0,
`total_memory_mb` BIGINT,
`total_gpus` INT DEFAULT 0,
`cpu_usage` DOUBLE,
`memory_usage` DOUBLE,
`gpu_usage` DOUBLE,
`active_tasks` INT DEFAULT 0,
`status` VARCHAR(32),
`is_leader` TINYINT(1),
`last_heartbeat` DATETIME DEFAULT CURRENT_TIMESTAMP,
`started_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_dashboard_metric` (
`id` BIGINT NOT NULL,
`metric` INT,
`dimension` INT,
`value` DOUBLE,
`tags` VARCHAR(500),
`timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_data_source` (
`id` BIGINT NOT NULL,
`name` VARCHAR(128),
`type` VARCHAR(32),
`jdbc_url` VARCHAR(512),
`username` VARCHAR(128),
`password` VARCHAR(255),
`driver_class` INT,
`pool_size` INT DEFAULT 0,
`min_idle` INT,
`max_lifetime` INT,
`enabled` INT DEFAULT 0,
`test_status` VARCHAR(32),
`test_message` TEXT,
`last_test_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`description` VARCHAR(64),
`tags` VARCHAR(500),
`created_by` BIGINT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_kb_chunk` (
`id` BIGINT NOT NULL,
`chunk_id` INT NOT NULL,
`doc_id` INT NOT NULL,
`kb_id` INT NOT NULL,
`seq` INT,
`content` TEXT,
`char_count` INT DEFAULT 0,
`token_count` INT DEFAULT 0,
`embedding` INT,
`embedding_model` VARCHAR(32),
`keywords` VARCHAR(64),
`summary` VARCHAR(500),
`location` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_kb_document` (
`id` BIGINT NOT NULL,
`doc_id` INT NOT NULL,
`kb_id` INT NOT NULL,
`filename` VARCHAR(128),
`mime_type` VARCHAR(32),
`size_bytes` BIGINT,
`sha256` INT,
`file_path` VARCHAR(255),
`source` VARCHAR(255),
`source_url` VARCHAR(512),
`status` VARCHAR(32),
`chunk_count` INT DEFAULT 0,
`embedding_count` INT DEFAULT 0,
`error` INT,
`tags` VARCHAR(500),
`owner_id` BIGINT NOT NULL,
`is_public` TINYINT(1),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_kb_permission` (
`id` BIGINT NOT NULL,
`kb_id` INT NOT NULL,
`subject_type` VARCHAR(255),
`subject_id` BIGINT,
`permission` INT,
`grant_by` BIGINT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_raft_log` (
`id` BIGINT NOT NULL,
`term` BIGINT,
`log_index` BIGINT,
`node_id` INT,
`command` INT,
`committed` TINYINT(1),
`committed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_model_license` (
`id` BIGINT NOT NULL,
`license_key` VARCHAR(64),
`model_entry_id` BIGINT,
`model_version_id` BIGINT,
`user_id` BIGINT NOT NULL,
`license_type` VARCHAR(32),
`status` VARCHAR(32),
`quota_calls` BIGINT,
`used_calls` BIGINT,
`start_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`expire_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`price_cents` BIGINT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_model_version` (
`id` BIGINT NOT NULL,
`version_id` INT,
`model_entry_id` BIGINT,
`version` INT,
`changelog` INT,
`file_path` VARCHAR(255),
`size_bytes` BIGINT,
`sha256` INT,
`input_schema` TEXT,
`output_schema` TEXT,
`status` VARCHAR(32),
`is_latest` TINYINT(1),
`uploader_id` BIGINT,
`backward_compatible` INT,
`metadata` TEXT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_moderation_record` (
`id` BIGINT NOT NULL,
`trace_id` VARCHAR(64),
`user_id` BIGINT NOT NULL,
`username` VARCHAR(128),
`content_type` VARCHAR(32),
`content_hash` TEXT,
`content_size` BIGINT,
`content_url` VARCHAR(512),
`moderation_status` VARCHAR(32),
`risk_level` VARCHAR(32),
`risk_labels` VARCHAR(500),
`risk_score` DECIMAL(20,6),
`moderator` VARCHAR(32),
`rejection_reason` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_multimedia_file` (
`id` BIGINT NOT NULL,
`file_id` INT,
`user_id` BIGINT NOT NULL,
`username` VARCHAR(128),
`file_name` VARCHAR(128),
`original_name` VARCHAR(128),
`file_type` VARCHAR(32),
`mime_type` VARCHAR(32),
`file_size` BIGINT,
`file_hash` INT,
`storage_path` VARCHAR(255),
`storage_type` VARCHAR(32),
`encrypted` INT,
`duration_ms` BIGINT,
`width` INT,
`height` INT,
`bitrate` INT,
`sample_rate` INT,
`channels` INT,
`codec` VARCHAR(64),
`exif` INT,
`moderation_status` VARCHAR(32),
`moderation_id` BIGINT,
`watermarked` INT,
`is_public` INT,
`access_count` INT DEFAULT 0,
`expire_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_pipeline_log` (
`id` BIGINT NOT NULL,
`session_id` INT NOT NULL,
`user_id` BIGINT NOT NULL,
`client_ip` VARCHAR(64),
`input_text` TEXT,
`input_modality` TEXT,
`intent` INT,
`output_text` TEXT,
`output_tokens` INT,
`compute_device` INT,
`compute_mode` VARCHAR(32),
`total_cost_ms` BIGINT,
`stage_costs` VARCHAR(32),
`risk_level` VARCHAR(32),
`needs_review` TINYINT(1),
`rag_hits` INT,
`tool_calls` INT,
`error_message` TEXT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_push_message` (
`id` BIGINT NOT NULL,
`message_id` TEXT,
`title` VARCHAR(255),
`body` TEXT,
`icon` VARCHAR(512),
`click_action` INT,
`data` VARCHAR(255),
`target_type` VARCHAR(32),
`target_value` INT,
`status` VARCHAR(32),
`success_count` INT DEFAULT 0,
`failure_count` INT DEFAULT 0,
`error` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_push_subscription` (
`id` BIGINT NOT NULL,
`subscription_id` VARCHAR(64),
`user_id` BIGINT NOT NULL,
`platform` INT,
`endpoint` VARCHAR(255),
`p256dh_key` VARCHAR(64),
`auth_key` VARCHAR(64),
`user_agent` VARCHAR(512),
`status` VARCHAR(32),
`last_active_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_sensitive_word` (
`id` BIGINT NOT NULL,
`word` INT,
`category` VARCHAR(32),
`level` INT,
`action` INT,
`enabled` INT DEFAULT 0,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_training_checkpoint` (
`id` BIGINT NOT NULL,
`task_id` INT,
`checkpoint_id` INT,
`name` VARCHAR(128),
`epoch` INT,
`step` INT,
`file_path` VARCHAR(255),
`size_bytes` BIGINT,
`sha256` INT,
`val_loss` DOUBLE,
`accuracy` DOUBLE,
`tags` VARCHAR(500),
`metadata` TEXT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_training_job` (
`id` BIGINT NOT NULL,
`task_id` INT,
`name` VARCHAR(128),
`model` VARCHAR(32),
`status` VARCHAR(32),
`total_epochs` INT DEFAULT 0,
`current_epoch` INT,
`current_step` INT,
`start_time_ms` BIGINT,
`end_time_ms` BIGINT,
`config` TEXT,
`error` INT,
`owner_id` BIGINT NOT NULL,
`tags` VARCHAR(500),
`last_loss` DOUBLE,
`last_val_loss` DOUBLE,
`last_accuracy` DOUBLE,
`total_steps` INT DEFAULT 0,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_training_metric` (
`id` BIGINT NOT NULL,
`task_id` INT,
`epoch` INT,
`step` INT,
`loss` DOUBLE,
`val_loss` DOUBLE,
`accuracy` DOUBLE,
`learning_rate` DOUBLE,
`elapsed_ms` BIGINT,
`timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 模块: minimax-rag
-- ============================================================
CREATE TABLE IF NOT EXISTS `mm_document` (
`id` BIGINT NOT NULL,
`kb_id` BIGINT NOT NULL,
`owner_id` BIGINT NOT NULL,
`title` VARCHAR(255),
`source_type` VARCHAR(32),
`source_uri` VARCHAR(512),
`content` TEXT,
`size_bytes` BIGINT,
`status` VARCHAR(32),
`error_msg` INT,
`chunk_count` INT DEFAULT 0,
`checksum` INT,
`tags` VARCHAR(500),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_document_chunk` (
`id` BIGINT NOT NULL,
`doc_id` BIGINT NOT NULL,
`kb_id` BIGINT NOT NULL,
`owner_id` BIGINT NOT NULL,
`chunk_index` INT DEFAULT 0 NOT NULL,
`content` TEXT,
`embedding` BLOB,
`dim` INT,
`char_count` INT DEFAULT 0,
`start_pos` INT,
`end_pos` INT,
`access_count` INT DEFAULT 0,
`last_access_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_knowledge_base` (
`id` BIGINT NOT NULL,
`owner_id` BIGINT NOT NULL,
`tenant_id` BIGINT NOT NULL,
`name` VARCHAR(128),
`description` VARCHAR(64),
`visibility` INT,
`doc_count` INT DEFAULT 0,
`chunk_count` INT DEFAULT 0,
`tags` VARCHAR(500),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 模块: minimax-analytics
-- ============================================================
CREATE TABLE IF NOT EXISTS `mm_analytics_datasource` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`name` VARCHAR(128),
`type` VARCHAR(32),
`jdbc_url` VARCHAR(512),
`username` VARCHAR(128),
`password_enc` VARCHAR(255),
`description` VARCHAR(64),
`deleted` INT DEFAULT 0,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_analytics_ingest_task` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`task_id` INT,
`filename` VARCHAR(128),
`file_type` VARCHAR(32),
`encoding` INT,
`separator` VARCHAR(255),
`file_size` BIGINT,
`status` VARCHAR(32),
`error_message` TEXT,
`quality_json` INT,
`total_rows` BIGINT,
`total_columns` BIGINT,
`columns_json` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`finished_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_analytics_nlsql_history` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`data_source_id` BIGINT,
`question` INT,
`generated_sql` TEXT,
`corrected_sql` TEXT,
`model` VARCHAR(32),
`prompt_tokens` INT,
`completion_tokens` INT,
`duration_ms` BIGINT,
`success` TINYINT(1),
`error_message` TEXT,
`feedback_rating` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_analytics_report` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`report_id` INT,
`title` VARCHAR(255),
`question` INT,
`sql_text` TEXT,
`markdown` INT,
`chart_options_json` VARCHAR(255),
`row_count` BIGINT,
`duration_ms` BIGINT,
`format` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 模块: minimax-monitor
-- ============================================================
CREATE TABLE IF NOT EXISTS `mm_alert_channel` (
`id` BIGINT NOT NULL,
`name` VARCHAR(128),
`channel_type` VARCHAR(32),
`type` VARCHAR(32),
`target` INT,
`config` TEXT,
`enabled` INT DEFAULT 0,
`priority` INT DEFAULT 0,
`description` VARCHAR(64),
`template` TEXT,
`created_by` BIGINT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_alert_event` (
`id` BIGINT NOT NULL,
`rule_id` BIGINT,
`rule_name` VARCHAR(128),
`severity` VARCHAR(32),
`metric_name` VARCHAR(128),
`message` TEXT,
`status` VARCHAR(32),
`fired_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`resolved_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`acked_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`acked_by` BIGINT,
`notes` VARCHAR(500),
`duration` BIGINT,
`silenced_until` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_alert_rule` (
`id` BIGINT NOT NULL,
`name` VARCHAR(128),
`description` VARCHAR(64),
`metric_name` VARCHAR(128),
`service` INT,
`operator` VARCHAR(255),
`severity` VARCHAR(32),
`cooldown_minutes` INT DEFAULT 0,
`enabled` INT DEFAULT 0,
`tags` VARCHAR(500),
`notify_channel` INT,
`silenced_until` DATETIME DEFAULT CURRENT_TIMESTAMP,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_metric_snapshot` (
`id` BIGINT NOT NULL,
`service` INT,
`metric_name` VARCHAR(128),
`metric_value` DECIMAL(20,6),
`tags` VARCHAR(500),
`recorded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 模块: minimax-pipeline
-- ============================================================
CREATE TABLE IF NOT EXISTS `mm_pipeline_node_log` (
`id` BIGINT NOT NULL,
`run_id` BIGINT,
`node_id` INT,
`node_type` VARCHAR(32),
`node_name` VARCHAR(128),
`status` VARCHAR(32),
`start_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
`end_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
`duration_ms` BIGINT,
`input_rows` INT,
`output_rows` INT,
`output_preview` TEXT,
`error_message` TEXT,
`config_snapshot` TEXT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_pipeline_run` (
`id` BIGINT NOT NULL,
`workflow_id` BIGINT,
`workflow_name` VARCHAR(128),
`status` VARCHAR(32),
`trigger_by` BIGINT,
`trigger_type` VARCHAR(32),
`definition_snapshot` TEXT,
`start_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
`end_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
`duration_ms` BIGINT,
`error_message` TEXT,
`result_summary` VARCHAR(500),
`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_pipeline_workflow` (
`id` BIGINT NOT NULL,
`name` VARCHAR(128),
`description` VARCHAR(64),
`definition` TEXT,
`version` INT,
`status` INT DEFAULT 0,
`create_by` BIGINT NOT NULL,
`update_by` BIGINT NOT NULL,
`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_pipeline_workflow_version` (
`id` BIGINT NOT NULL,
`workflow_id` BIGINT,
`version` INT,
`definition` TEXT,
`change_log` INT,
`create_by` BIGINT NOT NULL,
`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 模块: minimax-pipeline-fn
-- ============================================================
CREATE TABLE IF NOT EXISTS `mm_function_call_log` (
`id` BIGINT NOT NULL,
`user_id` BIGINT NOT NULL,
`session_id` BIGINT NOT NULL,
`tool_name` VARCHAR(128),
`arguments` INT,
`result` VARCHAR(32),
`status` VARCHAR(32),
`error_msg` INT,
`duration_ms` INT,
`ip` VARCHAR(64),
`user_agent` VARCHAR(512),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_function_tool` (
`id` BIGINT NOT NULL,
`name` VARCHAR(128),
`display_name` VARCHAR(128),
`description` VARCHAR(64),
`category` VARCHAR(32),
`scope` VARCHAR(32),
`owner_id` BIGINT NOT NULL,
`parameters` INT,
`endpoint` VARCHAR(255),
`http_method` VARCHAR(32),
`enabled` INT DEFAULT 0,
`tags` VARCHAR(500),
`risk_level` VARCHAR(32),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_skill_approval` (
`id` BIGINT NOT NULL,
`task_id` INT,
`user_id` BIGINT NOT NULL,
`username` VARCHAR(128),
`tool_name` VARCHAR(128),
`risk_level` VARCHAR(32),
`goal` TEXT,
`tool_params` TEXT,
`status` VARCHAR(32),
`approver_id` BIGINT,
`approver_name` VARCHAR(128),
`reason` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 模块: minimax-ws
-- ============================================================
CREATE TABLE IF NOT EXISTS `mm_collab_message` (
`id` BIGINT NOT NULL,
`room_id` INT,
`user_id` BIGINT NOT NULL,
`username` VARCHAR(128),
`nickname` VARCHAR(128),
`type` VARCHAR(32),
`content` TEXT,
`metadata` TEXT,
`client_msg_id` INT,
`broadcast` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_collab_participant` (
`id` BIGINT NOT NULL,
`room_id` INT,
`user_id` BIGINT NOT NULL,
`username` VARCHAR(128),
`nickname` VARCHAR(128),
`avatar` VARCHAR(512),
`role` INT,
`cursor_x` INT,
`cursor_y` INT,
`selection_id` INT,
`status` VARCHAR(32),
`joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`left_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`last_heartbeat` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_collab_room` (
`id` BIGINT NOT NULL,
`room_id` INT,
`name` VARCHAR(128),
`type` VARCHAR(32),
`owner_id` BIGINT NOT NULL,
`owner_name` VARCHAR(128),
`description` VARCHAR(64),
`is_public` INT,
`max_participants` INT,
`status` VARCHAR(32),
`current_participants` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`last_activity_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
`closed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 模块: minimax-admin
-- ============================================================
CREATE TABLE IF NOT EXISTS `mm_admin_audit_log` (
`id` BIGINT NOT NULL,
`actor_id` BIGINT,
`actor_name` VARCHAR(128),
`action` INT,
`resource_type` VARCHAR(32),
`resource_id` VARCHAR(255),
`detail` INT,
`result` VARCHAR(32),
`error_msg` INT,
`ip` VARCHAR(64),
`user_agent` VARCHAR(512),
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mm_audit_log_full` (
`id` BIGINT NOT NULL,
`trace_id` VARCHAR(64),
`user_id` BIGINT NOT NULL,
`username` VARCHAR(128),
`user_ip` VARCHAR(64),
`user_agent` VARCHAR(512),
`action` INT,
`resource_type` VARCHAR(32),
`resource_id` VARCHAR(255),
`method` VARCHAR(32),
`path` VARCHAR(255),
`request_body` TEXT,
`response_status` INT DEFAULT 0,
`result` VARCHAR(32),
`error_msg` INT,
`duration_ms` INT,
`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 外键约束 (后加，避免循环依赖)
-- ============================================================
ALTER TABLE `mm_chat_message` ADD CONSTRAINT `fk_chat_message_session_id` FOREIGN KEY (`session_id`) REFERENCES `mm_chat_session` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_chat_message` ADD CONSTRAINT `fk_chat_message_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_chat_session` ADD CONSTRAINT `fk_chat_session_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_chat_session` ADD CONSTRAINT `fk_chat_session_tenant_id` FOREIGN KEY (`tenant_id`) REFERENCES `mm_tenant` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_sys_user_role` ADD CONSTRAINT `fk_sys_user_role_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_sys_user_role` ADD CONSTRAINT `fk_sys_user_role_role_id` FOREIGN KEY (`role_id`) REFERENCES `mm_sys_role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_sys_user` ADD CONSTRAINT `fk_sys_user_tenant_id` FOREIGN KEY (`tenant_id`) REFERENCES `mm_tenant` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_model_config` ADD CONSTRAINT `fk_model_config_provider_id` FOREIGN KEY (`provider_id`) REFERENCES `mm_model_provider` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_model_quota` ADD CONSTRAINT `fk_model_quota_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_model_quota` ADD CONSTRAINT `fk_model_quota_model_id` FOREIGN KEY (`model_id`) REFERENCES `mm_model_config` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_user_api_key` ADD CONSTRAINT `fk_user_api_key_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_notification` ADD CONSTRAINT `fk_notification_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_auth_login_log` ADD CONSTRAINT `fk_auth_login_log_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_auth_refresh_token` ADD CONSTRAINT `fk_auth_refresh_token_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_knowledge_base` ADD CONSTRAINT `fk_knowledge_base_owner_id` FOREIGN KEY (`owner_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_knowledge_base` ADD CONSTRAINT `fk_knowledge_base_tenant_id` FOREIGN KEY (`tenant_id`) REFERENCES `mm_tenant` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_document` ADD CONSTRAINT `fk_document_kb_id` FOREIGN KEY (`kb_id`) REFERENCES `mm_knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_document` ADD CONSTRAINT `fk_document_owner_id` FOREIGN KEY (`owner_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_document_chunk` ADD CONSTRAINT `fk_document_chunk_doc_id` FOREIGN KEY (`doc_id`) REFERENCES `mm_document` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_document_chunk` ADD CONSTRAINT `fk_document_chunk_kb_id` FOREIGN KEY (`kb_id`) REFERENCES `mm_knowledge_base` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_document_chunk` ADD CONSTRAINT `fk_document_chunk_owner_id` FOREIGN KEY (`owner_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_kg_entity` ADD CONSTRAINT `fk_kg_entity_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_kg_relation` ADD CONSTRAINT `fk_kg_relation_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_kg_relation` ADD CONSTRAINT `fk_kg_relation_from_entity` FOREIGN KEY (`from_entity`) REFERENCES `mm_kg_entity` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_kg_relation` ADD CONSTRAINT `fk_kg_relation_to_entity` FOREIGN KEY (`to_entity`) REFERENCES `mm_kg_entity` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_plugin` ADD CONSTRAINT `fk_plugin_owner_id` FOREIGN KEY (`owner_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_ai_chat_session` ADD CONSTRAINT `fk_ai_chat_session_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_ai_chat_message` ADD CONSTRAINT `fk_ai_chat_message_session_id` FOREIGN KEY (`session_id`) REFERENCES `mm_ai_chat_session` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_ai_tool` ADD CONSTRAINT `fk_ai_tool_id` FOREIGN KEY (`id`) REFERENCES `mm_id` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_audit_log` ADD CONSTRAINT `fk_audit_log_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_billing_record` ADD CONSTRAINT `fk_billing_record_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_alert_rule` ADD CONSTRAINT `fk_alert_rule_id` FOREIGN KEY (`id`) REFERENCES `mm_id` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_pipeline_workflow` ADD CONSTRAINT `fk_pipeline_workflow_id` FOREIGN KEY (`id`) REFERENCES `mm_id` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_pipeline_workflow_version` ADD CONSTRAINT `fk_pipeline_workflow_version_workflow_id` FOREIGN KEY (`workflow_id`) REFERENCES `mm_pipeline_workflow` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_pipeline_run` ADD CONSTRAINT `fk_pipeline_run_workflow_id` FOREIGN KEY (`workflow_id`) REFERENCES `mm_pipeline_workflow` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_pipeline_node_log` ADD CONSTRAINT `fk_pipeline_node_log_run_id` FOREIGN KEY (`run_id`) REFERENCES `mm_pipeline_run` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_function_tool` ADD CONSTRAINT `fk_function_tool_id` FOREIGN KEY (`id`) REFERENCES `mm_id` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_skill_approval` ADD CONSTRAINT `fk_skill_approval_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_skill_approval` ADD CONSTRAINT `fk_skill_approval_workflow_id` FOREIGN KEY (`workflow_id`) REFERENCES `mm_pipeline_workflow` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_admin_audit_log` ADD CONSTRAINT `fk_admin_audit_log_actor_id` FOREIGN KEY (`actor_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_collab_room` ADD CONSTRAINT `fk_collab_room_id` FOREIGN KEY (`id`) REFERENCES `mm_id` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_collab_participant` ADD CONSTRAINT `fk_collab_participant_room_id` FOREIGN KEY (`room_id`) REFERENCES `mm_collab_room` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_collab_participant` ADD CONSTRAINT `fk_collab_participant_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_collab_message` ADD CONSTRAINT `fk_collab_message_room_id` FOREIGN KEY (`room_id`) REFERENCES `mm_collab_room` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_collab_message` ADD CONSTRAINT `fk_collab_message_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_training_task` ADD CONSTRAINT `fk_training_task_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_training_metric` ADD CONSTRAINT `fk_training_metric_task_id` FOREIGN KEY (`task_id`) REFERENCES `mm_training_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_oauth_binding` ADD CONSTRAINT `fk_oauth_binding_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_oauth_app_config` ADD CONSTRAINT `fk_oauth_app_config_id` FOREIGN KEY (`id`) REFERENCES `mm_id` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_wechat_config` ADD CONSTRAINT `fk_wechat_config_id` FOREIGN KEY (`id`) REFERENCES `mm_id` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_wechat_scan_session` ADD CONSTRAINT `fk_wechat_scan_session_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `mm_wechat_user_binding` ADD CONSTRAINT `fk_wechat_user_binding_user_id` FOREIGN KEY (`user_id`) REFERENCES `mm_sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;