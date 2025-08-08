#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/classification_nodes.md
# Operación: Get Root Nodes
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: classification_nodes_get_root.sh --project <proj> [--help]

Fuente: api_doc/wit_sections/classification_nodes.md

Descripción:
  Obtiene los nodos raíz de clasificación (Areas e Iterations) del proyecto.

Parámetros:
  --project   Nombre/ID del proyecto (obligatorio)

Ejemplos:
  scripts/curl/wit/classification_nodes_get_root.sh --project "Mi Proyecto"
USAGE
}

PROJECT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" ]]; then
  echo "Falta --project (obligatorio)" >&2; usage; exit 2
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
API_VER_OVERRIDE="7.2-preview"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/classificationnodes?api-version=${API_VER_OVERRIDE}"

curl_json "${URL}" | jq .
