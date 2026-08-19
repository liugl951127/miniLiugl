-- ============================================================
-- MiniMax Platform V6.8.1 建表脚本
-- 自动生成 from entity classes
-- 数据库: utf8mb4, 引擎: InnoDB
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- auth_login_log (AuthLoginLog)
-- ============================================================
DROP TABLE IF EXISTS `auth_login_log`;
CREATE TABLE `auth_login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `username` VARCHAR(255) NULL,
  `ip` VARCHAR(255) NULL,
  `user_agent` VARCHAR(255) NULL,
  `status` INT NULL,
  `message` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- auth_refresh_token (AuthRefreshToken)
-- ============================================================
DROP TABLE IF EXISTS `auth_refresh_token`;
CREATE TABLE `auth_refresh_token` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `token` VARCHAR(255) NULL,
  `expires_at` TIMESTAMP NULL,
  `revoked` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- notification (Notification)
-- ============================================================
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `type` VARCHAR(255) NULL,
  `title` VARCHAR(255) NULL,
  `content` VARCHAR(255) NULL,
  `is_read` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `notification` (id, user_id, type, title, content, is_read, created_at) VALUES
(1, 1, 'SYSTEM', '欢迎使用 MiniMax Platform', '平台已就绪，欢迎开始使用！', 0, NOW()),
(2, 2, 'TASK', '训练任务完成', '模型训练任务已完成，准确率 94.2%', 0, NOW()),
(3, 1, 'SYSTEM', '新功能上线', 'V6.8.1 版本已发布，包含多项优化', 1, NOW());

-- ============================================================
-- oauth_app_config (OAuthAppConfig)
-- ============================================================
DROP TABLE IF EXISTS `oauth_app_config`;
CREATE TABLE `oauth_app_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `platform` VARCHAR(255) NULL,
  `app_type` VARCHAR(255) NULL,
  `app_id` VARCHAR(255) NULL,
  `app_secret` VARCHAR(255) NULL,
  `public_key` VARCHAR(255) NULL,
  `redirect_uri` VARCHAR(255) NULL,
  `scopes` VARCHAR(255) NULL,
  `enabled` INT NULL,
  `extra_config` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `oauth_app_config` (id, platform, app_type, app_id, app_secret, enabled, created_at) VALUES
(1, 'wechat', 'WEB', 'wx_app_001', 'secret_xxx', 1, NOW());

-- ============================================================
-- oauth_binding (OAuthBinding)
-- ============================================================
DROP TABLE IF EXISTS `oauth_binding`;
CREATE TABLE `oauth_binding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `platform` VARCHAR(255) NULL,
  `app_type` VARCHAR(255) NULL,
  `openid` VARCHAR(255) NULL,
  `unionid` VARCHAR(255) NULL,
  `nickname` VARCHAR(255) NULL,
  `avatar` VARCHAR(255) NULL,
  `access_token` VARCHAR(255) NULL,
  `refresh_token` VARCHAR(255) NULL,
  `token_expires_at` TIMESTAMP NULL,
  `raw_data` VARCHAR(255) NULL,
  `bound_at` TIMESTAMP NULL,
  `last_login_at` TIMESTAMP NULL,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- sys_role (SysRole)
-- ============================================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(255) NULL,
  `name` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `sort` INT NULL,
  `enabled` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `sys_role` (id, role_name, role_key, description, created_at) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '拥有全部权限', NOW()),
(2, '普通用户', 'USER', '基础权限', NOW()),
(3, '运维人员', 'OPERATOR', '系统运维权限', NOW());

-- ============================================================
-- sys_user (SysUser)
-- ============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(255) NULL,
  `password` VARCHAR(255) NULL,
  `nickname` VARCHAR(255) NULL,
  `email` VARCHAR(255) NULL,
  `phone` VARCHAR(255) NULL,
  `avatar` VARCHAR(255) NULL,
  `gender` INT NULL,
  `status` INT NULL,
  `last_login_ip` VARCHAR(255) NULL,
  `last_login_at` TIMESTAMP NULL,
  `tenant_id` BIGINT NULL,
  `remark` VARCHAR(255) NULL,
  `wechat_openid` VARCHAR(255) NULL,
  `wechat_unionid` VARCHAR(255) NULL,
  `wechat_nickname` VARCHAR(255) NULL,
  `wechat_avatar` VARCHAR(255) NULL,
  `wechat_bound_at` TIMESTAMP NULL,
  `qq_openid` VARCHAR(255) NULL,
  `qq_unionid` VARCHAR(255) NULL,
  `qq_nickname` VARCHAR(255) NULL,
  `qq_avatar` VARCHAR(255) NULL,
  `qq_bound_at` TIMESTAMP NULL,
  `alipay_openid` VARCHAR(255) NULL,
  `alipay_user_id` VARCHAR(255) NULL,
  `alipay_nickname` VARCHAR(255) NULL,
  `alipay_avatar` VARCHAR(255) NULL,
  `alipay_bound_at` TIMESTAMP NULL,
  `created_by` BIGINT DEFAULT CURRENT_TIMESTAMP,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `sys_user` (id, username, password, email, phone, role, status, created_at) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt/Hzu', 'admin@minimax.com', '13800138000', 'SUPER_ADMIN', 1, NOW()),
(2, 'user01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt/Hzu', 'user01@minimax.com', '13800138001', 'USER', 1, NOW()),
(3, 'operator', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt/Hzu', 'op@minimax.com', '13800138002', 'OPERATOR', 1, NOW());

-- ============================================================
-- sys_user_role (SysUserRole)
-- ============================================================
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NULL,
  `role_id` BIGINT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `sys_user_role` (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3);

-- ============================================================
-- tenant (Tenant)
-- ============================================================
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(255) NULL,
  `name` VARCHAR(255) NULL,
  `plan` VARCHAR(255) NULL,
  `status` INT NULL,
  `max_users` INT NULL,
  `max_models` INT NULL,
  `qps_limit` INT NULL,
  `monthly_quota` BIGINT NULL,
  `used_quota` BIGINT NULL,
  `expire_at` TIMESTAMP NULL,
  `contact_email` VARCHAR(255) NULL,
  `contact_phone` VARCHAR(255) NULL,
  `remark` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- unionid_relations (UnionidRelations)
-- ============================================================
DROP TABLE IF EXISTS `unionid_relations`;
CREATE TABLE `unionid_relations` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `unionid` VARCHAR(255) NULL,
  `platform` VARCHAR(255) NULL,
  `first_seen_at` TIMESTAMP NULL,
  `last_seen_at` TIMESTAMP NULL,
  `binding_count` INT NULL,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- user_api_key (UserApiKey)
-- ============================================================
DROP TABLE IF EXISTS `user_api_key`;
CREATE TABLE `user_api_key` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `name` VARCHAR(255) NULL,
  `key_hash` VARCHAR(255) NULL,
  `key_prefix` VARCHAR(255) NULL,
  `scopes` VARCHAR(255) NULL,
  `expires_at` TIMESTAMP NULL,
  `last_used_at` TIMESTAMP NULL,
  `use_count` BIGINT NULL,
  `enabled` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `user_api_key` (user_id, name, key_hash, key_prefix, scopes, enabled, use_count, created_at) VALUES
(1, '测试 Key', SHA2('mmx_testkey001234567890abcdef', 256), 'mmx_test', 'chat:send,chat:stream', 1, 0, NOW()),
(2, '生产环境', SHA2('mmx_prodkey1234567890abcdef', 256), 'mmx_prod', 'chat:send,chat:stream,agent:run', 1, 47, NOW());

-- ============================================================
-- wechat_config (WechatConfig)
-- ============================================================
DROP TABLE IF EXISTS `wechat_config`;
CREATE TABLE `wechat_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `app_type` VARCHAR(255) NULL,
  `app_id` VARCHAR(255) NULL,
  `app_secret` VARCHAR(255) NULL,
  `token` VARCHAR(255) NULL,
  `aes_key` VARCHAR(255) NULL,
  `redirect_uri` VARCHAR(255) NULL,
  `scope` VARCHAR(255) NULL,
  `enabled` INT NULL,
  `remark` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- wechat_scan_session (WechatScanSession)
