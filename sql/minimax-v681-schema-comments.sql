-- ============================================================
-- MiniMax Platform V6.8 SQL Schema (全面重构版)
-- 规范: 驼峰字段命名 / 表级COMMENT / 字段级COMMENT
-- 数据库: utf8mb4, 引擎: InnoDB
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================
-- 登录日志表 (`auth_login_log`)
-- =============================================================
DROP TABLE IF EXISTS `auth_login_log`;
CREATE TABLE `auth_login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 登录日志表',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `username` VARCHAR(255) NULL COMMENT '用户名',
  `ip` VARCHAR(255) NULL COMMENT '客户端IP',
  `user_agent` VARCHAR(255) NULL COMMENT '客户端UA',
  `status` INT NULL COMMENT '登录状态(0失败/1成功)',
  `message` VARCHAR(255) NULL COMMENT '日志消息',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='登录日志表';

-- =============================================================
-- 刷新令牌表 (`auth_refresh_token`)
-- =============================================================
DROP TABLE IF EXISTS `auth_refresh_token`;
CREATE TABLE `auth_refresh_token` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 刷新令牌表',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `token` VARCHAR(255) NULL COMMENT '刷新令牌',
  `expires_at` TIMESTAMP NULL COMMENT '过期时间',
  `revoked` INT NULL COMMENT '是否撤销(0否/1是)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='刷新令牌表';

-- =============================================================
-- 系统通知表 (`notification`)
-- =============================================================
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 系统通知表',
  `user_id` BIGINT NULL COMMENT '接收用户ID',
  `type` VARCHAR(255) NULL COMMENT '通知类型',
  `title` VARCHAR(255) NULL COMMENT '通知标题',
  `content` VARCHAR(255) NULL COMMENT '通知内容',
  `is_read` INT NULL COMMENT '是否已读(0未读/1已读)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='系统通知表';

-- 种子数据
INSERT INTO `notification` (id, user_id, type, title, content, is_read, created_at) VALUES
(1, 1, 'SYSTEM', '欢迎使用 MiniMax Platform', '平台已就绪，欢迎开始使用！', 0, NOW()),
(2, 2, 'TASK', '训练任务完成', '模型训练任务已完成，准确率 94.2%', 0, NOW()),
(3, 1, 'SYSTEM', '新功能上线', 'V6.8.1 版本已发布，包含多项优化', 1, NOW());

-- =============================================================
-- OAuth应用配置表 (`oauth_app_config`)
-- =============================================================
DROP TABLE IF EXISTS `oauth_app_config`;
CREATE TABLE `oauth_app_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - OAuth应用配置表',
  `platform` VARCHAR(255) NULL COMMENT '平台(wechat/qq/alipay等)',
  `app_type` VARCHAR(255) NULL COMMENT '应用类型(WEB/Mobile等)',
  `app_id` VARCHAR(255) NULL COMMENT '应用ID',
  `app_secret` VARCHAR(255) NULL COMMENT '应用密钥(加密)',
  `public_key` VARCHAR(255) NULL COMMENT '公钥',
  `redirect_uri` VARCHAR(255) NULL COMMENT '回调地址',
  `scopes` VARCHAR(255) NULL COMMENT '授权范围',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `extra_config` VARCHAR(255) NULL COMMENT '扩展配置(JSON)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='OAuth应用配置表';

-- 种子数据
INSERT INTO `oauth_app_config` (id, platform, app_type, app_id, app_secret, enabled, created_at) VALUES
(1, 'wechat', 'WEB', 'wx_app_001', 'secret_xxx', 1, NOW());

-- =============================================================
-- OAuth第三方账号绑定表 (`oauth_binding`)
-- =============================================================
DROP TABLE IF EXISTS `oauth_binding`;
CREATE TABLE `oauth_binding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - OAuth第三方账号绑定表',
  `user_id` BIGINT NULL COMMENT '平台用户ID',
  `platform` VARCHAR(255) NULL COMMENT '第三方平台',
  `app_type` VARCHAR(255) NULL COMMENT '应用类型',
  `openid` VARCHAR(255) NULL COMMENT 'OpenID',
  `unionid` VARCHAR(255) NULL COMMENT 'UnionID',
  `nickname` VARCHAR(255) NULL COMMENT '昵称',
  `avatar` VARCHAR(255) NULL COMMENT '头像URL',
  `access_token` VARCHAR(255) NULL COMMENT '访问令牌',
  `refresh_token` VARCHAR(255) NULL COMMENT '刷新令牌',
  `token_expires_at` TIMESTAMP NULL COMMENT '令牌过期时间',
  `raw_data` VARCHAR(255) NULL COMMENT '平台返回原始数据',
  `bound_at` TIMESTAMP NULL COMMENT '绑定时间',
  `last_login_at` TIMESTAMP NULL COMMENT '最近登录时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='OAuth第三方账号绑定表';

-- =============================================================
-- 系统角色表 (`sys_role`)
-- =============================================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 系统角色表',
  `code` VARCHAR(255) NULL COMMENT '角色代码',
  `name` VARCHAR(255) NULL COMMENT '角色名称',
  `description` VARCHAR(255) NULL COMMENT '角色描述',
  `sort` INT NULL COMMENT '排序权重',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='系统角色表';

-- 种子数据
INSERT INTO `sys_role` (id, role_name, role_key, description, created_at) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '拥有全部权限', NOW()),
(2, '普通用户', 'USER', '基础权限', NOW()),
(3, '运维人员', 'OPERATOR', '系统运维权限', NOW());

-- =============================================================
-- 系统用户表 (`sys_user`)
-- =============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 系统用户表',
  `username` VARCHAR(255) NULL COMMENT '登录用户名',
  `password` VARCHAR(255) NULL COMMENT '登录密码(加密)',
  `nickname` VARCHAR(255) NULL COMMENT '用户昵称',
  `email` VARCHAR(255) NULL COMMENT '邮箱',
  `phone` VARCHAR(255) NULL COMMENT '手机号',
  `avatar` VARCHAR(255) NULL COMMENT '头像URL',
  `gender` INT NULL COMMENT '性别(0未知/1男/2女)',
  `status` INT NULL COMMENT '账号状态(0禁用/1正常)',
  `last_login_ip` VARCHAR(255) NULL COMMENT '最近登录IP',
  `last_login_at` TIMESTAMP NULL COMMENT '最近登录时间',
  `tenant_id` BIGINT NULL COMMENT '所属租户ID',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `wechat_openid` VARCHAR(255) NULL COMMENT '微信OpenID',
  `wechat_unionid` VARCHAR(255) NULL COMMENT '微信UnionID',
  `wechat_nickname` VARCHAR(255) NULL COMMENT '微信昵称',
  `wechat_avatar` VARCHAR(255) NULL COMMENT '微信头像',
  `wechat_bound_at` TIMESTAMP NULL COMMENT '微信绑定时间',
  `qq_openid` VARCHAR(255) NULL COMMENT 'QQ OpenID',
  `qq_unionid` VARCHAR(255) NULL COMMENT 'QQ UnionID',
  `qq_nickname` VARCHAR(255) NULL COMMENT 'QQ昵称',
  `qq_avatar` VARCHAR(255) NULL COMMENT 'QQ头像',
  `qq_bound_at` TIMESTAMP NULL COMMENT 'QQ绑定时间',
  `alipay_openid` VARCHAR(255) NULL COMMENT '支付宝OpenID',
  `alipay_user_id` VARCHAR(255) NULL COMMENT '支付宝用户ID',
  `alipay_nickname` VARCHAR(255) NULL COMMENT '支付宝昵称',
  `alipay_avatar` VARCHAR(255) NULL COMMENT '支付宝头像',
  `alipay_bound_at` TIMESTAMP NULL COMMENT '支付宝绑定时间',
  `created_by` BIGINT NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建人ID',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新人ID',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='系统用户表';

-- 种子数据
INSERT INTO `sys_user` (id, username, password, email, phone, role, status, created_at) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt/Hzu', 'admin@minimax.com', '13800138000', 'SUPER_ADMIN', 1, NOW()),
(2, 'user01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt/Hzu', 'user01@minimax.com', '13800138001', 'USER', 1, NOW()),
(3, 'operator', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt/Hzu', 'op@minimax.com', '13800138002', 'OPERATOR', 1, NOW());

-- =============================================================
-- 用户角色关联表 (`sys_user_role`)
-- =============================================================
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NULL COMMENT '用户ID',
  `role_id` BIGINT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='用户角色关联表';

-- 种子数据
INSERT INTO `sys_user_role` (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3);

-- =============================================================
-- 租户表 (`tenant`)
-- =============================================================
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 租户表',
  `code` VARCHAR(255) NULL COMMENT '租户代码',
  `name` VARCHAR(255) NULL COMMENT '租户名称',
  `plan` VARCHAR(255) NULL COMMENT '套餐(free/pro/enterprise)',
  `status` INT NULL COMMENT '状态(0禁用/1正常/2欠费)',
  `max_users` INT NULL COMMENT '最大用户数',
  `max_models` INT NULL COMMENT '最大模型数',
  `qps_limit` INT NULL COMMENT 'QPS限制',
  `monthly_quota` BIGINT NULL COMMENT '月度配额(分)',
  `used_quota` BIGINT NULL COMMENT '已用配额(分)',
  `expire_at` TIMESTAMP NULL COMMENT '套餐到期时间',
  `contact_email` VARCHAR(255) NULL COMMENT '联系邮箱',
  `contact_phone` VARCHAR(255) NULL COMMENT '联系电话',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='租户表';

-- =============================================================
-- UnionID关联表 (`unionid_relations`)
-- =============================================================
DROP TABLE IF EXISTS `unionid_relations`;
CREATE TABLE `unionid_relations` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - UnionID关联表',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `unionid` VARCHAR(255) NULL COMMENT '第三方UnionID',
  `platform` VARCHAR(255) NULL COMMENT '第三方平台',
  `first_seen_at` TIMESTAMP NULL COMMENT '首次出现时间',
  `last_seen_at` TIMESTAMP NULL COMMENT '最近出现时间',
  `binding_count` INT NULL COMMENT '绑定次数',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='UnionID关联表';

-- =============================================================
-- 用户API Key表 (`user_api_key`)
-- =============================================================
DROP TABLE IF EXISTS `user_api_key`;
CREATE TABLE `user_api_key` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 用户API Key表',
  `user_id` BIGINT NULL COMMENT '所属用户ID',
  `name` VARCHAR(255) NULL COMMENT 'Key名称',
  `key_hash` VARCHAR(255) NULL COMMENT 'Key哈希值(SHA256)',
  `key_prefix` VARCHAR(255) NULL COMMENT 'Key前缀(显示用)',
  `scopes` VARCHAR(255) NULL COMMENT '权限范围(JSON数组)',
  `expires_at` TIMESTAMP NULL COMMENT '过期时间',
  `last_used_at` TIMESTAMP NULL COMMENT '最近使用时间',
  `use_count` BIGINT NULL COMMENT '累计使用次数',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='用户API Key表';

-- 种子数据
INSERT INTO `user_api_key` (user_id, name, key_hash, key_prefix, scopes, enabled, use_count, created_at) VALUES
(1, '测试 Key', SHA2('mmx_testkey001234567890abcdef', 256), 'mmx_test', 'chat:send,chat:stream', 1, 0, NOW()),
(2, '生产环境', SHA2('mmx_prodkey1234567890abcdef', 256), 'mmx_prod', 'chat:send,chat:stream,agent:run', 1, 47, NOW());

-- =============================================================
-- 微信公众平台配置表 (`wechat_config`)
-- =============================================================
DROP TABLE IF EXISTS `wechat_config`;
CREATE TABLE `wechat_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 微信公众平台配置表',
  `app_type` VARCHAR(255) NULL COMMENT '应用类型(服务号/订阅号/小程序)',
  `app_id` VARCHAR(255) NULL COMMENT 'AppID',
  `app_secret` VARCHAR(255) NULL COMMENT 'AppSecret(加密)',
  `token` VARCHAR(255) NULL COMMENT '微信Token',
  `aes_key` VARCHAR(255) NULL COMMENT 'EncodingAESKey',
  `redirect_uri` VARCHAR(255) NULL COMMENT '授权回调地址',
  `scope` VARCHAR(255) NULL COMMENT '授权作用域',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='微信公众平台配置表';

