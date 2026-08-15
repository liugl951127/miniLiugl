#!/bin/bash
# V6.3+ SQL 部署验证脚本
# 验证 8 步: 表数 / 触发器 / 存储过程 / 函数 / 事件 / 视图 / 索引 / 种子

set -e
DB_HOST=${DB_HOST:-127.0.0.1}
DB_PORT=${DB_PORT:-3306}
DB_USER=${DB_USER:-root}
DB_PASS=${DB_PASS:-minimax_mariadb_2024}
DB_NAME=${DB_NAME:-minimax_platform}
MYSQL="mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS -N -B $DB_NAME"

echo "========================================="
echo "MiniMax SQL 部署验证 (V6.3+)"
echo "========================================="
echo ""

PASS=0
FAIL=0
TOTAL=8

# 1. 表数
echo "▶ 1/8 表数 (期望 89+)"
TABLES=$($MYSQL -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$DB_NAME';" 2>/dev/null)
if [ "$TABLES" -ge 89 ]; then
  echo "  ✓ 通过: $TABLES 张表"
  PASS=$((PASS+1))
else
  echo "  ✗ 失败: 只有 $TABLES 张表 (期望 89+)"
  FAIL=$((FAIL+1))
fi
echo ""

# 2. 触发器
echo "▶ 2/8 触发器 (期望 4)"
TRIGGERS=$($MYSQL -e "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema = '$DB_NAME';" 2>/dev/null)
if [ "$TRIGGERS" -ge 4 ]; then
  echo "  ✓ 通过: $TRIGGERS 个触发器"
  PASS=$((PASS+1))
else
  echo "  ✗ 失败: 只有 $TRIGGERS 个 (期望 4)"
  FAIL=$((FAIL+1))
fi
echo ""

# 3. 存储过程
echo "▶ 3/8 存储过程 (期望 4)"
PROCS=$($MYSQL -e "SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema = '$DB_NAME' AND routine_type = 'PROCEDURE';" 2>/dev/null)
if [ "$PROCS" -ge 4 ]; then
  echo "  ✓ 通过: $PROCS 个存储过程"
  PASS=$((PASS+1))
else
  echo "  ✗ 失败: 只有 $PROCS 个 (期望 4)"
  FAIL=$((FAIL+1))
fi
echo ""

# 4. 函数
echo "▶ 4/8 函数 (期望 4)"
FUNCS=$($MYSQL -e "SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema = '$DB_NAME' AND routine_type = 'FUNCTION';" 2>/dev/null)
if [ "$FUNCS" -ge 4 ]; then
  echo "  ✓ 通过: $FUNCS 个函数"
  PASS=$((PASS+1))
else
  echo "  ✗ 失败: 只有 $FUNCS 个 (期望 4)"
  FAIL=$((FAIL+1))
fi
echo ""

# 5. 事件
echo "▶ 5/8 事件 (期望 2)"
EVENTS=$($MYSQL -e "SELECT COUNT(*) FROM information_schema.events WHERE event_schema = '$DB_NAME';" 2>/dev/null)
if [ "$EVENTS" -ge 2 ]; then
  echo "  ✓ 通过: $EVENTS 个事件"
  PASS=$((PASS+1))
else
  echo "  ✗ 失败: 只有 $EVENTS 个 (期望 2)"
  FAIL=$((FAIL+1))
fi
echo ""

# 6. 视图
echo "▶ 6/8 视图 (期望 2)"
VIEWS=$($MYSQL -e "SELECT COUNT(*) FROM information_schema.views WHERE table_schema = '$DB_NAME';" 2>/dev/null)
if [ "$VIEWS" -ge 2 ]; then
  echo "  ✓ 通过: $VIEWS 个视图"
  PASS=$((PASS+1))
else
  echo "  ✗ 失败: 只有 $VIEWS 个 (期望 2)"
  FAIL=$((FAIL+1))
fi
echo ""

# 7. 索引 (V6.3+ 10 索引)
echo "▶ 7/8 索引 (期望 10+)"
INDEXES=$($MYSQL -e "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = '$DB_NAME' AND index_name LIKE 'idx_%';" 2>/dev/null)
if [ "$INDEXES" -ge 10 ]; then
  echo "  ✓ 通过: $INDEXES 个 idx_ 索引"
  PASS=$((PASS+1))
else
  echo "  ✗ 失败: 只有 $INDEXES 个 (期望 10+)"
  FAIL=$((FAIL+1))
fi
echo ""

# 8. 5 测试账号
echo "▶ 8/8 5 测试账号 (admin/demo/test/guest/vip)"
USERS=$($MYSQL -e "SELECT COUNT(*) FROM sys_user WHERE username IN ('admin','demo','test','guest','vip') AND deleted = 0;" 2>/dev/null)
if [ "$USERS" -eq 5 ]; then
  echo "  ✓ 通过: 5 个测试账号"
  PASS=$((PASS+1))
else
  echo "  ✗ 失败: 只有 $USERS 个 (期望 5)"
  FAIL=$((FAIL+1))
fi
echo ""

# 9. 3 租户
echo "▶ 9/9 3 租户 (default/enterprise/startup)"
TENANTS=$($MYSQL -e "SELECT COUNT(*) FROM tenant WHERE code IN ('default','enterprise','startup');" 2>/dev/null)
if [ "$TENANTS" -ge 3 ]; then
  echo "  ✓ 通过: $TENANTS 个核心租户"
  PASS=$((PASS+1))
else
  echo "  ✗ 失败: 只有 $TENANTS 个 (期望 3)"
  FAIL=$((FAIL+1))
fi
echo ""

# 10. schema_version
echo "▶ 10/10 schema_version (期望 V6.3.0/1/2)"
VERSIONS=$($MYSQL -e "SELECT COUNT(*) FROM schema_version WHERE version LIKE 'V6.3%';" 2>/dev/null)
if [ "$VERSIONS" -ge 3 ]; then
  echo "  ✓ 通过: $VERSIONS 个 V6.3 版本"
  PASS=$((PASS+1))
else
  echo "  ✗ 失败: 只有 $VERSIONS 个 (期望 3)"
  FAIL=$((FAIL+1))
fi
echo ""

# 总结
echo "========================================="
echo "总结: $PASS / 10 通过"
if [ $FAIL -eq 0 ]; then
  echo "✓ 全部通过 - 部署成功"
  exit 0
else
  echo "✗ $FAIL 项失败 - 请检查 minimax-mysql-final.sql"
  exit 1
fi
