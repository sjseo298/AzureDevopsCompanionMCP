#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <subjectDescriptor> [--out <archivo>]" >&2; exit 1; }
SUBJECT=${1:-}; shift || true
[[ -z "$SUBJECT" ]] && usage
OUT_FILE=""
if [[ "${1:-}" == "--out" ]]; then OUT_FILE=${2:-}; [[ -z "$OUT_FILE" ]] && usage; fi

URL="${VSSPS_BASE}/_apis/graph/avatars/${SUBJECT}?api-version=7.2-preview.1"

if [[ -n "$OUT_FILE" ]]; then
  curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: image/*" "$URL" -o "$OUT_FILE"
  echo "Guardado en: $OUT_FILE"
else
  # Devuelve base64 para inspección rápida
  curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: image/*" "$URL" | base64 -w0 | sed 's/.*/{"data":"&"}/' | jq .
fi
