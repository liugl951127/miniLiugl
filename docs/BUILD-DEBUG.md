# 🐛 MiniMax 编译启动调试笔记 (V5.3)

> 沙箱环境跑全套 12 微服务时发现的所有问题及解决方案
> 真实生产环境部署前必读

---

## 📑 目录

- [环境限制](#环境限制)
- [问题 1: 4 个模块缺 application.yml](#问题-1-4-个模块缺-applicationyml)
- [问题 2: 多个模块 pom 缺 spring-boot 打包](#问题-2-多个模块-pom-缺-spring-boot-打包)
- [问题 3: 5 个模块缺 mysql-connector-j 依赖](#问题-3-5-个模块缺-mysql-connector-j-依赖)
- [问题 4: ws 模块 yml 缺 datasource](#问题-4-ws-模块-yml-缺-datasource)
- [问题 5: application.yml 引用 druid 但没依赖](#问题-5-applicationyml-引用-druid-但没依赖)
- [问题 6: 沙箱 OOM (生产环境避免)](#问题-6-沙箱-oom-生产环境避免)
- [一键修复脚本](#一键修复脚本)

---

## 环境限制

- 沙箱: **2GB RAM**, Java 17, MySQL 10.11
- 生产: **≥ 4GB RAM** 推荐 (12 微服务 + DB + nginx)
- macOS 开发: **≥ 8GB RAM** (IntelliJ + 12 个 Run Dashboard)

---

## 问题 1: 4 个模块缺 application.yml

### 现象

启动时日志:
```
ERROR Web server failed to start. Port 8080 was already in use.
```

或:
```
Web server failed to start. Port XXXX was already in use.
```

### 原因

`minimax-rag`, `minimax-function`, `minimax-multimodal`, `minimax-monitor` 这 4 个模块的 `src/main/resources/` 目录**没有 `application.yml`**, 只有 `application-test.yml` (H2 测试用).

Spring Boot 默认会去 `application.yml` 找配置, 找不到就 fallback 到默认端口 8080 (跟 gateway 冲突).

### 修复

为每个模块创建 `application.yml`:

```yaml
server:
  port: 8085   # 按模块分配
  servlet:
    context-path: /

spring:
  application:
    name: minimax-rag
  profiles:
    active: dev
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 200MB
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST:127.0.0.1}:3306/minimax_platform?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${MYSQL_USER:minimax}
    password: ${MYSQL_PASSWORD:minimax_pass_2024}
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: 6379
      password: ${REDIS_PASSWORD:minimax_redis_2024}
      database: 0

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: ASSIGN_ID
      logic-delete-field: deleted

minimax:
  jwt:
    secret: ${JWT_SECRET:VwSWPd816F4nwowFzF5B0F8rihlle2836g6QAh5i13o=}
    header: Authorization
    prefix: "Bearer "

logging:
  level:
    root: INFO
    com.minimax: DEBUG
```

> ⚠️ **必须修改 port** (按模块):
> - rag → 8085
> - function → 8086
> - multimodal → 8088
> - monitor → 8089
> - ws → 8095

---

## 问题 2: 多个模块 pom 缺 spring-boot 打包

### 现象

`mvn package` 后:
```
target/minimax-rag-1.0.0-SNAPSHOT.jar  (60KB, thin jar)
```

启动时:
```
Error: no main manifest attribute, in minimax-rag-1.0.0-SNAPSHOT.jar
```

### 原因

`minimax-rag/function/multimodal/monitor/agent` 的 pom 显式关闭了 spring-boot-maven-plugin:

```xml
<plugin>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration><skip>true</skip></configuration>  <!-- ❌ 阻止了 fat jar 打包 -->
</plugin>
```

### 修复

去掉 `<configuration><skip>true</skip></configuration>`:

```bash
# 一键去掉
for pom in backend/minimax-*/pom.xml; do
  if grep -q "skip>true</skip>" "$pom"; then
    sed -i 's|<configuration><skip>true</skip></configuration>||g' "$pom"
    echo "  ✓ $(basename $(dirname $pom)) (skip removed)"
  fi
done
```

### 验证

```bash
mvn -B -DskipTests -T 1C -pl minimax-rag package

ls -la minimax-rag/target/minimax-rag-1.0.0-SNAPSHOT.jar
# 应该 > 50MB (fat jar, 含所有依赖)
```

---

## 问题 3: 5 个模块缺 mysql-connector-j 依赖

### 现象

启动日志:
```
Caused by: java.lang.IllegalStateException: Cannot load driver class: com.mysql.cj.jdbc.Driver
```

### 原因

`minimax-rag/function/multimodal/monitor/ws` 这 5 个模块的 pom **没添加 mysql-connector-j 依赖**, 但 application.yml 里 `driver-class-name: com.mysql.cj.jdbc.Driver`.

(其他模块通过传递依赖间接拿到了, 但这 5 个模块依赖树里没有.)

### 修复

为每个模块 pom 添加依赖 (在 `</dependencies>` 前):

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.0.33</version>
</dependency>
```

或用脚本批量注入 (见末尾 [一键修复脚本](#一键修复脚本)).

---

## 问题 4: ws 模块 yml 缺 datasource

### 现象

```
APPLICATION FAILED TO START
Description: Failed to configure a DataSource: 'url' attribute is not specified
Reason: Failed to determine a suitable driver class
```

### 原因

`minimax-ws/src/main/resources/application.yml` 只有 7 行, 没 datasource:

```yaml
server:
  port: 8095
spring:
  application:
    name: minimax-ws
  jackson:
    time-zone: GMT+8
```

但 ws 模块用了 JWT 过滤器 (引用了 `JwtAuthenticationFilter` 里 `@Autowired` SysUserMapper), 间接需要数据库.

### 修复

加完整 datasource 配置 (同问题 1 的模板, port 改 8095).

---

## 问题 5: application.yml 引用 druid 但没依赖

### 现象

```
Failed to bind properties under 'spring.datasource.type' to java.lang.Class<javax.sql.DataSource>:
  Reason: failed to convert java.lang.String to java.lang.Class<javax.sql.DataSource>
  (caused by java.lang.ClassNotFoundException: com.alibaba.druid.pool.DruidDataSource)
```

### 原因

application.yml 引用了 `type: com.alibaba.druid.pool.DruidDataSource`, 但模块没 druid 依赖.

### 修复 (二选一)

**方案 A: 加 druid 依赖** (推荐, druid 自带 SQL 监控):

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-3-starter</artifactId>
    <version>1.2.20</version>
</dependency>
```

**方案 B: 移除 druid 配置** (用默认 HikariCP):

```bash
sed -i 's|type: com.alibaba.druid.pool.DruidDataSource||g' \
  backend/minimax-*/src/main/resources/application.yml
```

### 已应用

本次选方案 B (HikariCP 性能更好, Spring Boot 默认).

---

## 问题 6: 沙箱 OOM (生产环境避免)

### 现象

`dmesg`:
```
[ 2677.419379] [20633]     0 20633   656924    42189   655360        0           100 java
```

12 个 java 进程同时跑, 每个 ~250MB, 总计 **3GB**, 沙箱只有 **2GB** → OOM killed.

### 生产环境推荐配置

| 资源 | 推荐 |
|------|------|
| **总内存** | ≥ 8GB (12 微服务 + DB + nginx + OS) |
| **每个 JVM** | `-Xms256m -Xmx512m` (生产) |
| **JVM 调优** | G1GC + 容器感知 |

### systemd service 模板

```ini
[Service]
Environment="JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ExecStart=/usr/bin/java $JAVA_OPTS -jar ${INSTALL_DIR}/apps/minimax-${module}.jar
```

### 限流启动

如果生产环境也只有 4GB, 分批启动:

```bash
# 启动第一批 (核心)
sudo systemctl start minimax-auth minimax-chat minimax-model

# 启动第二批 (功能)
sudo systemctl start minimax-memory minimax-rag minimax-function minimax-prompt

# 启动第三批 (管理)
sudo systemctl start minimax-admin minimax-multimodal minimax-monitor minimax-agent minimax-ws
```

---

## 一键修复脚本

适用: 在干净环境拉新代码后, 自动修复所有已知问题.

**位置**: `scripts/fix-build-issues.sh`

```bash
#!/bin/bash
# scripts/fix-build-issues.sh
# 修复 V5.3 之前的已知打包启动问题

set -e
cd /workspace/minimax-platform/backend

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