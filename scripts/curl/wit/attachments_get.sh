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
Uso: attachments_get.sh --id <guid> [--output <ruta>] [--help]

Fuente: api_doc/wit_sections/attachments.md

Descripción:
  Obtiene un adjunto por ID. Si se especifica --output, guarda el binario en la ruta y muestra headers.
  Si no se especifica --output, solo muestra los headers (la respuesta es binaria).

Parámetros:
  --id       GUID del adjunto (obligatorio)
  --output   Ruta para guardar el archivo (opcional)

Ejemplos:
  scripts/curl/wit/attachments_get.sh --id 11111111-2222-3333-4444-555555555555 --output ./descarga.bin
  scripts/curl/wit/attachments_get.sh --id 11111111-2222-3333-4444-555555555555
USAGE
}

ATT_ID=""
OUTPUT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --id) ATT_ID="$2"; shift 2;;
    --output) OUTPUT="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "${ATT_ID}" ]]; then
  echo "Falta --id (obligatorio)" >&2; usage; exit 2
fi

ENC_ID=$(jq -rn --arg s "${ATT_ID}" '$s|@uri')
# Nota: para descargar el binario se usa download=true y api-version 7.2-preview en esta org
URL="${DEVOPS_BASE}/_apis/wit/attachments/${ENC_ID}?download=true&api-version=7.2-preview"

# Mostrar headers siempre; si hay output, guardar binario
if [[ -n "${OUTPUT}" ]]; then
  curl -sS -u ":${AZURE_DEVOPS_PAT}" -D - -H "Accept: application/octet-stream" \
    -L -X GET "${URL}" --output "${OUTPUT}" | sed 's/^/H: /'
  echo "Archivo guardado en: ${OUTPUT}"
else
  curl -sS -u ":${AZURE_DEVOPS_PAT}" -D - -H "Accept: application/octet-stream" \
    -L -X GET "${URL}" --output /dev/null | sed 's/^/H: /'
fi
