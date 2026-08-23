FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/client-1.0-SNAPSHOT.jar client.jar

EXPOSE 8021 5005

ENTRYPOINT ["java", "-jar", "client.jar"]
