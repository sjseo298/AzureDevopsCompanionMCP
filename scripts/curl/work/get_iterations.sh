#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <project> <team> [iterationId] [--timeframe past|current|future]" >&2; exit 1; }
PROJECT=${1:-}
TEAM=${2:-}
ITERATION=${3:-}
shift 3 || true
TIMEFRAME=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --timeframe) TIMEFRAME=${2:-}; shift 2;;
    *) echo "Flag desconocida: $1" >&2; exit 1;;
  esac
done
[[ -z "$PROJECT" || -z "$TEAM" ]] && usage

urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PROJECT_ENC=$(urlencode "$PROJECT")
TEAM_ENC=$(urlencode "$TEAM")

if [[ -n "$ITERATION" ]]; then
  ITERATION_ENC=$(urlencode "$ITERATION")
  URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/teamsettings/iterations/${ITERATION_ENC}?api-version=${AZURE_DEVOPS_API_VERSION}"
else
  if [[ -n "$TIMEFRAME" ]]; then
    URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/teamsettings/iterations?timeframe=${TIMEFRAME}&api-version=${AZURE_DEVOPS_API_VERSION}"
  else
    URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/teamsettings/iterations?api-version=${AZURE_DEVOPS_API_VERSION}"
  fi
fi

curl_json "$URL" | jq .
