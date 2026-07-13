-- ===================================================================
-- MiniMax Platform V3.0.0 全量 DDL (单文件汇总)
-- 表数: 89
-- 字符集: utf8mb4 / 引擎: InnoDB
-- ===================================================================

-- CREATE DATABASE IF NOT EXISTS `minimax_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SET NAMES utf8mb4;
-- SET FOREIGN_KEY_CHECKS = 0;

-- AdminAuditLog -> admin_audit_log
CREATE TABLE IF NOT EXISTS `admin_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `actorId` BIGINT NOT NULL DEFAULT 0 COMMENT 'actorId(actorId)',
    `actorName` VARCHAR(255) DEFAULT NULL COMMENT 'actorName(actorName)',
    `action` VARCHAR(255) DEFAULT NULL COMMENT '操作类型(action)',
    `resourceType` VARCHAR(255) DEFAULT NULL COMMENT '资源类型(resourceType)',
    `resourceId` VARCHAR(255) DEFAULT NULL COMMENT '资源ID(resourceId)',
    `detail` TEXT DEFAULT NULL COMMENT '详情(detail)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT '处理结果(result)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT '错误信息(errorMsg)',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'IP地址(ip)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT '用户代理(userAgent)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AdminAuditLog (auto-generated V3.0.0)';

-- AgentGroup -> agent_group
CREATE TABLE IF NOT EXISTS `agent_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `groupId` VARCHAR(255) DEFAULT NULL COMMENT '分组ID(groupId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `strategy` VARCHAR(255) DEFAULT NULL COMMENT '策略(strategy)',
    `membersJson` VARCHAR(255) DEFAULT NULL COMMENT 'membersJson(membersJson)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    `lastRunAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastRunAt(lastRunAt)',
    `runCount` INT NOT NULL DEFAULT 0 COMMENT 'runCount(runCount)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AgentGroup (auto-generated V3.0.0)';

