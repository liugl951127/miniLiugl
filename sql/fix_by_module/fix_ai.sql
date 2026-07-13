-- =============================================================
-- MiniMax Platform V3.5.5+ 修复 SQL - ai 模块
-- 共 27 张表, 296 字段
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_ai.sql
-- 自动生成: scripts/split_fix_sql_by_module.py
-- =============================================================

-- 表: agent_group
ALTER TABLE `agent_group` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `agent_group` ADD COLUMN `lastRunAt` DATETIME DEFAULT NULL COMMENT 'lastRunAt(lastRunAt)';
ALTER TABLE `agent_group` ADD COLUMN `membersJson` VARCHAR(255) DEFAULT NULL COMMENT 'membersJson(membersJson)';
ALTER TABLE `agent_group` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `agent_group` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `agent_group` ADD COLUMN `runCount` INT DEFAULT 0 COMMENT 'runCount(runCount)';
ALTER TABLE `agent_group` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `agent_group` ADD COLUMN `strategy` VARCHAR(255) DEFAULT NULL COMMENT 'strategy(strategy)';
ALTER TABLE `agent_group` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';

-- 表: ai_chat_message
ALTER TABLE `ai_chat_message` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';
ALTER TABLE `ai_chat_message` ADD COLUMN `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)';
ALTER TABLE `ai_chat_message` ADD COLUMN `toolCode` VARCHAR(255) DEFAULT NULL COMMENT 'toolCode(toolCode)';
ALTER TABLE `ai_chat_message` ADD COLUMN `toolInput` VARCHAR(255) DEFAULT NULL COMMENT 'toolInput(toolInput)';
ALTER TABLE `ai_chat_message` ADD COLUMN `toolOutput` VARCHAR(255) DEFAULT NULL COMMENT 'toolOutput(toolOutput)';

-- 表: ai_chat_session
ALTER TABLE `ai_chat_session` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `ai_chat_session` ADD COLUMN `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)';
ALTER TABLE `ai_chat_session` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `ai_chat_session` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';

