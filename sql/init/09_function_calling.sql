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
