#!/usr/bin/env bash
set -euo pipefail

# Script de ayuda para trabajar con las imágenes Docker MCP Azure DevOps
# Proporciona un menú principal para todas las operaciones

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

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
  echo -e "${GREEN}0)${NC} ❌ Salir"
  echo ""
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
      docker rmi "$image" 2>/dev/null || true
      echo -e "${GREEN}✅ Eliminada: $image${NC}"
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
        echo -e "${GREEN}🚀 Ejecutando $selected_image en modo STDIO...${NC}"
        echo -e "${YELLOW}Presiona Ctrl+C para salir${NC}"
        docker run -i --env-file .env "$selected_image" stdio
        ;;
      2)
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

main() {
  cd "$PROJECT_ROOT"
  
  show_banner
  show_images_info
  
  while true; do
    show_menu
    read -p "Selecciona una opción (0-6): " choice
    echo ""
    
    case $choice in
      1)
        echo -e "${BLUE}🔨 Iniciando construcción interactiva...${NC}"
        "$SCRIPT_DIR/build-docker-image.sh"
        echo ""
        show_images_info
        ;;
      2)
        echo -e "${BLUE}🧪 Iniciando test interactivo...${NC}"
        "$SCRIPT_DIR/test-docker-image.sh"
        echo ""
        ;;
      3)
        list_all_images
        ;;
      4)
        cleanup_images
        echo ""
        show_images_info
        ;;
      5)
        show_documentation
        ;;
      6)
        quick_run
        echo ""
        ;;
      0)
        echo -e "${GREEN}👋 ¡Hasta luego!${NC}"
        exit 0
        ;;
      *)
        echo -e "${RED}❌ Opción inválida. Por favor selecciona 0-6.${NC}"
        echo ""
        ;;
    esac
    
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
