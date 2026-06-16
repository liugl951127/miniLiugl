-- Monitor H2 兼容 schema
CREATE TABLE IF NOT EXISTS metric_snapshot (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  service VARCHAR(32) NOT NULL,
  metric_name VARCHAR(64) NOT NULL,
  metric_value DECIMAL(20,4) NOT NULL,
  tags VARCHAR(500),
  recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS alert_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(255),
  metric_name VARCHAR(64) NOT NULL,
  service VARCHAR(32),
  operator VARCHAR(8) NOT NULL DEFAULT '>',
  threshold DECIMAL(20,4) NOT NULL,
  severity VARCHAR(16) NOT NULL DEFAULT 'warning',
  enabled INT NOT NULL DEFAULT 1,
  cooldown_minutes INT NOT NULL DEFAULT 5,
  notify_channel VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS alert_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  rule_id BIGINT NOT NULL,
  rule_name VARCHAR(64) NOT NULL,
  severity VARCHAR(16) NOT NULL,
  metric_name VARCHAR(64) NOT NULL,
  metric_value DECIMAL(20,4) NOT NULL,
  threshold DECIMAL(20,4) NOT NULL,
  message VARCHAR(500),
  status VARCHAR(16) NOT NULL DEFAULT 'firing',
  fired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP
);

-- 5 默认规则
INSERT INTO alert_rule (name, description, metric_name, service, operator, threshold, severity, cooldown_minutes) VALUES
('CPU 高', 'CPU > 80%', 'cpu_usage', NULL, '>', 80, 'warning', 5),
('JVM Heap 高', 'JVM 堆 > 85%', 'jvm_heap_usage', NULL, '>', 85, 'critical', 5),
('磁盘高', '磁盘 > 90%', 'disk_usage', NULL, '>', 90, 'critical', 10),
('LLM 延迟高', 'LLM 平均延迟 > 3000ms', 'llm_avg_latency', 'model', '>', 3000, 'warning', 3),
('错误率高', 'HTTP 5xx 错误率 > 5', 'http_5xx_rate', NULL, '>', 5, 'critical', 2);