-- =============================================================
-- 微信扫码登录会话表 (`wechat_scan_session`)
-- =============================================================
DROP TABLE IF EXISTS `wechat_scan_session`;
CREATE TABLE `wechat_scan_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 微信扫码登录会话表',
  `ticket` VARCHAR(255) NULL COMMENT '微信扫码票据',
  `scene_id` VARCHAR(255) NULL COMMENT '场景ID',
  `status` VARCHAR(255) NULL COMMENT '会话状态(0待扫/1已扫待确认/2已完成/3已过期)',
  `openid` VARCHAR(255) NULL COMMENT '扫码用户OpenID',
  `unionid` VARCHAR(255) NULL COMMENT '扫码用户UnionID',
  `nickname` VARCHAR(255) NULL COMMENT '用户昵称',
  `avatar` VARCHAR(255) NULL COMMENT '用户头像',
  `user_id` BIGINT NULL COMMENT '绑定用户ID',
  `access_token` VARCHAR(255) NULL COMMENT 'AccessToken',
  `refresh_token` VARCHAR(255) NULL COMMENT '刷新令牌',
  `client_ip` VARCHAR(255) NULL COMMENT '客户端IP',
  `user_agent` VARCHAR(255) NULL COMMENT '客户端UA',
  `expires_at` TIMESTAMP NULL COMMENT '会话过期时间',
  `confirmed_at` TIMESTAMP NULL COMMENT '确认时间',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='微信扫码登录会话表';

-- =============================================================
-- 微信用户绑定表 (`wechat_user_binding`)
-- =============================================================
DROP TABLE IF EXISTS `wechat_user_binding`;
CREATE TABLE `wechat_user_binding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 微信用户绑定表',
  `user_id` BIGINT NULL COMMENT '平台用户ID',
  `openid` VARCHAR(255) NULL COMMENT '微信OpenID',
  `unionid` VARCHAR(255) NULL COMMENT '微信UnionID',
  `app_type` VARCHAR(255) NULL COMMENT '应用类型',
  `nickname` VARCHAR(255) NULL COMMENT '微信昵称',
  `avatar` VARCHAR(255) NULL COMMENT '微信头像',
  `bound_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `last_login_at` TIMESTAMP NULL COMMENT '最近登录时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='微信用户绑定表';

-- =============================================================
-- 管理员审计日志表 (`admin_audit_log`)
-- =============================================================
DROP TABLE IF EXISTS `admin_audit_log`;
CREATE TABLE `admin_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 管理员审计日志表',
  `actor_id` BIGINT NULL COMMENT '操作人ID',
  `actor_name` VARCHAR(255) NULL COMMENT '操作人名称',
  `action` VARCHAR(255) NULL COMMENT '操作类型(CREATE/UPDATE/DELETE)',
  `resource_type` VARCHAR(255) NULL COMMENT '资源类型',
  `resource_id` VARCHAR(255) NULL COMMENT '资源ID',
  `detail` VARCHAR(255) NULL COMMENT '操作详情(JSON)',
  `result` VARCHAR(255) NULL COMMENT '操作结果(success/failed)',
  `error_msg` VARCHAR(255) NULL COMMENT '错误信息',
  `ip` VARCHAR(255) NULL COMMENT '操作人IP',
  `user_agent` VARCHAR(255) NULL COMMENT '操作人UA',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='管理员审计日志表';

-- 种子数据
INSERT INTO `admin_audit_log` (id, user_id, action, resource, ip, created_at) VALUES
(1, 1, 'CREATE', 'training_task', '127.0.0.1', NOW()),
(2, 2, 'QUERY', 'ai_chat_session', '127.0.0.1', NOW()),
(3, 1, 'DELETE', 'knowledge_base', '127.0.0.1', NOW());

-- =============================================================
-- 完整审计日志表(全字段) (`audit_log_full`)
-- =============================================================
DROP TABLE IF EXISTS `audit_log_full`;
CREATE TABLE `audit_log_full` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 完整审计日志表(全字段)',
  `trace_id` VARCHAR(255) NULL COMMENT '链路追踪ID',
  `user_id` BIGINT NULL COMMENT '操作用户ID',
  `username` VARCHAR(255) NULL COMMENT '用户名',
  `user_ip` VARCHAR(255) NULL COMMENT '用户IP',
  `user_agent` VARCHAR(255) NULL COMMENT '用户UA',
  `action` VARCHAR(255) NULL COMMENT '操作动作',
  `resource_type` VARCHAR(255) NULL COMMENT '资源类型',
  `resource_id` VARCHAR(255) NULL COMMENT '资源ID',
  `method` VARCHAR(255) NULL COMMENT 'HTTP方法(GET/POST等)',
  `path` VARCHAR(255) NULL COMMENT '请求路径',
  `request_body` VARCHAR(255) NULL COMMENT '请求体(脱敏)',
  `response_status` INT NULL COMMENT '响应状态码',
  `result` VARCHAR(255) NULL COMMENT '操作结果',
  `error_msg` VARCHAR(255) NULL COMMENT '错误信息',
  `duration_ms` INT NULL COMMENT '耗时(毫秒)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='完整审计日志表(全字段)';

-- =============================================================
-- 智能体群组表 (`agent_group`)
-- =============================================================
DROP TABLE IF EXISTS `agent_group`;
CREATE TABLE `agent_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 智能体群组表',
  `group_id` VARCHAR(255) NULL COMMENT '群组业务ID',
  `name` VARCHAR(255) NULL COMMENT '群组名称',
  `description` VARCHAR(255) NULL COMMENT '群组描述',
  `strategy` VARCHAR(255) NULL COMMENT '协作策略(parallel/sequential/hierarchical)',
  `members_json` VARCHAR(255) NULL COMMENT '成员配置(JSON数组)',
  `status` VARCHAR(255) NULL COMMENT '状态(0禁用/1启用)',
  `owner_id` BIGINT NULL COMMENT '创建人ID',
  `tags` VARCHAR(255) NULL COMMENT '标签(逗号分隔)',
  `last_run_at` TIMESTAMP NULL COMMENT '最近运行时间',
  `run_count` INT NULL COMMENT '累计运行次数',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='智能体群组表';

-- 种子数据
INSERT INTO `agent_group` (id, name, description, visibility, created_at) VALUES
(1, '客服组', '在线客服智能体组', 'PRIVATE', NOW()),
(2, '审核组', '内容审核智能体组', 'PRIVATE', NOW());

-- =============================================================
-- AI对话消息表 (`ai_chat_message`)
-- =============================================================
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - AI对话消息表',
  `session_id` VARCHAR(255) NULL COMMENT '会话ID',
  `role` VARCHAR(255) NULL COMMENT '角色(user/assistant/system/tool)',
  `content` VARCHAR(255) NULL COMMENT '消息内容',
  `tool_code` VARCHAR(255) NULL COMMENT '工具代码',
  `tool_input` VARCHAR(255) NULL COMMENT '工具输入(JSON)',
  `tool_output` VARCHAR(255) NULL COMMENT '工具输出(JSON)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='AI对话消息表';

-- 种子数据
INSERT INTO `ai_chat_message` (id, session_id, role, content, created_at) VALUES
(1, 1, 'user', '你好，请介绍一下你自己', NOW()),
(2, 1, 'assistant', '我是 MiniMax AI 助手，可以帮助你完成各种任务。', NOW());

-- =============================================================
-- AI对话会话表 (`ai_chat_session`)
-- =============================================================
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - AI对话会话表',
  `session_id` VARCHAR(255) NULL COMMENT '会话业务ID',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `username` VARCHAR(255) NULL COMMENT '用户名',
  `title` VARCHAR(255) NULL COMMENT '会话标题',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `intent` VARCHAR(255) NULL COMMENT '识别意图',
  `confidence` DOUBLE NULL COMMENT '置信度(0-1)',
  `alternatives` VARCHAR(255) NULL COMMENT '备选意图(JSON)',
  `model` VARCHAR(255) NULL COMMENT '使用的模型',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='AI对话会话表';

-- 种子数据
INSERT INTO `ai_chat_session` (id, user_id, title, model, status, created_at) VALUES
(1, 1, '测试会话', 'gpt-4o-mini', 'ACTIVE', NOW()),
(2, 2, '客服问答', 'gpt-4o', 'ACTIVE', NOW());

-- =============================================================
-- AI内容生成日志表 (`ai_generation_log`)
-- =============================================================
DROP TABLE IF EXISTS `ai_generation_log`;
CREATE TABLE `ai_generation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - AI内容生成日志表',
  `generation_id` VARCHAR(255) NULL COMMENT '生成任务ID',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `username` VARCHAR(255) NULL COMMENT '用户名',
  `user_ip` VARCHAR(255) NULL COMMENT '用户IP',
  `modality` VARCHAR(255) NULL COMMENT '模态(text/image/audio/video)',
  `model_name` VARCHAR(255) NULL COMMENT '模型名称',
  `model_version` VARCHAR(255) NULL COMMENT '模型版本',
  `prompt` VARCHAR(255) NULL COMMENT '用户提示词',
  `negative_prompt` VARCHAR(255) NULL COMMENT '反向提示词(仅图像)',
  `parameters` VARCHAR(255) NULL COMMENT '生成参数(JSON)',
  `output_url` VARCHAR(255) NULL COMMENT '输出文件URL',
  `output_size` BIGINT NULL COMMENT '输出文件大小(字节)',
  `output_hash` VARCHAR(255) NULL COMMENT '输出文件哈希',
  `watermarked` INT NULL COMMENT '是否带水印(0否/1是)',
  `watermark_text` VARCHAR(255) NULL COMMENT '水印文本',
  `duration_ms` INT NULL COMMENT '生成耗时(毫秒)',
  `status` VARCHAR(255) NULL COMMENT '状态(success/failed/running)',
  `error_msg` VARCHAR(255) NULL COMMENT '错误信息',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='AI内容生成日志表';

-- =============================================================
-- AI意图关键词表 (`ai_intent_keyword`)
-- =============================================================
DROP TABLE IF EXISTS `ai_intent_keyword`;
CREATE TABLE `ai_intent_keyword` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - AI意图关键词表',
  `intent` VARCHAR(255) NULL COMMENT '意图名称',
  `keyword` VARCHAR(255) NULL COMMENT '关键词或正则',
  `weight` INT NULL COMMENT '匹配权重',
  `is_regex` INT NULL COMMENT '是否正则表达式(0精确/1正则)',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `language` VARCHAR(255) NULL COMMENT '语言(zh/en/mixed)',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='AI意图关键词表';

