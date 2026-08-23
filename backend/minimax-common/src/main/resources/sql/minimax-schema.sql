-- ============================================================
-- MiniMax Platform 全量建表 SQL (V7.2)
-- 生成时间: 2026-08-22
-- 模块: 14 个微服务 (auth/agent/ai/analytics/chat/common/gateway/model/monitor/multimodal/pipeline/rag/system/ws)
-- 表数: 99 + 7 (V7.2 补) = 106 张
-- 命名: 驼峰字段, TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- 字符集: utf8mb4 / 排序: utf8mb4_unicode_ci
-- 说明: 本文件只含 CREATE TABLE, 种子数据见 minimax-seed.sql
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- MiniMax Platform 全量初始化 SQL (V1.0)
-- 生成时间: 2026-08-16
-- 驼峰字段命名，TIMESTAMP 使用 DEFAULT CURRENT_TIMESTAMP

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- [minimax-auth] sys_user
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64),
    password VARCHAR(255),
    nickname VARCHAR(128),
    email VARCHAR(256),
    phone VARCHAR(32),
    avatar VARCHAR(512),
    gender INT,
    status INT,
    lastLoginIp VARCHAR(64),
    lastLoginAt TIMESTAMP NULL,
    tenantId BIGINT,
    remark VARCHAR(512),
    wechatOpenid VARCHAR(128),
    wechatUnionid VARCHAR(128),
    wechatNickname VARCHAR(128),
    wechatAvatar VARCHAR(512),
    wechatBoundAt TIMESTAMP NULL,
    qqOpenid VARCHAR(128),
    qqUnionid VARCHAR(128),
    qqNickname VARCHAR(128),
    qqAvatar VARCHAR(512),
    qqBoundAt TIMESTAMP NULL,
    alipayOpenid VARCHAR(128),
    alipayUserId VARCHAR(128),
    alipayNickname VARCHAR(128),
    alipayAvatar VARCHAR(512),
    alipayBoundAt TIMESTAMP NULL,
    createdBy BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedBy BIGINT,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] sys_role
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64),
    name VARCHAR(128),
    description VARCHAR(512),
    sort INT,
    enabled INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] sys_user_role
CREATE TABLE IF NOT EXISTS sys_user_role (
    userId BIGINT,
    roleId BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] auth_login_log
CREATE TABLE IF NOT EXISTS auth_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    username VARCHAR(64),
    ip VARCHAR(64),
    userAgent VARCHAR(512),
    status INT,
    message VARCHAR(256),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] auth_refresh_token
CREATE TABLE IF NOT EXISTS auth_refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    token VARCHAR(512),
    expiresAt TIMESTAMP,
    revoked INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] notification
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    type VARCHAR(32),
    title VARCHAR(256),
    content TEXT,
    isRead INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] tenant
CREATE TABLE IF NOT EXISTS tenant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64),
    name VARCHAR(128),
    plan VARCHAR(32),
    status INT,
    maxUsers INT,
    maxModels INT,
    qpsLimit INT,
    monthlyQuota BIGINT,
    usedQuota BIGINT,
    expireAt TIMESTAMP NULL,
    contactEmail VARCHAR(256),
    contactPhone VARCHAR(32),
    remark VARCHAR(512),
    dataIsolation TINYINT DEFAULT 1 COMMENT 'V6.9+: 数据隔离标记 1=隔离 0=共享',
    ipWhitelist VARCHAR(1024) COMMENT 'V6.9+: IP 白名单, 多个逗号分隔',
    isDefault INT DEFAULT 0 COMMENT 'V6.9+: 是否默认租户 1=默认',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] unionid_relations
CREATE TABLE IF NOT EXISTS unionid_relations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    unionid VARCHAR(128),
    platform VARCHAR(32),
    firstSeenAt TIMESTAMP,
    lastSeenAt TIMESTAMP,
    bindingCount INT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] user_api_key
CREATE TABLE IF NOT EXISTS user_api_key (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    name VARCHAR(128),
    keyHash VARCHAR(256),
    keyPrefix VARCHAR(16),
    scopes VARCHAR(512),
    expiresAt TIMESTAMP NULL,
    lastUsedAt TIMESTAMP NULL,
    useCount BIGINT,
    enabled INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] wechat_config
CREATE TABLE IF NOT EXISTS wechat_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appType VARCHAR(32),
    appId VARCHAR(128),
    appSecret VARCHAR(256),
    token VARCHAR(64),
    aesKey VARCHAR(256),
    redirectUri VARCHAR(512),
    scope VARCHAR(128),
    enabled INT,
    remark VARCHAR(512),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] wechat_scan_session
CREATE TABLE IF NOT EXISTS wechat_scan_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket VARCHAR(128),
    sceneId VARCHAR(64),
    status VARCHAR(32),
    openid VARCHAR(128),
    unionid VARCHAR(128),
    nickname VARCHAR(128),
    avatar VARCHAR(512),
    userId BIGINT,
    accessToken VARCHAR(512),
    refreshToken VARCHAR(512),
    clientIp VARCHAR(64),
    userAgent VARCHAR(512),
    expiresAt TIMESTAMP,
    confirmedAt TIMESTAMP NULL,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] wechat_user_binding
CREATE TABLE IF NOT EXISTS wechat_user_binding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    openid VARCHAR(128),
    unionid VARCHAR(128),
    appType VARCHAR(32),
    nickname VARCHAR(128),
    avatar VARCHAR(512),
    boundAt TIMESTAMP,
    lastLoginAt TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] oauth_app_config
CREATE TABLE IF NOT EXISTS oauth_app_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform VARCHAR(32),
    appType VARCHAR(32),
    appId VARCHAR(128),
    appSecret VARCHAR(256),
    publicKey TEXT,
    redirectUri VARCHAR(512),
    scopes VARCHAR(512),
    enabled INT,
    extraConfig TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] oauth_binding
CREATE TABLE IF NOT EXISTS oauth_binding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    platform VARCHAR(32),
    appType VARCHAR(32),
    openid VARCHAR(128),
    unionid VARCHAR(128),
    nickname VARCHAR(128),
    avatar VARCHAR(512),
    accessToken TEXT,
    refreshToken VARCHAR(512),
    tokenExpiresAt TIMESTAMP,
    rawData TEXT,
    boundAt TIMESTAMP,
    lastLoginAt TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-model] model_provider
CREATE TABLE IF NOT EXISTS model_provider (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64),
    name VARCHAR(128),
    baseUrl VARCHAR(512),
    apiKey VARCHAR(512),
    protocol VARCHAR(32),
    enabled INT,
    sort INT,
    description VARCHAR(512),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-model] model_config
