-- ============================================================
-- V5.18: Anthropic Claude + Google Gemini Provider + 模型
-- ============================================================

-- 1. 插入 2 个新 provider
INSERT INTO `model_provider` (`code`,`name`,`base_url`,`api_key`,`protocol`,`enabled`,`sort`,`description`) VALUES
('anthropic','Anthropic Claude','https://api.anthropic.com',NULL,'anthropic',1,4,'Claude 3.5 Sonnet / Opus / Haiku (Anthropic Messages API)'),
('gemini','Google Gemini','https://generativelanguage.googleapis.com',NULL,'gemini',1,5,'Gemini 1.5 Pro / Flash (Google Generative Language API)');

-- 2. Anthropic 模型
INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'claude-3-5-sonnet-20241022', 'Claude 3.5 Sonnet', 200000, 8192, 0.003, 0.015, 1, 1, 1, 1, 1, '最新 Sonnet, 强逻辑' FROM model_provider p WHERE p.code='anthropic';

INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'claude-3-opus-20240229', 'Claude 3 Opus', 200000, 4096, 0.015, 0.075, 1, 1, 1, 1, 2, 'Opus 强推理' FROM model_provider p WHERE p.code='anthropic';

INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'claude-3-haiku-20240307', 'Claude 3 Haiku', 200000, 4096, 0.00025, 0.00125, 0, 1, 1, 1, 3, '快速便宜' FROM model_provider p WHERE p.code='anthropic';

-- 3. Gemini 模型
INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'gemini-1.5-pro', 'Gemini 1.5 Pro', 2000000, 8192, 0.0035, 0.0105, 1, 1, 1, 1, 1, 'Pro, 2M context' FROM model_provider p WHERE p.code='gemini';

INSERT INTO `model_config` (`provider_id`,`model_code`,`display_name`,`max_context`,`max_output`,`input_price`,`output_price`,`supports_vision`,`supports_tools`,`supports_stream`,`enabled`,`sort`,`description`)
SELECT p.id, 'gemini-1.5-flash', 'Gemini 1.5 Flash', 1000000, 8192, 0.000075, 0.0003, 1, 0, 1, 1, 2, 'Flash, 快速便宜' FROM model_provider p WHERE p.code='gemini';
