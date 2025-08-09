#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_item_type_categories.md
# Operación: Get (GET)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_type_categories_get.sh --project <project> --category <referenceName> [--help]

Fuente: api_doc/wit_sections/work_item_type_categories.md

Descripción:
  Obtiene una categoría de tipos de work item por su referenceName.

Parámetros:
  --project   Nombre o ID del proyecto (obligatorio)
  --category  referenceName de la categoría (obligatorio)
  --help      Muestra esta ayuda.

Ejemplos:
  scripts/curl/wit/work_item_type_categories_get.sh --project Gerencia_Tecnologia --category Microsoft.RequirementCategory
USAGE
}

PROJECT=""
CATEGORY=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --category) CATEGORY="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" ]]; then echo "Falta --project" >&2; usage; exit 2; fi
if [[ -z "$CATEGORY" ]]; then echo "Falta --category" >&2; usage; exit 2; fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_CAT=$(jq -rn --arg s "$CATEGORY" '$s|@uri')
API_VER="7.2-preview"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitemtypecategories/${ENC_CAT}?api-version=${API_VER}"

curl_json "$URL" | jq .
