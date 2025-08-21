#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/attachments.md (combinado con work_items.md)
# Operación: Create attachment + Link to Work Item (atomico)
# Combina: POST /_apis/wit/attachments + PATCH /{project}/_apis/wit/workitems/{id}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_attachment_add.sh --project <project> --id <id> --file <ruta> \
  [--name <nombre>] [--comment <texto>] [--content-type <mime>] \
  [--api-version <ver>] [--raw] [--help]

Fuente: api_doc/wit_sections/attachments.md + work_items.md

Descripción:
  Sube un archivo y lo asocia directamente a un Work Item como AttachedFile.
  Operación atómica que evita archivos huérfanos ejecutando ambas operaciones secuencialmente
  con rollback automático si falla la asociación.

Parámetros obligatorios:
  --project     Nombre del proyecto
  --id          ID numérico del work item
  --file        Ruta al archivo a subir

Parámetros opcionales:
  --name        Nombre a usar (por defecto basename del archivo)
  --comment     Comentario para la relación AttachedFile
  --content-type MIME type (por defecto detectado automáticamente)
  --api-version API version para PATCH (default 7.2-preview)
  --raw         Devuelve JSON crudo combinado de ambas operaciones

Ejemplos:
  scripts/curl/wit/work_item_attachment_add.sh --project "Mi Proyecto" --id 123 --file ./documento.pdf
  scripts/curl/wit/work_item_attachment_add.sh --project MyProject --id 456 --file "./mi archivo.png" --comment "Screenshot del bug"
  scripts/curl/wit/work_item_attachment_add.sh --project Test --id 789 --file ./data.json --content-type application/json --raw | jq .
USAGE
}

PROJECT=""
ID=""
FILE=""
NAME=""
COMMENT=""
CONTENT_TYPE=""
API_VERSION="7.2-preview"
RAW=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --id) ID="$2"; shift 2;;
    --file) FILE="$2"; shift 2;;
    --name) NAME="$2"; shift 2;;
    --comment) COMMENT="$2"; shift 2;;
    --content-type) CONTENT_TYPE="$2"; shift 2;;
    --api-version) API_VERSION="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "${PROJECT}" ]]; then
  echo "Falta --project (obligatorio)" >&2; usage; exit 2
fi
if [[ -z "${ID}" ]]; then
  echo "Falta --id (obligatorio)" >&2; usage; exit 2
fi
if [[ -z "${FILE}" ]]; then
  echo "Falta --file (obligatorio)" >&2; usage; exit 2
fi
if [[ ! -f "${FILE}" ]]; then
  echo "No existe el archivo: ${FILE}" >&2; exit 3
fi

# Validar que ID sea numérico
if ! [[ "${ID}" =~ ^[0-9]+$ ]]; then
  echo "El work item ID debe ser numérico: ${ID}" >&2; exit 4
fi

if [[ -z "${NAME}" ]]; then
  NAME="$(basename -- "${FILE}")"
fi

# Codificar parámetros para URL
PROJECT_ENCODED="$(jq -rn --arg s "${PROJECT}" '$s|@uri')"
ID_ENCODED="$(jq -rn --arg s "${ID}" '$s|@uri')"
NAME_ENCODED="$(jq -rn --arg s "${NAME}" '$s|@uri')"

# Para el endpoint de Azure DevOps attachments, siempre usar application/octet-stream
# El tipo de archivo se detecta automáticamente por el servidor
if [[ -z "${CONTENT_TYPE}" ]]; then
  CONTENT_TYPE="application/octet-stream"
fi

echo "=== Subiendo adjunto ===" >&2
echo "Proyecto: ${PROJECT}" >&2
echo "Work Item: ${ID}" >&2
echo "Archivo: ${FILE}" >&2
echo "Nombre: ${NAME}" >&2
echo "Content-Type: ${CONTENT_TYPE}" >&2

# Paso 1: Subir el archivo
UPLOAD_URL="${DEVOPS_BASE}/_apis/wit/attachments?fileName=${NAME_ENCODED}&api-version=7.2-preview"

echo "Subiendo archivo..." >&2
UPLOAD_RESPONSE=$(curl -sS -w "\n%{http_code}" \
  -X POST "${UPLOAD_URL}" \
  -u ":${AZURE_DEVOPS_PAT}" \
  -H "Accept: application/json" \
  -H "Content-Type: ${CONTENT_TYPE}" \
  --data-binary "@${FILE}")

UPLOAD_HTTP_CODE=$(echo "${UPLOAD_RESPONSE}" | tail -n1)
UPLOAD_BODY=$(echo "${UPLOAD_RESPONSE}" | head -n -1)

