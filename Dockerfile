# ===========================================
# Multi-Stage Dockerfile for Spring Boot App
# Optimized for production with security best practices
# ===========================================

# ---------- DEPENDENCIES STAGE ----------
FROM eclipse-temurin:21-jdk-alpine AS dependencies
WORKDIR /workspace

# Install necessary tools for build
RUN apk add --no-cache bash

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle* . || true
RUN chmod +x gradlew

# Download dependencies (cached layer)
RUN ./gradlew --no-daemon --parallel dependencies

# ---------- BUILD STAGE ----------
FROM dependencies AS build
WORKDIR /workspace

# Copy source code
COPY src src

# Build application with optimizations
RUN ./gradlew --no-daemon --parallel clean bootJar \
    -x test \
    -Dorg.gradle.caching=true \
    -Dorg.gradle.parallel=true

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install security updates and required packages
RUN apk add --no-cache \
    curl \
    tzdata \
    && rm -rf /var/cache/apk/*

# Create non-root users with specific UID for security
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring

# Set working directory
WORKDIR /app

# Copy application jar from build stage
COPY --from=build /workspace/build/libs/app.jar app.jar

# Change ownership to non-root users
RUN chown -R spring:spring /app
USER spring

# Expose port
EXPOSE 8080

# Health check with proper timeout and retries
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=postgres \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC"

# Application environment variables
ENV SPRING_PROFILES_ACTIVE=postgres \
    DB_URL=jdbc:postgresql://db:5432/usersdb \
    DB_USERNAME=user \
    DB_PASSWORD=password \
    TZ=UTC

# Use exec form for proper signal handling
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
