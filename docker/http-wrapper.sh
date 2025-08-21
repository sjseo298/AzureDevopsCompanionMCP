#!/bin/bash
# Wrapper HTTP para MCP STDIO Server
# Expone el servidor MCP vía HTTP para acceso remoto

set -e

JAVA_PROCESS_PID=""
PORT="${HTTP_PORT:-8080}"

cleanup() {
    echo "Cleaning up..."
    if [ ! -z "$JAVA_PROCESS_PID" ]; then
        kill $JAVA_PROCESS_PID 2>/dev/null || true
    fi
    exit 0
}

trap cleanup SIGTERM SIGINT

echo "Starting MCP-to-HTTP wrapper on port $PORT..."

# Iniciar el proceso Java MCP en modo STDIO en background
java $JAVA_OPTS -jar app.jar --mcp.stdio=true &
JAVA_PROCESS_PID=$!

# Usar socat para exponer STDIO vía TCP
echo "Exposing MCP STDIO via TCP port $PORT"
exec socat TCP4-LISTEN:$PORT,reuseaddr,fork EXEC:"java $JAVA_OPTS -jar app.jar --mcp.stdio=true"
