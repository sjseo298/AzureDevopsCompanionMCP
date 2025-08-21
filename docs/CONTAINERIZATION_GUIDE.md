# MCP Azure DevOps Server - Guía de Contenerización

## Resumen

Este MCP Server ahora soporta múltiples modos de transporte para diferentes escenarios de despliegue:

1. **STDIO Mode** (Original): Para clientes MCP locales
2. **HTTP Mode**: Para acceso remoto vía HTTP API
3. **WebSocket Mode**: Para aplicaciones web en tiempo real (futuro)

## Opciones de Despliegue

### 1. Modo STDIO (Local)

Para uso con clientes MCP que ejecutan el servidor como subproceso:

```bash
# Construir imagen
docker build -t mcp-azure-devops .

# Ejecutar en modo STDIO
docker run -i \
    -e AZURE_DEVOPS_ORGANIZATION=myorg \
    -e AZURE_DEVOPS_PAT=your-pat \
    mcp-azure-devops stdio
```

### 2. Modo HTTP (Remoto)

Para acceso remoto vía HTTP API:

```bash
# Ejecutar en modo HTTP
docker run -d -p 8080:8080 \
    --name mcp-azure-devops \
    -e AZURE_DEVOPS_ORGANIZATION=myorg \
    -e AZURE_DEVOPS_PAT=your-pat \
    mcp-azure-devops http

# Probar el servidor
curl http://localhost:8080/mcp/health
```

### 3. Docker Compose (Producción)

```bash
# Configurar variables de entorno
export AZURE_DEVOPS_ORGANIZATION=myorg
export AZURE_DEVOPS_PAT=your-pat

# Iniciar servicios
docker-compose up -d mcp-azure-devops-http

# Con proxy nginx
docker-compose --profile proxy up -d
```

## APIs Disponibles

### Health Check
```bash
GET /mcp/health
```

### MCP Protocol Endpoint
```bash
POST /mcp
Content-Type: application/json
MCP-Protocol-Version: 2025-06-18

{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-06-18",
    "capabilities": {},
    "clientInfo": {"name": "MyClient", "version": "1.0"}
  }
}
```

## Clientes Externos

### JavaScript/Node.js
```javascript
const fetch = require('node-fetch');

async function callMcpTool(toolName, args) {
  const response = await fetch('http://localhost:8080/mcp', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'MCP-Protocol-Version': '2025-06-18'
    },
    body: JSON.stringify({
      jsonrpc: '2.0',
      id: Date.now(),
      method: 'tools/call',
      params: {
        name: toolName,
        arguments: args
      }
    })
  });
  
  return response.json();
}

// Ejemplo: obtener proyectos
callMcpTool('azuredevops_core_get_projects', {})
  .then(result => console.log(result));
```

### Python
```python
import requests
import json

def call_mcp_tool(tool_name, args=None):
    url = "http://localhost:8080/mcp"
    headers = {
        'Content-Type': 'application/json',
        'MCP-Protocol-Version': '2025-06-18'
    }
    
    payload = {
        "jsonrpc": "2.0",
        "id": 1,
        "method": "tools/call",
        "params": {
            "name": tool_name,
            "arguments": args or {}
        }
    }
    
    response = requests.post(url, headers=headers, json=payload)
    return response.json()

# Ejemplo: obtener proyectos
result = call_mcp_tool('azuredevops_core_get_projects')
print(json.dumps(result, indent=2))
```

### cURL
```bash
# Inicialización
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "MCP-Protocol-Version: 2025-06-18" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-06-18",
      "capabilities": {},
      "clientInfo": {"name": "cURL", "version": "1.0"}
    }
  }'

# Listar herramientas
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "MCP-Protocol-Version: 2025-06-18" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list",
    "params": {}
  }'

# Llamar herramienta
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "MCP-Protocol-Version: 2025-06-18" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "azuredevops_core_get_projects",
      "arguments": {}
    }
  }'
```

## Seguridad

### Para Desarrollo Local
- El servidor HTTP bindea solo a localhost por defecto
- CORS habilitado para desarrollo

### Para Producción
- Use HTTPS con certificados válidos
- Configure CORS origins específicos
- Implemente autenticación adicional si es necesario
- Use variables de entorno seguras para PAT

## Troubleshooting

### Verificar logs
```bash
docker logs mcp-azure-devops
```

### Probar conectividad
```bash
# Test script incluido
./scripts/test-container.sh
```

### Problemas comunes
1. **Error 401**: Verificar PAT y organización
2. **Error 404**: Verificar que el endpoint esté correcto
3. **Timeout**: Verificar conectividad a Azure DevOps

## Ventajas de cada Modo

### STDIO Mode
- ✅ Compatible con clientes MCP existentes
- ✅ Sin configuración de red
- ✅ Seguro (no expone puertos)
- ❌ Solo para uso local

### HTTP Mode
- ✅ Acceso remoto
- ✅ Compatible con cualquier cliente HTTP
- ✅ Escalable
- ✅ Puede usar proxy/load balancer
- ❌ Requiere configuración de seguridad adicional

### WebSocket Mode (Futuro)
- ✅ Comunicación bidireccional
- ✅ Notificaciones en tiempo real
- ✅ Mejor para aplicaciones web interactivas
- ❌ Más complejo de implementar
