FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/client-1.0-SNAPSHOT.jar client.jar

ENTRYPOINT ["java", "-jar", "client.jar"]
