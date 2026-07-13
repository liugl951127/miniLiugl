-- =============================================================
-- MiniMax Platform V3.5.5+ 自动生成的 SQL 修复补丁
-- 基于 SQL vs Entity 字段差异比对 (scripts/diff_sql_entity.py)
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_missing.sql
-- 或在 docker-compose 启动后, 手动执行
-- =============================================================

-- === 2. ALTER TABLE 添加缺失字段 (75 张表) ===

-- admin_audit_log (admin)
ALTER TABLE `admin_audit_log` ADD COLUMN `detail` VARCHAR(255) DEFAULT NULL COMMENT 'detail(detail)';
ALTER TABLE `admin_audit_log` ADD COLUMN `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)';
ALTER TABLE `admin_audit_log` ADD COLUMN `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)';
ALTER TABLE `admin_audit_log` ADD COLUMN `resourceType` VARCHAR(255) DEFAULT NULL COMMENT 'resourceType(resourceType)';
ALTER TABLE `admin_audit_log` ADD COLUMN `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)';
ALTER TABLE `admin_audit_log` ADD COLUMN `resourceId` VARCHAR(255) DEFAULT NULL COMMENT 'resourceId(resourceId)';
ALTER TABLE `admin_audit_log` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';
ALTER TABLE `admin_audit_log` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';

-- agent_group (ai)
ALTER TABLE `agent_group` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `agent_group` ADD COLUMN `lastRunAt` DATETIME DEFAULT NULL COMMENT 'lastRunAt(lastRunAt)';
ALTER TABLE `agent_group` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `agent_group` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `agent_group` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `agent_group` ADD COLUMN `strategy` VARCHAR(255) DEFAULT NULL COMMENT 'strategy(strategy)';
ALTER TABLE `agent_group` ADD COLUMN `membersJson` VARCHAR(255) DEFAULT NULL COMMENT 'membersJson(membersJson)';
ALTER TABLE `agent_group` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `agent_group` ADD COLUMN `runCount` INT DEFAULT 0 COMMENT 'runCount(runCount)';

-- agent_task (agent)
ALTER TABLE `agent_task` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `agent_task` ADD COLUMN `goal` VARCHAR(255) DEFAULT NULL COMMENT 'goal(goal)';
ALTER TABLE `agent_task` ADD COLUMN `latencyMs` BIGINT DEFAULT 0 COMMENT 'latencyMs(latencyMs)';
ALTER TABLE `agent_task` ADD COLUMN `toolCalls` INT DEFAULT 0 COMMENT 'toolCalls(toolCalls)';
ALTER TABLE `agent_task` ADD COLUMN `llmCalls` INT DEFAULT 0 COMMENT 'llmCalls(llmCalls)';
ALTER TABLE `agent_task` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `agent_task` ADD COLUMN `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)';
ALTER TABLE `agent_task` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `agent_task` ADD COLUMN `totalTokens` INT DEFAULT 0 COMMENT 'totalTokens(totalTokens)';
ALTER TABLE `agent_task` ADD COLUMN `rounds` INT DEFAULT 0 COMMENT 'rounds(rounds)';
ALTER TABLE `agent_task` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';

-- ai_chat_message (ai)
ALTER TABLE `ai_chat_message` ADD COLUMN `toolCode` VARCHAR(255) DEFAULT NULL COMMENT 'toolCode(toolCode)';
ALTER TABLE `ai_chat_message` ADD COLUMN `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)';
ALTER TABLE `ai_chat_message` ADD COLUMN `toolOutput` VARCHAR(255) DEFAULT NULL COMMENT 'toolOutput(toolOutput)';
ALTER TABLE `ai_chat_message` ADD COLUMN `toolInput` VARCHAR(255) DEFAULT NULL COMMENT 'toolInput(toolInput)';
ALTER TABLE `ai_chat_message` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';

-- ai_chat_session (ai)
ALTER TABLE `ai_chat_session` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `ai_chat_session` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `ai_chat_session` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';
ALTER TABLE `ai_chat_session` ADD COLUMN `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)';

-- ai_generation_log (ai)
ALTER TABLE `ai_generation_log` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `ai_generation_log` ADD COLUMN `negativePrompt` VARCHAR(255) DEFAULT NULL COMMENT 'negativePrompt(negativePrompt)';
ALTER TABLE `ai_generation_log` ADD COLUMN `modelVersion` VARCHAR(255) DEFAULT NULL COMMENT 'modelVersion(modelVersion)';
ALTER TABLE `ai_generation_log` ADD COLUMN `prompt` VARCHAR(255) DEFAULT NULL COMMENT 'prompt(prompt)';
ALTER TABLE `ai_generation_log` ADD COLUMN `watermarked` INT DEFAULT 0 COMMENT 'watermarked(watermarked)';
ALTER TABLE `ai_generation_log` ADD COLUMN `modality` VARCHAR(255) DEFAULT NULL COMMENT 'modality(modality)';
ALTER TABLE `ai_generation_log` ADD COLUMN `watermarkText` VARCHAR(255) DEFAULT NULL COMMENT 'watermarkText(watermarkText)';
ALTER TABLE `ai_generation_log` ADD COLUMN `outputSize` BIGINT DEFAULT 0 COMMENT 'outputSize(outputSize)';
ALTER TABLE `ai_generation_log` ADD COLUMN `outputHash` VARCHAR(255) DEFAULT NULL COMMENT 'outputHash(outputHash)';
ALTER TABLE `ai_generation_log` ADD COLUMN `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `ai_generation_log` ADD COLUMN `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters(parameters)';
ALTER TABLE `ai_generation_log` ADD COLUMN `outputUrl` VARCHAR(255) DEFAULT NULL COMMENT 'outputUrl(outputUrl)';
ALTER TABLE `ai_generation_log` ADD COLUMN `modelName` VARCHAR(255) DEFAULT NULL COMMENT 'modelName(modelName)';
ALTER TABLE `ai_generation_log` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `ai_generation_log` ADD COLUMN `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp(userIp)';
ALTER TABLE `ai_generation_log` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';
ALTER TABLE `ai_generation_log` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';

-- ai_intent_keyword (ai)
ALTER TABLE `ai_intent_keyword` ADD COLUMN `weight` INT DEFAULT 0 COMMENT 'weight(weight)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `language` VARCHAR(255) DEFAULT NULL COMMENT 'language(language)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `keyword` VARCHAR(255) DEFAULT NULL COMMENT 'keyword(keyword)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `isRegex` INT DEFAULT 0 COMMENT 'isRegex(isRegex)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `updatedAt` DATETIME DEFAULT NULL COMMENT 'updatedAt(updatedAt)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark(remark)';

-- ai_tool (ai)
ALTER TABLE `ai_tool` ADD COLUMN `version` VARCHAR(255) DEFAULT NULL COMMENT 'version(version)';
ALTER TABLE `ai_tool` ADD COLUMN `implValue` VARCHAR(255) DEFAULT NULL COMMENT 'implValue(implValue)';
ALTER TABLE `ai_tool` ADD COLUMN `outputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'outputSchema(outputSchema)';
ALTER TABLE `ai_tool` ADD COLUMN `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon(icon)';
ALTER TABLE `ai_tool` ADD COLUMN `rateLimit` INT DEFAULT 0 COMMENT 'rateLimit(rateLimit)';
ALTER TABLE `ai_tool` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `ai_tool` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `ai_tool` ADD COLUMN `builtin` INT DEFAULT 0 COMMENT 'builtin(builtin)';
ALTER TABLE `ai_tool` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `ai_tool` ADD COLUMN `inputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'inputSchema(inputSchema)';
ALTER TABLE `ai_tool` ADD COLUMN `author` VARCHAR(255) DEFAULT NULL COMMENT 'author(author)';
ALTER TABLE `ai_tool` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `ai_tool` ADD COLUMN `implType` VARCHAR(255) DEFAULT NULL COMMENT 'implType(implType)';
ALTER TABLE `ai_tool` ADD COLUMN `timeoutSeconds` INT DEFAULT 0 COMMENT 'timeoutSeconds(timeoutSeconds)';
ALTER TABLE `ai_tool` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `ai_tool` ADD COLUMN `roleRequired` VARCHAR(255) DEFAULT NULL COMMENT 'roleRequired(roleRequired)';
ALTER TABLE `ai_tool` ADD COLUMN `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)';
ALTER TABLE `ai_tool` ADD COLUMN `defaultConfig` VARCHAR(255) DEFAULT NULL COMMENT 'defaultConfig(defaultConfig)';

-- ai_tool_invocation (ai)
ALTER TABLE `ai_tool_invocation` ADD COLUMN `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `inputJson` VARCHAR(255) DEFAULT NULL COMMENT 'inputJson(inputJson)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `outputJson` VARCHAR(255) DEFAULT NULL COMMENT 'outputJson(outputJson)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `dataSourceId` BIGINT DEFAULT 0 COMMENT 'dataSourceId(dataSourceId)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';

-- alert_channel (monitor)
ALTER TABLE `alert_channel` ADD COLUMN `type` VARCHAR(255) DEFAULT NULL COMMENT 'type(type)';
ALTER TABLE `alert_channel` ADD COLUMN `channelType` VARCHAR(255) DEFAULT NULL COMMENT 'channelType(channelType)';
ALTER TABLE `alert_channel` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `alert_channel` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `alert_channel` ADD COLUMN `config` VARCHAR(255) DEFAULT NULL COMMENT 'config(config)';
ALTER TABLE `alert_channel` ADD COLUMN `priority` INT DEFAULT 0 COMMENT 'priority(priority)';
ALTER TABLE `alert_channel` ADD COLUMN `target` VARCHAR(255) DEFAULT NULL COMMENT 'target(target)';

