# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies before copying source (speeds up rebuilds)
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Non-root user for security
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

# Application Insights Java agent for telemetry (optional — remove if not using App Insights)
# ADD https://github.com/microsoft/ApplicationInsights-Java/releases/latest/download/applicationinsights-agent.jar /app/ai-agent.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  # "-javaagent:/app/ai-agent.jar", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
