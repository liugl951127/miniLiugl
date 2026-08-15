-- Model 模块 H2 沙箱数据库初始化
-- V7.0: 使用 DOUBLE 而非 DECIMAL（避免 H2 MySQL mode 兼容问题）
-- V7.0 Flow④: 先 ALTER 添加缺失列（兼容已有数据），再确保表存在

-- 1. ALTER 已有表添加缺失列（已有 H2 数据库需要）
ALTER TABLE model_provider ADD COLUMN IF NOT EXISTS description VARCHAR(512);
ALTER TABLE model_provider ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE model_config ADD COLUMN IF NOT EXISTS description VARCHAR(512);
ALTER TABLE model_config ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 2. CREATE TABLE 仅当表不存在时（全新数据库）
CREATE TABLE IF NOT EXISTS model_provider (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(128),
    base_url VARCHAR(512),
    api_key VARCHAR(512),
    protocol VARCHAR(32),
    description VARCHAR(512),
    enabled INT DEFAULT 0,
    deleted INT DEFAULT 0,
    sort INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS model_config (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    model_code VARCHAR(64) NOT NULL,
    display_name VARCHAR(128),
    max_context INT DEFAULT 4096,
    max_output INT DEFAULT 2048,
    input_price DOUBLE DEFAULT 0,
    output_price DOUBLE DEFAULT 0,
    supports_vision INT DEFAULT 0,
    supports_tools INT DEFAULT 0,
    supports_stream INT DEFAULT 1,
    enabled INT DEFAULT 0,
    deleted INT DEFAULT 0,
    sort INT DEFAULT 0,
    description VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
