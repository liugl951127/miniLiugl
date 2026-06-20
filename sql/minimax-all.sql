-- ============================================================
-- Day 1 - 创建数据库
-- ============================================================
CREATE DATABASE IF NOT EXISTS `minimax_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- MiniMax Platform - Day 2: User & Auth Schema
-- MySQL 8.x  |  utf8mb4  |  InnoDB
-- ============================================================

CREATE DATABASE IF NOT EXISTS `minimax_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `minimax_platform`;

-- ------------------------------------------------------------
-- sys_user 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        VARCHAR(64)  NOT NULL                COMMENT '用户名(唯一)',
  `password`        VARCHAR(128) NOT NULL                COMMENT 'BCrypt 密码哈希',
  `nickname`        VARCHAR(64)           DEFAULT NULL   COMMENT '昵称',
  `email`           VARCHAR(128)          DEFAULT NULL   COMMENT '邮箱',
  `phone`           VARCHAR(32)           DEFAULT NULL   COMMENT '手机号',
  `avatar`          VARCHAR(256)          DEFAULT NULL   COMMENT '头像 URL',
  `gender`          TINYINT               DEFAULT 0      COMMENT '0未知 1男 2女',
  `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '0禁用 1正常',
  `last_login_ip`   VARCHAR(64)           DEFAULT NULL   COMMENT '最近登录 IP',
  `last_login_at`   DATETIME              DEFAULT NULL   COMMENT '最近登录时间',
  `tenant_id`       BIGINT       NOT NULL DEFAULT 0      COMMENT '租户 ID(预留多租户)',
  `remark`          VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
  `created_by`      BIGINT                DEFAULT NULL,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by`      BIGINT                DEFAULT NULL,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '0未删 1已删(逻辑)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- sys_role 角色表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(32)  NOT NULL COMMENT '角色编码: ADMIN / USER / GUEST',
  `name`        VARCHAR(64)  NOT NULL COMMENT '角色名',
  `description` VARCHAR(255)          DEFAULT NULL,
  `sort`        INT          NOT NULL DEFAULT 0,
  `enabled`     TINYINT      NOT NULL DEFAULT 1,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ------------------------------------------------------------
