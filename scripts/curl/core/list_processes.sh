#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

URL="${DEVOPS_BASE}/_apis/process/processes?api-version=7.2-preview.1"

curl_json "$URL" | jq .