-- alert_event (monitor)
ALTER TABLE `alert_event` ADD COLUMN `ackedBy` BIGINT DEFAULT 0 COMMENT 'ackedBy(ackedBy)';
ALTER TABLE `alert_event` ADD COLUMN `resolvedAt` DATETIME DEFAULT NULL COMMENT 'resolvedAt(resolvedAt)';
ALTER TABLE `alert_event` ADD COLUMN `ackedAt` DATETIME DEFAULT NULL COMMENT 'ackedAt(ackedAt)';
ALTER TABLE `alert_event` ADD COLUMN `duration` BIGINT DEFAULT 0 COMMENT 'duration(duration)';
ALTER TABLE `alert_event` ADD COLUMN `metricName` VARCHAR(255) DEFAULT NULL COMMENT 'metricName(metricName)';
ALTER TABLE `alert_event` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `alert_event` ADD COLUMN `message` VARCHAR(255) DEFAULT NULL COMMENT 'message(message)';
ALTER TABLE `alert_event` ADD COLUMN `firedAt` DATETIME DEFAULT NULL COMMENT 'firedAt(firedAt)';
ALTER TABLE `alert_event` ADD COLUMN `severity` VARCHAR(255) DEFAULT NULL COMMENT 'severity(severity)';

-- alert_rule (monitor)
ALTER TABLE `alert_rule` ADD COLUMN `cooldownMinutes` INT DEFAULT 0 COMMENT 'cooldownMinutes(cooldownMinutes)';
ALTER TABLE `alert_rule` ADD COLUMN `notifyChannel` VARCHAR(255) DEFAULT NULL COMMENT 'notifyChannel(notifyChannel)';
ALTER TABLE `alert_rule` ADD COLUMN `service` VARCHAR(255) DEFAULT NULL COMMENT 'service(service)';
ALTER TABLE `alert_rule` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `alert_rule` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `alert_rule` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `alert_rule` ADD COLUMN `metricName` VARCHAR(255) DEFAULT NULL COMMENT 'metricName(metricName)';
ALTER TABLE `alert_rule` ADD COLUMN `operator` VARCHAR(255) DEFAULT NULL COMMENT 'operator(operator)';
ALTER TABLE `alert_rule` ADD COLUMN `severity` VARCHAR(255) DEFAULT NULL COMMENT 'severity(severity)';

-- analytics_datasource (analytics)
ALTER TABLE `analytics_datasource` ADD COLUMN `type` VARCHAR(255) DEFAULT NULL COMMENT 'type(type)';
ALTER TABLE `analytics_datasource` ADD COLUMN `jdbcUrl` VARCHAR(255) DEFAULT NULL COMMENT 'jdbcUrl(jdbcUrl)';
ALTER TABLE `analytics_datasource` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `analytics_datasource` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `analytics_datasource` ADD COLUMN `passwordEnc` VARCHAR(255) DEFAULT NULL COMMENT 'passwordEnc(passwordEnc)';
ALTER TABLE `analytics_datasource` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `analytics_datasource` ADD COLUMN `updatedAt` DATETIME DEFAULT NULL COMMENT 'updatedAt(updatedAt)';
ALTER TABLE `analytics_datasource` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';

-- analytics_ingest_task (analytics)
ALTER TABLE `analytics_ingest_task` ADD COLUMN `fileType` VARCHAR(255) DEFAULT NULL COMMENT 'fileType(fileType)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `finishedAt` DATETIME DEFAULT NULL COMMENT 'finishedAt(finishedAt)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `filename` VARCHAR(255) DEFAULT NULL COMMENT 'filename(filename)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `qualityJson` VARCHAR(255) DEFAULT NULL COMMENT 'qualityJson(qualityJson)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `totalColumns` BIGINT DEFAULT 0 COMMENT 'totalColumns(totalColumns)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `encoding` VARCHAR(255) DEFAULT NULL COMMENT 'encoding(encoding)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `totalRows` BIGINT DEFAULT 0 COMMENT 'totalRows(totalRows)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `separator` VARCHAR(255) DEFAULT NULL COMMENT 'separator(separator)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `columnsJson` VARCHAR(255) DEFAULT NULL COMMENT 'columnsJson(columnsJson)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `fileSize` BIGINT DEFAULT 0 COMMENT 'fileSize(fileSize)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `analytics_ingest_task` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';

-- analytics_nlsql_history (analytics)
ALTER TABLE `analytics_nlsql_history` ADD COLUMN `generatedSql` VARCHAR(255) DEFAULT NULL COMMENT 'generatedSql(generatedSql)';
ALTER TABLE `analytics_nlsql_history` ADD COLUMN `question` VARCHAR(255) DEFAULT NULL COMMENT 'question(question)';
ALTER TABLE `analytics_nlsql_history` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `analytics_nlsql_history` ADD COLUMN `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)';
ALTER TABLE `analytics_nlsql_history` ADD COLUMN `promptTokens` INT DEFAULT 0 COMMENT 'promptTokens(promptTokens)';
ALTER TABLE `analytics_nlsql_history` ADD COLUMN `correctedSql` VARCHAR(255) DEFAULT NULL COMMENT 'correctedSql(correctedSql)';

-- analytics_report (analytics)
ALTER TABLE `analytics_report` ADD COLUMN `durationMs` BIGINT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `analytics_report` ADD COLUMN `sqlText` VARCHAR(255) DEFAULT NULL COMMENT 'sqlText(sqlText)';
ALTER TABLE `analytics_report` ADD COLUMN `question` VARCHAR(255) DEFAULT NULL COMMENT 'question(question)';
ALTER TABLE `analytics_report` ADD COLUMN `chartOptionsJson` VARCHAR(255) DEFAULT NULL COMMENT 'chartOptionsJson(chartOptionsJson)';
ALTER TABLE `analytics_report` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `analytics_report` ADD COLUMN `markdown` VARCHAR(255) DEFAULT NULL COMMENT 'markdown(markdown)';
ALTER TABLE `analytics_report` ADD COLUMN `format` VARCHAR(255) DEFAULT NULL COMMENT 'format(format)';
ALTER TABLE `analytics_report` ADD COLUMN `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)';
ALTER TABLE `analytics_report` ADD COLUMN `rowCount` BIGINT DEFAULT 0 COMMENT 'rowCount(rowCount)';

-- audit_log (ai)
ALTER TABLE `audit_log` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `audit_log` ADD COLUMN `requestBody` VARCHAR(255) DEFAULT NULL COMMENT 'requestBody(requestBody)';
ALTER TABLE `audit_log` ADD COLUMN `responseStatus` INT DEFAULT 0 COMMENT 'responseStatus(responseStatus)';
ALTER TABLE `audit_log` ADD COLUMN `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)';
ALTER TABLE `audit_log` ADD COLUMN `resourceId` VARCHAR(255) DEFAULT NULL COMMENT 'resourceId(resourceId)';
ALTER TABLE `audit_log` ADD COLUMN `method` VARCHAR(255) DEFAULT NULL COMMENT 'method(method)';
ALTER TABLE `audit_log` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';
ALTER TABLE `audit_log` ADD COLUMN `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `audit_log` ADD COLUMN `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)';
ALTER TABLE `audit_log` ADD COLUMN `path` VARCHAR(255) DEFAULT NULL COMMENT 'path(path)';
ALTER TABLE `audit_log` ADD COLUMN `resourceType` VARCHAR(255) DEFAULT NULL COMMENT 'resourceType(resourceType)';
ALTER TABLE `audit_log` ADD COLUMN `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp(userIp)';
ALTER TABLE `audit_log` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';
ALTER TABLE `audit_log` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';

-- audit_log_full (admin)
ALTER TABLE `audit_log_full` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `audit_log_full` ADD COLUMN `requestBody` VARCHAR(255) DEFAULT NULL COMMENT 'requestBody(requestBody)';
ALTER TABLE `audit_log_full` ADD COLUMN `responseStatus` INT DEFAULT 0 COMMENT 'responseStatus(responseStatus)';
ALTER TABLE `audit_log_full` ADD COLUMN `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)';
ALTER TABLE `audit_log_full` ADD COLUMN `resourceId` VARCHAR(255) DEFAULT NULL COMMENT 'resourceId(resourceId)';
ALTER TABLE `audit_log_full` ADD COLUMN `method` VARCHAR(255) DEFAULT NULL COMMENT 'method(method)';
ALTER TABLE `audit_log_full` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';
ALTER TABLE `audit_log_full` ADD COLUMN `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `audit_log_full` ADD COLUMN `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)';
ALTER TABLE `audit_log_full` ADD COLUMN `path` VARCHAR(255) DEFAULT NULL COMMENT 'path(path)';
ALTER TABLE `audit_log_full` ADD COLUMN `resourceType` VARCHAR(255) DEFAULT NULL COMMENT 'resourceType(resourceType)';
ALTER TABLE `audit_log_full` ADD COLUMN `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp(userIp)';
ALTER TABLE `audit_log_full` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';
ALTER TABLE `audit_log_full` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';

