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
  id BIGINT AUTO_INCREMENT,
  session_id VARCHAR(64) NOT NULL,
  role VARCHAR(32),
  content TEXT,
  tool_code VARCHAR(64),
  tool_input TEXT,
  tool_output TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ai_chat_session (
  id BIGINT AUTO_INCREMENT,
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
  kb_id BIGINT DEFAULT 0,
  kb_name VARCHAR(255),
  agent_id VARCHAR(128),
  agent_name VARCHAR(255),
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