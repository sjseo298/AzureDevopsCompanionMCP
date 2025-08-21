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
    echo "  stdio          - STDIO mode (default, para clientes MCP locales)"
    echo "  http           - HTTP wrapper mode (expone endpoint HTTP)"
    echo "  websocket      - WebSocket mode (futuro)"
    echo ""
    echo "Environment Variables:"
    echo "  AZURE_DEVOPS_ORGANIZATION - Nombre de la organización (requerido)"
    echo "  AZURE_DEVOPS_PAT         - Personal Access Token (requerido)"
    echo "  AZURE_DEVOPS_API_VERSION - Versión de API (default: 7.2-preview.1)"
    echo "  HTTP_PORT                - Puerto HTTP (default: 8080)"
    echo ""
    echo "Examples:"
    echo "  # STDIO mode (para uso con clients MCP locales)"
    echo "  docker run -e AZURE_DEVOPS_ORGANIZATION=myorg -e AZURE_DEVOPS_PAT=xxx mcp-azure-devops stdio"
    echo ""
    echo "  # HTTP wrapper mode (para acceso remoto)"
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

# Modo por defecto
MODE="${1:-stdio}"

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
        echo "Starting MCP Server in HTTP wrapper mode on port $HTTP_PORT..."
        
        # Crear un script simple que ejecute el servidor
        cat > /tmp/mcp-server.sh << 'EOF'
#!/bin/bash
exec java $JAVA_OPTS -jar /app/app.jar --mcp.stdio=true
EOF
        chmod +x /tmp/mcp-server.sh
        
        # Usar socat de manera más simple - cada conexión inicia su propio proceso
        # Esto es correcto para MCP ya que cada sesión es independiente
        exec socat TCP4-LISTEN:$HTTP_PORT,reuseaddr,fork EXEC:"/tmp/mcp-server.sh"
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
