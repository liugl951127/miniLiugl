-- =============================================================
-- MiniMax Platform V3.5.5+ 完整 SQL DDL (扫描所有 Entity 自动生成)
-- 共 77 张表 / 13 个模块
-- 生成时间: Mon Jul 13 10:20:37 UTC 2026
-- 生成工具: scripts/gen_complete_sql.py
--
-- 用法: 
--   1. 全新部署: docker compose -f docker-compose.mini.yml up -d
--   2. 增量修复: mysql -uroot -proot123456 minimax_platform < complete.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS `minimax_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `minimax_platform`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- =========================================
-- 模块: auth (14 张表)
-- =========================================

-- auth/AuthLoginLog.java
DROP TABLE IF EXISTS `auth_login_log`;
CREATE TABLE IF NOT EXISTS `auth_login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)',
    `status` INT DEFAULT 0 COMMENT '状态(status)',
    `message` VARCHAR(255) DEFAULT NULL COMMENT 'message(message)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='auth_login_log (auto-generated V3.5.5)';

-- auth/AuthRefreshToken.java
DROP TABLE IF EXISTS `auth_refresh_token`;
CREATE TABLE IF NOT EXISTS `auth_refresh_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `token` VARCHAR(255) DEFAULT NULL COMMENT 'token(token)',
    `expiresAt` DATETIME DEFAULT NULL COMMENT 'expiresAt(expiresAt)',
    `revoked` INT DEFAULT 0 COMMENT 'revoked(revoked)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='auth_refresh_token (auto-generated V3.5.5)';

-- auth/Notification.java
DROP TABLE IF EXISTS `notification`;
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `isRead` INT DEFAULT 0 COMMENT 'isRead(isRead)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='notification (auto-generated V3.5.5)';

-- auth/OAuthAppConfig.java
DROP TABLE IF EXISTS `oauth_app_config`;
CREATE TABLE IF NOT EXISTS `oauth_app_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform(platform)',
    `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)',
    `appId` VARCHAR(255) DEFAULT NULL COMMENT 'appId(appId)',
    `appSecret` VARCHAR(255) DEFAULT NULL COMMENT 'appSecret(appSecret)',
    `publicKey` VARCHAR(255) DEFAULT NULL COMMENT 'publicKey(publicKey)',
    `redirectUri` VARCHAR(255) DEFAULT NULL COMMENT 'redirectUri(redirectUri)',
    `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes(scopes)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `extraConfig` VARCHAR(255) DEFAULT NULL COMMENT 'extraConfig(extraConfig)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='oauth_app_config (auto-generated V3.5.5)';

-- auth/OAuthBinding.java
DROP TABLE IF EXISTS `oauth_binding`;
CREATE TABLE IF NOT EXISTS `oauth_binding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform(platform)',
    `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid(openid)',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)',
    `accessToken` VARCHAR(255) DEFAULT NULL COMMENT 'accessToken(accessToken)',
    `refreshToken` VARCHAR(255) DEFAULT NULL COMMENT 'refreshToken(refreshToken)',
    `tokenExpiresAt` DATETIME DEFAULT NULL COMMENT 'tokenExpiresAt(tokenExpiresAt)',
    `rawData` VARCHAR(255) DEFAULT NULL COMMENT 'rawData(rawData)',
    `boundAt` DATETIME DEFAULT NULL COMMENT 'boundAt(boundAt)',
    `lastLoginAt` DATETIME DEFAULT NULL COMMENT 'lastLoginAt(lastLoginAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='oauth_binding (auto-generated V3.5.5)';

-- auth/SysRole.java
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `code` VARCHAR(255) DEFAULT NULL COMMENT '编码(code)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `sort` INT DEFAULT 0 COMMENT '排序号(sort)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys_role (auto-generated V3.5.5)';

-- auth/SysUser.java
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `password` VARCHAR(255) DEFAULT NULL COMMENT 'password(password)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)',
    `email` VARCHAR(255) DEFAULT NULL COMMENT 'email(email)',
    `phone` VARCHAR(255) DEFAULT NULL COMMENT 'phone(phone)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)',
    `gender` INT DEFAULT 0 COMMENT 'gender(gender)',
    `status` INT DEFAULT 0 COMMENT '状态(status)',
    `lastLoginIp` VARCHAR(255) DEFAULT NULL COMMENT 'lastLoginIp(lastLoginIp)',
    `lastLoginAt` DATETIME DEFAULT NULL COMMENT 'lastLoginAt(lastLoginAt)',
    `tenantId` BIGINT DEFAULT 0 COMMENT '租户ID(tenantId)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注(remark)',
    `wechatOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'wechatOpenid(wechatOpenid)',
    `wechatUnionid` VARCHAR(255) DEFAULT NULL COMMENT 'wechatUnionid(wechatUnionid)',
    `wechatNickname` VARCHAR(255) DEFAULT NULL COMMENT 'wechatNickname(wechatNickname)',
    `wechatAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'wechatAvatar(wechatAvatar)',
    `wechatBoundAt` DATETIME DEFAULT NULL COMMENT 'wechatBoundAt(wechatBoundAt)',
    `qqOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'qqOpenid(qqOpenid)',
    `qqUnionid` VARCHAR(255) DEFAULT NULL COMMENT 'qqUnionid(qqUnionid)',
    `qqNickname` VARCHAR(255) DEFAULT NULL COMMENT 'qqNickname(qqNickname)',
    `qqAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'qqAvatar(qqAvatar)',
    `qqBoundAt` DATETIME DEFAULT NULL COMMENT 'qqBoundAt(qqBoundAt)',
    `alipayOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'alipayOpenid(alipayOpenid)',
    `alipayUserId` VARCHAR(255) DEFAULT NULL COMMENT 'alipayUserId(alipayUserId)',
    `alipayNickname` VARCHAR(255) DEFAULT NULL COMMENT 'alipayNickname(alipayNickname)',
    `alipayAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'alipayAvatar(alipayAvatar)',
    `alipayBoundAt` DATETIME DEFAULT NULL COMMENT 'alipayBoundAt(alipayBoundAt)',
    `createdBy` BIGINT DEFAULT 0 COMMENT '创建人ID(createdBy)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedBy` BIGINT DEFAULT 0 COMMENT '更新人ID(updatedBy)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys_user (auto-generated V3.5.5)';

