#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/wit_sections/reporting_work_item_revisions.md (POST)
# POST /{organization}/{project}/_apis/wit/reporting/workitemrevisions?api-version=7.2-preview.2

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: reporting_work_item_revisions_post.sh [--project <proyecto>] [--fields <lista>] [--types <lista>] \
       [--include-deleted] [--include-identity] [--include-latest-only] [--include-tag-ref]

Fuente: api_doc/wit_sections/reporting_work_item_revisions.md (POST)

Descripción:
  Obtiene revisiones (POST) enviando filtros en el cuerpo (útil para listas largas de campos). api-version 7.2-preview.2.

Parámetros:
  --project             Proyecto (opcional)
  --fields              Lista separada por comas de campos
  --types               Lista separada por comas de tipos
  --include-deleted     includeDeleted=true
  --include-identity    includeIdentityRef=true
  --include-latest-only includeLatestOnly=true
  --include-tag-ref     includeTagRef=true
  -h|--help             Muestra esta ayuda

Ejemplos:
  scripts/curl/wit/reporting_work_item_revisions_post.sh --fields System.Title,System.State --types Bug,Task
  scripts/curl/wit/reporting_work_item_revisions_post.sh --include-identity --include-deleted
USAGE
}

PROJECT=""; FIELDS=""; TYPES=""; INC_DELETED=false; INC_ID=false; INC_LATEST=false; INC_TAG=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --fields) FIELDS="$2"; shift 2;;
    --types) TYPES="$2"; shift 2;;
    --include-deleted) INC_DELETED=true; shift;;
    --include-identity) INC_ID=true; shift;;
    --include-latest-only) INC_LATEST=true; shift;;
    --include-tag-ref) INC_TAG=true; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

BASE_URL="$DEVOPS_BASE"; PATH_SEG="_apis/wit/reporting/workitemrevisions"
if [[ -n "$PROJECT" ]]; then ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri'); PATH_SEG="$ENC_PROJECT/$PATH_SEG"; fi
URL="$BASE_URL/$PATH_SEG?api-version=7.2-preview.2"

body='{}'
if [[ -n "$FIELDS" ]]; then body=$(echo "$body" | jq --arg v "$FIELDS" '. + {fields: ($v|split(","))}'); fi
if [[ -n "$TYPES" ]]; then body=$(echo "$body" | jq --arg v "$TYPES" '. + {types: ($v|split(","))}'); fi
$INC_DELETED && body=$(echo "$body" | jq '. + {includeDeleted:true}') || true
$INC_ID && body=$(echo "$body" | jq '. + {includeIdentityRef:true}') || true
$INC_LATEST && body=$(echo "$body" | jq '. + {includeLatestOnly:true}') || true
$INC_TAG && body=$(echo "$body" | jq '. + {includeTagRef:true}') || true

RAW=$(curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -H "Content-Type: application/json" -X POST -d "$body" "$URL" || true)
if [[ -z "$RAW" ]]; then echo "Respuesta vacía" >&2; exit 4; fi
if ! echo "$RAW" | jq -e . >/dev/null 2>&1; then echo "Respuesta no JSON" >&2; echo "$RAW" >&2; exit 5; fi
echo "$RAW" | jq '{count: (.values|length // 0), first: (.values[0] // {} | {id, rev}), continuationToken, isLastBatch}'
