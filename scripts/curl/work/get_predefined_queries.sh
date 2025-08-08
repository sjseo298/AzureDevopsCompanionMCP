#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() {
  cat >&2 <<EOF
Uso: $0 <project> [query]

Parámetros:
  project   Nombre o ID del proyecto (obligatorio)
  query     Nombre de la query predefinida (opcional). Valores posibles: AssignedToMe, FollowedWorkItems, MyActivity, RecentlyCreated, RecentlyCompleted, RecentlyUpdated

Ejemplos:
  # Listar queries predefinidas
  $0 MiProyecto

  # Obtener una query específica
  $0 MiProyecto AssignedToMe
EOF
  exit 1
}

PROJECT=${1:-}
QUERY=${2:-}
[[ -z "${PROJECT}" ]] && usage

# URL-encode helper usando jq
urlencode() { jq -rn --arg s "$1" '$s|@uri'; }

PROJECT_ENC=$(urlencode "$PROJECT")

if [[ -n "${QUERY}" ]]; then
  QUERY_ENC=$(urlencode "$QUERY")
  URL="${DEVOPS_BASE}/${PROJECT_ENC}/_apis/work/predefinedqueries/${QUERY_ENC}?api-version=${AZURE_DEVOPS_API_VERSION}"
else
  URL="${DEVOPS_BASE}/${PROJECT_ENC}/_apis/work/predefinedqueries?api-version=${AZURE_DEVOPS_API_VERSION}"
fi

curl_json "$URL" | jq .
