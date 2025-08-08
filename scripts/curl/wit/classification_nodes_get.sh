#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/classification_nodes.md
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: classification_nodes_get.sh --project <proj> --group <areas|iterations> [--path <ruta>] [--help]

Fuente: api_doc/wit_sections/classification_nodes.md

Descripción:
  Obtiene un nodo de clasificación (área o iteración). Si no se especifica --path, devuelve el root de ese grupo.

Parámetros:
  --project   Nombre/ID del proyecto (obligatorio)
  --group     'areas' o 'iterations' (obligatorio)
  --path      Ruta del nodo (opcional), e.g. "Area1/Subarea"

Ejemplos:
  scripts/curl/wit/classification_nodes_get.sh --project "Mi Proyecto" --group areas --path "NuevaArea"
  scripts/curl/wit/classification_nodes_get.sh --project "Mi Proyecto" --group iterations
USAGE
}

PROJECT=""
GROUP=""
NODE_PATH=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --group) GROUP="$2"; shift 2;;
    --path) NODE_PATH="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$GROUP" ]]; then
  echo "Faltan --project y/o --group (obligatorios)" >&2; usage; exit 2
fi

# Validar group
LOW_GROUP=$(echo "$GROUP" | tr '[:upper:]' '[:lower:]')
if [[ "$LOW_GROUP" != "areas" && "$LOW_GROUP" != "iterations" ]]; then
  echo "Parámetro --group inválido. Valores permitidos: areas, iterations" >&2; exit 3
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_GROUP=$(jq -rn --arg s "$LOW_GROUP" '$s|@uri')
# Forzar 7.2-preview (sin .1) según doc local
API_VER_OVERRIDE="7.2-preview"
if [[ -n "$NODE_PATH" ]]; then
  ENC_PATH=$(jq -rn --arg s "$NODE_PATH" '$s|@uri')
  URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/classificationnodes/${ENC_GROUP}/${ENC_PATH}?api-version=${API_VER_OVERRIDE}"
else
  URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/classificationnodes/${ENC_GROUP}?api-version=${API_VER_OVERRIDE}"
fi

curl_json "${URL}" | jq .
