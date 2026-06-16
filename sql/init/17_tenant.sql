-- ============================================================
-- V3.1: 多租户 (tenant_id 隔离)
-- adminLiugl 是平台所有者, 自带特殊跨租户权限
-- 其他用户通过 tenant_id 严格隔离
-- ============================================================

USE `minimax`;

-- 1. 租户表
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `code`            VARCHAR(32)  NOT NULL                COMMENT '租户代码(短, 用于URL/标识)',
  `name`            VARCHAR(64)  NOT NULL                COMMENT '租户名称',
  `plan`            VARCHAR(16)  NOT NULL DEFAULT 'free' COMMENT '套餐: free/pro/enterprise',
  `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '0禁用 1正常',
  `max_users`       INT          NOT NULL DEFAULT 10,
  `max_models`      INT          NOT NULL DEFAULT 5,
  `qps_limit`       INT          NOT NULL DEFAULT 100    COMMENT 'QPS 上限',
  `monthly_quota`   BIGINT       NOT NULL DEFAULT 100000 COMMENT '月度调用配额',
  `used_quota`      BIGINT       NOT NULL DEFAULT 0,
  `expire_at`       DATETIME              DEFAULT NULL   COMMENT '过期时间(NULL=永久)',
  `contact_email`   VARCHAR(128)          DEFAULT NULL,
  `contact_phone`   VARCHAR(32)           DEFAULT NULL,
  `remark`          VARCHAR(255)          DEFAULT NULL,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户';

-- 2. sys_user 增加 tenant_id (已有, 改为 NOT NULL 0)
-- 已有字段: tenant_id BIGINT NOT NULL DEFAULT 0
-- 含义: 0 = 平台所有者 (adminLiugl 跨租户), 1+ = 普通租户

-- 3. 初始化默认租户
INSERT INTO `tenant` (`code`, `name`, `plan`, `max_users`, `max_models`, `qps_limit`, `monthly_quota`, `contact_email`, `remark`)
VALUES
  ('default', '默认租户', 'pro', 100, 20, 500, 1000000, 'admin@minimax.local', '平台默认租户, 所有无租户用户归此处'),
  ('demo', '演示租户', 'free', 10, 5, 100, 100000, 'demo@minimax.local', '演示用免费租户');

-- 4. 把现有非 adminLiugl 用户 (admin) 分配到 default 租户
UPDATE `sys_user` SET `tenant_id` = 1 WHERE `username` <> 'adminLiugl' AND `tenant_id` = 0;

-- 5. 新增 sys_tenant_role 租户管理员角色 (可选)
-- adminLiugl 保持 tenant_id=0 表示跨租户超级管理员
