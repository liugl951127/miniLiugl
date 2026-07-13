-- =============================================================
-- MiniMax Platform V3.5.5+ 修复 SQL - auth 模块
-- 共 13 张表, 113 字段
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_auth.sql
-- 自动生成: scripts/split_fix_sql_by_module.py
-- =============================================================

-- 表: auth_login_log
ALTER TABLE `auth_login_log` ADD COLUMN `ip` VARCHAR(255) DEFAULT NULL COMMENT 'ip(ip)';
ALTER TABLE `auth_login_log` ADD COLUMN `message` VARCHAR(255) DEFAULT NULL COMMENT 'message(message)';
ALTER TABLE `auth_login_log` ADD COLUMN `status` INT DEFAULT 0 COMMENT 'status(status)';
ALTER TABLE `auth_login_log` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';

-- 表: auth_refresh_token
ALTER TABLE `auth_refresh_token` ADD COLUMN `expiresAt` DATETIME DEFAULT NULL COMMENT 'expiresAt(expiresAt)';
ALTER TABLE `auth_refresh_token` ADD COLUMN `revoked` INT DEFAULT 0 COMMENT 'revoked(revoked)';

-- 表: notification
ALTER TABLE `notification` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';
ALTER TABLE `notification` ADD COLUMN `isRead` INT DEFAULT 0 COMMENT 'isRead(isRead)';
ALTER TABLE `notification` ADD COLUMN `title` VARCHAR(255) DEFAULT NULL COMMENT 'title(title)';

-- 表: oauth_app_config
ALTER TABLE `oauth_app_config` ADD COLUMN `appId` VARCHAR(255) DEFAULT NULL COMMENT 'appId(appId)';
ALTER TABLE `oauth_app_config` ADD COLUMN `appSecret` VARCHAR(255) DEFAULT NULL COMMENT 'appSecret(appSecret)';
ALTER TABLE `oauth_app_config` ADD COLUMN `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)';
ALTER TABLE `oauth_app_config` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `oauth_app_config` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `oauth_app_config` ADD COLUMN `extraConfig` VARCHAR(255) DEFAULT NULL COMMENT 'extraConfig(extraConfig)';
ALTER TABLE `oauth_app_config` ADD COLUMN `publicKey` VARCHAR(255) DEFAULT NULL COMMENT 'publicKey(publicKey)';
ALTER TABLE `oauth_app_config` ADD COLUMN `redirectUri` VARCHAR(255) DEFAULT NULL COMMENT 'redirectUri(redirectUri)';
ALTER TABLE `oauth_app_config` ADD COLUMN `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes(scopes)';
ALTER TABLE `oauth_app_config` ADD COLUMN `updatedAt` DATETIME DEFAULT NULL COMMENT 'updatedAt(updatedAt)';

-- 表: oauth_binding
ALTER TABLE `oauth_binding` ADD COLUMN `accessToken` VARCHAR(255) DEFAULT NULL COMMENT 'accessToken(accessToken)';
ALTER TABLE `oauth_binding` ADD COLUMN `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)';
ALTER TABLE `oauth_binding` ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)';
ALTER TABLE `oauth_binding` ADD COLUMN `boundAt` DATETIME DEFAULT NULL COMMENT 'boundAt(boundAt)';
ALTER TABLE `oauth_binding` ADD COLUMN `lastLoginAt` DATETIME DEFAULT NULL COMMENT 'lastLoginAt(lastLoginAt)';
ALTER TABLE `oauth_binding` ADD COLUMN `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)';
ALTER TABLE `oauth_binding` ADD COLUMN `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid(openid)';
ALTER TABLE `oauth_binding` ADD COLUMN `rawData` VARCHAR(255) DEFAULT NULL COMMENT 'rawData(rawData)';
ALTER TABLE `oauth_binding` ADD COLUMN `refreshToken` VARCHAR(255) DEFAULT NULL COMMENT 'refreshToken(refreshToken)';
ALTER TABLE `oauth_binding` ADD COLUMN `tokenExpiresAt` DATETIME DEFAULT NULL COMMENT 'tokenExpiresAt(tokenExpiresAt)';
ALTER TABLE `oauth_binding` ADD COLUMN `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)';

-- 表: sys_role
ALTER TABLE `sys_role` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `sys_role` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `sys_role` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `sys_role` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `sys_role` ADD COLUMN `sort` INT DEFAULT 0 COMMENT 'sort(sort)';

