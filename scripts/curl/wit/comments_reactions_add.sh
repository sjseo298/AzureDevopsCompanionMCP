#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/comments_reactions.md
# Operación: Create (PUT)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: comments_reactions_add.sh --project <proj> --work-item-id <id> --comment-id <id> --type <like|dislike|heart|hooray|smile|confused> [--help]

Fuente: api_doc/wit_sections/comments_reactions.md

Descripción:
  Agrega una reacción a un comentario.

Parámetros:
  --project       Nombre/ID del proyecto (obligatorio)
  --work-item-id  ID del work item (obligatorio)
  --comment-id    ID del comentario (obligatorio)
  --type          Tipo de reacción (enum) (obligatorio)

Ejemplos:
  scripts/curl/wit/comments_reactions_add.sh --project "Mi Proyecto" --work-item-id 123 --comment-id 456 --type like
USAGE
}

PROJECT=""
WORK_ITEM_ID=""
COMMENT_ID=""
TYPE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --work-item-id) WORK_ITEM_ID="$2"; shift 2;;
    --comment-id) COMMENT_ID="$2"; shift 2;;
    --type) TYPE="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$WORK_ITEM_ID" || -z "$COMMENT_ID" || -z "$TYPE" ]]; then
  echo "Faltan --project, --work-item-id, --comment-id y/o --type (obligatorios)" >&2; usage; exit 2
fi

LOW_TYPE=$(echo "$TYPE" | tr '[:upper:]' '[:lower:]')
case "$LOW_TYPE" in
  like|dislike|heart|hooray|smile|confused) ;;
  *) echo "--type inválido. Valores: like, dislike, heart, hooray, smile, confused" >&2; exit 3;;

esac

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_WI=$(jq -rn --arg s "$WORK_ITEM_ID" '$s|@uri')
ENC_COMMENT=$(jq -rn --arg s "$COMMENT_ID" '$s|@uri')
API_VER_OVERRIDE="7.2-preview.1"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workItems/${ENC_WI}/comments/${ENC_COMMENT}/reactions/${LOW_TYPE}?api-version=${API_VER_OVERRIDE}"

HDR_FILE=$(mktemp)
BODY_FILE=$(mktemp)
HTTP_CODE=$(curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -H "Content-Type: application/json" -X PUT -D "$HDR_FILE" -o "$BODY_FILE" -w "%{http_code}" -d '{}' "$URL")

CT=$(awk 'BEGIN{IGNORECASE=1} /^Content-Type:/{print $2}' "$HDR_FILE" | tr -d '\r')

if [[ -s "$BODY_FILE" ]]; then
  if echo "$CT" | grep -qi 'application/json'; then
    jq . < "$BODY_FILE"
  else
    echo "HTTP ${HTTP_CODE}"
    cat "$BODY_FILE"
  fi
else
  echo "HTTP ${HTTP_CODE}"
fi

rm -f "$HDR_FILE" "$BODY_FILE"
