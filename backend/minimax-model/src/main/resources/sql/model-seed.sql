-- V7.0: Model 模块种子数据 (model-seed.sql)
-- 独立文件避免 H2 MySQL mode 解析器 bug

-- V7.0: 用 MERGE 避免重启时重复插入
MERGE INTO model_provider (code, name, base_url, api_key, protocol, enabled, deleted, sort)
KEY(code) VALUES ('deepseek', 'DeepSeek', 'https://api.deepseek.com', '${DEEPSEEK_API_KEY}', 'openai', 1, 0, 10);

MERGE INTO model_provider (code, name, base_url, api_key, protocol, enabled, deleted, sort)
KEY(code) VALUES ('minimax', 'MiniMax', 'https://api.minimax.chat', '${MINIMAX_API_KEY}', 'openai', 1, 0, 20);

-- V7.0 Flow④: 训练模型分类 provider (protocol=trained 供 /local/providers 接口展示)
MERGE INTO model_provider (code, name, base_url, api_key, protocol, enabled, deleted, sort)
KEY(code) VALUES ('trained', '训练模型', 'local://trained', '', 'trained', 1, 0, 99);

MERGE INTO model_config (provider_id, model_code, display_name, max_context, max_output, supports_stream, enabled, deleted, sort)
KEY(provider_id, model_code) VALUES (1, 'deepseek-chat', 'DeepSeek Chat', 16384, 4096, 1, 1, 0, 10);

MERGE INTO model_config (provider_id, model_code, display_name, max_context, max_output, supports_stream, enabled, deleted, sort)
KEY(provider_id, model_code) VALUES (2, 'MiniMax-Text-01', 'MiniMax Text', 8192, 2048, 1, 1, 0, 20);

MERGE INTO model_config (provider_id, model_code, display_name, max_context, max_output, supports_stream, enabled, deleted, sort)
KEY(provider_id, model_code) VALUES (1, 'deepseek-coder', 'DeepSeek Coder', 16384, 4096, 1, 1, 0, 30);
