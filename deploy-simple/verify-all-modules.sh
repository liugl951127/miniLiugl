#!/usr/bin/env bash
# =============================================================
# MiniMax Platform - 16 模块独立打包验证 (V1.9.1)
# 测试每个模块能否独立 mvn package 成功, 并验证产物
# =============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND="$PROJECT_ROOT/backend"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }
log_err()  { echo -e "${RED}[✗]${NC} $*"; }

# 前置检查
if ! command -v java &>/dev/null || ! command -v mvn &>/dev/null; then
  log_err "JDK 或 Maven 未安装"
  exit 1
fi

# 阿里云镜像
mkdir -p ~/.m2
if ! grep -q "aliyun" ~/.m2/settings.xml 2>/dev/null; then
  cat > ~/.m2/settings.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <mirrors>
    <mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror>
  </mirrors>
</settings>
EOF
fi

cd "$BACKEND"

# 16 个模块 (按业务顺序)
MODULES=(
  "minimax-common"      # 公共包 (非微服务, 但所有 jar 依赖它)
  "minimax-auth"        # 8081
  "minimax-chat"        # 8082
  "minimax-memory"      # 8083
  "minimax-model"       # 8084
  "minimax-rag"         # 8085
  "minimax-function"    # 8086
  "minimax-multimodal"  # 8087
  "minimax-agent"       # 8088
  "minimax-monitor"     # 8089
  "minimax-admin"       # 8090
  "minimax-prompt"      # 8091
  "minimax-analytics"   # 8092
  "minimax-pipeline"    # 8093
  "minimax-ws"          # 8095
  "minimax-gateway"     # 7080
)

declare -A RESULTS
declare -A SIZES
declare -A MAIN_CLASS
FAIL=0
START_TIME=$(date +%s)

for module in "${MODULES[@]}"; do
  echo ""
  log_info "==== [$module] 独立编译 ===="
  BUILD_START=$(date +%s)

  if ! mvn -B -pl "minimax-common,$module" -am clean package \
       -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true -q 2>&1 | tail -3; then
    log_err "$module 编译失败"
    RESULTS[$module]="FAIL"
    FAIL=$((FAIL+1))
    continue
  fi

  BUILD_END=$(date +%s)
  BUILD_DUR=$((BUILD_END - BUILD_START))

  # 找产物
  if [ "$module" = "minimax-common" ]; then
    JAR="$module/target/minimax-common.jar"
    EXPECTED_TYPE="library"
  else
    JAR="$module/target/${module}-spring-boot.jar"
    EXPECTED_TYPE="spring-boot"
  fi

  if [ ! -f "$JAR" ]; then
    log_err "$module 编译但产物缺失: $JAR"
    RESULTS[$module]="FAIL"
    FAIL=$((FAIL+1))
    continue
  fi

  SIZE=$(du -h "$JAR" | awk '{print $1}')
  SIZES[$module]=$SIZE
  RESULTS[$module]="OK"

  # 提取 main class (仅 spring-boot)
  if [ "$EXPECTED_TYPE" = "spring-boot" ]; then
    MAIN=$(unzip -p "$JAR" META-INF/MANIFEST.MF 2>/dev/null | grep "Start-Class:" | awk '{print $2}' | tr -d '\r')
    if [ -n "$MAIN" ]; then
      MAIN_CLASS[$module]=$MAIN
      log_ok "$module: $JAR ($SIZE, $BUILD_DUR s, main=$MAIN)"
    else
      log_ok "$module: $JAR ($SIZE, $BUILD_DUR s)"
    fi
  else
    log_ok "$module: $JAR ($SIZE, $BUILD_DUR s, 公共库)"
  fi
done

# ============================================================
# 汇总
# ============================================================
END_TIME=$(date +%s)
TOTAL=$((END_TIME - START_TIME))

echo ""
echo "=========================================="
echo "  📊 16 模块独立打包验证结果"
echo "=========================================="
printf "%-25s %-10s %-10s %s\n" "MODULE" "TYPE" "SIZE" "MAIN_CLASS"
printf "%-25s %-10s %-10s %s\n" "------" "----" "----" "----------"

for module in "${MODULES[@]}"; do
  r="${RESULTS[$module]:-?}"
  s="${SIZES[$module]:--}"
  m="${MAIN_CLASS[$module]:--}"
  if [ "$module" = "minimax-common" ]; then
    TYPE="library"
  else
    TYPE="spring-boot"
  fi
  if [ "$r" = "OK" ]; then
    printf "%-25s %-10s %-10s %s\n" "$module" "$TYPE" "$s" "$m"
  else
    printf "%-25s ${RED}%-10s${NC} %-10s %s\n" "$module" "$r" "-" "-"
  fi
done

echo ""
echo "通过: $((16 - FAIL))/16, 失败: $FAIL, 总耗时: ${TOTAL}s"
echo ""

if [ $FAIL -gt 0 ]; then
  log_err "有 $FAIL 个模块打包失败"
  exit 1
fi

log_ok "全部 16 个模块独立打包成功 🎉"