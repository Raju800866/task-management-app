# ---- Build stage: compile the jar with Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Download dependencies first so they're cached when only code changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Run stage: small JRE image, non-root user ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 appuser
COPY --from=build /app/target/task-service.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
