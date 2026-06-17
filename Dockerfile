FROM eclipse-temurin:21-jre-alpine
LABEL authors="eukon05"

COPY target/*.jar /app/app.jar
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]