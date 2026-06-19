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