-- auth_login_log (auth)
ALTER TABLE `auth_login_log` ADD COLUMN `status` INT DEFAULT 0 COMMENT 'status(status)';
ALTER TABLE `auth_login_log` ADD COLUMN `message` VARCHAR(255) DEFAULT NULL COMMENT 'message(message)';
ALTER TABLE `auth_login_log` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';
ALTER TABLE `auth_login_log` ADD COLUMN `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)';

-- auth_refresh_token (auth)
ALTER TABLE `auth_refresh_token` ADD COLUMN `expiresAt` DATETIME DEFAULT NULL COMMENT 'expiresAt(expiresAt)';
ALTER TABLE `auth_refresh_token` ADD COLUMN `revoked` INT DEFAULT 0 COMMENT 'revoked(revoked)';

-- billing_record (ai)
ALTER TABLE `billing_record` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `billing_record` ADD COLUMN `externalTransactionId` VARCHAR(255) DEFAULT NULL COMMENT 'externalTransactionId(externalTransactionId)';
ALTER TABLE `billing_record` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `billing_record` ADD COLUMN `currency` VARCHAR(255) DEFAULT NULL COMMENT 'currency(currency)';
ALTER TABLE `billing_record` ADD COLUMN `recordType` VARCHAR(255) DEFAULT NULL COMMENT 'recordType(recordType)';
ALTER TABLE `billing_record` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `billing_record` ADD COLUMN `licenseId` BIGINT DEFAULT 0 COMMENT 'licenseId(licenseId)';
ALTER TABLE `billing_record` ADD COLUMN `paymentMethod` VARCHAR(255) DEFAULT NULL COMMENT 'paymentMethod(paymentMethod)';
ALTER TABLE `billing_record` ADD COLUMN `modelEntryId` BIGINT DEFAULT 0 COMMENT 'modelEntryId(modelEntryId)';
ALTER TABLE `billing_record` ADD COLUMN `amountCents` BIGINT DEFAULT 0 COMMENT 'amountCents(amountCents)';

-- chat_message (chat)
ALTER TABLE `chat_message` ADD COLUMN `finishReason` VARCHAR(255) DEFAULT NULL COMMENT 'finishReason(finishReason)';
ALTER TABLE `chat_message` ADD COLUMN `tokens` INT DEFAULT 0 COMMENT 'tokens(tokens)';
ALTER TABLE `chat_message` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `chat_message` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `chat_message` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';

-- chat_session (chat)
ALTER TABLE `chat_session` ADD COLUMN `lastMessageAt` DATETIME DEFAULT NULL COMMENT 'lastMessageAt(lastMessageAt)';
ALTER TABLE `chat_session` ADD COLUMN `messageCount` INT DEFAULT 0 COMMENT 'messageCount(messageCount)';
ALTER TABLE `chat_session` ADD COLUMN `status` INT DEFAULT 0 COMMENT 'status(status)';
ALTER TABLE `chat_session` ADD COLUMN `systemPrompt` VARCHAR(255) DEFAULT NULL COMMENT 'systemPrompt(systemPrompt)';
ALTER TABLE `chat_session` ADD COLUMN `tenantId` BIGINT DEFAULT 0 COMMENT 'tenantId(tenantId)';
ALTER TABLE `chat_session` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `chat_session` ADD COLUMN `temperature` DECIMAL(20,4) DEFAULT 0 COMMENT 'temperature(temperature)';
ALTER TABLE `chat_session` ADD COLUMN `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)';

-- cluster_node (ai)
ALTER TABLE `cluster_node` ADD COLUMN `isLeader` TINYINT(1) DEFAULT 0 COMMENT 'isLeader(isLeader)';
ALTER TABLE `cluster_node` ADD COLUMN `cpuUsage` DOUBLE DEFAULT 0 COMMENT 'cpuUsage(cpuUsage)';
ALTER TABLE `cluster_node` ADD COLUMN `startedAt` DATETIME DEFAULT NULL COMMENT 'startedAt(startedAt)';
ALTER TABLE `cluster_node` ADD COLUMN `activeTasks` INT DEFAULT 0 COMMENT 'activeTasks(activeTasks)';
ALTER TABLE `cluster_node` ADD COLUMN `zone` VARCHAR(255) DEFAULT NULL COMMENT 'zone(zone)';
ALTER TABLE `cluster_node` ADD COLUMN `lastHeartbeat` DATETIME DEFAULT NULL COMMENT 'lastHeartbeat(lastHeartbeat)';
ALTER TABLE `cluster_node` ADD COLUMN `capabilities` VARCHAR(255) DEFAULT NULL COMMENT 'capabilities(capabilities)';
ALTER TABLE `cluster_node` ADD COLUMN `memoryUsage` DOUBLE DEFAULT 0 COMMENT 'memoryUsage(memoryUsage)';
ALTER TABLE `cluster_node` ADD COLUMN `totalCores` INT DEFAULT 0 COMMENT 'totalCores(totalCores)';
ALTER TABLE `cluster_node` ADD COLUMN `totalGpus` INT DEFAULT 0 COMMENT 'totalGpus(totalGpus)';
ALTER TABLE `cluster_node` ADD COLUMN `totalMemoryMb` BIGINT DEFAULT 0 COMMENT 'totalMemoryMb(totalMemoryMb)';
ALTER TABLE `cluster_node` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `cluster_node` ADD COLUMN `region` VARCHAR(255) DEFAULT NULL COMMENT 'region(region)';
ALTER TABLE `cluster_node` ADD COLUMN `address` VARCHAR(255) DEFAULT NULL COMMENT 'address(address)';
ALTER TABLE `cluster_node` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `cluster_node` ADD COLUMN `gpuUsage` DOUBLE DEFAULT 0 COMMENT 'gpuUsage(gpuUsage)';

-- collab_member (agent)
ALTER TABLE `collab_member` ADD COLUMN `joinedAt` DATETIME DEFAULT NULL COMMENT 'joinedAt(joinedAt)';
ALTER TABLE `collab_member` ADD COLUMN `lastActiveAt` DATETIME DEFAULT NULL COMMENT 'lastActiveAt(lastActiveAt)';

-- collab_message (ws)
ALTER TABLE `collab_message` ADD COLUMN `clientMsgId` VARCHAR(255) DEFAULT NULL COMMENT 'clientMsgId(clientMsgId)';
ALTER TABLE `collab_message` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `collab_message` ADD COLUMN `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata(metadata)';
ALTER TABLE `collab_message` ADD COLUMN `type` VARCHAR(255) DEFAULT NULL COMMENT 'type(type)';
ALTER TABLE `collab_message` ADD COLUMN `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)';
ALTER TABLE `collab_message` ADD COLUMN `broadcast` INT DEFAULT 0 COMMENT 'broadcast(broadcast)';
ALTER TABLE `collab_message` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `collab_message` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';
ALTER TABLE `collab_message` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';

-- collab_participant (ws)
ALTER TABLE `collab_participant` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `collab_participant` ADD COLUMN `cursorX` INT DEFAULT 0 COMMENT 'cursorX(cursorX)';
ALTER TABLE `collab_participant` ADD COLUMN `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)';
ALTER TABLE `collab_participant` ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)';
ALTER TABLE `collab_participant` ADD COLUMN `leftAt` DATETIME DEFAULT NULL COMMENT 'leftAt(leftAt)';
ALTER TABLE `collab_participant` ADD COLUMN `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)';
ALTER TABLE `collab_participant` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `collab_participant` ADD COLUMN `selectionId` VARCHAR(255) DEFAULT NULL COMMENT 'selectionId(selectionId)';
ALTER TABLE `collab_participant` ADD COLUMN `cursorY` INT DEFAULT 0 COMMENT 'cursorY(cursorY)';
ALTER TABLE `collab_participant` ADD COLUMN `joinedAt` DATETIME DEFAULT NULL COMMENT 'joinedAt(joinedAt)';
ALTER TABLE `collab_participant` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';
ALTER TABLE `collab_participant` ADD COLUMN `lastHeartbeat` DATETIME DEFAULT NULL COMMENT 'lastHeartbeat(lastHeartbeat)';

-- collab_room (ws)
ALTER TABLE `collab_room` ADD COLUMN `closedAt` DATETIME DEFAULT NULL COMMENT 'closedAt(closedAt)';
ALTER TABLE `collab_room` ADD COLUMN `type` VARCHAR(255) DEFAULT NULL COMMENT 'type(type)';
ALTER TABLE `collab_room` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `collab_room` ADD COLUMN `maxParticipants` INT DEFAULT 0 COMMENT 'maxParticipants(maxParticipants)';
ALTER TABLE `collab_room` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `collab_room` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `collab_room` ADD COLUMN `lastActivityAt` DATETIME DEFAULT NULL COMMENT 'lastActivityAt(lastActivityAt)';
ALTER TABLE `collab_room` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `collab_room` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `collab_room` ADD COLUMN `currentParticipants` INT DEFAULT 0 COMMENT 'currentParticipants(currentParticipants)';
ALTER TABLE `collab_room` ADD COLUMN `isPublic` INT DEFAULT 0 COMMENT 'isPublic(isPublic)';
ALTER TABLE `collab_room` ADD COLUMN `ownerName` VARCHAR(255) DEFAULT NULL COMMENT 'ownerName(ownerName)';

-- collab_session (agent)
ALTER TABLE `collab_session` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `collab_session` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `collab_session` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `collab_session` ADD COLUMN `maxUsers` INT DEFAULT 0 COMMENT 'maxUsers(maxUsers)';
ALTER TABLE `collab_session` ADD COLUMN `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)';

-- dashboard_metric (ai)
ALTER TABLE `dashboard_metric` ADD COLUMN `value` DOUBLE DEFAULT 0 COMMENT 'value(value)';
ALTER TABLE `dashboard_metric` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `dashboard_metric` ADD COLUMN `dimension` VARCHAR(255) DEFAULT NULL COMMENT 'dimension(dimension)';

-- data_source (ai)
ALTER TABLE `data_source` ADD COLUMN `maxLifetime` INT DEFAULT 0 COMMENT 'maxLifetime(maxLifetime)';
ALTER TABLE `data_source` ADD COLUMN `minIdle` INT DEFAULT 0 COMMENT 'minIdle(minIdle)';
ALTER TABLE `data_source` ADD COLUMN `type` VARCHAR(255) DEFAULT NULL COMMENT 'type(type)';
ALTER TABLE `data_source` ADD COLUMN `lastTestAt` DATETIME DEFAULT NULL COMMENT 'lastTestAt(lastTestAt)';
ALTER TABLE `data_source` ADD COLUMN `jdbcUrl` VARCHAR(255) DEFAULT NULL COMMENT 'jdbcUrl(jdbcUrl)';
ALTER TABLE `data_source` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `data_source` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `data_source` ADD COLUMN `testStatus` VARCHAR(255) DEFAULT NULL COMMENT 'testStatus(testStatus)';
ALTER TABLE `data_source` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `data_source` ADD COLUMN `password` VARCHAR(255) DEFAULT NULL COMMENT 'password(password)';
ALTER TABLE `data_source` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `data_source` ADD COLUMN `testMessage` VARCHAR(255) DEFAULT NULL COMMENT 'testMessage(testMessage)';
ALTER TABLE `data_source` ADD COLUMN `driverClass` VARCHAR(255) DEFAULT NULL COMMENT 'driverClass(driverClass)';
ALTER TABLE `data_source` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';
ALTER TABLE `data_source` ADD COLUMN `poolSize` INT DEFAULT 0 COMMENT 'poolSize(poolSize)';

-- document (rag)
ALTER TABLE `document` ADD COLUMN `sourceType` VARCHAR(255) DEFAULT NULL COMMENT 'sourceType(sourceType)';
ALTER TABLE `document` ADD COLUMN `chunkCount` INT DEFAULT 0 COMMENT 'chunkCount(chunkCount)';
ALTER TABLE `document` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `document` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `document` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `document` ADD COLUMN `sizeBytes` BIGINT DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)';
ALTER TABLE `document` ADD COLUMN `sourceUri` VARCHAR(255) DEFAULT NULL COMMENT 'sourceUri(sourceUri)';
ALTER TABLE `document` ADD COLUMN `checksum` VARCHAR(255) DEFAULT NULL COMMENT 'checksum(checksum)';
ALTER TABLE `document` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';
ALTER TABLE `document` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';

-- document_chunk (rag)
ALTER TABLE `document_chunk` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `document_chunk` ADD COLUMN `embedding` BLOB DEFAULT NULL COMMENT 'embedding(embedding)';

-- function_call_log (function)
ALTER TABLE `function_call_log` ADD COLUMN `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `function_call_log` ADD COLUMN `arguments` VARCHAR(255) DEFAULT NULL COMMENT 'arguments(arguments)';
ALTER TABLE `function_call_log` ADD COLUMN `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)';
ALTER TABLE `function_call_log` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `function_call_log` ADD COLUMN `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)';
ALTER TABLE `function_call_log` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';
ALTER TABLE `function_call_log` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';

