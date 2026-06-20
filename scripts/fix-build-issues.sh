#!/bin/bash
# =============================================================
# scripts/fix-build-issues.sh
# 修复 V5.3 之前的已知打包启动问题
#
# 适用: 在干净环境拉新代码后, 自动修复所有已知问题
# 用法: bash scripts/fix-build-issues.sh
# =============================================================
set -e
cd "$(cd "$(dirname "$0")/.." && pwd)/backend"

echo "=== 1. 去掉 spring-boot-maven-plugin skip ==="
for pom in minimax-*/pom.xml; do
  if grep -q "skip>true</skip>" "$pom"; then
    sed -i 's|<configuration><skip>true</skip></configuration>||g' "$pom"
    echo "  ✓ $(basename $(dirname $pom))"
  fi
done

echo ""
echo "=== 2. 给缺 mysql 依赖的模块加 mysql-connector-j ==="
for m in rag function multimodal monitor ws; do
  pom="minimax-${m}/pom.xml"
  if [ -f "$pom" ] && ! grep -q "mysql-connector-j" "$pom"; then
    python3 -c "
import re
with open('$pom') as f: c = f.read()
if 'mysql-connector-j' not in c:
    c = c.replace('    </dependencies>',
        '''        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>8.0.33</version>
        </dependency>
    </dependencies>''', 1)
    with open('$pom', 'w') as f: f.write(c)
    print('  ✓ ${m} (added mysql-connector-j)')
"
  fi
done

echo ""
echo "=== 3. 移除 application.yml 中的 druid type 引用 ==="
for yml in minimax-*/src/main/resources/application.yml; do
  if [ -f "$yml" ] && grep -q "druid.pool.DruidDataSource" "$yml"; then
    sed -i 's|type: com.alibaba.druid.pool.DruidDataSource||g' "$yml"
    echo "  ✓ $(basename $(dirname $(dirname $yml))) (removed druid type)"
  fi
done

echo ""
echo "=== 4. 给缺 application.yml 的模块创建 ==="
declare -A modules=(
  ["minimax-rag"]=8085
  ["minimax-function"]=8086
  ["minimax-multimodal"]=8088
  ["minimax-monitor"]=8089
  ["minimax-ws"]=8095
)
for mod in "${!modules[@]}"; do
  port=${modules[$mod]}
  yml="${mod}/src/main/resources/application.yml"
  if [ ! -f "$yml" ]; then
    cat > "$yml" <<YAMLEOF
server:
  port: ${port}
  servlet:
    context-path: /

spring:
  application:
    name: ${mod}
  profiles:
    active: dev
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 200MB
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://\${MYSQL_HOST:127.0.0.1}:3306/minimax_platform?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: \${MYSQL_USER:minimax}
    password: \${MYSQL_PASSWORD:minimax_pass_2024}
  data:
    redis:
      host: \${REDIS_HOST:127.0.0.1}
      port: 6379
      password: \${REDIS_PASSWORD:minimax_redis_2024}

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: ASSIGN_ID
      logic-delete-field: deleted

minimax:
  jwt:
    secret: \${JWT_SECRET:VwSWPd816F4nwowFzF5B0F8rihlle2836g6QAh5i13o=}
    header: Authorization
    prefix: "Bearer "
YAMLEOF
    echo "  ✓ ${mod} (created application.yml, port ${port})"
  fi
done

echo ""
echo "=== 5. 重新编译所有模块 ==="
mvn -B -DskipTests -T 1C clean install 2>&1 | tail -3
echo ""
echo "✅ 修复完成, 现在可以执行: bash scripts/dev-start.sh"