-- MarketplaceAgent -> agent_marketplace
CREATE TABLE IF NOT EXISTS `agent_marketplace` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `agentKey` VARCHAR(255) DEFAULT NULL COMMENT 'agentKey(agentKey)',
    `name` TEXT DEFAULT NULL COMMENT '名称(name)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT '分类(category)',
    `icon` BIGINT NOT NULL DEFAULT 0 COMMENT '图标(icon)',
    `authorId` VARCHAR(255) DEFAULT NULL COMMENT 'authorId(authorId)',
    `definitionJson` VARCHAR(255) DEFAULT NULL COMMENT 'definitionJson(definitionJson)',
    `version` VARCHAR(255) DEFAULT NULL COMMENT '乐观锁版本号(version)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `usageCount` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'usageCount(usageCount)',
    `avgRating` BIGINT NOT NULL DEFAULT 0 COMMENT 'avgRating(avgRating)',
    `ratingCount` VARCHAR(255) DEFAULT NULL COMMENT 'ratingCount(ratingCount)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    `capabilities` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '能力列表(capabilities)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `publishedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间(publishedAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_marketplace_agentKey` (`agentKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MarketplaceAgent (auto-generated V3.0.0)';

-- AgentRating -> agent_rating
CREATE TABLE IF NOT EXISTS `agent_rating` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `agentKey` VARCHAR(255) DEFAULT NULL COMMENT 'agentKey(agentKey)',
    `userId` VARCHAR(255) DEFAULT NULL COMMENT '用户ID(userId)',
    `username` INT NOT NULL DEFAULT 0 COMMENT '用户名(username)',
    `rating` VARCHAR(255) DEFAULT NULL COMMENT '评分(rating)',
    `comment` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论(comment)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_rating_agentKey` (`agentKey`),
    UNIQUE KEY `uk_agent_rating_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AgentRating (auto-generated V3.0.0)';

-- AgentTask -> agent_task
CREATE TABLE IF NOT EXISTS `agent_task` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT '任务ID(taskId)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `goal` VARCHAR(255) DEFAULT NULL COMMENT '目标(goal)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `rounds` INT NOT NULL DEFAULT 0 COMMENT 'rounds(rounds)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT '处理结果(result)',
    `llmCalls` INT NOT NULL DEFAULT 0 COMMENT 'llmCalls(llmCalls)',
    `toolCalls` INT NOT NULL DEFAULT 0 COMMENT 'toolCalls(toolCalls)',
    `totalTokens` INT NOT NULL DEFAULT 0 COMMENT '总Token数(totalTokens)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT '错误信息(errorMsg)',
    `latencyMs` BIGINT NOT NULL DEFAULT 0 COMMENT '延迟(毫秒)(latencyMs)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_task_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AgentTask (auto-generated V3.0.0)';

-- AiChatMessage -> ai_chat_message
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `sessionId` VARCHAR(255) DEFAULT NULL COMMENT '会话ID(sessionId)',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)',
    `content` TEXT DEFAULT NULL COMMENT '内容(content)',
    `toolCode` VARCHAR(255) DEFAULT NULL COMMENT 'toolCode(toolCode)',
    `toolInput` VARCHAR(255) DEFAULT NULL COMMENT 'toolInput(toolInput)',
    `toolOutput` VARCHAR(255) DEFAULT NULL COMMENT 'toolOutput(toolOutput)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiChatMessage (auto-generated V3.0.0)';

-- AiChatSession -> ai_chat_session
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `sessionId` VARCHAR(255) DEFAULT NULL COMMENT '会话ID(sessionId)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT '用户名(username)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '标题(title)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_chat_session_userId` (`userId`),
    UNIQUE KEY `uk_ai_chat_session_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiChatSession (auto-generated V3.0.0)';

-- AiGenerationLog -> ai_generation_log
CREATE TABLE IF NOT EXISTS `ai_generation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `generationId` VARCHAR(255) DEFAULT NULL COMMENT 'generationId(generationId)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT '用户名(username)',
    `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp(userIp)',
    `modality` VARCHAR(255) DEFAULT NULL COMMENT 'modality(modality)',
    `modelName` VARCHAR(255) DEFAULT NULL COMMENT '模型名称(modelName)',
    `modelVersion` VARCHAR(255) DEFAULT NULL COMMENT '模型版本(modelVersion)',
    `prompt` VARCHAR(255) DEFAULT NULL COMMENT '提示词(prompt)',
    `negativePrompt` VARCHAR(255) DEFAULT NULL COMMENT '反向提示词(negativePrompt)',
    `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters(parameters)',
    `outputUrl` VARCHAR(255) DEFAULT NULL COMMENT 'outputUrl(outputUrl)',
    `outputSize` BIGINT NOT NULL DEFAULT 0 COMMENT 'outputSize(outputSize)',
    `outputHash` VARCHAR(255) DEFAULT NULL COMMENT '输出哈希(outputHash)',
    `watermarked` INT NOT NULL DEFAULT 0 COMMENT 'watermarked(watermarked)',
    `watermarkText` VARCHAR(255) DEFAULT NULL COMMENT 'watermarkText(watermarkText)',
    `durationMs` INT NOT NULL DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT '错误信息(errorMsg)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_generation_log_userId` (`userId`),
    UNIQUE KEY `uk_ai_generation_log_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiGenerationLog (auto-generated V3.0.0)';

-- AiIntentKeyword -> ai_intent_keyword
CREATE TABLE IF NOT EXISTS `ai_intent_keyword` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `intent` VARCHAR(255) DEFAULT NULL COMMENT '意图(intent)',
    `keyword` VARCHAR(255) DEFAULT NULL COMMENT '搜索关键词(keyword)',
    `weight` INT NOT NULL DEFAULT 0 COMMENT '权重(weight)',
    `isRegex` INT NOT NULL DEFAULT 0 COMMENT 'isRegex(isRegex)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    `language` VARCHAR(255) DEFAULT NULL COMMENT '语言(language)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注(remark)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiIntentKeyword (auto-generated V3.0.0)';

-- AiTool -> ai_tool
CREATE TABLE IF NOT EXISTS `ai_tool` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdBy` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID(createdBy)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `code` VARCHAR(255) DEFAULT NULL COMMENT '编码(code)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT '分类(category)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '图标(icon)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    `builtin` INT NOT NULL DEFAULT 0 COMMENT 'builtin(builtin)',
    `inputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'inputSchema(inputSchema)',
    `outputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'outputSchema(outputSchema)',
    `defaultConfig` VARCHAR(255) DEFAULT NULL COMMENT 'defaultConfig(defaultConfig)',
    `implType` VARCHAR(255) DEFAULT NULL COMMENT 'implType(implType)',
    `implValue` VARCHAR(255) DEFAULT NULL COMMENT 'implValue(implValue)',
    `rateLimit` INT NOT NULL DEFAULT 0 COMMENT 'rateLimit(rateLimit)',
    `timeoutSeconds` INT NOT NULL DEFAULT 0 COMMENT 'timeoutSeconds(timeoutSeconds)',
    `roleRequired` VARCHAR(255) DEFAULT NULL COMMENT 'roleRequired(roleRequired)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    `version` VARCHAR(255) DEFAULT NULL COMMENT '乐观锁版本号(version)',
    `author` VARCHAR(255) DEFAULT NULL COMMENT 'author(author)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiTool (auto-generated V3.0.0)';

-- AiToolInvocation -> ai_tool_invocation
CREATE TABLE IF NOT EXISTS `ai_tool_invocation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `toolCode` VARCHAR(255) DEFAULT NULL COMMENT 'toolCode(toolCode)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT '用户名(username)',
    `inputJson` VARCHAR(255) DEFAULT NULL COMMENT 'inputJson(inputJson)',
    `outputJson` VARCHAR(255) DEFAULT NULL COMMENT 'outputJson(outputJson)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT '错误信息(errorMessage)',
    `durationMs` INT NOT NULL DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'IP地址(ip)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT '用户代理(userAgent)',
    `dataSourceId` BIGINT NOT NULL DEFAULT 0 COMMENT 'dataSourceId(dataSourceId)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_tool_invocation_userId` (`userId`),
    UNIQUE KEY `uk_ai_tool_invocation_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AiToolInvocation (auto-generated V3.0.0)';

-- AlertChannel -> alert_channel
CREATE TABLE IF NOT EXISTS `alert_channel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdBy` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID(createdBy)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `channelType` VARCHAR(255) DEFAULT NULL COMMENT 'channelType(channelType)',
    `config` TEXT DEFAULT NULL COMMENT '配置JSON(config)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级(priority)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AlertChannel (auto-generated V3.0.0)';

-- AlertEvent -> alert_event
CREATE TABLE IF NOT EXISTS `alert_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `ruleId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ruleId(ruleId)',
    `ruleName` VARCHAR(255) DEFAULT NULL COMMENT 'ruleName(ruleName)',
    `severity` VARCHAR(255) DEFAULT NULL COMMENT '严重程度(severity)',
    `metricName` VARCHAR(255) DEFAULT NULL COMMENT '指标名称(metricName)',
    `message` VARCHAR(255) DEFAULT NULL COMMENT '消息内容(message)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `resolvedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '解决时间(resolvedAt)',
    `ackedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'ackedAt(ackedAt)',
    `ackedBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'ackedBy(ackedBy)',
    `duration` BIGINT NOT NULL DEFAULT 0 COMMENT '持续时长(秒)(duration)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AlertEvent (auto-generated V3.0.0)';

-- AlertRule -> alert_rule
CREATE TABLE IF NOT EXISTS `alert_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `metricName` VARCHAR(255) DEFAULT NULL COMMENT '指标名称(metricName)',
    `service` VARCHAR(255) DEFAULT NULL COMMENT 'service(service)',
    `operator` VARCHAR(255) DEFAULT NULL COMMENT 'operator(operator)',
    `threshold` DECIMAL(20,4) DEFAULT NULL COMMENT '阈值(threshold)',
    `severity` VARCHAR(255) DEFAULT NULL COMMENT '严重程度(severity)',
    `cooldownMinutes` INT NOT NULL DEFAULT 0 COMMENT 'cooldownMinutes(cooldownMinutes)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    `notifyChannel` VARCHAR(255) DEFAULT NULL COMMENT 'notifyChannel(notifyChannel)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AlertRule (auto-generated V3.0.0)';

-- DataSource -> analytics_datasource
CREATE TABLE IF NOT EXISTS `analytics_datasource` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `passwordEnc` VARCHAR(255) DEFAULT NULL COMMENT 'passwordEnc(passwordEnc)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_analytics_datasource_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DataSource (auto-generated V3.0.0)';

-- IngestTask -> analytics_ingest_task
CREATE TABLE IF NOT EXISTS `analytics_ingest_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT '任务ID(taskId)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `finishedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'finishedAt(finishedAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_analytics_ingest_task_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IngestTask (auto-generated V3.0.0)';

-- Nl2SqlHistory -> analytics_nlsql_history
CREATE TABLE IF NOT EXISTS `analytics_nlsql_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `dataSourceId` BIGINT NOT NULL DEFAULT 0 COMMENT 'dataSourceId(dataSourceId)',
    `completionTokens` INT NOT NULL DEFAULT 0 COMMENT 'completionTokens(completionTokens)',
    `durationMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `success` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否成功(0否1是)(success)',
    `feedbackRating` INT NOT NULL DEFAULT 0 COMMENT 'feedbackRating(feedbackRating)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_analytics_nlsql_history_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nl2SqlHistory (auto-generated V3.0.0)';

-- Report -> analytics_report
CREATE TABLE IF NOT EXISTS `analytics_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `reportId` VARCHAR(255) DEFAULT NULL COMMENT 'reportId(reportId)',
    `format` VARCHAR(255) DEFAULT NULL COMMENT '格式(format)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_analytics_report_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Report (auto-generated V3.0.0)';

-- AsyncTask -> async_task
CREATE TABLE IF NOT EXISTS `async_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `startedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间(startedAt)',
    `finishedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'finishedAt(finishedAt)',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT '任务ID(taskId)',
    `taskType` VARCHAR(255) DEFAULT NULL COMMENT '任务类型(taskType)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `params` VARCHAR(255) DEFAULT NULL COMMENT '参数JSON(params)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT '处理结果(result)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT '错误信息(errorMsg)',
    `retryCount` INT NOT NULL DEFAULT 0 COMMENT '重试次数(retryCount)',
    `latencyMs` BIGINT NOT NULL DEFAULT 0 COMMENT '延迟(毫秒)(latencyMs)',
    `submitterId` BIGINT NOT NULL DEFAULT 0 COMMENT 'submitterId(submitterId)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AsyncTask (auto-generated V3.0.0)';

-- AuditLog -> audit_log
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `traceId` VARCHAR(255) DEFAULT NULL COMMENT '链路追踪ID(traceId)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT '用户名(username)',
    `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp(userIp)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT '用户代理(userAgent)',
    `action` VARCHAR(255) DEFAULT NULL COMMENT '操作类型(action)',
    `resourceType` VARCHAR(255) DEFAULT NULL COMMENT '资源类型(resourceType)',
    `resourceId` VARCHAR(255) DEFAULT NULL COMMENT '资源ID(resourceId)',
    `method` VARCHAR(255) DEFAULT NULL COMMENT '请求方法(method)',
    `path` VARCHAR(255) DEFAULT NULL COMMENT '路径(path)',
    `requestBody` VARCHAR(255) DEFAULT NULL COMMENT '请求体(requestBody)',
    `responseStatus` INT NOT NULL DEFAULT 0 COMMENT '响应状态(responseStatus)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT '处理结果(result)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT '错误信息(errorMsg)',
    `durationMs` INT NOT NULL DEFAULT 0 COMMENT 'durationMs(durationMs)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_audit_log_userId` (`userId`),
    UNIQUE KEY `uk_audit_log_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AuditLog (auto-generated V3.0.0)';

-- AuditLogFull -> audit_log_full
CREATE TABLE IF NOT EXISTS `audit_log_full` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `traceId` VARCHAR(255) DEFAULT NULL COMMENT '链路追踪ID(traceId)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT '用户名(username)',
    `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp(userIp)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT '用户代理(userAgent)',
    `action` VARCHAR(255) DEFAULT NULL COMMENT '操作类型(action)',
    `resourceType` VARCHAR(255) DEFAULT NULL COMMENT '资源类型(resourceType)',
    `resourceId` VARCHAR(255) DEFAULT NULL COMMENT '资源ID(resourceId)',
    `method` VARCHAR(255) DEFAULT NULL COMMENT '请求方法(method)',
    `path` VARCHAR(255) DEFAULT NULL COMMENT '路径(path)',
    `requestBody` VARCHAR(255) DEFAULT NULL COMMENT '请求体(requestBody)',
    `responseStatus` INT NOT NULL DEFAULT 0 COMMENT '响应状态(responseStatus)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT '处理结果(result)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT '错误信息(errorMsg)',
    `durationMs` INT NOT NULL DEFAULT 0 COMMENT 'durationMs(durationMs)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_audit_log_full_userId` (`userId`),
    UNIQUE KEY `uk_audit_log_full_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AuditLogFull (auto-generated V3.0.0)';

-- AuthLoginLog -> auth_login_log
CREATE TABLE IF NOT EXISTS `auth_login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT '用户名(username)',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'IP地址(ip)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT '用户代理(userAgent)',
    `status` INT NOT NULL DEFAULT 0 COMMENT '状态(0禁用1启用)(status)',
    `message` VARCHAR(255) DEFAULT NULL COMMENT '消息内容(message)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_auth_login_log_userId` (`userId`),
    UNIQUE KEY `uk_auth_login_log_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AuthLoginLog (auto-generated V3.0.0)';

-- AuthRefreshToken -> auth_refresh_token
CREATE TABLE IF NOT EXISTS `auth_refresh_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `token` VARCHAR(255) DEFAULT NULL COMMENT '令牌(token)',
    `expiresAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '过期时间(expiresAt)',
    `revoked` INT NOT NULL DEFAULT 0 COMMENT 'revoked(revoked)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_auth_refresh_token_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AuthRefreshToken (auto-generated V3.0.0)';

-- BillingRecord -> billing_record
CREATE TABLE IF NOT EXISTS `billing_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `recordId` VARCHAR(255) DEFAULT NULL COMMENT 'recordId(recordId)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `licenseId` BIGINT NOT NULL DEFAULT 0 COMMENT 'licenseId(licenseId)',
    `modelEntryId` BIGINT NOT NULL DEFAULT 0 COMMENT 'modelEntryId(modelEntryId)',
    `recordType` VARCHAR(255) DEFAULT NULL COMMENT 'recordType(recordType)',
    `amountCents` BIGINT NOT NULL DEFAULT 0 COMMENT 'amountCents(amountCents)',
    `currency` VARCHAR(255) DEFAULT NULL COMMENT '币种(currency)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `paymentMethod` VARCHAR(255) DEFAULT NULL COMMENT 'paymentMethod(paymentMethod)',
    `externalTransactionId` VARCHAR(255) DEFAULT NULL COMMENT 'externalTransactionId(externalTransactionId)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_billing_record_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BillingRecord (auto-generated V3.0.0)';

-- ChatMessage -> chat_message
CREATE TABLE IF NOT EXISTS `chat_message` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `sessionId` BIGINT NOT NULL DEFAULT 0 COMMENT '会话ID(sessionId)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)',
    `content` TEXT DEFAULT NULL COMMENT '内容(content)',
    `tokens` INT NOT NULL DEFAULT 0 COMMENT 'tokens(tokens)',
    `finishReason` VARCHAR(255) DEFAULT NULL COMMENT 'finishReason(finishReason)',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT '错误信息(errorMessage)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_message_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ChatMessage (auto-generated V3.0.0)';

-- ChatSession -> chat_session
CREATE TABLE IF NOT EXISTS `chat_session` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '标题(title)',
    `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)',
    `systemPrompt` VARCHAR(255) DEFAULT NULL COMMENT '系统提示词(systemPrompt)',
    `temperature` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '温度(temperature)',
    `status` INT NOT NULL DEFAULT 0 COMMENT '状态(0禁用1启用)(status)',
    `messageCount` INT NOT NULL DEFAULT 0 COMMENT 'messageCount(messageCount)',
    `lastMessageAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastMessageAt(lastMessageAt)',
    `tenantId` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID(tenantId)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_session_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ChatSession (auto-generated V3.0.0)';

-- ClusterNode -> cluster_node
CREATE TABLE IF NOT EXISTS `cluster_node` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `nodeId` VARCHAR(255) DEFAULT NULL COMMENT '节点ID(nodeId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '地址(address)',
    `region` VARCHAR(255) DEFAULT NULL COMMENT '区域(region)',
    `zone` VARCHAR(255) DEFAULT NULL COMMENT '可用区(zone)',
    `capabilities` VARCHAR(255) DEFAULT NULL COMMENT '能力列表(capabilities)',
    `totalCores` INT NOT NULL DEFAULT 0 COMMENT 'totalCores(totalCores)',
    `totalMemoryMb` BIGINT NOT NULL DEFAULT 0 COMMENT 'totalMemoryMb(totalMemoryMb)',
    `totalGpus` INT NOT NULL DEFAULT 0 COMMENT 'totalGpus(totalGpus)',
    `cpuUsage` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'CPU使用(cpuUsage)',
    `memoryUsage` DOUBLE NOT NULL DEFAULT 0.0 COMMENT '内存使用(memoryUsage)',
    `gpuUsage` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'gpuUsage(gpuUsage)',
    `activeTasks` INT NOT NULL DEFAULT 0 COMMENT 'activeTasks(activeTasks)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `isLeader` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'isLeader(isLeader)',
    `lastHeartbeat` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastHeartbeat(lastHeartbeat)',
    `startedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间(startedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ClusterNode (auto-generated V3.0.0)';

-- CollabMember -> collab_member
CREATE TABLE IF NOT EXISTS `collab_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `collabId` BIGINT NOT NULL DEFAULT 0 COMMENT 'collabId(collabId)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)',
    `joinedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'joinedAt(joinedAt)',
    `lastActiveAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastActiveAt(lastActiveAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collab_member_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CollabMember (auto-generated V3.0.0)';

-- CollabMessage -> collab_message
CREATE TABLE IF NOT EXISTS `collab_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `roomId` VARCHAR(255) DEFAULT NULL COMMENT '房间ID(roomId)',
    `userId` VARCHAR(255) DEFAULT NULL COMMENT '用户ID(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT '用户名(username)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT '内容(content)',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT '元数据JSON(metadata)',
    `clientMsgId` INT NOT NULL DEFAULT 0 COMMENT 'clientMsgId(clientMsgId)',
    `broadcast` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'broadcast(broadcast)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collab_message_roomId` (`roomId`),
    UNIQUE KEY `uk_collab_message_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CollabMessage (auto-generated V3.0.0)';

-- CollabParticipant -> collab_participant
CREATE TABLE IF NOT EXISTS `collab_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `roomId` VARCHAR(255) DEFAULT NULL COMMENT '房间ID(roomId)',
    `userId` VARCHAR(255) DEFAULT NULL COMMENT '用户ID(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT '用户名(username)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT '昵称(nickname)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL(avatar)',
    `role` INT NOT NULL DEFAULT 0 COMMENT 'role(role)',
    `cursorX` INT NOT NULL DEFAULT 0 COMMENT 'cursorX(cursorX)',
    `cursorY` VARCHAR(255) DEFAULT NULL COMMENT 'cursorY(cursorY)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `joinedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'joinedAt(joinedAt)',
    `leftAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'leftAt(leftAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collab_participant_roomId` (`roomId`),
    UNIQUE KEY `uk_collab_participant_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CollabParticipant (auto-generated V3.0.0)';

-- CollabRoom -> collab_room
CREATE TABLE IF NOT EXISTS `collab_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `roomId` VARCHAR(255) DEFAULT NULL COMMENT '房间ID(roomId)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `ownerId` VARCHAR(255) DEFAULT NULL COMMENT 'ownerId(ownerId)',
    `ownerName` TEXT DEFAULT NULL COMMENT 'ownerName(ownerName)',
    `description` INT NOT NULL DEFAULT 0 COMMENT '描述(description)',
    `isPublic` INT NOT NULL DEFAULT 0 COMMENT '是否公开(0否1是)(isPublic)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `currentParticipants` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'currentParticipants(currentParticipants)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `lastActivityAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastActivityAt(lastActivityAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collab_room_roomId` (`roomId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CollabRoom (auto-generated V3.0.0)';

-- CollabSession -> collab_session
CREATE TABLE IF NOT EXISTS `collab_session` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `sessionId` VARCHAR(255) DEFAULT NULL COMMENT '会话ID(sessionId)',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '标题(title)',
    `maxUsers` INT NOT NULL DEFAULT 0 COMMENT 'maxUsers(maxUsers)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CollabSession (auto-generated V3.0.0)';

-- DashboardMetric -> dashboard_metric
CREATE TABLE IF NOT EXISTS `dashboard_metric` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'timestamp(timestamp)',
    `metric` VARCHAR(255) DEFAULT NULL COMMENT '指标JSON(metric)',
    `dimension` VARCHAR(255) DEFAULT NULL COMMENT '维度(dimension)',
    `value` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'value(value)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DashboardMetric (auto-generated V3.0.0)';

-- DbDataSource -> data_source
CREATE TABLE IF NOT EXISTS `data_source` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdBy` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID(createdBy)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `jdbcUrl` VARCHAR(255) DEFAULT NULL COMMENT 'jdbcUrl(jdbcUrl)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT '用户名(username)',
    `password` VARCHAR(255) DEFAULT NULL COMMENT '密码(BCrypt)(password)',
    `driverClass` VARCHAR(255) DEFAULT NULL COMMENT 'driverClass(driverClass)',
    `poolSize` INT NOT NULL DEFAULT 0 COMMENT 'poolSize(poolSize)',
    `minIdle` INT NOT NULL DEFAULT 0 COMMENT '最小空闲(minIdle)',
    `maxLifetime` INT NOT NULL DEFAULT 0 COMMENT 'maxLifetime(maxLifetime)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    `testStatus` VARCHAR(255) DEFAULT NULL COMMENT 'testStatus(testStatus)',
    `testMessage` VARCHAR(255) DEFAULT NULL COMMENT 'testMessage(testMessage)',
    `lastTestAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastTestAt(lastTestAt)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_data_source_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DbDataSource (auto-generated V3.0.0)';

-- Document -> document
CREATE TABLE IF NOT EXISTS `document` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `kbId` BIGINT NOT NULL DEFAULT 0 COMMENT 'kbId(kbId)',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '标题(title)',
    `sourceType` VARCHAR(255) DEFAULT NULL COMMENT 'sourceType(sourceType)',
    `sourceUri` VARCHAR(255) DEFAULT NULL COMMENT 'sourceUri(sourceUri)',
    `content` TEXT DEFAULT NULL COMMENT '内容(content)',
    `sizeBytes` BIGINT NOT NULL DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT '错误信息(errorMsg)',
    `chunkCount` INT NOT NULL DEFAULT 0 COMMENT '分片总数(chunkCount)',
    `checksum` VARCHAR(255) DEFAULT NULL COMMENT '校验和(checksum)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Document (auto-generated V3.0.0)';

-- DocumentChunk -> document_chunk
CREATE TABLE IF NOT EXISTS `document_chunk` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `docId` BIGINT NOT NULL DEFAULT 0 COMMENT '文档ID(docId)',
    `kbId` BIGINT NOT NULL DEFAULT 0 COMMENT 'kbId(kbId)',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `chunkIndex` INT NOT NULL DEFAULT 0 COMMENT '分片序号(chunkIndex)',
    `content` TEXT DEFAULT NULL COMMENT '内容(content)',
    `dim` INT NOT NULL DEFAULT 0 COMMENT 'dim(dim)',
    `charCount` INT NOT NULL DEFAULT 0 COMMENT 'charCount(charCount)',
    `startPos` INT NOT NULL DEFAULT 0 COMMENT 'startPos(startPos)',
    `endPos` INT NOT NULL DEFAULT 0 COMMENT 'endPos(endPos)',
    `accessCount` INT NOT NULL DEFAULT 0 COMMENT 'accessCount(accessCount)',
    `lastAccessAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastAccessAt(lastAccessAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_document_chunk_docId` (`docId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DocumentChunk (auto-generated V3.0.0)';

-- FunctionCallLog -> function_call_log
CREATE TABLE IF NOT EXISTS `function_call_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `sessionId` BIGINT NOT NULL DEFAULT 0 COMMENT '会话ID(sessionId)',
    `toolName` VARCHAR(255) DEFAULT NULL COMMENT '工具名称(toolName)',
    `arguments` VARCHAR(255) DEFAULT NULL COMMENT 'arguments(arguments)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT '处理结果(result)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT '错误信息(errorMsg)',
    `durationMs` INT NOT NULL DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'IP地址(ip)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT '用户代理(userAgent)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_function_call_log_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FunctionCallLog (auto-generated V3.0.0)';

-- FunctionTool -> function_tool
CREATE TABLE IF NOT EXISTS `function_tool` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName(displayName)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT '分类(category)',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters(parameters)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FunctionTool (auto-generated V3.0.0)';

-- KbChunk -> kb_chunk
CREATE TABLE IF NOT EXISTS `kb_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `chunkId` VARCHAR(255) DEFAULT NULL COMMENT '分块ID(chunkId)',
    `docId` VARCHAR(255) DEFAULT NULL COMMENT '文档ID(docId)',
    `kbId` VARCHAR(255) DEFAULT NULL COMMENT 'kbId(kbId)',
    `seq` INT NOT NULL DEFAULT 0 COMMENT 'seq(seq)',
    `content` TEXT DEFAULT NULL COMMENT '内容(content)',
    `charCount` INT NOT NULL DEFAULT 0 COMMENT 'charCount(charCount)',
    `tokenCount` INT NOT NULL DEFAULT 0 COMMENT 'tokenCount(tokenCount)',
    `embedding` VARCHAR(255) DEFAULT NULL COMMENT '向量嵌入(embedding)',
    `embeddingModel` VARCHAR(255) DEFAULT NULL COMMENT 'embeddingModel(embeddingModel)',
    `keywords` VARCHAR(255) DEFAULT NULL COMMENT '关键词(keywords)',
    `summary` VARCHAR(255) DEFAULT NULL COMMENT '摘要(summary)',
    `location` VARCHAR(255) DEFAULT NULL COMMENT '位置(location)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_chunk_docId` (`docId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KbChunk (auto-generated V3.0.0)';

-- KbDocument -> kb_document
CREATE TABLE IF NOT EXISTS `kb_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `docId` VARCHAR(255) DEFAULT NULL COMMENT '文档ID(docId)',
    `kbId` VARCHAR(255) DEFAULT NULL COMMENT 'kbId(kbId)',
    `filename` VARCHAR(255) DEFAULT NULL COMMENT 'filename(filename)',
    `mimeType` VARCHAR(255) DEFAULT NULL COMMENT 'MIME类型(mimeType)',
    `sizeBytes` BIGINT NOT NULL DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)',
    `filePath` VARCHAR(255) DEFAULT NULL COMMENT '文件路径(filePath)',
    `source` VARCHAR(255) DEFAULT NULL COMMENT '来源(source)',
    `sourceUrl` VARCHAR(255) DEFAULT NULL COMMENT 'sourceUrl(sourceUrl)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `chunkCount` INT NOT NULL DEFAULT 0 COMMENT '分片总数(chunkCount)',
    `embeddingCount` INT NOT NULL DEFAULT 0 COMMENT 'embeddingCount(embeddingCount)',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `isPublic` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开(0否1是)(isPublic)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_document_docId` (`docId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KbDocument (auto-generated V3.0.0)';

-- KbPermission -> kb_permission
CREATE TABLE IF NOT EXISTS `kb_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `kbId` VARCHAR(255) DEFAULT NULL COMMENT 'kbId(kbId)',
    `subjectType` VARCHAR(255) DEFAULT NULL COMMENT 'subjectType(subjectType)',
    `subjectId` BIGINT NOT NULL DEFAULT 0 COMMENT 'subjectId(subjectId)',
    `permission` VARCHAR(255) DEFAULT NULL COMMENT 'permission(permission)',
    `grantBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'grantBy(grantBy)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KbPermission (auto-generated V3.0.0)';

-- KgEntity -> kg_entity
CREATE TABLE IF NOT EXISTS `kg_entity` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `entityType` VARCHAR(255) DEFAULT NULL COMMENT 'entityType(entityType)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `aliases` VARCHAR(255) DEFAULT NULL COMMENT 'aliases(aliases)',
    `importance` INT NOT NULL DEFAULT 0 COMMENT 'importance(importance)',
    `source` VARCHAR(255) DEFAULT NULL COMMENT '来源(source)',
    `refCount` INT NOT NULL DEFAULT 0 COMMENT 'refCount(refCount)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kg_entity_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KgEntity (auto-generated V3.0.0)';

-- KgRelation -> kg_relation
CREATE TABLE IF NOT EXISTS `kg_relation` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `fromEntity` BIGINT NOT NULL DEFAULT 0 COMMENT 'fromEntity(fromEntity)',
    `toEntity` BIGINT NOT NULL DEFAULT 0 COMMENT 'toEntity(toEntity)',
    `relationType` VARCHAR(255) DEFAULT NULL COMMENT 'relationType(relationType)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `weight` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '权重(weight)',
    `source` VARCHAR(255) DEFAULT NULL COMMENT '来源(source)',
    `refCount` INT NOT NULL DEFAULT 0 COMMENT 'refCount(refCount)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kg_relation_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KgRelation (auto-generated V3.0.0)';

-- KnowledgeBase -> knowledge_base
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `tenantId` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID(tenantId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `visibility` VARCHAR(255) DEFAULT NULL COMMENT 'visibility(visibility)',
    `docCount` INT NOT NULL DEFAULT 0 COMMENT 'docCount(docCount)',
    `chunkCount` INT NOT NULL DEFAULT 0 COMMENT '分片总数(chunkCount)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KnowledgeBase (auto-generated V3.0.0)';

-- LicenseTemplate -> license_template
CREATE TABLE IF NOT EXISTS `license_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `templateKey` VARCHAR(255) DEFAULT NULL COMMENT 'templateKey(templateKey)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `licenseType` VARCHAR(255) DEFAULT NULL COMMENT '许可证类型(licenseType)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `quotaCalls` BIGINT NOT NULL DEFAULT 0 COMMENT 'quotaCalls(quotaCalls)',
    `quotaDays` INT NOT NULL DEFAULT 0 COMMENT '有效期(天)(quotaDays)',
    `priceCents` BIGINT NOT NULL DEFAULT 0 COMMENT 'priceCents(priceCents)',
    `features` VARCHAR(255) DEFAULT NULL COMMENT '功能列表(features)',
    `limits` VARCHAR(255) DEFAULT NULL COMMENT 'limits(limits)',
    `isPublic` INT NOT NULL DEFAULT 0 COMMENT '是否公开(0否1是)(isPublic)',
    `isActive` INT NOT NULL DEFAULT 0 COMMENT '是否激活(0否1是)(isActive)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号(version)',
    `createdBy` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID(createdBy)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LicenseTemplate (auto-generated V3.0.0)';

-- LongTermMemory -> memory_long_term
CREATE TABLE IF NOT EXISTS `memory_long_term` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `sessionId` BIGINT NOT NULL DEFAULT 0 COMMENT '会话ID(sessionId)',
    `content` TEXT DEFAULT NULL COMMENT '内容(content)',
    `summary` VARCHAR(255) DEFAULT NULL COMMENT '摘要(summary)',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)',
    `dim` INT NOT NULL DEFAULT 0 COMMENT 'dim(dim)',
    `importance` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'importance(importance)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    `accessCount` INT NOT NULL DEFAULT 0 COMMENT 'accessCount(accessCount)',
    `lastAccessAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastAccessAt(lastAccessAt)',
    `expiresAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '过期时间(expiresAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_memory_long_term_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LongTermMemory (auto-generated V3.0.0)';

-- UserPref -> memory_user_pref
CREATE TABLE IF NOT EXISTS `memory_user_pref` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `prefKey` VARCHAR(255) DEFAULT NULL COMMENT 'prefKey(prefKey)',
    `prefValue` VARCHAR(255) DEFAULT NULL COMMENT 'prefValue(prefValue)',
    `weight` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '权重(weight)',
    `source` VARCHAR(255) DEFAULT NULL COMMENT '来源(source)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_memory_user_pref_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UserPref (auto-generated V3.0.0)';

-- MetricSnapshot -> metric_snapshot
CREATE TABLE IF NOT EXISTS `metric_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `recordedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'recordedAt(recordedAt)',
    `service` VARCHAR(255) DEFAULT NULL COMMENT 'service(service)',
    `metricName` VARCHAR(255) DEFAULT NULL COMMENT '指标名称(metricName)',
    `metricValue` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '指标值(metricValue)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MetricSnapshot (auto-generated V3.0.0)';

-- ModelBattleLog -> model_battle_log
CREATE TABLE IF NOT EXISTS `model_battle_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `battle_id` VARCHAR(255) DEFAULT NULL COMMENT 'battle_id(battle_id)',
    `user_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'user_id(user_id)',
    `model_id` VARCHAR(255) DEFAULT NULL COMMENT 'model_id(model_id)',
    `prompt_tokens` INT NOT NULL DEFAULT 0 COMMENT 'prompt_tokens(prompt_tokens)',
    `completion_tokens` INT NOT NULL DEFAULT 0 COMMENT 'completion_tokens(completion_tokens)',
    `latency_ms` VARCHAR(255) DEFAULT NULL COMMENT 'latency_ms(latency_ms)',
    `error_msg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg(error_msg)',
    `judge_model` VARCHAR(255) DEFAULT NULL COMMENT 'judge_model(judge_model)',
    `judge_reason` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'judge_reason(judge_reason)',
    `prompt` VARCHAR(255) DEFAULT NULL COMMENT '提示词(prompt)',
    `response` TEXT DEFAULT NULL COMMENT '响应数据(response)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `score` INT NOT NULL DEFAULT 0 COMMENT '分数(score)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelBattleLog (auto-generated V3.0.0)';

-- ModelConfig -> model_config
CREATE TABLE IF NOT EXISTS `model_config` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `providerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'providerId(providerId)',
    `modelCode` VARCHAR(255) DEFAULT NULL COMMENT 'modelCode(modelCode)',
    `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName(displayName)',
    `maxContext` INT NOT NULL DEFAULT 0 COMMENT 'maxContext(maxContext)',
    `maxOutput` INT NOT NULL DEFAULT 0 COMMENT 'maxOutput(maxOutput)',
    `inputPrice` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'inputPrice(inputPrice)',
    `outputPrice` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'outputPrice(outputPrice)',
    `supportsVision` INT NOT NULL DEFAULT 0 COMMENT 'supportsVision(supportsVision)',
    `supportsTools` INT NOT NULL DEFAULT 0 COMMENT 'supportsTools(supportsTools)',
    `supportsStream` INT NOT NULL DEFAULT 0 COMMENT 'supportsStream(supportsStream)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号(sort)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelConfig (auto-generated V3.0.0)';

-- ModelLicense -> model_license
CREATE TABLE IF NOT EXISTS `model_license` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `licenseKey` VARCHAR(255) DEFAULT NULL COMMENT '许可证Key(licenseKey)',
    `modelEntryId` BIGINT NOT NULL DEFAULT 0 COMMENT 'modelEntryId(modelEntryId)',
    `modelVersionId` BIGINT NOT NULL DEFAULT 0 COMMENT 'modelVersionId(modelVersionId)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `licenseType` VARCHAR(255) DEFAULT NULL COMMENT '许可证类型(licenseType)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `quotaCalls` BIGINT NOT NULL DEFAULT 0 COMMENT 'quotaCalls(quotaCalls)',
    `usedCalls` BIGINT NOT NULL DEFAULT 0 COMMENT 'usedCalls(usedCalls)',
    `startAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'startAt(startAt)',
    `expireAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '过期时间(expireAt)',
    `priceCents` BIGINT NOT NULL DEFAULT 0 COMMENT 'priceCents(priceCents)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_license_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelLicense (auto-generated V3.0.0)';

-- ModelEntry -> model_market
CREATE TABLE IF NOT EXISTS `model_market` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `modelKey` VARCHAR(255) DEFAULT NULL COMMENT 'modelKey(modelKey)',
    `name` TEXT DEFAULT NULL COMMENT '名称(name)',
    `modelType` VARCHAR(255) DEFAULT NULL COMMENT '模型类型(modelType)',
    `taskType` VARCHAR(255) DEFAULT NULL COMMENT '任务类型(taskType)',
    `baseModel` VARCHAR(255) DEFAULT NULL COMMENT 'baseModel(baseModel)',
    `version` VARCHAR(255) DEFAULT NULL COMMENT '乐观锁版本号(version)',
    `filePath` VARCHAR(255) DEFAULT NULL COMMENT '文件路径(filePath)',
    `fileName` BIGINT NOT NULL DEFAULT 0 COMMENT '文件名(fileName)',
    `fileSize` VARCHAR(255) DEFAULT NULL COMMENT '文件大小(字节)(fileSize)',
    `license` VARCHAR(255) DEFAULT NULL COMMENT 'license(license)',
    `authorId` VARCHAR(255) DEFAULT NULL COMMENT 'authorId(authorId)',
    `authorName` VARCHAR(255) DEFAULT NULL COMMENT 'authorName(authorName)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `downloadCount` DOUBLE NOT NULL DEFAULT 0.0 COMMENT '下载次数(downloadCount)',
    `avgRating` BIGINT NOT NULL DEFAULT 0 COMMENT 'avgRating(avgRating)',
    `ratingCount` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'ratingCount(ratingCount)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `publishedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间(publishedAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_market_modelKey` (`modelKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelEntry (auto-generated V3.0.0)';

-- ModelProvider -> model_provider
CREATE TABLE IF NOT EXISTS `model_provider` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `code` VARCHAR(255) DEFAULT NULL COMMENT '编码(code)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `baseUrl` VARCHAR(255) DEFAULT NULL COMMENT 'baseUrl(baseUrl)',
    `apiKey` VARCHAR(255) DEFAULT NULL COMMENT 'API密钥(apiKey)',
    `protocol` VARCHAR(255) DEFAULT NULL COMMENT '协议(protocol)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号(sort)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelProvider (auto-generated V3.0.0)';

-- ModelQuota -> model_quota
CREATE TABLE IF NOT EXISTS `model_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `modelId` BIGINT NOT NULL DEFAULT 0 COMMENT '模型ID(modelId)',
    `quotaDate` DATE DEFAULT NULL COMMENT 'quotaDate(quotaDate)',
    `usedTokens` BIGINT NOT NULL DEFAULT 0 COMMENT 'usedTokens(usedTokens)',
    `usedRequests` INT NOT NULL DEFAULT 0 COMMENT 'usedRequests(usedRequests)',
    `limitTokens` BIGINT NOT NULL DEFAULT 0 COMMENT 'limitTokens(limitTokens)',
    `limitRequests` INT NOT NULL DEFAULT 0 COMMENT 'limitRequests(limitRequests)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_quota_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelQuota (auto-generated V3.0.0)';

-- ModelRating -> model_rating
CREATE TABLE IF NOT EXISTS `model_rating` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `modelKey` VARCHAR(255) DEFAULT NULL COMMENT 'modelKey(modelKey)',
    `userId` VARCHAR(255) DEFAULT NULL COMMENT '用户ID(userId)',
    `username` INT NOT NULL DEFAULT 0 COMMENT '用户名(username)',
    `rating` VARCHAR(255) DEFAULT NULL COMMENT '评分(rating)',
    `comment` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论(comment)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_rating_modelKey` (`modelKey`),
    UNIQUE KEY `uk_model_rating_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelRating (auto-generated V3.0.0)';

-- ModelVersion -> model_version
CREATE TABLE IF NOT EXISTS `model_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `versionId` VARCHAR(255) DEFAULT NULL COMMENT 'versionId(versionId)',
    `modelEntryId` BIGINT NOT NULL DEFAULT 0 COMMENT 'modelEntryId(modelEntryId)',
    `version` VARCHAR(255) DEFAULT NULL COMMENT '乐观锁版本号(version)',
    `changelog` VARCHAR(255) DEFAULT NULL COMMENT 'changelog(changelog)',
    `filePath` VARCHAR(255) DEFAULT NULL COMMENT '文件路径(filePath)',
    `sizeBytes` BIGINT NOT NULL DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)',
    `inputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'inputSchema(inputSchema)',
    `outputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'outputSchema(outputSchema)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `isLatest` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'isLatest(isLatest)',
    `uploaderId` BIGINT NOT NULL DEFAULT 0 COMMENT 'uploaderId(uploaderId)',
    `backwardCompatible` VARCHAR(255) DEFAULT NULL COMMENT 'backwardCompatible(backwardCompatible)',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT '元数据JSON(metadata)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModelVersion (auto-generated V3.0.0)';

-- ModerationRecord -> moderation_record
CREATE TABLE IF NOT EXISTS `moderation_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `traceId` VARCHAR(255) DEFAULT NULL COMMENT '链路追踪ID(traceId)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT '用户名(username)',
    `contentType` VARCHAR(255) DEFAULT NULL COMMENT '内容类型(contentType)',
    `contentHash` VARCHAR(255) DEFAULT NULL COMMENT 'contentHash(contentHash)',
    `contentSize` BIGINT NOT NULL DEFAULT 0 COMMENT 'contentSize(contentSize)',
    `contentUrl` VARCHAR(255) DEFAULT NULL COMMENT 'contentUrl(contentUrl)',
    `moderationStatus` VARCHAR(255) DEFAULT NULL COMMENT 'moderationStatus(moderationStatus)',
    `riskLevel` VARCHAR(255) DEFAULT NULL COMMENT '风险等级(riskLevel)',
    `riskLabels` VARCHAR(255) DEFAULT NULL COMMENT 'riskLabels(riskLabels)',
    `riskScore` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '风险分数(riskScore)',
    `moderator` VARCHAR(255) DEFAULT NULL COMMENT 'moderator(moderator)',
    `rejectionReason` VARCHAR(255) DEFAULT NULL COMMENT 'rejectionReason(rejectionReason)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_moderation_record_userId` (`userId`),
    UNIQUE KEY `uk_moderation_record_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ModerationRecord (auto-generated V3.0.0)';

-- MultimediaFile -> multimedia_file
CREATE TABLE IF NOT EXISTS `multimedia_file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `fileId` VARCHAR(255) DEFAULT NULL COMMENT '文件ID(fileId)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT '用户名(username)',
    `fileName` VARCHAR(255) DEFAULT NULL COMMENT '文件名(fileName)',
    `originalName` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名(originalName)',
    `fileType` VARCHAR(255) DEFAULT NULL COMMENT '文件类型(fileType)',
    `mimeType` VARCHAR(255) DEFAULT NULL COMMENT 'MIME类型(mimeType)',
    `fileSize` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小(字节)(fileSize)',
    `fileHash` VARCHAR(255) DEFAULT NULL COMMENT 'fileHash(fileHash)',
    `storagePath` VARCHAR(255) DEFAULT NULL COMMENT 'storagePath(storagePath)',
    `storageType` VARCHAR(255) DEFAULT NULL COMMENT 'storageType(storageType)',
    `encrypted` INT NOT NULL DEFAULT 0 COMMENT 'encrypted(encrypted)',
    `durationMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `width` INT NOT NULL DEFAULT 0 COMMENT '宽度(width)',
    `height` INT NOT NULL DEFAULT 0 COMMENT '高度(height)',
    `bitrate` INT NOT NULL DEFAULT 0 COMMENT 'bitrate(bitrate)',
    `sampleRate` INT NOT NULL DEFAULT 0 COMMENT 'sampleRate(sampleRate)',
    `channels` INT NOT NULL DEFAULT 0 COMMENT 'channels(channels)',
    `codec` VARCHAR(255) DEFAULT NULL COMMENT 'codec(codec)',
    `exif` VARCHAR(255) DEFAULT NULL COMMENT 'exif(exif)',
    `moderationStatus` VARCHAR(255) DEFAULT NULL COMMENT 'moderationStatus(moderationStatus)',
    `moderationId` BIGINT NOT NULL DEFAULT 0 COMMENT 'moderationId(moderationId)',
    `watermarked` INT NOT NULL DEFAULT 0 COMMENT 'watermarked(watermarked)',
    `isPublic` INT NOT NULL DEFAULT 0 COMMENT '是否公开(0否1是)(isPublic)',
    `accessCount` INT NOT NULL DEFAULT 0 COMMENT 'accessCount(accessCount)',
    `expireAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '过期时间(expireAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_multimedia_file_userId` (`userId`),
    UNIQUE KEY `uk_multimedia_file_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MultimediaFile (auto-generated V3.0.0)';

-- Notification -> notification
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '标题(title)',
    `content` TEXT DEFAULT NULL COMMENT '内容(content)',
    `isRead` INT NOT NULL DEFAULT 0 COMMENT '是否已读(0否1是)(isRead)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Notification (auto-generated V3.0.0)';

-- OAuthAppConfig -> oauth_app_config
CREATE TABLE IF NOT EXISTS `oauth_app_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT '平台(platform)',
    `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)',
    `appId` VARCHAR(255) DEFAULT NULL COMMENT '应用ID(appId)',
    `appSecret` VARCHAR(255) DEFAULT NULL COMMENT '应用密钥(appSecret)',
    `publicKey` VARCHAR(255) DEFAULT NULL COMMENT '公钥(publicKey)',
    `redirectUri` VARCHAR(255) DEFAULT NULL COMMENT 'redirectUri(redirectUri)',
    `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes(scopes)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    `extraConfig` VARCHAR(255) DEFAULT NULL COMMENT 'extraConfig(extraConfig)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuthAppConfig (auto-generated V3.0.0)';

-- OAuthBinding -> oauth_binding
CREATE TABLE IF NOT EXISTS `oauth_binding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT '平台(platform)',
    `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid(openid)',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT '昵称(nickname)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL(avatar)',
    `accessToken` VARCHAR(255) DEFAULT NULL COMMENT 'accessToken(accessToken)',
    `refreshToken` VARCHAR(255) DEFAULT NULL COMMENT '刷新令牌(refreshToken)',
    `tokenExpiresAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'tokenExpiresAt(tokenExpiresAt)',
    `rawData` VARCHAR(255) DEFAULT NULL COMMENT '原始数据(rawData)',
    `boundAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'boundAt(boundAt)',
    `lastLoginAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间(lastLoginAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_oauth_binding_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuthBinding (auto-generated V3.0.0)';

-- PipelineLog -> pipeline_log
CREATE TABLE IF NOT EXISTS `pipeline_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `sessionId` VARCHAR(255) DEFAULT NULL COMMENT '会话ID(sessionId)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `clientIp` VARCHAR(255) DEFAULT NULL COMMENT 'clientIp(clientIp)',
    `inputText` VARCHAR(255) DEFAULT NULL COMMENT '输入文本(inputText)',
    `inputModality` VARCHAR(255) DEFAULT NULL COMMENT 'inputModality(inputModality)',
    `intent` VARCHAR(255) DEFAULT NULL COMMENT '意图(intent)',
    `outputText` VARCHAR(255) DEFAULT NULL COMMENT '输出文本(outputText)',
    `outputTokens` INT NOT NULL DEFAULT 0 COMMENT '输出Token数(outputTokens)',
    `computeDevice` VARCHAR(255) DEFAULT NULL COMMENT 'computeDevice(computeDevice)',
    `computeMode` VARCHAR(255) DEFAULT NULL COMMENT 'computeMode(computeMode)',
    `totalCostMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'totalCostMs(totalCostMs)',
    `stageCosts` VARCHAR(255) DEFAULT NULL COMMENT 'stageCosts(stageCosts)',
    `riskLevel` VARCHAR(255) DEFAULT NULL COMMENT '风险等级(riskLevel)',
    `needsReview` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'needsReview(needsReview)',
    `ragHits` INT NOT NULL DEFAULT 0 COMMENT 'ragHits(ragHits)',
    `toolCalls` INT NOT NULL DEFAULT 0 COMMENT 'toolCalls(toolCalls)',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT '错误信息(errorMessage)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pipeline_log_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineLog (auto-generated V3.0.0)';

-- PipelineNodeLog -> pipeline_node_log
CREATE TABLE IF NOT EXISTS `pipeline_node_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `runId` BIGINT NOT NULL DEFAULT 0 COMMENT 'runId(runId)',
    `nodeId` VARCHAR(255) DEFAULT NULL COMMENT '节点ID(nodeId)',
    `nodeName` VARCHAR(255) DEFAULT NULL COMMENT '节点名称(nodeName)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `endTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间(endTime)',
    `durationMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `inputRows` INT NOT NULL DEFAULT 0 COMMENT 'inputRows(inputRows)',
    `outputRows` INT NOT NULL DEFAULT 0 COMMENT 'outputRows(outputRows)',
    `outputPreview` VARCHAR(255) DEFAULT NULL COMMENT 'outputPreview(outputPreview)',
    `configSnapshot` VARCHAR(255) DEFAULT NULL COMMENT 'configSnapshot(configSnapshot)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineNodeLog (auto-generated V3.0.0)';

-- PipelineRun -> pipeline_run
CREATE TABLE IF NOT EXISTS `pipeline_run` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `workflowId` BIGINT NOT NULL DEFAULT 0 COMMENT '工作流ID(workflowId)',
    `workflowName` VARCHAR(255) DEFAULT NULL COMMENT '工作流名称(workflowName)',
    `triggerType` VARCHAR(255) DEFAULT NULL COMMENT '触发器类型(triggerType)',
    `endTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间(endTime)',
    `durationMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT '错误信息(errorMessage)',
    `resultSummary` VARCHAR(255) DEFAULT NULL COMMENT 'resultSummary(resultSummary)',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createTime(createTime)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineRun (auto-generated V3.0.0)';

-- PipelineWorkflow -> pipeline_workflow
CREATE TABLE IF NOT EXISTS `pipeline_workflow` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `definition` VARCHAR(255) DEFAULT NULL COMMENT 'definition(definition)',
    `status` INT NOT NULL DEFAULT 0 COMMENT '状态(0禁用1启用)(status)',
    `createBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'createBy(createBy)',
    `updateBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'updateBy(updateBy)',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createTime(createTime)',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'updateTime(updateTime)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineWorkflow (auto-generated V3.0.0)';

-- PipelineWorkflowVersion -> pipeline_workflow_version
CREATE TABLE IF NOT EXISTS `pipeline_workflow_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `workflowId` BIGINT NOT NULL DEFAULT 0 COMMENT '工作流ID(workflowId)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号(version)',
    `definition` VARCHAR(255) DEFAULT NULL COMMENT 'definition(definition)',
    `createBy` BIGINT NOT NULL DEFAULT 0 COMMENT 'createBy(createBy)',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'createTime(createTime)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PipelineWorkflowVersion (auto-generated V3.0.0)';

-- Plugin -> plugin
CREATE TABLE IF NOT EXISTS `plugin` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName(displayName)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `version` VARCHAR(255) DEFAULT NULL COMMENT '乐观锁版本号(version)',
    `author` VARCHAR(255) DEFAULT NULL COMMENT 'author(author)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT '分类(category)',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '图标(icon)',
    `entry` VARCHAR(255) DEFAULT NULL COMMENT 'entry(entry)',
    `pluginType` VARCHAR(255) DEFAULT NULL COMMENT 'pluginType(pluginType)',
    `config` TEXT DEFAULT NULL COMMENT '配置JSON(config)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    `downloads` INT NOT NULL DEFAULT 0 COMMENT 'downloads(downloads)',
    `rating` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '评分(rating)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Plugin (auto-generated V3.0.0)';

-- PromptTemplate -> prompt_template
CREATE TABLE IF NOT EXISTS `prompt_template` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT '分类(category)',
    `content` TEXT DEFAULT NULL COMMENT '内容(content)',
    `variables` VARCHAR(255) DEFAULT NULL COMMENT 'variables(variables)',
    `creatorId` BIGINT NOT NULL DEFAULT 0 COMMENT 'creatorId(creatorId)',
    `creatorName` VARCHAR(255) DEFAULT NULL COMMENT 'creatorName(creatorName)',
    `isPublic` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开(0否1是)(isPublic)',
    `useCount` INT NOT NULL DEFAULT 0 COMMENT 'useCount(useCount)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PromptTemplate (auto-generated V3.0.0)';

-- PushMessage -> push_message
CREATE TABLE IF NOT EXISTS `push_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `messageId` VARCHAR(255) DEFAULT NULL COMMENT '消息ID(messageId)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '标题(title)',
    `body` VARCHAR(255) DEFAULT NULL COMMENT '主体(body)',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '图标(icon)',
    `clickAction` VARCHAR(255) DEFAULT NULL COMMENT 'clickAction(clickAction)',
    `data` VARCHAR(255) DEFAULT NULL COMMENT '数据(data)',
    `targetType` VARCHAR(255) DEFAULT NULL COMMENT 'targetType(targetType)',
    `targetValue` VARCHAR(255) DEFAULT NULL COMMENT '目标值(targetValue)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `successCount` INT NOT NULL DEFAULT 0 COMMENT 'successCount(successCount)',
    `failureCount` INT NOT NULL DEFAULT 0 COMMENT 'failureCount(failureCount)',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PushMessage (auto-generated V3.0.0)';

-- PushSubscription -> push_subscription
CREATE TABLE IF NOT EXISTS `push_subscription` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `subscriptionId` VARCHAR(255) DEFAULT NULL COMMENT '订阅ID(subscriptionId)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT '平台(platform)',
    `endpoint` VARCHAR(255) DEFAULT NULL COMMENT '服务端点(endpoint)',
    `p256dhKey` VARCHAR(255) DEFAULT NULL COMMENT 'p256dhKey(p256dhKey)',
    `authKey` VARCHAR(255) DEFAULT NULL COMMENT 'authKey(authKey)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT '用户代理(userAgent)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `lastActiveAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastActiveAt(lastActiveAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_push_subscription_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PushSubscription (auto-generated V3.0.0)';

-- LogEntry -> raft_log
CREATE TABLE IF NOT EXISTS `raft_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `term` BIGINT NOT NULL DEFAULT 0 COMMENT 'term(term)',
    `logIndex` BIGINT NOT NULL DEFAULT 0 COMMENT 'logIndex(logIndex)',
    `nodeId` VARCHAR(255) DEFAULT NULL COMMENT '节点ID(nodeId)',
    `command` VARCHAR(255) DEFAULT NULL COMMENT '命令(command)',
    `committed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'committed(committed)',
    `committedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'committedAt(committedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LogEntry (auto-generated V3.0.0)';

-- RateLimitRule -> rate_limit_rule
CREATE TABLE IF NOT EXISTS `rate_limit_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `key` VARCHAR(255) DEFAULT NULL COMMENT 'key(key)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `capacity` INT NOT NULL DEFAULT 0 COMMENT '容量(capacity)',
    `refillTokens` INT NOT NULL DEFAULT 0 COMMENT 'refillTokens(refillTokens)',
    `periodSeconds` INT NOT NULL DEFAULT 0 COMMENT 'periodSeconds(periodSeconds)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级(priority)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RateLimitRule (auto-generated V3.0.0)';

-- RequestLog -> request_log
CREATE TABLE IF NOT EXISTS `request_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `traceId` VARCHAR(255) DEFAULT NULL COMMENT '链路追踪ID(traceId)',
    `method` VARCHAR(255) DEFAULT NULL COMMENT '请求方法(method)',
    `path` VARCHAR(255) DEFAULT NULL COMMENT '路径(path)',
    `queryString` VARCHAR(255) DEFAULT NULL COMMENT 'queryString(queryString)',
    `clientIp` VARCHAR(255) DEFAULT NULL COMMENT 'clientIp(clientIp)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT '用户代理(userAgent)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `status` INT NOT NULL DEFAULT 0 COMMENT '状态(0禁用1启用)(status)',
    `latencyMs` BIGINT NOT NULL DEFAULT 0 COMMENT '延迟(毫秒)(latencyMs)',
    `slow` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'slow(slow)',
    `error` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'error(error)',
    `module` VARCHAR(255) DEFAULT NULL COMMENT 'module(module)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_request_log_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RequestLog (auto-generated V3.0.0)';

-- SensitiveWord -> sensitive_word
CREATE TABLE IF NOT EXISTS `sensitive_word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `word` VARCHAR(255) DEFAULT NULL COMMENT 'word(word)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT '分类(category)',
    `level` VARCHAR(255) DEFAULT NULL COMMENT '等级(level)',
    `action` VARCHAR(255) DEFAULT NULL COMMENT '操作类型(action)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SensitiveWord (auto-generated V3.0.0)';

-- SysRole -> sys_role
CREATE TABLE IF NOT EXISTS `sys_role` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `code` VARCHAR(255) DEFAULT NULL COMMENT '编码(code)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` TEXT DEFAULT NULL COMMENT '描述(description)',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号(sort)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SysRole (auto-generated V3.0.0)';

-- SysUser -> sys_user
CREATE TABLE IF NOT EXISTS `sys_user` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdBy` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID(createdBy)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedBy` BIGINT NOT NULL DEFAULT 0 COMMENT '更新人ID(updatedBy)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT '用户名(username)',
    `password` VARCHAR(255) DEFAULT NULL COMMENT '密码(BCrypt)(password)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT '昵称(nickname)',
    `email` VARCHAR(255) DEFAULT NULL COMMENT '邮箱(email)',
    `phone` VARCHAR(255) DEFAULT NULL COMMENT '手机号(phone)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL(avatar)',
    `gender` INT NOT NULL DEFAULT 0 COMMENT '性别(0未知1男2女)(gender)',
    `status` INT NOT NULL DEFAULT 0 COMMENT '状态(0禁用1启用)(status)',
    `lastLoginIp` VARCHAR(255) DEFAULT NULL COMMENT '最后登录IP(lastLoginIp)',
    `lastLoginAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间(lastLoginAt)',
    `tenantId` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID(tenantId)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注(remark)',
    `wechatOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'wechatOpenid(wechatOpenid)',
    `wechatUnionid` VARCHAR(255) DEFAULT NULL COMMENT 'wechatUnionid(wechatUnionid)',
    `wechatNickname` VARCHAR(255) DEFAULT NULL COMMENT 'wechatNickname(wechatNickname)',
    `wechatAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'wechatAvatar(wechatAvatar)',
    `wechatBoundAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'wechatBoundAt(wechatBoundAt)',
    `qqOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'qqOpenid(qqOpenid)',
    `qqUnionid` VARCHAR(255) DEFAULT NULL COMMENT 'qqUnionid(qqUnionid)',
    `qqNickname` VARCHAR(255) DEFAULT NULL COMMENT 'qqNickname(qqNickname)',
    `qqAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'qqAvatar(qqAvatar)',
    `qqBoundAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'qqBoundAt(qqBoundAt)',
    `alipayOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'alipayOpenid(alipayOpenid)',
    `alipayUserId` VARCHAR(255) DEFAULT NULL COMMENT 'alipayUserId(alipayUserId)',
    `alipayNickname` VARCHAR(255) DEFAULT NULL COMMENT 'alipayNickname(alipayNickname)',
    `alipayAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'alipayAvatar(alipayAvatar)',
    `alipayBoundAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'alipayBoundAt(alipayBoundAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SysUser (auto-generated V3.0.0)';

-- SysUserRole -> sys_user_role
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `roleId` BIGINT NOT NULL DEFAULT 0 COMMENT '角色ID(roleId)',
    UNIQUE KEY `uk_sys_user_role_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SysUserRole (auto-generated V3.0.0)';

-- Tenant -> tenant
CREATE TABLE IF NOT EXISTS `tenant` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `code` VARCHAR(255) DEFAULT NULL COMMENT '编码(code)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `plan` VARCHAR(255) DEFAULT NULL COMMENT '计划(plan)',
    `status` INT NOT NULL DEFAULT 0 COMMENT '状态(0禁用1启用)(status)',
    `maxUsers` INT NOT NULL DEFAULT 0 COMMENT 'maxUsers(maxUsers)',
    `maxModels` INT NOT NULL DEFAULT 0 COMMENT 'maxModels(maxModels)',
    `qpsLimit` INT NOT NULL DEFAULT 0 COMMENT 'qpsLimit(qpsLimit)',
    `monthlyQuota` BIGINT NOT NULL DEFAULT 0 COMMENT 'monthlyQuota(monthlyQuota)',
    `usedQuota` BIGINT NOT NULL DEFAULT 0 COMMENT 'usedQuota(usedQuota)',
    `expireAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '过期时间(expireAt)',
    `contactEmail` VARCHAR(255) DEFAULT NULL COMMENT 'contactEmail(contactEmail)',
    `contactPhone` VARCHAR(255) DEFAULT NULL COMMENT 'contactPhone(contactPhone)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注(remark)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant (auto-generated V3.0.0)';

-- TrainingCheckpoint -> training_checkpoint
CREATE TABLE IF NOT EXISTS `training_checkpoint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT '任务ID(taskId)',
    `checkpointId` VARCHAR(255) DEFAULT NULL COMMENT '检查点ID(checkpointId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `epoch` INT NOT NULL DEFAULT 0 COMMENT '训练轮次(epoch)',
    `step` INT NOT NULL DEFAULT 0 COMMENT 'step(step)',
    `filePath` VARCHAR(255) DEFAULT NULL COMMENT '文件路径(filePath)',
    `sizeBytes` BIGINT NOT NULL DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)',
    `valLoss` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'valLoss(valLoss)',
    `accuracy` DOUBLE NOT NULL DEFAULT 0.0 COMMENT '准确率(accuracy)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT '元数据JSON(metadata)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TrainingCheckpoint (auto-generated V3.0.0)';

-- TrainingJob -> training_job
CREATE TABLE IF NOT EXISTS `training_job` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT '任务ID(taskId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `totalEpochs` INT NOT NULL DEFAULT 0 COMMENT 'totalEpochs(totalEpochs)',
    `currentEpoch` INT NOT NULL DEFAULT 0 COMMENT 'currentEpoch(currentEpoch)',
    `currentStep` INT NOT NULL DEFAULT 0 COMMENT 'currentStep(currentStep)',
    `startTimeMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'startTimeMs(startTimeMs)',
    `endTimeMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'endTimeMs(endTimeMs)',
    `config` TEXT DEFAULT NULL COMMENT '配置JSON(config)',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)(tags)',
    `lastLoss` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'lastLoss(lastLoss)',
    `lastValLoss` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'lastValLoss(lastValLoss)',
    `lastAccuracy` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'lastAccuracy(lastAccuracy)',
    `totalSteps` INT NOT NULL DEFAULT 0 COMMENT 'totalSteps(totalSteps)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TrainingJob (auto-generated V3.0.0)';

-- TrainingMetric -> training_metric
CREATE TABLE IF NOT EXISTS `training_metric` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'timestamp(timestamp)',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT '任务ID(taskId)',
    `epoch` INT NOT NULL DEFAULT 0 COMMENT '训练轮次(epoch)',
    `step` INT NOT NULL DEFAULT 0 COMMENT 'step(step)',
    `loss` DOUBLE NOT NULL DEFAULT 0.0 COMMENT '损失值(loss)',
    `valLoss` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'valLoss(valLoss)',
    `accuracy` DOUBLE NOT NULL DEFAULT 0.0 COMMENT '准确率(accuracy)',
    `learningRate` DOUBLE NOT NULL DEFAULT 0.0 COMMENT '学习率(learningRate)',
    `elapsedMs` BIGINT NOT NULL DEFAULT 0 COMMENT 'elapsedMs(elapsedMs)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TrainingMetric (auto-generated V3.0.0)';

-- TrainingTask -> training_task
CREATE TABLE IF NOT EXISTS `training_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `modelName` VARCHAR(255) DEFAULT NULL COMMENT '模型名称(modelName)',
    `corpusPath` VARCHAR(255) DEFAULT NULL COMMENT 'corpusPath(corpusPath)',
    `nHead` INT NOT NULL DEFAULT 0 COMMENT 'nHead(nHead)',
    `nEmbd` INT NOT NULL DEFAULT 0 COMMENT 'nEmbd(nEmbd)',
    `blockSize` INT NOT NULL DEFAULT 0 COMMENT '区块大小(blockSize)',
    `maxIters` INT NOT NULL DEFAULT 0 COMMENT 'maxIters(maxIters)',
    `batchSize` INT NOT NULL DEFAULT 0 COMMENT '批次大小(batchSize)',
    `learningRate` DOUBLE NOT NULL DEFAULT 0.0 COMMENT '学习率(learningRate)',
    `progress` INT NOT NULL DEFAULT 0 COMMENT '进度(0-100)(progress)',
    `currentIter` INT NOT NULL DEFAULT 0 COMMENT 'currentIter(currentIter)',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT '错误信息(errorMessage)',
    `completedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '完成时间(completedAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_training_task_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TrainingTask (auto-generated V3.0.0)';

-- UnionidRelations -> unionid_relations
CREATE TABLE IF NOT EXISTS `unionid_relations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT '平台(platform)',
    `firstSeenAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'firstSeenAt(firstSeenAt)',
    `lastSeenAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastSeenAt(lastSeenAt)',
    `bindingCount` INT NOT NULL DEFAULT 0 COMMENT 'bindingCount(bindingCount)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_unionid_relations_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UnionidRelations (auto-generated V3.0.0)';

-- UserApiKey -> user_api_key
CREATE TABLE IF NOT EXISTS `user_api_key` (
    `deleted` INT NOT NULL DEFAULT 0 COMMENT 'deleted (logic delete)',
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `keyHash` VARCHAR(255) DEFAULT NULL COMMENT 'keyHash(keyHash)',
    `keyPrefix` VARCHAR(255) DEFAULT NULL COMMENT 'keyPrefix(keyPrefix)',
    `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes(scopes)',
    `expiresAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '过期时间(expiresAt)',
    `lastUsedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'lastUsedAt(lastUsedAt)',
    `useCount` BIGINT NOT NULL DEFAULT 0 COMMENT 'useCount(useCount)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_api_key_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UserApiKey (auto-generated V3.0.0)';

-- Webhook -> webhook
CREATE TABLE IF NOT EXISTS `webhook` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `webhookId` VARCHAR(255) DEFAULT NULL COMMENT 'WebhookID(webhookId)',
    `name` TEXT DEFAULT NULL COMMENT '名称(name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `url` VARCHAR(255) DEFAULT NULL COMMENT '链接地址(url)',
    `events` VARCHAR(255) DEFAULT NULL COMMENT 'events(events)',
    `secret` VARCHAR(255) DEFAULT NULL COMMENT 'secret(secret)',
    `customHeaders` INT NOT NULL DEFAULT 0 COMMENT 'customHeaders(customHeaders)',
    `enabled` VARCHAR(255) DEFAULT NULL COMMENT '是否启用(0否1是)(enabled)',
    `status` BIGINT NOT NULL DEFAULT 0 COMMENT '状态(0禁用1启用)(status)',
    `deliveryCount` BIGINT NOT NULL DEFAULT 0 COMMENT 'deliveryCount(deliveryCount)',
    `successCount` BIGINT NOT NULL DEFAULT 0 COMMENT 'successCount(successCount)',
    `failCount` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'failCount(failCount)',
    `lastDeliveryAt` INT NOT NULL DEFAULT 0 COMMENT 'lastDeliveryAt(lastDeliveryAt)',
    `lastStatus` BIGINT NOT NULL DEFAULT 0 COMMENT 'lastStatus(lastStatus)',
    `ownerId` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'ownerId(ownerId)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_webhook_webhookId` (`webhookId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook (auto-generated V3.0.0)';

-- WebhookDelivery -> webhook_delivery
CREATE TABLE IF NOT EXISTS `webhook_delivery` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `webhookId` VARCHAR(255) DEFAULT NULL COMMENT 'WebhookID(webhookId)',
    `eventType` VARCHAR(255) DEFAULT NULL COMMENT '事件类型(eventType)',
    `eventId` MEDIUMTEXT DEFAULT NULL COMMENT '事件ID(eventId)',
    `payload` INT NOT NULL DEFAULT 0 COMMENT 'payload(payload)',
    `responseStatus` VARCHAR(255) DEFAULT NULL COMMENT '响应状态(responseStatus)',
    `responseBody` BIGINT NOT NULL DEFAULT 0 COMMENT '响应体(responseBody)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `retryCount` VARCHAR(255) DEFAULT NULL COMMENT '重试次数(retryCount)',
    `errorMsg` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '错误信息(errorMsg)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_webhook_delivery_webhookId` (`webhookId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WebhookDelivery (auto-generated V3.0.0)';

-- WechatConfig -> wechat_config
CREATE TABLE IF NOT EXISTS `wechat_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)',
    `appSecret` VARCHAR(255) DEFAULT NULL COMMENT '应用密钥(appSecret)',
    `token` VARCHAR(255) DEFAULT NULL COMMENT '令牌(token)',
    `aesKey` VARCHAR(255) DEFAULT NULL COMMENT 'aesKey(aesKey)',
    `redirectUri` VARCHAR(255) DEFAULT NULL COMMENT 'redirectUri(redirectUri)',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)',
    `enabled` INT NOT NULL DEFAULT 0 COMMENT '是否启用(0否1是)(enabled)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注(remark)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WechatConfig (auto-generated V3.0.0)';

-- WechatScanSession -> wechat_scan_session
CREATE TABLE IF NOT EXISTS `wechat_scan_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间(updatedAt)',
    `ticket` VARCHAR(255) DEFAULT NULL COMMENT 'ticket(ticket)',
    `sceneId` VARCHAR(255) DEFAULT NULL COMMENT 'sceneId(sceneId)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(0禁用1启用)(status)',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid(openid)',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT '昵称(nickname)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL(avatar)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `accessToken` VARCHAR(255) DEFAULT NULL COMMENT 'accessToken(accessToken)',
    `refreshToken` VARCHAR(255) DEFAULT NULL COMMENT '刷新令牌(refreshToken)',
    `clientIp` VARCHAR(255) DEFAULT NULL COMMENT 'clientIp(clientIp)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT '用户代理(userAgent)',
    `expiresAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '过期时间(expiresAt)',
    `confirmedAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'confirmedAt(confirmedAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wechat_scan_session_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WechatScanSession (auto-generated V3.0.0)';

-- WechatUserBinding -> wechat_user_binding
CREATE TABLE IF NOT EXISTS `wechat_user_binding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `boundAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'boundAt(boundAt)',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID(userId)',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid(openid)',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)',
    `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL(avatar)',
    `lastLoginAt` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间(lastLoginAt)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wechat_user_binding_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WechatUserBinding (auto-generated V3.0.0)';

-- SET FOREIGN_KEY_CHECKS = 1;

-- 全部 DDL 生成完毕


-- 种子数据 (合并)

-- ====================================================================
-- MiniMax Platform V3.0.0+ 初始种子数据
-- 让新装环境可立即使用: 1 个超管 + 21 个 AI 工具 + 3 个示例 Agent
--
-- 用法: 先跑 init.sql 建表, 再跑 init_seeds.sql 加种子
--   mysql -u root -p minimax_platform < sql/init.sql
--   mysql -u root -p minimax_platform < sql/init_seeds.sql
-- ====================================================================

-- USE minimax_platform; (已合并)

-- ===========================================
-- 1. 默认角色 (4 个)
-- ===========================================
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `createdAt`, `updatedAt`) VALUES (1, 'SUPER_ADMIN', '超管', '拥有所有权限', 1, NOW(), NOW());
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `createdAt`, `updatedAt`) VALUES (2, 'ADMIN', '管理员', '后台管理权限', 1, NOW(), NOW());
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `createdAt`, `updatedAt`) VALUES (3, 'USER', '普通用户', '基础使用权限', 1, NOW(), NOW());
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `enabled`, `createdAt`, `updatedAt`) VALUES (4, 'GUEST', '访客', '只读权限', 1, NOW(), NOW());

-- ===========================================
-- 2. 超管账号 (adminLiugl / Liugl@2026)
-- ===========================================
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`, `createdAt`, `updatedAt`) VALUES (1, 'adminLiugl', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超管', 'admin@minimax.com', 1, NOW(), NOW());
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`, `createdAt`, `updatedAt`) VALUES (2, 'demo', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '演示账号', 'demo@minimax.com', 1, NOW(), NOW());

-- ===========================================
-- 3. 角色分配
-- ===========================================
INSERT INTO `sys_user_role` (`userId`, `roleId`) VALUES (1, 1);
INSERT INTO `sys_user_role` (`userId`, `roleId`) VALUES (2, 3);

-- ===========================================
-- 4. AI 意图关键词 (10 个)
-- ===========================================
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (1, 'writing', '写文章', 1, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (2, 'writing', '写报告', 9, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (3, 'writing', '润色', 8, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (4, 'coding', '写代码', 10, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (5, 'coding', 'debug', 9, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (6, 'coding', '重构', 8, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (7, 'analysis', '分析', 10, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (8, 'analysis', '报表', 9, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (9, 'translate', '翻译', 10, 0, 1, NOW());
INSERT INTO `ai_intent_keyword` (`id`, `intent`, `keyword`, `weight`, `isRegex`, `enabled`, `createdAt`) VALUES (10, 'translate', 'convert', 8, 0, 1, NOW());

-- ===========================================
-- 5. AI 工具 (21 个)
-- ===========================================
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (1, 'nl2sql', 'NL2SQL', '自然语言转 SQL', 'data', '1.0.0', 1, '{"query":"string"}', '{"sql":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (2, 'nl2chart', 'NL2Chart', '自然语言转图表', 'data', '1.0.0', 1, '{"query":"string","data":"json"}', '{"chart":"base64"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (3, 'doc.parse', '文档解析', 'PDF/Word/Excel 解析', 'document', '1.0.0', 1, '{"url":"string"}', '{"text":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (4, 'doc.summary', '文档摘要', '长文本摘要', 'document', '1.0.0', 1, '{"text":"string"}', '{"summary":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (5, 'crdt.merge', 'CRDT 合并', '多人协同冲突合并', 'collab', '1.0.0', 1, '{"text":"string","ops":"json"}', '{"text":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (6, 'code.gen', '代码生成', '自然语言转代码', 'coding', '1.0.0', 1, '{"spec":"string","lang":"string"}', '{"code":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (7, 'code.review', '代码审查', '静态分析+建议', 'coding', '1.0.0', 1, '{"code":"string"}', '{"review":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (8, 'project.pack', '项目打包', 'ZIP 打包源码', 'coding', '1.0.0', 1, '{"files":"json"}', '{"zip":"base64"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (9, 'kg.extract', '知识图谱抽取', '实体关系抽取', 'kg', '1.0.0', 1, '{"text":"string"}', '{"entities":"json","relations":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (10, 'rag.search', 'RAG 检索', '向量+关键词混合检索', 'rag', '1.0.0', 1, '{"query":"string","topK":5}', '{"docs":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (11, 'moderation.text', '文本审核', '敏感词+合规检测', 'compliance', '1.0.0', 1, '{"text":"string"}', '{"safe":"boolean","hits":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (12, 'moderation.image', '图像审核', 'NSFW 检测', 'compliance', '1.0.0', 1, '{"url":"string"}', '{"safe":"boolean"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (13, 'function.call', '函数调用', 'OpenAI Functions 协议', 'function', '1.0.0', 1, '{"name":"string","args":"json"}', '{"result":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (14, 'pipeline.run', 'Pipeline 执行', '多阶段 AI 管线', 'pipeline', '1.0.0', 1, '{"workflow":"string","input":"json"}', '{"output":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (15, 'music.gen', '音乐生成', '文本转 MIDI', 'multimodal', '1.0.0', 1, '{"prompt":"string"}', '{"midi":"base64"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (16, 'vision.describe', '图像描述', 'Vision API', 'multimodal', '1.0.0', 1, '{"image":"base64"}', '{"text":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (17, 'intent.recog', '意图识别', '问句意图分类', 'agent', '1.0.0', 1, '{"query":"string"}', '{"intent":"string","score":"number"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (18, 'agent.route', 'Agent 路由', '能力匹配路由', 'agent', '1.0.0', 1, '{"query":"string"}', '{"agent":"string"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (19, 'agent.exec', 'Agent 执行', 'ReAct 循环', 'agent', '1.0.0', 1, '{"task":"string"}', '{"output":"string","steps":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (20, 'embedding', '向量化', '文本→向量', 'embedding', '1.0.0', 1, '{"text":"string"}', '{"vector":"json"}', NOW(), NOW());
INSERT INTO `ai_tool` (`id`, `code`, `name`, `description`, `category`, `version`, `enabled`, `inputSchema`, `outputSchema`, `createdAt`, `updatedAt`) VALUES (21, 'ppt.gen', 'PPT 生成', '大纲→PPTX', 'document', '1.0.0', 1, '{"outline":"string","theme":"string"}', '{"file":"base64"}', NOW(), NOW());

-- ===========================================
-- 6. 提示词模板 (4 个)
-- ===========================================
INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `createdAt`, `updatedAt`) VALUES (1, '系统基础', '系统默认提示词', 'system', '你是 MiniMax AI 助手。', NOW(), NOW());
INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `createdAt`, `updatedAt`) VALUES (2, '写文章', '通用文章模板', 'writing', '请以专业清晰的口吻撰写。', NOW(), NOW());
INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `createdAt`, `updatedAt`) VALUES (3, '代码审查', '代码审查模板', 'coding', '从可读性/性能/安全性审查。', NOW(), NOW());
INSERT INTO `prompt_template` (`id`, `name`, `description`, `category`, `content`, `createdAt`, `updatedAt`) VALUES (4, '分析报告', '分析报告模板', 'analysis', '基于数据生成分析报告。', NOW(), NOW());

-- ===========================================
-- 7. 模型提供方 (4 个)
-- ===========================================
INSERT INTO `model_provider` (`id`, `code`, `name`, `baseUrl`, `protocol`, `enabled`, `sort`, `createdAt`, `updatedAt`) VALUES (1, 'mock', 'Mock 提供方', 'builtin://mock', 'openai', 1, 0, NOW(), NOW());
INSERT INTO `model_provider` (`id`, `code`, `name`, `baseUrl`, `protocol`, `enabled`, `sort`, `createdAt`, `updatedAt`) VALUES (2, 'builtin', '自研提供方', 'builtin://self', 'openai', 1, 1, NOW(), NOW());
INSERT INTO `model_provider` (`id`, `code`, `name`, `baseUrl`, `protocol`, `enabled`, `sort`, `createdAt`, `updatedAt`) VALUES (3, 'onnx-local', '本地 ONNX', '/var/minimax/models', 'onnx', 0, 2, NOW(), NOW());
INSERT INTO `model_provider` (`id`, `code`, `name`, `baseUrl`, `protocol`, `enabled`, `sort`, `createdAt`, `updatedAt`) VALUES (4, 'openai', 'OpenAI 兼容', 'https://api.openai.com/v1', 'openai', 0, 9, NOW(), NOW());

-- ===========================================
-- 8. 数据源 (2 个) (只含实际列, userId 必须不同)
-- ===========================================
INSERT INTO `analytics_datasource` (`id`, `userId`, `name`, `type`, `passwordEnc`, `createdAt`, `updatedAt`) VALUES (1, 1, 'H2 默认', 'h2', '', NOW(), NOW());
INSERT INTO `analytics_datasource` (`id`, `userId`, `name`, `type`, `passwordEnc`, `createdAt`, `updatedAt`) VALUES (2, 2, 'MySQL 默认', 'mysql', '', NOW(), NOW());

-- ===========================================
-- 9. 告警规则 (4 个) (实际列: metricName 不是 metric)
-- ===========================================
INSERT INTO `alert_rule` (`id`, `name`, `metricName`, `operator`, `threshold`, `severity`, `enabled`, `createdAt`, `updatedAt`) VALUES (1, 'CPU 高', 'cpu_usage', '>', 0.9, 'critical', 1, NOW(), NOW());
INSERT INTO `alert_rule` (`id`, `name`, `metricName`, `operator`, `threshold`, `severity`, `enabled`, `createdAt`, `updatedAt`) VALUES (2, '内存高', 'memory_usage', '>', 0.85, 'warning', 1, NOW(), NOW());
INSERT INTO `alert_rule` (`id`, `name`, `metricName`, `operator`, `threshold`, `severity`, `enabled`, `createdAt`, `updatedAt`) VALUES (3, 'API 错误率高', 'api_error_rate', '>', 0.05, 'warning', 1, NOW(), NOW());
INSERT INTO `alert_rule` (`id`, `name`, `metricName`, `operator`, `threshold`, `severity`, `enabled`, `createdAt`, `updatedAt`) VALUES (4, 'AI 响应慢', 'ai_latency_ms', '>', 5000, 'warning', 1, NOW(), NOW());

-- ===========================================
-- 10. 敏感词 (3 个)
-- ===========================================
INSERT INTO `sensitive_word` (`id`, `word`, `category`, `level`, `createdAt`) VALUES (1, '违禁词示例', 'politics', 3, NOW());
INSERT INTO `sensitive_word` (`id`, `word`, `category`, `level`, `createdAt`) VALUES (2, '广告', 'ad', 1, NOW());
INSERT INTO `sensitive_word` (`id`, `word`, `category`, `level`, `createdAt`) VALUES (3, '暴力', 'violence', 2, NOW());

-- ===========================================
-- 11. 插件 (3 个) (实际列: name + displayName 等)
-- ===========================================
INSERT INTO `plugin` (`id`, `name`, `displayName`, `version`, `enabled`, `config`, `createdAt`, `updatedAt`) VALUES (1, 'pwa', 'PWA 离线插件', '1.0.0', 1, '{"cacheStrategy":"NetworkFirst"}', NOW(), NOW());
INSERT INTO `plugin` (`id`, `name`, `displayName`, `version`, `enabled`, `config`, `createdAt`, `updatedAt`) VALUES (2, 'capacitor', '移动端插件', '1.0.0', 1, '{"appId":"com.minimax.platform"}', NOW(), NOW());
INSERT INTO `plugin` (`id`, `name`, `displayName`, `version`, `enabled`, `config`, `createdAt`, `updatedAt`) VALUES (3, 'i18n', '国际化插件', '1.0.0', 1, '{"default":"zh-CN"}', NOW(), NOW());

-- ===========================================
-- 12. 协作房间 (跳过: gen_ddl.py 生成的 collab_room 不含 name 列)
-- ===========================================

-- 种子数据完毕 (已删除 ON DUPLICATE KEY UPDATE 以保证 H2/MySQL 兼容性)
