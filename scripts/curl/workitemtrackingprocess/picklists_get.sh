#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/workitemtrackingprocess/lists.md
# Operación: Get Picklist
# Nivel: Organización (process)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: picklists_get.sh --id <picklistId> [--api-version <ver>] [--raw]

Fuente: api_doc/workitemtrackingprocess/lists.md

Parámetros:
  --id            GUID del picklist (obligatorio)
  --api-version   Versión API (default 7.2-preview.1)
  --raw           JSON crudo
  -h|--help       Ayuda

Ejemplos:
  scripts/curl/workitemtrackingprocess/picklists_get.sh --id 01234567-89ab-cdef-0123-456789abcdef
USAGE
}

PICK_ID=""; API_VER="7.2-preview.1"; RAW=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --id) PICK_ID="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Arg no reconocido: $1" >&2; usage; exit 1;;
  esac
done
[[ -z "$PICK_ID" ]] && echo "Falta --id" >&2 && usage && exit 2

ENC_ID=$(jq -rn --arg s "$PICK_ID" '$s|@uri')
URL="${DEVOPS_BASE}/_apis/work/processes/lists/${ENC_ID}?api-version=${API_VER}"
RESP=$(curl_json "$URL") || { echo "Error endpoint" >&2; exit 3; }
if ! echo "$RESP" | jq empty >/dev/null 2>&1; then echo "Respuesta no JSON" >&2; printf '%s\n' "$RESP" >&2; exit 4; fi
if [[ $RAW -eq 1 ]]; then printf '%s\n' "$RESP"; else echo "$RESP" | jq '{id,name,type,itemCount:(.items|length),items}' ; fi
