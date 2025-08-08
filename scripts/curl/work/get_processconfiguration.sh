#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <project>" >&2; exit 1; }
PROJECT=${1:-}
[[ -z "$PROJECT" ]] && usage

urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PROJECT_ENC=$(urlencode "$PROJECT")

URL="${DEVOPS_BASE}/${PROJECT_ENC}/_apis/work/processconfiguration?api-version=${AZURE_DEVOPS_API_VERSION}"

curl_json "$URL" | jq .