-- ============================================================
DROP TABLE IF EXISTS `wechat_scan_session`;
CREATE TABLE `wechat_scan_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `ticket` VARCHAR(255) NULL,
  `scene_id` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `openid` VARCHAR(255) NULL,
  `unionid` VARCHAR(255) NULL,
  `nickname` VARCHAR(255) NULL,
  `avatar` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `access_token` VARCHAR(255) NULL,
  `refresh_token` VARCHAR(255) NULL,
  `client_ip` VARCHAR(255) NULL,
  `user_agent` VARCHAR(255) NULL,
  `expires_at` TIMESTAMP NULL,
  `confirmed_at` TIMESTAMP NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- wechat_user_binding (WechatUserBinding)
-- ============================================================
DROP TABLE IF EXISTS `wechat_user_binding`;
CREATE TABLE `wechat_user_binding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `openid` VARCHAR(255) NULL,
  `unionid` VARCHAR(255) NULL,
  `app_type` VARCHAR(255) NULL,
  `nickname` VARCHAR(255) NULL,
  `avatar` VARCHAR(255) NULL,
  `bound_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `last_login_at` TIMESTAMP NULL,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- admin_audit_log (AdminAuditLog)
-- ============================================================
DROP TABLE IF EXISTS `admin_audit_log`;
CREATE TABLE `admin_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `actor_id` BIGINT NULL,
  `actor_name` VARCHAR(255) NULL,
  `action` VARCHAR(255) NULL,
  `resource_type` VARCHAR(255) NULL,
  `resource_id` VARCHAR(255) NULL,
  `detail` VARCHAR(255) NULL,
  `result` VARCHAR(255) NULL,
  `error_msg` VARCHAR(255) NULL,
  `ip` VARCHAR(255) NULL,
  `user_agent` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `admin_audit_log` (id, user_id, action, resource, ip, created_at) VALUES
(1, 1, 'CREATE', 'training_task', '127.0.0.1', NOW()),
(2, 2, 'QUERY', 'ai_chat_session', '127.0.0.1', NOW()),
(3, 1, 'DELETE', 'knowledge_base', '127.0.0.1', NOW());

-- ============================================================
-- audit_log_full (AuditLogFull)
-- ============================================================
DROP TABLE IF EXISTS `audit_log_full`;
CREATE TABLE `audit_log_full` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `trace_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `username` VARCHAR(255) NULL,
  `user_ip` VARCHAR(255) NULL,
  `user_agent` VARCHAR(255) NULL,
  `action` VARCHAR(255) NULL,
  `resource_type` VARCHAR(255) NULL,
  `resource_id` VARCHAR(255) NULL,
  `method` VARCHAR(255) NULL,
  `path` VARCHAR(255) NULL,
  `request_body` VARCHAR(255) NULL,
  `response_status` INT NULL,
  `result` VARCHAR(255) NULL,
  `error_msg` VARCHAR(255) NULL,
  `duration_ms` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- agent_group (AgentGroup)
-- ============================================================
DROP TABLE IF EXISTS `agent_group`;
CREATE TABLE `agent_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `group_id` VARCHAR(255) NULL,
  `name` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `strategy` VARCHAR(255) NULL,
  `members_json` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `owner_id` BIGINT NULL,
  `tags` VARCHAR(255) NULL,
  `last_run_at` TIMESTAMP NULL,
  `run_count` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `agent_group` (id, name, description, visibility, created_at) VALUES
(1, '客服组', '在线客服智能体组', 'PRIVATE', NOW()),
(2, '审核组', '内容审核智能体组', 'PRIVATE', NOW());

