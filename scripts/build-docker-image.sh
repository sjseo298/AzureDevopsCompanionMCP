#!/usr/bin/env bash
set -euo pipefail

# Script para reconstruir la imagen Docker del servidor MCP Azure DevOps
# Basado en las instrucciones del README.md

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

usage() {
  cat <<'USAGE'
Uso: build-docker-image.sh [opciones]

Descripción:
  Reconstruye la imagen Docker del servidor MCP Azure DevOps según las
  especificaciones del README.md

Opciones:
  --tag <nombre>      Nombre/tag para la imagen (default: mcp-azure-devops)
  --dockerfile <file> Dockerfile a usar (default: Dockerfile)
                      Opciones: Dockerfile, Dockerfile.slim, Dockerfile.ultra
  --no-cache          Construir sin usar cache de Docker
  --push              Push la imagen tras construir (requiere registry configurado)
  --registry <url>    Registry URL para push (default: ninguno)
  --clean             Limpiar imágenes previas antes de construir
  --test              Ejecutar test básico tras la construcción
  --quiet             Salida mínima (solo errores)
  --help              Mostrar esta ayuda

Dockerfiles disponibles:
  Dockerfile        - Versión estándar (~473MB)
  Dockerfile.slim   - Versión optimizada (~200MB) - Alpine + capas
  Dockerfile.ultra  - Versión ultra-optimizada (~150MB) - JRE customizado

Ejemplos:
  ./build-docker-image.sh
  ./build-docker-image.sh --dockerfile Dockerfile.slim --tag mcp-azure-devops:slim
  ./build-docker-image.sh --dockerfile Dockerfile.ultra --tag mcp-azure-devops:ultra
  ./build-docker-image.sh --no-cache --clean --test --dockerfile Dockerfile.slim
USAGE
}

# Configuración por defecto
IMAGE_TAG="mcp-azure-devops"
DOCKERFILE="Dockerfile"
NO_CACHE=""
PUSH_IMAGE=false
REGISTRY=""
CLEAN_FIRST=false
RUN_TEST=false
QUIET=false
TAG_AS_LATEST=true
DOCKER_ARGS=""

# Parsear argumentos
while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag) IMAGE_TAG="$2"; shift 2;;
    --dockerfile) DOCKERFILE="$2"; shift 2;;
    --no-cache) NO_CACHE="--no-cache"; shift;;
    --push) PUSH_IMAGE=true; shift;;
    --registry) REGISTRY="$2"; shift 2;;
    --clean) CLEAN_FIRST=true; shift;;
    --test) RUN_TEST=true; shift;;
    --quiet) QUIET=true; shift;;
    -h|--help) usage; exit 0;;
    *) echo -e "${RED}Argumento no reconocido: $1${NC}" >&2; usage; exit 1;;
  esac
done

log() {
  if [[ "${QUIET}" == false ]]; then
    echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1"
  fi
}

log_success() {
  echo -e "${GREEN}[$(date +'%H:%M:%S')] ✅ $1${NC}"
}

log_warning() {
  echo -e "${YELLOW}[$(date +'%H:%M:%S')] ⚠️  $1${NC}"
}

log_error() {
  echo -e "${RED}[$(date +'%H:%M:%S')] ❌ $1${NC}" >&2
}

