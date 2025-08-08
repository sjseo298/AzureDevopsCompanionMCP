#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

if [[ $# -lt 1 ]]; then
  echo "Uso: $0 <accountId>" >&2
  exit 2
fi
ACCOUNT_ID="$1"

URL="${VSSPS_BASE}/_apis/accounts/${ACCOUNT_ID}?api-version=${AZURE_DEVOPS_VSSPS_API_VERSION}"

curl_json "$URL"
