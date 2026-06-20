#!/bin/bash
# =============================================================
# scripts/refactor-yml.sh
# 重构 13 个 application.yml 引用 application-common.yml
# 节省 ~80% 重复配置 (~500 行)
#
# V5.4 冗余重构
# =============================================================
set -e
cd "$(cd "$(dirname "$0")/.." && pwd)/backend"

# 13 个模块 (除 common)
modules=(
  "auth:8081:/"
  "chat:8082:/api/v1"
  "model:8083:/api/v1"
  "memory:8084:/api/v1"
  "rag:8085:/api/v1"
  "function:8086:/api/v1"
  "admin:8087:/api/v1"
  "multimodal:8088:/api/v1"
  "monitor:8089:/api/v1"
  "agent:8090:/api/v1"
  "prompt:8091:/api/v1"
  "ws:8095:/api/v1"
  "gateway:8080:/"
)

for entry in "${modules[@]}"; do
  module=$(echo $entry | cut -d: -f1)
  port=$(echo $entry | cut -d: -f2)
  ctx=$(echo $entry | cut -d: -f3)

  # gateway 模块特殊: 有 Spring Cloud Gateway 路由, 不能用此模板
  if [ "$module" = "gateway" ]; then
    echo "  ⏭  ${module} (Spring Cloud Gateway, skip template, keep own yml)"
    continue
  fi

  yml="minimax-${module}/src/main/resources/application.yml"
  if [ ! -f "$yml" ]; then
    echo "  ❌ ${module} (no yml, skip)"
    continue
  fi

  # 提取模块特殊配置 (minimax.*, 自定义 jwt.access-ttl-seconds 等)
  # 输出时去掉 2 个空格缩进, 因为会被加到 minimax 顶层
  custom=$(awk '
    /^minimax:/ { in_minimax=1; next }
    in_minimax && /^[a-z]/ { in_minimax=0 }
    in_minimax { sub(/^  /, ""); print }
  ' "$yml")
  if [ -z "$custom" ]; then custom="  # (无特殊配置)"; fi

  # 写新 yml
  cat > "$yml" <<YAMLEOF
# =============================================================
# ${module} 模块配置 (V5.4 重构后)
# 公共配置在 application-common.yml, 这里只放模块特有配置
# =============================================================

server:
  port: ${port}
  servlet:
    context-path: ${ctx}

spring:
  config:
    import: classpath:application-common.yml
  application:
    name: minimax-${module}
  profiles:
    active: dev

# 模块特殊配置 (保留原 minimax.* 块)
minimax:
${custom}
YAMLEOF

  echo "  ✓ ${module}:${port}${ctx} ($(wc -l < $yml) lines)"
done

echo ""
echo "=== 重构后总行数 ==="
total=0
count=0
for yml in minimax-*/src/main/resources/application.yml; do
  [ ! -f "$yml" ] && continue
  lines=$(wc -l < "$yml")
  total=$((total + lines))
  count=$((count + 1))
done
echo "  ${count} 个模块 yml 总: $total 行 (重构前 646 行)"
echo "  公共 yml: $(wc -l < minimax-common/src/main/resources/application-common.yml) 行"
echo "  节省: $((646 - total)) 行"