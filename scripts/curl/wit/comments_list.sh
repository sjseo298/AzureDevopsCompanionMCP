#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/comments.md
# Operación: List (GET)
# Nota: La doc de Comments principal no lista explicitamente List; usamos el endpoint de versiones/reacciones aparte. Aquí listamos comentarios simples del WI.
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: comments_list.sh --project <proj> --work-item-id <id> [--help]

Fuente: api_doc/wit_sections/comments.md

Descripción:
  Lista comentarios de un work item (paginación básica no incluida en este script). Útil para obtener commentId.

Parámetros:
  --project       Nombre/ID del proyecto (obligatorio)
  --work-item-id  ID del work item (obligatorio)

Ejemplos:
  scripts/curl/wit/comments_list.sh --project "Mi Proyecto" --work-item-id 123
USAGE
}

PROJECT=""
WORK_ITEM_ID=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --work-item-id) WORK_ITEM_ID="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$WORK_ITEM_ID" ]]; then
  echo "Faltan --project y/o --work-item-id (obligatorios)" >&2; usage; exit 2
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_WI=$(jq -rn --arg s "$WORK_ITEM_ID" '$s|@uri')
# Según doc Comments Versions, la list de comentarios usa 7.0-preview.3
API_VER_OVERRIDE="7.0-preview.3"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workItems/${ENC_WI}/comments?api-version=${API_VER_OVERRIDE}"

curl_json "${URL}" | jq .
