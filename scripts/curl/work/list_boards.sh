#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <project> [team]" >&2; exit 1; }
PROJECT=${1:-}
TEAM=${2:-}
[[ -z "$PROJECT" ]] && usage

if [[ -n "$TEAM" ]]; then
  URL="${DEVOPS_BASE}/${PROJECT}/${TEAM}/_apis/work/boards?api-version=7.2-preview.1"
else
  URL="${DEVOPS_BASE}/${PROJECT}/_apis/work/boards?api-version=7.2-preview.1"
fi

curl_json "$URL" | jq .