-- =============================================================
-- AI工具定义表 (`ai_tool`)
-- =============================================================
DROP TABLE IF EXISTS `ai_tool`;
CREATE TABLE `ai_tool` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - AI工具定义表',
  `code` VARCHAR(255) NULL COMMENT '工具唯一代码',
  `name` VARCHAR(255) NULL COMMENT '工具名称',
  `category` VARCHAR(255) NULL COMMENT '工具类别(search/calc/code等)',
  `description` VARCHAR(255) NULL COMMENT '工具描述',
  `icon` VARCHAR(255) NULL COMMENT '图标Emoji',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `builtin` INT NULL COMMENT '是否内置(0自定义/1内置)',
  `input_schema` VARCHAR(255) NULL COMMENT '输入参数Schema(JSON)',
  `output_schema` VARCHAR(255) NULL COMMENT '输出结果Schema(JSON)',
  `default_config` VARCHAR(255) NULL COMMENT '默认配置(JSON)',
  `impl_type` VARCHAR(255) NULL COMMENT '实现类型(http/function/script)',
  `impl_value` VARCHAR(255) NULL COMMENT '实现值(URL/函数名/脚本)',
  `rate_limit` INT NULL COMMENT '速率限制(次/分钟)',
  `timeout_seconds` INT NULL COMMENT '超时时间(秒)',
  `role_required` VARCHAR(255) NULL COMMENT '所需角色',
  `tags` VARCHAR(255) NULL COMMENT '标签',
  `version` VARCHAR(255) NULL COMMENT '版本号',
  `author` VARCHAR(255) NULL COMMENT '作者',
  `created_by` BIGINT NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建人ID',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='AI工具定义表';

-- 种子数据
INSERT INTO `ai_tool` (id, name, category, description, endpoint, enabled, created_at) VALUES
(1, 'weather', 'utility', '天气查询', '/api/weather', 1, NOW()),
(2, 'search', 'search', '搜索', '/api/search', 1, NOW());

-- =============================================================
-- AI工具调用日志表 (`ai_tool_invocation`)
-- =============================================================
DROP TABLE IF EXISTS `ai_tool_invocation`;
CREATE TABLE `ai_tool_invocation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - AI工具调用日志表',
  `tool_code` VARCHAR(255) NULL COMMENT '工具代码',
  `user_id` BIGINT NULL COMMENT '调用用户ID',
  `username` VARCHAR(255) NULL COMMENT '用户名',
  `input_json` VARCHAR(255) NULL COMMENT '输入参数(JSON)',
  `output_json` VARCHAR(255) NULL COMMENT '输出结果(JSON)',
  `status` VARCHAR(255) NULL COMMENT '调用状态(0失败/1成功)',
  `error_message` VARCHAR(255) NULL COMMENT '错误信息',
  `duration_ms` INT NULL COMMENT '调用耗时(毫秒)',
  `ip` VARCHAR(255) NULL COMMENT '调用方IP',
  `user_agent` VARCHAR(255) NULL COMMENT '调用方UA',
  `data_source_id` BIGINT NULL COMMENT '数据源ID',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='AI工具调用日志表';

-- =============================================================
-- AI多模型投票记录表 (`ai_voting_record`)
-- =============================================================
DROP TABLE IF EXISTS `ai_voting_record`;
CREATE TABLE `ai_voting_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - AI多模型投票记录表',
  `session_id` VARCHAR(255) NULL COMMENT '会话ID',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `username` VARCHAR(255) NULL COMMENT '用户名',
  `question` VARCHAR(255) NULL COMMENT '用户问题',
  `final_answer` VARCHAR(255) NULL COMMENT '最终答案',
  `strategy` VARCHAR(255) NULL COMMENT '投票策略(majority/weighted/best-of-n)',
  `total_votes` INT NULL COMMENT '参与投票的模型数量',
  `agreement_rate` DECIMAL(20,4) NULL COMMENT '一致率(0-1)',
  `model_votes` VARCHAR(255) NULL COMMENT '各模型投票详情(JSON)',
  `duration_ms` INT NULL COMMENT '投票耗时(毫秒)',
  `notify_email` VARCHAR(255) NULL COMMENT '通知邮箱',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='AI多模型投票记录表';

-- 种子数据
INSERT INTO `ai_voting_record` (id, question, final_answer, strategy, total_votes, model_votes, notify_email, created_at) VALUES
(1, '2+2等于多少？', '4', 'majority', 4, '[{"model":"gpt-4","answer":"4","confidence":0.99}]', NULL, NOW()),
(2, '北京是哪个国家的首都？', '中国', 'majority', 4, '[{"model":"gpt-4","answer":"中国","confidence":1.0}]', NULL, NOW());

-- =============================================================
-- 审计日志表 (`audit_log`)
-- =============================================================
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 审计日志表',
  `trace_id` VARCHAR(255) NULL COMMENT '链路追踪ID',
  `user_id` BIGINT NULL COMMENT '操作用户ID',
  `username` VARCHAR(255) NULL COMMENT '用户名',
  `user_ip` VARCHAR(255) NULL COMMENT '用户IP',
  `user_agent` VARCHAR(255) NULL COMMENT '用户UA',
  `action` VARCHAR(255) NULL COMMENT '操作动作',
  `resource_type` VARCHAR(255) NULL COMMENT '资源类型',
  `resource_id` VARCHAR(255) NULL COMMENT '资源ID',
  `method` VARCHAR(255) NULL COMMENT 'HTTP方法',
  `path` VARCHAR(255) NULL COMMENT '请求路径',
  `request_body` VARCHAR(255) NULL COMMENT '请求体',
  `response_status` INT NULL COMMENT '响应状态码',
  `result` VARCHAR(255) NULL COMMENT '操作结果',
  `error_msg` VARCHAR(255) NULL COMMENT '错误信息',
  `duration_ms` INT NULL COMMENT '耗时(毫秒)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='审计日志表';

-- =============================================================
-- 计费记录表 (`billing_record`)
-- =============================================================
DROP TABLE IF EXISTS `billing_record`;
CREATE TABLE `billing_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 计费记录表',
  `record_id` VARCHAR(255) NULL COMMENT '计费记录业务ID',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `license_id` BIGINT NULL COMMENT '许可证ID',
  `model_entry_id` BIGINT NULL COMMENT '模型ID',
  `record_type` VARCHAR(255) NULL COMMENT '计费类型(token/call/subscription)',
  `amount_cents` BIGINT NULL COMMENT '金额(分)',
  `currency` VARCHAR(255) NULL COMMENT '币种(CNY/USD)',
  `status` VARCHAR(255) NULL COMMENT '状态(pending/paid/failed)',
  `payment_method` VARCHAR(255) NULL COMMENT '支付方式(alipay/wechat/card)',
  `external_transaction_id` VARCHAR(255) NULL COMMENT '外部交易号',
  `description` VARCHAR(255) NULL COMMENT '费用描述',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='计费记录表';

-- =============================================================
-- 集群节点表 (`cluster_node`)
-- =============================================================
DROP TABLE IF EXISTS `cluster_node`;
CREATE TABLE `cluster_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 集群节点表',
  `node_id` VARCHAR(255) NULL COMMENT '节点业务ID',
  `name` VARCHAR(255) NULL COMMENT '节点名称',
  `address` VARCHAR(255) NULL COMMENT '节点地址(host:port)',
  `region` VARCHAR(255) NULL COMMENT '地域',
  `zone` VARCHAR(255) NULL COMMENT '可用区',
  `capabilities` VARCHAR(255) NULL COMMENT '节点能力(JSON数组)',
  `total_cores` INT NULL COMMENT 'CPU总核心数',
  `total_memory_mb` BIGINT NULL COMMENT '总内存(MB)',
  `total_gpus` INT NULL COMMENT 'GPU数量',
  `cpu_usage` DOUBLE NULL COMMENT 'CPU使用率(0-100)',
  `memory_usage` DOUBLE NULL COMMENT '内存使用率(0-100)',
  `gpu_usage` DOUBLE NULL COMMENT 'GPU使用率(0-100)',
  `active_tasks` INT NULL COMMENT '当前活跃任务数',
  `status` VARCHAR(255) NULL COMMENT '节点状态(0离线/1在线/2维护)',
  `is_leader` TINYINT(1) NULL COMMENT '是否为Leader节点(0否/1是)',
  `last_heartbeat` TIMESTAMP NULL COMMENT '最近心跳时间',
  `started_at` TIMESTAMP NULL COMMENT '启动时间',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='集群节点表';

-- 种子数据
INSERT INTO `cluster_node` (id, node_name, host, port, status, cpu_usage, memory_usage, gpu_count, labels, created_at) VALUES
(1, 'node-01', '10.0.0.11', 8080, 'ACTIVE', 45.2, 62.1, 2, '{"role":"worker"}', NOW()),
(2, 'node-02', '10.0.0.12', 8080, 'ACTIVE', 38.7, 55.3, 2, '{"role":"worker"}', NOW());

-- =============================================================
-- 仪表盘指标快照表 (`dashboard_metric`)
-- =============================================================
DROP TABLE IF EXISTS `dashboard_metric`;
CREATE TABLE `dashboard_metric` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 仪表盘指标快照表',
  `metric` VARCHAR(255) NULL COMMENT '指标名称',
  `dimension` VARCHAR(255) NULL COMMENT '维度(标签)',
  `value` DOUBLE NULL COMMENT '指标值',
  `tags` VARCHAR(255) NULL COMMENT '额外标签(JSON)',
  `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='仪表盘指标快照表';

-- =============================================================
-- 数据源配置表 (`data_source`)
-- =============================================================
DROP TABLE IF EXISTS `data_source`;
CREATE TABLE `data_source` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 数据源配置表',
  `name` VARCHAR(255) NULL COMMENT '数据源名称',
  `type` VARCHAR(255) NULL COMMENT '数据源类型(mysql/postgresql/oracle)',
  `jdbc_url` VARCHAR(255) NULL COMMENT 'JDBC连接URL',
  `username` VARCHAR(255) NULL COMMENT '数据库用户名',
  `password` VARCHAR(255) NULL COMMENT '数据库密码(加密)',
  `driver_class` VARCHAR(255) NULL COMMENT 'JDBC驱动类名',
  `pool_size` INT NULL COMMENT '连接池大小',
  `min_idle` INT NULL COMMENT '最小空闲连接数',
  `max_lifetime` INT NULL COMMENT '最大生命周期(毫秒)',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `test_status` VARCHAR(255) NULL COMMENT '连接测试状态(0失败/1成功)',
  `test_message` VARCHAR(255) NULL COMMENT '测试消息',
  `last_test_at` TIMESTAMP NULL COMMENT '最近测试时间',
  `description` VARCHAR(255) NULL COMMENT '数据源描述',
  `tags` VARCHAR(255) NULL COMMENT '标签',
  `created_by` BIGINT NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建人ID',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='数据源配置表';

-- =============================================================
-- 知识库文本块表 (`kb_chunk`)
-- =============================================================
DROP TABLE IF EXISTS `kb_chunk`;
CREATE TABLE `kb_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 知识库文本块表',
  `chunk_id` VARCHAR(255) NULL COMMENT '文本块业务ID',
  `doc_id` VARCHAR(255) NULL COMMENT '所属文档ID',
  `kb_id` VARCHAR(255) NULL COMMENT '所属知识库ID',
  `seq` INT NULL COMMENT '块序号',
  `content` VARCHAR(255) NULL COMMENT '文本内容',
  `char_count` INT NULL COMMENT '字符数',
  `token_count` INT NULL COMMENT 'Token数',
  `embedding` VARCHAR(255) NULL COMMENT '向量Embedding',
  `embedding_model` VARCHAR(255) NULL COMMENT 'Embedding模型',
  `keywords` VARCHAR(255) NULL COMMENT '关键词(逗号分隔)',
  `summary` VARCHAR(255) NULL COMMENT '文本摘要',
  `location` VARCHAR(255) NULL COMMENT '文档中的位置',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='知识库文本块表';

