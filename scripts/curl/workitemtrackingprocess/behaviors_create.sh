#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/workitemtrackingprocess/behaviors.md (Create)
# POST /_apis/work/processes/{processId}/behaviors?api-version=7.2-preview.1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: behaviors_create.sh --process-id <processId> --name <name> [--description <desc>] [--color <hex>] [--api-version <ver>]

Fuente: api_doc/workitemtrackingprocess/behaviors.md (Create)

Parámetros:
  --process-id    ID del proceso (obligatorio)
  --name          Nombre del comportamiento (obligatorio)
  --description   Descripción (opcional)
  --color         Color (hex) opcional, ej. #007acc
  --api-version   Versión API (default 7.2-preview.1)
  -h|--help       Ayuda

Ejemplos:
  scripts/curl/workitemtrackingprocess/behaviors_create.sh --process-id 0123 --name "Defecto" --description "Auto set" --color "#007acc"
USAGE
}

PROCESS_ID=""; NAME=""; DESC=""; COLOR=""; API_VER="7.2-preview.1"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --process-id) PROCESS_ID="$2"; shift 2;;
    --name) NAME="$2"; shift 2;;
    --description) DESC="$2"; shift 2;;
    --color) COLOR="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Arg no reconocido: $1" >&2; usage; exit 1;;
  esac
done
[[ -z "$PROCESS_ID" || -z "$NAME" ]] && echo "Faltan obligatorios" >&2 && usage && exit 2

ENC_PROC=$(jq -rn --arg s "$PROCESS_ID" '$s|@uri')
URL="${DEVOPS_BASE}/_apis/work/processes/${ENC_PROC}/behaviors?api-version=${API_VER}"
BODY=$(jq -n --arg name "$NAME" --arg desc "$DESC" --arg color "$COLOR" '{name:$name, description: ($desc|select(length>0)), color: ($color|select(length>0))} | with_entries(select(.value!=null))')

curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -H "Content-Type: application/json" -X POST -d "$BODY" "$URL" | jq .
