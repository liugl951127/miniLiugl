#!/usr/bin/env bash
# ============================================================
# scripts/init-sql.sh
# 一键跑 sql/init/ 下所有 SQL (脚本调用专用)
# 路径以 sql/init/ 为准
#
# 用法:
#   bash scripts/init-sql.sh              # 跑全部
#   bash scripts/init-sql.sh --force      # 强制重置 (DROP DATABASE 后重建)
#   bash scripts/init-sql.sh --dry-run    # 只列出文件, 不执行
# ============================================================

set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SQL_DIR="$ROOT/sql/init"
DB_USER="${DB_USER:-root}"
DB_NAME="minimax"
# 默认 localhost 走 socket (MariaDB default), 127.0.0.1 走 TCP
DB_HOST="${DB_HOST:-localhost}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

FORCE=false
DRY_RUN=false
for arg in "$@"; do
  case $arg in
    --force) FORCE=true ;;
    --dry-run) DRY_RUN=true ;;
    *) echo -e "${RED}未知参数: $arg${NC}"; exit 1 ;;
  esac
done

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  MiniMax SQL 初始化${NC}"
echo -e "${CYAN}  目录: $SQL_DIR${NC}"
echo -e "${CYAN}========================================${NC}"

# 检查 mysql 命令
if ! command -v mysql >/dev/null 2>&1; then
  echo -e "${RED}✗ mysql 命令不存在${NC}"
  echo "  安装: apt-get install -y default-mysql-client  或  mariadb-client"
  exit 1
fi

# 检查连接
if ! mysql -u"$DB_USER" -h "$DB_HOST" -e "SELECT 1" >/dev/null 2>&1; then
  echo -e "${RED}✗ MySQL/MariaDB 未启动 或 $DB_USER@$DB_HOST 连不上${NC}"
  echo "  启 DB:"
  echo "    nohup mariadbd --user=mysql --bind-address=127.0.0.1 > logs/mariadb.log 2>&1 &"
  exit 1
fi

# 列出 SQL 文件
echo -e "\n${YELLOW}[1/3] 扫描 SQL 文件...${NC}"
if [ ! -d "$SQL_DIR" ]; then
  echo -e "${RED}✗ 目录不存在: $SQL_DIR${NC}"
  exit 1
fi

# 按文件名升序 (01_, 02_, ... 已天然有序)
SQL_FILES=()
for f in "$SQL_DIR"/*.sql; do
  [ -f "$f" ] && SQL_FILES+=("$f")
done

if [ ${#SQL_FILES[@]} -eq 0 ]; then
  echo -e "${RED}✗ 未找到 SQL 文件${NC}"
  exit 1
fi

echo -e "  ${GREEN}找到 ${#SQL_FILES[@]} 个 SQL 文件:${NC}"
i=0
for f in "${SQL_FILES[@]}"; do
  i=$((i+1))
  printf "    %2d. %s\n" "$i" "$(basename "$f")"
done

if $DRY_RUN; then
  echo -e "\n${YELLOW}[dry-run] 不执行, 退出${NC}"
  exit 0
fi

# 强制重置
if $FORCE; then
  echo -e "\n${YELLOW}[2/3] --force 模式: DROP DATABASE $DB_NAME${NC}"
  mysql -u"$DB_USER" -h "$DB_HOST" -e "DROP DATABASE IF EXISTS $DB_NAME;"
fi

# 建库
echo -e "\n${YELLOW}[2/3] 建库 + 用户...${NC}"
mysql -u"$DB_USER" -h "$DB_HOST" -e "CREATE DATABASE IF NOT EXISTS $DB_NAME DEFAULT CHARSET utf8mb4;"
mysql -u"$DB_USER" -h "$DB_HOST" -e "CREATE USER IF NOT EXISTS 'minimax'@'127.0.0.1' IDENTIFIED BY 'minimax_pass_2024';" 2>/dev/null || true
mysql -u"$DB_USER" -h "$DB_HOST" -e "GRANT ALL ON $DB_NAME.* TO 'minimax'@'127.0.0.1'; FLUSH PRIVILEGES;" 2>/dev/null || true
echo -e "  ${GREEN}✓ 数据库 $DB_NAME${NC}"

# 跑 SQL
echo -e "\n${YELLOW}[3/3] 跑 SQL...${NC}"
TOTAL=0
OK=0
FAIL=0
for f in "${SQL_FILES[@]}"; do
  TOTAL=$((TOTAL+1))
  NAME=$(basename "$f")
  printf "  [%2d/%d] %-30s " "$TOTAL" "${#SQL_FILES[@]}" "$NAME"
  if mysql -u"$DB_USER" -h "$DB_HOST" "$DB_NAME" < "$f" >/tmp/sql_err 2>&1; then
    echo -e "${GREEN}OK${NC}"
    OK=$((OK+1))
  else
    echo -e "${RED}FAIL${NC}"
    cat /tmp/sql_err | head -2 | sed 's/^/      /'
    FAIL=$((FAIL+1))
  fi
done

# 验证
echo -e "\n${YELLOW}验证:${NC}"
TABLES=$(mysql -u"$DB_USER" -h "$DB_HOST" "$DB_NAME" -se "SHOW TABLES;" 2>/dev/null | wc -l)
PROVIDERS=$(mysql -u"$DB_USER" -h "$DB_HOST" "$DB_NAME" -se "SELECT COUNT(*) FROM model_provider;" 2>/dev/null)
MODELS=$(mysql -u"$DB_USER" -h "$DB_HOST" "$DB_NAME" -se "SELECT COUNT(*) FROM model_config;" 2>/dev/null)
echo -e "  ${GREEN}✓${NC} 表数: $TABLES"
echo -e "  ${GREEN}✓${NC} provider 数: $PROVIDERS"
echo -e "  ${GREEN}✓${NC} model 数: $MODELS"

# 补 mock (V4 沙箱)
echo -e "\n${YELLOW}补 V4 mock provider/model...${NC}"
mysql -u"$DB_USER" -h "$DB_HOST" "$DB_NAME" <<'SQL' 2>/dev/null || true
INSERT IGNORE INTO model_provider (code,name,base_url,api_key,protocol,enabled,sort,description)
VALUES ('mock','Mock Adapter','http://localhost','mock','mock',1,99,'沙箱演示用, 无 key 也能跑');

INSERT IGNORE INTO model_config (provider_id,model_code,display_name,max_context,max_output,supports_stream,enabled,sort,description)
SELECT id,'mock','Mock (沙箱演示)',8000,2048,1,1,99,'沙箱无 key 演示' FROM model_provider WHERE code='mock';
SQL
echo -e "  ${GREEN}✓${NC} mock 已就绪"

echo -e "\n${CYAN}========================================${NC}"
if [ $FAIL -eq 0 ]; then
  echo -e "${GREEN}  ✓ 全部 $OK/$TOTAL 个 SQL 执行成功${NC}"
else
  echo -e "${RED}  ✗ $FAIL 个 SQL 失败, $OK 个成功${NC}"
fi
echo -e "${CYAN}========================================${NC}"
echo ""
echo "下一步:"
echo "  1. 启服务: bash scripts/start-platform.sh"
echo "  2. 或单独: bash scripts/start-all.sh"
echo "  3. 登录:"
echo "     curl -X POST http://localhost:8081/auth/login \\"
echo "       -H 'Content-Type: application/json' \\"
echo "       -d '{\"username\":\"adminLiugl\",\"password\":\"Liugl@2026\"}'"
