# Multi-stage Dockerfile para MCP Azure DevOps Server
FROM gradle:9.4.1-jdk21 AS builder

# Configurar directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración de Gradle
COPY build.gradle gradle.properties ./
COPY gradle/ gradle/
COPY gradlew gradlew.bat ./

# Copiar el código fuente
COPY src/ src/

# Construir la aplicación
RUN gradle clean build -x test --no-daemon

# Imagen de runtime
FROM eclipse-temurin:21-jre-noble

# Instalar herramientas necesarias
RUN apt-get update && apt-get install -y \
    socat \
    netcat-openbsd \
    curl \
    bash \
    jq \
    git \
    net-tools && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Crear usuario no-root
RUN groupadd -g 1001 mcpuser && \
    useradd -d /home/mcpuser -m -u 1001 -g mcpuser mcpuser

# Crear directorios de trabajo
RUN mkdir -p /app /app/logs && \
    chown -R mcpuser:mcpuser /app

USER mcpuser
WORKDIR /app

# Copiar JAR desde la etapa de construcción
COPY --from=builder --chown=mcpuser:mcpuser /app/build/libs/*.jar app.jar

# Variables de entorno por defecto
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xms256m -Xmx512m"
ENV MCP_MODE="stdio"
ENV AZURE_DEVOPS_API_VERSION="7.2-preview.1"
ENV AZURE_DEVOPS_VSSPS_API_VERSION="7.1"

# Puertos para diferentes modos
EXPOSE 8080 8081 8082

# Scripts de entrada
COPY --chown=mcpuser:mcpuser docker/ docker/

# Hacer ejecutables los scripts
USER root
RUN chmod +x docker/*.sh
USER mcpuser

# Health check - usando un simple test de puerto TCP
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD netstat -an | grep ":8080.*LISTEN" > /dev/null || exit 1

# Script de entrada por defecto
ENTRYPOINT ["docker/entrypoint.sh"]
CMD ["stdio-http"]
