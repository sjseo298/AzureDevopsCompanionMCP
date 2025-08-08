#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 [--mine] [--top N] [--skip N]" >&2; exit 1; }
MINE=false; TOP=""; SKIP=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --mine) MINE=true; shift;;
    --top) TOP=${2:-}; shift 2;;
    --skip) SKIP=${2:-}; shift 2;;
    *) echo "Flag desconocida: $1" >&2; usage;;
  esac
done

QP=("api-version=7.2-preview.3")
[[ "$MINE" == true ]] && QP+=("mine=true")
[[ -n "$TOP" ]] && QP+=("top=$TOP")
[[ -n "$SKIP" ]] && QP+=("skip=$SKIP")
QS="$(IFS='&'; echo "${QP[*]}")"
URL="${DEVOPS_BASE}/_apis/teams?${QS}"

curl_json "$URL" | jq .
