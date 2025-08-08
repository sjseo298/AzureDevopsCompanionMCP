#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <projectId> <teamId>" >&2; exit 1; }
PROJECT_ID=${1:-}; TEAM_ID=${2:-}
[[ -z "$PROJECT_ID" || -z "$TEAM_ID" ]] && usage

URL="${DEVOPS_BASE}/_apis/projects/${PROJECT_ID}/teams/${TEAM_ID}/members?api-version=7.2-preview.3"

curl_json "$URL" | jq .
