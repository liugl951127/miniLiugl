#!/usr/bin/env bash
# MiniMax Platform - 关键文件存在性自检
set -e
cd "$(dirname "$0")/.."

echo "=== 自我检测 ($(date '+%Y-%m-%d %H:%M:%S')) ==="
echo ""
echo "[1] 关键文件存在性..."
MISSING=0
TOTAL=0
MISSING_LIST=()
for f in \
  PROGRESS.md README.md CHANGELOG.md LICENSE .gitignore docker-compose.yml \
  sql/init-minimax.sql sql/init/init-minimax.sql \
  backend/pom.xml \
  backend/minimax-auth/pom.xml \
  backend/minimax-auth/src/main/java/com/minimax/auth/AuthApplication.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/config/SecurityConfig.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/config/MybatisPlusConfig.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/controller/AuthController.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/jwt/JwtTokenProvider.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/jwt/JwtProperties.java \
  backend/minimax-common/src/main/java/com/minimax/common/security/JwtAuthenticationFilter.java \
  backend/minimax-common/src/main/java/com/minimax/common/security/RestAuthEntryPoint.java \
  backend/minimax-common/src/main/java/com/minimax/common/security/RestAccessDeniedHandler.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/service/AuthService.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/service/impl/AuthServiceImpl.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/entity/SysUser.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/entity/SysRole.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/entity/SysUserRole.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/entity/AuthRefreshToken.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/entity/AuthLoginLog.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/mapper/SysUserMapper.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/mapper/SysRoleMapper.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/mapper/SysUserRoleMapper.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/mapper/AuthRefreshTokenMapper.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/mapper/AuthLoginLogMapper.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/dto/LoginRequest.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/dto/RegisterRequest.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/dto/RefreshRequest.java \
  backend/minimax-auth/src/main/java/com/minimax/auth/vo/LoginResponse.java \
  backend/minimax-auth/src/main/resources/application.yml \
  backend/minimax-auth/src/main/resources/mapper/SysUserMapper.xml \
  backend/minimax-auth/src/main/resources/mapper/AuthRefreshTokenMapper.xml \
  backend/minimax-auth/src/test/java/com/minimax/auth/JwtTokenProviderTest.java \
  backend/minimax-gateway/pom.xml \
  backend/minimax-gateway/src/main/java/com/minimax/gateway/GatewayApplication.java \
  backend/minimax-gateway/src/main/java/com/minimax/gateway/filter/JwtAuthGlobalFilter.java \
  backend/minimax-common/pom.xml \
  backend/minimax-common/src/main/java/com/minimax/common/result/Result.java \
  backend/minimax-common/src/main/java/com/minimax/common/result/ResultCode.java \
  backend/minimax-common/src/main/java/com/minimax/common/exception/BizException.java \
  backend/minimax-common/src/main/java/com/minimax/common/exception/GlobalExceptionHandler.java \
  backend/minimax-common/src/main/java/com/minimax/common/utils/IpUtils.java \
  frontend/package.json \
  frontend/vite.config.js \
  frontend/index.html \
  frontend/src/main.js \
  frontend/src/App.vue \
  frontend/src/api/http.js \
  frontend/src/api/auth.js \
  frontend/src/api/system.js \
  frontend/src/router/index.js \
  frontend/src/store/user.js \
  frontend/src/layout/Index.vue \
  frontend/src/views/auth/Login.vue \
  frontend/src/views/chat/Index.vue \
  frontend/src/views/knowledge/Index.vue \
  frontend/src/views/memory/Index.vue \
  frontend/src/views/admin/Index.vue \
  frontend/src/views/About.vue \
  scripts/daily-build.sh \
  scripts/daily-pipeline.sh \
  scripts/java-static-check.sh \
  scripts/send-daily-report.py \
  scripts/git-push.sh \
  scripts/setup-cron.sh \
  scripts/self-check.sh \
  deploy/README.md \
  deploy/prometheus/prometheus.yml \
  reports/day-2-report.md \
  reports/day-3-report.md \
  reports/runbook-day-3.md \
  reports/next-day.txt; do
    TOTAL=$((TOTAL+1))
    if [ -f "$f" ]; then
        echo "  ✅ $f"
    else
        echo "  ❌ $f 缺失"
        MISSING=$((MISSING+1))
        MISSING_LIST+=("$f")
    fi
done
echo ""
echo "[2] 结果: $((TOTAL-MISSING))/$TOTAL 通过"
[ "$MISSING" = "0" ] && echo "    ✅ 0 缺失 - 可以 push" || { echo "    ❌ $MISSING 个文件缺失"; printf '    - %s\n' "${MISSING_LIST[@]}"; }
echo ""
echo "[3] 文件统计..."
JAVA=$(find backend -name "*.java" 2>/dev/null | wc -l)
VUE=$(find frontend/src -name "*.vue" 2>/dev/null | wc -l)
JS=$(find frontend/src -name "*.js" 2>/dev/null | wc -l)
SQL=$(find sql -name "*.sql" 2>/dev/null | wc -l)
MD=$(find . -maxdepth 3 -name "*.md" -not -path "*/node_modules/*" 2>/dev/null | wc -l)
echo "  Java: $JAVA / Vue: $VUE / JS: $JS / SQL: $SQL / MD: $MD"
echo ""
echo "[4] Java 静态体检 (轻量)..."
[ -f scripts/java-static-check.sh ] && bash scripts/java-static-check.sh 2>&1 | tail -3 || echo "  跳过"