CREATE TABLE IF NOT EXISTS model_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    providerId BIGINT,
    modelCode VARCHAR(64),
    displayName VARCHAR(128),
    maxContext INT,
    maxOutput INT,
    inputPrice DECIMAL(10,4),
    outputPrice DECIMAL(10,4),
    supportsVision INT,
    supportsTools INT,
    supportsStream INT,
    enabled INT,
    sort INT,
    description VARCHAR(512),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-model] model_battle_log
CREATE TABLE IF NOT EXISTS model_battle_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    battle_id VARCHAR(64),
    user_id BIGINT,
    model_id BIGINT,
    model_code VARCHAR(64),
    prompt TEXT,
    response TEXT,
    prompt_tokens INT,
    completion_tokens INT,
    latency_ms INT,
    status VARCHAR(32),
    error_msg VARCHAR(1024),
    score INT,
    judge_model VARCHAR(64),
    judge_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-model] model_quota
CREATE TABLE IF NOT EXISTS model_quota (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    modelId BIGINT,
    quotaDate DATE,
    usedTokens BIGINT,
    usedRequests INT,
    limitTokens BIGINT,
    limitRequests INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-model] training_task
CREATE TABLE IF NOT EXISTS training_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    modelName VARCHAR(128),
    corpusPath VARCHAR(512),
    nLayer INT,
    nHead INT,
    nEmbd INT,
    blockSize INT,
    maxIters INT,
    batchSize INT,
    learningRate DOUBLE,
    status VARCHAR(32),
    progress INT,
    currentLoss DOUBLE,
    currentIter INT,
    errorMessage TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completedAt TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-model] training_metric
CREATE TABLE IF NOT EXISTS training_metric (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    taskId BIGINT,
    iter INT,
    loss DOUBLE,
    accuracy DOUBLE,
    progress INT,
    lr VARCHAR(32),
    gpuUtil INT,
    vramGb DOUBLE,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] ai_tool
CREATE TABLE IF NOT EXISTS ai_tool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64),
    name VARCHAR(128),
    category VARCHAR(64),
    description TEXT,
    icon VARCHAR(512),
    enabled INT,
    builtin INT,
    inputSchema TEXT,
    outputSchema TEXT,
    defaultConfig TEXT,
    implType VARCHAR(64),
    implValue TEXT,
    rateLimit INT,
    timeoutSeconds INT,
    roleRequired VARCHAR(64),
    tags VARCHAR(512),
    version VARCHAR(32),
    author VARCHAR(128),
    createdBy BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status INT DEFAULT 1,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] ai_intent_keyword
CREATE TABLE IF NOT EXISTS ai_intent_keyword (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    intent VARCHAR(64),
    keyword VARCHAR(128),
    weight INT,
    isRegex INT,
    enabled INT,
    language VARCHAR(16),
    remark VARCHAR(256),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] ai_voting_record
CREATE TABLE IF NOT EXISTS ai_voting_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sessionId VARCHAR(64),
    userId BIGINT,
    username VARCHAR(64),
    question TEXT,
    finalAnswer TEXT,
    strategy VARCHAR(64),
    totalVotes INT,
    agreementRate DECIMAL(5,4),
    modelVotes TEXT,
    durationMs INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notifyEmail VARCHAR(256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] ai_chat_session
CREATE TABLE IF NOT EXISTS ai_chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sessionId VARCHAR(64),
    userId BIGINT,
    username VARCHAR(64),
    title VARCHAR(256),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status INT DEFAULT 1,
    intent VARCHAR(64),
    confidence DOUBLE,
    alternatives TEXT,
    model VARCHAR(64),
    kbId BIGINT,
    kbName VARCHAR(128),
    agentId VARCHAR(64),
    agentName VARCHAR(128),
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] ai_chat_message
CREATE TABLE IF NOT EXISTS ai_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sessionId VARCHAR(64),
    role VARCHAR(16),
    content TEXT,
    toolCode VARCHAR(64),
    toolInput TEXT,
    toolOutput TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] ai_generation_log
CREATE TABLE IF NOT EXISTS ai_generation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    generationId VARCHAR(64),
    userId BIGINT,
    username VARCHAR(64),
    userIp VARCHAR(64),
    modality VARCHAR(16),
    modelName VARCHAR(64),
    modelVersion VARCHAR(64),
    prompt TEXT,
    negativePrompt TEXT,
    parameters TEXT,
    outputUrl VARCHAR(512),
    outputSize BIGINT,
    outputHash VARCHAR(128),
    watermarked INT,
    watermarkText VARCHAR(256),
    durationMs INT,
    status VARCHAR(32),
    errorMsg TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] ai_tool_invocation
CREATE TABLE IF NOT EXISTS ai_tool_invocation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    toolCode VARCHAR(64),
    userId BIGINT,
    username VARCHAR(64),
    inputJson TEXT,
    outputJson TEXT,
    status VARCHAR(32),
    errorMessage TEXT,
    durationMs INT,
    ip VARCHAR(64),
    userAgent VARCHAR(512),
    dataSourceId BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] audit_log
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    traceId VARCHAR(64),
    userId BIGINT,
    username VARCHAR(64),
    userIp VARCHAR(64),
    userAgent VARCHAR(512),
    action VARCHAR(128),
    resourceType VARCHAR(64),
    resourceId VARCHAR(128),
    method VARCHAR(16),
    path VARCHAR(512),
    requestBody TEXT,
    responseStatus INT,
    result VARCHAR(64),
    errorMsg TEXT,
    durationMs INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] billing_record
CREATE TABLE IF NOT EXISTS billing_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recordId VARCHAR(64),
    userId BIGINT,
    licenseId BIGINT,
    modelEntryId BIGINT,
    recordType VARCHAR(32),
    amountCents BIGINT,
    currency VARCHAR(8),
    status VARCHAR(32),
    paymentMethod VARCHAR(32),
    externalTransactionId VARCHAR(128),
    description VARCHAR(512),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] cluster_node
CREATE TABLE IF NOT EXISTS cluster_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nodeId VARCHAR(64),
    name VARCHAR(128),
    address VARCHAR(256),
    region VARCHAR(64),
    zone VARCHAR(64),
    capabilities VARCHAR(512),
    totalCores INT,
    totalMemoryMb BIGINT,
    totalGpus INT,
    cpuUsage DOUBLE,
    memoryUsage DOUBLE,
    gpuUsage DOUBLE,
    activeTasks INT,
    status VARCHAR(32),
    isLeader TINYINT(1),
    lastHeartbeat TIMESTAMP,
    startedAt TIMESTAMP,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] dashboard_metric
CREATE TABLE IF NOT EXISTS dashboard_metric (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric VARCHAR(128),
    dimension VARCHAR(64),
    value DOUBLE,
    tags VARCHAR(512),
    timestamp TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] db_data_source
