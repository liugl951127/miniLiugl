-- =============================================================
-- MiniMax Platform V3.5.5+ 完整 SQL DDL (扫描所有 Entity 自动生成)
-- 共 77 张表 / 13 个模块
-- 生成时间: Mon Jul 13 11:32:36 UTC 2026
-- 生成工具: scripts/gen_complete_sql.py
--
-- 用法: 
--   1. 全新部署: docker compose -f docker-compose.mini.yml up -d
--   2. 增量修复: mysql -uroot -proot123456 minimax_platform < complete.sql
-- =============================================================


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- =========================================
-- 模块: auth (14 张表)
-- =========================================

-- auth/AuthLoginLog.java
DROP TABLE IF EXISTS `auth_login_log`;
CREATE TABLE IF NOT EXISTS `auth_login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent(user_agent)',
    `status` INT DEFAULT 0 COMMENT '状态(status)',
    `message` VARCHAR(255) DEFAULT NULL COMMENT 'message(message)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='auth_login_log (auto-generated V3.5.5)';

-- auth/AuthRefreshToken.java
DROP TABLE IF EXISTS `auth_refresh_token`;
CREATE TABLE IF NOT EXISTS `auth_refresh_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `token` VARCHAR(255) DEFAULT NULL COMMENT 'token(token)',
    `expires_at` DATETIME DEFAULT NULL COMMENT 'expires_at(expires_at)',
    `revoked` INT DEFAULT 0 COMMENT 'revoked(revoked)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='auth_refresh_token (auto-generated V3.5.5)';

-- auth/Notification.java
DROP TABLE IF EXISTS `notification`;
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `is_read` INT DEFAULT 0 COMMENT 'is_read(is_read)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='notification (auto-generated V3.5.5)';

-- auth/OAuthAppConfig.java
DROP TABLE IF EXISTS `oauth_app_config`;
CREATE TABLE IF NOT EXISTS `oauth_app_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform(platform)',
    `app_type` VARCHAR(255) DEFAULT NULL COMMENT 'app_type(app_type)',
    `app_id` VARCHAR(255) DEFAULT NULL COMMENT 'app_id(app_id)',
    `app_secret` VARCHAR(255) DEFAULT NULL COMMENT 'app_secret(app_secret)',
    `public_key` VARCHAR(255) DEFAULT NULL COMMENT 'public_key(public_key)',
    `redirect_uri` VARCHAR(255) DEFAULT NULL COMMENT 'redirect_uri(redirect_uri)',
    `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes(scopes)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `extra_config` VARCHAR(255) DEFAULT NULL COMMENT 'extra_config(extra_config)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='oauth_app_config (auto-generated V3.5.5)';

-- auth/OAuthBinding.java
DROP TABLE IF EXISTS `oauth_binding`;
CREATE TABLE IF NOT EXISTS `oauth_binding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform(platform)',
    `app_type` VARCHAR(255) DEFAULT NULL COMMENT 'app_type(app_type)',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid(openid)',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)',
    `access_token` VARCHAR(255) DEFAULT NULL COMMENT 'access_token(access_token)',
    `refresh_token` VARCHAR(255) DEFAULT NULL COMMENT 'refresh_token(refresh_token)',
    `token_expires_at` DATETIME DEFAULT NULL COMMENT 'token_expires_at(token_expires_at)',
    `raw_data` VARCHAR(255) DEFAULT NULL COMMENT 'raw_data(raw_data)',
    `bound_at` DATETIME DEFAULT NULL COMMENT 'bound_at(bound_at)',
    `last_login_at` DATETIME DEFAULT NULL COMMENT 'last_login_at(last_login_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='oauth_binding (auto-generated V3.5.5)';

-- auth/SysRole.java
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `code` VARCHAR(255) DEFAULT NULL COMMENT '编码(code)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `sort` INT DEFAULT 0 COMMENT '排序号(sort)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys_role (auto-generated V3.5.5)';

-- auth/SysUser.java
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `password` VARCHAR(255) DEFAULT NULL COMMENT 'password(password)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)',
    `email` VARCHAR(255) DEFAULT NULL COMMENT 'email(email)',
    `phone` VARCHAR(255) DEFAULT NULL COMMENT 'phone(phone)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)',
    `gender` INT DEFAULT 0 COMMENT 'gender(gender)',
    `status` INT DEFAULT 0 COMMENT '状态(status)',
    `last_login_ip` VARCHAR(255) DEFAULT NULL COMMENT 'last_login_ip(last_login_ip)',
    `last_login_at` DATETIME DEFAULT NULL COMMENT 'last_login_at(last_login_at)',
    `tenant_id` BIGINT DEFAULT 0 COMMENT 'tenant_id(tenant_id)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注(remark)',
    `wechat_openid` VARCHAR(255) DEFAULT NULL COMMENT 'wechat_openid(wechat_openid)',
    `wechat_unionid` VARCHAR(255) DEFAULT NULL COMMENT 'wechat_unionid(wechat_unionid)',
    `wechat_nickname` VARCHAR(255) DEFAULT NULL COMMENT 'wechat_nickname(wechat_nickname)',
    `wechat_avatar` VARCHAR(255) DEFAULT NULL COMMENT 'wechat_avatar(wechat_avatar)',
    `wechat_bound_at` DATETIME DEFAULT NULL COMMENT 'wechat_bound_at(wechat_bound_at)',
    `qq_openid` VARCHAR(255) DEFAULT NULL COMMENT 'qq_openid(qq_openid)',
    `qq_unionid` VARCHAR(255) DEFAULT NULL COMMENT 'qq_unionid(qq_unionid)',
    `qq_nickname` VARCHAR(255) DEFAULT NULL COMMENT 'qq_nickname(qq_nickname)',
    `qq_avatar` VARCHAR(255) DEFAULT NULL COMMENT 'qq_avatar(qq_avatar)',
    `qq_bound_at` DATETIME DEFAULT NULL COMMENT 'qq_bound_at(qq_bound_at)',
    `alipay_openid` VARCHAR(255) DEFAULT NULL COMMENT 'alipay_openid(alipay_openid)',
    `alipay_user_id` VARCHAR(255) DEFAULT NULL COMMENT 'alipay_user_id(alipay_user_id)',
    `alipay_nickname` VARCHAR(255) DEFAULT NULL COMMENT 'alipay_nickname(alipay_nickname)',
    `alipay_avatar` VARCHAR(255) DEFAULT NULL COMMENT 'alipay_avatar(alipay_avatar)',
    `alipay_bound_at` DATETIME DEFAULT NULL COMMENT 'alipay_bound_at(alipay_bound_at)',
    `created_by` BIGINT DEFAULT 0 COMMENT 'created_by(created_by)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_by` BIGINT DEFAULT 0 COMMENT 'updated_by(updated_by)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys_user (auto-generated V3.5.5)';

-- auth/SysUserRole.java
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `role_id` BIGINT DEFAULT 0 COMMENT 'role_id(role_id)',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys_user_role (auto-generated V3.5.5)';

-- auth/Tenant.java
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE IF NOT EXISTS `tenant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `code` VARCHAR(255) DEFAULT NULL COMMENT '编码(code)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `plan` VARCHAR(255) DEFAULT NULL COMMENT 'plan(plan)',
    `status` INT DEFAULT 0 COMMENT '状态(status)',
    `max_users` INT DEFAULT 0 COMMENT 'max_users(max_users)',
    `max_models` INT DEFAULT 0 COMMENT 'max_models(max_models)',
    `qps_limit` INT DEFAULT 0 COMMENT 'qps_limit(qps_limit)',
    `monthly_quota` BIGINT DEFAULT 0 COMMENT 'monthly_quota(monthly_quota)',
    `used_quota` BIGINT DEFAULT 0 COMMENT 'used_quota(used_quota)',
    `expire_at` DATETIME DEFAULT NULL COMMENT 'expire_at(expire_at)',
    `contact_email` VARCHAR(255) DEFAULT NULL COMMENT 'contact_email(contact_email)',
    `contact_phone` VARCHAR(255) DEFAULT NULL COMMENT 'contact_phone(contact_phone)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注(remark)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='tenant (auto-generated V3.5.5)';

-- auth/UnionidRelations.java
DROP TABLE IF EXISTS `unionid_relations`;
CREATE TABLE IF NOT EXISTS `unionid_relations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform(platform)',
    `first_seen_at` DATETIME DEFAULT NULL COMMENT 'first_seen_at(first_seen_at)',
    `last_seen_at` DATETIME DEFAULT NULL COMMENT 'last_seen_at(last_seen_at)',
    `binding_count` INT DEFAULT 0 COMMENT 'binding_count(binding_count)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='unionid_relations (auto-generated V3.5.5)';

