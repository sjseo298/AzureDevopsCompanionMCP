# MCP Azure DevOps Server

Servidor MCP (Model Context Protocol) para Azure DevOps que proporciona acceso completo a la API REST de Azure DevOps a través del protocolo MCP.

## 🚀 Características

- **Protocolo MCP completo**: Soporte para JSON-RPC 2.0 sobre STDIO y HTTP
- **APIs de Azure DevOps**: Acceso completo a Core, Work Items, Work Management, y más
- **Containerizado**: Imagen Docker lista para producción
- **Múltiples modos de transporte**: STDIO, HTTP wrapper, y WebSocket (próximamente)
- **Configuración flexible**: Variables de entorno y archivos de configuración

## 🔁 Refactorización de tools (routers)

Este servidor pasó de exponer decenas de tools individuales (p.ej. `azuredevops_wit_work_item_get`, `azuredevops_core_get_projects`, etc.) a exponer **exactamente 12 tools “router”**.

Cada router tool recibe un parámetro obligatorio `operation` y enruta internamente hacia la implementación existente.

### ¿Por qué se hizo?

- **Reducir superficie de exposición**: un catálogo muy grande de tools aumenta el riesgo de exponer operaciones destructivas/administrativas por accidente.
- **Mejor UX para el cliente MCP**: menos tools, más fáciles de descubrir y documentar.
- **Estandarización**: unifica convenciones de entrada/salida (un solo punto por dominio) y deja el terreno preparado para políticas de enablement por `env var` a nivel de operación.

### Impacto

- **Breaking change**: no se mantiene compatibilidad hacia atrás. Los nombres antiguos dejan de aparecer en `tools/list`.
- El servidor sigue conteniendo los tools “leaf” (internos), pero **solo los routers** se exponen por defecto.

## 📋 Requisitos

- Java 21+
- Docker (para uso containerizado)
- Azure DevOps PAT (Personal Access Token)
- Organización de Azure DevOps

## 🏃‍♂️ Inicio Rápido

### Opción 1: Usando Docker (Recomendado)

1. **Construir la imagen**:
```bash
docker build -t mcp-azure-devops .
```

2. **Crear archivo de variables de entorno**:
```bash
# .env
AZURE_DEVOPS_ORGANIZATION=tu-organizacion
AZURE_DEVOPS_PAT=tu-personal-access-token
```

3. **Ejecutar el contenedor**:
```bash
# Modo STDIO (para clientes MCP locales)
docker run -i --env-file .env mcp-azure-devops stdio

# Modo HTTP (para acceso remoto en puerto 8080)
docker run -p 8080:8080 --env-file .env mcp-azure-devops http
```

### Opción 2: Desarrollo Local

1. **Configurar variables de entorno**:
```bash
export AZURE_DEVOPS_ORGANIZATION=tu-organizacion
export AZURE_DEVOPS_PAT=tu-personal-access-token
```

2. **Ejecutar con Gradle**:
```bash
./gradlew bootRun
```

Nota (Dev Container / filesystem montado): en algunos entornos el directorio `.gradle/` dentro del workspace puede causar errores de lock al finalizar el build. Si te ocurre, ejecuta builds usando caches fuera del workspace:

```bash
./gradlew --no-daemon -g /tmp/gradle-user-home --project-cache-dir /tmp/gradle-project-cache clean build
```

## ⚙️ Configuración MCP

### Configuración básica con mcp.json

#### Para uso con Docker (STDIO)
```json
{
  "inputs": [
    {
      "id": "azure_devops_org",
      "type": "promptString",
      "description": "Azure DevOps organization name (e.g. 'contoso')"
    },
    {
      "id": "azure_devops_pat",
      "type": "promptString",
      "description": "Azure DevOps Personal Access Token (PAT)"
    }
  ],
  "servers": {
    "azure-devops-mcp": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "--env",
        "AZURE_DEVOPS_ORGANIZATION=${input:azure_devops_org}",
        "--env",
        "AZURE_DEVOPS_PAT=${input:azure_devops_pat}",
        "mcp-azure-devops",
        "stdio"
      ]
    }
  }
}
```

#### Para desarrollo local
```json
{
  "servers": {
    "azure-devops-local": {
      "command": "java",
      "args": [
        "-jar",
        "/ruta/completa/build/libs/mcp-azure-devops.jar",
        "--mcp.stdio=true"
      ],
      "env": {
        "AZURE_DEVOPS_ORGANIZATION": "tu-organizacion",
        "AZURE_DEVOPS_PAT": "tu-pat"
      }
    }
  }
}
```

#### Para servidor HTTP remoto
```json
{
  "servers": {
    "azure-devops-remote": {
      "command": "socat",
      "args": [
        "-",
        "TCP:servidor-remoto:8080"
      ]
    }
  }
}
```

### Configuración avanzada con múltiples instancias