CREATE TABLE IF NOT EXISTS db_data_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    type VARCHAR(32),
    jdbcUrl VARCHAR(1024),
    username VARCHAR(128),
    password VARCHAR(512),
    driverClass VARCHAR(256),
    poolSize INT,
    minIdle INT,
    maxLifetime INT,
    enabled INT,
    testStatus VARCHAR(32),
    testMessage VARCHAR(512),
    lastTestAt TIMESTAMP,
    description VARCHAR(512),
    tags VARCHAR(512),
    createdBy BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] kb_chunk
CREATE TABLE IF NOT EXISTS kb_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chunkId VARCHAR(64),
    docId VARCHAR(64),
    kbId VARCHAR(64),
    seq INT,
    content TEXT,
    charCount INT,
    tokenCount INT,
    embedding VARCHAR(4096),
    embeddingModel VARCHAR(128),
    keywords VARCHAR(512),
    summary TEXT,
    location VARCHAR(256),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] kb_document
CREATE TABLE IF NOT EXISTS kb_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    docId VARCHAR(64),
    kbId VARCHAR(64),
    filename VARCHAR(256),
    mimeType VARCHAR(128),
    sizeBytes BIGINT,
    sha256 VARCHAR(64),
    filePath VARCHAR(512),
    source VARCHAR(32),
    sourceUrl VARCHAR(1024),
    status VARCHAR(32),
    chunkCount INT,
    embeddingCount INT,
    error TEXT,
    tags VARCHAR(512),
    ownerId BIGINT,
    isPublic TINYINT(1),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] kb_permission
CREATE TABLE IF NOT EXISTS kb_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kbId VARCHAR(64),
    subjectType VARCHAR(32),
    subjectId BIGINT,
    permission VARCHAR(64),
    grantBy BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] log_entry
CREATE TABLE IF NOT EXISTS log_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term BIGINT,
    logIndex BIGINT,
    nodeId VARCHAR(64),
    command TEXT,
    committed TINYINT(1),
    committedAt TIMESTAMP,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] model_license
CREATE TABLE IF NOT EXISTS model_license (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    licenseKey VARCHAR(128),
    modelEntryId BIGINT,
    modelVersionId BIGINT,
    userId BIGINT,
    licenseType VARCHAR(32),
    status VARCHAR(32),
    quotaCalls BIGINT,
    usedCalls BIGINT,
    startAt TIMESTAMP,
    expireAt TIMESTAMP,
    priceCents BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] model_version
CREATE TABLE IF NOT EXISTS model_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    versionId VARCHAR(64),
    modelEntryId BIGINT,
    version VARCHAR(64),
    changelog TEXT,
    filePath VARCHAR(512),
    sizeBytes BIGINT,
    sha256 VARCHAR(64),
    inputSchema TEXT,
    outputSchema TEXT,
    status VARCHAR(32),
    isLatest TINYINT(1),
    uploaderId BIGINT,
    backwardCompatible VARCHAR(16),
    metadata TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] moderation_record
CREATE TABLE IF NOT EXISTS moderation_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    traceId VARCHAR(64),
    userId BIGINT,
    username VARCHAR(64),
    contentType VARCHAR(32),
    contentHash VARCHAR(64),
    contentSize BIGINT,
    contentUrl VARCHAR(1024),
    moderationStatus VARCHAR(32),
    riskLevel VARCHAR(32),
    riskLabels VARCHAR(512),
    riskScore DECIMAL(5,4),
    moderator VARCHAR(64),
    rejectionReason TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] multimedia_file
CREATE TABLE IF NOT EXISTS multimedia_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fileId VARCHAR(64),
    userId BIGINT,
    username VARCHAR(64),
    fileName VARCHAR(256),
    originalName VARCHAR(256),
    fileType VARCHAR(32),
    mimeType VARCHAR(128),
    fileSize BIGINT,
    fileHash VARCHAR(128),
    storagePath VARCHAR(512),
    storageType VARCHAR(32),
    encrypted INT,
    durationMs BIGINT,
    width INT,
    height INT,
    bitrate INT,
    sampleRate INT,
    channels INT,
    codec VARCHAR(64),
    exif TEXT,
    moderationStatus VARCHAR(32),
    moderationId BIGINT,
    watermarked INT,
    isPublic INT,
    accessCount INT,
    expireAt TIMESTAMP,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] pipeline_log
CREATE TABLE IF NOT EXISTS pipeline_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sessionId VARCHAR(64),
    userId BIGINT,
    clientIp VARCHAR(64),
    inputText TEXT,
    inputModality VARCHAR(32),
    intent VARCHAR(64),
    outputText TEXT,
    outputTokens INT,
    computeDevice VARCHAR(32),
    computeMode VARCHAR(32),
    totalCostMs BIGINT,
    stageCosts TEXT,
    riskLevel VARCHAR(32),
    needsReview TINYINT(1),
    ragHits INT,
    toolCalls INT,
    errorMessage TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] push_message
CREATE TABLE IF NOT EXISTS push_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    messageId VARCHAR(64),
    title VARCHAR(256),
    body TEXT,
    icon VARCHAR(512),
    clickAction VARCHAR(512),
    data TEXT,
    targetType VARCHAR(32),
    targetValue VARCHAR(512),
    status VARCHAR(32),
    successCount INT,
    failureCount INT,
    error TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] push_subscription
CREATE TABLE IF NOT EXISTS push_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscriptionId VARCHAR(64),
    userId BIGINT,
    platform VARCHAR(32),
    endpoint VARCHAR(1024),
    p256dhKey VARCHAR(256),
    authKey VARCHAR(64),
    userAgent VARCHAR(512),
    status VARCHAR(32),
    lastActiveAt TIMESTAMP,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] sensitive_word
CREATE TABLE IF NOT EXISTS sensitive_word (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(256),
    category VARCHAR(64),
    level VARCHAR(32),
    action VARCHAR(32),
    enabled INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] training_checkpoint
CREATE TABLE IF NOT EXISTS training_checkpoint (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    taskId VARCHAR(64),
    checkpointId VARCHAR(64),
    name VARCHAR(128),
    epoch INT,
    step INT,
    filePath VARCHAR(512),
    sizeBytes BIGINT,
    sha256 VARCHAR(64),
    valLoss DOUBLE,
    accuracy DOUBLE,
    tags VARCHAR(512),
    metadata TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] training_job
CREATE TABLE IF NOT EXISTS training_job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    taskId VARCHAR(64),
    name VARCHAR(128),
    model VARCHAR(128),
    status VARCHAR(32),
    totalEpochs INT,
    currentEpoch INT,
    currentStep INT,
    startTimeMs BIGINT,
    endTimeMs BIGINT,
    config TEXT,
    error TEXT,
    ownerId BIGINT,
    tags VARCHAR(512),
    lastLoss DOUBLE,
    lastValLoss DOUBLE,
    lastAccuracy DOUBLE,
    totalSteps INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- [minimax-ai] agent_group
