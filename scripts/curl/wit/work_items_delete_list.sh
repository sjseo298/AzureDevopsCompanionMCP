#!/usr/bin/env bash
set -euo pipefail

# Script: Delete multiple work items (Recycle Bin por defecto, usar --destroy para permanente)
# Fuente: api_doc/wit_sections/work_items.md

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_items_delete_list.sh --project <project> --ids <listaIds> [--destroy] [--api-version <ver>] [--raw]
USAGE
}

PROJECT=""; IDS=""; DESTROY=0; API_VER="7.2-preview"; RAW=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --ids) IDS="$2"; shift 2;;
    --destroy) DESTROY=1; shift;;
    --api-version) API_VER="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Arg desconocido: $1" >&2; usage; exit 1;;
  esac
done
[[ -z "$PROJECT" || -z "$IDS" ]] && echo "Faltan obligatorios" >&2 && usage && exit 2

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_IDS=$(jq -rn --arg s "$IDS" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitems?ids=${ENC_IDS}&api-version=${API_VER}"
[[ $DESTROY -eq 1 ]] && URL+="&destroy=true"
RESP=$(curl -sS -u ":${AZURE_DEVOPS_PAT}" -X DELETE -H "Accept: application/json" "$URL") || { echo "Error endpoint" >&2; exit 3; }
if [[ $RAW -eq 1 ]]; then printf '%s\n' "$RESP"; else echo "$RESP" | jq .; fi