-- ============================================================
-- ai_chat_message (AiChatMessage)
-- ============================================================
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` VARCHAR(255) NULL,
  `role` VARCHAR(255) NULL,
  `content` VARCHAR(255) NULL,
  `tool_code` VARCHAR(255) NULL,
  `tool_input` VARCHAR(255) NULL,
  `tool_output` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `ai_chat_message` (id, session_id, role, content, created_at) VALUES
(1, 1, 'user', '你好，请介绍一下你自己', NOW()),
(2, 1, 'assistant', '我是 MiniMax AI 助手，可以帮助你完成各种任务。', NOW());

-- ============================================================
-- ai_chat_session (AiChatSession)
-- ============================================================
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `username` VARCHAR(255) NULL,
  `title` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `intent` VARCHAR(255) NULL,
  `confidence` DOUBLE NULL,
  `alternatives` VARCHAR(255) NULL,
  `model` VARCHAR(255) NULL,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `ai_chat_session` (id, user_id, title, model, status, created_at) VALUES
(1, 1, '测试会话', 'gpt-4o-mini', 'ACTIVE', NOW()),
(2, 2, '客服问答', 'gpt-4o', 'ACTIVE', NOW());

-- ============================================================
-- ai_generation_log (AiGenerationLog)
-- ============================================================
DROP TABLE IF EXISTS `ai_generation_log`;
CREATE TABLE `ai_generation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `generation_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `username` VARCHAR(255) NULL,
  `user_ip` VARCHAR(255) NULL,
  `modality` VARCHAR(255) NULL,
  `model_name` VARCHAR(255) NULL,
  `model_version` VARCHAR(255) NULL,
  `prompt` VARCHAR(255) NULL,
  `negative_prompt` VARCHAR(255) NULL,
  `parameters` VARCHAR(255) NULL,
  `output_url` VARCHAR(255) NULL,
  `output_size` BIGINT NULL,
  `output_hash` VARCHAR(255) NULL,
  `watermarked` INT NULL,
  `watermark_text` VARCHAR(255) NULL,
  `duration_ms` INT NULL,
  `status` VARCHAR(255) NULL,
  `error_msg` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- ai_intent_keyword (AiIntentKeyword)
-- ============================================================
DROP TABLE IF EXISTS `ai_intent_keyword`;
CREATE TABLE `ai_intent_keyword` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `intent` VARCHAR(255) NULL,
  `keyword` VARCHAR(255) NULL,
  `weight` INT NULL,
  `is_regex` INT NULL,
  `enabled` INT NULL,
  `language` VARCHAR(255) NULL,
  `remark` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- ai_tool (AiTool)
-- ============================================================
DROP TABLE IF EXISTS `ai_tool`;
CREATE TABLE `ai_tool` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(255) NULL,
  `name` VARCHAR(255) NULL,
  `category` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `icon` VARCHAR(255) NULL,
  `enabled` INT NULL,
  `builtin` INT NULL,
  `input_schema` VARCHAR(255) NULL,
  `output_schema` VARCHAR(255) NULL,
  `default_config` VARCHAR(255) NULL,
  `impl_type` VARCHAR(255) NULL,
  `impl_value` VARCHAR(255) NULL,
  `rate_limit` INT NULL,
  `timeout_seconds` INT NULL,
  `role_required` VARCHAR(255) NULL,
  `tags` VARCHAR(255) NULL,
  `version` VARCHAR(255) NULL,
  `author` VARCHAR(255) NULL,
  `created_by` BIGINT DEFAULT CURRENT_TIMESTAMP,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `ai_tool` (id, name, category, description, endpoint, enabled, created_at) VALUES
(1, 'weather', 'utility', '天气查询', '/api/weather', 1, NOW()),
(2, 'search', 'search', '搜索', '/api/search', 1, NOW());

-- ============================================================
-- ai_tool_invocation (AiToolInvocation)
-- ============================================================
DROP TABLE IF EXISTS `ai_tool_invocation`;
CREATE TABLE `ai_tool_invocation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tool_code` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `username` VARCHAR(255) NULL,
  `input_json` VARCHAR(255) NULL,
  `output_json` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `error_message` VARCHAR(255) NULL,
  `duration_ms` INT NULL,
  `ip` VARCHAR(255) NULL,
  `user_agent` VARCHAR(255) NULL,
  `data_source_id` BIGINT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- ai_voting_record (AiVotingRecord)
-- ============================================================
DROP TABLE IF EXISTS `ai_voting_record`;
CREATE TABLE `ai_voting_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `username` VARCHAR(255) NULL,
  `question` VARCHAR(255) NULL,
  `final_answer` VARCHAR(255) NULL,
  `strategy` VARCHAR(255) NULL,
  `total_votes` INT NULL,
  `agreement_rate` DECIMAL(20,4) NULL,
  `model_votes` VARCHAR(255) NULL,
  `duration_ms` INT NULL,
  `notify_email` VARCHAR(255) NULL COMMENT '投票结束通知邮箱 (Day 43)',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `ai_voting_record` (id, question, final_answer, strategy, total_votes, model_votes, notify_email, created_at) VALUES
(1, '2+2等于多少？', '4', 'majority', 4, '[{"model":"gpt-4","answer":"4","confidence":0.99}]', NULL, NOW()),
(2, '北京是哪个国家的首都？', '中国', 'majority', 4, '[{"model":"gpt-4","answer":"中国","confidence":1.0}]', NULL, NOW());

-- ============================================================
-- audit_log (AuditLog)
-- ============================================================
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `trace_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `username` VARCHAR(255) NULL,
  `user_ip` VARCHAR(255) NULL,
  `user_agent` VARCHAR(255) NULL,
  `action` VARCHAR(255) NULL,
  `resource_type` VARCHAR(255) NULL,
  `resource_id` VARCHAR(255) NULL,
  `method` VARCHAR(255) NULL,
  `path` VARCHAR(255) NULL,
  `request_body` VARCHAR(255) NULL,
  `response_status` INT NULL,
  `result` VARCHAR(255) NULL,
  `error_msg` VARCHAR(255) NULL,
  `duration_ms` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- billing_record (BillingRecord)
-- ============================================================
DROP TABLE IF EXISTS `billing_record`;
CREATE TABLE `billing_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `license_id` BIGINT NULL,
  `model_entry_id` BIGINT NULL,
  `record_type` VARCHAR(255) NULL,
  `amount_cents` BIGINT NULL,
  `currency` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `payment_method` VARCHAR(255) NULL,
  `external_transaction_id` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- cluster_node (ClusterNode)
-- ============================================================
DROP TABLE IF EXISTS `cluster_node`;
CREATE TABLE `cluster_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `node_id` VARCHAR(255) NULL,
  `name` VARCHAR(255) NULL,
  `address` VARCHAR(255) NULL,
  `region` VARCHAR(255) NULL,
  `zone` VARCHAR(255) NULL,
  `capabilities` VARCHAR(255) NULL,
  `total_cores` INT NULL,
  `total_memory_mb` BIGINT NULL,
  `total_gpus` INT NULL,
  `cpu_usage` DOUBLE NULL,
  `memory_usage` DOUBLE NULL,
  `gpu_usage` DOUBLE NULL,
  `active_tasks` INT NULL,
  `status` VARCHAR(255) NULL,
  `is_leader` TINYINT(1) NULL,
  `last_heartbeat` TIMESTAMP NULL,
  `started_at` TIMESTAMP NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `cluster_node` (id, node_name, host, port, status, cpu_usage, memory_usage, gpu_count, labels, created_at) VALUES
(1, 'node-01', '10.0.0.11', 8080, 'ACTIVE', 45.2, 62.1, 2, '{"role":"worker"}', NOW()),
(2, 'node-02', '10.0.0.12', 8080, 'ACTIVE', 38.7, 55.3, 2, '{"role":"worker"}', NOW());

-- ============================================================
-- dashboard_metric (DashboardMetric)
-- ============================================================
DROP TABLE IF EXISTS `dashboard_metric`;
CREATE TABLE `dashboard_metric` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `metric` VARCHAR(255) NULL,
  `dimension` VARCHAR(255) NULL,
  `value` DOUBLE NULL,
  `tags` VARCHAR(255) NULL,
  `timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- data_source (DbDataSource)
-- ============================================================
DROP TABLE IF EXISTS `data_source`;
CREATE TABLE `data_source` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NULL,
  `type` VARCHAR(255) NULL,
  `jdbc_url` VARCHAR(255) NULL,
  `username` VARCHAR(255) NULL,
  `password` VARCHAR(255) NULL,
  `driver_class` VARCHAR(255) NULL,
  `pool_size` INT NULL,
  `min_idle` INT NULL,
  `max_lifetime` INT NULL,
  `enabled` INT NULL,
  `test_status` VARCHAR(255) NULL,
  `test_message` VARCHAR(255) NULL,
  `last_test_at` TIMESTAMP NULL,
  `description` VARCHAR(255) NULL,
  `tags` VARCHAR(255) NULL,
  `created_by` BIGINT DEFAULT CURRENT_TIMESTAMP,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- kb_chunk (KbChunk)
