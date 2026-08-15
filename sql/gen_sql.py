#!/usr/bin/env python3
"""从实体类自动生成建表SQL (V6.8.1)"""
import re, os, sys
from pathlib import Path

def to_snake(name):
    """camelCase → snake_case (MyBatis-Plus 默认命名策略)"""
    s1 = re.sub(r'(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s1).lower()

JAVA_TO_MYSQL = {
    'Long': 'BIGINT', 'long': 'BIGINT',
    'Integer': 'INT', 'int': 'INT',
    'String': 'VARCHAR(255)',
    'Boolean': 'TINYINT(1)', 'boolean': 'TINYINT(1)',
    'Double': 'DOUBLE',
    'Float': 'FLOAT',
    'BigDecimal': 'DECIMAL(20,4)',
    'LocalDateTime': 'TIMESTAMP',
    'LocalDate': 'DATE',
    'LocalTime': 'TIME',
    'Date': 'DATETIME',
    'Timestamp': 'TIMESTAMP',
    'byte[]': 'VARBINARY(2048)',
    'Object': 'TEXT',
    'JSON': 'JSON',
}

def java_type_to_mysql(t):
    t = t.strip()
    # 处理泛型/数组
    if t.startswith('List<'): t = t[5:-1]
    elif t.startswith('Set<'): t = t[4:-1]
    elif t.startswith('Map<'): t = 'TEXT'
    elif t.endswith('[]'): t = t[:-2]
    # 去掉包名
    t = t.split('.')[-1]
    # 带长度的String
    m = re.match(r'String\((\d+)\)', t)
    if m: return f'VARCHAR({m.group(1)})'
    return JAVA_TO_MYSQL.get(t, 'VARCHAR(255)')

def parse_entity(path):
    with open(path, 'r', errors='ignore') as f:
        content = f.read()

    # 类名
    class_m = re.search(r'class\s+(\w+)', content)
    if not class_m: return None
    class_name = class_m.group(1)

    # 表名
    tbl_m = re.search(r'@TableName\s*\(\s*"([^"]+)"\s*\)', content)
    table = tbl_m.group(1) if tbl_m else class_name.lower()

    # 主键（通过遍历字段时检测 @TableId）
    pk_col = None
    pk_auto = False

    # 字段
    cols = []
    lines = content.split('\n')
    for i, line in enumerate(lines):
        line = line.strip()
        # 跳过方法/内部类/注释
        if re.match(r'(public|private|protected)\s+\w+\s+\w+\s*\(', line): continue
        if re.match(r'class\s+\w+\s*(implements|extends)', line): continue
        if line.startswith('//') or line.startswith('/*') or line.startswith('*'): continue
        if line.startswith('}'): continue

        col_m = re.search(r'(?:private|protected)\s+(\S+)\s+(\w+)\s*;', line)
        if not col_m: continue

        java_type, field_name = col_m.group(1), col_m.group(2)
        if field_name in ('serialVersionUID', 'CREATED', 'UPDATED'): continue

        # MyBatis-Plus camelCase → snake_case 列名
        col_name = to_snake(field_name)

        # 检查注解（当前行 + 上一行，可能有 @TableId 在上方的注解行）
        prev_line = lines[i-1].strip() if i > 0 else ''
        annotation_line = prev_line + ' ' + line

        col_type = java_type_to_mysql(java_type)
        nullable = ' NULL'
        default_val = ''
        extra = ''

        # 自动给 created_at / updated_at 加 DEFAULT CURRENT_TIMESTAMP
        if col_name in ('created_at', 'updated_at') and col_type in ('TIMESTAMP', 'DATETIME'):
            default_val = ' DEFAULT CURRENT_TIMESTAMP'
            nullable = ''

        # 检查 @TableId
        if re.search(r'@TableId', annotation_line):
            pk_col = col_name
            if re.search(r'type\s*=\s*IdType\.AUTO', annotation_line):
                pk_auto = True

        if re.search(r'@TableField\s*\([^)]*exist\s*=\s*false', annotation_line): continue  # 非数据库字段

        fill_m = re.search(r'fill\s*=\s*FieldFill\.(\w+)', annotation_line)
        if fill_m:
            fill = fill_m.group(1)
            if fill in ('INSERT','INSERT_UPDATE'):
                default_val = ' DEFAULT CURRENT_TIMESTAMP'

        if re.search(r'@TableLogic', annotation_line):
            default_val = ' DEFAULT 0'

        if re.search(r'@Version', annotation_line):
            extra = ' DEFAULT 0'

        if default_val: nullable = ''

        cols.append((col_name, col_type, nullable, default_val, extra))

    # 如果没找到 @TableId，找 id 字段
    if not pk_col:
        for cn, ct, *rest in cols:
            if cn == 'id':
                pk_col = 'id'
                break

    return {
        'table': table,
        'class_name': class_name,
        'pk_col': pk_col or 'id',
        'pk_auto': pk_auto,
        'cols': cols,
    }

def gen_create_table(info, module_name):
    table = info['table']
    pk_col = info['pk_col']
    pk_auto = info['pk_auto']
    cols = info['cols']

    lines = [f"-- ============================================================",
             f"-- {table} ({info['class_name']})",
             f"-- ============================================================",
             f"DROP TABLE IF EXISTS `{table}`;",
             f"CREATE TABLE `{table}` ("]
    for col_name, col_type, nullable, default_val, extra in cols:
        col_def = f"  `{col_name}` {col_type}{nullable}{default_val}{extra}"
        # 主键加 AUTO_INCREMENT
        if col_name == pk_col and pk_auto:
            col_def = f"  `{col_name}` {col_type} NOT NULL AUTO_INCREMENT"
        lines.append(col_def + ',')

    pk_str = f"  PRIMARY KEY (`{pk_col}`)"
    if not pk_auto:
        lines.append("  " + pk_str)
    else:
        lines.append("  " + pk_str + ",")
        lines.append(f"  KEY `idx_{pk_col}` (`{pk_col}`)")
    lines.append(f") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_INCREMENT=1000;")
    lines.append("")
    return '\n'.join(lines)

def gen_seed_data(info):
    """根据表名生成种子数据（使用 snake_case 列名）"""
    table = info['table']
    seeds = []

    if table == 'sys_user':
        seeds = [
            f"INSERT INTO `{table}` (id, username, password, email, phone, role, status, created_at) VALUES",
            "(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt/Hzu', 'admin@minimax.com', '13800138000', 'SUPER_ADMIN', 1, NOW()),",
            "(2, 'user01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt/Hzu', 'user01@minimax.com', '13800138001', 'USER', 1, NOW()),",
            "(3, 'operator', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt/Hzu', 'op@minimax.com', '13800138002', 'OPERATOR', 1, NOW());",
        ]
    elif table == 'sys_role':
        seeds = [
            f"INSERT INTO `{table}` (id, role_name, role_key, description, created_at) VALUES",
            "(1, '超级管理员', 'SUPER_ADMIN', '拥有全部权限', NOW()),",
            "(2, '普通用户', 'USER', '基础权限', NOW()),",
            "(3, '运维人员', 'OPERATOR', '系统运维权限', NOW());",
        ]
    elif table == 'sys_user_role':
        seeds = [
            f"INSERT INTO `{table}` (user_id, role_id) VALUES",
            "(1, 1), (2, 2), (3, 3);",
        ]
    elif table == 'notification':
        seeds = [
            f"INSERT INTO `{table}` (id, user_id, type, title, content, is_read, created_at) VALUES",
            "(1, 1, 'SYSTEM', '欢迎使用 MiniMax Platform', '平台已就绪，欢迎开始使用！', 0, NOW()),",
            "(2, 2, 'TASK', '训练任务完成', '模型训练任务已完成，准确率 94.2%', 0, NOW()),",
            "(3, 1, 'SYSTEM', '新功能上线', 'V6.8.1 版本已发布，包含多项优化', 1, NOW());",
        ]
    elif table == 'user_api_key':
        seeds = [
            f"INSERT INTO `{table}` (user_id, name, key_hash, key_prefix, scopes, enabled, use_count, created_at) VALUES",
            "(1, '测试 Key', SHA2('mmx_testkey001234567890abcdef', 256), 'mmx_test', 'chat:send,chat:stream', 1, 0, NOW()),",
            "(2, '生产环境', SHA2('mmx_prodkey1234567890abcdef', 256), 'mmx_prod', 'chat:send,chat:stream,agent:run', 1, 47, NOW());",
        ]
    elif table == 'function_tool':
        seeds = [
            f"INSERT INTO `{table}` (id, name, description, enabled, category, risk_level, created_at) VALUES",
            "(1, 'search_web', '搜索互联网', 1, 'search', 'LOW', NOW()),",
            "(2, 'calculator', '数学计算', 1, 'utility', 'LOW', NOW()),",
            "(3, 'file_reader', '读取本地文件', 1, 'file', 'MEDIUM', NOW()),",
            "(4, 'code_executor', '执行代码', 1, 'code', 'HIGH', NOW()),",
            "(5, 'sql_query', '数据库查询', 1, 'database', 'CRITICAL', NOW());",
        ]
    elif table == 'ai_chat_session':
        seeds = [
            f"INSERT INTO `{table}` (id, user_id, title, model, status, created_at) VALUES",
            "(1, 1, '测试会话', 'gpt-4o-mini', 'ACTIVE', NOW()),",
            "(2, 2, '客服问答', 'gpt-4o', 'ACTIVE', NOW());",
        ]
    elif table == 'ai_chat_message':
        seeds = [
            f"INSERT INTO `{table}` (id, session_id, role, content, created_at) VALUES",
            "(1, 1, 'user', '你好，请介绍一下你自己', NOW()),",
            "(2, 1, 'assistant', '我是 MiniMax AI 助手，可以帮助你完成各种任务。', NOW());",
        ]
    elif table == 'ai_voting_record':
        seeds = [
            f"INSERT INTO `{table}` (id, question, final_answer, strategy, total_votes, model_votes, created_at) VALUES",
            "(1, '2+2等于多少？', '4', 'majority', 4, '[{\"model\":\"gpt-4\",\"answer\":\"4\",\"confidence\":0.99}]', NOW()),",
            "(2, '北京是哪个国家的首都？', '中国', 'majority', 4, '[{\"model\":\"gpt-4\",\"answer\":\"中国\",\"confidence\":1.0}]', NOW());",
        ]
    elif table == 'training_task':
        seeds = [
            f"INSERT INTO `{table}` (id, user_id, model_name, corpus_path, status, progress, created_at) VALUES",
            "(1, 1, 'chatglm-6b', 'chat通用', 'COMPLETED', 100, NOW()),",
            "(2, 1, 'llama-2-7b', 'code', 'TRAINING', 67, NOW()),",
            "(3, 2, 'qwen-7b', '客服', 'PENDING', 0, NOW());",
        ]
    elif table == 'training_metric':
        seeds = [
            f"INSERT INTO `{table}` (id, task_id, step, loss, accuracy, learning_rate, elapsed_ms) VALUES",
            "(1, 1, 100, 2.341, 0.623, 0.0003, 15200),",
            "(2, 1, 200, 1.892, 0.701, 0.0003, 30400),",
            "(3, 1, 300, 1.521, 0.758, 0.0003, 45600),",
            "(4, 1, 400, 1.234, 0.801, 0.0003, 60800),",
            "(5, 1, 500, 0.987, 0.845, 0.0003, 76000);",
        ]
    elif table == 'model_provider':
        seeds = [
            f"INSERT INTO `{table}` (id, code, name, protocol, base_url, enabled, sort, created_at) VALUES",
            "(1, 'openai', 'OpenAI', 'openai', 'https://api.openai.com', 1, 1, NOW()),",
            "(2, 'deepseek', 'DeepSeek', 'openai', 'https://api.deepseek.com', 1, 2, NOW()),",
            "(3, 'local-ollama', 'Ollama 本地', 'local', 'http://localhost:11434', 1, 3, NOW());",
        ]
    elif table == 'model_config':
        seeds = [
            f"INSERT INTO `{table}` (id, provider_id, model_code, display_name, max_context, enabled, supports_stream, supports_tools, input_price, output_price) VALUES",
            "(1, 1, 'gpt-4o-mini', 'GPT-4o Mini', 128000, 1, 1, 1, 0.15, 0.6),",
            "(2, 1, 'gpt-4o', 'GPT-4o', 128000, 1, 1, 1, 2.5, 10.0),",
            "(3, 2, 'deepseek-chat', 'DeepSeek V3', 64000, 1, 1, 1, 0.1, 0.3);",
        ]
    elif table == 'knowledge_base':
        seeds = [
            f"INSERT INTO `{table}` (id, owner_id, name, description, visibility, created_at) VALUES",
            "(1, 1, '产品文档', '内部产品文档知识库', 'PRIVATE', NOW()),",
            "(2, 1, '技术文档', '技术文档知识库', 'PRIVATE', NOW()),",
            "(3, 2, '公开知识库', '公共知识库', 'PUBLIC', NOW());",
        ]
    elif table == 'alert_rule':
        seeds = [
            f"INSERT INTO `{table}` (id, name, metric, threshold, operator, severity, enabled, created_at) VALUES",
            "(1, 'CPU 过高告警', 'cpu_usage', 80, '>', 'WARNING', 1, NOW()),",
            "(2, '内存过高告警', 'memory_usage', 85, '>', 'WARNING', 1, NOW()),",
            "(3, '错误率过高', 'error_rate', 5, '>', 'CRITICAL', 1, NOW());",
        ]
    elif table == 'admin_audit_log':
        seeds = [
            f"INSERT INTO `{table}` (id, user_id, action, resource, ip, created_at) VALUES",
            "(1, 1, 'CREATE', 'training_task', '127.0.0.1', NOW()),",
            "(2, 2, 'QUERY', 'ai_chat_session', '127.0.0.1', NOW()),",
            "(3, 1, 'DELETE', 'knowledge_base', '127.0.0.1', NOW());",
        ]
    elif table == 'skill_approval':
        seeds = [
            f"INSERT INTO `{table}` (id, task_id, skill_name, risk_level, status, requested_by, created_at) VALUES",
            "(1, 1, 'sql_query', 'CRITICAL', 'APPROVED', 1, NOW()),",
            "(2, 2, 'file_reader', 'MEDIUM', 'PENDING', 2, NOW());",
        ]
    elif table == 'model_quota':
        seeds = [
            f"INSERT INTO `{table}` (id, user_id, model_name, quota_limit, quota_used, created_at) VALUES",
            "(1, 1, 'gpt-4o', 100000, 2340, NOW()),",
            "(2, 2, 'deepseek-chat', 50000, 8920, NOW());",
        ]
    elif table == 'cluster_node':
        seeds = [
            f"INSERT INTO `{table}` (id, node_name, host, port, status, cpu_usage, memory_usage, gpu_count, labels, created_at) VALUES",
            "(1, 'node-01', '10.0.0.11', 8080, 'ACTIVE', 45.2, 62.1, 2, '{\"role\":\"worker\"}', NOW()),",
            "(2, 'node-02', '10.0.0.12', 8080, 'ACTIVE', 38.7, 55.3, 2, '{\"role\":\"worker\"}', NOW());",
        ]
    elif table == 'sensitive_word':
        seeds = [
            f"INSERT INTO `{table}` (id, word, category, severity, action, created_at) VALUES",
            "(1, '色情', '政治', 'HIGH', 'BLOCK', NOW()),",
            "(2, '暴力', '暴力', 'MEDIUM', 'REVIEW', NOW());",
        ]
    elif table == 'ai_tool':
        seeds = [
            f"INSERT INTO `{table}` (id, name, category, description, endpoint, enabled, created_at) VALUES",
            "(1, 'weather', 'utility', '天气查询', '/api/weather', 1, NOW()),",
            "(2, 'search', 'search', '搜索', '/api/search', 1, NOW());",
        ]
    elif table == 'agent_group':
        seeds = [
            f"INSERT INTO `{table}` (id, name, description, visibility, created_at) VALUES",
            "(1, '客服组', '在线客服智能体组', 'PRIVATE', NOW()),",
            "(2, '审核组', '内容审核智能体组', 'PRIVATE', NOW());",
        ]
    elif table == 'oauth_app_config':
        seeds = [
            f"INSERT INTO `{table}` (id, platform, app_type, app_id, app_secret, enabled, created_at) VALUES",
            "(1, 'wechat', 'WEB', 'wx_app_001', 'secret_xxx', 1, NOW());",
        ]
    elif table == 'pipeline_workflow':
        seeds = [
            f"INSERT INTO `{table}` (id, name, description, version, status, owner_id, created_at) VALUES",
            "(1, 'RAG Pipeline', '检索增强生成流程', 1, 'ACTIVE', 1, NOW()),",
            "(2, '客服分流', '多客服智能分流', 1, 'DRAFT', 1, NOW());",
        ]
    elif table == 'prompt_template':
        seeds = [
            f"INSERT INTO `{table}` (id, name, description, prompt, model_type, tags, created_at) VALUES",
            "(1, '客服开场白', '标准客服开场白', '您好，我是 AI 助手，请问有什么可以帮助您的？', 'gpt-4o-mini', '[\"客服\",\"开场\"]', NOW()),",
            "(2, '代码审查', '代码审查 prompt', '请审查以下代码，找出潜在问题：', 'gpt-4o', '[\"开发\",\"审查\"]', NOW());",
        ]
    else:
        return None

    return '\n'.join(seeds) + '\n' if seeds else None

def main():
    base = Path('/workspace/miniLiugl/backend')
    modules = {
        'minimax-auth': 'auth',
        'minimax-ai': 'ai',
        'minimax-admin': 'admin',
        'minimax-agent': 'agent',
        'minimax-model': 'model',
        'minimax-monitor': 'monitor',
        'minimax-pipeline': 'pipeline',
        'minimax-rag': 'rag',
        'minimax-analytics': 'analytics',
        'minimax-ws': 'ws',
        'minimax-chat': 'chat',
    }

    all_tables = []
    for module, prefix in modules.items():
        entity_dir = base / module / 'src/main/java'
        if not entity_dir.exists(): continue
        for java_file in entity_dir.rglob('entity/*.java'):
            info = parse_entity(java_file)
            if not info: continue
            info['module'] = prefix
            all_tables.append(info)

    # 排序：先基础表（auth, admin），再业务表
    order = ['auth','admin','ai','model','monitor','pipeline','rag','analytics','agent','chat','ws']
    all_tables.sort(key=lambda x: (order.index(x['module']) if x['module'] in order else 99, x['table']))

    # 生成 SQL
    output = [
        "-- ============================================================",
        "-- MiniMax Platform V6.8.1 建表脚本",
        "-- 自动生成 from entity classes",
        "-- 数据库: utf8mb4, 引擎: InnoDB",
        "-- ============================================================",
        "SET NAMES utf8mb4;",
        "SET FOREIGN_KEY_CHECKS = 0;",
        "",
    ]

    for info in all_tables:
        sql = gen_create_table(info, info['module'])
        if sql:
            output.append(sql)

        seed = gen_seed_data(info)
        if seed:
            output.append("-- 种子数据")
            output.append(seed)

    output.append("-- ============================================================")
    output.append("-- 外键约束开启（按依赖顺序）")
    output.append("SET FOREIGN_KEY_CHECKS = 1;")
    output.append("-- ============================================================")

    sql_content = '\n'.join(output)
    out_path = '/workspace/miniLiugl/sql/minimax-v681-schema.sql'
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(sql_content)

    print(f"生成 {len(all_tables)} 个表 → {out_path}")

    # 生成 fix 脚本
    fix_path = '/workspace/miniLiugl/sql/fix-v6.8.1-*.sql'
    import glob
    for old in glob.glob(fix_path):
        if 'minimax-v681-schema' not in old:
            os.remove(old)
            print(f"删除旧SQL: {old}")

    print("完成!")

if __name__ == '__main__':
    main()
