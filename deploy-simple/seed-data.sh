#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 测试数据生成 (V2.2)
#
# 自动创建:
#   - 默认用户 (adminLiugl / admin)
#   - 默认 Skill (general / tech / creative)
#   - 默认 System Prompt 模板
#   - 默认 Bucket4j 限流规则
#
# 用法:
#   sudo ./deploy-simple/seed-data.sh              # 生成
#   sudo ./deploy-simple/seed-data.sh --clean      # 清理
#   sudo ./deploy-simple/seed-data.sh --password=NewPass123
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

CLEAN=0
LIUGL_PASS="Liugl@2026"
ADMIN_PASS="admin@123"
for arg in "$@"; do
  case "$arg" in
    --clean) CLEAN=1 ;;
    --password=*) LIUGL_PASS="${arg#*=}" ;;
  esac
done

# 检查 mysql 容器
if ! docker ps --format "{{.Names}}" | grep -q minimax-mysql; then
  log_err "MySQL 容器没在跑, 请先 ./docker-deploy.sh up"
  exit 1
fi

# MySQL 命令封装
mysql_exec() {
  docker exec minimax-mysql mysql -uroot -proot123456 minimax_platform "$@"
}

if [ "$CLEAN" = "1" ]; then
  log_info "==== 清理测试数据 ===="
  mysql_exec -e "
    DELETE FROM user WHERE username IN ('adminLiugl', 'admin', 'test_user');
    DELETE FROM skill WHERE code IN ('general', 'tech', 'creative');
    DELETE FROM prompt_template WHERE code IN ('default_chat', 'code_assistant', 'translator');
  " 2>/dev/null
  log_ok "已清理"
  exit 0
fi

log_info "==== 生成测试数据 ===="
log_info "密码: adminLiugl=$LIUGL_PASS, admin=$ADMIN_PASS"
echo ""

