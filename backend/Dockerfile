# MiniMax Platform - 后端通用 Dockerfile (V5.28)
#
# 用法:
#   docker build -t minimax-{module}:latest --build-arg MODULE={module} backend/
#
# 或:
#   docker build -f backend/Dockerfile --build-arg MODULE=auth -t minimax-auth backend/
#
# 此 Dockerfile 利用多阶段构建, 复用公共层 (Maven 依赖下载), 各模块只编译自己

# =============================================================
# 阶段 1: 编译环境 (eclipse-temurin + maven)
# =============================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

# 先复制 pom, 利用 Docker 缓存 (pom 不变就不重新下载依赖)
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

# 编译 (跳过测试, 加速)
ARG MODULE=auth
RUN mvn -B -pl minimax-common,minimax-${MODULE} -am clean package -DskipTests \
    -Dspotless.check.skip=true -Djacoco.skip=true

# =============================================================
# 阶段 2: 运行时镜像 (仅 JRE, 体积小)
# =============================================================
FROM eclipse-temurin:17-jre-alpine

# 必要工具
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

# 复制编译产物 (Spring Boot 生成的 fat jar)
ARG MODULE=auth
COPY --from=builder /workspace/minimax-${MODULE}/target/minimax-${MODULE}-spring-boot.jar app.jar

# 容器内端口 (用环境变量覆盖)
ENV SERVER_PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# Spring 配置通过环境变量注入 (在 docker-compose.yml 中)
# 保持 dev profile (项目只有 application.yml + application-dev.yml)
ENV SPRING_PROFILES_ACTIVE=dev
ENV NACOS_HOST=nacos
ENV NACOS_PORT=8848
ENV MYSQL_HOST=mysql
ENV MYSQL_PORT=3306
ENV MYSQL_DATABASE=minimax_platform
ENV MYSQL_USER=minimax
ENV MYSQL_PASSWORD=minimax_pass_2024
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
ENV REDIS_PASSWORD=minimax_redis_2024
ENV MINIMAX_JWT_SECRET="0f6beadebfcee3e97845856757a3babf97b2af8c80f0b95690783ccc7a595352"

EXPOSE ${SERVER_PORT}

# 健康检查
HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=30s \
  CMD curl -sf http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]