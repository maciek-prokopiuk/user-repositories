# Stage 1: Build the application
FROM gradle:7.6.0-jdk17 AS build

WORKDIR /app
COPY gradlew gradlew
COPY gradle gradle

COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew

RUN ./gradlew build --no-daemon

FROM amazoncorretto:17

WORKDIR /app

COPY --from=build /app/build/libs/github_repos-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