CREATE TABLE IF NOT EXISTS agent_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    groupId VARCHAR(64),
    name VARCHAR(128),
    description TEXT,
    strategy VARCHAR(64),
    membersJson TEXT,
    status VARCHAR(32),
    ownerId BIGINT,
    tags VARCHAR(512),
    lastRunAt TIMESTAMP,
    runCount INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] agent_group_member (T1: 群成员细粒度表)
CREATE TABLE IF NOT EXISTS agent_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    agent_code VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'WORKER',
    position INT NOT NULL DEFAULT 0,
    config_json VARCHAR(2000) DEFAULT '',
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group (group_id, position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] agent_task
CREATE TABLE IF NOT EXISTS agent_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    taskId VARCHAR(64),
    userId BIGINT,
    goal TEXT,
    status VARCHAR(32),
    rounds INT,
    result TEXT,
    llmCalls INT,
    toolCalls INT,
    totalTokens INT,
    errorMsg TEXT,
    latencyMs BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] webhook
CREATE TABLE IF NOT EXISTS webhook (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    webhookId VARCHAR(64),
    name VARCHAR(128),
    description TEXT,
    url VARCHAR(1024),
    events VARCHAR(512),
    secret VARCHAR(256),
    customHeaders TEXT,
    enabled INT,
    status VARCHAR(32),
    deliveryCount BIGINT,
    successCount BIGINT,
    failCount BIGINT,
    lastDeliveryAt TIMESTAMP,
    lastStatus INT,
    ownerId BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] webhook_delivery
CREATE TABLE IF NOT EXISTS webhook_delivery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    webhookId VARCHAR(64),
    eventType VARCHAR(64),
    eventId VARCHAR(64),
    payload TEXT,
    responseStatus INT,
    responseBody TEXT,
    durationMs BIGINT,
    status VARCHAR(32),
    retryCount INT,
    errorMsg TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] model_market
CREATE TABLE IF NOT EXISTS model_market (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    modelKey VARCHAR(64),
    name VARCHAR(128),
    description TEXT,
    modelType VARCHAR(64),
    taskType VARCHAR(64),
    baseModel VARCHAR(128),
    version VARCHAR(64),
    filePath VARCHAR(512),
    fileName VARCHAR(256),
    fileSize BIGINT,
    sha256 VARCHAR(64),
    license VARCHAR(256),
    authorId BIGINT,
    authorName VARCHAR(128),
    tags VARCHAR(512),
    metricsJson TEXT,
    status VARCHAR(32),
    downloadCount BIGINT,
    avgRating DOUBLE,
    ratingCount BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    publishedAt TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] model_rating
CREATE TABLE IF NOT EXISTS model_rating (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    modelKey VARCHAR(64),
    userId BIGINT,
    username VARCHAR(64),
    rating INT,
    comment TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] agent_marketplace
CREATE TABLE IF NOT EXISTS agent_marketplace (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_key VARCHAR(64),
    name VARCHAR(128),
    description TEXT,
    category VARCHAR(64),
    icon VARCHAR(512),
    authorId BIGINT,
    authorName VARCHAR(128),
    definitionJson TEXT,
    version VARCHAR(64),
    visibility VARCHAR(32),
    status VARCHAR(32),
    usageCount BIGINT,
    avgRating DOUBLE,
    ratingCount BIGINT,
    tags VARCHAR(512),
    capabilities VARCHAR(512),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    publishedAt TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] agent_rating
CREATE TABLE IF NOT EXISTS agent_rating (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_key VARCHAR(64),
    userId BIGINT,
    username VARCHAR(64),
    rating INT,
    comment TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] license_template
CREATE TABLE IF NOT EXISTS license_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    templateKey VARCHAR(64),
    name VARCHAR(128),
    licenseType VARCHAR(64),
    description TEXT,
    quotaCalls BIGINT,
    quotaDays INT,
    priceCents BIGINT,
    features TEXT,
    limits TEXT,
    isPublic INT,
    isActive INT,
    version INT,
    createdBy BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-agent] kg_entity
CREATE TABLE IF NOT EXISTS kg_entity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    name VARCHAR(256),
    entityType VARCHAR(64),
    description TEXT,
    aliases VARCHAR(512),
    importance INT,
    source VARCHAR(64),
    refCount INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-agent] kg_relation
CREATE TABLE IF NOT EXISTS kg_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    fromEntity BIGINT,
    toEntity BIGINT,
    relationType VARCHAR(64),
    description TEXT,
    weight DECIMAL(10,4),
    source VARCHAR(64),
    refCount INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-agent] plugin
CREATE TABLE IF NOT EXISTS plugin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    displayName VARCHAR(128),
    description TEXT,
    version VARCHAR(32),
    author VARCHAR(128),
    category VARCHAR(64),
    scope VARCHAR(64),
    ownerId BIGINT,
    icon VARCHAR(512),
    entry VARCHAR(512),
    pluginType VARCHAR(32),
    config TEXT,
    enabled INT,
    downloads INT,
    rating DECIMAL(3,2),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-agent] collab_session
CREATE TABLE IF NOT EXISTS collab_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sessionId VARCHAR(64),
    ownerId BIGINT,
    title VARCHAR(256),
    maxUsers INT,
    status VARCHAR(32),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-agent] collab_member
CREATE TABLE IF NOT EXISTS collab_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collabId BIGINT,
    userId BIGINT,
    role VARCHAR(32),
    joinedAt TIMESTAMP,
    lastActiveAt TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-monitor] alert_channel
CREATE TABLE IF NOT EXISTS alert_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    channelType VARCHAR(32),
    type VARCHAR(32),
    target VARCHAR(512),
    config TEXT,
    enabled INT,
    priority INT,
    description TEXT,
    template TEXT,
    createdBy BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-monitor] alert_event
CREATE TABLE IF NOT EXISTS alert_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ruleId BIGINT,
    ruleName VARCHAR(128),
    severity VARCHAR(32),
    metricName VARCHAR(128),
    metricValue DECIMAL(20,4),
    threshold DECIMAL(20,4),
    message TEXT,
    status VARCHAR(32),
    firedAt TIMESTAMP,
    resolvedAt TIMESTAMP,
    ackedAt TIMESTAMP,
    ackedBy BIGINT,
    notes TEXT,
    duration BIGINT,
    silencedUntil TIMESTAMP,
    sessionId VARCHAR(64),
    escalated TINYINT DEFAULT 0 COMMENT 'Day 45: 是否已升级 (true=已触发升级通知)',
    escalatedAt TIMESTAMP NULL COMMENT 'Day 45: 升级时间',
    resolvedBy VARCHAR(64) COMMENT 'Day 46: 自动恢复操作人 (SYSTEM = 自动恢复, 其他 = 用户ID)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-monitor] alert_rule
