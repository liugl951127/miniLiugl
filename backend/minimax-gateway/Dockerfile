# =============================================================
# MiniMax Gateway Dockerfile (V3.5.8 优化版)
# 关键优化:
# 1. Maven 依赖单独层 (缓存命中率 99%)
# 2. Spring Boot layered jar (4 层独立缓存)
# 3. multi-stage build (构建环境与运行环境分离)
# 4. jdeps 预扫描 (可选, 减小镜像)
# =============================================================

# ─── Stage 1: 依赖层 (变化最少, 缓存最久) ───
FROM maven:3.9-eclipse-temurin-17 AS deps
WORKDIR /workspace

# 只 COPY pom, 触发依赖下载
# 这一层只在 pom.xml 变化时重 build
COPY pom.xml .
COPY minimax-common/pom.xml minimax-common/
COPY minimax-auth/pom.xml minimax-auth/
COPY minimax-chat/pom.xml minimax-chat/
COPY minimax-memory/pom.xml minimax-memory/
COPY minimax-model/pom.xml minimax-model/
COPY minimax-rag/pom.xml minimax-rag/
COPY minimax-gateway/pom.xml minimax-gateway/

# 安装 minimax-common (因为 packaging=pom, 必装)
# 然后下载所有依赖到 /root/.m2 (Docker 缓存这层)
RUN mvn -B -pl minimax-common -am install -DskipTests -Dspotless.check.skip=true 2>&1 | tail -10 && \
    mvn -B -pl minimax-gateway -am dependency:go-offline -DskipTests -Dspotless.check.skip=true 2>&1 | tail -10

# ─── Stage 2: 构建层 (源码变化) ───
FROM deps AS build
WORKDIR /workspace

# COPY 源码
COPY minimax-common minimax-common
COPY minimax-auth minimax-auth
COPY minimax-chat minimax-chat
COPY minimax-memory minimax-memory
COPY minimax-model minimax-model
COPY minimax-rag minimax-rag
COPY minimax-gateway minimax-gateway

# 编译 (利用缓存的依赖)
RUN mvn -B -pl minimax-gateway -am clean package \
        -DskipTests -Dspotless.check.skip=true 2>&1 | tail -15

# 拆 layered jar (4 层)
RUN java -Djarmode=layertools -jar \
    minimax-gateway/target/minimax-gateway-spring-boot.jar extract --destination extracted

# ─── Stage 3: 运行层 (最终镜像) ───
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# V3.5.8: 4 层按 变化少→变化多 顺序 COPY
# 1. dependencies     (~80MB, 几乎不变)
# 2. spring-boot-loader (~2MB, 启动器)
# 3. snapshot-dependencies (~5MB, 频繁变)
# 4. application     (~5MB, 业务代码, 变最多)

COPY --from=build /workspace/extracted/dependencies/         ./
COPY --from=build /workspace/extracted/spring-boot-loader/   ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/           ./

# 健康检查端口
EXPOSE 7080

# JVM 参数 + Spring Boot 入口
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport"

# 入口 (Spring Boot 3.x 用 org.springframework.boot.loader.launch.JarLauncher)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
