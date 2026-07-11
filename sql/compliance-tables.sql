-- ============================================================
-- V2.6 合规 + 多模态表 (compliance + multimodal)
-- ============================================================
-- 用法: mysql -h$MYSQL_HOST -uroot -p$MYSQL_PASS minimax_platform < sql/compliance-tables.sql

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================================
-- 1. 审计日志表 (audit_log)
-- 记录所有敏感操作: 登录 / 资料访问 / 数据导出 / AI 调用
-- ============================================================
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪 ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作人',
    `username` VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    `user_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端 IP',
    `user_agent` VARCHAR(512) DEFAULT NULL COMMENT 'UA',
    `action` VARCHAR(64) NOT NULL COMMENT '操作类型: LOGIN / LOGOUT / READ_USER / EXPORT_DATA / AI_GENERATE / FILE_UPLOAD / FILE_DOWNLOAD / CONFIG_CHANGE',
    `resource_type` VARCHAR(64) DEFAULT NULL COMMENT '资源类型: user/order/file/ai_tool/...',
    `resource_id` VARCHAR(128) DEFAULT NULL COMMENT '资源 ID',
    `method` VARCHAR(8) DEFAULT NULL COMMENT 'HTTP 方法',
    `path` VARCHAR(512) DEFAULT NULL COMMENT '请求路径',
    `request_body` TEXT COMMENT '请求体 (脱敏后)',
    `response_status` INT DEFAULT NULL COMMENT '响应状态码',
    `result` VARCHAR(32) DEFAULT 'SUCCESS' COMMENT '结果: SUCCESS / FAILURE / DENIED',
    `error_msg` VARCHAR(1024) DEFAULT NULL,
    `duration_ms` INT DEFAULT NULL COMMENT '耗时 ms',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_action` (`action`),
    KEY `idx_resource` (`resource_type`, `resource_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合规审计日志 (网络安全法 / GDPR 要求保留 6 个月以上)';

-- ============================================================
-- 2. 内容审核记录 (moderation_record)
-- 图片 / 文本 / 语音内容审核结果
-- ============================================================
DROP TABLE IF EXISTS `moderation_record`;
CREATE TABLE `moderation_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `trace_id` VARCHAR(64) DEFAULT NULL,
    `user_id` BIGINT DEFAULT NULL,
    `username` VARCHAR(64) DEFAULT NULL,
    `content_type` VARCHAR(32) NOT NULL COMMENT '类型: TEXT / IMAGE / VOICE / VIDEO / FILE',
    `content_hash` VARCHAR(128) NOT NULL COMMENT '内容 SHA-256 (用于去重)',
    `content_size` BIGINT DEFAULT NULL COMMENT '字节数',
    `content_url` VARCHAR(1024) DEFAULT NULL COMMENT '内容地址 (OSS / 本地路径)',
    `moderation_status` VARCHAR(32) NOT NULL COMMENT 'PASS / REJECT / REVIEW',
    `risk_level` VARCHAR(16) DEFAULT 'LOW' COMMENT '风险等级: LOW / MIDDLE / HIGH',
    `risk_labels` VARCHAR(1024) DEFAULT NULL COMMENT '风险标签: 政治,色情,暴力,广告,辱骂,... (逗号分隔)',
    `risk_score` DECIMAL(5,4) DEFAULT 0 COMMENT '风险分 (0-1)',
    `moderator` VARCHAR(64) DEFAULT 'auto' COMMENT '审核器: auto / manual / ai',
    `rejection_reason` VARCHAR(512) DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_content_hash` (`content_hash`),
    KEY `idx_status` (`moderation_status`),
    KEY `idx_risk` (`risk_level`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容审核记录 (网信办 / 互联网信息服务深度合成管理规定)';

-- ============================================================
-- 3. 用户协议 / 隐私政策同意记录 (consent_record)
-- 必须有: 同意时间 / 协议版本 / IP / UA
-- ============================================================
DROP TABLE IF EXISTS `consent_record`;
CREATE TABLE `consent_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT DEFAULT NULL,
    `username` VARCHAR(64) DEFAULT NULL,
    `consent_type` VARCHAR(64) NOT NULL COMMENT 'USER_AGREEMENT / PRIVACY_POLICY / CHILD_PROTECTION / AI_GENERATED_CONTENT / DATA_SHARING',
    `consent_version` VARCHAR(32) NOT NULL COMMENT '协议版本号 (例 v3.2.0)',
    `consent_action` VARCHAR(16) NOT NULL COMMENT 'AGREE / DISAGREE / WITHDRAW',
    `user_ip` VARCHAR(64) DEFAULT NULL,
    `user_agent` VARCHAR(512) DEFAULT NULL,
    `extra` TEXT COMMENT '附加信息 JSON',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_consent_type` (`consent_type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户协议同意记录 (个人信息保护法)';

-- ============================================================
-- 4. 多模态文件元数据 (multimedia_file)
-- 统一管理: 图片 / 视频 / 语音 / 文档
-- ============================================================
DROP TABLE IF EXISTS `multimedia_file`;
CREATE TABLE `multimedia_file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `file_id` VARCHAR(64) NOT NULL COMMENT '文件唯一 ID (UUID)',
    `user_id` BIGINT DEFAULT NULL,
    `username` VARCHAR(64) DEFAULT NULL,
    `file_name` VARCHAR(512) NOT NULL,
    `original_name` VARCHAR(512) DEFAULT NULL,
    `file_type` VARCHAR(32) NOT NULL COMMENT 'IMAGE / VIDEO / VOICE / DOCUMENT / OTHER',
    `mime_type` VARCHAR(128) DEFAULT NULL,
    `file_size` BIGINT NOT NULL,
    `file_hash` VARCHAR(128) NOT NULL COMMENT 'SHA-256 (去重 + 完整性)',
    `storage_path` VARCHAR(1024) NOT NULL COMMENT '本地路径或 OSS key',
    `storage_type` VARCHAR(16) DEFAULT 'LOCAL' COMMENT 'LOCAL / OSS / S3',
    `encrypted` TINYINT DEFAULT 0 COMMENT '是否加密存储',
    `duration_ms` BIGINT DEFAULT NULL COMMENT '音视频时长',
    `width` INT DEFAULT NULL COMMENT '图片/视频 宽',
    `height` INT DEFAULT NULL COMMENT '图片/视频 高',
    `bitrate` INT DEFAULT NULL COMMENT '音视频码率',
    `sample_rate` INT DEFAULT NULL COMMENT '音频采样率',
    `channels` INT DEFAULT NULL COMMENT '音频通道数',
    `codec` VARCHAR(64) DEFAULT NULL,
    `exif` TEXT COMMENT 'EXIF (图片)',
    `moderation_status` VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING / PASS / REJECT',
    `moderation_id` BIGINT DEFAULT NULL COMMENT '关联审核记录',
    `watermarked` TINYINT DEFAULT 0 COMMENT '是否已加水印',
    `is_public` TINYINT DEFAULT 0 COMMENT '是否公开',
    `access_count` INT DEFAULT 0,
    `expire_at` DATETIME DEFAULT NULL COMMENT '过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_id` (`file_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_file_type` (`file_type`),
    KEY `idx_file_hash` (`file_hash`),
    KEY `idx_moderation` (`moderation_status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多媒体文件元数据';

-- ============================================================
-- 5. AI 生成内容水印 (ai_generation_log)
-- 互联网信息服务深度合成管理规定: AI 生成内容必须可识别
-- ============================================================
DROP TABLE IF EXISTS `ai_generation_log`;
CREATE TABLE `ai_generation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `generation_id` VARCHAR(64) NOT NULL COMMENT '生成内容唯一 ID',
    `user_id` BIGINT DEFAULT NULL,
    `username` VARCHAR(64) DEFAULT NULL,
    `user_ip` VARCHAR(64) DEFAULT NULL,
    `modality` VARCHAR(32) NOT NULL COMMENT 'TEXT / IMAGE / VOICE / VIDEO',
    `model_name` VARCHAR(64) DEFAULT 'minimax-self-v1' COMMENT '模型名',
    `model_version` VARCHAR(32) DEFAULT '1.0.0',
    `prompt` TEXT COMMENT '输入 prompt (脱敏后)',
    `negative_prompt` TEXT,
    `parameters` TEXT COMMENT '生成参数 JSON',
    `output_url` VARCHAR(1024) DEFAULT NULL COMMENT '输出内容',
    `output_size` BIGINT DEFAULT NULL,
    `output_hash` VARCHAR(128) DEFAULT NULL COMMENT '输出 SHA-256',
    `watermarked` TINYINT DEFAULT 1 COMMENT '是否已添加 AI 水印',
    `watermark_text` VARCHAR(256) DEFAULT '本内容由 AI 生成' COMMENT '水印文字',
    `duration_ms` INT DEFAULT NULL COMMENT '生成耗时',
    `status` VARCHAR(32) DEFAULT 'SUCCESS' COMMENT 'SUCCESS / FAILURE / DENIED',
    `error_msg` VARCHAR(1024) DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_generation_id` (`generation_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_modality` (`modality`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 生成内容日志 (含水印标识)';

-- ============================================================
-- 6. 数据保留 / 删除策略执行记录 (data_retention_log)
-- GDPR / 个保法: 必须能删除用户数据
-- ============================================================
DROP TABLE IF EXISTS `data_retention_log`;
CREATE TABLE `data_retention_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT DEFAULT NULL,
    `username` VARCHAR(64) DEFAULT NULL,
    `action_type` VARCHAR(32) NOT NULL COMMENT 'ANONYMIZE / DELETE / EXPORT / RESTORE',
    `target_tables` TEXT COMMENT '影响表 (逗号分隔)',
    `affected_rows` INT DEFAULT 0,
    `operator_id` BIGINT DEFAULT NULL,
    `operator_name` VARCHAR(64) DEFAULT NULL,
    `reason` VARCHAR(512) DEFAULT NULL,
    `status` VARCHAR(32) DEFAULT 'SUCCESS' COMMENT 'SUCCESS / FAILURE / PARTIAL',
    `error_msg` VARCHAR(1024) DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_action_type` (`action_type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据保留/删除策略执行日志';

-- ============================================================
-- 初始化: 默认敏感词 (内容审核)
-- ============================================================
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `word` VARCHAR(64) NOT NULL,
    `category` VARCHAR(32) NOT NULL COMMENT 'POLITICS / PORN / VIOLENCE / ABUSE / AD / GAMBLING / DRUG',
    `level` VARCHAR(16) DEFAULT 'HIGH' COMMENT 'HIGH / MIDDLE / LOW',
    `action` VARCHAR(16) DEFAULT 'REJECT' COMMENT 'REJECT / REPLACE / REVIEW',
    `enabled` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`),
    KEY `idx_category` (`category`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词库';

-- 默认敏感词 (示例, 实际生产应该用更大的词库)
INSERT INTO `sensitive_word` (`word`, `category`, `level`, `action`) VALUES
('法轮功', 'POLITICS', 'HIGH', 'REJECT'),
('反动', 'POLITICS', 'HIGH', 'REJECT'),
('色情', 'PORN', 'HIGH', 'REJECT'),
('裸聊', 'PORN', 'HIGH', 'REJECT'),
('一夜情', 'PORN', 'MIDDLE', 'REPLACE'),
('暴力', 'VIOLENCE', 'MIDDLE', 'REVIEW'),
('杀人', 'VIOLENCE', 'HIGH', 'REJECT'),
('赌博', 'GAMBLING', 'HIGH', 'REJECT'),
('毒品', 'DRUG', 'HIGH', 'REJECT'),
('冰毒', 'DRUG', 'HIGH', 'REJECT'),
('海洛因', 'DRUG', 'HIGH', 'REJECT'),
('诈骗', 'ABUSE', 'HIGH', 'REJECT'),
('兼职刷单', 'AD', 'MIDDLE', 'REVIEW'),
('微商', 'AD', 'LOW', 'PASS');

-- ============================================================
-- 初始化: 默认 AI 模型版本
-- ============================================================
INSERT INTO `ai_generation_log` (generation_id, user_id, modality, model_name, status)
VALUES ('seed-initial', 0, 'TEXT', 'minimax-self-v1', 'SUCCESS')
ON DUPLICATE KEY UPDATE generation_id = generation_id;

-- ============================================================
-- 完成
-- ============================================================
SELECT 'V2.6 合规 + 多模态表初始化完成' AS status;
SELECT COUNT(*) AS sensitive_word_count FROM sensitive_word;