CREATE TABLE IF NOT EXISTS alert_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    description TEXT,
    metricName VARCHAR(128),
    service VARCHAR(64),
    operator VARCHAR(16),
    threshold DECIMAL(20,4),
    severity VARCHAR(32),
    cooldownMinutes INT,
    enabled INT,
    tags VARCHAR(512),
    notifyChannel VARCHAR(256),
    silencedUntil TIMESTAMP,
    sessionId VARCHAR(64),
    escalateAfterMinutes INT DEFAULT 30 COMMENT 'Day 45: 升级等待分钟数',
    escalationChannel VARCHAR(256) COMMENT 'Day 45: 升级通知渠道 (逗号分隔, 如 DINGTALK,EMAIL)',
    autoResolveMinutes INT DEFAULT 60 COMMENT 'Day 45: 自动恢复分钟数 (超过自动 resolved)',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-monitor] metric_snapshot
CREATE TABLE IF NOT EXISTS metric_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service VARCHAR(64),
    metricName VARCHAR(128),
    metricValue DECIMAL(20,4),
    tags VARCHAR(512),
    recordedAt TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-chat] chat_session
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    title VARCHAR(256),
    model VARCHAR(64),
    systemPrompt TEXT,
    temperature DECIMAL(4,2),
    status INT,
    messageCount INT,
    lastMessageAt TIMESTAMP,
    tenantId BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-chat] chat_message
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sessionId BIGINT,
    userId BIGINT,
    role VARCHAR(16),
    content TEXT,
    tokens INT,
    finishReason VARCHAR(32),
    errorMessage TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ws] collab_room
CREATE TABLE IF NOT EXISTS collab_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id VARCHAR(64),
    name VARCHAR(128),
    type VARCHAR(32),
    owner_id BIGINT,
    owner_name VARCHAR(128),
    description TEXT,
    is_public INT,
    max_participants INT,
    status VARCHAR(32),
    current_participants INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP,
    closed_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ws] collab_participant
CREATE TABLE IF NOT EXISTS collab_participant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id VARCHAR(64),
    user_id BIGINT,
    username VARCHAR(64),
    nickname VARCHAR(128),
    avatar VARCHAR(512),
    role VARCHAR(32),
    cursor_x INT,
    cursor_y INT,
    selection_id VARCHAR(64),
    status VARCHAR(32),
    joined_at TIMESTAMP,
    left_at TIMESTAMP,
    last_heartbeat TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ws] collab_message
CREATE TABLE IF NOT EXISTS collab_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id VARCHAR(64),
    user_id BIGINT,
    username VARCHAR(64),
    nickname VARCHAR(128),
    type VARCHAR(32),
    content TEXT,
    metadata TEXT,
    client_msg_id VARCHAR(64),
    broadcast INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-rag] document
CREATE TABLE IF NOT EXISTS document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kbId BIGINT,
    ownerId BIGINT,
    title VARCHAR(256),
    sourceType VARCHAR(32),
    sourceUri VARCHAR(1024),
    content LONGTEXT,
    sizeBytes BIGINT,
    status VARCHAR(32),
    errorMsg TEXT,
    chunkCount INT,
    checksum VARCHAR(64),
    tags VARCHAR(512),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-rag] document_chunk
CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    docId BIGINT,
    kbId BIGINT,
    ownerId BIGINT,
    chunkIndex INT,
    content TEXT,
    embedding MEDIUMBLOB,
    dim INT,
    charCount INT,
    startPos INT,
    endPos INT,
    accessCount INT,
    lastAccessAt TIMESTAMP,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-rag] knowledge_base
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ownerId BIGINT,
    tenantId BIGINT,
    name VARCHAR(128),
    description TEXT,
    visibility VARCHAR(32),
    docCount INT,
    chunkCount INT,
    tags VARCHAR(512),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-rag] kb_extracted_entity
-- 知识库自动抽取的实体 (EntityExtractor 启发式产出)
CREATE TABLE IF NOT EXISTS kb_extracted_entity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL DEFAULT 'CONCEPT',
    freq INT NOT NULL DEFAULT 1,
    source_doc_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_kb (kb_id, freq),
    INDEX idx_name (name),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-rag] kb_extracted_relation
-- 知识库自动抽取的关系 (CO_OCCUR 同段共现 / MENTION 提及 / RELATED 关联)
CREATE TABLE IF NOT EXISTS kb_extracted_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_id BIGINT NOT NULL,
    src_entity VARCHAR(128) NOT NULL,
    rel VARCHAR(32) NOT NULL,
    tgt_entity VARCHAR(128) NOT NULL,
    weight INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_kb_rel (kb_id, src_entity, tgt_entity),
    INDEX idx_src (src_entity),
    INDEX idx_tgt (tgt_entity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-pipeline] pipeline_workflow
CREATE TABLE IF NOT EXISTS pipeline_workflow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    description TEXT,
    definition LONGTEXT,
    version INT,
    status INT,
    createBy BIGINT,
    updateBy BIGINT,
    createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updateTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-pipeline] pipeline_workflow_version
CREATE TABLE IF NOT EXISTS pipeline_workflow_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflowId BIGINT,
    version INT,
    definition LONGTEXT,
    changeLog TEXT,
    createBy BIGINT,
    createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-pipeline] pipeline_run
CREATE TABLE IF NOT EXISTS pipeline_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflowId BIGINT,
    workflowName VARCHAR(128),
    status VARCHAR(32),
    triggerBy BIGINT,
    triggerType VARCHAR(32),
    definitionSnapshot LONGTEXT,
    startTime TIMESTAMP,
    endTime TIMESTAMP,
    durationMs BIGINT,
    errorMessage TEXT,
    resultSummary TEXT,
    createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-pipeline] pipeline_node_log
CREATE TABLE IF NOT EXISTS pipeline_node_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    runId BIGINT,
    nodeId VARCHAR(64),
    nodeType VARCHAR(64),
    nodeName VARCHAR(128),
    status VARCHAR(32),
    startTime TIMESTAMP,
    endTime TIMESTAMP,
    durationMs BIGINT,
    inputRows INT,
    outputRows INT,
    outputPreview TEXT,
    errorMessage TEXT,
    configSnapshot TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-pipeline] function_tool
CREATE TABLE IF NOT EXISTS function_tool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    displayName VARCHAR(128),
    description TEXT,
    category VARCHAR(64),
    scope VARCHAR(32),
    ownerId BIGINT,
    parameters TEXT,
    endpoint VARCHAR(1024),
    httpMethod VARCHAR(16),
    enabled INT,
    tags VARCHAR(512),
    riskLevel VARCHAR(32),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-pipeline] function_call_log
CREATE TABLE IF NOT EXISTS function_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    sessionId BIGINT,
    toolName VARCHAR(128),
    arguments TEXT,
    result TEXT,
    status VARCHAR(32),
    errorMsg TEXT,
    durationMs INT,
    ip VARCHAR(64),
    userAgent VARCHAR(512),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-pipeline] skill_approval
