#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() {
  cat >&2 <<EOF
Uso: $0 <project> <team>

Parámetros (api_doc/work/backlogconfiguration.md):
  project   Nombre o ID del proyecto (obligatorio)
  team      Nombre o ID del equipo (obligatorio en nuestra org)
EOF
  exit 1
}

PROJECT=${1:-}
TEAM=${2:-}
[[ -z "$PROJECT" || -z "$TEAM" ]] && usage

urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PROJECT_ENC=$(urlencode "$PROJECT")
TEAM_ENC=$(urlencode "$TEAM")

URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/backlogconfiguration?api-version=${AZURE_DEVOPS_API_VERSION}"

curl_json "$URL" | jq .