-- auth/UserApiKey.java
DROP TABLE IF EXISTS `user_api_key`;
CREATE TABLE IF NOT EXISTS `user_api_key` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `key_hash` VARCHAR(255) DEFAULT NULL COMMENT 'key_hash(key_hash)',
    `key_prefix` VARCHAR(255) DEFAULT NULL COMMENT 'key_prefix(key_prefix)',
    `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes(scopes)',
    `expires_at` DATETIME DEFAULT NULL COMMENT 'expires_at(expires_at)',
    `last_used_at` DATETIME DEFAULT NULL COMMENT 'last_used_at(last_used_at)',
    `use_count` BIGINT DEFAULT 0 COMMENT 'use_count(use_count)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user_api_key (auto-generated V3.5.5)';

-- auth/WechatConfig.java
DROP TABLE IF EXISTS `wechat_config`;
CREATE TABLE IF NOT EXISTS `wechat_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `app_type` VARCHAR(255) DEFAULT NULL COMMENT 'app_type(app_type)',
    `app_id` VARCHAR(255) DEFAULT NULL COMMENT 'app_id(app_id)',
    `app_secret` VARCHAR(255) DEFAULT NULL COMMENT 'app_secret(app_secret)',
    `token` VARCHAR(255) DEFAULT NULL COMMENT 'token(token)',
    `aes_key` VARCHAR(255) DEFAULT NULL COMMENT 'aes_key(aes_key)',
    `redirect_uri` VARCHAR(255) DEFAULT NULL COMMENT 'redirect_uri(redirect_uri)',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注(remark)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='wechat_config (auto-generated V3.5.5)';

-- auth/WechatScanSession.java
DROP TABLE IF EXISTS `wechat_scan_session`;
CREATE TABLE IF NOT EXISTS `wechat_scan_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `ticket` VARCHAR(255) DEFAULT NULL COMMENT 'ticket(ticket)',
    `scene_id` VARCHAR(255) DEFAULT NULL COMMENT 'scene_id(scene_id)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid(openid)',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `access_token` VARCHAR(255) DEFAULT NULL COMMENT 'access_token(access_token)',
    `refresh_token` VARCHAR(255) DEFAULT NULL COMMENT 'refresh_token(refresh_token)',
    `client_ip` VARCHAR(255) DEFAULT NULL COMMENT 'client_ip(client_ip)',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent(user_agent)',
    `expires_at` DATETIME DEFAULT NULL COMMENT 'expires_at(expires_at)',
    `confirmed_at` DATETIME DEFAULT NULL COMMENT 'confirmed_at(confirmed_at)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='wechat_scan_session (auto-generated V3.5.5)';

-- auth/WechatUserBinding.java
DROP TABLE IF EXISTS `wechat_user_binding`;
CREATE TABLE IF NOT EXISTS `wechat_user_binding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid(openid)',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)',
    `app_type` VARCHAR(255) DEFAULT NULL COMMENT 'app_type(app_type)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)',
    `bound_at` DATETIME DEFAULT NULL COMMENT 'bound_at(bound_at)',
    `last_login_at` DATETIME DEFAULT NULL COMMENT 'last_login_at(last_login_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='wechat_user_binding (auto-generated V3.5.5)';


-- =========================================
-- 模块: ai (27 张表)
-- =========================================

-- ai/AgentGroup.java
DROP TABLE IF EXISTS `agent_group`;
CREATE TABLE IF NOT EXISTS `agent_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `group_id` VARCHAR(255) DEFAULT NULL COMMENT 'group_id(group_id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `strategy` VARCHAR(255) DEFAULT NULL COMMENT 'strategy(strategy)',
    `members_json` VARCHAR(255) DEFAULT NULL COMMENT 'members_json(members_json)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id(owner_id)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `last_run_at` DATETIME DEFAULT NULL COMMENT 'last_run_at(last_run_at)',
    `run_count` INT DEFAULT 0 COMMENT 'run_count(run_count)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='agent_group (auto-generated V3.5.5)';

-- ai/AiChatMessage.java
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `session_id` VARCHAR(255) DEFAULT NULL COMMENT 'session_id(session_id)',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `tool_code` VARCHAR(255) DEFAULT NULL COMMENT 'tool_code(tool_code)',
    `tool_input` VARCHAR(255) DEFAULT NULL COMMENT 'tool_input(tool_input)',
    `tool_output` VARCHAR(255) DEFAULT NULL COMMENT 'tool_output(tool_output)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_chat_message (auto-generated V3.5.5)';

-- ai/AiChatSession.java
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `session_id` VARCHAR(255) DEFAULT NULL COMMENT 'session_id(session_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_chat_session (auto-generated V3.5.5)';

-- ai/AiGenerationLog.java
DROP TABLE IF EXISTS `ai_generation_log`;
CREATE TABLE IF NOT EXISTS `ai_generation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `generation_id` VARCHAR(255) DEFAULT NULL COMMENT 'generation_id(generation_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `user_ip` VARCHAR(255) DEFAULT NULL COMMENT 'user_ip(user_ip)',
    `modality` VARCHAR(255) DEFAULT NULL COMMENT 'modality(modality)',
    `model_name` VARCHAR(255) DEFAULT NULL COMMENT 'model_name(model_name)',
    `model_version` VARCHAR(255) DEFAULT NULL COMMENT 'model_version(model_version)',
    `prompt` VARCHAR(255) DEFAULT NULL COMMENT 'prompt(prompt)',
    `negative_prompt` VARCHAR(255) DEFAULT NULL COMMENT 'negative_prompt(negative_prompt)',
    `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters(parameters)',
    `output_url` VARCHAR(255) DEFAULT NULL COMMENT 'output_url(output_url)',
    `output_size` BIGINT DEFAULT 0 COMMENT 'output_size(output_size)',
    `output_hash` VARCHAR(255) DEFAULT NULL COMMENT 'output_hash(output_hash)',
    `watermarked` INT DEFAULT 0 COMMENT 'watermarked(watermarked)',
    `watermark_text` VARCHAR(255) DEFAULT NULL COMMENT 'watermark_text(watermark_text)',
    `duration_ms` INT DEFAULT 0 COMMENT 'duration_ms(duration_ms)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg(error_msg)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_generation_log (auto-generated V3.5.5)';

-- ai/AiIntentKeyword.java
DROP TABLE IF EXISTS `ai_intent_keyword`;
CREATE TABLE IF NOT EXISTS `ai_intent_keyword` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `intent` VARCHAR(255) DEFAULT NULL COMMENT 'intent(intent)',
    `keyword` VARCHAR(255) DEFAULT NULL COMMENT 'keyword(keyword)',
    `weight` INT DEFAULT 0 COMMENT 'weight(weight)',
    `is_regex` INT DEFAULT 0 COMMENT 'is_regex(is_regex)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `language` VARCHAR(255) DEFAULT NULL COMMENT 'language(language)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注(remark)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_intent_keyword (auto-generated V3.5.5)';

-- ai/AiTool.java
DROP TABLE IF EXISTS `ai_tool`;
CREATE TABLE IF NOT EXISTS `ai_tool` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `code` VARCHAR(255) DEFAULT NULL COMMENT '编码(code)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon(icon)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `builtin` INT DEFAULT 0 COMMENT 'builtin(builtin)',
    `input_schema` VARCHAR(255) DEFAULT NULL COMMENT 'input_schema(input_schema)',
    `output_schema` VARCHAR(255) DEFAULT NULL COMMENT 'output_schema(output_schema)',
    `default_config` VARCHAR(255) DEFAULT NULL COMMENT 'default_config(default_config)',
    `impl_type` VARCHAR(255) DEFAULT NULL COMMENT 'impl_type(impl_type)',
    `impl_value` VARCHAR(255) DEFAULT NULL COMMENT 'impl_value(impl_value)',
    `rate_limit` INT DEFAULT 0 COMMENT 'rate_limit(rate_limit)',
    `timeout_seconds` INT DEFAULT 0 COMMENT 'timeout_seconds(timeout_seconds)',
    `role_required` VARCHAR(255) DEFAULT NULL COMMENT 'role_required(role_required)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `version` VARCHAR(255) DEFAULT NULL COMMENT '乐观锁版本号(version)',
    `author` VARCHAR(255) DEFAULT NULL COMMENT 'author(author)',
    `created_by` BIGINT DEFAULT 0 COMMENT 'created_by(created_by)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_tool (auto-generated V3.5.5)';

-- ai/AiToolInvocation.java
DROP TABLE IF EXISTS `ai_tool_invocation`;
CREATE TABLE IF NOT EXISTS `ai_tool_invocation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `tool_code` VARCHAR(255) DEFAULT NULL COMMENT 'tool_code(tool_code)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `input_json` VARCHAR(255) DEFAULT NULL COMMENT 'input_json(input_json)',
    `output_json` VARCHAR(255) DEFAULT NULL COMMENT 'output_json(output_json)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message(error_message)',
    `duration_ms` INT DEFAULT 0 COMMENT 'duration_ms(duration_ms)',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent(user_agent)',
    `data_source_id` BIGINT DEFAULT 0 COMMENT 'data_source_id(data_source_id)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_tool_invocation (auto-generated V3.5.5)';