CREATE TABLE IF NOT EXISTS skill_approval (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    taskId VARCHAR(64),
    userId BIGINT,
    username VARCHAR(64),
    toolName VARCHAR(128),
    riskLevel VARCHAR(32),
    goal TEXT,
    toolParams TEXT,
    status VARCHAR(32),
    approverId BIGINT,
    approverName VARCHAR(128),
    reason TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-admin] admin_audit_log
CREATE TABLE IF NOT EXISTS admin_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actorId BIGINT,
    actorName VARCHAR(128),
    action VARCHAR(128),
    resourceType VARCHAR(64),
    resourceId VARCHAR(128),
    detail TEXT,
    result VARCHAR(64),
    errorMsg TEXT,
    ip VARCHAR(64),
    userAgent VARCHAR(512),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-admin] audit_log_full
CREATE TABLE IF NOT EXISTS audit_log_full (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    traceId VARCHAR(64),
    userId BIGINT,
    username VARCHAR(64),
    userIp VARCHAR(64),
    userAgent VARCHAR(512),
    action VARCHAR(128),
    resourceType VARCHAR(64),
    resourceId VARCHAR(128),
    method VARCHAR(16),
    path VARCHAR(512),
    requestBody TEXT,
    responseStatus INT,
    result VARCHAR(64),
    errorMsg TEXT,
    durationMs INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-common] async_task
CREATE TABLE IF NOT EXISTS async_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    taskId VARCHAR(64),
    taskType VARCHAR(64),
    status VARCHAR(32),
    params TEXT,
    result TEXT,
    errorMsg TEXT,
    retryCount INT,
    latencyMs BIGINT,
    submitterId BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    startedAt TIMESTAMP,
    finishedAt TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-common] request_log
CREATE TABLE IF NOT EXISTS request_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    traceId VARCHAR(64),
    method VARCHAR(16),
    path VARCHAR(512),
    queryString TEXT,
    clientIp VARCHAR(64),
    userAgent VARCHAR(512),
    userId BIGINT,
    status INT,
    latencyMs BIGINT,
    slow TINYINT(1),
    error TINYINT(1),
    module VARCHAR(64),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-common] rate_limit_rule
CREATE TABLE IF NOT EXISTS rate_limit_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scope VARCHAR(32),
    rule_key VARCHAR(128),
    description VARCHAR(256),
    capacity INT,
    refill_tokens INT,
    period_seconds INT,
    enabled INT,
    priority INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-analytics] analytics_datasource
CREATE TABLE IF NOT EXISTS analytics_datasource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    name VARCHAR(128),
    type VARCHAR(32),
    jdbcUrl VARCHAR(1024),
    username VARCHAR(128),
    passwordEnc VARCHAR(512),
    description VARCHAR(512),
    deleted INT DEFAULT 0,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-analytics] analytics_nlsql_history
CREATE TABLE IF NOT EXISTS analytics_nlsql_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    dataSourceId BIGINT,
    question TEXT,
    generatedSql TEXT,
    correctedSql TEXT,
    model VARCHAR(64),
    promptTokens INT,
    completionTokens INT,
    durationMs BIGINT,
    success TINYINT(1) DEFAULT 1,
    errorMessage TEXT,
    feedbackRating INT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-analytics] analytics_ingest_task
CREATE TABLE IF NOT EXISTS analytics_ingest_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    taskId VARCHAR(64),
    filename VARCHAR(256),
    fileType VARCHAR(32),
    encoding VARCHAR(32) DEFAULT 'UTF-8',
    separator VARCHAR(8),
    fileSize BIGINT,
    status VARCHAR(32) DEFAULT 'PENDING',
    errorMessage TEXT,
    qualityJson TEXT,
    totalRows BIGINT,
    totalColumns INT,
    columnsJson TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finishedAt TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-analytics] analytics_report
CREATE TABLE IF NOT EXISTS analytics_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    reportId VARCHAR(64),
    title VARCHAR(256),
    question TEXT,
    sqlText TEXT,
    markdown LONGTEXT,
    chartOptionsJson TEXT,
    rowCount BIGINT,
    durationMs BIGINT,
    format VARCHAR(32) DEFAULT 'markdown',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 演示数据 (demo 数据库, SQL 执行器动态连接)
-- ============================================================

