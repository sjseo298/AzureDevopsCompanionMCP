#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_item_type_categories.md
# Operación: List (GET)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_type_categories_list.sh --project <project> [--help]

Fuente: api_doc/wit_sections/work_item_type_categories.md

Descripción:
  Lista las categorías de tipos de work item en el proyecto.

Parámetros:
  --project  Nombre o ID del proyecto (obligatorio)
  --help     Muestra esta ayuda.

Ejemplos:
  scripts/curl/wit/work_item_type_categories_list.sh --project MiProyecto
  scripts/curl/wit/work_item_type_categories_list.sh --project Gerencia_Tecnologia
USAGE
}

PROJECT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" ]]; then echo "Falta --project" >&2; usage; exit 2; fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
API_VER="7.2-preview"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitemtypecategories?api-version=${API_VER}"

curl_json "$URL" | jq .
