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
Uso: attachments_create.sh --file <ruta> [--name <nombreArchivo>] [--help]

Fuente: api_doc/wit_sections/attachments.md

Descripción:
  Sube un archivo adjunto y devuelve su metadata (id, url).

Parámetros:
  --file    Ruta al archivo a subir (obligatorio)
  --name    Nombre a usar en fileName (opcional; por defecto basename de --file)

Ejemplos:
  scripts/curl/wit/attachments_create.sh --file ./imagen.png | jq .
  scripts/curl/wit/attachments_create.sh --file ./mi archivo.png --name "mi archivo.png" | jq .
USAGE
}

FILE=""
NAME=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --file) FILE="$2"; shift 2;;
    --name) NAME="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "${FILE}" ]]; then
  echo "Falta --file (obligatorio)" >&2; usage; exit 2
fi
if [[ ! -f "${FILE}" ]]; then
  echo "No existe el archivo: ${FILE}" >&2; exit 3
fi

if [[ -z "${NAME}" ]]; then
  NAME="$(basename -- "${FILE}")"
fi

ENC_NAME=$(jq -rn --arg s "${NAME}" '$s|@uri')
# Nota: este endpoint requiere 7.2-preview (sin .1) en esta org
URL="${DEVOPS_BASE}/_apis/wit/attachments?fileName=${ENC_NAME}&api-version=7.2-preview"

# Subir binario (octet-stream). No usar curl_json por content-type
curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -H "Content-Type: application/octet-stream" \
  --data-binary @"${FILE}" -X POST "${URL}" | jq .
