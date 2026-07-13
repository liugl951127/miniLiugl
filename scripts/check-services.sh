#!/usr/bin/env bash
# =============================================================
# 检查 16 个服务的启动日志, 找共性问题
# 用法: bash scripts/check-services.sh
# =============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

green() { echo -e "\033[32m$*\033[0m"; }
red()   { echo -e "\033[31m$*\033[0m"; }
yellow(){ echo -e "\033[33m$*\033[0m"; }
bold()  { echo -e "\033[1m$*\033[0m"; }

bold ""
bold "🔍 检查 16 个服务的状态 + 关键日志"
bold ""

# ============== 1. 检查 pom.xml packaging ==============
bold "[1/4] 检查 packaging 类型"
echo ""
printf "  %-15s %-12s %s\n" "模块" "packaging" "main class"
printf "  %-15s %-12s %s\n" "------" "----------" "----------"
for svc in admin agent ai analytics auth chat common function gateway memory model monitor multimodal pipeline prompt rag ws; do
    pkg=$(grep "<packaging>" backend/minimax-$svc/pom.xml 2>/dev/null | sed -E 's|.*<packaging>([^<]+).*|\1|' | head -1)
    pkg=${pkg:-"jar"}  # 默认 jar
    
    # 找 main class (有 @SpringBootApplication 的)
    main_file=$(grep -rl "@SpringBootApplication" backend/minimax-$svc/src 2>/dev/null | head -1)
    if [ -n "$main_file" ]; then
        main_class=$(basename $main_file .java)
        main_disp="✓ $main_class"
    else
        main_disp="✗ 无"
    fi
    
    if [ "$svc" = "common" ]; then
        if [ "$pkg" = "pom" ]; then
            printf "  %-15s \033[32m%-12s\033[0m %s\n" "$svc" "$pkg (✓ lib)" "$main_disp"
        else
            printf "  %-15s \033[31m%-12s\033[0m %s (✗ 应该是 pom)\n" "$svc" "$pkg" "$main_disp"
        fi
    else
        if [ "$pkg" = "jar" ]; then
            printf "  %-15s %-12s %s\n" "$svc" "$pkg" "$main_disp"
        else
            printf "  %-15s \033[33m%-12s\033[0m %s (⚠  应该是 jar)\n" "$svc" "$pkg" "$main_disp"
        fi
    fi
done
echo ""

# ============== 2. 检查 docker-compose 服务列表 ==============
bold "[2/4] 检查 docker-compose.yml 服务列表"
echo ""
if [ -f docker-compose.yml ]; then
    grep "container_name: minimax-" docker-compose.yml | awk '{print "  " $2}' | sort
    echo ""
    if grep -q "minimax-common" docker-compose.yml; then
        red "  ❌ docker-compose.yml 仍包含 minimax-common (应该删除)"
    else
        green "  ✓ minimax-common 已从 docker-compose.yml 删除"
    fi
fi
if [ -f docker-compose.mini.yml ]; then
    if grep -q "minimax-common" docker-compose.mini.yml; then
        red "  ❌ docker-compose.mini.yml 仍包含 minimax-common (应该删除)"
    else
        green "  ✓ minimax-common 不在 docker-compose.mini.yml"
    fi
fi
echo ""

# ============== 3. 检查启动日志 ==============
bold "[3/4] 检查最近启动日志 (找常见错误)"
echo ""
if command -v docker &>/dev/null; then
    for svc in minimax-auth minimax-ai minimax-gateway; do
        if docker ps -a --format '{{.Names}}' 2>/dev/null | grep -q "^${svc}$"; then
            state=$(docker inspect -f '{{.State.Status}}' $svc 2>/dev/null)
            health=$(docker inspect -f '{{.State.Health.Status}}' $svc 2>/dev/null)
            echo "  $svc: state=$state health=$health"
        else
            echo "  $svc: 未运行"
        fi
    done
else
    yellow "  Docker 不可用, 跳过"
fi
echo ""

# ============== 4. 检查 @TableField 命名 ==============
bold "[4/4] 检查 47 个 @TableField 显式映射"
echo ""
if [ -f scripts/check_tablefield.py ]; then
    python3 scripts/check_tablefield.py 2>&1 | head -10
fi
echo ""
green "✅ 检查完成"
