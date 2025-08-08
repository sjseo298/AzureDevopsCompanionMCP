#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <projectId> [keysCSV]" >&2; exit 1; }
PROJECT_ID=${1:-}
KEYS=${2:-}
[[ -z "$PROJECT_ID" ]] && usage

urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PID_ENC=$(urlencode "$PROJECT_ID")

URL="${DEVOPS_BASE}/_apis/projects/${PID_ENC}/properties?api-version=7.2-preview.1"
if [[ -n "$KEYS" ]]; then
  URL+="&keys=$(printf '%s' "$KEYS" | tr -d ' \t')"
fi

curl_json "$URL" | jq .
