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
  --tag <nombre>      Nombre/tag para la imagen (default: sjseo298/mcp-azure-devops)
  --dockerfile <file> Dockerfile a usar (default: Dockerfile)
                      Opciones: Dockerfile, Dockerfile.slim, Dockerfile.ultra
  --no-cache          Construir sin usar cache de Docker
  --push              Push la imagen tras construir (requiere registry configurado)
  --registry <url>    Registry URL para push (default: ninguno)
  --clean             Limpiar imágenes previas antes de construir
  --test              Ejecutar test básico tras la construcción
  --quiet             Salida mínima (solo errores)
  --platform <plats>  Plataformas a construir (default: linux/amd64,linux/arm64)
                      Ejemplos: linux/amd64 | linux/arm64 | linux/amd64,linux/arm64
  --no-multiplatform  Construir solo para la plataforma local (sin buildx)
  --help              Mostrar esta ayuda

Dockerfiles disponibles:
  Dockerfile        - Versión estándar (~473MB)
  Dockerfile.slim   - Versión optimizada (~200MB) - Alpine + capas
  Dockerfile.ultra  - Versión ultra-optimizada (~150MB) - JRE customizado

Ejemplos:
  ./build-docker-image.sh
  ./build-docker-image.sh --dockerfile Dockerfile.slim --tag sjseo298/mcp-azure-devops:slim
  ./build-docker-image.sh --dockerfile Dockerfile.ultra --tag sjseo298/mcp-azure-devops:ultra
  ./build-docker-image.sh --no-cache --clean --test --dockerfile Dockerfile.slim
  ./build-docker-image.sh --platform linux/amd64 --push  # Solo amd64
  ./build-docker-image.sh --no-multiplatform             # Plataforma local solamente
USAGE
}

# Configuración por defecto
DEFAULT_IMAGE_REPO="sjseo298/mcp-azure-devops"
IMAGE_TAG="${DEFAULT_IMAGE_REPO}"
DOCKERFILE="Dockerfile"
NO_CACHE=""
PUSH_IMAGE=false
REGISTRY=""
CLEAN_FIRST=false
RUN_TEST=false
QUIET=false
TAG_AS_LATEST=true
DOCKER_ARGS=""
# Usuario para Docker Hub (si no se especifica se pedirá al usuario en modo interactivo)
DOCKERHUB_USER=""
# Configuración multiplataforma (por defecto: activado con amd64+arm64)
MULTIPLATFORM=true
PLATFORMS="linux/amd64,linux/arm64"
BUILDX_BUILDER="mcp-multiplatform-builder"

# Guardar si se proporcionaron argumentos (para decidir modo interactivo)
HAD_ARGS=$#

# Parsear argumentos
while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag) IMAGE_TAG="$2"; shift 2;;
    --dockerfile) DOCKERFILE="$2"; shift 2;;
    --no-cache) NO_CACHE="--no-cache"; shift;;
    --push) PUSH_IMAGE=true; shift;;
    --dockerhub-user) DOCKERHUB_USER="$2"; shift 2;;
    --registry) REGISTRY="$2"; shift 2;;
    --clean) CLEAN_FIRST=true; shift;;
    --test) RUN_TEST=true; shift;;
    --quiet) QUIET=true; shift;;
    --platform) PLATFORMS="$2"; shift 2;;
    --no-multiplatform) MULTIPLATFORM=false; shift;;
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

compute_latest_tag() {
  local image_ref="$1"
  local last_segment="${image_ref##*/}"
  if [[ "${last_segment}" == *:* ]]; then
    echo "${image_ref%:*}:latest"
  else
    echo "${image_ref}:latest"
  fi
}

detect_local_platform() {
  local arch
  arch="$(uname -m)"
  case "${arch}" in
    x86_64|amd64) echo "linux/amd64";;
    aarch64|arm64) echo "linux/arm64";;
    armv7l) echo "linux/arm/v7";;
    armv6l) echo "linux/arm/v6";;
    *)
      echo "linux/amd64"
      ;;
  esac
}

