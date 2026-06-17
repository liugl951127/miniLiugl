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
