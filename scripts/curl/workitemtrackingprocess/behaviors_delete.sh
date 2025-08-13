#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/workitemtrackingprocess/behaviors.md (Delete)
# DELETE /_apis/work/processes/{processId}/behaviors/{behaviorId}?api-version=7.2-preview.1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: behaviors_delete.sh --process-id <processId> --behavior-id <behaviorId> [--api-version <ver>]

Fuente: api_doc/workitemtrackingprocess/behaviors.md (Delete)

Parámetros:
  --process-id     ID del proceso (obligatorio)
  --behavior-id    ID del comportamiento (obligatorio)
  --api-version    Versión API (default 7.2-preview.1)
  -h|--help        Ayuda

Ejemplos:
  scripts/curl/workitemtrackingprocess/behaviors_delete.sh --process-id 0123 --behavior-id abcd
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
[[ -z "$PROCESS_ID" || -z "$BEHAVIOR_ID" ]] && echo "Faltan obligatorios" >&2 && usage && exit 2

ENC_PROC=$(jq -rn --arg s "$PROCESS_ID" '$s|@uri')
ENC_BEH=$(jq -rn --arg s "$BEHAVIOR_ID" '$s|@uri')
URL="${DEVOPS_BASE}/_apis/work/processes/${ENC_PROC}/behaviors/${ENC_BEH}?api-version=${API_VER}"

CODE=$(curl -sS -o /dev/null -w '%{http_code}' -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -X DELETE "$URL")
if [[ "$CODE" == "200" || "$CODE" == "204" ]]; then echo '{"status":"deleted"}' | jq .; else echo "HTTP $CODE" >&2; exit 4; fi
