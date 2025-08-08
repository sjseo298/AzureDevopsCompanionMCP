#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() {
  cat >&2 <<'EOF'
Uso: $0 <projectIdOrName>

Parámetros (api_doc/core.md › Projects/Get Project):
  projectIdOrName  ID GUID o nombre del proyecto (obligatorio)

Ejemplos:
  $0 Gerencia_Tecnologia
  $0 985807ad-7ff9-438d-849c-794c9bbc50f4
EOF
  exit 1
}

PROJECT_ID=${1:-}
[[ -z "$PROJECT_ID" ]] && usage

urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PID_ENC=$(urlencode "$PROJECT_ID")

URL="${DEVOPS_BASE}/_apis/projects/${PID_ENC}?api-version=7.2-preview.4"

curl_json "$URL" | jq .
