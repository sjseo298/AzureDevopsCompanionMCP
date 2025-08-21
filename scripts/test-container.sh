#!/bin/bash
# Script de ejemplo para probar el MCP Server contenerizado

set -e

CONTAINER_NAME="mcp-azure-devops-test"
IMAGE_NAME="mcp-azure-devops"
PORT="8080"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== MCP Azure DevOps Server - Container Test ===${NC}"

# Verificar variables de entorno
if [ -z "$AZURE_DEVOPS_ORGANIZATION" ]; then
    echo -e "${RED}ERROR: AZURE_DEVOPS_ORGANIZATION environment variable is required${NC}"
    echo "Example: export AZURE_DEVOPS_ORGANIZATION=mycompany"
    exit 1
fi

if [ -z "$AZURE_DEVOPS_PAT" ]; then
    echo -e "${RED}ERROR: AZURE_DEVOPS_PAT environment variable is required${NC}"
    echo "Example: export AZURE_DEVOPS_PAT=your-personal-access-token"
    exit 1
fi

# Función para limpiar recursos
cleanup() {
    echo -e "${YELLOW}Cleaning up...${NC}"
    docker stop $CONTAINER_NAME 2>/dev/null || true
    docker rm $CONTAINER_NAME 2>/dev/null || true
}

# Configurar trap para cleanup
trap cleanup EXIT

echo -e "${YELLOW}Building Docker image...${NC}"
docker build -t $IMAGE_NAME .

echo -e "${YELLOW}Starting container in HTTP mode...${NC}"
docker run -d \
    --name $CONTAINER_NAME \
    -p $PORT:$PORT \
    -e AZURE_DEVOPS_ORGANIZATION="$AZURE_DEVOPS_ORGANIZATION" \
    -e AZURE_DEVOPS_PAT="$AZURE_DEVOPS_PAT" \
    $IMAGE_NAME http

# Esperar que el servidor esté listo
echo -e "${YELLOW}Waiting for server to be ready...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:$PORT/mcp/health > /dev/null 2>&1; then
        echo -e "${GREEN}Server is ready!${NC}"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

# Probar health check
echo -e "${YELLOW}Testing health endpoint...${NC}"
curl -s http://localhost:$PORT/mcp/health | jq .

# Probar inicialización MCP
echo -e "${YELLOW}Testing MCP initialization...${NC}"
curl -s -X POST http://localhost:$PORT/mcp \
    -H "Content-Type: application/json" \
    -H "MCP-Protocol-Version: 2025-06-18" \
    -H "Accept: application/json" \
    -d '{
        "jsonrpc": "2.0",
        "id": 1,
        "method": "initialize",
        "params": {
            "protocolVersion": "2025-06-18",
            "capabilities": {},
            "clientInfo": {
                "name": "Docker Test Client",
                "version": "1.0.0"
            }
        }
    }' | jq .

# Probar listado de herramientas
echo -e "${YELLOW}Testing tools list...${NC}"
curl -s -X POST http://localhost:$PORT/mcp \
    -H "Content-Type: application/json" \
    -H "MCP-Protocol-Version: 2025-06-18" \
    -H "Accept: application/json" \
    -d '{
        "jsonrpc": "2.0",
        "id": 2,
        "method": "tools/list",
        "params": {}
    }' | jq .

echo -e "${GREEN}Tests completed! Container is running on http://localhost:$PORT${NC}"
echo -e "${YELLOW}To stop the container: docker stop $CONTAINER_NAME${NC}"
echo -e "${YELLOW}To view logs: docker logs $CONTAINER_NAME${NC}"

# Opcional: mantener el script corriendo para mostrar logs
read -p "Press Enter to view logs (Ctrl+C to exit)..."
docker logs -f $CONTAINER_NAME