-- 表: ai_generation_log
ALTER TABLE `ai_generation_log` ADD COLUMN `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `ai_generation_log` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';
ALTER TABLE `ai_generation_log` ADD COLUMN `modality` VARCHAR(255) DEFAULT NULL COMMENT 'modality(modality)';
ALTER TABLE `ai_generation_log` ADD COLUMN `modelName` VARCHAR(255) DEFAULT NULL COMMENT 'modelName(modelName)';
ALTER TABLE `ai_generation_log` ADD COLUMN `modelVersion` VARCHAR(255) DEFAULT NULL COMMENT 'modelVersion(modelVersion)';
ALTER TABLE `ai_generation_log` ADD COLUMN `negativePrompt` VARCHAR(255) DEFAULT NULL COMMENT 'negativePrompt(negativePrompt)';
ALTER TABLE `ai_generation_log` ADD COLUMN `outputHash` VARCHAR(255) DEFAULT NULL COMMENT 'outputHash(outputHash)';
ALTER TABLE `ai_generation_log` ADD COLUMN `outputSize` BIGINT DEFAULT 0 COMMENT 'outputSize(outputSize)';
ALTER TABLE `ai_generation_log` ADD COLUMN `outputUrl` VARCHAR(255) DEFAULT NULL COMMENT 'outputUrl(outputUrl)';
ALTER TABLE `ai_generation_log` ADD COLUMN `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters(parameters)';
ALTER TABLE `ai_generation_log` ADD COLUMN `prompt` VARCHAR(255) DEFAULT NULL COMMENT 'prompt(prompt)';
ALTER TABLE `ai_generation_log` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `ai_generation_log` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `ai_generation_log` ADD COLUMN `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp(userIp)';
ALTER TABLE `ai_generation_log` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';
ALTER TABLE `ai_generation_log` ADD COLUMN `watermarkText` VARCHAR(255) DEFAULT NULL COMMENT 'watermarkText(watermarkText)';
ALTER TABLE `ai_generation_log` ADD COLUMN `watermarked` INT DEFAULT 0 COMMENT 'watermarked(watermarked)';

-- 表: ai_intent_keyword
ALTER TABLE `ai_intent_keyword` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `isRegex` INT DEFAULT 0 COMMENT 'isRegex(isRegex)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `keyword` VARCHAR(255) DEFAULT NULL COMMENT 'keyword(keyword)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `language` VARCHAR(255) DEFAULT NULL COMMENT 'language(language)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark(remark)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `updatedAt` DATETIME DEFAULT NULL COMMENT 'updatedAt(updatedAt)';
ALTER TABLE `ai_intent_keyword` ADD COLUMN `weight` INT DEFAULT 0 COMMENT 'weight(weight)';

-- 表: ai_tool
ALTER TABLE `ai_tool` ADD COLUMN `author` VARCHAR(255) DEFAULT NULL COMMENT 'author(author)';
ALTER TABLE `ai_tool` ADD COLUMN `builtin` INT DEFAULT 0 COMMENT 'builtin(builtin)';
ALTER TABLE `ai_tool` ADD COLUMN `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)';
ALTER TABLE `ai_tool` ADD COLUMN `defaultConfig` VARCHAR(255) DEFAULT NULL COMMENT 'defaultConfig(defaultConfig)';
ALTER TABLE `ai_tool` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `ai_tool` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `ai_tool` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `ai_tool` ADD COLUMN `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon(icon)';
ALTER TABLE `ai_tool` ADD COLUMN `implType` VARCHAR(255) DEFAULT NULL COMMENT 'implType(implType)';
ALTER TABLE `ai_tool` ADD COLUMN `implValue` VARCHAR(255) DEFAULT NULL COMMENT 'implValue(implValue)';
ALTER TABLE `ai_tool` ADD COLUMN `inputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'inputSchema(inputSchema)';
ALTER TABLE `ai_tool` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `ai_tool` ADD COLUMN `outputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'outputSchema(outputSchema)';
ALTER TABLE `ai_tool` ADD COLUMN `rateLimit` INT DEFAULT 0 COMMENT 'rateLimit(rateLimit)';
ALTER TABLE `ai_tool` ADD COLUMN `roleRequired` VARCHAR(255) DEFAULT NULL COMMENT 'roleRequired(roleRequired)';
ALTER TABLE `ai_tool` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `ai_tool` ADD COLUMN `timeoutSeconds` INT DEFAULT 0 COMMENT 'timeoutSeconds(timeoutSeconds)';
ALTER TABLE `ai_tool` ADD COLUMN `version` VARCHAR(255) DEFAULT NULL COMMENT 'version(version)';

-- 表: ai_tool_invocation
ALTER TABLE `ai_tool_invocation` ADD COLUMN `dataSourceId` BIGINT DEFAULT 0 COMMENT 'dataSourceId(dataSourceId)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `inputJson` VARCHAR(255) DEFAULT NULL COMMENT 'inputJson(inputJson)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `outputJson` VARCHAR(255) DEFAULT NULL COMMENT 'outputJson(outputJson)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `ai_tool_invocation` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';

-- 表: audit_log
ALTER TABLE `audit_log` ADD COLUMN `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)';
ALTER TABLE `audit_log` ADD COLUMN `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `audit_log` ADD COLUMN `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)';
ALTER TABLE `audit_log` ADD COLUMN `method` VARCHAR(255) DEFAULT NULL COMMENT 'method(method)';
ALTER TABLE `audit_log` ADD COLUMN `path` VARCHAR(255) DEFAULT NULL COMMENT 'path(path)';
ALTER TABLE `audit_log` ADD COLUMN `requestBody` VARCHAR(255) DEFAULT NULL COMMENT 'requestBody(requestBody)';
ALTER TABLE `audit_log` ADD COLUMN `resourceId` VARCHAR(255) DEFAULT NULL COMMENT 'resourceId(resourceId)';
ALTER TABLE `audit_log` ADD COLUMN `resourceType` VARCHAR(255) DEFAULT NULL COMMENT 'resourceType(resourceType)';
ALTER TABLE `audit_log` ADD COLUMN `responseStatus` INT DEFAULT 0 COMMENT 'responseStatus(responseStatus)';
ALTER TABLE `audit_log` ADD COLUMN `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)';
ALTER TABLE `audit_log` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';
ALTER TABLE `audit_log` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `audit_log` ADD COLUMN `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp(userIp)';
ALTER TABLE `audit_log` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';