-- sys_user_role 用户-角色关联
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `idx_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- ------------------------------------------------------------
-- auth_refresh_token 刷新令牌表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `auth_refresh_token`;
CREATE TABLE `auth_refresh_token` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL,
  `token`       VARCHAR(128) NOT NULL COMMENT 'refresh token 哈希',
  `expires_at`  DATETIME     NOT NULL,
  `revoked`     TINYINT      NOT NULL DEFAULT 0 COMMENT '1 已撤销',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_user` (`user_id`),
  KEY `idx_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷新令牌';

-- ------------------------------------------------------------
-- auth_login_log 登录日志
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `auth_login_log`;
CREATE TABLE `auth_login_log` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT                DEFAULT NULL,
  `username`   VARCHAR(64)           DEFAULT NULL,
  `ip`         VARCHAR(64)           DEFAULT NULL,
  `user_agent` VARCHAR(512)          DEFAULT NULL,
  `status`     TINYINT      NOT NULL COMMENT '0失败 1成功',
  `message`    VARCHAR(255)          DEFAULT NULL,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';

-- ============================================================
-- 初始数据
-- ============================================================
-- 默认密码: admin@123  (BCrypt cost=10)
-- 哈希: $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
INSERT INTO `sys_user` (`username`,`password`,`nickname`,`email`,`status`)
VALUES ('admin','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','超级管理员','admin@minimax.local',1);

INSERT INTO `sys_role` (`code`,`name`,`description`,`sort`) VALUES
('ADMIN','超级管理员','拥有所有权限',1),
('USER' ,'普通用户'  ,'日常对话与知识库',2);

INSERT INTO `sys_user_role` (`user_id`,`role_id`)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username='admin' AND r.code='ADMIN';

-- ============================================================
-- MiniMax Platform - Day 3: Chat Session & Message
-- MySQL 8.x  |  utf8mb4  |  InnoDB
-- ============================================================

USE `minimax_platform`;

-- ------------------------------------------------------------
-- chat_session 会话表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       NOT NULL COMMENT '所属用户',
  `title`        VARCHAR(255) NOT NULL DEFAULT '新会话' COMMENT '会话标题',
  `model`        VARCHAR(64)           DEFAULT NULL COMMENT '使用的模型: gpt-4 / qwen-max / ollama:llama3',
  `system_prompt` TEXT                  COMMENT '系统提示词',
  `temperature`  DECIMAL(3,2) NOT NULL DEFAULT 0.70 COMMENT '温度 0-2',
  `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '0归档 1正常',
  `message_count` INT          NOT NULL DEFAULT 0 COMMENT '消息数(冗余)',
  `last_message_at` DATETIME            DEFAULT NULL COMMENT '最后消息时间',
  `tenant_id`    BIGINT       NOT NULL DEFAULT 0,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_updated` (`user_id`, `updated_at`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_last_message` (`last_message_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话';

-- ------------------------------------------------------------
-- chat_message 消息表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id`          BIGINT     NOT NULL AUTO_INCREMENT,
  `session_id`  BIGINT     NOT NULL,
  `user_id`     BIGINT     NOT NULL COMMENT '冗余,加速查询',
  `role`        VARCHAR(16) NOT NULL COMMENT 'user / assistant / system / tool',
  `content`     MEDIUMTEXT  NOT NULL COMMENT '消息内容(支持长文)',
  `tokens`      INT          DEFAULT NULL COMMENT 'token 消耗',
  `finish_reason` VARCHAR(32) DEFAULT NULL COMMENT 'stop / length / tool_calls',
  `error_message` VARCHAR(512) DEFAULT NULL,
  `created_at`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`     TINYINT    NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_session_created` (`session_id`, `created_at`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息';

-- ============================================================
-- MiniMax Platform - Day 4: Model Router
-- 模型供应商 / 模型配置 / 配额使用
-- ============================================================

USE `minimax_platform`;

-- ------------------------------------------------------------
-- model_provider 模型供应商（如 openai / anthropic / ollama / minimax）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `model_provider`;
CREATE TABLE `model_provider` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(32)  NOT NULL COMMENT 'openai / anthropic / ollama / minimax / zhipu / qwen',
  `name`        VARCHAR(64)  NOT NULL,
  `base_url`    VARCHAR(255) NOT NULL COMMENT 'API base URL',
  `api_key`     VARCHAR(255)          DEFAULT NULL COMMENT 'API key（如需）',
  `protocol`    VARCHAR(16)  NOT NULL DEFAULT 'openai' COMMENT 'openai / anthropic / ollama',
  `enabled`     TINYINT      NOT NULL DEFAULT 1,
  `sort`        INT          NOT NULL DEFAULT 0,
  `description` VARCHAR(255)          DEFAULT NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型供应商';

-- ------------------------------------------------------------
-- model_config 模型（在某个 provider 下的具体模型）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `model_config`;
CREATE TABLE `model_config` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `provider_id`    BIGINT       NOT NULL,
  `model_code`     VARCHAR(64)  NOT NULL COMMENT 'gpt-4 / claude-3-opus / llama3:70b / MiniMax-Text-01',
  `display_name`   VARCHAR(128) NOT NULL,
  `max_context`    INT          NOT NULL DEFAULT 8192 COMMENT '最大上下文 tokens',
  `max_output`     INT          NOT NULL DEFAULT 4096,
  `input_price`    DECIMAL(12,6)        DEFAULT 0 COMMENT '每 1k token 输入价',
  `output_price`   DECIMAL(12,6)        DEFAULT 0 COMMENT '每 1k token 输出价',
  `supports_vision`  TINYINT    NOT NULL DEFAULT 0,
  `supports_tools`   TINYINT    NOT NULL DEFAULT 0,
  `supports_stream`  TINYINT    NOT NULL DEFAULT 1,
  `enabled`        TINYINT      NOT NULL DEFAULT 1,
  `sort`           INT          NOT NULL DEFAULT 0,
  `description`    VARCHAR(255)         DEFAULT NULL,
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_model` (`provider_id`, `model_code`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型配置';

-- ------------------------------------------------------------
-- model_quota 用户配额（按用户/模型/天 计数）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `model_quota`;
CREATE TABLE `model_quota` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL,
  `model_id`   BIGINT       NOT NULL,
  `quota_date` DATE         NOT NULL,
  `used_tokens` BIGINT      NOT NULL DEFAULT 0,
  `used_requests` INT        NOT NULL DEFAULT 0,
  `limit_tokens` BIGINT     NOT NULL DEFAULT 100000 COMMENT '每日 token 上限',
  `limit_requests` INT       NOT NULL DEFAULT 100 COMMENT '每日请求上限',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_model_date` (`user_id`, `model_id`, `quota_date`),
  KEY `idx_user_date` (`user_id`, `quota_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户配额';

-- ============================================================
-- 初始数据：3 个 provider + 6 个模型
-- ============================================================
INSERT INTO `model_provider` (`code`,`name`,`base_url`,`api_key`,`protocol`,`enabled`,`sort`,`description`) VALUES
('openai','OpenAI','https://api.openai.com/v1',NULL,'openai',1,1,'OpenAI 官方 API（需自备 key）'),
('minimax','Minimax-M3','https://api.minimax.chat/v1',NULL,'openai',1,2,'MiniMax 大模型（公司内部）'),
('ollama','Ollama（本地）','http://localhost:11434/v1',NULL,'openai',1,3,'本地 Ollama 兼容 OpenAI');

-- 给每个 provider 配 2 个模型
INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'gpt-4o-mini', 'GPT-4o mini', 128000, 16384, 0.00015, 0.0006, 1, 1, 1, 1, 1, '便宜快速的多模态模型' FROM model_provider p WHERE p.code='openai';

INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'gpt-4o', 'GPT-4o', 128000, 16384, 0.0025, 0.01, 1, 1, 1, 1, 2, '旗舰多模态模型' FROM model_provider p WHERE p.code='openai';

INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'MiniMax-Text-01', 'MiniMax-M3 Text', 1000000, 8192, 0.001, 0.002, 0, 1, 1, 1, 1, 'MiniMax 主力文本模型' FROM model_provider p WHERE p.code='minimax';

INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'MiniMax-VL-01', 'MiniMax-M3 Vision', 1000000, 8192, 0.002, 0.004, 1, 1, 1, 1, 2, 'MiniMax 多模态模型' FROM model_provider p WHERE p.code='minimax';

INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'llama3:8b', 'Llama 3 8B (本地)', 8192, 4096, 0, 0, 0, 0, 1, 1, 1, '本地 Ollama 模型' FROM model_provider p WHERE p.code='ollama';

INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'qwen2.5:7b', 'Qwen 2.5 7B (本地)', 32768, 8192, 0, 0, 0, 0, 1, 1, 2, '本地 Ollama 中文模型' FROM model_provider p WHERE p.code='ollama';

-- ============================================================
-- MiniMax Platform - Day 7: Long-Term Memory (Vector)
-- 长期记忆：用 MySQL BLOB 存向量，Java 端做余弦相似度检索
-- ============================================================

USE `minimax_platform`;

-- ------------------------------------------------------------
-- memory_long_term 长期记忆（向量 + 原文 + 元数据）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `memory_long_term`;
CREATE TABLE `memory_long_term` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       NOT NULL COMMENT '所属用户',
  `session_id`   BIGINT                DEFAULT NULL COMMENT '来源会话 (可空 = 跨会话)',
  `content`      MEDIUMTEXT   NOT NULL COMMENT '原文',
  `summary`      VARCHAR(500)         DEFAULT NULL COMMENT 'LLM 摘要',
  `role`         VARCHAR(16)           DEFAULT 'user' COMMENT 'user / assistant / system',
  `embedding`    LONGBLOB     NOT NULL COMMENT '向量 (float[] 序列化, 通常 384-1536 维)',
  `dim`          INT          NOT NULL COMMENT '向量维度',
  `importance`   DECIMAL(3,2) NOT NULL DEFAULT 0.50 COMMENT '重要性 0-1',
  `tags`         VARCHAR(255)          DEFAULT NULL COMMENT '逗号分隔标签',
  `access_count` INT          NOT NULL DEFAULT 0 COMMENT '被召回次数',
  `last_access_at` DATETIME            DEFAULT NULL,
  `expires_at`   DATETIME             DEFAULT NULL COMMENT 'null=永不过期',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_user_session` (`user_id`, `session_id`),
  KEY `idx_importance` (`importance`),
  KEY `idx_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='长期记忆（向量）';

-- ------------------------------------------------------------
-- memory_user_pref 用户偏好（轻量 KV）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `memory_user_pref`;
CREATE TABLE `memory_user_pref` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL,
  `pref_key`   VARCHAR(64)  NOT NULL COMMENT 'language / tone / style / likes / ...',
  `pref_value` TEXT         NOT NULL,
  `weight`     DECIMAL(3,2) NOT NULL DEFAULT 0.50,
  `source`     VARCHAR(32)           DEFAULT 'manual' COMMENT 'manual / inferred',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_key` (`user_id`, `pref_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好记忆';

-- ============================================================
-- MiniMax Platform - Day 8: RAG (Knowledge Base + Document + Chunk)
-- ============================================================

USE `minimax_platform`;

-- ------------------------------------------------------------
-- knowledge_base 知识库 (用户的"文档容器")
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `owner_id`    BIGINT       NOT NULL COMMENT '所属用户 (创建者)',
  `tenant_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户 (0=个人)',
  `name`        VARCHAR(128) NOT NULL,
  `description` VARCHAR(500)          DEFAULT NULL,
  `visibility`  VARCHAR(16)  NOT NULL DEFAULT 'private' COMMENT 'private/public/team',
  `doc_count`   INT          NOT NULL DEFAULT 0,
  `chunk_count` INT          NOT NULL DEFAULT 0,
  `tags`        VARCHAR(255)         DEFAULT NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_name` (`owner_id`, `name`, `deleted`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_visibility` (`visibility`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库';

-- ------------------------------------------------------------
-- document 文档
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `kb_id`         BIGINT       NOT NULL,
  `owner_id`      BIGINT       NOT NULL,
  `title`         VARCHAR(255) NOT NULL,
  `source_type`   VARCHAR(16)  NOT NULL DEFAULT 'txt' COMMENT 'txt/md/docx/pdf',
  `source_uri`    VARCHAR(500)          DEFAULT NULL COMMENT '原始文件位置/MinIO key/url',
  `content`       MEDIUMTEXT            DEFAULT NULL COMMENT '解析后纯文本',
  `size_bytes`    BIGINT       NOT NULL DEFAULT 0,
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/parsing/chunked/failed',
  `error_msg`     VARCHAR(500)          DEFAULT NULL,
  `chunk_count`   INT          NOT NULL DEFAULT 0,
  `checksum`      VARCHAR(64)           DEFAULT NULL COMMENT 'SHA-256 去重',
  `tags`          VARCHAR(255)          DEFAULT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_kb` (`kb_id`),
  KEY `idx_owner_created` (`owner_id`, `created_at`),
  KEY `idx_status` (`status`),
  KEY `idx_checksum` (`owner_id`, `checksum`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档';

-- ------------------------------------------------------------
-- document_chunk 文档切片 (含向量, 复用 Day 7 思路)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `doc_id`      BIGINT       NOT NULL,
  `kb_id`       BIGINT       NOT NULL,
  `owner_id`    BIGINT       NOT NULL,
  `chunk_index` INT          NOT NULL COMMENT '在文档内的顺序',
  `content`     MEDIUMTEXT   NOT NULL,
  `embedding`   LONGBLOB     NOT NULL,
  `dim`         INT          NOT NULL,
  `char_count`  INT          NOT NULL DEFAULT 0,
  `start_pos`   INT          NOT NULL DEFAULT 0,
  `end_pos`     INT          NOT NULL DEFAULT 0,
  `access_count` INT         NOT NULL DEFAULT 0,
  `last_access_at` DATETIME           DEFAULT NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_doc` (`doc_id`, `chunk_index`),
  KEY `idx_kb` (`kb_id`),
  KEY `idx_owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档切片(向量)';

-- ============================================================
-- MiniMax Platform - Day 9: Function Calling
-- ============================================================

USE `minimax_platform`;

-- ------------------------------------------------------------
-- function_tool 工具注册表 (系统 + 用户自定义)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `function_tool`;
CREATE TABLE `function_tool` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `name`         VARCHAR(64)  NOT NULL COMMENT '工具名 (唯一)',
  `display_name` VARCHAR(128) NOT NULL,
  `description`  VARCHAR(500) NOT NULL COMMENT '工具描述 (给 LLM 看的)',
  `category`     VARCHAR(32)  NOT NULL DEFAULT 'custom' COMMENT 'system/custom/http/shell/...',
  `scope`        VARCHAR(16)  NOT NULL DEFAULT 'builtin' COMMENT 'builtin/user',
  `owner_id`     BIGINT                DEFAULT NULL COMMENT '自定义工具的 owner (null=系统内置)',
  `parameters`   MEDIUMTEXT   NOT NULL COMMENT 'JSON Schema 形式的参数定义 (OpenAI tool format)',
  `endpoint`     VARCHAR(255)          DEFAULT NULL COMMENT '内置: Java class FQN; 自定义: HTTP URL',
  `http_method`  VARCHAR(8)            DEFAULT NULL COMMENT 'POST/GET (自定义 HTTP 工具)',
  `enabled`      TINYINT      NOT NULL DEFAULT 1,
  `tags`         VARCHAR(255)          DEFAULT NULL,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`, `deleted`),
  KEY `idx_scope_category` (`scope`, `category`),
  KEY `idx_owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具注册表';

-- ------------------------------------------------------------
-- function_call_log 调用审计
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `function_call_log`;
CREATE TABLE `function_call_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       NOT NULL,
  `session_id`   BIGINT                DEFAULT NULL,
  `tool_name`    VARCHAR(64)  NOT NULL,
  `arguments`    MEDIUMTEXT            DEFAULT NULL,
  `result`       MEDIUMTEXT            DEFAULT NULL,
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'ok' COMMENT 'ok / error / timeout',
  `error_msg`    VARCHAR(500)          DEFAULT NULL,
  `duration_ms`  INT          NOT NULL DEFAULT 0,
  `ip`           VARCHAR(64)           DEFAULT NULL,
  `user_agent`   VARCHAR(255)          DEFAULT NULL,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_tool` (`tool_name`, `created_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具调用审计';

-- ------------------------------------------------------------
-- 初始数据: 4 个内置工具
-- ------------------------------------------------------------
INSERT INTO `function_tool` (`name`, `display_name`, `description`, `category`, `scope`, `parameters`, `endpoint`, `enabled`, `tags`) VALUES
('get_current_time', '获取当前时间', '返回服务器当前时间，可选指定时区 (默认 Asia/Shanghai)。', 'system', 'builtin',
 '{"type":"object","properties":{"timezone":{"type":"string","description":"时区 ID，如 Asia/Shanghai, UTC","default":"Asia/Shanghai"}},"required":[]}',
 'com.minimax.function.builtin.TimeTool', 1, 'time,system'),

('calculator', '计算器', '执行数学表达式计算，支持 +, -, *, /, %, 括号, 数学函数 (sin, cos, sqrt, log, abs, pow 等)。', 'system', 'builtin',
 '{"type":"object","properties":{"expression":{"type":"string","description":"数学表达式，如 (1+2)*3 或 sqrt(16)"}},"required":["expression"]}',
 'com.minimax.function.builtin.CalculatorTool', 1, 'math,system'),

('http_get', 'HTTP GET 请求', '对指定 URL 发起 HTTP GET 请求，返回响应体 (前 5000 字符)。', 'system', 'builtin',
 '{"type":"object","properties":{"url":{"type":"string","description":"完整 URL (http/https)"},"timeout_seconds":{"type":"integer","default":10,"minimum":1,"maximum":60}},"required":["url"]}',
 'com.minimax.function.builtin.HttpGetTool', 1, 'http,system'),

('random_number', '随机数生成', '生成指定范围内的随机整数。', 'system', 'builtin',
 '{"type":"object","properties":{"min":{"type":"integer","default":1,"description":"最小值"},"max":{"type":"integer","default":100,"description":"最大值"}},"required":[]}',
 'com.minimax.function.builtin.RandomNumberTool', 1, 'random,system');

-- ============================================================
-- MiniMax Platform - Day 10: Admin 后台 + 统一审计
-- ============================================================

USE `minimax_platform`;

-- ------------------------------------------------------------
-- admin_audit_log 统一操作审计
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `admin_audit_log`;
CREATE TABLE `admin_audit_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `actor_id`      BIGINT       NOT NULL COMMENT '操作人 (admin user)',
  `actor_name`    VARCHAR(64)           DEFAULT NULL,
  `action`        VARCHAR(64)  NOT NULL COMMENT 'create_user/disable_user/reset_password/...',
  `resource_type` VARCHAR(32)  NOT NULL COMMENT 'user/model/kb/document/tool/...',
  `resource_id`   VARCHAR(64)           DEFAULT NULL,
  `detail`        MEDIUMTEXT            DEFAULT NULL COMMENT 'JSON 详情',
  `result`        VARCHAR(16)  NOT NULL DEFAULT 'ok' COMMENT 'ok/error',
  `error_msg`     VARCHAR(500)          DEFAULT NULL,
  `ip`            VARCHAR(64)           DEFAULT NULL,
  `user_agent`    VARCHAR(255)          DEFAULT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_actor_created` (`actor_id`, `created_at`),
  KEY `idx_action` (`action`),
  KEY `idx_resource` (`resource_type`, `resource_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一操作审计';

-- ============================================================
-- MiniMax Platform - Day 12: Monitor
-- ============================================================

USE `minimax_platform`;

-- ------------------------------------------------------------
-- metric_snapshot 指标快照 (定期落库, 供历史趋势)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `metric_snapshot`;
CREATE TABLE `metric_snapshot` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `service`       VARCHAR(32)  NOT NULL COMMENT '指标来源服务',
  `metric_name`   VARCHAR(64)  NOT NULL COMMENT '指标名 (e.g. cpu_usage, jvm_heap)',
  `metric_value`  DECIMAL(20,4) NOT NULL,
  `tags`          VARCHAR(500)         DEFAULT NULL COMMENT 'JSON 标签 {host: x, region: y}',
  `recorded_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_service_metric` (`service`, `metric_name`),
  KEY `idx_recorded` (`recorded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标快照';

-- ------------------------------------------------------------
-- alert_rule 告警规则
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE `alert_rule` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(64)  NOT NULL,
  `description`   VARCHAR(255)          DEFAULT NULL,
  `metric_name`   VARCHAR(64)  NOT NULL,
  `service`       VARCHAR(32)           DEFAULT NULL,
  `operator`      VARCHAR(8)   NOT NULL DEFAULT '>' COMMENT '>/</>=/<=/=/!=',
  `threshold`     DECIMAL(20,4) NOT NULL,
  `severity`      VARCHAR(16)  NOT NULL DEFAULT 'warning' COMMENT 'info/warning/critical',
  `enabled`       TINYINT      NOT NULL DEFAULT 1,
  `cooldown_minutes` INT       NOT NULL DEFAULT 5 COMMENT '触发后冷却时间',
  `notify_channel` VARCHAR(255)         DEFAULT NULL COMMENT 'JSON: {email, webhook, ...}',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则';

-- ------------------------------------------------------------
-- alert_event 告警事件
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `alert_event`;
CREATE TABLE `alert_event` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `rule_id`       BIGINT       NOT NULL,
  `rule_name`     VARCHAR(64)  NOT NULL,
  `severity`      VARCHAR(16)  NOT NULL,
  `metric_name`   VARCHAR(64)  NOT NULL,
  `metric_value`  DECIMAL(20,4) NOT NULL,
  `threshold`     DECIMAL(20,4) NOT NULL,
  `message`       VARCHAR(500)          DEFAULT NULL,
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'firing' COMMENT 'firing/resolved',
  `fired_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resolved_at`   DATETIME              DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_rule` (`rule_id`),
  KEY `idx_status` (`status`),
  KEY `idx_fired` (`fired_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警事件';

-- 初始 5 个默认告警规则
INSERT INTO `alert_rule` (`name`, `description`, `metric_name`, `service`, `operator`, `threshold`, `severity`, `cooldown_minutes`) VALUES
('CPU 高', 'CPU 使用率 > 80%', 'cpu_usage', NULL, '>', 80, 'warning', 5),
('JVM Heap 高', 'JVM 堆使用率 > 85%', 'jvm_heap_usage', NULL, '>', 85, 'critical', 5),
('磁盘高', '磁盘使用率 > 90%', 'disk_usage', NULL, '>', 90, 'critical', 10),
('LLM 延迟高', 'LLM 平均响应时间 > 3000ms', 'llm_avg_latency', 'model', '>', 3000, 'warning', 3),
('错误率高', 'HTTP 5xx 错误率 > 5%', 'http_5xx_rate', NULL, '>', 5, 'critical', 2);

-- ============================================================
-- MiniMax Platform - Day 13: Optimization
-- ============================================================

USE `minimax_platform`;

-- ------------------------------------------------------------
-- request_log 请求日志 (慢请求/错误请求采点)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `request_log`;
CREATE TABLE `request_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `trace_id`      VARCHAR(32)           DEFAULT NULL COMMENT '链路追踪 ID',
  `service`       VARCHAR(32)  NOT NULL,
  `method`        VARCHAR(8)   NOT NULL,
  `path`          VARCHAR(255) NOT NULL,
  `query_string`  VARCHAR(500)          DEFAULT NULL,
  `user_id`       BIGINT                DEFAULT NULL,
  `client_ip`     VARCHAR(64)           DEFAULT NULL,
  `user_agent`    VARCHAR(255)          DEFAULT NULL,
  `status`        INT          NOT NULL,
  `duration_ms`   INT          NOT NULL,
  `is_slow`       TINYINT      NOT NULL DEFAULT 0,
  `is_error`      TINYINT      NOT NULL DEFAULT 0,
  `error_msg`     VARCHAR(500)          DEFAULT NULL,
  `req_size`      INT          NOT NULL DEFAULT 0,
  `resp_size`     INT          NOT NULL DEFAULT 0,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_service_created` (`service`, `created_at`),
  KEY `idx_slow` (`is_slow`, `created_at`),
  KEY `idx_error` (`is_error`, `created_at`),
  KEY `idx_trace` (`trace_id`),
  KEY `idx_path` (`path`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请求日志';

-- ------------------------------------------------------------
-- async_task 异步任务
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `async_task`;
CREATE TABLE `async_task` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`       VARCHAR(64)  NOT NULL COMMENT '任务 ID (UUID)',
  `task_type`     VARCHAR(64)  NOT NULL COMMENT '任务类型',
  `payload`       MEDIUMTEXT            DEFAULT NULL COMMENT 'JSON 参数',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/running/done/failed',
  `priority`      INT          NOT NULL DEFAULT 5 COMMENT '1-10, 越小越优先',
  `attempts`      INT          NOT NULL DEFAULT 0,
  `max_attempts`  INT          NOT NULL DEFAULT 3,
  `result`        MEDIUMTEXT            DEFAULT NULL,
  `error_msg`     VARCHAR(1000)         DEFAULT NULL,
  `scheduled_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `started_at`    DATETIME              DEFAULT NULL,
  `finished_at`   DATETIME              DEFAULT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_status_priority` (`status`, `priority`, `scheduled_at`),
  KEY `idx_type` (`task_type`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步任务';

-- ------------------------------------------------------------
-- rate_limit_rule 限流规则 (可配置化)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `rate_limit_rule`;
CREATE TABLE `rate_limit_rule` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(64)  NOT NULL,
  `description`   VARCHAR(255)          DEFAULT NULL,
  `scope`         VARCHAR(16)  NOT NULL DEFAULT 'ip' COMMENT 'ip/user/global/api_key',
  `key_pattern`   VARCHAR(128)          DEFAULT NULL COMMENT 'url / path / method:path',
  `capacity`      INT          NOT NULL DEFAULT 10 COMMENT '桶容量',
  `refill_tokens` INT          NOT NULL DEFAULT 10 COMMENT '每次补充 token 数',
  `refill_period_seconds` INT   NOT NULL DEFAULT 60 COMMENT '补充周期',
  `enabled`       TINYINT      NOT NULL DEFAULT 1,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限流规则';

-- 初始限流规则
INSERT INTO `rate_limit_rule` (`name`, `description`, `scope`, `key_pattern`, `capacity`, `refill_tokens`, `refill_period_seconds`) VALUES
('default-ip', '默认 IP 限流', 'ip', NULL, 100, 100, 60),
('auth-login', '登录端点 IP 限流', 'ip', 'POST:/api/v1/auth/login', 10, 10, 60),
('chat-send', '聊天 IP 限流', 'ip', 'POST:/api/v1/sessions/*/messages', 60, 60, 60),
('rag-upload', 'RAG 上传 IP 限流', 'ip', 'POST:/api/v1/rag/doc/upload', 20, 20, 60);

-- ============================================================
-- MiniMax Platform - V2: Agent / 知识图谱 / 协作 / 插件
-- ============================================================

USE `minimax_platform`;

-- ------------------------------------------------------------
-- agent_task 自主任务
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `agent_task`;
CREATE TABLE `agent_task` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`       VARCHAR(64)  NOT NULL,
  `user_id`       BIGINT       NOT NULL,
  `session_id`   BIGINT                DEFAULT NULL,
  `goal`          VARCHAR(500) NOT NULL,
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/running/success/failed/timeout',
  `plan_json`     MEDIUMTEXT            DEFAULT NULL COMMENT '任务拆解的步骤',
  `steps_json`    MEDIUMTEXT            DEFAULT NULL COMMENT '执行步骤 + 中间结果',
  `final_answer`  MEDIUMTEXT            DEFAULT NULL,
  `error_msg`     VARCHAR(1000)         DEFAULT NULL,
  `rounds`        INT          NOT NULL DEFAULT 0 COMMENT 'ReAct 轮数',
  `max_rounds`    INT          NOT NULL DEFAULT 8,
  `tools_used`    VARCHAR(500)          DEFAULT NULL COMMENT '逗号分隔',
  `started_at`    DATETIME              DEFAULT NULL,
  `finished_at`   DATETIME              DEFAULT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 自主任务';

-- ------------------------------------------------------------
-- kg_entity 知识图谱 - 实体
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `kg_entity`;
CREATE TABLE `kg_entity` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL,
  `name`          VARCHAR(128) NOT NULL,
  `entity_type`   VARCHAR(32)  NOT NULL COMMENT 'person/place/org/concept/event/...',
  `description`   VARCHAR(500)          DEFAULT NULL,
  `aliases`       VARCHAR(255)          DEFAULT NULL COMMENT '别名逗号分隔',
  `importance`    INT          NOT NULL DEFAULT 5 COMMENT '1-10',
  `source`        VARCHAR(32)  NOT NULL DEFAULT 'manual' COMMENT 'manual/auto/manual+auto',
  `ref_count`     INT          NOT NULL DEFAULT 0,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_name` (`user_id`, `name`),
  KEY `idx_type` (`entity_type`),
  KEY `idx_importance` (`importance`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识图谱-实体';

-- ------------------------------------------------------------
-- kg_relation 知识图谱 - 关系
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `kg_relation`;
CREATE TABLE `kg_relation` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL,
  `from_entity`   BIGINT       NOT NULL,
  `to_entity`     BIGINT       NOT NULL,
  `relation_type` VARCHAR(32)  NOT NULL COMMENT 'works_at/located_in/friend_of/owns/...',
  `description`   VARCHAR(500)          DEFAULT NULL,
  `weight`        DECIMAL(3,2) NOT NULL DEFAULT 1.00,
  `source`        VARCHAR(32)  NOT NULL DEFAULT 'manual',
  `ref_count`     INT          NOT NULL DEFAULT 0,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_from` (`from_entity`),
  KEY `idx_to` (`to_entity`),
  KEY `idx_user_type` (`user_id`, `relation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识图谱-关系';

-- ------------------------------------------------------------
-- collab_session 协作会话
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `collab_session`;
CREATE TABLE `collab_session` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `session_id`    VARCHAR(64)  NOT NULL,
  `owner_id`      BIGINT       NOT NULL,
  `title`         VARCHAR(255) NOT NULL,
  `max_users`     INT          NOT NULL DEFAULT 10,
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT 'active/closed',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`, `deleted`),
  KEY `idx_owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作会话';

-- ------------------------------------------------------------
-- collab_member 协作成员
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `collab_member`;
CREATE TABLE `collab_member` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `collab_id`     BIGINT       NOT NULL,
  `user_id`       BIGINT       NOT NULL,
  `role`          VARCHAR(16)  NOT NULL DEFAULT 'editor' COMMENT 'owner/editor/viewer',
  `joined_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_active_at` DATETIME    DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collab_user` (`collab_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作成员';

-- ------------------------------------------------------------
-- plugin 插件
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `plugin`;
CREATE TABLE `plugin` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(64)  NOT NULL,
  `display_name`  VARCHAR(128) NOT NULL,
  `description`   VARCHAR(500)          DEFAULT NULL,
  `version`       VARCHAR(32)  NOT NULL DEFAULT '1.0.0',
  `author`        VARCHAR(64)           DEFAULT NULL,
  `category`      VARCHAR(32)           DEFAULT 'general',
  `scope`         VARCHAR(16)  NOT NULL DEFAULT 'system' COMMENT 'system/user',
  `owner_id`      BIGINT                DEFAULT NULL,
  `icon`          VARCHAR(255)          DEFAULT NULL,
  `entry`         VARCHAR(255) NOT NULL COMMENT 'plugin 入口 (类名/URL/JS path)',
  `plugin_type`   VARCHAR(16)  NOT NULL DEFAULT 'class' COMMENT 'class/url/js/wasm',
  `config`        MEDIUMTEXT            DEFAULT NULL,
  `enabled`       TINYINT      NOT NULL DEFAULT 1,
  `downloads`     INT          NOT NULL DEFAULT 0,
  `rating`        DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`, `deleted`),
  KEY `idx_scope_category` (`scope`, `category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件';

-- 初始 4 个示例插件
INSERT INTO `plugin` (`name`, `display_name`, `description`, `version`, `author`, `category`, `scope`, `entry`, `plugin_type`) VALUES
('weather-widget', '天气小组件', '在聊天窗口显示当前天气', '1.0.0', 'Mavis', 'ui', 'system',
 'com.minimax.agent.plugin.WeatherWidget', 'class'),
('markdown-export', 'Markdown 导出', '把会话导出为 Markdown 文件', '1.0.0', 'Mavis', 'export', 'system',
 'com.minimax.agent.plugin.MarkdownExport', 'class'),
('code-formatter', '代码格式化', '在 AI 回复中自动格式化代码块', '1.0.0', 'Mavis', 'enhance', 'system',
 'com.minimax.agent.plugin.CodeFormatter', 'class'),
('translation', '中英互译', '对话中遇到英文自动翻译', '1.0.0', 'Mavis', 'enhance', 'system',
 'com.minimax.agent.plugin.Translation', 'class');

-- ============================================================
-- MiniMax Platform - SUPER_ADMIN 角色
-- adminLiugl = 唯一超级管理员 (独立于普通 admin)
-- 独立密码、独立 JWT claim
-- ============================================================

USE `minimax_platform`;

-- 确保 SUPER_ADMIN 角色存在
INSERT INTO `sys_role` (`code`, `name`, `description`, `sort`, `enabled`)
VALUES ('SUPER_ADMIN', '超级管理员 (adminLiugl)', '拥有平台所有权限, 包括管理其他管理员', 0, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `enabled` = 1;

-- adminLiugl 用户 (独立于 admin, 密码 = Liugl@2026)
-- 密码哈希由 AdminDataInitializer 启动时 BCrypt 编码, 此处只占位
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `email`, `status`, `tenant_id`, `remark`)
VALUES ('adminLiugl', 'PLACEHOLDER', 'Liugl (Owner)', 'liugl951127@gmail.com', 1, 0, '平台所有者, 唯一超级管理员')
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`), `email` = VALUES(`email`), `status` = 1;

-- 绑定 SUPER_ADMIN 角色 (用 INSERT ... SELECT 关联)
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `sys_user` u, `sys_role` r
WHERE u.username = 'adminLiugl' AND r.code = 'SUPER_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_user_role` ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

-- ============================================================
-- V3.1: 多租户 (tenant_id 隔离)
-- adminLiugl 是平台所有者, 自带特殊跨租户权限
-- 其他用户通过 tenant_id 严格隔离
-- ============================================================

USE `minimax_platform`;

-- 1. 租户表
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `code`            VARCHAR(32)  NOT NULL                COMMENT '租户代码(短, 用于URL/标识)',
  `name`            VARCHAR(64)  NOT NULL                COMMENT '租户名称',
  `plan`            VARCHAR(16)  NOT NULL DEFAULT 'free' COMMENT '套餐: free/pro/enterprise',
  `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '0禁用 1正常',
  `max_users`       INT          NOT NULL DEFAULT 10,
  `max_models`      INT          NOT NULL DEFAULT 5,
  `qps_limit`       INT          NOT NULL DEFAULT 100    COMMENT 'QPS 上限',
  `monthly_quota`   BIGINT       NOT NULL DEFAULT 100000 COMMENT '月度调用配额',
  `used_quota`      BIGINT       NOT NULL DEFAULT 0,
  `expire_at`       DATETIME              DEFAULT NULL   COMMENT '过期时间(NULL=永久)',
  `contact_email`   VARCHAR(128)          DEFAULT NULL,
  `contact_phone`   VARCHAR(32)           DEFAULT NULL,
  `remark`          VARCHAR(255)          DEFAULT NULL,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户';

-- 2. sys_user 增加 tenant_id (已有, 改为 NOT NULL 0)
-- 已有字段: tenant_id BIGINT NOT NULL DEFAULT 0
-- 含义: 0 = 平台所有者 (adminLiugl 跨租户), 1+ = 普通租户

-- 3. 初始化默认租户
INSERT INTO `tenant` (`code`, `name`, `plan`, `max_users`, `max_models`, `qps_limit`, `monthly_quota`, `contact_email`, `remark`)
VALUES
  ('default', '默认租户', 'pro', 100, 20, 500, 1000000, 'admin@minimax.local', '平台默认租户, 所有无租户用户归此处'),
  ('demo', '演示租户', 'free', 10, 5, 100, 100000, 'demo@minimax.local', '演示用免费租户');

-- 4. 把现有非 adminLiugl 用户 (admin) 分配到 default 租户
UPDATE `sys_user` SET `tenant_id` = 1 WHERE `username` <> 'adminLiugl' AND `tenant_id` = 0;

-- 5. 新增 sys_tenant_role 租户管理员角色 (可选)
-- adminLiugl 保持 tenant_id=0 表示跨租户超级管理员

-- ============================================================
-- 18_v4_real_ai.sql
-- 真实 AI 对接: 补齐 6 个真实 provider (含视觉/语音/图像)
-- 关键: api_key = NULL 表示未配置, 走 mock 模式
--       api_key 有值表示真实 OpenAI 协议调用
-- ============================================================

-- 加视觉/语音/图像专用 provider
INSERT INTO `model_provider` (`code`,`name`,`base_url`,`api_key`,`protocol`,`enabled`,`sort`,`description`) VALUES
('siliconflow','SiliconFlow (硅基流动)','https://api.siliconflow.cn/v1',NULL,'openai',1,4,'国产高性价比推理 + 视觉/语音/图像/Embedding'),
('dashscope','阿里 DashScope','https://dashscope.aliyuncs.com/compatible-mode/v1',NULL,'openai',1,5,'阿里通义 (兼容 OpenAI 模式)'),
('deepseek','DeepSeek','https://api.deepseek.com/v1',NULL,'openai',1,6,'DeepSeek 国产高质模型');

-- SiliconFlow 视觉模型 (Qwen2-VL)
INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'Qwen/Qwen2-VL-72B-Instruct', 'Qwen2-VL 72B (硅基流动)', 32768, 4096, 0, 0, 1, 1, 1, 1, 10, '国产开源多模态领军' FROM model_provider p WHERE p.code='siliconflow';

-- SiliconFlow 视觉模型 (InternVL)
INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'internlm/internvl2-26b', 'InternVL2 26B (硅基流动)', 32768, 4096, 0, 0, 1, 1, 1, 1, 11, '商汤书生·多模态' FROM model_provider p WHERE p.code='siliconflow';

-- SiliconFlow 视觉模型 (GLM-4V)
INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'THUDM/glm-4v-plus', 'GLM-4V Plus (硅基流动)', 8192, 4096, 0, 0, 1, 1, 1, 1, 12, '智谱视觉增强' FROM model_provider p WHERE p.code='siliconflow';

-- SiliconFlow 文本 (Qwen2.5)
INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'Qwen/Qwen2.5-72B-Instruct', 'Qwen2.5 72B (硅基流动)', 32768, 8192, 0, 0, 0, 1, 1, 1, 13, '通义 2.5 72B' FROM model_provider p WHERE p.code='siliconflow';

-- SiliconFlow 文本 (DeepSeek-V3)
INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'deepseek-ai/DeepSeek-V3', 'DeepSeek V3 (硅基流动)', 32768, 8192, 0, 0, 0, 1, 1, 1, 14, 'DeepSeek-V3 MoE 671B' FROM model_provider p WHERE p.code='siliconflow';

-- DashScope 视觉 (Qwen-VL-Max)
INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'qwen-vl-max', 'Qwen-VL Max (DashScope)', 32000, 4096, 0, 0, 1, 1, 1, 1, 20, '通义千问视觉旗舰' FROM model_provider p WHERE p.code='dashscope';

-- DashScope 视觉 (Qwen-VL-Plus)
INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'qwen-vl-plus', 'Qwen-VL Plus (DashScope)', 32000, 4096, 0, 0, 1, 1, 1, 1, 21, '通义视觉增强' FROM model_provider p WHERE p.code='dashscope';

-- DashScope 文本 (Qwen-Max)
INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'qwen-max', 'Qwen Max (DashScope)', 32000, 8192, 0, 0, 0, 1, 1, 1, 22, '通义千问 Max 旗舰' FROM model_provider p WHERE p.code='dashscope';

-- DeepSeek
INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'deepseek-chat', 'DeepSeek V3 (官方)', 64000, 8192, 0, 0, 0, 1, 1, 1, 1, 'DeepSeek 官方旗舰' FROM model_provider p WHERE p.code='deepseek';

INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'deepseek-reasoner', 'DeepSeek R1 推理 (官方)', 64000, 8192, 0, 0, 0, 1, 1, 1, 1, 'DeepSeek R1 推理模型' FROM model_provider p WHERE p.code='deepseek';

-- 给 adminLiugl 一些测试用 quota
INSERT INTO `model_quota` (`user_id`,`model_id`,`quota_date`,`used_tokens`,`used_requests`,`limit_tokens`,`limit_requests`)
SELECT 1, m.id, CURDATE(), 0, 0, 1000000, 10000
FROM model_config m
WHERE NOT EXISTS (
  SELECT 1 FROM model_quota q WHERE q.user_id = 1 AND q.model_id = m.id AND q.quota_date = CURDATE()
);

-- 加 API 调用日志表 (用于多模型对决时打分)
DROP TABLE IF EXISTS `model_battle_log`;
CREATE TABLE `model_battle_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `battle_id` VARCHAR(64) NOT NULL COMMENT '对决 id',
  `user_id` BIGINT NOT NULL,
  `model_id` BIGINT NOT NULL,
  `model_code` VARCHAR(128) NOT NULL,
  `prompt` TEXT NOT NULL,
  `response` MEDIUMTEXT,
  `prompt_tokens` INT NOT NULL DEFAULT 0,
  `completion_tokens` INT NOT NULL DEFAULT 0,
  `latency_ms` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ok' COMMENT 'ok / error / timeout',
  `error_msg` VARCHAR(512) DEFAULT NULL,
  `score` INT NOT NULL DEFAULT 0 COMMENT '用户评分 1-5',
  `judge_model` VARCHAR(128) DEFAULT NULL COMMENT '用哪个模型做的裁判',
  `judge_reason` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_battle` (`battle_id`),
  KEY `idx_user_date` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多模型对决日志';

-- ============================================================
-- 19_v4_3_prompt.sql
-- Prompt 模板系统 (V4.3, minimax-prompt 8091)
--
-- 模块: backend/minimax-prompt/
-- 实体: PromptTemplate.java (@TableName("prompt_template"))
-- 字段 (14):
--   id           BIGINT       AUTO_INCREMENT  PRIMARY KEY
--   name         VARCHAR(128) NOT NULL                  -- 模板名
--   description  VARCHAR(512)                          -- 模板描述
--   category     VARCHAR(32)                           -- 翻译/代码/写作/分析/客服/其他
--   content      TEXT         NOT NULL                 -- 模板内容 (含 {{variable}})
--   variables    TEXT                                  -- 变量 JSON: [{"name","description","required"}]
--   creator_id   BIGINT                                 -- 创建者 user_id
--   creator_name VARCHAR(64)                            -- 创建者 username (冗余加速查询)
--   is_public    TINYINT(1)   DEFAULT 1                -- 公开/私有
--   use_count    INT          DEFAULT 0                 -- 使用次数
--   created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP
--   updated_at   DATETIME     ON UPDATE CURRENT_TIMESTAMP
--   deleted      INT          DEFAULT 0                 -- 逻辑删除 (MyBatis-Plus @TableLogic)
-- ============================================================

USE `minimax_platform`;

DROP TABLE IF EXISTS `prompt_template`;
CREATE TABLE `prompt_template` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(128) NOT NULL                            COMMENT '模板名称 (唯一 + 分类内)',
  `description`   VARCHAR(512)                         DEFAULT NULL COMMENT '模板描述',
  `category`      VARCHAR(32)                          DEFAULT '其他' COMMENT '分类: 翻译/代码/写作/分析/客服/其他',
  `content`       TEXT         NOT NULL                            COMMENT '模板内容, 含 {{variable}} 占位符',
  `variables`     TEXT                                  DEFAULT NULL COMMENT '变量列表 JSON: [{"name":"语言","description":"目标语言","required":true}]',
  `creator_id`    BIGINT                                DEFAULT NULL COMMENT '创建者 user_id',
  `creator_name`  VARCHAR(64)                          DEFAULT NULL COMMENT '创建者 username (冗余)',
  `is_public`     TINYINT(1)                           DEFAULT 1    COMMENT '是否公开 (1=公开, 0=私有)',
  `use_count`     INT                                  DEFAULT 0    COMMENT '使用次数',
  `created_at`    DATETIME                             DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME                             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       INT                                  DEFAULT 0    COMMENT '逻辑删除 (0=正常, 1=已删)',
  PRIMARY KEY (`id`),
  KEY `idx_category`     (`category`),
  KEY `idx_creator`      (`creator_id`),
  KEY `idx_public_count` (`is_public`, `use_count` DESC),
  KEY `idx_created`      (`created_at` DESC),
  KEY `idx_name_unique`  (`name`, `creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt 模板表 (V4.3)';

-- ============================================================
-- 初始化数据: 5 个内置系统模板 (PromptTemplateService.BUILTIN_TEMPLATES)
-- creator_id=1 = 系统 (admin)
-- ============================================================
INSERT INTO `prompt_template`
  (`name`, `description`, `category`, `content`, `variables`,
   `creator_id`, `creator_name`, `is_public`, `use_count`)
VALUES
('翻译助手',
 '中英日韩法德俄 7 国语言互译, 保留原文格式',
 '翻译',
 '你是一个专业翻译官, 请将以下文本翻译成 {{target_language}}, 保留原文格式、专有名词和语气:\n\n{{text}}',
 '[{"name":"target_language","description":"目标语言","required":true},{"name":"text","description":"原文","required":true}]',
 1, 'admin', 1, 0),

('代码审查',
 '资深工程师视角审查代码, 找出 bug / 性能 / 风格问题',
 '代码',
 '请以资深 {{language}} 工程师的视角, 审查以下代码:\n\n```{{language}}\n{{code}}\n```\n\n输出格式:\n1. 🐛 Bug (必现问题)\n2. ⚠️ 风险 (潜在问题)\n3. 🚀 性能 (可优化点)\n4. 🎨 风格 (可读性)\n5. ✅ 总结 (整体评分 1-10)',
 '[{"name":"language","description":"编程语言","required":true},{"name":"code","description":"代码","required":true}]',
 1, 'admin', 1, 0),

('会议纪要',
 '把会议录音转写或聊天记录整理成结构化纪要',
 '写作',
 '请把以下会议内容整理成结构化纪要:\n\n```\n{{transcript}}\n```\n\n输出格式:\n# 会议主题: <主题>\n# 时间地点: <推断>\n# 与会人员: <提取>\n## 决议事项\n- ...\n## 待办事项\n- [ ] 负责人 / 截止日期 / 任务\n## 讨论要点\n- ...',
 '[{"name":"transcript","description":"会议转写或聊天记录","required":true}]',
 1, 'admin', 1, 0),

('营销文案',
 '根据产品卖点生成小红书/抖音/朋友圈文案',
 '写作',
 '你是 {{platform}} 爆款文案写手, 擅长 {{tone}} 风格.\n请基于以下产品信息写 3 条文案:\n\n产品名: {{product_name}}\n核心卖点: {{selling_points}}\n目标人群: {{target_audience}}\n\n要求:\n- 标题 ≤ 20 字, 吸引点击\n- 正文 100-200 字\n- 末尾加 3-5 个 #标签',
 '[{"name":"platform","description":"平台 (小红书/抖音/朋友圈)","required":true},{"name":"tone","description":"语气 (搞笑/温暖/专业)","required":false},{"name":"product_name","description":"产品名","required":true},{"name":"selling_points","description":"卖点","required":true},{"name":"target_audience","description":"目标人群","required":false}]',
 1, 'admin', 1, 0),

('故障排查',
 '运维/SRE 视角, 根据现象逐步定位根因',
 '分析',
 '你是资深 SRE, 服务器/服务出现以下故障:\n\n现象: {{symptom}}\n环境: {{env}}\n最近变更: {{recent_changes}}\n\n请按以下步骤排查:\n1. 🔍 现象分析 (可能原因 TOP 3)\n2. 🧪 验证步骤 (具体命令)\n3. 🎯 根因推断\n4. 🛠 解决方案 (临时 + 永久)\n5. 📋 预防措施',
 '[{"name":"symptom","description":"故障现象","required":true},{"name":"env","description":"环境 (prod/staging)","required":false},{"name":"recent_changes","description":"最近变更","required":false}]',
 1, 'admin', 1, 0);

-- ============================================================
-- 20_v5_wechat_scan.sql
-- 微信扫码登录 (V5, 微信开放平台 / 公众号 网页授权)
--
-- 用法:
--   1. 管理员在 微信公众平台/开放平台 申请 网站应用, 获取 AppID + AppSecret
--   2. 配置授权回调域: api.your-domain.com
--   3. 把 AppID + AppSecret 写入 minimax.auth.wechat.app-id / app-secret (yml)
--   4. 用户扫码登录后, 通过 openid 唯一标识, 自动注册/绑定现有账号
--
-- 端点:
--   GET  /api/v1/auth/wechat/qrcode       - 生成二维码 (返回 ticket + qrcode_url + expires_in)
--   GET  /api/v1/auth/wechat/status        - 轮询扫码状态 (前端每 2s 调一次)
--   GET  /api/v1/auth/wechat/callback      - 微信回调 (用户扫码确认后微信重定向到此)
--   POST /api/v1/auth/wechat/mobile-login  - 移动端 (微信公众号) 静默登录
-- ============================================================

USE `minimax_platform`;

-- ============================================================
-- 1. 扩展 sys_user 表 (加微信字段)
-- ============================================================
ALTER TABLE `sys_user`
  ADD COLUMN `wechat_openid`   VARCHAR(64)          DEFAULT NULL COMMENT '微信 openid (公众号/小程序唯一)',
  ADD COLUMN `wechat_unionid`  VARCHAR(64)          DEFAULT NULL COMMENT '微信 unionid (开放平台跨应用唯一)',
  ADD COLUMN `wechat_nickname` VARCHAR(64)          DEFAULT NULL COMMENT '微信昵称 (冗余, 不随用户改名)',
  ADD COLUMN `wechat_avatar`   VARCHAR(512)         DEFAULT NULL COMMENT '微信头像 URL',
  ADD COLUMN `wechat_bound_at` DATETIME             DEFAULT NULL COMMENT '微信绑定时间',
  ADD KEY `idx_wechat_openid` (`wechat_openid`),
  ADD KEY `idx_wechat_unionid` (`wechat_unionid`);

-- ============================================================
-- 2. 微信扫码会话表 (二维码 ticket 与登录态映射)
-- ============================================================
DROP TABLE IF EXISTS `wechat_scan_session`;
CREATE TABLE `wechat_scan_session` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `ticket`          VARCHAR(64)  NOT NULL                            COMMENT '二维码 ticket (前端轮询用)',
  `scene_id`        VARCHAR(64)  NOT NULL                            COMMENT '微信 scene_id (UUID)',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'pending'          COMMENT 'pending/scanned/confirmed/expired/cancelled',
  `openid`          VARCHAR(64)           DEFAULT NULL               COMMENT '微信 openid (扫码后回填)',
  `unionid`         VARCHAR(64)           DEFAULT NULL               COMMENT '微信 unionid',
  `nickname`        VARCHAR(64)           DEFAULT NULL               COMMENT '微信昵称',
  `avatar`          VARCHAR(512)          DEFAULT NULL               COMMENT '微信头像',
  `user_id`         BIGINT                DEFAULT NULL               COMMENT '确认后绑定的用户 ID',
  `access_token`    VARCHAR(1024)         DEFAULT NULL               COMMENT '登录 access token (确认后回填)',
  `refresh_token`   VARCHAR(1024)         DEFAULT NULL               COMMENT '登录 refresh token',
  `client_ip`       VARCHAR(64)           DEFAULT NULL               COMMENT '生成二维码的客户端 IP',
  `user_agent`      VARCHAR(512)          DEFAULT NULL               COMMENT 'User-Agent',
  `expires_at`      DATETIME     NOT NULL                            COMMENT '过期时间 (默认 5 分钟)',
  `confirmed_at`    DATETIME              DEFAULT NULL               COMMENT '用户扫码确认时间',
  `created_at`      DATETIME              DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket` (`ticket`),
  KEY `idx_scene_id` (`scene_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信扫码登录会话 (V5)';

-- ============================================================
-- 3. 微信用户与平台账号绑定关系 (一个 openid 可绑定多平台账号)
-- ============================================================
DROP TABLE IF EXISTS `wechat_user_binding`;
CREATE TABLE `wechat_user_binding` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL                            COMMENT '平台 user_id',
  `openid`        VARCHAR(64)  NOT NULL                            COMMENT '微信 openid',
  `unionid`       VARCHAR(64)           DEFAULT NULL               COMMENT '微信 unionid',
  `app_type`      VARCHAR(16)  NOT NULL DEFAULT 'mp'               COMMENT 'mp=公众号, mini=小程序, open=开放平台, web=网页应用',
  `nickname`      VARCHAR(64)           DEFAULT NULL,
  `avatar`        VARCHAR(512)          DEFAULT NULL,
  `bound_at`      DATETIME              DEFAULT CURRENT_TIMESTAMP,
  `last_login_at` DATETIME              DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid_app` (`openid`, `app_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_unionid` (`unionid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信用户与平台账号绑定关系';

-- ============================================================
-- 4. 微信配置 (单条, id=1)
-- ============================================================
DROP TABLE IF EXISTS `wechat_config`;
CREATE TABLE `wechat_config` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `app_type`          VARCHAR(16)  NOT NULL DEFAULT 'mp'             COMMENT 'mp=公众号, mini=小程序, open=开放平台',
  `app_id`            VARCHAR(64)  NOT NULL,
  `app_secret`        VARCHAR(128) NOT NULL,
  `token`             VARCHAR(64)           DEFAULT NULL               COMMENT '消息校验 Token',
  `aes_key`           VARCHAR(64)           DEFAULT NULL               COMMENT '消息加解密 Key',
  `redirect_uri`      VARCHAR(512)          DEFAULT NULL               COMMENT '回调域名 (不带参数)',
  `scope`             VARCHAR(64)  NOT NULL DEFAULT 'snsapi_login'    COMMENT 'snsapi_login/snsapi_userinfo',
  `enabled`           TINYINT(1)   NOT NULL DEFAULT 1,
  `remark`            VARCHAR(255)          DEFAULT NULL,
  `created_at`        DATETIME              DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_type` (`app_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信应用配置';

-- 初始占位配置 (AppID/AppSecret 由管理员在管理后台填)
INSERT INTO `wechat_config` (`app_type`, `app_id`, `app_secret`, `redirect_uri`, `scope`, `enabled`, `remark`)
VALUES
  ('mp',    'wx0000000000000000', 'PLACEHOLDER_REPLACE_IN_ADMIN', 'https://api.your-domain.com/api/v1/auth/wechat/callback', 'snsapi_login', 0, '公众号 (网页扫码登录)'),
  ('mini',  'wx0000000000000001', 'PLACEHOLDER_REPLACE_IN_ADMIN', '',                                                    'snsapi_login', 0, '小程序 (静默登录)'),
  ('open',  'wx0000000000000002', 'PLACEHOLDER_REPLACE_IN_ADMIN', 'https://api.your-domain.com/api/v1/auth/wechat/callback', 'snsapi_userinfo', 0, '开放平台 (第三方网站)');

-- ============================================================
-- 5. 微信扫码登录日志
-- ============================================================
DROP TABLE IF EXISTS `wechat_scan_log`;
CREATE TABLE `wechat_scan_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT                DEFAULT NULL,
  `openid`       VARCHAR(64)           DEFAULT NULL,
  `action`       VARCHAR(32)  NOT NULL                            COMMENT 'qrcode/scan/confirm/cancel/expire/login_fail',
  `result`       VARCHAR(16)  NOT NULL DEFAULT 'ok'               COMMENT 'ok/error',
  `error_msg`    VARCHAR(512)          DEFAULT NULL,
  `client_ip`    VARCHAR(64)           DEFAULT NULL,
  `created_at`   DATETIME              DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信扫码日志';

-- =============================================================
-- V5.1: unionid 跨应用打通 + wechat_user_binding 唯一约束迁移
-- 同一微信开放平台下的公众号 / 小程序 / App 共享 unionid
-- 同一 user 可绑定多个 app 的 openid (互不冲突)
-- =============================================================

-- 1) wechat_user_binding 唯一约束迁移: openid → (app_type + openid)
-- (同一微信下不同应用的 openid 完全不同)
SET @drop_idx := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'wechat_user_binding' AND index_name = 'uk_openid') > 0,
  'ALTER TABLE wechat_user_binding DROP INDEX uk_openid',
  'SELECT 1');
PREPARE stmt FROM @drop_idx; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @add_idx := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'wechat_user_binding' AND index_name = 'uk_app_openid') = 0,
  'ALTER TABLE wechat_user_binding ADD UNIQUE KEY uk_app_openid (app_type, openid)',
  'SELECT 1');
PREPARE stmt FROM @add_idx; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 新增 unionid_relations 表
-- 跨应用 unionid 关联 (1 user_id 对应 1 unionid)
CREATE TABLE IF NOT EXISTS unionid_relations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '平台用户 ID',
    unionid VARCHAR(64) NOT NULL COMMENT '微信 unionid (跨应用唯一)',
    platform VARCHAR(32) DEFAULT 'wechat' COMMENT '平台: wechat/qq/alipay',
    first_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次出现时间',
    last_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    binding_count INT DEFAULT 1 COMMENT '关联应用数',
    UNIQUE KEY uk_user_unionid (user_id, unionid),
    UNIQUE KEY uk_unionid (unionid),
    INDEX idx_user_id (user_id),
    INDEX idx_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='unionid 跨应用关联';

-- 3) sys_user 加索引 (unionid 字段已有, 加索引加速匹配, IF NOT EXISTS 兼容)
DROP PROCEDURE IF EXISTS add_wechat_unionid_idx;
DELIMITER //
CREATE PROCEDURE add_wechat_unionid_idx()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_wechat_unionid') THEN
        ALTER TABLE sys_user ADD INDEX idx_wechat_unionid (wechat_unionid);
    END IF;
END //
DELIMITER ;
CALL add_wechat_unionid_idx();
DROP PROCEDURE add_wechat_unionid_idx;

-- 4) 预置示例 unionid (沙箱演示用)
INSERT IGNORE INTO unionid_relations (user_id, unionid, platform, binding_count) VALUES
(2, 'mock_union_admin', 'wechat', 1),
(6, 'mock_union_demo_user', 'wechat', 2);
-- =============================================================
-- V5.2: QQ/支付宝 跨平台 unionid 打通
-- 同一用户用 QQ/支付宝/微信 登录 → 共享平台账号
-- =============================================================

-- 1) sys_user 加 QQ/支付宝 字段 (兼容老库: 用 IF NOT EXISTS)
-- MariaDB 10.0.2+ 支持 ADD COLUMN IF NOT EXISTS
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS qq_openid VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS qq_unionid VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS qq_nickname VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS qq_avatar VARCHAR(512);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS qq_bound_at DATETIME;

ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS alipay_openid VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS alipay_user_id VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS alipay_nickname VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS alipay_avatar VARCHAR(512);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS alipay_bound_at DATETIME;

-- 加索引 (跨平台查询, 用存储过程判断是否存在)
DROP PROCEDURE IF EXISTS add_index_if_not_exists;
DELIMITER //
CREATE PROCEDURE add_index_if_not_exists()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_qq_openid') THEN
        ALTER TABLE sys_user ADD INDEX idx_qq_openid (qq_openid);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_qq_unionid') THEN
        ALTER TABLE sys_user ADD INDEX idx_qq_unionid (qq_unionid);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_alipay_openid') THEN
        ALTER TABLE sys_user ADD INDEX idx_alipay_openid (alipay_openid);
    END IF;
END //
DELIMITER ;
CALL add_index_if_not_exists();
DROP PROCEDURE add_index_if_not_exists;

-- 2) oauth_binding 表 (跨平台 binding, 替代 wechat_user_binding 模式, 支持任意平台)
CREATE TABLE IF NOT EXISTS oauth_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    platform VARCHAR(20) NOT NULL,        -- wechat/qq/alipay/weibo/github
    app_type VARCHAR(20) NOT NULL,        -- mp/mini/open/web/app/h5
    openid VARCHAR(128) NOT NULL,
    unionid VARCHAR(128),
    nickname VARCHAR(128),
    avatar VARCHAR(512),
    access_token VARCHAR(512),
    refresh_token VARCHAR(512),
    token_expires_at DATETIME,
    raw_data TEXT,                        -- 平台返回的原始 JSON
    bound_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME,
    UNIQUE KEY uk_platform_app_openid (platform, app_type, openid),
    INDEX idx_user_id (user_id),
    INDEX idx_unionid (unionid),
    INDEX idx_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth 跨平台 binding';

-- 3) unionid_relations 加 platform 字段索引 (支持按平台统计)
-- 已有 platform 字段, 加复合索引 (IF NOT EXISTS 兼容)
DROP PROCEDURE IF EXISTS add_unionid_idx;
DELIMITER //
CREATE PROCEDURE add_unionid_idx()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'unionid_relations' AND index_name = 'idx_platform_unionid') THEN
        ALTER TABLE unionid_relations ADD INDEX idx_platform_unionid (platform, unionid);
    END IF;
END //
DELIMITER ;
CALL add_unionid_idx();
DROP PROCEDURE add_unionid_idx;

-- 4) oauth_app_config 表 (各平台应用配置)
CREATE TABLE IF NOT EXISTS oauth_app_config (
    id INT PRIMARY KEY AUTO_INCREMENT,
    platform VARCHAR(20) NOT NULL,         -- wechat/qq/alipay/weibo/github
    app_type VARCHAR(20) NOT NULL,         -- mp/mini/open/web/app/h5
    app_id VARCHAR(128) NOT NULL,
    app_secret VARCHAR(256),
    public_key TEXT,                        -- 支付宝 RSA 公钥
    redirect_uri VARCHAR(512),
    scopes VARCHAR(256),                    -- 多 scopes 用逗号分隔
    enabled TINYINT(1) DEFAULT 1,
    extra_config TEXT,                      -- JSON 格式额外配置
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_platform_app_type (platform, app_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth 应用配置';

-- 5) 预置占位配置 (沙箱演示)
INSERT IGNORE INTO oauth_app_config (platform, app_type, app_id, app_secret, redirect_uri, enabled) VALUES
('wechat', 'mp',       'PLACEHOLDER_WECHAT_MP',   'PLACEHOLDER_WECHAT_MP_SECRET',   'https://api.your-domain.com/auth/oauth/wechat/callback', 0),
('wechat', 'mini',     'PLACEHOLDER_WECHAT_MINI', 'PLACEHOLDER_WECHAT_MINI_SECRET', '',                                                              0),
('wechat', 'open',     'PLACEHOLDER_WECHAT_OPEN', 'PLACEHOLDER_WECHAT_OPEN_SECRET', 'https://api.your-domain.com/auth/oauth/wechat/callback', 0),
('wechat', 'web',      'PLACEHOLDER_WECHAT_WEB',  'PLACEHOLDER_WECHAT_WEB_SECRET',  'https://api.your-domain.com/auth/oauth/wechat/callback', 0),
('wechat', 'h5',       'PLACEHOLDER_WECHAT_H5',   'PLACEHOLDER_WECHAT_H5_SECRET',   'https://api.your-domain.com/auth/oauth/wechat/h5-callback', 0),
('qq',     'web',      'PLACEHOLDER_QQ_WEB',      'PLACEHOLDER_QQ_WEB_SECRET',      'https://api.your-domain.com/auth/oauth/qq/callback',     0),
('qq',     'h5',       'PLACEHOLDER_QQ_H5',       'PLACEHOLDER_QQ_H5_SECRET',       'https://api.your-domain.com/auth/oauth/qq/callback',     0),
('qq',     'app',      'PLACEHOLDER_QQ_APP',      'PLACEHOLDER_QQ_APP_SECRET',      '',                                                              0),
('alipay', 'web',      'PLACEHOLDER_ALIPAY_WEB',  'PLACEHOLDER_ALIPAY_WEB_SECRET',  'https://api.your-domain.com/auth/oauth/alipay/callback', 0),
('alipay', 'h5',       'PLACEHOLDER_ALIPAY_H5',   'PLACEHOLDER_ALIPAY_H5_SECRET',   'https://api.your-domain.com/auth/oauth/alipay/callback', 0),
('alipay', 'app',      'PLACEHOLDER_ALIPAY_APP',  'PLACEHOLDER_ALIPAY_APP_SECRET',  '',                                                              0);
