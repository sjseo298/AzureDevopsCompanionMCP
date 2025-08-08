#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/attachments.md
# Nivel: Organización

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: attachments_delete.sh --id <guid> [--help]

Fuente: api_doc/wit_sections/attachments.md

Descripción:
  Elimina permanentemente un adjunto por ID.

Parámetros:
  --id   GUID del adjunto (obligatorio)

Ejemplos:
  scripts/curl/wit/attachments_delete.sh --id 11111111-2222-3333-4444-555555555555
USAGE
}

ATT_ID=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --id) ATT_ID="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "${ATT_ID}" ]]; then
  echo "Falta --id (obligatorio)" >&2; usage; exit 2
fi

ENC_ID=$(jq -rn --arg s "${ATT_ID}" '$s|@uri')
URL="${DEVOPS_BASE}/_apis/wit/attachments/${ENC_ID}?api-version=${AZURE_DEVOPS_API_VERSION}"

curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -X DELETE "${URL}" | jq .