```json
{
  "inputs": [
    {
      "id": "dev_org",
      "type": "promptString",
      "description": "Dev organization"
    },
    {
      "id": "prod_org", 
      "type": "promptString",
      "description": "Production organization"
    },
    {
      "id": "dev_pat",
      "type": "promptString",
      "description": "Development PAT"
    },
    {
      "id": "prod_pat",
      "type": "promptString", 
      "description": "Production PAT"
    }
  ],
  "servers": {
    "azure-devops-dev": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "--env", "AZURE_DEVOPS_ORGANIZATION=${input:dev_org}",
        "--env", "AZURE_DEVOPS_PAT=${input:dev_pat}",
        "mcp-azure-devops", "stdio"
      ]
    },
    "azure-devops-prod": {
      "command": "docker", 
      "args": [
        "run", "--rm", "-i",
        "--env", "AZURE_DEVOPS_ORGANIZATION=${input:prod_org}",
        "--env", "AZURE_DEVOPS_PAT=${input:prod_pat}",
        "mcp-azure-devops", "stdio"
      ]
    }
  }
}
```

## 🛠️ Modos de Uso

### 1. Modo STDIO (Por defecto)
Ideal para clientes MCP locales como VS Code, Claude Desktop, etc.
```bash
docker run -i --env-file .env mcp-azure-devops stdio
```

### 2. Modo HTTP Wrapper
Expone el servidor MCP via TCP para acceso remoto:
```bash
docker run -p 8080:8080 --env-file .env mcp-azure-devops http
```

### 3. Modo WebSocket (Próximamente)
```bash
docker run -p 8081:8081 --env-file .env mcp-azure-devops websocket
```

## 🔧 Variables de Entorno

| Variable | Descripción | Requerido | Default |
|----------|-------------|-----------|---------|
| `AZURE_DEVOPS_ORGANIZATION` | Nombre de la organización Azure DevOps | ✅ | - |
| `AZURE_DEVOPS_PAT` | Personal Access Token | ✅ | - |
| `AZURE_DEVOPS_API_VERSION` | Versión de la API | ❌ | `7.2-preview.1` |
| `AZURE_DEVOPS_VSSPS_API_VERSION` | Versión API VSSPS | ❌ | `7.1` |
| `HTTP_PORT` | Puerto HTTP (modo http) | ❌ | `8080` |
| `WS_PORT` | Puerto WebSocket (modo websocket) | ❌ | `8081` |

## 🎯 Herramientas Disponibles

El servidor expone **12 tools router**. Cada una recibe `operation` y parámetros según la operación.

### 1) Identidad / Perfil
- `azuredevops_profile_identity`
  - `operation: get_my_memberid`

### 2) Core
- `azuredevops_core_projects`
  - `operation: list | get | get_properties | create | update | delete | set_properties`
- `azuredevops_core_teams`
  - `operation: list | get | list_all | members | categorized | create | update | delete`
- `azuredevops_core_processes`
  - `operation: list | get`
- `azuredevops_core_avatars`
  - `operation: get | set`

### 3) Work / Planning
- `azuredevops_work_planning`
  - `operation: get_boards | get_backlogs`

### 4) Work Item Tracking (WIT)
- `azuredevops_wit_work_items`
  - `operation: get | create | update | delete | batch_get | bulk_delete`
- `azuredevops_wit_comments`
  - `operation: list | add | update | delete | versions_list | versions_get | reactions_list | reactions_add | reactions_delete | reactions_engaged_users`
- `azuredevops_wit_attachments`
  - `operation: get | delete | add_to_work_item`
- `azuredevops_wit_classification_nodes`
  - `operation: get_root | get | get_by_ids | upsert | update | delete`
- `azuredevops_wit_queries`
  - `operation: wiql_query | wiql_by_id | search_queries | list_root_folders`
- `azuredevops_wit_reporting`
  - `operation: revisions_list | revisions_get | reporting_links_get | reporting_revisions_get | reporting_revisions_post`

Nota: existen tools internos (“leaf”) en el código, pero no se exponen en `tools/list` tras esta refactorización.

## 📱 Ejemplos de Uso

### Listar proyectos
```bash
echo '{"jsonrpc":"2.0","method":"tools/call","id":1,"params":{"name":"azuredevops_core_projects","arguments":{"operation":"list"}}}' | docker run -i --env-file .env mcp-azure-devops stdio
```

### Crear work item
```bash
echo '{"jsonrpc":"2.0","method":"tools/call","id":2,"params":{"name":"azuredevops_wit_work_items","arguments":{"operation":"create","project":"MiProyecto","type":"User Story","title":"Nueva funcionalidad","description":"Implementar nueva característica"}}}' | docker run -i --env-file .env mcp-azure-devops stdio
```

### Ejecutar consulta WIQL
```bash
echo '{"jsonrpc":"2.0","method":"tools/call","id":3,"params":{"name":"azuredevops_wit_queries","arguments":{"operation":"wiql_query","project":"MiProyecto","query":"SELECT [System.Id], [System.Title] FROM workitems WHERE [System.WorkItemType] = '\''User Story'\'' AND [System.State] = '\''Active'\''"}}}'  | docker run -i --env-file .env mcp-azure-devops stdio
```

