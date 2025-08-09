#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/wit_sections/recycle_bin.md (Restore Work Item)
# PATCH /{organization}/{project}/_apis/wit/recyclebin/{id}?api-version=7.2-preview

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: recycle_bin_restore.sh --project <proyecto> --id <id>

Fuente: api_doc/wit_sections/recycle_bin.md (Restore Work Item)

Descripción:
  Restaura un work item eliminado (lo saca de la papelera).

Parámetros:
  --project   Nombre o ID del proyecto (obligatorio)
  --id        ID del work item eliminado (obligatorio)

Ejemplos:
  scripts/curl/wit/recycle_bin_restore.sh --project "Mi Proyecto" --id 12345
USAGE
}

PROJECT=""; ID=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --id) ID="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$ID" ]]; then echo "--project y --id son obligatorios" >&2; usage; exit 2; fi
if ! [[ "$ID" =~ ^[0-9]+$ ]]; then echo "--id debe ser numérico" >&2; exit 3; fi
ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/recyclebin/${ID}?api-version=7.2-preview"

curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -H "Content-Type: application/json" -X PATCH -d '{}' "$URL" | jq .