-- =============================================================
-- 知识库文档表 (`kb_document`)
-- =============================================================
DROP TABLE IF EXISTS `kb_document`;
CREATE TABLE `kb_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 知识库文档表',
  `doc_id` VARCHAR(255) NULL COMMENT '文档业务ID',
  `kb_id` VARCHAR(255) NULL COMMENT '所属知识库ID',
  `filename` VARCHAR(255) NULL COMMENT '文件名',
  `mime_type` VARCHAR(255) NULL COMMENT 'MIME类型',
  `size_bytes` BIGINT NULL COMMENT '文件大小(字节)',
  `sha256` VARCHAR(255) NULL COMMENT '文件SHA256哈希',
  `file_path` VARCHAR(255) NULL COMMENT '存储路径',
  `source` VARCHAR(255) NULL COMMENT '来源(upload/url/crawl)',
  `source_url` VARCHAR(255) NULL COMMENT '来源URL',
  `status` VARCHAR(255) NULL COMMENT '状态(0处理中/1就绪/2失败)',
  `chunk_count` INT NULL COMMENT '切分块数',
  `embedding_count` INT NULL COMMENT '已Embedding块数',
  `error` VARCHAR(255) NULL COMMENT '错误信息',
  `tags` VARCHAR(255) NULL COMMENT '标签',
  `owner_id` BIGINT NULL COMMENT '所有者ID',
  `is_public` TINYINT(1) NULL COMMENT '是否公开(0私有/1公开)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='知识库文档表';

-- =============================================================
-- 知识库权限表 (`kb_permission`)
-- =============================================================
DROP TABLE IF EXISTS `kb_permission`;
CREATE TABLE `kb_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 知识库权限表',
  `kb_id` VARCHAR(255) NULL COMMENT '知识库ID',
  `subject_type` VARCHAR(255) NULL COMMENT '授权主体类型(user/role/tenant)',
  `subject_id` BIGINT NULL COMMENT '授权主体ID',
  `permission` VARCHAR(255) NULL COMMENT '权限(read/write/admin)',
  `grant_by` BIGINT NULL COMMENT '授权人ID',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='知识库权限表';

-- =============================================================
-- 模型许可证表 (`model_license`)
-- =============================================================
DROP TABLE IF EXISTS `model_license`;
CREATE TABLE `model_license` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 模型许可证表',
  `license_key` VARCHAR(255) NULL COMMENT '许可证Key',
  `model_entry_id` BIGINT NULL COMMENT '模型ID',
  `model_version_id` BIGINT NULL COMMENT '模型版本ID',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `license_type` VARCHAR(255) NULL COMMENT '许可证类型(trial/standard/enterprise)',
  `status` VARCHAR(255) NULL COMMENT '状态(active/expired/revoked)',
  `quota_calls` BIGINT NULL COMMENT '配额调用次数',
  `used_calls` BIGINT NULL COMMENT '已使用次数',
  `start_at` TIMESTAMP NULL COMMENT '生效时间',
  `expire_at` TIMESTAMP NULL COMMENT '过期时间',
  `price_cents` BIGINT NULL COMMENT '价格(分)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='模型许可证表';

-- =============================================================
-- 模型版本表 (`model_version`)
-- =============================================================
DROP TABLE IF EXISTS `model_version`;
CREATE TABLE `model_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 模型版本表',
  `version_id` VARCHAR(255) NULL COMMENT '版本业务ID',
  `model_entry_id` BIGINT NULL COMMENT '所属模型ID',
  `version` VARCHAR(255) NULL COMMENT '版本号(v1.0.0)',
  `changelog` VARCHAR(255) NULL COMMENT '版本变更说明',
  `file_path` VARCHAR(255) NULL COMMENT '模型文件路径',
  `size_bytes` BIGINT NULL COMMENT '文件大小(字节)',
  `sha256` VARCHAR(255) NULL COMMENT '文件SHA256哈希',
  `input_schema` VARCHAR(255) NULL COMMENT '输入Schema(JSON)',
  `output_schema` VARCHAR(255) NULL COMMENT '输出Schema(JSON)',
  `status` VARCHAR(255) NULL COMMENT '状态(0待上线/1上线/2废弃)',
  `is_latest` TINYINT(1) NULL COMMENT '是否最新版本(0否/1是)',
  `uploader_id` BIGINT NULL COMMENT '上传人ID',
  `backward_compatible` VARCHAR(255) NULL COMMENT '向后兼容性(0否/1是)',
  `metadata` VARCHAR(255) NULL COMMENT '元数据(JSON)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='模型版本表';

-- =============================================================
-- 内容审核记录表 (`moderation_record`)
-- =============================================================
DROP TABLE IF EXISTS `moderation_record`;
CREATE TABLE `moderation_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 内容审核记录表',
  `trace_id` VARCHAR(255) NULL COMMENT '追踪ID',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `username` VARCHAR(255) NULL COMMENT '用户名',
  `content_type` VARCHAR(255) NULL COMMENT '内容类型(text/image/audio/video)',
  `content_hash` VARCHAR(255) NULL COMMENT '内容哈希',
  `content_size` BIGINT NULL COMMENT '内容大小(字节)',
  `content_url` VARCHAR(255) NULL COMMENT '内容URL',
  `moderation_status` VARCHAR(255) NULL COMMENT '审核状态(pass/reject/review)',
  `risk_level` VARCHAR(255) NULL COMMENT '风险等级(0安全/1低风险/2中风险/3高风险)',
  `risk_labels` VARCHAR(255) NULL COMMENT '风险标签(JSON)',
  `risk_score` DECIMAL(20,4) NULL COMMENT '风险分数(0-1)',
  `moderator` VARCHAR(255) NULL COMMENT '审核员(ai/human)',
  `rejection_reason` VARCHAR(255) NULL COMMENT '驳回原因',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='内容审核记录表';

-- =============================================================
-- 多媒体文件表 (`multimedia_file`)
-- =============================================================
DROP TABLE IF EXISTS `multimedia_file`;
CREATE TABLE `multimedia_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 多媒体文件表',
  `file_id` VARCHAR(255) NULL COMMENT '文件业务ID',
  `user_id` BIGINT NULL COMMENT '上传用户ID',
  `username` VARCHAR(255) NULL COMMENT '用户名',
  `file_name` VARCHAR(255) NULL COMMENT '存储文件名',
  `original_name` VARCHAR(255) NULL COMMENT '原始文件名',
  `file_type` VARCHAR(255) NULL COMMENT '文件类型(image/audio/video/document)',
  `mime_type` VARCHAR(255) NULL COMMENT 'MIME类型',
  `file_size` BIGINT NULL COMMENT '文件大小(字节)',
  `file_hash` VARCHAR(255) NULL COMMENT '文件哈希(SHA256)',
  `storage_path` VARCHAR(255) NULL COMMENT '存储路径',
  `storage_type` VARCHAR(255) NULL COMMENT '存储类型(local/oss/s3)',
  `encrypted` INT NULL COMMENT '是否加密(0否/1是)',
  `duration_ms` BIGINT NULL COMMENT '音视频时长(毫秒)',
  `width` INT NULL COMMENT '宽度(像素)',
  `height` INT NULL COMMENT '高度(像素)',
  `bitrate` INT NULL COMMENT '比特率(kbps)',
  `sample_rate` INT NULL COMMENT '采样率(Hz)',
  `channels` INT NULL COMMENT '声道数(1单/2双)',
  `codec` VARCHAR(255) NULL COMMENT '编解码器',
  `exif` VARCHAR(255) NULL COMMENT 'EXIF信息(JSON)',
  `moderation_status` VARCHAR(255) NULL COMMENT '审核状态(pass/reject/pending)',
  `moderation_id` BIGINT NULL COMMENT '审核记录ID',
  `watermarked` INT NULL COMMENT '是否带水印(0否/1是)',
  `is_public` INT NULL COMMENT '是否公开(0私有/1公开)',
  `access_count` INT NULL COMMENT '访问次数',
  `expire_at` TIMESTAMP NULL COMMENT '过期时间',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='多媒体文件表';

-- =============================================================
-- 对话流水线日志表 (`pipeline_log`)
-- =============================================================
DROP TABLE IF EXISTS `pipeline_log`;
CREATE TABLE `pipeline_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 对话流水线日志表',
  `session_id` VARCHAR(255) NULL COMMENT '会话ID',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `client_ip` VARCHAR(255) NULL COMMENT '客户端IP',
  `input_text` VARCHAR(255) NULL COMMENT '用户输入',
  `input_modality` VARCHAR(255) NULL COMMENT '输入模态(text/image/audio)',
  `intent` VARCHAR(255) NULL COMMENT '识别意图',
  `output_text` VARCHAR(255) NULL COMMENT '输出文本',
  `output_tokens` INT NULL COMMENT '输出Token数',
  `compute_device` VARCHAR(255) NULL COMMENT '计算设备(CPU/GPU)',
  `compute_mode` VARCHAR(255) NULL COMMENT '计算模式(local/cloud)',
  `total_cost_ms` BIGINT NULL COMMENT '总耗时(毫秒)',
  `stage_costs` VARCHAR(255) NULL COMMENT '各阶段耗时(JSON)',
  `risk_level` VARCHAR(255) NULL COMMENT '风险等级(0安全/1低风险/2中风险/3高风险)',
  `needs_review` TINYINT(1) NULL COMMENT '是否需人工审核(0否/1是)',
  `rag_hits` INT NULL COMMENT 'RAG检索命中数',
  `tool_calls` INT NULL COMMENT '工具调用详情(JSON)',
  `error_message` VARCHAR(255) NULL COMMENT '错误信息',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='对话流水线日志表';

-- =============================================================
-- 推送消息表 (`push_message`)
-- =============================================================
DROP TABLE IF EXISTS `push_message`;
CREATE TABLE `push_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 推送消息表',
  `message_id` VARCHAR(255) NULL COMMENT '消息业务ID',
  `title` VARCHAR(255) NULL COMMENT '消息标题',
  `body` VARCHAR(255) NULL COMMENT '消息内容',
  `icon` VARCHAR(255) NULL COMMENT '图标URL',
  `click_action` VARCHAR(255) NULL COMMENT '点击行为(打开URL/App页面)',
  `data` VARCHAR(255) NULL COMMENT '扩展数据(JSON)',
  `target_type` VARCHAR(255) NULL COMMENT '推送目标类型(user/tag/all)',
  `target_value` VARCHAR(255) NULL COMMENT '推送目标值',
  `status` VARCHAR(255) NULL COMMENT '发送状态(0待发送/1发送中/2已发送/3失败)',
  `success_count` INT NULL COMMENT '成功数',
  `failure_count` INT NULL COMMENT '失败数',
  `error` VARCHAR(255) NULL COMMENT '错误信息',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='推送消息表';

-- =============================================================
-- WebPush订阅表 (`push_subscription`)
-- =============================================================
DROP TABLE IF EXISTS `push_subscription`;
CREATE TABLE `push_subscription` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - WebPush订阅表',
  `subscription_id` VARCHAR(255) NULL COMMENT '订阅业务ID',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `platform` VARCHAR(255) NULL COMMENT '平台(Chrome/Firefox/Safari)',
  `endpoint` VARCHAR(255) NULL COMMENT 'Push服务端点URL',
  `p256dh_key` VARCHAR(255) NULL COMMENT 'P256DH公钥',
  `auth_key` VARCHAR(255) NULL COMMENT '认证密钥',
  `user_agent` VARCHAR(255) NULL COMMENT '浏览器UA',
  `status` VARCHAR(255) NULL COMMENT '订阅状态(0失效/1有效)',
  `last_active_at` TIMESTAMP NULL COMMENT '最近活跃时间',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '订阅时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='WebPush订阅表';

-- =============================================================
-- Raft共识日志表 (`raft_log`)
-- =============================================================
DROP TABLE IF EXISTS `raft_log`;
CREATE TABLE `raft_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - Raft共识日志表',
  `term` BIGINT NULL COMMENT 'Raft任期',
  `log_index` BIGINT NULL COMMENT '日志索引',
  `node_id` VARCHAR(255) NULL COMMENT '节点ID',
  `command` VARCHAR(255) NULL COMMENT '命令(JSON)',
  `committed` TINYINT(1) NULL COMMENT '是否已提交(0否/1是)',
  `committed_at` TIMESTAMP NULL COMMENT '提交时间',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='Raft共识日志表';

