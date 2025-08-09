#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_item_types.md
# Operación: Get (GET)
# Nivel: Proyecto + tipo

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_types_get.sh --project <project> --type <tipo> [--api-version <ver>] [--raw] [--help]

Fuente: api_doc/wit_sections/work_item_types.md

Descripción:
  Obtiene un tipo de work item por nombre.

Parámetros:
  --project      Nombre o ID del proyecto (obligatorio)
  --type         Nombre del tipo de work item (obligatorio)
  --api-version  Override de versión API (opcional, default 7.2-preview)
  --raw          Mostrar JSON crudo sin jq
  --help         Muestra esta ayuda.

Ejemplos:
  scripts/curl/wit/work_item_types_get.sh --project Gerencia_Tecnologia --type "User Story"
  scripts/curl/wit/work_item_types_get.sh --project Gerencia_Tecnologia --type Bug
USAGE
}

PROJECT=""
TYPE=""
API_VER="7.2-preview"
RAW=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --type) TYPE="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" ]]; then echo "Falta --project" >&2; usage; exit 2; fi
if [[ -z "$TYPE" ]]; then echo "Falta --type" >&2; usage; exit 2; fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_TYPE=$(jq -rn --arg s "$TYPE" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitemtypes/${ENC_TYPE}?api-version=${API_VER}"

RESP=$(curl_json "$URL") || { echo "Error al invocar endpoint" >&2; exit 3; }
if echo "$RESP" | jq empty >/dev/null 2>&1; then
  if [[ $RAW -eq 1 ]]; then printf '%s\n' "$RESP"; else echo "$RESP" | jq .; fi
else
  echo "Respuesta no es JSON válido" >&2; printf '%s\n' "$RESP" >&2; exit 4
fi
