-- Agent Schema Part 1: agent_task + collab_* tables (V7.0)
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

