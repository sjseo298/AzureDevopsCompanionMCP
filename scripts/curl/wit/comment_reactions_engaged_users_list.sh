#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/comment_reactions_engaged_users.md
# Operación: List
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: comment_reactions_engaged_users_list.sh --project <proj> --work-item-id <id> --comment-id <id> [--help]

Fuente: api_doc/wit_sections/comment_reactions_engaged_users.md

Descripción:
  Lista los usuarios que han reaccionado a un comentario de un work item.

Parámetros:
  --project       Nombre/ID del proyecto (obligatorio)
  --work-item-id  ID del work item (obligatorio)
  --comment-id    ID del comentario (obligatorio)

Ejemplos:
  scripts/curl/wit/comment_reactions_engaged_users_list.sh --project "Mi Proyecto" --work-item-id 123 --comment-id 456
USAGE
}

PROJECT=""
WORK_ITEM_ID=""
COMMENT_ID=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --work-item-id) WORK_ITEM_ID="$2"; shift 2;;
    --comment-id) COMMENT_ID="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$WORK_ITEM_ID" || -z "$COMMENT_ID" ]]; then
  echo "Faltan --project, --work-item-id y/o --comment-id (obligatorios)" >&2; usage; exit 2
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_WI=$(jq -rn --arg s "$WORK_ITEM_ID" '$s|@uri')
ENC_COMMENT=$(jq -rn --arg s "$COMMENT_ID" '$s|@uri')
API_VER_OVERRIDE="7.2-preview"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workItems/${ENC_WI}/comments/${ENC_COMMENT}/reactions/users?api-version=${API_VER_OVERRIDE}"

curl_json "${URL}" | jq .