### Adjuntar archivo a un Work Item
Operación atómica: si la asociación falla se elimina el attachment subido.
```bash
echo '{"jsonrpc":"2.0","method":"tools/call","id":4,"params":{"name":"azuredevops_wit_attachments","arguments":{"operation":"add_to_work_item","project":"MiProyecto","workItemId":123,"fileName":"evidencia.txt","dataBase64":"'"$(printf 'Hola mundo' | base64 -w0)"'","comment":"Evidencia de prueba"}}}' | docker run -i --env-file .env mcp-azure-devops stdio
```

## 🐳 Docker

### Health Check
El contenedor incluye un health check que verifica la disponibilidad del servicio:
```bash
docker ps  # Muestra (healthy) cuando todo está funcionando
```

### Logs limpios
Los logs están optimizados para producción sin spam del health check:
```bash
docker logs nombre-contenedor
# Output: Starting MCP Server in HTTP wrapper mode on port 8080...
```

### Multi-stage build
La imagen utiliza build multi-stage para optimización:
- **Builder**: Gradle + OpenJDK 21 (para construcción)  
- **Runtime**: OpenJDK 21 JRE + herramientas (imagen final ~470MB)

## 🚀 Despliegue

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mcp-azure-devops
spec:
  replicas: 2
  template:
    spec:
      containers:
      - name: mcp-server
        image: mcp-azure-devops:latest
        args: ["http"]
        env:
        - name: AZURE_DEVOPS_ORGANIZATION
          valueFrom:
            secretKeyRef:
              name: azure-devops-secrets
              key: organization
        - name: AZURE_DEVOPS_PAT
          valueFrom:
            secretKeyRef:
              name: azure-devops-secrets
              key: pat
        ports:
        - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: mcp-azure-devops-service
spec:
  selector:
    app: mcp-azure-devops
  ports:
  - port: 80
    targetPort: 8080
```

### Docker Compose
```yaml
version: '3.8'
services:
  mcp-azure-devops:
    build: .
    command: ["http"]
    ports:
      - "8080:8080"
    environment:
      - AZURE_DEVOPS_ORGANIZATION=${AZURE_DEVOPS_ORGANIZATION}
      - AZURE_DEVOPS_PAT=${AZURE_DEVOPS_PAT}
    healthcheck:
      test: ["CMD", "netstat", "-an", "|", "grep", ":8080.*LISTEN"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
```

## 🧪 Desarrollo

### Estructura del proyecto
```
src/
├── main/java/com/mcp/server/
│   ├── tools/azuredevops/          # Tools (routers + leaf tools)
│   ├── services/                   # Cliente HTTP unificado + helpers
│   └── transport/                  # Capas de transporte
├── test/                           # Tests unitarios
docker/                             # Scripts Docker
scripts/curl/                       # Scripts de validación cURL
api_doc/                           # Documentación de APIs
```

### Agregar nueva herramienta MCP

Nota: el catálogo expuesto al cliente es el set de **routers**. Si agregas un tool nuevo “leaf”, normalmente también debes:

1) Exponerlo como `operation` dentro de un router existente (o crear un router nuevo si aplica).
2) Mantener la lista blanca de tools expuestos.

1. **Crear helper** en `src/main/java/com/mcp/server/services/helpers/`:
```java
@Service
public class MyHelper {
    private final AzureDevOpsClientService client;
    
    public Map<String, Object> doSomething(String project) {
        return client.getCoreApi(project, null, "myendpoint");
    }
}
```

2. **Crear tool** en `src/main/java/com/mcp/server/tools/azuredevops/`:
```java
@Component
public class MyTool extends AbstractAzureDevOpsTool {
    private final MyHelper helper;
    
    @Override
    public String getName() {
        return "azuredevops_my_operation";
    }
    
    // Implementar métodos abstractos...
}
```

3. **Crear script cURL** en `scripts/curl/area/`:
```bash
#!/bin/bash
source "$(dirname "$0")/../_env.sh"

# Validar endpoint usando curl_json
curl_json "${DEVOPS_BASE}/_apis/my/endpoint?api-version=${AZURE_DEVOPS_API_VERSION}"
```

### Validación
```bash
# Validar endpoint con cURL
./scripts/curl/area/my_endpoint.sh

# Rebuild de imagen Docker
./scripts/build-docker-image.sh --dockerfile Dockerfile --tag mcp-azure-devops:latest

# Smoke test de contenedor
./scripts/test-docker-image.sh

# Ejecutar en modo STDIO
docker run -i --env-file .env mcp-azure-devops:latest stdio
```

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una branch para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la branch (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

---

Para más información sobre el protocolo MCP, visita [Model Context Protocol Specification](https://spec.modelcontextprotocol.io/).
