#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/comments.md
# Operación: Add Comment (POST)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: comments_add.sh --project <proj> --work-item-id <id> --text <comentario> [--help]

Fuente: api_doc/wit_sections/comments.md

Descripción:
  Agrega un comentario a un work item.

Parámetros:
  --project       Nombre/ID del proyecto (obligatorio)
  --work-item-id  ID del work item (obligatorio)
  --text          Texto del comentario (obligatorio)

Ejemplos:
  scripts/curl/wit/comments_add.sh --project "Mi Proyecto" --work-item-id 123 --text "Mi comentario"
USAGE
}

PROJECT=""
WORK_ITEM_ID=""
TEXT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --work-item-id) WORK_ITEM_ID="$2"; shift 2;;
    --text) TEXT="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$WORK_ITEM_ID" || -z "$TEXT" ]]; then
  echo "Faltan --project, --work-item-id y/o --text (obligatorios)" >&2; usage; exit 2
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_WI=$(jq -rn --arg s "$WORK_ITEM_ID" '$s|@uri')
API_VER_OVERRIDE="7.0-preview.3"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workItems/${ENC_WI}/comments?api-version=${API_VER_OVERRIDE}"

BODY=$(jq -n --arg t "$TEXT" '{text: $t}')

curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -H "Content-Type: application/json" -X POST \
  -d "$BODY" "$URL" | jq .
