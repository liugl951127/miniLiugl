# =============================================================
# AI Dockerfile (V3.5.8 优化版)
# 优化: 3-stage build + 依赖层缓存 + Spring Boot layered jar
# =============================================================

# ─── Stage 1: 依赖层 ───
FROM maven:3.9-eclipse-temurin-17 AS deps
WORKDIR /workspace

COPY pom.xml .
COPY minimax-common/pom.xml minimax-common/
COPY minimax-ai/pom.xml minimax-ai/

# 安装 common (packaging=pom) + 下载所有依赖
RUN mvn -B -pl minimax-common -am install -DskipTests -Dspotless.check.skip=true 2>&1 | tail -10 && \
    mvn -B -pl minimax-ai -am dependency:go-offline -DskipTests -Dspotless.check.skip=true 2>&1 | tail -10

# ─── Stage 2: 构建层 ───
FROM deps AS build
WORKDIR /workspace

COPY minimax-common minimax-common
COPY minimax-ai minimax-ai

RUN mvn -B -pl minimax-ai -am clean package \
        -DskipTests -Dspotless.check.skip=true 2>&1 | tail -15 && \
    ls -la /workspace/*/target/ 2>/dev/null | head -20  # V3.5.14: 验证 jar 产物

# 拆 layered jar
RUN java -Djarmode=layertools -jar \
    minimax-ai/target/minimax-ai-spring-boot.jar extract --destination extracted

# ─── Stage 3: 运行层 ───
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 4 层按 变化少→多 顺序 COPY (缓存命中率高)
COPY --from=build /workspace/extracted/dependencies/         ./
COPY --from=build /workspace/extracted/spring-boot-loader/   ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/           ./

EXPOSE 8094

ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
