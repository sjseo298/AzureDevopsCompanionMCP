#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

URL="${VSSPS_BASE}/_apis/profile/profiles/me?api-version=${AZURE_DEVOPS_VSSPS_API_VERSION}"

curl_json "$URL" | jq '.id, .displayName, .emailAddress'
