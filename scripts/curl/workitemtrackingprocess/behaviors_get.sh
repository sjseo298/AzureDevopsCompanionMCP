#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/workitemtrackingprocess/behaviors.md (Get)
# GET /_apis/work/processes/{processId}/behaviors/{behaviorId}?api-version=7.2-preview.1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: behaviors_get.sh --process-id <processId> --behavior-id <behaviorId> [--api-version <ver>]

Fuente: api_doc/workitemtrackingprocess/behaviors.md (Get)

Parámetros:
  --process-id     ID del proceso (obligatorio)
  --behavior-id    ID del comportamiento (obligatorio)
  --api-version    Versión API (default 7.2-preview.1)
  -h|--help        Ayuda

Ejemplos:
  scripts/curl/workitemtrackingprocess/behaviors_get.sh --process-id 01234567-89ab-cdef-0123-456789abcdef --behavior-id abcdef01-2345-6789-abcd-ef0123456789
USAGE
}

PROCESS_ID=""; BEHAVIOR_ID=""; API_VER="7.2-preview.1"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --process-id) PROCESS_ID="$2"; shift 2;;
    --behavior-id) BEHAVIOR_ID="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Arg no reconocido: $1" >&2; usage; exit 1;;
  esac
done
[[ -z "$PROCESS_ID" || -z "$BEHAVIOR_ID" ]] && echo "Falta --process-id y/o --behavior-id" >&2 && usage && exit 2

ENC_PROC=$(jq -rn --arg s "$PROCESS_ID" '$s|@uri')
ENC_BEH=$(jq -rn --arg s "$BEHAVIOR_ID" '$s|@uri')
URL="${DEVOPS_BASE}/_apis/work/processes/${ENC_PROC}/behaviors/${ENC_BEH}?api-version=${API_VER}"
RESP=$(curl_json "$URL") || { echo "Error endpoint" >&2; exit 3; }
echo "$RESP" | jq '{id,name,color,description,rank,custom,overrides,abstract}'>/dev/stdout
