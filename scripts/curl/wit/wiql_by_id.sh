#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/wiql.md
# Operación: Query By Id (GET)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: wiql_by_id.sh --project <proj> --id <queryId> [--help]

Fuente: api_doc/wit_sections/wiql.md

Descripción:
  Ejecuta una query guardada por su ID.

Parámetros:
  --project   Nombre/ID del proyecto (obligatorio)
  --id        ID de la query guardada (obligatorio)

Ejemplos:
  scripts/curl/wit/wiql_by_id.sh --project "Mi Proyecto" --id 00000000-0000-0000-0000-000000000000
USAGE
}

PROJECT=""
QID=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --id) QID="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$QID" ]]; then
  echo "Faltan --project y/o --id (obligatorios)" >&2; usage; exit 2
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_ID=$(jq -rn --arg s "$QID" '$s|@uri')
API_VER_OVERRIDE="7.2-preview"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/wiql/${ENC_ID}?api-version=${API_VER_OVERRIDE}"

curl_json "$URL" | jq .