-- ============================================================
DROP TABLE IF EXISTS `kb_chunk`;
CREATE TABLE `kb_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `chunk_id` VARCHAR(255) NULL,
  `doc_id` VARCHAR(255) NULL,
  `kb_id` VARCHAR(255) NULL,
  `seq` INT NULL,
  `content` VARCHAR(255) NULL,
  `char_count` INT NULL,
  `token_count` INT NULL,
  `embedding` VARCHAR(255) NULL,
  `embedding_model` VARCHAR(255) NULL,
  `keywords` VARCHAR(255) NULL,
  `summary` VARCHAR(255) NULL,
  `location` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- kb_document (KbDocument)
-- ============================================================
DROP TABLE IF EXISTS `kb_document`;
CREATE TABLE `kb_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `doc_id` VARCHAR(255) NULL,
  `kb_id` VARCHAR(255) NULL,
  `filename` VARCHAR(255) NULL,
  `mime_type` VARCHAR(255) NULL,
  `size_bytes` BIGINT NULL,
  `sha256` VARCHAR(255) NULL,
  `file_path` VARCHAR(255) NULL,
  `source` VARCHAR(255) NULL,
  `source_url` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `chunk_count` INT NULL,
  `embedding_count` INT NULL,
  `error` VARCHAR(255) NULL,
  `tags` VARCHAR(255) NULL,
  `owner_id` BIGINT NULL,
  `is_public` TINYINT(1) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- kb_permission (KbPermission)
-- ============================================================
DROP TABLE IF EXISTS `kb_permission`;
CREATE TABLE `kb_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `kb_id` VARCHAR(255) NULL,
  `subject_type` VARCHAR(255) NULL,
  `subject_id` BIGINT NULL,
  `permission` VARCHAR(255) NULL,
  `grant_by` BIGINT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- model_license (ModelLicense)
-- ============================================================
DROP TABLE IF EXISTS `model_license`;
CREATE TABLE `model_license` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `license_key` VARCHAR(255) NULL,
  `model_entry_id` BIGINT NULL,
  `model_version_id` BIGINT NULL,
  `user_id` BIGINT NULL,
  `license_type` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `quota_calls` BIGINT NULL,
  `used_calls` BIGINT NULL,
  `start_at` TIMESTAMP NULL,
  `expire_at` TIMESTAMP NULL,
  `price_cents` BIGINT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- model_version (ModelVersion)
-- ============================================================
DROP TABLE IF EXISTS `model_version`;
CREATE TABLE `model_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `version_id` VARCHAR(255) NULL,
  `model_entry_id` BIGINT NULL,
  `version` VARCHAR(255) NULL,
  `changelog` VARCHAR(255) NULL,
  `file_path` VARCHAR(255) NULL,
  `size_bytes` BIGINT NULL,
  `sha256` VARCHAR(255) NULL,
  `input_schema` VARCHAR(255) NULL,
  `output_schema` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `is_latest` TINYINT(1) NULL,
  `uploader_id` BIGINT NULL,
  `backward_compatible` VARCHAR(255) NULL,
  `metadata` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- moderation_record (ModerationRecord)
-- ============================================================
DROP TABLE IF EXISTS `moderation_record`;
CREATE TABLE `moderation_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `trace_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `username` VARCHAR(255) NULL,
  `content_type` VARCHAR(255) NULL,
  `content_hash` VARCHAR(255) NULL,
  `content_size` BIGINT NULL,
  `content_url` VARCHAR(255) NULL,
  `moderation_status` VARCHAR(255) NULL,
  `risk_level` VARCHAR(255) NULL,
  `risk_labels` VARCHAR(255) NULL,
  `risk_score` DECIMAL(20,4) NULL,
  `moderator` VARCHAR(255) NULL,
  `rejection_reason` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- multimedia_file (MultimediaFile)
-- ============================================================
DROP TABLE IF EXISTS `multimedia_file`;
CREATE TABLE `multimedia_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `file_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `username` VARCHAR(255) NULL,
  `file_name` VARCHAR(255) NULL,
  `original_name` VARCHAR(255) NULL,
  `file_type` VARCHAR(255) NULL,
  `mime_type` VARCHAR(255) NULL,
  `file_size` BIGINT NULL,
  `file_hash` VARCHAR(255) NULL,
  `storage_path` VARCHAR(255) NULL,
  `storage_type` VARCHAR(255) NULL,
  `encrypted` INT NULL,
  `duration_ms` BIGINT NULL,
  `width` INT NULL,
  `height` INT NULL,
  `bitrate` INT NULL,
  `sample_rate` INT NULL,
  `channels` INT NULL,
  `codec` VARCHAR(255) NULL,
  `exif` VARCHAR(255) NULL,
  `moderation_status` VARCHAR(255) NULL,
  `moderation_id` BIGINT NULL,
  `watermarked` INT NULL,
  `is_public` INT NULL,
  `access_count` INT NULL,
  `expire_at` TIMESTAMP NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- pipeline_log (PipelineLog)
-- ============================================================
DROP TABLE IF EXISTS `pipeline_log`;
CREATE TABLE `pipeline_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `client_ip` VARCHAR(255) NULL,
  `input_text` VARCHAR(255) NULL,
  `input_modality` VARCHAR(255) NULL,
  `intent` VARCHAR(255) NULL,
  `output_text` VARCHAR(255) NULL,
  `output_tokens` INT NULL,
  `compute_device` VARCHAR(255) NULL,
  `compute_mode` VARCHAR(255) NULL,
  `total_cost_ms` BIGINT NULL,
  `stage_costs` VARCHAR(255) NULL,
  `risk_level` VARCHAR(255) NULL,
  `needs_review` TINYINT(1) NULL,
  `rag_hits` INT NULL,
  `tool_calls` INT NULL,
  `error_message` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- push_message (PushMessage)
-- ============================================================
DROP TABLE IF EXISTS `push_message`;
CREATE TABLE `push_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `message_id` VARCHAR(255) NULL,
  `title` VARCHAR(255) NULL,
  `body` VARCHAR(255) NULL,
  `icon` VARCHAR(255) NULL,
  `click_action` VARCHAR(255) NULL,
  `data` VARCHAR(255) NULL,
  `target_type` VARCHAR(255) NULL,
  `target_value` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `success_count` INT NULL,
  `failure_count` INT NULL,
  `error` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- push_subscription (PushSubscription)
-- ============================================================
DROP TABLE IF EXISTS `push_subscription`;
CREATE TABLE `push_subscription` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `subscription_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `platform` VARCHAR(255) NULL,
  `endpoint` VARCHAR(255) NULL,
  `p256dh_key` VARCHAR(255) NULL,
  `auth_key` VARCHAR(255) NULL,
  `user_agent` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `last_active_at` TIMESTAMP NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- raft_log (LogEntry)
-- ============================================================
DROP TABLE IF EXISTS `raft_log`;
CREATE TABLE `raft_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `term` BIGINT NULL,
  `log_index` BIGINT NULL,
  `node_id` VARCHAR(255) NULL,
  `command` VARCHAR(255) NULL,
  `committed` TINYINT(1) NULL,
  `committed_at` TIMESTAMP NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- sensitive_word (SensitiveWord)
-- ============================================================
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `word` VARCHAR(255) NULL,
  `category` VARCHAR(255) NULL,
  `level` VARCHAR(255) NULL,
  `action` VARCHAR(255) NULL,
  `enabled` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `sensitive_word` (id, word, category, severity, action, created_at) VALUES
