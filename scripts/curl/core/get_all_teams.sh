#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

MINE=${1:-}
QP="api-version=7.2-preview.3"
if [[ "$MINE" == "--mine" ]]; then
  QP+="&mine=true"
fi
URL="${DEVOPS_BASE}/_apis/teams?${QP}"

curl_json "$URL" | jq .
