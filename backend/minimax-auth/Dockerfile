# =============================================================
# Auth Dockerfile (V3.5.8 优化版)
# 优化: 3-stage build + 依赖层缓存 + Spring Boot layered jar
# =============================================================

FROM maven:3.9-eclipse-temurin-17 AS deps
WORKDIR /workspace

COPY pom.xml .
COPY minimax-common/pom.xml minimax-common/
COPY minimax-auth/pom.xml minimax-auth/

RUN mvn -B -pl minimax-common -am install -DskipTests -Dspotless.check.skip=true 2>&1 | tail -10 && \
    mvn -B -pl minimax-auth -am dependency:go-offline -DskipTests -Dspotless.check.skip=true 2>&1 | tail -10

FROM deps AS build
WORKDIR /workspace

COPY minimax-common minimax-common
COPY minimax-auth minimax-auth

RUN mvn -B -pl minimax-auth -am clean package \
        -DskipTests -Dspotless.check.skip=true 2>&1 | tail -15 && \
    ls -la /workspace/*/target/ 2>/dev/null | head -20  # V3.5.14: 验证 jar 产物

RUN java -Djarmode=layertools -jar \
    minimax-auth/target/minimax-auth-spring-boot.jar extract --destination extracted

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /workspace/extracted/dependencies/         ./
COPY --from=build /workspace/extracted/spring-boot-loader/   ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/           ./

EXPOSE 8081

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