(1, '色情', '政治', 'HIGH', 'BLOCK', NOW()),
(2, '暴力', '暴力', 'MEDIUM', 'REVIEW', NOW());

-- ============================================================
-- training_checkpoint (TrainingCheckpoint)
-- ============================================================
DROP TABLE IF EXISTS `training_checkpoint`;
CREATE TABLE `training_checkpoint` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` VARCHAR(255) NULL,
  `checkpoint_id` VARCHAR(255) NULL,
  `name` VARCHAR(255) NULL,
  `epoch` INT NULL,
  `step` INT NULL,
  `file_path` VARCHAR(255) NULL,
  `size_bytes` BIGINT NULL,
  `sha256` VARCHAR(255) NULL,
  `val_loss` DOUBLE NULL,
  `accuracy` DOUBLE NULL,
  `tags` VARCHAR(255) NULL,
  `metadata` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- training_job (TrainingJob)
-- ============================================================
DROP TABLE IF EXISTS `training_job`;
CREATE TABLE `training_job` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` VARCHAR(255) NULL,
  `name` VARCHAR(255) NULL,
  `model` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `total_epochs` INT NULL,
  `current_epoch` INT NULL,
  `current_step` INT NULL,
  `start_time_ms` BIGINT NULL,
  `end_time_ms` BIGINT NULL,
  `config` VARCHAR(255) NULL,
  `error` VARCHAR(255) NULL,
  `owner_id` BIGINT NULL,
  `tags` VARCHAR(255) NULL,
  `last_loss` DOUBLE NULL,
  `last_val_loss` DOUBLE NULL,
  `last_accuracy` DOUBLE NULL,
  `total_steps` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- training_metric (TrainingMetric)
-- ============================================================
DROP TABLE IF EXISTS `training_metric`;
CREATE TABLE `training_metric` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` VARCHAR(255) NULL,
  `epoch` INT NULL,
  `step` INT NULL,
  `loss` DOUBLE NULL,
  `val_loss` DOUBLE NULL,
  `accuracy` DOUBLE NULL,
  `learning_rate` DOUBLE NULL,
  `elapsed_ms` BIGINT NULL,
  `timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `training_metric` (id, task_id, step, loss, accuracy, learning_rate, elapsed_ms) VALUES
(1, 1, 100, 2.341, 0.623, 0.0003, 15200),
(2, 1, 200, 1.892, 0.701, 0.0003, 30400),
(3, 1, 300, 1.521, 0.758, 0.0003, 45600),
(4, 1, 400, 1.234, 0.801, 0.0003, 60800),
(5, 1, 500, 0.987, 0.845, 0.0003, 76000);

-- ============================================================
-- model_battle_log (ModelBattleLog)
-- ============================================================
DROP TABLE IF EXISTS `model_battle_log`;
CREATE TABLE `model_battle_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `battle_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `model_id` BIGINT NULL,
  `model_code` VARCHAR(255) NULL,
  `prompt` VARCHAR(255) NULL,
  `response` VARCHAR(255) NULL,
  `prompt_tokens` INT NULL,
  `completion_tokens` INT NULL,
  `latency_ms` INT NULL,
  `status` VARCHAR(255) NULL,
  `error_msg` VARCHAR(255) NULL,
  `score` INT NULL,
  `judge_model` VARCHAR(255) NULL,
  `judge_reason` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- model_config (ModelConfig)
-- ============================================================
DROP TABLE IF EXISTS `model_config`;
CREATE TABLE `model_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `provider_id` BIGINT NULL,
  `model_code` VARCHAR(255) NULL,
  `display_name` VARCHAR(255) NULL,
  `max_context` INT NULL,
  `max_output` INT NULL,
  `input_price` DECIMAL(20,4) NULL,
  `output_price` DECIMAL(20,4) NULL,
  `supports_vision` INT NULL,
  `supports_tools` INT NULL,
  `supports_stream` INT NULL,
  `enabled` INT NULL,
  `sort` INT NULL,
  `description` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `model_config` (id, provider_id, model_code, display_name, max_context, enabled, supports_stream, supports_tools, input_price, output_price) VALUES
(1, 1, 'gpt-4o-mini', 'GPT-4o Mini', 128000, 1, 1, 1, 0.15, 0.6),
(2, 1, 'gpt-4o', 'GPT-4o', 128000, 1, 1, 1, 2.5, 10.0),
(3, 2, 'deepseek-chat', 'DeepSeek V3', 64000, 1, 1, 1, 0.1, 0.3),
-- V6.8.2: 训练模型 (provider=4, 内部路由到 deepseek-chat + system prompt)
(10, 4, 'minimax-01',     'MiniMax-01',     32000, 1, 1, 0, 0.0, 0.0),
(11, 4, 'minimax-02',     'MiniMax-02',     32000, 1, 1, 0, 0.0, 0.0),
(12, 4, 'minimax-vision', 'MiniMax-Vision', 32000, 1, 1, 1, 0.0, 0.0),
(13, 4, 'law-gpt-v1',     'Law-GPT',        32000, 1, 1, 0, 0.0, 0.0),
(14, 4, 'med-gpt-v2',     'Med-GPT',        32000, 1, 1, 0, 0.0, 0.0),
(15, 4, 'fin-gpt-v1',     'Fin-GPT',        32000, 1, 1, 0, 0.0, 0.0),
(16, 4, 'code-gpt-v1',    'Code-GPT',       32000, 1, 1, 0, 0.0, 0.0),
(17, 4, 'chat-gpt-v3',    'Chat-GPT',       32000, 1, 1, 0, 0.0, 0.0),
(18, 4, 'qa-gpt-v2',      'QA-GPT',         32000, 1, 1, 0, 0.0, 0.0);

-- ============================================================
-- model_provider (ModelProvider)
-- ============================================================
DROP TABLE IF EXISTS `model_provider`;
CREATE TABLE `model_provider` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(255) NULL,
  `name` VARCHAR(255) NULL,
  `base_url` VARCHAR(255) NULL,
  `api_key` VARCHAR(255) NULL,
  `protocol` VARCHAR(255) NULL,
  `enabled` INT NULL,
  `sort` INT NULL,
  `description` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `model_provider` (id, code, name, protocol, base_url, enabled, sort, created_at) VALUES
(1, 'openai', 'OpenAI', 'openai', 'https://api.openai.com', 1, 1, NOW()),
(2, 'deepseek', 'DeepSeek', 'openai', 'https://api.deepseek.com', 1, 2, NOW()),
(3, 'local-ollama', 'Ollama 本地', 'local', 'http://localhost:11434', 1, 3, NOW()),
(4, 'trained', '训练模型', 'openai', 'https://api.deepseek.com', 1, 4, NOW());

-- ============================================================
-- model_quota (ModelQuota)
-- ============================================================
DROP TABLE IF EXISTS `model_quota`;
CREATE TABLE `model_quota` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `model_id` BIGINT NULL,
  `quota_date` DATE NULL,
  `used_tokens` BIGINT NULL,
  `used_requests` INT NULL,
  `limit_tokens` BIGINT NULL,
  `limit_requests` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `model_quota` (id, user_id, model_name, quota_limit, quota_used, created_at) VALUES
(1, 1, 'gpt-4o', 100000, 2340, NOW()),
(2, 2, 'deepseek-chat', 50000, 8920, NOW());

-- ============================================================
-- prompt_template (PromptTemplate)
-- ============================================================
DROP TABLE IF EXISTS `prompt_template`;
CREATE TABLE `prompt_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `category` VARCHAR(255) NULL,
  `content` VARCHAR(255) NULL,
  `variables` VARCHAR(255) NULL,
  `creator_id` BIGINT NULL,
  `creator_name` VARCHAR(255) NULL,
  `is_public` TINYINT(1) NULL,
  `use_count` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `prompt_template` (id, name, description, prompt, model_type, tags, created_at) VALUES
