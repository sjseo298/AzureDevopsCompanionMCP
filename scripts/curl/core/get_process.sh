#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <processId>" >&2; exit 1; }
PID=${1:-}
[[ -z "$PID" ]] && usage

URL="${DEVOPS_BASE}/_apis/process/processes/${PID}?api-version=7.2-preview.1"

curl_json "$URL" | jq .
