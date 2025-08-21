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
Uso: attachments_delete.sh --id <guid> [--project <project>] [--help]

Fuente: api_doc/wit_sections/attachments.md

Descripción:
  Elimina permanentemente un adjunto por ID.

Parámetros:
  --id       GUID del adjunto (obligatorio)
  --project  Nombre del proyecto (opcional, pero recomendado)

Ejemplos:
  scripts/curl/wit/attachments_delete.sh --id 11111111-2222-3333-4444-555555555555
  scripts/curl/wit/attachments_delete.sh --project "Mi Proyecto" --id 11111111-2222-3333-4444-555555555555
USAGE
}

ATT_ID=""
PROJECT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --id) ATT_ID="$2"; shift 2;;
    --project) PROJECT="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "${ATT_ID}" ]]; then
  echo "Falta --id (obligatorio)" >&2; usage; exit 2
fi

ENC_ID=$(jq -rn --arg s "${ATT_ID}" '$s|@uri')
# Nota: este endpoint requiere 7.2-preview (sin .1) en esta org
if [[ -n "${PROJECT}" ]]; then
  PROJECT_ENCODED="$(jq -rn --arg s "${PROJECT}" '$s|@uri')"
  URL="${DEVOPS_BASE}/${PROJECT_ENCODED}/_apis/wit/attachments/${ENC_ID}?api-version=7.2-preview"
else
  URL="${DEVOPS_BASE}/_apis/wit/attachments/${ENC_ID}?api-version=7.2-preview"
fi

RESPONSE=$(curl -sS -w "\n%{http_code}" -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -X DELETE "${URL}")

HTTP_CODE=$(echo "${RESPONSE}" | tail -n1)
BODY=$(echo "${RESPONSE}" | head -n -1)

if [[ "${HTTP_CODE}" -eq 200 || "${HTTP_CODE}" -eq 204 ]]; then
  echo "Attachment ${ATT_ID} eliminado exitosamente" >&2
  if [[ -n "${BODY}" ]]; then
    echo "${BODY}" | jq .
  fi
elif [[ "${HTTP_CODE}" -eq 404 ]]; then
  echo "Attachment ${ATT_ID} no encontrado (ya eliminado o no existe)" >&2
  echo "{\"message\": \"Attachment not found\", \"httpStatus\": 404}"
else
  echo "Error eliminando attachment. HTTP ${HTTP_CODE}" >&2
  echo "${BODY}" | jq .
  exit 1
fi