-- ai/AuditLog.java
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `trace_id` VARCHAR(255) DEFAULT NULL COMMENT 'trace_id(trace_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `user_ip` VARCHAR(255) DEFAULT NULL COMMENT 'user_ip(user_ip)',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent(user_agent)',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)',
    `resource_type` VARCHAR(255) DEFAULT NULL COMMENT 'resource_type(resource_type)',
    `resource_id` VARCHAR(255) DEFAULT NULL COMMENT 'resource_id(resource_id)',
    `method` VARCHAR(255) DEFAULT NULL COMMENT 'method(method)',
    `path` VARCHAR(255) DEFAULT NULL COMMENT 'path(path)',
    `request_body` VARCHAR(255) DEFAULT NULL COMMENT 'request_body(request_body)',
    `response_status` INT DEFAULT 0 COMMENT 'response_status(response_status)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg(error_msg)',
    `duration_ms` INT DEFAULT 0 COMMENT 'duration_ms(duration_ms)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='audit_log (auto-generated V3.5.5)';

-- ai/BillingRecord.java
DROP TABLE IF EXISTS `billing_record`;
CREATE TABLE IF NOT EXISTS `billing_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `record_id` VARCHAR(255) DEFAULT NULL COMMENT 'record_id(record_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `license_id` BIGINT DEFAULT 0 COMMENT 'license_id(license_id)',
    `model_entry_id` BIGINT DEFAULT 0 COMMENT 'model_entry_id(model_entry_id)',
    `record_type` VARCHAR(255) DEFAULT NULL COMMENT 'record_type(record_type)',
    `amount_cents` BIGINT DEFAULT 0 COMMENT 'amount_cents(amount_cents)',
    `currency` VARCHAR(255) DEFAULT NULL COMMENT 'currency(currency)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `payment_method` VARCHAR(255) DEFAULT NULL COMMENT 'payment_method(payment_method)',
    `external_transaction_id` VARCHAR(255) DEFAULT NULL COMMENT 'external_transaction_id(external_transaction_id)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='billing_record (auto-generated V3.5.5)';

-- ai/ClusterNode.java
DROP TABLE IF EXISTS `cluster_node`;
CREATE TABLE IF NOT EXISTS `cluster_node` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `node_id` VARCHAR(255) DEFAULT NULL COMMENT 'node_id(node_id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `address` VARCHAR(255) DEFAULT NULL COMMENT 'address(address)',
    `region` VARCHAR(255) DEFAULT NULL COMMENT 'region(region)',
    `zone` VARCHAR(255) DEFAULT NULL COMMENT 'zone(zone)',
    `capabilities` VARCHAR(255) DEFAULT NULL COMMENT 'capabilities(capabilities)',
    `total_cores` INT DEFAULT 0 COMMENT 'total_cores(total_cores)',
    `total_memory_mb` BIGINT DEFAULT 0 COMMENT 'total_memory_mb(total_memory_mb)',
    `total_gpus` INT DEFAULT 0 COMMENT 'total_gpus(total_gpus)',
    `cpu_usage` DOUBLE DEFAULT 0 COMMENT 'cpu_usage(cpu_usage)',
    `memory_usage` DOUBLE DEFAULT 0 COMMENT 'memory_usage(memory_usage)',
    `gpu_usage` DOUBLE DEFAULT 0 COMMENT 'gpu_usage(gpu_usage)',
    `active_tasks` INT DEFAULT 0 COMMENT 'active_tasks(active_tasks)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `is_leader` TINYINT(1) DEFAULT 0 COMMENT 'is_leader(is_leader)',
    `last_heartbeat` DATETIME DEFAULT NULL COMMENT 'last_heartbeat(last_heartbeat)',
    `started_at` DATETIME DEFAULT NULL COMMENT 'started_at(started_at)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='cluster_node (auto-generated V3.5.5)';

-- ai/DashboardMetric.java
DROP TABLE IF EXISTS `dashboard_metric`;
CREATE TABLE IF NOT EXISTS `dashboard_metric` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `metric` VARCHAR(255) DEFAULT NULL COMMENT 'metric(metric)',
    `dimension` VARCHAR(255) DEFAULT NULL COMMENT 'dimension(dimension)',
    `value` DOUBLE DEFAULT 0 COMMENT 'value(value)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `timestamp` DATETIME DEFAULT NULL COMMENT 'timestamp(timestamp)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='dashboard_metric (auto-generated V3.5.5)';

-- ai/DbDataSource.java
DROP TABLE IF EXISTS `data_source`;
CREATE TABLE IF NOT EXISTS `data_source` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `jdbc_url` VARCHAR(255) DEFAULT NULL COMMENT 'jdbc_url(jdbc_url)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `password` VARCHAR(255) DEFAULT NULL COMMENT 'password(password)',
    `driver_class` VARCHAR(255) DEFAULT NULL COMMENT 'driver_class(driver_class)',
    `pool_size` INT DEFAULT 0 COMMENT 'pool_size(pool_size)',
    `min_idle` INT DEFAULT 0 COMMENT 'min_idle(min_idle)',
    `max_lifetime` INT DEFAULT 0 COMMENT 'max_lifetime(max_lifetime)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `test_status` VARCHAR(255) DEFAULT NULL COMMENT 'test_status(test_status)',
    `test_message` VARCHAR(255) DEFAULT NULL COMMENT 'test_message(test_message)',
    `last_test_at` DATETIME DEFAULT NULL COMMENT 'last_test_at(last_test_at)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `created_by` BIGINT DEFAULT 0 COMMENT 'created_by(created_by)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='data_source (auto-generated V3.5.5)';

-- ai/KbChunk.java
DROP TABLE IF EXISTS `kb_chunk`;
CREATE TABLE IF NOT EXISTS `kb_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `chunk_id` VARCHAR(255) DEFAULT NULL COMMENT 'chunk_id(chunk_id)',
    `doc_id` VARCHAR(255) DEFAULT NULL COMMENT 'doc_id(doc_id)',
    `kb_id` VARCHAR(255) DEFAULT NULL COMMENT 'kb_id(kb_id)',
    `seq` INT DEFAULT 0 COMMENT 'seq(seq)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `char_count` INT DEFAULT 0 COMMENT 'char_count(char_count)',
    `token_count` INT DEFAULT 0 COMMENT 'token_count(token_count)',
    `embedding` VARCHAR(255) DEFAULT NULL COMMENT 'embedding(embedding)',
    `embedding_model` VARCHAR(255) DEFAULT NULL COMMENT 'embedding_model(embedding_model)',
    `keywords` VARCHAR(255) DEFAULT NULL COMMENT 'keywords(keywords)',
    `summary` VARCHAR(255) DEFAULT NULL COMMENT 'summary(summary)',
    `location` VARCHAR(255) DEFAULT NULL COMMENT 'location(location)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='kb_chunk (auto-generated V3.5.5)';

-- ai/KbDocument.java
DROP TABLE IF EXISTS `kb_document`;
CREATE TABLE IF NOT EXISTS `kb_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `doc_id` VARCHAR(255) DEFAULT NULL COMMENT 'doc_id(doc_id)',
    `kb_id` VARCHAR(255) DEFAULT NULL COMMENT 'kb_id(kb_id)',
    `filename` VARCHAR(255) DEFAULT NULL COMMENT 'filename(filename)',
    `mime_type` VARCHAR(255) DEFAULT NULL COMMENT 'mime_type(mime_type)',
    `size_bytes` BIGINT DEFAULT 0 COMMENT 'size_bytes(size_bytes)',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)',
    `file_path` VARCHAR(255) DEFAULT NULL COMMENT 'file_path(file_path)',
    `source` VARCHAR(255) DEFAULT NULL COMMENT 'source(source)',
    `source_url` VARCHAR(255) DEFAULT NULL COMMENT 'source_url(source_url)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `chunk_count` INT DEFAULT 0 COMMENT 'chunk_count(chunk_count)',
    `embedding_count` INT DEFAULT 0 COMMENT 'embedding_count(embedding_count)',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id(owner_id)',
    `is_public` TINYINT(1) DEFAULT 0 COMMENT 'is_public(is_public)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='kb_document (auto-generated V3.5.5)';

-- ai/KbPermission.java
DROP TABLE IF EXISTS `kb_permission`;
CREATE TABLE IF NOT EXISTS `kb_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `kb_id` VARCHAR(255) DEFAULT NULL COMMENT 'kb_id(kb_id)',
    `subject_type` VARCHAR(255) DEFAULT NULL COMMENT 'subject_type(subject_type)',
    `subject_id` BIGINT DEFAULT 0 COMMENT 'subject_id(subject_id)',
    `permission` VARCHAR(255) DEFAULT NULL COMMENT 'permission(permission)',
    `grant_by` BIGINT DEFAULT 0 COMMENT 'grant_by(grant_by)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='kb_permission (auto-generated V3.5.5)';

-- ai/ModelLicense.java
DROP TABLE IF EXISTS `model_license`;
CREATE TABLE IF NOT EXISTS `model_license` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `license_key` VARCHAR(255) DEFAULT NULL COMMENT 'license_key(license_key)',
    `model_entry_id` BIGINT DEFAULT 0 COMMENT 'model_entry_id(model_entry_id)',
    `model_version_id` BIGINT DEFAULT 0 COMMENT 'model_version_id(model_version_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `license_type` VARCHAR(255) DEFAULT NULL COMMENT 'license_type(license_type)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `quota_calls` BIGINT DEFAULT 0 COMMENT 'quota_calls(quota_calls)',
    `used_calls` BIGINT DEFAULT 0 COMMENT 'used_calls(used_calls)',
    `start_at` DATETIME DEFAULT NULL COMMENT 'start_at(start_at)',
    `expire_at` DATETIME DEFAULT NULL COMMENT 'expire_at(expire_at)',
    `price_cents` BIGINT DEFAULT 0 COMMENT 'price_cents(price_cents)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='model_license (auto-generated V3.5.5)';

