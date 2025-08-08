#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <projectId> <teamId>" >&2; exit 1; }
PROJECT_ID=${1:-}; TEAM_ID=${2:-}
[[ -z "$PROJECT_ID" || -z "$TEAM_ID" ]] && usage

urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PID_ENC=$(urlencode "$PROJECT_ID")
TID_ENC=$(urlencode "$TEAM_ID")

URL="${DEVOPS_BASE}/_apis/projects/${PID_ENC}/teams/${TID_ENC}?api-version=7.2-preview.3"

curl_json "$URL" | jq .
