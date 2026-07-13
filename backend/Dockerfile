# MiniMax Platform - 后端通用 Dockerfile (V3.5.5+ 磁盘优化版)
#
# 关键优化:
#   1. Spring Boot layered jar (dependencies / snapshot / application)
#   2. 外部依赖 + SQL + 配置 全部 bind mount 到容器, 不占 image layer
#   3. /tmp + /app/logs 挂到 tmpfs, jar 解压到磁盘
#   4. JVM: -XX:+UseContainerSupport + G1GC + CDS archive
#
# 用法:
#   docker build -t minimax-{module}:latest --build-arg MODULE={module} backend/

# =============================================================
# 阶段 1: 编译 (Maven)
# =============================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

# 缓存层: pom + 公共模块
COPY pom.xml .
COPY minimax-common minimax-common
COPY minimax-auth minimax-auth
COPY minimax-chat minimax-chat
COPY minimax-memory minimax-memory
COPY minimax-model minimax-model
COPY minimax-rag minimax-rag
COPY minimax-function minimax-function
COPY minimax-admin minimax-admin
COPY minimax-multimodal minimax-multimodal
COPY minimax-monitor minimax-monitor
COPY minimax-agent minimax-agent
COPY minimax-prompt minimax-prompt
COPY minimax-analytics minimax-analytics
COPY minimax-pipeline minimax-pipeline
COPY minimax-ws minimax-ws
COPY minimax-gateway minimax-gateway

ARG MODULE=auth
RUN mvn -B -pl minimax-common,minimax-${MODULE} -am clean package -DskipTests \
    -Dspotless.check.skip=true -Djacoco.skip=true

# 用 spring-boot-maven-plugin 拆分为 layers
# 输出 target/layers/ 目录:
#   dependencies/   spring-boot 依赖 (基本不变)
#   spring-boot-loader/  启动器
#   snapshot-dependencies/  快照依赖
#   application/   业务代码
WORKDIR /workspace/minimax-${MODULE}
RUN java -Djarmode=layertools -jar target/minimax-${MODULE}-spring-boot.jar extract

# =============================================================
# 阶段 2: 运行时 (仅 JRE, 体积小)
# =============================================================
FROM eclipse-temurin:17-jre-alpine

# 必要工具
RUN apk add --no-cache curl tzdata bash && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建非 root 用户 + 持久目录
RUN addgroup -S minimax && adduser -S minimax -G minimax && \
    mkdir -p /app /app/logs /app/config /app/sql && \
    chown -R minimax:minimax /app

WORKDIR /app
USER minimax

# 复制分层 (按变化频率排序: 依赖最稳定, 应用代码最常变)
ARG MODULE=auth
COPY --from=builder --chown=minimax:minimax /workspace/minimax-${MODULE}/target/minimax-${MODULE}-spring-boot.jar app.jar
COPY --from=builder --chown=minimax:minimax /workspace/minimax-${MODULE}/target/dependency/dependencies/ ./
COPY --from=builder --chown=minimax:minimax /workspace/minimax-${MODULE}/target/dependency/spring-boot-loader/ ./
COPY --from=builder --chown=minimax:minimax /workspace/minimax-${MODULE}/target/dependency/snapshot-dependencies/ ./
COPY --from=builder --chown=minimax:minimax /workspace/minimax-${MODULE}/target/dependency/application/ ./

# Spring 配置 + SQL 全部 bind mount 到容器磁盘
# docker-compose 中:
#   /workspace/miniLiugl/sql    -> /app/sql   (只读)
#   /workspace/miniLiugl/config -> /app/config (可写, application*.yml)
#   /workspace/miniLiugl/logs/${MODULE} -> /app/logs

ENV SERVER_PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC \
    -XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.io.tmpdir=/tmp \
    -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"

ENV SPRING_PROFILES_ACTIVE=mysql
ENV MYSQL_HOST=mariadb
ENV MYSQL_PORT=3306
ENV MYSQL_DATABASE=minimax_platform
ENV MYSQL_USER=root
ENV MYSQL_PASSWORD=root123456
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
ENV REDIS_PASSWORD=minimax_redis_2024

EXPOSE ${SERVER_PORT}

HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=30s \
  CMD curl -sf http://localhost:${SERVER_PORT}/actuator/health || exit 1

# 用 exec 形式启动 (PID 1 信号处理正确)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]
