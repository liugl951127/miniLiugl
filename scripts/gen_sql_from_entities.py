#!/usr/bin/env python3
"""
V6.3+ 从 Entity 类生成 SQL DDL
- 扫描所有 @TableName 注解的 Java 类
- 提取字段, 类型, 注释, 索引
- 输出完整 CREATE TABLE
"""

import os
import re
import sys
from pathlib import Path

# 类型映射
TYPE_MAP = {
    'Long': 'BIGINT', 'long': 'BIGINT',
    'Integer': 'INT', 'int': 'INT',
    'Short': 'SMALLINT', 'short': 'SMALLINT',
    'Byte': 'TINYINT', 'byte': 'TINYINT',
    'Float': 'FLOAT', 'float': 'FLOAT',
    'Double': 'DOUBLE', 'double': 'DOUBLE',
    'BigDecimal': 'DECIMAL(20,4)',
    'BigInteger': 'BIGINT',
    'Boolean': 'TINYINT(1)', 'boolean': 'TINYINT(1)',
    'String': 'VARCHAR(255)',
    'LocalDate': 'DATE',
    'LocalDateTime': 'DATETIME',
    'LocalTime': 'TIME',
    'Date': 'DATETIME',
    'byte[]': 'BLOB',
    'Object': 'JSON',
}