-- 表: billing_record
ALTER TABLE `billing_record` ADD COLUMN `amountCents` BIGINT DEFAULT 0 COMMENT 'amountCents(amountCents)';
ALTER TABLE `billing_record` ADD COLUMN `currency` VARCHAR(255) DEFAULT NULL COMMENT 'currency(currency)';
ALTER TABLE `billing_record` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `billing_record` ADD COLUMN `externalTransactionId` VARCHAR(255) DEFAULT NULL COMMENT 'externalTransactionId(externalTransactionId)';
ALTER TABLE `billing_record` ADD COLUMN `licenseId` BIGINT DEFAULT 0 COMMENT 'licenseId(licenseId)';
ALTER TABLE `billing_record` ADD COLUMN `modelEntryId` BIGINT DEFAULT 0 COMMENT 'modelEntryId(modelEntryId)';
ALTER TABLE `billing_record` ADD COLUMN `paymentMethod` VARCHAR(255) DEFAULT NULL COMMENT 'paymentMethod(paymentMethod)';
ALTER TABLE `billing_record` ADD COLUMN `recordType` VARCHAR(255) DEFAULT NULL COMMENT 'recordType(recordType)';
ALTER TABLE `billing_record` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `billing_record` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';

-- 表: cluster_node
ALTER TABLE `cluster_node` ADD COLUMN `activeTasks` INT DEFAULT 0 COMMENT 'activeTasks(activeTasks)';
ALTER TABLE `cluster_node` ADD COLUMN `address` VARCHAR(255) DEFAULT NULL COMMENT 'address(address)';
ALTER TABLE `cluster_node` ADD COLUMN `capabilities` VARCHAR(255) DEFAULT NULL COMMENT 'capabilities(capabilities)';
ALTER TABLE `cluster_node` ADD COLUMN `cpuUsage` DOUBLE DEFAULT 0 COMMENT 'cpuUsage(cpuUsage)';
ALTER TABLE `cluster_node` ADD COLUMN `gpuUsage` DOUBLE DEFAULT 0 COMMENT 'gpuUsage(gpuUsage)';
ALTER TABLE `cluster_node` ADD COLUMN `isLeader` TINYINT(1) DEFAULT 0 COMMENT 'isLeader(isLeader)';
ALTER TABLE `cluster_node` ADD COLUMN `lastHeartbeat` DATETIME DEFAULT NULL COMMENT 'lastHeartbeat(lastHeartbeat)';
ALTER TABLE `cluster_node` ADD COLUMN `memoryUsage` DOUBLE DEFAULT 0 COMMENT 'memoryUsage(memoryUsage)';
ALTER TABLE `cluster_node` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `cluster_node` ADD COLUMN `region` VARCHAR(255) DEFAULT NULL COMMENT 'region(region)';
ALTER TABLE `cluster_node` ADD COLUMN `startedAt` DATETIME DEFAULT NULL COMMENT 'startedAt(startedAt)';
ALTER TABLE `cluster_node` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `cluster_node` ADD COLUMN `totalCores` INT DEFAULT 0 COMMENT 'totalCores(totalCores)';
ALTER TABLE `cluster_node` ADD COLUMN `totalGpus` INT DEFAULT 0 COMMENT 'totalGpus(totalGpus)';
ALTER TABLE `cluster_node` ADD COLUMN `totalMemoryMb` BIGINT DEFAULT 0 COMMENT 'totalMemoryMb(totalMemoryMb)';
ALTER TABLE `cluster_node` ADD COLUMN `zone` VARCHAR(255) DEFAULT NULL COMMENT 'zone(zone)';

-- 表: dashboard_metric
ALTER TABLE `dashboard_metric` ADD COLUMN `dimension` VARCHAR(255) DEFAULT NULL COMMENT 'dimension(dimension)';
ALTER TABLE `dashboard_metric` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `dashboard_metric` ADD COLUMN `value` DOUBLE DEFAULT 0 COMMENT 'value(value)';

-- 表: data_source
ALTER TABLE `data_source` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `data_source` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `data_source` ADD COLUMN `driverClass` VARCHAR(255) DEFAULT NULL COMMENT 'driverClass(driverClass)';
ALTER TABLE `data_source` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `data_source` ADD COLUMN `jdbcUrl` VARCHAR(255) DEFAULT NULL COMMENT 'jdbcUrl(jdbcUrl)';
ALTER TABLE `data_source` ADD COLUMN `lastTestAt` DATETIME DEFAULT NULL COMMENT 'lastTestAt(lastTestAt)';
ALTER TABLE `data_source` ADD COLUMN `maxLifetime` INT DEFAULT 0 COMMENT 'maxLifetime(maxLifetime)';
ALTER TABLE `data_source` ADD COLUMN `minIdle` INT DEFAULT 0 COMMENT 'minIdle(minIdle)';
ALTER TABLE `data_source` ADD COLUMN `password` VARCHAR(255) DEFAULT NULL COMMENT 'password(password)';
ALTER TABLE `data_source` ADD COLUMN `poolSize` INT DEFAULT 0 COMMENT 'poolSize(poolSize)';
ALTER TABLE `data_source` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `data_source` ADD COLUMN `testMessage` VARCHAR(255) DEFAULT NULL COMMENT 'testMessage(testMessage)';
ALTER TABLE `data_source` ADD COLUMN `testStatus` VARCHAR(255) DEFAULT NULL COMMENT 'testStatus(testStatus)';
ALTER TABLE `data_source` ADD COLUMN `type` VARCHAR(255) DEFAULT NULL COMMENT 'type(type)';
ALTER TABLE `data_source` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';

