#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_item_types_field.md
# Operación: Get (GET)
# Nivel: Proyecto + tipo + campo

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_types_field_get.sh --project <project> --type <tipo> --field <referenceName> [--api-version <ver>] [--raw] [--help]

Fuente: api_doc/wit_sections/work_item_types_field.md

Descripción:
  Obtiene el detalle de un campo específico de un tipo de work item.

Parámetros:
  --project      Nombre o ID del proyecto (obligatorio)
  --type         Nombre del tipo de work item (obligatorio)
  --field        referenceName del campo (obligatorio)
  --api-version  Override de versión API (opcional, default 7.2-preview)
  --raw          Mostrar JSON crudo sin jq
  --help         Muestra esta ayuda.

Ejemplos:
  scripts/curl/wit/work_item_types_field_get.sh --project Gerencia_Tecnologia --type "User Story" --field System.Title
USAGE
}

PROJECT=""
TYPE=""
FIELD=""
API_VER="7.2-preview"
RAW=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --type) TYPE="$2"; shift 2;;
    --field) FIELD="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" ]]; then echo "Falta --project" >&2; usage; exit 2; fi
if [[ -z "$TYPE" ]]; then echo "Falta --type" >&2; usage; exit 2; fi
if [[ -z "$FIELD" ]]; then echo "Falta --field" >&2; usage; exit 2; fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_TYPE=$(jq -rn --arg s "$TYPE" '$s|@uri')
ENC_FIELD=$(jq -rn --arg s "$FIELD" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitemtypes/${ENC_TYPE}/fields/${ENC_FIELD}?api-version=${API_VER}"

RESP=$(curl_json "$URL") || { echo "Error al invocar endpoint" >&2; exit 3; }
if echo "$RESP" | jq empty >/dev/null 2>&1; then
  if [[ $RAW -eq 1 ]]; then printf '%s\n' "$RESP"; else echo "$RESP" | jq .; fi
else
  echo "Respuesta no es JSON válido" >&2; printf '%s\n' "$RESP" >&2; exit 4
fi
