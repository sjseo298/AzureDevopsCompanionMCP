#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

PROJECT="${1:-}"
TEAM="${2:-}"
if [[ -z "$PROJECT" ]]; then
  echo "Uso: $0 <project> [team]" >&2
  exit 2
fi

if [[ -n "$TEAM" ]]; then
  URL="${DEVOPS_BASE}/${PROJECT}/${TEAM}/_apis/work/backlogs?api-version=${AZURE_DEVOPS_API_VERSION}"
else
  URL="${DEVOPS_BASE}/${PROJECT}/_apis/work/backlogs?api-version=${AZURE_DEVOPS_API_VERSION}"
fi

RESPONSE=$(curl_json "$URL")
if echo "$RESPONSE" | jq . >/dev/null 2>&1; then
  echo "$RESPONSE" | jq
else
  echo "Respuesta no es JSON válido:" >&2
  echo "$RESPONSE"
  exit 1
fi