-- ai/ModelVersion.java
DROP TABLE IF EXISTS `model_version`;
CREATE TABLE IF NOT EXISTS `model_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `version_id` VARCHAR(255) DEFAULT NULL COMMENT 'version_id(version_id)',
    `model_entry_id` BIGINT DEFAULT 0 COMMENT 'model_entry_id(model_entry_id)',
    `version` VARCHAR(255) DEFAULT NULL COMMENT '乐观锁版本号(version)',
    `changelog` VARCHAR(255) DEFAULT NULL COMMENT 'changelog(changelog)',
    `file_path` VARCHAR(255) DEFAULT NULL COMMENT 'file_path(file_path)',
    `size_bytes` BIGINT DEFAULT 0 COMMENT 'size_bytes(size_bytes)',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)',
    `input_schema` VARCHAR(255) DEFAULT NULL COMMENT 'input_schema(input_schema)',
    `output_schema` VARCHAR(255) DEFAULT NULL COMMENT 'output_schema(output_schema)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `is_latest` TINYINT(1) DEFAULT 0 COMMENT 'is_latest(is_latest)',
    `uploader_id` BIGINT DEFAULT 0 COMMENT 'uploader_id(uploader_id)',
    `backward_compatible` VARCHAR(255) DEFAULT NULL COMMENT 'backward_compatible(backward_compatible)',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata(metadata)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='model_version (auto-generated V3.5.5)';

-- ai/ModerationRecord.java
DROP TABLE IF EXISTS `moderation_record`;
CREATE TABLE IF NOT EXISTS `moderation_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `trace_id` VARCHAR(255) DEFAULT NULL COMMENT 'trace_id(trace_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `content_type` VARCHAR(255) DEFAULT NULL COMMENT 'content_type(content_type)',
    `content_hash` VARCHAR(255) DEFAULT NULL COMMENT 'content_hash(content_hash)',
    `content_size` BIGINT DEFAULT 0 COMMENT 'content_size(content_size)',
    `content_url` VARCHAR(255) DEFAULT NULL COMMENT 'content_url(content_url)',
    `moderation_status` VARCHAR(255) DEFAULT NULL COMMENT 'moderation_status(moderation_status)',
    `risk_level` VARCHAR(255) DEFAULT NULL COMMENT 'risk_level(risk_level)',
    `risk_labels` VARCHAR(255) DEFAULT NULL COMMENT 'risk_labels(risk_labels)',
    `risk_score` DECIMAL(20,4) DEFAULT 0 COMMENT 'risk_score(risk_score)',
    `moderator` VARCHAR(255) DEFAULT NULL COMMENT 'moderator(moderator)',
    `rejection_reason` VARCHAR(255) DEFAULT NULL COMMENT 'rejection_reason(rejection_reason)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='moderation_record (auto-generated V3.5.5)';

-- ai/MultimediaFile.java
DROP TABLE IF EXISTS `multimedia_file`;
CREATE TABLE IF NOT EXISTS `multimedia_file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `file_id` VARCHAR(255) DEFAULT NULL COMMENT 'file_id(file_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `file_name` VARCHAR(255) DEFAULT NULL COMMENT 'file_name(file_name)',
    `original_name` VARCHAR(255) DEFAULT NULL COMMENT 'original_name(original_name)',
    `file_type` VARCHAR(255) DEFAULT NULL COMMENT 'file_type(file_type)',
    `mime_type` VARCHAR(255) DEFAULT NULL COMMENT 'mime_type(mime_type)',
    `file_size` BIGINT DEFAULT 0 COMMENT 'file_size(file_size)',
    `file_hash` VARCHAR(255) DEFAULT NULL COMMENT 'file_hash(file_hash)',
    `storage_path` VARCHAR(255) DEFAULT NULL COMMENT 'storage_path(storage_path)',
    `storage_type` VARCHAR(255) DEFAULT NULL COMMENT 'storage_type(storage_type)',
    `encrypted` INT DEFAULT 0 COMMENT 'encrypted(encrypted)',
    `duration_ms` BIGINT DEFAULT 0 COMMENT 'duration_ms(duration_ms)',
    `width` INT DEFAULT 0 COMMENT 'width(width)',
    `height` INT DEFAULT 0 COMMENT 'height(height)',
    `bitrate` INT DEFAULT 0 COMMENT 'bitrate(bitrate)',
    `sample_rate` INT DEFAULT 0 COMMENT 'sample_rate(sample_rate)',
    `channels` INT DEFAULT 0 COMMENT 'channels(channels)',
    `codec` VARCHAR(255) DEFAULT NULL COMMENT 'codec(codec)',
    `exif` VARCHAR(255) DEFAULT NULL COMMENT 'exif(exif)',
    `moderation_status` VARCHAR(255) DEFAULT NULL COMMENT 'moderation_status(moderation_status)',
    `moderation_id` BIGINT DEFAULT 0 COMMENT 'moderation_id(moderation_id)',
    `watermarked` INT DEFAULT 0 COMMENT 'watermarked(watermarked)',
    `is_public` INT DEFAULT 0 COMMENT 'is_public(is_public)',
    `access_count` INT DEFAULT 0 COMMENT 'access_count(access_count)',
    `expire_at` DATETIME DEFAULT NULL COMMENT 'expire_at(expire_at)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='multimedia_file (auto-generated V3.5.5)';

-- ai/PipelineLog.java
DROP TABLE IF EXISTS `pipeline_log`;
CREATE TABLE IF NOT EXISTS `pipeline_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `session_id` VARCHAR(255) DEFAULT NULL COMMENT 'session_id(session_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `client_ip` VARCHAR(255) DEFAULT NULL COMMENT 'client_ip(client_ip)',
    `input_text` VARCHAR(255) DEFAULT NULL COMMENT 'input_text(input_text)',
    `input_modality` VARCHAR(255) DEFAULT NULL COMMENT 'input_modality(input_modality)',
    `intent` VARCHAR(255) DEFAULT NULL COMMENT 'intent(intent)',
    `output_text` VARCHAR(255) DEFAULT NULL COMMENT 'output_text(output_text)',
    `output_tokens` INT DEFAULT 0 COMMENT 'output_tokens(output_tokens)',
    `compute_device` VARCHAR(255) DEFAULT NULL COMMENT 'compute_device(compute_device)',
    `compute_mode` VARCHAR(255) DEFAULT NULL COMMENT 'compute_mode(compute_mode)',
    `total_cost_ms` BIGINT DEFAULT 0 COMMENT 'total_cost_ms(total_cost_ms)',
    `stage_costs` VARCHAR(255) DEFAULT NULL COMMENT 'stage_costs(stage_costs)',
    `risk_level` VARCHAR(255) DEFAULT NULL COMMENT 'risk_level(risk_level)',
    `needs_review` TINYINT(1) DEFAULT 0 COMMENT 'needs_review(needs_review)',
    `rag_hits` INT DEFAULT 0 COMMENT 'rag_hits(rag_hits)',
    `tool_calls` INT DEFAULT 0 COMMENT 'tool_calls(tool_calls)',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message(error_message)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='pipeline_log (auto-generated V3.5.5)';

-- ai/PushMessage.java
DROP TABLE IF EXISTS `push_message`;
CREATE TABLE IF NOT EXISTS `push_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `message_id` VARCHAR(255) DEFAULT NULL COMMENT 'message_id(message_id)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `body` VARCHAR(255) DEFAULT NULL COMMENT 'body(body)',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon(icon)',
    `click_action` VARCHAR(255) DEFAULT NULL COMMENT 'click_action(click_action)',
    `data` VARCHAR(255) DEFAULT NULL COMMENT 'data(data)',
    `target_type` VARCHAR(255) DEFAULT NULL COMMENT 'target_type(target_type)',
    `target_value` VARCHAR(255) DEFAULT NULL COMMENT 'target_value(target_value)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `success_count` INT DEFAULT 0 COMMENT 'success_count(success_count)',
    `failure_count` INT DEFAULT 0 COMMENT 'failure_count(failure_count)',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='push_message (auto-generated V3.5.5)';

-- ai/PushSubscription.java
DROP TABLE IF EXISTS `push_subscription`;
CREATE TABLE IF NOT EXISTS `push_subscription` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `subscription_id` VARCHAR(255) DEFAULT NULL COMMENT 'subscription_id(subscription_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform(platform)',
    `endpoint` VARCHAR(255) DEFAULT NULL COMMENT 'endpoint(endpoint)',
    `p256dh_key` VARCHAR(255) DEFAULT NULL COMMENT 'p256dh_key(p256dh_key)',
    `auth_key` VARCHAR(255) DEFAULT NULL COMMENT 'auth_key(auth_key)',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent(user_agent)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `last_active_at` DATETIME DEFAULT NULL COMMENT 'last_active_at(last_active_at)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='push_subscription (auto-generated V3.5.5)';

-- ai/LogEntry.java
DROP TABLE IF EXISTS `raft_log`;
CREATE TABLE IF NOT EXISTS `raft_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `term` BIGINT DEFAULT 0 COMMENT 'term(term)',
    `log_index` BIGINT DEFAULT 0 COMMENT 'log_index(log_index)',
    `node_id` VARCHAR(255) DEFAULT NULL COMMENT 'node_id(node_id)',
    `command` VARCHAR(255) DEFAULT NULL COMMENT 'command(command)',
    `committed` TINYINT(1) DEFAULT 0 COMMENT 'committed(committed)',
    `committed_at` DATETIME DEFAULT NULL COMMENT 'committed_at(committed_at)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='raft_log (auto-generated V3.5.5)';

