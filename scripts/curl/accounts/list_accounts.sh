#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

OWNER_ID="${1:-}"
MEMBER_ID="${2:-}"
PROPERTIES="${3:-}"

Q=()
[[ -n "$OWNER_ID" ]] && Q+=("ownerId=${OWNER_ID}")
[[ -n "$MEMBER_ID" ]] && Q+=("memberId=${MEMBER_ID}")
[[ -n "$PROPERTIES" ]] && Q+=("properties=${PROPERTIES}")
QS=""
if (( ${#Q[@]} > 0 )); then
  QS="?$(IFS='&'; echo "${Q[*]}")"
fi

URL="${VSSPS_BASE}/_apis/accounts${QS}"
URL+="$([[ -z "$QS" ]] && echo "?" || echo "&")api-version=${AZURE_DEVOPS_VSSPS_API_VERSION}"

curl_json "$URL"
