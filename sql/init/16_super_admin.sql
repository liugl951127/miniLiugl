-- ============================================================
-- MiniMax Platform - SUPER_ADMIN 角色
-- adminLiugl = 唯一超级管理员 (独立于普通 admin)
-- 独立密码、独立 JWT claim
-- ============================================================

USE `minimax`;

-- 确保 SUPER_ADMIN 角色存在
INSERT INTO `sys_role` (`code`, `name`, `description`, `sort`, `enabled`)
VALUES ('SUPER_ADMIN', '超级管理员 (adminLiugl)', '拥有平台所有权限, 包括管理其他管理员', 0, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `enabled` = 1;

-- adminLiugl 用户 (独立于 admin, 密码 = Liugl@2026)
-- 密码哈希由 AdminDataInitializer 启动时 BCrypt 编码, 此处只占位
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `email`, `status`, `tenant_id`, `remark`)
VALUES ('adminLiugl', 'PLACEHOLDER', 'Liugl (Owner)', 'liugl951127@gmail.com', 1, 0, '平台所有者, 唯一超级管理员')
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`), `email` = VALUES(`email`), `status` = 1;

-- 绑定 SUPER_ADMIN 角色 (用 INSERT ... SELECT 关联)
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `sys_user` u, `sys_role` r
WHERE u.username = 'adminLiugl' AND r.code = 'SUPER_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_user_role` ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