-- auth/SysUserRole.java
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `roleId` BIGINT DEFAULT 0 COMMENT 'roleId(roleId)',
    PRIMARY KEY (`userId`, `roleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sys_user_role (auto-generated V3.5.5)';

-- auth/Tenant.java
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE IF NOT EXISTS `tenant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `code` VARCHAR(255) DEFAULT NULL COMMENT '编码(code)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `plan` VARCHAR(255) DEFAULT NULL COMMENT 'plan(plan)',
    `status` INT DEFAULT 0 COMMENT '状态(status)',
    `maxUsers` INT DEFAULT 0 COMMENT 'maxUsers(maxUsers)',
    `maxModels` INT DEFAULT 0 COMMENT 'maxModels(maxModels)',
    `qpsLimit` INT DEFAULT 0 COMMENT 'qpsLimit(qpsLimit)',
    `monthlyQuota` BIGINT DEFAULT 0 COMMENT 'monthlyQuota(monthlyQuota)',
    `usedQuota` BIGINT DEFAULT 0 COMMENT 'usedQuota(usedQuota)',
    `expireAt` DATETIME DEFAULT NULL COMMENT 'expireAt(expireAt)',
    `contactEmail` VARCHAR(255) DEFAULT NULL COMMENT 'contactEmail(contactEmail)',
    `contactPhone` VARCHAR(255) DEFAULT NULL COMMENT 'contactPhone(contactPhone)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注(remark)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='tenant (auto-generated V3.5.5)';

-- auth/UnionidRelations.java
DROP TABLE IF EXISTS `unionid_relations`;
CREATE TABLE IF NOT EXISTS `unionid_relations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform(platform)',
    `firstSeenAt` DATETIME DEFAULT NULL COMMENT 'firstSeenAt(firstSeenAt)',
    `lastSeenAt` DATETIME DEFAULT NULL COMMENT 'lastSeenAt(lastSeenAt)',
    `bindingCount` INT DEFAULT 0 COMMENT 'bindingCount(bindingCount)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='unionid_relations (auto-generated V3.5.5)';

-- auth/UserApiKey.java
DROP TABLE IF EXISTS `user_api_key`;
CREATE TABLE IF NOT EXISTS `user_api_key` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `keyHash` VARCHAR(255) DEFAULT NULL COMMENT 'keyHash(keyHash)',
    `keyPrefix` VARCHAR(255) DEFAULT NULL COMMENT 'keyPrefix(keyPrefix)',
    `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes(scopes)',
    `expiresAt` DATETIME DEFAULT NULL COMMENT 'expiresAt(expiresAt)',
    `lastUsedAt` DATETIME DEFAULT NULL COMMENT 'lastUsedAt(lastUsedAt)',
    `useCount` BIGINT DEFAULT 0 COMMENT 'useCount(useCount)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user_api_key (auto-generated V3.5.5)';

-- auth/WechatConfig.java
DROP TABLE IF EXISTS `wechat_config`;
CREATE TABLE IF NOT EXISTS `wechat_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)',
    `appId` VARCHAR(255) DEFAULT NULL COMMENT 'appId(appId)',
    `appSecret` VARCHAR(255) DEFAULT NULL COMMENT 'appSecret(appSecret)',
    `token` VARCHAR(255) DEFAULT NULL COMMENT 'token(token)',
    `aesKey` VARCHAR(255) DEFAULT NULL COMMENT 'aesKey(aesKey)',
    `redirectUri` VARCHAR(255) DEFAULT NULL COMMENT 'redirectUri(redirectUri)',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注(remark)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='wechat_config (auto-generated V3.5.5)';

-- auth/WechatScanSession.java
DROP TABLE IF EXISTS `wechat_scan_session`;
CREATE TABLE IF NOT EXISTS `wechat_scan_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `ticket` VARCHAR(255) DEFAULT NULL COMMENT 'ticket(ticket)',
    `sceneId` VARCHAR(255) DEFAULT NULL COMMENT 'sceneId(sceneId)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid(openid)',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `accessToken` VARCHAR(255) DEFAULT NULL COMMENT 'accessToken(accessToken)',
    `refreshToken` VARCHAR(255) DEFAULT NULL COMMENT 'refreshToken(refreshToken)',
    `clientIp` VARCHAR(255) DEFAULT NULL COMMENT 'clientIp(clientIp)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)',
    `expiresAt` DATETIME DEFAULT NULL COMMENT 'expiresAt(expiresAt)',
    `confirmedAt` DATETIME DEFAULT NULL COMMENT 'confirmedAt(confirmedAt)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='wechat_scan_session (auto-generated V3.5.5)';

-- auth/WechatUserBinding.java
DROP TABLE IF EXISTS `wechat_user_binding`;
CREATE TABLE IF NOT EXISTS `wechat_user_binding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid(openid)',
    `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)',
    `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)',
    `boundAt` DATETIME DEFAULT NULL COMMENT 'boundAt(boundAt)',
    `lastLoginAt` DATETIME DEFAULT NULL COMMENT 'lastLoginAt(lastLoginAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='wechat_user_binding (auto-generated V3.5.5)';


-- =========================================
-- 模块: ai (27 张表)
-- =========================================

-- ai/AgentGroup.java
DROP TABLE IF EXISTS `agent_group`;
CREATE TABLE IF NOT EXISTS `agent_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `groupId` VARCHAR(255) DEFAULT NULL COMMENT 'groupId(groupId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `strategy` VARCHAR(255) DEFAULT NULL COMMENT 'strategy(strategy)',
    `membersJson` VARCHAR(255) DEFAULT NULL COMMENT 'membersJson(membersJson)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `lastRunAt` DATETIME DEFAULT NULL COMMENT 'lastRunAt(lastRunAt)',
    `runCount` INT DEFAULT 0 COMMENT 'runCount(runCount)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='agent_group (auto-generated V3.5.5)';

-- ai/AiChatMessage.java
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `sessionId` VARCHAR(255) DEFAULT NULL COMMENT 'sessionId(sessionId)',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `toolCode` VARCHAR(255) DEFAULT NULL COMMENT 'toolCode(toolCode)',
    `toolInput` VARCHAR(255) DEFAULT NULL COMMENT 'toolInput(toolInput)',
    `toolOutput` VARCHAR(255) DEFAULT NULL COMMENT 'toolOutput(toolOutput)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_chat_message (auto-generated V3.5.5)';

-- ai/AiChatSession.java
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `sessionId` VARCHAR(255) DEFAULT NULL COMMENT 'sessionId(sessionId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_chat_session (auto-generated V3.5.5)';

-- ai/AiGenerationLog.java
DROP TABLE IF EXISTS `ai_generation_log`;
CREATE TABLE IF NOT EXISTS `ai_generation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `generationId` VARCHAR(255) DEFAULT NULL COMMENT 'generationId(generationId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp(userIp)',
    `modality` VARCHAR(255) DEFAULT NULL COMMENT 'modality(modality)',
    `modelName` VARCHAR(255) DEFAULT NULL COMMENT 'modelName(modelName)',
    `modelVersion` VARCHAR(255) DEFAULT NULL COMMENT 'modelVersion(modelVersion)',
    `prompt` VARCHAR(255) DEFAULT NULL COMMENT 'prompt(prompt)',
    `negativePrompt` VARCHAR(255) DEFAULT NULL COMMENT 'negativePrompt(negativePrompt)',
    `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters(parameters)',
    `outputUrl` VARCHAR(255) DEFAULT NULL COMMENT 'outputUrl(outputUrl)',
    `outputSize` BIGINT DEFAULT 0 COMMENT 'outputSize(outputSize)',
    `outputHash` VARCHAR(255) DEFAULT NULL COMMENT 'outputHash(outputHash)',
    `watermarked` INT DEFAULT 0 COMMENT 'watermarked(watermarked)',
    `watermarkText` VARCHAR(255) DEFAULT NULL COMMENT 'watermarkText(watermarkText)',
    `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_generation_log (auto-generated V3.5.5)';

-- ai/AiIntentKeyword.java
DROP TABLE IF EXISTS `ai_intent_keyword`;
CREATE TABLE IF NOT EXISTS `ai_intent_keyword` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `intent` VARCHAR(255) DEFAULT NULL COMMENT 'intent(intent)',
    `keyword` VARCHAR(255) DEFAULT NULL COMMENT 'keyword(keyword)',
    `weight` INT DEFAULT 0 COMMENT 'weight(weight)',
    `isRegex` INT DEFAULT 0 COMMENT 'isRegex(isRegex)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `language` VARCHAR(255) DEFAULT NULL COMMENT 'language(language)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注(remark)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_intent_keyword (auto-generated V3.5.5)';

-- ai/AiTool.java
DROP TABLE IF EXISTS `ai_tool`;
CREATE TABLE IF NOT EXISTS `ai_tool` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `code` VARCHAR(255) DEFAULT NULL COMMENT '编码(code)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon(icon)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `builtin` INT DEFAULT 0 COMMENT 'builtin(builtin)',
    `inputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'inputSchema(inputSchema)',
    `outputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'outputSchema(outputSchema)',
    `defaultConfig` VARCHAR(255) DEFAULT NULL COMMENT 'defaultConfig(defaultConfig)',
    `implType` VARCHAR(255) DEFAULT NULL COMMENT 'implType(implType)',
    `implValue` VARCHAR(255) DEFAULT NULL COMMENT 'implValue(implValue)',
    `rateLimit` INT DEFAULT 0 COMMENT 'rateLimit(rateLimit)',
    `timeoutSeconds` INT DEFAULT 0 COMMENT 'timeoutSeconds(timeoutSeconds)',
    `roleRequired` VARCHAR(255) DEFAULT NULL COMMENT 'roleRequired(roleRequired)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `version` VARCHAR(255) DEFAULT NULL COMMENT '乐观锁版本号(version)',
    `author` VARCHAR(255) DEFAULT NULL COMMENT 'author(author)',
    `createdBy` BIGINT DEFAULT 0 COMMENT '创建人ID(createdBy)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_tool (auto-generated V3.5.5)';

-- ai/AiToolInvocation.java
DROP TABLE IF EXISTS `ai_tool_invocation`;
CREATE TABLE IF NOT EXISTS `ai_tool_invocation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `toolCode` VARCHAR(255) DEFAULT NULL COMMENT 'toolCode(toolCode)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `inputJson` VARCHAR(255) DEFAULT NULL COMMENT 'inputJson(inputJson)',
    `outputJson` VARCHAR(255) DEFAULT NULL COMMENT 'outputJson(outputJson)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)',
    `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)',
    `dataSourceId` BIGINT DEFAULT 0 COMMENT 'dataSourceId(dataSourceId)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_tool_invocation (auto-generated V3.5.5)';

-- ai/AuditLog.java
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `traceId` VARCHAR(255) DEFAULT NULL COMMENT 'traceId(traceId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp(userIp)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)',
    `resourceType` VARCHAR(255) DEFAULT NULL COMMENT 'resourceType(resourceType)',
    `resourceId` VARCHAR(255) DEFAULT NULL COMMENT 'resourceId(resourceId)',
    `method` VARCHAR(255) DEFAULT NULL COMMENT 'method(method)',
    `path` VARCHAR(255) DEFAULT NULL COMMENT 'path(path)',
    `requestBody` VARCHAR(255) DEFAULT NULL COMMENT 'requestBody(requestBody)',
    `responseStatus` INT DEFAULT 0 COMMENT 'responseStatus(responseStatus)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)',
    `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='audit_log (auto-generated V3.5.5)';

