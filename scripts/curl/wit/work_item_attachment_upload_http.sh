#!/usr/bin/env bash
set -euo pipefail

# Fuente: api_doc/wit_sections/attachments.md (sección "Flujo recomendado" con endpoint HTTP multipart)
# Operación: Subir archivo vía HTTP multipart al servidor MCP y asociarlo a un Work Item (AttachedFile)
# Endpoint: POST {MCP_BASE}/mcp/uploads/wit/workitems/{id}/attachment?project={project}[&apiVersion=...][&raw=true]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_attachment_upload_http.sh \
  --project <project> --id <id> --file <ruta|file://URI|data:URI> \
  [--name <nombre>] [--comment <texto>] [--content-type <mime>] \
  [--api-version <ver>] [--mcp-base <url>] [--raw] [--help]

Fuente: api_doc/wit_sections/attachments.md (endpoint HTTP multipart)

Descripción:
  Envía un archivo por multipart/form-data al servidor MCP y lo adjunta al Work Item indicado.
  Evita dependencia de base64 o filesystem compartido entre cliente y servidor.

Parámetros obligatorios:
  --project       Nombre del proyecto de Azure DevOps
  --id            ID numérico del work item
  --file          Ruta local, URI file:// o data:URI del archivo a subir

Parámetros opcionales:
  --name          Nombre a usar (por defecto basename del archivo o inferido)
  --comment       Comentario para la relación AttachedFile
  --content-type  MIME type (por defecto application/octet-stream)
  --api-version   Versión de API para el PATCH del work item (default 7.2-preview)
  --mcp-base      Base URL del servidor MCP (default http://localhost:8080)
  --raw           Solicita respuesta cruda combinada (adjunto + patch)

Ejemplos:
  scripts/curl/wit/work_item_attachment_upload_http.sh --project "Mi Proyecto" --id 123 --file ./doc.pdf
  scripts/curl/wit/work_item_attachment_upload_http.sh --project MyProject --id 456 --file "file:///tmp/imagen.png" --comment "Evidencia"
  scripts/curl/wit/work_item_attachment_upload_http.sh --project Test --id 789 --file "data:text/plain;base64,SG9sYQ==" --name hola.txt --raw
USAGE
}

PROJECT=""
ID=""
FILE_INPUT=""
NAME=""
COMMENT=""
CONTENT_TYPE=""
API_VERSION="7.2-preview"
RAW=0
MCP_BASE="${MCP_BASE:-http://localhost:8080}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --id) ID="$2"; shift 2;;
    --file) FILE_INPUT="$2"; shift 2;;
    --name) NAME="$2"; shift 2;;
    --comment) COMMENT="$2"; shift 2;;
    --content-type) CONTENT_TYPE="$2"; shift 2;;
    --api-version) API_VERSION="$2"; shift 2;;
    --mcp-base) MCP_BASE="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

# Validaciones
if [[ -z "${PROJECT}" ]]; then echo "Falta --project" >&2; usage; exit 2; fi
if [[ -z "${ID}" ]]; then echo "Falta --id" >&2; usage; exit 2; fi
if ! [[ "${ID}" =~ ^[0-9]+$ ]]; then echo "--id debe ser numérico" >&2; exit 2; fi
if [[ -z "${FILE_INPUT}" ]]; then echo "Falta --file" >&2; usage; exit 2; fi

# Normalizar origen del archivo: soportar file:// y data:
TMP_FILE=""
cleanup() { if [[ -n "${TMP_FILE}" && -f "${TMP_FILE}" ]]; then rm -f -- "${TMP_FILE}"; fi }
trap cleanup EXIT

FILE_PATH="${FILE_INPUT}"
if [[ "${FILE_INPUT}" == file://* ]]; then
  FILE_PATH="${FILE_INPUT#file://}"
elif [[ "${FILE_INPUT}" == data:* ]]; then
  META_AND_DATA="${FILE_INPUT#data:}"
  META="${META_AND_DATA%%,*}"
  DATA_PART="${META_AND_DATA#*,}"
  if [[ "${META}" == *";base64"* ]]; then
    TMP_FILE="$(mktemp)"
    printf '%s' "${DATA_PART}" | base64 -d > "${TMP_FILE}"
  else
    TMP_FILE="$(mktemp)"
    printf '%s' "${DATA_PART}" > "${TMP_FILE}"
  fi
  FILE_PATH="${TMP_FILE}"
  # Inferir nombre si no viene y meta incluye media-type
  if [[ -z "${NAME}" && -n "${META}" ]]; then
    case "${META}" in
      image/png*) NAME="attachment.png";;
      image/jpeg*) NAME="attachment.jpg";;
      image/gif*) NAME="attachment.gif";;
      application/pdf*) NAME="attachment.pdf";;
      text/plain*) NAME="attachment.txt";;
    esac
  fi
fi

if [[ ! -f "${FILE_PATH}" ]]; then echo "No existe el archivo: ${FILE_INPUT}" >&2; exit 3; fi
if [[ -z "${NAME}" ]]; then NAME="$(basename -- "${FILE_PATH}")"; fi
if [[ -z "${CONTENT_TYPE}" ]]; then CONTENT_TYPE="application/octet-stream"; fi

# Construir URL
PROJECT_ENC="$(jq -rn --arg s "${PROJECT}" '$s|@uri')"
ID_ENC="$(jq -rn --arg s "${ID}" '$s|@uri')"
RAW_QS=""; if [[ ${RAW} -eq 1 ]]; then RAW_QS="&raw=true"; fi
URL="${MCP_BASE%/}/mcp/uploads/wit/workitems/${ID_ENC}/attachment?project=${PROJECT_ENC}&apiVersion=${API_VERSION}${RAW_QS}"

# Mostrar info
{
  echo "=== Subiendo adjunto vía MCP HTTP ==="
  echo "MCP: ${MCP_BASE}"
  echo "Proyecto: ${PROJECT}"
  echo "Work Item: ${ID}"
  echo "Archivo: ${FILE_PATH}"
  echo "Nombre: ${NAME}"
  echo "Content-Type: ${CONTENT_TYPE}"
} >&2

# Ejecutar POST multipart
# Nota: No se envía Authorization hacia MCP; la seguridad del MCP depende de su despliegue (red, reverse proxy, auth propia).
HTTP_RESP=$(curl -sS -w "\n%{http_code}" \
  -X POST "${URL}" \
  -H "Accept: application/json" \
  -F "file=@${FILE_PATH};type=${CONTENT_TYPE}" \
  -F "fileName=${NAME}" \
  ${COMMENT:+-F "comment=${COMMENT}"} \
  ${CONTENT_TYPE:+-F "contentType=${CONTENT_TYPE}"})

HTTP_CODE=$(echo "${HTTP_RESP}" | tail -n1)
HTTP_BODY=$(echo "${HTTP_RESP}" | head -n -1)

if [[ "${HTTP_CODE}" -ge 200 && "${HTTP_CODE}" -lt 300 ]]; then
  if command -v jq >/dev/null 2>&1; then echo "${HTTP_BODY}" | jq .; else echo "${HTTP_BODY}"; fi
  exit 0
else
  echo "Error HTTP ${HTTP_CODE} al subir/adjuntar" >&2
  echo "${HTTP_BODY}" >&2
  exit 7
fi
