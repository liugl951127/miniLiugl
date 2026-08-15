CREATE TABLE IF NOT EXISTS moderation_record (
  id BIGINT NOT NULL,
  trace_id VARCHAR(64),
  user_id BIGINT NOT NULL,
  username VARCHAR(128),
  content_type VARCHAR(32),
  content_hash TEXT,
  content_size BIGINT,
  content_url VARCHAR(512),
  moderation_status VARCHAR(32),
  risk_level VARCHAR(32),
  risk_labels VARCHAR(500),
  risk_score DECIMAL(20,6),
  moderator VARCHAR(32),
  rejection_reason INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS multimedia_file (
  id BIGINT NOT NULL,
  file_id INT,
  user_id BIGINT NOT NULL,
  username VARCHAR(128),
  file_name VARCHAR(128),
  original_name VARCHAR(128),
  file_type VARCHAR(32),
  mime_type VARCHAR(32),
  file_size BIGINT,
  file_hash INT,
  storage_path VARCHAR(255),
  storage_type VARCHAR(32),
  encrypted INT,
  duration_ms BIGINT,
  width INT,
  height INT,
  bitrate INT,
  sample_rate INT,
  channels INT,
  codec VARCHAR(64),
  exif INT,
  moderation_status VARCHAR(32),
  moderation_id BIGINT,
  watermarked INT,
  is_public INT,
  access_count INT DEFAULT 0,
  expire_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pipeline_log (
  id BIGINT NOT NULL,
  session_id INT NOT NULL,
  user_id BIGINT NOT NULL,
  client_ip VARCHAR(64),
  input_text TEXT,
  input_modality TEXT,
  intent INT,
  output_text TEXT,
  output_tokens INT,
  compute_device INT,
  compute_mode VARCHAR(32),
  total_cost_ms BIGINT,
  stage_costs VARCHAR(32),
  risk_level VARCHAR(32),
  needs_review BOOLEAN,
  rag_hits INT,
  tool_calls INT,
  error_message TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS push_message (
  id BIGINT NOT NULL,
  message_id TEXT,
  title VARCHAR(255),
  body TEXT,
  icon VARCHAR(512),
  click_action INT,
  data VARCHAR(255),
  target_type VARCHAR(32),
  target_value INT,
  status VARCHAR(32),
  success_count INT DEFAULT 0,
  failure_count INT DEFAULT 0,
  error INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS push_subscription (
  id BIGINT NOT NULL,
  subscription_id VARCHAR(64),
  user_id BIGINT NOT NULL,
  platform INT,
  endpoint VARCHAR(255),
  p256dh_key VARCHAR(64),
  auth_key VARCHAR(64),
  user_agent VARCHAR(512),
  status VARCHAR(32),
  last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sensitive_word (
  id BIGINT NOT NULL,
  word INT,
  category VARCHAR(32),
  level INT,
  action INT,
  enabled INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS training_checkpoint (
  id BIGINT NOT NULL,
  task_id INT,
  name VARCHAR(128),
  epoch INT,
  step INT,
  file_path VARCHAR(255),
  size_bytes BIGINT,
  sha256 INT,
  val_loss DOUBLE,
  accuracy DOUBLE,
  tags VARCHAR(500),
  metadata TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  checkpoint_id INT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS training_job (
  id BIGINT NOT NULL,
  task_id INT,
  name VARCHAR(128),
  model VARCHAR(32),
  status VARCHAR(32),
  total_epochs INT DEFAULT 0,
  current_epoch INT,
  current_step INT,
  start_time_ms BIGINT,
  end_time_ms BIGINT,
  config TEXT,
  error INT,
  owner_id BIGINT NOT NULL,
  tags VARCHAR(500),
  last_loss DOUBLE,
  last_val_loss DOUBLE,
  last_accuracy DOUBLE,
  total_steps INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS training_metric (
  id BIGINT NOT NULL,
  task_id INT,
  epoch INT,
  step INT,
  loss DOUBLE,
  val_loss DOUBLE,
  accuracy DOUBLE,
  learning_rate DOUBLE,
  elapsed_ms BIGINT,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);


-- Seed data
INSERT INTO `ai_chat_session` (id, session_id, user_id, username, title, model, status, intent, confidence, created_at) VALUES
(1, 'sess-ai-001', 2, 'liugl', '通用对话',  'gpt-4o-mini', 1, NULL, NULL, '2026-08-10 09:00:00'),
(2, 'sess-ai-002', 2, 'liugl', '代码助手',  'deepseek-chat', 1, 'code', 0.95, '2026-08-11 14:00:00');
INSERT INTO `ai_chat_message` (id, session_id, role, content, created_at) VALUES
(1, 'sess-ai-001', 'user',     '什么是 Transformer 架构？', '2026-08-10 09:01:00'),
(2, 'sess-ai-001', 'assistant', 'Transformer 是一种基于自注意力机制的深度学习架构...', '2026-08-10 09:01:30'),
(3, 'sess-ai-002', 'user',     '写一个快速排序', '2026-08-11 14:01:00'),
(4, 'sess-ai-002', 'assistant', 'def quicksort(arr):\n    if len(arr) <= 1: return arr\n    pivot = arr[len(arr)//2]\n    return quicksort([x for x in arr if x < pivot])...', '2026-08-11 14:01:30');
INSERT INTO `ai_tool` (id, code, name, category, description, enabled, builtin, impl_type, impl_value, input_schema, created_at) VALUES
(1, 'web_search', '网络搜索',  'search',     '实时搜索互联网',        1, 1, 'http',     'https://api.search.minimax.io/search', '{"query":"string"}', '2026-01-01 00:00:00'),
(2, 'calculator', '科学计算',  'tool',       '数学计算',              1, 1, 'class',    'com.minimax.ai.tool.Calculator',       '{"expr":"string"}',   '2026-01-01 00:00:00'),
(3, 'rag',        '知识检索',  'retrieval',  'RAG 知识库检索',        1, 1, 'class',    'com.minimax.ai.tool.RagTool',         '{"query":"string"}',  '2026-01-01 00:00:00');
INSERT INTO `billing_record` (id, record_id, user_id, record_type, amount_cents, currency, status, description, created_at) VALUES
(1, 'bill-001', 2, 'CHAT', -50, 'CNY', 'COMPLETED', 'GPT-4o mini 聊天消费', '2026-08-01 00:00:00'),
(2, 'bill-002', 2, 'API_CALL', -30, 'CNY', 'COMPLETED', 'API 调用消费', '2026-08-02 00:00:00'),
(3, 'bill-003', 1, 'SUBSCRIPTION', 9900, 'CNY', 'COMPLETED', 'Pro 月费订阅', '2026-08-01 00:00:00');
INSERT INTO `audit_log` (id, trace_id, user_id, username, action, resource_type, resource_id, method, path, response_status, created_at) VALUES
(1, 'trace-001', 2, 'liugl', 'LOGIN',     'Auth',    NULL,       'POST', '/api/v1/auth/login',     200, '2026-08-10 09:00:00'),
(2, 'trace-002', 2, 'liugl', 'CHAT_SEND', 'Chat',    NULL,       'POST', '/api/v1/chat/stream',    200, '2026-08-10 09:01:00'),
(3, 'trace-003', 1, 'admin', 'MODEL_CREATE', 'Model', '1',       'POST', '/api/v1/admin/models',  201, '2026-08-10 10:00:00');
INSERT INTO `training_metric` (id, task_id, iter, loss, accuracy, progress, lr, gpu_util, vram_gb, created_at) VALUES
(1, 1, 100,  2.453, 0.123, 10, '0.05', 80, 6.5, '2026-07-01 01:00:00'),
(2, 1, 500,  1.234, 0.456, 50, '0.05', 85, 7.0, '2026-07-01 05:00:00'),
(3, 1, 1000, 0.567, 0.789, 100, '0.005', 85, 7.0, '2026-07-02 00:00:00'),
(4, 2, 100,  1.890, 0.345, 5, '0.03', 78, 6.2, '2026-08-01 01:00:00'),
(5, 2, 900,  0.890, 0.678, 45, '0.03', 82, 6.8, '2026-08-10 00:00:00');
INSERT INTO `sensitive_word` (id, word, category, level, action, enabled, created_at) VALUES
(1, '作弊',     '违规', 'HIGH', 'BLOCK',  1, '2026-01-01 00:00:00'),
(2, '作弊软件', '违规', 'HIGH', 'BLOCK',  1, '2026-01-01 00:00:00'),
(3, '政治敏感', '合规', 'MEDIUM', 'REVIEW', 1, '2026-01-01 00:00:00'),
(4, '暴力内容', '合规', 'MEDIUM', 'REVIEW', 1, '2026-01-01 00:00:00'),
(5, '色情',     '合规', 'HIGH', 'BLOCK',  1, '2026-01-01 00:00:00');