-- 表: kb_chunk
ALTER TABLE `kb_chunk` ADD COLUMN `charCount` INT DEFAULT 0 COMMENT 'charCount(charCount)';
ALTER TABLE `kb_chunk` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';
ALTER TABLE `kb_chunk` ADD COLUMN `docId` VARCHAR(255) DEFAULT NULL COMMENT 'docId(docId)';
ALTER TABLE `kb_chunk` ADD COLUMN `embedding` VARCHAR(255) DEFAULT NULL COMMENT 'embedding(embedding)';
ALTER TABLE `kb_chunk` ADD COLUMN `embeddingModel` VARCHAR(255) DEFAULT NULL COMMENT 'embeddingModel(embeddingModel)';
ALTER TABLE `kb_chunk` ADD COLUMN `kbId` VARCHAR(255) DEFAULT NULL COMMENT 'kbId(kbId)';
ALTER TABLE `kb_chunk` ADD COLUMN `keywords` VARCHAR(255) DEFAULT NULL COMMENT 'keywords(keywords)';
ALTER TABLE `kb_chunk` ADD COLUMN `location` VARCHAR(255) DEFAULT NULL COMMENT 'location(location)';
ALTER TABLE `kb_chunk` ADD COLUMN `seq` INT DEFAULT 0 COMMENT 'seq(seq)';
ALTER TABLE `kb_chunk` ADD COLUMN `summary` VARCHAR(255) DEFAULT NULL COMMENT 'summary(summary)';
ALTER TABLE `kb_chunk` ADD COLUMN `tokenCount` INT DEFAULT 0 COMMENT 'tokenCount(tokenCount)';

-- 表: kb_document
ALTER TABLE `kb_document` ADD COLUMN `chunkCount` INT DEFAULT 0 COMMENT 'chunkCount(chunkCount)';
ALTER TABLE `kb_document` ADD COLUMN `embeddingCount` INT DEFAULT 0 COMMENT 'embeddingCount(embeddingCount)';
ALTER TABLE `kb_document` ADD COLUMN `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)';
ALTER TABLE `kb_document` ADD COLUMN `filePath` VARCHAR(255) DEFAULT NULL COMMENT 'filePath(filePath)';
ALTER TABLE `kb_document` ADD COLUMN `filename` VARCHAR(255) DEFAULT NULL COMMENT 'filename(filename)';
ALTER TABLE `kb_document` ADD COLUMN `isPublic` TINYINT(1) DEFAULT 0 COMMENT 'isPublic(isPublic)';
ALTER TABLE `kb_document` ADD COLUMN `kbId` VARCHAR(255) DEFAULT NULL COMMENT 'kbId(kbId)';
ALTER TABLE `kb_document` ADD COLUMN `mimeType` VARCHAR(255) DEFAULT NULL COMMENT 'mimeType(mimeType)';
ALTER TABLE `kb_document` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `kb_document` ADD COLUMN `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)';
ALTER TABLE `kb_document` ADD COLUMN `sizeBytes` BIGINT DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)';
ALTER TABLE `kb_document` ADD COLUMN `source` VARCHAR(255) DEFAULT NULL COMMENT 'source(source)';
ALTER TABLE `kb_document` ADD COLUMN `sourceUrl` VARCHAR(255) DEFAULT NULL COMMENT 'sourceUrl(sourceUrl)';
ALTER TABLE `kb_document` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `kb_document` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';

-- 表: kb_permission
ALTER TABLE `kb_permission` ADD COLUMN `grantBy` BIGINT DEFAULT 0 COMMENT 'grantBy(grantBy)';
ALTER TABLE `kb_permission` ADD COLUMN `permission` VARCHAR(255) DEFAULT NULL COMMENT 'permission(permission)';
ALTER TABLE `kb_permission` ADD COLUMN `subjectId` BIGINT DEFAULT 0 COMMENT 'subjectId(subjectId)';
ALTER TABLE `kb_permission` ADD COLUMN `subjectType` VARCHAR(255) DEFAULT NULL COMMENT 'subjectType(subjectType)';

