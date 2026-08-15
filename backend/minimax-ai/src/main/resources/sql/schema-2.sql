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