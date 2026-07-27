-- =============================================================
-- MiniMax Platform V3.5.38 数据库迁移
-- 给 alert_rule / alert_event 加 threshold 字段 (DECIMAL)
-- 用于 V3.5.19 → V3.5.38 升级 (V3.5.19 schema 缺这字段)
-- 
-- 用法: 
--   mysql -uroot -proot123456 minimax_platform < sql/v3.5.38-migration.sql
-- =============================================================

-- 1. alert_rule 加 threshold
ALTER TABLE alert_rule ADD COLUMN IF NOT EXISTS `threshold` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'threshold' AFTER `operator`;

-- 2. alert_event 加 threshold (V3.5.19 同样缺, 但报没改 entity 之前)
ALTER TABLE alert_event ADD COLUMN IF NOT EXISTS `threshold` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT 'threshold' AFTER `operator`;

-- 3. 验证
SHOW COLUMNS FROM alert_rule LIKE 'threshold';
SHOW COLUMNS FROM alert_event LIKE 'threshold';

SELECT 'V3.5.38 migration done' AS status;
