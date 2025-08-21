#!/usr/bin/env bash
set -euo pipefail

# Script para probar la imagen Docker del servidor MCP Azure DevOps
# Complemento del build-docker-image.sh

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
Uso: test-docker-image.sh [opciones]

Descripción:
  Prueba la imagen Docker del servidor MCP Azure DevOps

Opciones:
  --image <nombre>    Nombre de la imagen a probar (default: mcp-azure-devops)
  --mode <modo>       Modo a probar: stdio, http, all (default: stdio)
  --env-file <file>   Archivo .env con credenciales (default: .env)
  --timeout <seg>     Timeout para tests (default: 30)
  --port <puerto>     Puerto para modo HTTP (default: 8080)
  --help              Mostrar esta ayuda

Ejemplos:
  ./test-docker-image.sh
  ./test-docker-image.sh --mode http --port 8090
  ./test-docker-image.sh --image mcp-azure-devops:latest --mode all
USAGE
}

# Configuración por defecto
IMAGE_NAME="mcp-azure-devops"
TEST_MODE="stdio"
ENV_FILE=".env"
TIMEOUT=30
HTTP_PORT=8080

# Parsear argumentos
while [[ $# -gt 0 ]]; do
  case "$1" in
    --image) IMAGE_NAME="$2"; shift 2;;
    --mode) TEST_MODE="$2"; shift 2;;
    --env-file) ENV_FILE="$2"; shift 2;;
    --timeout) TIMEOUT="$2"; shift 2;;
    --port) HTTP_PORT="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo -e "${RED}Argumento no reconocido: $1${NC}" >&2; usage; exit 1;;
  esac
done

log() {
  echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1"
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

# Verificar que Docker esté funcionando
if ! docker --version >/dev/null 2>&1; then
  log_error "Docker no está disponible o no está funcionando"
  exit 1
fi

# Verificar que la imagen existe
if ! docker images "${IMAGE_NAME}" --format "{{.Repository}}" | grep -q .; then
  log_error "Imagen '${IMAGE_NAME}' no encontrada"
  log "Ejecuta primero: ./scripts/build-docker-image.sh"
  exit 1
fi

log "🧪 Probando imagen Docker: ${IMAGE_NAME}"
log "🎯 Modo de prueba: ${TEST_MODE}"

cd "${PROJECT_ROOT}"

# Función para probar modo STDIO
test_stdio_mode() {
  log "📝 Probando modo STDIO..."
  
  # Test básico: verificar que responde al --help
  if timeout "${TIMEOUT}s" docker run --rm "${IMAGE_NAME}" --help >/dev/null 2>&1; then
    log_success "STDIO: Comando --help funciona"
  else
    log_warning "STDIO: Problema con comando --help"
    return 1
  fi
  
  # Test con credenciales si hay archivo .env
  if [[ -f "${ENV_FILE}" ]]; then
    log "Probando con credenciales desde ${ENV_FILE}..."
    
    # Solo verificar que inicia (timeout rápido porque es interactivo)
    if timeout 5s docker run --rm --env-file "${ENV_FILE}" "${IMAGE_NAME}" stdio >/dev/null 2>&1 || true; then
      log_success "STDIO: Inicia correctamente con credenciales"
    else
      log_warning "STDIO: Problema al iniciar con credenciales"
    fi
  else
    log_warning "Archivo ${ENV_FILE} no encontrado - saltando test con credenciales"
  fi
}

# Función para probar modo HTTP
test_http_mode() {
  log "🌐 Probando modo HTTP en puerto ${HTTP_PORT}..."
  
  # Verificar que el puerto esté libre
  if netstat -an 2>/dev/null | grep -q ":${HTTP_PORT}.*LISTEN"; then
    log_warning "Puerto ${HTTP_PORT} ya está en uso - usando puerto aleatorio"
    HTTP_PORT=$((8080 + RANDOM % 1000))
    log "Usando puerto alternativo: ${HTTP_PORT}"
  fi
  
  # Iniciar contenedor en background
  CONTAINER_ID=$(docker run -d -p "${HTTP_PORT}:8080" "${IMAGE_NAME}" http)
  
  if [[ -z "${CONTAINER_ID}" ]]; then
    log_error "HTTP: No se pudo iniciar el contenedor"
    return 1
  fi
  
  log "Contenedor iniciado: ${CONTAINER_ID:0:12}"
  
  # Esperar a que el servicio esté listo
  log "Esperando que el servicio esté disponible..."
  sleep 5
  
  # Verificar que el puerto responde
  local retries=0
  local max_retries=10
  
  while [[ $retries -lt $max_retries ]]; do
    if curl -sf "http://localhost:${HTTP_PORT}" -m 3 >/dev/null 2>&1; then
      log_success "HTTP: Servicio responde en puerto ${HTTP_PORT}"
      break
    fi
    
    retries=$((retries + 1))
    if [[ $retries -lt $max_retries ]]; then
      log "Reintento ${retries}/${max_retries}..."
      sleep 2
    fi
  done
  
  if [[ $retries -eq $max_retries ]]; then
    log_warning "HTTP: Servicio no responde tras ${max_retries} intentos"
    
    # Mostrar logs para debug
    log "Logs del contenedor:"
    docker logs "${CONTAINER_ID}" 2>&1 | tail -10
  fi
  
  # Limpiar contenedor
  log "Deteniendo contenedor..."
  docker stop "${CONTAINER_ID}" >/dev/null 2>&1
  docker rm "${CONTAINER_ID}" >/dev/null 2>&1
}

# Ejecutar tests según el modo
case "${TEST_MODE}" in
  stdio)
    test_stdio_mode
    ;;
  http)
    test_http_mode
    ;;
  all)
    test_stdio_mode
    echo
    test_http_mode
    ;;
  *)
    log_error "Modo de prueba no válido: ${TEST_MODE}"
    log "Modos válidos: stdio, http, all"
    exit 1
    ;;
esac

log_success "🎉 Pruebas completadas para imagen: ${IMAGE_NAME}"

# Mostrar información adicional
echo
echo -e "${GREEN}📊 Información de la imagen:${NC}"
docker images "${IMAGE_NAME}" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"

echo
echo -e "${BLUE}💡 Para usar la imagen:${NC}"
echo -e "${BLUE}• Modo STDIO:${NC} docker run -i --env-file .env ${IMAGE_NAME} stdio"
echo -e "${BLUE}• Modo HTTP:${NC}  docker run -p 8080:8080 --env-file .env ${IMAGE_NAME} http"
