#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() {
  echo "Uso: $0 <projectIdOrName>" >&2
  exit 1
}

PROJECT_ID=${1:-}
[[ -z "$PROJECT_ID" ]] && usage

URL="${DEVOPS_BASE}/_apis/projects/${PROJECT_ID}?api-version=7.2-preview.4"

curl_json "$URL" | jq .