-- ai/BillingRecord.java
DROP TABLE IF EXISTS `billing_record`;
CREATE TABLE IF NOT EXISTS `billing_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `recordId` VARCHAR(255) DEFAULT NULL COMMENT 'recordId(recordId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `licenseId` BIGINT DEFAULT 0 COMMENT 'licenseId(licenseId)',
    `modelEntryId` BIGINT DEFAULT 0 COMMENT 'modelEntryId(modelEntryId)',
    `recordType` VARCHAR(255) DEFAULT NULL COMMENT 'recordType(recordType)',
    `amountCents` BIGINT DEFAULT 0 COMMENT 'amountCents(amountCents)',
    `currency` VARCHAR(255) DEFAULT NULL COMMENT 'currency(currency)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `paymentMethod` VARCHAR(255) DEFAULT NULL COMMENT 'paymentMethod(paymentMethod)',
    `externalTransactionId` VARCHAR(255) DEFAULT NULL COMMENT 'externalTransactionId(externalTransactionId)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='billing_record (auto-generated V3.5.5)';

-- ai/ClusterNode.java
DROP TABLE IF EXISTS `cluster_node`;
CREATE TABLE IF NOT EXISTS `cluster_node` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `nodeId` VARCHAR(255) DEFAULT NULL COMMENT 'nodeId(nodeId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `address` VARCHAR(255) DEFAULT NULL COMMENT 'address(address)',
    `region` VARCHAR(255) DEFAULT NULL COMMENT 'region(region)',
    `zone` VARCHAR(255) DEFAULT NULL COMMENT 'zone(zone)',
    `capabilities` VARCHAR(255) DEFAULT NULL COMMENT 'capabilities(capabilities)',
    `totalCores` INT DEFAULT 0 COMMENT 'totalCores(totalCores)',
    `totalMemoryMb` BIGINT DEFAULT 0 COMMENT 'totalMemoryMb(totalMemoryMb)',
    `totalGpus` INT DEFAULT 0 COMMENT 'totalGpus(totalGpus)',
    `cpuUsage` DOUBLE DEFAULT 0 COMMENT 'cpuUsage(cpuUsage)',
    `memoryUsage` DOUBLE DEFAULT 0 COMMENT 'memoryUsage(memoryUsage)',
    `gpuUsage` DOUBLE DEFAULT 0 COMMENT 'gpuUsage(gpuUsage)',
    `activeTasks` INT DEFAULT 0 COMMENT 'activeTasks(activeTasks)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `isLeader` TINYINT(1) DEFAULT 0 COMMENT 'isLeader(isLeader)',
    `lastHeartbeat` DATETIME DEFAULT NULL COMMENT 'lastHeartbeat(lastHeartbeat)',
    `startedAt` DATETIME DEFAULT NULL COMMENT 'startedAt(startedAt)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='cluster_node (auto-generated V3.5.5)';

-- ai/DashboardMetric.java
DROP TABLE IF EXISTS `dashboard_metric`;
CREATE TABLE IF NOT EXISTS `dashboard_metric` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `metric` VARCHAR(255) DEFAULT NULL COMMENT 'metric(metric)',
    `dimension` VARCHAR(255) DEFAULT NULL COMMENT 'dimension(dimension)',
    `value` DOUBLE DEFAULT 0 COMMENT 'value(value)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `timestamp` DATETIME DEFAULT NULL COMMENT 'timestamp(timestamp)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='dashboard_metric (auto-generated V3.5.5)';

-- ai/DbDataSource.java
DROP TABLE IF EXISTS `data_source`;
CREATE TABLE IF NOT EXISTS `data_source` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `jdbcUrl` VARCHAR(255) DEFAULT NULL COMMENT 'jdbcUrl(jdbcUrl)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `password` VARCHAR(255) DEFAULT NULL COMMENT 'password(password)',
    `driverClass` VARCHAR(255) DEFAULT NULL COMMENT 'driverClass(driverClass)',
    `poolSize` INT DEFAULT 0 COMMENT 'poolSize(poolSize)',
    `minIdle` INT DEFAULT 0 COMMENT 'minIdle(minIdle)',
    `maxLifetime` INT DEFAULT 0 COMMENT 'maxLifetime(maxLifetime)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `testStatus` VARCHAR(255) DEFAULT NULL COMMENT 'testStatus(testStatus)',
    `testMessage` VARCHAR(255) DEFAULT NULL COMMENT 'testMessage(testMessage)',
    `lastTestAt` DATETIME DEFAULT NULL COMMENT 'lastTestAt(lastTestAt)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `createdBy` BIGINT DEFAULT 0 COMMENT '创建人ID(createdBy)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='data_source (auto-generated V3.5.5)';

-- ai/KbChunk.java
DROP TABLE IF EXISTS `kb_chunk`;
CREATE TABLE IF NOT EXISTS `kb_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `chunkId` VARCHAR(255) DEFAULT NULL COMMENT 'chunkId(chunkId)',
    `docId` VARCHAR(255) DEFAULT NULL COMMENT 'docId(docId)',
    `kbId` VARCHAR(255) DEFAULT NULL COMMENT 'kbId(kbId)',
    `seq` INT DEFAULT 0 COMMENT 'seq(seq)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `charCount` INT DEFAULT 0 COMMENT 'charCount(charCount)',
    `tokenCount` INT DEFAULT 0 COMMENT 'tokenCount(tokenCount)',
    `embedding` VARCHAR(255) DEFAULT NULL COMMENT 'embedding(embedding)',
    `embeddingModel` VARCHAR(255) DEFAULT NULL COMMENT 'embeddingModel(embeddingModel)',
    `keywords` VARCHAR(255) DEFAULT NULL COMMENT 'keywords(keywords)',
    `summary` VARCHAR(255) DEFAULT NULL COMMENT 'summary(summary)',
    `location` VARCHAR(255) DEFAULT NULL COMMENT 'location(location)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='kb_chunk (auto-generated V3.5.5)';

-- ai/KbDocument.java
DROP TABLE IF EXISTS `kb_document`;
CREATE TABLE IF NOT EXISTS `kb_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `docId` VARCHAR(255) DEFAULT NULL COMMENT 'docId(docId)',
    `kbId` VARCHAR(255) DEFAULT NULL COMMENT 'kbId(kbId)',
    `filename` VARCHAR(255) DEFAULT NULL COMMENT 'filename(filename)',
    `mimeType` VARCHAR(255) DEFAULT NULL COMMENT 'mimeType(mimeType)',
    `sizeBytes` BIGINT DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)',
    `filePath` VARCHAR(255) DEFAULT NULL COMMENT 'filePath(filePath)',
    `source` VARCHAR(255) DEFAULT NULL COMMENT 'source(source)',
    `sourceUrl` VARCHAR(255) DEFAULT NULL COMMENT 'sourceUrl(sourceUrl)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `chunkCount` INT DEFAULT 0 COMMENT 'chunkCount(chunkCount)',
    `embeddingCount` INT DEFAULT 0 COMMENT 'embeddingCount(embeddingCount)',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `isPublic` TINYINT(1) DEFAULT 0 COMMENT 'isPublic(isPublic)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='kb_document (auto-generated V3.5.5)';

-- ai/KbPermission.java
DROP TABLE IF EXISTS `kb_permission`;
CREATE TABLE IF NOT EXISTS `kb_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `kbId` VARCHAR(255) DEFAULT NULL COMMENT 'kbId(kbId)',
    `subjectType` VARCHAR(255) DEFAULT NULL COMMENT 'subjectType(subjectType)',
    `subjectId` BIGINT DEFAULT 0 COMMENT 'subjectId(subjectId)',
    `permission` VARCHAR(255) DEFAULT NULL COMMENT 'permission(permission)',
    `grantBy` BIGINT DEFAULT 0 COMMENT 'grantBy(grantBy)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='kb_permission (auto-generated V3.5.5)';

-- ai/ModelLicense.java
DROP TABLE IF EXISTS `model_license`;
CREATE TABLE IF NOT EXISTS `model_license` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `licenseKey` VARCHAR(255) DEFAULT NULL COMMENT 'licenseKey(licenseKey)',
    `modelEntryId` BIGINT DEFAULT 0 COMMENT 'modelEntryId(modelEntryId)',
    `modelVersionId` BIGINT DEFAULT 0 COMMENT 'modelVersionId(modelVersionId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `licenseType` VARCHAR(255) DEFAULT NULL COMMENT 'licenseType(licenseType)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `quotaCalls` BIGINT DEFAULT 0 COMMENT 'quotaCalls(quotaCalls)',
    `usedCalls` BIGINT DEFAULT 0 COMMENT 'usedCalls(usedCalls)',
    `startAt` DATETIME DEFAULT NULL COMMENT 'startAt(startAt)',
    `expireAt` DATETIME DEFAULT NULL COMMENT 'expireAt(expireAt)',
    `priceCents` BIGINT DEFAULT 0 COMMENT 'priceCents(priceCents)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='model_license (auto-generated V3.5.5)';