-- 表: raft_log
ALTER TABLE `raft_log` ADD COLUMN `command` VARCHAR(255) DEFAULT NULL COMMENT 'command(command)';
ALTER TABLE `raft_log` ADD COLUMN `committed` TINYINT(1) DEFAULT 0 COMMENT 'committed(committed)';
ALTER TABLE `raft_log` ADD COLUMN `committedAt` DATETIME DEFAULT NULL COMMENT 'committedAt(committedAt)';

-- 表: model_license
ALTER TABLE `model_license` ADD COLUMN `expireAt` DATETIME DEFAULT NULL COMMENT 'expireAt(expireAt)';
ALTER TABLE `model_license` ADD COLUMN `licenseType` VARCHAR(255) DEFAULT NULL COMMENT 'licenseType(licenseType)';
ALTER TABLE `model_license` ADD COLUMN `modelEntryId` BIGINT DEFAULT 0 COMMENT 'modelEntryId(modelEntryId)';
ALTER TABLE `model_license` ADD COLUMN `modelVersionId` BIGINT DEFAULT 0 COMMENT 'modelVersionId(modelVersionId)';
ALTER TABLE `model_license` ADD COLUMN `priceCents` BIGINT DEFAULT 0 COMMENT 'priceCents(priceCents)';
ALTER TABLE `model_license` ADD COLUMN `quotaCalls` BIGINT DEFAULT 0 COMMENT 'quotaCalls(quotaCalls)';
ALTER TABLE `model_license` ADD COLUMN `startAt` DATETIME DEFAULT NULL COMMENT 'startAt(startAt)';
ALTER TABLE `model_license` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `model_license` ADD COLUMN `usedCalls` BIGINT DEFAULT 0 COMMENT 'usedCalls(usedCalls)';
ALTER TABLE `model_license` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';

-- 表: model_version
ALTER TABLE `model_version` ADD COLUMN `backwardCompatible` VARCHAR(255) DEFAULT NULL COMMENT 'backwardCompatible(backwardCompatible)';
ALTER TABLE `model_version` ADD COLUMN `changelog` VARCHAR(255) DEFAULT NULL COMMENT 'changelog(changelog)';
ALTER TABLE `model_version` ADD COLUMN `filePath` VARCHAR(255) DEFAULT NULL COMMENT 'filePath(filePath)';
ALTER TABLE `model_version` ADD COLUMN `inputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'inputSchema(inputSchema)';
ALTER TABLE `model_version` ADD COLUMN `isLatest` TINYINT(1) DEFAULT 0 COMMENT 'isLatest(isLatest)';
ALTER TABLE `model_version` ADD COLUMN `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata(metadata)';
ALTER TABLE `model_version` ADD COLUMN `modelEntryId` BIGINT DEFAULT 0 COMMENT 'modelEntryId(modelEntryId)';
ALTER TABLE `model_version` ADD COLUMN `outputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'outputSchema(outputSchema)';
ALTER TABLE `model_version` ADD COLUMN `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)';
ALTER TABLE `model_version` ADD COLUMN `sizeBytes` BIGINT DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)';
ALTER TABLE `model_version` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `model_version` ADD COLUMN `uploaderId` BIGINT DEFAULT 0 COMMENT 'uploaderId(uploaderId)';
ALTER TABLE `model_version` ADD COLUMN `version` VARCHAR(255) DEFAULT NULL COMMENT 'version(version)';

-- 表: moderation_record
ALTER TABLE `moderation_record` ADD COLUMN `contentHash` VARCHAR(255) DEFAULT NULL COMMENT 'contentHash(contentHash)';
ALTER TABLE `moderation_record` ADD COLUMN `contentSize` BIGINT DEFAULT 0 COMMENT 'contentSize(contentSize)';
ALTER TABLE `moderation_record` ADD COLUMN `contentType` VARCHAR(255) DEFAULT NULL COMMENT 'contentType(contentType)';
ALTER TABLE `moderation_record` ADD COLUMN `contentUrl` VARCHAR(255) DEFAULT NULL COMMENT 'contentUrl(contentUrl)';
ALTER TABLE `moderation_record` ADD COLUMN `moderationStatus` VARCHAR(255) DEFAULT NULL COMMENT 'moderationStatus(moderationStatus)';
ALTER TABLE `moderation_record` ADD COLUMN `moderator` VARCHAR(255) DEFAULT NULL COMMENT 'moderator(moderator)';
ALTER TABLE `moderation_record` ADD COLUMN `rejectionReason` VARCHAR(255) DEFAULT NULL COMMENT 'rejectionReason(rejectionReason)';
ALTER TABLE `moderation_record` ADD COLUMN `riskLabels` VARCHAR(255) DEFAULT NULL COMMENT 'riskLabels(riskLabels)';
ALTER TABLE `moderation_record` ADD COLUMN `riskLevel` VARCHAR(255) DEFAULT NULL COMMENT 'riskLevel(riskLevel)';
ALTER TABLE `moderation_record` ADD COLUMN `riskScore` DECIMAL(20,4) DEFAULT 0 COMMENT 'riskScore(riskScore)';
ALTER TABLE `moderation_record` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `moderation_record` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';

