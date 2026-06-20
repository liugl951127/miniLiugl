-- ============================================================
-- 20_v5_wechat_scan.sql
-- 微信扫码登录 (V5, 微信开放平台 / 公众号 网页授权)
--
-- 用法:
--   1. 管理员在 微信公众平台/开放平台 申请 网站应用, 获取 AppID + AppSecret
--   2. 配置授权回调域: api.your-domain.com
--   3. 把 AppID + AppSecret 写入 minimax.auth.wechat.app-id / app-secret (yml)
--   4. 用户扫码登录后, 通过 openid 唯一标识, 自动注册/绑定现有账号
--
-- 端点:
--   GET  /api/v1/auth/wechat/qrcode       - 生成二维码 (返回 ticket + qrcode_url + expires_in)
--   GET  /api/v1/auth/wechat/status        - 轮询扫码状态 (前端每 2s 调一次)
--   GET  /api/v1/auth/wechat/callback      - 微信回调 (用户扫码确认后微信重定向到此)
--   POST /api/v1/auth/wechat/mobile-login  - 移动端 (微信公众号) 静默登录
-- ============================================================

USE `minimax_platform`;

-- ============================================================
-- 1. 扩展 sys_user 表 (加微信字段)
-- ============================================================
ALTER TABLE `sys_user`
  ADD COLUMN `wechat_openid`   VARCHAR(64)          DEFAULT NULL COMMENT '微信 openid (公众号/小程序唯一)',
  ADD COLUMN `wechat_unionid`  VARCHAR(64)          DEFAULT NULL COMMENT '微信 unionid (开放平台跨应用唯一)',
  ADD COLUMN `wechat_nickname` VARCHAR(64)          DEFAULT NULL COMMENT '微信昵称 (冗余, 不随用户改名)',
  ADD COLUMN `wechat_avatar`   VARCHAR(512)         DEFAULT NULL COMMENT '微信头像 URL',
  ADD COLUMN `wechat_bound_at` DATETIME             DEFAULT NULL COMMENT '微信绑定时间',
  ADD KEY `idx_wechat_openid` (`wechat_openid`),
  ADD KEY `idx_wechat_unionid` (`wechat_unionid`);

-- ============================================================
-- 2. 微信扫码会话表 (二维码 ticket 与登录态映射)
-- ============================================================
DROP TABLE IF EXISTS `wechat_scan_session`;
CREATE TABLE `wechat_scan_session` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `ticket`          VARCHAR(64)  NOT NULL                            COMMENT '二维码 ticket (前端轮询用)',
  `scene_id`        VARCHAR(64)  NOT NULL                            COMMENT '微信 scene_id (UUID)',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'pending'          COMMENT 'pending/scanned/confirmed/expired/cancelled',
  `openid`          VARCHAR(64)           DEFAULT NULL               COMMENT '微信 openid (扫码后回填)',
  `unionid`         VARCHAR(64)           DEFAULT NULL               COMMENT '微信 unionid',
  `nickname`        VARCHAR(64)           DEFAULT NULL               COMMENT '微信昵称',
  `avatar`          VARCHAR(512)          DEFAULT NULL               COMMENT '微信头像',
  `user_id`         BIGINT                DEFAULT NULL               COMMENT '确认后绑定的用户 ID',
  `access_token`    VARCHAR(1024)         DEFAULT NULL               COMMENT '登录 access token (确认后回填)',
  `refresh_token`   VARCHAR(1024)         DEFAULT NULL               COMMENT '登录 refresh token',
  `client_ip`       VARCHAR(64)           DEFAULT NULL               COMMENT '生成二维码的客户端 IP',
  `user_agent`      VARCHAR(512)          DEFAULT NULL               COMMENT 'User-Agent',
  `expires_at`      DATETIME     NOT NULL                            COMMENT '过期时间 (默认 5 分钟)',
  `confirmed_at`    DATETIME              DEFAULT NULL               COMMENT '用户扫码确认时间',
  `created_at`      DATETIME              DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket` (`ticket`),
  KEY `idx_scene_id` (`scene_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信扫码登录会话 (V5)';