-- function_tool (function)
ALTER TABLE `function_tool` ADD COLUMN `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters(parameters)';
ALTER TABLE `function_tool` ADD COLUMN `endpoint` VARCHAR(255) DEFAULT NULL COMMENT 'endpoint(endpoint)';
ALTER TABLE `function_tool` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `function_tool` ADD COLUMN `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)';
ALTER TABLE `function_tool` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `function_tool` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `function_tool` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `function_tool` ADD COLUMN `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName(displayName)';
ALTER TABLE `function_tool` ADD COLUMN `httpMethod` VARCHAR(255) DEFAULT NULL COMMENT 'httpMethod(httpMethod)';
ALTER TABLE `function_tool` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `function_tool` ADD COLUMN `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)';

-- kb_chunk (ai)
ALTER TABLE `kb_chunk` ADD COLUMN `embeddingModel` VARCHAR(255) DEFAULT NULL COMMENT 'embeddingModel(embeddingModel)';
ALTER TABLE `kb_chunk` ADD COLUMN `embedding` VARCHAR(255) DEFAULT NULL COMMENT 'embedding(embedding)';
ALTER TABLE `kb_chunk` ADD COLUMN `kbId` VARCHAR(255) DEFAULT NULL COMMENT 'kbId(kbId)';
ALTER TABLE `kb_chunk` ADD COLUMN `summary` VARCHAR(255) DEFAULT NULL COMMENT 'summary(summary)';
ALTER TABLE `kb_chunk` ADD COLUMN `tokenCount` INT DEFAULT 0 COMMENT 'tokenCount(tokenCount)';
ALTER TABLE `kb_chunk` ADD COLUMN `seq` INT DEFAULT 0 COMMENT 'seq(seq)';
ALTER TABLE `kb_chunk` ADD COLUMN `docId` VARCHAR(255) DEFAULT NULL COMMENT 'docId(docId)';
ALTER TABLE `kb_chunk` ADD COLUMN `charCount` INT DEFAULT 0 COMMENT 'charCount(charCount)';
ALTER TABLE `kb_chunk` ADD COLUMN `location` VARCHAR(255) DEFAULT NULL COMMENT 'location(location)';
ALTER TABLE `kb_chunk` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';
ALTER TABLE `kb_chunk` ADD COLUMN `keywords` VARCHAR(255) DEFAULT NULL COMMENT 'keywords(keywords)';

-- kb_document (ai)
ALTER TABLE `kb_document` ADD COLUMN `embeddingCount` INT DEFAULT 0 COMMENT 'embeddingCount(embeddingCount)';
ALTER TABLE `kb_document` ADD COLUMN `kbId` VARCHAR(255) DEFAULT NULL COMMENT 'kbId(kbId)';
ALTER TABLE `kb_document` ADD COLUMN `chunkCount` INT DEFAULT 0 COMMENT 'chunkCount(chunkCount)';
ALTER TABLE `kb_document` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `kb_document` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `kb_document` ADD COLUMN `sizeBytes` BIGINT DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)';
ALTER TABLE `kb_document` ADD COLUMN `mimeType` VARCHAR(255) DEFAULT NULL COMMENT 'mimeType(mimeType)';
ALTER TABLE `kb_document` ADD COLUMN `filePath` VARCHAR(255) DEFAULT NULL COMMENT 'filePath(filePath)';
ALTER TABLE `kb_document` ADD COLUMN `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)';
ALTER TABLE `kb_document` ADD COLUMN `filename` VARCHAR(255) DEFAULT NULL COMMENT 'filename(filename)';
ALTER TABLE `kb_document` ADD COLUMN `source` VARCHAR(255) DEFAULT NULL COMMENT 'source(source)';
ALTER TABLE `kb_document` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `kb_document` ADD COLUMN `sourceUrl` VARCHAR(255) DEFAULT NULL COMMENT 'sourceUrl(sourceUrl)';
ALTER TABLE `kb_document` ADD COLUMN `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)';
ALTER TABLE `kb_document` ADD COLUMN `isPublic` TINYINT(1) DEFAULT 0 COMMENT 'isPublic(isPublic)';

-- kb_permission (ai)
ALTER TABLE `kb_permission` ADD COLUMN `subjectId` BIGINT DEFAULT 0 COMMENT 'subjectId(subjectId)';
ALTER TABLE `kb_permission` ADD COLUMN `grantBy` BIGINT DEFAULT 0 COMMENT 'grantBy(grantBy)';
ALTER TABLE `kb_permission` ADD COLUMN `subjectType` VARCHAR(255) DEFAULT NULL COMMENT 'subjectType(subjectType)';
ALTER TABLE `kb_permission` ADD COLUMN `permission` VARCHAR(255) DEFAULT NULL COMMENT 'permission(permission)';

-- kg_entity (agent)
ALTER TABLE `kg_entity` ADD COLUMN `refCount` INT DEFAULT 0 COMMENT 'refCount(refCount)';
ALTER TABLE `kg_entity` ADD COLUMN `entityType` VARCHAR(255) DEFAULT NULL COMMENT 'entityType(entityType)';
ALTER TABLE `kg_entity` ADD COLUMN `aliases` VARCHAR(255) DEFAULT NULL COMMENT 'aliases(aliases)';
ALTER TABLE `kg_entity` ADD COLUMN `importance` INT DEFAULT 0 COMMENT 'importance(importance)';
ALTER TABLE `kg_entity` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `kg_entity` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `kg_entity` ADD COLUMN `source` VARCHAR(255) DEFAULT NULL COMMENT 'source(source)';

-- kg_relation (agent)
ALTER TABLE `kg_relation` ADD COLUMN `refCount` INT DEFAULT 0 COMMENT 'refCount(refCount)';
ALTER TABLE `kg_relation` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `kg_relation` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `kg_relation` ADD COLUMN `source` VARCHAR(255) DEFAULT NULL COMMENT 'source(source)';
ALTER TABLE `kg_relation` ADD COLUMN `weight` DECIMAL(20,4) DEFAULT 0 COMMENT 'weight(weight)';

-- knowledge_base (rag)
ALTER TABLE `knowledge_base` ADD COLUMN `visibility` VARCHAR(255) DEFAULT NULL COMMENT 'visibility(visibility)';
ALTER TABLE `knowledge_base` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `knowledge_base` ADD COLUMN `docCount` INT DEFAULT 0 COMMENT 'docCount(docCount)';
ALTER TABLE `knowledge_base` ADD COLUMN `chunkCount` INT DEFAULT 0 COMMENT 'chunkCount(chunkCount)';
ALTER TABLE `knowledge_base` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `knowledge_base` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';

-- metric_snapshot (monitor)
ALTER TABLE `metric_snapshot` ADD COLUMN `metricValue` DECIMAL(20,4) DEFAULT 0 COMMENT 'metricValue(metricValue)';
ALTER TABLE `metric_snapshot` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `metric_snapshot` ADD COLUMN `metricName` VARCHAR(255) DEFAULT NULL COMMENT 'metricName(metricName)';

-- model_battle_log (model)
ALTER TABLE `model_battle_log` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `model_battle_log` ADD COLUMN `latencyMs` INT DEFAULT 0 COMMENT 'latencyMs(latencyMs)';
ALTER TABLE `model_battle_log` ADD COLUMN `modelCode` VARCHAR(255) DEFAULT NULL COMMENT 'modelCode(modelCode)';
ALTER TABLE `model_battle_log` ADD COLUMN `prompt` VARCHAR(255) DEFAULT NULL COMMENT 'prompt(prompt)';
ALTER TABLE `model_battle_log` ADD COLUMN `completionTokens` INT DEFAULT 0 COMMENT 'completionTokens(completionTokens)';
ALTER TABLE `model_battle_log` ADD COLUMN `response` VARCHAR(255) DEFAULT NULL COMMENT 'response(response)';
ALTER TABLE `model_battle_log` ADD COLUMN `judgeModel` VARCHAR(255) DEFAULT NULL COMMENT 'judgeModel(judgeModel)';
ALTER TABLE `model_battle_log` ADD COLUMN `modelId` BIGINT DEFAULT 0 COMMENT 'modelId(modelId)';
ALTER TABLE `model_battle_log` ADD COLUMN `battleId` VARCHAR(255) DEFAULT NULL COMMENT 'battleId(battleId)';
ALTER TABLE `model_battle_log` ADD COLUMN `score` INT DEFAULT 0 COMMENT 'score(score)';
ALTER TABLE `model_battle_log` ADD COLUMN `judgeReason` VARCHAR(255) DEFAULT NULL COMMENT 'judgeReason(judgeReason)';
ALTER TABLE `model_battle_log` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `model_battle_log` ADD COLUMN `promptTokens` INT DEFAULT 0 COMMENT 'promptTokens(promptTokens)';
ALTER TABLE `model_battle_log` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `model_battle_log` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';