-- 表: multimedia_file
ALTER TABLE `multimedia_file` ADD COLUMN `accessCount` INT DEFAULT 0 COMMENT 'accessCount(accessCount)';
ALTER TABLE `multimedia_file` ADD COLUMN `bitrate` INT DEFAULT 0 COMMENT 'bitrate(bitrate)';
ALTER TABLE `multimedia_file` ADD COLUMN `channels` INT DEFAULT 0 COMMENT 'channels(channels)';
ALTER TABLE `multimedia_file` ADD COLUMN `codec` VARCHAR(255) DEFAULT NULL COMMENT 'codec(codec)';
ALTER TABLE `multimedia_file` ADD COLUMN `durationMs` BIGINT DEFAULT 0 COMMENT 'durationMs(durationMs)';
ALTER TABLE `multimedia_file` ADD COLUMN `encrypted` INT DEFAULT 0 COMMENT 'encrypted(encrypted)';
ALTER TABLE `multimedia_file` ADD COLUMN `exif` VARCHAR(255) DEFAULT NULL COMMENT 'exif(exif)';
ALTER TABLE `multimedia_file` ADD COLUMN `expireAt` DATETIME DEFAULT NULL COMMENT 'expireAt(expireAt)';
ALTER TABLE `multimedia_file` ADD COLUMN `fileHash` VARCHAR(255) DEFAULT NULL COMMENT 'fileHash(fileHash)';
ALTER TABLE `multimedia_file` ADD COLUMN `fileName` VARCHAR(255) DEFAULT NULL COMMENT 'fileName(fileName)';
ALTER TABLE `multimedia_file` ADD COLUMN `fileSize` BIGINT DEFAULT 0 COMMENT 'fileSize(fileSize)';
ALTER TABLE `multimedia_file` ADD COLUMN `fileType` VARCHAR(255) DEFAULT NULL COMMENT 'fileType(fileType)';
ALTER TABLE `multimedia_file` ADD COLUMN `height` INT DEFAULT 0 COMMENT 'height(height)';
ALTER TABLE `multimedia_file` ADD COLUMN `isPublic` INT DEFAULT 0 COMMENT 'isPublic(isPublic)';
ALTER TABLE `multimedia_file` ADD COLUMN `mimeType` VARCHAR(255) DEFAULT NULL COMMENT 'mimeType(mimeType)';
ALTER TABLE `multimedia_file` ADD COLUMN `moderationId` BIGINT DEFAULT 0 COMMENT 'moderationId(moderationId)';
ALTER TABLE `multimedia_file` ADD COLUMN `moderationStatus` VARCHAR(255) DEFAULT NULL COMMENT 'moderationStatus(moderationStatus)';
ALTER TABLE `multimedia_file` ADD COLUMN `originalName` VARCHAR(255) DEFAULT NULL COMMENT 'originalName(originalName)';
ALTER TABLE `multimedia_file` ADD COLUMN `sampleRate` INT DEFAULT 0 COMMENT 'sampleRate(sampleRate)';
ALTER TABLE `multimedia_file` ADD COLUMN `storagePath` VARCHAR(255) DEFAULT NULL COMMENT 'storagePath(storagePath)';
ALTER TABLE `multimedia_file` ADD COLUMN `storageType` VARCHAR(255) DEFAULT NULL COMMENT 'storageType(storageType)';
ALTER TABLE `multimedia_file` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `multimedia_file` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';
ALTER TABLE `multimedia_file` ADD COLUMN `watermarked` INT DEFAULT 0 COMMENT 'watermarked(watermarked)';
ALTER TABLE `multimedia_file` ADD COLUMN `width` INT DEFAULT 0 COMMENT 'width(width)';