# Función para modo interactivo
interactive_mode() {
  echo -e "${BLUE}🚀 Construcción Interactiva de Imagen Docker MCP Azure DevOps${NC}"
  echo ""
  
  # Mostrar Dockerfiles disponibles
  echo -e "${YELLOW}📄 Dockerfiles disponibles:${NC}"
  echo "1) Dockerfile        - Versión estándar (~473MB)"
  echo "2) Dockerfile.slim   - Versión optimizada (~358MB) - Alpine + capas"
  echo "3) Dockerfile.ultra  - Versión ultra-optimizada (~191MB) - JRE customizado"
  echo ""
  
  # Seleccionar Dockerfile
  while true; do
    read -p "🔸 Selecciona el Dockerfile (1-3) [default: 1]: " dockerfile_choice
    dockerfile_choice=${dockerfile_choice:-1}
    
    case $dockerfile_choice in
      1) DOCKERFILE="Dockerfile"; break;;
      2) DOCKERFILE="Dockerfile.slim"; break;;
      3) DOCKERFILE="Dockerfile.ultra"; break;;
      *) echo -e "${RED}Opción inválida. Por favor selecciona 1, 2 o 3.${NC}";;
    esac
  done
  
  # Configurar tag basado en dockerfile
  default_tag="mcp-azure-devops"
  case $DOCKERFILE in
    "Dockerfile.slim") default_tag="mcp-azure-devops:slim";;
    "Dockerfile.ultra") default_tag="mcp-azure-devops:ultra";;
  esac
  
  # Nombre de la imagen
  read -p "🏷️  Tag de la imagen [default: $default_tag]: " user_tag
  IMAGE_TAG=${user_tag:-$default_tag}
  
  # Preguntar si también tagear como latest
  echo ""
  read -p "🏆 ¿También tagear esta imagen como 'latest'? (Y/n): " latest_choice
  if [[ ! $latest_choice =~ ^[Nn]$ ]]; then
    TAG_AS_LATEST=true
    echo -e "${GREEN}   ✅ Se tageará también como: mcp-azure-devops:latest${NC}"
  else
    TAG_AS_LATEST=false
  fi
  
  # Opciones adicionales
  echo ""
  echo -e "${YELLOW}⚙️  Opciones de construcción:${NC}"
  
  read -p "🧹 ¿Limpiar imágenes previas? (y/N): " clean_choice
  if [[ $clean_choice =~ ^[Yy]$ ]]; then
    CLEAN_FIRST=true
  fi
  
  read -p "🚫 ¿Construir sin cache? (y/N): " cache_choice
  if [[ $cache_choice =~ ^[Yy]$ ]]; then
    NO_CACHE="--no-cache"
  fi
  
  read -p "🧪 ¿Ejecutar test tras la construcción? (y/N): " test_choice
  if [[ $test_choice =~ ^[Yy]$ ]]; then
    RUN_TEST=true
  fi
  
  read -p "📤 ¿Push la imagen tras construir? (y/N): " push_choice
  if [[ $push_choice =~ ^[Yy]$ ]]; then
    PUSH_IMAGE=true
    read -p "🌐 Registry URL (opcional): " registry_input
    REGISTRY=$registry_input
  fi
  
  echo ""
  echo -e "${GREEN}✅ Configuración completada:${NC}"
  echo -e "   📄 Dockerfile: ${DOCKERFILE}"
  echo -e "   🏷️  Tag: ${IMAGE_TAG}"
  if [[ "$TAG_AS_LATEST" == true ]]; then
    echo -e "   🏆 Tag adicional: mcp-azure-devops:latest"
  fi
  echo -e "   🧹 Limpiar: ${CLEAN_FIRST}"
  echo -e "   🚫 Sin cache: ${NO_CACHE:-false}"
  echo -e "   🧪 Test: ${RUN_TEST}"
  echo -e "   📤 Push: ${PUSH_IMAGE}"
  [[ -n "$REGISTRY" ]] && echo -e "   🌐 Registry: ${REGISTRY}"
  echo ""
  
  read -p "¿Continuar con la construcción? (Y/n): " confirm
  if [[ $confirm =~ ^[Nn]$ ]]; then
    echo -e "${YELLOW}Construcción cancelada por el usuario.${NC}"
    exit 0
  fi
}

