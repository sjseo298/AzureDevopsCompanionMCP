#!/usr/bin/env bash
set -euo pipefail

# Script de ayuda para trabajar con las imágenes Docker MCP Azure DevOps
# Proporciona un menú principal para todas las operaciones

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
LAST_CHOICE_FILE="${TMPDIR:-/tmp}/mcp-azure-devops-helper-last-choice"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

show_banner() {
  echo -e "${CYAN}${BOLD}"
  echo "╔══════════════════════════════════════════════════════════════╗"
  echo "║                   🚀 MCP Azure DevOps Helper                 ║"
  echo "║                   Docker Management Tool                     ║"
  echo "╚══════════════════════════════════════════════════════════════╝"
  echo -e "${NC}"
}

show_images_info() {
  echo -e "${YELLOW}📊 Imágenes Docker disponibles:${NC}"
  local images=$(docker images --format "table {{.Repository}}:{{.Tag}}\t{{.Size}}\t{{.CreatedSince}}" | grep mcp-azure-devops | grep -v REPOSITORY || true)
  
  if [[ -z "$images" ]]; then
    echo -e "${RED}   No hay imágenes MCP Azure DevOps construidas${NC}"
    echo -e "${BLUE}   Usa la opción 1 para construir una imagen${NC}"
  else
    echo "$images" | while read -r line; do
      echo -e "   ${GREEN}•${NC} $line"
    done
  fi
  echo ""
}

show_menu() {
  echo -e "${BOLD}🎯 ¿Qué deseas hacer?${NC}"
  echo ""
  echo -e "${GREEN}1)${NC} 🔨 Construir imagen Docker (Interactivo)"
  echo -e "${GREEN}2)${NC} 🧪 Probar imagen Docker (Interactivo)"
  echo -e "${GREEN}3)${NC} 📋 Listar todas las imágenes"
  echo -e "${GREEN}4)${NC} 🧹 Limpiar imágenes MCP"
  echo -e "${GREEN}5)${NC} 📖 Ver documentación de uso"
  echo -e "${GREEN}6)${NC} 🏃 Ejecutar imagen rápidamente"
  if last_choice_label=$(get_last_choice_label); then
    echo -e "${GREEN}7)${NC} 🔁 Última opción (${last_choice_label})"
  else
    echo -e "${GREEN}7)${NC} 🔁 Última opción (no disponible)"
  fi
  echo -e "${GREEN}0)${NC} ❌ Salir"
  echo ""
}

get_choice_label() {
  case "${1:-}" in
    1) echo "Construir imagen Docker" ;;
    2) echo "Probar imagen Docker" ;;
    3) echo "Listar todas las imágenes" ;;
    4) echo "Limpiar imágenes MCP" ;;
    5) echo "Ver documentación de uso" ;;
    6) echo "Ejecutar imagen rápidamente" ;;
    build:last) echo "Construir imagen Docker (última configuración)" ;;
    test:last) echo "Probar imagen Docker (última configuración)" ;;
    quick:*)
      local spec="${1#quick:}"
      local image="${spec%:*}"
      local mode="${spec##*:}"
      echo "Ejecutar ${image} en modo ${mode}"
      ;;
    *) return 1 ;;
  esac
}

get_last_choice() {
  if [[ ! -f "${LAST_CHOICE_FILE}" ]]; then
    return 1
  fi

  local choice
  choice="$(<"${LAST_CHOICE_FILE}")"
  if get_choice_label "${choice}" >/dev/null; then
    echo "${choice}"
    return 0
  fi

  return 1
}

get_last_choice_label() {
  local choice
  choice="$(get_last_choice)" || return 1
  get_choice_label "${choice}"
}

save_last_choice() {
  local choice="${1:-}"
  if get_choice_label "${choice}" >/dev/null; then
    printf '%s\n' "${choice}" > "${LAST_CHOICE_FILE}"
  fi
}

list_all_images() {
  echo -e "${BLUE}📋 Listado completo de imágenes:${NC}"
  docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedSince}}\t{{.ID}}" | grep -E "(REPOSITORY|mcp-azure-devops)" || {
    echo -e "${RED}No se encontraron imágenes MCP Azure DevOps${NC}"
  }
  echo ""
}

