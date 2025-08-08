#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() {
  echo "Uso: $0 <project> <team> <board> <name> [--width N] [--height N] [--showDetails true|false] [--title T]" >&2
  exit 1
}

PROJECT=${1:-}
TEAM=${2:-}
BOARD=${3:-}
NAME=${4:-}
shift 4 || true
WIDTH=""
HEIGHT=""
SHOWDETAILS=""
TITLE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --width) WIDTH=${2:-}; shift 2;;
    --height) HEIGHT=${2:-}; shift 2;;
    --showDetails) SHOWDETAILS=${2:-}; shift 2;;
    --title) TITLE=${2:-}; shift 2;;
    *) echo "Flag desconocida: $1" >&2; usage;;
  esac
done

[[ -z "$PROJECT" || -z "$TEAM" || -z "$BOARD" || -z "$NAME" ]] && usage

urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PROJECT_ENC=$(urlencode "$PROJECT")
TEAM_ENC=$(urlencode "$TEAM")
BOARD_ENC=$(urlencode "$BOARD")
NAME_ENC=$(urlencode "$NAME")

QS=("api-version=${AZURE_DEVOPS_API_VERSION}")
[[ -n "$WIDTH" ]] && QS+=("width=${WIDTH}")
[[ -n "$HEIGHT" ]] && QS+=("height=${HEIGHT}")
[[ -n "$SHOWDETAILS" ]] && QS+=("showDetails=${SHOWDETAILS}")
[[ -n "$TITLE" ]] && QS+=("title=$(urlencode "$TITLE")")
QUERY="$(IFS='&'; echo "${QS[*]}")"

URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/boards/${BOARD_ENC}/chartimages/${NAME_ENC}?${QUERY}"

# Devuelve imagen binaria; para validar, mostramos headers
curl -sS -D - -o /dev/null -u ":${AZURE_DEVOPS_PAT}" "$URL"
