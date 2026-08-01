# Multi-stage build for Spring Boot app

# Stage 1: Build
FROM maven:3.9-openjdk-11-slim AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:11-jre-slim
LABEL maintainer="Mavis@bank.com"
WORKDIR /app
COPY --from=builder /app/target/dual-record-backend.jar /app/dual-record-backend.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT exec java $JAVA_OPTS -jar /app/dual-record-backend.jar