LATEST_TAG="$(compute_latest_tag "${IMAGE_TAG}")"

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
  default_tag="${DEFAULT_IMAGE_REPO}"
  case $DOCKERFILE in
    "Dockerfile.slim") default_tag="${DEFAULT_IMAGE_REPO}:slim";;
    "Dockerfile.ultra") default_tag="${DEFAULT_IMAGE_REPO}:ultra";;
  esac
  
  # Nombre de la imagen
  read -p "🏷️  Tag de la imagen [default: $default_tag]: " user_tag
  IMAGE_TAG=${user_tag:-$default_tag}
  LATEST_TAG="$(compute_latest_tag "${IMAGE_TAG}")"
  
  # Preguntar si también tagear como latest
  echo ""
  read -p "🏆 ¿También tagear esta imagen como 'latest'? (Y/n): " latest_choice
  if [[ ! $latest_choice =~ ^[Nn]$ ]]; then
    TAG_AS_LATEST=true
    echo -e "${GREEN}   ✅ Se tageará también como: ${LATEST_TAG}${NC}"
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
    echo "Nota: Si dejas vacío el registry, se usará Docker Hub (se te pedirá el usuario si no fue proporcionado)."
    read -p "🌐 Registry URL (opcional) [enter para usar Docker Hub]: " registry_input
    REGISTRY=$registry_input
  fi
  
  # Opciones de multiplataforma
  echo ""
  echo -e "${YELLOW}🖥️  Configuración de plataformas:${NC}"
  echo "Por defecto se construye para: linux/amd64,linux/arm64 (multiplataforma)"
  read -p "🔧 ¿Construir multiplataforma (amd64+arm64)? (Y/n): " multiplatform_choice
  if [[ $multiplatform_choice =~ ^[Nn]$ ]]; then
    MULTIPLATFORM=false
    echo -e "${GREEN}   ✅ Se construirá solo para la plataforma local${NC}"
  else
    MULTIPLATFORM=true
    read -p "📦 Plataformas a construir [default: linux/amd64,linux/arm64]: " platform_input
    if [[ -n "$platform_input" ]]; then
      PLATFORMS="$platform_input"
    fi
    echo -e "${GREEN}   ✅ Se construirá para: ${PLATFORMS}${NC}"
    
    # Advertencia sobre push con multiplataforma
    if [[ "${PUSH_IMAGE}" != true ]]; then
      echo -e "${YELLOW}   ⚠️  Nota: En construcción multiplataforma (amd64+arm64) sin --push, Docker Buildx no puede cargar la imagen en el daemon local.${NC}"
      echo -e "${YELLOW}      El resultado queda en el caché del builder (no verás la imagen en 'docker images').${NC}"
      echo -e "${YELLOW}      Opciones:${NC}"
      echo -e "${YELLOW}        - Usa --push (y luego 'docker pull' / 'docker run --platform ...')${NC}"
      echo -e "${YELLOW}        - O construye UNA sola plataforma (ej. --platform linux/amd64) para habilitar --load${NC}"
      echo -e "${YELLOW}        - O usa --no-multiplatform para build local clásico${NC}"
    fi
  fi
  
  echo ""
  echo -e "${GREEN}✅ Configuración completada:${NC}"
  echo -e "   📄 Dockerfile: ${DOCKERFILE}"
  echo -e "   🏷️  Tag: ${IMAGE_TAG}"
  if [[ "$TAG_AS_LATEST" == true ]]; then
    echo -e "   🏆 Tag adicional: ${LATEST_TAG}"
  fi
  echo -e "   🧹 Limpiar: ${CLEAN_FIRST}"
  echo -e "   🚫 Sin cache: ${NO_CACHE:-false}"
  echo -e "   🧪 Test: ${RUN_TEST}"
  echo -e "   📤 Push: ${PUSH_IMAGE}"
  echo -e "   🖥️  Multiplataforma: ${MULTIPLATFORM}"
  if [[ "${MULTIPLATFORM}" == true ]]; then
    echo -e "   📦 Plataformas: ${PLATFORMS}"
  fi
  [[ -n "$REGISTRY" ]] && echo -e "   🌐 Registry: ${REGISTRY}"
  echo ""
  
  read -p "¿Continuar con la construcción? (Y/n): " confirm
  if [[ $confirm =~ ^[Nn]$ ]]; then
    echo -e "${YELLOW}Construcción cancelada por el usuario.${NC}"
    exit 0
  fi
}

# Verificar si se ejecuta sin argumentos (modo interactivo)
if [[ ${HAD_ARGS} -eq 0 ]]; then
  interactive_mode
fi

LATEST_TAG="$(compute_latest_tag "${IMAGE_TAG}")"