-- ai/ModelVersion.java
DROP TABLE IF EXISTS `model_version`;
CREATE TABLE IF NOT EXISTS `model_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `versionId` VARCHAR(255) DEFAULT NULL COMMENT 'versionId(versionId)',
    `modelEntryId` BIGINT DEFAULT 0 COMMENT 'modelEntryId(modelEntryId)',
    `version` VARCHAR(255) DEFAULT NULL COMMENT '乐观锁版本号(version)',
    `changelog` VARCHAR(255) DEFAULT NULL COMMENT 'changelog(changelog)',
    `filePath` VARCHAR(255) DEFAULT NULL COMMENT 'filePath(filePath)',
    `sizeBytes` BIGINT DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)',
    `inputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'inputSchema(inputSchema)',
    `outputSchema` VARCHAR(255) DEFAULT NULL COMMENT 'outputSchema(outputSchema)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `isLatest` TINYINT(1) DEFAULT 0 COMMENT 'isLatest(isLatest)',
    `uploaderId` BIGINT DEFAULT 0 COMMENT 'uploaderId(uploaderId)',
    `backwardCompatible` VARCHAR(255) DEFAULT NULL COMMENT 'backwardCompatible(backwardCompatible)',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata(metadata)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='model_version (auto-generated V3.5.5)';

-- ai/ModerationRecord.java
DROP TABLE IF EXISTS `moderation_record`;
CREATE TABLE IF NOT EXISTS `moderation_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `traceId` VARCHAR(255) DEFAULT NULL COMMENT 'traceId(traceId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `contentType` VARCHAR(255) DEFAULT NULL COMMENT 'contentType(contentType)',
    `contentHash` VARCHAR(255) DEFAULT NULL COMMENT 'contentHash(contentHash)',
    `contentSize` BIGINT DEFAULT 0 COMMENT 'contentSize(contentSize)',
    `contentUrl` VARCHAR(255) DEFAULT NULL COMMENT 'contentUrl(contentUrl)',
    `moderationStatus` VARCHAR(255) DEFAULT NULL COMMENT 'moderationStatus(moderationStatus)',
    `riskLevel` VARCHAR(255) DEFAULT NULL COMMENT 'riskLevel(riskLevel)',
    `riskLabels` VARCHAR(255) DEFAULT NULL COMMENT 'riskLabels(riskLabels)',
    `riskScore` DECIMAL(20,4) DEFAULT 0 COMMENT 'riskScore(riskScore)',
    `moderator` VARCHAR(255) DEFAULT NULL COMMENT 'moderator(moderator)',
    `rejectionReason` VARCHAR(255) DEFAULT NULL COMMENT 'rejectionReason(rejectionReason)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='moderation_record (auto-generated V3.5.5)';

-- ai/MultimediaFile.java
DROP TABLE IF EXISTS `multimedia_file`;
CREATE TABLE IF NOT EXISTS `multimedia_file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `fileId` VARCHAR(255) DEFAULT NULL COMMENT 'fileId(fileId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `fileName` VARCHAR(255) DEFAULT NULL COMMENT 'fileName(fileName)',
    `originalName` VARCHAR(255) DEFAULT NULL COMMENT 'originalName(originalName)',
    `fileType` VARCHAR(255) DEFAULT NULL COMMENT 'fileType(fileType)',
    `mimeType` VARCHAR(255) DEFAULT NULL COMMENT 'mimeType(mimeType)',
    `fileSize` BIGINT DEFAULT 0 COMMENT 'fileSize(fileSize)',
    `fileHash` VARCHAR(255) DEFAULT NULL COMMENT 'fileHash(fileHash)',
    `storagePath` VARCHAR(255) DEFAULT NULL COMMENT 'storagePath(storagePath)',
    `storageType` VARCHAR(255) DEFAULT NULL COMMENT 'storageType(storageType)',
    `encrypted` INT DEFAULT 0 COMMENT 'encrypted(encrypted)',
    `durationMs` BIGINT DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `width` INT DEFAULT 0 COMMENT 'width(width)',
    `height` INT DEFAULT 0 COMMENT 'height(height)',
    `bitrate` INT DEFAULT 0 COMMENT 'bitrate(bitrate)',
    `sampleRate` INT DEFAULT 0 COMMENT 'sampleRate(sampleRate)',
    `channels` INT DEFAULT 0 COMMENT 'channels(channels)',
    `codec` VARCHAR(255) DEFAULT NULL COMMENT 'codec(codec)',
    `exif` VARCHAR(255) DEFAULT NULL COMMENT 'exif(exif)',
    `moderationStatus` VARCHAR(255) DEFAULT NULL COMMENT 'moderationStatus(moderationStatus)',
    `moderationId` BIGINT DEFAULT 0 COMMENT 'moderationId(moderationId)',
    `watermarked` INT DEFAULT 0 COMMENT 'watermarked(watermarked)',
    `isPublic` INT DEFAULT 0 COMMENT 'isPublic(isPublic)',
    `accessCount` INT DEFAULT 0 COMMENT 'accessCount(accessCount)',
    `expireAt` DATETIME DEFAULT NULL COMMENT 'expireAt(expireAt)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='multimedia_file (auto-generated V3.5.5)';

-- ai/PipelineLog.java
DROP TABLE IF EXISTS `pipeline_log`;
CREATE TABLE IF NOT EXISTS `pipeline_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `sessionId` VARCHAR(255) DEFAULT NULL COMMENT 'sessionId(sessionId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `clientIp` VARCHAR(255) DEFAULT NULL COMMENT 'clientIp(clientIp)',
    `inputText` VARCHAR(255) DEFAULT NULL COMMENT 'inputText(inputText)',
    `inputModality` VARCHAR(255) DEFAULT NULL COMMENT 'inputModality(inputModality)',
    `intent` VARCHAR(255) DEFAULT NULL COMMENT 'intent(intent)',
    `outputText` VARCHAR(255) DEFAULT NULL COMMENT 'outputText(outputText)',
    `outputTokens` INT DEFAULT 0 COMMENT 'outputTokens(outputTokens)',
    `computeDevice` VARCHAR(255) DEFAULT NULL COMMENT 'computeDevice(computeDevice)',
    `computeMode` VARCHAR(255) DEFAULT NULL COMMENT 'computeMode(computeMode)',
    `totalCostMs` BIGINT DEFAULT 0 COMMENT 'totalCostMs(totalCostMs)',
    `stageCosts` VARCHAR(255) DEFAULT NULL COMMENT 'stageCosts(stageCosts)',
    `riskLevel` VARCHAR(255) DEFAULT NULL COMMENT 'riskLevel(riskLevel)',
    `needsReview` TINYINT(1) DEFAULT 0 COMMENT 'needsReview(needsReview)',
    `ragHits` INT DEFAULT 0 COMMENT 'ragHits(ragHits)',
    `toolCalls` INT DEFAULT 0 COMMENT 'toolCalls(toolCalls)',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='pipeline_log (auto-generated V3.5.5)';

-- ai/PushMessage.java
DROP TABLE IF EXISTS `push_message`;
CREATE TABLE IF NOT EXISTS `push_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `messageId` VARCHAR(255) DEFAULT NULL COMMENT 'messageId(messageId)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `body` VARCHAR(255) DEFAULT NULL COMMENT 'body(body)',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon(icon)',
    `clickAction` VARCHAR(255) DEFAULT NULL COMMENT 'clickAction(clickAction)',
    `data` VARCHAR(255) DEFAULT NULL COMMENT 'data(data)',
    `targetType` VARCHAR(255) DEFAULT NULL COMMENT 'targetType(targetType)',
    `targetValue` VARCHAR(255) DEFAULT NULL COMMENT 'targetValue(targetValue)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `successCount` INT DEFAULT 0 COMMENT 'successCount(successCount)',
    `failureCount` INT DEFAULT 0 COMMENT 'failureCount(failureCount)',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='push_message (auto-generated V3.5.5)';

-- ai/PushSubscription.java
DROP TABLE IF EXISTS `push_subscription`;
CREATE TABLE IF NOT EXISTS `push_subscription` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `subscriptionId` VARCHAR(255) DEFAULT NULL COMMENT 'subscriptionId(subscriptionId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform(platform)',
    `endpoint` VARCHAR(255) DEFAULT NULL COMMENT 'endpoint(endpoint)',
    `p256dhKey` VARCHAR(255) DEFAULT NULL COMMENT 'p256dhKey(p256dhKey)',
    `authKey` VARCHAR(255) DEFAULT NULL COMMENT 'authKey(authKey)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `lastActiveAt` DATETIME DEFAULT NULL COMMENT 'lastActiveAt(lastActiveAt)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='push_subscription (auto-generated V3.5.5)';

-- ai/LogEntry.java
DROP TABLE IF EXISTS `raft_log`;
CREATE TABLE IF NOT EXISTS `raft_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `term` BIGINT DEFAULT 0 COMMENT 'term(term)',
    `logIndex` BIGINT DEFAULT 0 COMMENT 'logIndex(logIndex)',
    `nodeId` VARCHAR(255) DEFAULT NULL COMMENT 'nodeId(nodeId)',
    `command` VARCHAR(255) DEFAULT NULL COMMENT 'command(command)',
    `committed` TINYINT(1) DEFAULT 0 COMMENT 'committed(committed)',
    `committedAt` DATETIME DEFAULT NULL COMMENT 'committedAt(committedAt)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='raft_log (auto-generated V3.5.5)';

-- ai/SensitiveWord.java
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE IF NOT EXISTS `sensitive_word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `word` VARCHAR(255) DEFAULT NULL COMMENT 'word(word)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)',
    `level` VARCHAR(255) DEFAULT NULL COMMENT 'level(level)',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sensitive_word (auto-generated V3.5.5)';

