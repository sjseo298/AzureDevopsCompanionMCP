# Scripts del Proyecto MCP Azure DevOps

Esta carpeta contiene scripts útiles para el desarrollo, construcción y testing del servidor MCP Azure DevOps.

## 📜 Scripts Disponibles

### 🐳 Docker

#### `build-docker-image.sh`
Construye la imagen Docker del servidor MCP según las especificaciones del README principal.

**Uso básico:**
```bash
./scripts/build-docker-image.sh
```

**Opciones avanzadas:**
```bash
# Construcción limpia con tests
./scripts/build-docker-image.sh --clean --test

# Con tag personalizado
./scripts/build-docker-image.sh --tag myregistry.io/mcp-azure-devops:v1.0.0

# Sin caché
./scripts/build-docker-image.sh --no-cache

# Para push a registry
./scripts/build-docker-image.sh --registry ghcr.io --push --tag ghcr.io/usuario/mcp-azure-devops:latest
```

#### `test-docker-image.sh`
Prueba la imagen Docker construida para verificar que funciona correctamente.

**Uso:**
```bash
# Probar modo STDIO
./scripts/test-docker-image.sh

# Probar modo HTTP
./scripts/test-docker-image.sh --mode http

# Probar ambos modos
./scripts/test-docker-image.sh --mode all

# Con imagen específica
./scripts/test-docker-image.sh --image mcp-azure-devops:custom
```

### 🔄 Workflow Típico

1. **Después de cambios en el código:**
   ```bash
   # Reconstruir imagen
   ./scripts/build-docker-image.sh --clean
   
   # Probar que funciona
   ./scripts/test-docker-image.sh --mode all
   ```

2. **Para producción:**
   ```bash
   # Construcción para producción
   ./scripts/build-docker-image.sh \
     --no-cache \
     --tag mcp-azure-devops:$(date +%Y%m%d-%H%M%S) \
     --test
   ```

3. **Para development/testing:**
   ```bash
   # Construcción rápida
   ./scripts/build-docker-image.sh --test
   ```

### 📋 Requisitos

- **Docker**: Para construcción y testing de imágenes
- **bash**: Todos los scripts requieren bash
- **curl**: Para tests HTTP
- **jq**: Para JSON processing (ya incluido en la imagen)
- **netstat**: Para verificar puertos (generalmente disponible)

### 🔧 Variables de Entorno

Los scripts respetan las siguientes variables de entorno si están definidas:

- `DOCKER_REGISTRY`: Registry por defecto para push
- `MCP_IMAGE_TAG`: Tag por defecto para la imagen
- `MCP_HTTP_PORT`: Puerto por defecto para modo HTTP

Ejemplo:
```bash
export DOCKER_REGISTRY="ghcr.io/miusuario"
export MCP_IMAGE_TAG="mcp-azure-devops:dev"
./scripts/build-docker-image.sh --push
```

### 🚨 Troubleshooting

#### Error: "Docker no está disponible"
```bash
# Verificar Docker
docker --version

# En sistemas Linux, agregar usuario al grupo docker
sudo usermod -aG docker $USER
# Luego logout/login
```

#### Error: "Imagen no encontrada"
```bash
# Listar imágenes disponibles
docker images | grep mcp

# Construir imagen si no existe
./scripts/build-docker-image.sh
```

#### Error: "Puerto en uso" (modo HTTP)
```bash
# Ver qué está usando el puerto
netstat -an | grep :8080

# Usar puerto alternativo
./scripts/test-docker-image.sh --mode http --port 8090
```

#### Error: "Timeout en tests"
```bash
# Aumentar timeout
./scripts/test-docker-image.sh --timeout 60

# Ver logs de contenedor para debugging
docker logs <container_id>
```

### 💡 Tips

1. **Construcción más rápida**: Si solo cambiaste código Java, la construcción será más rápida por el cache de Docker.

2. **Debugging**: Si algo falla, usa `--test` para ver más información:
   ```bash
   ./scripts/build-docker-image.sh --test
   ```

3. **Limpieza**: Para liberar espacio en disco:
   ```bash
   ./scripts/build-docker-image.sh --clean
   
   # O más agresivo
   docker system prune -a
   ```

4. **Multiple tags**: Puedes crear múltiples tags:
   ```bash
   ./scripts/build-docker-image.sh --tag mcp-azure-devops:latest
   docker tag mcp-azure-devops:latest mcp-azure-devops:stable
   ```

5. **CI/CD Integration**: Los scripts son compatibles con pipelines de CI/CD:
   ```bash
   # En pipeline
   ./scripts/build-docker-image.sh --quiet --no-cache --tag $CI_REGISTRY/$CI_PROJECT_PATH:$CI_COMMIT_SHA --push
   ```

### 🔗 Enlaces Relacionados

- [README principal](../README.md) - Documentación completa del proyecto
- [Dockerfile](../Dockerfile) - Definición de la imagen Docker  
- [docker/](../docker/) - Scripts internos del contenedor
