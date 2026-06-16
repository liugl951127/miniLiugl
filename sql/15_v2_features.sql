-- ============================================================
-- MiniMax Platform - V2: Agent / 知识图谱 / 协作 / 插件
-- ============================================================

USE `minimax`;

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
