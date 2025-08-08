#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/wiql.md
# Operación: Query By Wiql (POST)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: wiql_by_query.sh --project <proj> --wiql <consultaWIQL> [--help]

Fuente: api_doc/wit_sections/wiql.md

Descripción:
  Ejecuta una consulta WIQL en el contexto del proyecto.

Parámetros:
  --project   Nombre/ID del proyecto (obligatorio)
  --wiql      Consulta WIQL (obligatorio)

Ejemplos:
  scripts/curl/wit/wiql_by_query.sh --project "Mi Proyecto" --wiql "SELECT [System.Id] FROM WorkItems WHERE [System.TeamProject] = @project ORDER BY [System.ChangedDate] DESC"
USAGE
}

PROJECT=""
WIQL=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --wiql) WIQL="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$WIQL" ]]; then
  echo "Faltan --project y/o --wiql (obligatorios)" >&2; usage; exit 2
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
API_VER_OVERRIDE="7.2-preview"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/wiql?api-version=${API_VER_OVERRIDE}"

BODY=$(jq -n --arg q "$WIQL" '{query: $q}')

curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -H "Content-Type: application/json" -X POST \
  -d "$BODY" "$URL" | jq .
