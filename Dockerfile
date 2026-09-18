# Single-stage: copy the pre-built JAR from target/
# Run  mvn clean package -DskipTests  before  docker compose up
FROM docker.io/eclipse-temurin:17-jre-alpine

# Run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --chown=appuser:appgroup target/enterprise-rag-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
