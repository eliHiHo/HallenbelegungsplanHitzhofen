# Stage 1: build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Separate dependency download for layer caching
COPY pom.xml .
RUN mvn -B dependency:resolve dependency:resolve-plugins -q

COPY src ./src
RUN mvn -B package -DskipTests -q

# Stage 2: runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/quarkus-app/ .
EXPOSE 8080
CMD ["java", "-jar", "quarkus-run.jar"]
