#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <subjectDescriptor> <imagePath> [--type image/png|image/jpeg]" >&2; exit 1; }
SUBJECT=${1:-}; IMG=${2:-}; shift 2 || true
[[ -z "$SUBJECT" || -z "$IMG" ]] && usage
CT="image/png"
if [[ "${1:-}" == "--type" ]]; then CT=${2:-image/png}; fi

URL="${VSSPS_BASE}/_apis/graph/avatars/${SUBJECT}?api-version=7.2-preview.1"

curl -sS -X PUT \
  -u ":${AZURE_DEVOPS_PAT}" \
  -H "Content-Type: ${CT}" \
  --data-binary @"${IMG}" \
  "$URL" | jq .
