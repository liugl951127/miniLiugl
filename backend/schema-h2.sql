CREATE TABLE IF NOT EXISTS auth_login_log (
  id BIGINT AUTO_INCREMENT NOT NULL,
  user_id BIGINT NOT NULL,
  username VARCHAR(128),
  ip VARCHAR(64),
  user_agent VARCHAR(512),
  status INT DEFAULT 0,
  message TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS auth_refresh_token (
  id BIGINT AUTO_INCREMENT NOT NULL,
  user_id BIGINT NOT NULL,
  token VARCHAR(255),
  expires_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  revoked INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS notification (
  id BIGINT AUTO_INCREMENT NOT NULL,
  user_id BIGINT NOT NULL,
  type VARCHAR(32),
  title VARCHAR(255),
  content TEXT,
  is_read INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS oauth_app_config (
  id BIGINT AUTO_INCREMENT NOT NULL,
  platform INT,
  app_type VARCHAR(32),
  app_id INT,
  app_secret VARCHAR(255),
  public_key VARCHAR(64),
  redirect_uri VARCHAR(512),
  scopes VARCHAR(32),
  enabled INT DEFAULT 0,
  extra_config TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS oauth_binding (
  id BIGINT AUTO_INCREMENT NOT NULL,
  user_id BIGINT NOT NULL,
  platform INT,
  app_type VARCHAR(32),
  openid VARCHAR(128),
  unionid VARCHAR(128),
  nickname VARCHAR(128),
  avatar VARCHAR(512),
  access_token VARCHAR(255),
  refresh_token VARCHAR(255),
  token_expires_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  raw_data INT,
  bound_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT AUTO_INCREMENT NOT NULL,
  code VARCHAR(64),
  name VARCHAR(128),
  description VARCHAR(64),
  sort INT DEFAULT 0,
  enabled INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT NOT NULL,
  username VARCHAR(128),
  password VARCHAR(255),
  nickname VARCHAR(128),
  email VARCHAR(128),
  phone VARCHAR(32),
  avatar VARCHAR(512),
  gender INT DEFAULT 0,
  status INT DEFAULT 0,
  last_login_ip VARCHAR(64),
  last_login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  tenant_id BIGINT NOT NULL,
  remark VARCHAR(500),
  wechat_openid VARCHAR(128),
  wechat_unionid VARCHAR(128),
  wechat_nickname VARCHAR(128),
  wechat_avatar VARCHAR(512),
  wechat_bound_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  qq_openid VARCHAR(128),
  qq_unionid VARCHAR(128),
  qq_nickname VARCHAR(128),
  qq_avatar VARCHAR(512),
  qq_bound_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  alipay_openid VARCHAR(64),
  alipay_user_id VARCHAR(64),
  alipay_nickname VARCHAR(64),
  alipay_avatar VARCHAR(64),
  alipay_bound_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS tenant (
  id BIGINT AUTO_INCREMENT NOT NULL,
  code VARCHAR(64),
  name VARCHAR(128),
  plan VARCHAR(64),
  status INT DEFAULT 0,
  max_users INT,
  max_models INT DEFAULT 0,
  qps_limit INT,
  monthly_quota BIGINT,
  used_quota BIGINT,
  expire_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  contact_email VARCHAR(128),
  contact_phone VARCHAR(32),
  remark VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS unionid_relations (
  id BIGINT AUTO_INCREMENT NOT NULL,
  user_id BIGINT NOT NULL,
  unionid VARCHAR(128),
  platform INT,
  first_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  binding_count INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user_api_key (
  id BIGINT AUTO_INCREMENT NOT NULL,
  user_id BIGINT NOT NULL,
  name VARCHAR(128),
  key_hash VARCHAR(64),
  key_prefix VARCHAR(64),
  scopes VARCHAR(32),
  expires_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  use_count BIGINT,
  enabled INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS wechat_config (
  id BIGINT AUTO_INCREMENT NOT NULL,
  app_type VARCHAR(32),
  app_id INT,
  app_secret VARCHAR(255),
  token VARCHAR(255),
  aes_key VARCHAR(64),
  redirect_uri VARCHAR(512),
  scope VARCHAR(32),
  enabled INT DEFAULT 0,
  remark VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS wechat_scan_session (
  id BIGINT AUTO_INCREMENT NOT NULL,
  ticket INT,
  scene_id INT,
  status VARCHAR(32),
  openid VARCHAR(128),
  unionid VARCHAR(128),
  nickname VARCHAR(128),
  avatar VARCHAR(512),
  user_id BIGINT NOT NULL,
  access_token VARCHAR(255),
  refresh_token VARCHAR(255),
  client_ip VARCHAR(64),
  user_agent VARCHAR(512),
  expires_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  confirmed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS wechat_user_binding (
  id BIGINT AUTO_INCREMENT NOT NULL,
  user_id BIGINT NOT NULL,
  openid VARCHAR(128),
  unionid VARCHAR(128),
  app_type VARCHAR(32),
  nickname VARCHAR(128),
  avatar VARCHAR(512),
  bound_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role INT,
  content TEXT,
  tokens INT,
  finish_reason INT,
  error_message TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(255),
  model VARCHAR(32),
  system_prompt TEXT,
  temperature DECIMAL(20,6),
  status INT DEFAULT 0,
  message_count INT DEFAULT 0,
  last_message_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  tenant_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS model_battle_log (
  id BIGINT NOT NULL,
  battle_id INT,
  user_id BIGINT NOT NULL,
  model_id BIGINT,
  model_code VARCHAR(64),
  prompt TEXT,
  response INT,
  prompt_tokens INT,
  completion_tokens INT,
  latency_ms INT,
  status VARCHAR(32),
  error_msg INT,
  score INT,
  judge_model VARCHAR(32),
  judge_reason INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS model_config (
  id BIGINT NOT NULL,
  provider_id BIGINT,
  model_code VARCHAR(64),
  display_name VARCHAR(128),
  max_context INT,
  max_output INT,
  input_price DECIMAL(20,6),
  output_price DECIMAL(20,6),
  supports_vision INT,
  supports_tools INT,
  supports_stream INT,
  enabled INT DEFAULT 0,
  sort INT DEFAULT 0,
  description VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS model_provider (
  id BIGINT NOT NULL,
  code VARCHAR(64),
  name VARCHAR(128),
  base_url VARCHAR(512),
  api_key VARCHAR(255),
  protocol VARCHAR(255),
  enabled INT DEFAULT 0,
  sort INT DEFAULT 0,
  description VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS model_quota (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  model_id BIGINT,
  quota_date DATE,
  used_tokens BIGINT,
  used_requests INT,
  limit_tokens BIGINT,
  limit_requests INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

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

CREATE TABLE IF NOT EXISTS training_task (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  model_name VARCHAR(128),
  corpus_path VARCHAR(255),
  n_layer INT,
  n_head INT,
  n_embd INT,
  block_size INT DEFAULT 0,
  max_iters INT,
  batch_size INT DEFAULT 0,
  learning_rate DOUBLE,
  status VARCHAR(32),
  progress INT,
  current_loss DOUBLE,
  current_iter INT,
  error_message TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS prompt_template (
  id BIGINT NOT NULL,
  name VARCHAR(128),
  description VARCHAR(64),
  category VARCHAR(32),
  content TEXT,
  variables VARCHAR(512),
  creator_id BIGINT,
  creator_name VARCHAR(128),
  is_public BOOLEAN,
  use_count INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS agent_task (
  id BIGINT NOT NULL,
  task_id INT,
  user_id BIGINT NOT NULL,
  goal TEXT,
  status VARCHAR(32),
  rounds INT,
  result VARCHAR(32),
  llm_calls INT,
  tool_calls INT,
  total_tokens INT DEFAULT 0,
  error_msg INT,
  latency_ms BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS collab_member (
  id BIGINT NOT NULL,
  collab_id BIGINT,
  user_id BIGINT NOT NULL,
  role INT,
  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS collab_session (
  id BIGINT NOT NULL,
  session_id INT NOT NULL,
  owner_id BIGINT NOT NULL,
  title VARCHAR(255),
  max_users INT,
  status VARCHAR(32),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS kg_entity (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  name VARCHAR(128),
  entity_type VARCHAR(32),
  description VARCHAR(64),
  aliases VARCHAR(500),
  importance INT,
  source VARCHAR(255),
  ref_count INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS kg_relation (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  from_entity BIGINT NOT NULL,
  to_entity BIGINT NOT NULL,
  relation_type VARCHAR(32),
  description VARCHAR(64),
  weight DECIMAL(20,6),
  source VARCHAR(255),
  ref_count INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS plugin (
  id BIGINT NOT NULL,
  name VARCHAR(128),
  display_name VARCHAR(128),
  description VARCHAR(64),
  version INT,
  author INT,
  category VARCHAR(32),
  scope VARCHAR(32),
  owner_id BIGINT NOT NULL,
  icon VARCHAR(512),
  entry VARCHAR(64),
  plugin_type VARCHAR(32),
  config TEXT,
  enabled INT DEFAULT 0,
  downloads INT,
  rating DECIMAL(20,6),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
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

CREATE TABLE IF NOT EXISTS document (
  id BIGINT NOT NULL,
  kb_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  title VARCHAR(255),
  source_type VARCHAR(32),
  source_uri VARCHAR(512),
  content TEXT,
  size_bytes BIGINT,
  status VARCHAR(32),
  error_msg INT,
  chunk_count INT DEFAULT 0,
  tags VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  checksum INT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS document_chunk (
  id BIGINT NOT NULL,
  doc_id BIGINT NOT NULL,
  kb_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  chunk_index INT DEFAULT 0 NOT NULL,
  content TEXT,
  embedding BLOB,
  dim INT,
  char_count INT DEFAULT 0,
  start_pos INT,
  end_pos INT,
  access_count INT DEFAULT 0,
  last_access_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS knowledge_base (
  id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(128),
  description VARCHAR(64),
  visibility INT,
  doc_count INT DEFAULT 0,
  chunk_count INT DEFAULT 0,
  tags VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS analytics_datasource (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  name VARCHAR(128),
  type VARCHAR(32),
  jdbc_url VARCHAR(512),
  username VARCHAR(128),
  password_enc VARCHAR(255),
  description VARCHAR(64),
  deleted INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS analytics_ingest_task (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  task_id INT,
  filename VARCHAR(128),
  file_type VARCHAR(32),
  encoding INT,
  separator VARCHAR(255),
  file_size BIGINT,
  status VARCHAR(32),
  error_message TEXT,
  quality_json INT,
  total_rows BIGINT,
  total_columns BIGINT,
  columns_json INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS analytics_nlsql_history (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  data_source_id BIGINT,
  question INT,
  generated_sql TEXT,
  corrected_sql TEXT,
  model VARCHAR(32),
  prompt_tokens INT,
  completion_tokens INT,
  duration_ms BIGINT,
  success BOOLEAN,
  error_message TEXT,
  feedback_rating INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS analytics_report (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  report_id INT,
  title VARCHAR(255),
  question INT,
  sql_text TEXT,
  markdown INT,
  chart_options_json VARCHAR(255),
  row_count BIGINT,
  duration_ms BIGINT,
  format INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS alert_channel (
  id BIGINT NOT NULL,
  name VARCHAR(128),
  channel_type VARCHAR(32),
  type VARCHAR(32),
  target INT,
  config TEXT,
  enabled INT DEFAULT 0,
  priority INT DEFAULT 0,
  description VARCHAR(64),
  template TEXT,
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS alert_event (
  id BIGINT NOT NULL,
  rule_id BIGINT,
  rule_name VARCHAR(128),
  severity VARCHAR(32),
  metric_name VARCHAR(128),
  message TEXT,
  status VARCHAR(32),
  fired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  acked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  acked_by BIGINT,
  notes VARCHAR(500),
  duration BIGINT,
  silenced_until TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS alert_rule (
  id BIGINT NOT NULL,
  name VARCHAR(128),
  description VARCHAR(64),
  metric_name VARCHAR(128),
  service INT,
  operator VARCHAR(255),
  severity VARCHAR(32),
  cooldown_minutes INT DEFAULT 0,
  enabled INT DEFAULT 0,
  tags VARCHAR(500),
  notify_channel INT,
  silenced_until TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS metric_snapshot (
  id BIGINT NOT NULL,
  service INT,
  metric_name VARCHAR(128),
  metric_value DECIMAL(20,6),
  tags VARCHAR(500),
  recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pipeline_node_log (
  id BIGINT NOT NULL,
  run_id BIGINT,
  node_id INT,
  node_type VARCHAR(32),
  node_name VARCHAR(128),
  status VARCHAR(32),
  start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  end_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  duration_ms BIGINT,
  input_rows INT,
  output_rows INT,
  output_preview TEXT,
  error_message TEXT,
  config_snapshot TEXT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pipeline_run (
  id BIGINT NOT NULL,
  workflow_id BIGINT,
  workflow_name VARCHAR(128),
  status VARCHAR(32),
  trigger_by BIGINT,
  trigger_type VARCHAR(32),
  definition_snapshot TEXT,
  start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  end_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  duration_ms BIGINT,
  error_message TEXT,
  result_summary VARCHAR(500),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pipeline_workflow (
  id BIGINT NOT NULL,
  name VARCHAR(128),
  description VARCHAR(64),
  definition TEXT,
  version INT,
  status INT DEFAULT 0,
  create_by BIGINT NOT NULL,
  update_by BIGINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pipeline_workflow_version (
  id BIGINT NOT NULL,
  workflow_id BIGINT,
  version INT,
  definition TEXT,
  change_log INT,
  create_by BIGINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS function_call_log (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  tool_name VARCHAR(128),
  arguments INT,
  result VARCHAR(32),
  status VARCHAR(32),
  error_msg INT,
  duration_ms INT,
  ip VARCHAR(64),
  user_agent VARCHAR(512),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS function_tool (
  id BIGINT NOT NULL,
  name VARCHAR(128),
  display_name VARCHAR(128),
  description VARCHAR(64),
  category VARCHAR(32),
  scope VARCHAR(32),
  owner_id BIGINT NOT NULL,
  parameters INT,
  endpoint VARCHAR(255),
  http_method VARCHAR(32),
  enabled INT DEFAULT 0,
  tags VARCHAR(500),
  risk_level VARCHAR(32),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS skill_approval (
  id BIGINT NOT NULL,
  task_id INT,
  user_id BIGINT NOT NULL,
  username VARCHAR(128),
  tool_name VARCHAR(128),
  risk_level VARCHAR(32),
  goal TEXT,
  tool_params TEXT,
  status VARCHAR(32),
  approver_id BIGINT,
  approver_name VARCHAR(128),
  reason INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted INT DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS collab_message (
  id BIGINT NOT NULL,
  room_id INT,
  user_id BIGINT NOT NULL,
  username VARCHAR(128),
  nickname VARCHAR(128),
  type VARCHAR(32),
  content TEXT,
  metadata TEXT,
  client_msg_id INT,
  broadcast INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS collab_participant (
  id BIGINT NOT NULL,
  room_id INT,
  user_id BIGINT NOT NULL,
  username VARCHAR(128),
  nickname VARCHAR(128),
  avatar VARCHAR(512),
  role INT,
  cursor_x INT,
  cursor_y INT,
  selection_id INT,
  status VARCHAR(32),
  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  left_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS collab_room (
  id BIGINT NOT NULL,
  room_id INT,
  name VARCHAR(128),
  type VARCHAR(32),
  owner_id BIGINT NOT NULL,
  owner_name VARCHAR(128),
  description VARCHAR(64),
  is_public INT,
  max_participants INT,
  status VARCHAR(32),
  current_participants INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  closed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS admin_audit_log (
  id BIGINT NOT NULL,
  actor_id BIGINT,
  actor_name VARCHAR(128),
  action INT,
  resource_type VARCHAR(32),
  resource_id VARCHAR(255),
  detail INT,
  result VARCHAR(32),
  error_msg INT,
  ip VARCHAR(64),
  user_agent VARCHAR(512),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS audit_log_full (
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

-- =============================================================
-- 种子数据 (Seed Data)
-- =============================================================
-- ============================================================
-- MiniMax Platform V6.8.2 种子数据
-- 生成时间: 2026-08-12
-- 说明: BCrypt 密码占位符 (部署后建议通过管理后台修改)
--       Admin@123 → $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy (password)
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 模块: minimax-auth  认证与用户
-- ============================================================

-- 角色 (先插角色，因为用户依赖 role)
INSERT INTO `sys_role` (id, code, name, description, sort, enabled, created_at) VALUES
(1, 'ADMIN', '系统管理员', '拥有所有系统管理权限', 1, 1, '2026-01-01 00:00:00'),
(2, 'USER',  '普通用户',   '基础功能使用权限',       2, 1, '2026-01-01 00:00:00'),
(3, 'GUEST', '访客',      '只读权限',                3, 1, '2026-01-01 00:00:00');

-- 租户
INSERT INTO `tenant` (id, code, name, plan, status, max_users, max_models, qps_limit, monthly_quota, used_quota, expire_at, contact_email, created_at) VALUES
(1, 'default',   '默认租户',     'free',  1, 10,  3, 10, 100000, 0, '2027-12-31 23:59:59', 'admin@minimax.io',  '2026-01-01 00:00:00'),
(2, 'enterprise','企业版租户',    'pro',   1, 100, 20, 100, 10000000, 0, '2027-12-31 23:59:59', 'corp@minimax.io',  '2026-01-15 00:00:00');

-- 用户 (BCrypt hash = password)
INSERT INTO `sys_user` (id, username, password, nickname, email, phone, avatar, gender, status, last_login_ip, tenant_id, created_at) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'admin@minimax.io', '13800138000', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', 1, 1, '127.0.0.1', 1, '2026-01-01 00:00:00'),
(2, 'liugl',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '刘GL',        'liugl@minimax.io',  '13900139000', 'https://api.dicebear.com/7.x/avataaars/svg?seed=liugl',  1, 1, '127.0.0.1', 1, '2026-01-02 00:00:00'),
(3, 'guest',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '访客用户',    'guest@minimax.io',  NULL,           'https://api.dicebear.com/7.x/avataaars/svg?seed=guest',  0, 1, NULL, 1, '2026-01-03 00:00:00');

-- 用户-角色关联
INSERT INTO `sys_user_role` (user_id, role_id) VALUES
(1, 1),  -- admin → ADMIN
(2, 2),  -- liugl → USER
(3, 3);  -- guest → GUEST

-- API Key
INSERT INTO `user_api_key` (id, user_id, name, key_hash, key_prefix, scopes, expires_at, last_used_at, use_count, enabled, created_at) VALUES
(1, 2, '默认 Key', 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6', 'sk-a1b2****', 'chat,rag,agent', '2027-12-31 00:00:00', '2026-08-01 00:00:00', 0, 1, '2026-01-10 00:00:00'),
(2, 1, 'Admin Key', 'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1', 'sk-b2c3****', '*', '2027-12-31 00:00:00', '2026-08-10 00:00:00', 150, 1, '2026-01-10 00:00:00');

-- 通知
INSERT INTO `notification` (id, user_id, type, title, content, is_read, created_at) VALUES
(1, 1, 'SYSTEM', '欢迎使用 MiniMax', '系统初始化完成，V6.8.2 版本已就绪', 0, '2026-01-01 00:00:00'),
(2, 2, 'SYSTEM', '账号激活',         '您的账号已激活，可以开始使用了', 1, '2026-01-02 00:00:00');

-- 登录日志
INSERT INTO `auth_login_log` (id, user_id, username, ip, user_agent, status, fail_reason, created_at) VALUES
(1, 1, 'admin', '127.0.0.1', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 1, NULL, '2026-01-01 08:00:00'),
(2, 2, 'liugl', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 1, NULL, '2026-01-02 09:00:00');

-- ============================================================
-- 模块: minimax-model  模型服务
-- ============================================================

-- 模型提供商
INSERT INTO `model_provider` (id, code, name, base_url, api_key, protocol, enabled, sort, description, created_at) VALUES
(1, 'openai',      'OpenAI',          'https://api.openai.com',        '',                  'openai',     1, 1, 'OpenAI GPT 系列模型',                          '2026-01-01 00:00:00'),
(2, 'siliconflow', 'SiliconFlow',     'https://api.siliconflow.cn',     '',                  'openai',     1, 2, 'SiliconFlow 聚合 API (DeepSeek/Qwen/GLM 等)',   '2026-01-01 00:00:00'),
(3, 'ollama',      'Ollama (本地)',    'http://localhost:11434',         '',                  'ollama',     1, 3, 'Ollama 本地推理 (MiniTransformer/Qwen 等)',     '2026-01-01 00:00:00'),
(4, 'deepseek',    'DeepSeek',         'https://api.deepseek.com',       '',                  'openai',     1, 4, 'DeepSeek 大模型',                               '2026-01-01 00:00:00'),
(5, 'minimax',     'MiniMax 自研',     'http://localhost:8080/model',    '',                  'onnx',       1, 5, 'MiniMax 自研 MiniTransformer ONNX 推理',       '2026-01-01 00:00:00');

-- 模型配置
INSERT INTO `model_config` (id, provider_id, model_code, display_name, max_context, max_output, input_price, output_price, supports_vision, supports_tools, supports_stream, enabled, sort, description, created_at) VALUES
-- OpenAI
(1, 1, 'gpt-4o',             'GPT-4o',            128000, 16384, 0.005,    0.015,   1, 1, 1, 1, 10, 'OpenAI 最强多模态模型',                   '2026-01-01 00:00:00'),
(2, 1, 'gpt-4o-mini',         'GPT-4o mini',        128000, 16384, 0.00015, 0.0006,  1, 1, 1, 1, 20, 'GPT-4o 轻量版，性价比高',                '2026-01-01 00:00:00'),
(3, 1, 'gpt-4-turbo',         'GPT-4 Turbo',        128000, 4096,  0.01,    0.03,    1, 1, 1, 0, 15, 'GPT-4 高速版',                           '2026-01-01 00:00:00'),
-- SiliconFlow
(4, 2, 'deepseek-chat',       'DeepSeek Chat',      64000,  8192,  0.0001,  0.0003,  1, 1, 1, 1, 30, 'DeepSeek 深度思考模型',                  '2026-01-01 00:00:00'),
(5, 2, 'Qwen/Qwen2.5-72B-Instruct', 'Qwen 72B',     32000,  4096,  0.0006,  0.0018,  1, 1, 1, 1, 40, '通义千问 72B 大模型',                     '2026-01-01 00:00:00'),
-- Ollama 本地
(6, 3, 'llama3.2',            'Llama 3.2',          8192,   4096,  0,       0,        0, 1, 1, 1, 50, 'Meta Llama 3.2 (本地)',                  '2026-01-01 00:00:00'),
(7, 3, 'qwen2.5',             'Qwen 2.5 (本地)',    8192,   4096,  0,       0,        1, 1, 1, 1, 51, '通义千问 2.5 (本地)',                    '2026-01-01 00:00:00'),
(8, 3, 'min-transformer',     'MiniTransformer',    4096,   1024,  0,       0,        0, 1, 1, 1, 99, 'MiniMax 自研 Transformer (ONNX)',         '2026-01-01 00:00:00'),
-- DeepSeek
(9, 4, 'deepseek-reasoner',   'DeepSeek R1',         64000,  8192,  0.0001,  0.0003,  0, 1, 1, 1, 35, 'DeepSeek 推理模型 R1',                   '2026-01-01 00:00:00'),
-- Embedding
(10, 2, 'BAAI/bge-m3',        'BGE-M3 Embedding',   4096,   0,     0.00001, 0,       0, 0, 0, 1, 60, '智谱 BGE-M3 多语言 Embedding',           '2026-01-01 00:00:00');

-- 模型配额 (每个用户每天免费额度)
INSERT INTO `model_quota` (id, user_id, model_id, quota_date, used_tokens, used_requests, limit_tokens, limit_requests, created_at, updated_at) VALUES
(1, 2, 4,  CURDATE(), 0, 0, 100000, 100, NOW(), NOW()),
(2, 2, 5,  CURDATE(), 0, 0, 100000, 100, NOW(), NOW()),
(3, 2, 8,  CURDATE(), 0, 0, 999999999, 999999, NOW(), NOW()),
(4, 3, 4,  CURDATE(), 0, 0, 10000, 10, NOW(), NOW());

-- ============================================================
-- 模块: minimax-chat  聊天会话
-- ============================================================

INSERT INTO `chat_session` (id, user_id, title, model, system_prompt, temperature, status, message_count, last_message_at, tenant_id, created_at, updated_at) VALUES
(1, 2, 'AI 助手对话',   'gpt-4o-mini',     '你是一个有帮助的 AI 助手。', 0.7, 1, 4, '2026-08-10 10:00:00', 1, '2026-08-10 09:00:00', '2026-08-10 10:00:00'),
(2, 2, '本地模型测试',  'min-transformer', '本地 MiniTransformer 推理测试', 0.8, 1, 2, '2026-08-11 15:00:00', 1, '2026-08-11 14:00:00', '2026-08-11 15:00:00'),
(3, 2, 'RAG 知识问答',  'deepseek-chat',   '基于知识库回答问题。', 0.5, 1, 3, '2026-08-12 08:00:00', 1, '2026-08-12 08:00:00', '2026-08-12 08:00:00');

INSERT INTO `chat_message` (id, session_id, user_id, role, content, tokens, finish_reason, created_at) VALUES
(1, 1, 2, 'user',   '你好，请介绍一下 MiniMax 平台', NULL, NULL, '2026-08-10 09:05:00'),
(2, 1, 2, 'assistant','MiniMax 是一个企业级 AI Agent 平台，支持 RAG、Agent 编排、多模态模型接入。', 128, 'stop', '2026-08-10 09:05:30'),
(3, 1, 2, 'user',   'V6.8.2 有哪些新特性？', NULL, NULL, '2026-08-10 09:10:00'),
(4, 1, 2, 'assistant','V6.8.2: RAG SSE 流式、Agent Canvas 可视化、多模态真实 API、安全加固。', 96, 'stop', '2026-08-10 09:10:15'),
(5, 2, 2, 'user',   '用本地模型生成一段 Python 代码', NULL, NULL, '2026-08-11 14:05:00'),
(6, 2, 2, 'assistant','以下是 Python 示例代码：\n```python\ndef hello():\n    print("Hello from MiniTransformer!")\n```', 64, 'stop', '2026-08-11 14:05:30'),
(7, 3, 2, 'user',   '什么是 RAG？', NULL, NULL, '2026-08-12 08:05:00'),
(8, 3, 2, 'assistant','RAG (检索增强生成) 结合向量检索与 LLM 推理，实时从知识库获取相关片段，提升回答准确性。', 112, 'stop', '2026-08-12 08:05:30');

-- ============================================================
-- 模块: minimax-rag  知识库 RAG
-- ============================================================

INSERT INTO `knowledge_base` (id, owner_id, tenant_id, name, description, visibility, doc_count, chunk_count, tags, created_at, updated_at) VALUES
(1, 1, 1, 'MiniMax 产品文档', 'MiniMax V6.8.2 产品功能文档', 'public', 3, 12, '产品,文档,RAG', '2026-06-01 00:00:00', '2026-08-12 00:00:00'),
(2, 2, 1, '我的私人知识库', '个人笔记与收藏', 'private', 1, 5, '私人,笔记', '2026-07-01 00:00:00', '2026-08-10 00:00:00'),
(3, 1, 1, '企业知识库', '公司内部知识共享', 'public', 5, 20, '企业,内部,知识', '2026-05-01 00:00:00', '2026-08-11 00:00:00');

INSERT INTO `document` (id, kb_id, owner_id, title, source_type, source_uri, content, size_bytes, status, chunk_count, tags, created_at, updated_at) VALUES
(1, 1, 1, 'MiniMax 平台介绍',    'url',     'https://minimax.io/doc/intro',      'MiniMax 是新一代企业级 AI Agent 平台...', 2048, 'completed', 4, '产品介绍', '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
(2, 1, 1, 'RAG 使用手册',        'file',    'doc://rag-guide.pdf',               '本文档介绍如何在 MiniMax 中使用 RAG 功能...', 8192, 'completed', 6, 'RAG,手册', '2026-06-15 00:00:00', '2026-06-15 00:00:00'),
(3, 2, 2, '个人笔记：AI 趋势',   'text',    NULL,                                '我认为 AI 的发展方向是：多模态、Agent、RAG...', 512, 'completed', 2, 'AI趋势', '2026-07-01 00:00:00', '2026-07-01 00:00:00'),
(4, 3, 1, '公司规章制度',         'file',    'doc://company-rules.pdf',           '第一章 总则...', 4096, 'completed', 5, '制度,HR', '2026-05-01 00:00:00', '2026-05-01 00:00:00');

INSERT INTO `document_chunk` (id, doc_id, kb_id, owner_id, chunk_index, content, dim, char_count, start_pos, end_pos, access_count, created_at) VALUES
(1, 1, 1, 1, 0, 'MiniMax 是新一代企业级 AI Agent 平台，支持 RAG、Agent 编排、多模态模型接入。', 384, 52, 0, 52, 10, '2026-06-01 00:00:00'),
(2, 1, 1, 1, 1, 'V6.8.2 版本带来：真实 LLM 路由、Agent Canvas、RAG SSE 流式推送、安全加固。', 384, 56, 52, 108, 8, '2026-06-01 00:00:00'),
(3, 2, 1, 1, 0, 'RAG (Retrieval-Augmented Generation) 是一种结合检索与生成的 AI 技术。', 384, 48, 0, 48, 15, '2026-06-15 00:00:00'),
(4, 2, 1, 1, 1, '在 MiniMax 中创建知识库：上传文档 → 自动分块 → 向量化 → 检索问答。', 384, 44, 48, 92, 12, '2026-06-15 00:00:00'),
(5, 3, 2, 2, 0, '我认为 AI 的发展方向是：多模态、Agent、RAG、端侧推理。', 384, 38, 0, 38, 3, '2026-07-01 00:00:00');

-- ============================================================
-- 模块: minimax-agent  Agent 智能体
-- ============================================================

INSERT INTO `plugin` (id, name, display_name, description, version, author, category, scope, owner_id, icon, entry, plugin_type, enabled, created_at) VALUES
(1, 'web-search',     '网络搜索',    '实时搜索互联网，获取最新信息',          '1.0.0', 'MiniMax', 'search',    'global', 1, '🔍', 'SearchPlugin',     'class',  1, '2026-01-01 00:00:00'),
(2, 'calculator',     '计算器',      '执行数学计算，支持函数运算',             '1.0.0', 'MiniMax', 'tool',      'global', 1, '🧮', 'CalcPlugin',       'class',  1, '2026-01-01 00:00:00'),
(3, 'weather',       '天气查询',    '查询指定城市当前天气',                   '1.0.0', 'MiniMax', 'tool',      'global', 1, '🌤',  'WeatherPlugin',    'class',  1, '2026-01-01 00:00:00'),
(4, 'code-runner',   '代码执行',    '沙箱环境执行 Python/JS 代码',           '1.0.0', 'MiniMax', 'analysis',  'global', 1, '💻', 'CodeRunnerPlugin', 'class',  1, '2026-01-01 00:00:00'),
(5, 'image-gen',     '图片生成',    '调用 DALL-E/MJ 生成图片',               '1.0.0', 'MiniMax', 'creative',  'global', 1, '🎨', 'ImageGenPlugin',   'class',  0, '2026-01-01 00:00:00'),
(6, 'rag-retriever', 'RAG 检索',    '从知识库检索相关文档片段',              '1.0.0', 'MiniMax', 'tool',      'global', 1, '📚', 'RagPlugin',        'class',  1, '2026-01-01 00:00:00'),
(7, 'sql-query',     'SQL 查询',    '执行只读 SQL 查询（防注入）',           '1.0.0', 'MiniMax', 'analysis',  'global', 1, '🗄', 'SqlPlugin',         'class',  1, '2026-01-01 00:00:00');

INSERT INTO `kg_entity` (id, user_id, name, entity_type, description, aliases, importance, source, ref_count, created_at, updated_at) VALUES
(1, 2, 'MiniMax',        'Organization', 'MiniMax AI 科技公司',       'MiniMax,稀宇科技', 10, 'manual', 3, '2026-03-01 00:00:00', '2026-03-01 00:00:00'),
(2, 2, 'RAG',            'Technology',   '检索增强生成技术',           'Retrieval-Augmented Generation', 8, 'manual', 2, '2026-03-01 00:00:00', '2026-03-01 00:00:00'),
(3, 2, 'GPT-4o',         'Model',        'OpenAI GPT-4o 多模态模型',  'gpt4o,gpt-4o', 7, 'manual', 1, '2026-03-01 00:00:00', '2026-03-01 00:00:00'),
(4, 2, 'Agent',          'Technology',   'AI 智能体 / Agent 框架',     'ai-agent,智能体', 9, 'manual', 2, '2026-03-01 00:00:00', '2026-03-01 00:00:00');

INSERT INTO `kg_relation` (id, user_id, from_entity, to_entity, relation_type, description, weight, source, created_at) VALUES
(1, 2, 1, 2, 'DEVELOPS',    'MiniMax 开发了 RAG 平台', 1.0, 'manual', '2026-03-01 00:00:00'),
(2, 2, 2, 3, 'USED_BY',     'RAG 常用 GPT-4o 作为基座', 0.9, 'manual', '2026-03-01 00:00:00'),
(3, 2, 4, 2, 'USES',        'Agent 使用 RAG 做知识增强', 1.0, 'manual', '2026-03-01 00:00:00'),
(4, 2, 4, 3, 'USES',        'Agent 调用 GPT-4o 执行推理', 0.8, 'manual', '2026-03-01 00:00:00');

INSERT INTO `collab_session` (id, session_id, owner_id, title, status, max_users, created_at) VALUES
(1, 1001, 2, 'AI 产品评审', 'ACTIVE', 5, '2026-08-10 10:00:00');

INSERT INTO `collab_member` (id, collab_id, user_id, role, joined_at) VALUES
(1, 1, 2, 0,  '2026-08-10 10:00:00'),
(2, 1, 1, 1, '2026-08-10 10:05:00');

-- ============================================================
-- 模块: minimax-ai  AI 服务
-- ============================================================

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

-- ============================================================
-- 模块: minimax-monitor  监控告警
-- ============================================================

INSERT INTO `alert_channel` (id, name, channel_type, target, config, enabled, priority, description, template, created_by, created_at) VALUES
(1, '钉钉告警群', 'dingtalk', 'https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN', '{"secret":"SEC...","atMobiles":[]}', 1, 1, '生产环境钉钉告警', '【告警】${severity} ${name}: ${message}', 1, '2026-01-01 00:00:00'),
(2, '管理员邮箱', 'email',    'admin@minimax.io', '{"smtp":"smtp.example.com","from":"alert@minimax.io"}', 1, 2, '邮件告警通知', '告警通知: ${name}', 1, '2026-01-01 00:00:00'),
(3, '企业微信',   'wechat',   'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY', '{}', 0, 3, '企业微信备用告警', NULL, 1, '2026-01-01 00:00:00');

INSERT INTO `alert_rule` (id, name, description, metric_name, service, operator, threshold, severity, cooldown_minutes, enabled, tags, notify_channel, created_at) VALUES
(1, 'CPU 过高告警',   'CPU 使用率超过 80%',          'cpu_usage',    'cs-auth',    '>', 80.0,  'WARNING', 10, 1, 'prod',      '1',     '2026-01-01 00:00:00'),
(2, '内存告警',       '内存使用超过 90%',            'memory_usage', 'cs-auth',    '>', 90.0,  'CRITICAL',5, 1, 'prod',      '1,2',   '2026-01-01 00:00:00'),
(3, 'API 延迟过高',   'P99 响应时间超过 3 秒',       'api_latency_p99', 'gateway', '>', 3000.0, 'WARNING', 15, 1, 'prod,api', '2',     '2026-01-01 00:00:00'),
(4, '错误率告警',     '5xx 错误率超过 5%',            'error_rate_5xx', 'gateway', '>', 5.0,   'CRITICAL',5, 1, 'prod',      '1,2',   '2026-01-01 00:00:00'),
(5, 'Token 配额不足', 'Token 配额使用超过 80%',       'quota_usage',  'cs-model',   '>', 80.0,  'WARNING', 60, 1, 'quota',    '1',     '2026-01-01 00:00:00');

INSERT INTO `alert_event` (id, rule_id, channel_id, severity, message, metric_value, triggered_at, acknowledged, acknowledged_by, acknowledged_at, resolved_at) VALUES
(1, 1, 1, 'WARNING', 'CPU 使用率 85%，触发告警', 85.0, '2026-08-10 14:00:00', 1, 'admin', '2026-08-10 14:05:00', NULL),
(2, 3, 2, 'WARNING', 'API P99 延迟 3.5s，偏高', 3500.0, '2026-08-11 10:00:00', 0, NULL, NULL, NULL);

-- ============================================================
-- 模块: minimax-pipeline  流程编排
-- ============================================================

INSERT INTO `pipeline_workflow` (id, name, description, definition, version, status, create_by, create_time, update_time) VALUES
(1, 'RAG 问答流程', '{"nodes":[{"id":"1","type":"input","label":"用户问题"},{"id":"2","type":"rag","label":"知识检索"},{"id":"3","type":"llm","label":"生成回答"},{"id":"4","type":"output","label":"返回结果"}],"edges":[{"from":"1","to":"2"},{"from":"2","to":"3"},{"from":"3","to":"4"}]}', 1, 1, 1, '2026-06-01 00:00:00', '2026-06-01 00:00:00'),
(2, 'Agent 任务流', '{"nodes":[{"id":"1","type":"input"},{"id":"2","type":"planner"},{"id":"3","type":"executor"},{"id":"4","type":"critic"},{"id":"5","type":"output"}],"edges":[{"from":"1","to":"2"},{"from":"2","to":"3"},{"from":"3","to":"4"},{"from":"4","to":"5"}]}', 1, 1, 1, '2026-07-01 00:00:00', '2026-07-01 00:00:00');

INSERT INTO `skill_approval` (id, task_id, user_id, username, tool_name, risk_level, goal, tool_params, status, approver_id, approver_name, reason, created_at, updated_at) VALUES
(1, 'skill-001', 2, 'liugl', 'rag.retrieve', 'HIGH', '需要跨知识库检索', NULL, 'APPROVED', 1, 'admin', '已通过', '2026-08-01 00:00:00', '2026-08-01 00:00:00'),
(2, 'skill-002', 2, 'liugl', 'code.execute', 'CRITICAL', '需要执行用户代码', NULL, 'PENDING', NULL, NULL, NULL, '2026-08-10 00:00:00', '2026-08-10 00:00:00');

-- ============================================================
-- 模块: minimax-prompt  提示词模板
-- ============================================================

INSERT INTO `prompt_template` (id, name, description, category, content, variables, creator_id, creator_name, is_public, use_count, created_at, updated_at) VALUES
(1, '通用助手',      '默认 AI 助手提示词',        'system',   '你是一个专业、友好的 AI 助手。请根据用户问题给出准确、简洁的回答。\n当前时间: ${current_time}\n用户语言: ${language}', 'current_time,language', 1, 'admin', 1, 50, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, '代码助手',      '编程任务专用提示词',         'system',   '你是一个资深程序员。请帮助用户解决编程问题。\n语言偏好: ${language}\n代码风格: ${style}', 'language,style', 1, 'admin', 1, 30, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 'RAG 检索增强',  'RAG 场景下的 LLM 提示词',   'system',   '基于以下上下文信息回答用户问题。\n\n上下文:\n${context}\n\n问题: ${question}\n\n要求: 答案必须仅基于提供的上下文，不要编造信息。', 'context,question', 1, 'admin', 1, 20, '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- ============================================================
-- 模块: minimax-admin  审计日志
-- ============================================================

INSERT INTO `admin_audit_log` (id, actor_id, actor_name, action, resource_type, resource_id, detail, result, ip, created_at) VALUES
(1, 1, 'admin', 'USER_CREATE',   'User',    '2',   '{"username":"liugl","email":"liugl@minimax.io"}',       'SUCCESS', '127.0.0.1', '2026-01-02 00:00:00'),
(2, 1, 'admin', 'MODEL_CREATE',  'Model',   '1',   '{"modelCode":"gpt-4o","providerId":1}',                'SUCCESS', '127.0.0.1', '2026-01-01 00:00:00'),
(3, 1, 'admin', 'RATE_LIMIT',    'Model',   '1',   '{"code":"gpt-4o","capacity":100,"refillPerMin":60}',   'SUCCESS', '127.0.0.1', '2026-01-10 00:00:00'),
(4, 1, 'admin', 'KB_CREATE',     'KnowledgeBase', '1', '{"name":"MiniMax 产品文档"}',                      'SUCCESS', '127.0.0.1', '2026-06-01 00:00:00'),
(5, 1, 'admin', 'PLUGIN_ENABLE', 'Plugin',  '4',   '{"plugin":"code-runner","enabled":true}',             'SUCCESS', '127.0.0.1', '2026-08-01 00:00:00');

-- ============================================================
-- 模块: minimax-ws  WebSocket 协作
-- ============================================================

INSERT INTO `collab_room` (id, name, owner_id, status, max_participants, created_at) VALUES
(1, 'AI 评审室', 2, 1, 5, '2026-08-10 10:00:00');

INSERT INTO `collab_participant` (id, room_id, user_id, role, joined_at) VALUES
(1, 1, 2, 'owner',  '2026-08-10 10:00:00'),
(2, 1, 1, 'member', '2026-08-10 10:05:00');

INSERT INTO `collab_message` (id, room_id, user_id, content, message_type, created_at) VALUES
(1, 1, 2, '开始评审 MiniMax V6.8.2', 'text', '2026-08-10 10:01:00'),
(2, 1, 1, 'RAG SSE 流式效果很好', 'text', '2026-08-10 10:02:00');

-- ============================================================
-- 模块: minimax-analytics  数据分析
-- ============================================================

INSERT INTO `analytics_datasource` (id, name, type, connection_config, status, created_at) VALUES
(1, 'MySQL 主库', 'mysql', '{"host":"localhost","port":3306,"database":"minimax"}', 1, '2026-01-01 00:00:00'),
(2, 'Elasticsearch', 'elasticsearch', '{"host":"localhost","port":9200}', 1, '2026-01-01 00:00:00');

INSERT INTO `analytics_report` (id, name, description, report_type, config, created_by, created_at) VALUES
(1, '日活用户报表', '统计每日活跃用户数', 'daily_active_users', '{"metrics":["active_users","new_users"],"granularity":"day"}', 1, '2026-01-01 00:00:00'),
(2, 'Token 消耗报表', 'Token 使用趋势', 'token_consumption', '{"metrics":["input_tokens","output_tokens"]}', 1, '2026-01-01 00:00:00');

-- ============================================================
-- 模块: minimax-pipeline-fn  函数扩展
-- ============================================================

INSERT INTO `function_tool` (id, name, description, category, input_schema, output_schema, enabled, builtin, created_at) VALUES
(1, 'send_email',   '发送邮件',         'notification', '{"to":"string","subject":"string","body":"string"}', '{"sent":"boolean"}', 1, 1, '2026-01-01 00:00:00'),
(2, 'create_issue', '创建工单',         'workflow',    '{"title":"string","desc":"string","priority":"string"}', '{"id":"string"}', 1, 1, '2026-01-01 00:00:00'),
(3, 'webhook_call', 'Webhook 调用',     'http',        '{"url":"string","method":"string","body":"object"}', '{"status":200}', 1, 1, '2026-01-01 00:00:00');

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 补充：训练任务 + 监控告警事件
-- ============================================================

INSERT INTO `training_task` (id, user_id, model_name, corpus_path, n_layer, n_head, n_embd, block_size, max_iters, batch_size, learning_rate, status, progress, current_iter, created_at) VALUES
(1, 2, 'MiniTransformer-v1', '/data/corpus/ai-text-100k.txt', 4, 4, 128, 128, 1000, 16, 0.05, 'COMPLETED', 100, 1000, '2026-07-01 00:00:00'),
(2, 2, 'MiniTransformer-v2', '/data/corpus/ai-text-500k.txt', 6, 6, 256, 256, 2000, 32, 0.03, 'TRAINING', 45, 900, '2026-08-01 00:00:00');

INSERT INTO `training_metric` (id, task_id, iter, loss, accuracy, progress, lr, gpu_util, vram_gb, created_at) VALUES
(1, 1, 100,  2.453, 0.123, 10, '0.05', 80, 6.5, '2026-07-01 01:00:00'),
(2, 1, 500,  1.234, 0.456, 50, '0.05', 85, 7.0, '2026-07-01 05:00:00'),
(3, 1, 1000, 0.567, 0.789, 100, '0.005', 85, 7.0, '2026-07-02 00:00:00'),
(4, 2, 100,  1.890, 0.345, 5, '0.03', 78, 6.2, '2026-08-01 01:00:00'),
(5, 2, 900,  0.890, 0.678, 45, '0.03', 82, 6.8, '2026-08-10 00:00:00');

INSERT INTO `metric_snapshot` (id, service, metric_name, metric_value, recorded_at) VALUES
(1, 'cs-auth',    'cpu_usage',    45.5, '2026-08-12 00:00:00'),
(2, 'cs-auth',    'memory_usage', 62.3, '2026-08-12 00:00:00'),
(3, 'cs-chat',    'cpu_usage',    72.1, '2026-08-12 00:00:00'),
(4, 'cs-chat',    'memory_usage', 55.0, '2026-08-12 00:00:00'),
(5, 'gateway',    'qps',         125.0, '2026-08-12 00:00:00'),
(6, 'gateway',    'error_rate',   0.5,  '2026-08-12 00:00:00'),
(7, 'cs-model',   'active_tokens', 850000, '2026-08-12 00:00:00'),
(8, 'cs-model',   'quota_usage',  38.5, '2026-08-12 00:00:00');

INSERT INTO `sensitive_word` (id, word, category, level, action, enabled, created_at) VALUES
(1, '作弊',     '违规', 'HIGH', 'BLOCK',  1, '2026-01-01 00:00:00'),
(2, '作弊软件', '违规', 'HIGH', 'BLOCK',  1, '2026-01-01 00:00:00'),
(3, '政治敏感', '合规', 'MEDIUM', 'REVIEW', 1, '2026-01-01 00:00:00'),
(4, '暴力内容', '合规', 'MEDIUM', 'REVIEW', 1, '2026-01-01 00:00:00'),
(5, '色情',     '合规', 'HIGH', 'BLOCK',  1, '2026-01-01 00:00:00');

