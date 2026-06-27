-- H2 兼容 schema for model
CREATE TABLE IF NOT EXISTS model_provider (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  base_url VARCHAR(255) NOT NULL,
  api_key VARCHAR(255),
  protocol VARCHAR(16) NOT NULL DEFAULT 'openai',
  enabled INT NOT NULL DEFAULT 1,
  sort INT NOT NULL DEFAULT 0,
  description VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS model_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  provider_id BIGINT NOT NULL,
  model_code VARCHAR(64) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  max_context INT NOT NULL DEFAULT 8192,
  max_output INT NOT NULL DEFAULT 4096,
  input_price DECIMAL(12,6) DEFAULT 0,
  output_price DECIMAL(12,6) DEFAULT 0,
  supports_vision INT NOT NULL DEFAULT 0,
  supports_tools INT NOT NULL DEFAULT 0,
  supports_stream INT NOT NULL DEFAULT 1,
  enabled INT NOT NULL DEFAULT 1,
  sort INT NOT NULL DEFAULT 0,
  description VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS model_quota (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  model_id BIGINT NOT NULL,
  quota_date DATE NOT NULL,
  used_tokens BIGINT NOT NULL DEFAULT 0,
  used_requests INT NOT NULL DEFAULT 0,
  limit_tokens BIGINT NOT NULL DEFAULT 100000,
  limit_requests INT NOT NULL DEFAULT 100,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 训练任务表 (Day 23)
CREATE TABLE IF NOT EXISTS training_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  model_name VARCHAR(64) NOT NULL,
  corpus_path VARCHAR(512) NOT NULL,
  n_layer INT DEFAULT 12,
  n_head INT DEFAULT 12,
  n_embd INT DEFAULT 768,
  block_size INT DEFAULT 128,
  max_iters INT DEFAULT 100,
  batch_size INT DEFAULT 32,
  learning_rate DOUBLE DEFAULT 0.0003,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  progress INT DEFAULT 0,
  current_loss DOUBLE,
  current_iter INT DEFAULT 0,
  error_message VARCHAR(512),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP
);

-- 初始数据
INSERT INTO model_provider (id, code, name, base_url, protocol, enabled, sort, description) VALUES
(1, 'openai',  'OpenAI',  'https://api.openai.com/v1', 'openai', 1, 1, 'OpenAI 官方'),
(2, 'minimax', 'Minimax-M3', 'https://api.minimax.chat/v1', 'openai', 1, 2, 'MiniMax 内部'),
(3, 'ollama',  'Ollama',  'http://localhost:11434/v1', 'openai', 1, 3, '本地 Ollama');

INSERT INTO model_config (provider_id, model_code, display_name, max_context, max_output, supports_vision, supports_tools, supports_stream, enabled, sort, description) VALUES
(1, 'gpt-4o-mini', 'GPT-4o mini', 128000, 16384, 1, 1, 1, 1, 1, '便宜快速'),
(1, 'gpt-4o',      'GPT-4o',      128000, 16384, 1, 1, 1, 1, 2, '旗舰多模态'),
(2, 'MiniMax-Text-01', 'MiniMax-M3 Text', 1000000, 8192, 0, 1, 1, 1, 1, 'MiniMax 文本'),
(2, 'MiniMax-VL-01',  'MiniMax-M3 Vision', 1000000, 8192, 1, 1, 1, 1, 2, 'MiniMax 多模态'),
(3, 'llama3:8b',  'Llama 3 8B',  8192, 4096, 0, 0, 1, 1, 1, '本地'),
(3, 'qwen2.5:7b', 'Qwen 2.5 7B', 32768, 8192, 0, 0, 1, 1, 2, '本地中文');
