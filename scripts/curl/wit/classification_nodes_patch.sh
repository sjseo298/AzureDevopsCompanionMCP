#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/classification_nodes.md
# Operación: Update (PATCH)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: classification_nodes_patch.sh --project <proj> --group <areas|iterations> --path <ruta> [--name <nuevoNombre>] [--help]

Fuente: api_doc/wit_sections/classification_nodes.md

Descripción:
  Actualiza propiedades de un nodo de clasificación (área o iteración) en el proyecto indicado.

Parámetros:
  --project   Nombre/ID del proyecto (obligatorio)
  --group     'areas' o 'iterations' (obligatorio)
  --path      Ruta completa del nodo (obligatorio)
  --name      Nuevo nombre del nodo (opcional)

Ejemplos:
  scripts/curl/wit/classification_nodes_patch.sh --project "Mi Proyecto" --group areas --path "NuevaArea" --name "AreaRenombrada"
USAGE
}

PROJECT=""
GROUP=""
NODE_PATH=""
NEW_NAME=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --group) GROUP="$2"; shift 2;;
    --path) NODE_PATH="$2"; shift 2;;
    --name) NEW_NAME="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$GROUP" || -z "$NODE_PATH" ]]; then
  echo "Faltan --project, --group y/o --path (obligatorios)" >&2; usage; exit 2
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_GROUP=$(jq -rn --arg s "$GROUP" '$s|@uri')
ENC_PATH=$(jq -rn --arg s "$NODE_PATH" '$s|@uri')
API_VER_OVERRIDE="7.2-preview"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/classificationnodes/${ENC_GROUP}/${ENC_PATH}?api-version=${API_VER_OVERRIDE}"

BODY='{}'
if [[ -n "$NEW_NAME" ]]; then
  BODY=$(jq -n --arg name "$NEW_NAME" '{name: $name}')
fi

curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -H "Content-Type: application/json" -X PATCH \
  -d "$BODY" "$URL" | jq .
