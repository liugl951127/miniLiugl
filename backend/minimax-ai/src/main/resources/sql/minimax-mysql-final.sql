CREATE TABLE IF NOT EXISTS training_metric (
  id BIGINT NOT NULL,
  task_id BIGINT,
  iter INT,
  loss DOUBLE,
  accuracy DOUBLE,
  progress INT,
  lr VARCHAR(32),
  gpu_util INT,
  vram_gb DOUBLE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS agent_group (
  id BIGINT NOT NULL,
  group_id INT,
  name VARCHAR(128),
  description VARCHAR(64),
  strategy DECIMAL(20,6),
  members_json INT,
  status VARCHAR(32),
  owner_id BIGINT NOT NULL,
  tags VARCHAR(500),
  last_run_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  run_count INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ai_chat_message (
  id BIGINT NOT NULL,
  session_id INT NOT NULL,
  role INT,
  content TEXT,
  tool_code VARCHAR(64),
  tool_input TEXT,
  tool_output TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ai_chat_session (
  id BIGINT NOT NULL,
  session_id INT NOT NULL,
  user_id BIGINT NOT NULL,
  username VARCHAR(128),
  title VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status INT DEFAULT 0,
  intent INT,
  confidence DOUBLE,
  alternatives INT,
  model VARCHAR(32),
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ai_generation_log (
  id BIGINT NOT NULL,
  generation_id DOUBLE,
  user_id BIGINT NOT NULL,
  username VARCHAR(128),
  user_ip VARCHAR(64),
  modality INT,
  model_name VARCHAR(128),
  model_version VARCHAR(32),
  prompt TEXT,
  negative_prompt TEXT,
  parameters INT,
  output_url VARCHAR(512),
  output_size BIGINT,
  output_hash TEXT,
  watermarked INT,
  watermark_text TEXT,
  duration_ms INT,
  status VARCHAR(32),
  error_msg INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ai_intent_keyword (
  id BIGINT NOT NULL,
  intent INT,
  keyword VARCHAR(64),
  weight INT,
  is_regex INT,
  enabled INT DEFAULT 0,
  language INT,
  remark VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ai_tool (
  id BIGINT NOT NULL,
  code VARCHAR(64),
  name VARCHAR(128),
  category VARCHAR(32),
  description VARCHAR(64),
  icon VARCHAR(512),
  enabled INT DEFAULT 0,
  builtin INT,
  input_schema TEXT,
  output_schema TEXT,
  default_config TEXT,
  impl_type VARCHAR(32),
  impl_value VARCHAR(64),
  rate_limit INT,
  timeout_seconds INT,
  role_required INT,
  tags VARCHAR(500),
  version INT,
  author INT,
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status INT DEFAULT 0,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ai_tool_invocation (
  id BIGINT NOT NULL,
  tool_code VARCHAR(64),
  user_id BIGINT NOT NULL,
  username VARCHAR(128),
  input_json TEXT,
  output_json TEXT,
  status VARCHAR(32),
  error_message TEXT,
  duration_ms INT,
  ip VARCHAR(64),
  user_agent VARCHAR(512),
  data_source_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ai_voting_record (
  id BIGINT NOT NULL,
  session_id INT NOT NULL,
  user_id BIGINT NOT NULL,
  username VARCHAR(128),
  question INT,
  final_answer INT,
  strategy DECIMAL(20,6),
  total_votes INT DEFAULT 0,
  agreement_rate DECIMAL(20,6),
  model_votes VARCHAR(32),
  duration_ms INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT NOT NULL,
  trace_id VARCHAR(64),
  user_id BIGINT NOT NULL,
  username VARCHAR(128),
  user_ip VARCHAR(64),
  user_agent VARCHAR(512),
  action INT,
  resource_type VARCHAR(32),
  resource_id VARCHAR(255),
  method VARCHAR(32),
  path VARCHAR(255),
  request_body TEXT,
  response_status INT DEFAULT 0,
  result VARCHAR(32),
  error_msg INT,
  duration_ms INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS billing_record (
  id BIGINT NOT NULL,
  record_id INT,
  user_id BIGINT NOT NULL,
  license_id BIGINT,
  model_entry_id BIGINT,
  record_type VARCHAR(32),
  amount_cents BIGINT,
  currency VARCHAR(8),
  status VARCHAR(32),
  payment_method VARCHAR(128),
  external_transaction_id VARCHAR(128),
  description VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS cluster_node (
  id BIGINT NOT NULL,
  node_id INT,
  name VARCHAR(128),
  address INT,
  region VARCHAR(64),
  zone VARCHAR(64),
  capabilities INT,
  total_cores INT DEFAULT 0,
  total_memory_mb BIGINT,
  total_gpus INT DEFAULT 0,
  cpu_usage DOUBLE,
  memory_usage DOUBLE,
  gpu_usage DOUBLE,
  active_tasks INT DEFAULT 0,
  status VARCHAR(32),
  is_leader BOOLEAN,
  last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS dashboard_metric (
  id BIGINT NOT NULL,
  metric INT,
  dimension INT,
  value DOUBLE,
  tags VARCHAR(500),
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS data_source (
  id BIGINT NOT NULL,
  name VARCHAR(128),
  type VARCHAR(32),
  jdbc_url VARCHAR(512),
  username VARCHAR(128),
  password VARCHAR(255),
  driver_class INT,
  pool_size INT DEFAULT 0,
  min_idle INT,
  max_lifetime INT,
  enabled INT DEFAULT 0,
  test_status VARCHAR(32),
  test_message TEXT,
  last_test_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  description VARCHAR(64),
  tags VARCHAR(500),
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS kb_chunk (
  id BIGINT NOT NULL,
  chunk_id INT NOT NULL,
  doc_id INT NOT NULL,
  kb_id INT NOT NULL,
  seq INT,
  content TEXT,
  char_count INT DEFAULT 0,
  token_count INT DEFAULT 0,
  embedding INT,
  embedding_model VARCHAR(32),
  keywords VARCHAR(64),
  summary VARCHAR(500),
  location INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS kb_document (
  id BIGINT NOT NULL,
  doc_id INT NOT NULL,
  kb_id INT NOT NULL,
  filename VARCHAR(128),
  mime_type VARCHAR(32),
  size_bytes BIGINT,
  sha256 INT,
  file_path VARCHAR(255),
  source VARCHAR(255),
  source_url VARCHAR(512),
  status VARCHAR(32),
  chunk_count INT DEFAULT 0,
  embedding_count INT DEFAULT 0,
  error INT,
  tags VARCHAR(500),
  owner_id BIGINT NOT NULL,
  is_public BOOLEAN,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS kb_permission (
  id BIGINT NOT NULL,
  kb_id INT NOT NULL,
  subject_type VARCHAR(255),
  subject_id BIGINT,
  permission INT,
  grant_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS raft_log (
  id BIGINT NOT NULL,
  term BIGINT,
  log_index BIGINT,
  node_id INT,
  command INT,
  committed BOOLEAN,
  committed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS model_license (
  id BIGINT NOT NULL,
  license_key VARCHAR(64),
  model_entry_id BIGINT,
  model_version_id BIGINT,
  user_id BIGINT NOT NULL,
  license_type VARCHAR(32),
  status VARCHAR(32),
  quota_calls BIGINT,
  used_calls BIGINT,
  start_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expire_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  price_cents BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS model_version (
  id BIGINT NOT NULL,
  version_id INT,
  model_entry_id BIGINT,
  version INT,
  changelog INT,
  file_path VARCHAR(255),
  size_bytes BIGINT,
  sha256 INT,
  input_schema TEXT,
  output_schema TEXT,
  status VARCHAR(32),
  is_latest BOOLEAN,
  uploader_id BIGINT,
  backward_compatible INT,
  metadata TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

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