#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/comments.md
# Operación: Update (PATCH)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: comments_update.sh --project <proj> --work-item-id <id> --comment-id <id> --text <nuevoTexto> [--help]

Fuente: api_doc/wit_sections/comments.md

Descripción:
  Actualiza el texto de un comentario en un work item.

Parámetros:
  --project       Nombre/ID del proyecto (obligatorio)
  --work-item-id  ID del work item (obligatorio)
  --comment-id    ID del comentario (obligatorio)
  --text          Nuevo texto del comentario (obligatorio)

Ejemplos:
  scripts/curl/wit/comments_update.sh --project "Mi Proyecto" --work-item-id 123 --comment-id 456 --text "Texto actualizado"
USAGE
}

PROJECT=""
WORK_ITEM_ID=""
COMMENT_ID=""
TEXT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --work-item-id) WORK_ITEM_ID="$2"; shift 2;;
    --comment-id) COMMENT_ID="$2"; shift 2;;
    --text) TEXT="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$WORK_ITEM_ID" || -z "$COMMENT_ID" || -z "$TEXT" ]]; then
  echo "Faltan --project, --work-item-id, --comment-id y/o --text (obligatorios)" >&2; usage; exit 2
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_WI=$(jq -rn --arg s "$WORK_ITEM_ID" '$s|@uri')
ENC_COMMENT=$(jq -rn --arg s "$COMMENT_ID" '$s|@uri')
API_VER_OVERRIDE="7.0-preview.3"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workItems/${ENC_WI}/comments/${ENC_COMMENT}?api-version=${API_VER_OVERRIDE}"

BODY=$(jq -n --arg t "$TEXT" '{text: $t}')

curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -H "Content-Type: application/json" -X PATCH \
  -d "$BODY" "$URL" | jq .
