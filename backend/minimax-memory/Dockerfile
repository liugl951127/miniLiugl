# =============================================================
# memory Dockerfile (V3.5.8 优化版)
# 3-stage build: deps (缓存) → build (拆层) → runtime (4 层)
# =============================================================

# ─── Stage 1: 依赖层 ───
FROM maven:3.9-eclipse-temurin-17 AS deps
WORKDIR /workspace

COPY pom.xml .
COPY minimax-common/pom.xml minimax-common/
COPY minimax-auth/pom.xml minimax-auth/
COPY minimax-memory/pom.xml minimax-memory/

# 安装 common + 下载所有依赖 (这一层缓存最久)
RUN mvn -B -pl minimax-common -am install -DskipTests -Dspotless.check.skip=true 2>&1 | tail -10 && \
    mvn -B -pl minimax-memory -am dependency:go-offline -DskipTests -Dspotless.check.skip=true 2>&1 | tail -10

# ─── Stage 2: 构建层 ───
FROM deps AS build
WORKDIR /workspace

COPY minimax-common minimax-common
COPY minimax-auth minimax-auth
COPY minimax-memory minimax-memory

RUN mvn -B -pl minimax-memory -am clean package \
        -DskipTests -Dspotless.check.skip=true 2>&1 | tail -15 && \
    ls -la /workspace/*/target/ 2>/dev/null | head -20  # V3.5.14: 验证 jar 产物

# 拆 Spring Boot layered jar (4 层)
RUN java -Djarmode=layertools -jar \
    minimax-memory/target/minimax-memory-spring-boot.jar extract --destination extracted

# ─── Stage 3: 运行层 ───
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 4 层按 变化少→变化多 顺序 COPY
COPY --from=build /workspace/extracted/dependencies/         ./
COPY --from=build /workspace/extracted/spring-boot-loader/   ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/           ./

# 无对外端口

# JVM 参数
ENV JAVA_OPTS="-Xms256m -Xmx384m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=70"

# Spring Boot 入口
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