-- ai/SensitiveWord.java
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE IF NOT EXISTS `sensitive_word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `word` VARCHAR(255) DEFAULT NULL COMMENT 'word(word)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)',
    `level` VARCHAR(255) DEFAULT NULL COMMENT 'level(level)',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sensitive_word (auto-generated V3.5.5)';

-- ai/TrainingCheckpoint.java
DROP TABLE IF EXISTS `training_checkpoint`;
CREATE TABLE IF NOT EXISTS `training_checkpoint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `task_id` VARCHAR(255) DEFAULT NULL COMMENT 'task_id(task_id)',
    `checkpoint_id` VARCHAR(255) DEFAULT NULL COMMENT 'checkpoint_id(checkpoint_id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `epoch` INT DEFAULT 0 COMMENT 'epoch(epoch)',
    `step` INT DEFAULT 0 COMMENT 'step(step)',
    `file_path` VARCHAR(255) DEFAULT NULL COMMENT 'file_path(file_path)',
    `size_bytes` BIGINT DEFAULT 0 COMMENT 'size_bytes(size_bytes)',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)',
    `val_loss` DOUBLE DEFAULT 0 COMMENT 'val_loss(val_loss)',
    `accuracy` DOUBLE DEFAULT 0 COMMENT 'accuracy(accuracy)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata(metadata)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='training_checkpoint (auto-generated V3.5.5)';

-- ai/TrainingJob.java
DROP TABLE IF EXISTS `training_job`;
CREATE TABLE IF NOT EXISTS `training_job` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `task_id` VARCHAR(255) DEFAULT NULL COMMENT 'task_id(task_id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `total_epochs` INT DEFAULT 0 COMMENT 'total_epochs(total_epochs)',
    `current_epoch` INT DEFAULT 0 COMMENT 'current_epoch(current_epoch)',
    `current_step` INT DEFAULT 0 COMMENT 'current_step(current_step)',
    `start_time_ms` BIGINT DEFAULT 0 COMMENT 'start_time_ms(start_time_ms)',
    `end_time_ms` BIGINT DEFAULT 0 COMMENT 'end_time_ms(end_time_ms)',
    `config` VARCHAR(255) DEFAULT NULL COMMENT 'config(config)',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id(owner_id)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `last_loss` DOUBLE DEFAULT 0 COMMENT 'last_loss(last_loss)',
    `last_val_loss` DOUBLE DEFAULT 0 COMMENT 'last_val_loss(last_val_loss)',
    `last_accuracy` DOUBLE DEFAULT 0 COMMENT 'last_accuracy(last_accuracy)',
    `total_steps` INT DEFAULT 0 COMMENT 'total_steps(total_steps)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='training_job (auto-generated V3.5.5)';

-- ai/TrainingMetric.java
DROP TABLE IF EXISTS `training_metric`;
CREATE TABLE IF NOT EXISTS `training_metric` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `task_id` VARCHAR(255) DEFAULT NULL COMMENT 'task_id(task_id)',
    `epoch` INT DEFAULT 0 COMMENT 'epoch(epoch)',
    `step` INT DEFAULT 0 COMMENT 'step(step)',
    `loss` DOUBLE DEFAULT 0 COMMENT 'loss(loss)',
    `val_loss` DOUBLE DEFAULT 0 COMMENT 'val_loss(val_loss)',
    `accuracy` DOUBLE DEFAULT 0 COMMENT 'accuracy(accuracy)',
    `learning_rate` DOUBLE DEFAULT 0 COMMENT 'learning_rate(learning_rate)',
    `elapsed_ms` BIGINT DEFAULT 0 COMMENT 'elapsed_ms(elapsed_ms)',
    `timestamp` DATETIME DEFAULT NULL COMMENT 'timestamp(timestamp)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='training_metric (auto-generated V3.5.5)';


-- =========================================
-- 模块: agent (6 张表)
-- =========================================

-- agent/AgentTask.java
DROP TABLE IF EXISTS `agent_task`;
CREATE TABLE IF NOT EXISTS `agent_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `task_id` VARCHAR(255) DEFAULT NULL COMMENT 'task_id(task_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `goal` VARCHAR(255) DEFAULT NULL COMMENT 'goal(goal)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `rounds` INT DEFAULT 0 COMMENT 'rounds(rounds)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)',
    `llm_calls` INT DEFAULT 0 COMMENT 'llm_calls(llm_calls)',
    `tool_calls` INT DEFAULT 0 COMMENT 'tool_calls(tool_calls)',
    `total_tokens` INT DEFAULT 0 COMMENT 'total_tokens(total_tokens)',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg(error_msg)',
    `latency_ms` BIGINT DEFAULT 0 COMMENT 'latency_ms(latency_ms)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='agent_task (auto-generated V3.5.5)';

-- agent/CollabMember.java
DROP TABLE IF EXISTS `collab_member`;
CREATE TABLE IF NOT EXISTS `collab_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `collab_id` BIGINT DEFAULT 0 COMMENT 'collab_id(collab_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)',
    `joined_at` DATETIME DEFAULT NULL COMMENT 'joined_at(joined_at)',
    `last_active_at` DATETIME DEFAULT NULL COMMENT 'last_active_at(last_active_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='collab_member (auto-generated V3.5.5)';

-- agent/CollabSession.java
DROP TABLE IF EXISTS `collab_session`;
CREATE TABLE IF NOT EXISTS `collab_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `session_id` VARCHAR(255) DEFAULT NULL COMMENT 'session_id(session_id)',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id(owner_id)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `max_users` INT DEFAULT 0 COMMENT 'max_users(max_users)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='collab_session (auto-generated V3.5.5)';

-- agent/KgEntity.java
DROP TABLE IF EXISTS `kg_entity`;
CREATE TABLE IF NOT EXISTS `kg_entity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `entity_type` VARCHAR(255) DEFAULT NULL COMMENT 'entity_type(entity_type)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `aliases` VARCHAR(255) DEFAULT NULL COMMENT 'aliases(aliases)',
    `importance` INT DEFAULT 0 COMMENT 'importance(importance)',
    `source` VARCHAR(255) DEFAULT NULL COMMENT 'source(source)',
    `ref_count` INT DEFAULT 0 COMMENT 'ref_count(ref_count)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='kg_entity (auto-generated V3.5.5)';

-- agent/KgRelation.java
DROP TABLE IF EXISTS `kg_relation`;
CREATE TABLE IF NOT EXISTS `kg_relation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `from_entity` BIGINT DEFAULT 0 COMMENT 'from_entity(from_entity)',
    `to_entity` BIGINT DEFAULT 0 COMMENT 'to_entity(to_entity)',
    `relation_type` VARCHAR(255) DEFAULT NULL COMMENT 'relation_type(relation_type)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `weight` DECIMAL(20,4) DEFAULT 0 COMMENT 'weight(weight)',
    `source` VARCHAR(255) DEFAULT NULL COMMENT 'source(source)',
    `ref_count` INT DEFAULT 0 COMMENT 'ref_count(ref_count)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='kg_relation (auto-generated V3.5.5)';

-- agent/Plugin.java
DROP TABLE IF EXISTS `plugin`;
CREATE TABLE IF NOT EXISTS `plugin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `display_name` VARCHAR(255) DEFAULT NULL COMMENT 'display_name(display_name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `version` VARCHAR(255) DEFAULT NULL COMMENT '乐观锁版本号(version)',
    `author` VARCHAR(255) DEFAULT NULL COMMENT 'author(author)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id(owner_id)',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon(icon)',
    `entry` VARCHAR(255) DEFAULT NULL COMMENT 'entry(entry)',
    `plugin_type` VARCHAR(255) DEFAULT NULL COMMENT 'plugin_type(plugin_type)',
    `config` VARCHAR(255) DEFAULT NULL COMMENT 'config(config)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `downloads` INT DEFAULT 0 COMMENT 'downloads(downloads)',
    `rating` DECIMAL(20,4) DEFAULT 0 COMMENT 'rating(rating)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='plugin (auto-generated V3.5.5)';


-- =========================================
-- 模块: model (5 张表)
-- =========================================

