-- =============================================================
-- V5.2: QQ/支付宝 跨平台 unionid 打通
-- 同一用户用 QQ/支付宝/微信 登录 → 共享平台账号
-- =============================================================

-- 1) sys_user 加 QQ/支付宝 字段 (兼容老库: 用 IF NOT EXISTS)
-- MariaDB 10.0.2+ 支持 ADD COLUMN IF NOT EXISTS
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS qq_openid VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS qq_unionid VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS qq_nickname VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS qq_avatar VARCHAR(512);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS qq_bound_at DATETIME;

ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS alipay_openid VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS alipay_user_id VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS alipay_nickname VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS alipay_avatar VARCHAR(512);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS alipay_bound_at DATETIME;

-- 加索引 (跨平台查询, 用存储过程判断是否存在)
DROP PROCEDURE IF EXISTS add_index_if_not_exists;
DELIMITER //
CREATE PROCEDURE add_index_if_not_exists()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_qq_openid') THEN
        ALTER TABLE sys_user ADD INDEX idx_qq_openid (qq_openid);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_qq_unionid') THEN
        ALTER TABLE sys_user ADD INDEX idx_qq_unionid (qq_unionid);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_alipay_openid') THEN
        ALTER TABLE sys_user ADD INDEX idx_alipay_openid (alipay_openid);
    END IF;
END //
DELIMITER ;
CALL add_index_if_not_exists();
DROP PROCEDURE add_index_if_not_exists;

-- 2) oauth_binding 表 (跨平台 binding, 替代 wechat_user_binding 模式, 支持任意平台)
CREATE TABLE IF NOT EXISTS oauth_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    platform VARCHAR(20) NOT NULL,        -- wechat/qq/alipay/weibo/github
    app_type VARCHAR(20) NOT NULL,        -- mp/mini/open/web/app/h5
    openid VARCHAR(128) NOT NULL,
    unionid VARCHAR(128),
    nickname VARCHAR(128),
    avatar VARCHAR(512),
    access_token VARCHAR(512),
    refresh_token VARCHAR(512),
    token_expires_at DATETIME,
    raw_data TEXT,                        -- 平台返回的原始 JSON
    bound_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME,
    UNIQUE KEY uk_platform_app_openid (platform, app_type, openid),
    INDEX idx_user_id (user_id),
    INDEX idx_unionid (unionid),
    INDEX idx_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth 跨平台 binding';

-- 3) unionid_relations 加 platform 字段索引 (支持按平台统计)
-- 已有 platform 字段, 加复合索引 (IF NOT EXISTS 兼容)
DROP PROCEDURE IF EXISTS add_unionid_idx;
DELIMITER //
CREATE PROCEDURE add_unionid_idx()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'unionid_relations' AND index_name = 'idx_platform_unionid') THEN
        ALTER TABLE unionid_relations ADD INDEX idx_platform_unionid (platform, unionid);
    END IF;
END //
DELIMITER ;
CALL add_unionid_idx();
DROP PROCEDURE add_unionid_idx;

-- 4) oauth_app_config 表 (各平台应用配置)
CREATE TABLE IF NOT EXISTS oauth_app_config (
    id INT PRIMARY KEY AUTO_INCREMENT,
    platform VARCHAR(20) NOT NULL,         -- wechat/qq/alipay/weibo/github
    app_type VARCHAR(20) NOT NULL,         -- mp/mini/open/web/app/h5
    app_id VARCHAR(128) NOT NULL,
    app_secret VARCHAR(256),
    public_key TEXT,                        -- 支付宝 RSA 公钥
    redirect_uri VARCHAR(512),
    scopes VARCHAR(256),                    -- 多 scopes 用逗号分隔
    enabled TINYINT(1) DEFAULT 1,
    extra_config TEXT,                      -- JSON 格式额外配置
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_platform_app_type (platform, app_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth 应用配置';

-- 5) 预置占位配置 (沙箱演示)
INSERT IGNORE INTO oauth_app_config (platform, app_type, app_id, app_secret, redirect_uri, enabled) VALUES
('wechat', 'mp',       'PLACEHOLDER_WECHAT_MP',   'PLACEHOLDER_WECHAT_MP_SECRET',   'https://api.your-domain.com/auth/oauth/wechat/callback', 0),
('wechat', 'mini',     'PLACEHOLDER_WECHAT_MINI', 'PLACEHOLDER_WECHAT_MINI_SECRET', '',                                                              0),
('wechat', 'open',     'PLACEHOLDER_WECHAT_OPEN', 'PLACEHOLDER_WECHAT_OPEN_SECRET', 'https://api.your-domain.com/auth/oauth/wechat/callback', 0),
('wechat', 'web',      'PLACEHOLDER_WECHAT_WEB',  'PLACEHOLDER_WECHAT_WEB_SECRET',  'https://api.your-domain.com/auth/oauth/wechat/callback', 0),
('wechat', 'h5',       'PLACEHOLDER_WECHAT_H5',   'PLACEHOLDER_WECHAT_H5_SECRET',   'https://api.your-domain.com/auth/oauth/wechat/h5-callback', 0),
('qq',     'web',      'PLACEHOLDER_QQ_WEB',      'PLACEHOLDER_QQ_WEB_SECRET',      'https://api.your-domain.com/auth/oauth/qq/callback',     0),
('qq',     'h5',       'PLACEHOLDER_QQ_H5',       'PLACEHOLDER_QQ_H5_SECRET',       'https://api.your-domain.com/auth/oauth/qq/callback',     0),
('qq',     'app',      'PLACEHOLDER_QQ_APP',      'PLACEHOLDER_QQ_APP_SECRET',      '',                                                              0),
('alipay', 'web',      'PLACEHOLDER_ALIPAY_WEB',  'PLACEHOLDER_ALIPAY_WEB_SECRET',  'https://api.your-domain.com/auth/oauth/alipay/callback', 0),
('alipay', 'h5',       'PLACEHOLDER_ALIPAY_H5',   'PLACEHOLDER_ALIPAY_H5_SECRET',   'https://api.your-domain.com/auth/oauth/alipay/callback', 0),
('alipay', 'app',      'PLACEHOLDER_ALIPAY_APP',  'PLACEHOLDER_ALIPAY_APP_SECRET',  '',                                                              0);