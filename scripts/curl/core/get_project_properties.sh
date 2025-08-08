#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <projectId> [keysCSV]" >&2; exit 1; }
PROJECT_ID=${1:-}
KEYS=${2:-}
[[ -z "$PROJECT_ID" ]] && usage

URL="${DEVOPS_BASE}/_apis/projects/${PROJECT_ID}/properties?api-version=7.2-preview.1"
if [[ -n "${KEYS}" ]]; then
  URL+="&keys=$(printf '%s' "$KEYS" | sed 's/\s//g')"
fi

curl_json "$URL" | jq .
