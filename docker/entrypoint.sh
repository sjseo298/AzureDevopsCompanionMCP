#!/bin/bash
set -e

# Configurar variables de entorno por defecto
export AZURE_DEVOPS_API_VERSION="${AZURE_DEVOPS_API_VERSION:-7.2-preview.1}"
export AZURE_DEVOPS_VSSPS_API_VERSION="${AZURE_DEVOPS_VSSPS_API_VERSION:-7.1}"

# Función para mostrar ayuda
show_help() {
    echo "MCP Azure DevOps Server - Container Mode"
    echo ""
    echo "Usage: docker run [options] mcp-azure-devops [mode]"
    echo ""
    echo "Modes:"
    echo "  stdio-http     - STDIO + HTTP real (default recomendado; permite MCP + uploads multipart)"
    echo "  stdio          - STDIO puro (sin endpoint HTTP de uploads)"
    echo "  http           - HTTP real (expone /mcp y /mcp/uploads)"
    echo "  websocket      - WebSocket mode (futuro)"
    echo ""
    echo "Environment Variables:"
    echo "  AZURE_DEVOPS_ORGANIZATION - Nombre de la organización (requerido)"
    echo "  AZURE_DEVOPS_PAT         - Personal Access Token (requerido)"
    echo "  AZURE_DEVOPS_API_VERSION - Versión de API (default: 7.2-preview.1)"
    echo "  HTTP_PORT                - Puerto HTTP (default: 8080)"
    echo ""
    echo "Examples:"
    echo "  # STDIO + HTTP para clientes MCP locales y uploads grandes"
    echo "  docker run -i -p 9090:8080 -e AZURE_DEVOPS_ORGANIZATION=myorg -e AZURE_DEVOPS_PAT=xxx mcp-azure-devops stdio-http"
    echo ""
    echo "  # HTTP real (para acceso remoto sin STDIO)"
    echo "  docker run -p 8080:8080 -e AZURE_DEVOPS_ORGANIZATION=myorg -e AZURE_DEVOPS_PAT=xxx mcp-azure-devops http"
}

# Validar variables requeridas
validate_env() {
    if [ -z "$AZURE_DEVOPS_ORGANIZATION" ]; then
        echo "ERROR: AZURE_DEVOPS_ORGANIZATION environment variable is required"
        exit 1
    fi
    
    if [ -z "$AZURE_DEVOPS_PAT" ]; then
        echo "ERROR: AZURE_DEVOPS_PAT environment variable is required"
        exit 1
    fi
}

# Modo por defecto recomendado para contenedor: conserva STDIO y habilita HTTP uploads.
MODE="${1:-stdio-http}"

case "$MODE" in
    "help"|"-h"|"--help")
        show_help
        exit 0
        ;;
    "stdio")
        validate_env
        echo "Starting MCP Server in STDIO mode..."
        exec java $JAVA_OPTS -jar app.jar --mcp.stdio=true
        ;;
    "http")
        validate_env
        export HTTP_PORT="${HTTP_PORT:-8080}"
        echo "Starting MCP Server in real HTTP mode on port $HTTP_PORT..."
        exec java $JAVA_OPTS -Dspring.main.web-application-type=servlet -Dserver.port=$HTTP_PORT -Dmcp.http=true -jar app.jar --mcp.http=true
        ;;
    "stdio-http")
        validate_env
        export HTTP_PORT="${HTTP_PORT:-8080}"
        echo "Starting MCP Server in STDIO + HTTP mode on port $HTTP_PORT..."
        exec java $JAVA_OPTS -Dspring.main.web-application-type=servlet -Dserver.port=$HTTP_PORT -Dmcp.http=true -Dmcp.stdio=true -jar app.jar --mcp.stdio=true --mcp.http=true
        ;;
    "websocket")
        validate_env
        export WS_PORT="${WS_PORT:-8081}"
        echo "Starting MCP Server in WebSocket mode on port $WS_PORT..."
        exec java $JAVA_OPTS -jar app.jar --server.port=$WS_PORT --mcp.websocket=true
        ;;
    *)
        echo "ERROR: Unknown mode '$MODE'"
        show_help
        exit 1
        ;;
esac
