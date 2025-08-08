#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/classification_nodes.md
# Operación: Get Classification Nodes by ids
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: classification_nodes_get_by_ids.sh --project <proj> --ids <id1,id2,...> [--help]

Fuente: api_doc/wit_sections/classification_nodes.md

Descripción:
  Obtiene nodos de clasificación por IDs.

Parámetros:
  --project   Nombre/ID del proyecto (obligatorio)
  --ids       Lista de IDs separados por coma (obligatorio)

Ejemplos:
  scripts/curl/wit/classification_nodes_get_by_ids.sh --project "Mi Proyecto" --ids 1,2,3
USAGE
}

PROJECT=""
IDS=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --ids) IDS="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$IDS" ]]; then
  echo "Faltan --project y/o --ids (obligatorios)" >&2; usage; exit 2
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_IDS=$(jq -rn --arg s "$IDS" '$s|@uri')
API_VER_OVERRIDE="7.2-preview"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/classificationnodes?ids=${ENC_IDS}&api-version=${API_VER_OVERRIDE}"

curl_json "${URL}" | jq .
