# FORMA backend image (FOR-82). Multi-stage: build the Spring Boot jar with the
# committed Gradle wrapper, then run it on a slim JRE as a non-root user.

# --- Build stage ---------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy the wrapper and build scripts first so dependency resolution is cached
# independently of source changes.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Copy sources and build the executable jar.
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# --- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# curl is used by the compose healthcheck against the actuator endpoint.
RUN apk add --no-cache curl \
    && addgroup -S forma && adduser -S forma -G forma

COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown -R forma:forma /app

USER forma
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
