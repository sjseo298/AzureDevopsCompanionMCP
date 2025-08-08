#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() {
  cat >&2 <<'EOF'
Uso: $0 <project> <team> [--id <backlogId>] [--include-work-items]

Parámetros (api_doc/work/backlogs.md):
  project   Nombre o ID del proyecto (obligatorio)
  team      Nombre o ID del equipo (opcional en doc, obligatorio en nuestra org)
  --id      ID de nivel de backlog (opcional)
  --include-work-items  Si se especifica --id, retorna los work items del backlog
EOF
  exit 1
}

PROJECT=${1:-}
TEAM=${2:-}
shift 2 || true
BACKLOG_ID=""
INCLUDE_WI=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --id)
      BACKLOG_ID=${2:-}; shift 2;;
    --include-work-items)
      INCLUDE_WI=true; shift;;
    *) echo "Flag desconocida: $1" >&2; usage;;
  esac
done

[[ -z "$PROJECT" || -z "$TEAM" ]] && usage

urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PROJECT_ENC=$(urlencode "$PROJECT")
TEAM_ENC=$(urlencode "$TEAM")

if [[ -z "$BACKLOG_ID" ]]; then
  URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/backlogs?api-version=${AZURE_DEVOPS_API_VERSION}"
else
  BACKLOG_ID_ENC=$(urlencode "$BACKLOG_ID")
  if $INCLUDE_WI; then
    URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/backlogs/${BACKLOG_ID_ENC}/workItems?api-version=${AZURE_DEVOPS_API_VERSION}"
  else
    URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/backlogs/${BACKLOG_ID_ENC}?api-version=${AZURE_DEVOPS_API_VERSION}"
  fi
fi

curl_json "$URL" | jq .
