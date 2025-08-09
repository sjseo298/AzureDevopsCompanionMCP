#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/wit_sections/recycle_bin.md (Get Deleted Work Item Shallow References)
# GET /{organization}/{project}/_apis/wit/recyclebin?api-version=7.2-preview

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: recycle_bin_list.sh --project <proyecto> [--skip <n>] [--top <n>] [--deleted-since <RFC3339>]

Fuente: api_doc/wit_sections/recycle_bin.md (Get Deleted Work Item Shallow References)

Descripción:
  Lista referencias superficiales de work items eliminados. Si hay demasiados, use --skip/--top
  o intente limitar por fecha aproximada (--deleted-since) si la organización lo soporta.

Parámetros:
  --project        Proyecto (obligatorio)
  --skip           Omitir n (opcional)
  --top            Limitar a n (opcional)
  --deleted-since  Fecha/hora mínima (RFC3339) tentativa (experimental)

Ejemplos:
  scripts/curl/wit/recycle_bin_list.sh --project "Mi Proyecto" --top 100
  scripts/curl/wit/recycle_bin_list.sh --project "Mi Proyecto" --deleted-since 2025-07-01T00:00:00Z --top 200
USAGE
}

PROJECT=""; SKIP=""; TOP=""; DELETED_SINCE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --skip) SKIP="$2"; shift 2;;
    --top) TOP="$2"; shift 2;;
    --deleted-since) DELETED_SINCE="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" ]]; then echo "--project es obligatorio" >&2; usage; exit 2; fi
if [[ -n "$SKIP" && ! "$SKIP" =~ ^[0-9]+$ ]]; then echo "--skip debe ser numérico" >&2; exit 3; fi
if [[ -n "$TOP" && ! "$TOP" =~ ^[0-9]+$ ]]; then echo "--top debe ser numérico" >&2; exit 4; fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
QS="api-version=7.2-preview"
if [[ -n "$SKIP" ]]; then QS="${QS}&%24skip=${SKIP}"; fi
if [[ -n "$TOP" ]]; then QS="${QS}&%24top=${TOP}"; fi
if [[ -n "$DELETED_SINCE" ]]; then ENC_DS=$(jq -rn --arg s "$DELETED_SINCE" '$s|@uri'); QS="${QS}&deletedTimeMin=${ENC_DS}"; fi
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/recyclebin?${QS}"

RESP=$(curl_json "$URL") || { echo "Error en request" >&2; exit 1; }
if echo "$RESP" | jq -e . >/dev/null 2>&1; then
  MSG=$(echo "$RESP" | jq -r '.message? // empty')
  TYPE=$(echo "$RESP" | jq -r '.typeKey? // empty')
  if [[ "$TYPE" == "WorkItemTrackingQueryResultSizeLimitExceededException" ]]; then
    echo "Demasiados elementos en la papelera. Utiliza --deleted-since, --skip y --top para reducir el volumen." >&2
    exit 1
  fi
  echo "$RESP" | jq .
else
  echo "$RESP"
fi
