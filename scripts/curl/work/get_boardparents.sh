#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() {
  cat >&2 <<'EOF'
Uso: $0 <project> <team> <childBacklogContextCategoryRefName> <workitemIdsCSV>

Parámetros (api_doc/work/boardparents.md):
  project    Nombre o ID del proyecto (obligatorio)
  team       Nombre o ID del equipo (obligatorio)
  childBacklogContextCategoryRefName  RefName de categoría del backlog hijo (obligatorio)
  workitemIdsCSV   Lista de IDs separados por comas (obligatorio)

Ejemplo:
  $0 MiProyecto MiEquipo Microsoft.RequirementCategory 123,124,125
EOF
  exit 1
}

PROJECT=${1:-}
TEAM=${2:-}
CHILD_CAT=${3:-}
IDS_CSV=${4:-}
[[ -z "$PROJECT" || -z "$TEAM" || -z "$CHILD_CAT" || -z "$IDS_CSV" ]] && usage

urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PROJECT_ENC=$(urlencode "$PROJECT")
TEAM_ENC=$(urlencode "$TEAM")
CHILD_ENC=$(urlencode "$CHILD_CAT")
IDS_ENC=$(urlencode "$IDS_CSV")

URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/boards/boardparents?childBacklogContextCategoryRefName=${CHILD_ENC}&workitemIds=${IDS_ENC}&api-version=${AZURE_DEVOPS_API_VERSION}"

curl_json "$URL" | jq .
