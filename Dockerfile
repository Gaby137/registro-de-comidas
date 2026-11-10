# syntax=docker/dockerfile:1

# ===== Build Stage =====
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# 1) Wrapper + pom primero: las dependencias quedan cacheadas mientras no cambie el pom
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# 2) Código fuente
COPY src ./src
RUN ./mvnw -B -q clean package -DskipTests

# ===== Runtime Stage =====
FROM eclipse-temurin:25-jre
WORKDIR /app

# Usuario no-root por seguridad
RUN groupadd -r appuser && useradd -r -g appuser appuser \
    && mkdir -p /app/uploads && chown -R appuser:appuser /app

COPY --from=builder --chown=appuser:appuser /app/target/*.jar app.jar
USER appuser

EXPOSE 8080

# Usa hasta el 75% de la memoria asignada al contenedor
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
