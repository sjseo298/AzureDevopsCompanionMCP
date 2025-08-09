#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/wit_sections/recycle_bin.md (Get Deleted Work Items batch)
# GET /{organization}/{project}/_apis/wit/recyclebin?ids={ids}&api-version=7.2-preview

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: recycle_bin_get_batch.sh --project <proyecto> --ids <id1,id2,...>

Fuente: api_doc/wit_sections/recycle_bin.md (Get Deleted Work Items)

Descripción:
  Obtiene múltiples work items eliminados mediante sus IDs.

Parámetros:
  --project   Nombre o ID del proyecto (obligatorio)
  --ids       Lista de IDs separados por coma (obligatorio)

Ejemplos:
  scripts/curl/wit/recycle_bin_get_batch.sh --project "Mi Proyecto" --ids 123,456,789
USAGE
}

PROJECT=""; IDS=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --ids) IDS="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$IDS" ]]; then echo "--project y --ids son obligatorios" >&2; usage; exit 2; fi
if ! [[ "$IDS" =~ ^[0-9,]+$ ]]; then echo "--ids debe ser lista de enteros separados por coma" >&2; exit 3; fi
ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_IDS=$(jq -rn --arg s "$IDS" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/recyclebin?ids=${ENC_IDS}&api-version=7.2-preview"

curl_json "$URL" | jq .