-- 表: pipeline_log
ALTER TABLE `pipeline_log` ADD COLUMN `clientIp` VARCHAR(255) DEFAULT NULL COMMENT 'clientIp(clientIp)';
ALTER TABLE `pipeline_log` ADD COLUMN `computeDevice` VARCHAR(255) DEFAULT NULL COMMENT 'computeDevice(computeDevice)';
ALTER TABLE `pipeline_log` ADD COLUMN `computeMode` VARCHAR(255) DEFAULT NULL COMMENT 'computeMode(computeMode)';
ALTER TABLE `pipeline_log` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `pipeline_log` ADD COLUMN `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)';
ALTER TABLE `pipeline_log` ADD COLUMN `inputModality` VARCHAR(255) DEFAULT NULL COMMENT 'inputModality(inputModality)';
ALTER TABLE `pipeline_log` ADD COLUMN `inputText` VARCHAR(255) DEFAULT NULL COMMENT 'inputText(inputText)';
ALTER TABLE `pipeline_log` ADD COLUMN `intent` VARCHAR(255) DEFAULT NULL COMMENT 'intent(intent)';
ALTER TABLE `pipeline_log` ADD COLUMN `needsReview` TINYINT(1) DEFAULT 0 COMMENT 'needsReview(needsReview)';
ALTER TABLE `pipeline_log` ADD COLUMN `outputText` VARCHAR(255) DEFAULT NULL COMMENT 'outputText(outputText)';
ALTER TABLE `pipeline_log` ADD COLUMN `outputTokens` INT DEFAULT 0 COMMENT 'outputTokens(outputTokens)';
ALTER TABLE `pipeline_log` ADD COLUMN `ragHits` INT DEFAULT 0 COMMENT 'ragHits(ragHits)';
ALTER TABLE `pipeline_log` ADD COLUMN `riskLevel` VARCHAR(255) DEFAULT NULL COMMENT 'riskLevel(riskLevel)';
ALTER TABLE `pipeline_log` ADD COLUMN `stageCosts` VARCHAR(255) DEFAULT NULL COMMENT 'stageCosts(stageCosts)';
ALTER TABLE `pipeline_log` ADD COLUMN `toolCalls` INT DEFAULT 0 COMMENT 'toolCalls(toolCalls)';
ALTER TABLE `pipeline_log` ADD COLUMN `totalCostMs` BIGINT DEFAULT 0 COMMENT 'totalCostMs(totalCostMs)';
ALTER TABLE `pipeline_log` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';

-- 表: push_message
ALTER TABLE `push_message` ADD COLUMN `body` VARCHAR(255) DEFAULT NULL COMMENT 'body(body)';
ALTER TABLE `push_message` ADD COLUMN `clickAction` VARCHAR(255) DEFAULT NULL COMMENT 'clickAction(clickAction)';
ALTER TABLE `push_message` ADD COLUMN `data` VARCHAR(255) DEFAULT NULL COMMENT 'data(data)';
ALTER TABLE `push_message` ADD COLUMN `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)';
ALTER TABLE `push_message` ADD COLUMN `failureCount` INT DEFAULT 0 COMMENT 'failureCount(failureCount)';
ALTER TABLE `push_message` ADD COLUMN `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon(icon)';
ALTER TABLE `push_message` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `push_message` ADD COLUMN `successCount` INT DEFAULT 0 COMMENT 'successCount(successCount)';
ALTER TABLE `push_message` ADD COLUMN `targetType` VARCHAR(255) DEFAULT NULL COMMENT 'targetType(targetType)';
ALTER TABLE `push_message` ADD COLUMN `targetValue` VARCHAR(255) DEFAULT NULL COMMENT 'targetValue(targetValue)';
ALTER TABLE `push_message` ADD COLUMN `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)';

