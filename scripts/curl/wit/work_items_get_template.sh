#!/usr/bin/env bash
set -euo pipefail

# Script: Get Work Item Template
# Fuente: api_doc/wit_sections/work_items.md

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_items_get_template.sh --project <project> --id <id> --template <template> [--api-version <ver>] [--raw]
USAGE
}

PROJECT=""; ID=""; TEMPLATE=""; API_VER="7.2-preview"; RAW=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --id) ID="$2"; shift 2;;
    --template) TEMPLATE="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Arg desconocido: $1" >&2; usage; exit 1;;
  esac
done
[[ -z "$PROJECT" || -z "$ID" || -z "$TEMPLATE" ]] && echo "Faltan obligatorios" >&2 && usage && exit 2

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_TEMPLATE=$(jq -rn --arg s "$TEMPLATE" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitems/${ID}/templates/${ENC_TEMPLATE}?api-version=${API_VER}"
RESP=$(curl_json "$URL") || { echo "Error endpoint" >&2; exit 3; }
if echo "$RESP" | jq empty >/dev/null 2>&1; then
  if [[ $RAW -eq 1 ]]; then printf '%s\n' "$RESP"; else echo "$RESP" | jq .; fi
else
  echo "Respuesta no JSON" >&2; printf '%s\n' "$RESP" >&2; exit 4
fi