-- model/ModelBattleLog.java
DROP TABLE IF EXISTS `model_battle_log`;
CREATE TABLE IF NOT EXISTS `model_battle_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `battle_id` VARCHAR(255) DEFAULT NULL COMMENT 'battle_id(battle_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `model_id` BIGINT DEFAULT 0 COMMENT 'model_id(model_id)',
    `model_code` VARCHAR(255) DEFAULT NULL COMMENT 'model_code(model_code)',
    `prompt` VARCHAR(255) DEFAULT NULL COMMENT 'prompt(prompt)',
    `response` VARCHAR(255) DEFAULT NULL COMMENT 'response(response)',
    `prompt_tokens` INT DEFAULT 0 COMMENT 'prompt_tokens(prompt_tokens)',
    `completion_tokens` INT DEFAULT 0 COMMENT 'completion_tokens(completion_tokens)',
    `latency_ms` INT DEFAULT 0 COMMENT 'latency_ms(latency_ms)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg(error_msg)',
    `score` INT DEFAULT 0 COMMENT 'score(score)',
    `judge_model` VARCHAR(255) DEFAULT NULL COMMENT 'judge_model(judge_model)',
    `judge_reason` VARCHAR(255) DEFAULT NULL COMMENT 'judge_reason(judge_reason)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='model_battle_log (auto-generated V3.5.5)';

-- model/ModelConfig.java
DROP TABLE IF EXISTS `model_config`;
CREATE TABLE IF NOT EXISTS `model_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `provider_id` BIGINT DEFAULT 0 COMMENT 'provider_id(provider_id)',
    `model_code` VARCHAR(255) DEFAULT NULL COMMENT 'model_code(model_code)',
    `display_name` VARCHAR(255) DEFAULT NULL COMMENT 'display_name(display_name)',
    `max_context` INT DEFAULT 0 COMMENT 'max_context(max_context)',
    `max_output` INT DEFAULT 0 COMMENT 'max_output(max_output)',
    `input_price` DECIMAL(20,4) DEFAULT 0 COMMENT 'input_price(input_price)',
    `output_price` DECIMAL(20,4) DEFAULT 0 COMMENT 'output_price(output_price)',
    `supports_vision` INT DEFAULT 0 COMMENT 'supports_vision(supports_vision)',
    `supports_tools` INT DEFAULT 0 COMMENT 'supports_tools(supports_tools)',
    `supports_stream` INT DEFAULT 0 COMMENT 'supports_stream(supports_stream)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `sort` INT DEFAULT 0 COMMENT '排序号(sort)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='model_config (auto-generated V3.5.5)';

-- model/ModelProvider.java
DROP TABLE IF EXISTS `model_provider`;
CREATE TABLE IF NOT EXISTS `model_provider` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `code` VARCHAR(255) DEFAULT NULL COMMENT '编码(code)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `base_url` VARCHAR(255) DEFAULT NULL COMMENT 'base_url(base_url)',
    `api_key` VARCHAR(255) DEFAULT NULL COMMENT 'api_key(api_key)',
    `protocol` VARCHAR(255) DEFAULT NULL COMMENT 'protocol(protocol)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `sort` INT DEFAULT 0 COMMENT '排序号(sort)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='model_provider (auto-generated V3.5.5)';

-- model/ModelQuota.java
DROP TABLE IF EXISTS `model_quota`;
CREATE TABLE IF NOT EXISTS `model_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `model_id` BIGINT DEFAULT 0 COMMENT 'model_id(model_id)',
    `quota_date` DATE DEFAULT NULL COMMENT 'quota_date(quota_date)',
    `used_tokens` BIGINT DEFAULT 0 COMMENT 'used_tokens(used_tokens)',
    `used_requests` INT DEFAULT 0 COMMENT 'used_requests(used_requests)',
    `limit_tokens` BIGINT DEFAULT 0 COMMENT 'limit_tokens(limit_tokens)',
    `limit_requests` INT DEFAULT 0 COMMENT 'limit_requests(limit_requests)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='model_quota (auto-generated V3.5.5)';

-- model/TrainingTask.java
DROP TABLE IF EXISTS `training_task`;
CREATE TABLE IF NOT EXISTS `training_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `model_name` VARCHAR(255) DEFAULT NULL COMMENT 'model_name(model_name)',
    `corpus_path` VARCHAR(255) DEFAULT NULL COMMENT 'corpus_path(corpus_path)',
    `n_layer` INT DEFAULT 0 COMMENT 'n_layer(n_layer)',
    `n_head` INT DEFAULT 0 COMMENT 'n_head(n_head)',
    `n_embd` INT DEFAULT 0 COMMENT 'n_embd(n_embd)',
    `block_size` INT DEFAULT 0 COMMENT 'block_size(block_size)',
    `max_iters` INT DEFAULT 0 COMMENT 'max_iters(max_iters)',
    `batch_size` INT DEFAULT 0 COMMENT 'batch_size(batch_size)',
    `learning_rate` DOUBLE DEFAULT 0 COMMENT 'learning_rate(learning_rate)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `progress` INT DEFAULT 0 COMMENT 'progress(progress)',
    `current_loss` DOUBLE DEFAULT 0 COMMENT 'current_loss(current_loss)',
    `current_iter` INT DEFAULT 0 COMMENT 'current_iter(current_iter)',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message(error_message)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `completed_at` DATETIME DEFAULT NULL COMMENT 'completed_at(completed_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='training_task (auto-generated V3.5.5)';


-- =========================================
-- 模块: rag (3 张表)
-- =========================================

-- rag/Document.java
DROP TABLE IF EXISTS `document`;
CREATE TABLE IF NOT EXISTS `document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `kb_id` BIGINT DEFAULT 0 COMMENT 'kb_id(kb_id)',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id(owner_id)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `source_type` VARCHAR(255) DEFAULT NULL COMMENT 'source_type(source_type)',
    `source_uri` VARCHAR(255) DEFAULT NULL COMMENT 'source_uri(source_uri)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `size_bytes` BIGINT DEFAULT 0 COMMENT 'size_bytes(size_bytes)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg(error_msg)',
    `chunk_count` INT DEFAULT 0 COMMENT 'chunk_count(chunk_count)',
    `checksum` VARCHAR(255) DEFAULT NULL COMMENT 'checksum(checksum)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='document (auto-generated V3.5.5)';

-- rag/DocumentChunk.java
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE IF NOT EXISTS `document_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `doc_id` BIGINT DEFAULT 0 COMMENT 'doc_id(doc_id)',
    `kb_id` BIGINT DEFAULT 0 COMMENT 'kb_id(kb_id)',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id(owner_id)',
    `chunk_index` INT DEFAULT 0 COMMENT 'chunk_index(chunk_index)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `embedding` BLOB DEFAULT NULL COMMENT 'embedding(embedding)',
    `dim` INT DEFAULT 0 COMMENT 'dim(dim)',
    `char_count` INT DEFAULT 0 COMMENT 'char_count(char_count)',
    `start_pos` INT DEFAULT 0 COMMENT 'start_pos(start_pos)',
    `end_pos` INT DEFAULT 0 COMMENT 'end_pos(end_pos)',
    `access_count` INT DEFAULT 0 COMMENT 'access_count(access_count)',
    `last_access_at` DATETIME DEFAULT NULL COMMENT 'last_access_at(last_access_at)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='document_chunk (auto-generated V3.5.5)';

-- rag/KnowledgeBase.java
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id(owner_id)',
    `tenant_id` BIGINT DEFAULT 0 COMMENT 'tenant_id(tenant_id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `visibility` VARCHAR(255) DEFAULT NULL COMMENT 'visibility(visibility)',
    `doc_count` INT DEFAULT 0 COMMENT 'doc_count(doc_count)',
    `chunk_count` INT DEFAULT 0 COMMENT 'chunk_count(chunk_count)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='knowledge_base (auto-generated V3.5.5)';


-- =========================================
-- 模块: function (2 张表)
-- =========================================

-- function/FunctionCallLog.java
DROP TABLE IF EXISTS `function_call_log`;
CREATE TABLE IF NOT EXISTS `function_call_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `session_id` BIGINT DEFAULT 0 COMMENT 'session_id(session_id)',
    `tool_name` VARCHAR(255) DEFAULT NULL COMMENT 'tool_name(tool_name)',
    `arguments` VARCHAR(255) DEFAULT NULL COMMENT 'arguments(arguments)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg(error_msg)',
    `duration_ms` INT DEFAULT 0 COMMENT 'duration_ms(duration_ms)',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent(user_agent)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='function_call_log (auto-generated V3.5.5)';

-- function/FunctionTool.java
DROP TABLE IF EXISTS `function_tool`;
CREATE TABLE IF NOT EXISTS `function_tool` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `display_name` VARCHAR(255) DEFAULT NULL COMMENT 'display_name(display_name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)',
    `owner_id` BIGINT DEFAULT 0 COMMENT 'owner_id(owner_id)',
    `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters(parameters)',
    `endpoint` VARCHAR(255) DEFAULT NULL COMMENT 'endpoint(endpoint)',
    `http_method` VARCHAR(255) DEFAULT NULL COMMENT 'http_method(http_method)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='function_tool (auto-generated V3.5.5)';


-- =========================================
-- 模块: chat (2 张表)
-- =========================================

