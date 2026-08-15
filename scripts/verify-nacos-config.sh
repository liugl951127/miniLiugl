#!/usr/bin/env bash
# V5.4+ 验证 7 个 AI 业务 module 都正确配置 nacos
set -e
echo "================================================"
echo "AI 业务模块 Nacos 注册配置验证"
echo "================================================"

for m in minimax-ai minimax-agent minimax-model minimax-pipeline minimax-rag minimax-multimodal minimax-chat; do
  f="backend/$m/src/main/resources/application-mysql.yml"
  if [ -f "$f" ]; then
    if grep -q "nacos" "$f"; then
      nacos_lines=$(grep -A 3 "nacos:" "$f" | head -5 | sed 's/^/      /')
    else
      nacos_lines="      (无 nacos 配置 - 继承 common.yml, 默认 NACOS_ENABLED=true)"
    fi
    
    # 启动类
    appfile=$(find backend/$m -name "*.java" -path "*/main/*" | xargs grep -l "SpringBootApplication" 2>/dev/null | head -1)
    has_disco=$(grep -l "EnableDiscoveryClient" "$appfile" 2>/dev/null && echo "✓" || echo "✗")
    
    # pom nacos 依赖
    has_dep=$(grep -q "nacos-discovery" backend/$m/pom.xml && echo "✓" || echo "✗")
    
    echo ""
    echo "[$m]"
    echo "  nacos 配置:"
    echo "$nacos_lines"
    echo "  @EnableDiscoveryClient: $has_disco"
    echo "  pom 依赖: $has_dep"
  fi
done
echo "================================================"