(1, '客服开场白', '标准客服开场白', '您好，我是 AI 助手，请问有什么可以帮助您的？', 'gpt-4o-mini', '["客服","开场"]', NOW()),
(2, '代码审查', '代码审查 prompt', '请审查以下代码，找出潜在问题：', 'gpt-4o', '["开发","审查"]', NOW());

-- ============================================================
-- training_metric (TrainingMetric)
-- ============================================================
DROP TABLE IF EXISTS `training_metric`;
CREATE TABLE `training_metric` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` BIGINT NULL,
  `iter` INT NULL,
  `loss` DOUBLE NULL,
  `accuracy` DOUBLE NULL,
  `progress` INT NULL,
  `lr` VARCHAR(255) NULL,
  `gpu_util` INT NULL,
  `vram_gb` DOUBLE NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `training_metric` (id, task_id, step, loss, accuracy, learning_rate, elapsed_ms) VALUES
(1, 1, 100, 2.341, 0.623, 0.0003, 15200),
(2, 1, 200, 1.892, 0.701, 0.0003, 30400),
(3, 1, 300, 1.521, 0.758, 0.0003, 45600),
(4, 1, 400, 1.234, 0.801, 0.0003, 60800),
(5, 1, 500, 0.987, 0.845, 0.0003, 76000);

-- ============================================================
-- training_task (TrainingTask)
-- ============================================================
DROP TABLE IF EXISTS `training_task`;
CREATE TABLE `training_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `model_name` VARCHAR(255) NULL,
  `corpus_path` VARCHAR(255) NULL,
  `n_layer` INT NULL,
  `n_head` INT NULL,
  `n_embd` INT NULL,
  `block_size` INT NULL,
  `max_iters` INT NULL,
  `batch_size` INT NULL,
  `learning_rate` DOUBLE NULL,
  `status` VARCHAR(255) NULL,
  `progress` INT NULL,
  `current_loss` DOUBLE NULL,
  `current_iter` INT NULL,
  `error_message` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `completed_at` TIMESTAMP NULL,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `training_task` (id, user_id, model_name, corpus_path, status, progress, created_at) VALUES
(1, 1, 'chatglm-6b', 'chat通用', 'COMPLETED', 100, NOW()),
(2, 1, 'llama-2-7b', 'code', 'TRAINING', 67, NOW()),
(3, 2, 'qwen-7b', '客服', 'PENDING', 0, NOW());

-- ============================================================
-- alert_channel (AlertChannel)
-- ============================================================
DROP TABLE IF EXISTS `alert_channel`;
CREATE TABLE `alert_channel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NULL,
  `channel_type` VARCHAR(255) NULL,
  `type` VARCHAR(255) NULL,
  `target` VARCHAR(255) NULL,
  `config` VARCHAR(255) NULL,
  `enabled` INT NULL,
  `priority` INT NULL,
  `description` VARCHAR(255) NULL,
  `template` VARCHAR(255) NULL,
  `created_by` BIGINT DEFAULT CURRENT_TIMESTAMP,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- alert_event (AlertEvent)
-- ============================================================
DROP TABLE IF EXISTS `alert_event`;
CREATE TABLE `alert_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_id` BIGINT NULL,
  `rule_name` VARCHAR(255) NULL,
  `severity` VARCHAR(255) NULL,
  `metric_name` VARCHAR(255) NULL,
  `metric_value` DECIMAL(20,4) NULL,
  `threshold` DECIMAL(20,4) NULL,
  `message` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `fired_at` TIMESTAMP NULL,
  `resolved_at` TIMESTAMP NULL,
  `acked_at` TIMESTAMP NULL,
  `acked_by` BIGINT NULL,
  `notes` VARCHAR(255) NULL,
  `duration` BIGINT NULL,
  `silenced_until` TIMESTAMP NULL,
  `escalated` TINYINT(1) NULL DEFAULT 0 COMMENT 'Day45: 是否已升级',
  `escalated_at` TIMESTAMP NULL COMMENT 'Day45: 升级时间',
  `resolved_by` VARCHAR(100) NULL COMMENT 'Day46: SYSTEM=自动恢复 其他=用户ID',
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- alert_rule (AlertRule)
-- ============================================================
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE `alert_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `metric_name` VARCHAR(255) NULL,
  `service` VARCHAR(255) NULL,
  `operator` VARCHAR(255) NULL,
  `threshold` DECIMAL(20,4) NULL,
  `severity` VARCHAR(255) NULL,
  `cooldown_minutes` INT NULL,
  `enabled` INT NULL,
  `tags` VARCHAR(255) NULL,
  `notify_channel` VARCHAR(255) NULL,
  `silenced_until` TIMESTAMP NULL,
  `escalate_after_minutes` INT NULL COMMENT 'Day45: CRITICAL 告警升级等待分钟数',
  `escalation_channel` VARCHAR(255) NULL COMMENT 'Day45: 升级通知渠道，逗号分隔',
  `auto_resolve_minutes` INT NULL COMMENT 'Day45: 自动恢复分钟数',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `alert_rule` (id, name, metric, threshold, operator, severity, enabled, created_at) VALUES
(1, 'CPU 过高告警', 'cpu_usage', 80, '>', 'WARNING', 1, NOW()),
(2, '内存过高告警', 'memory_usage', 85, '>', 'WARNING', 1, NOW()),
(3, '错误率过高', 'error_rate', 5, '>', 'CRITICAL', 1, NOW());

-- ============================================================
-- metric_snapshot (MetricSnapshot)
-- ============================================================
DROP TABLE IF EXISTS `metric_snapshot`;
CREATE TABLE `metric_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `service` VARCHAR(255) NULL,
  `metric_name` VARCHAR(255) NULL,
  `metric_value` DECIMAL(20,4) NULL,
  `tags` VARCHAR(255) NULL,
  `recorded_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- function_call_log (FunctionCallLog)
