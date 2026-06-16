-- Function Calling H2 兼容 schema
CREATE TABLE IF NOT EXISTS function_tool (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  description VARCHAR(500) NOT NULL,
  category VARCHAR(32) NOT NULL DEFAULT 'custom',
  scope VARCHAR(16) NOT NULL DEFAULT 'builtin',
  owner_id BIGINT,
  parameters CLOB NOT NULL,
  endpoint VARCHAR(255),
  http_method VARCHAR(8),
  enabled INT NOT NULL DEFAULT 1,
  tags VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS function_call_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  session_id BIGINT,
  tool_name VARCHAR(64) NOT NULL,
  arguments CLOB,
  result CLOB,
  status VARCHAR(16) NOT NULL DEFAULT 'ok',
  error_msg VARCHAR(500),
  duration_ms INT NOT NULL DEFAULT 0,
  ip VARCHAR(64),
  user_agent VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 初始 4 个内置工具
INSERT INTO function_tool (name, display_name, description, category, scope, parameters, endpoint, enabled, tags) VALUES
('get_current_time', '获取当前时间', '返回服务器当前时间, 可选指定时区 (默认 Asia/Shanghai)。', 'system', 'builtin',
 '{"type":"object","properties":{"timezone":{"type":"string","default":"Asia/Shanghai"}},"required":[]}',
 'com.minimax.function.builtin.TimeTool', 1, 'time,system'),

('calculator', '计算器', '执行数学表达式, 支持 + - * / % 括号与数学函数 (sin, cos, sqrt, log, abs, pow)。', 'system', 'builtin',
 '{"type":"object","properties":{"expression":{"type":"string","description":"数学表达式"}},"required":["expression"]}',
 'com.minimax.function.builtin.CalculatorTool', 1, 'math,system'),

('http_get', 'HTTP GET', '对指定 URL 发起 GET 请求, 返回响应体 (前 5000 字符)。', 'system', 'builtin',
 '{"type":"object","properties":{"url":{"type":"string","description":"完整 URL"},"timeout_seconds":{"type":"integer","default":10}},"required":["url"]}',
 'com.minimax.function.builtin.HttpGetTool', 1, 'http,system'),

('random_number', '随机数', '生成指定范围内的随机整数。', 'system', 'builtin',
 '{"type":"object","properties":{"min":{"type":"integer","default":1},"max":{"type":"integer","default":100}},"required":[]}',
 'com.minimax.function.builtin.RandomNumberTool', 1, 'random,system');
