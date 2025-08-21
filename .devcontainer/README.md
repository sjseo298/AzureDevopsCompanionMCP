# Configuración Docker para Dev Container

Este devcontainer ahora incluye soporte para Docker usando "Docker-outside-of-Docker" que comparte el socket del Docker del host.

## Características añadidas:

### 1. Docker Feature
- **docker-outside-of-docker**: Permite usar Docker commands dentro del container
- **Docker Compose v2**: Para orquestación de containers
- **Docker Buildx**: Para builds avanzados multi-arquitectura

### 2. Mounts
- Monta el socket Docker del host (`/var/run/docker.sock`) para acceso directo

### 3. Port Forwarding  
- Puertos `8080, 8081, 8082` automáticamente disponibles para el MCP server

### 4. VS Code Extensions
- **Docker Extension**: Para administrar containers visualmente
- **Java Pack**: Para desarrollo Java completo
- **Remote Containers**: Para manejo de devcontainers

## Comandos disponibles después del rebuild:

```bash
# Verificar Docker
docker --version
docker-compose --version

# Build del MCP server
docker build -t mcp-azure-devops .

# Ejecutar tests de container
./scripts/test-container.sh

# Docker compose
docker-compose up -d
```

## Para aplicar los cambios:

1. **Command Palette** (Ctrl+Shift+P)
2. Buscar: "Dev Containers: Rebuild Container"  
3. Seleccionar y esperar el rebuild

## Verificación post-rebuild:

```bash
# Estos comandos deben funcionar:
docker ps
docker images
docker-compose --version
```

## Troubleshooting:

Si Docker no funciona después del rebuild:
```bash
# Verificar permisos del socket
ls -la /var/run/docker.sock

# Si es necesario, ajustar permisos
sudo chmod 666 /var/run/docker.sock
```