-- ============================================================
DROP TABLE IF EXISTS `function_call_log`;
CREATE TABLE `function_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `session_id` BIGINT NULL,
  `tool_name` VARCHAR(255) NULL,
  `arguments` VARCHAR(255) NULL,
  `result` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `error_msg` VARCHAR(255) NULL,
  `duration_ms` INT NULL,
  `ip` VARCHAR(255) NULL,
  `user_agent` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- function_tool (FunctionTool)
-- ============================================================
DROP TABLE IF EXISTS `function_tool`;
CREATE TABLE `function_tool` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NULL,
  `display_name` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `category` VARCHAR(255) NULL,
  `scope` VARCHAR(255) NULL,
  `owner_id` BIGINT NULL,
  `parameters` VARCHAR(255) NULL,
  `endpoint` VARCHAR(255) NULL,
  `http_method` VARCHAR(255) NULL,
  `enabled` INT NULL,
  `tags` VARCHAR(255) NULL,
  `risk_level` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `function_tool` (id, name, description, enabled, category, risk_level, created_at) VALUES
(1, 'search_web', '搜索互联网', 1, 'search', 'LOW', NOW()),
(2, 'calculator', '数学计算', 1, 'utility', 'LOW', NOW()),
(3, 'file_reader', '读取本地文件', 1, 'file', 'MEDIUM', NOW()),
(4, 'code_executor', '执行代码', 1, 'code', 'HIGH', NOW()),
(5, 'sql_query', '数据库查询', 1, 'database', 'CRITICAL', NOW());

-- ============================================================
-- pipeline_node_log (PipelineNodeLog)
-- ============================================================
DROP TABLE IF EXISTS `pipeline_node_log`;
CREATE TABLE `pipeline_node_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `run_id` BIGINT NULL,
  `node_id` VARCHAR(255) NULL,
  `node_type` VARCHAR(255) NULL,
  `node_name` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `start_time` TIMESTAMP NULL,
  `end_time` TIMESTAMP NULL,
  `duration_ms` BIGINT NULL,
  `input_rows` INT NULL,
  `output_rows` INT NULL,
  `output_preview` VARCHAR(255) NULL,
  `error_message` VARCHAR(255) NULL,
  `config_snapshot` VARCHAR(255) NULL,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- pipeline_run (PipelineRun)
-- ============================================================
DROP TABLE IF EXISTS `pipeline_run`;
CREATE TABLE `pipeline_run` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `workflow_id` BIGINT NULL,
  `workflow_name` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `trigger_by` BIGINT NULL,
  `trigger_type` VARCHAR(255) NULL,
  `definition_snapshot` VARCHAR(255) NULL,
  `start_time` TIMESTAMP NULL,
  `end_time` TIMESTAMP NULL,
  `duration_ms` BIGINT NULL,
  `error_message` VARCHAR(255) NULL,
  `result_summary` VARCHAR(255) NULL,
  `create_time` TIMESTAMP NULL,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- pipeline_workflow (PipelineWorkflow)
-- ============================================================
DROP TABLE IF EXISTS `pipeline_workflow`;
CREATE TABLE `pipeline_workflow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `definition` VARCHAR(255) NULL,
  `version` INT NULL,
  `status` INT NULL,
  `create_by` BIGINT NULL,
  `update_by` BIGINT NULL,
  `create_time` TIMESTAMP NULL,
  `update_time` TIMESTAMP NULL,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `pipeline_workflow` (id, name, description, version, status, owner_id, created_at) VALUES
(1, 'RAG Pipeline', '检索增强生成流程', 1, 'ACTIVE', 1, NOW()),
(2, '客服分流', '多客服智能分流', 1, 'DRAFT', 1, NOW());

-- ============================================================
-- pipeline_workflow_version (PipelineWorkflowVersion)
-- ============================================================
DROP TABLE IF EXISTS `pipeline_workflow_version`;
CREATE TABLE `pipeline_workflow_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `workflow_id` BIGINT NULL,
  `version` INT NULL,
  `definition` VARCHAR(255) NULL,
  `change_log` VARCHAR(255) NULL,
  `create_by` BIGINT NULL,
  `create_time` TIMESTAMP NULL,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- skill_approval (SkillApproval)
-- ============================================================
DROP TABLE IF EXISTS `skill_approval`;
CREATE TABLE `skill_approval` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `username` VARCHAR(255) NULL,
  `tool_name` VARCHAR(255) NULL,
  `risk_level` VARCHAR(255) NULL,
  `goal` VARCHAR(255) NULL,
  `tool_params` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `approver_id` BIGINT NULL,
  `approver_name` VARCHAR(255) NULL,
  `reason` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `skill_approval` (id, task_id, skill_name, risk_level, status, requested_by, created_at) VALUES
(1, 1, 'sql_query', 'CRITICAL', 'APPROVED', 1, NOW()),
(2, 2, 'file_reader', 'MEDIUM', 'PENDING', 2, NOW());

-- ============================================================
-- document (Document)
-- ============================================================
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `kb_id` BIGINT NULL,
  `owner_id` BIGINT NULL,
  `title` VARCHAR(255) NULL,
  `source_type` VARCHAR(255) NULL,
  `source_uri` VARCHAR(255) NULL,
  `content` VARCHAR(255) NULL,
  `size_bytes` BIGINT NULL,
  `status` VARCHAR(255) NULL,
  `error_msg` VARCHAR(255) NULL,
  `chunk_count` INT NULL,
  `checksum` VARCHAR(255) NULL,
  `tags` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- document_chunk (DocumentChunk)
-- ============================================================
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `doc_id` BIGINT NULL,
  `kb_id` BIGINT NULL,
  `owner_id` BIGINT NULL,
  `chunk_index` INT NULL,
  `content` VARCHAR(255) NULL,
  `embedding` VARCHAR(255) NULL,
  `dim` INT NULL,
  `char_count` INT NULL,
  `start_pos` INT NULL,
  `end_pos` INT NULL,
  `access_count` INT NULL,
  `last_access_at` TIMESTAMP NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- knowledge_base (KnowledgeBase)
-- ============================================================
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `owner_id` BIGINT NULL,
  `tenant_id` BIGINT NULL,
  `name` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `visibility` VARCHAR(255) NULL,
  `doc_count` INT NULL,
  `chunk_count` INT NULL,
  `tags` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- 种子数据
INSERT INTO `knowledge_base` (id, owner_id, name, description, visibility, created_at) VALUES
(1, 1, '产品文档', '内部产品文档知识库', 'PRIVATE', NOW()),
(2, 1, '技术文档', '技术文档知识库', 'PRIVATE', NOW()),
(3, 2, '公开知识库', '公共知识库', 'PUBLIC', NOW());

-- ============================================================
-- analytics_datasource (DataSource)
-- ============================================================
DROP TABLE IF EXISTS `analytics_datasource`;
CREATE TABLE `analytics_datasource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `name` VARCHAR(255) NULL,
  `type` VARCHAR(255) NULL,
  `jdbc_url` VARCHAR(255) NULL,
  `username` VARCHAR(255) NULL,
  `password_enc` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `deleted` INT DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- analytics_ingest_task (IngestTask)
-- ============================================================
DROP TABLE IF EXISTS `analytics_ingest_task`;
CREATE TABLE `analytics_ingest_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `task_id` VARCHAR(255) NULL,
  `filename` VARCHAR(255) NULL,
  `file_type` VARCHAR(255) NULL,
  `encoding` VARCHAR(255) NULL,
  `separator` VARCHAR(255) NULL,
  `file_size` BIGINT NULL,
  `status` VARCHAR(255) NULL,
  `error_message` VARCHAR(255) NULL,
  `quality_json` VARCHAR(255) NULL,
  `total_rows` BIGINT NULL,
  `total_columns` BIGINT NULL,
  `columns_json` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `finished_at` TIMESTAMP NULL,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- analytics_nlsql_history (Nl2SqlHistory)