-- =============================================================
-- 敏感词表 (`sensitive_word`)
-- =============================================================
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 敏感词表',
  `word` VARCHAR(255) NULL COMMENT '敏感词',
  `category` VARCHAR(255) NULL COMMENT '类别(politics/ads/porn/crime)',
  `level` VARCHAR(255) NULL COMMENT '风险等级(1低/2中/3高)',
  `action` VARCHAR(255) NULL COMMENT '处置动作(block/warn/audit)',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='敏感词表';

-- 种子数据
INSERT INTO `sensitive_word` (id, word, category, severity, action, created_at) VALUES
(1, '色情', '政治', 'HIGH', 'BLOCK', NOW()),
(2, '暴力', '暴力', 'MEDIUM', 'REVIEW', NOW());

-- =============================================================
-- 训练检查点表 (`training_checkpoint`)
-- =============================================================
DROP TABLE IF EXISTS `training_checkpoint`;
CREATE TABLE `training_checkpoint` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 训练检查点表',
  `task_id` VARCHAR(255) NULL COMMENT '训练任务ID',
  `checkpoint_id` VARCHAR(255) NULL COMMENT '检查点业务ID',
  `name` VARCHAR(255) NULL COMMENT '检查点名称',
  `epoch` INT NULL COMMENT '训练轮次(Epoch)',
  `step` INT NULL COMMENT '训练步数(Step)',
  `file_path` VARCHAR(255) NULL COMMENT '模型文件路径',
  `size_bytes` BIGINT NULL COMMENT '文件大小(字节)',
  `sha256` VARCHAR(255) NULL COMMENT '文件哈希',
  `val_loss` DOUBLE NULL COMMENT '验证损失值',
  `accuracy` DOUBLE NULL COMMENT '准确率(0-1)',
  `tags` VARCHAR(255) NULL COMMENT '标签(JSON)',
  `metadata` VARCHAR(255) NULL COMMENT '元数据(JSON)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='训练检查点表';

-- =============================================================
-- 训练任务表 (`training_job`)
-- =============================================================
DROP TABLE IF EXISTS `training_job`;
CREATE TABLE `training_job` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 训练任务表',
  `task_id` VARCHAR(255) NULL COMMENT '任务业务ID',
  `name` VARCHAR(255) NULL COMMENT '任务名称',
  `model` VARCHAR(255) NULL COMMENT '基础模型',
  `status` VARCHAR(255) NULL COMMENT '状态(pending/running/completed/failed/paused)',
  `total_epochs` INT NULL COMMENT '总训练轮次',
  `current_epoch` INT NULL COMMENT '当前轮次',
  `current_step` INT NULL COMMENT '当前步数',
  `start_time_ms` BIGINT NULL COMMENT '开始时间戳(毫秒)',
  `end_time_ms` BIGINT NULL COMMENT '结束时间戳(毫秒)',
  `config` VARCHAR(255) NULL COMMENT '训练配置(JSON)',
  `error` VARCHAR(255) NULL COMMENT '错误信息',
  `owner_id` BIGINT NULL COMMENT '创建人ID',
  `tags` VARCHAR(255) NULL COMMENT '标签(JSON)',
  `last_loss` DOUBLE NULL COMMENT '最新损失值',
  `last_val_loss` DOUBLE NULL COMMENT '最新验证损失值',
  `last_accuracy` DOUBLE NULL COMMENT '最新准确率(0-1)',
  `total_steps` INT NULL COMMENT '总步数',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='训练任务表';

-- =============================================================
-- 训练指标表(Job版) (`training_metric`)
-- =============================================================
DROP TABLE IF EXISTS `training_metric`;
CREATE TABLE `training_metric` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 训练指标表(Job版)',
  `task_id` VARCHAR(255) NULL COMMENT '训练任务ID',
  `epoch` INT NULL COMMENT '训练轮次',
  `step` INT NULL COMMENT '训练步数',
  `loss` DOUBLE NULL COMMENT '损失值',
  `val_loss` DOUBLE NULL COMMENT '验证损失值',
  `accuracy` DOUBLE NULL COMMENT '准确率(0-1)',
  `learning_rate` DOUBLE NULL COMMENT '学习率',
  `elapsed_ms` BIGINT NULL COMMENT '耗时(毫秒)',
  `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='训练指标表(Job版)';

-- 种子数据
INSERT INTO `training_metric` (id, task_id, step, loss, accuracy, learning_rate, elapsed_ms) VALUES
(1, 1, 100, 2.341, 0.623, 0.0003, 15200),
(2, 1, 200, 1.892, 0.701, 0.0003, 30400),
(3, 1, 300, 1.521, 0.758, 0.0003, 45600),
(4, 1, 400, 1.234, 0.801, 0.0003, 60800),
(5, 1, 500, 0.987, 0.845, 0.0003, 76000);

-- 种子数据
INSERT INTO `training_metric` (id, task_id, step, loss, accuracy, learning_rate, elapsed_ms) VALUES
(1, 1, 100, 2.341, 0.623, 0.0003, 15200),
(2, 1, 200, 1.892, 0.701, 0.0003, 30400),
(3, 1, 300, 1.521, 0.758, 0.0003, 45600),
(4, 1, 400, 1.234, 0.801, 0.0003, 60800),
(5, 1, 500, 0.987, 0.845, 0.0003, 76000);

-- =============================================================
-- 模型对比对战日志表 (`model_battle_log`)
-- =============================================================
DROP TABLE IF EXISTS `model_battle_log`;
CREATE TABLE `model_battle_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 模型对比对战日志表',
  `battle_id` VARCHAR(255) NULL COMMENT '对战ID',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `model_id` BIGINT NULL COMMENT '模型ID',
  `model_code` VARCHAR(255) NULL COMMENT '模型代码',
  `prompt` VARCHAR(255) NULL COMMENT '对战Prompt',
  `response` VARCHAR(255) NULL COMMENT '模型回复',
  `prompt_tokens` INT NULL COMMENT 'Prompt Token数',
  `completion_tokens` INT NULL COMMENT '回复Token数',
  `latency_ms` INT NULL COMMENT '推理延迟(毫秒)',
  `status` VARCHAR(255) NULL COMMENT '状态(0失败/1成功)',
  `error_msg` VARCHAR(255) NULL COMMENT '错误信息',
  `score` INT NULL COMMENT '对战评分(1-5)',
  `judge_model` VARCHAR(255) NULL COMMENT '评判模型',
  `judge_reason` VARCHAR(255) NULL COMMENT '评判理由',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '对战时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='模型对比对战日志表';

-- =============================================================
-- 模型配置表 (`model_config`)
-- =============================================================
DROP TABLE IF EXISTS `model_config`;
CREATE TABLE `model_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 模型配置表',
  `provider_id` BIGINT NULL COMMENT '供应商ID',
  `model_code` VARCHAR(255) NULL COMMENT '模型代码',
  `display_name` VARCHAR(255) NULL COMMENT '显示名称',
  `max_context` INT NULL COMMENT '最大上下文Token数',
  `max_output` INT NULL COMMENT '最大输出Token数',
  `input_price` DECIMAL(20,4) NULL COMMENT '输入价格(元/千Token)',
  `output_price` DECIMAL(20,4) NULL COMMENT '输出价格(元/千Token)',
  `supports_vision` INT NULL COMMENT '是否支持视觉(0否/1是)',
  `supports_tools` INT NULL COMMENT '是否支持工具调用(0否/1是)',
  `supports_stream` INT NULL COMMENT '是否支持流式(0否/1是)',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `sort` INT NULL COMMENT '排序权重',
  `description` VARCHAR(255) NULL COMMENT '模型描述',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='模型配置表';

-- 种子数据
INSERT INTO `model_config` (id, provider_id, model_code, display_name, max_context, enabled, supports_stream, supports_tools, input_price, output_price) VALUES
(1, 1, 'gpt-4o-mini', 'GPT-4o Mini', 128000, 1, 1, 1, 0.15, 0.6),
(2, 1, 'gpt-4o', 'GPT-4o', 128000, 1, 1, 1, 2.5, 10.0),
(3, 2, 'deepseek-chat', 'DeepSeek V3', 64000, 1, 1, 1, 0.1, 0.3),
-- V6.8.2: 训练模型 (provider=4, 内部路由到 deepseek-chat + system prompt)
(10, 4, 'minimax-01',     'MiniMax-01',     32000, 1, 1, 0, 0.0, 0.0),
(11, 4, 'minimax-02',     'MiniMax-02',     32000, 1, 1, 0, 0.0, 0.0),
(12, 4, 'minimax-vision', 'MiniMax-Vision', 32000, 1, 1, 1, 0.0, 0.0),
(13, 4, 'law-gpt-v1',     'Law-GPT',        32000, 1, 1, 0, 0.0, 0.0),
(14, 4, 'med-gpt-v2',     'Med-GPT',        32000, 1, 1, 0, 0.0, 0.0),
(15, 4, 'fin-gpt-v1',     'Fin-GPT',        32000, 1, 1, 0, 0.0, 0.0),
(16, 4, 'code-gpt-v1',    'Code-GPT',       32000, 1, 1, 0, 0.0, 0.0),
(17, 4, 'chat-gpt-v3',    'Chat-GPT',       32000, 1, 1, 0, 0.0, 0.0),
(18, 4, 'qa-gpt-v2',      'QA-GPT',         32000, 1, 1, 0, 0.0, 0.0);

-- =============================================================
-- 模型供应商表 (`model_provider`)
-- =============================================================
DROP TABLE IF EXISTS `model_provider`;
CREATE TABLE `model_provider` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 模型供应商表',
  `code` VARCHAR(255) NULL COMMENT '供应商代码(openai/anthropic/deepseek等)',
  `name` VARCHAR(255) NULL COMMENT '供应商名称',
  `base_url` VARCHAR(255) NULL COMMENT 'API基础地址',
  `api_key` VARCHAR(255) NULL COMMENT 'API密钥(加密)',
  `protocol` VARCHAR(255) NULL COMMENT '协议(openai-compatible/custom)',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `sort` INT NULL COMMENT '排序权重',
  `description` VARCHAR(255) NULL COMMENT '供应商描述',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='模型供应商表';

-- 种子数据
INSERT INTO `model_provider` (id, code, name, protocol, base_url, enabled, sort, created_at) VALUES
(1, 'openai', 'OpenAI', 'openai', 'https://api.openai.com', 1, 1, NOW()),
(2, 'deepseek', 'DeepSeek', 'openai', 'https://api.deepseek.com', 1, 2, NOW()),
(3, 'local-ollama', 'Ollama 本地', 'local', 'http://localhost:11434', 1, 3, NOW()),
(4, 'trained', '训练模型', 'openai', 'https://api.deepseek.com', 1, 4, NOW());

-- =============================================================
-- 用户模型配额表 (`model_quota`)
-- =============================================================
DROP TABLE IF EXISTS `model_quota`;
CREATE TABLE `model_quota` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 用户模型配额表',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `model_id` BIGINT NULL COMMENT '模型ID',
  `quota_date` DATE NULL COMMENT '配额统计日期',
  `used_tokens` BIGINT NULL COMMENT '已用Token数',
  `used_requests` INT NULL COMMENT '已用请求次数',
  `limit_tokens` BIGINT NULL COMMENT 'Token额度',
  `limit_requests` INT NULL COMMENT '请求额度',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='用户模型配额表';

-- 种子数据
INSERT INTO `model_quota` (id, user_id, model_name, quota_limit, quota_used, created_at) VALUES
(1, 1, 'gpt-4o', 100000, 2340, NOW()),
(2, 2, 'deepseek-chat', 50000, 8920, NOW());

-- =============================================================
-- Prompt模板表 (`prompt_template`)
-- =============================================================
DROP TABLE IF EXISTS `prompt_template`;
CREATE TABLE `prompt_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - Prompt模板表',
  `name` VARCHAR(255) NULL COMMENT '模板名称',
  `description` VARCHAR(255) NULL COMMENT '模板描述',
  `category` VARCHAR(255) NULL COMMENT '分类(agent/rag/classification等)',
  `content` VARCHAR(255) NULL COMMENT '模板内容',
  `variables` VARCHAR(255) NULL COMMENT '变量定义(JSON)',
  `creator_id` BIGINT NULL COMMENT '创建人ID',
  `creator_name` VARCHAR(255) NULL COMMENT '创建人名称',
  `is_public` TINYINT(1) NULL COMMENT '是否公开(0私有/1公开)',
  `use_count` INT NULL COMMENT '使用次数',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='Prompt模板表';

