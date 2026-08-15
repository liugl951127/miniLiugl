-- Agent Schema Part 2: kg_* + plugin + function_tool (V7.0)
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
  description TEXT,
  category VARCHAR(32),
  scope VARCHAR(32),
  owner_id BIGINT NOT NULL,
  parameters TEXT,
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
  description TEXT,
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

-- Seed plugin (V7.0)
INSERT INTO plugin (id, name, display_name, description, version, author, category, scope, owner_id, plugin_type, enabled, downloads, rating)
VALUES
  (1, 'assistant-agent', 'Assistant Agent', 'General AI agent with ReAct reasoning', 1, 1, 'agent', 'user', 1, 'builtin', 1, 0, 5.000000);


INSERT INTO function_tool (id, name, display_name, description, category, scope, owner_id, parameters, endpoint, http_method, enabled, tags, risk_level)
VALUES
  (1, 'assistant-agent', 'Assistant Agent', 'General AI agent with ReAct reasoning', 'agent', 'user', 1,
   '{"type":"object","properties":{"task":{"type":"string","description":"Task description"}},"required":["task"]}',
   'builtin:assistant-agent', 'POST', 1, 'AI,Agent', 'MEDIUM'),

  (2, 'search_knowledge', 'Knowledge Search', 'Search knowledge base for relevant info', 'rag', 'agent', 1,
   '{"type":"object","properties":{"query":{"type":"string","description":"Search query"}},"required":["query"]}',
   'builtin:search_knowledge', 'POST', 1, 'KB,RAG', 'LOW'),

  (3, 'calculator', 'Calculator', 'Execute math calculations', 'utility', 'agent', 1,
   '{"type":"object","properties":{"expression":{"type":"string","description":"Math expression"}},"required":["expression"]}',
   'builtin:calculator', 'POST', 1, 'Math,Tool', 'SAFE'),

  (4, 'web_search', 'Web Search', 'Search the internet for latest info', 'search', 'agent', 1,
   '{"type":"object","properties":{"query":{"type":"string","description":"Search query"}},"required":["query"]}',
   'builtin:web_search', 'POST', 1, 'Search,Web', 'MEDIUM'),

  (5, 'weather', 'Weather Query', 'Query weather for a city', 'lifestyle', 'agent', 1,
   '{"type":"object","properties":{"city":{"type":"string","description":"City name"}},"required":["city"]}',
   'builtin:weather', 'GET', 1, 'Weather,Life', 'SAFE'),

  (6, 'code_interpreter', 'Code Interpreter', 'Execute Python code snippets', 'developer', 'agent', 1,
   '{"type":"object","properties":{"code":{"type":"string","description":"Python code"}},"required":["code"]}',
   'builtin:code_interpreter', 'POST', 1, 'Code,Dev', 'HIGH');

