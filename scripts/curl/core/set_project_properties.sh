#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <projectId> <name> <value>" >&2; exit 1; }
PROJECT_ID=${1:-}; NAME=${2:-}; VALUE=${3:-}
[[ -z "$PROJECT_ID" || -z "$NAME" || -z "$VALUE" ]] && usage

DATA=$(jq -n --arg n "$NAME" --arg v "$VALUE" '[{op:"add", path:"/"+$n, value:$v}]')
URL="${DEVOPS_BASE}/_apis/projects/${PROJECT_ID}/properties?api-version=7.2-preview.1"

curl -sS -X PATCH \
  -H "Authorization: Basic ${BASIC_AUTH}" \
  -H 'Content-Type: application/json' \
  -d "$DATA" \
  "$URL" | jq .