def camel_to_snake(name):
    s1 = re.sub(r'(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s1).lower()

def parse_field_type(type_str):
    type_str = type_str.strip()
    generic = ''
    if '<' in type_str:
        m = re.match(r'([^<]+)<(.+)>', type_str)
        if m:
            type_str = m.group(1).strip()
            generic = m.group(2).strip()
    is_array = type_str.endswith('[]')
    if is_array:
        type_str = type_str[:-2]
    return type_str, is_array, generic

def get_java_type(field_type):
    base, is_array, generic = parse_field_type(field_type)
    if is_array:
        return 'TEXT'
    if base in TYPE_MAP:
        return TYPE_MAP[base]
    return 'VARCHAR(255)'

def parse_table_name(content):
    m = re.search(r'@TableName\s*\(\s*["\']([^"\']+)["\']', content)
    return m.group(1) if m else None

def parse_class_comment(content):
    m = re.search(r'/\*\*\s*\n\s*\*\s*([^\n@]+)', content)
    return m.group(1).strip() if m else None

def parse_fields(content):
    fields = []
    javadoc_re = re.compile(r'/\*\*((?:\s*\*[^/]*?)*?)\*/', re.DOTALL)
    line_comment_re = re.compile(r'//\s*(.+?)$', re.MULTILINE)
    annotation_re = re.compile(r'@(\w+)(?:\(([^)]*)\))?')
    field_re = re.compile(r'(private|protected|public)\s+([\w<>,\[\]\s]+?)\s+(\w+)\s*[;=]')
    
    # 找所有 /** ... */ javadoc 块
    javadoc_blocks = []
    for m in javadoc_re.finditer(content):
        jcontent = m.group(1)
        # 跳过含 @ 注解的 javadoc (那些不是注释)
        if '@' in jcontent:
            continue
        # 跳过全装饰行 (==== ====)
        if all('=' in l for l in jcontent.split('\n') if l.strip()):
            continue
        # 提取非空非 @ 行
        lines = [l.strip().lstrip('*').strip() for l in jcontent.split('\n') if l.strip() and not l.strip().startswith('@') and not l.strip().startswith('=')]
        comment = ' '.join(lines[:3])
        if comment:
            javadoc_blocks.append({'start': m.start(), 'end': m.end(), 'comment': comment})
    
    # 找所有 // 单行注释
    line_comments = []
    for m in line_comment_re.finditer(content):
        line_comments.append({'pos': m.start(), 'comment': m.group(1).strip()})
    
    # 找所有字段
    for m in field_re.finditer(content):
        java_type = m.group(2).strip()
        field_name = m.group(3)
        start = m.start()
        
        if field_name == 'serialVersionUID':
            continue
        
        # 找字段专属注释:
        # 1. 找字段前最近的 /** ... */ 块, 块必须在 start 之前
        # 2. 该块和字段之间不能有其他字段 (private/protected/public)
        comment = ''
        for jb in javadoc_blocks:
            if jb['end'] >= start:
                break  # javadoc 在字段之后, 跳出
            # 检查 javadoc 和字段之间
            between = content[jb['end']:start]
            if 'private ' in between or 'public ' in between or 'protected ' in between:
                continue  # 中间有其他字段
            comment = jb['comment']
            break  # 找到最近的
        
        if not comment:
            # 试 // 单行注释
            for lc in line_comments:
                if lc['pos'] >= start:
                    break
                between = content[lc['pos']:start]
                if 'private ' in between or 'public ' in between or 'protected ' in between:
                    continue
                comment = lc['comment']
                break
        
        # 注解 - 字段前 200 字符 (允许跨行)
        very_close = content[max(0, start-200):start]
        # 严格检测 is_id: @TableId 紧跟字段 (中间无其他字段声明)
        is_id = False
        # 找 @TableId 在 very_close 中
        tid_pos = very_close.rfind('@TableId')
        if tid_pos >= 0:
            # @TableId 后到字段前没其他 private/public/protected
            between = very_close[tid_pos:].split('@TableId')[-1] if '@TableId' in very_close[tid_pos:] else very_close[tid_pos:]
            if 'private ' not in between and 'public ' not in between and 'protected ' not in between:
                is_id = True
        # 严格检测: @TableLogic / @Version 紧跟字段 (中间无其他 private)
        is_logic_delete = False
        tl_pos = very_close.rfind('@TableLogic')
        if tl_pos >= 0:
            between = very_close[tl_pos:].split('@TableLogic')[-1] if '@TableLogic' in very_close[tl_pos:] else very_close[tl_pos:]
            if 'private ' not in between and 'public ' not in between and 'protected ' not in between:
                is_logic_delete = True
        is_version = False
        v_pos = very_close.rfind('@Version')
        if v_pos >= 0:
            between = very_close[v_pos:].split('@Version')[-1] if '@Version' in very_close[v_pos:] else very_close[v_pos:]
            if 'private ' not in between and 'public ' not in between and 'protected ' not in between:
                is_version = True
        # 严格检测 is_lob: @TableField 含 'lob' 注解紧跟字段
        # 注意: '@TableLogic' 含 'lob', 但 @TableLogic 不是 LOB 注解
        is_lob = False
        for lob_annotation in ['@Lob', 'lob = true', 'jdbcType=LONG']:
            lob_pos = very_close.rfind(lob_annotation)
            if lob_pos >= 0:
                between = very_close[lob_pos:].split(lob_annotation)[-1]
                if 'private ' not in between and 'public ' not in between and 'protected ' not in between:
                    is_lob = True
                    break
        
        # @TableField 解析: 两种形式都支持
        # 但 f['name'] 始终用 snake_case (与 MyBatis-Plus 默认一致)
        # @TableField 仅用于检测 column_name (存到 f['explicit_name'])
        explicit_name = None
        # 形式 1: @TableField("xxx")
        tf_match = re.search(r'@TableField\s*\(\s*["\']([^"\']+)["\']', very_close)
        if tf_match:
            explicit_name = tf_match.group(1)
        else:
            # 形式 2: @TableField(value = "xxx")
            tf_match = re.search(r'@TableField\s*\(\s*value\s*=\s*["\']([^"\']+)["\']', very_close)
            if tf_match:
                explicit_name = tf_match.group(1)
        # 始终用 snake_case 作为 f['name'], 保证 SQL 命名一致
        column_name = camel_to_snake(field_name)
        
        # 长度
        length = None
        len_match = re.search(r'length\s*=\s*(\d+)', very_close)
        if len_match:
            length = int(len_match.group(1))
        
        auto_increment = is_id and 'AUTO' in very_close
        nullable = False
        
        fields.append({
            'name': column_name,
            'java_name': field_name,
            'type': get_java_type(java_type),
            'java_type': java_type,
            'comment': comment[:200] if comment else '',
            'is_id': is_id,
            'is_logic_delete': is_logic_delete,
            'is_version': is_version,
            'is_lob': is_lob,
            'auto_increment': auto_increment,
            'nullable': nullable,
            'length': length,
        })
    
    return fields



V63_ENHANCEMENT = """
-- =============================================================
-- V6.3+ 字段增强 (10 业务表加 4 字段)
-- =============================================================
USE `minimax_platform`;

-- ALTER TABLE `sys_user` ADD COLUMN `tags` VARCHAR(500) DEFAULT '' COMMENT '业务标签';  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `sys_user` ADD COLUMN `extra` JSON COMMENT '扩展元数据 JSON';  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `api_key` ADD COLUMN `tags` VARCHAR(500) DEFAULT '';  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `api_key` ADD COLUMN `extra` JSON;  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `api_key` ADD COLUMN `last_used_at` DATETIME DEFAULT NULL;  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `tenant` ADD COLUMN `tags` VARCHAR(500) DEFAULT '';  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `tenant` ADD COLUMN `extra` JSON;  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `tenant` ADD COLUMN `logo_url` VARCHAR(500) DEFAULT '';  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `ai_chat_session` ADD COLUMN `tags` VARCHAR(500) DEFAULT '';  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `ai_chat_session` ADD COLUMN `extra` JSON;  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `ai_chat_session` ADD COLUMN `pinned` INT DEFAULT 0;  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `rag_kb` ADD COLUMN `tags` VARCHAR(500) DEFAULT '';  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `rag_kb` ADD COLUMN `extra` JSON;  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `rag_kb` ADD COLUMN `icon` VARCHAR(200) DEFAULT '';  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `alert_rule` ADD COLUMN `tags` VARCHAR(500) DEFAULT '';  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `alert_rule` ADD COLUMN `extra` JSON;  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `alert_rule` ADD COLUMN `runbook_url` VARCHAR(500) DEFAULT '';  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `ai_tool` ADD COLUMN `tags` VARCHAR(500) DEFAULT '';  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `ai_prompt` ADD COLUMN `tags` VARCHAR(500) DEFAULT '';  (兼容模式: 字段已包含在 DDL)
-- ALTER TABLE `workflow` ADD COLUMN `tags` VARCHAR(500) DEFAULT '';  (兼容模式: 字段已包含在 DDL)

-- =============================================================
-- V6.3+ 核心种子 (5 账号 + 3 租户)
-- =============================================================
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `sys_user` WHERE `username` IN ('admin', 'demo', 'test', 'guest', 'vip');
INSERT IGNORE INTO `sys_user` (`id`, `username`, `nickname`, `email`, `phone`, `password`, `status`, `tags`, `created_at`, `updated_at`) VALUES
(1, 'admin', '系统管理员', 'admin@minimax.io', '13800000001', '{BCRYPT_PLACEHOLDER}', 1, 'admin,super,VIP', NOW(), NOW()),
(2, 'demo', '演示账号', 'demo@minimax.io', '13800000002', '{BCRYPT_PLACEHOLDER}', 1, 'demo', NOW(), NOW()),
(3, 'test', '测试账号', 'test@minimax.io', '13800000003', '{BCRYPT_PLACEHOLDER}', 1, 'test,beta', NOW(), NOW()),
(4, 'guest', '访客账号', 'guest@minimax.io', '13800000004', '{BCRYPT_PLACEHOLDER}', 1, 'guest,readonly', NOW(), NOW()),
(5, 'vip', 'VIP 账号', 'vip@minimax.io', '13800000005', '{BCRYPT_PLACEHOLDER}', 1, 'vip,premium', NOW(), NOW());

DELETE FROM `tenant` WHERE `code` IN ('default', 'enterprise', 'startup');
INSERT IGNORE INTO `tenant` (`id`, `code`, `name`, `plan`, `status`, `max_users`, `contact_email`, `contact_phone`, `created_at`) VALUES
(1, 'default', '默认租户', 'enterprise', 1, 100, 'admin@minimax.local', '13800000001', NOW()),
(2, 'enterprise', '企业客户 A', 'enterprise', 1, 500, 'enterprise@minimax.local', '13900000001', NOW()),
(3, 'startup', '创业团队 B', 'pro', 1, 50, 'startup@minimax.local', '13900000002', NOW());

-- =============================================================
-- V6.3+ 触发器 (2)
-- =============================================================
SET @SQL_LOG_BIN_TRUST = ON;

DROP TRIGGER IF EXISTS trg_sys_user_updated_at;
DELIMITER $$
CREATE TRIGGER trg_sys_user_updated_at BEFORE UPDATE ON sys_user
FOR EACH ROW BEGIN
    SET NEW.updated_at = NOW();
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_api_key_prefix;
DELIMITER $$
CREATE TRIGGER trg_api_key_prefix BEFORE INSERT ON api_key
FOR EACH ROW BEGIN
    IF NEW.key_prefix IS NULL OR NEW.key_prefix = '' THEN
        SET NEW.key_prefix = CONCAT('sk-', SUBSTRING(MD5(RAND()), 1, 8), '-');
    END IF;
END$$
DELIMITER ;

-- =============================================================
-- V6.3+ 存储过程 (1)
-- =============================================================
DROP PROCEDURE IF EXISTS sp_user_key_count;
DELIMITER $$
CREATE PROCEDURE sp_user_key_count(IN p_user_id BIGINT, OUT o_total INT, OUT o_active INT)
BEGIN
    SELECT COUNT(*) INTO o_total FROM api_key WHERE user_id = p_user_id AND deleted = 0;
    SELECT COUNT(*) INTO o_active FROM api_key WHERE user_id = p_user_id AND status = 1 AND deleted = 0;
END$$
DELIMITER ;

-- =============================================================
-- V6.3+ 函数 (2)
-- =============================================================
DROP FUNCTION IF EXISTS fn_mask_phone;
DELIMITER $$
CREATE FUNCTION fn_mask_phone(p_phone VARCHAR(20)) RETURNS VARCHAR(20)
DETERMINISTIC
BEGIN
    IF p_phone IS NULL OR LENGTH(p_phone) < 7 THEN
        RETURN p_phone;
    END IF;
    RETURN CONCAT(LEFT(p_phone, 3), '****', RIGHT(p_phone, 4));
END$$
DELIMITER ;

DROP FUNCTION IF EXISTS fn_user_activity_score;
DELIMITER $$
CREATE FUNCTION fn_user_activity_score(p_user_id BIGINT) RETURNS INT
DETERMINISTIC
BEGIN
    DECLARE v_score INT DEFAULT 0;
    DECLARE v_login_count INT DEFAULT 0;
    DECLARE v_chat_count INT DEFAULT 0;
    SELECT COUNT(*) INTO v_login_count FROM auth_login_log WHERE user_id = p_user_id AND created_at > DATE_SUB(NOW(), INTERVAL 30 DAY);
    SELECT COUNT(*) INTO v_chat_count FROM ai_chat_session WHERE user_id = p_user_id AND created_at > DATE_SUB(NOW(), INTERVAL 30 DAY);
    SET v_score = LEAST(100, v_login_count * 2 + v_chat_count);
    RETURN v_score;
END$$
DELIMITER ;

-- schema_version
CREATE TABLE IF NOT EXISTS `schema_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `version` VARCHAR(50) NOT NULL DEFAULT '',
    `description` VARCHAR(500) NOT NULL DEFAULT '',
    `applied_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_version` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT IGNORE INTO `schema_version` (`version`, `description`) VALUES
('V6.3.0', '从 Entity 自动生成 89 张表 DDL'),
('V6.3.1', 'V6.3+ 字段增强 + 12 模块种子'),
('V6.3.2', 'V6.3+ 触发器/过程/函数');
"""


def generate_module_seeds(module_entities):
    """为每个模块生成基础种子 (1-3 条示例)"""
    out = []
    out.append('-- =============================================================')
    out.append('-- V6.3+ 模块种子数据 (按模块分组)')
    out.append('-- =============================================================')
    out.append('USE `minimax_platform`;')
    out.append('SET FOREIGN_KEY_CHECKS = 0;')
    out.append('')
    
    for module in sorted(module_entities.keys()):
        out.append(f'-- ============ 模块: {module} ============')
        for table_name, entity_name, fields in module_entities[module]:
            # 跳过主表/已有种子的表
            if table_name in SKIP_SEED_TABLES:
                continue
            
            # 生成 INSERT
            insert = generate_seed_row(table_name, entity_name, fields)
            if insert:
                out.append(insert)
        out.append('')
    
    out.append('SET FOREIGN_KEY_CHECKS = 1;')
    out.append('')
    return '\n'.join(out)


SKIP_SEED_TABLES = {
    'sys_user', 'sys_role', 'sys_user_role', 'sys_menu', 'sys_dict',  # 单独处理
    'tenant',  # 单独处理
    'auth_login_log', 'audit_log', 'audit_log_full',  # 日志
    'ai_chat_message',  # 消息
    'api_call_log', 'request_log', 'function_call_log', 'model_battle_log',  # 日志
    'memory_short_term', 'memory_long_term',  # 临时
    'api_key',  # 单独处理
    'schema_version',  # 系统表
}

def generate_seed_row(table_name, entity_name, fields):
    """生成 1 条种子数据"""
    if not fields:
        return None
    
    # 找合适的列生成值
    cols = []
    vals = []
    for f in fields:
        if f['is_id'] and f['auto_increment']:
            continue  # 跳过自增 id
        if f['is_logic_delete']:
            cols.append(f['name'])
            vals.append('0')
            continue
        if f['is_version']:
            cols.append(f['name'])
            vals.append('1')
            continue
        if f['name'] in ('created_at', 'updated_at', 'created_time', 'update_time'):
            cols.append(f['name'])
            vals.append('NOW()')
            continue
        # 其他列
        if f['java_type'] == 'String':
            v = generate_string_value(table_name, f, entity_name)
            if v is not None:
                cols.append(f['name'])
                vals.append(f"'{v}'")
        elif f['java_type'] in ('Integer', 'int', 'Long', 'long', 'Short', 'short'):
            v = generate_int_value(table_name, f)
            if v is not None:
                cols.append(f['name'])
                vals.append(str(v))
        elif f['java_type'] == 'Boolean':
            cols.append(f['name'])
            vals.append('1')
        elif 'DATETIME' in f['type'] or 'DATE' in f['type']:
            cols.append(f['name'])
            vals.append('NOW()')
    
    if not cols:
        return None
    
    cols_str = ', '.join(f'`{c}`' for c in cols)
    vals_str = ', '.join(vals)
    return f"-- {entity_name} ({table_name})\nINSERT IGNORE INTO `{table_name}` ({cols_str}) VALUES ({vals_str});"


def generate_string_value(table_name, field, entity_name):
    """生成字符串种子值"""
    name = field['name']
    
    # 已知模式
    if 'email' in name:
        return f'seed@{table_name}.io'
    if 'phone' in name:
        return '13900000000'
    if 'url' in name or 'avatar' in name or 'logo' in name:
        return f'https://cdn.{table_name}.io/{name}.png'
    if 'ip' in name:
        return '127.0.0.1'
    if name in ('name', 'title'):
        return f'种子 {entity_name}'
    if 'code' == name:
        return f'SEED_{table_name.upper()}_1'
    if 'type' == name:
        return 'default'
    if 'category' == name:
        return 'general'
    if 'status' in name or 'enabled' in name or 'pinned' in name or 'active' in name:
        return None  # 数字字段
    if 'content' in name or 'description' in name or 'remark' in name or 'detail' in name or 'message' in name or 'text' in name:
        return f'种子 {entity_name} {field["name"]} 描述'
    if 'body' in name:
        return '{}'
    if 'tags' in name:
        return 'seed,demo'
    if 'extra' in name:
        return None  # JSON
    if 'template' in name or 'config' in name:
        return '{}'
    if 'password' in name:
        return '{BCRYPT_PLACEHOLDER}'
    if 'token' in name:
        return 'PLACEHOLDER_TOKEN'
    if 'secret' in name or 'api_key' in name or 'key' in name:
        return 'PLACEHOLDER_KEY'
    if 'pinyin' in name:
        return 'zhongzi'
    return f'seed-{name}'


def generate_int_value(table_name, field):
    """生成数字种子值"""
    name = field['name']
    
    if 'user_id' in name:
        return 1
    if 'tenant_id' in name:
        return 1
    if name in ('count', 'total', 'max_users', 'max_models', 'max_storage', 'quota', 'limit', 'qps_limit'):
        return 100
    if 'status' in name or 'enabled' in name or 'active' in name:
        return 1
    if name in ('deleted', 'is_default', 'is_regex', 'pinned', 'public'):
        return 0
    if 'priority' in name or 'sort' in name or 'level' in name:
        return 1
    if 'port' in name:
        return 8080
    if 'progress' in name:
        return 100
    if 'usage' in name or 'percent' in name:
        return 0
    if 'version' in name or 'seq' in name:
        return 1
    if 'weight' in name or 'score' in name:
        return 100
    if 'frequency' in name or 'limit' in name:
        return 100
    if 'latency' in name or 'duration' in name or 'timeout' in name or 'interval' in name or 'ttl' in name or 'minutes' in name:
        return 60
    if 'tokens' in name or 'size' in name or 'length' in name or 'bytes' in name or 'max' in name:
        return 1024
    if 'time' in name or 'date' in name:
        return None  # datetime
    return 0


def generate_create_table(table_name, entity_name, comment, fields):
    lines = []
    table_comment = comment or entity_name
    lines.append(f"-- {entity_name} -> {table_name} ({len(fields)} 字段)")
    lines.append(f"DROP TABLE IF EXISTS `{table_name}`;")
    lines.append(f"CREATE TABLE IF NOT EXISTS `{table_name}` (")
    
    field_lines = []
    primary_key = None
    indices = []
    
    for f in fields:
        # 类型处理
        col_type = f['type']
        if f['length'] and 'VARCHAR' in col_type:
            col_type = f'VARCHAR({f["length"]})'
        if f['is_lob']:
            col_type = 'LONGTEXT'
        elif f['java_type'] == 'byte[]':
            col_type = 'BLOB'
        elif col_type == 'VARCHAR(255)' and f['length'] is None and f['java_type'] == 'String':
            if any(k in f['name'] for k in ('content', 'text', 'desc', 'remark', 'body', 'config', 'detail', 'template')):
                col_type = 'TEXT'
        
        # 自增
        auto = ' AUTO_INCREMENT' if f['auto_increment'] else ''
        
        # NULL + DEFAULT
        if f['is_id'] and f['auto_increment']:
            null_default = ' NOT NULL'
        elif f['is_id']:
            null_default = ' NOT NULL'
        elif f['nullable'] is False:
            # 业务时间字段 (非 created/updated) 允许 NULL
            if f['java_type'] == 'LocalDateTime' and 'DATETIME' in col_type:
                # 任何 create_* / update_*/ created_* / updated_* 都视作创建/更新时间
                if f['name'].startswith(('create_', 'update_', 'created_', 'updated_')):
                    null_default = ' NOT NULL'
                else:
                    # 业务时间字段 (heartbeat/timestamp/end_time 等) 允许 NULL
                    null_default = ' DEFAULT NULL'
            elif f['java_type'] == 'LocalDate' and 'DATE' in col_type:
                # 业务日期字段允许 NULL
                null_default = ' DEFAULT NULL'
            else:
                null_default = ' NOT NULL'
        else:
            null_default = ' DEFAULT NULL'

        # BLOB/TEXT/JSON/GEOMETRY 不允许 DEFAULT (MariaDB 严格模式)
        skip_default = col_type in ('BLOB', 'TEXT', 'LONGTEXT', 'JSON', 'GEOMETRY') or col_type.startswith(('BLOB', 'TEXT', 'JSON'))
        
        # 必填字段加 default (跳过 BLOB/TEXT/JSON)
        if 'NOT NULL' in null_default and not f['auto_increment'] and not skip_default:
            if f['is_logic_delete']:
                null_default += ' DEFAULT 0'
            elif f['is_version']:
                null_default += ' DEFAULT 1'
            elif f['java_type'] == 'Boolean':
                # Boolean 业务字段默认 false (0)
                null_default += ' DEFAULT 0'
            elif (f['name'] in ('created_at', 'updated_at', 'create_time', 'update_time') 
                  and 'DATETIME' in col_type):
                null_default += ' DEFAULT CURRENT_TIMESTAMP'
            elif f['name'] == 'created_at' and 'DATE' in col_type and 'TIME' not in col_type:
                null_default += ' DEFAULT (CURRENT_DATE)'
            elif 'status' in f['name'] and col_type in ('INT', 'TINYINT'):
                null_default += ' DEFAULT 1'
            elif 'deleted' in f['name']:
                null_default += ' DEFAULT 0'
            elif 'sort' in f['name'] or 'priority' in f['name']:
                null_default += ' DEFAULT 0'
            elif 'enabled' in f['name']:
                null_default += ' DEFAULT 1'
            elif col_type in ('INT', 'BIGINT', 'SMALLINT', 'TINYINT', 'DECIMAL(20,4)', 'FLOAT', 'DOUBLE'):
                null_default += ' DEFAULT 0'
            elif col_type.startswith('VARCHAR') or col_type in ('TEXT', 'LONGTEXT'):
                null_default += " DEFAULT ''"
            elif 'DATETIME' in col_type or 'DATE' in col_type:
                pass
            elif 'TIME' in col_type:
                pass
        
        # 注释
        comment_str = f" COMMENT '{f['comment'].replace(chr(39), chr(39)+chr(39))}'" if f['comment'] else ''
        
        field_line = f"    `{f['name']}` {col_type}{auto}{null_default}{comment_str}"
        field_lines.append(field_line)
        
        if f['is_id']:
            primary_key = f['name']
        
        # 索引
        if f['name'] == 'username' and f['java_type'] == 'String':
            indices.append(f"ALTER TABLE `{table_name}` DROP INDEX IF EXISTS `idx_{table_name}_username`;\nCREATE INDEX `idx_{table_name}_username` ON `{table_name}`(`{f['name']}`);")
        elif f['name'] == 'code' and f['java_type'] == 'String':
            indices.append(f"ALTER TABLE `{table_name}` DROP INDEX IF EXISTS `idx_{table_name}_code`;\nCREATE INDEX `idx_{table_name}_code` ON `{table_name}`(`{f['name']}`);")
        elif f['name'] in ('user_id', 'tenant_id', 'session_id', 'parent_id', 'created_by', 'updated_by', 'owner_id'):
            indices.append(f"ALTER TABLE `{table_name}` DROP INDEX IF EXISTS `idx_{table_name}_{f['name']}`;\nCREATE INDEX `idx_{table_name}_{f['name']}` ON `{table_name}`(`{f['name']}`);")
        elif f['name'] == 'status' and f['java_type'] in ('Integer', 'int'):
            indices.append(f"ALTER TABLE `{table_name}` DROP INDEX IF EXISTS `idx_{table_name}_status`;\nCREATE INDEX `idx_{table_name}_status` ON `{table_name}`(`{f['name']}`);")
    
    if primary_key:
        field_lines.append(f"    PRIMARY KEY (`{primary_key}`)")
    
    # 特殊表: sys_user_role 联合主键 (无 id 字段)
    if table_name == 'sys_user_role':
        field_lines.append('    PRIMARY KEY (`user_id`, `role_id`)')
    lines.append(',\n'.join(field_lines))
    lines.append(f") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='{table_comment}';")
    lines.append('')
    
    return '\n'.join(lines), indices

def main():
    backend_dir = Path('backend')
    files = list(backend_dir.rglob('*.java'))
    
    entities = []
    for f in files:
        if 'target' in str(f):
            continue
        try:
            content = f.read_text(encoding='utf-8', errors='ignore')
        except:
            continue
        if '@TableName' in content:
            entities.append((f, content))
    
    print(f'找到 {len(entities)} 个 @TableName 实体类', file=sys.stderr)
    
    tables = []
    all_indices = []
    seen = set()
    module_entities = {}  # module -> [(table, fields)]
    
    for f, content in entities:
        table_name = parse_table_name(content)
        if not table_name or table_name in seen:
            continue
        seen.add(table_name)
        
        entity_name = f.stem
        comment = parse_class_comment(content) or entity_name
        fields = parse_fields(content)
        
        if not fields:
            print(f'⚠️  {entity_name} 无字段, 跳过', file=sys.stderr)
            continue
        
        ddl, indices = generate_create_table(table_name, entity_name, comment, fields)
        tables.append(ddl)
        all_indices.extend(indices)
        
        # 按模块分组
        module = f.parts[1] if len(f.parts) > 1 else 'unknown'  # backend/minimax-XXX/...
        if 'minimax-' in str(f):
            for part in f.parts:
                if part.startswith('minimax-'):
                    module = part
                    break
        module_entities.setdefault(module, []).append((table_name, entity_name, fields))
    
    # 生成模块种子
    seeds_ddl = generate_module_seeds(module_entities)
    
    out = []
    out.append('-- =============================================================')
    out.append('-- MiniMax Platform V6.3+ 完整 SQL DDL (从 Entity 自动生成)')
    out.append(f'-- 共 {len(tables)} 张表 / 扫描 {len(entities)} 个实体类 / {len(module_entities)} 模块')
    out.append('-- =============================================================')
    out.append('')
    out.append('CREATE DATABASE IF NOT EXISTS `minimax_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;')
    out.append('USE `minimax_platform`;')
    out.append('')
    out.append('SET NAMES utf8mb4;')
    out.append('SET FOREIGN_KEY_CHECKS = 0;')
    out.append('')
    out.extend(tables)
    out.append('SET FOREIGN_KEY_CHECKS = 1;')
    out.append('')
    out.append('-- =============================================================')
    out.append(f'-- 索引 ({len(all_indices)} 个)')
    out.append('-- =============================================================')
    out.extend(all_indices)
    out.append('')
    out.append(seeds_ddl)
    out.append(V63_ENHANCEMENT)
    out.append('')
    out.append(f'-- 完成: 共 {len(tables)} 张表 / {len(all_indices)} 个索引')
    out.append('')
    
    output = '\n'.join(out)
    
    Path('sql').mkdir(exist_ok=True)
    with open('sql/minimax-mysql-final.sql', 'w', encoding='utf-8') as f:
        f.write(output)
    
    print(f'✓ 生成 sql/minimax-mysql-final.sql ({len(output)} 字节 / {len(tables)} 表 / {len(all_indices)} 索引 / {len(module_entities)} 模块)', file=sys.stderr)


if __name__ == '__main__':
    main()