-- 表: push_subscription
ALTER TABLE `push_subscription` ADD COLUMN `authKey` VARCHAR(255) DEFAULT NULL COMMENT 'authKey(authKey)';
ALTER TABLE `push_subscription` ADD COLUMN `endpoint` VARCHAR(255) DEFAULT NULL COMMENT 'endpoint(endpoint)';
ALTER TABLE `push_subscription` ADD COLUMN `lastActiveAt` DATETIME DEFAULT NULL COMMENT 'lastActiveAt(lastActiveAt)';
ALTER TABLE `push_subscription` ADD COLUMN `p256dhKey` VARCHAR(255) DEFAULT NULL COMMENT 'p256dhKey(p256dhKey)';
ALTER TABLE `push_subscription` ADD COLUMN `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform(platform)';
ALTER TABLE `push_subscription` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `push_subscription` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';
ALTER TABLE `push_subscription` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';

-- 表: sensitive_word
ALTER TABLE `sensitive_word` ADD COLUMN `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)';
ALTER TABLE `sensitive_word` ADD COLUMN `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)';
ALTER TABLE `sensitive_word` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `sensitive_word` ADD COLUMN `level` VARCHAR(255) DEFAULT NULL COMMENT 'level(level)';

-- 表: training_checkpoint
ALTER TABLE `training_checkpoint` ADD COLUMN `accuracy` DOUBLE DEFAULT 0 COMMENT 'accuracy(accuracy)';
ALTER TABLE `training_checkpoint` ADD COLUMN `checkpointId` VARCHAR(255) DEFAULT NULL COMMENT 'checkpointId(checkpointId)';
ALTER TABLE `training_checkpoint` ADD COLUMN `epoch` INT DEFAULT 0 COMMENT 'epoch(epoch)';
ALTER TABLE `training_checkpoint` ADD COLUMN `filePath` VARCHAR(255) DEFAULT NULL COMMENT 'filePath(filePath)';
ALTER TABLE `training_checkpoint` ADD COLUMN `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata(metadata)';
ALTER TABLE `training_checkpoint` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `training_checkpoint` ADD COLUMN `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)';
ALTER TABLE `training_checkpoint` ADD COLUMN `sizeBytes` BIGINT DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)';
ALTER TABLE `training_checkpoint` ADD COLUMN `step` INT DEFAULT 0 COMMENT 'step(step)';
ALTER TABLE `training_checkpoint` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `training_checkpoint` ADD COLUMN `valLoss` DOUBLE DEFAULT 0 COMMENT 'valLoss(valLoss)';

-- 表: training_job
ALTER TABLE `training_job` ADD COLUMN `config` VARCHAR(255) DEFAULT NULL COMMENT 'config(config)';
ALTER TABLE `training_job` ADD COLUMN `currentEpoch` INT DEFAULT 0 COMMENT 'currentEpoch(currentEpoch)';
ALTER TABLE `training_job` ADD COLUMN `currentStep` INT DEFAULT 0 COMMENT 'currentStep(currentStep)';
ALTER TABLE `training_job` ADD COLUMN `endTimeMs` BIGINT DEFAULT 0 COMMENT 'endTimeMs(endTimeMs)';
ALTER TABLE `training_job` ADD COLUMN `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)';
ALTER TABLE `training_job` ADD COLUMN `lastAccuracy` DOUBLE DEFAULT 0 COMMENT 'lastAccuracy(lastAccuracy)';
ALTER TABLE `training_job` ADD COLUMN `lastLoss` DOUBLE DEFAULT 0 COMMENT 'lastLoss(lastLoss)';
ALTER TABLE `training_job` ADD COLUMN `lastValLoss` DOUBLE DEFAULT 0 COMMENT 'lastValLoss(lastValLoss)';
ALTER TABLE `training_job` ADD COLUMN `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)';
ALTER TABLE `training_job` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `training_job` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `training_job` ADD COLUMN `startTimeMs` BIGINT DEFAULT 0 COMMENT 'startTimeMs(startTimeMs)';
ALTER TABLE `training_job` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `training_job` ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)';
ALTER TABLE `training_job` ADD COLUMN `totalEpochs` INT DEFAULT 0 COMMENT 'totalEpochs(totalEpochs)';
ALTER TABLE `training_job` ADD COLUMN `totalSteps` INT DEFAULT 0 COMMENT 'totalSteps(totalSteps)';

-- 表: training_metric
ALTER TABLE `training_metric` ADD COLUMN `accuracy` DOUBLE DEFAULT 0 COMMENT 'accuracy(accuracy)';
ALTER TABLE `training_metric` ADD COLUMN `elapsedMs` BIGINT DEFAULT 0 COMMENT 'elapsedMs(elapsedMs)';
ALTER TABLE `training_metric` ADD COLUMN `epoch` INT DEFAULT 0 COMMENT 'epoch(epoch)';
ALTER TABLE `training_metric` ADD COLUMN `learningRate` DOUBLE DEFAULT 0 COMMENT 'learningRate(learningRate)';
ALTER TABLE `training_metric` ADD COLUMN `loss` DOUBLE DEFAULT 0 COMMENT 'loss(loss)';
ALTER TABLE `training_metric` ADD COLUMN `step` INT DEFAULT 0 COMMENT 'step(step)';
ALTER TABLE `training_metric` ADD COLUMN `valLoss` DOUBLE DEFAULT 0 COMMENT 'valLoss(valLoss)';