-- ai/TrainingCheckpoint.java
DROP TABLE IF EXISTS `training_checkpoint`;
CREATE TABLE IF NOT EXISTS `training_checkpoint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT 'taskId(taskId)',
    `checkpointId` VARCHAR(255) DEFAULT NULL COMMENT 'checkpointId(checkpointId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `epoch` INT DEFAULT 0 COMMENT 'epoch(epoch)',
    `step` INT DEFAULT 0 COMMENT 'step(step)',
    `filePath` VARCHAR(255) DEFAULT NULL COMMENT 'filePath(filePath)',
    `sizeBytes` BIGINT DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)',
    `sha256` VARCHAR(255) DEFAULT NULL COMMENT 'sha256(sha256)',
    `valLoss` DOUBLE DEFAULT 0 COMMENT 'valLoss(valLoss)',
    `accuracy` DOUBLE DEFAULT 0 COMMENT 'accuracy(accuracy)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata(metadata)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='training_checkpoint (auto-generated V3.5.5)';

-- ai/TrainingJob.java
DROP TABLE IF EXISTS `training_job`;
CREATE TABLE IF NOT EXISTS `training_job` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT 'taskId(taskId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `totalEpochs` INT DEFAULT 0 COMMENT 'totalEpochs(totalEpochs)',
    `currentEpoch` INT DEFAULT 0 COMMENT 'currentEpoch(currentEpoch)',
    `currentStep` INT DEFAULT 0 COMMENT 'currentStep(currentStep)',
    `startTimeMs` BIGINT DEFAULT 0 COMMENT 'startTimeMs(startTimeMs)',
    `endTimeMs` BIGINT DEFAULT 0 COMMENT 'endTimeMs(endTimeMs)',
    `config` VARCHAR(255) DEFAULT NULL COMMENT 'config(config)',
    `error` VARCHAR(255) DEFAULT NULL COMMENT 'error(error)',
    `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `lastLoss` DOUBLE DEFAULT 0 COMMENT 'lastLoss(lastLoss)',
    `lastValLoss` DOUBLE DEFAULT 0 COMMENT 'lastValLoss(lastValLoss)',
    `lastAccuracy` DOUBLE DEFAULT 0 COMMENT 'lastAccuracy(lastAccuracy)',
    `totalSteps` INT DEFAULT 0 COMMENT 'totalSteps(totalSteps)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='training_job (auto-generated V3.5.5)';

-- ai/TrainingMetric.java
DROP TABLE IF EXISTS `training_metric`;
CREATE TABLE IF NOT EXISTS `training_metric` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT 'taskId(taskId)',
    `epoch` INT DEFAULT 0 COMMENT 'epoch(epoch)',
    `step` INT DEFAULT 0 COMMENT 'step(step)',
    `loss` DOUBLE DEFAULT 0 COMMENT 'loss(loss)',
    `valLoss` DOUBLE DEFAULT 0 COMMENT 'valLoss(valLoss)',
    `accuracy` DOUBLE DEFAULT 0 COMMENT 'accuracy(accuracy)',
    `learningRate` DOUBLE DEFAULT 0 COMMENT 'learningRate(learningRate)',
    `elapsedMs` BIGINT DEFAULT 0 COMMENT 'elapsedMs(elapsedMs)',
    `timestamp` DATETIME DEFAULT NULL COMMENT 'timestamp(timestamp)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='training_metric (auto-generated V3.5.5)';


-- =========================================
-- 模块: agent (6 张表)
-- =========================================

-- agent/AgentTask.java
DROP TABLE IF EXISTS `agent_task`;
CREATE TABLE IF NOT EXISTS `agent_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT 'taskId(taskId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `goal` VARCHAR(255) DEFAULT NULL COMMENT 'goal(goal)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `rounds` INT DEFAULT 0 COMMENT 'rounds(rounds)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)',
    `llmCalls` INT DEFAULT 0 COMMENT 'llmCalls(llmCalls)',
    `toolCalls` INT DEFAULT 0 COMMENT 'toolCalls(toolCalls)',
    `totalTokens` INT DEFAULT 0 COMMENT 'totalTokens(totalTokens)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)',
    `latencyMs` BIGINT DEFAULT 0 COMMENT 'latencyMs(latencyMs)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='agent_task (auto-generated V3.5.5)';

-- agent/CollabMember.java
DROP TABLE IF EXISTS `collab_member`;
CREATE TABLE IF NOT EXISTS `collab_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `collabId` BIGINT DEFAULT 0 COMMENT 'collabId(collabId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)',
    `joinedAt` DATETIME DEFAULT NULL COMMENT 'joinedAt(joinedAt)',
    `lastActiveAt` DATETIME DEFAULT NULL COMMENT 'lastActiveAt(lastActiveAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='collab_member (auto-generated V3.5.5)';

-- agent/CollabSession.java
DROP TABLE IF EXISTS `collab_session`;
CREATE TABLE IF NOT EXISTS `collab_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `sessionId` VARCHAR(255) DEFAULT NULL COMMENT 'sessionId(sessionId)',
    `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `maxUsers` INT DEFAULT 0 COMMENT 'maxUsers(maxUsers)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='collab_session (auto-generated V3.5.5)';

-- agent/KgEntity.java
DROP TABLE IF EXISTS `kg_entity`;
CREATE TABLE IF NOT EXISTS `kg_entity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `entityType` VARCHAR(255) DEFAULT NULL COMMENT 'entityType(entityType)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `aliases` VARCHAR(255) DEFAULT NULL COMMENT 'aliases(aliases)',
    `importance` INT DEFAULT 0 COMMENT 'importance(importance)',
    `source` VARCHAR(255) DEFAULT NULL COMMENT 'source(source)',
    `refCount` INT DEFAULT 0 COMMENT 'refCount(refCount)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='kg_entity (auto-generated V3.5.5)';

-- agent/KgRelation.java
DROP TABLE IF EXISTS `kg_relation`;
CREATE TABLE IF NOT EXISTS `kg_relation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `fromEntity` BIGINT DEFAULT 0 COMMENT 'fromEntity(fromEntity)',
    `toEntity` BIGINT DEFAULT 0 COMMENT 'toEntity(toEntity)',
    `relationType` VARCHAR(255) DEFAULT NULL COMMENT 'relationType(relationType)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `weight` DECIMAL(20,4) DEFAULT 0 COMMENT 'weight(weight)',
    `source` VARCHAR(255) DEFAULT NULL COMMENT 'source(source)',
    `refCount` INT DEFAULT 0 COMMENT 'refCount(refCount)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='kg_relation (auto-generated V3.5.5)';

-- agent/Plugin.java
DROP TABLE IF EXISTS `plugin`;
CREATE TABLE IF NOT EXISTS `plugin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName(displayName)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `version` VARCHAR(255) DEFAULT NULL COMMENT '乐观锁版本号(version)',
    `author` VARCHAR(255) DEFAULT NULL COMMENT 'author(author)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)',
    `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT 'icon(icon)',
    `entry` VARCHAR(255) DEFAULT NULL COMMENT 'entry(entry)',
    `pluginType` VARCHAR(255) DEFAULT NULL COMMENT 'pluginType(pluginType)',
    `config` VARCHAR(255) DEFAULT NULL COMMENT 'config(config)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `downloads` INT DEFAULT 0 COMMENT 'downloads(downloads)',
    `rating` DECIMAL(20,4) DEFAULT 0 COMMENT 'rating(rating)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='plugin (auto-generated V3.5.5)';


-- =========================================
-- 模块: model (5 张表)
-- =========================================