-- ============================================================
-- 3. 微信用户与平台账号绑定关系 (一个 openid 可绑定多平台账号)
-- ============================================================
DROP TABLE IF EXISTS `wechat_user_binding`;
CREATE TABLE `wechat_user_binding` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL                            COMMENT '平台 user_id',
  `openid`        VARCHAR(64)  NOT NULL                            COMMENT '微信 openid',
  `unionid`       VARCHAR(64)           DEFAULT NULL               COMMENT '微信 unionid',
  `app_type`      VARCHAR(16)  NOT NULL DEFAULT 'mp'               COMMENT 'mp=公众号, mini=小程序, open=开放平台, web=网页应用',
  `nickname`      VARCHAR(64)           DEFAULT NULL,
  `avatar`        VARCHAR(512)          DEFAULT NULL,
  `bound_at`      DATETIME              DEFAULT CURRENT_TIMESTAMP,
  `last_login_at` DATETIME              DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid_app` (`openid`, `app_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_unionid` (`unionid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信用户与平台账号绑定关系';

-- ============================================================
-- 4. 微信配置 (单条, id=1)
-- ============================================================
DROP TABLE IF EXISTS `wechat_config`;
CREATE TABLE `wechat_config` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `app_type`          VARCHAR(16)  NOT NULL DEFAULT 'mp'             COMMENT 'mp=公众号, mini=小程序, open=开放平台',
  `app_id`            VARCHAR(64)  NOT NULL,
  `app_secret`        VARCHAR(128) NOT NULL,
  `token`             VARCHAR(64)           DEFAULT NULL               COMMENT '消息校验 Token',
  `aes_key`           VARCHAR(64)           DEFAULT NULL               COMMENT '消息加解密 Key',
  `redirect_uri`      VARCHAR(512)          DEFAULT NULL               COMMENT '回调域名 (不带参数)',
  `scope`             VARCHAR(64)  NOT NULL DEFAULT 'snsapi_login'    COMMENT 'snsapi_login/snsapi_userinfo',
  `enabled`           TINYINT(1)   NOT NULL DEFAULT 1,
  `remark`            VARCHAR(255)          DEFAULT NULL,
  `created_at`        DATETIME              DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_type` (`app_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信应用配置';

-- 初始占位配置 (AppID/AppSecret 由管理员在管理后台填)
INSERT INTO `wechat_config` (`app_type`, `app_id`, `app_secret`, `redirect_uri`, `scope`, `enabled`, `remark`)
VALUES
  ('mp',    'wx0000000000000000', 'PLACEHOLDER_REPLACE_IN_ADMIN', 'https://api.your-domain.com/api/v1/auth/wechat/callback', 'snsapi_login', 0, '公众号 (网页扫码登录)'),
  ('mini',  'wx0000000000000001', 'PLACEHOLDER_REPLACE_IN_ADMIN', '',                                                    'snsapi_login', 0, '小程序 (静默登录)'),
  ('open',  'wx0000000000000002', 'PLACEHOLDER_REPLACE_IN_ADMIN', 'https://api.your-domain.com/api/v1/auth/wechat/callback', 'snsapi_userinfo', 0, '开放平台 (第三方网站)');

-- ============================================================
-- 5. 微信扫码登录日志
-- ============================================================
DROP TABLE IF EXISTS `wechat_scan_log`;
CREATE TABLE `wechat_scan_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT                DEFAULT NULL,
  `openid`       VARCHAR(64)           DEFAULT NULL,
  `action`       VARCHAR(32)  NOT NULL                            COMMENT 'qrcode/scan/confirm/cancel/expire/login_fail',
  `result`       VARCHAR(16)  NOT NULL DEFAULT 'ok'               COMMENT 'ok/error',
  `error_msg`    VARCHAR(512)          DEFAULT NULL,
  `client_ip`    VARCHAR(64)           DEFAULT NULL,
  `created_at`   DATETIME              DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信扫码日志';
