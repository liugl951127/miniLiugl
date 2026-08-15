-- V7.0 Flow⑤: Monitor h2local schema
-- AlertEvent 表 (V3.5.30+)
CREATE TABLE IF NOT EXISTS alert_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  rule_id BIGINT,
  rule_name VARCHAR(255),
  severity VARCHAR(32),
  metric_name VARCHAR(128),
  metric_value DECIMAL(16,4),
  threshold DECIMAL(16,4),
  message TEXT,
  status VARCHAR(32),
  fired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP,
  acked_at TIMESTAMP,
  acked_by BIGINT,
  notes TEXT,
  duration BIGINT,
  silenced_until TIMESTAMP,
  session_id VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS alert_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255),
  metric_name VARCHAR(128),
  operator VARCHAR(16),
  threshold DECIMAL(16,4),
  severity VARCHAR(32),
  enabled TINYINT DEFAULT 1,
  cooldown_minutes INT DEFAULT 5,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS metric_snapshot (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  service_name VARCHAR(128),
  metric_name VARCHAR(128),
  value DECIMAL(16,4),
  unit VARCHAR(32),
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