-- model/ModelBattleLog.java
DROP TABLE IF EXISTS `model_battle_log`;
CREATE TABLE IF NOT EXISTS `model_battle_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `battleId` VARCHAR(255) DEFAULT NULL COMMENT 'battle_id(battle_id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'user_id(user_id)',
    `modelId` BIGINT DEFAULT 0 COMMENT 'model_id(model_id)',
    `modelCode` VARCHAR(255) DEFAULT NULL COMMENT 'model_code(model_code)',
    `prompt` VARCHAR(255) DEFAULT NULL COMMENT 'prompt(prompt)',
    `response` VARCHAR(255) DEFAULT NULL COMMENT 'response(response)',
    `promptTokens` INT DEFAULT 0 COMMENT 'prompt_tokens(prompt_tokens)',
    `completionTokens` INT DEFAULT 0 COMMENT 'completion_tokens(completion_tokens)',
    `latencyMs` INT DEFAULT 0 COMMENT 'latency_ms(latency_ms)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'error_msg(error_msg)',
    `score` INT DEFAULT 0 COMMENT 'score(score)',
    `judgeModel` VARCHAR(255) DEFAULT NULL COMMENT 'judge_model(judge_model)',
    `judgeReason` VARCHAR(255) DEFAULT NULL COMMENT 'judge_reason(judge_reason)',
    `createdAt` DATETIME DEFAULT NULL COMMENT 'created_at(created_at)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='model_battle_log (auto-generated V3.5.5)';

-- model/ModelConfig.java
DROP TABLE IF EXISTS `model_config`;
CREATE TABLE IF NOT EXISTS `model_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `providerId` BIGINT DEFAULT 0 COMMENT 'providerId(providerId)',
    `modelCode` VARCHAR(255) DEFAULT NULL COMMENT 'modelCode(modelCode)',
    `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName(displayName)',
    `maxContext` INT DEFAULT 0 COMMENT 'maxContext(maxContext)',
    `maxOutput` INT DEFAULT 0 COMMENT 'maxOutput(maxOutput)',
    `inputPrice` DECIMAL(20,4) DEFAULT 0 COMMENT 'inputPrice(inputPrice)',
    `outputPrice` DECIMAL(20,4) DEFAULT 0 COMMENT 'outputPrice(outputPrice)',
    `supportsVision` INT DEFAULT 0 COMMENT 'supportsVision(supportsVision)',
    `supportsTools` INT DEFAULT 0 COMMENT 'supportsTools(supportsTools)',
    `supportsStream` INT DEFAULT 0 COMMENT 'supportsStream(supportsStream)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `sort` INT DEFAULT 0 COMMENT '排序号(sort)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='model_config (auto-generated V3.5.5)';

-- model/ModelProvider.java
DROP TABLE IF EXISTS `model_provider`;
CREATE TABLE IF NOT EXISTS `model_provider` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `code` VARCHAR(255) DEFAULT NULL COMMENT '编码(code)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `baseUrl` VARCHAR(255) DEFAULT NULL COMMENT 'baseUrl(baseUrl)',
    `apiKey` VARCHAR(255) DEFAULT NULL COMMENT 'apiKey(apiKey)',
    `protocol` VARCHAR(255) DEFAULT NULL COMMENT 'protocol(protocol)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `sort` INT DEFAULT 0 COMMENT '排序号(sort)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='model_provider (auto-generated V3.5.5)';

-- model/ModelQuota.java
DROP TABLE IF EXISTS `model_quota`;
CREATE TABLE IF NOT EXISTS `model_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `modelId` BIGINT DEFAULT 0 COMMENT 'modelId(modelId)',
    `quotaDate` DATE DEFAULT NULL COMMENT 'quotaDate(quotaDate)',
    `usedTokens` BIGINT DEFAULT 0 COMMENT 'usedTokens(usedTokens)',
    `usedRequests` INT DEFAULT 0 COMMENT 'usedRequests(usedRequests)',
    `limitTokens` BIGINT DEFAULT 0 COMMENT 'limitTokens(limitTokens)',
    `limitRequests` INT DEFAULT 0 COMMENT 'limitRequests(limitRequests)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='model_quota (auto-generated V3.5.5)';

-- model/TrainingTask.java
DROP TABLE IF EXISTS `training_task`;
CREATE TABLE IF NOT EXISTS `training_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `modelName` VARCHAR(255) DEFAULT NULL COMMENT 'modelName(modelName)',
    `corpusPath` VARCHAR(255) DEFAULT NULL COMMENT 'corpusPath(corpusPath)',
    `nLayer` INT DEFAULT 0 COMMENT 'nLayer(nLayer)',
    `nHead` INT DEFAULT 0 COMMENT 'nHead(nHead)',
    `nEmbd` INT DEFAULT 0 COMMENT 'nEmbd(nEmbd)',
    `blockSize` INT DEFAULT 0 COMMENT 'blockSize(blockSize)',
    `maxIters` INT DEFAULT 0 COMMENT 'maxIters(maxIters)',
    `batchSize` INT DEFAULT 0 COMMENT 'batchSize(batchSize)',
    `learningRate` DOUBLE DEFAULT 0 COMMENT 'learningRate(learningRate)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `progress` INT DEFAULT 0 COMMENT 'progress(progress)',
    `currentLoss` DOUBLE DEFAULT 0 COMMENT 'currentLoss(currentLoss)',
    `currentIter` INT DEFAULT 0 COMMENT 'currentIter(currentIter)',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `completedAt` DATETIME DEFAULT NULL COMMENT 'completedAt(completedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='training_task (auto-generated V3.5.5)';


-- =========================================
-- 模块: rag (3 张表)
-- =========================================

-- rag/Document.java
DROP TABLE IF EXISTS `document`;
CREATE TABLE IF NOT EXISTS `document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `kbId` BIGINT DEFAULT 0 COMMENT 'kbId(kbId)',
    `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `sourceType` VARCHAR(255) DEFAULT NULL COMMENT 'sourceType(sourceType)',
    `sourceUri` VARCHAR(255) DEFAULT NULL COMMENT 'sourceUri(sourceUri)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `sizeBytes` BIGINT DEFAULT 0 COMMENT 'sizeBytes(sizeBytes)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)',
    `chunkCount` INT DEFAULT 0 COMMENT 'chunkCount(chunkCount)',
    `checksum` VARCHAR(255) DEFAULT NULL COMMENT 'checksum(checksum)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='document (auto-generated V3.5.5)';

-- rag/DocumentChunk.java
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE IF NOT EXISTS `document_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `docId` BIGINT DEFAULT 0 COMMENT 'docId(docId)',
    `kbId` BIGINT DEFAULT 0 COMMENT 'kbId(kbId)',
    `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `chunkIndex` INT DEFAULT 0 COMMENT 'chunkIndex(chunkIndex)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `embedding` BLOB DEFAULT NULL COMMENT 'embedding(embedding)',
    `dim` INT DEFAULT 0 COMMENT 'dim(dim)',
    `charCount` INT DEFAULT 0 COMMENT 'charCount(charCount)',
    `startPos` INT DEFAULT 0 COMMENT 'startPos(startPos)',
    `endPos` INT DEFAULT 0 COMMENT 'endPos(endPos)',
    `accessCount` INT DEFAULT 0 COMMENT 'accessCount(accessCount)',
    `lastAccessAt` DATETIME DEFAULT NULL COMMENT 'lastAccessAt(lastAccessAt)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='document_chunk (auto-generated V3.5.5)';

-- rag/KnowledgeBase.java
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `tenantId` BIGINT DEFAULT 0 COMMENT '租户ID(tenantId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `visibility` VARCHAR(255) DEFAULT NULL COMMENT 'visibility(visibility)',
    `docCount` INT DEFAULT 0 COMMENT 'docCount(docCount)',
    `chunkCount` INT DEFAULT 0 COMMENT 'chunkCount(chunkCount)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='knowledge_base (auto-generated V3.5.5)';


-- =========================================
-- 模块: function (2 张表)
-- =========================================

-- function/FunctionCallLog.java
DROP TABLE IF EXISTS `function_call_log`;
CREATE TABLE IF NOT EXISTS `function_call_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `sessionId` BIGINT DEFAULT 0 COMMENT 'sessionId(sessionId)',
    `toolName` VARCHAR(255) DEFAULT NULL COMMENT 'toolName(toolName)',
    `arguments` VARCHAR(255) DEFAULT NULL COMMENT 'arguments(arguments)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)',
    `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='function_call_log (auto-generated V3.5.5)';

-- function/FunctionTool.java
DROP TABLE IF EXISTS `function_tool`;
CREATE TABLE IF NOT EXISTS `function_tool` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `displayName` VARCHAR(255) DEFAULT NULL COMMENT 'displayName(displayName)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)',
    `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)',
    `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `parameters` VARCHAR(255) DEFAULT NULL COMMENT 'parameters(parameters)',
    `endpoint` VARCHAR(255) DEFAULT NULL COMMENT 'endpoint(endpoint)',
    `httpMethod` VARCHAR(255) DEFAULT NULL COMMENT 'httpMethod(httpMethod)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='function_tool (auto-generated V3.5.5)';


-- =========================================
-- 模块: chat (2 张表)
-- =========================================

-- chat/ChatMessage.java
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `sessionId` BIGINT DEFAULT 0 COMMENT 'sessionId(sessionId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `tokens` INT DEFAULT 0 COMMENT 'tokens(tokens)',
    `finishReason` VARCHAR(255) DEFAULT NULL COMMENT 'finishReason(finishReason)',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='chat_message (auto-generated V3.5.5)';

-- chat/ChatSession.java
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)',
    `systemPrompt` VARCHAR(255) DEFAULT NULL COMMENT 'systemPrompt(systemPrompt)',
    `temperature` DECIMAL(20,4) DEFAULT 0 COMMENT 'temperature(temperature)',
    `status` INT DEFAULT 0 COMMENT '状态(status)',
    `messageCount` INT DEFAULT 0 COMMENT 'messageCount(messageCount)',
    `lastMessageAt` DATETIME DEFAULT NULL COMMENT 'lastMessageAt(lastMessageAt)',
    `tenantId` BIGINT DEFAULT 0 COMMENT '租户ID(tenantId)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='chat_session (auto-generated V3.5.5)';


-- =========================================
-- 模块: monitor (4 张表)
-- =========================================

-- monitor/AlertChannel.java
DROP TABLE IF EXISTS `alert_channel`;
CREATE TABLE IF NOT EXISTS `alert_channel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `channelType` VARCHAR(255) DEFAULT NULL COMMENT 'channelType(channelType)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `target` VARCHAR(255) DEFAULT NULL COMMENT 'target(target)',
    `config` VARCHAR(255) DEFAULT NULL COMMENT 'config(config)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `priority` INT DEFAULT 0 COMMENT 'priority(priority)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `createdBy` BIGINT DEFAULT 0 COMMENT '创建人ID(createdBy)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='alert_channel (auto-generated V3.5.5)';

