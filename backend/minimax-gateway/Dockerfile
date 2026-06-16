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
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/minimax-gateway/target/minimax-gateway.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
