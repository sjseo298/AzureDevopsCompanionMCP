#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/artifact_link_types.md
# Nivel: Organización

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: artifact_link_types.sh [--help]

Fuente: api_doc/wit_sections/artifact_link_types.md

Descripción:
  Lista los tipos de enlaces de artefactos (artifact link types).

Parámetros:
  (ninguno) – endpoint a nivel de organización

Ejemplos:
  scripts/curl/wit/artifact_link_types.sh | jq .
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

URL="${DEVOPS_BASE}/_apis/wit/artifactlinktypes?api-version=${AZURE_DEVOPS_API_VERSION}"

curl_json "${URL}" | jq .