-- monitor/AlertEvent.java
DROP TABLE IF EXISTS `alert_event`;
CREATE TABLE IF NOT EXISTS `alert_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `ruleId` BIGINT DEFAULT 0 COMMENT 'ruleId(ruleId)',
    `ruleName` VARCHAR(255) DEFAULT NULL COMMENT 'ruleName(ruleName)',
    `severity` VARCHAR(255) DEFAULT NULL COMMENT 'severity(severity)',
    `metricName` VARCHAR(255) DEFAULT NULL COMMENT 'metricName(metricName)',
    `message` VARCHAR(255) DEFAULT NULL COMMENT 'message(message)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `firedAt` DATETIME DEFAULT NULL COMMENT 'firedAt(firedAt)',
    `resolvedAt` DATETIME DEFAULT NULL COMMENT 'resolvedAt(resolvedAt)',
    `ackedAt` DATETIME DEFAULT NULL COMMENT 'ackedAt(ackedAt)',
    `ackedBy` BIGINT DEFAULT 0 COMMENT 'ackedBy(ackedBy)',
    `duration` BIGINT DEFAULT 0 COMMENT 'duration(duration)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='alert_event (auto-generated V3.5.5)';

-- monitor/AlertRule.java
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE IF NOT EXISTS `alert_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `metricName` VARCHAR(255) DEFAULT NULL COMMENT 'metricName(metricName)',
    `service` VARCHAR(255) DEFAULT NULL COMMENT 'service(service)',
    `operator` VARCHAR(255) DEFAULT NULL COMMENT 'operator(operator)',
    `severity` VARCHAR(255) DEFAULT NULL COMMENT 'severity(severity)',
    `cooldownMinutes` INT DEFAULT 0 COMMENT 'cooldownMinutes(cooldownMinutes)',
    `enabled` INT DEFAULT 0 COMMENT '是否启用(enabled)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `notifyChannel` VARCHAR(255) DEFAULT NULL COMMENT 'notifyChannel(notifyChannel)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='alert_rule (auto-generated V3.5.5)';

-- monitor/MetricSnapshot.java
DROP TABLE IF EXISTS `metric_snapshot`;
CREATE TABLE IF NOT EXISTS `metric_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `service` VARCHAR(255) DEFAULT NULL COMMENT 'service(service)',
    `metricName` VARCHAR(255) DEFAULT NULL COMMENT 'metricName(metricName)',
    `metricValue` DECIMAL(20,4) DEFAULT 0 COMMENT 'metricValue(metricValue)',
    `tags` VARCHAR(255) DEFAULT NULL COMMENT 'tags(tags)',
    `recordedAt` DATETIME DEFAULT NULL COMMENT 'recordedAt(recordedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='metric_snapshot (auto-generated V3.5.5)';


-- =========================================
-- 模块: admin (2 张表)
-- =========================================

-- admin/AdminAuditLog.java
DROP TABLE IF EXISTS `admin_audit_log`;
CREATE TABLE IF NOT EXISTS `admin_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `actorId` BIGINT DEFAULT 0 COMMENT 'actorId(actorId)',
    `actorName` VARCHAR(255) DEFAULT NULL COMMENT 'actorName(actorName)',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)',
    `resourceType` VARCHAR(255) DEFAULT NULL COMMENT 'resourceType(resourceType)',
    `resourceId` VARCHAR(255) DEFAULT NULL COMMENT 'resourceId(resourceId)',
    `detail` VARCHAR(255) DEFAULT NULL COMMENT 'detail(detail)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)',
    `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='admin_audit_log (auto-generated V3.5.5)';

-- admin/AuditLogFull.java
DROP TABLE IF EXISTS `audit_log_full`;
CREATE TABLE IF NOT EXISTS `audit_log_full` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `traceId` VARCHAR(255) DEFAULT NULL COMMENT 'traceId(traceId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `userIp` VARCHAR(255) DEFAULT NULL COMMENT 'userIp(userIp)',
    `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)',
    `action` VARCHAR(255) DEFAULT NULL COMMENT 'action(action)',
    `resourceType` VARCHAR(255) DEFAULT NULL COMMENT 'resourceType(resourceType)',
    `resourceId` VARCHAR(255) DEFAULT NULL COMMENT 'resourceId(resourceId)',
    `method` VARCHAR(255) DEFAULT NULL COMMENT 'method(method)',
    `path` VARCHAR(255) DEFAULT NULL COMMENT 'path(path)',
    `requestBody` VARCHAR(255) DEFAULT NULL COMMENT 'requestBody(requestBody)',
    `responseStatus` INT DEFAULT 0 COMMENT 'responseStatus(responseStatus)',
    `result` VARCHAR(255) DEFAULT NULL COMMENT 'result(result)',
    `errorMsg` VARCHAR(255) DEFAULT NULL COMMENT 'errorMsg(errorMsg)',
    `durationMs` INT DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='audit_log_full (auto-generated V3.5.5)';


-- =========================================
-- 模块: prompt (1 张表)
-- =========================================

-- prompt/PromptTemplate.java
DROP TABLE IF EXISTS `prompt_template`;
CREATE TABLE IF NOT EXISTS `prompt_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `category` VARCHAR(255) DEFAULT NULL COMMENT 'category(category)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `variables` VARCHAR(255) DEFAULT NULL COMMENT 'variables(variables)',
    `creatorId` BIGINT DEFAULT 0 COMMENT 'creatorId(creatorId)',
    `creatorName` VARCHAR(255) DEFAULT NULL COMMENT 'creatorName(creatorName)',
    `isPublic` TINYINT(1) DEFAULT 0 COMMENT 'isPublic(isPublic)',
    `useCount` INT DEFAULT 0 COMMENT 'useCount(useCount)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='prompt_template (auto-generated V3.5.5)';


-- =========================================
-- 模块: analytics (4 张表)
-- =========================================

-- analytics/DataSource.java
DROP TABLE IF EXISTS `analytics_datasource`;
CREATE TABLE IF NOT EXISTS `analytics_datasource` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `jdbcUrl` VARCHAR(255) DEFAULT NULL COMMENT 'jdbcUrl(jdbcUrl)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `passwordEnc` VARCHAR(255) DEFAULT NULL COMMENT 'passwordEnc(passwordEnc)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `updatedAt` DATETIME DEFAULT NULL COMMENT '更新时间(updatedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='analytics_datasource (auto-generated V3.5.5)';

-- analytics/IngestTask.java
DROP TABLE IF EXISTS `analytics_ingest_task`;
CREATE TABLE IF NOT EXISTS `analytics_ingest_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `taskId` VARCHAR(255) DEFAULT NULL COMMENT 'taskId(taskId)',
    `filename` VARCHAR(255) DEFAULT NULL COMMENT 'filename(filename)',
    `fileType` VARCHAR(255) DEFAULT NULL COMMENT 'fileType(fileType)',
    `encoding` VARCHAR(255) DEFAULT NULL COMMENT 'encoding(encoding)',
    `separator` VARCHAR(255) DEFAULT NULL COMMENT 'separator(separator)',
    `fileSize` BIGINT DEFAULT 0 COMMENT 'fileSize(fileSize)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)',
    `qualityJson` VARCHAR(255) DEFAULT NULL COMMENT 'qualityJson(qualityJson)',
    `totalRows` BIGINT DEFAULT 0 COMMENT 'totalRows(totalRows)',
    `totalColumns` BIGINT DEFAULT 0 COMMENT 'totalColumns(totalColumns)',
    `columnsJson` VARCHAR(255) DEFAULT NULL COMMENT 'columnsJson(columnsJson)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `finishedAt` DATETIME DEFAULT NULL COMMENT 'finishedAt(finishedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='analytics_ingest_task (auto-generated V3.5.5)';

-- analytics/Nl2SqlHistory.java
DROP TABLE IF EXISTS `analytics_nlsql_history`;
CREATE TABLE IF NOT EXISTS `analytics_nlsql_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `dataSourceId` BIGINT DEFAULT 0 COMMENT 'dataSourceId(dataSourceId)',
    `question` VARCHAR(255) DEFAULT NULL COMMENT 'question(question)',
    `generatedSql` VARCHAR(255) DEFAULT NULL COMMENT 'generatedSql(generatedSql)',
    `correctedSql` VARCHAR(255) DEFAULT NULL COMMENT 'correctedSql(correctedSql)',
    `model` VARCHAR(255) DEFAULT NULL COMMENT 'model(model)',
    `promptTokens` INT DEFAULT 0 COMMENT 'promptTokens(promptTokens)',
    `completionTokens` INT DEFAULT 0 COMMENT 'completionTokens(completionTokens)',
    `durationMs` BIGINT DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `success` TINYINT(1) DEFAULT 0 COMMENT 'success(success)',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)',
    `feedbackRating` INT DEFAULT 0 COMMENT 'feedbackRating(feedbackRating)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='analytics_nlsql_history (auto-generated V3.5.5)';

-- analytics/Report.java
DROP TABLE IF EXISTS `analytics_report`;
CREATE TABLE IF NOT EXISTS `analytics_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `reportId` VARCHAR(255) DEFAULT NULL COMMENT 'reportId(reportId)',
    `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)',
    `question` VARCHAR(255) DEFAULT NULL COMMENT 'question(question)',
    `sqlText` VARCHAR(255) DEFAULT NULL COMMENT 'sqlText(sqlText)',
    `markdown` VARCHAR(255) DEFAULT NULL COMMENT 'markdown(markdown)',
    `chartOptionsJson` VARCHAR(255) DEFAULT NULL COMMENT 'chartOptionsJson(chartOptionsJson)',
    `rowCount` BIGINT DEFAULT 0 COMMENT 'rowCount(rowCount)',
    `durationMs` BIGINT DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `format` VARCHAR(255) DEFAULT NULL COMMENT 'format(format)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='analytics_report (auto-generated V3.5.5)';


-- =========================================
-- 模块: pipeline (4 张表)
-- =========================================

-- pipeline/PipelineNodeLog.java
DROP TABLE IF EXISTS `pipeline_node_log`;
CREATE TABLE IF NOT EXISTS `pipeline_node_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `runId` BIGINT DEFAULT 0 COMMENT 'runId(runId)',
    `nodeId` VARCHAR(255) DEFAULT NULL COMMENT 'nodeId(nodeId)',
    `nodeType` VARCHAR(255) DEFAULT NULL COMMENT 'nodeType(nodeType)',
    `nodeName` VARCHAR(255) DEFAULT NULL COMMENT 'nodeName(nodeName)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `startTime` DATETIME DEFAULT NULL COMMENT 'startTime(startTime)',
    `endTime` DATETIME DEFAULT NULL COMMENT 'endTime(endTime)',
    `durationMs` BIGINT DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `inputRows` INT DEFAULT 0 COMMENT 'inputRows(inputRows)',
    `outputRows` INT DEFAULT 0 COMMENT 'outputRows(outputRows)',
    `outputPreview` VARCHAR(255) DEFAULT NULL COMMENT 'outputPreview(outputPreview)',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)',
    `configSnapshot` VARCHAR(255) DEFAULT NULL COMMENT 'configSnapshot(configSnapshot)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='pipeline_node_log (auto-generated V3.5.5)';