-- model_config (model)
ALTER TABLE `model_config` ADD COLUMN `sort` INT DEFAULT 0 COMMENT 'sort(sort)';
ALTER TABLE `model_config` ADD COLUMN `outputPrice` DECIMAL(20,4) DEFAULT 0 COMMENT 'outputPrice(outputPrice)';
ALTER TABLE `model_config` ADD COLUMN `inputPrice` DECIMAL(20,4) DEFAULT 0 COMMENT 'inputPrice(inputPrice)';
ALTER TABLE `model_config` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `model_config` ADD COLUMN `maxContext` INT DEFAULT 0 COMMENT 'maxContext(maxContext)';
ALTER TABLE `model_config` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `model_config` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `model_config` ADD COLUMN `maxOutput` INT DEFAULT 0 COMMENT 'maxOutput(maxOutput)';
ALTER TABLE `model_config` ADD COLUMN `supportsVision` INT DEFAULT 0 COMMENT 'supportsVision(supportsVision)';
ALTER TABLE `model_config` ADD COLUMN `supportsStream` INT DEFAULT 0 COMMENT 'supportsStream(supportsStream)';
ALTER TABLE `model_config` ADD COLUMN `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName(displayName)';
ALTER TABLE `model_config` ADD COLUMN `supportsTools` INT DEFAULT 0 COMMENT 'supportsTools(supportsTools)';

-- model_license (ai)
ALTER TABLE `model_license` ADD COLUMN `licenseType` VARCHAR(255) DEFAULT NULL COMMENT 'licenseType(licenseType)';
ALTER TABLE `model_license` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `model_license` ADD COLUMN `modelVersionId` BIGINT DEFAULT 0 COMMENT 'modelVersionId(modelVersionId)';
ALTER TABLE `model_license` ADD COLUMN `expireAt` DATETIME DEFAULT NULL COMMENT 'expireAt(expireAt)';
ALTER TABLE `model_license` ADD COLUMN `priceCents` BIGINT DEFAULT 0 COMMENT 'priceCents(priceCents)';
ALTER TABLE `model_license` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `model_license` ADD COLUMN `quotaCalls` BIGINT DEFAULT 0 COMMENT 'quotaCalls(quotaCalls)';
ALTER TABLE `model_license` ADD COLUMN `startAt` DATETIME DEFAULT NULL COMMENT 'startAt(startAt)';
ALTER TABLE `model_license` ADD COLUMN `usedCalls` BIGINT DEFAULT 0 COMMENT 'usedCalls(usedCalls)';
ALTER TABLE `model_license` ADD COLUMN `modelEntryId` BIGINT DEFAULT 0 COMMENT 'modelEntryId(modelEntryId)';

-- model_provider (model)
ALTER TABLE `model_provider` ADD COLUMN `sort` INT DEFAULT 0 COMMENT 'sort(sort)';
ALTER TABLE `model_provider` ADD COLUMN `apiKey` VARCHAR(255) DEFAULT NULL COMMENT 'apiKey(apiKey)';
ALTER TABLE `model_provider` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `model_provider` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `model_provider` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `model_provider` ADD COLUMN `protocol` VARCHAR(255) DEFAULT NULL COMMENT 'protocol(protocol)';
ALTER TABLE `model_provider` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `model_provider` ADD COLUMN `baseUrl` VARCHAR(255) DEFAULT NULL COMMENT 'baseUrl(baseUrl)';

-- model_version (ai)
ALTER TABLE `model_version` ADD COLUMN `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata(metadata)';
ALTER TABLE `model_version` ADD COLUMN `version` VARCHAR(255) DEFAULT NULL COMMENT 'version(version)';
ALTER TABLE `model_version` ADD COLUMN `outputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'outputSchema(outputSchema)';
ALTER TABLE `model_version` ADD COLUMN `inputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'inputSchema(inputSchema)';
ALTER TABLE `model_version` ADD COLUMN `isLatest` TINYINT(1) DEFAULT 0 COMMENT 'isLatest(isLatest)';
ALTER TABLE `model_version` ADD COLUMN `uploaderId` BIGINT DEFAULT 0 COMMENT 'uploaderId(uploaderId)';
ALTER TABLE `model_version` ADD COLUMN `changelog` VARCHAR(255) DEFAULT NULL COMMENT 'changelog(changelog)';
ALTER TABLE `model_version` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `model_version` ADD COLUMN `filePath` VARCHAR(255) DEFAULT NULL COMMENT 'filePath(filePath)';
ALTER TABLE `model_version` ADD COLUMN `sizeBytes` BIGINT DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)';
ALTER TABLE `model_version` ADD COLUMN `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)';
ALTER TABLE `model_version` ADD COLUMN `backwardCompatible` VARCHAR(255) DEFAULT NULL COMMENT 'backwardCompatible(backwardCompatible)';
ALTER TABLE `model_version` ADD COLUMN `modelEntryId` BIGINT DEFAULT 0 COMMENT 'modelEntryId(modelEntryId)';

-- moderation_record (ai)
ALTER TABLE `moderation_record` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `moderation_record` ADD COLUMN `contentHash` VARCHAR(255) DEFAULT NULL COMMENT 'contentHash(contentHash)';
ALTER TABLE `moderation_record` ADD COLUMN `rejectionReason` VARCHAR(255) DEFAULT NULL COMMENT 'rejectionReason(rejectionReason)';
ALTER TABLE `moderation_record` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';
ALTER TABLE `moderation_record` ADD COLUMN `riskLevel` VARCHAR(255) DEFAULT NULL COMMENT 'riskLevel(riskLevel)';
ALTER TABLE `moderation_record` ADD COLUMN `contentType` VARCHAR(255) DEFAULT NULL COMMENT 'contentType(contentType)';
ALTER TABLE `moderation_record` ADD COLUMN `moderator` VARCHAR(255) DEFAULT NULL COMMENT 'moderator(moderator)';
ALTER TABLE `moderation_record` ADD COLUMN `riskScore` DECIMAL(20,4) DEFAULT 0 COMMENT 'riskScore(riskScore)';
ALTER TABLE `moderation_record` ADD COLUMN `contentSize` BIGINT DEFAULT 0 COMMENT 'contentSize(contentSize)';
ALTER TABLE `moderation_record` ADD COLUMN `contentUrl` VARCHAR(255) DEFAULT NULL COMMENT 'contentUrl(contentUrl)';
ALTER TABLE `moderation_record` ADD COLUMN `riskLabels` VARCHAR(255) DEFAULT NULL COMMENT 'riskLabels(riskLabels)';
ALTER TABLE `moderation_record` ADD COLUMN `moderationStatus` VARCHAR(255) DEFAULT NULL COMMENT 'moderationStatus(moderationStatus)';

-- multimedia_file (ai)
ALTER TABLE `multimedia_file` ADD COLUMN `encrypted` INT DEFAULT 0 COMMENT 'encrypted(encrypted)';
ALTER TABLE `multimedia_file` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `multimedia_file` ADD COLUMN `fileType` VARCHAR(255) DEFAULT NULL COMMENT 'fileType(fileType)';
ALTER TABLE `multimedia_file` ADD COLUMN `fileHash` VARCHAR(255) DEFAULT NULL COMMENT 'fileHash(fileHash)';
ALTER TABLE `multimedia_file` ADD COLUMN `sampleRate` INT DEFAULT 0 COMMENT 'sampleRate(sampleRate)';
ALTER TABLE `multimedia_file` ADD COLUMN `height` INT DEFAULT 0 COMMENT 'height(height)';
ALTER TABLE `multimedia_file` ADD COLUMN `channels` INT DEFAULT 0 COMMENT 'channels(channels)';
ALTER TABLE `multimedia_file` ADD COLUMN `watermarked` INT DEFAULT 0 COMMENT 'watermarked(watermarked)';
ALTER TABLE `multimedia_file` ADD COLUMN `exif` VARCHAR(255) DEFAULT NULL COMMENT 'exif(exif)';
ALTER TABLE `multimedia_file` ADD COLUMN `storageType` VARCHAR(255) DEFAULT NULL COMMENT 'storageType(storageType)';
ALTER TABLE `multimedia_file` ADD COLUMN `width` INT DEFAULT 0 COMMENT 'width(width)';
ALTER TABLE `multimedia_file` ADD COLUMN `mimeType` VARCHAR(255) DEFAULT NULL COMMENT 'mimeType(mimeType)';
ALTER TABLE `multimedia_file` ADD COLUMN `codec` VARCHAR(255) DEFAULT NULL COMMENT 'codec(codec)';
ALTER TABLE `multimedia_file` ADD COLUMN `storagePath` VARCHAR(255) DEFAULT NULL COMMENT 'storagePath(storagePath)';
ALTER TABLE `multimedia_file` ADD COLUMN `moderationStatus` VARCHAR(255) DEFAULT NULL COMMENT 'moderationStatus(moderationStatus)';
ALTER TABLE `multimedia_file` ADD COLUMN `durationMs` BIGINT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `multimedia_file` ADD COLUMN `moderationId` BIGINT DEFAULT 0 COMMENT 'moderationId(moderationId)';
ALTER TABLE `multimedia_file` ADD COLUMN `expireAt` DATETIME DEFAULT NULL COMMENT 'expireAt(expireAt)';
ALTER TABLE `multimedia_file` ADD COLUMN `fileName` VARCHAR(255) DEFAULT NULL COMMENT 'fileName(fileName)';
ALTER TABLE `multimedia_file` ADD COLUMN `accessCount` INT DEFAULT 0 COMMENT 'accessCount(accessCount)';
ALTER TABLE `multimedia_file` ADD COLUMN `fileSize` BIGINT DEFAULT 0 COMMENT 'fileSize(fileSize)';
ALTER TABLE `multimedia_file` ADD COLUMN `originalName` VARCHAR(255) DEFAULT NULL COMMENT 'originalName(originalName)';
ALTER TABLE `multimedia_file` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';
ALTER TABLE `multimedia_file` ADD COLUMN `isPublic` INT DEFAULT 0 COMMENT 'isPublic(isPublic)';
ALTER TABLE `multimedia_file` ADD COLUMN `bitrate` INT DEFAULT 0 COMMENT 'bitrate(bitrate)';

-- notification (auth)
ALTER TABLE `notification` ADD COLUMN `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)';
ALTER TABLE `notification` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';
ALTER TABLE `notification` ADD COLUMN `isRead` INT DEFAULT 0 COMMENT 'isRead(isRead)';

