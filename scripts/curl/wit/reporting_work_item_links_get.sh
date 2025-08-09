#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/wit_sections/reporting_work_item_links.md
# GET /{organization}/{project}/_apis/wit/reporting/workitemlinks?api-version=7.2-preview.3

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: reporting_work_item_links_get.sh [--project <proyecto>] [--link-types <lista>] [--types <lista>] [--start <ISO8601>] [--continuation <token>]

Fuente: api_doc/wit_sections/reporting_work_item_links.md

Descripción:
  Obtiene vínculos (links) entre work items para reporting. Soporta filtros y paginación por continuationToken.
  Usa api-version 7.2-preview.3.

Parámetros:
  --project        Proyecto (opcional; si se omite, ámbito organización)
  --link-types     Lista separada por comas de tipos de vínculo (ej: System.LinkTypes.Hierarchy,System.LinkTypes.Related)
  --types          Lista separada por comas de tipos de work item (ej: Bug,Task)
  --start          startDateTime (ISO 8601, ej: 2025-01-01T00:00:00Z)
  --continuation   continuationToken devuelto por lote previo
  -h|--help        Muestra esta ayuda

Ejemplos:
  scripts/curl/wit/reporting_work_item_links_get.sh --project "Mi Proyecto"
  scripts/curl/wit/reporting_work_item_links_get.sh --link-types System.LinkTypes.Hierarchy --types Bug,Task
  scripts/curl/wit/reporting_work_item_links_get.sh --project "Mi Proyecto" --start 2025-01-01T00:00:00Z
  scripts/curl/wit/reporting_work_item_links_get.sh --continuation 6281123
USAGE
}

PROJECT=""; LINK_TYPES=""; TYPES=""; START=""; CONT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --link-types) LINK_TYPES="$2"; shift 2;;
    --types) TYPES="$2"; shift 2;;
    --start) START="$2"; shift 2;;
    --continuation) CONT="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

BASE_URL="$DEVOPS_BASE"
PATH_SEG="_apis/wit/reporting/workitemlinks"
if [[ -n "$PROJECT" ]]; then ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri'); PATH_SEG="$ENC_PROJECT/$PATH_SEG"; fi

# Construir query
q=("api-version=7.2-preview.3")
if [[ -n "$LINK_TYPES" ]]; then q+=("linkTypes=$(jq -rn --arg s "$LINK_TYPES" '$s|@uri')"); fi
if [[ -n "$TYPES" ]]; then q+=("types=$(jq -rn --arg s "$TYPES" '$s|@uri')"); fi
if [[ -n "$START" ]]; then q+=("startDateTime=$(jq -rn --arg s "$START" '$s|@uri')"); fi
if [[ -n "$CONT" ]]; then q+=("continuationToken=$(jq -rn --arg s "$CONT" '$s|@uri')"); fi
QUERY=$(IFS='&'; echo "${q[*]}")
URL="$BASE_URL/$PATH_SEG?$QUERY"

RAW=$(curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" "$URL" || true)
if [[ -z "$RAW" ]]; then echo "Respuesta vacía" >&2; exit 4; fi
if ! echo "$RAW" | jq -e . >/dev/null 2>&1; then echo "Respuesta no JSON" >&2; echo "$RAW" >&2; exit 5; fi

echo "$RAW" | jq '{count: (.values|length // 0), sample: (.values[0:5] // []), nextLink, isLastBatch}'