-- pipeline/PipelineRun.java
DROP TABLE IF EXISTS `pipeline_run`;
CREATE TABLE IF NOT EXISTS `pipeline_run` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `workflowId` BIGINT DEFAULT 0 COMMENT 'workflowId(workflowId)',
    `workflowName` VARCHAR(255) DEFAULT NULL COMMENT 'workflowName(workflowName)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `triggerBy` BIGINT DEFAULT 0 COMMENT 'triggerBy(triggerBy)',
    `triggerType` VARCHAR(255) DEFAULT NULL COMMENT 'triggerType(triggerType)',
    `definitionSnapshot` VARCHAR(255) DEFAULT NULL COMMENT 'definitionSnapshot(definitionSnapshot)',
    `startTime` DATETIME DEFAULT NULL COMMENT 'startTime(startTime)',
    `endTime` DATETIME DEFAULT NULL COMMENT 'endTime(endTime)',
    `durationMs` BIGINT DEFAULT 0 COMMENT 'durationMs(durationMs)',
    `errorMessage` VARCHAR(255) DEFAULT NULL COMMENT 'errorMessage(errorMessage)',
    `resultSummary` VARCHAR(255) DEFAULT NULL COMMENT 'resultSummary(resultSummary)',
    `createTime` DATETIME DEFAULT NULL COMMENT 'createTime(createTime)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='pipeline_run (auto-generated V3.5.5)';

-- pipeline/PipelineWorkflow.java
DROP TABLE IF EXISTS `pipeline_workflow`;
CREATE TABLE IF NOT EXISTS `pipeline_workflow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `definition` VARCHAR(255) DEFAULT NULL COMMENT 'definition(definition)',
    `version` INT DEFAULT 0 COMMENT '乐观锁版本号(version)',
    `status` INT DEFAULT 0 COMMENT '状态(status)',
    `createBy` BIGINT DEFAULT 0 COMMENT 'createBy(createBy)',
    `updateBy` BIGINT DEFAULT 0 COMMENT 'updateBy(updateBy)',
    `createTime` DATETIME DEFAULT NULL COMMENT 'createTime(createTime)',
    `updateTime` DATETIME DEFAULT NULL COMMENT 'updateTime(updateTime)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除标记(deleted)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='pipeline_workflow (auto-generated V3.5.5)';

-- pipeline/PipelineWorkflowVersion.java
DROP TABLE IF EXISTS `pipeline_workflow_version`;
CREATE TABLE IF NOT EXISTS `pipeline_workflow_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `workflowId` BIGINT DEFAULT 0 COMMENT 'workflowId(workflowId)',
    `version` INT DEFAULT 0 COMMENT '乐观锁版本号(version)',
    `definition` VARCHAR(255) DEFAULT NULL COMMENT 'definition(definition)',
    `changeLog` VARCHAR(255) DEFAULT NULL COMMENT 'changeLog(changeLog)',
    `createBy` BIGINT DEFAULT 0 COMMENT 'createBy(createBy)',
    `createTime` DATETIME DEFAULT NULL COMMENT 'createTime(createTime)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='pipeline_workflow_version (auto-generated V3.5.5)';


-- =========================================
-- 模块: ws (3 张表)
-- =========================================

-- ws/CollabMessage.java
DROP TABLE IF EXISTS `collab_message`;
CREATE TABLE IF NOT EXISTS `collab_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `roomId` VARCHAR(255) DEFAULT NULL COMMENT 'roomId(roomId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)',
    `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata(metadata)',
    `clientMsgId` VARCHAR(255) DEFAULT NULL COMMENT 'clientMsgId(clientMsgId)',
    `broadcast` INT DEFAULT 0 COMMENT 'broadcast(broadcast)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='collab_message (auto-generated V3.5.5)';

-- ws/CollabParticipant.java
DROP TABLE IF EXISTS `collab_participant`;
CREATE TABLE IF NOT EXISTS `collab_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `roomId` VARCHAR(255) DEFAULT NULL COMMENT 'roomId(roomId)',
    `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)',
    `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)',
    `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)',
    `cursorX` INT DEFAULT 0 COMMENT 'cursorX(cursorX)',
    `cursorY` INT DEFAULT 0 COMMENT 'cursorY(cursorY)',
    `selectionId` VARCHAR(255) DEFAULT NULL COMMENT 'selectionId(selectionId)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `joinedAt` DATETIME DEFAULT NULL COMMENT 'joinedAt(joinedAt)',
    `leftAt` DATETIME DEFAULT NULL COMMENT 'leftAt(leftAt)',
    `lastHeartbeat` DATETIME DEFAULT NULL COMMENT 'lastHeartbeat(lastHeartbeat)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='collab_participant (auto-generated V3.5.5)';

-- ws/CollabRoom.java
DROP TABLE IF EXISTS `collab_room`;
CREATE TABLE IF NOT EXISTS `collab_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID(id)',
    `roomId` VARCHAR(255) DEFAULT NULL COMMENT 'roomId(roomId)',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称(name)',
    `type` VARCHAR(255) DEFAULT NULL COMMENT '类型(type)',
    `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)',
    `ownerName` VARCHAR(255) DEFAULT NULL COMMENT 'ownerName(ownerName)',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述(description)',
    `isPublic` INT DEFAULT 0 COMMENT 'isPublic(isPublic)',
    `maxParticipants` INT DEFAULT 0 COMMENT 'maxParticipants(maxParticipants)',
    `status` VARCHAR(255) DEFAULT NULL COMMENT '状态(status)',
    `currentParticipants` INT DEFAULT 0 COMMENT 'currentParticipants(currentParticipants)',
    `createdAt` DATETIME DEFAULT NULL COMMENT '创建时间(createdAt)',
    `lastActivityAt` DATETIME DEFAULT NULL COMMENT 'lastActivityAt(lastActivityAt)',
    `closedAt` DATETIME DEFAULT NULL COMMENT 'closedAt(closedAt)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='collab_room (auto-generated V3.5.5)';

SET FOREIGN_KEY_CHECKS = 1;

-- 完成: 共 77 张表