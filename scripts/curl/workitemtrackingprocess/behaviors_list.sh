#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/workitemtrackingprocess/behaviors.md (List)
# GET /_apis/work/processes/{processId}/behaviors?api-version=7.2-preview.1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: behaviors_list.sh --process-id <processId> [--api-version <ver>] [--jq <filter>]

Fuente: api_doc/workitemtrackingprocess/behaviors.md (List)

Parámetros:
  --process-id    ID del proceso (obligatorio)
  --api-version   Versión API (default 7.2-preview.1)
  --jq            Filtro jq opcional
  -h|--help       Ayuda

Ejemplos:
  scripts/curl/workitemtrackingprocess/behaviors_list.sh --process-id 01234567-89ab-cdef-0123-456789abcdef
USAGE
}

PROCESS_ID=""; API_VER="7.2-preview.1"; JQ_FILTER=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --process-id) PROCESS_ID="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --jq) JQ_FILTER="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Arg no reconocido: $1" >&2; usage; exit 1;;
  esac
done
[[ -z "$PROCESS_ID" ]] && echo "Falta --process-id" >&2 && usage && exit 2

ENC_PROC=$(jq -rn --arg s "$PROCESS_ID" '$s|@uri')
URL="${DEVOPS_BASE}/_apis/work/processes/${ENC_PROC}/behaviors?api-version=${API_VER}"
RESP=$(curl_json "$URL") || { echo "Error endpoint" >&2; exit 3; }
if [[ -n "$JQ_FILTER" ]]; then echo "$RESP" | jq -r "$JQ_FILTER"; else echo "$RESP" | jq '{count, value:[.value[]|{id,name,color,description}]}' ; fi