-- 种子数据
INSERT INTO `prompt_template` (id, name, description, prompt, model_type, tags, created_at) VALUES
(1, '客服开场白', '标准客服开场白', '您好，我是 AI 助手，请问有什么可以帮助您的？', 'gpt-4o-mini', '["客服","开场"]', NOW()),
(2, '代码审查', '代码审查 prompt', '请审查以下代码，找出潜在问题：', 'gpt-4o', '["开发","审查"]', NOW());

-- =============================================================
-- 训练指标表(Job版) (`training_metric`)
-- =============================================================
DROP TABLE IF EXISTS `training_metric`;
CREATE TABLE `training_metric` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 训练指标表(Job版)',
  `task_id` BIGINT NULL COMMENT '训练任务ID',
  `iter` INT NULL COMMENT '迭代次数',
  `loss` DOUBLE NULL COMMENT '损失值',
  `accuracy` DOUBLE NULL COMMENT '准确率(0-1)',
  `progress` INT NULL COMMENT '进度百分比',
  `lr` VARCHAR(255) NULL COMMENT '学习率',
  `gpu_util` INT NULL COMMENT 'GPU利用率(0-100%)',
  `vram_gb` DOUBLE NULL COMMENT '显存占用(GB)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='训练指标表(Job版)';

-- 种子数据
INSERT INTO `training_metric` (id, task_id, step, loss, accuracy, learning_rate, elapsed_ms) VALUES
(1, 1, 100, 2.341, 0.623, 0.0003, 15200),
(2, 1, 200, 1.892, 0.701, 0.0003, 30400),
(3, 1, 300, 1.521, 0.758, 0.0003, 45600),
(4, 1, 400, 1.234, 0.801, 0.0003, 60800),
(5, 1, 500, 0.987, 0.845, 0.0003, 76000);

-- 种子数据
INSERT INTO `training_metric` (id, task_id, step, loss, accuracy, learning_rate, elapsed_ms) VALUES
(1, 1, 100, 2.341, 0.623, 0.0003, 15200),
(2, 1, 200, 1.892, 0.701, 0.0003, 30400),
(3, 1, 300, 1.521, 0.758, 0.0003, 45600),
(4, 1, 400, 1.234, 0.801, 0.0003, 60800),
(5, 1, 500, 0.987, 0.845, 0.0003, 76000);

-- =============================================================
-- 训练任务表(Task版) (`training_task`)
-- =============================================================
DROP TABLE IF EXISTS `training_task`;
CREATE TABLE `training_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 训练任务表(Task版)',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `model_name` VARCHAR(255) NULL COMMENT '模型名称',
  `corpus_path` VARCHAR(255) NULL COMMENT '语料库路径',
  `n_layer` INT NULL COMMENT 'Transformer层数',
  `n_head` INT NULL COMMENT '注意力头数',
  `n_embd` INT NULL COMMENT 'Embedding维度',
  `block_size` INT NULL COMMENT '上下文窗口大小',
  `max_iters` INT NULL COMMENT '最大迭代次数',
  `batch_size` INT NULL COMMENT '批次大小',
  `learning_rate` DOUBLE NULL COMMENT '学习率',
  `status` VARCHAR(255) NULL COMMENT '状态(preparing/running/finished/failed)',
  `progress` INT NULL COMMENT '进度百分比',
  `current_loss` DOUBLE NULL COMMENT '当前损失值',
  `current_iter` INT NULL COMMENT '当前迭代次数',
  `error_message` VARCHAR(255) NULL COMMENT '错误信息',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `completed_at` TIMESTAMP NULL COMMENT '完成时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='训练任务表(Task版)';

-- 种子数据
INSERT INTO `training_task` (id, user_id, model_name, corpus_path, status, progress, created_at) VALUES
(1, 1, 'chatglm-6b', 'chat通用', 'COMPLETED', 100, NOW()),
(2, 1, 'llama-2-7b', 'code', 'TRAINING', 67, NOW()),
(3, 2, 'qwen-7b', '客服', 'PENDING', 0, NOW());

-- =============================================================
-- 告警渠道表 (`alert_channel`)
-- =============================================================
DROP TABLE IF EXISTS `alert_channel`;
CREATE TABLE `alert_channel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 告警渠道表',
  `name` VARCHAR(255) NULL COMMENT '渠道名称',
  `channel_type` VARCHAR(255) NULL COMMENT '渠道类型(email/dingtalk/sms/webhook)',
  `type` VARCHAR(255) NULL COMMENT '类型(alias同channel_type)',
  `target` VARCHAR(255) NULL COMMENT '目标地址/账号',
  `config` VARCHAR(255) NULL COMMENT '配置信息(JSON)',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `priority` INT NULL COMMENT '优先级(1低/2中/3高)',
  `description` VARCHAR(255) NULL COMMENT '描述',
  `template` VARCHAR(255) NULL COMMENT '消息模板',
  `created_by` BIGINT NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建人ID',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='告警渠道表';

-- =============================================================
-- 告警事件表 (`alert_event`)
-- =============================================================
DROP TABLE IF EXISTS `alert_event`;
CREATE TABLE `alert_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 告警事件表',
  `rule_id` BIGINT NULL COMMENT '规则ID',
  `rule_name` VARCHAR(255) NULL COMMENT '规则名称',
  `severity` VARCHAR(255) NULL COMMENT '严重程度(critical/high/medium/low)',
  `metric_name` VARCHAR(255) NULL COMMENT '指标名称',
  `metric_value` DECIMAL(20,4) NULL COMMENT '指标值',
  `threshold` DECIMAL(20,4) NULL COMMENT '阈值',
  `message` VARCHAR(255) NULL COMMENT '告警消息',
  `status` VARCHAR(255) NULL COMMENT '状态(firing/resolved/acknowledged)',
  `fired_at` TIMESTAMP NULL COMMENT '触发时间',
  `resolved_at` TIMESTAMP NULL COMMENT '解决时间',
  `acked_at` TIMESTAMP NULL COMMENT '确认时间',
  `acked_by` BIGINT NULL COMMENT '确认人',
  `notes` VARCHAR(255) NULL COMMENT '备注',
  `duration` BIGINT NULL COMMENT '持续时长(秒)',
  `silenced_until` TIMESTAMP NULL COMMENT '静默截止时间',
  `escalated` TINYINT(1) NULL DEFAULT 0 COMMENT '是否已升级(0否/1是)',
  `escalated_at` TIMESTAMP NULL COMMENT '升级时间',
  `resolved_by` VARCHAR(100) NULL COMMENT '解决人',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='告警事件表';

-- =============================================================
-- 告警规则表 (`alert_rule`)
-- =============================================================
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE `alert_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 告警规则表',
  `name` VARCHAR(255) NULL COMMENT '规则名称',
  `description` VARCHAR(255) NULL COMMENT '规则描述',
  `metric_name` VARCHAR(255) NULL COMMENT '监控指标名称',
  `service` VARCHAR(255) NULL COMMENT '服务名称',
  `operator` VARCHAR(255) NULL COMMENT '比较操作符(>/</>=/<=/==)',
  `threshold` DECIMAL(20,4) NULL COMMENT '阈值',
  `severity` VARCHAR(255) NULL COMMENT '严重程度(critical/high/medium/low)',
  `cooldown_minutes` INT NULL COMMENT '冷却时间(分钟)',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `tags` VARCHAR(255) NULL COMMENT '标签',
  `notify_channel` VARCHAR(255) NULL COMMENT '通知渠道(JSON数组)',
  `silenced_until` TIMESTAMP NULL COMMENT '静默截止时间',
  `escalate_after_minutes` INT NULL COMMENT '升级等待分钟数',
  `escalation_channel` VARCHAR(255) NULL COMMENT '升级渠道',
  `auto_resolve_minutes` INT NULL COMMENT '自动恢复分钟数',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='告警规则表';

-- 种子数据
INSERT INTO `alert_rule` (id, name, metric, threshold, operator, severity, enabled, created_at) VALUES
(1, 'CPU 过高告警', 'cpu_usage', 80, '>', 'WARNING', 1, NOW()),
(2, '内存过高告警', 'memory_usage', 85, '>', 'WARNING', 1, NOW()),
(3, '错误率过高', 'error_rate', 5, '>', 'CRITICAL', 1, NOW());

-- =============================================================
-- 指标快照表 (`metric_snapshot`)
-- =============================================================
DROP TABLE IF EXISTS `metric_snapshot`;
CREATE TABLE `metric_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 指标快照表',
  `service` VARCHAR(255) NULL COMMENT '服务名称',
  `metric_name` VARCHAR(255) NULL COMMENT '指标名称',
  `metric_value` DECIMAL(20,4) NULL COMMENT '指标值',
  `tags` VARCHAR(255) NULL COMMENT '标签(JSON)',
  `recorded_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='指标快照表';

