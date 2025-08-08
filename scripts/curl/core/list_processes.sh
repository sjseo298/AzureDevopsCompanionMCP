#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() {
  cat >&2 <<'EOF'
Uso: list_processes.sh

Parámetros (api_doc/core.md › Processes/List):
  —  No requiere parámetros.

Ejemplos:
  ./list_processes.sh | jq '.value[].name'
EOF
  exit 1
}

# sin parámetros; mostrar ayuda si se pasan argumentos inesperados
[[ $# -gt 0 ]] && usage

URL="${DEVOPS_BASE}/_apis/process/processes?api-version=7.2-preview.1"

curl_json "$URL" | jq .
