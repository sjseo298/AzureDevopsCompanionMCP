#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

URL="${DEVOPS_BASE}/_apis/projects?api-version=${AZURE_DEVOPS_API_VERSION}"

RESPONSE=$(curl_json "$URL")
if echo "$RESPONSE" | jq . >/dev/null 2>&1; then
  echo "$RESPONSE" | jq
else
  echo "Respuesta no es JSON válido:" >&2
  echo "$RESPONSE"
  exit 1
fi