-- =============================================================
-- Function调用日志表 (`function_call_log`)
-- =============================================================
DROP TABLE IF EXISTS `function_call_log`;
CREATE TABLE `function_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - Function调用日志表',
  `user_id` BIGINT NULL COMMENT '调用用户ID',
  `session_id` BIGINT NULL COMMENT '会话ID',
  `tool_name` VARCHAR(255) NULL COMMENT '工具名称',
  `arguments` VARCHAR(255) NULL COMMENT '调用参数(JSON)',
  `result` VARCHAR(255) NULL COMMENT '调用结果(JSON)',
  `status` VARCHAR(255) NULL COMMENT '状态(0失败/1成功)',
  `error_msg` VARCHAR(255) NULL COMMENT '错误信息',
  `duration_ms` INT NULL COMMENT '调用耗时(毫秒)',
  `ip` VARCHAR(255) NULL COMMENT '调用方IP',
  `user_agent` VARCHAR(255) NULL COMMENT '调用方UA',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='Function调用日志表';

-- =============================================================
-- Function工具表 (`function_tool`)
-- =============================================================
DROP TABLE IF EXISTS `function_tool`;
CREATE TABLE `function_tool` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - Function工具表',
  `name` VARCHAR(255) NULL COMMENT '工具名称(英文)',
  `display_name` VARCHAR(255) NULL COMMENT '显示名称(中文)',
  `description` VARCHAR(255) NULL COMMENT '工具描述',
  `category` VARCHAR(255) NULL COMMENT '类别(search/calc/code等)',
  `scope` VARCHAR(255) NULL COMMENT '作用域(public/private/team)',
  `owner_id` BIGINT NULL COMMENT '拥有者ID',
  `parameters` VARCHAR(255) NULL COMMENT '参数定义(JSON Schema)',
  `endpoint` VARCHAR(255) NULL COMMENT 'API端点',
  `http_method` VARCHAR(255) NULL COMMENT 'HTTP方法(GET/POST)',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `tags` VARCHAR(255) NULL COMMENT '标签',
  `risk_level` VARCHAR(255) NULL COMMENT '风险等级(0低/1中/2高)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='Function工具表';

-- 种子数据
INSERT INTO `function_tool` (id, name, description, enabled, category, risk_level, created_at) VALUES
(1, 'search_web', '搜索互联网', 1, 'search', 'LOW', NOW()),
(2, 'calculator', '数学计算', 1, 'utility', 'LOW', NOW()),
(3, 'file_reader', '读取本地文件', 1, 'file', 'MEDIUM', NOW()),
(4, 'code_executor', '执行代码', 1, 'code', 'HIGH', NOW()),
(5, 'sql_query', '数据库查询', 1, 'database', 'CRITICAL', NOW());

-- =============================================================
-- 流水线节点日志表 (`pipeline_node_log`)
-- =============================================================
DROP TABLE IF EXISTS `pipeline_node_log`;
CREATE TABLE `pipeline_node_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 流水线节点日志表',
  `run_id` BIGINT NULL COMMENT '运行ID',
  `node_id` VARCHAR(255) NULL COMMENT '节点ID',
  `node_type` VARCHAR(255) NULL COMMENT '节点类型',
  `node_name` VARCHAR(255) NULL COMMENT '节点名称',
  `status` VARCHAR(255) NULL COMMENT '状态(running/success/failed)',
  `start_time` TIMESTAMP NULL COMMENT '开始时间',
  `end_time` TIMESTAMP NULL COMMENT '结束时间',
  `duration_ms` BIGINT NULL COMMENT '耗时(毫秒)',
  `input_rows` INT NULL COMMENT '输入行数',
  `output_rows` INT NULL COMMENT '输出行数',
  `output_preview` VARCHAR(255) NULL COMMENT '输出预览(前100行JSON)',
  `error_message` VARCHAR(255) NULL COMMENT '错误信息',
  `config_snapshot` VARCHAR(255) NULL COMMENT '配置快照(JSON)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='流水线节点日志表';

-- =============================================================
-- 流水线运行表 (`pipeline_run`)
-- =============================================================
DROP TABLE IF EXISTS `pipeline_run`;
CREATE TABLE `pipeline_run` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 流水线运行表',
  `workflow_id` BIGINT NULL COMMENT '工作流ID',
  `workflow_name` VARCHAR(255) NULL COMMENT '工作流名称',
  `status` VARCHAR(255) NULL COMMENT '状态(pending/running/success/failed)',
  `trigger_by` BIGINT NULL COMMENT '触发人',
  `trigger_type` VARCHAR(255) NULL COMMENT '触发类型(manual/schedule/webhook)',
  `definition_snapshot` VARCHAR(255) NULL COMMENT '定义快照(JSON)',
  `start_time` TIMESTAMP NULL COMMENT '开始时间',
  `end_time` TIMESTAMP NULL COMMENT '结束时间',
  `duration_ms` BIGINT NULL COMMENT '耗时(毫秒)',
  `error_message` VARCHAR(255) NULL COMMENT '错误信息',
  `result_summary` VARCHAR(255) NULL COMMENT '结果摘要(JSON)',
  `create_time` TIMESTAMP NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='流水线运行表';

-- =============================================================
-- 流水线工作流表 (`pipeline_workflow`)
-- =============================================================
DROP TABLE IF EXISTS `pipeline_workflow`;
CREATE TABLE `pipeline_workflow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 流水线工作流表',
  `name` VARCHAR(255) NULL COMMENT '工作流名称',
  `description` VARCHAR(255) NULL COMMENT '工作流描述',
  `definition` VARCHAR(255) NULL COMMENT '流程定义(JSON)',
  `version` INT NULL COMMENT '版本号',
  `status` INT NULL COMMENT '状态(draft/published/archived)',
  `create_by` BIGINT NULL COMMENT '创建人ID',
  `update_by` BIGINT NULL COMMENT '更新人ID',
  `create_time` TIMESTAMP NULL COMMENT '创建时间',
  `update_time` TIMESTAMP NULL COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='流水线工作流表';

-- 种子数据
INSERT INTO `pipeline_workflow` (id, name, description, version, status, owner_id, created_at) VALUES
(1, 'RAG Pipeline', '检索增强生成流程', 1, 'ACTIVE', 1, NOW()),
(2, '客服分流', '多客服智能分流', 1, 'DRAFT', 1, NOW());

-- =============================================================
-- 工作流版本表 (`pipeline_workflow_version`)
-- =============================================================
DROP TABLE IF EXISTS `pipeline_workflow_version`;
CREATE TABLE `pipeline_workflow_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 工作流版本表',
  `workflow_id` BIGINT NULL COMMENT '所属工作流ID',
  `version` INT NULL COMMENT '版本号',
  `definition` VARCHAR(255) NULL COMMENT '流程定义(JSON)',
  `change_log` VARCHAR(255) NULL COMMENT '变更日志',
  `create_by` BIGINT NULL COMMENT '创建人ID',
  `create_time` TIMESTAMP NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='工作流版本表';

-- =============================================================
-- Skill审批表 (`skill_approval`)
-- =============================================================
DROP TABLE IF EXISTS `skill_approval`;
CREATE TABLE `skill_approval` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - Skill审批表',
  `task_id` VARCHAR(255) NULL COMMENT '任务ID',
  `user_id` BIGINT NULL COMMENT '申请人ID',
  `username` VARCHAR(255) NULL COMMENT '申请人用户名',
  `tool_name` VARCHAR(255) NULL COMMENT '工具名称',
  `risk_level` VARCHAR(255) NULL COMMENT '风险等级(low/medium/high/critical)',
  `goal` VARCHAR(255) NULL COMMENT '使用目标描述',
  `tool_params` VARCHAR(255) NULL COMMENT '工具参数(JSON)',
  `status` VARCHAR(255) NULL COMMENT '审批状态(pending/approved/rejected)',
  `approver_id` BIGINT NULL COMMENT '审批人ID',
  `approver_name` VARCHAR(255) NULL COMMENT '审批人名称',
  `reason` VARCHAR(255) NULL COMMENT '审批意见',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='Skill审批表';

-- 种子数据
INSERT INTO `skill_approval` (id, task_id, skill_name, risk_level, status, requested_by, created_at) VALUES
(1, 1, 'sql_query', 'CRITICAL', 'APPROVED', 1, NOW()),
(2, 2, 'file_reader', 'MEDIUM', 'PENDING', 2, NOW());

-- =============================================================
-- 通用文档表 (`document`)
-- =============================================================
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 通用文档表',
  `kb_id` BIGINT NULL COMMENT '所属知识库ID',
  `owner_id` BIGINT NULL COMMENT '所有者ID',
  `title` VARCHAR(255) NULL COMMENT '文档标题',
  `source_type` VARCHAR(255) NULL COMMENT '来源类型(file/url/api)',
  `source_uri` VARCHAR(255) NULL COMMENT '来源URI',
  `content` VARCHAR(255) NULL COMMENT '文档内容',
  `size_bytes` BIGINT NULL COMMENT '文件大小(字节)',
  `status` VARCHAR(255) NULL COMMENT '状态(uploading/processing/ready/failed)',
  `error_msg` VARCHAR(255) NULL COMMENT '错误信息',
  `chunk_count` INT NULL COMMENT '切分块数',
  `checksum` VARCHAR(255) NULL COMMENT '内容校验和',
  `tags` VARCHAR(255) NULL COMMENT '标签',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='通用文档表';

-- =============================================================
-- 文档块表 (`document_chunk`)
-- =============================================================
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 文档块表',
  `doc_id` BIGINT NULL COMMENT '所属文档ID',
  `kb_id` BIGINT NULL COMMENT '所属知识库ID',
  `owner_id` BIGINT NULL COMMENT '所有者ID',
  `chunk_index` INT NULL COMMENT '块序号(从0开始)',
  `content` VARCHAR(255) NULL COMMENT '块内容',
  `embedding` VARCHAR(255) NULL COMMENT '向量Embedding',
  `dim` INT NULL COMMENT '向量维度',
  `char_count` INT NULL COMMENT '字符数',
  `start_pos` INT NULL COMMENT '原文起始位置',
  `end_pos` INT NULL COMMENT '原文结束位置',
  `access_count` INT NULL COMMENT '访问次数',
  `last_access_at` TIMESTAMP NULL COMMENT '最近访问时间',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='文档块表';

-- =============================================================
-- 知识库表 (`knowledge_base`)
-- =============================================================
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 知识库表',
  `owner_id` BIGINT NULL COMMENT '拥有者ID',
  `tenant_id` BIGINT NULL COMMENT '租户ID',
  `name` VARCHAR(255) NULL COMMENT '知识库名称',
  `description` VARCHAR(255) NULL COMMENT '知识库描述',
  `visibility` VARCHAR(255) NULL COMMENT '可见性(private/public/team)',
  `doc_count` INT NULL COMMENT '文档数',
  `chunk_count` INT NULL COMMENT '块总数',
  `tags` VARCHAR(255) NULL COMMENT '标签',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='知识库表';

-- 种子数据
INSERT INTO `knowledge_base` (id, owner_id, name, description, visibility, created_at) VALUES
(1, 1, '产品文档', '内部产品文档知识库', 'PRIVATE', NOW()),
(2, 1, '技术文档', '技术文档知识库', 'PRIVATE', NOW()),
(3, 2, '公开知识库', '公共知识库', 'PUBLIC', NOW());

-- =============================================================
-- 分析数据源表 (`analytics_datasource`)
-- =============================================================
DROP TABLE IF EXISTS `analytics_datasource`;
CREATE TABLE `analytics_datasource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 分析数据源表',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `name` VARCHAR(255) NULL COMMENT '数据源名称',
  `type` VARCHAR(255) NULL COMMENT '数据源类型(mysql/postgresql/clickhouse)',
  `jdbc_url` VARCHAR(255) NULL COMMENT 'JDBC连接URL',
  `username` VARCHAR(255) NULL COMMENT '用户名',
  `password_enc` VARCHAR(255) NULL COMMENT '密码(加密)',
  `description` VARCHAR(255) NULL COMMENT '描述',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='分析数据源表';

-- =============================================================
-- 数据导入任务表 (`analytics_ingest_task`)
-- =============================================================
DROP TABLE IF EXISTS `analytics_ingest_task`;
CREATE TABLE `analytics_ingest_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 数据导入任务表',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `task_id` VARCHAR(255) NULL COMMENT '任务业务ID',
  `filename` VARCHAR(255) NULL COMMENT '文件名',
  `file_type` VARCHAR(255) NULL COMMENT '文件类型(csv/excel/json)',
  `encoding` VARCHAR(255) NULL COMMENT '文件编码(UTF-8/GBK)',
  `separator` VARCHAR(255) NULL COMMENT '列分隔符',
  `file_size` BIGINT NULL COMMENT '文件大小(字节)',
  `status` VARCHAR(255) NULL COMMENT '状态(pending/processing/done/failed)',
  `error_message` VARCHAR(255) NULL COMMENT '错误信息',
  `quality_json` VARCHAR(255) NULL COMMENT '数据质量报告(JSON)',
  `total_rows` BIGINT NULL COMMENT '总行数',
  `total_columns` BIGINT NULL COMMENT '总列数',
  `columns_json` VARCHAR(255) NULL COMMENT '列信息(JSON)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `finished_at` TIMESTAMP NULL COMMENT '完成时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='数据导入任务表';

