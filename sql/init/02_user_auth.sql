-- ============================================================
-- MiniMax Platform - Day 2: User & Auth Schema
-- MySQL 8.x  |  utf8mb4  |  InnoDB
-- ============================================================

CREATE DATABASE IF NOT EXISTS `minimax_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `minimax_platform`;

-- ------------------------------------------------------------
-- sys_user 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        VARCHAR(64)  NOT NULL                COMMENT '用户名(唯一)',
  `password`        VARCHAR(128) NOT NULL                COMMENT 'BCrypt 密码哈希',
  `nickname`        VARCHAR(64)           DEFAULT NULL   COMMENT '昵称',
  `email`           VARCHAR(128)          DEFAULT NULL   COMMENT '邮箱',
  `phone`           VARCHAR(32)           DEFAULT NULL   COMMENT '手机号',
  `avatar`          VARCHAR(256)          DEFAULT NULL   COMMENT '头像 URL',
  `gender`          TINYINT               DEFAULT 0      COMMENT '0未知 1男 2女',
  `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '0禁用 1正常',
  `last_login_ip`   VARCHAR(64)           DEFAULT NULL   COMMENT '最近登录 IP',
  `last_login_at`   DATETIME              DEFAULT NULL   COMMENT '最近登录时间',
  `tenant_id`       BIGINT       NOT NULL DEFAULT 0      COMMENT '租户 ID(预留多租户)',
  `remark`          VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
  `created_by`      BIGINT                DEFAULT NULL,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by`      BIGINT                DEFAULT NULL,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '0未删 1已删(逻辑)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- sys_role 角色表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(32)  NOT NULL COMMENT '角色编码: ADMIN / USER / GUEST',
  `name`        VARCHAR(64)  NOT NULL COMMENT '角色名',
  `description` VARCHAR(255)          DEFAULT NULL,
  `sort`        INT          NOT NULL DEFAULT 0,
  `enabled`     TINYINT      NOT NULL DEFAULT 1,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ------------------------------------------------------------
-- sys_user_role 用户-角色关联
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `idx_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- ------------------------------------------------------------
-- auth_refresh_token 刷新令牌表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `auth_refresh_token`;
CREATE TABLE `auth_refresh_token` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL,
  `token`       VARCHAR(128) NOT NULL COMMENT 'refresh token 哈希',
  `expires_at`  DATETIME     NOT NULL,
  `revoked`     TINYINT      NOT NULL DEFAULT 0 COMMENT '1 已撤销',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_user` (`user_id`),
  KEY `idx_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷新令牌';

-- ------------------------------------------------------------
-- auth_login_log 登录日志
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `auth_login_log`;
CREATE TABLE `auth_login_log` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT                DEFAULT NULL,
  `username`   VARCHAR(64)           DEFAULT NULL,
  `ip`         VARCHAR(64)           DEFAULT NULL,
  `user_agent` VARCHAR(512)          DEFAULT NULL,
  `status`     TINYINT      NOT NULL COMMENT '0失败 1成功',
  `message`    VARCHAR(255)          DEFAULT NULL,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';

-- ============================================================
-- 初始数据
-- ============================================================
-- 默认密码: admin@123  (BCrypt cost=10)
-- 哈希: $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
INSERT INTO `sys_user` (`username`,`password`,`nickname`,`email`,`status`)
VALUES ('admin','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','超级管理员','admin@minimax.local',1);

INSERT INTO `sys_role` (`code`,`name`,`description`,`sort`) VALUES
('ADMIN','超级管理员','拥有所有权限',1),
('USER' ,'普通用户'  ,'日常对话与知识库',2);

INSERT INTO `sys_user_role` (`user_id`,`role_id`)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username='admin' AND r.code='ADMIN';
