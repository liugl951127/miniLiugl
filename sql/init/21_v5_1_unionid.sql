-- =============================================================
-- V5.1: unionid 跨应用打通 + wechat_user_binding 唯一约束迁移
-- 同一微信开放平台下的公众号 / 小程序 / App 共享 unionid
-- 同一 user 可绑定多个 app 的 openid (互不冲突)
-- =============================================================

-- 1) wechat_user_binding 唯一约束迁移: openid → (app_type + openid)
-- (同一微信下不同应用的 openid 完全不同)
SET @drop_idx := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'wechat_user_binding' AND index_name = 'uk_openid') > 0,
  'ALTER TABLE wechat_user_binding DROP INDEX uk_openid',
  'SELECT 1');
PREPARE stmt FROM @drop_idx; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @add_idx := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'wechat_user_binding' AND index_name = 'uk_app_openid') = 0,
  'ALTER TABLE wechat_user_binding ADD UNIQUE KEY uk_app_openid (app_type, openid)',
  'SELECT 1');
PREPARE stmt FROM @add_idx; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 新增 unionid_relations 表
-- 跨应用 unionid 关联 (1 user_id 对应 1 unionid)
CREATE TABLE IF NOT EXISTS unionid_relations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '平台用户 ID',
    unionid VARCHAR(64) NOT NULL COMMENT '微信 unionid (跨应用唯一)',
    platform VARCHAR(32) DEFAULT 'wechat' COMMENT '平台: wechat/qq/alipay',
    first_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次出现时间',
    last_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    binding_count INT DEFAULT 1 COMMENT '关联应用数',
    UNIQUE KEY uk_user_unionid (user_id, unionid),
    UNIQUE KEY uk_unionid (unionid),
    INDEX idx_user_id (user_id),
    INDEX idx_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='unionid 跨应用关联';

-- 3) sys_user 加索引 (unionid 字段已有, 加索引加速匹配)
ALTER TABLE sys_user ADD INDEX idx_wechat_unionid (wechat_unionid);

-- 4) 预置示例 unionid (沙箱演示用)
INSERT IGNORE INTO unionid_relations (user_id, unionid, platform, binding_count) VALUES
(2, 'mock_union_admin', 'wechat', 1),
(6, 'mock_union_demo_user', 'wechat', 2);