-- chat/ChatMessage.java
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `session_id` BIGINT DEFAULT 0 COMMENT 'session_id(session_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `tokens` INT DEFAULT 0 COMMENT 'tokens(tokens)',
    `finish_reason` VARCHAR(255) DEFAULT NULL COMMENT 'finish_reason(finish_reason)',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message(error_message)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='chat_message (auto-generated V3.5.5)';

-- chat/ChatSession.java
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)',
    `system_prompt` VARCHAR(255) DEFAULT NULL COMMENT 'system_prompt(system_prompt)',
    `temperature` DECIMAL(20,4) DEFAULT 0 COMMENT 'temperature(temperature)',
    `status` INT DEFAULT 0 COMMENT '状态(status)',
    `message_count` INT DEFAULT 0 COMMENT 'message_count(message_count)',
    `last_message_at` DATETIME DEFAULT NULL COMMENT 'last_message_at(last_message_at)',
    `tenant_id` BIGINT DEFAULT 0 COMMENT 'tenant_id(tenant_id)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='chat_session (auto-generated V3.5.5)';


-- =========================================
-- 模块: monitor (4 张表)
-- =========================================

-- monitor/AlertChannel.java
DROP TABLE IF EXISTS `alert_channel`;
CREATE TABLE IF NOT EXISTS `alert_channel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `channel_type` VARCHAR(255) DEFAULT NULL COMMENT 'channel_type(channel_type)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `target` VARCHAR(255) DEFAULT NULL COMMENT 'target(target)',
    `config` VARCHAR(255) DEFAULT NULL COMMENT 'config(config)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `priority` INT DEFAULT 0 COMMENT 'priority(priority)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `created_by` BIGINT DEFAULT 0 COMMENT 'created_by(created_by)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='alert_channel (auto-generated V3.5.5)';

-- monitor/AlertEvent.java
DROP TABLE IF EXISTS `alert_event`;
CREATE TABLE IF NOT EXISTS `alert_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `rule_id` BIGINT DEFAULT 0 COMMENT 'rule_id(rule_id)',
    `rule_name` VARCHAR(255) DEFAULT NULL COMMENT 'rule_name(rule_name)',
    `severity` VARCHAR(255) DEFAULT NULL COMMENT 'severity(severity)',
    `metric_name` VARCHAR(255) DEFAULT NULL COMMENT 'metric_name(metric_name)',
    `message` VARCHAR(255) DEFAULT NULL COMMENT 'message(message)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `fired_at` DATETIME DEFAULT NULL COMMENT 'fired_at(fired_at)',
    `resolved_at` DATETIME DEFAULT NULL COMMENT 'resolved_at(resolved_at)',
    `acked_at` DATETIME DEFAULT NULL COMMENT 'acked_at(acked_at)',
    `acked_by` BIGINT DEFAULT 0 COMMENT 'acked_by(acked_by)',
    `duration` BIGINT DEFAULT 0 COMMENT 'duration(duration)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='alert_event (auto-generated V3.5.5)';

