#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <project> <team> <iterationId>" >&2; exit 1; }
PROJECT=${1:-}
TEAM=${2:-}
ITERATION=${3:-}
[[ -z "$PROJECT" || -z "$TEAM" || -z "$ITERATION" ]] && usage

urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PROJECT_ENC=$(urlencode "$PROJECT")
TEAM_ENC=$(urlencode "$TEAM")
ITERATION_ENC=$(urlencode "$ITERATION")

URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/teamsettings/iterations/${ITERATION_ENC}/capacities?api-version=7.2-preview.3"

curl_json "$URL" | jq .