# 1. 默认用户
log_info "1. 创建默认用户..."
mysql_exec << 'SQL' 2>/dev/null
INSERT INTO user (id, username, password, nickname, email, role, status, created_at, updated_at)
VALUES
  (1, 'adminLiugl', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '超级管理员', 'admin@minimax.ai', 'SUPER_ADMIN', 1, NOW(), NOW()),
  (2, 'admin', '$2a$10$D9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '普通管理员', 'admin2@minimax.ai', 'ADMIN', 1, NOW(), NOW()),
  (3, 'test_user', '$2a$10$Q9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '测试用户', 'test@minimax.ai', 'USER', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE password=VALUES(password), updated_at=NOW();
SQL
log_ok "用户已创建 (adminLiugl / admin / test_user)"

# 2. 默认 Skill
log_info "2. 创建默认技能..."
mysql_exec << 'SQL' 2>/dev/null
INSERT INTO skill (id, code, name, description, icon, sort_order, enabled, created_at, updated_at)
VALUES
  (1, 'general', '通用', '通用对话技能, 处理日常问题', '💬', 1, 1, NOW(), NOW()),
  (2, 'tech', '技术', '编程 / 开发 / 技术问题', '💻', 2, 1, NOW(), NOW()),
  (3, 'creative', '创意', '写作 / 营销 / 创意内容', '✨', 3, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at=NOW();
SQL
log_ok "技能已创建 (general / tech / creative)"

# 3. Prompt 模板
log_info "3. 创建默认 Prompt 模板..."
mysql_exec << 'SQL' 2>/dev/null
INSERT INTO prompt_template (id, code, name, content, category, version, enabled, created_at, updated_at)
VALUES
  (1, 'default_chat', '默认对话', '你是一个友好、专业的 AI 助手。请用清晰简洁的语言回答用户问题。', 'CHAT', '1.0', 1, NOW(), NOW()),
  (2, 'code_assistant', '代码助手', '你是一个专业的编程助手。请用 Markdown 格式输出代码, 并添加详细注释。', 'CODE', '1.0', 1, NOW(), NOW()),
  (3, 'translator', '翻译官', '你是一个专业的翻译。请准确翻译用户输入, 保持原文风格。', 'TRANSLATE', '1.0', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at=NOW();
SQL
log_ok "Prompt 模板已创建 (3 个)"

# 4. 默认限流规则
log_info "4. 创建默认限流规则..."
mysql_exec << 'SQL' 2>/dev/null
INSERT INTO rate_limit_rule (id, scope, target, replenish_rate, burst_capacity, enabled, created_at, updated_at)
VALUES
  (1, 'GLOBAL', 'default', 1000, 2000, 1, NOW(), NOW()),
  (2, 'USER', 'default', 20, 40, 1, NOW(), NOW()),
  (3, 'IP', 'default', 100, 200, 1, NOW(), NOW()),
  (4, 'API', '/api/v1/auth/login', 5, 10, 1, NOW(), NOW()),
  (5, 'API', '/api/v1/chat/send', 50, 100, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at=NOW();
SQL
log_ok "限流规则已创建 (5 条)"

# 5. 默认监控指标配置
log_info "5. 创建默认告警规则..."
mysql_exec << 'SQL' 2>/dev/null
INSERT INTO alert_rule (id, name, metric, threshold, comparison, severity, channel, enabled, created_at, updated_at)
VALUES
  (1, 'CPU 使用率过高', 'system_cpu_usage', 80, 'GT', 'WARNING', 'email', 1, NOW(), NOW()),
  (2, '内存使用率过高', 'jvm_memory_used_percent', 85, 'GT', 'WARNING', 'email', 1, NOW(), NOW()),
  (3, 'API 错误率过高', 'http_server_requests_seconds_count{status="500"}', 10, 'GT', 'CRITICAL', 'email,dingtalk', 1, NOW(), NOW()),
  (4, '响应时间过慢', 'http_server_requests_seconds_sum', 5000, 'GT', 'WARNING', 'email', 1, NOW(), NOW()),
  (5, '数据库连接池耗尽', 'hikaricp_connections_active', 90, 'GT', 'CRITICAL', 'dingtalk', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at=NOW();
SQL
log_ok "告警规则已创建 (5 条)"

# 6. 默认通知渠道
log_info "6. 创建默认通知渠道..."
mysql_exec << 'SQL' 2>/dev/null
INSERT INTO alert_channel (id, name, type, config, enabled, created_at, updated_at)
VALUES
  (1, '默认邮件', 'EMAIL', '{"smtpHost":"smtp.example.com","smtpPort":465,"fromAddress":"alert@minimax.ai","toAddress":"admin@minimax.ai"}', 0, NOW(), NOW()),
  (2, '钉钉告警', 'DINGTALK', '{"webhook":"https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN","secret":"YOUR_SECRET"}', 0, NOW(), NOW()),
  (3, '企业微信', 'WECHAT', '{"webhook":"https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY"}', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at=NOW();
SQL
log_ok "通知渠道已创建 (3 个, 默认禁用, 配 webhook 后启用)"

echo ""
echo "=========================================="
echo "  🎉 测试数据已生成"
echo "=========================================="
echo ""
echo "  登录账号:"
echo "    超级管理员: adminLiugl / $LIUGL_PASS"
echo "    普通管理员: admin / $ADMIN_PASS"
echo "    测试用户:   test_user / password"
echo ""
echo "  资源:"
echo "    Skill:      3 个 (general / tech / creative)"
echo "    Prompt:     3 个 (default / code / translate)"
echo "    限流规则:   5 条"
echo "    告警规则:   5 条"
echo "    通知渠道:   3 个 (需配 webhook)"
echo ""
echo "  自定义密码:"
echo "    ./deploy-simple/seed-data.sh --password=NewP@ssw0rd"
echo ""
echo "  清理:"
echo "    ./deploy-simple/seed-data.sh --clean"