-- oauth_app_config (auth)
ALTER TABLE `oauth_app_config` ADD COLUMN `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)';
ALTER TABLE `oauth_app_config` ADD COLUMN `extraConfig` VARCHAR(255) DEFAULT NULL COMMENT 'extraConfig(extraConfig)';
ALTER TABLE `oauth_app_config` ADD COLUMN `appId` VARCHAR(255) DEFAULT NULL COMMENT 'appId(appId)';
ALTER TABLE `oauth_app_config` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `oauth_app_config` ADD COLUMN `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes(scopes)';
ALTER TABLE `oauth_app_config` ADD COLUMN `appSecret` VARCHAR(255) DEFAULT NULL COMMENT 'appSecret(appSecret)';
ALTER TABLE `oauth_app_config` ADD COLUMN `updatedAt` DATETIME DEFAULT NULL COMMENT 'updatedAt(updatedAt)';
ALTER TABLE `oauth_app_config` ADD COLUMN `publicKey` VARCHAR(255) DEFAULT NULL COMMENT 'publicKey(publicKey)';
ALTER TABLE `oauth_app_config` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `oauth_app_config` ADD COLUMN `redirectUri` VARCHAR(255) DEFAULT NULL COMMENT 'redirectUri(redirectUri)';

-- oauth_binding (auth)
ALTER TABLE `oauth_binding` ADD COLUMN `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)';
ALTER TABLE `oauth_binding` ADD COLUMN `accessToken` VARCHAR(255) DEFAULT NULL COMMENT 'accessToken(accessToken)';
ALTER TABLE `oauth_binding` ADD COLUMN `boundAt` DATETIME DEFAULT NULL COMMENT 'boundAt(boundAt)';
ALTER TABLE `oauth_binding` ADD COLUMN `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)';
ALTER TABLE `oauth_binding` ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)';
ALTER TABLE `oauth_binding` ADD COLUMN `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)';
ALTER TABLE `oauth_binding` ADD COLUMN `lastLoginAt` DATETIME DEFAULT NULL COMMENT 'lastLoginAt(lastLoginAt)';
ALTER TABLE `oauth_binding` ADD COLUMN `rawData` VARCHAR(255) DEFAULT NULL COMMENT 'rawData(rawData)';
ALTER TABLE `oauth_binding` ADD COLUMN `refreshToken` VARCHAR(255) DEFAULT NULL COMMENT 'refreshToken(refreshToken)';
ALTER TABLE `oauth_binding` ADD COLUMN `tokenExpiresAt` DATETIME DEFAULT NULL COMMENT 'tokenExpiresAt(tokenExpiresAt)';
ALTER TABLE `oauth_binding` ADD COLUMN `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid(openid)';

-- pipeline_log (ai)
ALTER TABLE `pipeline_log` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `pipeline_log` ADD COLUMN `computeMode` VARCHAR(255) DEFAULT NULL COMMENT 'computeMode(computeMode)';
ALTER TABLE `pipeline_log` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `pipeline_log` ADD COLUMN `intent` VARCHAR(255) DEFAULT NULL COMMENT 'intent(intent)';
ALTER TABLE `pipeline_log` ADD COLUMN `inputText` VARCHAR(255) DEFAULT NULL COMMENT 'inputText(inputText)';
ALTER TABLE `pipeline_log` ADD COLUMN `outputText` VARCHAR(255) DEFAULT NULL COMMENT 'outputText(outputText)';
ALTER TABLE `pipeline_log` ADD COLUMN `needsReview` TINYINT(1) DEFAULT 0 COMMENT 'needsReview(needsReview)';
ALTER TABLE `pipeline_log` ADD COLUMN `riskLevel` VARCHAR(255) DEFAULT NULL COMMENT 'riskLevel(riskLevel)';
ALTER TABLE `pipeline_log` ADD COLUMN `toolCalls` INT DEFAULT 0 COMMENT 'toolCalls(toolCalls)';
ALTER TABLE `pipeline_log` ADD COLUMN `totalCostMs` BIGINT DEFAULT 0 COMMENT 'totalCostMs(totalCostMs)';
ALTER TABLE `pipeline_log` ADD COLUMN `clientIp` VARCHAR(255) DEFAULT NULL COMMENT 'clientIp(clientIp)';
ALTER TABLE `pipeline_log` ADD COLUMN `stageCosts` VARCHAR(255) DEFAULT NULL COMMENT 'stageCosts(stageCosts)';
ALTER TABLE `pipeline_log` ADD COLUMN `outputTokens` INT DEFAULT 0 COMMENT 'outputTokens(outputTokens)';
ALTER TABLE `pipeline_log` ADD COLUMN `computeDevice` VARCHAR(255) DEFAULT NULL COMMENT 'computeDevice(computeDevice)';
ALTER TABLE `pipeline_log` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `pipeline_log` ADD COLUMN `inputModality` VARCHAR(255) DEFAULT NULL COMMENT 'inputModality(inputModality)';
ALTER TABLE `pipeline_log` ADD COLUMN `ragHits` INT DEFAULT 0 COMMENT 'ragHits(ragHits)';

-- pipeline_node_log (pipeline)
ALTER TABLE `pipeline_node_log` ADD COLUMN `durationMs` BIGINT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `nodeType` VARCHAR(255) DEFAULT NULL COMMENT 'nodeType(nodeType)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `outputPreview` VARCHAR(255) DEFAULT NULL COMMENT 'outputPreview(outputPreview)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `endTime` DATETIME DEFAULT NULL COMMENT 'endTime(endTime)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `configSnapshot` VARCHAR(255) DEFAULT NULL COMMENT 'configSnapshot(configSnapshot)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `startTime` DATETIME DEFAULT NULL COMMENT 'startTime(startTime)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `inputRows` INT DEFAULT 0 COMMENT 'inputRows(inputRows)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `nodeName` VARCHAR(255) DEFAULT NULL COMMENT 'nodeName(nodeName)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `pipeline_node_log` ADD COLUMN `outputRows` INT DEFAULT 0 COMMENT 'outputRows(outputRows)';

-- pipeline_run (pipeline)
ALTER TABLE `pipeline_run` ADD COLUMN `durationMs` BIGINT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `pipeline_run` ADD COLUMN `resultSummary` VARCHAR(255) DEFAULT NULL COMMENT 'resultSummary(resultSummary)';
ALTER TABLE `pipeline_run` ADD COLUMN `endTime` DATETIME DEFAULT NULL COMMENT 'endTime(endTime)';
ALTER TABLE `pipeline_run` ADD COLUMN `startTime` DATETIME DEFAULT NULL COMMENT 'startTime(startTime)';
ALTER TABLE `pipeline_run` ADD COLUMN `triggerBy` BIGINT DEFAULT 0 COMMENT 'triggerBy(triggerBy)';
ALTER TABLE `pipeline_run` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `pipeline_run` ADD COLUMN `triggerType` VARCHAR(255) DEFAULT NULL COMMENT 'triggerType(triggerType)';
ALTER TABLE `pipeline_run` ADD COLUMN `createTime` DATETIME DEFAULT NULL COMMENT 'createTime(createTime)';
ALTER TABLE `pipeline_run` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `pipeline_run` ADD COLUMN `definitionSnapshot` VARCHAR(255) DEFAULT NULL COMMENT 'definitionSnapshot(definitionSnapshot)';

-- pipeline_workflow (pipeline)
ALTER TABLE `pipeline_workflow` ADD COLUMN `version` INT DEFAULT 0 COMMENT 'version(version)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `createBy` BIGINT DEFAULT 0 COMMENT 'createBy(createBy)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `status` INT DEFAULT 0 COMMENT 'status(status)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `updateTime` DATETIME DEFAULT NULL COMMENT 'updateTime(updateTime)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `definition` VARCHAR(255) DEFAULT NULL COMMENT 'definition(definition)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `createTime` DATETIME DEFAULT NULL COMMENT 'createTime(createTime)';
ALTER TABLE `pipeline_workflow` ADD COLUMN `updateBy` BIGINT DEFAULT 0 COMMENT 'updateBy(updateBy)';

-- pipeline_workflow_version (pipeline)
ALTER TABLE `pipeline_workflow_version` ADD COLUMN `createBy` BIGINT DEFAULT 0 COMMENT 'createBy(createBy)';
ALTER TABLE `pipeline_workflow_version` ADD COLUMN `createTime` DATETIME DEFAULT NULL COMMENT 'createTime(createTime)';
ALTER TABLE `pipeline_workflow_version` ADD COLUMN `changeLog` VARCHAR(255) DEFAULT NULL COMMENT 'changeLog(changeLog)';

