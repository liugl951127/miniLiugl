-- =============================================================
-- MiniMax Platform V3.5.5+ 修复 SQL - ws 模块
-- 共 3 张表, 33 字段
-- 用法: mysql -uroot -proot123456 minimax_platform < fix_ws.sql
-- 自动生成: scripts/split_fix_sql_by_module.py
-- =============================================================

-- 表: collab_message
ALTER TABLE `collab_message` ADD COLUMN `broadcast` INT DEFAULT 0 COMMENT 'broadcast(broadcast)';
ALTER TABLE `collab_message` ADD COLUMN `clientMsgId` VARCHAR(255) DEFAULT NULL COMMENT 'clientMsgId(clientMsgId)';
ALTER TABLE `collab_message` ADD COLUMN `content` VARCHAR(255) DEFAULT NULL COMMENT 'content(content)';
ALTER TABLE `collab_message` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `collab_message` ADD COLUMN `metadata` VARCHAR(255) DEFAULT NULL COMMENT 'metadata(metadata)';
ALTER TABLE `collab_message` ADD COLUMN `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)';
ALTER TABLE `collab_message` ADD COLUMN `type` VARCHAR(255) DEFAULT NULL COMMENT 'type(type)';
ALTER TABLE `collab_message` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `collab_message` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';

-- 表: collab_participant
ALTER TABLE `collab_participant` ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT 'avatar(avatar)';
ALTER TABLE `collab_participant` ADD COLUMN `cursorX` INT DEFAULT 0 COMMENT 'cursorX(cursorX)';
ALTER TABLE `collab_participant` ADD COLUMN `cursorY` INT DEFAULT 0 COMMENT 'cursorY(cursorY)';
ALTER TABLE `collab_participant` ADD COLUMN `joinedAt` DATETIME DEFAULT NULL COMMENT 'joinedAt(joinedAt)';
ALTER TABLE `collab_participant` ADD COLUMN `lastHeartbeat` DATETIME DEFAULT NULL COMMENT 'lastHeartbeat(lastHeartbeat)';
ALTER TABLE `collab_participant` ADD COLUMN `leftAt` DATETIME DEFAULT NULL COMMENT 'leftAt(leftAt)';
ALTER TABLE `collab_participant` ADD COLUMN `nickname` VARCHAR(255) DEFAULT NULL COMMENT 'nickname(nickname)';
ALTER TABLE `collab_participant` ADD COLUMN `role` VARCHAR(255) DEFAULT NULL COMMENT 'role(role)';
ALTER TABLE `collab_participant` ADD COLUMN `selectionId` VARCHAR(255) DEFAULT NULL COMMENT 'selectionId(selectionId)';
ALTER TABLE `collab_participant` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `collab_participant` ADD COLUMN `userId` BIGINT DEFAULT 0 COMMENT 'userId(userId)';
ALTER TABLE `collab_participant` ADD COLUMN `username` VARCHAR(255) DEFAULT NULL COMMENT 'username(username)';

-- 表: collab_room
ALTER TABLE `collab_room` ADD COLUMN `closedAt` DATETIME DEFAULT NULL COMMENT 'closedAt(closedAt)';
ALTER TABLE `collab_room` ADD COLUMN `createdAt` DATETIME DEFAULT NULL COMMENT 'createdAt(createdAt)';
ALTER TABLE `collab_room` ADD COLUMN `currentParticipants` INT DEFAULT 0 COMMENT 'currentParticipants(currentParticipants)';
ALTER TABLE `collab_room` ADD COLUMN `description` VARCHAR(255) DEFAULT NULL COMMENT 'description(description)';
ALTER TABLE `collab_room` ADD COLUMN `isPublic` INT DEFAULT 0 COMMENT 'isPublic(isPublic)';
ALTER TABLE `collab_room` ADD COLUMN `lastActivityAt` DATETIME DEFAULT NULL COMMENT 'lastActivityAt(lastActivityAt)';
ALTER TABLE `collab_room` ADD COLUMN `maxParticipants` INT DEFAULT 0 COMMENT 'maxParticipants(maxParticipants)';
ALTER TABLE `collab_room` ADD COLUMN `name` VARCHAR(255) DEFAULT NULL COMMENT 'name(name)';
ALTER TABLE `collab_room` ADD COLUMN `ownerId` BIGINT DEFAULT 0 COMMENT 'ownerId(ownerId)';
ALTER TABLE `collab_room` ADD COLUMN `ownerName` VARCHAR(255) DEFAULT NULL COMMENT 'ownerName(ownerName)';
ALTER TABLE `collab_room` ADD COLUMN `status` VARCHAR(255) DEFAULT NULL COMMENT 'status(status)';
ALTER TABLE `collab_room` ADD COLUMN `type` VARCHAR(255) DEFAULT NULL COMMENT 'type(type)';