-- =============================================================
-- NL2SQL历史表 (`analytics_nlsql_history`)
-- =============================================================
DROP TABLE IF EXISTS `analytics_nlsql_history`;
CREATE TABLE `analytics_nlsql_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - NL2SQL历史表',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `data_source_id` BIGINT NULL COMMENT '数据源ID',
  `question` VARCHAR(255) NULL COMMENT '自然语言问题',
  `generated_sql` VARCHAR(255) NULL COMMENT '生成的SQL',
  `corrected_sql` VARCHAR(255) NULL COMMENT '修正后SQL',
  `model` VARCHAR(255) NULL COMMENT '使用的模型',
  `prompt_tokens` INT NULL COMMENT 'Prompt Token数',
  `completion_tokens` INT NULL COMMENT '回复Token数',
  `duration_ms` BIGINT NULL COMMENT '耗时(毫秒)',
  `success` TINYINT(1) NULL COMMENT '是否成功(0失败/1成功)',
  `error_message` VARCHAR(255) NULL COMMENT '错误信息',
  `feedback_rating` INT NULL COMMENT '用户评分(1-5)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='NL2SQL历史表';

-- =============================================================
-- 分析报告表 (`analytics_report`)
-- =============================================================
DROP TABLE IF EXISTS `analytics_report`;
CREATE TABLE `analytics_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 分析报告表',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `report_id` VARCHAR(255) NULL COMMENT '报告业务ID',
  `title` VARCHAR(255) NULL COMMENT '报告标题',
  `question` VARCHAR(255) NULL COMMENT '原始问题',
  `sql_text` VARCHAR(255) NULL COMMENT '执行的SQL',
  `markdown` VARCHAR(255) NULL COMMENT '报告内容(Markdown)',
  `chart_options_json` VARCHAR(255) NULL COMMENT '图表配置(JSON)',
  `row_count` BIGINT NULL COMMENT '结果行数',
  `duration_ms` BIGINT NULL COMMENT '生成耗时(毫秒)',
  `format` VARCHAR(255) NULL COMMENT '报告格式(html/pdf/markdown)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='分析报告表';

-- =============================================================
-- Agent任务表 (`agent_task`)
-- =============================================================
DROP TABLE IF EXISTS `agent_task`;
CREATE TABLE `agent_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - Agent任务表',
  `task_id` VARCHAR(255) NULL COMMENT '任务业务ID',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `goal` VARCHAR(255) NULL COMMENT '任务目标',
  `status` VARCHAR(255) NULL COMMENT '状态(pending/running/completed/failed)',
  `rounds` INT NULL COMMENT '执行轮次',
  `result` VARCHAR(255) NULL COMMENT '执行结果(JSON)',
  `llm_calls` INT NULL COMMENT 'LLM调用次数',
  `tool_calls` INT NULL COMMENT '工具调用次数',
  `total_tokens` INT NULL COMMENT '总Token数',
  `error_msg` VARCHAR(255) NULL COMMENT '错误信息',
  `latency_ms` BIGINT NULL COMMENT '总耗时(毫秒)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='Agent任务表';

-- =============================================================
-- 协作成员表 (`collab_member`)
-- =============================================================
DROP TABLE IF EXISTS `collab_member`;
CREATE TABLE `collab_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 协作成员表',
  `collab_id` BIGINT NULL COMMENT '协作空间ID',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `role` VARCHAR(255) NULL COMMENT '角色(owner/editor/viewer)',
  `joined_at` TIMESTAMP NULL COMMENT '加入时间',
  `last_active_at` TIMESTAMP NULL COMMENT '最近活跃时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='协作成员表';

-- =============================================================
-- 协作会话表 (`collab_session`)
-- =============================================================
DROP TABLE IF EXISTS `collab_session`;
CREATE TABLE `collab_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 协作会话表',
  `session_id` VARCHAR(255) NULL COMMENT '会话业务ID',
  `owner_id` BIGINT NULL COMMENT '创建人ID',
  `title` VARCHAR(255) NULL COMMENT '会话标题',
  `max_users` INT NULL COMMENT '最大用户数',
  `status` VARCHAR(255) NULL COMMENT '状态(active/archived)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='协作会话表';

-- =============================================================
-- 知识图谱实体表 (`kg_entity`)
-- =============================================================
DROP TABLE IF EXISTS `kg_entity`;
CREATE TABLE `kg_entity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 知识图谱实体表',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `name` VARCHAR(255) NULL COMMENT '实体名称',
  `entity_type` VARCHAR(255) NULL COMMENT '实体类型(person/organization/location/product/event)',
  `description` VARCHAR(255) NULL COMMENT '实体描述',
  `aliases` VARCHAR(255) NULL COMMENT '别名(逗号分隔)',
  `importance` INT NULL COMMENT '重要度(1-10)',
  `source` VARCHAR(255) NULL COMMENT '来源(manual/import/ai)',
  `ref_count` INT NULL COMMENT '被引用次数',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='知识图谱实体表';

-- =============================================================
-- 知识图谱关系表 (`kg_relation`)
-- =============================================================
DROP TABLE IF EXISTS `kg_relation`;
CREATE TABLE `kg_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 知识图谱关系表',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `from_entity` BIGINT NULL COMMENT '起始实体ID',
  `to_entity` BIGINT NULL COMMENT '目标实体ID',
  `relation_type` VARCHAR(255) NULL COMMENT '关系类型(属于/相关/位于等)',
  `description` VARCHAR(255) NULL COMMENT '关系描述',
  `weight` DECIMAL(20,4) NULL COMMENT '权重(0-1)',
  `source` VARCHAR(255) NULL COMMENT '来源(manual/import/ai)',
  `ref_count` INT NULL COMMENT '被引用次数',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='知识图谱关系表';

-- =============================================================
-- 插件表 (`plugin`)
-- =============================================================
DROP TABLE IF EXISTS `plugin`;
CREATE TABLE `plugin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 插件表',
  `name` VARCHAR(255) NULL COMMENT '插件名称(英文唯一)',
  `display_name` VARCHAR(255) NULL COMMENT '显示名称(中文)',
  `description` VARCHAR(255) NULL COMMENT '插件描述',
  `version` VARCHAR(255) NULL COMMENT '版本号',
  `author` VARCHAR(255) NULL COMMENT '作者',
  `category` VARCHAR(255) NULL COMMENT '类别(image/audio/code等)',
  `scope` VARCHAR(255) NULL COMMENT '作用域(public/private)',
  `owner_id` BIGINT NULL COMMENT '拥有者ID',
  `icon` VARCHAR(255) NULL COMMENT '图标URL',
  `entry` VARCHAR(255) NULL COMMENT '入口文件路径',
  `plugin_type` VARCHAR(255) NULL COMMENT '插件类型(marketplace/custom)',
  `config` VARCHAR(255) NULL COMMENT '配置(JSON)',
  `enabled` INT NULL COMMENT '是否启用(0禁用/1启用)',
  `downloads` INT NULL COMMENT '下载次数',
  `rating` DECIMAL(20,4) NULL COMMENT '评分(1-5星)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='插件表';

-- =============================================================
-- 聊天消息表 (`chat_message`)
-- =============================================================
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 聊天消息表',
  `session_id` BIGINT NULL COMMENT '会话ID',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `role` VARCHAR(255) NULL COMMENT '角色(user/assistant/system)',
  `content` VARCHAR(255) NULL COMMENT '消息内容',
  `tokens` INT NULL COMMENT 'Token数',
  `finish_reason` VARCHAR(255) NULL COMMENT '结束原因(stop/length/error)',
  `error_message` VARCHAR(255) NULL COMMENT '错误信息',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='聊天消息表';

-- =============================================================
-- 聊天会话表 (`chat_session`)
-- =============================================================
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 聊天会话表',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `title` VARCHAR(255) NULL COMMENT '会话标题',
  `model` VARCHAR(255) NULL COMMENT '使用的模型',
  `system_prompt` VARCHAR(255) NULL COMMENT '系统提示词',
  `temperature` DECIMAL(20,4) NULL COMMENT '温度参数(0-2)',
  `status` INT NULL COMMENT '状态(active/archived)',
  `message_count` INT NULL COMMENT '消息数量',
  `last_message_at` TIMESTAMP NULL COMMENT '最后消息时间',
  `tenant_id` BIGINT NULL COMMENT '租户ID',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT NOT NULL DEFAULT 0 COMMENT '是否删除(0未删/1已删)',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='聊天会话表';

-- =============================================================
-- 协作消息表 (`collab_message`)
-- =============================================================
DROP TABLE IF EXISTS `collab_message`;
CREATE TABLE `collab_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 协作消息表',
  `room_id` VARCHAR(255) NULL COMMENT '房间ID',
  `user_id` BIGINT NULL COMMENT '发送用户ID',
  `username` VARCHAR(255) NULL COMMENT '用户名',
  `nickname` VARCHAR(255) NULL COMMENT '昵称',
  `type` VARCHAR(255) NULL COMMENT '消息类型(text/image/file)',
  `content` VARCHAR(255) NULL COMMENT '消息内容',
  `metadata` VARCHAR(255) NULL COMMENT '元数据(JSON)',
  `client_msg_id` VARCHAR(255) NULL COMMENT '客户端消息ID',
  `broadcast` INT NULL COMMENT '是否广播(0否/1是)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='协作消息表';

-- =============================================================
-- 协作参与者表 (`collab_participant`)
-- =============================================================
DROP TABLE IF EXISTS `collab_participant`;
CREATE TABLE `collab_participant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 协作参与者表',
  `room_id` VARCHAR(255) NULL COMMENT '房间ID',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `username` VARCHAR(255) NULL COMMENT '用户名',
  `nickname` VARCHAR(255) NULL COMMENT '昵称',
  `avatar` VARCHAR(255) NULL COMMENT '头像URL',
  `role` VARCHAR(255) NULL COMMENT '角色(host/editor/viewer)',
  `cursor_x` INT NULL COMMENT '光标X坐标',
  `cursor_y` INT NULL COMMENT '光标Y坐标',
  `selection_id` VARCHAR(255) NULL COMMENT '选区ID',
  `status` VARCHAR(255) NULL COMMENT '在线状态(online/away/offline)',
  `joined_at` TIMESTAMP NULL COMMENT '加入时间',
  `left_at` TIMESTAMP NULL COMMENT '离开时间',
  `last_heartbeat` TIMESTAMP NULL COMMENT '最后心跳时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='协作参与者表';

-- =============================================================
-- 协作房间表 (`collab_room`)
-- =============================================================
DROP TABLE IF EXISTS `collab_room`;
CREATE TABLE `collab_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID - 协作房间表',
  `room_id` VARCHAR(255) NULL COMMENT '房间业务ID',
  `name` VARCHAR(255) NULL COMMENT '房间名称',
  `type` VARCHAR(255) NULL COMMENT '房间类型(chat/whiteboard/canvas)',
  `owner_id` BIGINT NULL COMMENT '房主用户ID',
  `owner_name` VARCHAR(255) NULL COMMENT '房主名称',
  `description` VARCHAR(255) NULL COMMENT '房间描述',
  `is_public` INT NULL COMMENT '是否公开(0私有/1公开)',
  `max_participants` INT NULL COMMENT '最大参与人数',
  `status` VARCHAR(255) NULL COMMENT '房间状态(active/archived)',
  `current_participants` INT NULL COMMENT '当前参与人数',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `last_activity_at` TIMESTAMP NULL COMMENT '最近活动时间',
  `closed_at` TIMESTAMP NULL COMMENT '关闭时间',
  PRIMARY KEY (`id`)
  KEY `idx_id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000 COMMENT='协作房间表';

SET FOREIGN_KEY_CHECKS = 1;