# Verificar si se ejecuta sin argumentos (modo interactivo)
if [[ $# -eq 0 ]]; then
  interactive_mode
fi

# Verificar que estamos en el directorio correcto
if [[ ! -f "${PROJECT_ROOT}/${DOCKERFILE}" ]]; then
  log_error "No se encontró ${DOCKERFILE} en ${PROJECT_ROOT}"
  log_error "Dockerfiles disponibles:"
  ls -1 "${PROJECT_ROOT}"/Dockerfile* 2>/dev/null || log_error "No se encontraron Dockerfiles"
  exit 1
fi

if [[ ! -f "${PROJECT_ROOT}/README.md" ]]; then
  log_error "No se encontró README.md - no parece ser el proyecto correcto"
  exit 1
fi

log "🚀 Iniciando construcción de imagen Docker MCP Azure DevOps"
log "📁 Directorio del proyecto: ${PROJECT_ROOT}"
log "📄 Dockerfile: ${DOCKERFILE}"
log "🏷️  Tag de imagen: ${IMAGE_TAG}"

cd "${PROJECT_ROOT}"

# Limpiar imágenes previas si se solicita
if [[ "${CLEAN_FIRST}" == true ]]; then
  log "🧹 Limpiando imágenes previas..."
  
  # Remover imágenes con el mismo tag
  if docker images "${IMAGE_TAG}" -q | grep -q .; then
    log "Eliminando imagen existente: ${IMAGE_TAG}"
    docker rmi "${IMAGE_TAG}" || log_warning "No se pudo eliminar imagen ${IMAGE_TAG}"
  fi
  
  # Limpiar imágenes dangling
  if docker images -f "dangling=true" -q | grep -q .; then
    log "Eliminando imágenes dangling..."
    docker image prune -f || true
  fi
  
  log_success "Limpieza completada"
fi

# Verificar que los archivos necesarios existen
log "🔍 Verificando archivos necesarios..."

REQUIRED_FILES=(
  "${DOCKERFILE}"
  "build.gradle"
  "gradle.properties"
  "src/main/java/com/mcp/server/McpServerApplication.java"
  "docker/entrypoint.sh"
)

for file in "${REQUIRED_FILES[@]}"; do
  if [[ ! -f "${file}" ]]; then
    log_error "Archivo requerido no encontrado: ${file}"
    exit 1
  fi
done

log_success "Todos los archivos necesarios están presentes"

# Construir la imagen
log "🔨 Construyendo imagen Docker..."
log "Comando: docker build ${NO_CACHE} -f ${DOCKERFILE} -t ${IMAGE_TAG} ."

if [[ "${QUIET}" == true ]]; then
  DOCKER_ARGS="--quiet"
fi

if ! docker build ${NO_CACHE} ${DOCKER_ARGS} -f "${DOCKERFILE}" -t "${IMAGE_TAG}" .; then
  log_error "Falló la construcción de la imagen Docker"
  exit 1
fi

log_success "Imagen construida exitosamente: ${IMAGE_TAG}"

# Tagear como latest si se solicita
if [[ "$TAG_AS_LATEST" == true ]]; then
  log "🏆 Tageando imagen como latest..."
  if docker tag "${IMAGE_TAG}" "mcp-azure-devops:latest"; then
    log_success "Imagen tageada como: mcp-azure-devops:latest"
  else
    log_error "Error al tagear la imagen como latest"
  fi
fi

# Mostrar información de la imagen
log "📊 Información de la imagen:"
if [[ "$TAG_AS_LATEST" == true ]]; then
  # Mostrar ambas imágenes
  echo "REPOSITORY         TAG       IMAGE ID       CREATED         SIZE"
  docker images "${IMAGE_TAG}" --format "{{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"
  docker images "mcp-azure-devops:latest" --format "{{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"
else
  docker images "${IMAGE_TAG}" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"
fi

# Test básico si se solicita
if [[ "${RUN_TEST}" == true ]]; then
  log "🧪 Ejecutando test básico..."
  
  # Test 1: Verificar que la imagen arranca
  log "Test 1: Verificando inicio de contenedor..."
  if timeout 10s docker run --rm "${IMAGE_TAG}" --help >/dev/null 2>&1; then
    log_success "Test 1 pasó: Contenedor inicia correctamente"
  else
    log_warning "Test 1 falló: Problema al iniciar contenedor"
  fi
  
  # Test 2: Verificar estructura interna
  log "Test 2: Verificando estructura interna..."
  if docker run --rm "${IMAGE_TAG}" sh -c "ls -la /app/app.jar" >/dev/null 2>&1; then
    log_success "Test 2 pasó: JAR encontrado en ubicación correcta"
  else
    log_warning "Test 2 falló: JAR no encontrado"
  fi
  
  log_success "Tests básicos completados"
fi

# Push si se solicita
if [[ "${PUSH_IMAGE}" == true ]]; then
  if [[ -n "${REGISTRY}" && "${IMAGE_TAG}" != *"${REGISTRY}"* ]]; then
    # Re-tag la imagen con el registry
    REGISTRY_TAG="${REGISTRY}/${IMAGE_TAG}"
    log "🏷️  Re-etiquetando imagen para registry: ${REGISTRY_TAG}"
    docker tag "${IMAGE_TAG}" "${REGISTRY_TAG}"
    IMAGE_TAG="${REGISTRY_TAG}"
  fi
  
  log "⬆️  Subiendo imagen: ${IMAGE_TAG}"
  
  if ! docker push "${IMAGE_TAG}"; then
    log_error "Falló el push de la imagen"
    exit 1
  fi
  
  log_success "Imagen subida exitosamente"
fi

# Mostrar instrucciones de uso
log_success "🎉 ¡Construcción completada!"
echo
echo -e "${GREEN}📖 Instrucciones de uso:${NC}"
echo
echo -e "${BLUE}1. Crear archivo .env con tus credenciales:${NC}"
echo "   AZURE_DEVOPS_ORGANIZATION=tu-organizacion"
echo "   AZURE_DEVOPS_PAT=tu-personal-access-token"
# Determinar qué tag usar en las instrucciones (preferir latest si existe)
INSTRUCTION_TAG="${IMAGE_TAG}"
if [[ "$TAG_AS_LATEST" == true ]]; then
  INSTRUCTION_TAG="mcp-azure-devops:latest"
fi

echo
echo -e "${BLUE}2. Ejecutar en modo STDIO (para clientes MCP locales):${NC}"
echo "   docker run -i --env-file .env ${INSTRUCTION_TAG} stdio"
echo
echo -e "${BLUE}3. Ejecutar en modo HTTP (para acceso remoto):${NC}"
echo "   docker run -p 8080:8080 --env-file .env ${INSTRUCTION_TAG} http"
echo
echo -e "${BLUE}4. Configurar en mcp.json:${NC}"
cat <<'CONFIG'
{
  "servers": {
    "azure-devops-mcp": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "--env", "AZURE_DEVOPS_ORGANIZATION=tu-org",
        "--env", "AZURE_DEVOPS_PAT=tu-pat",
        "IMAGE_TAG_HERE", "stdio"
      ]
    }
  }
}
CONFIG

echo -e "${YELLOW}Reemplaza 'IMAGE_TAG_HERE' con: ${INSTRUCTION_TAG}${NC}"
echo
