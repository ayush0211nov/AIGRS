# ============================================
# AIGRS Backend - Multi-stage Docker Build
# ============================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S aigrs && adduser -S aigrs -G aigrs

# Copy the built JAR
COPY --from=builder /app/target/*.jar app.jar

# Health check aligned with dynamic platform port
HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=120s \
    CMD sh -c "wget --no-verbose --tries=1 --spider http://127.0.0.1:${PORT:-8080}/actuator/health/liveness || exit 1"

# Switch to non-root user
USER aigrs

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