cleanup_images() {
  local images=($(docker images --format "{{.Repository}}:{{.Tag}}" | grep mcp-azure-devops || true))
  
  if [[ ${#images[@]} -eq 0 ]]; then
    echo -e "${YELLOW}No hay imágenes MCP para limpiar${NC}"
    return
  fi
  
  echo -e "${YELLOW}🧹 Imágenes MCP encontradas:${NC}"
  local i=1
  for image in "${images[@]}"; do
    local size=$(docker images --format "{{.Size}}" "$image")
    echo "   $i) $image ($size)"
    ((i++))
  done
  echo ""
  
  read -p "¿Eliminar TODAS las imágenes MCP? (y/N): " confirm
  if [[ $confirm =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}Eliminando imágenes...${NC}"
    for image in "${images[@]}"; do
      local containers=()
      mapfile -t containers < <(docker ps -a --filter "ancestor=$image" --format "{{.ID}} {{.Names}}" || true)
      if [[ ${#containers[@]} -gt 0 ]]; then
        echo -e "${YELLOW}   La imagen $image está en uso por ${#containers[@]} contenedor(es). Eliminando contenedores...${NC}"
        local line id name
        for line in "${containers[@]}"; do
          id="${line%% *}"
          name="${line#* }"
          docker rm -f "$id" >/dev/null || true
          echo -e "${GREEN}   ✅ Contenedor eliminado: $name ($id)${NC}"
        done
      fi
      if docker rmi "$image"; then
        echo -e "${GREEN}✅ Eliminada: $image${NC}"
      else
        echo -e "${YELLOW}⚠️  No se pudo eliminar: $image${NC}"
      fi
    done
    echo -e "${GREEN}🎉 Limpieza completada${NC}"
  else
    echo -e "${YELLOW}Limpieza cancelada${NC}"
  fi
}

show_documentation() {
  echo -e "${BLUE}📖 Documentación de uso:${NC}"
  echo ""
  echo -e "${BOLD}🏷️  Tipos de imágenes disponibles:${NC}"
  echo "   • mcp-azure-devops:latest - Versión estándar (473MB)"
  echo "   • mcp-azure-devops:slim   - Versión optimizada (358MB)"
  echo "   • mcp-azure-devops:ultra  - Versión ultra-optimizada (191MB)"
  echo ""
  echo -e "${BOLD}🚀 Comandos de ejecución:${NC}"
  echo "   # Modo STDIO (para clientes MCP):"
  echo "   docker run -i --env-file .env mcp-azure-devops:ultra stdio"
  echo ""
  echo "   # Modo HTTP (para acceso web):"
  echo "   docker run -p 8080:8080 --env-file .env mcp-azure-devops:ultra http"
  echo ""
  echo -e "${BOLD}📄 Archivo .env requerido:${NC}"
  echo "   AZURE_DEVOPS_ORGANIZATION=tu-organizacion"
  echo "   AZURE_DEVOPS_PAT=tu-personal-access-token"
  echo ""
  echo -e "${BOLD}🔧 Configuración MCP (mcp.json):${NC}"
  echo '   {'
  echo '     "servers": {'
  echo '       "azure-devops-mcp": {'
  echo '         "command": "docker",'
  echo '         "args": ["run", "--rm", "-i", "--env-file", ".env", "mcp-azure-devops:ultra", "stdio"]'
  echo '       }'
  echo '     }'
  echo '   }'
  echo ""
}

quick_run() {
  local images=($(docker images --format "{{.Repository}}:{{.Tag}}" | grep mcp-azure-devops || true))
  
  if [[ ${#images[@]} -eq 0 ]]; then
    echo -e "${RED}No hay imágenes MCP disponibles${NC}"
    echo -e "${BLUE}Construye una imagen primero (opción 1)${NC}"
    return
  fi
  
  echo -e "${YELLOW}🏃 Ejecución rápida - Selecciona imagen:${NC}"
  local i=1
  for image in "${images[@]}"; do
    local size=$(docker images --format "{{.Size}}" "$image")
    echo "$i) $image ($size)"
    ((i++))
  done
  echo ""
  
  read -p "Selecciona imagen (1-${#images[@]}): " image_choice
  if [[ "$image_choice" =~ ^[0-9]+$ ]] && [[ "$image_choice" -ge 1 ]] && [[ "$image_choice" -le ${#images[@]} ]]; then
    local selected_image="${images[$((image_choice-1))]}"
    
    echo ""
    echo "1) STDIO (para clientes MCP)"
    echo "2) HTTP (para acceso web)"
    read -p "Modo de ejecución (1-2): " mode_choice
    
    case $mode_choice in
      1)
        save_last_choice "quick:${selected_image}:stdio"
        echo -e "${GREEN}🚀 Ejecutando $selected_image en modo STDIO...${NC}"
        echo -e "${YELLOW}Presiona Ctrl+C para salir${NC}"
        docker run -i --env-file .env "$selected_image" stdio
        ;;
      2)
        save_last_choice "quick:${selected_image}:http"
        echo -e "${GREEN}🚀 Ejecutando $selected_image en modo HTTP en puerto 8080...${NC}"
        echo -e "${YELLOW}Accede a http://localhost:8080 - Presiona Ctrl+C para salir${NC}"
        docker run -p 8080:8080 --env-file .env "$selected_image" http
        ;;
      *)
        echo -e "${RED}Opción inválida${NC}"
        ;;
    esac
  else
    echo -e "${RED}Opción inválida${NC}"
  fi
}

run_quick_spec() {
  local spec="$1"
  local payload="${spec#quick:}"
  local image="${payload%:*}"
  local mode="${payload##*:}"

  if [[ -z "${image}" || -z "${mode}" || "${image}" == "${mode}" ]]; then
    echo -e "${RED}Última ejecución rápida inválida: ${spec}${NC}"
    return 1
  fi

  if ! docker images "${image}" --format "{{.Repository}}:{{.Tag}}" | grep -q .; then
    echo -e "${RED}Imagen no encontrada: ${image}${NC}"
    return 1
  fi

  save_last_choice "${spec}"
  case "${mode}" in
    stdio)
      echo -e "${GREEN}🚀 Ejecutando ${image} en modo STDIO...${NC}"
      echo -e "${YELLOW}Presiona Ctrl+C para salir${NC}"
      docker run -i --env-file .env "${image}" stdio
      ;;
    http)
      echo -e "${GREEN}🚀 Ejecutando ${image} en modo HTTP en puerto 8080...${NC}"
      echo -e "${YELLOW}Accede a http://localhost:8080 - Presiona Ctrl+C para salir${NC}"
      docker run -p 8080:8080 --env-file .env "${image}" http
      ;;
    *)
      echo -e "${RED}Modo de ejecución rápida inválido: ${mode}${NC}"
      return 1
      ;;
  esac
}

run_choice() {
  local choice="$1"

  case $choice in
    1)
      save_last_choice "build:last"
      echo -e "${BLUE}🔨 Iniciando construcción interactiva...${NC}"
      "$SCRIPT_DIR/build-docker-image.sh"
      echo ""
      show_images_info
      ;;
    2)
      save_last_choice "test:last"
      echo -e "${BLUE}🧪 Iniciando test interactivo...${NC}"
      "$SCRIPT_DIR/test-docker-image.sh"
      echo ""
      ;;
    3)
      save_last_choice "$choice"
      list_all_images
      ;;
    4)
      save_last_choice "$choice"
      cleanup_images
      echo ""
      show_images_info
      ;;
    5)
      save_last_choice "$choice"
      show_documentation
      ;;
    6)
      quick_run
      echo ""
      ;;
    build:last)
      save_last_choice "$choice"
      echo -e "${BLUE}🔨 Repitiendo última construcción guardada...${NC}"
      "$SCRIPT_DIR/build-docker-image.sh" --last
      echo ""
      show_images_info
      ;;
    test:last)
      save_last_choice "$choice"
      echo -e "${BLUE}🧪 Repitiendo último test guardado...${NC}"
      "$SCRIPT_DIR/test-docker-image.sh" --last
      echo ""
      ;;
    quick:*)
      run_quick_spec "$choice"
      echo ""
      ;;
    7)
      local last_choice
      if ! last_choice="$(get_last_choice)"; then
        echo -e "${YELLOW}No hay una última opción guardada todavía.${NC}"
        echo ""
        return 0
      fi
      echo -e "${CYAN}🔁 Ejecutando última opción: $(get_choice_label "${last_choice}")${NC}"
      echo ""
      run_choice "${last_choice}"
      ;;
    0)
      echo -e "${GREEN}👋 ¡Hasta luego!${NC}"
      exit 0
      ;;
    *)
      echo -e "${RED}❌ Opción inválida. Por favor selecciona 0-7.${NC}"
      echo ""
      ;;
  esac
}

main() {
  cd "$PROJECT_ROOT"
  
  show_banner
  show_images_info
  
  while true; do
    show_menu
    read -p "Selecciona una opción (0-7): " choice
    echo ""

    run_choice "$choice"
    
    read -p "Presiona Enter para continuar..." -r
    clear
    show_banner
    show_images_info
  done
}

# Verificar que Docker esté funcionando
if ! docker --version >/dev/null 2>&1; then
  echo -e "${RED}❌ Docker no está disponible o no está funcionando${NC}"
  exit 1
fi

# Ejecutar si se llama directamente
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main "$@"
fi
