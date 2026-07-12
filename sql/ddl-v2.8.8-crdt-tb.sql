-- ===========================================================
-- V2.8.8 协作 CRDT + TensorBoard 缓存 DDL
-- ===========================================================

-- CRDT 文档表 (V2.8.8 真实多人编辑)
CREATE TABLE IF NOT EXISTS `collab_doc` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `roomId` VARCHAR(32) NOT NULL COMMENT '房间ID',
    `docId` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '文档ID(预留, 未来多文档)',
    `version` BIGINT NOT NULL DEFAULT 0 COMMENT 'CRDT 版本号',
    `itemsJson` MEDIUMTEXT COMMENT 'CRDT items 快照 (JSON)',
    `tombstonesJson` TEXT COMMENT '已删除项 (JSON 数组)',
    `textCache` MEDIUMTEXT COMMENT '派生文本缓存',
    `updatedAt` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_doc` (`roomId`, `docId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协作 CRDT 文档表 V2.8.8';

-- CRDT op 持久化表 (供回放, 限保留 24h)
CREATE TABLE IF NOT EXISTS `collab_op` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `roomId` VARCHAR(32) NOT NULL COMMENT '房间ID',
    `userId` BIGINT NOT NULL DEFAULT 0,
    `opJson` TEXT NOT NULL COMMENT 'CRDT op JSON',
    `vectorClock` BIGINT NOT NULL DEFAULT 0 COMMENT '向量时钟',
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_room_time` (`roomId`, `createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRDT 操作日志 V2.8.8';

-- TensorBoard runs 缓存 (避免每次重读文件系统)
CREATE TABLE IF NOT EXISTS `tensorboard_run` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `runId` VARCHAR(64) NOT NULL,
    `name` VARCHAR(128) DEFAULT NULL,
    `model` VARCHAR(64) DEFAULT NULL,
    `totalSteps` BIGINT NOT NULL DEFAULT 0,
    `latestLoss` DOUBLE DEFAULT NULL,
    `latestAccuracy` DOUBLE DEFAULT NULL,
    `tagsJson` TEXT COMMENT '该 run 包含的 tag 列表 (JSON)',
    `lastSyncAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `createdAt` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_run` (`runId`),
    KEY `idx_model` (`model`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TensorBoard runs 缓存 V2.8.8';