-- plugin (agent)
ALTER TABLE `plugin` ADD COLUMN `version` VARCHAR(255) DEFAULT NULL COMMENT 'version(version)';
ALTER TABLE `plugin` ADD COLUMN `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon(icon)';
ALTER TABLE `plugin` ADD COLUMN `entry` VARCHAR(255) DEFAULT NULL COMMENT 'entry(entry)';
ALTER TABLE `plugin` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `plugin` ADD COLUMN `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)';
ALTER TABLE `plugin` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `plugin` ADD COLUMN `config` VARCHAR(255) DEFAULT NULL COMMENT 'config(config)';
ALTER TABLE `plugin` ADD COLUMN `author` VARCHAR(255) DEFAULT NULL COMMENT 'author(author)';
ALTER TABLE `plugin` ADD COLUMN `downloads` INT DEFAULT 0 COMMENT 'downloads(downloads)';
ALTER TABLE `plugin` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `plugin` ADD COLUMN `rating` DECIMAL(20,4) DEFAULT 0 COMMENT 'rating(rating)';
ALTER TABLE `plugin` ADD COLUMN `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName(displayName)';
ALTER TABLE `plugin` ADD COLUMN `pluginType` VARCHAR(255) DEFAULT NULL COMMENT 'pluginType(pluginType)';
ALTER TABLE `plugin` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `plugin` ADD COLUMN `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)';

-- prompt_template (prompt)
ALTER TABLE `prompt_template` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `prompt_template` ADD COLUMN `useCount` INT DEFAULT 0 COMMENT 'useCount(useCount)';
ALTER TABLE `prompt_template` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `prompt_template` ADD COLUMN `variables` VARCHAR(255) DEFAULT NULL COMMENT 'variables(variables)';
ALTER TABLE `prompt_template` ADD COLUMN `creatorName` VARCHAR(255) DEFAULT NULL COMMENT 'creatorName(creatorName)';
ALTER TABLE `prompt_template` ADD COLUMN `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)';
ALTER TABLE `prompt_template` ADD COLUMN `isPublic` TINYINT(1) DEFAULT 0 COMMENT 'isPublic(isPublic)';
ALTER TABLE `prompt_template` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';
ALTER TABLE `prompt_template` ADD COLUMN `creatorId` BIGINT DEFAULT 0 COMMENT 'creatorId(creatorId)';

-- push_message (ai)
ALTER TABLE `push_message` ADD COLUMN `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon(icon)';
ALTER TABLE `push_message` ADD COLUMN `clickAction` VARCHAR(255) DEFAULT NULL COMMENT 'clickAction(clickAction)';
ALTER TABLE `push_message` ADD COLUMN `targetValue` VARCHAR(255) DEFAULT NULL COMMENT 'targetValue(targetValue)';
ALTER TABLE `push_message` ADD COLUMN `body` VARCHAR(255) DEFAULT NULL COMMENT 'body(body)';
ALTER TABLE `push_message` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `push_message` ADD COLUMN `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)';
ALTER TABLE `push_message` ADD COLUMN `data` VARCHAR(255) DEFAULT NULL COMMENT 'data(data)';
ALTER TABLE `push_message` ADD COLUMN `targetType` VARCHAR(255) DEFAULT NULL COMMENT 'targetType(targetType)';
ALTER TABLE `push_message` ADD COLUMN `successCount` INT DEFAULT 0 COMMENT 'successCount(successCount)';
ALTER TABLE `push_message` ADD COLUMN `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)';
ALTER TABLE `push_message` ADD COLUMN `failureCount` INT DEFAULT 0 COMMENT 'failureCount(failureCount)';

-- push_subscription (ai)
ALTER TABLE `push_subscription` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `push_subscription` ADD COLUMN `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform(platform)';
ALTER TABLE `push_subscription` ADD COLUMN `p256dhKey` VARCHAR(255) DEFAULT NULL COMMENT 'p256dhKey(p256dhKey)';
ALTER TABLE `push_subscription` ADD COLUMN `endpoint` VARCHAR(255) DEFAULT NULL COMMENT 'endpoint(endpoint)';
ALTER TABLE `push_subscription` ADD COLUMN `authKey` VARCHAR(255) DEFAULT NULL COMMENT 'authKey(authKey)';
ALTER TABLE `push_subscription` ADD COLUMN `lastActiveAt` DATETIME DEFAULT NULL COMMENT 'lastActiveAt(lastActiveAt)';
ALTER TABLE `push_subscription` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `push_subscription` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';

-- raft_log (ai)
ALTER TABLE `raft_log` ADD COLUMN `committed` TINYINT(1) DEFAULT 0 COMMENT 'committed(committed)';
ALTER TABLE `raft_log` ADD COLUMN `committedAt` DATETIME DEFAULT NULL COMMENT 'committedAt(committedAt)';
ALTER TABLE `raft_log` ADD COLUMN `command` VARCHAR(255) DEFAULT NULL COMMENT 'command(command)';

-- sensitive_word (ai)
ALTER TABLE `sensitive_word` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `sensitive_word` ADD COLUMN `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)';
ALTER TABLE `sensitive_word` ADD COLUMN `level` VARCHAR(255) DEFAULT NULL COMMENT 'level(level)';
ALTER TABLE `sensitive_word` ADD COLUMN `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)';

-- sys_role (auth)
ALTER TABLE `sys_role` ADD COLUMN `sort` INT DEFAULT 0 COMMENT 'sort(sort)';
ALTER TABLE `sys_role` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `sys_role` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `sys_role` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `sys_role` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';

-- sys_user (auth)
ALTER TABLE `sys_user` ADD COLUMN `qqNickname` VARCHAR(255) DEFAULT NULL COMMENT 'qqNickname(qqNickname)';
ALTER TABLE `sys_user` ADD COLUMN `phone` VARCHAR(255) DEFAULT NULL COMMENT 'phone(phone)';
ALTER TABLE `sys_user` ADD COLUMN `email` VARCHAR(255) DEFAULT NULL COMMENT 'email(email)';
ALTER TABLE `sys_user` ADD COLUMN `lastLoginAt` DATETIME DEFAULT NULL COMMENT 'lastLoginAt(lastLoginAt)';
ALTER TABLE `sys_user` ADD COLUMN `gender` INT DEFAULT 0 COMMENT 'gender(gender)';
ALTER TABLE `sys_user` ADD COLUMN `qqOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'qqOpenid(qqOpenid)';
ALTER TABLE `sys_user` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark(remark)';
ALTER TABLE `sys_user` ADD COLUMN `alipayAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'alipayAvatar(alipayAvatar)';
ALTER TABLE `sys_user` ADD COLUMN `qqUnionid` VARCHAR(255) DEFAULT NULL COMMENT 'qqUnionid(qqUnionid)';
ALTER TABLE `sys_user` ADD COLUMN `wechatUnionid` VARCHAR(255) DEFAULT NULL COMMENT 'wechatUnionid(wechatUnionid)';
ALTER TABLE `sys_user` ADD COLUMN `alipayUserId` VARCHAR(255) DEFAULT NULL COMMENT 'alipayUserId(alipayUserId)';
ALTER TABLE `sys_user` ADD COLUMN `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)';
ALTER TABLE `sys_user` ADD COLUMN `wechatNickname` VARCHAR(255) DEFAULT NULL COMMENT 'wechatNickname(wechatNickname)';
ALTER TABLE `sys_user` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `sys_user` ADD COLUMN `qqBoundAt` DATETIME DEFAULT NULL COMMENT 'qqBoundAt(qqBoundAt)';
ALTER TABLE `sys_user` ADD COLUMN `wechatBoundAt` DATETIME DEFAULT NULL COMMENT 'wechatBoundAt(wechatBoundAt)';
ALTER TABLE `sys_user` ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)';
ALTER TABLE `sys_user` ADD COLUMN `qqAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'qqAvatar(qqAvatar)';
ALTER TABLE `sys_user` ADD COLUMN `alipayNickname` VARCHAR(255) DEFAULT NULL COMMENT 'alipayNickname(alipayNickname)';
ALTER TABLE `sys_user` ADD COLUMN `password` VARCHAR(255) DEFAULT NULL COMMENT 'password(password)';
ALTER TABLE `sys_user` ADD COLUMN `alipayOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'alipayOpenid(alipayOpenid)';
ALTER TABLE `sys_user` ADD COLUMN `lastLoginIp` VARCHAR(255) DEFAULT NULL COMMENT 'lastLoginIp(lastLoginIp)';
ALTER TABLE `sys_user` ADD COLUMN `wechatOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'wechatOpenid(wechatOpenid)';
ALTER TABLE `sys_user` ADD COLUMN `wechatAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'wechatAvatar(wechatAvatar)';
ALTER TABLE `sys_user` ADD COLUMN `alipayBoundAt` DATETIME DEFAULT NULL COMMENT 'alipayBoundAt(alipayBoundAt)';
ALTER TABLE `sys_user` ADD COLUMN `status` INT DEFAULT 0 COMMENT 'status(status)';
ALTER TABLE `sys_user` ADD COLUMN `tenantId` BIGINT DEFAULT 0 COMMENT 'tenantId(tenantId)';

-- tenant (auth)
ALTER TABLE `tenant` ADD COLUMN `expireAt` DATETIME DEFAULT NULL COMMENT 'expireAt(expireAt)';
ALTER TABLE `tenant` ADD COLUMN `qpsLimit` INT DEFAULT 0 COMMENT 'qpsLimit(qpsLimit)';
ALTER TABLE `tenant` ADD COLUMN `contactEmail` VARCHAR(255) DEFAULT NULL COMMENT 'contactEmail(contactEmail)';
ALTER TABLE `tenant` ADD COLUMN `maxModels` INT DEFAULT 0 COMMENT 'maxModels(maxModels)';
ALTER TABLE `tenant` ADD COLUMN `monthlyQuota` BIGINT DEFAULT 0 COMMENT 'monthlyQuota(monthlyQuota)';
ALTER TABLE `tenant` ADD COLUMN `usedQuota` BIGINT DEFAULT 0 COMMENT 'usedQuota(usedQuota)';
ALTER TABLE `tenant` ADD COLUMN `status` INT DEFAULT 0 COMMENT 'status(status)';
ALTER TABLE `tenant` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `tenant` ADD COLUMN `plan` VARCHAR(255) DEFAULT NULL COMMENT 'plan(plan)';
ALTER TABLE `tenant` ADD COLUMN `contactPhone` VARCHAR(255) DEFAULT NULL COMMENT 'contactPhone(contactPhone)';
ALTER TABLE `tenant` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `tenant` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark(remark)';
ALTER TABLE `tenant` ADD COLUMN `maxUsers` INT DEFAULT 0 COMMENT 'maxUsers(maxUsers)';

