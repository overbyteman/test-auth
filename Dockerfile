# ===========================================
# Multi-Stage Dockerfile for Spring Boot App
# Builds the application inside Docker for consistency
# Optimized for production with security best practices
# ===========================================

# ---------- BUILD STAGE ----------
FROM amazoncorretto:17-alpine AS builder

# Install build dependencies including bash for security script
RUN apk add --no-cache \
    git \
    bash \
    openssl \
    util-linux \
    && rm -rf /var/cache/apk/*

# Set working directory for build
WORKDIR /build

# Copy Gradle wrapper and build files first (for better caching)
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY lombok.config .

# Make gradlew executable
RUN chmod +x gradlew

# Copy and execute security setup script
COPY docker-security-setup.sh .
RUN chmod +x docker-security-setup.sh && \
    ./docker-security-setup.sh && \
    source /tmp/build-env.sh

# Copy source code
COPY src/ src/

# Create gradle.properties to disable daemon and configure JVM
RUN echo "org.gradle.daemon=false" > gradle.properties && \
    echo "org.gradle.parallel=false" >> gradle.properties && \
    echo "org.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m" >> gradle.properties && \
    echo "org.gradle.configureondemand=false" >> gradle.properties

# Build the application
RUN ./gradlew clean bootJar --no-daemon --stacktrace

# Verify the JAR was created correctly
RUN ls -la build/libs/ && \
    java -Djarmode=layertools -jar build/libs/app.jar list

# Show generated .env file for reference
RUN echo "📋 Arquivo .env gerado:" && cat .env

# ---------- EXTRACT STAGE ----------
FROM eclipse-temurin:21-jre-alpine AS extract

# Copy the built JAR from builder stage
COPY --from=builder /build/build/libs/app.jar app.jar

# Extract JAR layers for better caching
RUN java -Djarmode=layertools -jar app.jar extract

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install runtime dependencies and security updates
RUN apk add --no-cache \
    curl \
    tzdata \
    dumb-init \
    && rm -rf /var/cache/apk/* \
    && addgroup -g 1001 -S spring \
    && adduser -u 1001 -S spring -G spring

# Set working directory
WORKDIR /app

# Copy application layers from extract stage for better caching
COPY --from=extract --chown=spring:spring dependencies/ ./
COPY --from=extract --chown=spring:spring spring-boot-loader/ ./
COPY --from=extract --chown=spring:spring snapshot-dependencies/ ./
COPY --from=extract --chown=spring:spring application/ ./

# Switch to non-root user
USER spring

# Expose port
EXPOSE 8080

# Health check with proper configuration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=postgres \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC \
    -Djava.awt.headless=true"

# Application environment variables
ENV TZ=UTC \
    LANG=en_US.UTF-8 \
    LC_ALL=en_US.UTF-8

# Use dumb-init for proper signal handling and exec form
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]