#!/bin/bash
# V6.7+ SQL 静态 + 9 维检查 (本地用, CI 用 .github/workflows/verify-sql-real.yml)

set -e
SQL=${1:-sql/minimax-mysql-final.sql}

echo "════════════════════════════════════════"
echo "V6.7+ SQL 静态 9 维检查"
echo "════════════════════════════════════════"
echo ""

# 1. 静态 9 维
python3 scripts/sql-validate.py "$SQL"

echo ""
echo "════════════════════════════════════════"
echo "✅ 本地静态检查通过"
echo ""
echo "真跑验证 (需 Docker 或 mariadb-client):"
echo "  docker run -d --name mariadb-test -e MARIADB_ROOT_PASSWORD=test mariadb:10.6"
echo "  mariadb -h127.0.0.1 -uroot -ptest test < $SQL"
echo "  或: GitHub Actions 自动跑 (verify-sql-real.yml)"