-- training_checkpoint (ai)
ALTER TABLE `training_checkpoint` ADD COLUMN `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata(metadata)';
ALTER TABLE `training_checkpoint` ADD COLUMN `valLoss` DOUBLE DEFAULT 0 COMMENT 'valLoss(valLoss)';
ALTER TABLE `training_checkpoint` ADD COLUMN `accuracy` DOUBLE DEFAULT 0 COMMENT 'accuracy(accuracy)';
ALTER TABLE `training_checkpoint` ADD COLUMN `epoch` INT DEFAULT 0 COMMENT 'epoch(epoch)';
ALTER TABLE `training_checkpoint` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `training_checkpoint` ADD COLUMN `filePath` VARCHAR(255) DEFAULT NULL COMMENT 'filePath(filePath)';
ALTER TABLE `training_checkpoint` ADD COLUMN `sizeBytes` BIGINT DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)';
ALTER TABLE `training_checkpoint` ADD COLUMN `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)';
ALTER TABLE `training_checkpoint` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `training_checkpoint` ADD COLUMN `step` INT DEFAULT 0 COMMENT 'step(step)';
ALTER TABLE `training_checkpoint` ADD COLUMN `checkpointId` VARCHAR(255) DEFAULT NULL COMMENT 'checkpointId(checkpointId)';

-- training_job (ai)
ALTER TABLE `training_job` ADD COLUMN `endTimeMs` BIGINT DEFAULT 0 COMMENT 'endTimeMs(endTimeMs)';
ALTER TABLE `training_job` ADD COLUMN `lastAccuracy` DOUBLE DEFAULT 0 COMMENT 'lastAccuracy(lastAccuracy)';
ALTER TABLE `training_job` ADD COLUMN `lastLoss` DOUBLE DEFAULT 0 COMMENT 'lastLoss(lastLoss)';
ALTER TABLE `training_job` ADD COLUMN `totalEpochs` INT DEFAULT 0 COMMENT 'totalEpochs(totalEpochs)';
ALTER TABLE `training_job` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `training_job` ADD COLUMN `lastValLoss` DOUBLE DEFAULT 0 COMMENT 'lastValLoss(lastValLoss)';
ALTER TABLE `training_job` ADD COLUMN `config` VARCHAR(255) DEFAULT NULL COMMENT 'config(config)';
ALTER TABLE `training_job` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `training_job` ADD COLUMN `currentStep` INT DEFAULT 0 COMMENT 'currentStep(currentStep)';
ALTER TABLE `training_job` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `training_job` ADD COLUMN `startTimeMs` BIGINT DEFAULT 0 COMMENT 'startTimeMs(startTimeMs)';
ALTER TABLE `training_job` ADD COLUMN `totalSteps` INT DEFAULT 0 COMMENT 'totalSteps(totalSteps)';
ALTER TABLE `training_job` ADD COLUMN `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)';
ALTER TABLE `training_job` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `training_job` ADD COLUMN `currentEpoch` INT DEFAULT 0 COMMENT 'currentEpoch(currentEpoch)';
ALTER TABLE `training_job` ADD COLUMN `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)';

-- training_metric (ai)
ALTER TABLE `training_metric` ADD COLUMN `valLoss` DOUBLE DEFAULT 0 COMMENT 'valLoss(valLoss)';
ALTER TABLE `training_metric` ADD COLUMN `accuracy` DOUBLE DEFAULT 0 COMMENT 'accuracy(accuracy)';
ALTER TABLE `training_metric` ADD COLUMN `epoch` INT DEFAULT 0 COMMENT 'epoch(epoch)';
ALTER TABLE `training_metric` ADD COLUMN `elapsedMs` BIGINT DEFAULT 0 COMMENT 'elapsedMs(elapsedMs)';
ALTER TABLE `training_metric` ADD COLUMN `loss` DOUBLE DEFAULT 0 COMMENT 'loss(loss)';
ALTER TABLE `training_metric` ADD COLUMN `step` INT DEFAULT 0 COMMENT 'step(step)';
ALTER TABLE `training_metric` ADD COLUMN `learningRate` DOUBLE DEFAULT 0 COMMENT 'learningRate(learningRate)';

-- training_task (model)
ALTER TABLE `training_task` ADD COLUMN `completedAt` DATETIME DEFAULT NULL COMMENT 'completedAt(completedAt)';
ALTER TABLE `training_task` ADD COLUMN `nHead` INT DEFAULT 0 COMMENT 'nHead(nHead)';
ALTER TABLE `training_task` ADD COLUMN `progress` INT DEFAULT 0 COMMENT 'progress(progress)';
ALTER TABLE `training_task` ADD COLUMN `nEmbd` INT DEFAULT 0 COMMENT 'nEmbd(nEmbd)';
ALTER TABLE `training_task` ADD COLUMN `corpusPath` VARCHAR(255) DEFAULT NULL COMMENT 'corpusPath(corpusPath)';
ALTER TABLE `training_task` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `training_task` ADD COLUMN `currentLoss` DOUBLE DEFAULT 0 COMMENT 'currentLoss(currentLoss)';
ALTER TABLE `training_task` ADD COLUMN `nLayer` INT DEFAULT 0 COMMENT 'nLayer(nLayer)';
ALTER TABLE `training_task` ADD COLUMN `blockSize` INT DEFAULT 0 COMMENT 'blockSize(blockSize)';
ALTER TABLE `training_task` ADD COLUMN `currentIter` INT DEFAULT 0 COMMENT 'currentIter(currentIter)';
ALTER TABLE `training_task` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `training_task` ADD COLUMN `maxIters` INT DEFAULT 0 COMMENT 'maxIters(maxIters)';
ALTER TABLE `training_task` ADD COLUMN `learningRate` DOUBLE DEFAULT 0 COMMENT 'learningRate(learningRate)';
ALTER TABLE `training_task` ADD COLUMN `batchSize` INT DEFAULT 0 COMMENT 'batchSize(batchSize)';

-- unionid_relations (auth)
ALTER TABLE `unionid_relations` ADD COLUMN `lastSeenAt` DATETIME DEFAULT NULL COMMENT 'lastSeenAt(lastSeenAt)';
ALTER TABLE `unionid_relations` ADD COLUMN `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform(platform)';
ALTER TABLE `unionid_relations` ADD COLUMN `bindingCount` INT DEFAULT 0 COMMENT 'bindingCount(bindingCount)';
ALTER TABLE `unionid_relations` ADD COLUMN `firstSeenAt` DATETIME DEFAULT NULL COMMENT 'firstSeenAt(firstSeenAt)';

-- user_api_key (auth)
ALTER TABLE `user_api_key` ADD COLUMN `keyHash` VARCHAR(255) DEFAULT NULL COMMENT 'keyHash(keyHash)';
ALTER TABLE `user_api_key` ADD COLUMN `lastUsedAt` DATETIME DEFAULT NULL COMMENT 'lastUsedAt(lastUsedAt)';
ALTER TABLE `user_api_key` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `user_api_key` ADD COLUMN `expiresAt` DATETIME DEFAULT NULL COMMENT 'expiresAt(expiresAt)';
ALTER TABLE `user_api_key` ADD COLUMN `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes(scopes)';
ALTER TABLE `user_api_key` ADD COLUMN `useCount` BIGINT DEFAULT 0 COMMENT 'useCount(useCount)';
ALTER TABLE `user_api_key` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `user_api_key` ADD COLUMN `keyPrefix` VARCHAR(255) DEFAULT NULL COMMENT 'keyPrefix(keyPrefix)';

-- wechat_config (auth)
ALTER TABLE `wechat_config` ADD COLUMN `aesKey` VARCHAR(255) DEFAULT NULL COMMENT 'aesKey(aesKey)';
ALTER TABLE `wechat_config` ADD COLUMN `appId` VARCHAR(255) DEFAULT NULL COMMENT 'appId(appId)';
ALTER TABLE `wechat_config` ADD COLUMN `token` VARCHAR(255) DEFAULT NULL COMMENT 'token(token)';
ALTER TABLE `wechat_config` ADD COLUMN `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)';
ALTER TABLE `wechat_config` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `wechat_config` ADD COLUMN `appSecret` VARCHAR(255) DEFAULT NULL COMMENT 'appSecret(appSecret)';
ALTER TABLE `wechat_config` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark(remark)';
ALTER TABLE `wechat_config` ADD COLUMN `redirectUri` VARCHAR(255) DEFAULT NULL COMMENT 'redirectUri(redirectUri)';

-- wechat_scan_session (auth)
ALTER TABLE `wechat_scan_session` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `accessToken` VARCHAR(255) DEFAULT NULL COMMENT 'accessToken(accessToken)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `clientIp` VARCHAR(255) DEFAULT NULL COMMENT 'clientIp(clientIp)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `confirmedAt` DATETIME DEFAULT NULL COMMENT 'confirmedAt(confirmedAt)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `sceneId` VARCHAR(255) DEFAULT NULL COMMENT 'sceneId(sceneId)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `expiresAt` DATETIME DEFAULT NULL COMMENT 'expiresAt(expiresAt)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `refreshToken` VARCHAR(255) DEFAULT NULL COMMENT 'refreshToken(refreshToken)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid(openid)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';

-- wechat_user_binding (auth)
ALTER TABLE `wechat_user_binding` ADD COLUMN `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)';
ALTER TABLE `wechat_user_binding` ADD COLUMN `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)';
ALTER TABLE `wechat_user_binding` ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)';
ALTER TABLE `wechat_user_binding` ADD COLUMN `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)';
ALTER TABLE `wechat_user_binding` ADD COLUMN `lastLoginAt` DATETIME DEFAULT NULL COMMENT 'lastLoginAt(lastLoginAt)';
