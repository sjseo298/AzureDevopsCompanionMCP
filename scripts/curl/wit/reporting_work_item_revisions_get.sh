#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/wit_sections/reporting_work_item_revisions.md (GET)
# GET /{organization}/{project}/_apis/wit/reporting/workitemrevisions?api-version=7.2-preview.2

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: reporting_work_item_revisions_get.sh [--project <proyecto>] [--fields <lista>] [--types <lista>] [--start <ISO8601>] \
       [--continuation <token>] [--expand <none|fields>] [--max <num>] [--include-deleted] [--include-identity] \
       [--include-latest-only] [--include-tag-ref] [--include-discussion-only]

Fuente: api_doc/wit_sections/reporting_work_item_revisions.md (GET)

Descripción:
  Obtiene revisiones de work items para reporting con filtros y paginación (GET). api-version 7.2-preview.2.

Parámetros:
  --project                 Proyecto (opcional)
  --fields                  Lista separada por comas de campos (ej: System.Title,System.State)
  --types                   Lista separada por comas de tipos de work item (ej: Bug,Task)
  --start                   startDateTime (ISO8601)
  --continuation            continuationToken devuelto previamente
  --expand                  none|fields
  --max                     $maxPageSize (entero)
  --include-deleted         Incluir work items eliminados
  --include-identity        includeIdentityRef=true
  --include-latest-only     includeLatestOnly=true
  --include-tag-ref         includeTagRef=true
  --include-discussion-only includeDiscussionChangesOnly=true
  -h|--help                 Muestra esta ayuda

Ejemplos:
  scripts/curl/wit/reporting_work_item_revisions_get.sh --project "Mi Proyecto" --fields System.Title,System.State --types Bug,Task
  scripts/curl/wit/reporting_work_item_revisions_get.sh --expand fields --max 500
USAGE
}

PROJECT=""; FIELDS=""; TYPES=""; START=""; CONT=""; EXPAND=""; MAX=""; INC_DELETED=false; INC_ID=false; INC_LATEST=false; INC_TAG=false; INC_DISC=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --fields) FIELDS="$2"; shift 2;;
    --types) TYPES="$2"; shift 2;;
    --start) START="$2"; shift 2;;
    --continuation) CONT="$2"; shift 2;;
    --expand) EXPAND="$2"; shift 2;;
    --max) MAX="$2"; shift 2;;
    --include-deleted) INC_DELETED=true; shift;;
    --include-identity) INC_ID=true; shift;;
    --include-latest-only) INC_LATEST=true; shift;;
    --include-tag-ref) INC_TAG=true; shift;;
    --include-discussion-only) INC_DISC=true; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

BASE_URL="$DEVOPS_BASE"; PATH_SEG="_apis/wit/reporting/workitemrevisions"
if [[ -n "$PROJECT" ]]; then ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri'); PATH_SEG="$ENC_PROJECT/$PATH_SEG"; fi
q=("api-version=7.2-preview.2")
enc(){ jq -rn --arg s "$1" '$s|@uri'; }
if [[ -n "$FIELDS" ]]; then q+=("fields=$(enc "$FIELDS")"); fi
if [[ -n "$TYPES" ]]; then q+=("types=$(enc "$TYPES")"); fi
if [[ -n "$START" ]]; then q+=("startDateTime=$(enc "$START")"); fi
if [[ -n "$CONT" ]]; then q+=("continuationToken=$(enc "$CONT")"); fi
if [[ -n "$EXPAND" ]]; then q+=("$expand=$(enc "$EXPAND")"); fi
if [[ -n "$MAX" ]]; then q+=("$maxPageSize=$(enc "$MAX")"); fi
$INC_DELETED && q+=("includeDeleted=true") || true
$INC_ID && q+=("includeIdentityRef=true") || true
$INC_LATEST && q+=("includeLatestOnly=true") || true
$INC_TAG && q+=("includeTagRef=true") || true
$INC_DISC && q+=("includeDiscussionChangesOnly=true") || true
QUERY=$(IFS='&'; echo "${q[*]}")
URL="$BASE_URL/$PATH_SEG?$QUERY"
RAW=$(curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" "$URL" || true)
if [[ -z "$RAW" ]]; then echo "Respuesta vacía" >&2; exit 4; fi
if ! echo "$RAW" | jq -e . >/dev/null 2>&1; then echo "Respuesta no JSON" >&2; echo "$RAW" >&2; exit 5; fi
echo "$RAW" | jq '{count: (.values|length // 0), first: (.values[0] // {} | {id, rev}), continuationToken, isLastBatch}'
