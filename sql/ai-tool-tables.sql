-- =============================================================
-- MiniMax AI 工具配置系统 (V2.5)
-- 数据库表设计
-- =============================================================

-- 1. AI 工具定义表 (企业级工具注册)
CREATE TABLE IF NOT EXISTS ai_tool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL UNIQUE COMMENT '工具唯一编码',
    name VARCHAR(128) NOT NULL COMMENT '工具名称',
    category VARCHAR(32) NOT NULL COMMENT '分类: DATA_CLEAN / DATA_ANALYZE / CODE_GEN / CHAT / SQL_QUERY / CUSTOM',
    description TEXT COMMENT '工具描述',
    icon VARCHAR(255) COMMENT '图标 URL',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用 0否 1是',
    builtin TINYINT DEFAULT 0 COMMENT '是否内置 0否 1是',

    -- 工具定义 (JSON Schema,定义输入参数)
    input_schema JSON COMMENT 'JSON Schema: {"type":"object","properties":{...}}',
    -- 输出格式定义
    output_schema JSON COMMENT '输出 schema',
    -- 默认配置
    default_config JSON COMMENT '默认配置',

    -- 实现方式: java / sql / prompt
    impl_type VARCHAR(16) NOT NULL DEFAULT 'java' COMMENT 'java / sql / prompt / http',
    -- 实现类名 (java) / SQL (sql) / Prompt (prompt) / HTTP URL (http)
    impl_value TEXT NOT NULL COMMENT '实现类/SQL/Prompt/URL',

    -- 限流
    rate_limit INT DEFAULT 100 COMMENT '每分钟调用次数',
    -- 超时 (秒)
    timeout_seconds INT DEFAULT 30,

    -- 权限: 谁可以调用
    role_required VARCHAR(128) DEFAULT 'USER' COMMENT '角色: USER / ADMIN / SUPER_ADMIN',

    -- 元数据
    tags VARCHAR(255) COMMENT '标签, 逗号分隔',
    version VARCHAR(16) DEFAULT '1.0.0',
    author VARCHAR(64) COMMENT '作者',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    INDEX idx_code (code),
    INDEX idx_category (category),
    INDEX idx_enabled (enabled, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 工具定义表';

-- 2. 数据源连接表 (支持多种数据库)
CREATE TABLE IF NOT EXISTS data_source (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL UNIQUE COMMENT '数据源名称',
    type VARCHAR(32) NOT NULL COMMENT '类型: mysql/postgresql/oracle/sqlserver/mongodb/clickhouse/doris',
    jdbc_url VARCHAR(512) NOT NULL COMMENT 'JDBC URL',
    username VARCHAR(128),
    password VARCHAR(255) COMMENT '加密存储',
    driver_class VARCHAR(255) COMMENT '驱动类 (可自动推断)',

    -- 连接池配置
    pool_size INT DEFAULT 10,
    min_idle INT DEFAULT 2,
    max_lifetime INT DEFAULT 1800 COMMENT '秒',

    -- 状态
    enabled TINYINT DEFAULT 1,
    test_status VARCHAR(16) DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN / OK / FAILED',
    test_message TEXT,
    last_test_at TIMESTAMP NULL,

    -- 元数据
    description TEXT,
    tags VARCHAR(255),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    INDEX idx_name (name),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源连接';

-- 3. AI 工具调用记录 (审计)
CREATE TABLE IF NOT EXISTS ai_tool_invocation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tool_code VARCHAR(64) NOT NULL,
    user_id BIGINT,
    username VARCHAR(64),

    -- 输入输出
    input_json TEXT,
    output_json LONGTEXT,

    -- 状态
    status VARCHAR(16) NOT NULL COMMENT 'SUCCESS / FAILED / TIMEOUT',
    error_message TEXT,

    -- 性能
    duration_ms INT,

    -- 元数据
    ip VARCHAR(64),
    user_agent VARCHAR(255),
    data_source_id BIGINT COMMENT '关联数据源',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_tool (tool_code, created_at),
    INDEX idx_user (user_id, created_at),
    INDEX idx_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 工具调用审计';

-- 4. AI 对话会话 (类似 chat 但走 AI 工具)
CREATE TABLE IF NOT EXISTS ai_chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    username VARCHAR(64),
    title VARCHAR(255) COMMENT '会话标题',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    INDEX idx_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 聊天会话';

-- 5. AI 对话消息
CREATE TABLE IF NOT EXISTS ai_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL COMMENT 'user / assistant / tool',
    content LONGTEXT,
    tool_code VARCHAR(64) COMMENT '调用的工具',
    tool_input TEXT,
    tool_output LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_session (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话消息';

-- 6. 数据分析任务 (统计 / 趋势 / 异常检测)
CREATE TABLE IF NOT EXISTS ai_analysis_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL COMMENT 'STATS / TREND / ANOMALY / CORRELATION / DISTRIBUTION',
    data_source_id BIGINT NOT NULL,
    table_name VARCHAR(128),
    column_name VARCHAR(128),
    config JSON COMMENT '分析配置 (时间窗口/分组等)',
    schedule VARCHAR(64) COMMENT 'cron 表达式',

    -- 上次结果
    last_result LONGTEXT,
    last_run_at TIMESTAMP NULL,
    last_duration_ms INT,
    last_status VARCHAR(16),

    enabled TINYINT DEFAULT 1,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据分析任务';

-- =============================================================
-- 内置 AI 工具初始化 (V2.5 默认提供)
-- =============================================================

INSERT INTO ai_tool (code, name, category, description, impl_type, impl_value, builtin, enabled, input_schema, default_config, tags) VALUES

-- 数据清洗
('data.clean.missing', '缺失值填充', 'DATA_CLEAN', '自动检测并填充缺失值 (均值/中位数/众数/0)', 'java', 'com.minimax.ai.tool.builtin.MissingValueTool', 1, 1,
'{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"column":{"type":"string"},"strategy":{"type":"string","enum":["mean","median","mode","zero"]}}}',
'{"strategy":"mean"}', 'data,clean,missing,builtin'),

('data.clean.deduplicate', '数据去重', 'DATA_CLEAN', '根据指定列去除重复行', 'java', 'com.minimax.ai.tool.builtin.DeduplicateTool', 1, 1,
'{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"columns":{"type":"array","items":{"type":"string"}}}}',
'{}', 'data,clean,dedup,builtin'),

-- 数据分析
('data.analyze.stats', '描述统计', 'DATA_ANALYZE', '对数值列计算 count/min/max/mean/std/quartiles', 'java', 'com.minimax.ai.tool.builtin.StatsTool', 1, 1,
'{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"column":{"type":"string"}}}',
'{}', 'data,analyze,stats,builtin'),

('data.analyze.trend', '趋势分析', 'DATA_ANALYZE', '时间序列趋势 + 同比环比', 'java', 'com.minimax.ai.tool.builtin.TrendTool', 1, 1,
'{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"timeColumn":{"type":"string"},"valueColumn":{"type":"string"}}}',
'{"interval":"day","periods":7}', 'data,analyze,trend,builtin'),

('data.analyze.anomaly', '异常检测', 'DATA_ANALYZE', '基于 Z-Score / IQR 检测异常值', 'java', 'com.minimax.ai.tool.builtin.AnomalyTool', 1, 1,
'{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"column":{"type":"string"},"method":{"type":"string","enum":["zscore","iqr"]},"threshold":{"type":"number"}}}',
'{"method":"zscore","threshold":3.0}', 'data,analyze,anomaly,builtin'),

('data.analyze.distribution', '分布分析', 'DATA_ANALYZE', '直方图 + 分位数', 'java', 'com.minimax.ai.tool.builtin.DistributionTool', 1, 1,
'{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"column":{"type":"string"},"bins":{"type":"number"}}}',
'{"bins":10}', 'data,analyze,distribution,builtin'),

-- 代码生成
('code.gen.from-schema', '从表结构生成代码', 'CODE_GEN', '根据数据库表结构自动生成 Spring Boot CRUD', 'java', 'com.minimax.ai.codegen.SchemaCodeGenTool', 1, 1,
'{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"framework":{"type":"string","enum":["spring-boot","mybatis-plus"]}}}',
'{"framework":"spring-boot"}', 'code,gen,schema,builtin'),

-- SQL 查询
('sql.query', '自然语言转 SQL', 'SQL_QUERY', '用中文描述查询,自动生成 SQL 并执行', 'java', 'com.minimax.ai.tool.builtin.Nl2SqlTool', 1, 1,
'{"type":"object","properties":{"dataSourceId":{"type":"number"},"question":{"type":"string"}}}',
'{}', 'sql,nl2sql,builtin'),

-- 通用聊天
('chat.assistant', 'AI 聊天助手', 'CHAT', '基于自研 AI 的对话助手', 'java', 'com.minimax.ai.tool.builtin.ChatAssistantTool', 1, 1,
'{"type":"object","properties":{"message":{"type":"string"},"sessionId":{"type":"string"}}}',
'{}', 'chat,assistant,builtin')
;

-- =============================================================
-- 初始化数据源示例
-- =============================================================
INSERT INTO data_source (name, type, jdbc_url, username, password, description) VALUES
('MiniMax 主库', 'mysql', 'jdbc:mysql://mysql:3306/minimax_platform?useSSL=false&serverTimezone=UTC', 'root', 'root123456', 'MiniMax 平台主数据库'),
('本地 H2', 'h2', 'jdbc:h2:mem:testdb', 'sa', '', '本地测试库')
ON DUPLICATE KEY UPDATE updated_at = NOW();