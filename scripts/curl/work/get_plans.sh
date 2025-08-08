#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <project> <team> [planId]" >&2; exit 1; }
PROJECT=${1:-}
TEAM=${2:-}
PLAN=${3:-}
[[ -z "$PROJECT" || -z "$TEAM" ]] && usage

urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PROJECT_ENC=$(urlencode "$PROJECT")
TEAM_ENC=$(urlencode "$TEAM")

if [[ -n "$PLAN" ]]; then
  PLAN_ENC=$(urlencode "$PLAN")
  URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/plans/${PLAN_ENC}?api-version=${AZURE_DEVOPS_API_VERSION}"
else
  URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/plans?api-version=${AZURE_DEVOPS_API_VERSION}"
fi

curl_json "$URL" | jq .