if [[ "${UPLOAD_HTTP_CODE}" -lt 200 || "${UPLOAD_HTTP_CODE}" -ge 300 ]]; then
  echo "Error subiendo archivo. HTTP ${UPLOAD_HTTP_CODE}" >&2
  echo "${UPLOAD_BODY}" >&2
  exit 5
fi

# Extraer URL del attachment
ATTACHMENT_URL=$(echo "${UPLOAD_BODY}" | jq -r '.url // empty')
ATTACHMENT_ID=$(echo "${UPLOAD_BODY}" | jq -r '.id // empty')

if [[ -z "${ATTACHMENT_URL}" ]]; then
  echo "Error: No se obtuvo URL del attachment" >&2
  echo "${UPLOAD_BODY}" >&2
  exit 6
fi

echo "Archivo subido. URL: ${ATTACHMENT_URL}" >&2

# Paso 2: Asociar al work item
echo "Asociando al work item ${ID}..." >&2

# Construir el JSON patch usando jq para manejo seguro de strings
if [[ -n "${COMMENT}" ]]; then
  PATCH_JSON=$(jq -cn --arg url "${ATTACHMENT_URL}" --arg comment "${COMMENT}" '
  [{
    "op": "add",
    "path": "/relations/-",
    "value": {
      "rel": "AttachedFile",
      "url": $url,
      "attributes": {
        "comment": $comment
      }
    }
  }]')
else
  PATCH_JSON=$(jq -cn --arg url "${ATTACHMENT_URL}" '
  [{
    "op": "add",
    "path": "/relations/-",
    "value": {
      "rel": "AttachedFile",
      "url": $url
    }
  }]')
fi

PATCH_URL="${DEVOPS_BASE}/${PROJECT_ENCODED}/_apis/wit/workitems/${ID_ENCODED}?api-version=${API_VERSION}"

PATCH_RESPONSE=$(curl -sS -w "\n%{http_code}" \
  -X PATCH "${PATCH_URL}" \
  -u ":${AZURE_DEVOPS_PAT}" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json-patch+json" \
  -d "${PATCH_JSON}")

PATCH_HTTP_CODE=$(echo "${PATCH_RESPONSE}" | tail -n1)
PATCH_BODY=$(echo "${PATCH_RESPONSE}" | head -n -1)

if [[ "${PATCH_HTTP_CODE}" -lt 200 || "${PATCH_HTTP_CODE}" -ge 300 ]]; then
  echo "Error asociando attachment al work item. HTTP ${PATCH_HTTP_CODE}" >&2
  echo "${PATCH_BODY}" >&2
  
  # Intentar rollback - eliminar el attachment huérfano
  if [[ -n "${ATTACHMENT_ID}" ]]; then
    echo "Ejecutando rollback (eliminando attachment ${ATTACHMENT_ID})..." >&2
    DELETE_URL="${DEVOPS_BASE}/${PROJECT_ENCODED}/_apis/wit/attachments/${ATTACHMENT_ID}?api-version=7.2-preview"
    DELETE_RESPONSE=$(curl -sS -w "\n%{http_code}" \
      -X DELETE "${DELETE_URL}" \
      -u ":${AZURE_DEVOPS_PAT}" \
      -H "Accept: application/json")
    
    DELETE_HTTP_CODE=$(echo "${DELETE_RESPONSE}" | tail -n1)
    if [[ "${DELETE_HTTP_CODE}" -eq 200 || "${DELETE_HTTP_CODE}" -eq 204 || "${DELETE_HTTP_CODE}" -eq 404 ]]; then
      echo "Rollback exitoso: attachment huérfano eliminado." >&2
    else
      echo "Advertencia: No se pudo eliminar el attachment huérfano (ID: ${ATTACHMENT_ID})" >&2
      echo "Elimínalo manualmente usando: scripts/curl/wit/attachments_delete.sh --project \"${PROJECT}\" --id ${ATTACHMENT_ID}" >&2
    fi
  else
    echo "No se pudo determinar ID del attachment para rollback." >&2
  fi
  exit 7
fi

echo "¡Archivo adjuntado exitosamente!" >&2

# Mostrar resultado según modo
if [[ ${RAW} -eq 1 ]]; then
  echo "{"
  echo "  \"attachment\": ${UPLOAD_BODY},"
  echo "  \"workItemPatch\": ${PATCH_BODY},"
  echo "  \"operationStatus\": \"SUCCESS\""
  echo "}"
else
  echo "=== Attachment Adjuntado ===

WorkItem: ${ID}
Archivo: ${NAME}
URL: ${ATTACHMENT_URL}"
  if [[ -n "${COMMENT}" ]]; then
    echo "Comentario: ${COMMENT}"
  fi
fi