-- [demo] demo_user
CREATE TABLE IF NOT EXISTS demo_user (
    userId BIGINT,
    userName VARCHAR(64),
    realName VARCHAR(128),
    email VARCHAR(256),
    phone VARCHAR(32),
    gender VARCHAR(8),
    age INT,
    city VARCHAR(64),
    level INT DEFAULT 1,
    balance DECIMAL(12,2) DEFAULT 0.00,
    status VARCHAR(16) DEFAULT 'ACTIVE',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [demo] demo_category
CREATE TABLE IF NOT EXISTS demo_category (
    categoryId BIGINT,
    categoryName VARCHAR(128),
    parentId BIGINT,
    sortOrder INT DEFAULT 0,
    iconUrl VARCHAR(512),
    description VARCHAR(512),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [demo] demo_product
CREATE TABLE IF NOT EXISTS demo_product (
    productId BIGINT,
    productName VARCHAR(256),
    categoryId BIGINT,
    brand VARCHAR(128),
    price DECIMAL(10,2),
    cost DECIMAL(10,2),
    stock INT DEFAULT 0,
    soldCount INT,
    rating DECIMAL(3,2),
    reviewCount INT,
    tags VARCHAR(512),
    status VARCHAR(16) DEFAULT 'ONLINE',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [demo] demo_order
CREATE TABLE IF NOT EXISTS demo_order (
    orderId VARCHAR(32),
    userId BIGINT,
    orderStatus VARCHAR(32) DEFAULT 'PENDING',
    totalAmount DECIMAL(12,2),
    discountAmount DECIMAL(10,2) DEFAULT 0.00,
    payAmount DECIMAL(12,2),
    payMethod VARCHAR(32),
    payTime TIMESTAMP,
    shippingFee DECIMAL(8,2) DEFAULT 0.00,
    receiverName VARCHAR(128),
    receiverPhone VARCHAR(32),
    receiverAddress VARCHAR(512),
    remark VARCHAR(512),
    orderDate DATE,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [demo] demo_order_item
CREATE TABLE IF NOT EXISTS demo_order_item (
    itemId BIGINT,
    orderId VARCHAR(32),
    productId BIGINT,
    productName VARCHAR(256),
    categoryId BIGINT,
    skuCode VARCHAR(64),
    unitPrice DECIMAL(10,2),
    quantity INT,
    totalAmount DECIMAL(12,2),
    discountAmount DECIMAL(10,2) DEFAULT 0.00,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [demo] demo_payment
CREATE TABLE IF NOT EXISTS demo_payment (
    paymentId VARCHAR(64),
    orderId VARCHAR(32),
    userId BIGINT,
    amount DECIMAL(12,2),
    payMethod VARCHAR(32),
    transactionId VARCHAR(128),
    payStatus VARCHAR(16) DEFAULT 'SUCCESS',
    payTime TIMESTAMP,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [V7.1] multi-agent collaboration session (Planner → Executor → Critic loop)
CREATE TABLE IF NOT EXISTS agent_execution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    executionId VARCHAR(64) NOT NULL,
    userId BIGINT,
    goal TEXT NOT NULL COMMENT 'user goal/prompt',
    status VARCHAR(32) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING / COMPLETED / FAILED',
    maxRounds INT DEFAULT 10,
    completedRounds INT DEFAULT 0,
    criticPassed INT DEFAULT 0 COMMENT '1=passed, 0=failed',
    finalScore INT COMMENT 'critic final score 0-10',
    totalPromptTokens BIGINT DEFAULT 0,
    totalCompletionTokens BIGINT DEFAULT 0,
    totalTokens BIGINT DEFAULT 0,
    plannerTokens BIGINT DEFAULT 0 COMMENT 'planner LLM tokens',
    criticTokens BIGINT DEFAULT 0 COMMENT 'critic LLM tokens',
    totalLatencyMs BIGINT DEFAULT 0,
    result TEXT COMMENT 'final execution result',
    errorMsg TEXT,
    startedAt TIMESTAMP,
    finishedAt TIMESTAMP,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_execution_id (executionId),
    KEY idx_user_id (userId),
    KEY idx_status (status),
    KEY idx_created_at (createdAt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [V7.1] execution steps (Planner / Executor / Critic)
CREATE TABLE IF NOT EXISTS agent_execution_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    executionId VARCHAR(64) NOT NULL,
    stepId VARCHAR(64) NOT NULL COMMENT 'unique within execution, e.g. step-1',
    phase VARCHAR(16) NOT NULL COMMENT 'PLANNER / EXECUTOR / CRITIC',
    roundNum INT DEFAULT 1 COMMENT 'which round this step belongs to',
    stepIndex INT COMMENT 'step order within phase',
    status VARCHAR(16) DEFAULT 'RUNNING' COMMENT 'RUNNING / PASSED / FAILED',
    score INT COMMENT 'critic score 0-10 (only for CRITIC phase)',
    feedback TEXT COMMENT 'critic feedback / improvement suggestion',
    output TEXT COMMENT 'step output (plan JSON / execution result)',
    durationMs BIGINT,
    promptTokens BIGINT DEFAULT 0,
    completionTokens BIGINT DEFAULT 0,
    totalTokens BIGINT DEFAULT 0,
    errorMsg TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_step (executionId, stepId),
    KEY idx_execution_id (executionId),
    KEY idx_phase (phase)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [V7.1] token usage per LLM call (supports token-update SSE events)
CREATE TABLE IF NOT EXISTS agent_token_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    executionId VARCHAR(64) NOT NULL,
    stepId VARCHAR(64),
    caller VARCHAR(32) NOT NULL COMMENT 'PLANNER / CRITIC / EXECUTOR',
    modelCode VARCHAR(64),
    promptTokens BIGINT DEFAULT 0,
    completionTokens BIGINT DEFAULT 0,
    totalTokens BIGINT DEFAULT 0,
    latencyMs BIGINT DEFAULT 0,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    KEY idx_execution_id (executionId),
    KEY idx_caller (caller)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [V7.1] critic evaluation history (radar chart multi-dimension scoring)
CREATE TABLE IF NOT EXISTS agent_evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    executionId VARCHAR(64) NOT NULL,
    stepId VARCHAR(64),
    roundNum INT DEFAULT 1,
    phase VARCHAR(16) DEFAULT 'CRITIC',
    dimension VARCHAR(32) NOT NULL COMMENT 'ACCURACY / RELEVANCE / SAFETY / EFFICIENCY / COMPLETION',
    score DECIMAL(5,2) DEFAULT 0 COMMENT 'dimension score 0-10',
    overallScore INT DEFAULT 0 COMMENT 'overall score 0-10',
    passed INT DEFAULT 0 COMMENT '1=passed, 0=failed',
    reasoning TEXT COMMENT 'critic reasoning',
    suggestion TEXT COMMENT 'improvement suggestion for next round',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    KEY idx_execution_id (executionId),
    KEY idx_dimension (dimension)

-- ============================================================
-- [V7.2] 新增 7 张表 (P0 修复)
-- ============================================================

-- [minimax-chat/memory_ext] long-term memory (用户长期记忆)
CREATE TABLE IF NOT EXISTS memory_long_term (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    sessionId BIGINT,
    content TEXT,
    summary VARCHAR(512),
    role VARCHAR(32),
    embedding BLOB,
    dim INT,
    importance DECIMAL(5,2) DEFAULT 0,
    tags VARCHAR(512),
    accessCount INT DEFAULT 0,
    lastAccessAt TIMESTAMP NULL,
    expiresAt TIMESTAMP NULL,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    KEY idx_userId (userId),
    KEY idx_sessionId (sessionId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-chat/memory_ext] user preferences (用户偏好记忆)
CREATE TABLE IF NOT EXISTS memory_user_pref (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    prefKey VARCHAR(128) NOT NULL,
    prefValue TEXT,
    weight DECIMAL(5,2) DEFAULT 1.0,
    source VARCHAR(64),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_user_prefKey (userId, prefKey),
    KEY idx_userId (userId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-model/prompt] prompt templates (提示词模板)
CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    category VARCHAR(64),
    content TEXT,
    variables VARCHAR(1024),
    creatorId BIGINT,
    creatorName VARCHAR(128),
    isPublic TINYINT DEFAULT 0,
    useCount INT DEFAULT 0,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    KEY idx_category (category),
    KEY idx_creatorId (creatorId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai/cluster/raft] raft log (分布式一致性日志)
CREATE TABLE IF NOT EXISTS raft_log (
    idx BIGINT AUTO_INCREMENT PRIMARY KEY,
    term BIGINT NOT NULL,
    commandType VARCHAR(64),
    commandPayload TEXT,
    timestampMs BIGINT NOT NULL,
    KEY idx_term (term),
    KEY idx_timestampMs (timestampMs)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] ai_raft_log (Raft 日志条目, 与 cluster/raft 区分)
CREATE TABLE IF NOT EXISTS ai_raft_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term BIGINT NOT NULL DEFAULT 0,
    logIndex BIGINT NOT NULL,
    nodeId VARCHAR(64) NOT NULL,
    command TEXT,
    committed TINYINT DEFAULT 0,
    committedAt TIMESTAMP NULL,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_term_idx (term, logIndex),
    KEY idx_node (nodeId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] user preferences (用户偏好设置, 主题/语言等)
CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    theme VARCHAR(32) DEFAULT 'light',
    language VARCHAR(16) DEFAULT 'zh-CN',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_userId (userId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-agent] knowledge graph entity (知识图谱实体, 为未来切 DB 预留)
CREATE TABLE IF NOT EXISTS kg_entity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    name VARCHAR(128) NOT NULL,
    entityType VARCHAR(64),
    description VARCHAR(512),
    aliases VARCHAR(512),
    importance INT DEFAULT 0,
    source VARCHAR(64),
    refCount INT DEFAULT 0,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    KEY idx_userId (userId),
    KEY idx_name (name),
    KEY idx_entityType (entityType)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-agent] knowledge graph relation (知识图谱关系, 为未来切 DB 预留)
CREATE TABLE IF NOT EXISTS kg_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    fromEntity BIGINT NOT NULL,
    toEntity BIGINT NOT NULL,
    relationType VARCHAR(64),
    description VARCHAR(512),
    weight DECIMAL(5,2) DEFAULT 1.0,
    source VARCHAR(64),
    refCount INT DEFAULT 0,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    KEY idx_userId (userId),
    KEY idx_fromEntity (fromEntity),
    KEY idx_toEntity (toEntity),
    KEY idx_relationType (relationType)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [V7.2] data_source 表名 alias (兼容 DbDataSource 实体)
CREATE TABLE IF NOT EXISTS data_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    type VARCHAR(32),
    jdbcUrl VARCHAR(1024),
    username VARCHAR(128),
    password VARCHAR(512),
    driverClass VARCHAR(256),
    poolSize INT,
    minIdle INT,
    maxLifetime INT,
    enabled INT,
    testStatus VARCHAR(32),
    testMessage VARCHAR(512),
    lastTestAt TIMESTAMP,
    description VARCHAR(512),
    tags VARCHAR(512),
    createdBy BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- [T1-backend-apis / P0] 5 new tables for 7 new API endpoints
-- Modules:
--   rule_definition     (minimax-pipeline, /api/v1/rule)
--   trained_model       (minimax-ai,        /api/v1/training/models)
--   notification_settings (minimax-auth,    /api/v1/notification/settings)
--   collab_invite       (minimax-ai,        /api/v1/collab/rooms/{id}/invite)
--   system_settings     (minimax-system,    /api/v1/system/settings)
-- ============================================================

-- [minimax-pipeline] rule_definition
CREATE TABLE IF NOT EXISTS rule_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    jsonContent LONGTEXT NOT NULL,
    scope VARCHAR(64) DEFAULT 'GLOBAL',
    enabled INT DEFAULT 1,
    createdBy BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    KEY idx_name (name),
    KEY idx_scope (scope),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] trained_model
CREATE TABLE IF NOT EXISTS trained_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    accuracy DECIMAL(6,3) DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'ENABLED / DISABLED / DRAFT',
    publishedAt TIMESTAMP NULL,
    createdBy BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_code (code),
    KEY idx_status (status),
    KEY idx_publishedAt (publishedAt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-auth] notification_settings
CREATE TABLE IF NOT EXISTS notification_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT NOT NULL,
    channels VARCHAR(256) DEFAULT 'email,webhook' COMMENT 'email/sms/dingtalk/webhook/push',
    events VARCHAR(256) DEFAULT 'login,error,alert,system' COMMENT 'login/error/alert/system',
    quietStart VARCHAR(8) DEFAULT '22:00',
    quietEnd VARCHAR(8) DEFAULT '08:00',
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_userId (userId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-ai] collab_invite
CREATE TABLE IF NOT EXISTS collab_invite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    roomId BIGINT NOT NULL,
    inviterId BIGINT NOT NULL,
    inviteeEmail VARCHAR(256) NOT NULL,
    inviteeUserId BIGINT,
    token VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / ACCEPTED / EXPIRED',
    expiresAt TIMESTAMP NULL,
    acceptedAt TIMESTAMP NULL,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_token (token),
    KEY idx_roomId (roomId),
    KEY idx_inviteeEmail (inviteeEmail),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [minimax-system] system_settings (single-row table, id=1)
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    siteName VARCHAR(128) DEFAULT 'MiniMax 平台',
    siteLogo VARCHAR(512),
    maintenanceMode INT DEFAULT 0 COMMENT '0=normal 1=maintenance',
    allowRegister INT DEFAULT 1 COMMENT '0=disable 1=enable',
    defaultModelCode VARCHAR(64) DEFAULT 'gpt-4o',
    description VARCHAR(512),
    contactEmail VARCHAR(256),
    updatedBy BIGINT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- [minimax-deployer] Agent Forge V2.0 (智能体群流水线)
-- ============================================
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

-- V4.0: Agent 子表 (替代 agent_definitions JSON 字符串)
CREATE TABLE IF NOT EXISTS forge_agent (
  id BIGINT NOT NULL AUTO_INCREMENT,
  release_id BIGINT NOT NULL,
  project_id BIGINT DEFAULT NULL,
  name VARCHAR(64) NOT NULL,
  role VARCHAR(128) DEFAULT NULL,
  emoji VARCHAR(16) DEFAULT NULL,
  description VARCHAR(512) DEFAULT NULL,
  color VARCHAR(256) DEFAULT NULL,
  tools VARCHAR(512) DEFAULT NULL,
  model VARCHAR(64) DEFAULT NULL,
  sort_order INT DEFAULT 0,
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_release_id (release_id),
  KEY idx_project_id (project_id),
  KEY idx_name (name)
);

-- V4.0: Workflow Step 子表
CREATE TABLE IF NOT EXISTS forge_workflow_step (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT DEFAULT NULL,
  release_id BIGINT DEFAULT NULL,
  step_no INT NOT NULL,
  name VARCHAR(128) NOT NULL,
  type VARCHAR(32) DEFAULT 'agent',
  agent_id BIGINT DEFAULT NULL,
  remark VARCHAR(256) DEFAULT NULL,
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_project_id (project_id),
  KEY idx_release_id (release_id)
);

-- V4.0: Manifest 子表 (替代 manifests JSON 字符串)
CREATE TABLE IF NOT EXISTS forge_manifest (
  id BIGINT NOT NULL AUTO_INCREMENT,
  release_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  path VARCHAR(256) NOT NULL,
  content MEDIUMTEXT,
  content_hash VARCHAR(64) DEFAULT NULL,
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_release_id (release_id),
  KEY idx_type (type)
);

-- V4.0: Deployment Log 子表 (替代 logs TEXT)
CREATE TABLE IF NOT EXISTS forge_deployment_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  deployment_id BIGINT NOT NULL,
  level VARCHAR(16) DEFAULT 'INFO',
  stage VARCHAR(64) DEFAULT NULL,
  message VARCHAR(1024) DEFAULT NULL,
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_deployment_id (deployment_id),
  KEY idx_created_at (created_at)
);

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

