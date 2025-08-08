#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/artifact_uri_query.md
# Nivel: Organización

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: artifact_uri_query.sh --uris <uri1,uri2,...> [--help]

Fuente: api_doc/wit_sections/artifact_uri_query.md

Descripción:
  Consulta los work items vinculados a una lista de URIs de artefactos.

Parámetros:
  --uris    Lista separada por comas de URIs de artefactos (obligatorio)

Ejemplos:
  scripts/curl/wit/artifact_uri_query.sh --uris "vstfs:///Build/Build/12345,vstfs:///Git/PullRequestId/67890" | jq .
USAGE
}

URIS=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --uris)
      URIS="$2"; shift 2;;
    -h|--help)
      usage; exit 0;;
    *)
      echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "${URIS}" ]]; then
  echo "Falta --uris (obligatorio)" >&2
  usage
  exit 2
fi

# Construir body JSON
# Split por comas preservando espacios
IFS=',' read -r -a uri_array <<< "${URIS}"
json_array="["
for u in "${uri_array[@]}"; do
  u_trimmed="$(echo -n "$u" | sed 's/^ *//;s/ *$//')"
  json_array+="\"${u_trimmed//"/\"}\","  # escapado simple de comillas
done
json_array="${json_array%,}]"

BODY="{\"uris\": ${json_array}}"

URL="${DEVOPS_BASE}/_apis/wit/artifacturiquery?api-version=${AZURE_DEVOPS_API_VERSION}"

curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -H "Content-Type: application/json" \
  -X POST -d "${BODY}" "${URL}" | jq .
