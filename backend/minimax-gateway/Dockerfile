FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY minimax-common minimax-common
COPY minimax-auth minimax-auth
COPY minimax-chat minimax-chat
COPY minimax-memory minimax-memory
COPY minimax-model minimax-model
COPY minimax-rag minimax-rag
COPY minimax-gateway minimax-gateway
# V3.5.5+: minimax-common 是 packaging=pom library, mvn install 才能传递依赖
RUN mvn -B -pl minimax-gateway -am clean install -DskipTests -Dspotless.check.skip=true

FROM eclipse-temurin:17-jre
WORKDIR /app
# V3.5.5+: 修正为 ${MODULE}-spring-boot.jar (Spring Boot fat jar, 110MB, 含 Main-Class)
#   之前 minimax-gateway.jar 是普通 jar (200KB), 启动报 "no main manifest attribute"
COPY --from=build /workspace/minimax-gateway/target/minimax-gateway-spring-boot.jar app.jar
EXPOSE 7080
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
