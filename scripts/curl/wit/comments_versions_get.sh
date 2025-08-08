#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/comments_versions.md
# Operación: Get (GET)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: comments_versions_get.sh --project <proj> --work-item-id <id> --comment-id <id> --version <n> [--help]

Fuente: api_doc/wit_sections/comments_versions.md

Descripción:
  Obtiene una versión específica de un comentario.

Parámetros:
  --project       Nombre/ID del proyecto (obligatorio)
  --work-item-id  ID del work item (obligatorio)
  --comment-id    ID del comentario (obligatorio)
  --version       Número de versión (obligatorio)

Ejemplos:
  scripts/curl/wit/comments_versions_get.sh --project "Mi Proyecto" --work-item-id 123 --comment-id 456 --version 2
USAGE
}

PROJECT=""
WORK_ITEM_ID=""
COMMENT_ID=""
VERSION=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --work-item-id) WORK_ITEM_ID="$2"; shift 2;;
    --comment-id) COMMENT_ID="$2"; shift 2;;
    --version) VERSION="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$WORK_ITEM_ID" || -z "$COMMENT_ID" || -z "$VERSION" ]]; then
  echo "Faltan --project, --work-item-id, --comment-id y/o --version (obligatorios)" >&2; usage; exit 2
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_WI=$(jq -rn --arg s "$WORK_ITEM_ID" '$s|@uri')
ENC_COMMENT=$(jq -rn --arg s "$COMMENT_ID" '$s|@uri')
ENC_VER=$(jq -rn --arg s "$VERSION" '$s|@uri')
API_VER_OVERRIDE="7.2-preview.3"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workItems/${ENC_WI}/comments/${ENC_COMMENT}/versions/${ENC_VER}?api-version=${API_VER_OVERRIDE}"

curl_json "${URL}" | jq .