-- 表: sys_user
ALTER TABLE `sys_user` ADD COLUMN `alipayAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'alipayAvatar(alipayAvatar)';
ALTER TABLE `sys_user` ADD COLUMN `alipayBoundAt` DATETIME DEFAULT NULL COMMENT 'alipayBoundAt(alipayBoundAt)';
ALTER TABLE `sys_user` ADD COLUMN `alipayNickname` VARCHAR(255) DEFAULT NULL COMMENT 'alipayNickname(alipayNickname)';
ALTER TABLE `sys_user` ADD COLUMN `alipayOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'alipayOpenid(alipayOpenid)';
ALTER TABLE `sys_user` ADD COLUMN `alipayUserId` VARCHAR(255) DEFAULT NULL COMMENT 'alipayUserId(alipayUserId)';
ALTER TABLE `sys_user` ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)';
ALTER TABLE `sys_user` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `sys_user` ADD COLUMN `email` VARCHAR(255) DEFAULT NULL COMMENT 'email(email)';
ALTER TABLE `sys_user` ADD COLUMN `gender` INT DEFAULT 0 COMMENT 'gender(gender)';
ALTER TABLE `sys_user` ADD COLUMN `lastLoginAt` DATETIME DEFAULT NULL COMMENT 'lastLoginAt(lastLoginAt)';
ALTER TABLE `sys_user` ADD COLUMN `lastLoginIp` VARCHAR(255) DEFAULT NULL COMMENT 'lastLoginIp(lastLoginIp)';
ALTER TABLE `sys_user` ADD COLUMN `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)';
ALTER TABLE `sys_user` ADD COLUMN `password` VARCHAR(255) DEFAULT NULL COMMENT 'password(password)';
ALTER TABLE `sys_user` ADD COLUMN `phone` VARCHAR(255) DEFAULT NULL COMMENT 'phone(phone)';
ALTER TABLE `sys_user` ADD COLUMN `qqAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'qqAvatar(qqAvatar)';
ALTER TABLE `sys_user` ADD COLUMN `qqBoundAt` DATETIME DEFAULT NULL COMMENT 'qqBoundAt(qqBoundAt)';
ALTER TABLE `sys_user` ADD COLUMN `qqNickname` VARCHAR(255) DEFAULT NULL COMMENT 'qqNickname(qqNickname)';
ALTER TABLE `sys_user` ADD COLUMN `qqOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'qqOpenid(qqOpenid)';
ALTER TABLE `sys_user` ADD COLUMN `qqUnionid` VARCHAR(255) DEFAULT NULL COMMENT 'qqUnionid(qqUnionid)';
ALTER TABLE `sys_user` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark(remark)';
ALTER TABLE `sys_user` ADD COLUMN `status` INT DEFAULT 0 COMMENT 'status(status)';
ALTER TABLE `sys_user` ADD COLUMN `tenantId` BIGINT DEFAULT 0 COMMENT 'tenantId(tenantId)';
ALTER TABLE `sys_user` ADD COLUMN `wechatAvatar` VARCHAR(255) DEFAULT NULL COMMENT 'wechatAvatar(wechatAvatar)';
ALTER TABLE `sys_user` ADD COLUMN `wechatBoundAt` DATETIME DEFAULT NULL COMMENT 'wechatBoundAt(wechatBoundAt)';
ALTER TABLE `sys_user` ADD COLUMN `wechatNickname` VARCHAR(255) DEFAULT NULL COMMENT 'wechatNickname(wechatNickname)';
ALTER TABLE `sys_user` ADD COLUMN `wechatOpenid` VARCHAR(255) DEFAULT NULL COMMENT 'wechatOpenid(wechatOpenid)';
ALTER TABLE `sys_user` ADD COLUMN `wechatUnionid` VARCHAR(255) DEFAULT NULL COMMENT 'wechatUnionid(wechatUnionid)';

-- 表: tenant
ALTER TABLE `tenant` ADD COLUMN `contactEmail` VARCHAR(255) DEFAULT NULL COMMENT 'contactEmail(contactEmail)';
ALTER TABLE `tenant` ADD COLUMN `contactPhone` VARCHAR(255) DEFAULT NULL COMMENT 'contactPhone(contactPhone)';
ALTER TABLE `tenant` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `tenant` ADD COLUMN `expireAt` DATETIME DEFAULT NULL COMMENT 'expireAt(expireAt)';
ALTER TABLE `tenant` ADD COLUMN `maxModels` INT DEFAULT 0 COMMENT 'maxModels(maxModels)';
ALTER TABLE `tenant` ADD COLUMN `maxUsers` INT DEFAULT 0 COMMENT 'maxUsers(maxUsers)';
ALTER TABLE `tenant` ADD COLUMN `monthlyQuota` BIGINT DEFAULT 0 COMMENT 'monthlyQuota(monthlyQuota)';
ALTER TABLE `tenant` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `tenant` ADD COLUMN `plan` VARCHAR(255) DEFAULT NULL COMMENT 'plan(plan)';
ALTER TABLE `tenant` ADD COLUMN `qpsLimit` INT DEFAULT 0 COMMENT 'qpsLimit(qpsLimit)';
ALTER TABLE `tenant` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark(remark)';
ALTER TABLE `tenant` ADD COLUMN `status` INT DEFAULT 0 COMMENT 'status(status)';
ALTER TABLE `tenant` ADD COLUMN `usedQuota` BIGINT DEFAULT 0 COMMENT 'usedQuota(usedQuota)';

-- 表: unionid_relations
ALTER TABLE `unionid_relations` ADD COLUMN `bindingCount` INT DEFAULT 0 COMMENT 'bindingCount(bindingCount)';
ALTER TABLE `unionid_relations` ADD COLUMN `firstSeenAt` DATETIME DEFAULT NULL COMMENT 'firstSeenAt(firstSeenAt)';
ALTER TABLE `unionid_relations` ADD COLUMN `lastSeenAt` DATETIME DEFAULT NULL COMMENT 'lastSeenAt(lastSeenAt)';
ALTER TABLE `unionid_relations` ADD COLUMN `platform` VARCHAR(255) DEFAULT NULL COMMENT 'platform(platform)';

-- 表: user_api_key
ALTER TABLE `user_api_key` ADD COLUMN `deleted` INT DEFAULT 0 COMMENT 'deleted(deleted)';
ALTER TABLE `user_api_key` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `user_api_key` ADD COLUMN `expiresAt` DATETIME DEFAULT NULL COMMENT 'expiresAt(expiresAt)';
ALTER TABLE `user_api_key` ADD COLUMN `keyHash` VARCHAR(255) DEFAULT NULL COMMENT 'keyHash(keyHash)';
ALTER TABLE `user_api_key` ADD COLUMN `keyPrefix` VARCHAR(255) DEFAULT NULL COMMENT 'keyPrefix(keyPrefix)';
ALTER TABLE `user_api_key` ADD COLUMN `lastUsedAt` DATETIME DEFAULT NULL COMMENT 'lastUsedAt(lastUsedAt)';
ALTER TABLE `user_api_key` ADD COLUMN `scopes` VARCHAR(255) DEFAULT NULL COMMENT 'scopes(scopes)';
ALTER TABLE `user_api_key` ADD COLUMN `useCount` BIGINT DEFAULT 0 COMMENT 'useCount(useCount)';

-- 表: wechat_config
ALTER TABLE `wechat_config` ADD COLUMN `aesKey` VARCHAR(255) DEFAULT NULL COMMENT 'aesKey(aesKey)';
ALTER TABLE `wechat_config` ADD COLUMN `appId` VARCHAR(255) DEFAULT NULL COMMENT 'appId(appId)';
ALTER TABLE `wechat_config` ADD COLUMN `appSecret` VARCHAR(255) DEFAULT NULL COMMENT 'appSecret(appSecret)';
ALTER TABLE `wechat_config` ADD COLUMN `enabled` INT DEFAULT 0 COMMENT 'enabled(enabled)';
ALTER TABLE `wechat_config` ADD COLUMN `redirectUri` VARCHAR(255) DEFAULT NULL COMMENT 'redirectUri(redirectUri)';
ALTER TABLE `wechat_config` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark(remark)';
ALTER TABLE `wechat_config` ADD COLUMN `scope` VARCHAR(255) DEFAULT NULL COMMENT 'scope(scope)';
ALTER TABLE `wechat_config` ADD COLUMN `token` VARCHAR(255) DEFAULT NULL COMMENT 'token(token)';

-- 表: wechat_scan_session
ALTER TABLE `wechat_scan_session` ADD COLUMN `accessToken` VARCHAR(255) DEFAULT NULL COMMENT 'accessToken(accessToken)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `clientIp` VARCHAR(255) DEFAULT NULL COMMENT 'clientIp(clientIp)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `confirmedAt` DATETIME DEFAULT NULL COMMENT 'confirmedAt(confirmedAt)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `expiresAt` DATETIME DEFAULT NULL COMMENT 'expiresAt(expiresAt)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `openid` VARCHAR(255) DEFAULT NULL COMMENT 'openid(openid)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `refreshToken` VARCHAR(255) DEFAULT NULL COMMENT 'refreshToken(refreshToken)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `sceneId` VARCHAR(255) DEFAULT NULL COMMENT 'sceneId(sceneId)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `userAgent` VARCHAR(255) DEFAULT NULL COMMENT 'userAgent(userAgent)';
ALTER TABLE `wechat_scan_session` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';

-- 表: wechat_user_binding
ALTER TABLE `wechat_user_binding` ADD COLUMN `appType` VARCHAR(255) DEFAULT NULL COMMENT 'appType(appType)';
ALTER TABLE `wechat_user_binding` ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)';
ALTER TABLE `wechat_user_binding` ADD COLUMN `lastLoginAt` DATETIME DEFAULT NULL COMMENT 'lastLoginAt(lastLoginAt)';
ALTER TABLE `wechat_user_binding` ADD COLUMN `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)';
ALTER TABLE `wechat_user_binding` ADD COLUMN `unionid` VARCHAR(255) DEFAULT NULL COMMENT 'unionid(unionid)';

