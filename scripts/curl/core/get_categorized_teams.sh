#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <projectIdOrName> [--mine]" >&2; exit 1; }
PROJECT_ID=${1:-}; shift || true
[[ -z "$PROJECT_ID" ]] && usage
MINE=false
if [[ "${1:-}" == "--mine" ]]; then MINE=true; fi

urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PID_ENC=$(urlencode "$PROJECT_ID")

URL="${DEVOPS_BASE}/_apis/projects/${PID_ENC}/teams?api-version=7.2-preview.1"
if [[ "$MINE" == true ]]; then URL+="&mine=true"; fi

curl_json "$URL" | jq .
