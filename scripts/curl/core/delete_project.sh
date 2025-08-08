#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <projectId>" >&2; exit 1; }
PROJECT_ID=${1:-}
[[ -z "$PROJECT_ID" ]] && usage

URL="${DEVOPS_BASE}/_apis/projects/${PROJECT_ID}?api-version=7.2-preview.4"

curl -sS -X DELETE -H "Authorization: Basic ${BASIC_AUTH}" "$URL" -o /dev/null -w '%{http_code}\n'
