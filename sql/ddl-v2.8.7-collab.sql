-- ===========================================================
-- V2.8.7 实时协作 DDL
-- ===========================================================
-- 包含 3 张表: collab_room / collab_participant / collab_message
-- 适用: minimax-ws 模块 (端口 8095)
-- ===========================================================

-- 协作房间表
CREATE TABLE IF NOT EXISTS `collab_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `roomId` VARCHAR(32) NOT NULL COMMENT '房间唯一标识 (8位短码)',
    `name` VARCHAR(128) DEFAULT '未命名房间' COMMENT '房间名称',
    `type` VARCHAR(32) NOT NULL DEFAULT 'AI_CHAT' COMMENT '类型: AI_CHAT/DOC/TRAINING/DASHBOARD/CODE',
    `ownerId` BIGINT NOT NULL DEFAULT 0 COMMENT '创建者用户ID',
    `ownerName` VARCHAR(64) DEFAULT NULL COMMENT '创建者用户名',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '房间描述',
    `isPublic` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开',
    `maxParticipants` INT(11) NOT NULL DEFAULT 50 COMMENT '最大参与人数',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/CLOSED',
    `currentParticipants` INT(11) NOT NULL DEFAULT 0 COMMENT '当前参与人数',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `lastActivityAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `closedAt` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_roomId` (`roomId`),
    KEY `idx_owner` (`ownerId`),
    KEY `idx_status` (`status`, `lastActivityAt`),
    KEY `idx_public` (`isPublic`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作房间表 V2.8.7';

-- 协作参与者表
CREATE TABLE IF NOT EXISTS `collab_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `roomId` VARCHAR(32) NOT NULL COMMENT '房间ID',
    `userId` BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID',
    `username` VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `role` VARCHAR(16) NOT NULL DEFAULT 'EDITOR' COMMENT '角色: OWNER/EDITOR/VIEWER',
    `cursorX` INT(11) DEFAULT NULL COMMENT '光标X',
    `cursorY` INT(11) DEFAULT NULL COMMENT '光标Y',
    `selectionId` VARCHAR(64) DEFAULT NULL COMMENT '当前选中元素ID',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ONLINE' COMMENT '状态: ONLINE/AWAY/OFFLINE',
    `joinedAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `leftAt` DATETIME DEFAULT NULL COMMENT '离开时间(null=在线)',
    `lastHeartbeat` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_user` (`roomId`, `userId`, `leftAt`),
    KEY `idx_user` (`userId`),
    KEY `idx_status` (`roomId`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作参与者表 V2.8.7';

-- 协作消息表
CREATE TABLE IF NOT EXISTS `collab_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `roomId` VARCHAR(32) NOT NULL COMMENT '房间ID',
    `userId` BIGINT DEFAULT NULL COMMENT '发送者用户ID(SYSTEM为NULL)',
    `username` VARCHAR(64) DEFAULT NULL COMMENT '发送者用户名',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '发送者昵称',
    `type` VARCHAR(16) NOT NULL COMMENT '类型: CHAT/EDIT/CURSOR/SELECTION/JOIN/LEAVE/AI/SYSTEM',
    `content` MEDIUMTEXT COMMENT '消息内容(聊天文字/CRDT op JSON/光标坐标)',
    `metadata` TEXT COMMENT '额外元数据JSON',
    `clientMsgId` VARCHAR(64) DEFAULT NULL COMMENT '客户端消息ID(去重用)',
    `broadcast` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否广播',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_room_time` (`roomId`, `createdAt`),
    KEY `idx_type` (`type`),
    KEY `idx_client` (`clientMsgId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作消息表 V2.8.7';

-- ===========================================================
-- 测试种子数据 (可选, 演示用)
-- ===========================================================
INSERT INTO `collab_room` (`roomId`, `name`, `type`, `ownerId`, `ownerName`, `isPublic`, `maxParticipants`)
VALUES
    ('DEMO0001', 'AI 协作演示房间', 'AI_CHAT', 1, 'adminLiugl', 1, 20),
    ('TRAIN001', '训练任务协作监控', 'TRAINING', 1, 'adminLiugl', 1, 10),
    ('DOCEDIT01', '产品需求文档协同', 'DOC', 1, 'adminLiugl', 0, 5);