# Si estamos en modo no interactivo y el usuario no pasó --dockerhub-user ni --registry
# y se pidió push, pedimos confirmación y solicitamos el usuario de Docker Hub
if [[ "${PUSH_IMAGE}" == true && -z "${REGISTRY}" && -z "${DOCKERHUB_USER}" ]]; then
  if [[ -t 0 ]]; then
    # TTY disponible - preguntar ahora
    read -p "Introduce tu usuario de Docker Hub (ej: sjseo298): " input_dh_user
    DOCKERHUB_USER=${input_dh_user}
  else
    # No hay TTY: abortar para evitar push accidental
    log_error "Se solicitó --push pero no se proporcionó --registry ni --dockerhub-user y no hay TTY para preguntar. Aborting."
    exit 1
  fi
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

# Función para configurar buildx para multiplataforma
setup_buildx() {
  log "🔧 Configurando Docker Buildx para construcción multiplataforma..."
  
  # Verificar si buildx está disponible
  if ! docker buildx version >/dev/null 2>&1; then
    log_error "Docker Buildx no está disponible. Por favor actualiza Docker o usa --no-multiplatform"
    exit 1
  fi
  
  # Verificar si el builder ya existe
  if docker buildx inspect "${BUILDX_BUILDER}" >/dev/null 2>&1; then
    log "Builder '${BUILDX_BUILDER}' ya existe, usándolo..."
    docker buildx use "${BUILDX_BUILDER}"
  else
    log "Creando nuevo builder '${BUILDX_BUILDER}'..."
    docker buildx create --name "${BUILDX_BUILDER}" --driver docker-container --bootstrap --use
  fi
  
  # Nota: NO ejecutamos multiarch/qemu-user-static aquí porque puede corromper
  # los binfmt handlers y causar "Exec format error" en binarios nativos.
  # Docker Buildx con driver docker-container ya incluye soporte QEMU integrado.
  # Si necesitas configurar QEMU manualmente, hazlo FUERA de este script con:
  #   docker run --rm --privileged multiarch/qemu-user-static --reset -p yes
  # y reinicia el Docker daemon después si hay problemas.
  
  # Verificar que el builder soporta las plataformas solicitadas
  log "Verificando plataformas soportadas por el builder..."
  SUPPORTED_PLATFORMS=$(docker buildx inspect "${BUILDX_BUILDER}" --bootstrap 2>/dev/null | grep -i "Platforms:" | head -1 || echo "")
  if [[ -n "${SUPPORTED_PLATFORMS}" ]]; then
    log "Plataformas disponibles: ${SUPPORTED_PLATFORMS}"
  fi
  
  log_success "Docker Buildx configurado correctamente"
}

# Construir la imagen
log "🔨 Construyendo imagen Docker..."

if [[ "${QUIET}" == true ]]; then
  DOCKER_ARGS="--quiet"
fi

