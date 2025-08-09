#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/wit_sections/recycle_bin.md (Destroy Work Item)
# DELETE /{organization}/{project}/_apis/wit/recyclebin/{id}?api-version=7.2-preview

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: recycle_bin_destroy.sh --project <proyecto> --id <id>

Fuente: api_doc/wit_sections/recycle_bin.md (Destroy Work Item)

Descripción:
  Elimina permanentemente (destroy) un work item que está en la papelera.

Parámetros:
  --project   Nombre o ID del proyecto (obligatorio)
  --id        ID del work item eliminado (obligatorio)

Advertencia:
  Operación irreversible. Asegúrate antes de ejecutarla.

Ejemplos:
  scripts/curl/wit/recycle_bin_destroy.sh --project "Mi Proyecto" --id 12345
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

HTTP_CODE=$(curl -sS -o /dev/null -w '%{http_code}' -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -X DELETE "$URL")
if [[ "$HTTP_CODE" == "204" ]]; then
  echo "Eliminado permanentemente (ID=${ID})."
else
  echo "Error (HTTP $HTTP_CODE) al eliminar permanentemente ID=${ID}" >&2
  exit 1
fi
