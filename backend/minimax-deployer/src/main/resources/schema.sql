-- Agent Forge 数据库 Schema (V2.0)
-- 4 张表: forge_project / forge_release / forge_deployment / agent_template

CREATE TABLE IF NOT EXISTS forge_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    industry VARCHAR(50),
    scenario TEXT,
    raw_requirements TEXT,
    parsed_requirements TEXT,
    recommended_agents TEXT,
    current_release_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    owner_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_forge_project_owner ON forge_project(owner_id);
CREATE INDEX IF NOT EXISTS idx_forge_project_status ON forge_project(status);

CREATE TABLE IF NOT EXISTS forge_release (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    version VARCHAR(20) NOT NULL,
    title VARCHAR(200),
    changelog TEXT,
    agent_definitions TEXT,
    deploy_config TEXT,
    manifests TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    deploy_target VARCHAR(20),
    replicas INT DEFAULT 2,
    image_registry VARCHAR(200),
    image_tag VARCHAR(100),
    deploy_duration INT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deployed_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_forge_release_project ON forge_release(project_id);
CREATE INDEX IF NOT EXISTS idx_forge_release_status ON forge_release(status);

CREATE TABLE IF NOT EXISTS forge_deployment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    release_id BIGINT NOT NULL,
    instance_name VARCHAR(100),
    stages TEXT,
    logs TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    target VARCHAR(20),
    namespace VARCHAR(50),
    running_replicas INT DEFAULT 0,
    desired_replicas INT DEFAULT 2,
    current_qps DOUBLE,
    error_message TEXT,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_forge_deployment_release ON forge_deployment(release_id);
CREATE INDEX IF NOT EXISTS idx_forge_deployment_status ON forge_deployment(status);

CREATE TABLE IF NOT EXISTS agent_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    industry VARCHAR(50),
    description TEXT,
    emoji VARCHAR(10),
    color VARCHAR(200),
    agents TEXT,
    workflow TEXT,
    tools TEXT,
    recommended_model VARCHAR(100),
    usage_count INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_agent_template_industry ON agent_template(industry);
CREATE INDEX IF NOT EXISTS idx_agent_template_status ON agent_template(status);
