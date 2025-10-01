FROM openjdk:17-jdk-slim

WORKDIR /app

COPY build/libs/*SNAPSHOT.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]

EXPOSE 8080
