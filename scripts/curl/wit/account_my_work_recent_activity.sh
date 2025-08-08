#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/account_my_work_recent_activity.md
# Nivel: Organización (no requiere project/team)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# Cargar entorno común
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: account_my_work_recent_activity.sh [--help]

Fuente: api_doc/wit_sections/account_my_work_recent_activity.md

Descripción:
  Lista actividades recientes de work items del usuario autenticado.

Parámetros:
  (ninguno) – endpoint a nivel de organización

Ejemplos:
  scripts/curl/wit/account_my_work_recent_activity.sh | jq .
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

# Nota: En esta organización el endpoint operativo está bajo el área 'work'
URL="${DEVOPS_BASE}/_apis/work/accountmyworkrecentactivity?api-version=${AZURE_DEVOPS_API_VERSION}"

# Realiza la solicitud y formatea salida JSON
curl_json "${URL}" | jq .