-- ============================================================
DROP TABLE IF EXISTS `analytics_nlsql_history`;
CREATE TABLE `analytics_nlsql_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `data_source_id` BIGINT NULL,
  `question` VARCHAR(255) NULL,
  `generated_sql` VARCHAR(255) NULL,
  `corrected_sql` VARCHAR(255) NULL,
  `model` VARCHAR(255) NULL,
  `prompt_tokens` INT NULL,
  `completion_tokens` INT NULL,
  `duration_ms` BIGINT NULL,
  `success` TINYINT(1) NULL,
  `error_message` VARCHAR(255) NULL,
  `feedback_rating` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- analytics_report (Report)
-- ============================================================
DROP TABLE IF EXISTS `analytics_report`;
CREATE TABLE `analytics_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `report_id` VARCHAR(255) NULL,
  `title` VARCHAR(255) NULL,
  `question` VARCHAR(255) NULL,
  `sql_text` VARCHAR(255) NULL,
  `markdown` VARCHAR(255) NULL,
  `chart_options_json` VARCHAR(255) NULL,
  `row_count` BIGINT NULL,
  `duration_ms` BIGINT NULL,
  `format` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- agent_task (AgentTask)
-- ============================================================
DROP TABLE IF EXISTS `agent_task`;
CREATE TABLE `agent_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `goal` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `rounds` INT NULL,
  `result` VARCHAR(255) NULL,
  `llm_calls` INT NULL,
  `tool_calls` INT NULL,
  `total_tokens` INT NULL,
  `error_msg` VARCHAR(255) NULL,
  `latency_ms` BIGINT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- collab_member (CollabMember)
-- ============================================================
DROP TABLE IF EXISTS `collab_member`;
CREATE TABLE `collab_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `collab_id` BIGINT NULL,
  `user_id` BIGINT NULL,
  `role` VARCHAR(255) NULL,
  `joined_at` TIMESTAMP NULL,
  `last_active_at` TIMESTAMP NULL,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- collab_session (CollabSession)
-- ============================================================
DROP TABLE IF EXISTS `collab_session`;
CREATE TABLE `collab_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` VARCHAR(255) NULL,
  `owner_id` BIGINT NULL,
  `title` VARCHAR(255) NULL,
  `max_users` INT NULL,
  `status` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- kg_entity (KgEntity)
-- ============================================================
DROP TABLE IF EXISTS `kg_entity`;
CREATE TABLE `kg_entity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `name` VARCHAR(255) NULL,
  `entity_type` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `aliases` VARCHAR(255) NULL,
  `importance` INT NULL,
  `source` VARCHAR(255) NULL,
  `ref_count` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- kg_relation (KgRelation)
-- ============================================================
DROP TABLE IF EXISTS `kg_relation`;
CREATE TABLE `kg_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `from_entity` BIGINT NULL,
  `to_entity` BIGINT NULL,
  `relation_type` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `weight` DECIMAL(20,4) NULL,
  `source` VARCHAR(255) NULL,
  `ref_count` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- plugin (Plugin)
-- ============================================================
DROP TABLE IF EXISTS `plugin`;
CREATE TABLE `plugin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NULL,
  `display_name` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `version` VARCHAR(255) NULL,
  `author` VARCHAR(255) NULL,
  `category` VARCHAR(255) NULL,
  `scope` VARCHAR(255) NULL,
  `owner_id` BIGINT NULL,
  `icon` VARCHAR(255) NULL,
  `entry` VARCHAR(255) NULL,
  `plugin_type` VARCHAR(255) NULL,
  `config` VARCHAR(255) NULL,
  `enabled` INT NULL,
  `downloads` INT NULL,
  `rating` DECIMAL(20,4) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- chat_message (ChatMessage)
-- ============================================================
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT NULL,
  `user_id` BIGINT NULL,
  `role` VARCHAR(255) NULL,
  `content` VARCHAR(255) NULL,
  `tokens` INT NULL,
  `finish_reason` VARCHAR(255) NULL,
  `error_message` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- chat_session (ChatSession)
-- ============================================================
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `title` VARCHAR(255) NULL,
  `model` VARCHAR(255) NULL,
  `system_prompt` VARCHAR(255) NULL,
  `temperature` DECIMAL(20,4) NULL,
  `status` INT NULL,
  `message_count` INT NULL,
  `last_message_at` TIMESTAMP NULL,
  `tenant_id` BIGINT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- collab_message (CollabMessage)
-- ============================================================
DROP TABLE IF EXISTS `collab_message`;
CREATE TABLE `collab_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `room_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `username` VARCHAR(255) NULL,
  `nickname` VARCHAR(255) NULL,
  `type` VARCHAR(255) NULL,
  `content` VARCHAR(255) NULL,
  `metadata` VARCHAR(255) NULL,
  `client_msg_id` VARCHAR(255) NULL,
  `broadcast` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- collab_participant (CollabParticipant)
-- ============================================================
DROP TABLE IF EXISTS `collab_participant`;
CREATE TABLE `collab_participant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `room_id` VARCHAR(255) NULL,
  `user_id` BIGINT NULL,
  `username` VARCHAR(255) NULL,
  `nickname` VARCHAR(255) NULL,
  `avatar` VARCHAR(255) NULL,
  `role` VARCHAR(255) NULL,
  `cursor_x` INT NULL,
  `cursor_y` INT NULL,
  `selection_id` VARCHAR(255) NULL,
  `status` VARCHAR(255) NULL,
  `joined_at` TIMESTAMP NULL,
  `left_at` TIMESTAMP NULL,
  `last_heartbeat` TIMESTAMP NULL,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- collab_room (CollabRoom)
-- ============================================================
DROP TABLE IF EXISTS `collab_room`;
CREATE TABLE `collab_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `room_id` VARCHAR(255) NULL,
  `name` VARCHAR(255) NULL,
  `type` VARCHAR(255) NULL,
  `owner_id` BIGINT NULL,
  `owner_name` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `is_public` INT NULL,
  `max_participants` INT NULL,
  `status` VARCHAR(255) NULL,
  `current_participants` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `last_activity_at` TIMESTAMP NULL,
  `closed_at` TIMESTAMP NULL,
    PRIMARY KEY (`id`),
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;

-- ============================================================
-- user_preferences (UserPreference) V6.8.9 深色模式
-- ============================================================
DROP TABLE IF EXISTS `user_preferences`;
CREATE TABLE `user_preferences` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `theme` VARCHAR(20) DEFAULT 'light',
  `language` VARCHAR(20) DEFAULT 'zh-CN',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1;

-- ============================================================
-- 外键约束开启（按依赖顺序）
SET FOREIGN_KEY_CHECKS = 1;
-- ============================================================