if [[ "${MULTIPLATFORM}" == true ]]; then
  # Construcción multiplataforma con buildx
  setup_buildx
  
  # Construir tags
  BUILD_TAGS="-t ${IMAGE_TAG}"
  if [[ "$TAG_AS_LATEST" == true ]]; then
    BUILD_TAGS="${BUILD_TAGS} -t ${LATEST_TAG}"
  fi
  
  # Determinar si usar --push o --load
  # Nota: --load solo funciona con una plataforma, --push funciona con múltiples
  OUTPUT_FLAG=""
  LOCAL_PLATFORM_FOR_LOAD=""
  if [[ "${PUSH_IMAGE}" == true ]]; then
    # Para push, necesitamos el tag completo con registry/user
    if [[ -z "${REGISTRY}" && -n "${DOCKERHUB_USER}" ]]; then
      # Re-construir tags para Docker Hub
      if [[ "${IMAGE_TAG}" != */* ]]; then
        BUILD_TAGS="-t ${DOCKERHUB_USER}/${IMAGE_TAG}"
        if [[ "$TAG_AS_LATEST" == true ]]; then
          BUILD_TAGS="${BUILD_TAGS} -t ${DOCKERHUB_USER}/${LATEST_TAG}"
        fi
        IMAGE_TAG="${DOCKERHUB_USER}/${IMAGE_TAG}"
        LATEST_TAG="$(compute_latest_tag "${IMAGE_TAG}")"
      fi
    elif [[ -n "${REGISTRY}" ]]; then
      if [[ "${IMAGE_TAG}" != *"${REGISTRY}"* ]]; then
        BUILD_TAGS="-t ${REGISTRY}/${IMAGE_TAG}"
        if [[ "$TAG_AS_LATEST" == true ]]; then
          BUILD_TAGS="${BUILD_TAGS} -t ${REGISTRY}/${LATEST_TAG}"
        fi
        IMAGE_TAG="${REGISTRY}/${IMAGE_TAG}"
        LATEST_TAG="$(compute_latest_tag "${IMAGE_TAG}")"
      fi
    fi
    OUTPUT_FLAG="--push"
    log "📤 Las imágenes se subirán automáticamente al registry tras la construcción"
  else
    # Sin push: si son múltiples plataformas, no podemos usar --load.
    # Para garantizar que el tag quede usable localmente, construimos y cargamos SOLO la plataforma local.
    if [[ "${PLATFORMS}" == *","* ]]; then
      LOCAL_PLATFORM="$(detect_local_platform)"
      log_warning "Construcción multiplataforma sin --push no se puede cargar al daemon local."
      log_warning "Construyendo y cargando SOLO la plataforma local (${LOCAL_PLATFORM}) para dejar la imagen disponible en 'docker images' y 'docker run'."
      log_warning "Si necesitas un verdadero multi-arch (amd64+arm64) usable por otros hosts, usa --push."
      PLATFORMS="${LOCAL_PLATFORM}"
      OUTPUT_FLAG="--load"
    else
      # Una sola plataforma, podemos usar --load
      OUTPUT_FLAG="--load"
    fi
  fi
  
  log "Comando: docker buildx build ${NO_CACHE} --platform ${PLATFORMS} ${BUILD_TAGS} ${OUTPUT_FLAG} -f ${DOCKERFILE} ."
  
  if ! docker buildx build ${NO_CACHE} ${DOCKER_ARGS} --platform "${PLATFORMS}" ${BUILD_TAGS} ${OUTPUT_FLAG} -f "${DOCKERFILE}" .; then
    if [[ "${OUTPUT_FLAG}" == "--push" ]]; then
      log_error "Falló la construcción/push de la imagen Docker con buildx. Causas comunes: no estar logueado ('docker login'), repo inexistente o falta de permisos en el registry."
      LOCAL_PLATFORM_FOR_LOAD="$(detect_local_platform)"
      log_warning "Intentando dejar imagen disponible localmente (best-effort) construyendo ${LOCAL_PLATFORM_FOR_LOAD} con --load..."
      if docker buildx build ${NO_CACHE} ${DOCKER_ARGS} --platform "${LOCAL_PLATFORM_FOR_LOAD}" ${BUILD_TAGS} --load -f "${DOCKERFILE}" .; then
        log_success "Imagen cargada localmente tras fallo de push: ${IMAGE_TAG}"
        log_success "Tag local adicional (si aplica): ${LATEST_TAG}"
      else
        log_warning "También falló el build local con --load."
      fi
    else
      log_error "Falló la construcción de la imagen Docker con buildx"
    fi
    exit 1
  fi
  
  log_success "Imagen multiplataforma construida exitosamente: ${IMAGE_TAG}"
  log "📦 Plataformas: ${PLATFORMS}"
  
  # Si hicimos push con buildx, marcar para no hacer push de nuevo
  if [[ "${PUSH_IMAGE}" == true ]]; then
    PUSH_IMAGE=false  # Ya se hizo push con buildx
    log_success "Imagen subida exitosamente a registry"

    # Verificación best-effort del manifest remoto (no falla el script si no hay acceso)
    if docker buildx imagetools inspect "${LATEST_TAG}" >/dev/null 2>&1; then
      log_success "Verificación: manifest disponible en registry (${LATEST_TAG})"
    else
      log_warning "No se pudo verificar el manifest remoto con 'docker buildx imagetools inspect'."
    fi

    # Garantizar disponibilidad local aunque se haya hecho push
    # (buildx --push no carga al daemon local; hacemos un build adicional de la plataforma local con --load)
    LOCAL_PLATFORM_FOR_LOAD="$(detect_local_platform)"
    log "📦 Cargando imagen local para '${LOCAL_PLATFORM_FOR_LOAD}' (para 'docker run')..."
    if docker buildx build ${NO_CACHE} ${DOCKER_ARGS} --platform "${LOCAL_PLATFORM_FOR_LOAD}" ${BUILD_TAGS} --load -f "${DOCKERFILE}" .; then
      log_success "Imagen disponible localmente: ${IMAGE_TAG}"
      if [[ "$TAG_AS_LATEST" == true ]]; then
        log_success "Tag local adicional: ${LATEST_TAG}"
      fi
    else
      log_warning "El push se completó, pero no se pudo cargar la imagen localmente con --load."
    fi
  fi
  
else
  # Construcción tradicional (plataforma local)
  log "Comando: docker build ${NO_CACHE} -f ${DOCKERFILE} -t ${IMAGE_TAG} ."
  
  if ! docker build ${NO_CACHE} ${DOCKER_ARGS} -f "${DOCKERFILE}" -t "${IMAGE_TAG}" .; then
    log_error "Falló la construcción de la imagen Docker"
    exit 1
  fi
  
  log_success "Imagen construida exitosamente: ${IMAGE_TAG}"
  
  # Tagear como latest si se solicita (solo en modo no-multiplataforma)
  if [[ "$TAG_AS_LATEST" == true ]]; then
    log "🏆 Tageando imagen como latest..."
    if docker tag "${IMAGE_TAG}" "${LATEST_TAG}"; then
      log_success "Imagen tageada como: ${LATEST_TAG}"
    else
      log_error "Error al tagear la imagen como latest"
    fi
  fi
fi

# Mostrar información de la imagen
log "📊 Información de la imagen:"
if [[ "${MULTIPLATFORM}" == true ]]; then
  # Para imágenes multiplataforma, mostrar información del manifest
  if [[ "${PUSH_IMAGE}" == false ]] && [[ "${PLATFORMS}" == *","* ]]; then
    log_warning "Las imágenes multiplataforma sin --push no se cargan localmente"
    log "Para ver las plataformas disponibles después de push, usa:"
    log "  docker buildx imagetools inspect ${IMAGE_TAG}"
  else
    # Si se hizo push o es una sola plataforma
    if docker images "${IMAGE_TAG}" -q 2>/dev/null | grep -q .; then
      docker images "${IMAGE_TAG}" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"
    fi
    log "🖥️  Plataformas construidas: ${PLATFORMS}"
  fi
else
  if [[ "$TAG_AS_LATEST" == true ]]; then
    # Mostrar ambas imágenes
    echo "REPOSITORY         TAG       IMAGE ID       CREATED         SIZE"
    docker images "${IMAGE_TAG}" --format "{{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"
    docker images "${LATEST_TAG}" --format "{{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"
  else
    docker images "${IMAGE_TAG}" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"
  fi
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
  # Si no se indicó un registry, usar Docker Hub bajo el usuario DOCKERHUB_USER
  if [[ -z "${REGISTRY}" ]]; then
    # Determinar nombre y tag base
    # Si IMAGE_TAG incluye un tag (user/repo:tag o repo:tag), separar
    REPO_AND_TAG="${IMAGE_TAG}"
    # Si IMAGE_TAG no contiene '/', asumimos repo:tag y preprendemos el user
    if [[ "${REPO_AND_TAG}" != */* ]]; then
      REGISTRY_TAG="${DOCKERHUB_USER}/${REPO_AND_TAG}"
    else
      # Ya contiene user/repo, solo asegurarse que use el DOCKERHUB_USER si no se proporcionó registry
      # Si ya tiene formato user/repo, mantenerlo
      REGISTRY_TAG="${REPO_AND_TAG}"
    fi

    log "🏷️  Re-etiquetando imagen para Docker Hub: ${REGISTRY_TAG}"
    if ! docker tag "${IMAGE_TAG}" "${REGISTRY_TAG}"; then
      log_error "Error al tagear la imagen para Docker Hub"
      exit 1
    fi
    IMAGE_TAG="${REGISTRY_TAG}"
  else
    if [[ "${IMAGE_TAG}" != *"${REGISTRY}"* ]]; then
      # Re-tag la imagen con el registry proporcionado
      REGISTRY_TAG="${REGISTRY}/${IMAGE_TAG}"
      log "🏷️  Re-etiquetando imagen para registry: ${REGISTRY_TAG}"
      docker tag "${IMAGE_TAG}" "${REGISTRY_TAG}"
      IMAGE_TAG="${REGISTRY_TAG}"
    fi
  fi

  log "⬆️  Subiendo imagen: ${IMAGE_TAG}"

  if ! docker push "${IMAGE_TAG}"; then
    log_error "Falló el push de la imagen"
    exit 1
  fi

  log_success "Imagen subida exitosamente: ${IMAGE_TAG}"
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
  INSTRUCTION_TAG="${LATEST_TAG}"
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
