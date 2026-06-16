-- Admin H2 兼容 schema
CREATE TABLE IF NOT EXISTS admin_audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor_id BIGINT NOT NULL,
  actor_name VARCHAR(64),
  action VARCHAR(64) NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  resource_id VARCHAR(64),
  detail CLOB,
  result VARCHAR(16) NOT NULL DEFAULT 'ok',
  error_msg VARCHAR(500),
  ip VARCHAR(64),
  user_agent VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
