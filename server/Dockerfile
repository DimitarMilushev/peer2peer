FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/server-1.0-SNAPSHOT.jar server.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "server.jar"]