-- monitor/AlertRule.java
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE IF NOT EXISTS `alert_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `metric_name` VARCHAR(255) DEFAULT NULL COMMENT 'metric_name(metric_name)',
    `service` VARCHAR(255) DEFAULT NULL COMMENT 'service(service)',
    `operator` VARCHAR(255) DEFAULT NULL COMMENT 'operator(operator)',
    `severity` VARCHAR(255) DEFAULT NULL COMMENT 'severity(severity)',
    `cooldown_minutes` INT DEFAULT 0 COMMENT 'cooldown_minutes(cooldown_minutes)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `notify_channel` VARCHAR(255) DEFAULT NULL COMMENT 'notify_channel(notify_channel)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='alert_rule (auto-generated V3.5.5)';

-- monitor/MetricSnapshot.java
DROP TABLE IF EXISTS `metric_snapshot`;
CREATE TABLE IF NOT EXISTS `metric_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `service` VARCHAR(255) DEFAULT NULL COMMENT 'service(service)',
    `metric_name` VARCHAR(255) DEFAULT NULL COMMENT 'metric_name(metric_name)',
    `metric_value` DECIMAL(20,4) DEFAULT 0 COMMENT 'metric_value(metric_value)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `recorded_at` DATETIME DEFAULT NULL COMMENT 'recorded_at(recorded_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='metric_snapshot (auto-generated V3.5.5)';


-- =========================================
-- 模块: admin (2 张表)
-- =========================================

-- admin/AdminAuditLog.java
DROP TABLE IF EXISTS `admin_audit_log`;
CREATE TABLE IF NOT EXISTS `admin_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `actor_id` BIGINT DEFAULT 0 COMMENT 'actor_id(actor_id)',
    `actor_name` VARCHAR(255) DEFAULT NULL COMMENT 'actor_name(actor_name)',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)',
    `resource_type` VARCHAR(255) DEFAULT NULL COMMENT 'resource_type(resource_type)',
    `resource_id` VARCHAR(255) DEFAULT NULL COMMENT 'resource_id(resource_id)',
    `detail` VARCHAR(255) DEFAULT NULL COMMENT 'detail(detail)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg(error_msg)',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent(user_agent)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='admin_audit_log (auto-generated V3.5.5)';

-- admin/AuditLogFull.java
DROP TABLE IF EXISTS `audit_log_full`;
CREATE TABLE IF NOT EXISTS `audit_log_full` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `trace_id` VARCHAR(255) DEFAULT NULL COMMENT 'trace_id(trace_id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `user_ip` VARCHAR(255) DEFAULT NULL COMMENT 'user_ip(user_ip)',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'user_agent(user_agent)',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)',
    `resource_type` VARCHAR(255) DEFAULT NULL COMMENT 'resource_type(resource_type)',
    `resource_id` VARCHAR(255) DEFAULT NULL COMMENT 'resource_id(resource_id)',
    `method` VARCHAR(255) DEFAULT NULL COMMENT 'method(method)',
    `path` VARCHAR(255) DEFAULT NULL COMMENT 'path(path)',
    `request_body` VARCHAR(255) DEFAULT NULL COMMENT 'request_body(request_body)',
    `response_status` INT DEFAULT 0 COMMENT 'response_status(response_status)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg(error_msg)',
    `duration_ms` INT DEFAULT 0 COMMENT 'duration_ms(duration_ms)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='audit_log_full (auto-generated V3.5.5)';


-- =========================================
-- 模块: prompt (1 张表)
-- =========================================

-- prompt/PromptTemplate.java
DROP TABLE IF EXISTS `prompt_template`;
CREATE TABLE IF NOT EXISTS `prompt_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `variables` VARCHAR(255) DEFAULT NULL COMMENT 'variables(variables)',
    `creator_id` BIGINT DEFAULT 0 COMMENT 'creator_id(creator_id)',
    `creator_name` VARCHAR(255) DEFAULT NULL COMMENT 'creator_name(creator_name)',
    `is_public` TINYINT(1) DEFAULT 0 COMMENT 'is_public(is_public)',
    `use_count` INT DEFAULT 0 COMMENT 'use_count(use_count)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='prompt_template (auto-generated V3.5.5)';


-- =========================================
-- 模块: analytics (4 张表)
-- =========================================

-- analytics/DataSource.java
DROP TABLE IF EXISTS `analytics_datasource`;
CREATE TABLE IF NOT EXISTS `analytics_datasource` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `jdbc_url` VARCHAR(255) DEFAULT NULL COMMENT 'jdbc_url(jdbc_url)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `password_enc` VARCHAR(255) DEFAULT NULL COMMENT 'password_enc(password_enc)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `updated_at` DATETIME DEFAULT NULL COMMENT 'updated_at(updated_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='analytics_datasource (auto-generated V3.5.5)';

-- analytics/IngestTask.java
DROP TABLE IF EXISTS `analytics_ingest_task`;
CREATE TABLE IF NOT EXISTS `analytics_ingest_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `task_id` VARCHAR(255) DEFAULT NULL COMMENT 'task_id(task_id)',
    `filename` VARCHAR(255) DEFAULT NULL COMMENT 'filename(filename)',
    `file_type` VARCHAR(255) DEFAULT NULL COMMENT 'file_type(file_type)',
    `encoding` VARCHAR(255) DEFAULT NULL COMMENT 'encoding(encoding)',
    `separator` VARCHAR(255) DEFAULT NULL COMMENT 'separator(separator)',
    `file_size` BIGINT DEFAULT 0 COMMENT 'file_size(file_size)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message(error_message)',
    `quality_json` VARCHAR(255) DEFAULT NULL COMMENT 'quality_json(quality_json)',
    `total_rows` BIGINT DEFAULT 0 COMMENT 'total_rows(total_rows)',
    `total_columns` BIGINT DEFAULT 0 COMMENT 'total_columns(total_columns)',
    `columns_json` VARCHAR(255) DEFAULT NULL COMMENT 'columns_json(columns_json)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    `finished_at` DATETIME DEFAULT NULL COMMENT 'finished_at(finished_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='analytics_ingest_task (auto-generated V3.5.5)';

-- analytics/Nl2SqlHistory.java
DROP TABLE IF EXISTS `analytics_nlsql_history`;
CREATE TABLE IF NOT EXISTS `analytics_nlsql_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `data_source_id` BIGINT DEFAULT 0 COMMENT 'data_source_id(data_source_id)',
    `question` VARCHAR(255) DEFAULT NULL COMMENT 'question(question)',
    `generated_sql` VARCHAR(255) DEFAULT NULL COMMENT 'generated_sql(generated_sql)',
    `corrected_sql` VARCHAR(255) DEFAULT NULL COMMENT 'corrected_sql(corrected_sql)',
    `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)',
    `prompt_tokens` INT DEFAULT 0 COMMENT 'prompt_tokens(prompt_tokens)',
    `completion_tokens` INT DEFAULT 0 COMMENT 'completion_tokens(completion_tokens)',
    `duration_ms` BIGINT DEFAULT 0 COMMENT 'duration_ms(duration_ms)',
    `success` TINYINT(1) DEFAULT 0 COMMENT 'success(success)',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message(error_message)',
    `feedback_rating` INT DEFAULT 0 COMMENT 'feedback_rating(feedback_rating)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='analytics_nlsql_history (auto-generated V3.5.5)';

-- analytics/Report.java
DROP TABLE IF EXISTS `analytics_report`;
CREATE TABLE IF NOT EXISTS `analytics_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `user_id` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `report_id` VARCHAR(255) DEFAULT NULL COMMENT 'report_id(report_id)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `question` VARCHAR(255) DEFAULT NULL COMMENT 'question(question)',
    `sql_text` VARCHAR(255) DEFAULT NULL COMMENT 'sql_text(sql_text)',
    `markdown` VARCHAR(255) DEFAULT NULL COMMENT 'markdown(markdown)',
    `chart_options_json` VARCHAR(255) DEFAULT NULL COMMENT 'chart_options_json(chart_options_json)',
    `row_count` BIGINT DEFAULT 0 COMMENT 'row_count(row_count)',
    `duration_ms` BIGINT DEFAULT 0 COMMENT 'duration_ms(duration_ms)',
    `format` VARCHAR(255) DEFAULT NULL COMMENT 'format(format)',
    `created_at` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='analytics_report (auto-generated V3.5.5)';


-- =========================================
-- 模块: pipeline (4 张表)
-- =========================================

-- pipeline/PipelineNodeLog.java
DROP TABLE IF EXISTS `pipeline_node_log`;
CREATE TABLE IF NOT EXISTS `pipeline_node_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `run_id` BIGINT DEFAULT 0 COMMENT 'run_id(run_id)',
    `node_id` VARCHAR(255) DEFAULT NULL COMMENT 'node_id(node_id)',
    `node_type` VARCHAR(255) DEFAULT NULL COMMENT 'node_type(node_type)',
    `node_name` VARCHAR(255) DEFAULT NULL COMMENT 'node_name(node_name)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `start_time` DATETIME DEFAULT NULL COMMENT 'start_time(start_time)',
    `end_time` DATETIME DEFAULT NULL COMMENT 'end_time(end_time)',
    `duration_ms` BIGINT DEFAULT 0 COMMENT 'duration_ms(duration_ms)',
    `input_rows` INT DEFAULT 0 COMMENT 'input_rows(input_rows)',
    `output_rows` INT DEFAULT 0 COMMENT 'output_rows(output_rows)',
    `output_preview` VARCHAR(255) DEFAULT NULL COMMENT 'output_preview(output_preview)',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message(error_message)',
    `config_snapshot` VARCHAR(255) DEFAULT NULL COMMENT 'config_snapshot(config_snapshot)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='pipeline_node_log (auto-generated V3.5.5)';

-- pipeline/PipelineRun.java
DROP TABLE IF EXISTS `pipeline_run`;
CREATE TABLE IF NOT EXISTS `pipeline_run` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `workflow_id` BIGINT DEFAULT 0 COMMENT 'workflow_id(workflow_id)',
    `workflow_name` VARCHAR(255) DEFAULT NULL COMMENT 'workflow_name(workflow_name)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `trigger_by` BIGINT DEFAULT 0 COMMENT 'trigger_by(trigger_by)',
    `trigger_type` VARCHAR(255) DEFAULT NULL COMMENT 'trigger_type(trigger_type)',
    `definition_snapshot` VARCHAR(255) DEFAULT NULL COMMENT 'definition_snapshot(definition_snapshot)',
    `start_time` DATETIME DEFAULT NULL COMMENT 'start_time(start_time)',
    `end_time` DATETIME DEFAULT NULL COMMENT 'end_time(end_time)',
    `duration_ms` BIGINT DEFAULT 0 COMMENT 'duration_ms(duration_ms)',
    `error_message` VARCHAR(255) DEFAULT NULL COMMENT 'error_message(error_message)',
    `result_summary` VARCHAR(255) DEFAULT NULL COMMENT 'result_summary(result_summary)',
    `create_time` DATETIME DEFAULT NULL COMMENT 'create_time(create_time)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='pipeline_run (auto-generated V3.5.5)';

-- pipeline/PipelineWorkflow.java
DROP TABLE IF EXISTS `pipeline_workflow`;
CREATE TABLE IF NOT EXISTS `pipeline_workflow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `definition` VARCHAR(255) DEFAULT NULL COMMENT 'definition(definition)',
    `version` INT DEFAULT 0 COMMENT '乐观锁版本号(version)',
    `status` INT DEFAULT 0 COMMENT '状态(status)',
    `create_by` BIGINT DEFAULT 0 COMMENT 'create_by(create_by)',
    `update_by` BIGINT DEFAULT 0 COMMENT 'update_by(update_by)',
    `create_time` DATETIME DEFAULT NULL COMMENT 'create_time(create_time)',
    `update_time` DATETIME DEFAULT NULL COMMENT 'update_time(update_time)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='pipeline_workflow (auto-generated V3.5.5)';

-- pipeline/PipelineWorkflowVersion.java
DROP TABLE IF EXISTS `pipeline_workflow_version`;
CREATE TABLE IF NOT EXISTS `pipeline_workflow_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `workflow_id` BIGINT DEFAULT 0 COMMENT 'workflow_id(workflow_id)',
    `version` INT DEFAULT 0 COMMENT '乐观锁版本号(version)',
    `definition` VARCHAR(255) DEFAULT NULL COMMENT 'definition(definition)',
    `change_log` VARCHAR(255) DEFAULT NULL COMMENT 'change_log(change_log)',
    `create_by` BIGINT DEFAULT 0 COMMENT 'create_by(create_by)',
    `create_time` DATETIME DEFAULT NULL COMMENT 'create_time(create_time)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='pipeline_workflow_version (auto-generated V3.5.5)';


-- =========================================
-- 模块: ws (3 张表)
-- =========================================

-- ws/CollabMessage.java
DROP TABLE IF EXISTS `collab_message`;
CREATE TABLE IF NOT EXISTS `collab_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `roomId` VARCHAR(255) DEFAULT NULL COMMENT 'roomId(roomId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata(metadata)',
    `clientMsgId` VARCHAR(255) DEFAULT NULL COMMENT 'clientMsgId(clientMsgId)',
    `broadcast` INT DEFAULT 0 COMMENT 'broadcast(broadcast)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='collab_message (auto-generated V3.5.5)';

-- ws/CollabParticipant.java
DROP TABLE IF EXISTS `collab_participant`;
CREATE TABLE IF NOT EXISTS `collab_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `roomId` VARCHAR(255) DEFAULT NULL COMMENT 'roomId(roomId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)',
    `cursorX` INT DEFAULT 0 COMMENT 'cursorX(cursorX)',
    `cursorY` INT DEFAULT 0 COMMENT 'cursorY(cursorY)',
    `selectionId` VARCHAR(255) DEFAULT NULL COMMENT 'selectionId(selectionId)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `joinedAt` DATETIME DEFAULT NULL COMMENT 'joinedAt(joinedAt)',
    `leftAt` DATETIME DEFAULT NULL COMMENT 'leftAt(leftAt)',
    `lastHeartbeat` DATETIME DEFAULT NULL COMMENT 'lastHeartbeat(lastHeartbeat)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='collab_participant (auto-generated V3.5.5)';

-- ws/CollabRoom.java
DROP TABLE IF EXISTS `collab_room`;
CREATE TABLE IF NOT EXISTS `collab_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `roomId` VARCHAR(255) DEFAULT NULL COMMENT 'roomId(roomId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `ownerName` VARCHAR(255) DEFAULT NULL COMMENT 'ownerName(ownerName)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `isPublic` INT DEFAULT 0 COMMENT 'isPublic(isPublic)',
    `maxParticipants` INT DEFAULT 0 COMMENT 'maxParticipants(maxParticipants)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `currentParticipants` INT DEFAULT 0 COMMENT 'currentParticipants(currentParticipants)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `lastActivityAt` DATETIME DEFAULT NULL COMMENT 'lastActivityAt(lastActivityAt)',
    `closedAt` DATETIME DEFAULT NULL COMMENT 'closedAt(closedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='collab_room (auto-generated V3.5.5)';

SET FOREIGN_KEY_CHECKS = 1;

-- 完成: 共 77 张表