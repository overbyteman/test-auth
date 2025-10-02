# ===========================================
# Multi-Stage Dockerfile for Spring Boot App
# Optimized for production with security best practices
# Uses pre-built JAR from buildSrc directory
# ===========================================

# ---------- EXTRACT STAGE ----------
FROM eclipse-temurin:21-jre-alpine AS extract

# Copy pre-built JAR from buildSrc
COPY buildSrc/libs/app.jar app.jar

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
